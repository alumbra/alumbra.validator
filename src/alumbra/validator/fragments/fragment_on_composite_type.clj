(ns alumbra.validator.fragments.fragment-on-composite-type
  (:require [alumbra.validator.fragments.utils :as u]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.4.1.3)
;; ---
;; - For each `fragment` defined in the document.
;;   - The target type of ``fragment must have kind UNION, INTERFACE, or OBJECT.

(defn invariant
  [{:keys [analyzer/type->kind]}]
  (u/fragment-spread-invariant
    (invariant/value
      :validator/fragment-on-composite-type
      (fn [fragment]
        (let [t    (u/type-name fragment)
              kind (get type->kind t ::none)]
          (contains? #{::none :union :interface :type} kind))))))