(ns rando-planner.diagram-test
  (:require [rando-planner.diagram :as diagram]
            [clojure.test :refer [deftest is testing]]))

(def example-plan
  {:gpx "gpx/be-rostock.gpx"
   :average-speed 20
   :daily-plans [{:date "2024-04-01"
                  :label "First day"
                  :color "red"
                  :activities [{:start "10:00" :type :ride :length 3}
                               {:start "17:00" :type :ride :length 2}
                               {:start "20:00" :type :ride :length 3}]}
                 {:date "2024-04-02"
                  :label "Second day"
                  :color "orange"
                  :activities [{:start "10:00" :type :ride :length 4}
                               {:start "16:00" :type :ride :length 2.5}]}]})

(def example-plan-with-daily-avg-speed
  {:gpx "gpx/be-rostock.gpx"
   :daily-plans [{:date "2024-04-01"
                  :label "First day"
                  :color "red"
                  :average-speed 20
                  :activities [{:start "10:00" :type :ride :length 3}
                               {:start "17:00" :type :ride :length 2}
                               {:start "20:00" :type :ride :length 3}]}
                 {:date "2024-04-02"
                  :label "Second day"
                  :color "orange"
                  :average-speed 20
                  :activities [{:start "10:00" :type :ride :length 4}
                               {:start "16:00" :type :ride :length 2.5}]}]})

(deftest plan-diagram
  (testing "Plan diagram"
    (is (some? (diagram/plan-diagram example-plan)))
    (is (some? (diagram/plan-diagram example-plan-with-daily-avg-speed)))))
