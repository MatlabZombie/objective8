;;https://github.com/ring-clojure/ring/blob/master/ring-devel/src/ring/middleware/reload.clj

(ns d-cent.middleware.reload
  "Middleware that reloads modified namespaces on each request.
  This middleware should be limited to use in development environments."
  (:use [ns-tracker.core :only (ns-tracker)]))

(defn wrap-reload
  "Reload namespaces of modified files before the request is passed to the
  supplied handler.
  Accepts the following options:
  :dirs - A list of directories that contain the source files.
          Defaults to [\"src\"]."
  {:arglists '([handler] [handler options])}
  [handler & [options]]
  (let [source-dirs (:dirs options ["src"])
        modified-namespaces (ns-tracker source-dirs)
        always-reload (:always-reload options [])]
    (fn [request]
      (doseq [ns-sym (modified-namespaces)]
        (require ns-sym :reload))
      (doseq [ns-sym always-reload]
        (require ns-sym :reload))
      (handler request))))