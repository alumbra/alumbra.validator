(ns alumbra.validator.errors
  (:require [invariant.core :as invariant]
            [clojure.set :refer [rename-keys]]))

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

;; ## Invariant Contexts

(defn with-field-context
  [{:keys [type-name fields]} invariant]
  (invariant/with-error-context
    invariant
    (fn [_ {:keys [alumbra/field-name]}]
      {:alumbra/field-name           field-name
       :alumbra/containing-type-name type-name
       :alumbra/valid-field-names    (set (keys fields))})))

(defn with-argument-context
  [invariant]
  (invariant/with-error-context
    invariant
    (fn [_ arg]
      (select-keys arg [:alumbra/argument-name]))))

(defn with-operation-context
  [invariant]
  (invariant/with-error-context
    invariant
    (fn [_ op]
      (select-keys op [:alumbra/operation-name
                       :alumbra/operation-type]))))

(defn with-fragment-context
  [invariant]
  (invariant/with-error-context
    invariant
    (fn [_ frag]
      (merge
        (select-keys frag [:alumbra/fragment-name])
        (some->> frag
                 :alumbra/type-condition
                 :alumbra/type-name
                 (hash-map :alumbra/fragment-type-name))))))
