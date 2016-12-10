(ns alumbra.validator.document.types
  (:require [alumbra.validator.document
             [context :refer [with-variable-context]]
             [state :as state]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]
            [clojure.set :as set]))

;; ## Helper

(defn with-value-context
  [invariant]
  (invariant/with-error-context
    invariant
    (fn [_ {:keys [::expected] :as value}]
      {:alumbra/value value
       :alumbra/type-description expected})))

;; ## Scalars

(defn- scalar-invariant
  "Scalar values have to be strings, integers, floats or booleans – with
   user-defined scalar validation TBD."
  [scalar-type-name k]
  (with-value-context
    (invariant/value
      k
      (comp
        (case scalar-type-name
          "String"  #{:string}
          "ID"      #{:string :integer}
          "Int"     #{:integer}
          "Float"   #{:float :integer}
          "Boolean" #{:boolean}
          #{:string :integer :float :boolean})
        :alumbra/value-type))))

(defn- scalar-invariants
  [k {:keys [scalars]} self]
  (->> (for [t (keys scalars)]
         [t (scalar-invariant t k)])
       (into {})))

;; ## Enums

(defn- enum-invariant
  [enum-type-name k enum-values]
  (with-value-context
    (invariant/value
      k
      (fn [{:keys [alumbra/value-type alumbra/enum]}]
        (and (= value-type :enum)
             (contains? enum-values enum))))))

(defn- enum-invariants
  [k {:keys [enums]} self]
  (->> (for [[t {:keys [enum-values]}] enums]
         [t (enum-invariant t k enum-values)])
       (into {})))

;; ## Input Objects

;; ### Required Fields are given

(defn- collect-required-fields
  [{:keys [fields]}]
  (set
    (keep
      (fn [[field-name {:keys [non-null?]}]]
        (when non-null?
          field-name))
      fields)))

(defn- required-fields-invariant
  [{:keys [type-name] :as input-type}]
  (let [required-fields (collect-required-fields input-type)]
    (-> (invariant/value
          :input/required-fields-given
          (fn [{:keys [alumbra/object]}]
            (let [given-fields (set (map :alumbra/field-name object))]
              (empty? (set/difference required-fields given-fields)))))
        (invariant/with-error-context
          (fn [_ {:keys [alumbra/field-name]}]
            {:alumbra/input-type-name            type-name
             :alumbra/required-input-field-names required-fields})))))

;; ### Field is known

(defn- known-field-invariant
  [{:keys [fields type-name]}]
  (let [known-fields (set (keys fields))]
    (-> (invariant/value
          :input/field-name-in-scope
          (fn [{:keys [alumbra/field-name]}]
            (contains? known-fields field-name)))
        (invariant/with-error-context
          (fn [_ {:keys [alumbra/field-name]}]
            {:alumbra/field-name              field-name
             :alumbra/input-type-name         type-name
             :alumbra/valid-input-field-names known-fields})))))

;; ### Fields match expected Types

(defn- attach-expected-field-type
  [{:keys [fields]} {:keys [alumbra/field-name] :as field}]
  (let [expected-type (get-in fields [field-name :type-description])]
    (assoc-in field [:alumbra/value ::expected] expected-type)))

(defn- fields-invariant
  [input-type self]
  (invariant/and
    (required-fields-invariant input-type)
    (-> (invariant/on [:alumbra/object ALL])
        (invariant/each (known-field-invariant input-type))
        (invariant/fmap #(attach-expected-field-type input-type  %))
        (invariant/on [:alumbra/value])
        (invariant/each self))))

;; ### Combined Invariant

(defn- input-invariant
  [input-type k self]
  (let [fields-invariant (fields-invariant input-type self)
        failed           (with-value-context (invariant/fail k))]
    (invariant/bind
      (fn [_ {:keys [alumbra/value-type]}]
        (if (= value-type :object)
          fields-invariant
          failed)))))

(defn- input-invariants
  [k {:keys [input-types]} self]
  (->> (for [[t input-type] input-types]
         [t (input-invariant input-type k self)])
       (into {})))

;; ## Variable Invariant

(defn- matches-type?
  [{variable-non-null?    :non-null?
    variable-element-type :type-description
    variable-type-name    :type-name
    default-value         :default-value}
   {:keys [non-null? type-description type-name]}]
  (and (or (not non-null?)
           variable-non-null?
           default-value)
       (cond type-name
             (= variable-type-name type-name)

             (and type-description variable-element-type)
             (recur variable-element-type type-description)

             :else false)))

(defn- make-variable-invariant
  [k self]
  (with-value-context
    (invariant/property
      k
      (fn [state {:keys [alumbra/variable-name ::expected]}]
        (let [variable-type (state/variable-type state variable-name)]
          (or (not variable-type)
              (matches-type? variable-type expected)))))))

;; ## Compound Value Invariant

;; ### Non-Null

(defn- make-non-null-invariant
  [k self]
  (let [failed (with-value-context (invariant/fail k))]
    (invariant/bind
      (fn [_ {:keys [alumbra/value-type ::expected] :as value}]
        (if (= value-type :null)
          failed
          (-> (invariant/on-current-value)
              (invariant/fmap
                #(assoc-in % [::expected :non-null?] false))
              (invariant/is? self)))))))

;; ### List

(defn- make-list-invariant
  [k self]
  (let [failed (with-value-context (invariant/fail k))]
    (invariant/bind
      (fn [_ {:keys [alumbra/value-type ::expected] :as value}]
        (if (not= value-type :list)
          failed
          (let [{:keys [type-description]} expected]
            (-> (invariant/on [:alumbra/list ALL])
                (invariant/fmap #(assoc % ::expected type-description))
                (invariant/each self))))))))

;; ### Full Invariant

(defn invariant
  "Generate an invariant that operates on a GraphQL value, expecting
   `::expected` (an `:alumbra/type-description`) to be present. It will verify
   that the value matches the type."
  [k schema]
  (invariant/recursive
    [self]
    (let [type->invariant (merge
                            (scalar-invariants k schema self)
                            (enum-invariants k schema self)
                            (input-invariants k schema self))
          variable-invariant (make-variable-invariant k self)
          non-null-invariant (make-non-null-invariant k self)
          list-invariant     (make-list-invariant k self)]
      (invariant/bind
        (fn [_ {:keys [alumbra/value-type ::expected]}]
          (when-let [{:keys [non-null? type-description type-name]} expected]
            (if-not (= value-type :variable)
              (cond non-null?            non-null-invariant
                    (= value-type :null) nil
                    type-name            (type->invariant type-name)
                    :else                list-invariant)
              variable-invariant)))))))

(defn invariant-constructor
  "Generate a function that, for a given `:alumbra/type-description`, produces
   an invariant verifying a GraphQL value matches the type."
  ([schema]
   (invariant-constructor :value/type-correct schema))
  ([k schema]
   (let [inv (invariant k schema)]
     (fn [expected-type-description]
       (-> (invariant/on-current-value)
           (invariant/fmap #(assoc % ::expected expected-type-description))
           (invariant/is? inv))))))
