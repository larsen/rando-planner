(ns rando-planner.suncalc-test
  (:require [rando-planner.suncalc :as sut]
            [clojure.test :refer [deftest is testing]]))

(defn epsilon-diff
  ([a b]
   (epsilon-diff a b 0.001))
  ([a b epsilon]
   (< (Math/abs (- a b))
      epsilon)))

(deftest suncalc-tests
  (testing "Julian date & day"
    (is (epsilon-diff (sut/ts->julian-date 1709128197.3056235)
                      2460369.076))
    (is (= (sut/julian-date->julian-day 2460369.061)
           8825.0)))
  (testing "Mean solar time"
    (is (= (sut/mean-solar-time 8825.0 35.08794)
           8824.903433500)))
  (testing "Solar mean anomaly"
    (is (epsilon-diff (sut/mean-anomaly 8824.903433500)
                      55.356)))
  (testing "Equation of the center"
    (is (epsilon-diff (sut/equation-of-the-center (Math/toRadians 55.356))
                      1.594)))
  (testing "Ecliptic longitude"
    (is (epsilon-diff (sut/ecliptic-longitude
                       (sut/mean-anomaly 8824.903433500)
                       (sut/equation-of-the-center
                        (Math/toRadians 55.356)))
                      5.932)))
  (testing "Solar transit"
    (is (epsilon-diff (sut/julian-date->ts
                       (sut/solar-transit 8824.903433500
                                          0.966
                                          5.932))
                      1709200418.3787212
                      0.2 ; Double check! Epsilon too large
                      ))))
