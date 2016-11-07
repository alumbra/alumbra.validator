(ns alumbra.validator.fragments.fragments-must-be-used
  (:require [alumbra.validator.fragments.utils :as u]
            [alumbra.validator.utils :refer [dfs]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.4.1.4)
;; ---
;; - For each `fragment` defined in the document.
;; - `fragment` must be the target of at least one spread in the document.

(defn- all-fragments-in
  [base-key]
  [base-key
   ALL
   :graphql/selection-set
   ALL
   (dfs :graphql/fragment-name)])

(def invariant
  (-> (invariant/on [:graphql/fragments ALL])
      (invariant/collect-as
        :validator/used-fragments
        (multi-path
          (all-fragments-in :graphql/operations)
          (all-fragments-in :graphql/fragments)))
      (invariant/each
        (u/with-fragment-context
          (invariant/property
            :validator/fragment-must-be-used
            (fn [{:keys [validator/used-fragments]}
                 {:keys [graphql/fragment-name]}]
              (contains? used-fragments fragment-name)))))))
