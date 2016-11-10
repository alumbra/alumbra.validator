(ns alumbra.analyzer.types
  (:require [alumbra.analyzer.types
             [arguments :refer [read-arguments]]
             [default-fields :refer [default-type-fields]]
             [type-description :refer [describe-type]]]))

;; TODO (?): This might benefit from using specter.

;; ## Fields

(defn- add-type-fields
  [type-name fields data]
  (reduce
    (fn [data {:keys [alumbra/field-name
                      alumbra/type
                      alumbra/type-field-arguments]}]
      (->> {:field-name field-name
            :containing-type-name type-name
            :arguments (read-arguments type-field-arguments)}
           (merge (describe-type type))
           (assoc-in data [:fields field-name])))
    data fields))

;; ## Interfaces

(defn- add-interfaces
  [data {:keys [alumbra/interface-definitions]}]
  (reduce
    (fn [data {:keys [alumbra/type-name
                      alumbra/type-fields]
               :as interface}]
      (->> {:implemented-by #{}
            :fields         (default-type-fields type-name)
            :type-name      type-name}
           (add-type-fields type-name type-fields)
           (assoc-in data  [:interfaces type-name])))
    data interface-definitions))

(defn- add-implements
  [implementing-type-name
   implements
   {:keys [types interfaces] :as data}]
  (reduce
    (fn [data {:keys [alumbra/type-name]}]
      (cond-> data
        (contains? types implementing-type-name)
        (update-in
          [:types implementing-type-name :implements]
          conj
          type-name)
        (contains? interfaces type-name)
        (update-in
          [:interfaces type-name :implemented-by]
          conj
          implementing-type-name)))
    data implements))

;; ## Types

(defn- add-types
  [data {:keys [alumbra/type-definitions]}]
  (reduce
    (fn [data {:keys [alumbra/type-name
                      alumbra/type-fields
                      alumbra/interface-types]}]
      (->> {:implements #{}
            :fields     (default-type-fields type-name)
            :type-name  type-name}
           (add-type-fields type-name type-fields)
           (assoc-in data [:types type-name])
           (add-implements type-name interface-types)))
    data type-definitions))

;; ## Type Extensions

(defn- extend-types
  [data {:keys [alumbra/type-extensions]}]
  (reduce
    (fn [data {:keys [alumbra/type-name
                      alumbra/type-fields
                      alumbra/interface-types]}]
      (if (get-in data [:types type-name])
        (-> data
            (update-in [:types type-name]
                       #(add-type-fields type-name type-fields %))
            (->> (add-implements type-name interface-types)))
        data))
    data type-extensions))

;; ## Input Types

(defn- add-input-types
  [data {:keys [alumbra/input-type-definitions]}]
  (reduce
    (fn [data {:keys [alumbra/type-name
                      alumbra/input-type-fields
                      alumbra/interface-types]}]
      (->> {:implements #{}
            :fields     {}
            :type-name  type-name}
           (add-type-fields type-name input-type-fields)
           (assoc-in data [:input-types type-name])))
    data input-type-definitions))

;; ## Public API

(defn analyze
  "Analyze the following parts of a GraphQL schema conforming to
   `:alumbra/schema`:

   - interface definitions,
   - type definitions,
   - type extensions,
   - input type definitions."
  [schema]
  (-> {:types       {}
       :input-types {}
       :interfaces  {}}
      (add-interfaces schema)
      (add-types schema)
      (extend-types schema)
      (add-input-types schema)))
