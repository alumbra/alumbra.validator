(ns alumbra.validator.fragments.utils
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; ## Specter

(def all-named-fragments
  (recursive-path
    []
    p
    (cond-path
      :graphql/selection-set
      [:graphql/selection-set
       ALL
       (multi-path
         #(contains? % :graphql/fragment-name)
         p)]
      STAY)))

(def all-fragment-names
  [all-named-fragments :graphql/fragment-name])

(defn all-fragment-names-in
  [base-key]
  [base-key
   ALL
   all-fragment-names])

;; ## Invariant

(defn type-name
  "Extract the type name from a fragment spread."
  [fragment]
  (-> fragment :graphql/type-condition :graphql/type-name))

(defn with-fragment-context
  "Add fragment information to invariant error context."
  [invariant]
  (invariant/with-error-context
    invariant
    (fn [_ {:keys [graphql/fragment-name] :as fragment}]
      {:analyzer/fragment-name fragment-name
       :analyzer/type-name     (type-name fragment)})))
