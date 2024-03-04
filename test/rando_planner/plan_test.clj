(ns rando-planner.plan-test
  (:require [rando-planner.plan :as plan]
            [clojure.test :refer [deftest is testing]]))

(def example-plan
  {:gpx "gpx/be-rostock.gpx"
   :average-speed 20
   :daily-plans [{:date "2024-04-01"
                  :label "First day"
                  :activities [{:start "10:00" :type :ride :length 3}
                               {:start "17:00" :type :ride :length 2}
                               {:start "20:00" :type :ride :length 3}]}
                 {:date "2024-04-02"
                  :label "Second day"
                  :activities [{:start "10:00" :type :ride :length 4}
                               {:start "16:00" :type :ride :length 2.5}]}]})

(deftest plan-tests
  (testing "Pauses"
    (is (= '([{:start "13:00", :after 3, :length 4 :cumulative-pause 0}
              {:start "19:00", :after 9, :length 1 :cumulative-pause 4}]
             [{:start "14:00", :after 4, :length 2 :cumulative-pause 0}])
           (map plan/pauses (:daily-plans example-plan))))))
