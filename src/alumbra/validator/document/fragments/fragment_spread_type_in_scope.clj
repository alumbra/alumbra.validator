(ns alumbra.validator.document.fragments.fragment-spread-type-in-scope
  (:require [alumbra.validator.document.selection-set :as selection-set]
            [alumbra.validator.document.context
             :refer [with-fragment-context]]
            [alumbra.validator.document.state :as state]
            [alumbra.validator.document.fragments.utils :as u]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; ## Helpers

(defn- valid-inline-spread-type?
  [{:keys [type->kind]}
   {:keys [valid-fragment-spreads]}]
  (fn [_ value]
    (let [t (u/type-name value)
          kind (get type->kind t)]
      (or (not (contains? #{:type :interface :union} kind))
          (contains? valid-fragment-spreads t)))))

(defn- valid-named-spread-type?
  [{:keys [type->kind]}
   {:keys [valid-fragment-spreads]}]
  (fn [state {:keys [alumbra/fragment-name]}]
    (let [t (state/fragment-type state fragment-name)]
      (or (not t)
          (not (contains? type->kind t))
          (contains? valid-fragment-spreads t)))))

(defn- fragment-spread-invariant
  [f schema type]
  (-> (invariant/property
        :fragment/type-in-scope
        (f schema type))
      (invariant/with-error-context
        (fn [state {:keys [alumbra/fragment-name]}]
          (merge
            (some->> type
                     :type-name
                     (hash-map :alumbra/containing-type-name))
            (some->> (state/fragment-type state fragment-name)
                     (hash-map :alumbra/fragment-type-name)))))
      (with-fragment-context)))

;; ## Invariant

(defn inline-spread-invariant
  [schema type]
  (fragment-spread-invariant valid-inline-spread-type? schema type))

(defn named-spread-invariant
  [schema type]
  (fragment-spread-invariant valid-named-spread-type? schema type))
