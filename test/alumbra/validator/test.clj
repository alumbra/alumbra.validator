(ns alumbra.validator.test
  (:require [invariant.core :as invariant]
            [clojure.test :refer :all]
            [alumbra.parser :as ql]))

(defmacro verify
  [queries invariant]
  `(do
     ~@(for [query queries]
         `(let [q# ~query
                ast# (ql/parse-document q#)]
            (is (nil? (invariant/check ~invariant ast#)))))))

(defmacro verify-error
  [queries invariant]
  `(do
     ~@(for [query queries]
         `(let [q# ~query
                ast# (ql/parse-document q#)]
            (is (seq (invariant/check ~invariant ast#)))))))
