(ns alumbra.analyzer.schema-root
  (:require [com.rpl.specter :refer [traverse ALL collect-one]]))

(defn analyze
  [{:keys [graphql/schema-definitions]}]
  (let [{:keys [graphql/schema-fields]} (first schema-definitions)]
    {:analyzer/schema-root
     (->> schema-fields
          (traverse
            [ALL
             (collect-one :graphql/operation-type)
             :graphql/schema-type
             :graphql/type-name])
          (into {}))}))
