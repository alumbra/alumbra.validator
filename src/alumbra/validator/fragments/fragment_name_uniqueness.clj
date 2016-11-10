(ns alumbra.validator.fragments.fragment-name-uniqueness
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer [ALL]]))

;; Formal Specification (5.4.1.1)
;; ---
;; - For each fragment definition `fragment` in the document
;; - Let `fragmentName` be the name of fragment.
;; - Let `fragments` be all fragment definitions in the document named
;;   `fragmentName`.
;;   - `fragments` must be a set of one.

(def invariant
  (constantly
    (-> (invariant/on [ALL])
        (invariant/unique :validator/fragment-name-uniqueness
                          {:unique-by :alumbra/fragment-name}))))
