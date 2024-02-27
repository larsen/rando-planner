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
             :gpx-content (xml/xml-data-raw (:gpx plan))
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
                    {:package ["leaflet@1.7.1/dist/leaflet.min.js"
                               "leaflet-gpx@1.5.1/gpx.min.js"]}
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
                                        (let [tile-layer (.tileLayer js/L "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
                                                                     (clj->js {:attribution attribution}))
                                              gpx-layer (new js/L.GPX (:gpx-content value)
                                                             (clj->js {:async true
                                                                       :marker_options marker-options}))]
                                          (reset! m (.map js/L map-div-id))
                                          (if (:bounds value)
                                            (.fitBounds @m (clj->js (:bounds value)))
                                            (.setView @m (clj->js (:center value)) (:zoom value)))
                                          (.addTo tile-layer @m)
                                          (.addTo gpx-layer @m)
                                          (when (:markers value)
                                            (doseq [pp (:markers value)]
                                              (.bindPopup
                                               (.addTo (.marker js/L (clj->js [(:lat pp) (:lon pp)])) @m)
                                               (:popup-message pp)))))
                                        (.remove @m)))}]))]))})
