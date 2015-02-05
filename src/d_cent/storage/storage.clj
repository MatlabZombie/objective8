(ns d-cent.storage.storage
  (:require [korma.core :as korma]
            [d-cent.storage.mappings :as mappings]
            [d-cent.storage.database :as database]))

(defprotocol Store
  (init [this])
  (store! [this map])
  (retrieve [this query]))

(defn request->store
  "Fetches the store from the request"
  [request]
  (:store (:d-cent request)))

;; PostGres store

(defn insert
  "Wrapper around Korma's insert call"
  [entity data]
  (korma/insert entity (korma/values data)))

(defn pg-store!
  "Transform a map according to it's :entity value and save it in the database"
  [{:keys [entity] :as m}]
  (if-let [ent (mappings/get-mapping m)]
    (insert ent m)
    (throw (Exception. "Could not find database mapping for " entity))))

(defn select
  "Wrapper around Korma's select call"
  [entity where]
  (korma/select entity (korma/where where)))

(defn- -to_
  "Replaces hyphens in keys with underscores"
  [m]
  (let [ks (keys m) vs (vals m)]
    (zipmap (map (fn [k] (-> (clojure.string/replace k #"-" "_")
                             (subs 1)
                             keyword)) ks)
            vs)))

(defn pg-retrieve
  "Retrieves objects from the database based on a query map
  
   - The map must include an :entity key
   - Hyphens in key words are replaced with underscores"
  [{:keys [entity] :as query}]
  (if entity
    (let [result (select (mappings/get-mapping query) (-to_ (dissoc query :entity)))]
      {:query query
       :result result})
    (throw (Exception. "Query map requires an :entity key"))))

(defrecord PostgresStore [spec]
  Store
  (init [this]
    (database/connect! (:spec this))
    this)
  (store! [this m] (pg-store! m))
  (retrieve [this query] (pg-retrieve query)))

;; In memory store

(defn gen-id [store-atom] (:__last-id (swap! store-atom update-in [:__last-id] inc)))

(defn im-insert [entity data]
  (throw (ex-info "not implemented")))

(defn im-store! [store-atom record]
  (let [record-to-save (assoc record :_id (gen-id store-atom))
        collection (:entity record)]
    (swap! store-atom update-in [collection] conj record-to-save)
    record-to-save))

(defn find-by 
  "Retrieves the first record to match predicate"
  [store collection predicate]
  (first (filter predicate (get @store collection))))

(defn is-submap? [map1 map2]
  (= map1 (select-keys map2 (keys map1))))

(defn im-retrieve [store-atom query]
  (let [pred (partial is-submap? query)
        collection (:entity query)
        result (filter pred (collection @store-atom))]
    {:query query
     :result result}
    ))

(defrecord InMemoryStore [store-atom]
  Store
  (init [this] (assoc this :store-atom (atom {:__last-id 0})))
  (store! [this m] (im-store! (:store-atom this) m))
  (retrieve [this query] (im-retrieve (:store-atom this) query)))
