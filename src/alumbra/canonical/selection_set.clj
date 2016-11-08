(ns alumbra.canonical.selection-set)

(declare resolve-selection-set)

;; ## Helpers

(defn- field-key
  [{:keys [graphql/field-alias
           graphql/field-name]}]
  (or field-alias field-name))

(defn- add-type-condition
  [{:keys [graphql/type-name]} field]
  (if type-name
    (assoc field :graphql/canonical-field-type-condition type-name)
    field))

(defn- type-from-schema
  [schema type-name]
  (or (get-in schema [:analyzer/types type-name])
      (get-in schema [:analyzer/interfaces type-name])))

(defn- field-type-of
  [{:keys [analyzer/fields]} {:keys [graphql/field-name]}]
  (get fields field-name))

;; ## Field Resolution

(defn- data-for-field
  [schema type field]
  (let [{:keys [analyzer/non-null?]} (field-type-of type field)]
    (-> field
        (select-keys [:graphql/field-name])
        (assoc :graphql/canonical-field-type :leaf
               :graphql/non-null? non-null?))))

(defn- data-for-subselection
  [schema fragments type {:keys [graphql/selection-set] :as field}]
  (when selection-set
    (let [{:keys [analyzer/type-name]} (field-type-of type field)]
      {:graphql/canonical-selection (resolve-selection-set
                                      schema
                                      fragments
                                      type-name
                                      nil
                                      selection-set)})))

(defn- resolve-field*
  [schema fragments current-type field]
  (let [type (type-from-schema schema current-type)]
    (merge
      (data-for-field schema type field)
      (data-for-subselection schema fragments type field))))

(defn- resolve-field
  [result schema fragments current-type type-condition field]
  (->> (resolve-field* schema fragments current-type field)
       (add-type-condition type-condition)
       (assoc result (field-key field))))

;; ## Inline Spread Resolution
;;
;; Inline spreads are merged into the current selection set, adding a type
;; condition to each field.

(defn- resolve-inline-spread
  [result schema fragments {:keys [graphql/type-condition
                                   graphql/selection-set]}]
  (->> (resolve-selection-set
         schema
         fragments
         (:graphql/type-name type-condition)
         type-condition
         selection-set)
       (merge result)))

;; ## Named Spread Resolution
;;
;; Named spreads are inlined directly using the preprocessed fragment selection
;; sets.

(defn- resolve-named-spread
  [result fragments {:keys [graphql/fragment-name]}]
  (merge result (get fragments fragment-name)))

;; ## Selection Set Traversal

(defn resolve-selection-set
  [schema fragments current-type type-condition selection-set]
  (reduce
    (fn [result selection]
      (condp #(contains? %2 %1) selection
        :graphql/fragment-name
        (resolve-named-spread result fragments selection)

        :graphql/type-condition
        (resolve-inline-spread result schema fragments selection)

        :graphql/field-name
        (resolve-field
          result schema fragments current-type type-condition selection)))
    {} selection-set))
