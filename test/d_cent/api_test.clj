(ns d-cent.api-test
  (:use org.httpkit.fake)
  (:require [org.httpkit.client :as http]
            [midje.sweet :refer :all]
            [d-cent.http-api :as api]
            [d-cent.utils :as utils]
            [cheshire.core :as json]))

(def host-url utils/host-url)

;USERS
(def USER_ID 1)
(def the-user {:twitter-id "twitter-TWITTER_ID"
               :email-address "blah@blah.com"})

(def the-stored-user (into the-user {:_id USER_ID}))
(def profile-posts
  {:successful {:status 201
                :headers {"Content-Type" "application/json"}
                :body (json/generate-string the-stored-user)}
   :failure    {:status 500}})


(facts "about user profiles"
       (fact "returns stored profile when post succeeds"
             (with-fake-http [(str host-url "/api/v1/users") (:successful profile-posts)]
               (api/create-user the-user))
             => the-stored-user)

       (fact "returns api-failure when post fails"
             (with-fake-http [(str host-url "/api/v1/users") (:failure profile-posts)]
               (api/create-user the-user))
             => api/api-failure))

;OBJECTIVES
(def OBJECTIVE_ID 234)

(def the-objective {:title "My Objective"
                    :goals "To rock out, All day"
                    :description "I like cake"
                    :end-date "2015-01-31"
                    :username "my username"})

(def the-stored-objective (into the-objective {:_id OBJECTIVE_ID}))
(def objective-posts
  {:successful {:status 201
                :headers {"Content-Type" "application/json"}
                :body (json/generate-string the-stored-objective)}
   :failure    {:status 500}})

(facts "about posting objectives"
       (fact "returns a stored objective when post succeeds"
          (with-fake-http [(str host-url "/api/v1/objectives") (:successful objective-posts)]
            (api/create-objective the-objective))
          => the-stored-objective)

    (fact "returns api-failure when post fails"
             (with-fake-http [(str host-url "/api/v1/objectives") (:failure objective-posts)]
               (api/create-objective the-objective))
             => api/api-failure))

(facts "about getting objectives"
       (fact "returns a stored objective when one exists with given id"
             (with-fake-http [(str host-url "/api/v1/objectives/" OBJECTIVE_ID)
                              {:status 200
                               :headers {"Content-Type" "application/json"}
                               :body (json/generate-string
                                      {:_id OBJECTIVE_ID
                                       :title "Objective title"
                                       :goals "Objective goals"
                                       :description "Objective description"
                                       :end-date "2015-01-31T00:00:00.000Z"
                                       :user-guid USER_ID})}]
               (api/get-objective OBJECTIVE_ID))
             => (contains {:_id OBJECTIVE_ID
                           :title "Objective title"
                           :goals "Objective goals"
                           :description "Objective description"
                           :end-date (utils/time-string->date-time "2015-01-31T00:00:00.000Z")}))
       (fact "returns api-failure when no objective found"
             (with-fake-http [(str host-url "/api/v1/objectives/" OBJECTIVE_ID)
                              {:status 404}]
               (api/get-objective OBJECTIVE_ID))
             => api/api-failure))

;; COMMENTS

(def the-comment {:comment "The comment"
                  :objective-id OBJECTIVE_ID
                  :username "my username"})

(def the-stored-comment (into the-comment {:_id 1}))
(def comment-response
  {:successful {:status 201
                :headers {"Content-Type" "application/json"}
                :body (json/generate-string the-stored-comment)}
   :failure    {:status 500}})


(facts "about posting comments"
       (fact "returns a stored comment when post succeeds"
          (with-fake-http [(str host-url "/api/v1/comments") (:successful comment-response)]
            (api/create-comment the-comment))
          => the-stored-comment)

    (fact "returns api-failure when post fails"
             (with-fake-http [(str host-url "/api/v1/comments") (:failure comment-response)]
               (api/create-comment the-comment))
             => api/api-failure))
