(ns objective8.handlers.api
  (:require [clojure.tools.logging :as log]
            [ring.util.response :as response]
            [cheshire.core :as json]
            [objective8.storage.storage :as storage]
            [objective8.objectives :as objectives]
            [objective8.comments :as comments]
            [objective8.users :as users]
            [objective8.utils :as utils]))

;; USERS
(defn post-user-profile [request]
  (let [twitter-id (get-in request [:params :twitter-id])
        email-address (get-in request [:params :email-address])
        user (users/store-user! {:twitter-id twitter-id
                                        :email-address email-address})
        resource-location (str utils/host-url "/api/v1/users/" (:_id user))]
    {:status 201
     :headers {"Content-Type" "application/json"
               "Location" resource-location}
     :body user}))

(defn find-user-by-query [request]
  (let [twitter-id (get-in request [:params :twitter])]
    (if-let [user (users/find-user-by-twitter-id twitter-id)]
      (response/content-type (response/response user) "application/json")
      (response/not-found ""))))

(defn get-user [{:keys [route-params] :as request}]
  (try
    (let [id (-> (:id route-params)
                 Integer/parseInt)]
      (if-let [user (users/retrieve-user id)]
        (-> user
            response/response
            (response/content-type "application/json"))
        (response/not-found "")))
    (catch NumberFormatException e
      {:status 400
       :header {}
       :body "User id must be an integer"})))

;; OBJECTIVE
(defn post-objective [{:keys [params] :as request}]
  (let [objective (-> params
                      (select-keys [:title :goal-1 :goal-2 :goal-3 :description :end-date :created-by-id]))
        stored-objective (objectives/store-objective! objective)
        resource-location (str utils/host-url "/api/v1/objectives/" (:_id stored-objective))]
    {:status 201
     :headers {"Content-Type" "application/json"
               "Location" resource-location}
     :body stored-objective}))

(defn get-objective [{:keys [route-params] :as request}]
  (try
    (let [id (-> (:id route-params)
                 Integer/parseInt)]
      (if-let [objective (objectives/retrieve-objective id)]
        (-> objective
            (update-in [:end-date] utils/date-time->iso-date-string)
            response/response
            (response/content-type "application/json"))
        (response/not-found "")))
    (catch NumberFormatException e
      {:status 400
       :header {}
       :body "Objective id must be an integer"})))

;; COMMENT
(defn post-comment [{:keys [params] :as request}]
  (let [comment (-> params
                  (select-keys [:comment :objective-id :created-by-id]))
        stored-comment (comments/store-comment! comment)
        resource-location (str utils/host-url "/api/v1/comments/" (:_id stored-comment))]
    {:status 201
     :headers {"Content-Type" "application/json"
               "Location" resource-location}
     :body stored-comment}))

(defn retrieve-comments [{:keys [route-params] :as request}]
  (try
    (let [id (-> (:id route-params)
                 Integer/parseInt)]
      (if-let [comments (comments/retrieve-comments id)]
        (-> comments
            response/response
            (response/content-type "application/json"))
        (response/not-found "")))
    (catch NumberFormatException e
      {:status 400
       :header {}
       :body "Objective id must be an integer"})))