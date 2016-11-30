(ns alumbra.validator.errors
  (:require [clojure.set :refer [rename-keys]]))

;; ## Context Processing

(defmulti error-data
  (fn [name error-context]
    name)
  :default ::none)

(defmethod error-data ::none
  [_ error-context]
  error-context)

(defmethod error-data :operation/name-unique
  [_ error-context]
  (rename-keys
    error-context
    {:invariant/duplicate-value :alumbra/operation-name}))

(defmethod error-data :argument/name-unique
  [_ error-context]
  (rename-keys
    error-context
    {:invariant/duplicate-value :alumbra/argument-name}))

(defmethod error-data :fragment/name-unique
  [_ error-context]
  (rename-keys
    error-context
    {:invariant/duplicate-value :alumbra/fragment-name}))

(defmethod error-data :fragment/acyclic
  [_ error-context]
  (rename-keys
    error-context
    {:invariant/cycle :alumbra/cycle-fragment-names
     :invariant/edges :alumbra/cycle-fragment-edges}))

(defmethod error-data :directive/name-unique
  [_ error-context]
  (rename-keys
    error-context
    {:invariant/duplicate-value :alumbra/directive-name}))

(defmethod error-data :variable/name-unique
  [_ error-context]
  (rename-keys
    error-context
    {:invariant/duplicate-value :alumbra/variable-name}))

;; ## Conversion

(defn- as-location
  [{:keys [alumbra/metadata]}]
  metadata)

(defn- as-validation-error
  [{:keys [invariant/name
           invariant/values
           invariant/error-context]}]
  (merge
    {:alumbra/validation-error-class
     name
     :alumbra/locations
     (keep as-location values)}
    (error-data name error-context)))

(defn as-validation-errors
  [errors]
  (when errors
    (mapv as-validation-error errors)))
