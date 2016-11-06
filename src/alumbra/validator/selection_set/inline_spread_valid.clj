(ns alumbra.validator.selection-set.inline-spread-valid
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

(defn- add-scope-type
  [{:keys [graphql/type-condition] :as data}]
  (assoc data
         :validator/scope-type
         (:graphql/type-name type-condition)))

(defn collect-valid-spread-types
  [{:keys [analyzer/unions]}
   {:keys [analyzer/implements
           analyzer/implemented-by
           analyzer/union-types]}]
  (let [allowed-types (set (concat implements implemented-by union-types))
        allowed-union-types
        (keep
          (fn [[union-type-name union-types]]
            (when-not (empty? (set/intersection union-types allowed-types))
              union-type-name))
          unions)]
    (into allowed-types allowed-union-types)))

(defn- fragment-spread-type
  [spread]
  (-> spread :graphql/type-condition :graphql/type-name))

(defn- valid-spread-type?
  [schema type]
  (let [spread-type-valid? (set (collect-valid-spread-types schema type))]
    (fn [{:keys [validator/fragment-spread-scope-type]} value]
      (let [t (fragment-spread-type value)]
        (or (spread-type-valid? t)
            (= fragment-spread-scope-type t))))))

;; ## Invariant

(defn invariant
  [schema type selection-set-valid?]
  (-> (invariant/on
        [:graphql/selection-set ALL inline-spread-selection?])
      (invariant/as
        :validator/fragment-spread-scope-type :validator/scope-type)
      (invariant/fmap add-scope-type)
      (invariant/each
        (invariant/and
          (invariant/property
            :validator/fragment-spread-type-in-scope
            (valid-spread-type? schema type))
          selection-set-valid?))))
