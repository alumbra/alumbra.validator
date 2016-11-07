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
  (-> (invariant/on [(walker :graphql/arguments) :graphql/arguments])
      (invariant/each
        (-> (invariant/on [ALL])
            (invariant/unique :validator/argument-uniqueness
                              {:unique-by :graphql/argument-name})))))