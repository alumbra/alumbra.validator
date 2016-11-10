(ns alumbra.analyzer.scalars
  (:require [com.rpl.specter :refer [traverse ALL]]))

(defn analyze
  "Analyze scalar definitions in a GraphQL schema conforming to
   `:alumbra/schema`."
  [{:keys [alumbra/scalar-definitions]}]
  {:scalars
   (->> scalar-definitions
        (traverse [ALL :alumbra/type-name])
        (into #{}))})
