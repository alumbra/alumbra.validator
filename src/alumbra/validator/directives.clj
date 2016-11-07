(ns alumbra.validator.directives
  (:require [alumbra.validator.directives
             [directives-defined :as directives-defined]
             [directive-uniqueness :as directive-uniqueness]]
            [alumbra.validator.builder :as b]
            [invariant.core :as invariant]))

(def ^:private selection-set-invariants
  [directive-uniqueness/invariant
   directives-defined/invariant])

(def builder
  (reify b/ValidatorBuilder
    (invariant-state [_ invariant]
      invariant)
    (for-fields [_ schema]
      selection-set-invariants)
    (for-fragment-spreads [_ schema]
      selection-set-invariants)
    (for-inline-spreads [_ schema]
      selection-set-invariants)
    (for-operations [_ _])
    (for-fragments [_ _])))
