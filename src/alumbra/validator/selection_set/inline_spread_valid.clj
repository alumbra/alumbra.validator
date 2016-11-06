(ns alumbra.validator.selection-set.inline-spread-valid
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer [ALL]]))

;; ## Helpers

(defn- inline-spread-selection?
  [selection]
  (contains? selection :graphql/type-condition))

(defn- add-scope-type
  [{:keys [graphql/type-condition] :as data}]
  (assoc data
         :validator/scope-type
         (:graphql/type-name type-condition)))

(defn collect-valid-spread-types
  [schema {:keys [analyzer/implements
                  analyzer/implemented-by
                  analyzer/union-types]}]
  (set (concat implements implemented-by union-types)))

(defn- fragment-spread-type
  [spread]
  (-> spread :graphql/type-condition :graphql/type-name))

(defn- valid-spread-type?
  [schema type]
  (let [spread-type-valid? (set (collect-valid-spread-types schema type))]
    (fn [{:keys [validator/fragment-spread-scope-type]} value]
      (let [t (fragment-spread-type value)]
        (or (spread-type-valid? t)
            (= fragment-spread-scope-type t))))))

;; ## Invariant

(defn invariant
  [schema type selection-set-valid?]
  (-> (invariant/on
        [:graphql/selection-set ALL inline-spread-selection?])
      (invariant/as
        :validator/fragment-spread-scope-type :validator/scope-type)
      (invariant/fmap add-scope-type)
      (invariant/each
        (invariant/and
          (invariant/property
            :validator/fragment-spread-type-in-scope
            (valid-spread-type? schema type))
          selection-set-valid?))))
