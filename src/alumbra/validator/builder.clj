(ns alumbra.validator.builder
  (:require [alumbra.validator.selection-set :as selection-set]
            [invariant.potemkin :refer [defprotocol+]]
            [invariant.core :as invariant]))

;; ## Protocol

(defprotocol+ ValidatorBuilder
  "Protocol for validator builders, producing invariants based on the
   analyzed schema. All `for-*` functions should produce seqs
   of functions."
  (invariant-state [_ invariant]
    "Add validator-specific state to the given invariant (generated from
     the whole `:graphql/document`).")
  (for-operations [_ schema]
    "Generate invariants to be run on the `:graphql/operations` entry.")
  (for-fragments [_ schema]
    "Generate invariants to be run on the `:graphql/fragments` entry.")
  (for-fields [_ schema]
    "Generate invariants to be run on `:graphql/field` entries.")
  (for-fragment-spreads [_ schema]
    "Generate invariants to be run on `:graphql/fragment-spread entries.")
  (for-inline-spreads [_ schema]
    "Generate invariants to be run on `:graphql/inline-fragment` entries."))

;; ## Build

(defn- mapcat-fns
  [f schema builders]
  (mapcat (memoize #(f % schema)) builders))

(defn- merge-invariant-fns
  [invariant-fns]
  (when (seq invariant-fns)
    #(->> invariant-fns
          (map (fn [f] (f %1 %2)))
          (apply invariant/and))))

(defn- merge-invariants
  [invariants]
  (when (seq invariants)
    (apply invariant/and invariants)))

(defn- invariant-with-state
  [builders]
  (reduce
    (fn [invariant builder]
      (invariant-state builder invariant))
    (invariant/on-current-value)
    builders))

(defn build
  [builders schema]
  (let [field-fns           (mapcat-fns for-fields schema builders)
        fragment-spread-fns (mapcat-fns for-fragment-spreads schema builders)
        inline-spread-fns   (mapcat-fns for-inline-spreads schema builders)
        operation-invs      (mapcat-fns for-operations schema builders)
        fragment-invs       (mapcat-fns for-fragments schema builders)]
    (-> (invariant-with-state builders)
        (invariant/is?
          (invariant/and
            (selection-set/invariant
              schema
              {:fields         (merge-invariant-fns field-fns)
               :named-spreads  (merge-invariant-fns fragment-spread-fns)
               :inline-spreads (merge-invariant-fns inline-spread-fns)})
            (-> (invariant/on [:graphql/operations])
                (invariant/is? (merge-invariants operation-invs)))
            (-> (invariant/on [:graphql/fragments])
                (invariant/is? (merge-invariants fragment-invs))))))))
