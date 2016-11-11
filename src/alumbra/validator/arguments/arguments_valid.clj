(ns alumbra.validator.arguments.arguments-valid
  (:require [alumbra.validator
             [errors :refer [with-argument-context]]
             [selection-set :as selection-set]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.3.1)
;; ---
;; - For each `argument` in the document
;; - Let `argumentName` be the Name of `argument`.
;; - Let `argumentDefinition` be the argument definition provided by the parent
;;   field or definition named `argumentName`.
;; - `argumentDefinition` must exist.

;; Formal Specification (5.3.3.2)
;; ---
;; - For each Field or Directive in the document.
;; - Let `arguments` be the arguments provided by the Field or Directive.
;; - Let `argumentDefinitions` be the set of argument definitions of that
;;   Field or Directive.
;; - For each `definition` in argumentDefinitions:
;;   - Let `type` be the expected type of `definition`.
;;   - If `type` is Non‐Null:
;;     - Let `argumentName` be the name of `definition`.
;;     - Let `argument` be the argument in arguments named `argumentName`
;;     - `argument` must exist.
;;     - Let `value` be the value of `argument`.
;;     - `value` must not be the null literal.

;; ## Predicates

(defn- valid-argument-name?
  [{:keys [arguments]}]
  (comp (set (keys arguments)) :alumbra/argument-name))

(defn- collect-required-arguments
  [{:keys [arguments]}]
  (->> (vals arguments)
       (filter :non-null?)
       (map :argument-name)
       (set)))

(defn- argument-nullable?
  [field]
  (let [required-argument? (collect-required-arguments field)]
    (fn [{:keys [alumbra/argument-name
                 alumbra/argument-value]}]
      (or (not (required-argument? argument-name))
          (not= (:alumbra/value-type argument-value) :null)))))

;; ## Argument Invariants

(defn- required-arguments-invariant
  [field]
  (let [required-arguments (collect-required-arguments field)
        find-missing (fn [{:keys [alumbra/arguments]}]
                       (->> (map :alumbra/argument-name arguments)
                            (reduce disj required-arguments)))]
    (invariant/with-error-context
      (invariant/value
        :argument/required-given
        (comp empty? find-missing))
      (fn [_ value]
        {:alumbra/required-argument-names
         (find-missing value)}))))

(defn- argument-name-in-scope-invariant
  [field]
  (invariant/value
    :argument/name-in-scope
    (valid-argument-name? field)))

(defn- argument-nullable-invariant
  [field]
  (invariant/value
    :validator/argument-nullable
    (argument-nullable? field)))

(defn- field-arguments-invariant
  [field]
  (-> (invariant/on-current-value)
      (invariant/is?
        (invariant/and
          (required-arguments-invariant field)
          (-> (invariant/on [:alumbra/arguments ALL])
              (invariant/each
                (with-argument-context
                  (invariant/and
                    (argument-name-in-scope-invariant field)
                    (argument-nullable-invariant field)))))))))

;; ## Combined Invariant

(defn invariant
  [_ {:keys [fields]}]
  (let [field->invariant
        (->> (for [[field-name type] fields]
               [field-name (field-arguments-invariant type)])
             (into {}))]
    (invariant/bind
      (fn [_ {:keys [alumbra/field-name]}]
        (field->invariant field-name)))))
