(ns objective8.core
  (:use [org.httpkit.server :only [run-server]])
  (:require [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.flash :refer [wrap-flash]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [bidi.ring :refer [make-handler ->Resources]]
            [taoensso.tower.ring :refer [wrap-tower]]
            [objective8.config :as config]
            [objective8.utils :as utils]
            [objective8.translation :refer [translation-config]]
            [objective8.storage.storage :as storage]
            [objective8.storage.database :as db]
            [objective8.workflows.twitter :refer [twitter-workflow]]
            [objective8.workflows.sign-up :refer [sign-up-workflow]]
            [objective8.handlers.api :as api-handlers]
            [objective8.handlers.front-end :as front-end-handlers]
            [objective8.middleware :as m]
            [objective8.bearer-tokens :as bt])
  (:gen-class))

(def handlers {; Front End Handlers
               :index front-end-handlers/index
               :sign-in front-end-handlers/sign-in
               :sign-out front-end-handlers/sign-out
               :project-status front-end-handlers/project-status
               :create-objective-form (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/create-objective-form) #{:signed-in})
               :create-objective-form-post (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/create-objective-form-post) #{:signed-in})
               :objective-list front-end-handlers/objective-list
               :objective (utils/anti-forgery-hook front-end-handlers/objective-detail)
               :create-comment-form-post (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/create-comment-form-post) #{:signed-in})
               :add-question-form-post (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/add-question-form-post) #{:signed-in})
               :add-question-form (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/add-question-form) #{:signed-in})
               :question front-end-handlers/question-detail

               
               ; API Handlers
               :post-user-profile api-handlers/post-user-profile
               :find-user-by-query api-handlers/find-user-by-query
               :get-user api-handlers/get-user
               :post-objective api-handlers/post-objective
               :get-objective api-handlers/get-objective 
               :get-comments-for-objective api-handlers/retrieve-comments
               :post-comment api-handlers/post-comment
               :post-question api-handlers/post-question
               :get-question api-handlers/get-question
               :get-answers-for-question api-handlers/retrieve-answers
               :post-answer api-handlers/post-answer})

(def routes
  ["/" {""                  :index

        "sign-in"           :sign-in

        "sign-out"          :sign-out

        "project-status"    :project-status

        "static/"           (->Resources {:prefix "public/"})

        "objectives"        {:get :objective-list
                             :post :create-objective-form-post
                             ["/create"] :create-objective-form
                             ["/" :id] {:get :objective
                                        "/questions" {:post :add-question-form-post
                                                      "/add" :add-question-form
                                                      ["/" :q-id] :question}}}

        "comments"          {:post :create-comment-form-post}

        "api/v1"            {"/users" {:post :post-user-profile
                                       :get :find-user-by-query
                                       ["/" :id] :get-user}

                             "/objectives" {:post :post-objective
                                            ["/" :id] {:get :get-objective
                                                       "/comments" :get-comments-for-objective
                                                       "/questions" {:post :post-question
                                                                     ["/" :q-id] {:get :get-question
                                                                                  "/answers" {:get :get-answers-for-question
                                                                                              :post :post-answer}}}}}

                             "/comments"   {:post :post-comment}}}])

(defn wrap-not-found [handler]
  (fn [request]
    (if-let [response (handler request)]
      response
      (front-end-handlers/error-404 request))))

(defn app [app-config]
  (-> (make-handler routes (some-fn handlers #(when (fn? %) %)))
      wrap-not-found
      (friend/authenticate (:authentication app-config))
      (wrap-tower (:translation app-config))
      wrap-keyword-params
      wrap-params
      wrap-json-params
      wrap-json-response
      wrap-flash
      wrap-session))

(defonce server (atom nil))
(defonce postgres-connection-pool (atom nil))


(def app-config
  {:authentication {:allow-anon? true
                    :workflows [twitter-workflow,
                                sign-up-workflow]
                    :login-uri "/sign-in"}
   :translation translation-config})

(defn start-server []
  (let [port (Integer/parseInt (config/get-var "APP_PORT" "8080"))]
    (reset! postgres-connection-pool (db/connect! db/postgres-spec))
    (log/info (str "Starting objective8 on port " port))
    (reset! server (run-server (app app-config) {:port port}))))

(defn -main []
  (start-server))

(defn stop-server []
  (when-not (nil? @server)
    (@server)
    (reset! server nil)))

(defn restart-server []
  (stop-server)
  (start-server))
