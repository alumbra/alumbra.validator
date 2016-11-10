(ns alumbra.analyzer.unions
  (:require [alumbra.analyzer.types.default-fields
             :refer [default-type-fields]]
            [com.rpl.specter :refer [traverse ALL collect-one]]))

(defn analyze
  "Analyze union definitions in a GraphQL schema conforming to
   `:alumbra/schema`."
  [{:keys [alumbra/union-definitions]}]
  {:unions
   (->> union-definitions
        (traverse
          [ALL
           (collect-one :alumbra/type-name)
           :alumbra/union-types
           ALL
           :alumbra/type-name])
        (reduce
          (fn [result [type-name union-type-name]]
            (update result
                    type-name
                    (fnil
                      #(update % :union-types conj union-type-name)
                      {:type-name   type-name
                       :fields      (default-type-fields type-name)
                       :union-types #{}})))
          {}))})
