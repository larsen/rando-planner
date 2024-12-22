(ns rando-planner.diagram
  (:require [clojure.string :as str]
            [tick.core :as tick]
            [rando-planner.gpx :as gpx]
            [rando-planner.plan :as plan]
            [rando-planner.suncalc :as suncalc]
            [nextjournal.clerk :as clerk]))

(alter-var-root #'nextjournal.clerk.view/include-css+js
                (fn [include-fn]
                  (fn [state]
                    (concat (include-fn state)
                            (list (hiccup.page/include-css "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"))))))

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

(def palette
  {:background "white"
   :text-color "black"
   :elevation-trend "darkgreen"
   :elevation-legend-stroke "black"
   :elevation-legend-text "black"
   :day-background1 "#eeeeff"
   :day-background2 "#ddddee"
   :pause-marker "green"
   :pause-text "darkgreen"
   :light-text "#0d3d56" ; Indigo
   :light-fill "#43abc9"
   :dark-text "white"
   :dark-fill "#0d3d56"})

(defn get-from-palette [element]
  (get palette element "pink"))

(def alternating-background (atom :day-background1))

(defn reset-alternating-background! []
  (reset! alternating-background :day-background1))

(defn get-alternating-background! []
  (get-from-palette
   (swap! alternating-background
          (fn [bkg]
            (if (= :day-background1 bkg)
              :day-background2
              :day-background1)))))

(defn elevation-diagram [{:keys [plan
                                 from to
                                 viewbox with-legend]}]
  (let [elevation (gpx/elevation (:gpx plan))
        average-speed (:average-speed plan)
        daily-stats (plan/daily-stats plan)
        selected-elevation (filter (fn [{x :kilometer}]
                                     (and (>= x from)
                                          (< x to)))
                                   elevation)
        min-elevation 0
        max-elevation (reduce max (map :elevation elevation))
        pointspace [from to min-elevation max-elevation]
        points (points-in-viewbox-space selected-elevation pointspace viewbox)
        {up-right-x :x up-right-y :y} (pointspace-to-viewbox-space
                                       {:x to :y max-elevation
                                        :pointspace pointspace
                                        :viewbox viewbox})
        {bottom-right-x :x bottom-right-y :y} (pointspace-to-viewbox-space
                                               {:x to :y min-elevation
                                                :pointspace pointspace
                                                :viewbox viewbox})]
    [:g
     (when with-legend
       [:g
        [:line {:x1 up-right-x :y1 up-right-y
                :x2 bottom-right-x :y2 bottom-right-y
                :stroke (get-from-palette :elevation-legend-stroke)}]
        ;; Ticks
        [:line {:x1 up-right-x :y1 up-right-y
                :x2 (- up-right-x 5) :y2 up-right-y
                :stroke (get-from-palette :elevation-legend-stroke)}]
        [:line {:x1 up-right-x :y1 bottom-right-y
                :x2 (- up-right-x 5) :y2 bottom-right-y
                :stroke (get-from-palette :elevation-legend-stroke)}]
        [:text {:x (- up-right-x 15)
                :y (- bottom-right-y 2)
                :font-family "Fira Sans"
                :font-size "60%"
                :fill (get-from-palette :elevation-legend-text)}
         (str min-elevation)]
        [:text {:x (- up-right-x 35)
                :y 2
                :font-family "Fira Sans"
                :font-size "60%"
                :dominant-baseline "hanging"
                :fill (get-from-palette :elevation-legend-stroke)}
         (str max-elevation)]])
     ;; TODO I should have finer control here
     (when (and with-legend daily-stats)
       (reset-alternating-background!)
       (for [d daily-stats]
         (let [{dx1 :x} (pointspace-to-viewbox-space
                         {:x (:covered d)
                          :y 0
                          :pointspace pointspace
                          :viewbox viewbox})
               {dx2 :x} (pointspace-to-viewbox-space
                         {:x (:kilometers d)
                          :y 0
                          :pointspace pointspace
                          :viewbox viewbox})]
           [:g
            [:text {:x (+ dx1 2)
                    :y 2
                    :font-family "Fira Sans"
                    :font-size "60%"
                    :font-weight "bold"
                    :dominant-baseline "hanging"
                    :fill (get-from-palette :elevation-legend-stroke)}
             (str (:label d))]
            [:text {:x (+ dx1 2)
                    :y 15
                    :font-family "Fira Sans"
                    :font-size "60%"
                    :dominant-baseline "hanging"
                    :fill (get-from-palette :elevation-legend-stroke)}
             (str "↔ " (:kilometers d) " km")]
            [:text {:x (+ dx1 2)
                    :y 27
                    :font-family "Fira Sans"
                    :font-size "60%"
                    :dominant-baseline "hanging"
                    :fill (get-from-palette :elevation-legend-stroke)}
             (str "▲ " (Math/floor (:elevation d)) " m")]
            [:rect {:x dx1
                    :y 0
                    :width dx2 :height bottom-right-y
                    :fill (get-alternating-background!)
                    :fill-opacity 0.4}]
            (when (:pauses d)
              (for [p (:pauses d)]
                (let [{px :x} (pointspace-to-viewbox-space
                               {:x (+ (:covered d)
                                      (* average-speed
                                         (- (:after p)
                                            (:cumulative-pause p))))
                                :y 0  ; not important, not used
                                :pointspace pointspace
                                :viewbox viewbox})]
                  [:g
                   [:line {:x1 px :y1 0 :x2 px :y2 bottom-right-y
                           :stroke (get-from-palette :elevation-legend-stroke)
                           :stroke-dasharray "4 2"}]
                   [:rect {:x (- px 10) :y (- bottom-right-y 12)
                           :width 20 :height 12
                           :fill (get-from-palette :pause-marker)}]
                   [:text {:x px :y (- bottom-right-y 2)
                           :text-anchor "middle"
                           :font-weight "bold"
                           :font-family "Fira Sans"
                           :font-size "55%"
                           :fill (get-from-palette :dark-text)}
                    (str (:length p) "h")]])))])))
     [:path {:stroke (get-from-palette :elevation-trend)
             :stroke-width 1
             :fill "none"
             :d (points->path points)}]]))

