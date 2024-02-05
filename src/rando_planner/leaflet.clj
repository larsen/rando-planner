(ns rando-planner.leaflet
  (:require [nextjournal.clerk :as clerk]
            ; [nextjournal.clerk.viewer :as v]
            [rando-planner.xml :as xml]
            [rando-planner.gpx :as gpx]
            [rando-planner.plan :as plan]))

(defn add-center-and-bounds [value]
  (if (:gpx-resource value)
    (let [points (gpx/points (:gpx-resource value))]
      (assoc value
             :gpx-content (xml/xml-data-raw (:gpx-resource value))
             :center (gpx/center points)
             :bounds (gpx/bounds points)))
    value))

(defn add-plan-markers [value]
  (if (and (:plan value)
           (:gpx-resource value))
    (assoc value :markers (plan/points-at-daily-kilometers
                           (:gpx-resource value)
                           (:plan value)))
    value))

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
                            attribution "&copy; <a href='http://www.openstreetmap.org/copyright'>OpenStreetMap</a>"
                            marker-options {:startIconUrl "https://stefanorodighiero.net/misc/pin-icon-start.png"
                                            :endIconUrl "https://stefanorodighiero.net/misc/pin-icon-end.png"
                                            :shadowUrl "https://stefanorodighiero.net/misc/pin-shadow.png"}]
                        [:div {:id map-div-id
                               :height "400px"
                               :style {:height "400px"}
                               :ref (fn [el]
                                      (when el
                                        (let [m (.map js/L map-div-id)
                                              tile-layer (.tileLayer js/L "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
                                                                     (clj->js {:attribution attribution}))
                                              gpx-layer (new js/L.GPX (or (:gpx value)
                                                                          (:gpx-content value))
                                                             (clj->js {:async true
                                                                       :marker_options marker-options}))]
                                          (if (:bounds value)
                                            (.fitBounds m (clj->js (:bounds value)))
                                            (.setView m (clj->js (:center value)) (:zoom value)))
                                          (.addTo tile-layer m)
                                          (.addTo gpx-layer m)
                                          (when (:markers value)
                                            (doseq [pp (:markers value)]
                                              (.addTo (.marker js/L (clj->js [(:lat pp) (:lon pp)])) m))))))}]))]))})
