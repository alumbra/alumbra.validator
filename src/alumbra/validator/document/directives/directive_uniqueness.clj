(ns alumbra.validator.document.directives.directive-uniqueness
  (:require [invariant.core :as invariant]
            [invariant.debug :as debug]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.6.3)
;; ---
;; - For every `location` in the document for which Directives can apply:
;; Let directives be the set of Directives which apply to location.
;; For each directive in directives:
;; Let directiveName be the name of directive.
;; Let namedDirectives be the set of all Directives named directiveName in directives.
;; namedDirectives must be a set of one.

(def invariant
  (constantly
    (-> (invariant/on [:alumbra/directives ALL])
        (invariant/unique
          :directive/name-unique
          {:unique-by :alumbra/directive-name}))))
