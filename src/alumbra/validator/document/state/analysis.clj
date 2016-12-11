(ns alumbra.validator.document.state.analysis
  (:require [alumbra.validator.document.paths :as paths]
            [invariant.core :as invariant]
            [clojure.set :as set]
            [com.stuartsierra.dependency :as dep]
            [com.rpl.specter :refer :all]))

;; ## Paths

(def ^:private variable-name-path
  [paths/variable-values
   (must :alumbra/variable-name)
   (putval :variable)])

(def ^:private arguments-path
  [(must :alumbra/arguments)
   ALL
   (must :alumbra/argument-value)
   variable-name-path])

(def ^:private directive-arguments-path
  [(must :alumbra/directives)
   ALL
   arguments-path])

(def ^:private named-fragment-path
  [(must :alumbra/fragment-name) (putval :fragment)])

(def ^:private selection-set-path
  (recursive-path
    []
    p
    [(must :alumbra/selection-set)
     ALL
     (multi-path
       arguments-path
       directive-arguments-path
       named-fragment-path
       p)]))

(def ^:private fragment-path
  [(must :alumbra/fragments)
   ALL
   (collect-one :alumbra/fragment-name)
   selection-set-path])

(def ^:private provided-variable-path
  [(must :alumbra/variables)
   ALL
   (must :alumbra/variable-name)
   (putval :provided)])

(def ^:private operation-path
  [(must :alumbra/operations)
   ALL
   (collect-one :alumbra/operation-name)
   (multi-path
     provided-variable-path
     selection-set-path)])

;; ## Graph

(defn- cycle?
  [graph dependent-fragment other-fragment]
  (or (= dependent-fragment other-fragment)
      (dep/depends? graph
                    [:fragment other-fragment]
                    [:fragment dependent-fragment])))

(defn- build-fragment-graph
  [doc]
  (->> (traverse fragment-path doc)
       (reduce
         (fn [graph [fragment-name k v]]
           (if (and (= k :fragment) (cycle? graph fragment-name v))
             graph
             (dep/depend graph [:fragment fragment-name] [k v])))
         (dep/graph))))

(defn- build-operation-graph
  [graph doc]
  (->> (traverse operation-path doc)
       (reduce
         (fn [graph [operation-name k v]]
           (if (= k :provided)
             (dep/depend graph [k [operation-name v]] [:operation operation-name])
             (dep/depend graph [:operation operation-name] [k v])))
         graph)))

;; ## Analysis

;; ### Helper

(defn- filter-by-tag
  ([k deps]
   (filter-by-tag k identity deps))
  ([k f deps]
   (reduce
     (fn [result [k' v]]
       (if (= k k')
         (conj result (f v))
         result))
     #{} deps)))

;; ### Operations

(defn- analyze-operation
  [graph node]
  (let [dependencies (dep/transitive-dependencies graph node)]
    {:used-variables (filter-by-tag :variable dependencies)
     :used-fragments (filter-by-tag :fragment dependencies)}))

(defn- analyze-operations
  [graph nodes]
  (reduce
    (fn [result [_ n :as node]]
      (assoc result n (analyze-operation graph node)))
    {} nodes))

;; ### Fragments

(defn- analyze-fragment
  [graph node]
  (let [dependents   (dep/transitive-dependents graph node)
        dependencies (dep/immediate-dependencies graph node)]
    {:used-by-operations (filter-by-tag :operation dependents)
     :used-variables     (filter-by-tag :variable dependencies)
     :used-fragments     (filter-by-tag :fragment dependencies)}))

(defn- analyze-fragments
  [graph nodes]
  (reduce
    (fn [result [_ n :as node]]
      (assoc result n (analyze-fragment graph node)))
    {} nodes))

;; ### Invariant

(defn- analyze
  "Generate a map describing relationships between fragments, operations and
   variables."
  [doc]
  (let [graph (-> (build-fragment-graph doc)
                  (build-operation-graph doc))
        {:keys [fragment operation]} (group-by first (dep/nodes graph))]
    {:fragments  (analyze-fragments graph fragment)
     :operations (analyze-operations graph operation)}))

(defn initialize
  [invariant]
  (-> invariant
      (invariant/compute-as ::data  #(analyze (first %2)))
      (invariant/compute-as ::by-operation
                            (fn [{:keys [::data]} _]
                              (get data :operations)))
      (invariant/compute-as ::by-fragment
                            (fn [{:keys [::data]} _]
                              (get data :fragments)))))
