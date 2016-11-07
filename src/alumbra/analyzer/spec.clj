(ns alumbra.analyzer.spec
  (:require [clojure.spec :as s]))

;; ## Names

(s/def :analyzer/type-name
  :graphql/type-name)

(s/def :analyzer/containing-type-name
  :graphql/type-name)

(s/def :analyzer/argument-name
  :graphql/argument-name)

(s/def :analyzer/field-name
  :graphql/field-name)

(s/def :analyzer/directive-name
  :graphql/directive-name)

(s/def :analyzer/operation-type
  :graphql/operation-type)

;; ## Schema

(s/def :analyzer/schema
  (s/keys :req [:analyzer/types
                :analyzer/input-types
                :analyzer/interfaces
                :analyzer/directives
                :analyzer/schema-root
                :analyzer/scalars
                :analyzer/unions
                :analyzer/type->kind]))

;; ### Type/Kind Mapping

(s/def :analyzer/kind
  #{:type :interface :input-type :union :directive :scalar :enum})

(s/def :analyzer/type->kind
  (s/map-of :analyzer/type-name :analyzer/kind))

;; ### Fragment Spreads

(s/def :analyzer/valid-fragment-spreads
  (s/coll-of :analyzer/type-name
             :gen-max 3))

;; ### Structured Types

(s/def :analyzer/types
  (s/map-of :analyzer/type-name :analyzer/type))

(s/def :analyzer/input-types
  (s/map-of :analyzer/type-name :analyzer/input-type))

(s/def :analyzer/input-type
  (s/keys :req [:analyzer/type-name
                :analyzer/implements
                :analyzer/fields]))

(s/def :analyzer/type
  (s/keys :req [:analyzer/type-name
                :analyzer/implements
                :analyzer/valid-fragment-spreads
                :analyzer/fields]))

(s/def :analyzer/implements
  (s/coll-of :analyzer/type-name
             :gen-max 3))

(s/def :analyzer/fields
  (s/map-of :analyzer/field-name :analyzer/field))

(s/def :analyzer/field
  (s/keys :req [:analyzer/field-name
                :analyzer/containing-type-name
                :analyzer/type-name
                :analyzer/non-null?
                :analyzer/arguments]))

(s/def :analyzer/arguments
  (s/map-of :analyzer/argument-name
            :analyzer/argument))

(s/def :analyzer/argument
  (s/keys :req [:analyzer/argument-name
                :analyzer/non-null?
                :analyzer/type-name]))

(s/def :analyzer/non-null?
  :graphql/non-null?)

;; ### Interfaces

(s/def :analyzer/interfaces
  (s/map-of :analyzer/type-name :analyzer/interface))

(s/def :analyzer/interface
  (s/keys :req [:analyzer/type-name
                :analyzer/implemented-by
                :analyzer/valid-fragment-spreads
                :analyzer/fields]))

(s/def :analyzer/implemented-by
  (s/coll-of :analyzer/type-name
             :gen-max 2))

;; ### Union Types

(s/def :analyzer/unions
  (s/map-of :analyzer/type-name :analyzer/union))

(s/def :analyzer/union
  (s/keys :req [:analyzer/type-name
                :analyzer/valid-fragment-spreads
                :analyzer/union-types]))

(s/def :analyzer/union-types
  (s/coll-of :analyzer/type-name
             :min-count 1
             :gen-max 3))

;; ### Scalars

(s/def :analyzer/scalars
  (s/coll-of :analyzer/type-name
             :gen-max 1))

;; ### Directives

(s/def :analyzer/directives
  (s/map-of :analyzer/directive-name :analyzer/type-name))

;; ### Schema Root

(s/def :analyzer/schema-root
  (s/map-of :analyzer/operation-type
            :analyzer/type-name))
