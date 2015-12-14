(ns konu-notes.authentication
  (:require
   [konu-notes.mapper :as mapper]
   [konu-notes.app-state :as app-state]
   [monger.json] ; Serialization support for Mongo types.
   [compojure.core :refer :all]
   [cheshire.core :as cheshire]
   [buddy.hashers :as hashers]
   [buddy.core.nonce :as nonce]
   [buddy.core.codecs :as codecs]
   [clj-time.core :as t]
   monger.joda-time))


(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (cheshire/generate-string data)})

;; Token generator helper.
(defn random-token
  []
  (let [randomdata (nonce/random-bytes 16)]
    (codecs/bytes->hex randomdata)))

;; Mapper functions for users and permissions.
(def get-namespace
  "users")

(def user-role
  "user")

(def admin-role
  "admin")

(def session-tokens-coll "session-tokens")

(defn fetch-user [id]
  (mapper/fetch get-namespace id))

(defn search-user [params]
  (mapper/search get-namespace params))

(defn create-user [newUser]
  (let [hashedUser {:email (:email newUser)
                    :username (:username newUser)
                    :password (hashers/encrypt (:password newUser)
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

(defn get-user-by [query]
  (let [found-user (search-user query)]
    (first found-user)))

(defn get-user-by-username [username]
  (print (str "event=login_attempt, username=" username))
  (flush)
  (get-user-by {:username username}))

(defn create-session [token username]
  (mapper/create session-tokens-coll {:token token :username username :lastActive (t/now)}))

(defn no-auth-login
  "Login without a password (for use by backend)."
  [params]
  (let [username (:username params)
        valid? (< 0 (count (search-user {:username username})))]
    (if valid?
      (let [token (random-token)]
        (do
          (create-session token username)
          (json-response {:token token} 200)))
      (json-response {:message "User not found."} 400))))

(defn update-last-active [token]
  (mapper/update-by-query session-tokens-coll {:token token} {:$set {:lastActive (t/now)}}))
