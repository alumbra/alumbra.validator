(ns alumbra.validator.values
  (:require [alumbra.validator.values
             [input-object-field-uniqueness :as input-object-field-uniqueness]]))

(def builder
  {:fields
   [input-object-field-uniqueness/invariant]})
