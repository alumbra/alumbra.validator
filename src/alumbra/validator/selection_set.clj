(ns alumbra.validator.selection-set
  (:require [alumbra.validator.selection-set
             [field :as field]
             [inline-spread :as inline-spread]
             [named-spread :as named-spread]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; ## Recursive Selection Set Traversal

(defn- make-selection-set-invariant
  [schema type {:keys [fields inline-spreads named-spreads]} self]
  (let [field-invariant         (field/make-invariant schema type fields self)
        named-spread-invariant  (named-spread/make-invariant schema type named-spreads self)
        inline-spread-invariant (inline-spread/make-invariant schema type inline-spreads self)]
    (-> (invariant/on [:graphql/selection-set ALL])
        (invariant/each
          (invariant/bind
            (fn [_ {:keys [graphql/field-name
                           graphql/type-condition
                           graphql/fragment-name] :as x}]
              (cond field-name     field-invariant
                    fragment-name  named-spread-invariant
                    type-condition inline-spread-invariant)))))))

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

(defn merged-invariant
  [schema child-invariants]
  (let [child-invariants (apply merge-with
                                (fn [a b]
                                  #(invariant/and
                                     (a %1 %2)
                                     (b %1 %2)))
                                child-invariants)]
    (invariant schema child-invariants)))
