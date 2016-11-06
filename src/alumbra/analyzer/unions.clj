(ns alumbra.analyzer.unions
  (:require [com.rpl.specter :refer [traverse ALL collect-one]]))

(defn analyze
  "Analyze union definitions in a GraphQL schema conforming to
   `:graphql/schema`."
  [{:keys [graphql/union-definitions]}]
  {:analyzer/unions
   (->> union-definitions
        (traverse
          [ALL
           (collect-one :graphql/type-name)
           :graphql/union-types
           ALL
           :graphql/type-name])
        (reduce
          (fn [result [type-name union-type-name]]
            (update result type-name (fnil conj #{}) union-type-name))
          {}))})
