(ns konu-notes.note
  (:require [monger.collection :as mc]
            [monger.core :as mg]
            [cheshire.core :as json])
  (:import [org.bson.types ObjectId]))

(def to-json json/generate-string)

(defn init-db [name]
  (println "connecting db")
  (mg/connect!)
  (mg/set-db! (mg/get-db name)))

(defn fetch-note [id]
  (mc/find-one-as-map "notes" { :_id id }))

(defn search-note [params]
  (mc/find-maps "notes" params))

(defn create [newPost]
  (println "making new post")
  (println (str newPost))
  (let [id (ObjectId.)]
    (mc/insert-and-return "notes"  newPost)
))

(defn keywordify-keys
  "Returns a map otherwise same as the argument but
   with all keys turned to keywords"
  [m]
  (zipmap
    (map keyword (keys m))
    (vals m)))

(defn merge-with-kw-keys
  "Merges maps converting all keys to keywords"
  [& maps]
  (reduce
    merge
    (map keywordify-keys maps)))

(defn update-note [data]
  (println "this shoudl bprint")
  (mc/update-by-id "notes" (ObjectId. (get-in data [:_id])) data))

 ;(let [found-note (fetch-note (get-in data [:_id]))]

;(defn destroy-note [id]
;  (destroy! :notes
;    (find-task id)))

(defn find-all-notes []
  (mc/find-maps "notes"))

;(defn find-note [id]
;  (fetch-one :notes :where {:_id id}))
