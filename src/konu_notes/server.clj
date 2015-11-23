(ns konu-notes.server
  (:require [ring.adapter.jetty :as jetty]
            [konu-notes.buddy_handler :as handler]
            [konu-notes.note :as note]
            [konu-notes.mongo :as mongo]
            )
  (:gen-class))

(defn -main
  [& [port]]
  (do
    (mongo/init-db "development")
    (let [port (Integer. (or port (System/getenv "PORT") 8080))]
      (jetty/run-jetty #'handler/app {:port  port
                                      :join? false}))))
