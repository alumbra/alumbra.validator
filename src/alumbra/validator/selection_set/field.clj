(ns alumbra.validator.selection-set.field
  (:require [invariant.core :as invariant]))

(defn- add-scope-type
  [{:keys [fields]} {:keys [alumbra/field-name] :as data}]
  (if-let [t (get-in fields [field-name :type-name])]
    (assoc data :validator/scope-type t)
    data))

(defn- with-field-context
  [{:keys [type-name
           fields]} invariant]
  (invariant/with-error-context
    invariant
    (fn [_ {:keys [alumbra/field-name]}]
      {:field-name           field-name
       :containing-type-name type-name
       :valid-field-names    (set (keys fields))})))

(defn make-invariant
  [type invariant-fn self]
  (-> (invariant/on-current-value)
      (invariant/fmap #(add-scope-type type %))
      (invariant/is?
        (invariant/and
          (when invariant-fn
            (with-field-context type
              (invariant-fn type)))
          self))))
