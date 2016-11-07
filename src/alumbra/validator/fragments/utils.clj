(ns alumbra.validator.fragments.utils
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

(defn type-name
  "Extract the type name from a fragment spread."
  [fragment]
  (-> fragment :graphql/type-condition :graphql/type-name))

(defn- with-fragment-context
  "Add fragment information to invariant error context."
  [invariant]
  (invariant/with-error-context
    invariant
    (fn [_ {:keys [graphql/fragment-name] :as fragment}]
      {:analyzer/fragment-name fragment-name
       :analyzer/type-name     (type-name fragment)})))

(def spread?
  "A specter path finding all fragment spreads."
  (walker :graphql/type-condition))

(defn fragment-spread-invariant
  "An invariant applied to each fragment spread within
   the document."
  [invariant]
  (invariant/recursive
    [self]
    (-> (invariant/on [spread?])
        (invariant/each
          (invariant/and
            (with-fragment-context invariant)
            (-> (invariant/on [:graphql/selection-set])
                (invariant/is? self)))))))
