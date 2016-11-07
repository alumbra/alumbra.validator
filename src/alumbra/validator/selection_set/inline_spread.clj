(ns alumbra.validator.selection-set.inline-spread
  (:require [invariant.core :as invariant]))

(defn- add-scope-type
  [{:keys [graphql/type-condition] :as data}]
  (assoc data
         :validator/scope-type
         (:graphql/type-name type-condition)))

#_(defn- with-fragment-context
  [{:keys [analyzer/containing-type-name
           analyzer/type-name]} invariant]
  (invariant/with-error-context
    invariant
    (fn [_ {:keys [graphql/field-name]}]
      {:analyzer/field-name           field-name
       :analyzer/containing-type-name containing-type-name
       :analyzer/type-name            type-name})))

(defn make-invariant
  [schema type invariant-fn self]
  (-> (invariant/on-current-value)
      (invariant/fmap add-scope-type)
      (invariant/is?
        (invariant/and
          (when invariant-fn
            (invariant-fn schema type))
          self))))
