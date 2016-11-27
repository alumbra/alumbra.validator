(ns alumbra.validator.variables.variable-usages
  (:require [alumbra.validator.variables.utils :as u]
            [alumbra.validator.errors
             :refer [with-variable-context
                     with-operation-context]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.7.4)
;; ---
;; - For each `operation` in a document
;;   - For each `variableUsage` in scope, variable must be in `operation`‘s variable list.
;;   - Let `fragments` be every `fragment` referenced by that `operation` transitively
;;   - For each `fragment` in `fragments`
;;     - For each `variableUsage` in scope of `fragment`, variable must be in
;;       `operation`‘s variable list.

;; Formal Specification (5.7.5)
;; ---
;; - For every `operation` in the document.
;; - Let `variables` be the variables defined by that `operation`
;; - Each `variable` in `variables` must be used at least once in either the
;;   operation scope itself or any fragment transitively referenced by that
;;   operation.
;; ## Invariant

;; ### State

(defn state
  [invariant]
  (invariant/as invariant ::usages (comp u/analyze-variables first)))

;; ### Operations

(def ^:private variable-used?
  (with-variable-context
    (invariant/property
      :variable/must-be-used
      (fn [{:keys [::usages ::operation-name]}
           {:keys [alumbra/variable-name]}]
        (not (contains?
               (get-in usages [:operations operation-name :unused-variables])
               variable-name))))))

(def operation-invariant
  (constantly
    (-> (invariant/on [ALL])
        (invariant/each
          (with-operation-context
            (-> (invariant/as ::operation-name
                              (comp :alumbra/operation-name first))
                (invariant/on [:alumbra/variables ALL])
                (invariant/each variable-used?)))))))
