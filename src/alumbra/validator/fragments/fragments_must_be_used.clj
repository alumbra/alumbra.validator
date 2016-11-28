(ns alumbra.validator.fragments.fragments-must-be-used
  (:require [alumbra.validator.fragments.utils :as u]
            [alumbra.validator.errors
             :refer [with-fragment-context]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.4.1.4)
;; ---
;; - For each `fragment` defined in the document.
;; - `fragment` must be the target of at least one spread in the document.

(defn state
  [invariant]
  (-> invariant
      (invariant/collect-as
        ::used-fragments
        (multi-path
          (u/all-fragment-names-in :alumbra/operations)
          (u/all-fragment-names-in :alumbra/fragments)))))

(def invariant
  (constantly
    (invariant/property
      :fragment/must-be-used
      (fn [{:keys [::used-fragments]}
           {:keys [alumbra/fragment-name]}]
        (contains? used-fragments fragment-name)))))
