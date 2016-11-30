(ns alumbra.validator.document.variables.variable-uniqueness
  (:require [alumbra.validator.document.context
             :refer [with-operation-context]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.7.1)
;; ---
;; - For every `operation` in the document
;;   - For every `variable` defined on `operation`
;;     - Let `variableName` be the name of `variable`
;;     - Let `variables` be the set of all variables named `variableName`
;;       on `operation`
;;     - `variables` must be a set of one

(def invariant
  (constantly
    (-> (invariant/on [:alumbra/variables ALL])
        (invariant/unique :variable/name-unique
                          {:unique-by :alumbra/variable-name}))))
