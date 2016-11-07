(ns alumbra.validator.fields.field-selection-in-scope
  (:require [alumbra.validator.selection-set :as selection-set]
            [invariant.core :as invariant]))

(defn- valid-field-name?
  [{:keys [analyzer/fields]}]
  (comp (into #{"__typename"} (keys fields))
        :graphql/field-name))

(defn- field-invariant
  [field]
  (invariant/value
    :validator/field-selection-in-scope
    (valid-field-name? field)))

(defn invariant
  [schema]
  (->> {:fields #(field-invariant %2)}
       (selection-set/invariant schema)))
