(ns alumbra.validator.variables.variables-are-input-types
  (:require [invariant.core :as invariant]
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
  [{:keys [graphql/type-class
           graphql/type-name
           graphql/element-type]}]
  (case type-class
    :named-type type-name
    :list-type  (read-type-name element-type)))

(defn- kind-of-type
  [{:keys [analyzer/type->kind]}
   {:keys [graphql/type]}]
  (type->kind (read-type-name type)))

;; ## Invariant

(defn invariant
  [schema]
  (-> (invariant/on [ALL])
      (invariant/each
        (-> (invariant/on [:graphql/variables ALL])
            (invariant/each
              (invariant/value
                :validator/variables-are-input-types
                (fn [variable]
                  (contains?
                    #{:input-type :scalar :enum}
                    (kind-of-type schema variable)))))))))