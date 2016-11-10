(ns alumbra.validator.operations.operation-allowed
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; [NOT IN SPEC]
;;
;; All `operations` need to have type (i.e. "query", "mutation", ...) for which
;; there is a mapping in the schema's `schema { ... }` block.

(defn invariant
  [{:keys [schema-root]}]
  (let [allowed-type? (set (keys schema-root))]
    (-> (invariant/on [ALL])
        (invariant/each
          (invariant/value
            :validator/operation-allowed
            (comp allowed-type? :alumbra/operation-type))))))
