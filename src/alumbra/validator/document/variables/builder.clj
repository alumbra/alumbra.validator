(ns alumbra.validator.document.variables.builder
  (:require [alumbra.validator.document.variables
             [variable-default-values :as variable-default-values]
             [variable-uniqueness :as variable-uniqueness]
             [variable-usages :as variable-usages]
             [variables-used :as variables-used]
             [variables-are-input-types :as variables-are-input-types]]))

(def builder
  {:fields
   [variable-usages/invariant]
   :operations
   [variable-default-values/invariant
    variable-uniqueness/invariant
    variables-used/invariant
    variables-are-input-types/invariant]})
