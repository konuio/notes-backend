(ns konu-notes.buddy_handler
  (:require
   [clojure.string :as string]
   [compojure.handler :as handler]
   [compojure.route :as route]
   [ring.middleware.json :as middleware]
   [ring.util.response :as ring]
   [cheshire.core :as cheshire]
   [konu-notes.note :as note]
   [konu-notes.authentication :as authentication]
   [konu-notes.notebook :as notebook]
   [monger.json] ; Serialization support for Mongo types.
   [compojure.core :refer :all]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [cemerick.friend [credentials :as creds]]
   [buddy.hashers :as hashers]
   [buddy.auth.backends.token :refer [token-backend]]
   [buddy.core.nonce :as nonce]
   [buddy.core.codecs :as codecs]
   [buddy.auth :refer [authenticated? throw-unauthorized]]
   [buddy.auth.backends.token :refer [token-backend]]
   [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]])
  (:import [org.bson.types ObjectId])
  (:gen-class))



;; TODO middleware for returning 401 not authorized
(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (cheshire/generate-string data)})

(defn json [form]
  (-> form
      cheshire/encode
      ring/response
      (ring/content-type "application/json; charset=utf-8")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Semantic response helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ok [d] {:status 200 :body d})
(defn bad-request [d] {:status 400 :body d})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Token generator helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn random-token
  []
  (let [randomdata (nonce/random-bytes 16)]
    (codecs/bytes->hex randomdata)))

;; Global var that stores valid users with their
;; respective passwords. TODO temporary!

(def authdata {:admin "secret"
               :test "secret"})

;; Global storage for store generated tokens.
(def tokens (atom {}))

;; Authentication handler.
(defn login
  [request]
  (print "in login method")
  (print (get request :username))
  (print (get request :password))
  (print (creds/hash-bcrypt (get request :password)))
  (flush)
  (let [username (get request :username)
        password (get request :password)
        valid? (< 0 (count (authentication/search-user {:username username
                                                        :password (hashers/encrypt password
                                                                                   {:alg :bcrypt+sha512 :salt
                                                                                    (byte-array
                                                                                     [(byte 0) (byte 1) (byte 2) (byte 3)
                                                                                      (byte 0) (byte 1) (byte 2) (byte 3)
                                                                                      (byte 0) (byte 1) (byte 2) (byte 3)
                                                                                      (byte 0) (byte 1) (byte 2) (byte 3)])}) })))]
    (if valid?
      (let [token (random-token)]
        (swap! tokens assoc (keyword token) (keyword username))
        (json-response {:token token} 200))
      (json-response {:message "Incorrect username or password."} 400))))

(defn parse-header
  [headers ^String header-name]
  (first (filter #(.equalsIgnoreCase header-name (key %)) headers)))

;; Expect an authorization header containing the following:
;; Authorization: Token 123abc
(defn parse-authorization-header
  [request]
  (println "logout attempt")
  (print request)
  (last (parse-header (get request :headers) "authorization")))

(defn tokenAuthFxn
  [req token]
  (when-let [user (get @tokens (keyword token))]
    user))

;; Create an instance of auth backend.
(def auth-backend
  (token-backend {:authfn tokenAuthFxn}))

;; temp route to test buddy auth
(defn testLoggedIn
  [request]
  (if-not (authenticated? request)
    (throw-unauthorized)
    (ok {:status "Logged" :message (str "hello logged user"
                                        (:identity request))})))

(defn ping-route [version]
  (GET "/ping" []
       (json {:ping "pong"
              :date (java.util.Date.)
              :version version})))

;; Helper fxn that should be wrapped with authentication.
(defn logout
  [request]
  (let [token (parse-authorization-header request)]
    (println (str "found token for logout " token))
    (swap! tokens dissoc @tokens (keyword token))
    (ok {:message (str "You have been signed out.")})))

(defroutes authorized-routes

  (POST "/note" request
        (json (note/create-note
               (assoc (:params request)
                :username (name (:identity request))))))

  (PUT "/note/:id" request
       (json (note/update-note (get (:params request) :id) (dissoc (:params request) :id))))

  (GET "/note" request
       (json (note/search-note
              (assoc (:params request)
                :username (name (:identity request))))))

  (GET "/note/:id" request
       (json (note/search-note (json {:_id (ObjectId. (:id (:params request)))}))))

  (DELETE "/note/:id" request
          (do
            (note/delete-note (:id (:params request)))
            (json {:_id (:_id (:id (:params request)))})))

  (POST "/notebook" request
        (json (notebook/create-notebook
               (assoc (:params request)
                :username (name (:identity request))))))

  (PUT "/notebook/:id" request
       (json (notebook/update-notebook (get (:params request) :id) (dissoc (:params request) :id))))

  (GET "/notebook" request
       (json (notebook/search-notebook
              (assoc (:params request)
                :username (name (:identity request))))))

  (GET "/notebook/:id" request
       (json (notebook/search-notebook (json {:_id (ObjectId. (:id (:params request)))}))))

  (DELETE "/notebook/:id" request
          (do
            (notebook/delete-notebook (:id (:params request)))
            (json {:_id (:_id (:id (:params request)))})))

  (GET "/testLoggedIn" request
       (json (testLoggedIn request)))

  (POST "/logout" {:keys [headers params body] :as request}
        (json (logout request)))
)


(defroutes public-routes

  ; Account creation with user-level privilege.
  (POST "/user" {data :params}
        (json (authentication/create-user data)))

  (GET "/" [] "Welcome to Konu Notes!")

  (GET "/login" [] (ring.util.response/file-response "login.html" {:root "resources"}))

  (POST "/login" {data :params}
        (login data))

  ; contexts /api/v2/ping etc.
  (context "/api" []
           (context "/v:version" [version]
                    (ping-route version)))

  ; serve public resources
  (route/resources "/")

  ; nothing matched
  (route/not-found "Not Found")
  )

(defn get-users [arg]
  (authentication/find-all-users))

(defn authenticate-user [request]
  (if-not (authenticated? request)
    (throw-unauthorized)
    ({:status "ok"}))) ;; TODO load the corresponding user id so functions can retrieve user data

(defn wrap-custom-authentication [auth-fxn client-fxn]
  (fn [request]
    (do
      (auth-fxn request)
      (client-fxn request))))

(defn wrap-auth [handler]
  (fn [request]
    (if (authenticated? request)
      (handler request)
      (throw-unauthorized))))

(defroutes app-routes
  (routes (-> authorized-routes
              (wrap-routes wrap-auth))
          public-routes ))

(def app
  (->
   app-routes
   (wrap-authorization auth-backend)
   (wrap-authentication auth-backend) ;; Associates :identity in the request.
   (wrap-keyword-params)
   (wrap-params)
   (wrap-session)
   handler/site
   (middleware/wrap-json-body {:keywords? true :bigdecimals? true})
   middleware/wrap-json-params
   ;; Allow origin.
   (wrap-cors :access-control-allow-origin #"http://localhost:8888"
              :access-control-allow-methods [:get :put :post :delete])))

