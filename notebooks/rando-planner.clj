^{:nextjournal.clerk/visibility {:code :hide}}
(ns notebooks.rando-planner
    (:require [rando-planner.gpx :as gpx]
              [rando-planner.plan :as plan]
              [rando-planner.diagram :as diagram]
              [rando-planner.leaflet :as leaflet]
              [nextjournal.clerk :as clerk]))


;; #### Custom viewers for [Clerk](https://github.com/nextjournal/clerk) for planning multi-day bike routes

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/image "rando-planner-index.png")

;; # [rando-planner](https://github.com/larsen/rando-planner) 🚲

^{:nextjournal.clerk/visibility {:code :hide}}
(let [example-plan {:gpx "gpx/be-rostock.gpx"
                    :average-speed 20
                    :daily-plans [{:date "2024-04-01"
                                   :label "First day"
                                   :activities [{:start "10:00" :type :ride :length 3}
                                                {:start "15:00" :type :ride :length 5}]}
                                  {:date "2024-04-02"
                                   :label "Second day"
                                   :activities [{:start "10:00" :type :ride :length 4}
                                                {:start "16:00" :type :ride :length 2.5}]}]}]
  (merge
   {:nextjournal/width :full}
   (clerk/row
    (clerk/col
     (clerk/with-viewer diagram/elevation-viewer example-plan))
    (clerk/col
     (clerk/with-viewer diagram/plan-viewer example-plan)))))

;; **rando-planner**:

;; - allows users to break down the route with more control, providing
;; visualization tools that help evaluating and comparing different
;; plans

;; - assumes the user already has a GPX track defined (provided for
;; example by the organizers of a bike event), and let the user plan
;; the daily effort to a higher granularity.

;; - is based on Clerk, a notebook library for Clojure

;; Traditional tools (such as Komoot, Strava, Cycle.travel, etc.)
;; focus on planning the route itself. Komoot offers an extra tool to
;; divide the route into multiple days, but they distribute distance
;; uniformly across the days. Cycle.travel allows to break down the
;; route at arbitrary points (and helps finding accomodation for the
;; night).

;; rando-planner provides different types of visualization that can be
;; used to study different strategies to cover the distance.

;; This page has been composed with Clerk and rando-planner

