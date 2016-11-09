(ns alumbra.canonical.operations
  (:require [alumbra.canonical.selection-set
             :refer [resolve-selection-set]]))

;; ## Helpers

(defn- root-type
  [{{:keys [analyzer/schema-root]} :schema}
   {:keys [graphql/operation-type]}]
  (get schema-root operation-type))

;; ## Operation Resolution

(defn- resolve-operation
  [opts {:keys [graphql/selection-set] :as op}]
  (let [root-type (root-type opts op)
        selection (resolve-selection-set
                    (assoc opts :scope-type root-type)
                    selection-set)]
    (-> op
        (select-keys [:graphql/operation-type :graphql/operation-name])
        (assoc :graphql/canonical-selection selection))))

(defn resolve-operations
  [opts operations]
  (map #(resolve-operation opts %) operations))
