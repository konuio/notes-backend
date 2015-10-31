(ns konu-notes.handler
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.json :as middleware]
            [ring.util.response :refer [response content-type]]
            [cheshire.core :as cheshire]
            [konu-notes.note :as note]
            [monger.json]
            [compojure.core :refer :all]))

(defn json [form]
  (-> form
      cheshire/encode
      response
      (content-type "application/json; charset=utf-8")))

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (cheshire/generate-string data)})

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

  (POST "/note" {data :params}
        (json (note/create data)))

  (GET "/note" {data :params}
       (json {:notes [{:id 1
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
                       :data "Milk, apples, oranges"},
                      {:id 5
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
                       :data "teeth cleaning"},
                      {:id 6
                       :title "project"
                       :notebook 2
                       :data "zxcvzxcvzxcvz"},
                      {:id 7
                       :title "Spain"
                       :notebook 3
                       :data "have tapas"},
                      {:id 8
                       :title "Italy"
                       :notebook 3
                       :data "eat Italian food"}

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
   #_middleware/wrap-json-response))
