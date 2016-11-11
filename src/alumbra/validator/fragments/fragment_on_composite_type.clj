(ns alumbra.validator.fragments.fragment-on-composite-type
  (:require [alumbra.validator.fragments.utils :as u]
            [alumbra.validator.errors
             :refer [with-fragment-context]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.4.1.3)
;; ---
;; - For each `fragment` defined in the document.
;;   - The target type of ``fragment must have kind UNION, INTERFACE, or OBJECT.

(defn make-invariant
  [{:keys [type->kind]}]
  (with-fragment-context
    (invariant/value
      :fragment/on-composite-type
      (fn [fragment]
        (let [t    (u/type-name fragment)
              kind (get type->kind t ::none)]
          (contains? #{::none :union :interface :type} kind))))))

(defn inline-spread-invariant
  [schema _]
  (make-invariant schema))

(defn invariant
  [schema]
  (-> (invariant/on [ALL])
      (invariant/each
        (make-invariant schema))))
