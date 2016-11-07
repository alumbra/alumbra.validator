(ns alumbra.validator.arguments
  (:require [alumbra.validator.arguments
             [argument-uniqueness :as argument-uniqueness]
             [arguments-valid :as arguments-valid]]
            [invariant.core :as invariant]))

(def selection-set-invariants
  "Invariants that should be part of the recursive traversal of the selection
   set."
  [arguments-valid/selection-set-invariant
   argument-uniqueness/selection-set-invariant])
