(ns alumbra.validator.operations.lone-anonymous-operation-test
  (:require [clojure.test :refer :all]
            [alumbra.validator.operations.lone-anonymous-operation :refer :all]
            [alumbra.validator.test :as test]))

(deftest t-lone-anonymous-operation
  (test/verify
    ["{ dog  { name } }"]
    invariant)
  (test/verify-error
    ["{ dog { name } }
      query getName { dog  { owner { name } } }"]
    invariant))
