(defproject todomvc "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :jvm-opts ^:replace ["-Xms4g" "-Xmx4g" "-server"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
								 [com.vitalreactor/derive "0.2.0-SNAPSHOT"]
								 [prismatic/om-tools "0.3.6"]
								 [prismatic/schema "0.3.2"]
								 [datascript "0.5.2"]
								 [secretary "1.2.1"]
								 [sablono "0.2.22"]
                 [om "0.8.0-beta3"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]]

  :source-paths ["src"]

	:aliases {
	 	"develop" ["do" ["cljsbuild" "clean"] ["cljsbuild" "auto" "dev"]]
		"publish" ["do" ["cljsbuild" "clean"] ["cljsbuild" "once" "release"]]
	}

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src"]
              :compiler {
                :output-to "out/app.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}
						 {:id "release"
							:source-paths ["src"]
							:compiler {
								:output-to "out/app.js"
								:optimizations :advanced
								:elide-asserts true
								:pretty-print false
								:output-wrapper false
								:source-map "app.js.map"
								:externs ["src/react-externs.js"]}}]})

