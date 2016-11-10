(ns alumbra.validator.fragments.fragment-spread-target-existence
  (:require [alumbra.validator.fragments.utils :as u]
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
    (-> (invariant/on [ALL u/all-named-fragments])
        (invariant/each
          (u/with-fragment-context
            (invariant/property
              :validator/fragment-spread-target-existence
              (fn [{:keys [::known-fragments]}
                   {:keys [alumbra/fragment-name]}]
                (contains? known-fragments fragment-name))))))))
