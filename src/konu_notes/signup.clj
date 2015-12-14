(ns konu-notes.signup
  (:require [konu-notes.mailer :as mailer]
            [monger.collection :as mc]
            [monger.core :as mg]
            [monger.json] ; Serialization support for Mongo types.
            [konu-notes.constants :as konu-constants]
            [konu-notes.config :as config]
            [konu-notes.mapper :as mapper]
            [konu-notes.authentication :as authentication])
  (:import [org.bson.types ObjectId]))

(defn create-signup-token [hashed-user]
  (let [token (authentication/random-token)]
    (do
      (mapper/create konu-constants/signup-tokens-coll {:token token
                                         :username (:username hashed-user)})
      token)
    ))

(defn signup [new-user]
  "Creates new user and sends a registration email."
  (let [username-user (authentication/get-user-by-username (:username new-user))
        email-user (authentication/get-user-by {:email (:email new-user)})]
    (cond
      username-user {:error :duplicate-username}
      email-user {:error :duplicate-email}
      :else (let [hashed-user (authentication/create-staged-user new-user)
                  signup-token (create-signup-token hashed-user)]
        (mailer/send-mail {:from (:email-from config/config)
                           :to (:email new-user)
                           :subject "Konu Notes Registration"
                           :body (str "Thank you for signing up, " (:username new-user) "! "  "Please visit the following link to confirm your registration: https://konu.io/redeem-signup?token=" signup-token)})))))

(defn redeem-signup [request]
  (let [found-tokens (mapper/search konu-constants/signup-tokens-coll {:token (:token request)})]
    (print found-tokens)
    (println (str "found token in request " (:token request)))
    (flush)
    (if (< 0 (count found-tokens))
      ;; Authenticate user and provide session token. Delete used signup token. Migrate staged user.
      (do
        (let [stagedUser (first (mapper/search
                          konu-constants/staged-users-coll
                          {:username (:username (first found-tokens))}))]
          (do
            (mapper/create konu-constants/users-coll (dissoc stagedUser :_id))
            (mapper/delete-by-id konu-constants/staged-users-coll (:_id stagedUser))
            ))
        (mapper/remove-from-collection konu-constants/signup-tokens-coll {:token (:token request)})
        (authentication/no-auth-login (first found-tokens)))
      (throw (Exception. "Signup link is invalid.")))))
