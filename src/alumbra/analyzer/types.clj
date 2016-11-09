(ns alumbra.analyzer.types)

;; TODO (?): This might benefit from using specter.

;; ## Type

(defn- read-type-name
  [{:keys [graphql/type-class
           graphql/type-name
           graphql/element-type]}]
  (case type-class
    :named-type type-name
    :list-type  (read-type-name element-type)))

(defn- read-type-description
  [{:keys [graphql/type-class
           graphql/non-null?
           graphql/type-name
           graphql/element-type]}]
  (case type-class
    :named-type {:analyzer/non-null? non-null?
                 :analyzer/type-name type-name}
    :list-type {:analyzer/non-null? non-null?
                :analyzer/type-description
                (read-type-description element-type)}))

(defn- read-non-null?
  [{:keys [graphql/non-null?]}]
  non-null?)

(defn- read-type
  [type]
  {:analyzer/type-description (read-type-description type)
   :analyzer/type-name        (read-type-name type)
   :analyzer/non-null?        (read-non-null? type)})

;; ## Arguments

(defn- read-arguments
  [arguments]
  (reduce
    (fn [result {:keys [graphql/argument-name
                        graphql/argument-type]}]
      (->> {:analyzer/argument-name argument-name}
           (merge (read-type argument-type))
           (assoc result argument-name)))
    {} arguments))

;; ## Fields

(defn- add-type-fields
  [type-name fields data]
  (reduce
    (fn [data {:keys [graphql/field-name
                      graphql/type
                      graphql/type-field-arguments]}]
      (->> {:analyzer/field-name field-name
            :analyzer/containing-type-name type-name
            :analyzer/arguments (read-arguments type-field-arguments)}
           (merge (read-type type))
           (assoc-in data [:analyzer/fields field-name])))
    data fields))

;; ## Interfaces

(defn- add-interfaces
  [data {:keys [graphql/interface-definitions]}]
  (reduce
    (fn [data {:keys [graphql/type-name
                      graphql/type-fields]
               :as interface}]
      (->> {:analyzer/implemented-by #{}
            :analyzer/fields         {}
            :analyzer/type-name      type-name}
           (add-type-fields type-name type-fields)
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
            :analyzer/fields     {}
            :analyzer/type-name  type-name}
           (add-type-fields type-name type-fields)
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
      (if (get-in data [:analyzer/types type-name])
        (-> data
            (update-in [:analyzer/types type-name]
                       #(add-type-fields type-name type-fields %))
            (->> (add-implements type-name interface-types)))
        data))
    data type-extensions))

;; ## Input Types

(defn- add-input-types
  [data {:keys [graphql/input-type-definitions]}]
  (reduce
    (fn [data {:keys [graphql/type-name
                      graphql/input-type-fields
                      graphql/interface-types]}]
      (->> {:analyzer/implements #{}
            :analyzer/fields     {}
            :analyzer/type-name  type-name}
           (add-type-fields type-name input-type-fields)
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
  (-> {:analyzer/types       {}
       :analyzer/input-types {}
       :analyzer/interfaces  {}}
      (add-interfaces schema)
      (add-types schema)
      (extend-types schema)
      (add-input-types schema)))
