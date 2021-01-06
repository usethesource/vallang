/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation, 2009-2015 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
 *    Jurgen Vinju - initial API and implementation
 *******************************************************************************/

package io.usethesource.vallang.type;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.checkerframework.dataflow.qual.Pure;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.exceptions.IllegalOperationException;

/**
 * This class is the abstract implementation for all types. Types are ordered in
 * a partially ordered type hierarchy with 'value' as the largest type and
 * 'void' as the smallest. Each type represents a set of values.
 * <p>
 * Users of this API will generally use the interface of @{link Type} and
 * {@link TypeFactory}, the other classes in this package are not part of the
 * interface. To construct {@link IValue}'s, use the 'make' methods of
 * 
 * @{link Type}.
 *        <p>
 *        Technical detail: since void is a sub-type of all types and alias
 *        types may be sub-types of any types, a Java encoding of the hierarchy
 *        using single inheritance will not work. Therefore, all methods of all
 *        types are present on this abstract class Type. void and alias type
 *        implement all methods, while the other methods implement only the
 *        relevant methods. Calling a method that is not present on any of the
 *        specific types will lead to a @{link FactTypeError} exception.
 */
public abstract class Type implements Iterable<Type>, Comparable<Type> {
  protected static final TypeFactory TF = TypeFactory.getInstance();
  
  public abstract TypeFactory.TypeReifier getTypeReifier();
  
  /**
   * Retrieve the type of elements in a set or a relation.
   * 
   * @return type of elements
   */
  public Type getElementType() {
    throw new IllegalOperationException("getElementType", this);
  }

  /**
   * Retrieve the key type of a map
   * 
   * @return key type
   */
  public Type getKeyType() {
    throw new IllegalOperationException("getKeyType", this);
  }

  /**
   * Retrieve the value type of a map
   * 
   * @return value type
   */
  public Type getValueType() {
    throw new IllegalOperationException("getValueType", this);
  }

  /**
   * Retrieve the name of a named type, a tree node type or a parameter type.
   * 
   * @return name of the type
   */
  public String getName() {
    throw new IllegalOperationException("getName", this);
  }

  /**
   * Retrieve the type of a field of a tuple type, a relation type or a tree
   * node type.
   * 
   * @param i
   *          index of the field to retrieve
   * @return type of the field at index i
   */
  public Type getFieldType(int i) {
    throw new IllegalOperationException("getFieldType", this);
  }

  /**
   * Retrieve the type of a field of a tuple type, a relation type or a tree
   * node type.
   * <p>
   * 
   * @param fieldName
   *          label of the field to retrieve
   * @return type of the field at index i
   * @throws FactTypeUseException
   *           when the type has no field labels (tuples and relations
   *           optionally have field labels).          
   */
  public Type getFieldType(String fieldName) throws FactTypeUseException {
    throw new IllegalOperationException("getFieldType", this);
  }

  /**
   * Retrieve the field types of a tree node type or a relation, represented as
   * a tuple type.
   * 
   * @return a tuple type representing the field types
   */
  public Type getFieldTypes() {
    throw new IllegalOperationException("getFieldTypes", this);
  }

  /**
   * Retrieve the field name at a certain index for a tuple type, a relation
   * type or a tree node type.
   * 
   * @param i
   *          index of the field name to retrieve
   * @return the field name at index i
   * @throws FactTypeUseException
   *           when this type does not have field labels. Tuples and relations
   *           optionally have field labels.
   */
  public String getFieldName(int i) {
    throw new IllegalOperationException("getFieldName", this);
  }

  /**
   * Retrieve the field name at a certain index for a tuple type, a relation
   * type or a tree node type.
   * 
   * @param i index of the field name to retrieve
   * @return the field name at the given index, optionally.
   */
  public Optional<String> getOptionalFieldName(int i) {
    if (hasFieldNames()) {
      return Optional.of(Objects.requireNonNull(getFieldName(i)));
    } else {
      return Optional.empty();
    }
  }

  /**
   * Retrieve all the field names of tuple type, a relation type or a tree node
   * type.
   * 
   * @return the field name at index i
   * @throws FactTypeUseException
   *           when this type does not have field labels. Tuples and relations
   *           optionally have field labels.
   */
  @Pure
  public String[] getFieldNames() {
    throw new IllegalOperationException("getFieldNames", this);
  }

