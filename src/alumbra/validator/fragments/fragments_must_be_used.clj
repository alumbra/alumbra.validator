(ns alumbra.validator.fragments.fragments-must-be-used
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.4.1.4)
;; ---
;; - For each `fragment` defined in the document.
;; - `fragment` must be the target of at least one spread in the document.

(def dfs
  (recursive-path
    [k]
    p
    (cond-path
      map?  (multi-path (must k) [MAP-VALS p])
      coll? [ALL p]
      STAY)))

(def invariant
  (-> (invariant/on [:graphql/fragments ALL])
      (invariant/collect-as
        :validator/used-fragments
        (multi-path
          [:graphql/operations
           ALL
           :graphql/selection-set
           ALL
           (dfs :graphql/fragment-name)]
          [:graphql/fragments
           ALL
           :graphql/selection-set
           ALL
           (dfs :graphql/fragment-name)]))
      (invariant/each
        (invariant/property
          :validator/fragment-must-be-used
          (fn [{:keys [validator/used-fragments]}
               {:keys [graphql/fragment-name]}]
            (contains? used-fragments fragment-name))))))
