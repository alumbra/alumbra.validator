(ns alumbra.validator.analyzed-schema.types.builder
  (:require [alumbra.validator.analyzed-schema.types
             [interface-implemented :as interface-implemented]]))

(defn builder
  []
  {:types [interface-implemented/invariant]})
