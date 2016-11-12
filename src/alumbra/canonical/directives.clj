(ns alumbra.canonical.directives
  (:require [alumbra.canonical.arguments :refer [resolve-arguments]]))

(defn resolve-directives
  [opts directives]
  (for [{:keys [alumbra/directive-name
                alumbra/arguments]} directives]
    (cond-> {:directive-name directive-name}
      (seq arguments)
      (assoc :arguments
             (resolve-arguments opts arguments)))))
