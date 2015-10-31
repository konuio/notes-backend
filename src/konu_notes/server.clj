(ns konu-notes.server
  (:require [ring.adapter.jetty :as jetty]
            [konu-notes.handler :as handler]
            [konu-notes.note :as note])
  (:gen-class))

(note/init-db "development")

(defn -main

  [& [port]]
  (do
    (note/init-db "development")

    (let [port (Integer. (or port (System/getenv "PORT") 8080))]
      (jetty/run-jetty #'handler/app {:port  port
                                      :join? false}))))
