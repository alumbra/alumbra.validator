(ns alumbra.validator.selection-set.field-valid
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer [ALL]]))

;; ## Helper

(defn- add-scope-type
  [lookup-table by data]
  (if-let [t (lookup-table (get data by))]
    (assoc data :validator/scope-type t)
    data))

;; ## Fields

(defn- field-selection?
  [selection]
  (contains? selection :graphql/field-name))

(defn invariant
  [schema {:keys [analyzer/fields]} selection-set-valid?]
  (let [allowed-field? (comp (set (keys fields)) :graphql/field-name)
        field->scope   (comp :graphql/type-name fields)]
    (-> (invariant/on
          [:graphql/selection-set ALL field-selection?])
        (invariant/as
          :validator/field-scope-type :validator/scope-type)
        (invariant/fmap
          #(add-scope-type field->scope :graphql/field-name %))
        (invariant/each
          (invariant/and
            (invariant/value :validator/field-name-in-scope allowed-field?)
            selection-set-valid?)))))
