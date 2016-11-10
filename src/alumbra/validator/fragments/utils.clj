(ns alumbra.validator.fragments.utils
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; ## Specter

(def all-named-fragments
  (recursive-path
    []
    p
    (cond-path
      :alumbra/selection-set
      [:alumbra/selection-set
       ALL
       (multi-path
         #(contains? % :alumbra/fragment-name)
         p)]
      STAY)))

(def all-fragment-names
  [all-named-fragments :alumbra/fragment-name])

(defn all-fragment-names-in
  [base-key]
  [base-key
   ALL
   all-fragment-names])

;; ## Invariant

(defn type-name
  "Extract the type name from a fragment spread."
  [fragment]
  (-> fragment :alumbra/type-condition :alumbra/type-name))

(defn with-fragment-context
  "Add fragment information to invariant error context."
  [invariant]
  (invariant/with-error-context
    invariant
    (fn [_ {:keys [alumbra/fragment-name] :as fragment}]
      {:fragment-name fragment-name
       :type-name     (type-name fragment)})))
