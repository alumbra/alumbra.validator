(ns alumbra.validator.document-test
  (:require [clojure.test :refer :all]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [properties :as prop]]
            [alumbra.validator :refer [validator]]
            [alumbra.analyzer :as analyzer]
            [alumbra.parser :as ql]
            [alumbra.generators :as alumbra-gen]
            [alumbra.spec]
            [clojure.spec :as s]
            [clojure.java.io :as io]))

;; ## Fixtures

(def schema
  (-> (io/resource "alumbra/validator/TestSchema.graphql")
      (analyzer/analyze-schema ql/parse-schema)))

(def validate!*
  (comp (validator schema)
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
            (set (map :alumbra/validation-error-class ~'errors)))
         (str "   query: " (pr-str ~query) "\n"
              "  errors: " (pr-str ~'errors)))
     (when (seq ~'errors)
       (is (s/valid? :alumbra/validation-errors ~'errors)
           (str "   query: " (pr-str ~query))))))

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

;; ## Generative Tests

(def ^:private excusable-errors
  "The generator is not perfect. Sorry."
  #{:field/selection-mergeable})

(defspec t-valid-queries-pass-validation 1000
  (let [gen-operation (alumbra-gen/operation schema)]
    (prop/for-all
      [query (gen-operation :query)]
      (let [result (validate!* query)]
        (or (nil? result)
            (every?
              (comp excusable-errors
                    :alumbra/validation-error-class)
              result))))))

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
     subscription getDogName { mutateDog { id } }"))

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

(deftest t-field-selection-merging
  (testing-errors
    #{}
    "query { dog {... mergeIdenticalFields } }
     fragment mergeIdenticalFields on Dog { name name }"
    "query { dog {... mergeIdenticalAliasesAndFields } }
     fragment mergeIdenticalAliasesAndFields on Dog { otherName: name, otherName: name }"
    "query { dog {... mergeIdenticalFieldsWithIdenticalArgs } }
     fragment mergeIdenticalFieldsWithIdenticalArgs on Dog {
     doesKnowCommand(dogCommand: SIT)
     doesKnowCommand(dogCommand: SIT)
     }"
    "query ($dogCommand: DogCommand!) { dog {... mergeIdenticalFieldsWithIdenticalValues } }
     fragment mergeIdenticalFieldsWithIdenticalValues on Dog {
     doesKnowCommand(dogCommand: $dogCommand)
     doesKnowCommand(dogCommand: $dogCommand)
     }"
    "query { dog { ...safeDifferingFields } }
     fragment safeDifferingFields on Pet {
     ... on Dog { volume: barkVolume }
     ... on Cat { volume: meowVolume }
     }"
    "query { dog { ...safeDifferingArgs } }
     fragment safeDifferingArgs on Pet {
     ... on Dog { doesKnowCommand(dogCommand: SIT) }
     ... on Cat { doesKnowCommand(catCommand: JUMP) }
     }"

    #{:field/selection-mergeable}
    "query { dog { ...conflictingBecauseAlias } }
     fragment conflictingBecauseAlias on Dog { name: nickname, name }"
    "query { dog { ... conflictingArgsOnValues } }
     fragment conflictingArgsOnValues on Dog {
     doesKnowCommand(dogCommand: SIT)
     doesKnowCommand(dogCommand: HEEL)
     }"
    "query ($dogCommand: DogCommand!) { dog { ... conflictingArgsValueAndVar } }
     fragment conflictingArgsValueAndVar on Dog {
     doesKnowCommand(dogCommand: SIT)
     doesKnowCommand(dogCommand: $dogCommand)
     }"
    "query ($varOne: DogCommand!, $varTwo: DogCommand!) { dog {... conflictingArgsWithVars } }
     fragment conflictingArgsWithVars on Dog {
     doesKnowCommand(dogCommand: $varOne)
     doesKnowCommand(dogCommand: $varTwo)
     }"
    "query ($varOne: DogCommand!, $varTwo: DogCommand!) { dog {...proxyFragment} }
     fragment proxyFragment on Dog { ...conflictingArgsWithVars }
     fragment conflictingArgsWithVars on Dog {
     doesKnowCommand(dogCommand: $varOne)
     doesKnowCommand(dogCommand: $varTwo)
     }"
    "query { dog { ...differingArgs } }
     fragment differingArgs on Dog {
     isHousetrained(atOtherHomes: true)
     isHousetrained
     }"
    "query { dog { ... conflictingDifferingResponses } }
     fragment conflictingDifferingResponses on Pet {
     ... on Dog { someValue: nickname }
     ... on Cat { someValue: meowVolume }
     }"))

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
    "fragment invalidArgName on Dog {
     isHousetrained (atOtherHomes: true) @include (if: true, unless: false)
     }"))

;; ### 5.3.2 Argument Uniqueness

(deftest t-argument-uniqueness
  (testing-errors
    #{:argument/name-unique}
    "fragment argOnRequiredArg on Dog {
     doesKnowCommand(dogCommand: SIT, dogCommand: HEEL)
     }"))

;; ### 5.3.3.1 Argument Type Correctness

(deftest t-argument-type-correctness
  (testing-errors
    #{}
    "{ booleanList(booleanListArg: [true, false]) }"
    "{ booleanList(booleanListArg: []) }"
    "{ booleanList(booleanListArg: null) }"
    "fragment goodBooleanArg on Arguments { booleanArgField(booleanArg: true) }"
    "fragment coercedIntIntoFloatArg on Arguments { floatArgField(floatArg: 1) }"
    "fragment intIntoFloat on Arguments { floatArgField(floatArg: 3) }"
    "fragment nullableIntArg on Arguments { intArgField(intArg: null) }"
    #{:value/type-correct}
    "{ booleanList(booleanListArg: [true, 1]) }"
    "fragment stringIntoInt on Arguments { intArgField(intArg: \"3\") }"
    "fragment invalidDirectiveArg on Dog { name @skip(if: 5) }"
    "{ booleanList(booleanListArg: [true, null]) }"
    "fragment invalidNull on Arguments { nonNullBooleanArgField(nonNullBooleanArg: null) }"
    "fragment invalidNullDirectiveArg on Dog { name @skip(if: null) }"))

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
    "fragment missingRequiredArg on Arguments { nonNullBooleanArgField }"
    "fragment missingDirectiveArg on Dog { name @skip }"))

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

(deftest t-input-object-field-uniqueness
  (testing-errors
    #{}
    "{ findDog(complex:{name: \"Doggo\"}) { nickname } }"
    "{ findDog(complex:{child:{name: \"Doggo\"}, name: \"Pupper\"}) { nickname } }"

    #{:input/field-name-unique}
    "{ findDog(complex:{name: \"Doggo\", name: \"Snoop\"}) { nickname } }"
    "{ findDog(complex:{child:{name: \"Doggo\", name: \"Snoop\"}, name: \"Pupper\"}) { nickname } }"))

(deftest t-input-object-field-exists
  (testing-errors
    #{:input/field-name-in-scope}
    "{ findDog(complex:{name: \"Doggo\", unknown: 1}) { nickname } }"))

(deftest t-input-object-required-fields-given
  (testing-errors
    #{:input/required-fields-given}
    "{ findDog(complex:{}) { nickname } }"))

;; ### 5.6.1 Directives Are Defined

(deftest t-directives-are-defined
  (testing-errors
    #{}
    "{ dog { name @include(if: true) } }"
    #{:directive/exists}
    "{ dog { name @unknown } }"))

;; ### 5.6.2 Directives Are In Valid Locations

(deftest t-directives-are-in-valid-locations
  (testing-errors
    #{}
    "{ dog { name @skip(if: false) } }"
    "{ dog { ... on Dog @skip(if: false) { name } } }"
    "mutation @unordered { callDog(name: \"Doggo\") { nickname } }"
    "{ dog { ... X @skip(if: false)} }
     fragment X on Dog { name }"

    #{:directive/location-valid}
    "query @skip(if: $foo) { dog { name } }"
    "query @unordered { dog { name } }"
    "{ dog { ... on Dog @deprecated { name } } }"
    "{ dog { ... X } }
     fragment X on Dog @skip(if: false) { name }"))

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
    #{:variable/name-unique}
    "query houseTrainedQuery($atOtherHomes: Boolean, $atOtherHomes: Boolean) {
     dog {
     isHousetrained(atOtherHomes: $atOtherHomes)
     }
     }"))

;; ### 5.7.2 Variable Default Values are Correctly Typed

(deftest t-variable-default-values-are-correctly-typed
  (testing-errors
    #{}
    "query houseTrainedQuery($atOtherHomes: Boolean = true) {
     dog { isHousetrained(atOtherHomes: $atOtherHomes) }
     }"
    "query intToFloatQuery($floatVar: Float = 1) {
     arguments { floatArgField(floatArg: $floatVar) }
     }"
    "query intToFloatQuery($floatVar: Float = null) {
     arguments { floatArgField(floatArg: $floatVar) }
     }"
    "query ($bools: [Boolean!] = [true, true]) {
     booleanList(booleanListArg: $bools)
     }"

    #{:variable/default-value-correct}
    "query houseTrainedQuery($atOtherHomes: Boolean! = true) {
     dog { isHousetrained(atOtherHomes: $atOtherHomes) }
     }"
    "query houseTrainedQuery($atOtherHomes: Boolean = \"true\") {
     dog { isHousetrained(atOtherHomes: $atOtherHomes) }
     }"))

;; ### 5.7.3 Variables are Input Types

(deftest t-variables-are-input-types
  (testing-errors
    #{}
    "query takesBoolean($atOtherHomes: Boolean) {
     dog { isHousetrained(atOtherHomes: $atOtherHomes) }
     }"
    "query takesComplexInput($complexInput: ComplexInput) {
     findDog(complex: $complexInput) { name }
     }"
    "query TakesListOfBooleanBang($booleans: [Boolean!]) {
     booleanList(booleanListArg: $booleans)
     }"
    #{:variable/must-be-used :variable/input-type}
    "query takesCat($cat: Cat) { dog { name} }"
    "query takesDogBang($dog: Dog!) { dog { name } }"
    "query takesListOfPet($pets: [Pet]) { dog { name } }"
    "query takesCatOrDog($catOrDog: CatOrDog) { dog { name } }"))

;; ### 5.7.4 All Variable Uses Defined

(deftest t-variable-uses-defined
  (testing-errors
    #{}
    "query variableIsDefined($atOtherHomes: Boolean) {
     dog { isHousetrained(atOtherHomes: $atOtherHomes) }
     }"
    "query variableIsDefinedUsedInSingleFragment($atOtherHomes: Boolean) {
     dog { ...isHousetrainedFragment }
     }
     fragment isHousetrainedFragment on Dog { isHousetrained(atOtherHomes: $atOtherHomes) }"
    "query housetrainedQueryOne($atOtherHomes: Boolean) { dog { ...isHousetrainedFragment } }
     query housetrainedQueryTwo($atOtherHomes: Boolean) { dog { ...isHousetrainedFragment } }
     fragment isHousetrainedFragment on Dog { isHousetrained(atOtherHomes: $atOtherHomes) }"

    #{:variable/name-in-operation-scope}
    "query variableIsNotDefined { dog { isHousetrained(atOtherHomes: $atOtherHomes) } }"

    #{:variable/name-in-fragment-scope}
    "query variableIsNotDefinedUsedInSingleFragment { dog { ...isHousetrainedFragment } }
     fragment isHousetrainedFragment on Dog { isHousetrained(atOtherHomes: $atOtherHomes) }"
    "query variableIsNotDefinedUsedInNestedFragment { dog { ...outerHousetrainedFragment } }
     fragment outerHousetrainedFragment on Dog { ...isHousetrainedFragment }
     fragment isHousetrainedFragment on Dog { isHousetrained(atOtherHomes: $atOtherHomes) }"
    "query housetrainedQueryOne($atOtherHomes: Boolean) { dog { ...isHousetrainedFragment } }
     query housetrainedQueryTwoNotDefined { dog { ...isHousetrainedFragment } }
     fragment isHousetrainedFragment on Dog { isHousetrained(atOtherHomes: $atOtherHomes) }"))

;; ### 5.7.5 All Variables Used

(deftest t-variables-must-be-used
  (testing-errors
    #{}
    "query variableUsedInFragment($atOtherHomes: Boolean) {
     dog { ...isHousetrainedFragment }
     }
     fragment isHousetrainedFragment on Dog {
     isHousetrained(atOtherHomes: $atOtherHomes)
     }"
    "query ($bool: Boolean!) {
     arguments { booleanListArgField(booleanListArg: [$bool]) }
     }"

    #{:variable/must-be-used}
    "query variableUnused($atOtherHomes: Boolean) { dog { isHousetrained } }"
    "query variableNotUsedWithinFragment($atOtherHomes: Boolean) {
     dog { ...isHousetrainedWithoutVariableFragment }
     }
     fragment isHousetrainedWithoutVariableFragment on Dog { isHousetrained }"
    "query queryWithUsedVar($atOtherHomes: Boolean) { dog { ...isHousetrainedFragment } }
     query queryWithExtraVar($atOtherHomes: Boolean, $extra: Int) { dog { ...isHousetrainedFragment } }
     fragment isHousetrainedFragment on Dog { isHousetrained(atOtherHomes: $atOtherHomes) }"))

