(ns alumbra.validator.document.variables.utils
  (:require [clojure.set :as set]
            [com.stuartsierra.dependency :as dep]
            [com.rpl.specter :refer :all]))

;; ## Paths

(def variable-value-path
  (recursive-path
    []
    p
    (cond-path
      (comp #{:variable} :alumbra/value-type)
      STAY

      (comp #{:list} :alumbra/value-type)
      [(must :alumbra/list) ALL p]

      (comp #{:object} :alumbra/value-type)
      [(must :alumbra/object) ALL (must :alumbra/value) p])))

(def ^:private variable-name-path
  [variable-value-path
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
   (set
     (keep
       (fn [[k' v]]
         (when (= k k')
           (f v)))
       deps))))

;; ### Operations

(defn- analyze-operation
  [graph node]
  (let [provided (->> (dep/immediate-dependents graph node)
                      (filter-by-tag :provided second))
        used     (->> (dep/transitive-dependencies graph node)
                      (filter-by-tag :variable))]
    {:provided-variables provided
     :unused-variables   (set/difference provided used)}))

(defn- analyze-operations
  [graph nodes]
  (reduce
    (fn [result [_ n :as node]]
      (assoc result n (analyze-operation graph node)))
    {} nodes))

;; ### Fragments

(defn- operations-using-fragment
  [dependents]
  (filter-by-tag :operation dependents))

(defn- group-provided-variables
  [dependents]
  (->> (filter-by-tag :provided dependents)
       (reduce
         (fn [result [o v]]
           (update result v (fnil conj #{}) o))
         {})))

(defn- find-unprovided-variables
  [dependencies dependents]
  (let [operations  (operations-using-fragment dependents)
        provided-by (group-provided-variables dependents)]
    (->> (filter-by-tag :variable dependencies)
         (reduce
           (fn [result v]
             (let [unprovided-by (set/difference operations
                                                 (set (provided-by v)))]
               (if (empty? unprovided-by)
                 result
                 (assoc result v unprovided-by))))
           {}))))

(defn- analyze-fragment
  [graph node]
  (let [dependents   (dep/transitive-dependents graph node)
        dependencies (dep/immediate-dependencies graph node)
        unprovided   (find-unprovided-variables dependencies dependents)]
    {:unprovided-variables unprovided}))

(defn- analyze-fragments
  [graph nodes]
  (reduce
    (fn [result [_ n :as node]]
      (assoc result n (analyze-fragment graph node)))
    {} nodes))

;; ### Variables

(defn analyze-variables
  "Generate a map describing variable usage inconsistencies, i.e. unused and
   unprovided variables."
  [doc]
  (let [graph (-> (build-fragment-graph doc)
                  (build-operation-graph doc))
        {:keys [fragment operation]} (group-by first (dep/nodes graph))]
    {:fragments  (analyze-fragments graph fragment)
     :operations (analyze-operations graph operation)}))
