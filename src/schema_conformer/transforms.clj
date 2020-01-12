(ns schema-conformer.transforms
  (:require [clojure.string :as strings]
            [clojure.set :as sets]
            [schema.core :as s]
            [schema.utils :as su]
            [schema.coerce :as sc]
            [schema.experimental.abstract-map :as sam])
  (:import (clojure.lang Reflector Keyword Symbol)
           (schema.core Constrained OptionalKey RequiredKey EnumSchema Either)
           (java.util UUID)
           (java.time Instant)
           (schema.experimental.abstract_map AbstractSchema SchemaExtension)))

(defn date-time-schema? [s]
  (and (class? s) (= "org.joda.time.DateTime" (.getName ^Class s))))

(defn instant-schema? [s]
  (and (class? s) (= "java.time.Instant" (.getName ^Class s))))

(defn object-id-schema? [s]
  (and (class? s) (= "org.bson.types.ObjectId" (.getName ^Class s))))

(defn within-range [start stop]
  (let [pred (fn [x] (<= start x stop))]
    (s/both s/Int (s/pred pred (format "(<= %d x %d)" start stop)))))

(defn can [f]
  (fn [& args]
    (try
      (boolean (apply f args))
      (catch Exception e false))))

(defn construct [class & args]
  (when (some? (Class/forName class))
    (Reflector/invokeConstructor
      (resolve (symbol class)) (to-array args))))

(defn uuid-schema? [s]
  (or (= UUID s) (= s/Uuid s)))

(defn boolean-schema? [s]
  (or (= Boolean s)
      (= s/Bool s)
      (= (s/enum true false) s)))

(defn date-time? [v]
  (date-time-schema? (class v)))

(defn instant? [v]
  (instant-schema? (class v)))

(defn object-id? [v]
  (object-id-schema? (class v)))

(defn uuid->string [s]
  (str s))

(defn string->uuid [s]
  (UUID/fromString s))

(defn string->instant [s]
  (Instant/parse s))

(defn number->instant [s]
  (Instant/ofEpochMilli s))

(defn number->datetime [s]
  (construct "org.joda.time.DateTime" s))

(defn instant->string [s]
  (str s))

(defn lazy [sym]
  (delay (var-get (requiring-resolve sym))))

