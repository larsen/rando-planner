(ns rando-planner.diagram
  (:require [clojure.string :as str]
            [nextjournal.clerk :as clerk]
            [rando-planner.gpx :as gpx]
            [rando-planner.plan :as plan]))

(defn viewbox-dimensions-to-str [viewbox-dimensions]
  (str/join " " (map str viewbox-dimensions)))

(defn points->path
  "Turn a sequence of points into an SVG path string."
  [[{start-x :x, start-y :y} & pts]]
  (reduce str
          (str "M " start-x "," start-y)
          (map (fn [{x :x, y :y}]
                 (str " L" x "," y)) pts)))

(defn pointspace-to-viewbox-space [{:keys [x y pointspace viewbox]}]
  (let [[pointspace-min-x pointspace-max-x
         pointspace-min-y pointspace-max-y] pointspace
        [min-x min-y width height] viewbox]
    {:x (/ (* (Math/abs (- pointspace-min-x x)) (- width min-x))
           (- pointspace-max-x pointspace-min-x))
     :y (/ (* (- pointspace-max-y y) (- height min-y))
           (- pointspace-max-y pointspace-min-y))}))

(defn points-in-viewbox-space [elevation pointspace viewbox]
  (map (fn [{x :kilometer, y :elevation}]
         (pointspace-to-viewbox-space
          {:x x :y y
           :pointspace pointspace
           :viewbox viewbox}))
       elevation))

(def box-size 10)
(def left-margin 50)
(def diagram-width 600)
(def diagram-height 450)
(def viewbox-dimensions [0 0 diagram-width diagram-height])
(def viewbox-dimensions-as-str
  (viewbox-dimensions-to-str viewbox-dimensions))

(defn elevation-diagram [{:keys [elevation from to viewbox]}]
  (let [selected-elevation (filter (fn [{x :kilometer}]
                                     (and (>= x from)
                                          (< x to)))
                                   elevation)
        min-elevation 0
        max-elevation (->> elevation
                           (map :elevation)
                           (apply max))
        points (points-in-viewbox-space selected-elevation
                                        [from to
                                         min-elevation
                                         max-elevation]
                                        viewbox)]
    [:path {:stroke "orange"
            :stroke-width 1
            :fill "none"
            :d (points->path points)}]))

(defn pauses-diagram [{:keys [pauses]}]
  (loop [i 0
         pauses-diagram [:g]]
    (if (< i (count pauses))
      (let [p (nth pauses i)]
        (recur (inc i)
               (conj pauses-diagram
                     (let [position-x (+ left-margin
                                         (* (:after (nth pauses i)) box-size))]
                       [:g
                        [:polygon {
                                   :points (str/join \space
                                                     (map #(str/join \, %)
                                                          [[position-x 0]
                                                           [(- position-x 2) -2]
                                                           [(+ position-x 2) -2]]))}]
                        [:text {:x position-x
                                :y -3
                                :font-family "Fira Sans"
                                :font-size ".25em"
                                :text-anchor "middle"}
                         (str (:length (nth pauses i)))]]))))
      [:g {:transform "translate(0 29)"}
       pauses-diagram])))

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
        (plan/time-after-n-hours (:start activity) (+ 1 n))]
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
  (let [pauses (plan/pauses day-plan)
        main-offset (* (/ km average-speed) box-size)
        total-km-for-day (plan/kilometers-in-a-day day-plan
                                                   average-speed)]
    (into [:svg
           [:text {:x 0 :y 15
                   :font-family "Fira Sans Condensed"
                   :font-size ".35em"
                   :dominant-baseline "middle"}
            (:label day-plan)]
           [:text {:x 0 :y 22
                   :font-family "Fira Sans"
                   :font-size ".28em"
                   :dominant-baseline "middle"}
            (str "~" total-km-for-day " km")]
           [:g {:transform (str "translate(" (+ left-margin
                                                main-offset) " 0)")}
            (elevation-diagram {:elevation elevation
                                :from km
                                :to (+ km total-km-for-day)
                                :viewbox [0 0 (* box-size
                                                 (/ total-km-for-day
                                                    average-speed)) 20]})]
           (pauses-diagram {:pauses pauses})
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
               [:g {:transform "translate(0 20)"}
                activities-diagram]))])))

(defn plan-title [description]
  [:svg
   [:text {:x 0 :y 20                  ; guessing the baseline position
           :font-family "Fira Sans Condensed"
           :font-size ".5em"}
    description]])

(defn plan-main-kilometers-svg [total-distance average-speed elevation]
  (into [:svg]
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
    [:svg {:width diagram-width
           :height diagram-height
           :viewBox "0 0 300 230"}
     (plan-title (:description plan))
     [:g {:transform "translate(0 25)"}
      (plan-main-kilometers-svg total-distance
                                average-speed
                                elevation)]
     (loop [i 0
            total-kilometers-covered 0
            output [:g]]
       (if (< i (count (:daily-plans plan)))
         (recur (inc i)
                (+ total-kilometers-covered
                   (plan/kilometers-covered (nth (:daily-plans plan) i)
                                            average-speed))
                (conj output [:g {:transform (str "translate(0 "
                                                  (+ 50
                                                     (* i 50)) ")")}
                              (day-plan->svg (nth (:daily-plans plan) i)
                                             total-kilometers-covered
                                             average-speed
                                             elevation)]))
         output))]))
