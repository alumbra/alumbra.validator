(ns alumbra.canonical.core
  (:require [alumbra.canonical
             [fragments :refer [resolve-fragments]]
             [selection-set :refer [resolve-selection-set]]]))

;; ### Operation Processing

(defn- process-operations
  [schema fragments operations]
  (map
    (fn [{:keys [graphql/selection-set] :as op}]
      (merge
        {:graphql/canonical-selection
         (resolve-selection-set schema fragments "QueryRoot" nil selection-set)}
        (select-keys op [:graphql/operation-type
                         :graphql/operation-name])))
    operations))

;; ## Canonicalize Function

(defn canonicalize
  [schema {:keys [graphql/fragments graphql/operations]}]
  (let [fragments (resolve-fragments schema fragments)]
    (process-operations schema fragments operations)))
