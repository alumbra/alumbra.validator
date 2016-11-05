(ns alumbra.validator.operations.operation-name-uniqueness-test
  (:require [clojure.test :refer :all]
            [alumbra.validator.operations.operation-name-uniqueness :refer :all]
            [alumbra.validator.test :as test]))

(deftest t-operation-name-uniqueness
  (test/verify
    ["query getDogName {
        dog {
          name
        }
      }

      query getOwnerName {
          dog {
            owner {
            name
          }
        }
      }"]
    invariant)

  (test/verify-error
    ["query getName {
        dog {
          name
        }
      }

      query getName {
          dog {
            owner {
            name
          }
        }
      }"

     "query dogOperation {
        dog {
          name
        }
      }

      mutation dogOperation {
        mutateDog {
          id
        }
      }"]
    invariant))
