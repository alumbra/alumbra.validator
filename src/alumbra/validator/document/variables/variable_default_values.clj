(ns alumbra.validator.document.variables.variable-default-values
  (:require [alumbra.validator.document
             [context :as context]
             [state :as state]
             [types :as types]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; ## Helpers

(defn- read-type-description
  [state {:keys [alumbra/variable-name]}]
  (state/variable-type state variable-name))

(defn- make-type-constructor
  [schema]
  (types/invariant-constructor :variable/default-value-correct schema))

(defn- make-invariant
  [type-constructor {:keys [non-null?] :as type-description}]
  (if non-null?
    (invariant/with-static-error-context
      (invariant/fail :variable/default-value-correct)
      {:alumbra/type-description type-description})
    (-> (invariant/on [:alumbra/default-value])
        (invariant/is?
          (type-constructor type-description)))))

;; ## Invariant

(defn invariant
  [schema]
  (let [type-constructor (make-type-constructor schema)]
    (-> (invariant/on [:alumbra/variables ALL])
        (invariant/each
          (context/with-variable-context
            (invariant/bind
              (fn [state {:keys [alumbra/default-value] :as variable}]
                (when default-value
                  (invariant/with-static-error-context
                    (->> (read-type-description state variable)
                         (make-invariant type-constructor))
                    {:alumbra/value default-value})))))))))
