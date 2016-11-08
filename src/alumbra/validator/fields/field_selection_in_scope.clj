(ns alumbra.validator.fields.field-selection-in-scope
  (:require [alumbra.validator.selection-set :as selection-set]
            [invariant.core :as invariant]))

;; Formal Specification (5.2.1)
;; ---
;; - For each `selection` in the document.
;; - Let `fieldName` be the target field of `selection`
;;   - `fieldName` must be defined on type in scope

(defn- valid-field-name?
  [{:keys [analyzer/fields]}]
  (comp (into #{"__typename"} (keys fields))
        :graphql/field-name))

(defn invariant
  [_ field]
  (invariant/value
    :validator/field-selection-in-scope
    (valid-field-name? field)))
