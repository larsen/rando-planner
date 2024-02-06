(ns rando-planner.gpx
  (:require [rando-planner.xml :as xml]))

(defn points [gpx-resource]
  (letfn [(pick-tag [tag data]
            (->> data
                 (sequence (xml/tag= tag))
                 first
                 :content))]
    (let [trkpts (->> gpx-resource
                      xml/xml-data
                      :content
                      (pick-tag :trk)
                      (pick-tag :trkseg))]
      (map (fn [trkpt]
             (let [lat (-> trkpt :attrs :lat)
                   lon (-> trkpt :attrs :lon)
                   ele (-> trkpt :content first :content first)]
               {:lat (Double. lat)
                :lon (Double. lon)
                :ele (Double. ele)}))
           trkpts))))

(defn bounds
  [points]
  (loop [i 0 min-lat 180 max-lat 0 min-lon 180 max-lon 0]
    (if (< i (count points))
      (let [p (nth points i)
            lat (:lat p)
            lon (:lon p)]
        (recur (inc i)
               (min min-lat lat)
               (max max-lat lat)
               (min min-lon lon)
               (max max-lon lon)))
      [[min-lat min-lon]
       [max-lat max-lon]])))

(defn center
  "Given a set of points (represented as dictionaries with a :lat and a :lon),
  it returns the centre of those points. Useful to display the GPX
  track on a map"
  [points]
  (let [[[min-lat min-lon] [max-lat max-lon]] (bounds points)]
    [(+ min-lat (/ (- max-lat min-lat) 2 ))
     (+ min-lon (/ (- max-lon min-lon) 2 ))]))

(defn haversine [point1 point2]
  (let [earth-radius 6371
        lat1 (Math/toRadians (:lat point1))
        lat2 (Math/toRadians (:lat point2))
        dlat (Math/toRadians (- (:lat point2)
                                (:lat point1)))
        dlon (Math/toRadians (- (:lon point2)
                                (:lon point1)))
        a (+ (Math/pow (Math/sin (/ dlat 2)) 2)
             (* (Math/pow (Math/sin (/ dlon 2)) 2)
                (Math/cos lat1)
                (Math/cos lat2)))
        c (* 2 (Math/asin (Math/sqrt a)))]
    (* c earth-radius)))

(defn points-with-distance [points]
  (map-indexed (fn [idx point]
                 (if (zero? idx)
                   (assoc point :distance 0)
                   (assoc point :distance (haversine
                                           (nth points (dec idx)) point))))
               points))

(defn points-with-cumulative-distance [points]
  (let [points-with-distance (points-with-distance points)]
    (map-indexed (fn [idx point]
                   (assoc point :cumulative-distance
                          (reduce + (map :distance
                                         (take idx points-with-distance)))))
                 points-with-distance)))

(defn group-by-kilometer [points-with-cumulative-distance]
  (let [groups (group-by #(->> % :cumulative-distance (Math/floor))
                         points-with-cumulative-distance)]
    (->> groups
         (map (fn [[kilometer group]]
                {:kilometer kilometer
                 :elevation (->> group
                                 (map :ele)
                                 (apply max))}))
         (sort-by :kilometer))))

(defn elevation [gpx-resource]
  (-> gpx-resource
      points
      points-with-cumulative-distance
      group-by-kilometer))

(defn total-distance [gpx-resource]
  (->> (points gpx-resource)
       (partition 2 1)
       (map (fn [[point1 point2]]
              (haversine point1 point2)))
       (reduce +)))
