(ns alumbra.validator.fragments.fragment-spread-type-existence
  (:require [alumbra.validator.fragments
             [fragment-on-composite-type :as fragment-on-composite-type]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.4.1.2)
;; ---
;; - For each named spread `namedSpread` in the document
;; - Let `fragment` be the target of `namedSpread`
;;   - The target type of `fragment` must be defined in the schema

;; ## Helpers

(def inline-spread?
  (walker :graphql/type-condition))

(defn- type-name
  [fragment]
  (-> fragment :graphql/type-condition :graphql/type-name))

(defn- with-fragment-context
  [& invariants]
  (invariant/with-error-context
    (apply invariant/and invariants)
    (fn [_ {:keys [graphql/fragment-name] :as fragment}]
      {:analyzer/fragment-name fragment-name
       :analyzer/type-name     (type-name fragment)})))

;; ## Invariant

(defn- fragment-spread-type-exists
  [{:keys [analyzer/type->kind]}]
  (invariant/value
    :validator/fragment-spread-type-existence
    #(contains? type->kind (type-name %))))

(defn invariant
  [schema]
  (invariant/recursive
    [self]
    (-> (invariant/on [inline-spread?])
        (invariant/each
          (invariant/and
            (with-fragment-context
              (fragment-spread-type-exists schema)
              (fragment-on-composite-type/invariant schema))
            (-> (invariant/on [:graphql/selection-set])
                (invariant/is? self)))))))
