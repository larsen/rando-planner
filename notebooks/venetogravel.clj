^{:nextjournal.clerk/visibility {:code :hide}}
(ns notebooks.venetogravel
  (:require [rando-planner.gpx :as gpx]
            [rando-planner.diagram :as diagram]
            [rando-planner.leaflet :as leaflet]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as v]))

;; # Venetogravel 2024

;; ## Notes and planning

;; I'm considering the Short Lake route,
;; consisting in a total of 400 kilometers.

(def gpx-resource "gpx/VG-2024_400k_lake_provvis.gpx")

(clerk/with-viewer leaflet/leaflet-gpx-viewer
  {:gpx gpx-resource})

;; The route does not show particularly challenging climbs.

(let [elevation (gpx/elevation gpx-resource)]
  (clerk/html
   [:svg {:width 600 :height 200}
    (diagram/elevation-diagram {:elevation elevation
                                :with-legend true
                                :from 0
                                :to 400
                                :viewbox [0 0 600 200]})]))

;; I think I can maintain an average speed of 20 km/h
;; (this quantity ought to be further decreased because of how
;; I use the parameter, that is it includes time I spend in short pauses
;; to take pictures, consult the map, eat, and stuff like that…)

(def average-speed 20)

;; Resulting total time

^{:nextjournal.clerk/visibility {:code :fold}}
(defn total-time-required []
  (/ (gpx/total-distance gpx-resource)
     average-speed))

(total-time-required)

;; ### Scenari

;; The first scenario starts in the afternoon of the first day.
;; Pedaling until dark then find a place for the night.
;; The second day is the largest effort.

(def plan-start-19
  {:description "Starting on April 19th"
   :gpx gpx-resource
   :average-speed average-speed
   :daily-plans [{:label "Day 1"
                  :date "2024-04-19"
                  :activities [{:start "15:00" :length 6 :type :ride}]}
                 {:label "Day 2"
                  :date "2024-04-20"
                  :activities [{:start "07:00" :length 5 :type :ride}
                               {:start "17:00" :length 3 :type :ride}]}
                 {:label "Day 3"
                  :date "2024-04-21"
                  :activities [{:start "08:00" :length 6 :type :ride}]}]})

(clerk/html
 (diagram/plan->diagram plan-start-19))

(clerk/with-viewer leaflet/leaflet-gpx-viewer plan-start-19)


;; Another possibility is starting on the 20th,
;; so having a long ahead immediately. Perhaps too ambitious?

(def plan-start-20
  {:description "Partenza il 20"
   :gpx gpx-resource
   :average-speed average-speed
   :daily-plans [{:label "Giorno 1"
                  :date "2024-04-20"
                  :activities [{:start "8:00" :length 8 :type :ride}]}
                 {:label "Giorno 2"
                  :date "2024-04-21"
                  :activities [{:start "07:00" :length 5 :type :ride}
                               {:start "17:00" :length 3 :type :ride}]}
                 {:label "Giorno 3"
                  :date "2024-04-22"
                  :activities [{:start "09:00" :length 4 :type :ride}]}]})


(clerk/html
 (diagram/plan->diagram plan-start-20))

(clerk/with-viewer leaflet/leaflet-gpx-viewer plan-start-20)
