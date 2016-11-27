(ns alumbra.validator.variables
  (:require [alumbra.validator.variables
             [variable-uniqueness :as variable-uniqueness]
             [variable-usages :as variable-usages]
             [variables-are-input-types :as variables-are-input-types]]))

(def builder
  {:state variable-usages/state
   :operations
   [variable-uniqueness/invariant
    variable-usages/operation-invariant
    variables-are-input-types/invariant]})
