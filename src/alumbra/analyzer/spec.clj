(ns alumbra.analyzer.spec
  (:require [clojure.spec :as s]))

(s/def :analyzer/schema
  (s/keys :req [:analyzer/types
                :analyzer/input-types
                :analyzer/interfaces
                :analyzer/directives
                :analyzer/schema-root
                :analyzer/scalars
                :analyzer/unions
                :analyzer/known-types
                :analyzer/known-composite-types]))

(s/def :analyzer/known-types
  (s/coll-of :graphql/type-name
             :gen-max 5))

(s/def :analyzer/known-composite-types
  (s/coll-of :graphql/type-name
             :gen-max 5))

(s/def :analyzer/types
  (s/map-of :graphql/type-name :analyzer/type))

(s/def :analyzer/input-types
  :analyzer/types)

(s/def :analyzer/interfaces
  (s/map-of :graphql/type-name :analyzer/interface))

(s/def :analyzer/type
  (s/keys :req [:analyzer/implements
                :analyzer/type-name
                :analyzer/fields]))

(s/def :analyzer/type-name
  :graphql/type-name)

(s/def :analyzer/implements
  (s/coll-of :graphql/type-name
             :gen-max 3))

(s/def :analyzer/fields
  (s/map-of :graphql/field-name :analyzer/field))

(s/def :analyzer/field
  (s/keys :req [:graphql/type-name]))

(s/def :analyzer/interface
  (s/keys :req [:analyzer/implemented-by
                :analyzer/fields]))

(s/def :analyzer/implemented-by
  (s/coll-of :graphql/type-name
             :gen-max 2))

(s/def :analyzer/scalars
  (s/coll-of :graphql/type-name
             :gen-max 1))

(s/def :analyzer/unions
  (s/map-of :graphql/type-name :analyzer/union-types))

(s/def :analyzer/union-types
  (s/coll-of :graphql/type-name
             :min-count 1
             :gen-max 3))

(s/def :analyzer/directives
  (s/map-of :graphql/directive-name :graphql/type-name))

(s/def :analyzer/schema-root
  (s/map-of :graphql/operation-type
            :graphql/type-name))
