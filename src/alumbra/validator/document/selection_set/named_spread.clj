(ns alumbra.validator.document.selection-set.named-spread
  (:require [invariant.core :as invariant]))

(defn- add-scope-type
  [{:keys [alumbra/type-condition] :as data}]
  (assoc data
         :validator/scope-type
         (:alumbra/type-name type-condition)))

(defn make-invariant
  [type {:keys [named-spreads]} self]
  (when named-spreads
    (named-spreads type)))
