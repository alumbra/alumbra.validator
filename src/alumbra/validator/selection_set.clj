(ns alumbra.validator.selection-set
  (:require [alumbra.validator.selection-set
             [field :as field]
             [inline-spread :as inline-spread]
             [named-spread :as named-spread]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; ## Recursive Selection Set Traversal

(defn- make-selection-set-invariant
  [type {:keys [fields inline-spreads named-spreads]} self]
  (let [field-invariant         (field/make-invariant type fields self)
        named-spread-invariant  (named-spread/make-invariant type named-spreads self)
        inline-spread-invariant (inline-spread/make-invariant type inline-spreads self)]
    (-> (invariant/on [:alumbra/selection-set ALL])
        (invariant/each
          (invariant/bind
            (fn [_ {:keys [alumbra/field-name
                           alumbra/type-condition
                           alumbra/fragment-name] :as x}]
              (cond field-name     field-invariant
                    fragment-name  named-spread-invariant
                    type-condition inline-spread-invariant)))))))

(defn- generate-invariants
  [schema k child-invariants self]
  (for [[type-name type] (get schema k)]
    [type-name
     (make-selection-set-invariant type child-invariants self)]))

(defn- selection-set-valid?
  [schema child-invariants]
  (invariant/recursive
    [self]
    (let [mk #(generate-invariants schema % child-invariants self)
          type->invariant (->> [:types
                                :interfaces
                                :unions]
                               (mapcat mk)
                               (into {}))]
      (invariant/bind
        (fn [_ {:keys [validator/scope-type]}]
          (type->invariant scope-type))))))

;; ## Invariant

(defn- add-operation-scope-type
  [{:keys [schema-root]} {:keys [alumbra/operation-type] :as data}]
  (if-let [t (get-in schema-root [:schema-root-types operation-type])]
    (assoc data :validator/scope-type t)
    data))

(defn- add-fragment-scope-type
  [{:keys [alumbra/type-condition] :as data}]
  (if-let [t (:alumbra/type-name type-condition)]
    (assoc data :validator/scope-type t)
    data))

(defn invariant
  "Generate a recursive invariant on selection sets, enforcing the invariants
   given in `child-invariants` on:

   - `:fields`
   - `:inline-spreads`
   - `:named-spreads`

   "
  [child-invariants schema]
  (let [inv (selection-set-valid? schema child-invariants)]
    (invariant/and
      (-> (invariant/on [:alumbra/operations ALL])
          (invariant/fmap #(add-operation-scope-type schema %))
          (invariant/each inv))
      (-> (invariant/on [:alumbra/fragments ALL])
          (invariant/fmap #(add-fragment-scope-type %))
          (invariant/each inv)))))
