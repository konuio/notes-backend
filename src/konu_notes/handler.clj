(ns konu-notes.handler
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
   [cemerick.friend :as friend]
   (cemerick.friend [workflows :as workflows]
                    [credentials :as creds])
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]])
  (:import [org.bson.types ObjectId]))

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

(defn ping-route [version]
  (GET "/ping" []
       (json {:ping "pong"
              :date (java.util.Date.)
              :version version})))

(defroutes user-routes

  (POST "/note" {data :params}
        (json (note/create-note data)))

  (PUT "/note/:id" {data :params}
       (json (note/update-note (get data :id) (dissoc data :id))))

  (GET "/note" {data :params}
       (json (note/search-note data)))

  ; path parameters returning json
  (GET "/note/:id" [id]
       (json (note/search-note (json {:_id (ObjectId. id)}))))

  (DELETE "/note/:id" [id]
          (note/delete-note id)
          (json {:_id (:_id id)}))

  (POST "/notebook" {data :params}
        (json (notebook/create-notebook data)))

  (PUT "/notebook/:id" {data :params}
       (json (notebook/update-notebook (get data :id) (dissoc data :id))))

  (GET "/notebook" {data :params}
       (json (notebook/search-notebook data)))

  (GET "/notebook/:id" [id]
       (json (notebook/search-notebook (json {:_id (ObjectId. id)}))))

  (DELETE "/notebook/:id" [id]
          (notebook/delete-notebook id)
          (json {:_id (:_id id)}))

  )

(defroutes app-routes

  ;; requires user role
  (context "/authenticated" request
           (friend/wrap-authorize user-routes authentication/user-role))

  ;; requires admin role
  (GET "/admin" request (friend/authorize authentication/admin-role
                                          "Admin page."))

  ; Account creation with user-level privilege.
  (POST "/user" {data :params}
        (json (authentication/create-user data)))

  ; static route
  (GET "/" [] "Welcome to Konu Notes!")

  (GET "/login" [] (ring.util.response/file-response "login.html" {:root "resources"}))

  (friend/logout (ANY "/logout" request (ring.util.response/redirect "/")))

  ; contexts /api/v2/ping etc.
  (context "/api" []
           (context "/v:version" [version]
                    (ping-route version)))

  ; serve public resources
  (route/resources "/")

  ; nothing matched
  (route/not-found "Not Found"))


(defn get-users [arg]
  (authentication/find-all-users))

;; TODO allow heirarchical privileges?
;(derive ::admin ::user)

(def app
  (->
   app-routes
   (friend/authenticate {:credential-fn (partial creds/bcrypt-credential-fn
                                                 (fn [id]
                                                   (when-let [found-user
                                                              (authentication/get-user-by-username id)]
                                                     found-user)))
                         :workflows [(workflows/interactive-form)]
                         :redirect-on-auth? false
                         :unauthenticated-handler (constantly {:status 401})})

   (wrap-keyword-params)
   (wrap-params)
   (wrap-session)
   handler/site
   middleware/wrap-json-body
   middleware/wrap-json-params

   (wrap-cors :access-control-allow-origin #"http://localhost:8888"
              :access-control-allow-methods [:get :put :post :delete])))
