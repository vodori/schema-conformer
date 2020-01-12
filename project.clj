(defproject com.vodori/schema-conformer "0.1.0-SNAPSHOT"

  :description
  "A library for configurable conforming of data according to prismatic schemas."

  :url
  "https://github.com/vodori/schema-conformer"

  :license
  {:name "MIT License" :url "http://opensource.org/licenses/MIT" :year 2020 :key "mit"}

  :scm
  {:name "git" :url "https://github.com/vodori/schema-conformer"}

  :pom-addition
  [:developers
   [:developer
    [:name "Paul Rutledge"]
    [:url "https://github.com/rutledgepaulv"]
    [:email "paul.rutledge@vodori.com"]
    [:timezone "-5"]]]

  :deploy-repositories
  {"releases"  {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/" :creds :gpg}
   "snapshots" {:url "https://oss.sonatype.org/content/repositories/snapshots/" :creds :gpg}}

  :dependencies
  [[org.clojure/clojure "1.10.1"]
   [prismatic/schema "1.1.12"]]

  :repl-options
  {:init-ns schema-conformer.core}

  :profiles
  {:test {:dependencies [[clj-time "0.15.2" :scope "test"]]}})
