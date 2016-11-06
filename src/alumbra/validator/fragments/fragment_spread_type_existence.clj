(ns alumbra.validator.fragments.fragment-spread-type-existence
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.4.1.2)
;; ---
;; - For each named spread `namedSpread` in the document
;; - Let `fragment` be the target of `namedSpread`
;;   - The target type of `fragment` must be defined in the schema

(def inline-spread?
  (walker :graphql/type-condition))

(defn invariant
  [{:keys [analyzer/known-selection-types]}]
  (invariant/recursive
    [self]
    (-> (invariant/on [inline-spread?])
        (invariant/each
          (invariant/and
            (invariant/value
              :validator/fragment-spread-type-existence
              (fn [{:keys [graphql/type-condition]}]
                (contains?
                  known-selection-types
                  (:graphql/type-name type-condition))))
            (-> (invariant/on [:graphql/selection-set])
                (invariant/is? self)))))))
