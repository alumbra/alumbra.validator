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
    (-> (invariant/as ::operation-name
                      (comp :alumbra/operation-name first))
        (invariant/on [:alumbra/variables ALL])
        (invariant/each variable-used?))))

;; ### Fields

(def ^:private operation-variable-defined?
  (invariant/property
    :variable/exists
    (fn [{:keys [root-operation-name ::usages]}
         {:keys [alumbra/variable-name]}]
      (let [provided (get-in usages [:operations
                                     (first root-operation-name)
                                     :provided-variables])]
        (contains? provided variable-name)))))

(defn- non-providing-operations
  [{:keys [root-fragment-name ::usages]}
   {:keys [alumbra/variable-name]}]
  (get-in usages
          [:fragments
           (first root-fragment-name)
           :unprovided-variables
           variable-name]
          #{}))

(defn- fragment-variable-defined?*
  [state variable]
  (empty? (non-providing-operations state variable)))

(def ^:private fragment-variable-defined?
  (-> (invariant/property :variable/exists fragment-variable-defined?*)
      (invariant/with-error-context
        (fn [state variable]
          {:alumbra/operation-names
           (non-providing-operations state variable)}))))

(def ^:private variable-defined?
  (with-variable-context
    (invariant/bind
      (fn [state _]
        (cond (contains? state :root-operation-name)
              operation-variable-defined?
              (contains? state :root-fragment-name)
              fragment-variable-defined?
              :else nil)))))

(def ^:private argument-variable-path
  [(must :alumbra/arguments)
   ALL
   (must :alumbra/argument-value)
   u/variable-value-path])

(def ^:private nested-variable-path
  (multi-path
    argument-variable-path
    [(must :alumbra/directives) ALL argument-variable-path]))

(defn invariant
  [_ field]
  (-> (invariant/on [nested-variable-path])
      (invariant/each variable-defined?)))
