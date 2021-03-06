(ns alumbra.validator.document.operations.builder
  (:require [alumbra.validator.document.operations
             [lone-anonymous-operation :as lone-anonymous-operation]
             [operation-allowed :as operation-allowed]
             [operation-name-uniqueness :as operation-name-uniqueness]]
            [invariant.core :as invariant]))

(def builder
  {:root
   [lone-anonymous-operation/invariant
    operation-name-uniqueness/invariant]
   :operations
   [operation-allowed/invariant]})
