(ns alumbra.validator.fragments
  (:require [alumbra.validator.fragments
             [fragment-name-uniqueness :as fragment-name-uniqueness]
             [fragment-spread-target-existence :as fragment-spread-target-existence]
             [fragment-spread-type-existence :as fragment-spread-type-existence]]
            [invariant.core :as invariant]))

(defn invariant
  "Generate invariant to be applied to all fragment-related subtrees in a
   GraphQL query document."
  [schema]
  (invariant/and
    fragment-name-uniqueness/invariant
    (fragment-spread-target-existence/invariant schema)
    (fragment-spread-type-existence/invariant schema)))