;; ### 5.7.6 All Variable Usages Allowed

(deftest t-all-variable-usages-are-allowed
  (testing-errors
    #{}
    "query nonNullListToList($nonNullBooleanList: [Boolean]!) {
     arguments { booleanListArgField(booleanListArg: $nonNullBooleanList) }
     }"
    "query booleanArgQueryWithDefault($booleanArg: Boolean = true) {
     arguments { nonNullBooleanArgField(nonNullBooleanArg: $booleanArg) }
     }"
    "query ($nonNullBooleanList: [Boolean]!) {
     arguments { booleanListArgField(booleanListArg: $nonNullBooleanList) }
     }"
    "query ($booleanArg: Boolean = true) {
     arguments { nonNullBooleanArgField(nonNullBooleanArg: $booleanArg) }
     }"
    "query ($booleanArg: Boolean = true) { arguments { ... F } }
     fragment F on Arguments { nonNullBooleanArgField(nonNullBooleanArg: $booleanArg) }"
    "query ($nonNullBooleanList: [Boolean]!) { arguments { ... F } }
     fragment F on Arguments { booleanListArgField(booleanListArg: $nonNullBooleanList) }"

    #{:value/type-correct}
    "query intCannotGoIntoBoolean($intArg: Int) {
     arguments { booleanArgField(booleanArg: $intArg) }
     }"
    "query ($intArg: Int) {
     arguments { booleanArgField(booleanArg: $intArg) }
     }"
    "query booleanListCannotGoIntoBoolean($booleanListArg: [Boolean]) {
     arguments { booleanArgField(booleanArg: $booleanListArg) }
     }"
    "query booleanArgQuery($booleanArg: Boolean) {
     arguments { nonNullBooleanArgField(nonNullBooleanArg: $booleanArg) }
     }"
    "query listToNonNullList($booleanList: [Boolean]) {
     arguments { nonNullBooleanListArgField(nonNullBooleanListArg: $booleanList) }
     }"))

;; ## Introspection Tests

(deftest t-introspection-queries
  (testing-errors
    #{}
    "{ __type(name: \"User\") { name fields { name type { name } } } }"
    "{ __schema { q: queryType { name fields(includeDeprecated: true) { name } } } }"))
