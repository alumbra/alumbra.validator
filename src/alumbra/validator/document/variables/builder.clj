(ns alumbra.validator.document.variables.builder
  (:require [alumbra.validator.document.variables
             [variable-uniqueness :as variable-uniqueness]
             [variable-usages :as variable-usages]
             [variables-are-input-types :as variables-are-input-types]]))

(def builder
  {:state variable-usages/state
   :fields
   [variable-usages/invariant]
   :operations
   [variable-uniqueness/invariant
    variable-usages/operation-invariant
    variables-are-input-types/invariant]})
