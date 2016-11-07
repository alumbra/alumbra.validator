(ns alumbra.validator.arguments.arguments-valid
  (:require [alumbra.validator.selection-set :as selection-set]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.3.1)
;; ---
;; - For each `argument` in the document
;; - Let `argumentName` be the Name of `argument`.
;; - Let `argumentDefinition` be the argument definition provided by the parent
;;   field or definition named `argumentName`.
;; - `argumentDefinition` must exist.

(defn- field-arguments-invariant
  [{:keys [analyzer/arguments]}]
  (let [valid-argument-name? (set (keys arguments))]
    (-> (invariant/on [:graphql/arguments ALL])
        (invariant/each
          (-> (invariant/value
                :validator/argument-name-in-scope
                (comp valid-argument-name? :graphql/argument-name))
              (invariant/with-error-context
                (fn [_ {:keys [graphql/argument-name]}]
                  {:analyzer/argument-name argument-name})))))))

(defn- argument-invariant
  [{:keys [analyzer/fields]}]
  (let [field->invariant
        (->> (for [[field-name type] fields]
               [field-name (field-arguments-invariant type)])
             (into {}))]
    (invariant/bind
      (fn [_ {:keys [graphql/field-name]}]
        (field->invariant field-name)))))

(defn invariant
  [schema]
  (->> {:fields #(argument-invariant %2)}
       (selection-set/invariant schema)))
