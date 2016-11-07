(ns alumbra.validator.arguments
  (:require [alumbra.validator.arguments
             [argument-uniqueness :as argument-uniqueness]
             [arguments-valid :as arguments-valid]]
            [invariant.core :as invariant]))

(defn invariant
  "Generate invariant to be applied to all argument-related subtrees in a
   GraphQL query document."
  [schema]
  (invariant/and
    (arguments-valid/invariant schema)
    argument-uniqueness/invariant))
