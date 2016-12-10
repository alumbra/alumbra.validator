(ns alumbra.validator.document.builder
  (:require [alumbra.validator.document
             [context :as context]
             [selection-set :as selection-set]
             [state :as state]]
            [invariant.core :as invariant]
            [com.rpl.specter :as specter]))

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
  [_]
  (-> (invariant/on-current-value)
      (state/initialize)))

(defn- make-selection-set-invariant
  [schema builders]
  (let [mk #(make-scoped-invariant-fn schema % builders)]
    (selection-set/invariant
      {:fields         (mk :fields)
       :inline-spreads (mk :inline-spreads)
       :named-spreads  (mk :named-spreads)}
      schema)))

(defn- make-fragment-invariant
  [schema builders {:keys [selection-set]}]
  (let [inv (make-invariant schema :fragments builders)]
    (-> (invariant/on [:alumbra/fragments specter/ALL])
        (invariant/each
          (-> (state/prepare-fragment)
              (invariant/is?
                (context/with-fragment-context
                  (invariant/and
                    selection-set
                    inv))))))))

(defn- make-operation-invariant
  [schema builders {:keys [selection-set]}]
  (let [inv (make-invariant schema :operations builders)]
    (-> (invariant/on [:alumbra/operations specter/ALL])
        (invariant/each
          (-> (state/prepare-operation)
              (invariant/is?
                (context/with-operation-context
                  (invariant/and
                    inv
                    selection-set))))))))

(defn- make-root-invariant
  [schema builders]
  (make-invariant schema :root builders))

;; ## Build Function

(defn- add-selection-set-invariant
  [invs schema builders]
  (assoc invs
         :selection-set
         (make-selection-set-invariant schema builders)))

(defn build
  [builders schema]
  (let [invs (-> {}
                 (add-selection-set-invariant schema builders))]
    (-> (initialize-invariant builders)
        (invariant/is?
          (invariant/and
            (make-root-invariant schema builders)
            (make-fragment-invariant schema builders invs)
            (make-operation-invariant schema builders invs))))))
