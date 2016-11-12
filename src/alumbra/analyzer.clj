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
  {:pre [(not (:alumbra/parser-errors schema))]}
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
  [{:keys [schema-root types]
    :as schema}]
  (if-let [root-type (get schema-root "query")]
    (if (contains? types root-type)
      (->> (introspection-query-fields-for root-type)
           (update-in schema [:types root-type :fields] merge))
      schema)
    schema))

(defn- analyze-schema-with-introspection
  "Analyze a GraphQL schema conforming to `:alumbra/schema` to produce a
   more compact representation conforming to `:alumbra/analyzed-schema`.

   Adds the types/fields necessary for introspection."
  [schema]
  (if (:alumbra/parser-errors schema)
    schema
    (->> (analyze* schema)
         (merge-with into introspection-schema)
         (add-introspection-queries))))

;; ## Analysis

(defprotocol AnalyzeableSchema
  (analyze-schema [schema]
    "Analyze a GraphQL schema conforming to `:alumbra/schema` to produce a
     more compact representation conforming to `:alumbra/analyzed-schema`."))

(extend-protocol AnalyzeableSchema
  String
  (analyze-schema [s]
    (let [schema (ql/parse-schema s)]
      (analyze-schema schema)))

  java.io.File
  (analyze-schema [f]
    (analyze-schema (slurp f)))

  java.io.InputStream
  (analyze-schema [in]
    (analyze-schema (slurp in)))

  java.net.URL
  (analyze-schema [in]
    (analyze-schema (slurp in)))

  java.net.URI
  (analyze-schema [in]
    (analyze-schema (slurp in)))

  clojure.lang.IPersistentMap
  (analyze-schema [m]
    (analyze-schema-with-introspection m)))
