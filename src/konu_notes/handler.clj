(ns konu-notes.handler
    (:require
      [compojure.handler :as handler]
      [compojure.route :as route]
      [ring.middleware.json :as middleware]
      [ring.util.response :as ring]
      [cheshire.core :as cheshire]
      [konu-notes.note :as note]
      [monger.json]
      [compojure.core :refer :all]
      [ring.middleware.cors :refer [wrap-cors]]))

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (cheshire/generate-string data)})

(defn json [form]
  (-> form
      cheshire/encode
      ring/response
      (ring/content-type "application/json; charset=utf-8")))
    ; TODO consolidate response handling
    ;  (ring/header "access-control-allow-origin" "http://localhost:8888")))


(defn ping-route [version]
  (GET "/ping" []
       (json {:ping "pong"
              :date (java.util.Date.)
              :version version})))

(defroutes app-routes
  ; static route
  (GET "/" [] "Welcome to Konu Notes!")

  ; query paramters
  (GET "/hello" [name] (str "hello, " name))

  ; path parameters returning json
  (GET "/note/:id" [id]
       (json {:id "1"
              :data "milk, apples, oranges"
              :notebook "1"
              :title "Shopping List"}))

  (POST "/note" {data :params}
        (json (note/create data)))

  (PUT "/note" {data :params}
       (note/update-note data)
       (json {:_id (:_id data)}))

  (GET "/note" {data :params}
       (json (note/search-note data)))

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

  ; contexts /api/v2/ping etc.
  (context "/api" []
           (context "/v:version" [version]
                    (ping-route version)))

  ; serve public resources
  (route/resources "/")

  ; nothing matched
  (route/not-found "Not Found"))

(def app
  (->
    app-routes
    handler/site
    middleware/wrap-json-body
    middleware/wrap-json-params
    ;middleware/wrap-json-response
    (wrap-cors :access-control-allow-origin #"http://localhost:8888"
               :access-control-allow-methods [:get :put :post :delete])))
