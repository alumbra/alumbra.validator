(ns alumbra.validator.document.variables.state
  (:require [alumbra.validator.document.variables.utils :as u]
            [invariant.core :as invariant]
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

(defn state
  [invariant]
  (-> invariant
      (invariant/as :variables/usages (comp u/analyze-variables first))
      (invariant/as :variables/types
                    [:alumbra/operations
                     ALL
                     (collect-one :alumbra/operation-name)
                     :alumbra/variables
                     ALL
                     (collect-one :alumbra/variable-name)
                     (collect-one :alumbra/type)]
                    (fn [result [operation-name variable-name type]]
                      (assoc-in result
                                [operation-name variable-name]
                                (as-type-description type)))
                    {})))
