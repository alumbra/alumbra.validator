(ns alumbra.validator.document.fragments.fragment-spread-target-existence
  (:require [alumbra.validator.document.fragments.utils :as u]
            [alumbra.validator.document.context
             :refer [with-fragment-context]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.4.2.1)
;; ---
;; - For every `namedSpread` in the document.
;; - Let `fragment` be the target of `namedSpread`
;;   - `fragment` must be defined in the document

(defn state
  [invariant]
  (invariant/collect-as
    invariant
    ::known-fragments
    [:alumbra/fragments ALL (must :alumbra/fragment-name)]))

(def invariant
  (constantly
    (-> (invariant/on [u/all-named-fragments])
        (invariant/each
          (with-fragment-context
            (invariant/property
              :fragment/target-exists
              (fn [{:keys [::known-fragments]}
                   {:keys [alumbra/fragment-name]}]
                (contains? known-fragments fragment-name))))))))
