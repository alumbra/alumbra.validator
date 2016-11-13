(ns alumbra.canonical
  (:require [alumbra.canonical
             [fragments :refer [resolve-fragments]]
             [operations :refer [resolve-operation]]]))

(defn- select-operation
  [operations operation-name']
  (cond operation-name'
        (or (some
              (fn [{:keys [alumbra/operation-name] :as operation}]
                (when (= operation-name operation-name')
                  operation))
              operations)
            (throw
              (IllegalArgumentException. "unknown operation")))

        (next operations)
        (throw
          (IllegalArgumentException. "no operation name supplied"))

        :else
        (first operations)))

(defn canonicalizer
  "Given an analyzed schema, create a function that produces the canonical
   representation of a validated GraphQL document."
  [analyzed-schema]
  (fn canonicalize-document
    ([document]
     (canonicalize-document document nil {}))
    ([document operation-name]
     (canonicalize-document document operation-name {}))
    ([document operation-name variables]
     (let [{:keys [alumbra/fragments alumbra/operations]} document
           operation (select-operation operations operation-name)]
       (-> {:schema    analyzed-schema
            :variables variables}
           (resolve-fragments fragments)
           (resolve-operation operation))))))

(defn canonicalize
  "Canonicalize a validated GraphQL document based on the given analyzed
   schema."
  [analyzed-schema document & args]
  (apply (canonicalizer analyzed-schema) document  args))
