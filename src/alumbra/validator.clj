(ns alumbra.validator
  (:require [alumbra.validator.operations
             [lone-anonymous-operation :as lone-anonymous-operation]
             [operation-name-uniqueness :as operation-name-uniqueness]]
            [invariant.core :as invariant]))

(defn- generate-invariant
  "Generate an AST invariant based on the given schema."
  [schema]
  (invariant/and
    lone-anonymous-operation/invariant
    operation-name-uniqueness/invariant))

(defn validator
  "Generate a function that will valid a GraphQL AST conforming to the
   spec `:graphql/document`."
  [schema]
  (let [invariant (generate-invariant schema)]
    (fn [ast]
      (invariant/check invariant ast))))

(defn validate
  "Validate a GraphQL AST conforming to the spec `:graphql/document` using a
   GraphQL schema conforming to `:graphql/schema`.

   This generates the AST invariant on the fly, so in most cases you'll want
   to use [[validator]] which caches it."
  [schema ast]
  ((validator schema) ast))
