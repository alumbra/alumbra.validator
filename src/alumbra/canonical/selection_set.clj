(ns alumbra.canonical.selection-set
  (:require [alumbra.canonical.value :refer [resolve-value]]))

(declare resolve-selection-set)

;; ## Helpers

(defn- field-key
  [{:keys [graphql/field-alias
           graphql/field-name]}]
  (or field-alias field-name))

(defn- add-type-condition
  [{:keys [type-condition]} field]
  (if type-condition
    (assoc field :graphql/canonical-field-type-condition type-condition)
    field))

(defn- field-type-of
  [{:keys [schema scope-type]} {:keys [graphql/field-name]}]
  (let [type (or (get-in schema [:analyzer/types scope-type])
                 (get-in schema [:analyzer/interfaces scope-type]))]
    (get-in type [:analyzer/fields field-name])))

(defn- generate-nested-selection
  [{:keys [analyzer/type-description
           analyzer/type-name
           analyzer/non-null?]}
   selection]
  (if type-name
    {:graphql/canonical-field-type :object
     :graphql/non-null?            non-null?
     :graphql/canonical-selection  selection}
    {:graphql/canonical-field-type :list
     :graphql/non-null?            non-null?
     :graphql/canonical-field
     (generate-nested-selection type-description selection)}))

(defn- generate-nested-leaf
  [{:keys [analyzer/type-name
           analyzer/type-description
           analyzer/non-null?]}]
  (if type-description
    {:graphql/canonical-field-type :list
     :graphql/non-null?            non-null?
     :graphql/canonical-field
     (generate-nested-leaf type-description)}
    {:graphql/canonical-field-type :leaf
     :graphql/non-null?            non-null?}))

;; ## Field Resolution

(defn- leaf?
  [{:keys [graphql/selection-set]}]
  (not selection-set))

(defn- data-for-leaf
  [_ {:keys [analyzer/type-description]} _]
  (generate-nested-leaf type-description))

(defn- data-for-arguments
  [opts _ {:keys [graphql/arguments]}]
  (->> (for [{:keys [graphql/argument-name
                     graphql/argument-value]} arguments]
         [argument-name (resolve-value opts argument-value)])
       (into {})
       (hash-map :graphql/canonical-arguments)))

(defn- data-for-subselection
  [opts
   {:keys [analyzer/type-description
           analyzer/type-name]}
   {:keys [graphql/selection-set]}]
  (->> (resolve-selection-set
         (assoc opts
                :scope-type     type-name
                :type-condition nil)
         selection-set)
       (generate-nested-selection type-description)))

(defn- resolve-field*
  [opts field]
  (let [field-type (field-type-of opts field)]
    (merge
      (select-keys field [:analyzer/field-name])
      (data-for-arguments opts field-type field)
      (if (leaf? field)
        (data-for-leaf opts field-type field)
        (data-for-subselection opts field-type field)))))

(defn- resolve-field
  [result opts field]
  (->> (resolve-field* opts field)
       (add-type-condition opts)
       (assoc result (field-key field))))

;; ## Inline Spread Resolution
;;
;; Inline spreads are merged into the current selection set, adding a type
;; condition to each field.

(defn- resolve-inline-spread
  [result opts {:keys [graphql/type-condition graphql/selection-set]}]
  (let [fragment-type-name (:graphql/type-name type-condition)]
    (->> (resolve-selection-set
           (assoc opts
                  :scope-type     fragment-type-name
                  :type-condition fragment-type-name)
           selection-set)
         (merge result))))

;; ## Named Spread Resolution
;;
;; Named spreads are inlined directly using the preprocessed fragment selection
;; sets.

(defn- resolve-named-spread
  [result {:keys [fragments]} {:keys [graphql/fragment-name]}]
  (merge result (get fragments fragment-name)))

;; ## Selection Set Traversal

(defn resolve-selection-set
  [opts selection-set]
  (reduce
    (fn [result selection]
      (condp #(contains? %2 %1) selection
        :graphql/fragment-name  (resolve-named-spread result opts selection)
        :graphql/type-condition (resolve-inline-spread result opts selection)
        :graphql/field-name     (resolve-field result opts selection)))
    {} selection-set))
