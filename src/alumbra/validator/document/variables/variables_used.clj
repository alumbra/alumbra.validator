(ns alumbra.validator.document.variables.variables-used
  (:require [alumbra.validator.document
             [context :refer [with-variable-context]]
             [state :as state]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.7.5)
;; ---
;; - For every `operation` in the document.
;; - Let `variables` be the variables defined by that `operation`
;; - Each `variable` in `variables` must be used at least once in either the
;;   operation scope itself or any fragment transitively referenced by that
;;   operation.

(def ^:private variable-used?
  (with-variable-context
    (invariant/property
      :variable/must-be-used
      (fn [state {:keys [alumbra/variable-name]}]
        (state/variable-used? state variable-name)))))

(def invariant
  (constantly
    (-> (invariant/on [:alumbra/variables ALL])
        (invariant/each variable-used?))))
