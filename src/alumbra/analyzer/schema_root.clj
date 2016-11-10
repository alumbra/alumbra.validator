(ns alumbra.analyzer.schema-root
  (:require [com.rpl.specter :refer [traverse ALL collect-one]]))

(defn analyze
  [{:keys [alumbra/schema-definitions]}]
  (let [{:keys [alumbra/schema-fields]} (first schema-definitions)]
    {:schema-root
     (->> schema-fields
          (traverse
            [ALL
             (collect-one :alumbra/operation-type)
             :alumbra/schema-type
             :alumbra/type-name])
          (into {}))}))
