(ns alumbra.validator.document.fields.field-selection-in-scope
  (:require [alumbra.validator.document.selection-set :as selection-set]
            [invariant.core :as invariant]))

;; Formal Specification (5.2.1)
;; ---
;; - For each `selection` in the document.
;; - Let `fieldName` be the target field of `selection`
;;   - `fieldName` must be defined on type in scope

(defn- valid-field-name?
  [{:keys [fields]}]
  (comp (set (keys fields)) :alumbra/field-name))

(defn invariant
  [_ field]
  (invariant/value
    :field/name-in-scope
    (valid-field-name? field)))
