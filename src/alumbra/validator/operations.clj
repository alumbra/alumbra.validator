(ns alumbra.validator.operations
  (:require [alumbra.validator.operations
             [lone-anonymous-operation :as lone-anonymous-operation]
             [operation-allowed :as operation-allowed]
             [operation-name-uniqueness :as operation-name-uniqueness]]
            [invariant.core :as invariant]))

(def builder
  {:operations
   [lone-anonymous-operation/invariant
    operation-name-uniqueness/invariant
    operation-allowed/invariant]})
