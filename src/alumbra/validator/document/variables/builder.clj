(ns alumbra.validator.document.variables.builder
  (:require [alumbra.validator.document.variables
             [state :as state]
             [variable-default-values :as variable-default-values]
             [variable-uniqueness :as variable-uniqueness]
             [variable-usages :as variable-usages]
             [variables-are-input-types :as variables-are-input-types]]))

(def builder
  {:state
   state/state
   :fields
   [variable-usages/invariant]
   :operations
   [variable-default-values/invariant
    variable-uniqueness/invariant
    variable-usages/operation-invariant
    variables-are-input-types/invariant]})