  /**
   * Retrieve a field index for a certain label for a tuple type, a relation
   * type or a tree node type.
   * 
   * @param fieldName
   *          name of the field to retrieve
   * @return the index of fieldName
   */
  public int getFieldIndex(String fieldName) {
    throw new IllegalOperationException("getFieldIndex", this);
  }

  /**
   * @param fieldName
   *          name of the field to check for
   * @return true iff this type has a field named fieldName
   */
  public boolean hasField(String fieldName) {
    throw new IllegalOperationException("hasField", this);
  }

  /**
   * @param fieldName
   *          name of the field to check for
   * @return true iff this type has a field named fieldName
   */
  public boolean hasField(String fieldName, TypeStore store) {
    return hasField(fieldName);
  }
  
  /**
   * @param fieldName
   *          name of the keyword field to check for
   * @return true iff this type has a keyword field named fieldName
   */
  public boolean hasKeywordField(String fieldName, TypeStore store) {
	  throw new IllegalOperationException("hasKeywordField", this);
  }

  /**
   * Retrieve the width, a.k.a. arity, of a tuple, a relation or a tree node
   * type.
   * 
   * @return the arity
   */
  public int getArity() {
    throw new IllegalOperationException("getArity", this);
  }
  
  /**
   * Represent this type as a value of the abstract data-type "Symbol". As a side-effect
   * it will also add Production values to the grammar map, including all necessary productions
   * to build values of the receiver type, transitively.
   * 
   * @param  vf valuefactory to use 
   * @param store store to lookup additional necessary definitions in to store in the grammar
   * @param grammar map to store production values in as a side-effect
   * @param done a working set to store data-types which have been explored already to avoid infinite recursion
   * @return a value to uniquely represent this type.
   */
  public IConstructor asSymbol(IValueFactory vf, TypeStore store, ISetWriter grammar, Set<IConstructor> done) {
	  return getTypeReifier().toSymbol(this, vf, store, grammar, done);
  }

  /**
   * Map the given typestore to a set of production values, with only definitions
   * reachable from the receiver type
   * 
   * @param  vf valuefactory to use 
   * @param  store typestore which contains source definitions
   * @param done a working set to store data-types which have been explored already to avoid infinite recursion
   */
  public void asProductions(IValueFactory vf, TypeStore store, ISetWriter grammar, Set<IConstructor> done) {
	  getTypeReifier().asProductions(this, vf, store, grammar, done);
  }

  
  /**
   * Compose two binary tuples or binary relation types.
   * 
   * @param other
   * @return a new type that represent the composition
   * @throws IllegalOperationException
   *           if the receiver or the other is not binary or if the last type of
   *           the receiver is not comparable to the first type of the other.
   */
  public Type compose(Type other) {
    throw new IllegalOperationException("compose", this, other);
  }

  /**
   * For relation types rel[t_1,t_2] this will compute rel[t_3,t_3] where t_3 =
   * t_1.lub(t_2).
   * 
   * @return rel[t_3,t_3]
   * @throws IllegalOperationException
   *           when this is not a binary relation or t_1 is not comparable to
   *           t_2 (i.e. the relation is not reflexive)
   */
  public Type closure() {
    throw new IllegalOperationException("closure", this);
  }

  /**
   * Computes the least upper bound of all elements of this type and returns a
   * set of this type. Works on all types that have elements/fields or children
   * such as tuples, relations, sets and constructors.
   * 
   * @return a set[lub].
   */
  public Type carrier() {
    throw new IllegalOperationException("carrier", this);
  }

  /**
   * Iterate over fields of the type
   */
  public Iterator<Type> iterator() {
    throw new IllegalOperationException("iterator", this);
  }

  /**
   * @return a stream of Types using the Type.iterator
   */
  public Stream<Type> stream() {
      return StreamSupport.stream(spliterator(), false);
  }

  /**
   * Select fields from tuples and relation
   * 
   * @param fields
   * @return a new tuple or relation type with the selected fields
   */
  public Type select(int... fields) {
    throw new IllegalOperationException("select", this);

  }

  /**
   * Select fields from tuples and relation
   * 
   * @param fields
   * @return a new tuple or relation type with the selected fields
   */
  public Type select(String... names) {
    throw new IllegalOperationException("select", this);
  }

  /**
   * For a constructor, return the algebraic data-type it constructs
   * 
   * @return a type
   */
  public Type getAbstractDataType() {
    throw new IllegalOperationException("getAbstractDataType", this);
  }

