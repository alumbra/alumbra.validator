(ns alumbra.validator.selection-set.inline-spread
  (:require [invariant.core :as invariant]))

(defn- add-scope-type
  [{:keys [graphql/type-condition] :as data}]
  (assoc data
         :validator/scope-type
         (:graphql/type-name type-condition)))

(defn make-invariant
  [schema type invariant-fn self]
  (-> (invariant/on-current-value)
      (invariant/fmap add-scope-type)
      (invariant/is?
        (invariant/and
          (when invariant-fn
            (invariant-fn schema type))
          self))))
