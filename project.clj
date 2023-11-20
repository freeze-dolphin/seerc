(defproject seerc "0.1.0-SNAPSHOT"
  :description "RCON client written in clojure with seesaw"
  :url "http://example.com/FIXME"
  :license {:name "GPL-3"
            :url "https://www.gnu.org/licenses/gpl-3.0.en.html"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [seesaw "1.5.0"]
                 [nl.vv32.rcon/rcon "1.2.0"]]
  :main seerc.core
  :aot :all
  :repl-options {:init-ns seerc.core})
