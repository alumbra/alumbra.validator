(ns alumbra.validator.analyzed-schema.builder
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; ## Helpers

(defn- mapcat-fns
  "Collect all functions at the given key in the given builders."
  [k builders]
  (mapcat #(get % k) builders))

(defn- merge-invariants
  [invariant-fns]
  (when (seq invariant-fns)
    (->> invariant-fns
         (map (fn [f] (f)))
         (apply invariant/and))))

(defn- make-invariant
  [k builders]
  (-> (mapcat-fns k builders)
      (merge-invariants)))

;; ## Invariants

(defn- make-types-invariant
  [builders]
  (-> (invariant/on [:types MAP-VALS])
      (invariant/each
        (-> (invariant/compute-as :state/type #(do (first %2)))
            (invariant/is?
              (make-invariant :types builders))))))

;; ## State

(defn- initialize-invariant
  []
  (-> (invariant/on-current-value)
      (invariant/compute-as :state/schema #(do (first %2)))))

;; ## Builder

(defn build
  [builders]
  (-> (initialize-invariant)
      (invariant/is?
        (invariant/and
          (make-types-invariant builders)))))
