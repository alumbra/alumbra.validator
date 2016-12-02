(ns alumbra.validator.document.variables.variable-default-values
  (:require [alumbra.validator.document.variables.state :as state]
            [alumbra.validator.document
             [context :as context]
             [types :as types]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; ## Helpers

(defn- read-type-description
  [{:keys [variables/types current-operation]} {:keys [alumbra/variable-name]}]
  (get-in types [current-operation variable-name]))

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
                  (->> (read-type-description state variable)
                       (make-invariant type-constructor))))))))))
