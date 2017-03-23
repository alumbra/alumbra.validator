(ns alumbra.validator.analyzed-schema-test
  (:require [alumbra.validator :as validator]
            [alumbra.parser :as parser]
            [alumbra.analyzer :as analyzer]
            [clojure.test :refer :all]))

;; ## Helper

(defn validate!
  [schema-string]
  (-> schema-string
      (analyzer/analyze-schema parser/parse-schema)
      (validator/validate-analyzed-schema)))

;; ## Types

;; ### Interfaces Implemented

(let [base-schema "interface Page {
                     currentPage: Int!
                     totalPages: Int!
                   }
                   type UserPage implements Page {
                     currentPage: Int!
                     totalPages:  Int!
                     usernames:   [String!]!
                   }"]
  (deftest t-types-implement-interfaces
    (testing "correct interface implementation."
      (is (nil? (validate! base-schema))))
    (testing "missing interface fields."
      (is (validate!
            [base-schema
             "type InvalidPage implements Page {
                currentPage: Int!
                data:        String!
              }"])))))
