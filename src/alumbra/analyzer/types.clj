(ns alumbra.analyzer.types)

;; TODO (?): This might benefit from using specter.

;; ## Fields

(defn- read-type-name
  [{:keys [graphql/type-class
           graphql/type-name
           graphql/element-type]}]
  (case type-class
    :named-type type-name
    :list-type  (read-type-name element-type)))

(defn- add-type-fields
  [fields data]
  (reduce
    (fn [data {:keys [graphql/field-name graphql/type]}]
      (assoc-in data
                [:analyzer/fields field-name]
                {:graphql/type-name (read-type-name type)}))
    data fields))

;; ## Interfaces

(defn- add-interfaces
  [data {:keys [graphql/interface-definitions]}]
  (reduce
    (fn [data {:keys [graphql/type-name
                      graphql/type-fields]
               :as interface}]
      (->> {:analyzer/implemented-by #{}
            :analyzer/fields {}}
           (add-type-fields type-fields)
           (assoc-in data  [:analyzer/interfaces type-name])))
    data interface-definitions))

(defn- add-implements
  [implementing-type-name
   implements
   {:keys [analyzer/types analyzer/interfaces] :as data}]
  (reduce
    (fn [data {:keys [graphql/type-name]}]
      (cond-> data
        (contains? types implementing-type-name)
        (update-in
          [:analyzer/types implementing-type-name :analyzer/implements]
          conj
          type-name)
        (contains? interfaces type-name)
        (update-in
          [:analyzer/interfaces type-name :analyzer/implemented-by]
          conj
          implementing-type-name)))
    data implements))

;; ## Types

(defn- add-types
  [data {:keys [graphql/type-definitions]}]
  (reduce
    (fn [data {:keys [graphql/type-name
                      graphql/type-fields
                      graphql/interface-types]}]
      (->> {:analyzer/implements #{}
            :analyzer/fields {}}
           (add-type-fields type-fields)
           (assoc-in data [:analyzer/types type-name])
           (add-implements type-name interface-types)))
    data type-definitions))

;; ## Type Extensions

(defn- extend-types
  [data {:keys [graphql/type-extensions]}]
  (reduce
    (fn [data {:keys [graphql/type-name
                      graphql/type-fields
                      graphql/interface-types]}]
      (-> data
          (update-in [:analyzer/types type-name]
                     #(add-type-fields type-fields %))
          (->> (add-implements type-name interface-types))))
    data type-extensions))

;; ## Input Types

(defn- add-input-types
  [data {:keys [graphql/input-type-definitions]}]
  (reduce
    (fn [data {:keys [graphql/type-name
                      graphql/input-type-fields
                      graphql/interface-types]}]
      (->> {:analyzer/implements #{}
            :analyzer/fields {}}
           (add-type-fields input-type-fields)
           (assoc-in data [:analyzer/input-types type-name])))
    data input-type-definitions))

;; ## Public API

(defn analyze
  "Analyze the following parts of a GraphQL schema conforming to
   `:graphql/schema`:

   - interface definitions,
   - type definitions,
   - type extensions,
   - input type definitions."
  [schema]
  (-> {:analyzer/types {}
       :analyzer/input-types {}
       :analyzer/interfaces {}}
      (add-interfaces schema)
      (add-types schema)
      (extend-types schema)
      (add-input-types schema)))
