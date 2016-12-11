(ns alumbra.validator.document.variables.variable-usages
  (:require [alumbra.validator.document
             [context :refer [with-variable-context]]
             [paths :as paths]
             [state :as state]]
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

;; ## Invariant

(def ^:private variable-in-operation-scope?
  (with-variable-context
    (invariant/property
      :variable/name-in-operation-scope
      (fn [state {:keys [alumbra/variable-name]}]
        (state/variable-in-scope? state variable-name)))))

(def ^:private variable-in-fragment-scope?
  (with-variable-context
    (invariant/property
      :variable/name-in-fragment-scope
      (fn [state {:keys [alumbra/variable-name]}]
        (state/variable-in-scope? state variable-name)))))

(def ^:private argument-variable-path
  [(must :alumbra/arguments)
   ALL
   (must :alumbra/argument-value)
   paths/variable-values])

(def ^:private nested-variable-path
  (multi-path
    argument-variable-path
    [(must :alumbra/directives) ALL argument-variable-path]))

(defn invariant
  [_ field]
  (-> (invariant/on [nested-variable-path])
      (invariant/each
        (invariant/bind
          (fn [state _]
            (if (state/in-operation? state)
              variable-in-operation-scope?
              variable-in-fragment-scope?))))))
