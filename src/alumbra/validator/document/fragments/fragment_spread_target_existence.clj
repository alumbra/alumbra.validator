(ns alumbra.validator.document.fragments.fragment-spread-target-existence
  (:require [alumbra.validator.document.context
             :refer [with-fragment-context]]
            [alumbra.validator.document.state :as state]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.4.2.1)
;; ---
;; - For every `namedSpread` in the document.
;; - Let `fragment` be the target of `namedSpread`
;;   - `fragment` must be defined in the document

(def invariant
  (constantly
    (with-fragment-context
      (invariant/property
        :fragment/target-exists
        (fn [state {:keys [alumbra/fragment-name]}]
          (state/fragment-known? state fragment-name))))))
