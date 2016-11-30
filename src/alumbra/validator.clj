(ns alumbra.validator
  (:require [alumbra.validator
             [document :as document]
             [errors :as errors]]
            [invariant.core :as invariant]))

;; ## Query Document Validation

(defn validator
  "Generate a function that will validate a GraphQL AST conforming to the spec
   `:alumbra/document`, based on the given `:alumbra/analyzed-schema`."
  [schema]
  (let [invariant (document/invariant schema)]
    (fn validate-graphql-document
      ([ast]
       (validate-graphql-document ast nil {}))
      ([ast operation-name]
       (validate-graphql-document ast operation-name {}))
      ([ast operation-name variables]
       (if (:alumbra/parser-errors ast)
         ast
         ;; TODO: variables/operation name
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
