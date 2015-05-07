(defproject pot "0.1.0-SNAPSHOT"
            :description "Power of Two Puzzle"
            :dependencies [[org.clojure/clojure "1.6.0"]
                           [org.clojure/clojurescript "0.0-3211"]
                           [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                           [org.clojure/core.match "0.2.1"]
                           [sablono "0.3.4"]
                           [org.omcljs/om "0.8.8"]]
            :plugins [[lein-cljsbuild "1.0.5"]
                      [lein-figwheel "0.3.1"]
                      [lein-garden "0.2.5"]]
            :profiles {:dev {:cljsbuild {:builds [{:id           :pot
                                                   :source-paths ["src"]
                                                   :figwheel     {:on-jsload "pot.ui.client/on-js-reload"}
                                                   :compiler     {:output-to     "out/js/pot/pot.js"
                                                                  :output-dir    "out/js/pot"
                                                                  :asset-path    "out/js/pot"
                                                                  :pretty-print  false
                                                                  :main          pot.ui.client
                                                                  :optimizations :none}}]}}}
            :aliases {"dev" ["do" ["clean"] ["cljsbuild" "once"]]})
