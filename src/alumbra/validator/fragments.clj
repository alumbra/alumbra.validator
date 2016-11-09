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
  {:inline-spreads
   [fragment-spread-type-existence/inline-spread-invariant
    fragment-spread-type-in-scope/inline-spread-invariant
    fragment-on-composite-type/inline-spread-invariant]
   :named-spreads
   [fragment-spread-type-in-scope/named-spread-invariant]
   :operations
   [fragment-spread-target-existence/invariant]
   :fragments
   [fragment-name-uniqueness/invariant
    fragment-spreads-acyclic/invariant
    fragments-must-be-used/invariant
    fragment-spread-target-existence/invariant
    fragment-on-composite-type/invariant
    fragment-spread-type-existence/invariant]
   :state
   (fn [invariant]
     (-> invariant
         (fragments-must-be-used/state)
         (fragment-spread-type-in-scope/state)
         (fragment-spread-target-existence/state)))})
