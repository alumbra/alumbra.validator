(ns alumbra.validator.document.fields.leaf-field-selections
  (:require [alumbra.validator.document.selection-set :as selection-set]
            [invariant.core :as invariant]))

;; Formal Specification (5.2.3)
;; ---
;; - For each `selection` in the document
;; - Let `selectionType` be the result type of selection
;;   - If `selectionType` is a scalar:
;;     - The subselection set of that `selection` must be empty
;;   - If `selectionType` is an interface, union, or object
;;     - The subselection set of that `selection` must NOT BE empty

(defn- valid-subselection?
  [{:keys [type->kind]}]
  (fn [{:keys [validator/scope-type
               alumbra/selection-set]}]
    (let [kind (get type->kind scope-type ::none)]
      (or (= kind ::none)
          (if (contains? #{:type :interface :union} kind)
            (seq selection-set)
            (empty? selection-set))))))

(defn invariant
  [schema _]
  (invariant/value
    :field/leaf-selection
    (valid-subselection? schema)))
