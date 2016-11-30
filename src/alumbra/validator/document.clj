(ns alumbra.validator.document
  (:require [alumbra.validator.document.arguments.builder :as arguments]
            [alumbra.validator.document.directives.builder :as directives]
            [alumbra.validator.document.fields.builder :as fields]
            [alumbra.validator.document.fragments.builder :as fragments]
            [alumbra.validator.document.operations.builder :as operations]
            [alumbra.validator.document.values.builder :as values]
            [alumbra.validator.document.variables.builder :as variables]
            [alumbra.validator.document.builder :as builder]))

(defn invariant
  "Generate an AST invariant based on the given schema."
  [schema]
  (builder/build
    [arguments/builder
     directives/builder
     fields/builder
     fragments/builder
     operations/builder
     values/builder
     variables/builder]
    schema))
