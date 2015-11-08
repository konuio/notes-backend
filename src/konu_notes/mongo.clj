(ns konu-notes.mongo
  (:require [monger.operators :refer :all]
            [monger.core :as mg]
            [monger.collection :as mc]
            ; [monger.ring.session-store :refer [session-store]]
            [monger.json])
  (:import [org.bson.types ObjectId]))

;(def uri "mongodb://127.0.0.1/konu-notes")
(defn init-db [name]
  (mg/connect!)
  (mg/set-db! (mg/get-db name))
  (let [conn  (mg/connect)
        db    (mg/get-db conn name)]))
