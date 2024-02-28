(ns rando-planner.suncalc-test
  (:require [rando-planner.suncalc :as sut]
            [clojure.test :refer [deftest is testing]]))


(deftest suncalc-tests
  (testing "Julian date & day"
    (is (< (Math/abs (- (sut/ts->julian-date 1709128197.3056235)
                        2460369.076))
           0.001))
    (is (= (sut/julian-date->julian-day 2460369.061)
           8825.0)))
  (testing "Mean solar time"
    (is (= (sut/mean-solar-time 8825.0 35.08794)
           8824.903433500)))
  (testing "Solar mean anomaly"
    (is (< (Math/abs (- (sut/mean-anomaly 8824.903433500)
                        55.356))
           0.001))))
