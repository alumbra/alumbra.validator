(ns alumbra.validator.selection-set
  (:require [alumbra.validator.selection-set
             [field-valid :as field-valid]
             [fragment-spread-valid :as fragment-spread-valid]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; ## Recursive Selection Set Traversal

(defn- make-selection-set-invariant
  [schema type self]
  (invariant/and
    (field-valid/invariant schema type self)
    (fragment-spread-valid/invariant schema type self)))

(defn- generate-invariants
  [schema k self]
  (for [[type-name type] (get schema k)]
    [type-name (make-selection-set-invariant schema type self)]))

(defn- selection-set-valid?
  "Recursive invariant on selection sets. Expects input data to have the field
   `:validator/scope-type`, so before calling this invariant the field should
   be attached."
  [schema]
  (invariant/recursive
    [self]
    (let [type->invariant
          (->> (concat
                 (generate-invariants schema :analyzer/types self)
                 (generate-invariants schema :analyzer/interfaces self)
                 (generate-invariants schema :analyzer/unions self))
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

(defn- add-known-fragments
  [invariant]
  (invariant/as
    invariant
    :validator/fragment-types
    [:graphql/fragments
     ALL
     (collect-one :graphql/fragment-name)
     :graphql/type-condition
     (must :graphql/type-name)]
    conj {}))

(defn invariant
  [schema]
  (let [inv (selection-set-valid? schema)]
    (-> (invariant/on-current-value)
        (add-known-fragments)
        (invariant/is?
          (invariant/and
            (-> (invariant/on [:graphql/operations ALL])
                (invariant/fmap #(add-operation-scope-type schema %))
                (invariant/each inv))
            (-> (invariant/on [:graphql/fragments ALL])
                (invariant/fmap #(add-fragment-scope-type %))
                (invariant/each inv)))))))
