(ns alumbra.validator.document.selection-set.field
  (:require [alumbra.validator.document.context
             :refer [with-field-context]]
            [invariant.core :as invariant]))

(defn- add-scope-type
  [{:keys [fields]} {:keys [alumbra/field-name] :as data}]
  (if-let [t (get-in fields [field-name :type-name])]
    (assoc data :validator/scope-type t)
    data))

(defn make-invariant
  [type {:keys [fields]} self]
  (-> (invariant/on-current-value)
      (invariant/fmap #(add-scope-type type %))
      (invariant/is?
        (invariant/and
          (when fields
            (with-field-context type
              (fields type)))
          self))))
