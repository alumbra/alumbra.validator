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

(defn validator*
  "Generate a function that will validate a GraphQL AST conforming to the spec
   `:graphql/document`.

   Note that `schema` has to be an analyzed schema."
  [schema]
  (let [invariant (generate-invariant schema)]
    (fn validator
      ([ast] (validator ast {}))
      ([ast variables]
       ;; TODO: variables
       (invariant/check invariant ast)))))

(defn validator
  "Generate a function that will validate a GraphQL AST conforming to the spec
   `:graphql/document`.

   Note that the schema can be given as either a string or an already parsed
   schema AST conforming to `:graphql/schema`.
   "
  [schema]
  (-> (if (string? schema)
        (a/analyze-schema-string schema)
        (a/analyze-schema schema))
      (validator*)))

(defn validate
  "Validate a GraphQL AST conforming to the spec `:graphql/document` using a
   GraphQL schema conforming to `:graphql/schema`.

   This generates the AST invariant on the fly, so in most cases you'll want
   to use [[validator]] which caches it."
  ([schema ast]
   (validate schema ast {}))
  ([schema ast variables]
   ((validator schema) ast variables)))
