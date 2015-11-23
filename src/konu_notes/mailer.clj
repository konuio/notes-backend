(ns konu-notes.mailer
  (:require [postal.core :refer :all]
            [konu-notes.config :as config]))

(def get-server-info
  {:host (:email-host config/config)
   :user (:email-user config/config)
   :pass (:email-pass config/config)
   :ssl :yes!!!11})

(defn send-mail [info]
  ;; Info is a map containing :from, :to, :subject, :body.
  (do
    (println get-server-info)
    (println info)
    (flush)
    (send-message get-server-info info)
    ))
