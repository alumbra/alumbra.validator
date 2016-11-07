(ns alumbra.validator.selection-set.named-spread
  (:require [invariant.core :as invariant]))

(defn- add-scope-type
  [{:keys [graphql/type-condition] :as data}]
  (assoc data
         :validator/scope-type
         (:graphql/type-name type-condition)))

(defn make-invariant
  [schema type invariant-fn self]
  (when invariant-fn
    (invariant-fn schema type)))
