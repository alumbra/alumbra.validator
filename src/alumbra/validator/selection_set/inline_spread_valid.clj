(ns alumbra.validator.selection-set.inline-spread-valid
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer [ALL]]))

;; ## Helpers

(defn- inline-spread-selection?
  [selection]
  (contains? selection :graphql/type-condition))

(defn- add-scope-type
  [{:keys [graphql/type-condition] :as data}]
  (assoc data
         :validator/scope-type
         (:graphql/type-name type-condition)))

;; ## Invariant

(defn invariant
  [schema {:keys [analyzer/implements]} selection-set-valid?]
  (-> (invariant/on
        [:graphql/selection-set ALL inline-spread-selection?])
      (invariant/fmap add-scope-type)
      (invariant/each
        (invariant/and
          ;; TODO: fragment type must exist
          ;; TODO: fragment type must either be an interface the scope type
          ;;       implements or a union containing it (or the scope type
          ;;       itself).
          selection-set-valid?))))
