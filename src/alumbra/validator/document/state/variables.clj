(ns alumbra.validator.document.state.variables
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

(defn- as-type-description
  [{:keys [alumbra/type-class
           alumbra/type-name
           alumbra/non-null?
           alumbra/element-type]}]
  (if (= type-class :list-type)
    {:non-null?        non-null?
     :type-description (as-type-description element-type)}
    {:non-null? non-null?
     :type-name type-name}))

(defn initialize
  [invariant]
  (invariant/as
    invariant
    ::types
    [:alumbra/operations
     ALL
     (collect-one :alumbra/operation-name)
     :alumbra/variables
     ALL
     (collect-one)]
    (fn [result
         [operation-name {:keys [alumbra/variable-name
                                 alumbra/type
                                 alumbra/default-value]}]]
      (->> (as-type-description type)
           (merge {:default-value default-value})
           (assoc-in result [operation-name variable-name])))
    {}))
