[![Build Status](https://travis-ci.com/vodori/schema-conformer.svg?branch=master)](https://travis-ci.com/vodori/schema-conformer) [![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/vodori/schema-conformer/maven-metadata.xml.svg)](https://mvnrepository.com/artifact/com.vodori/schema-conformer)


### Problem

We're users of [prismatic/schema](https://github.com/plumatic/schema). Schema is expressive and useful but we always 
find ourselves using custom matchers in order to be liberal in what we accept 
and conservative in what we send. The basic matchers bundled in Schema aren't 
enough.

### Installation 

```clojure
[com.vodori/schema-conformer "0.1.2"]
```

### Usage

```clojure

(require '[schema-conformer.core :as scc]
         '[clojure.pprint :as pprint]
         '[schema.core :as s])

(s/defschema TestSchema
  {:set-of-strings                  #{s/Str}
   :required-field                  [s/Keyword]
   (s/optional-key :optional-field) (s/maybe s/Str)
   :default-value-field             (scc/default s/Str "NOT_FOUND")
   :enum-of-keywords                (s/enum :one :two)})

(def data
  {:set-of-strings   [:example]
   :enum-of-keywords "one"
   :optional-field   nil})

(pprint/pprint (scc/conform TestSchema data))

{:set-of-strings      #{"example"},  ; converted vector to a set, converted keywords to strings
 :required-field      [],            ; added missing required keys, set a missing required collection to empty
 :default-value-field "NOT_FOUND",   ; added missing required key with the default value set by the schema
 :enum-of-keywords    :one}          ; converted the string to a keyword because the enum used a keyword value

```

---

### Options

By default, all coercions are enabled. You can disable specific coercions 
by passing an options map. Available options are:

```clojure
{:align-map-keys      true,
 :constrained->nested true,
 :datetime->string    true,
 :default->nested     true,
 :either->nested      true,
 :enum->nested        true,
 :instant->string     true,
 :integer->datetime   true,
 :integer->instant    true,
 :keyword->string     true,
 :keyword->symbol     true,
 :list->set           true,
 :list->vector        true,
 :nil->abstract       true,
 :nil->extension      true,
 :nil->map            true,
 :nil->set            true,
 :nil->vector         true,
 :number->boolean     true,
 :object-id->string   true,
 :set->vector         true,
 :string->boolean     true,
 :string->datetime    true,
 :string->instant     true,
 :string->keyword     true,
 :string->object-id   true,
 :string->symbol      true,
 :string->uuid        true,
 :symbol->keyword     true,
 :symbol->string      true,
 :uuid->string        true,
 :vector->set         true}
```

___

### Classpath Specific Coercions

If joda time is on your classpath:

- Converting org.joda.time.DateTime to ISO strings
- Converting ISO strings to org.joda.time.DateTime

If mongodb is on your classpath:

- Converting org.bson.types.ObjectId to strings
- Converting strings to org.bson.types.ObjectId

___

### Production Notes

Be sure to create a conformer once for your schema and reuse it for each piece of data
rather than calling conform each time (same as you would with Schema).

---

### Alternatives

- [Malli](https://github.com/metosin/malli) is a nicer data-driven alternative to Schema.

___

### License
This project is licensed under [MIT license](http://opensource.org/licenses/MIT).

