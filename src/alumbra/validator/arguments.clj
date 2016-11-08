(ns alumbra.validator.arguments
  (:require [alumbra.validator.arguments
             [argument-uniqueness :as argument-uniqueness]
             [arguments-valid :as arguments-valid]]
            [alumbra.validator.builder :as b]
            [invariant.core :as invariant]))

(def builder
  {:fields [arguments-valid/invariant
            argument-uniqueness/invariant]})
