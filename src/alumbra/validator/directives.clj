(ns alumbra.validator.directives
  (:require [alumbra.validator.directives
             [directives-defined :as directives-defined]
             [directive-uniqueness :as directive-uniqueness]]
            [invariant.core :as invariant]))

(defn invariant
  [schema]
  (invariant/and
    directive-uniqueness/invariant
    (directives-defined/invariant schema)))
