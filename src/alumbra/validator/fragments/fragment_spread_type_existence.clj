(ns alumbra.validator.fragments.fragment-spread-type-existence
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.4.1.2)
;; ---
;; - For each named spread `namedSpread` in the document
;; - Let `fragment` be the target of `namedSpread`
;;   - The target type of `fragment` must be defined in the schema

;; Formal Specification (5.4.1.3)
;; ---
;; - For each `fragment` defined in the document.
;;   - The target type of ``fragment must have kind UNION, INTERFACE, or OBJECT.

(def inline-spread?
  (walker :graphql/type-condition))

(defn invariant
  [{:keys [analyzer/known-types
           analyzer/known-composite-types]}]
  (invariant/recursive
    [self]
    (-> (invariant/on [inline-spread?])
        (invariant/each
          (invariant/and
            (invariant/value
              :validator/fragment-spread-on-composite-type
              (fn [{:keys [graphql/type-condition]}]
                (let [t (:graphql/type-name type-condition)]
                  (or (not (contains? known-types t))
                      (contains? known-composite-types t)))))
            (invariant/value
              :validator/fragment-spread-type-existence
              (fn [{:keys [graphql/type-condition]}]
                (contains?
                  known-types
                  (:graphql/type-name type-condition))))
            (-> (invariant/on [:graphql/selection-set])
                (invariant/is? self)))))))
