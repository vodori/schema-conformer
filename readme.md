[![Build Status](https://travis-ci.com/vodori/schema-conformer.svg?branch=master)](https://travis-ci.com/vodori/schema-conformer) [![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/vodori/schema-conformer/maven-metadata.xml.svg)](https://mvnrepository.com/artifact/com.vodori/schema-conformer)


### Problem

We're users of prismatic/schema. Schema is expressive and useful but we always 
find ourselves using custom matchers in order to be liberal in what we accept 
and conservative in what we send. The basic matchers built-in to prismatic/schema 
aren't enough.

### Installation 

```clojure
[com.vodori/schema-conformer "0.1.0"]
```

### Usage

```clojure
(require '[schema-conformer.core :refer :all])

(conform {:test #{s/Str}} {:test [:example]})
=> {:test #{"example"}}


; there's also a default schema type that you can use to 
; set default values during the conforming process

(conform {:test (default s/Keyword :bingo)} {})
=> {:test :bingo}

```

---

### Options

By default, all coercions are enabled. You can disable specific coercions 
by passing an options map. Available options are:

```clojure
{:align-map-keys      true,
 :constrained->nested true,
 :datetime->string    true,
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

### Alternatives

- [Malli](https://github.com/metosin/malli) is a nicer data-driven alternative to prismatic/schema.

___

### License
This project is licensed under [MIT license](http://opensource.org/licenses/MIT).

