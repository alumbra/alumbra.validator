(ns alumbra.validator.document.state
  (:require [alumbra.validator.document.state
             [analysis :as analysis]
             [fragments :as fragments]
             [variables :as variables]]
            [clojure.set :as set]
            [invariant.core :as invariant]))

;; ## Initialize

(defn initialize
  [invariant]
  (-> invariant
      (analysis/initialize)
      (fragments/initialize)
      (variables/initialize)))

;; ## Prepare Fragment/Operation

(defn- select-fragment-variables
  [{:keys [::analysis/by-fragment
           ::variables/types]}
   fragment-name]
  (let [{:keys [used-by-operations]} (get by-fragment fragment-name)]
    (if (seq used-by-operations)
      (->> (map #(get types %) used-by-operations)
           (reduce
             (fn [result vars]
               (merge result (select-keys vars (keys result))))))
      {})))

(defn- set-variables-in-scope
  [invariant]
  (invariant/compute-as
    invariant
    ::variables-in-scope
    (fn [{:keys [::variables/types
                 ::fragment-name
                 ::operation-name]
          :as state}
         _]
      (if fragment-name
        (select-fragment-variables state fragment-name)
        (get types operation-name)))))

(defn prepare-fragment
  "Prepare the state to be able to verify a fragment definition."
  [& [invariant]]
  (-> invariant
      (invariant/first-as ::fragment-name [:alumbra/fragment-name])
      (set-variables-in-scope)))

(defn prepare-operation
  "Prepare the state to be able to verify an operation definition."
  [& [invariant]]
  (-> invariant
      (invariant/first-as ::operation-name [:alumbra/operation-name])
      (set-variables-in-scope)))

;; ## Variables

(defn variable-type
  "Fetch the type of a variable in the current scope using its name."
  [{:keys [::variables-in-scope]} variable-name]
  (get variables-in-scope variable-name))

(defn variable-in-scope?
  "Check whether a variable is actually available within the current scope."
  [{:keys [::variables-in-scope]} variable-name]
  (contains? variables-in-scope variable-name))

(defn variable-used?
  [{:keys [::analysis/by-operation ::operation-name]} variable-name]
  (contains?
    (get-in by-operation [operation-name :used-variables])
    variable-name))

;; ## Fragments

(defn fragment-used?
  [{:keys [::analysis/by-fragment]} fragment-name]
  (not (empty? (get-in by-fragment [fragment-name :used-by-operations]))))

(defn fragment-known?
  [{:keys [::fragments/types]} fragment-name]
  (contains? types fragment-name))

(defn fragment-type
  [{:keys [::fragments/types]} fragment-name]
  (get types fragment-name))

(defn sorted-fragments
  [{:keys [::analysis/sorted-fragments]}]
  sorted-fragments)

;; ## Operations

(defn in-operation?
  [state]
  (contains? state ::operation-name))

(defn operation-count
  [{:keys [::analysis/operation-count] :as state}]
  operation-count)
