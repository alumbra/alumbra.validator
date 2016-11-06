(ns alumbra.validator.utils
  (:require [com.rpl.specter :refer :all]))

(def dfs
  "DFS specter navigator."
  (recursive-path
    [k]
    p
    (cond-path
      map?  (multi-path (must k) [MAP-VALS p])
      coll? [ALL p]
      STAY)))
