(ns konu-notes.authentication
  (:require
   [konu-notes.mapper :as mapper]
   [monger.json] ; Serialization support for Mongo types.
   [compojure.core :refer :all]
   [cemerick.friend :as friend]
   (cemerick.friend [workflows :as workflows]
                    [credentials :as creds])))

; a dummy in-memory user "database"
(def users {"root" {:username "root"
                    :password (creds/hash-bcrypt "admin_password")
                    :roles #{::admin}}
            "jane" {:username "jane"
                    :password (creds/hash-bcrypt "user_password")
                    :roles #{::user}}})

; Mapper functions for users and permissions

; Mapper methods for notes.
(def get-namespace
  "users")

(defn fetch-user [id]
  (mapper/fetch get-namespace id))

(defn search-user [params]
  (mapper/search get-namespace params))

(defn create-user [newPost]
  (mapper/create get-namespace newPost))

(defn update-user [id data]
  (mapper/update get-namespace id data))

(defn delete-user [id]
  (mapper/delete get-namespace id))

(defn find-all-users []
  (mapper/find-all get-namespace))


; customize authentication
;(defn do-login [req]
;  (let [credential-fn (get-in req [::friend/auth-config :credential-fn])]
;    (make-auth (credential-fn (select-keys (:params req) [:username :password])))))

;; (defn password-workflow [req]
;;   (when (and (= (:request-method req) :post)
;;              (= (:uri req) "/login"))
;;     (do-login req)))

;; (defn password-credential-fn [{:keys [username password] :as creds}]
;;   (when-let [user (get-user-by-username username)]
;;     (when (= (:hashed_password user) (friend/hash-bcrypt password))
;;       {:identity (:id user) :roles #{::user} :user user})))
