(ns alumbra.validator.fragments.fragment-spread-type-existence
  (:require [alumbra.validator.fragments.utils :as u]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.4.1.2)
;; ---
;; - For each named spread `namedSpread` in the document
;; - Let `fragment` be the target of `namedSpread`
;;   - The target type of `fragment` must be defined in the schema

;; ## Helpers

(defn- make-invariant
  [{:keys [analyzer/type->kind]}]
  (u/with-fragment-context
    (invariant/value
      :validator/fragment-spread-type-existence
      #(contains? type->kind (u/type-name %)))))

(defn inline-spread-invariant
  [schema _]
  (make-invariant schema))

(defn invariant
  [schema]
  (-> (invariant/on [ALL])
      (invariant/each
        (make-invariant schema))))