(let [parse-fn      (lazy 'clj-time.format/parse)
      unparse-fn    (lazy 'clj-time.format/unparse)
      iso-formatter (lazy 'clj-time.format/formatters)]
  (defn string->datetime [s]
    ((force parse-fn) (:date-time (force iso-formatter)) s))
  (defn datetime->string [dt]
    ((force unparse-fn) (:date-time (force iso-formatter)) dt)))

(defn string-schema? [s]
  (or (= String s) (= s/Str s)))

(defn enum-schema? [s]
  (instance? EnumSchema s))

(defn symbol-schema? [s]
  (or (= Symbol s) (= s/Symbol s)))

(defn keyword-schema? [s]
  (or (= Keyword s) (= s/Keyword s)))

(defn set-schema? [s]
  (set? s))

(defn vector-schema? [s]
  (vector? s))

(defn constrained-schema? [s]
  (instance? Constrained s))

(defn either-schema? [s]
  (instance? Either s))

(defn abstract? [s]
  (instance? AbstractSchema s))

(defn extension? [s]
  (instance? SchemaExtension s))

(defn schema-record? [s]
  (let [clazz (.getName (class s))]
    (or (strings/starts-with? clazz "schema.core.")
        (strings/starts-with? clazz "schema.experimental."))))

(defn map-schema? [s]
  (and (map? s) (not (schema-record? s))))

(defn object-id->string [o]
  (.toHexString o))

(defn string->object-id [s]
  (construct "org.bson.types.ObjectId" s))

(defn optional-key? [s]
  (instance? OptionalKey s))

(defn required-key? [s]
  (instance? RequiredKey s))

(defn allow-arbitrary-keys? [schema]
  (or
    (contains? schema s/Str)
    (contains? schema s/Keyword)
    (contains? schema String)
    (contains? schema Keyword)))

(defn get-required-keys [schema]
  (cond-> #{}
    (or (string? schema) (keyword? schema))
    (conj schema)
    (required-key? schema)
    (conj (:k schema))))

(defn get-optional-keys [schema]
  (cond-> #{}
    (optional-key? schema)
    (conj (:k schema))
    (or (instance? EnumSchema schema))
    (sets/union (set (:ks schema)))))

(defn ensure-required-keys [m required]
  (reduce (fn [m' k] (update m' k identity)) m required))

(defn remove-additional-keys [m allowed]
  (reduce-kv (fn [m' k _] (if (contains? allowed k) m' (dissoc m' k))) m m))

(defn remove-optional-nil-keys [m optional]
  (reduce-kv (fn [m' k v] (if (and (contains? optional k) (nil? v)) (dissoc m' k) m')) m m))

(defn align-map-keys [schema x]
  (let [optional-keys  (set (mapcat get-optional-keys (keys schema)))
        required-keys  (set (mapcat get-required-keys (keys schema)))
        available-keys (sets/union optional-keys required-keys)]
    (cond-> x
      :always
      (ensure-required-keys required-keys)
      (not (allow-arbitrary-keys? schema))
      (remove-additional-keys available-keys)
      :always
      (remove-optional-nil-keys optional-keys))))

(defmacro lift [transform]
  `(fn [_# v#] (~transform v#)))

(declare conform)

(defn align-map [schema x]
  (let [result (align-map-keys schema x)]
    (if (= x result) result (conform schema result))))

(defn string->boolean [s]
  ({"true" true "false" false}
   (strings/lower-case s)
   s))

(defn number->boolean [x]
  ({1 true 0 false} (int x) x))

(defn defaulting [v]
  (fn [schema x] (conform schema v)))

(defn constrained [schema x]
  (conform (:schema schema) x))

(defn either [schema x]
  (or (->> (get schema :schemas)
           (map (fn [s] (conform s x)))
           (remove su/error?)
           (first))
      x))

(defn enumeration [schema x]
  (or (->> (get schema :vs)
           (map (fn [s] (conform (class s) x)))
           (remove su/error?)
           (first))
      x))

(defn nil->abstract-schema [{:keys [dispatch-key] :as schema} x]
  (conform schema {dispatch-key nil}))

(defn nil->extension [{:keys [base-schema] :as schema} x]
  (let [dispatch       (:dispatch-key base-schema)
        table          (->> @(:sub-schemas base-schema)
                            (map (comp vec reverse))
                            (into {}))
        dispatch-value (get table schema)]
    (conform schema {dispatch dispatch-value})))

(def MAPPINGS
  [[string-schema?
    [{:key :keyword->string :default true :value? keyword? :transform (lift name)}
     {:key :symbol->string :default true :value? symbol? :transform (lift name)}
     {:key :uuid->string :default true :value? uuid? :transform (lift uuid->string)}
     {:key :datetime->string :default true :value? date-time? :transform (lift datetime->string)}
     {:key :instant->string :default true :value? instant? :transform (lift instant->string)}
     {:key :object-id->string :default true :value? object-id? :transform (lift object-id->string)}]]
   [enum-schema?
    [{:key :enum->nested :default true :value? any? :transform enumeration}]]
   [either-schema?
    [{:key :either->nested :default true :value? any? :transform either}]]
   [abstract?
    [{:key :nil->abstract :default true :value? nil? :transform nil->abstract-schema}]]
   [extension?
    [{:key :nil->extension :default true :value? nil? :transform nil->extension}]]
   [boolean-schema?
    [{:key :string->boolean :default true :value? string? :transform (lift string->boolean)}
     {:key :number->boolean :default true :value? integer? :transform (lift number->boolean)}]]
   [keyword-schema?
    [{:key :string->keyword :default true :value? string? :transform (lift keyword)}
     {:key :symbol->keyword :default true :value? symbol? :transform (lift keyword)}]]
   [uuid-schema?
    [{:key :string->uuid :default true :value? string? :transform (lift string->uuid)}]]
   [set-schema?
    [{:key :list->set :default true :value? list? :transform (lift set)}
     {:key :vector->set :default true :value? vector? :transform (lift set)}
     {:key :nil->set :default true :value? nil? :transform (defaulting #{})}]]
   [vector-schema?
    [{:key :set->vector :default true :value? set? :transform (lift vec)}
     {:key :list->vector :default true :value? list? :transform (lift vec)}
     {:key :nil->vector :default true :value? nil? :transform (defaulting [])}]]
   [date-time-schema?
    [{:key :string->datetime :default true :value? string? :transform (lift string->datetime)}
     {:key :integer->datetime :default true :value? integer? :transform (lift number->datetime)}]]
   [instant-schema?
    [{:key :string->instant :default true :value? string? :transform (lift string->instant)}
     {:key :integer->instant :default true :value? integer? :transform (lift number->instant)}]]
   [object-id-schema?
    [{:key :string->object-id :default true :value? string? :transform (lift string->object-id)}]]
   [symbol-schema?
    [{:key :keyword->symbol :default true :value? keyword? :transform (lift symbol)}
     {:key :string->symbol :default true :value? string? :transform (lift symbol)}]]
   [constrained-schema?
    [{:key :constrained->nested :default true :value? any? :transform constrained}]]
   [map-schema?
    [{:key :nil->map :default true :value? nil? :transform (fn [schema _] (conform schema {}))}
     {:key :align-map-keys :default true :value? map? :transform align-map}]]])

(def DEFAULTS
  (into {} (for [[_ transforms] MAPPINGS
                 {:keys [key default]
                  :or   {default true}} transforms]
             [key default])))


(def ^:dynamic *options* DEFAULTS)

(defn schema-matcher
  ([schema]
   (letfn [(reducer [res {:keys [value? transform]}]
             (if (value? res) (reduced (transform schema res)) res))]
     (loop [[[test transforms] & more] MAPPINGS]
       (if (test schema)
         (let [applicable (filter (comp *options* :key) transforms)]
           (when (not-empty applicable)
             #(reduce reducer % applicable)))
         (when (not-empty more)
           (recur more))))))
  ([schema options]
   (binding [*options* options]
     (when-some [match (schema-matcher schema)]
       (fn [x] (binding [*options* options] (match x)))))))

(defn conformer
  ([schema]
   (sc/coercer schema (memoize schema-matcher)))
  ([schema options]
   (sc/coercer schema (memoize #(schema-matcher % options)))))

(defn conform
  ([schema data]
   ((conformer schema) data))
  ([schema options data]
   ((conformer schema options) data)))