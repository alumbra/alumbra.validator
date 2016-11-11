(ns alumbra.validate-against-spec.validate-test
  (:require [clojure.test :refer :all]
            [alumbra.validator :refer [validator]]
            [alumbra.parser :as ql]
            [alumbra.spec]
            [clojure.spec :as s]
            [clojure.java.io :as io]))

;; ## Fixtures

(def schema
  (-> (io/resource "alumbra/validate_against_spec/ValidationSchema.graphql")
      (io/input-stream)
      (ql/parse-schema)))

(def validate!*
  (comp (validator schema)
        #(if (ql/error? %)
           (throw (Exception. %))
           %)
        ql/parse-document))

(def validate!
  (comp #(remove
           (comp #{:fragment/must-be-used}
                 :alumbra/validation-error-class)
           %)
        validate!*))

(defmacro errors=
  [f expected query]
  `(let [~'errors (~f ~query)]
     (is (= ~(set expected)
            (set (map :alumbra/validation-error-class ~'errors))))
     (when (seq ~'errors)
       (is (s/valid? :alumbra/validation-errors ~'errors)))))

(defmacro testing-errors*
  [f & body]
  `(do
     ~@(let [pairs (partition-all 2 (partition-by set? body))]
         (for [[[expected] queries] pairs
               query queries]
           `(errors= ~f ~expected ~query)))))

(defmacro testing-errors
  [& body]
  `(testing-errors* validate! ~@body))

;; ## Tests

;; ### 5.1.1 Operation Name Uniqueness

(deftest t-operation-name-uniqueness
  (testing-errors
    #{}
    "query getDogName { dog { name } }
     query getOwnerName { dog { owner { name } } }"

    #{:operation/name-unique}
    "query getDogName { dog { name } }
     query getDogName { dog { owner { name } } }"

    #{:operation/name-unique
      :operation/allowed}
    "query getDogName { dog { name } }
     mutation getDogName { mutateDog { id } }"))

;; ### 5.1.2 Lone Anonymous Operation

(deftest t-lone-anonymous-operation
  (testing-errors
    #{}
    "{ dog { name } }"

    #{:operation/lone-anonymous}
    "{ dog { name } }
     query getName { dog { owner { name } } }"))

;; ### 5.2.1 Field Selection on Objects, Interfaces and Unions Typrs

(deftest t-field-selection-in-scope
  (testing-errors
    #{}
    "fragment interfaceFieldSelection on Pet { name }"
    "fragment inDirectFieldSelectionOnUnion on CatOrDog {
     __typename
     ... on Pet { name }
     ... on Dog { barkVolume }
     }"

    #{:field/name-in-scope}
    "fragment fieldNotDefined on Dog { meowVolume }
     fragment aliasedLyingFieldTargetNotDefined on Dog { barkVolume: kawVolume }"
    "fragment definedOnImplementorsButNotInterface on Pet { nickname }"
    "fragment directFieldSelectionOnUnion on CatOrDog { name barkVolume }"))

;; ### 5.2.2 Field Selection Merging

;; TODO

;; ### 5.2.3 Leaf Field Selection

(deftest t-leaf-field-selection
  (testing-errors
    #{}
    "fragment scalarSelection on Dog { barkVolume }"

    #{:field/leaf-selection}
    "fragment scalarSelectionsNotAllowedOnBoolean on Dog {
     barkVolume {
     sinceWhen
     }
     }"
    "query directQueryOnObjectWithoutSubFields { human }"
    "query directQueryOnInterfaceWithoutSubFields { pet }"
    "query directQueryOnUnionWithoutSubFields { catOrDog }"))

;; ### 5.3.1 Argument Names

(deftest t-argument-names
  (testing-errors
    #{}
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
     }"

    #{:argument/name-in-scope}
    "fragment invalidArgName on Dog {
     doesKnowCommand(dogCommand: SIT, command: CLEAN_UP_HOUSE)
     }"
    ;; TODO: Directive Definitions?
    #_"fragment invalidArgName on Dog {
       isHousetrained (atOtherHomes: true) @include (unless: false)
       }"))

;; ### 5.3.2 Argument Uniqueness

(deftest t-argument-uniqueness
  (testing-errors
    #{:argument/name-unique}
    "fragment argOnRequiredArg on Dog {
     doesKnowCommand(dogCommand: SIT, dogCommand: HEEL)
     }"))

;; ### 5.3.3.1 Argument Type Uniqueness

;; TODO

;; ### 5.3.3.2 Required Non-Null Arguments

(deftest t-required-non-null-arguments
  (testing-errors
    #{}
    "fragment goodBooleanArg on Arguments {
     booleanArgField(booleanArg: true)
     }"

    "fragment goodNonNullArg on Arguments {
     nonNullBooleanArgField(nonNullBooleanArg: true)
     }"

    "fragment goodBooleanArgDefault on Arguments {
     booleanArgField
     }"

    #{:argument/required-given}
    "fragment missingRequiredArg on Arguments {
     nonNullBooleanArgField
     }"
    ;; TODO: Null Literal Handling
    #_"fragment missingRequiredArg on Arguments {
       notNullBooleanArgField(nonNullBooleanArg: null)
       }"))

;; ### 5.4.1.1 Fragment Name Uniqueness

(deftest t-fragment-name-uniqueness
  (testing-errors
    #{}
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
     }"

    #{:fragment/name-unique}
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
     }"))


;; ### 5.4.1.2 Fragment Spread Type Existence

(deftest t-fragment-spread-type-existence
  (testing-errors
    #{}
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
     }"

    #{:fragment/type-exists}
    "fragment notOnExistingType on NotInSchema {
     name
     }

     fragment inlineNotExistingType on Dog {
     ... on NotInSchema {
     name
     }
     }"))

;; ### 5.4.1.3 Fragment On Composite Types

(deftest t-fragment-on-composite-types
  (testing-errors
    #{}
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
     }"

    #{:fragment/on-composite-type}
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
  (testing-errors*
    validate!*

    #{:fragment/must-be-used}
    "fragment nameFragment on Dog { name }
     { dog { name } }"))

;; ### 5.4.2.1 Fragment Spread Target Defined

(deftest t-fragment-spread-target-defined
  (testing-errors
    #{:fragment/target-exists}
    "{ dog { ...undefinedFragment } }"))

;; ### 5.4.2.2 Fragment Spreads Must Not Form Cycles

(deftest t-fragment-spreads-must-not-form-cycles
  (testing-errors
    #{:fragment/acyclic}
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
  (testing-errors
    #{}
    "fragment dogFragment on Dog { ... on Dog { barkVolume } }"
    "fragment petNameFragment on Pet { name }
     fragment interfaceWithinObjectFragment on Dog { ...petNameFragment }"
    "fragment catOrDogNameFragment on CatOrDog { ... on Cat { meowVolume } }
     fragment unionWithObjectFragment on Dog { ...catOrDogNameFragment }"
    "fragment petFragment on Pet { name ... on Dog { barkVolume } }"
    "fragment catOrDogFragment on CatOrDog { ... on Cat { meowVolume } }"
    "fragment unionWithInterface on Pet { ...dogOrHumanFragment }
     fragment dogOrHumanFragment on DogOrHuman { ... on Dog { barkVolume } }"
    #{:fragment/type-in-scope}
    "fragment catInDogFragmentInvalid on Dog { ... on Cat { meowVolume } }"
    "fragment sentientFragment on Sentient { ... on Dog { barkVolume } }"
    "fragment humanOrAlienFragment on HumanOrAlien { ... on Cat { meowVolume } }"
    "fragment nonIntersectingInterfaces on Pet { ...sentientFragment }
     fragment sentientFragment on Sentient { name }"))

;; ### 5.5.1 Input Object Field Uniqueness

;; TODO

;; ### 5.6.1 Directives Are Defined

(deftest t-directives-are-defined
  (testing-errors
    #{}
    "{ dog { name @include(if: true) } }"
    #{:directive/exists}
    "{ dog { name @unknown } }"))

;; ### 5.6.2 Directives Are In Valid Locations

;; TODO

;; ### 5.6.3 Directives Are Unique Per Location

(deftest t-directives-are-unique-per-location
  (testing-errors
    #{}
    "query ($foo: Boolean = true, $bar: Boolean = false) {
     dog @skip(if: $foo) {
     name
     }
     dog @skip(if: $bar) {
     nickname
     }
     }"
    #{:directive/name-unique}
    "query ($foo: Boolean = true, $bar: Boolean = false) {
     dog @skip(if: $foo) @skip(if: $bar) { name }
     }"))

;; ### 5.7.1 Variable Uniqueness

(deftest t-variable-uniqueness
  (testing-errors
    #{}
    "query A($atOtherHomes: Boolean) { ...HouseTrainedFragment }
     query B($atOtherHomes: Boolean) { ...HouseTrainedFragment }
     fragment HouseTrainedFragment on QueryRoot {
     dog { isHousetrained(atOtherHomes: $atOtherHomes) }
     }"
    #{:validator/variable-uniqueness}
    "query houseTrainedQuery($atOtherHomes: Boolean, $atOtherHomes: Boolean) {
     dog {
     isHousetrained(atOtherHomes: $atOtherHomes)
     }
     }"))

;; ### 5.7.2 Variable Default Values are Correctly Typed

;; TODO

;; ### 5.7.3 Variables are Input Types

(deftest t-variables-are-input-types
  (testing-errors
    #{}
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
     }"
    #{:validator/variables-are-input-types}
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