;; You can see the code for the project [on
;; github.com](https://github.com/larsen/rando-planner).


;; ## Features

;; - Schematic visualization of the plan, that includes:
;;   - running count of kilometers
;;   - elevation
;;   - scheduled pauses
;;   - total amount of kilometers accumulated every day
;;   - sunrise and sunset indicators (WIP)

;; - Map visualization (based
;;   on [Leaflet](https://github.com/Leaflet/Leaflet)) of the route,
;;   with markers corresponding to the location reached after each day
;;   is completed.

;; ## Usage example

;; Suppose you want to plan a bike trip from Berlin to Rostock, in the
;; Spring. You've already prepared a GPX route using your favorite
;; tool, but now you want to study different ways to cover the
;; distance over multiple days.

;; (you can download the route I'm using for this example
;; from [here](https://stefanorodighiero.net/misc/be-rostock.gpx))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html [:div {:class "bg-amber-100 p-2"}
             [:h3 {:class "!text-black"} "⚠️ Important note"]
             [:span {:class "text-black"}
              "In general, it's not a good idea to strictly adhere to
distance plans when preparing for a bike journey.  rando-planner is
intended as a tool to assist in studying different scenarios, and
preparing for them as much as possible. However, unexpected events can
occur, such as bad weather, road closures, or discovering a route you
really want to explore. Falling short your daily distance plans could
be morale-crushing."]])

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
              [rando-planner.leaflet :as leaflet]
              [nextjournal.clerk :as clerk])))

;; You are ready to visualize the route in your Clerk notebook.

(clerk/with-viewer leaflet/leaflet-gpx-viewer
  ;; The simplest plan is no plan!
  ;; (no need to specify `resources` in the path)
  {:gpx "gpx/be-rostock.gpx"})

;; So far so good. How many kilometers you have to cover to reach Rostock?

(gpx/total-distance "gpx/be-rostock.gpx")

;; Distance alone is not enough to get an idea of the effort. Let's
;; have a look at the elevation

(clerk/with-viewer diagram/elevation-viewer {:gpx "gpx/be-rostock.gpx"})

;; Let's say we feel like we can maintain an
;; average speed of 18 km/h throughout the entire journey.
;; Let's see how many hours on saddle will result:

(def average-speed 18)

(/ (gpx/total-distance "gpx/be-rostock.gpx")
   average-speed)

;; So, probably something we can pull in two days. But how, exactly?
;; Let's put together a plan. First idea would be to split the time
;; in two equivalent days:

^{::clerk/visibility {:result :hide}}
(def equally-split-plan
  {:gpx "gpx/be-rostock.gpx"
   :average-speed average-speed
   :daily-plans [{:date "2024-04-01"
                  :label "First day"
                  :activities [{:start "10:00" :type :ride :length 8}]}
                 {:date "2024-04-02"
                  :label "Second day"
                  :activities [{:start "10:00" :type :ride :length 6.5}]}]})

;; We've defined the GPX route we're going to use and the average
;; speed we intend to maintain. Additionally, we've defined a vector
;; of plans (one for each day).  Each individual plan contains the
;; date when we're going to ride and a vector of activities. With this
;; information, We can now obtain a diagram of the journey:

(clerk/with-viewer diagram/plan-viewer equally-split-plan)

;; Each square in the diagram corresponds to one hour of ride.  The
;; color of the square displays the light condition at that time.
;; This is why it is important to provide a `:date` in the plan:
;; rando-planner uses that, along with the GPX route, to calculate
;; when the Sun is setting in a particular place and time.

;; _(I am currently working to review the code responsible for this
;; functionality and ensure its accuracy)_

;; The plan diagram looks fine, but we notice we didn't plan any time to
;; have lunch. Let's make a new plan:

^{::clerk/visibility {:result :hide}}
(def equally-split-plan-with-pauses
  {:gpx "gpx/be-rostock.gpx"
   :average-speed average-speed
   :daily-plans [{:date "2024-04-01"
                  :label "First day"
                  :activities [{:start "10:00" :type :ride :length 3}
                               {:start "15:00" :type :ride :length 5}]}
                 {:date "2024-04-02"
                  :label "Second day"
                  :activities [{:start "10:00" :type :ride :length 4}
                               {:start "16:00" :type :ride :length 2.5}]}]})

(clerk/with-viewer diagram/plan-viewer equally-split-plan-with-pauses)

;; The plan diagram looks almost exactly the same, but notice two things:

;; - There are two markers (one for each day) indicating when, and for
;;   how long, the pause is taking place

;; - As a consequence of the two hours pauses, we're going to have to
;;   ride more time in the dark.

;; Now that the plan is fleshed out, we can obtain more information
;; from the elevation viewer

(clerk/with-viewer diagram/elevation-viewer equally-split-plan-with-pauses)

;; Notice also that we can use the notebook to visually compare two
;; (or more) plans for the same route. You can even put them side by
;; side, thanks to Clerk facilities, with minimal extra work

(merge
 {:nextjournal/width :full}
 (clerk/row
  (clerk/col
   (clerk/html [:h3 "Plan with no lunch!"])
   (clerk/with-viewer diagram/elevation-viewer equally-split-plan)
   (clerk/with-viewer diagram/plan-viewer equally-split-plan))
  (clerk/col
   (clerk/html [:h3 "Plan with a pause"])
   (clerk/with-viewer diagram/elevation-viewer equally-split-plan-with-pauses)
   (clerk/with-viewer diagram/plan-viewer equally-split-plan-with-pauses))))

;; The last plan we made looks promising, let's see on the map where
;; the first day of riding will bring us. We're going to use the same
;; viewer we used before, but since the plan is now more detailed, it
;; will also display more information.

;; You can zoom in and use the markers to see where you'll land at the
;; end of the first day. Intermediate markers, when clicked, display a
;; popup that shows the label of the day that _ends_ there, and the
;; amount of kilometers planned for that day. With this information,
;; you can use other tools to find accomodation or other services
;; you're going to need on the road.

;; Here the new map, this time displayed taking advantage of all the
;; screen width:

(merge
 {:nextjournal/width :full}
 (clerk/with-viewer leaflet/leaflet-gpx-viewer equally-split-plan-with-pauses))

;; ## How plans are defined

;; Examining an example, we'll provide a more formal definition of
;; each option that can be specified:

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/code {:gpx "gpx/be-rostock.gpx"
             :average-speed average-speed
             :daily-plans [{:date "2024-04-01"
                            :label "First day"
                            :activities [{:start "10:00" :type :ride :length 3}
                                         {:start "15:00" :type :ride :length 5}]}
                           {:date "2024-04-02"
                            :label "Second day"
                            :activities [{:start "10:00" :type :ride :length 4}
                                         {:start "16:00" :type :ride :length 2.5}]}]})

;; A plan is defined as a dictionary containing the following keys:

;; - `:gpx`: This key refers to the location of a GPX (GPS Exchange
;;   Format) file

;; - `:average-speed`: This key represents the average speed
;;   maintained during the activities. The value associated with this
;;   key is expected to be a numerical value denoting speed, expressed
;;   as km/h

;; - `:daily-plans`: This key holds a vector of daily plans. Each
;;   daily plan is represented as a map containing information about
;;   activities planned for a single day. Inside each element in the
;;   vector:

;;   - `:date`: This key denotes the date of the daily plan in the format "YYYY-MM-DD". It is used to compute the sun rise and set times.
;;   - `:label`: An arbitrary string, a label or description of the daily plan
;;   - `:activities`: This key holds a vector of activities planned for the day. Each activity is represented as a map containing:
;;     - `:start`: This key denotes the starting time of an activity, in the format "HH:mm"
;;     - `:type`: This key specifies the type of activity. At the moment only the type "ride" is in use. This key is reserved for future uses
;;     - `:length`: This key represents the duration or length of the activity, expressed in hours. It is expected to be a numerical value.
