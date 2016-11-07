(ns alumbra.validator.fragments.fragment-spread-type-in-scope
  (:require [alumbra.validator.selection-set :as selection-set]
            [alumbra.validator.fragments.utils :as u]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; ## Helpers

(defn- valid-inline-spread-type?
  [{:keys [analyzer/valid-fragment-spreads]}]
  (fn [value]
    (contains? valid-fragment-spreads (u/type-name value))))

(defn- valid-named-spread-type?
  [{:keys [analyzer/valid-fragment-spreads]}]
  (fn [{:keys [::fragment-types]}
       {:keys [graphql/fragment-name]}]
    (let [t (get fragment-types fragment-name)]
      (or (not t)
          (contains? valid-fragment-spreads t)))))

(defn- fragment-spread-invariant
  [f type]
  (u/with-fragment-context
    (invariant/property
      :validator/fragment-spread-type-in-scope
      (f type))))

;; ## Invariant

(defn invariant
  [schema]
  (-> (invariant/on-current-value)
      (invariant/as
        ::fragment-types
        [:graphql/fragments
         ALL
         (collect-one :graphql/fragment-name)
         :graphql/type-condition
         (must :graphql/type-name)]
        conj {})
      (invariant/is?
        (->> {:inline-spreads
              #(fragment-spread-invariant valid-inline-spread-type?  %2)
              :named-spreads
              #(fragment-spread-invariant valid-named-spread-type?  %2)}
             (selection-set/invariant schema)))))
