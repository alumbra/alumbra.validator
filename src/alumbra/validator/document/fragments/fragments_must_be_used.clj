(ns alumbra.validator.document.fragments.fragments-must-be-used
  (:require [alumbra.validator.document.fragments.utils :as u]
            [alumbra.validator.document.context
             :refer [with-fragment-context]]
            [alumbra.validator.document.state :as state]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.4.1.4)
;; ---
;; - For each `fragment` defined in the document.
;; - `fragment` must be the target of at least one spread in the document.

(def invariant
  (constantly
    (invariant/property
      :fragment/must-be-used
      (fn [state {:keys [alumbra/fragment-name]}]
        (state/fragment-used? state fragment-name)))))
