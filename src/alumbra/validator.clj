(ns alumbra.validator
  (:require [alumbra.validator
             [arguments :as arguments]
             [builder :as builder]
             [directives :as directives]
             [errors :as errors]
             [fields :as fields]
             [fragments :as fragments]
             [operations :as operations]
             [selection-set :as selection-set]
             [variables :as variables]]
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
  "Generate a function that will validate a GraphQL AST conforming to the spec
   `:alumbra/document`, based on the given `:alumbra/analyzed-schema`."
  [schema]
  (let [invariant (generate-invariant schema)]
    (fn validate-graphql-document
      ([ast] (validate-graphql-document ast {}))
      ([ast variables]
       (if (:alumbra/parser-errors ast)
         ast
         ;; TODO: variables
         (->> (invariant/check invariant ast)
              (errors/as-validation-errors)))))))

(defn validate
  "Validate a GraphQL AST conforming to the spec `:alumbra/document` using a
   GraphQL schema conforming to `:alumbra/analyzed-schema`.

   This generates the AST invariant on the fly, so in most cases you'll want
   to use [[validator]] which caches it."
  ([schema ast]
   (validate schema ast {}))
  ([schema ast variables]
   ((validator schema) ast variables)))
