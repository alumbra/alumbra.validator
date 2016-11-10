(ns alumbra.canonical.operations
  (:require [alumbra.canonical.selection-set
             :refer [resolve-selection-set]]))

;; ## Helpers

(defn- root-type
  [{{:keys [schema-root]} :schema}
   {:keys [alumbra/operation-type]}]
  (get schema-root operation-type))

;; ## Operation Resolution

(defn- resolve-operation
  [opts {:keys [alumbra/selection-set
                alumbra/operation-type
                alumbra/operation-name] :as op}]
  (let [root-type (root-type opts op)
        selection (resolve-selection-set
                    (assoc opts :scope-type root-type)
                    selection-set)]
     (cond-> {:operation-type operation-type
              :selection-set  selection}
       operation-name (assoc :operation-name operation-name))))

(defn resolve-operations
  [opts operations]
  (map #(resolve-operation opts %) operations))
