(ns alumbra.analyzer.scalars
  (:require [com.rpl.specter :refer [traverse ALL]]))

(defn analyze
  "Analyze scalar definitions in a GraphQL schema conforming to
   `:graphql/schema`."
  [{:keys [graphql/scalar-definitions]}]
  {:analyzer/scalars
   (->> scalar-definitions
        (traverse [ALL :graphql/type-name])
        (into #{}))})
