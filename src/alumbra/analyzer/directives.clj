(ns alumbra.analyzer.directives
  (:require [com.rpl.specter :refer [traverse ALL collect-one]]))

(defn analyze
  "Analyze directive definitions in a GraphQL schema conforming to
   `:alumbra/schema`."
  [{:keys [alumbra/directive-definitions] :as x}]
  {:directives
   (->> directive-definitions
        (traverse
          [ALL
           (collect-one :alumbra/directive-name)
           :alumbra/directive-locations])
        (reduce
          (fn [result [directive-name locations]]
            (assoc result
                   directive-name
                   {:directive-locations locations
                    :arguments {}}))
          {}))})
