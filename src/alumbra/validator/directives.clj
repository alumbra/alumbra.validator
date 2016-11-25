(ns alumbra.validator.directives
  (:require [alumbra.validator.directives
             [directives-defined :as directives-defined]
             [directives-in-valid-locations :as directives-in-valid-locations]
             [directive-uniqueness :as directive-uniqueness]]
            [alumbra.validator.builder :as b]
            [invariant.core :as invariant]))

(let [invs [directive-uniqueness/invariant
            directives-defined/invariant]]
  (def builder
    {:fields
     (conj invs
           (directives-in-valid-locations/invariant :field))
     :inline-spreads
     (conj invs
           (directives-in-valid-locations/invariant :inline-fragment))
     :named-spreads
     (conj invs
           (directives-in-valid-locations/invariant :fragment-spread))
     :fragments
     [directives-in-valid-locations/fragment-invariant]
     :operations
     [directives-in-valid-locations/operation-invariant]}))
