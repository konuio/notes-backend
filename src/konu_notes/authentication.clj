(ns konu-notes.authentication
  (:require
   [konu-notes.mapper :as mapper]
   [monger.json] ; Serialization support for Mongo types.
   [compojure.core :refer :all]
   [cheshire.core :as cheshire]
   [cemerick.friend :as friend]
   [buddy.hashers :as hashers]
   [cemerick.friend.workflows :refer [make-auth]]
   (cemerick.friend [workflows :as workflows]
                    [credentials :as creds])))

; a dummy in-memory user "database" for testing purposes
(def users {"root" {:username "root"
                    :password (creds/hash-bcrypt "admin_password")
                    :roles #{::admin}}
            "jane" {:username "jane"
                    :password (creds/hash-bcrypt "user_password")
                    :roles #{::user}}})

; Mapper functions for users and permissions
(def get-namespace
  "users")

(def user-role
  "user")

(def admin-role
  "admin")

(defn fetch-user [id]
  (mapper/fetch get-namespace id))

(defn search-user [params]
  (mapper/search get-namespace params))

(defn create-user [newUser]
  (let [hashedUser {:username (get newUser :username)
                    ; :password (creds/hash-bcrypt (get newUser :password))
                    :password (hashers/encrypt (get newUser :password)
                                               {:alg :bcrypt+sha512 :salt
                                                (byte-array
                                                 [(byte 0) (byte 1) (byte 2) (byte 3)
                                                  (byte 0) (byte 1) (byte 2) (byte 3)
                                                  (byte 0) (byte 1) (byte 2) (byte 3)
                                                  (byte 0) (byte 1) (byte 2) (byte 3)])})
                    :roles user-role}]

    (print hashedUser)
    (mapper/create get-namespace hashedUser)))

(defn update-user [id data]
  (mapper/update get-namespace id data))

(defn delete-user [id]
  (mapper/delete-by-id get-namespace id))

(defn find-all-users []
  (mapper/find-all get-namespace))


; Customize authentication.
;; (defn do-login [req]
;;   (let [credential-fn (get-in req [::friend/auth-config :credential-fn])]
;;     (make-auth (credential-fn (select-keys (:params req) [:username :password])))))

;; (defn password-workflow [req]
;;   (when (and (= (:request-method req) :post)
;;              (= (:uri req) "/login"))
;;     (do-login req)))

(defn get-user-by-username [username]
  (print (str "event=login_attempt, username=" username))
  (flush)
  (let [found-user (search-user {:username username})]
    (first found-user)))

;; Customize credential fxn here.
;; (defn password-credential-fn [creds-map]
;;   (when-let [user (get-user-by-username (get creds-map :username))]
;;     (print (str "event=login_success, username=" (get creds-map :username)))
;;     (flush)
;;     (when (= (:password user) (creds/hash-bcrypt (get creds-map :password)))
;;       {:identity (:_id user) :roles #{::user} :user user})))

