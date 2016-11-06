(ns alumbra.analyzer
  (:require [alumbra.analyzer
             [directives :as directives]
             [scalars :as scalars]
             [schema-root :as schema-root]
             [types :as types]
             [unions :as unions]
             spec]
            [clojure.spec :as s]
            [com.rpl.specter :refer :all]))

(defn aggregate
  [{:keys [analyzer/scalars] :as schema}]
  (let [composite-types (->> (for [k [:analyzer/types
                                      :analyzer/interfaces
                                      :analyzer/unions]]
                               (keys (get schema k)))
                             (apply concat)
                             (into #{}))]
    (assoc schema
           :analyzer/known-types (into scalars composite-types)
           :analyzer/known-composite-types composite-types)))

(defn analyze
  "Analyze a GraphQL schema conforming to `:graphql/schema` to produce a
   more compact representation conforming to `:analyzer/schema`."
  [schema]
  (-> (merge
        (directives/analyze schema)
        (scalars/analyze schema)
        (schema-root/analyze schema)
        (types/analyze schema)
        (unions/analyze schema))
      (aggregate)))

(s/fdef analyze
        :args (s/cat :schema :graphql/schema)
        :ret  :analyzer/schema)
