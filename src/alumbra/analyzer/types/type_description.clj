(ns alumbra.analyzer.types.type-description)

(defn- read-type-name
  [{:keys [graphql/type-class
           graphql/type-name
           graphql/element-type]}]
  (case type-class
    :named-type type-name
    :list-type  (read-type-name element-type)))

(defn- read-type-description
  [{:keys [graphql/type-class
           graphql/non-null?
           graphql/type-name
           graphql/element-type]}]
  (case type-class
    :named-type {:analyzer/non-null? non-null?
                 :analyzer/type-name type-name}
    :list-type {:analyzer/non-null? non-null?
                :analyzer/type-description
                (read-type-description element-type)}))

(defn- read-non-null?
  [{:keys [graphql/non-null?]}]
  non-null?)

(defn describe-type
  [type]
  {:analyzer/type-description (read-type-description type)
   :analyzer/type-name        (read-type-name type)
   :analyzer/non-null?        (read-non-null? type)})