  /**
   * For an alias type, return which type it aliases.
   * 
   * @return a type
   */
  public Type getAliased() {
    throw new IllegalOperationException("getAliased", this);
  }

  /**
   * For a parameter type, return its bound
   * 
   * @return a type
   */
  public Type getBound() {
    throw new IllegalOperationException("getBound", this);
  }

  /**
   * For a tuple type or a relation type, determine whether the fields are
   * labelled or not.
   * 
   * @return if the fields of a type or relation have been labelled
   */
  @Pure
  public boolean hasFieldNames() {
    return false;
  }

  public String getKeyLabel() {
    throw new IllegalOperationException("getKeyLabel", this);
  }

  public String getValueLabel() {
    throw new IllegalOperationException("getValueLabel", this);
  }

  /**
   * Returns the smallest non-strict supertype of two types, which contains all values of 
   * the receiver as well as the argument type. Lub can be seen as `type union`, but
   * not all unions have a representation in the type lattice; so the smallest set which
   * contains the whole union must be returned (which does have a representation).
   * 
   * Consider for example `int.lub(str)`; the smallest union that contains both in 
   * the vallang type lattica is `value`, even though we by-catch all other values now
   * as well. Other lubs are more tight: `tuple[int,num].lub(tuple[num,int]])` is
   * `tuple[num,num]`.
   * 
   * @return the least upper bound type of the receiver and the argument type.
   */
  public abstract Type lub(Type type);

  /**
   * Returns the largest non-strict sub-type of two types which contains the intersection
   * of both types. Glb is the dual of `lub`. Glb can be seen as "type intersection", but
   * not all intersections have a representation in the vallang type lattice; so the largest
   * set which contains the intersection must be returned (which does have a representation)
   * 
   * The glb is commonly `void` for non-comparable types, say for example
   * @param type
   * @return
   */
  public abstract Type glb(Type type);
  
  /**
   * The sub-type relation. Value is the biggest type and void is the smallest.
   * Value is the top and void is the bottom of the type hierarchy.
   * 
   * @param other
   * @return true if the receiver is a subtype of the other type
   */
  public final boolean isSubtypeOf(Type other) {
    return other == this || other.isSupertypeOf(this);
  }
  
  public final boolean isStrictSubtypeOf(Type other) {
    return (!other.equivalent(this)) && other.isSupertypeOf(this);
  }

  protected abstract boolean isSupertypeOf(Type type);

  /**
   * Return whether an ADT or an alias Type has any type parameters
   * 
   * @return true if the type is parameterized
   */
  public boolean isParameterized() {
    return false;
  }
  
  /**
   * @return true iff the type contains any uninstantiated type parameters 
   */
  public boolean isOpen() {
    return false;
  }
  
  /**
   * @return true iff the type is an alias
   */
  public boolean isAliased() {
    return false;
  }
  
  public boolean isSet() {
    return false;
  }
  
  public boolean isList() {
    return false;
  }
  
  public boolean isMap() {
    return false;
  }
  
  public boolean isBool() {
    return false;
  }
  
  public boolean isRelation() {
    return false;
  }
  
  public boolean isListRelation() {
    return false;
  }
  
  public boolean isInteger() {
    return false;
  }
  
  public boolean isReal() {
    return false;
  }
  
  public boolean isRational() {
    return false;
  }
  
  public  boolean isNumber() {
    return false;
  }
  
  public  boolean isTop() {
    return false;
  }
  
  public  boolean isBottom() {
    return false;
  }
  
  public  boolean isNode() {
	  return false;
  }
  
  public boolean isAbstractData() {
    return false;
  }
  
  public  boolean isConstructor() {
	  return false;
  }
  
  public  boolean isString() {
	  return false;
  }
  
  public  boolean isSourceLocation() {
    return false;
  }
  
  public  boolean isDateTime() {
	  return false;
  }
  
  public  boolean isTuple() {
	  return false;
  }
  
  public boolean isExternalType() {
    return false;
  }
  
  /**
   * @return true iff type is a tuple
   */
  public boolean isFixedWidth() {
    return false;
  }

  /**
   * Compute whether this type is a subtype of the other or vice versa.
   * 
   * @param other
   *          type to compare to
   * @return true if the types are comparable.
   */
  public final boolean comparable(Type other) {
    return (other == this) || isSubtypeOf(other) || other.isSubtypeOf(this);
  }
  
