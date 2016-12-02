(ns alumbra.validator.document.selection-set.inline-spread
  (:require [invariant.core :as invariant]))

(defn- add-scope-type
  [{:keys [alumbra/type-condition] :as data}]
  (assoc data
         :validator/scope-type
         (:alumbra/type-name type-condition)))

(defn make-invariant
  [type {:keys [inline-spreads]} self]
  (-> (invariant/on-current-value)
      (invariant/fmap add-scope-type)
      (invariant/is?
        (invariant/and
          (when inline-spreads
            (inline-spreads type))
          self))))
