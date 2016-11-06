(ns alumbra.validator.operations
  (:require [alumbra.validator.operations
             [lone-anonymous-operation :as lone-anonymous-operation]
             [operation-allowed :as operation-allowed]
             [operation-name-uniqueness :as operation-name-uniqueness]]
            [invariant.core :as invariant]))

(defn invariant
  "Generate invariant to be applied to operations in a GraphQL query
   document."
  [schema]
  (invariant/and
    lone-anonymous-operation/invariant
    (operation-allowed/invariant schema)
    operation-name-uniqueness/invariant))
