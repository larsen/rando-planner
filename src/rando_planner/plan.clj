(ns rando-planner.plan
  (:require [clj-time.core :as t]
            [clj-time.format :as f]
            [rando-planner.gpx :as gpx]))

(defn string-to-date [date-str]
  (let [formatter (f/formatter "YYYY-MM-DD")]
    (f/parse formatter date-str)))

(defn string-to-time [time-str]
  (let [formatter (f/formatter "HH:mm")]
    (f/parse formatter time-str)))

(defn time-to-string [dt]
  (let [formatter (f/formatter "HH:mm")]
    (f/unparse formatter dt)))

(defn time-after-n-hours [start n]
  (t/plus (string-to-time start) (t/hours n)))

(defn time-after-n-hours-as-str [start n]
  (time-to-string (time-after-n-hours start n)))

(defn kilometers-in-a-day [day-plan average-speed]
  (reduce + (map #(* (:length %) average-speed)
                 (filter #(= (:type %) :ride)
                         (:activities day-plan)))))

(defn pauses
  "Given a day plan it returns a vector of pauses objects,
  each one characterized by its LENGTH (in hours),
  when it starts (as a string representing a time of the day),
  and how many hours after the start of the day the pause occurs."
  [day-plan]
  (let [activities (:activities day-plan)
        plan-starts-at (string-to-time (:start (first activities)))]
    (loop [p []
           curr-activity (first activities)
           next-activities (rest activities)]
      (if (first next-activities)
        (let [pause-start (t/plus (string-to-time (:start curr-activity))
                                  (t/hours (:length curr-activity)))
              pause-start-as-str (time-to-string pause-start)
              pause-length (t/interval pause-start
                                       (string-to-time (:start (first next-activities))))]
          (recur (conj p {:start pause-start-as-str
                          :after (t/in-hours (t/interval plan-starts-at
                                                         pause-start))
                          ;; TODO
                          ;; it should manage pauses that last
                          ;; a fractional number of hours
                          :length (t/in-hours pause-length)})
                 (first next-activities)
                 (rest next-activities)))
        p))))

(defn daily-kilometers
  "Given a plan, it returns a vector of structures containing how many
  kilometers are done in each day, and from what kilometer each day starts"
  [plan]
  (let [daily-plans (:daily-plans plan)]
    (loop [result []
           acc-km 0
           i 0]
      (if (< i (count daily-plans))
        (let [km (kilometers-in-a-day (nth daily-plans i)
                                      (:average-speed plan))]
          (recur (conj result
                       {:day (+ 1 i)
                        :label (:label (nth daily-plans i))
                        :kilometers km
                        :covered acc-km})
                 (+ acc-km km)
                 (inc i)))
        result))))

(defn points-at-daily-kilometers [gpx-resource plan]
  (let [points-wcd (gpx/points-with-cumulative-distance
                    (gpx/points gpx-resource))
        daily-km-plans (butlast (daily-kilometers plan))
        points-at-end-of-days (for [dk daily-km-plans]
                                (first (filter (fn [p]
                                                 (> (:cumulative-distance p)
                                                    (+ (:covered dk)
                                                       (:kilometers dk))))
                                               points-wcd)))]
    (map merge
         points-at-end-of-days
         daily-km-plans
         )))
