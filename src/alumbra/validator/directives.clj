(ns alumbra.validator.directives
  (:require [alumbra.validator.directives
             [directives-defined :as directives-defined]
             [directive-uniqueness :as directive-uniqueness]]
            [alumbra.validator.builder :as b]
            [invariant.core :as invariant]))

(let [invs [directive-uniqueness/invariant
            directives-defined/invariant]]
  (def builder
    {:fields         invs
     :inline-spreads invs
     :named-spreads  invs}))
