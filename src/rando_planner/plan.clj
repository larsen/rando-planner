(ns rando-planner.plan)

(defn kilometers-in-a-day [day-plan average-speed]
  (reduce + (map #(* (:length %) average-speed)
                 (filter #(= (:type %) :ride)
                         (:activities day-plan)))))

(defn kilometers-covered [day-plan average-speed]
  (* average-speed
     (reduce + (map :length
                    (filter #(= :ride (:type %))
                            (:activities day-plan))))))
