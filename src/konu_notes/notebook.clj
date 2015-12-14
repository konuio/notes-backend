(ns konu-notes.notebook
  (:require [konu-notes.mapper :as mapper]
            [konu-notes.note :as note]
            [konu-notes.constants :as konu-constants]
            [monger.collection :as mc]
            [monger.core :as mg]
            [monger.conversion :as mconversion]
            [cheshire.core :as json])
  (:import [org.bson.types ObjectId]))


; Mapper methods for notebooks.

(defn fetch-notebook [id]
  (mapper/fetch konu-constants/notebooks-coll id))

(defn search-notebook [params]
  (mapper/search konu-constants/notebooks-coll params))

(defn create-notebook [newPost]
  (mapper/create konu-constants/notebooks-coll newPost))

(defn update-notebook [id data]
  (mapper/update konu-constants/notebooks-coll id data))

(defn delete-notebook [id]
  (mapper/delete-by-id konu-constants/notebooks-coll id)
  ; Delete associated notes.
  (mapper/remove-from-collection konu-constants/notes-coll {:notebook id}))

(defn find-all-notebooks []
  (mapper/find-all konu-constants/notebooks-coll))
