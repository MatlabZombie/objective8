(ns d-cent.main
  (:gen-class))

(defn -main [& args]
  (require 'd-cent.core)
  (eval `(apply d-cent.core/main ~args)))
