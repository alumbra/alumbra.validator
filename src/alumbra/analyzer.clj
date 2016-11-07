(ns alumbra.analyzer
  (:require [alumbra.analyzer
             [directives :as directives]
             [kinds :as kinds]
             [scalars :as scalars]
             [schema-root :as schema-root]
             [types :as types]
             [unions :as unions]
             [valid-fragment-spreads :as valid-fragment-spreads]
             spec]
            [clojure.spec :as s]
            [com.rpl.specter :refer :all]))

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
      (kinds/aggregate)
      (valid-fragment-spreads/aggregate)))

(s/fdef analyze
        :args (s/cat :schema :graphql/schema)
        :ret  :analyzer/schema)
