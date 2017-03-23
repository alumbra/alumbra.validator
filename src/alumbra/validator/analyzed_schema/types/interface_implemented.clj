(ns alumbra.validator.analyzed-schema.types.interface-implemented
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

;; Types implementing interfaces need to implement all the fields.
;; TODO: find out whether it's okay to specialize the interface.

(def invariant
  (constantly
    nil))
