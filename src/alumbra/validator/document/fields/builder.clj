(ns alumbra.validator.document.fields.builder
  (:require [alumbra.validator.document.fields
             [leaf-field-selections :as leaf-field-selections]
             [field-selection-merging :as field-selection-merging]
             [field-selection-in-scope :as field-selection-in-scope]]
            [invariant.core :as invariant]))

(def builder
  {:fields [field-selection-in-scope/invariant
            leaf-field-selections/invariant]
   :root   [field-selection-merging/invariant]})
