(ns rando-planner.leaflet
  (:require [nextjournal.clerk :as clerk]
            [rando-planner.gpx :as gpx]
            [rando-planner.plan :as plan]))

(def leaflet-gpx-viewer
  {:transform-fn clerk/mark-presented
   :render-fn '(fn [value]
                 (when value
                   [nextjournal.clerk.render/with-d3-require
                    {:package ["leaflet@1.7.1/dist/leaflet.min.js"
                               "leaflet-gpx@1.5.1/gpx.min.js"]}
                    (fn [leaflet]
                      (let [map-div-id (str (gensym))]
                        [:div {:id map-div-id
                               :height "400px"
                               :style {:height "400px"}
                               :ref (fn [el]
                                      (when el
                                        (let [attribution "&copy; <a href='http://www.openstreetmap.org/copyright'>OpenStreetMap</a>"
                                              marker-options {:startIconUrl "https://stefanorodighiero.net/misc/pin-icon-start.png"
                                                              :endIconUrl "https://stefanorodighiero.net/misc/pin-icon-end.png"
                                                              :shadowUrl "https://stefanorodighiero.net/misc/pin-shadow.png"}
                                              m (.map js/L map-div-id)
                                              tile-layer (.tileLayer js/L "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
                                                                     (clj->js {:attribution attribution}))
                                              gpx-layer (new js/L.GPX (or (:gpx value)
                                                                          (:gpx-content value))
                                                             (clj->js {:async true
                                                                       :marker_options marker-options}))]
                                          (.setView m
                                                    (clj->js (:center value))
                                                    (:zoom value))
                                          (.addTo tile-layer m)
                                          (.addTo gpx-layer m)
                                          (when (:markers value)
                                            (.log js/console (clj->js (:markers value)))
                                            (doseq [pp (:markers value)]
                                              (.addTo (.marker js/L (clj->js [(:lat pp) (:lon pp)])) m)))
                                          )))}]))]))})
