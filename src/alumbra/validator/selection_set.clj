(ns alumbra.validator.selection-set
  (:require [alumbra.validator.selection-set
             [field-valid :as field-valid]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; ## Recursive Selection Set Traversal

(defn- make-selection-set-invariant
  [schema type self]
  (invariant/and
    (field-valid/invariant schema type self)))

(defn- generate-invariants
  [schema k self]
  (for [[type-name type] (get schema k)]
    [type-name (make-selection-set-invariant schema type self)]))

(defn- selection-set-valid?
  [schema]
  (invariant/recursive-fn
    [self]
    (comp
      (->> (concat
             (generate-invariants schema :analyzer/types self)
             (generate-invariants schema :analyzer/interfaces self))
           (into {}))
      :validator/scope-type)))

;; ## Invariant

(defn- add-scope-type
  [{:keys [analyzer/schema-root]} {:keys [graphql/operation-type] :as data}]
  (if-let [t (get schema-root operation-type)]
    (assoc data :validator/scope-type t)
    data))

(defn invariant
  [{:keys [analyzer/schema-root] :as schema}]
  (-> (invariant/on [:graphql/operations ALL])
      (invariant/fmap #(add-scope-type schema %))
      (invariant/each (selection-set-valid? schema))))
