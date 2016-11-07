(ns alumbra.validator.fragments.fragment-on-composite-type
  (:require [invariant.core :as invariant]))

;; Formal Specification (5.4.1.3)
;; ---
;; - For each `fragment` defined in the document.
;;   - The target type of ``fragment must have kind UNION, INTERFACE, or OBJECT.

(defn- type-name
  [fragment]
  (-> fragment :graphql/type-condition :graphql/type-name))

(defn invariant
  [{:keys [analyzer/type->kind]}]
  (invariant/value
    :validator/fragment-spread-on-composite-type
    (fn [fragment]
      (let [t    (type-name fragment)
            kind (get type->kind t ::none)]
        (contains? #{::none :union :interface :type} kind)))))
