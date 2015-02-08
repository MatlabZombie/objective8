(ns d-cent.core
  (:require [org.httpkit.server :refer [run-server]]
            [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.flash :refer [wrap-flash]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [bidi.ring :refer [make-handler ->Resources]]
            [taoensso.tower.ring :refer [wrap-tower]]
            [d-cent.config :as config]
            [d-cent.utils :as utils]
            [d-cent.translation :refer [translation-config]]
            [d-cent.storage.storage :as storage]
            [d-cent.storage.database :as db]
            [d-cent.workflows.twitter :refer [twitter-workflow]]
            [d-cent.workflows.sign-up :refer [sign-up-workflow]]
            [d-cent.handlers.api :as api-handlers]
            [d-cent.handlers.front-end :as front-end-handlers]))

;; Custom ring middleware

(defn inject-db [handler store]
  (fn [request] (handler (assoc request :d-cent {:store store}))))

(def handlers {; Front End Handlers
               :index front-end-handlers/index
               :sign-in front-end-handlers/sign-in
               :sign-out front-end-handlers/sign-out
               :create-objective-form (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/create-objective-form) #{:signed-in})
               :create-objective-form-post (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/create-objective-form-post) #{:signed-in})
               :objective (utils/anti-forgery-hook front-end-handlers/objective-detail)
               :create-comment-form-post (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/create-comment-form-post) #{:signed-in})
               ; API Handlers
               :post-user-profile api-handlers/post-user-profile
               :find-user-by-query api-handlers/find-user-by-query
               :get-user api-handlers/get-user
               :post-objective api-handlers/post-objective
               :get-objective api-handlers/get-objective
               :post-comment api-handlers/post-comment})

(def routes
  ["/" {""                  :index

        "sign-in"           :sign-in

        "sign-out"          :sign-out

        "static/"           (->Resources {:prefix "public/"})

        "objectives"        {:post :create-objective-form-post
                             ["/create"] :create-objective-form
                             ["/" :id] :objective }

        "comments"          {:post :create-comment-form-post}

        "api/v1"            {"/users" {:post :post-user-profile
                                       :get :find-user-by-query
                                       ["/" :id] :get-user}

                            "/objectives" {:post :post-objective
                                          ["/" :id] :get-objective}

                            "/comments"   {:post :post-comment}}}

   ])

(defn app [configuration]
  (-> (make-handler routes (some-fn handlers #(when (fn? %) %)))
      (friend/authenticate (:authentication configuration))
      (wrap-tower (:translation configuration))
      wrap-keyword-params
      wrap-params
      wrap-json-params
      wrap-json-response
      wrap-flash
      wrap-session
      (inject-db (:store configuration))))

(defn create-configuration

  ([]
   (create-configuration (atom {})))

  ([store]
   {:authentication {:allow-anon? true
                     :workflows [twitter-workflow
                                 sign-up-workflow]
                     :login-uri "/sign-in"}
    :translation translation-config
    :store store
    :port (Integer/parseInt (config/get-var "PORT" "8080"))}))

(defn start [{:keys [port] :as configuration}]
  (log/info (str "Starting d-cent on port " port))
  (let [server (run-server (app configuration) {:port port})
        postgres-connection-pool (db/connect! db/postgres-spec)]
    (assoc configuration
           :server server
           :db-connection-pool postgres-connection-pool)))

(defn stop [configuration]
  (when-let [server (:server configuration)]
    (server))
  (dissoc configuration :server :db-connection-pool))

(defn main [& args]
  (-> (create-configuration)
      start))
