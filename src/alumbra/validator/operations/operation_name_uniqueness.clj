(ns alumbra.validator.operations.operation-name-uniqueness
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer [ALL]]))

;; - For each operation definition `operation` in the document
;; - Let `operationName` be the name of `operation`.
;; - If `operationName` exists
;;   - Let `operations` be all operation definitions in the document named
;;     `operationName`.
;;   - `operations` must be a set of one.

(def invariant
  (constantly
    (-> (invariant/on [ALL #(contains? % :alumbra/operation-name)])
        (invariant/unique :operation/name-unique
                          {:unique-by :alumbra/operation-name}))))
