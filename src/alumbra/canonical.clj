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

(defn canonicalize
  "Given an analyzed schema and a valid (!) document, create the canonical
   representation of the document."
  ([analyzed-schema document]
   (canonicalize analyzed-schema document {} nil))
  ([analyzed-schema document variables]
   (canonicalize analyzed-schema document variables nil))
  ([analyzed-schema document variables operation-name]
   (let [{:keys [alumbra/fragments alumbra/operations]} document
         operation (select-operation operations operation-name)]
     (-> {:schema    analyzed-schema
          :variables variables}
         (resolve-fragments fragments)
         (resolve-operation operation)))))
