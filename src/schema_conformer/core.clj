(ns schema-conformer.core
  "Be conservative in what you do, be liberal in what you accept from others."
  (:require [schema.utils :as su]
            [schema.coerce :as sc]
            [schema-conformer.transforms :as t]
            [schema.core :as s]))

(defn conformer
  "Creates a function that will conform data to the provided schema."
  ([schema]
   (sc/coercer schema (memoize t/schema-matcher)))
  ([schema options]
   (let [opts (merge t/DEFAULTS options)]
     (sc/coercer schema (memoize #(t/schema-matcher % opts))))))

(defn conform
  "Conforms the given data to the given schema."
  ([schema data]
   ((conformer schema) data))
  ([schema options data]
   ((conformer schema options) data)))

(defn conforms?
  "Checks if the given data can conform to the given schema."
  ([schema data]
   (not (su/error? (conform schema data))))
  ([schema options data]
   (not (su/error? (conform schema options data)))))

(defn default
  "Wraps a schema with a Default record that will result
   in a particular value being 'plugged in' by the conformer
   when not provided by the data."
  [schema default]
  (let [conformed (conform schema default)]
    (assert (not (su/error? conformed)) "Default value does not conform to schema.")
    (t/->Default schema conformed)))

(defn deep-merge
  "Merges two schemas. If the schemas are 'constrained' then
   it combines the maps and applies both constraint conditions."
  [& s]
  (letfn [(combine [a b]
            (cond
              (t/constrained-schema? b)
              (s/constrained
                (combine a (:schema b))
                (:postcondition b)
                (:post-name b))
              (t/constrained-schema? a)
              (s/constrained
                (combine (:schema a) b)
                (:postcondition a)
                (:post-name a))
              (and (t/map-schema? a) (t/map-schema? b))
              (merge-with combine a b)
              :else
              (if (some? b) b a)))]
    (reduce combine {} s)))