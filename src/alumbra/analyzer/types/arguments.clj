(ns alumbra.analyzer.types.arguments
  (:require [alumbra.analyzer.types.type-description
             :refer [describe-type]]))

(defn read-arguments
  [arguments]
  (reduce
    (fn [result {:keys [graphql/argument-name
                        graphql/argument-type]}]
      (->> {:analyzer/argument-name argument-name}
           (merge (describe-type argument-type))
           (assoc result argument-name)))
    {} arguments))