  /**
   * Compute whether these types have a non-empty intersection.
   * 
   * All types which are `comparable` have a non-empty intersection,
   * but there are some more. For example `tuple[int, value]` and 
   * `tuple[value,int]` intersect at `tuple[int, int]`.
   * 
   * If the `glb` of two types is not `void` then they have a non-empty 
   * intersection.
   * 
   * Another example is `lrel[int, int]` and `list[tuple[int,int]]`;
   * their `glb` is `list[void]` and its only element is `[]` 
   * -the empty list- so indeed their intersection is non-empty.
   * 
   * Another way of explaining `t.intersects(u)` is as a fast check of: 
   * `t.glb(u) != void` without memory allocation.  
   * 
   * @param other type to intersect with.
   * @return true iff these two types share values.
   */
  public abstract boolean intersects(Type other);

  /**
   * Computer whether this type is equivalent to another.
   * 
   * @param other
   *          type to compare to
   * @return true if the two types are sub-types of each-other;
   */
  public final boolean equivalent(Type other) {
     return (other == this) || (isSubtypeOf(other) && other.isSubtypeOf(this));
  }

  /**
   * If this type has parameters and there are parameter types embedded in it,
   * instantiate will replace the parameter types using the given bindings.
   * 
   * @param bindings
   *          a map from parameter type names to actual types.
   * @return a type with all parameter types substituted.
   */
  public Type instantiate(Map<Type, Type> bindings) {
    return this;
  }

  /**
   * Construct a map of parameter type names to actual type names. The receiver
   * is a pattern that may contain parameter types.
   * 
   * @param matched
   *          a type to matched to the receiver.
   * @throws FactTypeUseException
   *           when a pattern can not be matched because the matched types do
   *           not fit the bounds of the parameter types, or when a pattern
   *           simply can not be matched because of incompatibility.
   */
  public boolean match(Type matched, Map<Type, Type> bindings) throws FactTypeUseException {
    return matched.isSubtypeOf(this);
  }

  public abstract <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E;

  /**
   * For alias types and adt types return which type parameters there are.
   * 
   * @return void if there are no type parameters, or a tuple of type parameters
   *         otherwise.
   */
  public Type getTypeParameters() {
    throw new IllegalOperationException("getTypeParameters", this);
  }

