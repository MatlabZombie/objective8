(ns objective8.front-end-helpers
  (:require [cemerick.friend :as friend]
            [objective8.utils :as utils]))

(defn request->question
  "Returns a map of a question if all parts are in the request. Otherwise returns nil"
  [{{id :id} :route-params
    :keys [params]} user-id]
  (assoc (select-keys params [:question])
          :created-by-id user-id
          :objective-id (Integer/parseInt id)))

(defn request->comment
  "Returns a map of a comment if all the parts are in the request params."
  [{:keys [params]} user-id]
  (if-let [objective-id (Integer/parseInt (params :objective-id))]
    (assoc (select-keys params [:comment])
            :objective-id objective-id
            :created-by-id user-id)))

(defn request->objective
  "Returns a map of an objective if all the parts are in the
  request params. Otherwise returns nil"
  [{:keys [params]} user-id]
    (let [iso-time (utils/string->date-time (:end-date params))]
      (assoc (select-keys params [:title :goal-1 :goal-2 :goal-3 :description ])
                                  :end-date iso-time
                                  :created-by-id user-id)))