(def elevation-viewer
  {:transform-fn (comp clerk/mark-presented
                       (clerk/update-val
                        #(clerk/html
                          (let [total-distance (gpx/total-distance (:gpx %))
                                viewbox [0 0 600 200]]
                            [:svg {:width 600
                                   :height 200
                                   :style {:background-color (get-from-palette :background)}}
                             (elevation-diagram {:plan %
                                                 :from 0
                                                 :to total-distance
                                                 :with-legend true
                                                 :viewbox viewbox})]))))})

(defn pauses-diagram [{:keys [pauses]}]
  (loop [i 0
         pauses-diagram [:g]]
    (if (< i (count pauses))
      (let [p (nth pauses i)]
        (recur (inc i)
               (conj pauses-diagram
                     (let [position-x (* (- (:after p)
                                            (:cumulative-pause p))
                                         box-size)
                           triangle-vertexes [[position-x 0]
                                              [(- position-x 2) -2]
                                              [(+ position-x 2) -2]]]
                       [:g
                        [:polygon {:style {:fill (get-from-palette :pause-marker)}
                                   :points (->> triangle-vertexes
                                                (map (partial str/join \,))
                                                (str/join \space))}]
                        [:text {:x position-x
                                :y -3
                                :font-family "Fira Sans"
                                :font-size ".20em"
                                :fill (get-from-palette :pause-text)
                                :text-anchor "middle"}
                         (str (:length p) " hour(s)")]]))))
      pauses-diagram)))

