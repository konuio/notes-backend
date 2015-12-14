(ns konu-notes.note
  (:require [konu-notes.mapper :as mapper]
            [konu-notes.constants :as konu-constants]
            [monger.collection :as mc]
            [monger.core :as mg]
            [monger.conversion :as mconversion]
            [cheshire.core :as json])
  (:import [org.bson.types ObjectId]))


; Mapper methods for notes.

(defn fetch-note [id]
  (mapper/fetch konu-constants/notes-coll id))

(defn search-note [params]
  (mapper/search konu-constants/notes-coll params))

(defn create-note [newPost]
  (mapper/create konu-constants/notes-coll newPost))

(defn update-note [id data]
  (mapper/update konu-constants/notes-coll id data))

(defn delete-note [id]
  (mapper/delete-by-id konu-constants/notes-coll id))

(defn find-all-notes []
  (mapper/find-all konu-constants/notes-coll))
