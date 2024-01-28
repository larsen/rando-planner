(ns rando-planner.xml-test
  (:require [clojure.data.xml :as xml]
            [rando-planner.xml :as rxml]
            [clojure.test :refer [deftest is testing]]))

(deftest xml-tests
  (testing "filter-xml"
    (let [resource "gpx/test.gpx"
          filtered-trkpt "gpx/test-filtered-trkpt.gpx"]
      (testing "identity"
        (is (= (xml/parse-str
                (rxml/xml-data-raw resource))
               (xml/parse-str
                (rxml/filter-xml-data-raw identity
                                         (rxml/xml-data-raw resource))))))
      (testing "filtering all instances of a tag"
        ;; (is (= (xml/parse-str
        ;;         (rxml/xml-data-raw filtered-trkpt))
        ;;        (xml/parse-str
        ;;         (rxml/filter-xml-data-raw (rxml/tag!= :trkpt)
        ;;                                  (rxml/xml-data-raw resource)))))
        ))))
