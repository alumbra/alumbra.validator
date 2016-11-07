(ns alumbra.analyzer.kinds)

(defn- aggregate-map
  [schema k kind]
  (->> (get schema k)
       (keys)
       (map #(vector % kind))
       (update schema :analyzer/type->kind into)))

(defn- aggregate-set
  [schema k kind]
  (->> (get schema k)
       (map #(vector % kind))
       (update schema :analyzer/type->kind into)))

(def ^:private known-types
  {"Boolean" :scalar
   "Float"   :scalar
   "ID"      :scalar
   "Int"     :scalar
   "String"  :scalar})

(defn aggregate
  [schema]
  (-> schema
      (assoc :analyzer/type->kind known-types)
      (aggregate-map :analyzer/types       :type)
      (aggregate-map :analyzer/interfaces  :interface)
      (aggregate-map :analyzer/unions      :union)
      (aggregate-map :analyzer/directives  :directive)
      (aggregate-map :analyzer/input-types :input-type)
      (aggregate-set :analyzer/scalars     :scalar)))
