(ns konu-notes.buddy_handler
  (:require
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
        ;valid? (some-> authdata
        ;               (get (keyword username))
        ;               (= password))]
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
  (println " in parse header")
  (print request)
  (flush)
  (print (parse-header (get request :headers) "authorization"))
  (flush)
  (some->> (parse-header (get request :headers) "authorization")
           (re-find (re-pattern (str "^" "Token" " (.+)$")))
           (second)))

(defn logout
  [request]
  (if-not (authenticated? request)
    (throw-unauthorized)
    (let [token (parse-authorization-header request)]
      (swap! tokens dissoc @tokens (key token))
      (ok {:message (str "You have been signed out.")}))))

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
; )
(defn ping-route [version]
  (GET "/ping" []
       (json {:ping "pong"
              :date (java.util.Date.)
              :version version})))

;; (defn wrap-authentication [request fxn params]
;;   (if-not (authenticated? request)
;;     (throw-unauthorized)
;;     (fxn params)))


(defroutes user-routes
;; TODO refactor
  (POST "/note" {data :params}
        (if-not (authenticated? data)
          (throw-unauthorized)
          (json (note/create-note data))))

  (PUT "/note/:id" {data :params}
       (if-not (authenticated? data)
         (throw-unauthorized)
         (json (note/update-note (get data :id) (dissoc data :id)))))

  (GET "/note" {data :params}
       (if-not (authenticated? data)
         (throw-unauthorized)
         (json (note/search-note data))))

  (GET "/note/:id" {data :params}
       (if-not (authenticated? data)
         (throw-unauthorized)
         (json (note/search-note (json {:_id (ObjectId. (:id data))})))))

  (DELETE "/note/:id" {data :params}
          (if-not (authenticated? data)
            (throw-unauthorized)
            (do
            (note/delete-note (:id data))
            (json {:_id (:_id (:id data))}))))

  (POST "/notebook" {data :params}
        (if-not (authenticated? data)
          (throw-unauthorized)
          (json (notebook/create-notebook data))))

  (PUT "/notebook/:id" {data :params}
       (if-not (authenticated? data)
         (throw-unauthorized)
         (json (notebook/update-notebook (get data :id) (dissoc data :id)))))

  (GET "/notebook" {data :params}
       (if-not (authenticated? data)
         (throw-unauthorized)
         (json (notebook/search-notebook data))))

  (GET "/notebook/:id" {data :params}
       (if-not (authenticated? data)
         (throw-unauthorized)
         (json (notebook/search-notebook (json {:_id (ObjectId. (:id data))})))))

  (DELETE "/notebook/:id" {data :params}
          (if-not (authenticated? data)
            (throw-unauthorized)
            (do
            (notebook/delete-notebook (:id data))
            (json {:_id (:_id (:id data))}))))

  )

(defroutes app-routes

  (POST "/note" request
        (do
          (println request)
        (if-not (authenticated? request)
          (throw-unauthorized)
          (json (note/create-note (:params request))))))

  (PUT "/note/:id" request
       (if-not (authenticated? request)
         (throw-unauthorized)
         (json (note/update-note (get (:params request) :id) (dissoc (:params request) :id)))))

  (GET "/note" request
       (do
         (println request)
       (if-not (authenticated? request)
         (throw-unauthorized)
         (json (note/search-note (:params request))))))

  (GET "/note/:id" request
       (if-not (authenticated? request)
         (throw-unauthorized)
         (json (note/search-note (json {:_id (ObjectId. (:id (:params request)))})))))

  (DELETE "/note/:id" request
          (if-not (authenticated? request)
            (throw-unauthorized)
            (do
            (note/delete-note (:id (:params request)))
            (json {:_id (:_id (:id (:params request)))}))))

  (POST "/notebook" request
        (if-not (authenticated? (:params request))
          (throw-unauthorized)
          (json (notebook/create-notebook (:params request)))))

  (PUT "/notebook/:id" request
       (if-not (authenticated? request)
         (throw-unauthorized)
         (json (notebook/update-notebook (get (:params request) :id) (dissoc (:params request) :id)))))

  (GET "/notebook" request
       (if-not (authenticated? request)
         (throw-unauthorized)
         (json (notebook/search-notebook (:params request)))))

  (GET "/notebook/:id" request
       (if-not (authenticated? request)
         (throw-unauthorized)
         (json (notebook/search-notebook (json {:_id (ObjectId. (:id (:params request)))})))))

  (DELETE "/notebook/:id" request
          (if-not (authenticated? request)
            (throw-unauthorized)
            (do
            (notebook/delete-notebook (:id (:params request)))
            (json {:_id (:_id (:id (:params request)))}))))

  (GET "/testLoggedIn" request
       (json (testLoggedIn request)))
  ; Account creation with user-level privilege.
  (POST "/user" {data :params}
        (json (authentication/create-user data)))

  (POST "/logout" {:keys [headers params body] :as request} (logout request))

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

(def app
  (->
   app-routes
   (wrap-authorization auth-backend)
   (wrap-authentication auth-backend)
   (wrap-keyword-params)
   (wrap-params)
   (wrap-session)
   handler/site
   (middleware/wrap-json-body {:keywords? true :bigdecimals? true})
   middleware/wrap-json-params
   ;; Allow origin.
   (wrap-cors :access-control-allow-origin #"http://localhost:8888"
              :access-control-allow-methods [:get :put :post :delete])))

