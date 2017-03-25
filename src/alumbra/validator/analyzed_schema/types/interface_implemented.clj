(ns alumbra.validator.analyzed-schema.types.interface-implemented
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer :all]
            [clojure.set :as set]))

;; Types implementing interfaces need to implement all the fields.
;; TODO: find out whether it's okay to specialize the interface.

(defn- missing-fields
  [{:keys [interfaces]} {:keys [implements fields]}]
  (if-not (empty? implements)
    (let [interface-fields (->> implements
                                (mapcat (comp keys :fields interfaces))
                                (set))
          given-fields (set (keys fields))]
      (set/difference interface-fields given-fields))
    #{}))

(def invariant
  (constantly
    (invariant/property
      :type/implements-all-interface-fields
      (fn [{:keys [state/schema]} type]
        (empty? (missing-fields schema type))))))
