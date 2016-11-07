(ns alumbra.validator.fragments
  (:require [alumbra.validator.fragments
             [fragments-must-be-used :as fragments-must-be-used]
             [fragment-on-composite-type :as fragment-on-composite-type]
             [fragment-name-uniqueness :as fragment-name-uniqueness]
             [fragment-spreads-acyclic :as fragment-spreads-acyclic]
             [fragment-spread-target-existence :as fragment-spread-target-existence]
             [fragment-spread-type-in-scope :as fragment-spread-type-in-scope]
             [fragment-spread-type-existence :as fragment-spread-type-existence]]
            [alumbra.validator.builder :as b]
            [invariant.core :as invariant]))

(def builder
  (reify b/ValidatorBuilder
    (invariant-state [_ invariant]
      (-> invariant
          (fragments-must-be-used/invariant-state)
          (fragment-spread-target-existence/invariant-state)))
    (for-fragments [_ schema]
      [fragment-name-uniqueness/invariant
       fragment-spreads-acyclic/invariant
       fragments-must-be-used/invariant
       fragment-spread-target-existence/invariant
       (fragment-on-composite-type/invariant schema)
       (fragment-spread-type-existence/fragment-invariant schema)])
    (for-operations [_ _]
      [fragment-spread-target-existence/invariant])
    (for-fields [_ schema])
    (for-fragment-spreads [_ schema]
      [fragment-spread-type-in-scope/named-spread-invariant])
    (for-inline-spreads [_ schema]
      [fragment-spread-type-existence/inline-spread-invariant
       fragment-spread-type-in-scope/inline-spread-invariant])))
