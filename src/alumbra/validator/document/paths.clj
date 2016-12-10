(ns alumbra.validator.document.paths
  (:require [com.rpl.specter :refer :all]))

(def variable-values
  "Path to find all variable references within an alumbra value."
  (recursive-path
    []
    p
    (cond-path
      (comp #{:variable} :alumbra/value-type)
      STAY

      (comp #{:list} :alumbra/value-type)
      [(must :alumbra/list) ALL p]

      (comp #{:object} :alumbra/value-type)
      [(must :alumbra/object) ALL (must :alumbra/value) p])))
