(ns konu-notes.notebook
  (:require [konu-notes.mapper :as mapper]
            [konu-notes.note :as note]
            [monger.collection :as mc]
            [monger.core :as mg]
            [monger.conversion :as mconversion]
            [cheshire.core :as json])
  (:import [org.bson.types ObjectId]))


; Mapper methods for notebooks.
(def get-namespace
  "notebooks")

(defn fetch-notebook [id]
  (mapper/fetch get-namespace id))

(defn search-notebook [params]
  (mapper/search get-namespace params))

(defn create-notebook [newPost]
  (mapper/create get-namespace newPost))

(defn update-notebook [id data]
  (mapper/update get-namespace id data))

(defn delete-notebook [id]
  (mapper/delete-by-id get-namespace id)
  ; Delete associated notes.
  (mapper/remove-from-collection note/get-namespace {:notebook id}))

(defn find-all-notebooks []
  (mapper/find-all get-namespace))
