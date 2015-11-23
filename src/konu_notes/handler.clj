(ns konu-notes.handler
  (:require
   [clojure.string :as string]
   [konu-notes.app-state :as app-state]
   [konu-notes.note :as note]
   [konu-notes.mapper :as mapper]
   [konu-notes.mailer :as mailer]
   [konu-notes.signup :as signup]
   [konu-notes.authentication :as authentication]
   [konu-notes.notebook :as notebook]
   [compojure.handler :as handler]
   [compojure.route :as route]
   [compojure.core :refer :all]
   [ring.middleware.json :as middleware]
   [ring.util.response :as ring]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [cheshire.core :as cheshire]
   [monger.json] ;; Serialization support for Mongo types.
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

;; Semantic response helpers
(defn ok [d] {:status 200 :body d})
(defn bad-request [d] {:status 400 :body d})

(def session-tokens-coll "session-tokens")

;; Authentication handler.
(defn login
  [request]
  (print "in login method")
  (print (get request :username))
  (print (get request :password))
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
      (let [token (authentication/random-token)]
        (mapper/create session-tokens-coll {:token token :username username})
        (json-response {:token token} 200))
      (json-response {:message "Incorrect username or password."} 400))))

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

(defn parse-header
  [headers ^String header-name]
  (first (filter #(.equalsIgnoreCase header-name (key %)) headers)))

;; Expect an authorization header containing the following:
;; Authorization: Token 123abc
(defn parse-authorization-header
  [request]
  (last (parse-header (get request :headers) "authorization")))

;; Helper fxn that should be wrapped with authentication.
(defn logout
  [request]
  (let [token (parse-authorization-header request)]
    (println (str "found token for logout " token))
    ;(swap! app-state/tokens dissoc app-state/get-tokens-state (keyword token))
    (mapper/remove-from-collection session-tokens-coll {:token token})
    (ok {:message (str "You have been signed out.")})))

(defroutes authorized-routes

  (POST "/note" request
        (json (note/create-note
               (assoc (:params request)
                :username (:identity request)))))

  (PUT "/note/:id" request
       (json (note/update-note (get (:params request) :id) (dissoc (:params request) :id))))

  (GET "/note" request
       (json (note/search-note
              (assoc (:params request)
                :username (:identity request)))))

  (GET "/note/:id" request
       (json (note/search-note (json {:_id (ObjectId. (:id (:params request)))}))))

  (DELETE "/note/:id" request
          (do
            (note/delete-note (:id (:params request)))
            (json {:_id (:_id (:id (:params request)))})))

  (POST "/notebook" request
        (json (notebook/create-notebook
               (assoc (:params request)
                :username (:identity request)))))

  (PUT "/notebook/:id" request
       (json (notebook/update-notebook (get (:params request) :id) (dissoc (:params request) :id))))

  (GET "/notebook" request
       (json (notebook/search-notebook
              (assoc (:params request)
                :username (:identity request)))))

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

  (POST "/signup" {data :params}
        (json (signup/signup {:email (:email data)
                              :username (:username data)
                              :password (:password data)})))

  (POST "/redeem-signup" {data :params}
        (json (signup/redeem-signup data)))

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

(defn tokenAuthFxn
  [req token]
  (when-let [entry (mapper/search session-tokens-coll {:token token})]
    (:username (first entry))))

;; Create an instance of auth backend.
(def auth-backend
  (token-backend {:authfn tokenAuthFxn}))

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

