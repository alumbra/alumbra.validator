(ns alumbra.canonical.operations
  (:require [alumbra.canonical.selection-set
             :refer [resolve-selection-set]]))

(defn- root-type
  [{:keys [analyzer/schema-root]} {:keys [graphql/operation-type]}]
  (get schema-root operation-type))

(defn- resolve-operation
  [schema fragments {:keys [graphql/selection-set] :as op}]
  (let [t (root-type schema op)
        selection (resolve-selection-set schema fragments t nil selection-set)]
    (-> op
        (select-keys [:graphql/operation-type :graphql/operation-name])
        (assoc :graphql/canonical-selection selection))))

(defn resolve-operations
  [schema fragments operations]
  (map #(resolve-operation schema fragments %) operations))
