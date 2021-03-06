(defproject alumbra/validator "0.2.2-SNAPSHOT"
  :description "Validator for GraphQL ASTs."
  :url "https://github.com/alumbra/alumbra.validator"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2016
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14" :scope "provided"]
                 [alumbra/spec "0.1.6" :scope "provided"]
                 [com.stuartsierra/dependency "0.2.0"]
                 [invariant "0.1.4"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]
                                  [alumbra/analyzer "0.1.9"]
                                  [alumbra/parser "0.1.6"]
                                  [alumbra/generators "0.2.2"]]}}
  :pedantic? :abort)
