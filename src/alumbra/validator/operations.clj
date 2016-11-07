(ns alumbra.validator.operations
  (:require [alumbra.validator.operations
             [lone-anonymous-operation :as lone-anonymous-operation]
             [operation-allowed :as operation-allowed]
             [operation-name-uniqueness :as operation-name-uniqueness]]
            [alumbra.validator.builder :as b]
            [invariant.core :as invariant]))

(def builder
  (reify b/ValidatorBuilder
    (invariant-state [_ invariant]
      invariant)
    (for-operations [_ schema]
      [lone-anonymous-operation/invariant
       operation-name-uniqueness/invariant
       (operation-allowed/invariant schema)])
    (for-fragments [_ schema])
    (for-fields [_ schema])
    (for-fragment-spreads [_ schema])
    (for-inline-spreads [_ schema])))
