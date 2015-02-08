(ns user
  (:require [clojure.tools.namespace.repl :as tnr]
            [d-cent.core :as core]))

(defonce the-system nil)

; Don't try to load ./test and ./integration
(tnr/set-refresh-dirs "./src" "./dev")

(defn init []
  (alter-var-root #'the-system
                  (constantly (core/create-configuration))))

(defn start []
  (alter-var-root #'the-system core/start))

(defn stop []
  (alter-var-root #'the-system 
                  (fn [s] (when s (core/stop s)))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (tnr/refresh :after 'user/go)
  (prn "Reset"))
