(ns alumbra.validator.operations.operation-allowed
  (:require [alumbra.validator.errors
             :refer [with-operation-context]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; [NOT IN SPEC]
;;
;; All `operations` need to have type (i.e. "query", "mutation", ...) for which
;; there is a mapping in the schema's `schema { ... }` block.

(defn invariant
  [{:keys [schema-root]}]
  (let [allowed-type? (set (keys (:schema-root-types schema-root)))]
    (invariant/value
      :operation/allowed
      (comp allowed-type? :alumbra/operation-type))))
