(ns konu-notes.handler
  (:require
   [compojure.handler :as handler]
   [compojure.route :as route]
   [ring.middleware.json :as middleware]
   [ring.util.response :as ring]
   [cheshire.core :as cheshire]
   [konu-notes.note :as note]
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

  (GET "/notebook" []
       (json {:notebooks [{:id 1
                           :name "Personal"},
                          {:id 2
                           :name "Work"},
                          {:id 3
                           :name "Vacation"},
                          {:id 4
                           :name "Shopping"}
                          ]}))

  (GET "/notebook/:id" [id]
       (json
        (case id
          "1"
          {:notes [{:id 5
                    :title "todos"
                    :notebook 1
                    :data "konu notes"},
                   {:id 9
                    :title "birthday"
                    :notebook 1
                    :data "plan party"},
                   {:id 10
                    :title "doctor appointments"
                    :notebook 1
                    :data "teeth cleaning"}]}

          "2"
          {:notes[ {:id 6
                    :title "project"
                    :notebook 2
                    :data "zxcvzxcvzxcvz"}]}

          "3"
          {:notes [{:id 7
                    :title "Spain"
                    :notebook 3
                    :data "have tapas"},
                   {:id 8
                    :title "Italy"
                    :notebook 3
                    :data "eat Italian food"}]}

          "4"
          {:notes[{:id 1
                   :title "Shopping List"
                   :notebook 4
                   :data "Milk, apples, oranges"},
                  {:id 2
                   :title "Shopping List2"
                   :notebook 4
                   :data "asdfasdfasdf"},
                  {:id 3
                   :title "Shopping List3"
                   :notebook 4
                   :data "qwerqwerqwer"},
                  {:id 4
                   :title "Shopping List4"
                   :notebook 4
                   :data "Milk, apples, oranges"}]}

          "Error"

          )))
  )

(defroutes app-routes

  ;; requires user role
  (context "/user" request
           (friend/wrap-authorize user-routes #{::user}))

  ;; requires admin role
  (GET "/admin" request (friend/authorize #{::admin}
                                          #_any-code-requiring-admin-authorization
                                          "Admin page."))

  ; static route
  (GET "/" [] "Welcome to Konu Notes!")

  (GET "/login" [] (ring.util.response/file-response "login.html" {:root "resources"}))
  ;(GET "/login" request "Login page.")

  (friend/logout (ANY "/logout" request (ring.util.response/redirect "/")))

  ; contexts /api/v2/ping etc.
  (context "/api" []
           (context "/v:version" [version]
                    (ping-route version)))

  ; serve public resources
  (route/resources "/")

  ; nothing matched
  (route/not-found "Not Found"))

; a dummy in-memory user "database"
(def users {"root" {:username "root"
                    :password (creds/hash-bcrypt "admin_password")
                    :roles #{::admin}}
            "jane" {:username "jane"
                    :password (creds/hash-bcrypt "user_password")
                    :roles #{::user}}})

(derive ::admin ::user)

(def app
  (->
   app-routes
   (friend/authenticate {:credential-fn (partial creds/bcrypt-credential-fn users)
                         :workflows [(workflows/interactive-form)]})
   (wrap-keyword-params)
   (wrap-params)
   (wrap-session)
   handler/site
   middleware/wrap-json-body
   middleware/wrap-json-params

   (wrap-cors :access-control-allow-origin #"http://localhost:8888"
              :access-control-allow-methods [:get :put :post :delete])))
