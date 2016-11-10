(ns alumbra.analyzer
  (:require [alumbra.analyzer
             [directives :as directives]
             [kinds :as kinds]
             [scalars :as scalars]
             [schema-root :as schema-root]
             [types :as types]
             [unions :as unions]
             [valid-fragment-spreads :as valid-fragment-spreads]]
            [alumbra.parser :as ql]
            [clojure.java.io :as io]
            [com.rpl.specter :refer :all]))

;; ## Analyzer

(defn- analyze*
  "Analyze a GraphQL schema conforming to `:alumbra/schema` to produce a
   more compact representation conforming to `:alumbra/analyzed-schema`."
  [schema]
  (-> (merge
        (directives/analyze schema)
        (scalars/analyze schema)
        (schema-root/analyze schema)
        (types/analyze schema)
        (unions/analyze schema))
      (kinds/aggregate)
      (valid-fragment-spreads/aggregate)))

;; ## Introspection Schema/Operations

(def ^:private introspection-schema
  (-> (io/resource "alumbra/GraphQLIntrospection.graphql")
      (io/input-stream)
      (ql/parse-schema)
      (analyze*)))

(def ^:private introspection-query-fields
  (-> "type __Introspection {
         __schema: __Schema!
         __type(name: String!): __Type
       }"
      (ql/parse-schema)
      (analyze*)
      (get-in [:types "__Introspection" :fields])
      (dissoc "__typename")))

(defn- ^:private introspection-query-fields-for
  [type-name]
  (-> introspection-query-fields
      (assoc-in ["__schema" :containing-type-name] type-name)
      (assoc-in ["__type" :containing-type-name] type-name)))

(defn- add-introspection-queries
  [{:keys [analyzer/schema-root
           analyzer/types]
    :as schema}]
  (if-let [root-type (get schema-root "query")]
    (if (contains? types root-type)
      (->> (introspection-query-fields-for root-type)
           (update-in schema [:types root-type :fields] merge))
      schema)
    schema))

;; ## Analyzer + Introspection

(defn analyze-schema
  "Analyze a schema AST conforming to `:graphql/schema`."
  [schema]
  (->> (analyze* schema)
       (merge-with into introspection-schema)
       (add-introspection-queries)))

(defn analyze-schema-string
  "Uses alumbra's parser to generate the schema's AST before passing it to
   [[analyze-schema]]."
  [schema-string]
  (let [schema (ql/parse-schema schema-string)]
    (when (ql/error? schema)
      (throw schema))
    (analyze-schema schema)))
