(ns rando-planner.gpx-test
  (:require [clojure.test :refer [deftest is testing]]
            [rando-planner.gpx :as gpx]))

(deftest gpx-tests
  (testing "points"
    (is (= (gpx/points "gpx/test.gpx")
           '({:lat 45.765875, :lon 11.731275, :ele 117.0}
             {:lat 45.765557, :lon 11.731155, :ele 118.0}
             {:lat 45.765322, :lon 11.731128, :ele 118.0}
             {:lat 45.764854, :lon 11.731074, :ele 118.0}
             {:lat 45.764723, :lon 11.731075, :ele 116.0})))))
