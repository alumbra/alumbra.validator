(ns alumbra.validator
  (:require [alumbra.validator
             [arguments :as arguments]
             [directives :as directives]
             [fields :as fields]
             [fragments :as fragments]
             [operations :as operations]
             [selection-set :as selection-set]]
            [alumbra.analyzer :as a]
            [invariant.core :as invariant]))

(defn- generate-invariant
  "Generate an AST invariant based on the given schema."
  [analyzed-schema]
  (invariant/and
    (->> (concat
           arguments/selection-set-invariants
           fields/selection-set-invariants)
         (selection-set/merged-invariant analyzed-schema))
    (directives/invariant analyzed-schema)
    (fragments/invariant analyzed-schema)
    (operations/invariant analyzed-schema)))

(defn validator
  "Generate a function that will valid a GraphQL AST conforming to the
   spec `:graphql/document`."
  [schema]
  (let [analyzed-schema (a/analyze schema)
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
