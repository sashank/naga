(defproject naga "0.1.0-SNAPSHOT"
  :description "Forward Chaining Rule Engine"
  :url "http://github.com/threatgrid/naga"
  #_:license #_{:name "Eclipse Public License"
                :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [prismatic/schema "1.0.5"]
                 [org.clojure/tools.cli "0.3.5"]
                 [the/parsatron "0.0.7"]]
  :main naga.cli)
