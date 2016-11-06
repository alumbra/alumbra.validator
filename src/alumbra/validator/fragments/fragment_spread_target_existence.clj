(ns alumbra.validator.fragments.fragment-spread-target-existence
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.4.2.1)
;; ---
;; - For every `namedSpread` in the document.
;; - Let `fragment` be the target of `namedSpread`
;;   - `fragment` must be defined in the document

(defn invariant
  [_]
  (-> (invariant/on
        [(multi-path
           [:graphql/operations (walker :graphql/fragment-name)]
           [:graphql/fragments
            ALL
            (must :graphql/selection-set)
            (walker :graphql/fragment-name) ])])
      (invariant/collect-as
        :validator/known-fragments
        [:graphql/fragments ALL (must :graphql/fragment-name)])
      (invariant/each
        (invariant/property
          :validator/fragment-spread-target-existence
          (fn [{:keys [validator/known-fragments]}
               {:keys [graphql/fragment-name]}]
            (contains? known-fragments fragment-name))))))
