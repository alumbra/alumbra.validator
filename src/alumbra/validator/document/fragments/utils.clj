(ns alumbra.validator.document.fragments.utils)

(defn type-name
  "Extract the type name from a fragment spread."
  [fragment]
  (-> fragment :alumbra/type-condition :alumbra/type-name))
