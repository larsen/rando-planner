(ns rando-planner.plan
  (:require [clj-time.core :as t]
            [clj-time.format :as f]))

(defn string-to-time [time-str]
  (let [formatter (f/formatter "HH:mm")]
    (f/parse formatter time-str)))

(defn time-to-string [dt]
  (let [formatter (f/formatter "HH:mm")]
    (f/unparse formatter dt)))

(defn time-after-n-hours [start n]
  (time-to-string (t/plus (string-to-time start) (t/hours n))))

(defn kilometers-in-a-day [day-plan average-speed]
  (reduce + (map #(* (:length %) average-speed)
                 (filter #(= (:type %) :ride)
                         (:activities day-plan)))))

(defn kilometers-covered [day-plan average-speed]
  (* average-speed
     (reduce + (map :length
                    (filter #(= :ride (:type %))
                            (:activities day-plan))))))

(defn pauses [day-plan]
  (let [activities (:activities day-plan)]
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
                          :lenght (t/in-hours pause-length)})
                 (first next-activities)
                 (rest next-activities)))
        p))))


