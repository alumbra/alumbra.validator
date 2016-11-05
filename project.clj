(defproject alumbra/validator "0.1.0-SNAPSHOT"
  :description "Validator for GraphQL ASTs."
  :url "https://github.com/alumbra/alumbra.validator"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2016
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14" :scope "provided"]
                 [invariant "0.1.0-SNAPSHOT"]]
  :profiles {:dev {:dependencies [[alumbra/parser "0.1.0-SNAPSHOT"]]}}
  :pedantic? :abort)
