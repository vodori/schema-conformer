(ns schema-conformer.schemas
  (:require [schema.core :as s]
            [schema-conformer.transforms :as t])
  (:import (java.util.regex Pattern)
           (java.net URL)
           (java.time Instant)))

(def VersionNumberPattern #"^\d(\.\d)*$")
(def MD5ChecksumPattern #"^[0-9a-fA-F]{32}$")
(def HexColorPattern #"^#[0-9a-fA-F]{2,6}$")
(def MimeTypePattern #"^[^/]+/[^/]+$")
(def HexStringPattern #"^[0-9a-fA-F]$")
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
