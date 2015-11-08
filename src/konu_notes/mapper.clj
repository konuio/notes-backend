(ns konu-notes.mapper
  (:require [monger.collection :as mc]
            [monger.core :as mg]
            [monger.conversion :as mconversion]
            [cheshire.core :as json])
  (:import [org.bson.types ObjectId]))

(def to-json json/generate-string)

(defn fetch [id collection]
  (mc/find-one-as-map collection { :_id id }))

(defn search [collection params]
  (mc/find-maps collection params))

(defn create [collection newObject]
  (println "making new entity")
  (println (str newObject))
  (let [id (ObjectId.)]
    (mc/insert-and-return collection newObject)
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

(defn update [collection id data]
  ; Do not include the id in updated values.
  (get
   (mc/update-by-id collection (ObjectId. id) (dissoc data :_id))
   :err))

(defn delete-by-id [collection id]
  (mc/remove-by-id collection (ObjectId. id)))

(defn remove-from-collection [collection params]
  (mc/remove collection params))

(defn find-all [collection]
  (mc/find-maps collection))

