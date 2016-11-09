(ns alumbra.analyzer.types.default-fields)

(defn default-type-fields
  [type-name]
  {"__typename"
   {:analyzer/field-name           "__typename"
    :analyzer/containing-type-name type-name
    :analyzer/arguments            {}
    :analyzer/type-name            "String"
    :analyzer/non-null?            true
    :analyzer/type-description     {:analyzer/type-name "String"
                                    :analyzer/non-null? true}}})
