(ns alumbra.validator.document.operations.lone-anonymous-operation
  (:require [alumbra.validator.document.state :as state]
            [invariant.core :as invariant]
            [com.rpl.specter :refer [ALL]]))

;; Formal Specification (5.1.2.1)
;; ---
;; - Let `operations` be all operation definitions in the document.
;; - Let `anonymous` be all anonymous operation definitions in the document.
;; - If `operations` is a set of more than 1:
;;   - `anonymous` must be empty.

(def invariant
  (constantly
    (-> (invariant/on
          [:alumbra/operations
           ALL
           #(not (contains? % :alumbra/operation-name))])
        (invariant/each
          (invariant/state
            :operation/lone-anonymous
            #(= (state/operation-count %) 1))))))
