(ns alumbra.analyzer.directives
  (:require [alumbra.analyzer.types.arguments
             :refer [read-arguments]]))

(defn analyze
  "Analyze directive definitions in a GraphQL schema conforming to
   `:alumbra/schema`."
  [{:keys [alumbra/directive-definitions]}]
  {:directives
   (->> directive-definitions
        (reduce
          (fn [result {:keys [alumbra/directive-name
                              alumbra/directive-locations
                              alumbra/type-field-arguments] :as x}]
            (assoc result
                   directive-name
                   {:directive-locations (set directive-locations)
                    :arguments (read-arguments type-field-arguments)}))
          {}))})
