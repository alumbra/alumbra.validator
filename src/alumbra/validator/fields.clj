(ns alumbra.validator.fields
  (:require [alumbra.validator.fields
             [leaf-field-selections :as leaf-field-selections]
             [field-selection-in-scope :as field-selection-in-scope]]
            [alumbra.validator.builder :as b]
            [invariant.core :as invariant]))

(def builder
  (reify b/ValidatorBuilder
    (invariant-state [_ invariant]
      invariant)
    (for-fields [_ schema]
      [field-selection-in-scope/field-invariant
       leaf-field-selections/field-invariant])
    (for-fragment-spreads [_ _])
    (for-inline-spreads [_ _])
    (for-operations [_ _])
    (for-fragments [_ _])))
