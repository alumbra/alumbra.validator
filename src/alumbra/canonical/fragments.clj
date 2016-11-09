(ns alumbra.canonical.fragments
  (:require [alumbra.canonical.selection-set :refer [resolve-selection-set]]
            [com.stuartsierra.dependency :as dep]
            [com.rpl.specter :refer :all]))

;; ## Topological Sort of Fragments

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

(defn- sort-fragments
  "Sort fragments topologically."
  [fragments]
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
           (map #(get result %))))))

;; ## Resolve Fragments

(defn- resolve-fragment
  [opts {:keys [graphql/fragment-name
                graphql/type-condition
                graphql/selection-set]}]
  (let [scope-type (:graphql/type-name type-condition)]
    (->> (resolve-selection-set
           (assoc opts
                  :scope-type     scope-type
                  :type-condition scope-type)
           selection-set)
         (assoc-in opts [:fragments fragment-name]))))

(defn resolve-fragments
  "Resolve fragments, inlining fields from all dependent fragments directly
   into them. Produces a map associating fragment names with the canonical
   selection set for that fragment."
  [opts fragments]
  (->> (sort-fragments fragments)
       (reduce resolve-fragment opts)))
