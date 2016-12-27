(ns alumbra.validator.document.fields.field-selection-merging
  (:require [alumbra.validator.document.state :as state]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; ## Expansion
;;
;; We'll flatten all selection sets by inlining fragments/inline fragments
;; and attaching the parent type information to each field.

;; ### Selection Set

(declare expand-selection-set)

(defn- read-type
  [{:keys [type->kind] :as schema} type-name]
  (case (type->kind type-name)
    :type      (get-in schema [:types type-name])
    :interface (get-in schema [:interfaces type-name])
    :union     (get-in schema [:unions type-name])
    nil))

(defn- expand-field-selection-set
  [name->fragment
   schema
   {:keys [type-name fields] :as parent-type}
   {:keys [alumbra/field-name alumbra/selection-set] :as selection}]
  (when-let [field-type (get fields field-name)]
    (-> (if (seq selection-set)
          (->> (expand-selection-set
                 name->fragment
                 schema
                 (:type-name field-type)
                 selection-set)
               (assoc selection :alumbra/selection-set))
          selection)
        (assoc ::field-type
               (:type-description field-type)
               ::parent-type
               type-name
               ::parent-type-object?
               (= (get-in schema [:type->kind type-name]) :type))
        (vector))))

(defn- expand-fragment-selection-set
  [name->fragment
   schema
   {:keys [alumbra/selection-set alumbra/type-condition]}]
  (expand-selection-set
    name->fragment
    schema
    (:alumbra/type-name type-condition)
    selection-set))

(defn- expand-selection-set
  [name->fragment schema parent-type selection-set]
  (when-let [full-parent-type (read-type schema parent-type)]
    (mapcat
      (fn [{:keys [alumbra/field-name
                   alumbra/type-condition
                   alumbra/fragment-name]
            :as selection}]
        (cond field-name
              (expand-field-selection-set
                name->fragment schema full-parent-type selection)

              type-condition
              (expand-fragment-selection-set
                name->fragment schema selection)

              :else
              (name->fragment fragment-name)))
      selection-set)))

;; ### Fragments

(defn- expand-fragments
  [schema state {:keys [alumbra/fragments]}]
  (let [name->fragment (->> fragments
                            (map (juxt :alumbra/fragment-name identity))
                            (into {}))]
    (reduce
      (fn [name->fragment fragment-name]
        (->> #(expand-fragment-selection-set name->fragment schema %)
             (update name->fragment fragment-name)))
      name->fragment
      (state/sorted-fragments state))))

;; ### Operations

(defn- expand-operations
  [expanded-fragments schema {:keys [alumbra/operations]}]
  (keep
    (fn [{:keys [alumbra/operation-type alumbra/selection-set]}]
      (when-let [parent-type (get-in schema [:schema-root :schema-root-types operation-type])]
        (expand-selection-set expanded-fragments schema parent-type selection-set)))
    operations))

;; ### Full Expansion

(defn- expanded-operation-selection-sets
  [schema state [document]]
  (-> (expand-fragments schema state document)
      (expand-operations schema document)))

;; ## Helpers

(defn- field-name
  [{:keys [alumbra/field-alias alumbra/field-name]}]
  (or field-alias field-name))

(defn- with-field-context
  [invariant]
  ;; TODO
  (invariant/with-error-context
    invariant
    (fn [_ [field]]
      {:alumbra/field-name (field-name field)
       :alumbra/containing-type-name "x"})))

;; ## Predicates

(defn- same-field-name?
  [[field-a field-b]]
  (= (:alumbra/field-name field-a)
     (:alumbra/field-name field-b)))

(defn- read-argument-value
  [{:keys [alumbra/value-type] :as value}]
  (case value-type
    :variable (symbol (:alumbra/variable-name value))
    :enum     (keyword (:alumbra/enum value))
    :integer (:alumbra/integer value)
    :string  (:alumbra/string value)
    :boolean (:alumbra/boolean value)
    :list    (mapv read-argument-value (:alumbra/list value))
    :object  (->> (for [{:keys [alumbra/field-name alumbra/value]}
                        (:alumbra/object value)]
                    [field-name (read-argument-value value)])
                  (into {}))))

(defn- read-arguments
  [args]
  (->> (for [{:keys [alumbra/argument-name alumbra/argument-value]} args]
         [argument-name (read-argument-value argument-value)])
       (into {})))

(defn- same-arguments?
  [[{args-a :alumbra/arguments}
    {args-b :alumbra/arguments}]]
  (= (read-arguments args-a)
     (read-arguments args-b)))

(defn- nullable-compatible?
  [{non-null-a :non-null?}
   {non-null-b :non-null?}]
  (if (or non-null-a non-null-b)
    (and non-null-a non-null-b)
    true))

(defn- types-compatible?
  [field-type-a field-type-b]
  (and (nullable-compatible? field-type-a field-type-b)
       (let [{type-description-a :type-description} field-type-a
             {type-description-b :type-description} field-type-b]
         (if (or type-description-a type-description-b)
           (and type-description-a
                type-description-b
                (recur type-description-a type-description-b))
           (= (:type-name field-type-a) (:type-name field-type-b))))))

(defn- same-response-shape?
  [[{field-type-a ::field-type}
    {field-type-b ::field-type}]]
  (types-compatible? field-type-a field-type-b))

;; ## Recursion on Field Selections

(defn- merge-selections
  [state [field-a field-b]]
  (let [merged (concat
                 (:alumbra/selection-set field-a)
                 (:alumbra/selection-set field-b))]
    (when (next merged)
      [merged])))

(defn- recur-invariant
  [self]
  (-> (invariant/on-values merge-selections)
      (invariant/is? self)))

;; ## Field Pair Comparison

(defn- field-pair-comparable?
  [state [field-a field-b]]
  (or (= (::parent-type field-a)
         (::parent-type field-b))
      (not (::parent-type-object? field-a))
      (not (::parent-type-object? field-b))))

(defn- field-pair-invariant
  [self]
  (let [sub-invariant
        (with-field-context
          (invariant/and
            (invariant/values :field/selection-mergeable same-field-name?)
            (invariant/values :field/selection-mergeable same-arguments?)))
        shape-invariant
        (with-field-context
          (invariant/values :field/selection-mergeable same-response-shape?))]
    (invariant/and
      shape-invariant
      (invariant/bind
        (fn [state pair]
          (if (field-pair-comparable? state pair)
            sub-invariant)))
      (recur-invariant self))))

;; ## Invariant on expanded Selection Sets

(defn select-pairs
  [group]
  (loop [[h & rst] group
         result []]
    (if rst
      (->> (reduce
             (fn [result v]
               (conj result [h v]))
             result rst)
           (recur rst))
      result)))

(defn fields-with-same-name
  [state [selection-set]]
  (->> (group-by field-name selection-set)
       (vals)
       (mapcat
         (fn [group]
           (if (next group)
             (select-pairs group)
             [group])))))

(def fields-in-set-can-merge?
  (invariant/recursive
    [self]
    (let [pair-invariant (field-pair-invariant self)
          solo-invariant (-> (invariant/on [FIRST :alumbra/selection-set])
                             (invariant/is? self))]
      (-> (invariant/on-values fields-with-same-name)
          (invariant/each
            (invariant/bind
              (fn [_ [_ other]]
                (if (nil? other)
                  solo-invariant
                  pair-invariant))))))))

;; ## Invariant

(defn invariant
  [schema]
  (-> (invariant/on-values
        (partial expanded-operation-selection-sets schema))
      (invariant/each fields-in-set-can-merge?)))
