(ns alumbra.analyzer.directives
  (:require [com.rpl.specter :refer [traverse ALL collect-one]]))

(defn analyze
  "Analyze directive definitions in a GraphQL schema conforming to
   `:graphql/schema`."
  [{:keys [graphql/directive-definitions]}]
  {:analyzer/directives
   (->> directive-definitions
        (traverse
          [ALL
           (collect-one :graphql/directive-name)
           :graphql/type-condition
           :graphql/type-name])
        (into {}))})
