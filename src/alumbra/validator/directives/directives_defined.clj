(ns alumbra.validator.directives.directives-defined
  (:require [alumbra.validator.errors
             :refer [with-directive-context]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.6.1)
;; ---
;; - For every `directive` in a document.
;; - Let `directiveName` be the name of `directive`.
;; - Let `directiveDefinition` be the directive named `directiveName`.
;; - `directiveDefinition` must exist.

(defn directive-defined?
  [{:keys [directives]}]
  (comp (set (keys directives))
        :alumbra/directive-name))

(defn invariant
  [schema _]
  (-> (invariant/on [:alumbra/directives ALL])
      (invariant/each
        (with-directive-context
          (invariant/value
              :directive/exists
              (directive-defined? schema))))))
