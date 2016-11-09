(ns alumbra.validate-against-spec.validate-test
  (:require [clojure.test :refer :all]
            [alumbra.validator :refer [validator]]
            [alumbra.parser :as ql]
            [clojure.java.io :as io]))

;; ## Fixtures

(def schema
  (-> (io/resource "alumbra/validate_against_spec/ValidationSchema.graphql")
      (io/input-stream)
      (ql/parse-schema)))

(def validate!*
  (comp set
        #(map :invariant/name %)
        (validator schema)
        #(if (ql/error? %)
           (throw (Exception. %))
           %)
        ql/parse-document))

(def validate!
  (comp #(disj % :validator/fragment-must-be-used) validate!*))

;; ## Tests

;; ### 5.1.1 Operation Name Uniqueness

(deftest t-operation-name-uniqueness
  (is (= #{}
         (validate!
           "query getDogName { dog { name } }
            query getOwnerName { dog { owner { name } } }")))
  (is (= #{:validator/operation-name-uniqueness}
         (validate!
           "query getDogName { dog { name } }
            query getDogName { dog { owner { name } } }")))
  (is (= #{:validator/operation-name-uniqueness
           :validator/operation-allowed}
         (validate!
           "query getDogName { dog { name } }
            mutation getDogName { mutateDog { id } }"))))

;; ### 5.1.2 Lone Anonymour Operation

(deftest t-lone-anonymous-operation
  (is (= #{}
         (validate!
           "{ dog { name } }")))
  (is (= #{:validator/lone-anonymous-operation}
         (validate!
           "{ dog { name } }
            query getName { dog { owner { name } } }"))))

;; ### 5.2.1 Field Selection on Objects, Interfaces and Unions Typrs

(deftest t-field-selection-in-scope
  (is (= #{}
         (validate!
           "fragment interfaceFieldSelection on Pet { name }")))
  (is (= #{:validator/field-selection-in-scope}
         (validate!
           "fragment fieldNotDefined on Dog { meowVolume }
            fragment aliasedLyingFieldTargetNotDefined on Dog { barkVolume: kawVolume }")))
  (is (= #{:validator/field-selection-in-scope}
         (validate!
           "fragment definedOnImplementorsButNotInterface on Pet { nickname }")))
  (is (= #{}
         (validate!
           "fragment inDirectFieldSelectionOnUnion on CatOrDog {
              __typename
              ... on Pet  {
                name
              }
              ... on Dog  {
                barkVolume
              }
            }")))
  (is (= #{:validator/field-selection-in-scope}
         (validate!
           "fragment directFieldSelectionOnUnion on CatOrDog { name barkVolume }"))))

;; ### 5.2.2 Field Selection Merging

;; TODO

;; ### 5.2.3 Leaf Field Selection

(deftest t-leaf-field-selection
  (is (= #{}
         (validate!
           "fragment scalarSelection on Dog { barkVolume }")))
  (is (= #{:validator/leaf-field-selection}
         (validate!
           "fragment scalarSelectionsNotAllowedOnBoolean on Dog {
            barkVolume {
            sinceWhen
            }
            }")))
  (are [s] (= #{:validator/leaf-field-selection} (validate! s))
       "query directQueryOnObjectWithoutSubFields { human }"
       "query directQueryOnInterfaceWithoutSubFields { pet }"
       "query directQueryOnUnionWithoutSubFields { catOrDog }"))
