(ns alumbra.validator.document.arguments.arguments-valid
  (:require [alumbra.validator.document
             [context :refer [with-argument-context
                              with-directive-context]]
             [types :as types]]
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
  [arguments]
  (comp (set (keys arguments)) :alumbra/argument-name))

(defn- collect-required-arguments
  [arguments]
  (->> (vals arguments)
       (filter :non-null?)
       (map :argument-name)
       (set)))

;; ## Argument Invariants

(defn- required-arguments-invariant
  [arguments]
  (let [required-arguments (collect-required-arguments arguments)
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
  [arguments]
  (invariant/value
    :argument/name-in-scope
    (valid-argument-name? arguments)))

(defn- argument-type-invariant
  [type-constructor arguments]
  (let [argument-name->invariant
        (->> (for [[argument-name {:keys [type-description]}]
                   arguments]
               [argument-name
                (-> (invariant/on [:alumbra/argument-value])
                    (invariant/is?
                      (type-constructor type-description)))])
             (into {}))]
    (invariant/bind
      (fn [_ {:keys [alumbra/argument-name]}]
        (argument-name->invariant argument-name)))))

(defn- arguments-invariant
  [type-constructor {:keys [arguments]}]
  (invariant/and
    (required-arguments-invariant arguments)
    (-> (invariant/on [:alumbra/arguments ALL])
        (invariant/each
          (with-argument-context
            (invariant/and
              (argument-name-in-scope-invariant arguments)
              (argument-type-invariant type-constructor arguments)))))))

;; ## Combined Invariant

(defn invariant
  [{:keys [directives] :as schema} {:keys [fields]}]
  (let [type-constructor
        (types/invariant-constructor schema)
        field->invariant
        (->> (for [[field-name type] fields]
               [field-name (arguments-invariant type-constructor type)])
             (into {}))
        directive->invariant
        (->> (for [[directive-name directive] directives]
               [directive-name (arguments-invariant type-constructor directive)])
             (into {}))]
    (invariant/and
      (invariant/bind
        (fn [_ {:keys [alumbra/field-name]}]
          (field->invariant field-name)))
      (-> (invariant/on [:alumbra/directives ALL])
          (invariant/each
            (with-directive-context
              (invariant/bind
                (fn [_ {:keys [alumbra/directive-name]}]
                  (directive->invariant directive-name)))))))))
