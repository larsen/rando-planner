(ns rando-planner.xml
  (:require [clojure.java.io :as io]
            [clojure.data.xml :as cdxml]))

(defn xml-data-raw [resource]
  (slurp (io/input-stream
          (io/resource resource))))

(defn xml-data [resource]
  (cdxml/parse (io/input-stream
                (io/resource resource))))

;; (->> (c-d-xml/parse-str data)
;;     xml-seq
;;     (filter #(not-empty (:attrs %)))
;;     first
;;     as-short-xml)

;; (sequence (tag= :trkpt) (xml-seq
;;                          (xml/parse-str
;;                           (xml-data-raw "gpx/test.gpx"))))

(defn filter-xml-data-raw [pred xml-raw]
  (cdxml/emit-str
   (sequence pred (xml-seq (cdxml/parse-str xml-raw)))))

(defn tagp [pred]
  (filter (comp pred :tag)))

(defn tag= [tag]
  (tagp (partial = tag)))

(defn tag!= [tag]
  (tagp (partial not= tag)))
