(ns alumbra.validator.fields.leaf-field-selections
  (:require [alumbra.validator.selection-set :as selection-set]
            [invariant.core :as invariant]))

(defn- valid-subselection?
  [{:keys [analyzer/type->kind]}]
  (fn [{:keys [validator/scope-type
               graphql/selection-set]}]
    (let [kind (get type->kind scope-type ::none)]
      (or (= kind ::none)
          (if (contains? #{:type :interface :union} kind)
            (seq selection-set)
            (empty? selection-set))))))

(defn- field-invariant
  [schema]
  (invariant/value
    :validator/leaf-field-selection
    (valid-subselection? schema)))

(defn invariant
  [schema]
  (->> {:fields
        (fn [schema _] (field-invariant schema))}
       (selection-set/invariant schema)))