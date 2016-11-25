(ns alumbra.validator.directives.directives-in-valid-locations
  (:require [alumbra.validator.errors
             :refer [with-directive-context
                     with-fragment-context]]
            [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Formal Specification (5.6.2)
;; ---
;; - For every `directive` in a document.
;; - Let `directiveName` be the name of `directive`.
;; - Let `directiveDefinition` be the directive named `directiveName`.
;; - Let `locations` be the valid locations for `directiveDefinition`.
;; - Let `adjacent` be the AST node the directive affects.
;; - `adjacent` must be represented by an item within `locations`.

(defn- make-invariant
  [{:keys [directives]} expected-location]
  (-> (invariant/on [:alumbra/directives ALL])
      (invariant/each
        (with-directive-context
          (invariant/value
            :directive/location-valid
            (fn [{:keys [alumbra/directive-name]}]
              (let [locs (get-in directives
                                 [directive-name :directive-locations]
                                 ::none)]
                (or (= locs ::none)
                    (contains? locs expected-location)))))))))

(defn invariant
  [expected-location]
  (fn [schema & _]
    (make-invariant schema expected-location)))

(defn fragment-invariant
  [schema & _]
  (-> (invariant/on [ALL])
      (invariant/each
        (with-fragment-context
          (make-invariant schema :fragment-definition)))))

(defn operation-invariant
  [schema & _]
  (let [operation-type->invariant
        (->> ["query" "mutation" "subscription"]
             (map (juxt identity #(make-invariant schema (keyword %))))
             (into {}))]
    (-> (invariant/on [ALL])
        (invariant/each
          (invariant/bind
            (fn [_ {:keys [alumbra/operation-type]}]
              (operation-type->invariant operation-type)))))))
