(ns alumbra.validator.document.arguments.builder
  (:require [alumbra.validator.document.arguments
             [argument-uniqueness :as argument-uniqueness]
             [arguments-valid :as arguments-valid]]
            [invariant.core :as invariant]))

(def builder
  {:fields [arguments-valid/invariant
            argument-uniqueness/invariant]})
