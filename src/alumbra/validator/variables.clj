(ns alumbra.validator.variables
  (:require [alumbra.validator.variables
             [variable-uniqueness :as variable-uniqueness]]))

(def builder
  {:operations
   [variable-uniqueness/invariant]})
