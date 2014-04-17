(defproject pot "0.1.0-SNAPSHOT"
  :description "Power of Two Puzzle"
  :url "http://pavel-v-chernykh.github.io/pot/"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2202"]
                 [org.clojure/google-closure-library-third-party "0.0-20130212-95c19e7f0f5f"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 [org.clojure/core.match "0.2.1"]
                 [om "0.5.3"]
                 [sablono "0.2.14"]]
  :plugins [[lein-cljsbuild "1.0.3"]]
  :source-paths ["src"]
  :profiles {:dev {:source-paths ["dev"]
                   :cljsbuild    {:builds [{:id           "pot"
                                            :source-paths ["src" "dev"]
                                            :compiler     {:output-to     "resources/public/js/pot/pot.js"
                                                           :output-dir    "resources/public/js/pot"
                                                           :optimizations :none
                                                           :source-map    true}}]}}}
  :aliases {"dev" ["do" ["clean"] ["cljsbuild" "once"]]})
