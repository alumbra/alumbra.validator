(ns alumbra.validator.document.fields.builder
  (:require [alumbra.validator.document.fields
             [leaf-field-selections :as leaf-field-selections]
             [field-selection-in-scope :as field-selection-in-scope]]
            [invariant.core :as invariant]))

(def builder
  {:fields [field-selection-in-scope/invariant
            leaf-field-selections/invariant]})
