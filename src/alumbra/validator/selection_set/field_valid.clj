(ns alumbra.validator.selection-set.field-valid
  (:require [invariant.core :as invariant]
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
  (if-let [t (get-in fields [field-name :graphql/type-name])]
    (assoc data :validator/scope-type t)
    data))

(defn- valid-field-name?
  [{:keys [analyzer/fields]}]
  (comp (into #{"__typename"} (keys fields))
        :graphql/field-name))

(defn- valid-subselection?
  [{:keys [analyzer/known-composite-types]}]
  (fn [{:keys [validator/scope-type
               graphql/selection-set]}]
    (if (contains? known-composite-types scope-type)
      (seq selection-set)
      (empty? selection-set))))

;; ## Fields

(defn invariant
  [schema type selection-set-valid?]
  (let [allowed-field? (valid-field-name? type)
        allowed-subselection? (valid-subselection? schema)]
    (-> (invariant/on
          [:graphql/selection-set ALL field-selection?])
        (invariant/as
          :validator/field-scope-type :validator/scope-type)
        (invariant/fmap
          #(add-scope-type type %))
        (invariant/each
          (invariant/and
            (invariant/value
              :validator/field-name-in-scope
              allowed-field?)
            (invariant/value
              :validator/leaf-field-selection
              allowed-subselection?)
            selection-set-valid?)))))
