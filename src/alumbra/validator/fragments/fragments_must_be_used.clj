(ns alumbra.validator.fragments.fragments-must-be-used
  (:require [alumbra.validator.fragments.utils :as u]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.4.1.4)
;; ---
;; - For each `fragment` defined in the document.
;; - `fragment` must be the target of at least one spread in the document.

(def invariant
  (-> (invariant/on [:graphql/fragments ALL])
      (invariant/collect-as
        :validator/used-fragments
        (multi-path
          (u/all-fragments-in :graphql/operations)
          (u/all-fragments-in :graphql/fragments)))
      (invariant/each
        (u/with-fragment-context
          (invariant/property
            :validator/fragment-must-be-used
            (fn [{:keys [validator/used-fragments]}
                 {:keys [graphql/fragment-name]}]
              (contains? used-fragments fragment-name)))))))
