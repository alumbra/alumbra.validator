(ns alumbra.validator.arguments.arguments-valid
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

(defn- describe-error
  [field-name]
  (fn [_ {:keys [graphql/argument-name]}]
    {:field-name    field-name
     :argument-name argument-name}))

(defn- field-arguments-invariant
  [field-name {:keys [analyzer/arguments]}]
  (let [valid-argument-name? (set (keys arguments))]
    (-> (invariant/on [:graphql/arguments ALL])
        (invariant/each
          (invariant/value
            :validator/argument-name-in-scope
            (comp valid-argument-name? :graphql/argument-name)
            (describe-error field-name))))))

(defn invariant
  [{:keys [analyzer/fields]}]
  (let [field->invariant
        (->> (for [[field-name type] fields]
               [field-name (field-arguments-invariant field-name type)])
             (into {}))]
    (invariant/bind
      (fn [_ {:keys [graphql/field-name]}]
        (field->invariant field-name)))))
