(ns konu-notes.config
  (:require [clojure.edn :as edn]))

(def config (edn/read-string (slurp "./config/notes.edn")))