  /**
   * Compare against another type.
   * 
   * A type is 'less' than another if it is a subtype, 'greater' if the other is
   * a subtype, or 'equal' if both are subtypes of each other.
   * 
   * Note: this class has a natural ordering that is inconsistent with equals.
   * equals() on types is exact equality, which may be different from
   * compareTo(o) == 0
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(Type o) {
    if (isSubtypeOf(o)) {
      return -1;
    } else if (o.isSubtypeOf(this)) {
      return 1;
    }
    return 0;
  }

  protected boolean isSubtypeOfParameter(Type type) {
    return isSubtypeOf(type.getBound());
  }
  
  protected final boolean isSubtypeOfAlias(Type type) {
    return isSubtypeOf(type.getAliased());
  }
  
  abstract protected boolean isSubtypeOfReal(Type type);
  abstract protected boolean isSubtypeOfInteger(Type type);
  abstract protected boolean isSubtypeOfRational(Type type);
  abstract protected boolean isSubtypeOfList(Type type);
  abstract protected boolean isSubtypeOfMap(Type type);
  abstract protected boolean isSubtypeOfNumber(Type type);
  abstract protected boolean isSubtypeOfSet(Type type);
  abstract protected boolean isSubtypeOfSourceLocation(Type type);
  abstract protected boolean isSubtypeOfString(Type type);
  abstract protected boolean isSubtypeOfNode(Type type);
  abstract protected boolean isSubtypeOfConstructor(Type type);
  abstract protected boolean isSubtypeOfAbstractData(Type type);
  abstract protected boolean isSubtypeOfTuple(Type type);
  abstract protected boolean isSubtypeOfValue(Type type);
  abstract protected boolean isSubtypeOfVoid(Type type);
  abstract protected boolean isSubtypeOfBool(Type type);
  abstract protected boolean isSubtypeOfExternal(Type type);
  abstract protected boolean isSubtypeOfDateTime(Type type);
  
  abstract protected boolean intersectsWithReal(Type type);
  abstract protected boolean intersectsWithInteger(Type type);
  abstract protected boolean intersectsWithRational(Type type);
  abstract protected boolean intersectsWithList(Type type);
  abstract protected boolean intersectsWithMap(Type type);
  abstract protected boolean intersectsWithNumber(Type type);
  abstract protected boolean intersectsWithSet(Type type);
  abstract protected boolean intersectsWithSourceLocation(Type type);
  abstract protected boolean intersectsWithString(Type type);
  abstract protected boolean intersectsWithNode(Type type);
  abstract protected boolean intersectsWithConstructor(Type type);
  abstract protected boolean intersectsWithAbstractData(Type type);
  abstract protected boolean intersectsWithTuple(Type type);
  abstract protected boolean intersectsWithValue(Type type);
  abstract protected boolean intersectsWithVoid(Type type);
  abstract protected boolean intersectsWithBool(Type type);
  protected boolean intersectsWithExternal(Type type) {
      // delegate to the external type always
      return type.intersects(this);
  }
  abstract protected boolean intersectsWithDateTime(Type type);
  
  protected Type lubWithAlias(Type type) {
    return lub(type.getAliased());
  }
  
  protected Type lubWithParameter(Type type) {
      if (type == this) {
          return this;
      }
      
      return lub(type.getBound());
  }
  
  abstract protected Type lubWithReal(Type type) ;
  abstract protected Type lubWithInteger(Type type) ;
  abstract protected Type lubWithRational(Type type) ;
  abstract protected Type lubWithList(Type type) ;
  abstract protected Type lubWithMap(Type type) ;
  abstract protected Type lubWithNumber(Type type) ;
  abstract protected Type lubWithSet(Type type) ;
  abstract protected Type lubWithSourceLocation(Type type) ;
  abstract protected Type lubWithString(Type type) ;
  abstract protected Type lubWithNode(Type type) ;
  abstract protected Type lubWithConstructor(Type type) ;
  abstract protected Type lubWithAbstractData(Type type) ;
  abstract protected Type lubWithTuple(Type type) ;
  abstract protected Type lubWithValue(Type type) ;
  abstract protected Type lubWithVoid(Type type) ;
  abstract protected Type lubWithBool(Type type) ;
  abstract protected Type lubWithDateTime(Type type) ;
  
  protected Type glbWithAlias(Type type) {
    return glb(type.getAliased());
  }
  
  protected Type glbWithParameter(Type type) {
      if (type == this) {
          return this;
      }
      
      return glb(type.getBound());
  }
  
  abstract protected Type glbWithReal(Type type) ;
  abstract protected Type glbWithInteger(Type type) ;
  abstract protected Type glbWithRational(Type type) ;
  abstract protected Type glbWithList(Type type) ;
  abstract protected Type glbWithMap(Type type) ;
  abstract protected Type glbWithNumber(Type type) ;
  abstract protected Type glbWithSet(Type type) ;
  abstract protected Type glbWithSourceLocation(Type type) ;
  abstract protected Type glbWithString(Type type) ;
  abstract protected Type glbWithNode(Type type) ;
  abstract protected Type glbWithConstructor(Type type) ;
  abstract protected Type glbWithAbstractData(Type type) ;
  abstract protected Type glbWithTuple(Type type) ;
  abstract protected Type glbWithValue(Type type) ;
  abstract protected Type glbWithVoid(Type type) ;
  abstract protected Type glbWithBool(Type type) ;
  abstract protected Type glbWithDateTime(Type type) ;
  
  /**
   * This makes sure that lubbing can be done by the external type whether
   * or not it is the initial receiver or the second parameter to lub.
   */
  protected Type lubWithExternal(Type type) {
    // the external type should be the receiver
    return lub(type);
  }
  
  protected Type glbWithExternal(Type type) {
    // the external type should be the receiver
    return glb(type);
  }

  /**
   * Generate a random value which is guaranteed to have a type that is
   * a (non-strict) sub-type of the receiver.
   * 
   * @param random         pass in reused Random instance for better random uniformity between calls to this method.
   * @param vf             IValueFactory to use when building values
   * @param store          TypeStore to lookup constructors and fields in
   * @param typeParameters will be filled with the inferred (least-upper-bound) type for every open type parameter as a side-effect.
   * @param maxDepth       how deeply to generate recursive values
   * @param maxWidth       how wide collections and fixed-width data-types should be (maximally)
   * @return
   */
  abstract public IValue randomValue(Random random, IValueFactory vf, TypeStore store, Map<Type, Type> typeParameters, int maxDepth, int maxWidth);
}