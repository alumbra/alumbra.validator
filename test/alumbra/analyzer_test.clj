(ns alumbra.analyzer-test
  (:require [clojure.test.check
             [clojure-test :refer [defspec]]
             [properties :as prop]]
            [alumbra.parser :as ql]
            [alumbra.analyzer :as analyzer]
            [alumbra.generators.schema :as g]
            [clojure.spec :as s]))

(defspec t-analyzer-conforms-to-spec 500
  (prop/for-all
    [schema g/-schema]
    (let [ast (ql/parse-schema schema)]
      (when-not (ql/error? ast)
        (s/valid? :analyzer/schema (analyzer/analyze ast))))))
