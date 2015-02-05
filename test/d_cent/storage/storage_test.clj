(ns d-cent.storage.storage_test
  (:require [midje.sweet :refer :all]
            [korma.core :as korma]
            [d-cent.storage.storage :as s]
            [d-cent.storage.mappings :as m]
            ))

(def test-db (s/init (s/->InMemoryStore nil)))

(facts "about in memory DB"

       (fact "can store and retrieve entities"
             (let [saved-record (s/store! test-db {:entity :user
                                                   :user-id "twitter-USER_ID"
                                                   :email-address "blah@blah.com"})
                   {query :query result :result} (s/retrieve test-db {:entity :user :user-id "twitter-USER_ID"})]
               (first result) => (contains {:_id anything})
               query => {:entity :user :user-id "twitter-USER_ID"}))

       (fact "attempts to store an object by looking up the entity mapping"
             (let [some-map {:foo "bar" :entity :i-am-entity}]
               (s/store! test-db some-map) => :the-id
               (provided
                (m/get-mapping some-map) => :fake-entity
                (s/in-memory-insert :fake-entity anything) => :the-id))))

(future-fact "attempts to store an object by looking up the entity mapping"
      (let [some-map {:foo "bar" :entity :i-am-entity}]
        (s/pg-store! some-map) => :the-id
        (provided
          (m/get-mapping some-map) => :fake-entity
          (s/insert :fake-entity anything) => :the-id)))

(future-fact "attempts to find an object based on a query containing entity"
      (let [some-query {:entity :i-am-entity :foo "bar" :zap "pow"}]
        (s/pg-retrieve some-query) => {:query some-query
                                       :result :expected-object }
        (provided
          (m/get-mapping some-query) => :fake-entity
          (s/select :fake-entity {:foo "bar" :zap "pow"}) => :expected-object)))

(future-fact "converts hyphens to underscores"
      (let [some-query {:entity :ent :foo-bar "wibble"}]
        (s/pg-retrieve some-query) => {:query some-query :result :expected-object}
        (provided
          (m/get-mapping some-query) => :fake-entity
          (s/select :fake-entity {:foo_bar "wibble"}) => :expected-object)))

(future-fact "throws exception if no entity key is present"
      (s/pg-retrieve {}) => (throws Exception "Query map requires an :entity key"))




;; old
(future-fact "generates ids for records"
      (let [saved-record (s/store! test-db "some-collection" {:A "B"})]
        (:_id saved-record) =not=> nil))

(future-fact "can store and retrieve records in collections"
      (let [saved-record-1 (s/store! test-db "some-collection" {:is "a document"})
            saved-record-2 (s/store! test-db "some-other-collection" {:is "another document"})]
        
        saved-record-1 => (contains {:is "a document"} :in-any-order) 
        saved-record-2 => (contains {:is "another document"} :in-any-order) 

        (s/retrieve test-db "some-collection" (:_id saved-record-1)) => (contains {:is "a document"} :in-any-order)
        (s/retrieve test-db "some-other-collection" (:_id saved-record-2)) => (contains {:is "another document"} :in-any-order)))

(future-fact "returns nil if a record doesn't exist"
      (s/store! test-db "collection" {:foo "bar"})
      (s/retrieve test-db "collection" "not an id") => nil)

(future-fact "returns nil if a collection doesn't exist"
      (s/retrieve test-db "does not exist" "not an id") => nil)

(future-fact "fetches storage atom from request map"
      (s/request->store {:d-cent {:store :the-store}}) => :the-store)

(future-fact "fetches based on predicate"
      (s/store! test-db "users" {:a 1})
      (s/store! test-db "users" {:a 2})
      (s/store! test-db "users" {:a 3})
      (s/find-by test-db "users" #(= 1 (:a %)))
      => (contains {:a 1}))

(future-fact "returns nil if fetching based on predicate returns no records"
      (s/find-by test-db "users" #(= 4 (:a %)))
      => nil)

