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

(defn invariant
  [{:keys [analyzer/type->kind]}]
  (u/fragment-spread-invariant
    (invariant/value
      :validator/fragment-spread-type-existence
      #(contains? type->kind (u/type-name %)))))
