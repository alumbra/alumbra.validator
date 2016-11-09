(ns alumbra.analyzer.valid-fragment-spreads
  (:require [clojure.set :as set]))

(defn- add-valid-fragment-spreads-to-union
  [{:keys [analyzer/types]} union-types allowed-types]
  (->> (mapcat
         (comp :analyzer/valid-fragment-spreads types)
         union-types)
       (into allowed-types)))

(defn- add-matching-unions
  [{:keys [analyzer/unions]} allowed-types]
  (->> unions
       (keep
         (fn [[type-name {:keys [analyzer/union-types]}]]
           (when (seq (set/intersection allowed-types union-types))
             type-name)))
       (into allowed-types)))

(defn- add-valid-fragment-spreads-to-type
  [schema {:keys [analyzer/type-name
                  analyzer/implements
                  analyzer/implemented-by
                  analyzer/union-types] :as type}]
  (->> (concat implements implemented-by union-types)
       (into #{type-name})
       (add-matching-unions schema)
       (add-valid-fragment-spreads-to-union schema union-types)
       (assoc type :analyzer/valid-fragment-spreads)))

(defn- add-valid-fragment-spreads
  [schema k]
  (->> (fn [m]
         (->> (for [[k v] m]
                [k (add-valid-fragment-spreads-to-type schema v)])
              (into {})))
       (update schema k)))

(defn aggregate
  [{:keys [analyzer/unions] :as schema}]
  (-> schema
      (add-valid-fragment-spreads :analyzer/types)
      (add-valid-fragment-spreads :analyzer/interfaces)
      (add-valid-fragment-spreads :analyzer/unions)))
