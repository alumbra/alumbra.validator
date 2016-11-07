(ns alumbra.validator.selection-set.field-valid
  (:require [alumbra.validator.arguments
             [arguments-valid :as arguments-valid]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer [ALL]]))

;; Formal Specification (5.2.1)
;; ---
;; - For each `selection` in the document.
;; - Let `fieldName` be the target field of `selection`
;;   - `fieldName` must be defined on type in scope

;; Formal Specification (5.2.3)
;; ---
;; - For each `selection` in the document
;; - Let `selectionType` be the result type of selection
;;   - If `selectionType` is a scalar:
;;     - The subselection set of that `selection` must be empty
;;   - If `selectionType` is an interface, union, or object
;;     - The subselection set of that `selection` must NOT BE empty

;; ## Helper

(defn- field-selection?
  [selection]
  (contains? selection :graphql/field-name))

(defn- add-scope-type
  [{:keys [analyzer/fields]} {:keys [graphql/field-name] :as data}]
  (if-let [t (get-in fields [field-name :analyzer/type-name])]
    (assoc data :validator/scope-type t)
    data))

(defn- valid-field-name?
  [{:keys [analyzer/fields]}]
  (comp (into #{"__typename"} (keys fields))
        :graphql/field-name))

(defn- valid-subselection?
  [{:keys [analyzer/type->kind]}]
  (fn [{:keys [validator/scope-type
               graphql/selection-set]}]
    (let [kind (get type->kind scope-type ::none)]
      (or (= kind ::none)
          (if (contains? #{:type :interface :union} kind)
            (seq selection-set)
            (empty? selection-set))))))

(defn- with-field-context
  [{:keys [analyzer/type-name]} & invariants]
  (invariant/with-error-context
    (apply invariant/and invariants)
    (fn [_ {:keys [graphql/field-name]}]
      {:analyzer/field-name           field-name
       :analyzer/containing-type-name type-name})))

;; ## Fields

(defn invariant
  [schema field selection-set-valid?]
  (let [allowed-field? (valid-field-name? field)
        allowed-subselection? (valid-subselection? schema)]
    (-> (invariant/on [:graphql/selection-set ALL field-selection?])
        (invariant/fmap #(add-scope-type field %))
        (invariant/each
          (invariant/and
            (with-field-context field
              (invariant/value :validator/field-name-in-scope allowed-field?)
              (invariant/value :validator/leaf-field-selection allowed-subselection?)
              (arguments-valid/invariant field))
            selection-set-valid?)))))
