(ns konu-notes.note
  (:require [konu-notes.mapper :as mapper]
            [monger.collection :as mc]
            [monger.core :as mg]
            [monger.conversion :as mconversion]
            [cheshire.core :as json])
  (:import [org.bson.types ObjectId]))


; Mapper methods for notes.
(def get-namespace
  "notes")

(defn fetch-note [id]
  (mapper/fetch get-namespace id))

(defn search-note [params]
  (mapper/search get-namespace params))

(defn create-note [newPost]
  (mapper/create get-namespace newPost))

(defn update-note [id data]
  (mapper/update get-namespace id data))

(defn delete-note [id]
  (mapper/delete-by-id get-namespace id))

(defn find-all-notes []
  (mapper/find-all get-namespace))
