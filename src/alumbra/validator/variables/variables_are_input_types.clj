(ns alumbra.validator.variables.variables-are-input-types
  (:require [alumbra.validator.errors
             :refer [with-variable-context
                     with-operation-context]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.7.3)
;; ---
;; - For every `operation` in a `document`
;; - For every `variable` on each `operation`
;;   - Let `variableType` be the type of `variable`
;;   - While `variableType` is LIST or NON_NULL
;;     - Let `variableType` be the referenced type of `variableType`
;;   - `variableType` must be of kind SCALAR, ENUM or INPUT_OBJECT

;; ## Helpers

(defn- read-type-name
  [{:keys [alumbra/type-class
           alumbra/type-name
           alumbra/element-type]}]
  (case type-class
    :named-type type-name
    :list-type  (read-type-name element-type)))

(defn- kind-of-type
  [{:keys [type->kind]}
   {:keys [alumbra/type]}]
  (type->kind (read-type-name type)))

;; ## Invariant

(defn invariant
  [schema]
  (-> (invariant/on [:alumbra/variables ALL])
      (invariant/each
        (-> (invariant/value
              :variable/input-type
              (fn [variable]
                (contains?
                  #{:input-type :scalar :enum}
                  (kind-of-type schema variable))))
            (invariant/with-error-context
              (fn [_ {:keys [alumbra/type]}]
                {:alumbra/variable-type-name (read-type-name type)}))
            (with-variable-context)))))
