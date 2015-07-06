About
=====

Pdb.values contains the definition of interfaces and several alternative implementations
for representing facts about source code. Such facts need to be extracted first from source code,
then they are stored as PDB "Values". Any kind of analysis can then enrich these facts before
they are presented to the user in the Eclipse IDE, or used in pre-condition checking for source-to-source
transformations such as refactorings.

Design principles
=================

* Values are immutable. This is to prevent aliasing and related issues, to allow reuse between different
  parts of an analysis without interference and to allow certain optimization such as maximal sub-term sharing.
* Those values which are collections (`list`, `set`, ...) can be constructed using "writer" interfaces. A writer
  provides a mutable interface that usually allows insertion and deletion of elements. Writer objects should
  not be passed on easily. Writer objects can not be embedded into other values. A real value can be
  obtained by calling the `done()` method which returns an immutable full fledged value.
* Values are typed. I.e. all values carry a type which documents WHAT fact it represents and HOW it
  can be manipulated.
* The kinds of values are: unlimited size ints and reals, booleans, n-ary relations, sets, maps, tuples,
  untyped trees, and typed trees (abstract data types).
* What-you-see-is-what-you-get: each value has a unique textual representation, so does its type.
* Values can be implemented in different ways. There is not a single data-structure that performs
  optimal for all kinds of analysis. Therefore, pdb.values is based on the `AbstractFactory` design pattern.
* There is a sub-typing relation between the types of values. Each type represents a (possibly infinite)
  set of values. The sub-typing relation coincides with the sub-set relation. For some types that
  are parametrized, like `list` and `set`, the sub-typing relation is *co-variant*. The type hierarchy has
  `value` at the top (the set of all values) and `void` at the bottom (the empty set of values).

NOTE: values from different factories are currently not interchangeable.

IO
==

* Values can be written and read using the `IValueReader` and `IValueWriter` interfaces. Different implementations
should provide mappings to existing and new serialized formats.
* It should be noted that while de-serializing the types of the values are validated explicitly.

Implementation and usage
========================

There is one `TypeFactory`. There can be only one `TypeFactory`. It is a singleton. Use it to construct types.

From a specific type one can construct a value using its respective "make" methods. Each make method takes
an `IValueFactory` as an argument, and some other values if needed. Each `IValueFactory` has a make methods
for all kinds of values that can be constructed. Values can thus also be constructed using an `IValueFactory`
directly.

Each value implements the `IValue` interface. Then, for each kind of value there is an interface, like
`ISet`, `IRelation`, `ITuple`, `IList`, etc. Collection values each have an associated
writer interface, i.e. `IListWriter`, `ISetWriter`, `IRelationWriter` and `IMapWriter`.

There are 3 different implementations of `IValueFactory` currently present in pdb.values, with their respective
implementations of the `IValue` interfaces:

  1. The "reference" implementation. It uses the Java standard library to implement all interface as
     simply as possible.
  2. The "shared" implementation. This implements a form of maximal sub-term sharing that in some cases
     is beneficial for speed.
  3. The "fast" implementation. This uses all the tricks to make things fast.

Needless to say, we are aiming for more implementations, such as a "streamed" implementation which keeps
most facts on disk while computing. Also, specialized implementations for particular combinations like
set of integers or binary relations of integers are candidates. Please let us know if you are willing
to contribute.

Authors
=======

  * Bob Fuhrer (IBM TJ Watson)
  * Jurgen Vinju (CWI)
  * Arnold Lankamp (CWI)
  * Michael Steindorfer (CWI)
