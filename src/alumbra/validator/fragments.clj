(ns alumbra.validator.fragments
  (:require [alumbra.validator.fragments
             [fragment-spread-type-existence :as fragment-spread-type-existence]]
            [invariant.core :as invariant]))

(defn invariant
  "Generate invariant to be applied to all fragment-related subtrees in a
   GraphQL query document."
  [schema]
  (invariant/and
    (fragment-spread-type-existence/invariant schema)))
