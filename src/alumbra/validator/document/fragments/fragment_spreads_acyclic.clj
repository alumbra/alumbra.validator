(ns alumbra.validator.document.fragments.fragment-spreads-acyclic
  (:require [alumbra.validator.document.paths :as paths]
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

(def all-named-fragments
  (recursive-path
    []
    p
    (cond-path
      :alumbra/selection-set
      [:alumbra/selection-set
       ALL
       (multi-path
         #(contains? % :alumbra/fragment-name)
         p)]
      STAY)))

(def all-fragment-names
  [all-named-fragments :alumbra/fragment-name])

(defn- collect-edges
  [fragments]
  (->> (for [{:keys [alumbra/fragment-name] :as fragment}
             fragments]
         (->> fragment
              (traverse all-fragment-names)
              (into #{})
              (vector fragment-name)))
       (into {})))

(defn- describe-fragments
  [fragments]
  (into {} (map (juxt :alumbra/fragment-name identity) fragments)))

(def invariant
  (constantly
    (-> (invariant/on [:alumbra/fragments])
        (invariant/is?
          (invariant/acyclic
            :fragment/acyclic
            #(collect-edges %2)
            #(describe-fragments %2))))))
