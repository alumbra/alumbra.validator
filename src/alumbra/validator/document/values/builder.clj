(ns alumbra.validator.document.values.builder
  (:require [alumbra.validator.document.values
             [input-object-field-uniqueness :as input-object-field-uniqueness]]))

(def builder
  {:fields
   [input-object-field-uniqueness/invariant]})
