## Status

* [![Build Status](https://ci.usethesource.io/job/usethesource/job/vallang/job/master/badge/icon)](https://ci.usethesource.io/job/usethesource/job/vallang/job/master/)
 
## What is Vallang?

**Vallang** is a highly integrated and mostly-closed collection of mutually recursive fundamental data-types on the Java Virtual Machine:

* locations represented by URIs: `|java+class://java/lang/String|` and `|file:///tmp/HelloWorld.java|`
* integers of arbitrary size: `1,2,3, 134812345123841234`
* reals of arbitrary size, precision and scale: `1., 1.0, 1e10`
* rational numbers: `1r1, 1r7`
* unicode strings: `"hello 🌐"`
* lists: `[1,2, 1.0, "hello 🌐"]`, `[]`
* sets: `{1,2, 1.0, "hello 🌐"}`, `{}`
* maps: `(1:0, "a":"b")`, `()`
* n-ary tuples with named fields: `<1,2,"a",1.0>`, `<>`
* n-ary relations (represented as sets of n-ary tuples): `{<1,2,"a",1.0>, <>}`
* tree nodes: `"myNode"(1,2,3)`
* many-sorted algebraic terms, acting as typed tree nodes: `myNode(1,2,3)`.
* keyword fields or properties to tree nodes and algebraic data-types: `"myNode"(name="Winston")`, `myNode(age=12)`

Operations on these data-types are too many to list here. A selection is listed below, but you should expect the features to be pretty low level; i.e. directly accessing and manipulating the data rather than providing analysis algorithms. Algorithms in the library are added only if programming them below the abstraction layer of **vallang** provides a major efficiency benefit or it can factors out highly common client code into a reusable feature. More on this design decision later. 

* relational calculus operators such as transitive (reflexive) closure, query and projections, compositions and joins
* generic tree traversal and primitives for implementing pattern matching

**Vallang** has a type system based on type systems in functional programming, but note that each value has a most specific dynamic type associated always at run-time. More on the design of the type system below, but here is a list of **vallang** types:

* `void` - the bottom type with no values
* `value` - the top type for all values
* `loc` - the type for URI locations
* `int`, `real`, `rat` are all sub-types of the aggregate type `num`
* `tuple[t1,...,tn]` and `tuple[t1 l1, ..., tn ln]` to represent tuples of fixed but arbitrary arity
* `list[t]`, `set[t]`, `map[t1,t2]` as incomparable alternative collection types.
* `node` for trees
* user-defined many-sorted mutually recursive algebraic data-types, acting as grammars for instances of tree nodes: `data MyADT = myNode(int a, int b, int c, int age=...)`
* alias types for short-handed type equivalences
* `rel[t1, ..., tn]` is an alias for `set[tuple[t1,...tn]]`
* open type parameters with upper bounds, `&T <: node`, can be used to type parameterize composite types (used in type aliases) and to construct higher-order abstract algebraic datatypes.

Sub-typing is _co-variant_ for the container types `list`, `map`, `set` and `tuple`. Otherwise these rules define the entire type system:

* `value` is a strict supertype of all other types other than itself
* `node` is the common supertype of all algebraic data-types
* each algebraic constructor is a strict sub-type of its abstract sort
* `void` is the sub-type of all types
* `num` is the supertype of `rat`, `int` and `real`
* an alias `alias X = Y` is a type equivalence
* constructor types are sub-types if they have the same name and arity, and they are comparable in their argument types. 
   * Within a single abstract sort no two alternative constructors may be sub-types of each other. 

There exists an extension mechanism for adding type kinds and their associated value kinds to the **vallang** system. Rascal, for example, uses this to represent functions and co-routines at run-time. The extension mechanism works by declaring a bi-directional transformation between the extensions and a symbolic representation of choice (chosen freely from the core representation mechanisms of **vallang**). This bidirectional mapping is mostly used when serializing and deserializing values (see below).

The types of **vallang** in Java are represented by a Composite design pattern with maximally shared instances of (the otherwise opaque) abstract class `Type`. These types expose fast implementations of sub-type and type equivalence for implementing fast pattern matching. 

The values of **vallang** are all instances of `IValue` and sub-interfaces thereof. For every kind of value there is an interface, e.g. `ISet`, `IList`, `ITuple` and `IConstructor` but they are not type-parametrized because Java's type system can not represent the aforementioned co-variant sub-typing rules we require. 

## Why does Vallang exist?

**vallang** is a [UseTheSource](http://www.usethesource.io) project recently renamed from **rascal-values**, which was known earlier as **pdb.values**. 

The project started as a part of the [IDE metatooling platform](http://homepages.cwi.nl/~jurgenv/papers/OOPSLA-2009.pdf) in 2007 as a *generic library for representing symbolic facts about source code*, for use in the construction of IDE services in Eclipse, then it continued to form the basis of the run-time system for [Rascal](http://www.rascal-mpl.org) starting 2009, and finally was renamed to **vallang** to serve a wider scope.

We designed of **vallang** based on experience with and studying the [ATerm library](http://www.meta-environment.org/Meta-Environment/ATerms.html) and [ASF+SDF](http://www.meta-environment.org/Meta-Environment/WebHome.html), but also by learning from RSF (Rigi Standard Format), Rscript and GXL and S-expressions. Perhaps JSON and YAML have also had a minor influence.

> The main purpose of **vallang** is to provide a flexible and fully typed collection of _symbolic_ representations of data, specifically "ready" to represent facts about software systems but amenable to basically any form of symbolic data analysis purposes.

This purpose aligns with the mission of the [Rascal metaprogramming language](http://www.rascalmpl.org) which is made to _analyze_ and _manipulate_ exactly such symbolic representations. Therefore **vallang** is the run-time environment for both interpreted and compiled Rascal programs. 

Note that while **vallang** is a great fit for symbolic data analysis, it is currently not the best fit for numerical data analysis as it features only a uniform symbolic represetation of numbers of arbitrary precision and size (ints, reals, rationals). In other words, the numbers and collections of numbers in **vallang** are optimized for storage size, clarity and equational reasoning rather than optimal computational efficiency. This also means that indirect numerical encodings of data (i.e. using numerical vectors and matrices), which are often used in symbolic analyses to optimize computational efficiency are not the right strategy when using **vallang**: it's better to stick with a more direct symbolic representation and let **vallang** maintainers optimize them. 

Next to the maintainers of Rascal, the main users of **vallang** are currently programmers who write data acquisition and (de)serialisation adapters for the Rascal ecosystem:

* connecting open-compiler front-ends to Rascal
* providing external data-sources such as SQL and MongoDB databases
* connecting reusable interaction and visualization front-ends to Rascal

> Nevertheless **vallang** is a generic and Rascal-independent library which may serve as the run-time system for other programming languages or analysis systems, such as term rewriting systems, relational calculus systems, constraint solvers, model checkers, model transformations, etc.

The latter perspective is the reason for the re-branding of **rascal-values** to **vallang**. You might consider **vallang** as a functional replacement for ECore, an alternative to the ATerm library on the JVM, or an alternative to JSON-based noSQL in-memory database systems, or a way of implementing graph databases. 

Finally, **vallang** is a JVM library because that is where we needed it for Rascal and the Eclipse IDE Metatooling Platform. We hope other JVM programmers will also benefit from it and we have no plans of porting it at the moment to any other technological space. 


## What are the main design considerations of Vallang?

#### Vallang values are symbolic and immutable.

We think software analysis is complex enough to be confusing to even the most experienced programmers. Manipulating huge stores of hierarchical and relational data about software easily goes wrong; trivial bugs due to aliasing and sharing data between different stages of an analysis or transformation can take weeks to resolve, or worse: will never even be diagnosed. 

Since our goal is to provide many more variants of all kind of software analyses, we wish to focus on the interesting algorithmic details rather than the trivial mistakes we make. Therefore, **vallang** values are _immutable_. Sharing of values or parts of values is allowed under-the-hood but is not observable. The library is implemented using persistent and/or maximally shared data structures for reasons of efficiency. 

Users of **vallang** freely share references to their data to other parts of an analysis because they know the data can not change due to an unforeseen interaction. We also believe that the immutable values can be shared freely between threads on the JVM, but there are not enough tests yet to make such a bold claim with full confidence. 

#### Vallang values are generated via the AbstractFactory design pattern and do not leak implementation representations

The reason is that client code _must_ abstract from the implementation details to arrive at the mathematical precision of symbolic reasoning which **vallang** should provide.

This also serves a maintenance and evolution purpose for implementations of the library. We can plug in a new implementation of the library without affecting client code.

Note that for efficiency reasons values produced from different implementations of an abstract value factory (different implementations of `IValueFactory`) are not required to interact correctly.

#### Vallang values uniquely deserialize/serialize from/to a standard and simple expression language

The general rule is that for any two JVM object reference `o` and `p` to any **vallang** object the following rule holds: `o.toString().equals(p.toString) <==> o.equals(p)`

We currently random test this rule and it sometimes fails due to a deprecated feature called "annotations" which we are removing to make the above contract true.

The intended effects of the toString/equals contract of **vallang** are the following:

* What-you-see-is-what-you-get: debugging values by printing them means that you get as a programmer full disclosure about the meaning of the object
* Structural equality and equational reasoning: the context in which values are created can not have any influence on their identity
* Sharing is safe
* Serialisation and deserialisation is never lossy
* The sub-type relation for types of values coincides exactly with sublanguage concept of the set of sentences for all values of the given types. 

The latter point is one of the main reasons why **vallang** is called a **lang**uage. The result of `anyValue.toString()` is a member of a precisely defined textual language. The full textual language is generated from the `value` type, and sub-languages are generated from the respective sub-types. `void` is the empty language. In this manner the types of **vallang** act like non-terminals of a precise context-free grammar. The **vallang** language as defined above is a strict sub-language of the `Expression` sub-language of Rascal.

The other reason why **vallang** is names as a language is because the implementations of the `IValue` interface and its sub-interfaces are seen as a closed combinator language for computations on the values, and their implementations are interpreters for this language. 

#### Vallang values always know their most-precise concrete ad-hoc run-time type

* This is nice for debugging purposes, the types are descriptions of values and if matching or equality checking fails then the type abstraction usually explains why without having to go into reading the entire value.
* Types may be computed lazily or eagerly, cached or not. This is not functionally observable but it may affect run-time efficiency
* Having precise run-time types for every (nested) value, and having efficient access to this, is a prerequisite for building fast and type-safe rank-2 polymorphic higher order functional computations. Or in functional terms: you need this to make folds and maps work on heterogenous recursive and open data-types. Or in simpler terms: using this we can build statically type-safe data traversal and data transformation features into Rascal.

#### Vallang values include both trees and relations

Even though both trees and relations are generic enough to represent any data, sometimes a graph or table is more natural than a tree and sometimes the other way around.

* trees are nice for abstract and concrete syntax representations
* trees are nice for abstract symbolic domains, such as terms for constraint variables and binary constraints
* relations are nice for graph-like unstructured data, such as project dependencies, call graphs, etc.
* relations are nice for access to external data stored in spreadsheets and databases
* trees are nice for access to web data stored in HTML, XML, JSON formats etc.
* trees are good for transformation purposes, where we parse something, rewrite it and unparse it again
* relations are good for analysis purposes, where we extract facts, elaborate on them and finally report the result.

Rascal is a language which can be used to easily switch between different representations of the same information, using pattern matching, querying, comprehensions, etc. From **vallang** you should not expect any help in this regard: the choice of representation for any information is a key design decision for the user of **vallang**.

#### Vallang supports query and (persistent) updates to all symbolic values efficiently

#### Vallang equality checking is fast

## Who contributed to Vallang?

* Robert M. Fuhrer (IBM TJ Watson)
* Jurgen J. Vinju (IBM TJ Watson and Centrum Wiskunde & Informatica)
* Arnold Lankamp (Centrum Wiskunde & Informatica)
* Anya Helene Bagge (University of Bergen)
* Michael Steindorfer (Centrum Wiskunde & Informatica and TU Delft)
* Davy Landman (Centrum Wiskunde & Informatica and SWAT.engineering)
* Paul Klint (Centrum Wiskunde & Informatica)

and occasional contributions from others please see [github's factual overview](https://github.com/usethesource/rascal-value/graphs/contributors)

## What is in the near future for Vallang?

1. Removal of the "annotations" feature, which is completely replaces by the "keyword fields" feature. The main differences between these features are:
   * While they both offer extensibility to the set of names and typed fields of nodes and constructors, annotations can never influence `equals()` while keyword fields always do. 
   * Syntactically the notation for keyword fields is more compact: `f()[@myAnno=1]` versus `f(myField=1)`
2. Further integration of the capabilities of [Capsule](http://www.usethesource.io/projects/capsule) for persistent and optimized immutable collections under the hood of `IMap`, `ISet`, `IRelationAlgebra`:
   * Reflexive relations with two indices (for both columns)
   * Heterogeneous collections of numbers (unboxing down to primitive types to safe space)
   * Smooth and incremental transitions from map to multimap representations
3. `IBag`, the `bag[&T]` type
