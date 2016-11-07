(ns alumbra.validator.arguments
  (:require [alumbra.validator.arguments
             [argument-uniqueness :as argument-uniqueness]
             [arguments-valid :as arguments-valid]]
            [alumbra.validator.builder :as b]
            [invariant.core :as invariant]))

(def builder
  (reify b/ValidatorBuilder
    (invariant-state [_ invariant]
      invariant)
    (for-fields [_ schema]
      [arguments-valid/field-invariant
       argument-uniqueness/field-invariant])
    (for-operations [_ schema])
    (for-fragments [_ schema])
    (for-fragment-spreads [_ schema])
    (for-inline-spreads [_ schema])))
