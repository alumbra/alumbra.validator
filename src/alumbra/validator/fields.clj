(ns alumbra.validator.fields
  (:require [alumbra.validator.fields
             [leaf-field-selections :as leaf-field-selections]
             [field-selection-in-scope :as field-selection-in-scope]]
            [invariant.core :as invariant]))

(defn invariant
  [schema]
  (invariant/and
    (field-selection-in-scope/invariant schema)
    (leaf-field-selections/invariant schema)))
