(ns alumbra.validator.analyzed-schema
  (:require [alumbra.validator.analyzed-schema.types.builder :as types]
            [alumbra.validator.analyzed-schema.builder :as builder]))

(defn invariant
  []
  (builder/build
    [(types/builder)]))
