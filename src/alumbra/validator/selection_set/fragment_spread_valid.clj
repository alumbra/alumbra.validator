(ns alumbra.validator.selection-set.fragment-spread-valid
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer [ALL]]
            [clojure.set :as set]))

;; Explanatory Text (5.4.2.3.1)
;; ---
;; In the scope of an object type, the only valid object type fragment spread
;; is one that applies to the same type that is in scope.

;; Explanatory Text (5.4.2.3.2)
;; ---
;; In scope of an object type, unions or interface spreads can be used if the
;; object type implements the interface or is a member of the union.

;; Explanatory Text (5.4.2.3.3)
;; ---
;; Union or interface spreads can be used within the context of an object type
;; fragment, but only if the object type is one of the possible types of that
;; interface or union.

;; Explanatory Text (5.4.2.3.4)
;; ---
;; Union or interfaces fragments can be used within each other. As long as
;; there exists at least one object type that exists in the intersection of the
;; possible types of the scope and the spread, the spread is considered valid.

;; ## Helpers

(defn- inline-spread-selection?
  [selection]
  (contains? selection :graphql/type-condition))

(defn- named-spread-selection?
  [selection]
  (contains? selection :graphql/fragment-name))

(defn- add-scope-type
  [{:keys [graphql/type-condition] :as data}]
  (assoc data
         :validator/scope-type
         (:graphql/type-name type-condition)))

(defn- fragment-spread-type
  [spread]
  (-> spread :graphql/type-condition :graphql/type-name))

(defn- valid-inline-spread-type?
  [{:keys [analyzer/valid-fragment-spreads]}]
  (fn [value]
    (contains?
      valid-fragment-spreads
      (fragment-spread-type value))))

(defn- valid-named-spread-type?
  [{:keys [analyzer/valid-fragment-spreads]}]
  (fn [{:keys [validator/fragment-types]}
       {:keys [graphql/fragment-name]}]
    (let [t (get fragment-types fragment-name)]
      (or (not t)
          (contains? valid-fragment-spreads t)))))

;; ## Invariants

(defn inline-spread-invariant
  [type selection-set-valid?]
  (-> (invariant/on
        [:graphql/selection-set ALL inline-spread-selection?])
      (invariant/fmap add-scope-type)
      (invariant/each
        (invariant/and
          (invariant/value
            :validator/fragment-spread-type-in-scope
            (valid-inline-spread-type? type))
          selection-set-valid?))))

(defn named-spread-invariant
  [type selection-set-valid?]
  (-> (invariant/on
        [:graphql/selection-set ALL named-spread-selection?])
      (invariant/each
        (invariant/and
          (invariant/property
            :validator/fragment-spread-type-in-scope
            (valid-named-spread-type? type))
          selection-set-valid?))))

(defn invariant
  [schema type selection-set-valid?]
  (invariant/and
    (inline-spread-invariant type selection-set-valid?)
    (named-spread-invariant type selection-set-valid?)))
