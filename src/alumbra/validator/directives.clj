(ns alumbra.validator.directives
  (:require [alumbra.validator.directives
             [directives-defined :as directives-defined]]
            [invariant.core :as invariant]))

(defn invariant
  [schema]
  (invariant/and
    (directives-defined/invariant schema)))
