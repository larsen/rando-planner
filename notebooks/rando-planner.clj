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


;; When planning a long-distance bike ride, you face two main
;; challenges: determining the route from point A to B, and organizing
;; your journey—deciding how much distance to cover each day, when to
;; take breaks, and where to refuel. Traditional route planners, such
;; as Komoot, Strava, and Cycle.travel, primarily address the first
;; challenge: designing the route.

;; Komoot includes a tool for dividing the route into multiple days,
;; but it simply distributes the distance evenly across
;; them. Cycle.travel, on the other hand, allows users to break the
;; route at specific points and even helps locate accommodations for
;; overnight stays.

;; Rando-planner takes a different approach. It assumes you already
;; have a GPX file of your route and focuses exclusively on the second
;; challenge: planning your journey in detail.

;; With rando-planner, users can divide the route into individual
;; activities or daily plans. The tool provides visualization tools to
;; assess daily effort with greater precision and compare various
;; strategies for covering the distance effectively.


;; This page was composed with Clerk and rando-planner

;; You can see the code for the project [on
;; github.com](https://github.com/larsen/rando-planner).


;; ## Features

;; - Schematic Visualization of the Plan
;;   - Displays a comprehensive overview of your route, including:
;;   - Running count of kilometers.
;;   - Elevation profile.
;;   - Scheduled pauses.
;;   - Total distance accumulated daily.
;;   - Sunrise and sunset indicators (currently a work in progress).
;; - Daily Elevation Visualization
;;   - Provides a focused view of the elevation profile for each day, helping you gauge the effort required for specific segments of the route.
;; - Map Visualization
;;   - Interactive mapping powered by Leaflet.
;;   - Highlights the route with markers indicating the location reached at the end of each day.

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
;; you can use other tools to find accommodation or other services
;; you're going to need on the road.

;; Here the new map, this time displayed taking advantage of all the
;; screen width:

(merge
 {:nextjournal/width :full}
 (clerk/with-viewer leaflet/leaflet-gpx-viewer equally-split-plan-with-pauses))

;; Additionally, you can focus the map on the portion of map
;; corresponding to a single day

(clerk/with-viewer leaflet/leaflet-gpx-viewer
  (assoc equally-split-plan-with-pauses :focus-on "Second day"))

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

;; - `:average-speed` [Optional]: When appears only at this level,
;;   this key represents the average speed maintained during all the
;;   activities. The value associated with this key is expected to be
;;   a numerical value denoting speed, expressed as km/h.  If
;;   `:average-speed` is used in a daily-plan (see below) then that
;;   value overrides this option.

;; - `:daily-plans` [Optional]: This key holds a vector of daily
;;   plans. If no daily-plans is provided, rando-planner will use a
;;   default value (one single activity, long enough to cover the
;;   entire distance). Each daily plan is represented as a map
;;   containing information about activities planned for a single
;;   day. Inside each element in the vector:

;;   - `:date`: This key denotes the date of the daily plan in the format "YYYY-MM-DD". It is used to compute the sun rise and set times.
;;   - `:label`: An arbitrary string, a label or description of the daily plan
;;   - `:average-speed' [Optional]: The value associated with this key is expected to be
;;       a numerical value denoting speed, expressed as km/h.
;;   - `:activities`: This key holds a vector of activities planned for the day. Each activity is represented as a map containing:
;;     - `:start`: This key denotes the starting time of an activity, in the format "HH:mm"
;;     - `:type`: This key specifies the type of activity. At the moment only the type "ride" is in use. This key is reserved for future uses
;;     - `:length`: This key represents the duration or length of the activity, expressed in hours. It is expected to be a numerical value.
