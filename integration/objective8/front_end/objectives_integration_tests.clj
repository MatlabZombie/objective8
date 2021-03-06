(ns objective8.front-end.objectives-integration-tests
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.handlers.front-end :as front-end]
            [objective8.http-api :as http-api]
            [objective8.config :as config]
            [objective8.integration-helpers :as helpers]
            [objective8.utils :as utils]
            [objective8.core :as core]))
(def TWITTER_ID "TWITTER_ID")

(def OBJECTIVE_ID 234)
(def USER_ID 1)

(def objectives-create-request (mock/request :get "/objectives/create"))
(def objectives-post-request (mock/request :post "/objectives"))
(def objective-view-get-request (mock/request :get (str "/objectives/" OBJECTIVE_ID)))

(def default-app (core/app core/app-config))

(facts "objectives" :integration
       (binding [config/enable-csrf false]
         (fact "authorised user can post and retrieve objective"
               (against-background (http-api/create-objective
                                     {:title "my objective title"
                                      :goal-1 "my objective goal"
                                      :description "my objective description"
                                      :end-date (utils/string->date-time "2012-12-12")
                                      :created-by-id USER_ID}) => {:_id OBJECTIVE_ID})
               (against-background
                 ;; Twitter authentication background
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
                 (http-api/create-user anything) => {:_id USER_ID})
               (let [user-session (helpers/test-context)
                     params {:title "my objective title"
                             :goal-1 "my objective goal"
                             :description "my objective description"
                             :end-date "2012-12-12"}
                     response (:response
                                (-> user-session
                                    (helpers/with-sign-in "http://localhost:8080/objectives/create")
                                    (p/request "http://localhost:8080/objectives"
                                               :request-method :post
                                               :params params)))]
                 (:flash response) => (contains "Your objective has been created!")
                 (-> response
                     :headers
                     (get "Location")) => (contains (str "/objectives/" OBJECTIVE_ID)))))

       (fact "Any user can view an objective"
             (against-background
               (http-api/get-objective OBJECTIVE_ID) => {:title "my objective title"
                                                         :goal-1 "my objective goal"
                                                         :description "my objective description"
                                                         :end-date (utils/string->date-time "2015-12-01")})
             (default-app objective-view-get-request) => (contains {:status 200})
             (default-app objective-view-get-request) => (contains {:body (contains "my objective title")})
             (default-app objective-view-get-request) => (contains {:body (contains "my objective goal")})
             (default-app objective-view-get-request) => (contains {:body (contains "my objective description")})
             (default-app objective-view-get-request) => (contains {:body (contains "01-12-2015")}))

      (fact "A user should receive a 404 if an objective doesn't exist"
            (against-background
              (http-api/get-objective OBJECTIVE_ID) => {:status 404})
              (default-app objective-view-get-request) => (contains {:status 404}))

       (fact "Any user can view comments on an objective"
             (against-background
              (http-api/get-objective OBJECTIVE_ID) => {:title "my objective title"
                                                        :goal-1 "my objective goal"
                                                        :description "my objective description"
                                                        :end-date (utils/string->date-time "2015-12-01")}
              (http-api/retrieve-comments OBJECTIVE_ID)
              => [{:_id 1
                   :_created_at "2015-02-12T16:46:18.838Z"
                   :objective-id OBJECTIVE_ID
                   :created-by-id USER_ID
                   :comment "Comment 1"}])
             (let [user-session (helpers/test-context)
                   peridot-response (p/request user-session (str "http://localhost:8080/objectives/" OBJECTIVE_ID))]
               peridot-response) => (contains {:response (contains {:body (contains "Comment 1")})})))
