(ns konu-notes.note
  (:require [monger.collection :as mc]
            [monger.core :as mg]
            [monger.conversion :as mconversion]
            [cheshire.core :as json])
  (:import [org.bson.types ObjectId]))

(def to-json json/generate-string)

; Mapper methods for notes.

(defn fetch-note [id]
  (mc/find-one-as-map "notes" { :_id id }))

(defn search-note [params]
  (mc/find-maps "notes" params))

(defn create [newPost]
  (println "making new post")
  (println (str newPost))
  (let [id (ObjectId.)]
    (mc/insert-and-return "notes" newPost)
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

(defn update-note [id data]
  ; Do not include the id in updated values.
  (get
   (mc/update-by-id "notes" (ObjectId. id) (dissoc data :_id))
   :err))

(defn delete-note [id]
  (mc/remove-by-id "notes" (ObjectId. id)))

(defn find-all-notes []
  (mc/find-maps "notes"))
