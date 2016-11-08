(ns alumbra.validator.fields
  (:require [alumbra.validator.fields
             [leaf-field-selections :as leaf-field-selections]
             [field-selection-in-scope :as field-selection-in-scope]]
            [alumbra.validator.builder :as b]
            [invariant.core :as invariant]))

(def builder
  {:fields [field-selection-in-scope/invariant
            leaf-field-selections/invariant]})
