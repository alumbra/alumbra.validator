(ns alumbra.canonical.selection-set)

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

;; ## Field Resolution

(defn- data-for-field
  [_ {:keys [analyzer/non-null?]} field]
  (-> field
      (select-keys [:graphql/field-name])
      (assoc :graphql/canonical-field-type :leaf
             :graphql/non-null? non-null?)))

(defn- data-for-subselection
  [opts {:keys [analyzer/type-name]} {:keys [graphql/selection-set] :as field}]
  (when selection-set
    {:graphql/canonical-selection
     (resolve-selection-set
       (assoc opts
              :scope-type     type-name
              :type-condition nil)
       selection-set)}))

(defn- resolve-field*
  [opts field]
  (let [field-type (field-type-of opts field)]
    (merge
      (data-for-field opts field-type field)
      (data-for-subselection opts field-type field))))

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
