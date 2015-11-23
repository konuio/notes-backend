(defproject konu-notes "0.1.0-SNAPSHOT"
  :description "Konu notes backend"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"] ; may not work with friend auth library
                ; [org.clojure/clojure "1.5.1"]
                 [cheshire "5.5.0"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [compojure "1.4.0"]
                 ;[com.novemberain/monger "3.0.1"] ; may not work with clojure 1.5.1
                 [com.novemberain/monger "1.5.0"]
                 [org.clojure/data.json "0.2.6"]
                 [ring-cors "0.1.7"]
                 [com.cemerick/friend "0.2.1"]
                 [buddy "0.8.0"] ; all deps
                 [com.draines/postal "1.11.3"]
                ; [buddy/buddy-auth "0.7.0"]
                ; [buddy/buddy-hashers "0.9.0"]
]

  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler konu-notes.buddy_handler/app}
  :main konu-notes.server
  :profiles {:uberjar {:aot :all}
             :dev {:plugins [[lein2-eclipse "2.0.0"]
                             [lein-idea "1.0.1"]]
                   :dependencies [[ring/ring-mock "0.3.0"]
                                  [midje "1.8.1"]
                                  [clj-http "2.0.0" :exclusions [commons-codec]]]}
             :debug [:dev {:jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"]}]})
