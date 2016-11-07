(ns alumbra.validator.selection-set
  (:require [alumbra.validator.selection-set
             [field :as field]
             [inline-spread :as inline-spreads]
             [named-spread :as named-spreads]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; ## Recursive Selection Set Traversal

(defn- make-selection-set-invariant
  [schema type {:keys [fields inline-spreads named-spreads]} self]
  (->> (vector
         (field/make-invariant schema type fields self)
         (named-spreads/make-invariant schema type named-spreads self)
         (inline-spreads/make-invariant schema type inline-spreads self))
       (filter identity)
       (apply invariant/and)))

(defn- generate-invariants
  [schema k child-invariants self]
  (for [[type-name type] (get schema k)]
    [type-name
     (make-selection-set-invariant schema type child-invariants self)]))

(defn- selection-set-valid?
  [schema child-invariants]
  (invariant/recursive
    [self]
    (let [mk #(generate-invariants schema % child-invariants self)
          type->invariant (->> [:analyzer/types
                                :analyzer/interfaces
                                :analyzer/unions]
                               (mapcat mk)
                               (into {}))]
      (invariant/bind
        (fn [_ {:keys [validator/scope-type]}]
          (type->invariant scope-type))))))

;; ## Invariant

(defn- add-operation-scope-type
  [{:keys [analyzer/schema-root]} {:keys [graphql/operation-type] :as data}]
  (if-let [t (get schema-root operation-type)]
    (assoc data :validator/scope-type t)
    data))

(defn- add-fragment-scope-type
  [{:keys [graphql/type-condition] :as data}]
  (if-let [t (:graphql/type-name type-condition)]
    (assoc data :validator/scope-type t)
    data))

(defn invariant
  "Generate a recursive invariant on selection sets, enforcing the invariants
   given in `child-invariants` on:

   - `:fields`
   - `:inline-spreads`
   - `:named-spreads`

   "
  [schema child-invariants]
  (let [inv (selection-set-valid? schema child-invariants)]
    (invariant/and
      (-> (invariant/on [:graphql/operations ALL])
          (invariant/fmap #(add-operation-scope-type schema %))
          (invariant/each inv))
      (-> (invariant/on [:graphql/fragments ALL])
          (invariant/fmap #(add-fragment-scope-type %))
          (invariant/each inv)))))