(defn single-activity-streak [{:keys [activity day-plan
                                      average-speed
                                      gpx-center
                                      offset-of-previous-activities
                                      kilometers]}]
  (let [[lat lon] gpx-center
        [sunrise sunset] (suncalc/sunset-sunrise-times
                          (+ (/ 86400 2)
                             (/ (plan/date-to-timestamp
                                 (str (:date day-plan) "T00:00"))
                                1000))
                          lat lon
                          ;; TODO The altitude should be configurable
                          ;; by the user
                          0)
        margin (+ left-margin offset-of-previous-activities)]
    (for [n (range (:length activity))]
      (let [start (tick/date-time
                   (str (:date day-plan) "T" (:start activity)))
            t1 (tick/>> start (tick/new-duration (+ 1 n) :hours))
            t1-str (tick/format (tick/formatter "hh:mm") t1)
            before-sunrise? (tick/< t1 sunrise)
            after-sunset? (tick/> t1 sunset)]
        [:g
         [:text {:x (+ margin box-size (* n box-size))
                 :y 25
                 :font-family "Fira Sans"
                 :font-size ".2em"
                 :text-anchor "middle"}
          t1-str]
         [:rect {:x (+ margin (* n box-size))
                 :y box-size
                 :width box-size :height box-size
                 :fill (get-from-palette
                        (if (or before-sunrise? after-sunset?)
                          :dark-fill
                          :light-fill))
                 :stroke "black"
                 :stroke-width 0.5}]
         [:text {:x (+ margin 5 (* n box-size))
                 :y 17
                 :font-family "Fira Sans"
                 :font-size ".22em"
                 :text-anchor "middle"
                 :fill (get-from-palette
                        (if (or before-sunrise? after-sunset?)
                          :dark-text
                          :light-text))}
          (str (+ kilometers (* average-speed (+ 1 n))))]]))))

(defn day-plan->svg [plan index km center]
  (let [day-plan (nth (:daily-plans plan) index)
        average-speed (average-speed day-plan plan)
        elevation (gpx/elevation (:gpx plan))
        pauses (plan/pauses day-plan)
        main-offset (* (/ km average-speed) box-size)
        total-km-for-day (plan/kilometers-in-a-day day-plan average-speed)]
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
            (str "↔ ~" total-km-for-day " km")]
           [:text {:x 0 :y 29
                   :font-family "Fira Sans"
                   :font-size ".28em"
                   :dominant-baseline "middle"}
            (str "▴ ~" (Math/floor
                        (gpx/elevation-gain
                         elevation km (+ km total-km-for-day))) " m")]
           [:g {:transform (str "translate(" (+ left-margin
                                                main-offset) " 0)")}
            (elevation-diagram {:plan plan
                                :from km
                                :to (+ km total-km-for-day)
                                :viewbox [0 0 (* box-size
                                                 (/ total-km-for-day
                                                    average-speed)) 20]})]
           [:g {:transform (str "translate(" (+ left-margin
                                                main-offset) " 29)")}
            (pauses-diagram {:pauses pauses})]
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
                                      (single-activity-streak
                                       {:activity activity
                                        :offset-of-previous-activities offset
                                        :kilometers kilometers
                                        :average-speed average-speed
                                        :day-plan day-plan
                                        :gpx-center center}))))))
               [:g {:transform "translate(0 20)"}
                activities-diagram]))])))

(defn plan-title [description]
  [:svg
   [:text {:x 0 :y 20                  ; guessing the baseline position
           :font-family "Fira Sans Condensed"
           :font-size ".5em"}
    description]])

(defn plan-main-kilometers-svg [total-distance average-speed]
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
                                  :fill (get-from-palette :background)
                                  :stroke "black"
                                  :stroke-width 0.5}]))
             output))
         ;; Opaque box to create the illusion of ticks
         [:rect {:x 0 :y 0 :width 400 :height 17 :fill (get-from-palette :background)}]
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

(defn plan-diagram [plan]
  (let [total-distance (gpx/total-distance (:gpx plan))
        center (gpx/center (gpx/points (:gpx plan)))
        average-speed (:average-speed plan)]
    [:svg {:width diagram-width
           :height diagram-height
           :viewBox "0 0 300 230"
           :style {:background-color (get-from-palette :background)}}
     (plan-title (:description plan))
     [:g {:transform "translate(0 25)"}
      (plan-main-kilometers-svg total-distance
                                average-speed)]
     (loop [index 0
            total-kilometers-covered 0
            output [:g]]
       (if (< index (count (:daily-plans plan)))
         (recur (inc index)
                (+ total-kilometers-covered
                   (plan/kilometers-in-a-day (nth (:daily-plans plan) index)
                                             average-speed))
                (conj output [:g {:transform (str "translate(0 "
                                                  (+ 50 (* index 50)) ")")}
                              (day-plan->svg plan index
                                             total-kilometers-covered
                                             center)]))
         output))]))

(def plan-viewer
  {:transform-fn (comp clerk/mark-presented
                       (clerk/update-val #(clerk/html (plan-diagram %))))})
