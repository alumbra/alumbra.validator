(ns alumbra.validator.document.context
  (:require [alumbra.validator.document.state :as state]
            [invariant.core :as invariant]))

(defn with-field-context
  [{:keys [type-name fields]} invariant]
  (invariant/with-error-context
    invariant
    (fn [_ {:keys [alumbra/field-name]}]
      {:alumbra/field-name           field-name
       :alumbra/containing-type-name type-name
       :alumbra/valid-field-names    (set (keys fields))})))

(defn with-argument-context
  [invariant]
  (invariant/with-error-context
    invariant
    (fn [_ arg]
      (select-keys arg [:alumbra/argument-name]))))

(defn with-operation-context
  [invariant]
  (invariant/with-error-context
    invariant
    (fn [_ {:keys [alumbra/operation-name
                   alumbra/operation-type]}]
      {:alumbra/operation-name operation-name
       :alumbra/operation-type operation-type})))

(defn with-fragment-context
  [invariant]
  (invariant/with-error-context
    invariant
    (fn [_ frag]
      (merge
        (select-keys frag [:alumbra/fragment-name])
        (some->> frag
                 :alumbra/type-condition
                 :alumbra/type-name
                 (hash-map :alumbra/fragment-type-name))))))

(defn with-directive-context
  [invariant]
  (invariant/with-error-context
    invariant
    (fn [_ dir]
      (select-keys dir [:alumbra/directive-name]))))

(defn with-variable-context
  [invariant]
  (invariant/with-error-context
    invariant
    (fn [state {:keys [alumbra/variable-name] :as var}]
      (merge
        (select-keys var [:alumbra/variable-name])
        (when-let [t (state/variable-type state variable-name)]
          {:alumbra/type-description t})))))
