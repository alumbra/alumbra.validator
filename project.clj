(defproject alumbra/validator "0.1.0-SNAPSHOT"
  :description "Validator for GraphQL ASTs."
  :url "https://github.com/alumbra/alumbra.validator"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2016
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14" :scope "provided"]
                 [alumbra/spec "0.1.0-SNAPSHOT"]
                 [invariant "0.1.0-SNAPSHOT"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]
                                  [alumbra/parser "0.1.0-SNAPSHOT"]
                                  [alumbra/generators "0.1.0"]]}}
  :pedantic? :abort)
