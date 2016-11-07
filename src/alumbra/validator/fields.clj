(ns alumbra.validator.fields
  (:require [alumbra.validator.fields
             [leaf-field-selections :as leaf-field-selections]
             [field-selection-in-scope :as field-selection-in-scope]]
            [invariant.core :as invariant]))

(def selection-set-invariants
  [field-selection-in-scope/selection-set-invariant
   leaf-field-selections/selection-set-invariant])
