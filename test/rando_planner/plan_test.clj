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

(def a-daily-plan
  (nth (:daily-plans example-plan) 0))

(deftest plan-tests
  (testing "daily-distance"
    (is (= '[{:day 1,
              :label "First day",
              :average-speed 20,
              :kilometers 160,
              :covered 0,
              :elevation 375.69999999999993}
             {:day 2,
              :label "Second day",
              :average-speed 20,
              :kilometers 140,
              :covered 160,
              :elevation 286.9000000000001}]
           (plan/daily-distance example-plan))))
  (testing "daily-stats"
    (is (= '({:day 1,
              :cumulative-distance 160.07331099360235,
              :elevation 375.69999999999993,
              :ele 79.4,
              :average-speed 20,
              :pauses
              [{:cumulative-pause 0, :start "13:00", :after 3, :length 4}
               {:cumulative-pause 4, :start "19:00", :after 9, :length 1}],
              :label "First day",
              :lon 12.42219,
              :lat 53.50526,
              :distance 0.08814470079957809,
              :covered 0,
              :kilometers 160}
             {:day 2,
              :elevation 286.9000000000001,
              :average-speed 20,
              :label "Second day",
              :covered 160,
              :kilometers 140,
              :pauses
              [{:cumulative-pause 0, :start "14:00", :after 4, :length 2}]})
           (plan/daily-stats example-plan))))
  (testing "Pauses"
    (is (= '([{:start "13:00", :after 3, :length 4 :cumulative-pause 0}
              {:start "19:00", :after 9, :length 1 :cumulative-pause 4}]
             [{:start "14:00", :after 4, :length 2 :cumulative-pause 0}])
           (map plan/pauses (:daily-plans example-plan))))))

(deftest daily-measures
  (testing "kilometers-in-a-day"
    (is (= 160
           (plan/kilometers-in-a-day a-daily-plan 20)))
    (is (= '(20 40 60 80 100 120 140 160)
           (plan/hourly-cumulative-distances-in-a-day a-daily-plan 20)))))
