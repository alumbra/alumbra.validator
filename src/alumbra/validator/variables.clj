(ns alumbra.validator.variables
  (:require [alumbra.validator.variables
             [variable-uniqueness :as variable-uniqueness]
             [variables-are-input-types :as variables-are-input-types]]))

(def builder
  {:operations
   [variable-uniqueness/invariant
    variables-are-input-types/invariant]})
