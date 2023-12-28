# Clerk notebooks to plan bikepacking events

```clojure
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

(clerk/html
 (diagram/plan->diagram plan-start-19))
```

![Example result](rando-planner-example.png)
