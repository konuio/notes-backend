(ns konu-notes.mongo
  (:require [monger.operators :refer :all]
            [monger.core :as m]
            [monger.collection :as mc]
            [monger.json])
  (:import [org.bson.types ObjectId]))

(def uri "mongodb://127.0.0.1/konu-notes")

(comment
  ;; connect
  (m/connect-via-uri! uri)
)
