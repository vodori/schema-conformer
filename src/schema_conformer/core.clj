(ns schema-conformer.core
  "Be conservative in what you do, be liberal in what you accept from others."
  (:require [schema.utils :as su]
            [schema.coerce :as sc]
            [schema-conformer.transforms :as t]
            [schema.core :as s])
  (:import (java.net URL)
           (java.time Instant)
           (java.util.regex Pattern)))

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


(def VersionNumberPattern #"^\d(\.\d)*$")
(def MD5ChecksumPattern #"^[0-9a-fA-F]{32}$")
(def HexColorPattern #"^#[0-9a-fA-F]{2,6}$")
(def MimeTypePattern #"^[^/]+/[^/]+$")
(def HexStringPattern (Pattern/compile "^[0-9a-f]$" Pattern/CASE_INSENSITIVE))
(def EmailPattern (Pattern/compile "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]+$" Pattern/CASE_INSENSITIVE))
(def HexPattern #"^[0-9a-fA-F]*$")
(def Base64Pattern #"^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$")
(def IPV4AddressPattern #"^([01]?\d\d?|2[0-4]\d|25[0-5])\.([01]?\d\d?|2[0-4]\d|25[0-5])\.([01]?\d\d?|2[0-4]\d|25[0-5])\.([01]?\d\d?|2[0-4]\d|25[0-5])$")
(def DomainNamePattern #"^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}$")

(s/defschema DateTimeString
  (s/both s/Str (s/pred (t/can #(Instant/parse %)) "date-time-string?")))

(s/defschema PrivilegedPort
  (t/within-range 0 1024))

(s/defschema UnprivilegedPort
  (t/within-range 1025 65535))

(s/defschema Port
  (t/within-range 0 65535))

(s/defschema Url
  (s/both s/Str (s/pred (t/can #(URL. ^String %)) "url?")))

(s/defschema PositiveNumber
  (s/both s/Num (s/pred pos? "pos?")))

(s/defschema PositiveInt
  (s/both s/Int (s/pred pos? "pos?")))

(s/defschema NegativeNumber
  (s/both s/Num (s/pred neg? "neg?")))

(s/defschema NegativeInt
  (s/both s/Int (s/pred neg? "neg?")))

(s/defschema NaturalNumber
  (s/both s/Int (s/pred (fn [x] (or (zero? x) (pos? x))) "natural?")))

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