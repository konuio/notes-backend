(ns konu-notes.server
  (:require [ring.adapter.jetty :as jetty]
            [konu-notes.handler :as handler])
  (:gen-class))

(defn -main
  (note/init-db "development")
  [& [port]]
  (let [port (Integer. (or port (System/getenv "PORT") 8080))]
    (jetty/run-jetty #'handler/app {:port  port
                                    :join? false})))
