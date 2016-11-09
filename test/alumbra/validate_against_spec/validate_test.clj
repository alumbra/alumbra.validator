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

;; ### 5.3.1 Argument Names

(deftest t-argument-names
  (are [s] (= #{} (validate! s))
       "fragment argOnRequiredArg on Dog {
        doesKnowCommand(dogCommand: SIT)
        }"
       "fragment argOnOptional on Dog {
        isHousetrained(atOtherHomes: true) @include(if: true)
        }"
       "fragment multipleArgs on Arguments {
        multipleReqs(x: 1, y: 2)
        }"
       "fragment multipleArgsReverseOrder on Arguments {
        multipleReqs(y: 1, x: 2)
        }")
  (are [s] (= #{:validator/argument-name-in-scope}
              (validate! s))
       "fragment invalidArgName on Dog {
        doesKnowCommand(dogCommand: SIT, command: CLEAN_UP_HOUSE)
        }"
       ;; TODO: Directive Definitions?
       #_"fragment invalidArgName on Dog {
          isHousetrained (atOtherHomes: true) @include (unless: false)
          }"))

;; ### 5.3.2 Argument Uniqueness

(deftest t-argument-uniqueness
  (is (= #{:validator/argument-uniqueness}
         (validate!
           "fragment argOnRequiredArg on Dog {
            doesKnowCommand(dogCommand: SIT, dogCommand: HEEL)
            }"))))

;; ### 5.3.3.1 Argument Type Uniqueness

;; TODO

;; ### 5.3.3.2 Required Non-Null Arguments

(deftest t-required-non-null-arguments
  (are [s] (= #{} (validate! s))
       "fragment goodBooleanArg on Arguments {
        booleanArgField(booleanArg: true)
        }"

       "fragment goodNonNullArg on Arguments {
        nonNullBooleanArgField(nonNullBooleanArg: true)
        }"

       "fragment goodBooleanArgDefault on Arguments {
        booleanArgField
        }")
  (are [s] (= #{:validator/required-non-null-arguments}
              (validate! s))
       "fragment missingRequiredArg on Arguments {
        nonNullBooleanArgField
        }"
       ;; TODO: Null Literal Handling
       #_"fragment missingRequiredArg on Arguments {
          notNullBooleanArgField(nonNullBooleanArg: null)
          }"))

;; ### 5.4.1.1 Fragment Name Uniqueness

(deftest t-fragment-name-uniqueness
  (is (= #{}
         (validate!
           "{
            dog {
            ...fragmentOne
            ...fragmentTwo
            }
            }
            fragment fragmentOne on Dog {
            name
            }
            fragment fragmentTwo on Dog {
            owner {
            name
            }
            }")))
  (is (= #{:validator/fragment-name-uniqueness}
         (validate!
           "{
            dog {
            ...fragmentOne
            }
            }
            fragment fragmentOne on Dog {
            name
            }
            fragment fragmentOne on Dog {
            owner {
            name
            }
            }"))))


;; ### 5.4.1.2 Fragment Spread Type Existence

(deftest t-fragment-spread-type-existence
  (is (= #{}
         (validate!
           "fragment correctType on Dog {
            name
            }
            fragment inlineFragment on Dog {
            ... on Dog {
            name
            }
            }
            fragment inlineFragment2 on Dog {
            ... @include(if: true) {
            name
            }
            }")))
  (is (= #{:validator/fragment-spread-type-existence}
         (validate!
           "fragment notOnExistingType on NotInSchema {
            name
            }

            fragment inlineNotExistingType on Dog {
            ... on NotInSchema {
            name
            }
            }"))))

;; ### 5.4.1.3 Fragment On Composite Types

(deftest t-fragment-spread-type-existence
  (are [s] (= #{} (validate! s))
       "fragment fragOnObject on Dog {
        name
        }"
       "fragment fragOnInterface on Pet {
        name
        }"
       "fragment fragOnUnion on CatOrDog {
        ... on Dog {
        name
        }
        }")
  (are [s] (= #{:validator/fragment-on-composite-type}
              (validate! s))
       "fragment fragOnScalar on Int {
        something
        }"
       "fragment inlineFragOnScalar on Dog {
        ... on Boolean {
        somethingElse
        }
        }"))

;; ### 5.4.1.4 Fragments Must Be Used

(deftest t-fragments-must-be-used
  (is (= #{:validator/fragment-must-be-used}
         (validate!*
           "fragment nameFragment on Dog { name }
            { dog { name } }"))))

;; ### 5.4.2.1 Fragment Spread Target Defined

(deftest t-fragment-spread-target-defined
  (is (= #{:validator/fragment-spread-target-existence}
         (validate!
           "{ dog { ...undefinedFragment } }"))))

;; ### 5.4.2.2 Fragment Spreads Must Not Form Cycles

(deftest t-fragment-spreads-must-not-form-cycles
  (are [s] (= #{:validator/fragment-spreads-acyclic}
              (validate! s))
       "{
        dog {
        ...nameFragment
        }
        }
        fragment nameFragment on Dog {
        name
        ...barkVolumeFragment
        }
        fragment barkVolumeFragment on Dog {
        barkVolume
        ...nameFragment
        }"
       "{
        dog {
        ...dogFragment
        }
        }
        fragment dogFragment on Dog {
        name
        owner {
        ...ownerFragment
        }
        }
        fragment ownerFragment on Human {
        name
        pets {
        ...dogFragment
        }
        }"))

;; ### 5.4.2.3 Fragment Spread Is Possible

(deftest t-fragment-spread-is-possible
  (are [s] (= #{} (validate! s))
       "fragment dogFragment on Dog { ... on Dog { barkVolume } }"
       "fragment petNameFragment on Pet { name }
        fragment interfaceWithinObjectFragment on Dog { ...petNameFragment }"
       "fragment catOrDogNameFragment on CatOrDog { ... on Cat { meowVolume } }
        fragment unionWithObjectFragment on Dog { ...catOrDogNameFragment }"
       "fragment petFragment on Pet { name ... on Dog { barkVolume } }"
       "fragment catOrDogFragment on CatOrDog { ... on Cat { meowVolume } }"
       "fragment unionWithInterface on Pet { ...dogOrHumanFragment }
        fragment dogOrHumanFragment on DogOrHuman { ... on Dog { barkVolume } }")
  (are [s] (= #{:validator/fragment-spread-type-in-scope}
              (validate! s))
       "fragment catInDogFragmentInvalid on Dog { ... on Cat { meowVolume } }"
       "fragment sentientFragment on Sentient { ... on Dog { barkVolume } }"
       "fragment humanOrAlienFragment on HumanOrAlien { ... on Cat { meowVolume } }"
       "fragment nonIntersectingInterfaces on Pet { ...sentientFragment }
        fragment sentientFragment on Sentient { name }"))

;; ### 5.5.1 Input Object Field Uniqueness

;; TODO

;; ### 5.6.1 Directives Are Defined

(deftest t-directives-are-defined
  (are [s] (= #{} (validate! s))
       "{ dog { name @include(if: true) } }")
  (are [s] (= #{:validator/directive-defined}
              (validate! s))
       "{ dog { name @unknown } }"))

;; ### 5.6.2 Directives Are In Valid Locations

;; TODO

;; ### 5.6.3 Directives Are Unique Per Location

(deftest t-directives-are-unique-per-location
  (are [s] (= #{} (validate! s))
       "query ($foo: Boolean = true, $bar: Boolean = false) {
        dog @skip(if: $foo) {
        name
        }
        dog @skip(if: $bar) {
        nickname
        }
        }")
  (are [s] (= #{:validator/directive-uniqueness}
              (validate! s))
       "query ($foo: Boolean = true, $bar: Boolean = false) {
        dog @skip(if: $foo) @skip(if: $bar) { name }
        }"))

;; ### 5.7.1 Variable Uniqueness

(deftest t-variable-uniqueness
  (are [s] (= #{} (validate! s))
       "query A($atOtherHomes: Boolean) { ...HouseTrainedFragment }
        query B($atOtherHomes: Boolean) { ...HouseTrainedFragment }
        fragment HouseTrainedFragment on QueryRoot {
        dog { isHousetrained(atOtherHomes: $atOtherHomes) }
        }")
  (are [s] (= #{:validator/variable-uniqueness}
              (validate! s))
       "query houseTrainedQuery($atOtherHomes: Boolean, $atOtherHomes: Boolean) {
        dog {
        isHousetrained(atOtherHomes: $atOtherHomes)
        }
        }"))

;; ### 5.7.2 Variable Default Values are Correctly Typed

;; TODO

;; ### 5.7.3 Variables are Input Types

(deftest t-variables-are-input-types
  (are [s] (= #{} (validate! s))
       "query takesBoolean($atOtherHomes: Boolean) {
        dog {
        isHousetrained(atOtherHomes: $atOtherHomes)
        }
        }"
       "query takesComplexInput($complexInput: ComplexInput) {
        findDog(complex: $complexInput) {
        name
        }
        }"
       "query TakesListOfBooleanBang($booleans: [Boolean!]) {
        booleanList(booleanListArg: $booleans)
        }")
  (are [s] (= #{:validator/variables-are-input-types}
              (validate! s))
       "query takesCat($cat: Cat) { dog { name} }"
       "query takesDogBang($dog: Dog!) { dog { name } }"
       "query takesListOfPet($pets: [Pet]) { dog { name } }"
       "query takesCatOrDog($catOrDog: CatOrDog) { dog { name } }"))

;; ### 5.7.4 All Variable Uses Defined

;; TODO

;; ### 5.7.5 All Variables Used

;; TODO

;; ### 5.7.6 All Variable Usages Allowed

;; TODO
