(defproject todomvc "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :jvm-opts ^:replace ["-Xms4g" "-Xmx4g" "-server"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2280"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
								 [prismatic/om-tools "0.3.6"]
								 [prismatic/schema "0.3.2"]
								 [secretary "1.2.0"]
								 [sablono "0.2.22"]
                 [om "0.7.1"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]]

  :source-paths ["src"]

  :aliases {
    "develop" ["do" ["clean"] ["cljsbuild" "auto" "dev"]]
    "publish" ["do" ["clean"] ["cljsbuild" "once" "release"]]}

  :cljsbuild {
      :builds [{:id "dev"
                :source-paths ["src"]
                :compiler {:output-to "out/dev/app.js"
                           :output-dir "out/dev"
                           :optimizations :none
                           :source-map true}}
               {:id "release"
                :source-paths ["src"]
                :compiler {:output-dir "out/prod"
                           :output-to "out/prod/app.js"
                           :optimizations :advanced
                           :elide-asserts true
                           :pretty-print false
                           :output-wrapper false
                           :source-map "out/prod/app.js.map"
                           :externs ["src/react-externs.js"]}}]})

