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

(defn fetch [id]
  (mc/find-one-as-map "notes" { :_id id }))

(defn search [params]
  (mc/find-maps "notes" params))

(defn create [newPost]
  (println "making new post")
  (println (str newPost))
  (let [id (ObjectId.)]
   ;(mc/insert-and-return "notes" (assoc newPost :_id id))
    (mc/insert-and-return "notes"  newPost)
))

;(defn add-task [task]
;  (insert! :notes (assoc task :_id (uuid))))

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

;(defn update-task [id task]
;  (let [task-in-db (find-task id)]
;    (update! :notes
;      task-in-db
;      (merge-with-kw-keys task-in-db task))))

;(defn destroy-note [id]
;  (destroy! :notes
;    (find-task id)))

(defn find-all-notes []
  (mc/find-maps "notes"))

;(defn find-note [id]
;  (fetch-one :notes :where {:_id id}))
