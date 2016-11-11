(ns alumbra.validator.errors)

;; ## Context Processing

(defmulti error-data
  (fn [name error-context]
    name)
  :default ::none)

(defmethod error-data ::none
  [_ error-context]
  error-context)

(defmethod error-data :argument/name-unique
  [_ error-context]
  (rename-keys
    error-context
    {:invariant/duplicate-value :alumbra/argument-name}))

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
