(ns rando-planner.diagram
  (:require [clojure.string :as str]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [nextjournal.clerk :as clerk]
            [rando-planner.gpx :as gpx]))

(defn string-to-time [time-str]
  (let [formatter (f/formatter "HH:mm")]
    (f/parse formatter time-str)))

(defn time-to-string [dt]
  (let [formatter (f/formatter "HH:mm")]
    (f/unparse formatter dt)))

(defn viewbox-dimensions-to-str [viewbox-dimensions]
  (str/join " " (map str viewbox-dimensions)))

(defn points->path
  "Turn a sequence of points into an SVG path string."
  [[{start-x :x, start-y :y} & pts]]
  (reduce str
          (str "M " start-x "," start-y)
          (map (fn [{x :x, y :y}]
                 (str " L" x "," y)) pts)))

(defn point-to-viewbox-space [{:keys [x y pointspace viewbox]}]
  (let [[pointspace-min-x pointspace-max-x
         pointspace-min-y pointspace-max-y] pointspace
        [min-x min-y width height] viewbox]
    {:x (/ (* (- pointspace-max-x x) (- width min-x))
           (- pointspace-max-x pointspace-min-x))
     :y (/ (* (- pointspace-max-y y) (- height min-y))
           (- pointspace-max-y pointspace-min-y))}))

(defn elevation-diagram [{:keys [elevation from to viewbox]}]
  (let [[min-x min-y width height] viewbox
        selected-elevation (filter (fn [{x :kilometer}]
                                     (and (>= x from)
                                          (< x to)))
                                   elevation)
        min-elevation 0
        max-elevation (->> elevation
                           (map :elevation)
                           (apply max))
        points-in-viewbox-space (map
                                 (fn [{x :kilometer, y :elevation}]
                                   (point-to-viewbox-space
                                    {:x x :y y
                                     :pointspace [from to
                                                  min-elevation
                                                  max-elevation]
                                     :viewbox [min-x min-y
                                               width height]}))
                                 selected-elevation)]
    [:path {:stroke "orange"
            :stroke-width 1
            :fill "none"
            :d (points->path points-in-viewbox-space)}]))

(def box-size 10)
(def left-margin 50)
(def diagram-width 600)
(def diagram-height 70)
(def viewbox-dimensions [0 0 diagram-width diagram-height])
(def viewbox-dimensions-as-str
  (viewbox-dimensions-to-str viewbox-dimensions))

(defn single-activity-streak [activity
                              offset-of-previous-activities
                              kilometers
                              average-speed]
  (let [type (:type activity)
        margin (+ left-margin
                  offset-of-previous-activities)
        color (if (= type :ride)
                "yellow"
                "white")]
    (for [n (range (:length activity))]
      [:g
       [:text {:x (+ margin box-size (* n box-size))
               :y 25
               :font-family "Fira Sans"
               :font-size ".2em"
               :text-anchor "middle"}
        (time-to-string (t/plus (string-to-time (:start activity))
                                (t/hours (+ 1 n))))]
       [:rect {:x (+ margin (* n box-size))
               :y box-size
               :width box-size :height box-size
               :fill color
               :stroke "black"
               :stroke-width 0.5}]
       [:text {:x (+ margin 5 (* n box-size)) :y 17
               :font-family "Fira Sans"
               :font-size ".22em"
               :text-anchor "middle"}
        (str (+ kilometers (* average-speed (+ 1 n))))]])))

(defn day-plan->svg [day-plan km average-speed elevation]
  (let [main-offset (* (/ km average-speed) box-size)]
    (into [:svg {:width diagram-width
                 :height diagram-height
                 :viewBox viewbox-dimensions-as-str}
           [:text {:x 0 :y 15
                   :font-family "Fira Sans Condensed"
                   :font-size ".35em"
                   :dominant-baseline "middle"}
            (:label day-plan)]
           (loop [i 0
                  kilometers km
                  elapsed-hours 0
                  offset main-offset
                  activities-diagram [:g]]
             (if (< i (count (:activities day-plan)))
               (let [activity (nth (:activities day-plan) i)
                     type (:type activity)]
                 (recur (inc i)
                        (if (= type :ride)
                          (+ kilometers
                             (* average-speed
                                (:length activity)))
                          kilometers)
                        (+ elapsed-hours
                           (:length activity))
                        (+ offset
                           (* box-size (:length activity)))
                        (conj activities-diagram
                              (into [:g]
                                    (when (= type :ride)
                                      (single-activity-streak activity
                                                              offset
                                                              kilometers
                                                              average-speed))))))
               [:svg
                [:g {:transform (str "translate(" main-offset ",0)")}
                 (elevation-diagram {:elevation elevation
                                     :from 0
                                     :to 400
                                     :viewbox [0 0 150 25]})]
                activities-diagram]))])))

(defn kilometers-covered [day-plan average-speed]
  (* average-speed
     (reduce + (map :length
                    (filter #(= :ride (:type %))
                            (:activities day-plan))))))

(defn plan-title [description]
  [:svg {:width diagram-width
         :height diagram-height
         :viewBox viewbox-dimensions-as-str}
   [:text {:x 0 :y 20
           :font-family "Fira Sans Condensed"
           :font-size ".5em"}
    description]])

(defn plan-main-kilometers-svg [total-distance average-speed]
  (into [:svg {:width diagram-width
               :height diagram-height
               :viewBox viewbox-dimensions-as-str}]
        [:g
         (loop [i 0
                output [:g]]
           (if (< i (/ total-distance average-speed))
             (recur (inc i)
                    (conj output
                          [:rect {:x (+ (* 5 box-size)
                                        (* i box-size))
                                  :y box-size
                                  :width box-size :height box-size
                                  :fill "white"
                                  :stroke "black"
                                  :stroke-width 0.5}]))
             output))
         ;; Opaque box to create the illusion of ticks
         [:rect {:x 0 :y 0 :width 400 :height 17 :fill "white"}]
         (loop [i 0
                output [:g]]
           (if (< i (/ total-distance average-speed))
             (recur (inc i)
                    (conj output
                          [:text {:x (+ left-margin 5 (* i box-size))
                                  :y 17
                                  :font-family "Fira Sans"
                                  :font-size ".25em"
                                  :text-anchor "middle"}
                           (str (* average-speed (+ 1 i)))]))
             output))]))

(defn plan->diagram [plan]
  (let [total-distance (gpx/total-distance (:gpx plan))
        elevation (gpx/elevation (:gpx plan))
        average-speed (:average-speed plan)]
    (clerk/html
     [:g
      (plan-title (:description plan))
      (plan-main-kilometers-svg total-distance average-speed)
      (loop [i 0
             total-kilometers-covered 0
             output [:g]]
        (if (< i (count (:daily-plans plan)))
          (recur (inc i)
                 (+ total-kilometers-covered
                    (kilometers-covered (nth (:daily-plans plan) i)
                                        average-speed))
                 (conj output (day-plan->svg (nth (:daily-plans plan) i)
                                             total-kilometers-covered
                                             average-speed
                                             elevation)))
          output))])))
