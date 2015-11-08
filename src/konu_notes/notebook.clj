(ns konu-notes.notebook
  (:require [konu-notes.mapper :as mapper]
            [monger.collection :as mc]
            [monger.core :as mg]
            [monger.conversion :as mconversion]
            [cheshire.core :as json])
  (:import [org.bson.types ObjectId]))


; Mapper methods for notes.
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
  (mapper/delete get-namespace id))

(defn find-all-notebooks []
  (mapper/find-all get-namespace))
