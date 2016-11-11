(ns alumbra.validator.arguments.argument-uniqueness
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.3.2)
;; ---
;; - For each `argument` in the Document.
;; - Let `argumentName` be the Name of `argument`.
;; - Let `arguments` be all Arguments named `argumentName` in the Argument Set
;;   which contains `argument`.
;; - `arguments` must be the set containing only argument.

(def invariant
  (constantly
    (let [inv (-> (invariant/on [:alumbra/arguments ALL])
                  (invariant/unique :argument/name-unique
                                    {:unique-by :alumbra/argument-name}))]
      (invariant/and
        inv
        (-> (invariant/on [:alumbra/directives ALL])
            (invariant/each inv))))))
