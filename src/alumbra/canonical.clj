(ns alumbra.canonical
  (:require [alumbra.canonical
             [fragments :refer [resolve-fragments]]
             [operations :refer [resolve-operations]]]
            [alumbra.analyzer :as a]))

(defn canonicalize*
  "Given an analyzed schema and a valid (!) document, create the canonical
   representation of the document.

   If the document declares any non-null variables a map with variable values
   has to be given."
  ([analyzed-schema document]
   (canonicalize* analyzed-schema {} document))
  ([analyzed-schema variables document]
   (let [{:keys [graphql/fragments graphql/operations]} document]
     (-> {:schema analyzed-schema}
         (resolve-fragments fragments)
         (resolve-operations operations)))))

(defn canonicalize
  "Given an unanalyzed schema and a valid (!) document, create the canonical
   representation of the document. Use [[canonicalize*]] if you want to reuse
   a previously analyzed schema.

   If the document declares any non-null variables a map with variable values
   has to be given."
  ([schema document]
   (canonicalize schema {} document))
  ([schema variables document]
   (canonicalize* (a/analyze schema) variables document)))
