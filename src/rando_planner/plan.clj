(ns rando-planner.plan
  (:require [tick.core :as tick]
            [cljc.java-time.instant :as i]
            [rando-planner.gpx :as gpx]))

(defn date-to-timestamp [date-time-str]
  (-> date-time-str
      tick/date-time
      tick/instant
      i/to-epoch-milli))

(defn time-after-n-hours [start n]
  (tick/>> (tick/time start)
           (tick/new-duration n :hours)))

(def default-average-speed 18)

(defn average-speed
  ([day-plan] (or (:average-speed day-plan)
                  default-average-speed))
  ([day-plan plan] (or (:average-speed day-plan)
                       (:average-speed plan)
                       default-average-speed)))

(defn hourly-distances-in-a-day [day-plan average-speed]
  (->> (:activities day-plan)
       (filter #(= (:type %) :ride))
       (map #(take (:length %) (repeat average-speed)))
       flatten))

(defn hourly-cumulative-distances-in-a-day [day-plan average-speed]
  (reductions + (hourly-distances-in-a-day day-plan average-speed)))

(defn hourly-cumulative-distances-in-a-plan [plan]
  (->> plan
       :daily-plans
       (map #(hourly-distances-in-a-day % (average-speed % plan)))
       flatten
       (reductions +)))

(defn kilometers-in-a-day [day-plan average-speed]
  (reduce + (hourly-distances-in-a-day day-plan average-speed)))

(defn pauses
  "Given a day plan it returns a vector of pauses objects,
  each one characterized by its LENGTH (in hours),
  when it starts (as a string representing a time of the day),
  and how many hours after the start of the day the pause occurs."
  [day-plan]
  (let [activities (:activities day-plan)
        plan-starts-at (tick/time (:start (first activities)))]
    (loop [p []
           cumulative-pause 0
           curr-activity (first activities)
           next-activities (rest activities)]
      (if (first next-activities)
        (let [pause-start (time-after-n-hours (tick/time (:start curr-activity))
                                              (:length curr-activity))
              pause-start-as-str (str pause-start)
              pause-length (tick/between pause-start
                                         (tick/time
                                          (:start (first next-activities))))]
          (recur (conj p {:start pause-start-as-str
                          :after (tick/hours (tick/between plan-starts-at
                                                           pause-start))
                          ;; TODO
                          ;; it should manage pauses that last
                          ;; a fractional number of hours
                          :length (tick/hours pause-length)
                          :cumulative-pause cumulative-pause})
                 (+ cumulative-pause (tick/hours pause-length))
                 (first next-activities)
                 (rest next-activities)))
        p))))

(defn daily-plan [plan n]
  (nth (:daily-plans plan) n))

(defn default-daily-plans [plan]
  [{:date (str (tick/today))
    :label "_day0"
    :activities [{:start "05:00"
                  :type :ride
                  :length (/ (gpx/total-distance
                              (:gpx plan))
                             default-average-speed)}]}])

(defn daily-distance
  "Given a plan, it returns a vector of structures containing how many
  kilometers are done in each day, and from what kilometer each day starts"
  [plan]
  (let [daily-plans (or (:daily-plans plan)
                        ;; If the user provided no plan, then a ficticious one
                        ;; is used just for the sake of the calculations
                        (default-daily-plans plan))
        elevation (gpx/elevation (:gpx plan))]
    (loop [result []
           acc-km 0
           i 0]
      (if (< i (count daily-plans))
        (let [dp (daily-plan plan i)
              average-speed (average-speed dp plan)
              km (kilometers-in-a-day dp average-speed)]
          (recur (conj result
                       {:day (+ 1 i)
                        :label (:label dp)
                        :average-speed average-speed
                        :kilometers km
                        :covered acc-km
                        :elevation (gpx/elevation-gain elevation
                                                       acc-km
                                                       (+ km acc-km))})
                 (+ acc-km km)
                 (inc i)))
        result))))

(defn group-points-by-day [plan]
  (let [points-with-cumulative-distance (gpx/with-cumulative-distance
                                          (gpx/points (:gpx plan)))]
    (into {} (map (fn [d]
                    [(:label d)
                     (filter #(and (>= (:cumulative-distance %)
                                       (:covered d))
                                   (< (:cumulative-distance %)
                                      (+ (:covered d)
                                         (:kilometers d))))
                             points-with-cumulative-distance)])
                  (daily-distance plan)))))

(defn daily-stats
  "Given a plan, it returns a list of dictionaries with info and
  statistics for each day in the plan, including:

  :day
  :cumulative-distance
  :average-speed
  :elevation
  :ele
  :label
  :lon
  :lat
  :distance
  :covered
  :kilometers"
  [plan]
  (let [points-with-cumulative-distance (-> (:gpx plan)
                                            gpx/points
                                            gpx/with-cumulative-distance)
        daily-distance (daily-distance plan)
        points-at-end-of-days (for [dk daily-distance]
                                (->> points-with-cumulative-distance
                                     (filter #(> (:cumulative-distance %)
                                                (+ (:covered dk)
                                                   (:kilometers dk))))
                                     first))
        daily-pauses (vec (map #(assoc {} :pauses %)
                           (map pauses (:daily-plans plan))))]
    (map merge
         points-at-end-of-days
         daily-distance
         daily-pauses)))
