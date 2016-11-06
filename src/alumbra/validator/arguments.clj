(ns alumbra.validator.arguments
  (:require [alumbra.validator.arguments
             [argument-uniqueness :as argument-uniqueness]]
            [invariant.core :as invariant]))

(defn invariant
  "Generate invariant to be applied to all argument-related subtrees in a
   GraphQL query document."
  [schema]
  (invariant/and
    argument-uniqueness/invariant))
