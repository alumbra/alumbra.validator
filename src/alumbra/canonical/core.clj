(ns alumbra.canonical.core
  (:require [com.stuartsierra.dependency :as dep]
            [com.rpl.specter :refer :all]))

;; ## Resolution

(declare resolve-selection-set)

(defn- add-type-condition
  [{:keys [graphql/type-name]} selection-set]
  (into {} (map
             (fn [[field-name field-value]]
               [field-name
                (update field-value
                        :graphql/canonical-field-type-condition
                        #(or % type-name))]))
        selection-set))

(defn- resolve-field
  [schema fragments {:keys [graphql/field-name
                            graphql/selection-set]}]
  ;; TODO: Track current type and attach non-null/list/object information.
  (merge
    {:graphql/field-name field-name}
    (if selection-set
      {:graphql/canonical-selection
       (resolve-selection-set schema fragments selection-set)}
      {:graphql/canonical-field-type :leaf})))

(defn- resolve-selection-set
  [schema fragments selection-set]
  (reduce
    (fn [result {:keys [graphql/field-name
                        graphql/field-alias
                        graphql/type-condition
                        graphql/fragment-name]
                 :as selection}]
      (cond fragment-name
            (merge result (get fragments fragment-name))

            field-name
            (assoc result
                   (or field-alias field-name)
                   (resolve-field schema fragments selection))

            type-condition
            (->> (:graphql/selection-set selection)
                 (resolve-selection-set schema fragments)
                 (add-type-condition type-condition)
                 (merge result))

            :else
            result))
    {} selection-set))

;; ## Fragment Analysis

;; ### Dependency Collection

(def all-fragment-dependencies
  (comp-paths
    (recursive-path
      []
      p
      (cond-path
        :graphql/selection-set
        [:graphql/selection-set
         ALL
         (multi-path
           (must :graphql/fragment-name)
           p)]
        STAY))))

(defn- add-fragment-dependencies
  [graph {:keys [graphql/fragment-name] :as fragment}]
  (->> (traverse all-fragment-dependencies fragment)
       (reduce
         #(dep/depend %1 fragment-name %2)
         (dep/depend graph ::root fragment-name))))

;; ### Fragment Resolution

(defn- resolve-fragments
  [schema topo-sorted-fragments]
  (reduce
    (fn [done-fragments {:keys [graphql/fragment-name
                                graphql/type-condition
                                graphql/selection-set]}]
      (->> (resolve-selection-set schema done-fragments selection-set)
           (add-type-condition type-condition)
           (assoc done-fragments fragment-name)))
    {} topo-sorted-fragments))

;; ### Fragment Processing

(defn- process-fragments
  [schema {:keys [graphql/fragments]}]
  (loop [graph     (dep/graph)
         result    {}
         fragments fragments]
    (if (seq fragments)
      (let [[{:keys [graphql/fragment-name] :as fragment} & rst] fragments]
        (recur
          (add-fragment-dependencies graph fragment)
          (assoc result fragment-name fragment)
          rst))
      (->> (dep/topo-sort graph)
           (butlast)
           (map #(get result %))
           (resolve-fragments schema)))))

;; ### Operation Processing

(defn- process-operations
  [schema fragments {:keys [graphql/operations]}]
  (map
    (fn [{:keys [graphql/selection-set] :as op}]
      (merge
        {:graphql/canonical-selection
         (resolve-selection-set schema fragments selection-set)}
        (select-keys op [:graphql/operation-type
                         :graphql/operation-name])))
    operations))

;; ## Canonicalize Function

(defn canonicalize
  [schema document]
  (let [fragments (process-fragments schema document)]
    (process-operations schema fragments document)))
