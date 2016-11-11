(ns alumbra.validator.errors)

;; ## Context Processing

(defmulti error-data
  (fn [name error-context]
    name)
  :default ::none)

(defmethod error-data ::none
  [_ error-context]
  error-context)

;; ## Conversion

(defn- as-location
  [{:keys [alumbra/metadata]}]
  (some-> metadata
          (select-keys [:row :column])))

(defn- as-validation-error
  [{:keys [invariant/name
           invariant/values
           invarient/error-context]}]
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
