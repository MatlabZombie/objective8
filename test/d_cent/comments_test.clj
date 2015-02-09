(ns d-cent.comments-test
  (:require [midje.sweet :refer :all]
            [cemerick.friend :as friend]
            [d-cent.comments :as comments]))

(def USER_ID 1)

(fact "creates a comment from a request"
      (against-background
        (friend/current-authentication) => {:username USER_ID})
      (let [comment (comments/request->comment {:params {:comment "the comment"
                                                         :root-id "123"
                                                         :parent-id "123"}})]
           comment => {:comment "the comment"
                       :root-id 123
                       :parent-id 123
                       :created-by-id USER_ID}))
