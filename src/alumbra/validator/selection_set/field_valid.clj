(ns alumbra.validator.selection-set.field-valid
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer [ALL]]))

;; ## Helper

(defn- field-selection?
  [selection]
  (contains? selection :graphql/field-name))

(defn- add-scope-type
  [{:keys [analyzer/fields]} {:keys [graphql/field-name] :as data}]
  (if-let [t (get-in fields [field-name :graphql/type-name])]
    (assoc data :validator/scope-type t)
    data))

(defn- valid-field-name?
  [{:keys [analyzer/fields]}]
  (comp (into #{"__typename"} (keys fields))
        :graphql/field-name))

;; ## Fields

(defn invariant
  [schema type selection-set-valid?]
  (let [allowed-field? (valid-field-name? type)]
    (-> (invariant/on
          [:graphql/selection-set ALL field-selection?])
        (invariant/as
          :validator/field-scope-type :validator/scope-type)
        (invariant/fmap
          #(add-scope-type type %))
        (invariant/each
          (invariant/and
            (invariant/value :validator/field-name-in-scope allowed-field?)
            selection-set-valid?)))))
