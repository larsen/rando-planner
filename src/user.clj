(ns user)

(require '[nextjournal.clerk :as clerk])

;; start Clerk's built-in webserver on the default port 7777, opening the browser when done
;; (clerk/serve! {:browse? true})

;; ;; either call `clerk/show!` explicitly
;; (clerk/show! "notebooks/test")

;; ;; or let Clerk watch the given `:paths` for changes
;; (clerk/serve! {:watch-paths ["notebooks" "src"]})

(clerk/serve! {:watch-paths ["notebooks" "src"]})

;; start with watcher and show filter function to enable notebook pinning
;; (clerk/serve! {:watch-paths ["notebooks" "src"] :show-filter-fn #(clojure.string/starts-with? % "notebooks")})

;(clerk/build! {:paths ["notebooks/ber_ko.clj"]})
(clerk/build! {:paths ["notebooks/venetogravel.clj"]})
