(ns alumbra.validator.fragments.fragment-spreads-acyclic
  (:require [alumbra.validator.fragments.utils :as u]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.4.2.2)
;; ---
;; - For each `fragmentDefinition` in the document
;; - Let `visited` be the empty set.
;; - `DetectCycles(fragmentDefinition, visited)`
;;
;; DetectCycles(fragmentDefinition, visited) :
;;
;; - Let `spreads` be all fragment spread descendants of `fragmentDefinition`
;; - For each `spread` in `spreads`
;;   - `visited` must not contain `spread`
;;   - Let `nextVisited` be the set including `spread` and members of `visited`
;;   - Let `nextFragmentDefinition` be the target of spread
;;   - `DetectCycles(nextFragmentDefinition, nextVisited)`

(defn- collect-edges
  [fragments]
  (->> (for [{:keys [graphql/fragment-name] :as fragment}
             fragments]
         (->> fragment
              (traverse u/all-fragment-names)
              (into #{})
              (vector fragment-name)))
       (into {})))

(defn- describe-fragments
  [fragments]
  (into {} (map (juxt :graphql/fragment-name identity) fragments)))

(def invariant
  (invariant/acyclic
    :validator/fragment-spreads-acyclic
    #(collect-edges %2)
    #(describe-fragments %2)))
