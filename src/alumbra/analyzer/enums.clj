(ns alumbra.analyzer.enums
  (:require [com.rpl.specter :refer :all]))

(defn analyze
  "Analyze enum definitions in a GraphQL schema conforming to
   `:alumbra/schema`."
  [{:keys [alumbra/enum-definitions]}]
  {:enums
   (->> enum-definitions
        (traverse
          [ALL
           (collect-one :alumbra/type-name)
           :alumbra/enum-fields
           ALL
           :alumbra/enum])
        (reduce
          #(update %1 (first %2) (fnil conj #{}) (second %2))
          {}))})
