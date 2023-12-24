^{:nextjournal.clerk/visibility {:code :hide}}
(ns notebooks.venetogravel
  (:require [rando-planner.gpx :as gpx]
            [rando-planner.diagram :as diagram]
            [nextjournal.clerk :as clerk]))

;; # Venetogravel 2024

;; ## Note e pianificazione

;; Il percorso che sto considerando è lo Short Lake,
;; che consiste in un totale di circa 400 chilometri.

(def gpx-resource "gpx/VG23-SHORT-LAKE-DEF.gpx")

;; Il percorso non presenta sezioni particuolarmente
;; impegnative quanto ad elevazione.

(let [elevation (gpx/elevation gpx-resource)]
  (clerk/html
   [:svg {:width 600 :height 200}
    (diagram/elevation-diagram {:elevation elevation
                                :from 0
                                :to 400
                                :viewbox [0 0 600 200]})]))

;; Penso di poter mantenere una velocità media di 20 km/h lungo tutto
;; il percorso (con adeguato riposo).  Questa quantità dovrebbe essere
;; in realtà ulteriormente abbassata, perché per come uso il dato
;; questo esprime anche tutto il tempo speso facendo pause (per fare
;; foto, consultare mappe, riposare, mangiare e bere, e cose di questo
;; tipo).

(def average-speed 20)

;; Ne risulta un tempo totale

^{:nextjournal.clerk/visibility {:code :fold}}
(defn total-time-required []
  (/ (gpx/total-distance gpx-resource)
     average-speed))

(total-time-required)

;; ### Scenari

(def plan-start-19
  {:description "Starting on April 19th"
   :gpx gpx-resource
   :average-speed average-speed
   :daily-plans [{:label "Day 1"
                  :activities [{:start "15:00" :length 6 :type :ride}]}
                 {:label "Day 2"
                  :activities [{:start "07:00" :length 5 :type :ride}
                               {:start "17:00" :length 3 :type :ride}]}
                 {:label "Day 3"
                  :activities [{:start "08:00" :length 6 :type :ride}]}]})

(diagram/plan->diagram plan-start-19)


(def plan-start-20
  {:description "Partenza il 20"
   :gpx gpx-resource
   :average-speed average-speed
   :daily-plans [{:label "Giorno 1"
                  :activities [{:start "8:00" :length 8 :type :ride}]}
                 {:label "Giorno 2"
                  :activities [{:start "07:00" :length 5 :type :ride}
                               {:start "17:00" :length 3 :type :ride}]}
                 {:label "Giorno 3"
                  :activities [{:start "09:00" :length 6 :type :ride}]}]})

(diagram/plan->diagram plan-start-20)
