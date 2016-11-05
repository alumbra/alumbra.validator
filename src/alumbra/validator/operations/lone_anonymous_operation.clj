(ns alumbra.validator.operations.lone-anonymous-operation
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer [ALL]]))


;; - Let `operations` be all operation definitions in the document.
;; - Let `anonymous` be all anonymous operation definitions in the document.
;; - If `operations` is a set of more than 1:
;;   - `anonymous` must be empty.

(def invariant
  (-> (invariant/on-current-value)
      (invariant/count-as
        :operations
        [:graphql/operations ALL])
      (invariant/count-as
        :anonymous-operations
        [:graphql/operations ALL #(not (contains? % :graphql/operation-name))])
      (invariant/is?
        (invariant/state :validator/lone-anonymous-operation
                         (fn [{:keys [operations anonymous-operations]}]
                           (or (<= operations 1)
                               (zero? anonymous-operations)))))))
