(ns alumbra.validator.selection-set.field
  (:require [invariant.core :as invariant]))

(defn- add-scope-type
  [{:keys [analyzer/fields]} {:keys [graphql/field-name] :as data}]
  (if-let [t (get-in fields [field-name :analyzer/type-name])]
    (assoc data :validator/scope-type t)
    data))

(defn- with-field-context
  [{:keys [analyzer/type-name
           analyzer/fields]} invariant]
  (invariant/with-error-context
    invariant
    (fn [_ {:keys [graphql/field-name]}]
      {:analyzer/field-name           field-name
       :analyzer/containing-type-name type-name
       :analyzer/valid-field-names    (into #{"__typename"} (keys fields))})))

(defn make-invariant
  [schema type invariant-fn self]
  (-> (invariant/on-current-value)
      (invariant/fmap #(add-scope-type type %))
      (invariant/is?
        (invariant/and
          (when invariant-fn
            (with-field-context type
              (invariant-fn schema type)))
          self))))
