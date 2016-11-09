(ns alumbra.canonical.value)

;; ## Helpers

(defn- resolve-object
  [continue-fn opts {:keys [graphql/object]}]
  (->> (for [{:keys [graphql/field-name
                     graphql/value]} object]
         [field-name (continue-fn opts value)])
       (into {})))

(defn- resolve-list
  [continue-fn opts {:keys [graphql/list]}]
  (mapv #(continue-fn opts %) list))

(defn- resolve-variable
  [{:keys [variables]} {{:keys [graphql/variable-name]} :graphql/variable}]
  (cond (contains? variables variable-name)
        (get variables variable-name)

        ;; TODO: check default value

        :else
        (throw
          (IllegalStateException.
            (str "variable missing: $" variable-name)))))

;; ## Resolve Value

(defn resolve-value
  [opts {:keys [graphql/value-type] :as value}]
  (case value-type
    :string   (:graphql/string value)
    :integer  (:graphql/integer value)
    :float    (:graphql/float value)
    :boolean  (:graphql/boolean value)
    :variable (resolve-variable opts value)
    :object   (resolve-object resolve-value opts value)
    :list     (resolve-list resolve-value opts value)))
