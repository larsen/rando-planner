(ns rando-planner.suncalc-test
  (:require [rando-planner.suncalc :as sut]
            [tick.core :as tick]
            [clojure.test :refer [deftest is testing]]))

(defn small-diff
  ([a b]
   (small-diff a b 0.001))
  ([a b epsilon]
   (< (Math/abs (- a b))
      epsilon)))

(defn small-time-diff
  ([t1 t2]
   (small-time-diff t1 t2 5))
  ([t1 t2 duration]
   (< (tick/between t2 t1 :seconds)
      duration)))

(def lat 33.00801)
(def lon 35.08794)
(def elevation 0)
(def timestamp 1709128197.3056235)

(deftest suncalc-tests
  (testing "Julian date & day"
    (is (small-diff 2460369.076
                    (sut/ts->julian-date timestamp)))
    (is (= 8825.0
           (sut/julian-date->julian-day 2460369.061))))
  (testing "Mean solar time"
    (is (= 8824.903433500
           (sut/mean-solar-time 8825.0 lon))))
  (testing "Solar mean anomaly"
    (is (small-diff 55.356
                    (sut/mean-anomaly 8824.903433500))))
  (testing "Equation of the center"
    (is (small-diff 1.594
                    (sut/equation-of-the-center (Math/toRadians 55.356)))))
  (testing "Ecliptic longitude"
    (is (small-diff 5.932
                    (sut/ecliptic-longitude
                     (sut/mean-anomaly 8824.903433500)
                     (sut/equation-of-the-center
                      (Math/toRadians 55.356))))))
  (testing "Solar transit"
    (is (small-diff 1709200418.3787212
                    (sut/julian-date->ts
                     (sut/solar-transit 8824.903433500 0.966 5.932))
                    ;; Double check! Epsilon too large
                    0.2)))
  (testing "Hour angle"
    (is (small-diff 85.860
                    (sut/hour-angle
                     lat
                     elevation
                     (sut/ecliptic-longitude
                      (sut/mean-anomaly 8824.903433500)
                      (sut/equation-of-the-center
                       (Math/toRadians 55.356)))))))
  (testing "Sunrise & sunset"
    (let [[sunrise sunset] (sut/sunset-sunrise-times timestamp
                                                     lat lon elevation
                                                     "Europe/Berlin")]
      (is (small-time-diff (tick/date-time "2024-02-29T05:10:12")
                           sunrise))
      (is (small-time-diff (tick/date-time "2024-02-29T16:37:04")
                           sunset)))))
