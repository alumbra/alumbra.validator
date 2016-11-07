(ns alumbra.validator.fragments.fragment-spread-type-in-scope
  (:require [alumbra.validator.selection-set :as selection-set]
            [alumbra.validator.fragments.utils :as u]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; ## Helpers

(defn- valid-inline-spread-type?
  [{:keys [analyzer/type->kind]}
   {:keys [analyzer/valid-fragment-spreads]}]
  (fn [_ value]
    (let [t (u/type-name value)]
      (or (not (contains? type->kind t))
          (contains? valid-fragment-spreads t)))))

(defn- valid-named-spread-type?
  [{:keys [analyzer/type->kind]}
   {:keys [analyzer/valid-fragment-spreads]}]
  (fn [{:keys [::fragment-types]}
       {:keys [graphql/fragment-name]}]
    (let [t (get fragment-types fragment-name)]
      (or (not t)
          (not (contains? type->kind t))
          (contains? valid-fragment-spreads t)))))

(defn- fragment-spread-invariant
  [f schema type]
  (u/with-fragment-context
    (invariant/property
      :validator/fragment-spread-type-in-scope
      (f schema type))))

;; ## Invariant

(defn invariant-state
  [invariant]
  (invariant/as
    invariant
    ::fragment-types
    [:graphql/fragments
     ALL
     (collect-one :graphql/fragment-name)
     :graphql/type-condition
     (must :graphql/type-name)]
    conj {}))

(defn inline-spread-invariant
  [schema type]
  (fragment-spread-invariant valid-inline-spread-type? schema type))

(defn named-spread-invariant
  [schema type]
  (fragment-spread-invariant valid-named-spread-type? schema type))
