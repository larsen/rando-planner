^{:nextjournal.clerk/visibility {:code :hide}}

(ns notebooks.venetogravel
  (:require [nextjournal.clerk :as clerk]
            [clojure.java.io :as io]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.coerce :as c]
            [clojure.string :as str]
            [rando-planner.gpx :as gpx]))

;; # Venetogravel 2024

;; ## Note e pianificazione

;; Il percorso che sto considerando è lo Short Lake,
;; che consiste in un totale di 400 chilometri.

(def total-distance 400)

;; Penso di poter mantenere una velocità media di 20 km/h
;; lungo tutto il percorso (con adeguato riposo)

(def average-speed 20)

;; Ne risulta un tempo totale

^{:nextjournal.clerk/visibility {:code :fold}}
(defn total-time-required []
  (/ total-distance
     average-speed))

(total-time-required)

(defn string-to-time [time-str]
  (let [formatter (f/formatter "HH:mm")]
    (f/parse formatter time-str)))

(defn time-to-string [dt]
  (let [formatter (f/formatter "HH:mm")]
    (f/unparse formatter dt)))

;; ### Processare il file GPX


(def vg2023-gpx-altitude
  (gpx/process-gpx "VG23-SHORT-LAKE-DEF.gpx"))

^{:nextjournal.clerk/visibility {:code :hide}}
(defn viewbox-dimensions-to-str [viewbox-dimensions]
  (str/join " " (map str viewbox-dimensions)))

^{:nextjournal.clerk/visibility {:code :hide}}
(defn points->path
  "Turn a sequence of points into an SVG path string."
  [[{start-x :x, start-y :y} & pts]]
  (reduce str
          (str "M " start-x "," start-y)
          (map (fn [{x :x, y :y}]
                 (str " L" x "," y)) pts)))

(defn point-to-viewbox-space [x y
                              min-x max-x
                              min-y max-y
                              viewbox-x0 viewbox-y0
                              viewbox-x viewbox-y]
  {:x (/ (* (- max-x x) (- viewbox-x viewbox-x0))
         (- max-x min-x))
   :y (/ (* (- max-y y) (- viewbox-y viewbox-y0))
         (- max-y min-y))})

^{:nextjournal.clerk/visibility {:code :hide}}
(defn altimetry-diagram [altimetry from to vx0 vy0 vx vy]
  (let [selected-altimetry (filter (fn [{x :kilometer, y :altitude}]
                                     (and (>= x from)
                                          (< x to)))
                                   altimetry)
        min-altitude 0
        max-altitude (->> altimetry
                          (map :altitude)
                          (apply max))
        points-in-viewbox-space
        (map (fn [{x :kilometer, y :altitude}]
               (point-to-viewbox-space x y
                                       from to
                                       min-altitude max-altitude
                                       vx0 vy0 vx vy))
             selected-altimetry)]
    [:path {:stroke "orange"
            :stroke-width 1
            :fill "none"
            :d (points->path points-in-viewbox-space)}]))

;; ### Scenari

;; #### Functioni per la rappresentazione degli scenari

(def box-size 10)
(def left-margin 50)
(def diagram-width 600)
(def diagram-height 70)
(def viewbox-dimensions [0 0 diagram-width diagram-height])
(def viewbox-dimensions-as-str
  (viewbox-dimensions-to-str viewbox-dimensions))

^{:nextjournal.clerk/visibility {:code :fold}}
(defn single-activity-streak [activity
                              offset-of-previous-activities
                              kilometers]
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

^{:nextjournal.clerk/visibility {:code :fold}}
(defn day-plan->svg [day-plan km]
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
                                                              kilometers))))))
               [:svg
                [:g {:transform (str "translate(" main-offset ",0)")}
                 (altimetry-diagram vg2023-gpx-altitude 0 400 0 0 150 25)]
                activities-diagram]))])))

; Una funzione per calcolare il numero di kilometri percorsi durante una
; giornata, dati i parametri iniziali

^{:nextjournal.clerk/visibility {:code :fold}}
(defn kilometers-covered [day-plan]
  (* average-speed
     (reduce + (map :length
                    (filter #(= :ride (:type %))
                            (:activities day-plan))))))

; Una funzione per convertire un piano di gara in un diagramma

^{:nextjournal.clerk/visibility {:code :fold}}
(defn plan-title [plan]
  [:svg {:width diagram-width
         :height diagram-height
         :viewBox viewbox-dimensions-as-str}
   [:text {:x 0 :y 20
           :font-family "Fira Sans Condensed"
           :font-size ".5em"}
    (:description plan)]])

^{:nextjournal.clerk/visibility {:code :fold}}
(defn plan-main-kilometers-svg []
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

^{:nextjournal.clerk/visibility {:code :fold}}
(defn plan->diagram [plan]
  (clerk/html
   [:g
    (plan-title plan)
    (plan-main-kilometers-svg)
    (loop [i 0
           total-kilometers-covered 0
           output [:g]]
      (if (< i (count (:daily-plans plan)))
        (recur (inc i)
               (+ total-kilometers-covered
                  (kilometers-covered (nth (:daily-plans plan) i)))
               (conj output (day-plan->svg (nth (:daily-plans plan) i)
                                           total-kilometers-covered)))
        output))]))

(def plan-start-19
  {:description "Starting on April 19th"
   :daily-plans [{:label "Day 1"
                  :activities [{:start "15:00" :length 6 :type :ride}]}
                 {:label "Day 2"
                  :activities [{:start "07:00" :length 5 :type :ride}
                               {:start "17:00" :length 3 :type :ride}]}
                 {:label "Day 3"
                  :activities [{:start "08:00" :length 6 :type :ride}]}]})

(plan->diagram plan-start-19)

(def plan-start-20
  {:description "Partenza il 20"
   :daily-plans [{:label "Giorno 1"
                  :activities [{:start "8:00" :length 8 :type :ride}]}
                 {:label "Giorno 2"
                  :activities [{:start "07:00" :length 5 :type :ride}
                               {:start "17:00" :length 3 :type :ride}]}
                 {:label "Giorno 3"
                  :activities [{:start "09:00" :length 6 :type :ride}]}]})

(plan->diagram plan-start-20)
