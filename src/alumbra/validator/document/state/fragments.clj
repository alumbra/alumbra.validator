(ns alumbra.validator.document.state.fragments
  (:require [invariant.core :as invariant]
            [com.rpl.specter :refer :all]))

(defn initialize
  [invariant]
  (invariant/as
    invariant
    ::types
    [:alumbra/fragments
     ALL
     (collect-one :alumbra/fragment-name)
     :alumbra/type-condition
     (must :alumbra/type-name)]
    conj {}))
