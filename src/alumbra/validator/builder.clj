(ns alumbra.validator.builder
  (:require [alumbra.validator.selection-set :as selection-set]
            [invariant.potemkin :refer [defprotocol+]]
            [invariant.core :as invariant]))

;; ## Builder Maps
;;
;; Validator builders are represented as maps of invariant builder fns.
;;
;; The following functions should only take the schema as parameter:
;;
;; - `:operations`: to be run on [:alumbra/operations]
;; - `:fragments`: to be run on [:alumbra/fragments]
;;
;; The following functions should take the schema and the current scope's
;; `:type` map:
;;
;; - `:fields`: to be run on [* :alumbra/field]
;; - `:inline-spreads`: to be run on [* :alumbra/inline-spread]
;; - `:named-spreads`: to be run on [* :alumbra/fragment-spread]
;;
;; Additionally, the following keys are allowed:
;;
;; - `:state`: a function attaching state to the invariant.

;; ## Builder Helpers

(defn- mapcat-fns
  "Collect all functions at the given key in the given builders."
  [k builders]
  (mapcat #(get % k) builders))

(defn- merge-invariants
  [invariant-fns schema]
  (when (seq invariant-fns)
    (->> invariant-fns
         (map (fn [f] (f schema)))
         (apply invariant/and))))

(defn- merge-scoped-invariant-fns
  [invariant-fns schema]
  (when (seq invariant-fns)
    (fn [type]
      (->> invariant-fns
           (map (fn [f] (f schema type)))
           (apply invariant/and)))))

(defn make-invariant
  [schema k builders]
  (-> (mapcat-fns k builders)
      (merge-invariants schema)))

(defn- make-scoped-invariant-fn
  [schema k builders]
  (-> (mapcat-fns k builders)
      (merge-scoped-invariant-fns schema)))

;; ## Invariant Builders

(defn- initialize-invariant
  [builders]
  (reduce
    (fn [invariant {:keys [state]}]
      (if state
        (state invariant)
        invariant))
    (invariant/on-current-value)
    builders))

(defn- make-selection-set-invariant
  [schema builders]
  (let [mk #(make-scoped-invariant-fn schema % builders)]
    (selection-set/invariant
      {:fields         (mk :fields)
       :inline-spreads (mk :inline-spreads)
       :named-spreads  (mk :named-spreads)}
      schema)))

(defn- make-fragment-invariant
  [schema builders]
  (let [inv (make-invariant schema :fragments builders)]
    (-> (invariant/on [:alumbra/fragments])
        (invariant/is? inv))))

(defn- make-operation-invariant
  [schema builders]
  (let [inv (make-invariant schema :operations builders)]
    (-> (invariant/on [:alumbra/operations])
        (invariant/is? inv))))

;; ## Build Function

(defn build
  [builders schema]
  (-> (initialize-invariant builders)
      (invariant/is?
        (invariant/and
          (make-selection-set-invariant schema builders)
          (make-fragment-invariant schema builders)
          (make-operation-invariant schema builders)))))
