(defproject todomvc "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :jvm-opts ^:replace ["-Xms4g" "-Xmx4g" "-server"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371"]
								 [datascript "0.7.0"]
                 [om "0.8.0-beta3"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]]

  :source-paths ["src"]

	:aliases {
	 	"develop" ["do" ["clean"] ["cljsbuild" "auto" "dev"]]
		"publish" ["do" ["clean"] ["cljsbuild" "once" "release"]]
	}

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src"]
              :compiler {
                :output-to "target/app.js"
                :output-dir "target"
                :optimizations :none
                :source-map true}}
						 {:id "release"
							:source-paths ["src"]
							:compiler {
								:output-to "target/app.js"
								:optimizations :advanced
								:elide-asserts true
								:pretty-print false
								:output-wrapper false
								:source-map "app.js.map"
								:externs ["src/react-externs.js"]}}]})

