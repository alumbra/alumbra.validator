(ns alumbra.validator.directives.directives-defined
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.6.1)
;; ---
;; - For every `directive` in a document.
;; - Let `directiveName` be the name of `directive`.
;; - Let `directiveDefinition` be the directive named `directiveName`.
;; - `directiveDefinition` must exist.

(defn directive-defined?
  [{:keys [analyzer/directives]}]
  (comp (set (keys directives))
        :graphql/directive-name))

(defn invariant
  [schema]
  (-> (invariant/on [(walker :graphql/directive-name)])
      (invariant/each
        (-> (invariant/value
              :validator/directive-defined
              (directive-defined? schema))
            (invariant/with-error-context
              (fn [_ {:keys [graphql/directive-name]}]
                {:analyzer/directive-name directive-name}))))))
