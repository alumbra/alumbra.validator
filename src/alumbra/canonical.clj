(ns alumbra.canonical
  (:require [alumbra.canonical.core :as c]))

(defn canonicalize
  "Given an analyzed schema and a valid (!) document, create the canonical
   representation of the document."
  [schema document]
  (c/canonicalize schema document))
