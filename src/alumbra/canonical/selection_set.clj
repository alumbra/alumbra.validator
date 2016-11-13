(ns alumbra.canonical.selection-set
  (:require [alumbra.canonical
             [arguments :refer [resolve-arguments]]
             [directives :refer [resolve-directives]]]))

(declare resolve-selection-set)

;; ## Helpers

(defn- field-alias
  [{:keys [alumbra/field-alias
           alumbra/field-name]}]
  (or field-alias field-name))

(defn- field-type-of
  [{:keys [schema scope-type]} {:keys [alumbra/field-name]}]
  (let [kind (get-in schema [:type->kind scope-type])
        type (case kind
               :type      (get-in schema [:types scope-type])
               :interface (get-in schema [:interfaces scope-type])
               :union     (get-in schema [:unions scope-type]))]
    (get-in type [:fields field-name])))

(defn- generate-nested-selection
  [{:keys [type-description
           type-name
           non-null?]}
   selection]
  (if type-name
    {:field-type    :object
     :non-null?     non-null?
     :selection-set selection}
    {:field-type :list
     :non-null?  non-null?
     :field-spec (generate-nested-selection type-description selection)}))

(defn- generate-nested-leaf
  [{:keys [type-name
           type-description
           non-null?]}]
  (if type-description
    {:field-type :list
     :non-null?  non-null?
     :field-spec (generate-nested-leaf type-description)}
    {:field-type :leaf
     :type-name  type-name
     :non-null?  non-null?}))

;; ## Field Resolution

(defn- leaf?
  [{:keys [alumbra/selection-set]}]
  (not selection-set))

(defn- data-for-leaf
  [_ {:keys [type-description]} _]
  (generate-nested-leaf type-description))

(defn- data-for-arguments
  [opts _ {:keys [alumbra/arguments]}]
  (when (seq arguments)
    {:arguments (resolve-arguments opts arguments)}))

(defn- data-for-directives
  [opts _ {:keys [alumbra/directives]}]
  (when (seq directives)
    {:directives (resolve-directives opts directives)}))

(defn- data-for-subselection
  [opts
   {:keys [type-description
           type-name]}
   {:keys [alumbra/selection-set]}]
  (->> (resolve-selection-set
         (assoc opts :scope-type type-name)
         selection-set)
       (generate-nested-selection type-description)))

(defn- resolve-field*
  [opts field]
  (let [field-type (field-type-of opts field)]
    (merge
      {:field-name (:alumbra/field-name field)
       :field-alias (field-alias field)}
      (data-for-directives opts field-type field)
      (data-for-arguments opts field-type field)
      (if (leaf? field)
        (data-for-leaf opts field-type field)
        (data-for-subselection opts field-type field)))))

(defn- resolve-field
  [result opts field]
  (->> (resolve-field* opts field)
       (conj result)))

;; ## Inline Spread Resolution
;;
;; Inline spreads are merged into the current selection set, adding a type
;; condition to each field.

(defn resolve-fragment-selection-set
  [opts {:keys [alumbra/type-condition
                alumbra/selection-set
                alumbra/directives]}]
  (let [fragment-type-name (:alumbra/type-name type-condition)
        selection-set (resolve-selection-set
                        (assoc opts :scope-type fragment-type-name)
                        selection-set)]
    (cond-> {:type-condition fragment-type-name
             :selection-set  selection-set}
      (seq directives)
      (assoc :directives (resolve-directives opts directives)))))

(defn- inline-fragment-if-in-scope
  [{:keys [scope-type]} {:keys [type-condition] :as fragment-data}]
  (if (= scope-type (:type-condition fragment-data))
    (dissoc fragment-data :type-condition)
    fragment-data))

(defn- resolve-inline-spread
  [result opts inline-spread]
  (->> (resolve-fragment-selection-set opts inline-spread)
       (inline-fragment-if-in-scope opts)
       (conj result)))

;; ## Named Spread Resolution
;;
;; Named spreads are inlined directly using the preprocessed fragment selection
;; sets.

(defn- resolve-named-spread
  [result {:keys [fragments] :as opts} {:keys [alumbra/fragment-name]}]
  (->> (get fragments fragment-name)
       (inline-fragment-if-in-scope opts)
       (conj result )))

;; ## Selection Set Traversal

(defn resolve-selection-set
  [opts selection-set]
  (reduce
    (fn [result selection]
      (condp #(contains? %2 %1) selection
        :alumbra/fragment-name  (resolve-named-spread result opts selection)
        :alumbra/type-condition (resolve-inline-spread result opts selection)
        :alumbra/field-name     (resolve-field result opts selection)))
    [] selection-set))
