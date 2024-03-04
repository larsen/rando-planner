(ns rando-planner.leaflet
  (:require [nextjournal.clerk :as clerk]
            ; [nextjournal.clerk.viewer :as v]
            [rando-planner.xml :as xml]
            [rando-planner.gpx :as gpx]
            [rando-planner.plan :as plan]))

(defn add-center-and-bounds [plan]
  (if (:gpx plan)
    (let [points (gpx/points (:gpx plan))]
      (assoc plan
             :points points
             :center (gpx/center points)
             :bounds (gpx/bounds points)))
    plan))

(defn add-plan-markers [plan]
  (if (and (:gpx plan)
           (:daily-plans plan))
    (assoc plan :markers
           (map (fn [pp]
                  (assoc pp :popup-message
                         (str "<small>"
                              "<strong>" (:label pp) "</strong>"
                              "<br />"
                              (Math/floor (:kilometers pp))
                              " km / ("
                              (Math/floor (:cumulative-distance pp))
                              " km) ▲ "
                              (Math/floor (:elevation pp))
                              "</small>")))
                (butlast (plan/daily-stats plan))))
    plan))

(defn enrich-with-gpx-details [value]
  (-> value
      add-center-and-bounds
      add-plan-markers))

(def leaflet-gpx-viewer
  {:transform-fn (comp clerk/mark-presented
                       (clerk/update-val enrich-with-gpx-details))
   :render-fn '(fn [value]
                 (when value
                   [nextjournal.clerk.render/with-d3-require
                    {:package ["leaflet@1.7.1/dist/leaflet.min.js"]}
                    (fn [leaflet]
                      (let [map-div-id (str (gensym))
                            m (atom nil)
                            attribution "&copy; <a href='http://www.openstreetmap.org/copyright'>OpenStreetMap</a>"
                            marker-options {:startIconUrl "https://stefanorodighiero.net/misc/pin-icon-start.png"
                                            :endIconUrl "https://stefanorodighiero.net/misc/pin-icon-end.png"
                                            :shadowUrl "https://stefanorodighiero.net/misc/pin-shadow.png"}]
                        [:div {:id map-div-id
                               :style {:height "400px"}
                               :ref (fn [el]
                                      (if el
                                        (let [start (first (:points value))
                                              end (last (:points value))
                                              start-icon (.icon js/L (clj->js {:iconUrl (:startIconUrl marker-options)
                                                                               :iconSize [33, 45]
                                                                               :iconAnchor [16, 45]}))
                                              end-icon (.icon js/L (clj->js {:iconUrl (:endIconUrl marker-options)
                                                                             :iconSize [33, 45]
                                                                             :iconAnchor [16, 45]}))
                                              tile-layer (.tileLayer js/L "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
                                                                     (clj->js {:attribution attribution}))
                                              polyline (.polyline js/L
                                                                  (clj->js (vec (map (fn [{lat :lat, lon :lon} p]
                                                                                       [lat lon])
                                                                                     (:points value))))
                                                                  (clj->js (:color "red")))]
                                          (reset! m (.map js/L map-div-id))
                                          (if (:bounds value)
                                            (.fitBounds @m (clj->js (:bounds value)))
                                            (.setView @m (clj->js (:center value)) (:zoom value)))
                                          (.addTo tile-layer @m)
                                          (.addTo polyline @m)
                                          (.addTo (.marker js/L (clj->js [(:lat start) (:lon start)])
                                                           (clj->js {:icon start-icon})) @m)
                                          (.addTo (.marker js/L (clj->js [(:lat end) (:lon end)])
                                                           (clj->js {:icon end-icon})) @m)
                                          (when (:markers value)
                                            (doseq [pp (:markers value)]
                                              (.bindPopup
                                               (.addTo (.marker js/L (clj->js [(:lat pp) (:lon pp)])) @m)
                                               (:popup-message pp)))))
                                        (.remove @m)))}]))]))})
