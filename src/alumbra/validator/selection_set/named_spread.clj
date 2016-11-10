(ns alumbra.validator.selection-set.named-spread
  (:require [invariant.core :as invariant]))

(defn- add-scope-type
  [{:keys [alumbra/type-condition] :as data}]
  (assoc data
         :validator/scope-type
         (:alumbra/type-name type-condition)))

(defn make-invariant
  [type invariant-fn self]
  (when invariant-fn
    (invariant-fn type)))
