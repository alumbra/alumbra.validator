(ns alumbra.validator
  (:require [alumbra.validator
             [arguments :as arguments]
             [builder :as builder]
             [directives :as directives]
             [fields :as fields]
             [fragments :as fragments]
             [operations :as operations]
             [selection-set :as selection-set]
             [variables :as variables]]
            [alumbra.analyzer :as a]
            [invariant.core :as invariant]))

(defn- generate-invariant
  "Generate an AST invariant based on the given schema."
  [schema]
  (builder/build
    [arguments/builder
     directives/builder
     fields/builder
     fragments/builder
     operations/builder
     variables/builder]
    schema))

(defn validator
  "Generate a function that will valid a GraphQL AST conforming to the
   spec `:graphql/document`."
  [schema]
  (let [analyzed-schema (a/analyze-schema schema)
        invariant (generate-invariant analyzed-schema)]
    (fn [ast]
      (invariant/check invariant ast))))

(defn validate
  "Validate a GraphQL AST conforming to the spec `:graphql/document` using a
   GraphQL schema conforming to `:graphql/schema`.

   This generates the AST invariant on the fly, so in most cases you'll want
   to use [[validator]] which caches it."
  [schema ast]
  ((validator schema) ast))
