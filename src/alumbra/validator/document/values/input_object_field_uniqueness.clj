(ns alumbra.validator.document.values.input-object-field-uniqueness
  (:require [alumbra.validator.document.context
             :refer [with-argument-context]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

(def fields-unique-invariant
  (invariant/recursive
    [self]
    (invariant/bind
      (fn [_ {:keys [alumbra/value-type] :as x}]
        (when (= value-type :object)
          (-> (invariant/on [:alumbra/object ALL])
              (invariant/unique :input/field-name-unique
                                {:unique-by :alumbra/field-name})
              (invariant/on [:alumbra/value])
              (invariant/each self)))))))

(def invariant
  (constantly
    (-> (invariant/on [:alumbra/arguments ALL])
        (invariant/each
          (with-argument-context
            (-> (invariant/on [:alumbra/argument-value])
                (invariant/is? fields-unique-invariant)))))))
