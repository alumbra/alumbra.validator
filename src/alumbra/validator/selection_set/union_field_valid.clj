(ns alumbra.validator.selection-set.union-field-valid
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer [ALL]]))

;; Explanatory Text (5.2.1):
;; ---
;; Because unions do not define fields, fields may not be directly selected
;; from a union‐typed selection set, with the exception of the meta‐field
;; `__typename`.

(defn- field-selection?
  [selection]
  (contains? selection :graphql/field-name))

(def ^:private allowed-field?
  (comp
    #{"__typename"}
    :graphql/field-name))

(defn invariant
  [schema selection-set-valid?]
  (-> (invariant/on
        [:graphql/selection-set ALL field-selection?])
      (invariant/as
        :validator/field-scope-type :validator/scope-type)
      (invariant/each
        (invariant/value :validator/field-name-in-scope allowed-field?))))
