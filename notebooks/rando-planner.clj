^{:nextjournal.clerk/visibility {:code :hide}}
(ns notebooks.rando-planner
    (:require [rando-planner.gpx :as gpx]
              [rando-planner.diagram :as diagram]
              [rando-planner.leaflet :as leaflet]
              [nextjournal.clerk :as clerk]
              [nextjournal.clerk.viewer :as v]))

;; #### Custom viewers for [Clerk](https://github.com/nextjournal/clerk) for planning multi-day bike routes

^{:nextjournal.clerk/visibility {:code :hide}}
(javax.imageio.ImageIO/read
 (java.net.URL.
  "https://stefanorodighiero.net/misc/rando-planner-cover.jpg"))

;; # rando-planner 🚲


;; **rando-planner**:

;; - allows to break down the route with more control, providing
;; visualization tools that help evaluating and comparing different
;; plans

;; - assumes the user already have a GPX track defined (provided for
;; example by the organizers of a bike event), and let the user plan
;; the daily effort to a higher granularity.

;; - is based on Clerk, a notebook library for Clojure


;; Traditional tools (Komoot, Strava, Cycle.travel, and so on…) focus
;; on planning the route itself, and in some cases (Komoot) providing
;; extra tools to divide the route into multiple days, distributing
;; the distance to cover roughly the same way.

;; After a data structure called plan is defined, rando-planner provides different types of visualization that can be used to study different strategies to cover the distance.

;; This page has been composed with Clerk and rando-planner


;; ## Features

;; - Schematic visualization of the plan, that includes:
;;   - running count of kilometers
;;   - elevation
;;   - scheduled pauses
;;   - total amount of kilometers accumulated every day
;;   - sunrise and sunset indicators (WIP)

;; - Map visualization (based
;;   on [Leaflet](https://github.com/Leaflet/Leaflet)
;;   and [leaflet-gpx](https://github.com/mpetazzoni/leaflet-gpx)) of
;;   the route, with markers corresponding to the location reached
;;   after each day is completed.

;; ## Usage example

;; Suppose you want to plan a bike trip from Berlin to Rostock, in
;; Spring. You already prepared a GPX route using your favorite tool,
;; but you want to study how can you cover the distance over multiple
;; days.

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html [:div {:class "bg-amber-100 p-2"}
             [:h3 {:class "!text-black"} "⚠️ Important note"]
             [:span {:class "text-black"}
              "In general, it's not a good idea to stick to distance
plans when preparing to a bike journey.  rando-planner is intended as
a tool to assist in studying different scenarios, and prepare for them
as much as possible. But things happen, and you can not know in
advance what you will encounter (bad weather, a closed road, a road
you really want to explore, and so forth…). Falling below your daily
distance plans could be morale crushing."]])

;; Put the GPX file in the `resources/` directory (or a
;; sub-directory). For the sake of the example, let's say you choose
;; the directory `resources/gpx/`.


;; First of all define the notebook namespace, and require some packages
;; we're going to use:

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/code
 '(ns notebooks.berko2024
    (:require [rando-planner.diagram :as diagram]
              [rando-planner.gpx :as gpx]
              [rando-planner.plan :as plan]
              [rando-planner.leaflet :as leaflet]
              [nextjournal.clerk :as clerk])))

;; You are ready to visualize the route in your Clerk notebook.

(clerk/with-viewer leaflet/leaflet-gpx-viewer
  ;; The simplest plan is no plan!
  ;; (no need to specify `resources` in the path)
  {:gpx "gpx/be-rostock.gpx"})

;; So far so good. How many kilometers you have to cover to reach Copenhagen?

(gpx/total-distance "gpx/be-rostock.gpx")

;; But distance alone is not enough to get an idea. Let's have a look
;; at the elevation

(let [elevation (gpx/elevation "gpx/be-rostock.gpx")]
  (clerk/html
   [:svg {:width 600 :height 200}
    (diagram/elevation-diagram {:elevation elevation
                                :with-legend true
                                :from 0
                                :to (gpx/total-distance "gpx/be-rostock.gpx")
                                :viewbox [0 0 600 200]})]))


;; Let's say we feel like we can maintain an
;; average speed of 18 km/h during the entire journey.
;; Let's see how many hours on saddle will result:

(def average-speed 18)

(/ (gpx/total-distance "gpx/be-prg.gpx")
   average-speed)

;; So, probably something we can pull in two days. But how, exactly?
;; Let's put together a plan. First idea would be to split the time
;; in two equivalent days:

(def equally-split-plan
  {:gpx "gpx/be-prg.gpx"
   :average-speed average-speed
   :daily-plans [{:date "2024-04-01"
                  :label "First day"
                  :activities [{:start "08:00" :type :ride :length 10}]}
                 {:date "2024-04-02"
                  :label "Second day"
                  :activities [{:start "08:00" :type :ride :length 10}]}]})

;; We defined the GPX route we're going to use, the average speed we
;; intend to maintain, and we defined a vector of plans (one for each
;; day).  Each individual plan contains the date when we're going to
;; ride, and a vector of activities. We can now obtain a diagram of
;; the journey

(clerk/with-viewer diagram/plan-viewer equally-split-plan)

;; Each square in the diagram corresponds to one hour of ride.
;; The color of the square displays the light conditiion at the time
;; (this is why is importante to prodive a `:date` in the plan:
;; rando-planner uses that, and the GPX route, to calculate when the
;; Sun is setting in a particular place and time)

;; The plan diagram looks fine, but we notice we didn't account the
;; time we'll spend having lunch. Let's make a new one:

(def equally-split-plan-with-pauses
  {:gpx "gpx/be-prg.gpx"
   :average-speed average-speed
   :daily-plans [{:date "2024-04-01"
                  :label "First day"
                  :activities [{:start "08:00" :type :ride :length 5}
                               {:start "15:00" :type :ride :length 5}]}
                 {:date "2024-04-02"
                  :label "Second day"
                  :activities [{:start "08:00" :type :ride :length 5}
                               {:start "15:00" :type :ride :length 5}]}]})

(clerk/with-viewer diagram/plan-viewer equally-split-plan-with-pauses)

;; The plan diagram looks almost exactly the same, but notice two things:

;; - There are two markers (one for each day) indicating when, and for
;;   how long, the pause is taking place

;; - As a consequence of the two hours pauses, we're going to have to
;;   ride more time in the dark.

;; Notice, also, that we can use the notebook to visually compare two
;; (or more) plans for the same route

;; This plan looks promising, let's see on the map where the first day
;; of riding will bring us. We're going to use the same viewer we used
;; before, but since the plan is now more detailed, it will as well
;; display more details

(clerk/with-viewer leaflet/leaflet-gpx-viewer equally-split-plan-with-pauses)

;; You can zoom in and use the markers to see where you'll land at the
;; end of the first day. Intermediate markers, when clicked, display a
;; popup that shows the label of the day that _ends_ there, and the
;; amount of kilometers planned for that day. With this information
;; you can use other tools and services to find an accomodation.
