/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

 *******************************************************************************/

package org.eclipse.imp.pdb.facts.type;

import java.net.URI;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.IWriter;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.IllegalOperationException;


/**
 * This class is the abstract implementation for all types. Types
 * are ordered in a partially ordered type hierarchy with 'value'
 * as the largest type and 'void' as the smallest. Each type represents
 * a set of values.  
 * <p>
 * Users of this API will generally use the interface of @{link Type} and
 * {@link TypeFactory}, the other classes in this package are not part of the
 * interface. To construct {@link IValue}'s, use the 'make' methods of 
 * @{link Type}.
 * <p>
 * Technical detail: since void is a sub-type of all types and alias types 
 * may be sub-types of any types, a Java encoding of the hierarchy using
 * single inheritance will not work. Therefore, all methods of all types are
 * present on this abstract class Type. void and alias type implement all
 * methods, while the other methods implement only the relevant methods.
 * Calling a method that is not present on any of the specific types will
 * lead to a @{link FactTypeError} exception. 
 */
public abstract class Type implements Iterable<Type>, Comparable<Type> {
	/**
	 * Retrieve the type of elements in a set or a relation.
	 * @return type of elements
	 */
	public Type getElementType() {
		throw new IllegalOperationException("getElementType", this);
	}
	
	/**
	 * Retrieve the key type of a map
	 * @return key type
	 */
	public Type getKeyType() {
		throw new IllegalOperationException("getKeyType", this);
	}

	/**
	 * Retrieve the value type of a map
	 * @return value type
	 */
	public Type getValueType() {
		throw new IllegalOperationException("getValueType", this);
	}
	
	/**
	 * Retrieve the name of a named type, a tree node type or 
	 * a parameter type.
	 *  
	 * @return name of the type
	 */
	public String getName() {
		throw new IllegalOperationException("getName", this);
	}
	
	/**
	 * Retrieve the type of a field of a tuple type, a relation type
	 * or a tree node type.
	 * 
	 * @param i index of the field to retrieve
	 * @return type of the field at index i
	 */
	public Type getFieldType(int i) {
		throw new IllegalOperationException("getFieldType", this);
	}

	/**
	 * Retrieve the type of a field of a tuple type, a relation type
	 * or a tree node type.
	 * <p>
	 * @param fieldName label of the field to retrieve
	 * @return type of the field at index i
	 * @throws FactTypeUseException when the type has no field labels (tuples
	 *         and relations optionally have field labels).
	 */
	public Type getFieldType(String fieldName) throws FactTypeUseException {
		throw new IllegalOperationException("getFieldType", this);
	}

	/**
	 * Retrieve the field types of a tree node type or a relation, 
	 * represented as a tuple type.
	 * 
	 * @return a tuple type representing the field types
	 */
	public Type getFieldTypes() {
		throw new IllegalOperationException("getFieldTypes", this);
	}
	
	/**
	 * Retrieve the field name at a certain index for a tuple type,
	 * a relation type or a tree node type.
	 * 
	 * @param i index of the field name to retrieve
	 * @return the field name at index i
	 * @throws FactTypeUseException when this type does not have field labels.
	 *         Tuples and relations optionally have field labels.
	 */
	public String getFieldName(int i) {
		throw new IllegalOperationException("getFieldName", this);
	}
	
	/**
	 * Retrieve all the field names of tuple type,
	 * a relation type or a tree node type.
	 * 
	 * @return the field name at index i
	 * @throws FactTypeUseException when this type does not have field labels.
	 *         Tuples and relations optionally have field labels.
	 */
	public String[] getFieldNames(){
		throw new IllegalOperationException("getFieldNames", this);
	}
	
	/**
	 * Retrieve a field index for a certain label for a tuple type,
	 * a relation type or a tree node type.
	 * 
	 * @param fieldName name of the field to retrieve
	 * @return the index of fieldName
	 */
	public int getFieldIndex(String fieldName)  {
		throw new IllegalOperationException("getFieldIndex", this);
	}
	
	/**
	 * @param fieldName name of the field to check for
	 * @return true iff this type has a field named fieldName
	 */
	public boolean hasField(String fieldName) {
		throw new IllegalOperationException("hasField", this);
	}
	
	/**
	 * @param fieldName name of the field to check for
	 * @return true iff this type has a field named fieldName
	 */
	public boolean hasField(String fieldName, TypeStore store) {
		return hasField(fieldName);
	}

	/**
	 * Retrieve the width, a.k.a. arity, of a tuple, a relation or 
	 * a tree node type.
	 * @return the arity
	 */
	public int getArity() {
		throw new IllegalOperationException("getArity", this);
	}
	
	/**
	 * Compose two binary tuples or binary relation types.
	 * 
	 * @param other
	 * @return a new type that represent the composition
	 * @throws IllegalOperationException if the receiver or the other is not binary
	 * or if the last type of the receiver is not comparable to the first type of the other.
	 */
	public Type compose(Type other) {
		throw new IllegalOperationException("compose", this, other);
	}
	
	/**
	 * For relation types rel[t_1,t_2] this will compute rel[t_3,t_3] where t_3 = t_1.lub(t_2).
	 * 
	 * @return rel[t_3,t_3]
	 * @throws IllegalOperationException when this is not a binary relation or
	 * t_1 is not comparable to t_2 (i.e. the relation is not reflexive)
	 */
	public Type closure() {
		throw new IllegalOperationException("closure", this);
	}
	
	/**
	 * Computes the least upper bound of all elements of this type and returns
	 * a set of this type. Works on all types that have elements/fields or children
	 * such as tuples, relations, sets and constructors.
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
	 * Select fields from tuples and relation
	 * @param fields
	 * @return a new tuple or relation type with the selected fields
	 */
	public Type select(int... fields) {
		throw new IllegalOperationException("select", this);
		
	}
	
	/**
	 * Select fields from tuples and relation
	 * @param fields
	 * @return a new tuple or relation type with the selected fields
	 */
	public Type select(String... names) {
		throw new IllegalOperationException("select", this);
	}
	
	/**
	 * For a constructor, return the algebraic data-type it constructs
	 * @return a type
	 */
	public Type getAbstractDataType() {
		throw new IllegalOperationException("getAbstractDataType", this);
	}
	
	/**
	 * For an alias type, return which type it aliases.
	 * @return a type
	 */ 
	public Type getAliased() {
		throw new IllegalOperationException("getAliased", this);
	}
	
	/**
	 * For a parameter type, return its bound
	 * @return a type
	 */
	public Type getBound() {
		throw new IllegalOperationException("getBound", this);
	}
	
	/**
	 * For a tuple type or a relation type, determine whether the
	 * fields are labelled or not.
	 * @return if the fields of a type or relation have been labelled
	 */
	public boolean hasFieldNames() {
		return false;
	}
	
	/**
	 * For a AbstractDataType or a ConstructorType, return whether a certain
	 * annotation label was declared.
	 * 
	 * @param label
	 * @param store to find the declaration in
	 * @return true if this type has an annotation named label declared for it. 
	 */
	public boolean declaresAnnotation(TypeStore store, String label) {
		return false;
	}
	
	public Type getAnnotationType(TypeStore store, String label) throws FactTypeUseException {
		throw new IllegalOperationException("getAnnotationType", this);
	}
	
	public String getKeyLabel() {
		throw new IllegalOperationException("getKeyLabel", this);
	}

	public String getValueLabel() {
		throw new IllegalOperationException("getValueLabel", this);
	}
	
	/**
	 * @return the least upper bound type of the receiver and the argument type
	 */
	public Type lub(Type other) {
		// this is the default implementation. Subclasses should override
		// to take their immediate super types into account.
		if (other == this) {
			return this;
		}
		else if (other.isVoidType()) {
			return this;
		}
		else if (other.isAliasType()) {
			return lub(other.getAliased());
		}
		else {
			return TypeFactory.getInstance().valueType();
		}
	}

	/*
	 * A Subtype class declares which other subtypes the current type is 
	 * a subtype of. This default class returns false to every request.
	 * It is intended to be subclasses by concrete Types.
	 */
	protected static class DefaultSubtype implements ITypeVisitor<Boolean> {
    public DefaultSubtype() { /* for visibility */ }

    @Override
    public Boolean visitReal(Type type) {
      return false;
    }

    @Override
    public Boolean visitInteger(Type type) {
      return false;
    }

    @Override
    public Boolean visitRational(Type type) {
      return false;
    }

    @Override
    public Boolean visitList(Type type) {
      return false;
    }

    @Override
    public Boolean visitMap(Type type) {
      return false;
    }

    @Override
    public Boolean visitNumber(Type type) {
      return false;
    }

    @Override
    public final Boolean visitAlias(Type type) {
      // dealt with here to avoid cloning acros all types
      return type.getAliased().accept(this);
    }

    @Override
    public Boolean visitRelationType(Type type) {
      return false;
    }

    @Override
    public Boolean visitListRelationType(Type type) {
      return false;
    }

    @Override
    public Boolean visitSet(Type type) {
      return false;
    }

    @Override
    public Boolean visitSourceLocation(Type type) {
      return false;
    }

    @Override
    public Boolean visitString(Type type) {
      return false;
    }

    @Override
    public Boolean visitNode(Type type) {
      return false;
    }

    @Override
    public Boolean visitConstructor(Type type) {
      return false;
    }

    @Override
    public Boolean visitAbstractData(Type type) {
      return false;
    }

    @Override
    public Boolean visitTuple(Type type) {
      return false;
    }

    @Override
    public final Boolean visitValue(Type type) {
      // since all types are a sub-type of value, this one is safe to factor out.
      return true;
    }

    @Override
    public Boolean visitVoid(Type type) {
      return false;
    }

    @Override
    public Boolean visitBool(Type boolType) {
      return false;
    }

    @Override
    public final Boolean visitParameter(Type parameterType) {
      // dealt with here to avoid cloning acros types
      return parameterType.getBound().accept(this);
    }

    @Override
    public Boolean visitExternal(Type externalType) {
      return false;
    }

    @Override
    public Boolean visitDateTime(Type type) {
      return false;
    }
	  
	}
	
	protected static class ForwardSubtype extends DefaultSubtype {
	  private final Type fwd;

    public ForwardSubtype(Type to) {
	    this.fwd = to;
    }
    
    @Override
    public Boolean visitReal(Type type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public Boolean visitInteger(Type type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public Boolean visitRational(Type type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public Boolean visitList(Type type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public Boolean visitMap(Type type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public Boolean visitNumber(Type type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public Boolean visitRelationType(Type type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public Boolean visitListRelationType(Type type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public Boolean visitSet(Type type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public Boolean visitSourceLocation(Type type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public Boolean visitString(Type type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public Boolean visitNode(Type type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public Boolean visitConstructor(Type type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public Boolean visitAbstractData(Type type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public Boolean visitTuple(Type type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public Boolean visitVoid(Type type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public Boolean visitBool(Type type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public Boolean visitExternal(Type type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public Boolean visitDateTime(Type type) {
      return fwd.isSubtypeOf(type);
    }
	}
	
	/**
	 * Concrete types should return a DefaultSubType that returns true
	 * for the types that the current type is a sub-type of.
	 */
	abstract protected DefaultSubtype getSubtype();
	
	/**
	 * The sub-type relation. Value is the biggest type and void is
	 * the smallest. Value is the top and void is the bottom of the
	 * type hierarchy.
	 * 
	 * @param other
	 * @return true if the receiver is a subtype of the other type
	 */
	 public final boolean isSubtypeOf(Type other) {
	   return this == other || other.accept(getSubtype());
	 }
	
  /**
	 * Return whether an ADT or an alias Type has any type parameters
	 * @return true if the type is parameterized
	 */
	public boolean isParameterized() {
		return false;
	}

	/**
	 * Compute whether this type is a subtype of the other or vice versa
	 * @param other type to compare to
	 * @return true if the types are comparable.
	 */
	public final boolean comparable(Type other) {
		return (other == this) || isSubtypeOf(other) || other.isSubtypeOf(this);
	}

	/**
	 * Computer whether this type is equivalent to another. 
	 * @param other type to compare to
	 * @return true if the two types are sub-types of each-other;
	 */
	public final boolean equivalent(Type other) {
		return (other == this) || (isSubtypeOf(other) && other.isSubtypeOf(this));
	}
	
	/**
	 * If this type has parameters and there are parameter types embedded in it,
	 * instantiate will replace the parameter types using the given bindings.
	 * @param bindings a map from parameter type names to actual types.
	 * @return a type with all parameter types substituted.
	 */
	public Type instantiate(Map<Type, Type> bindings) {
    	return this;
	}
	
	/**
	 * Construct a map of parameter type names to actual type names.
	 * The receiver is a pattern that may contain parameter types.
	 * 
	 * @param matched a type to matched to the receiver.
	 * @throws FactTypeUseException when a pattern can not be matches because the
	 *         matched types do not fit the bounds of the parameter types,
	 *         or when a pattern simply can not be matched because of 
	 *         incompatibility.         
	 */
	public boolean match(Type matched, Map<Type, Type> bindings) throws FactTypeUseException {
		return matched.isSubtypeOf(this);
	}
	
	public abstract <T> T accept(ITypeVisitor<T> visitor);

	@Deprecated
	public boolean isRelationType() {
		return false;
	}
	
	@Deprecated
	public boolean isListRelationType() {
		return false;
	}

	@Deprecated
	public boolean isSetType() {
		return false;
	}

	@Deprecated
	public boolean isTupleType() {
		return false;
	}

	@Deprecated
	public boolean isListType() {
		return false;
	}

	@Deprecated
	public boolean isIntegerType() {
		return false;
	}

	@Deprecated
	public boolean isRationalType() {
		return false;
	}

	@Deprecated
	public boolean isRealType() {
		return false;
	}

	@Deprecated
	public boolean isStringType() {
		return false;
	}

	@Deprecated
	public boolean isSourceLocationType() {
		return false;
	}

	@Deprecated
	public boolean isSourceRangeType() {
		return false;
	}

	@Deprecated
	public boolean isAliasType() {
		return false;
	}

	@Deprecated
	public boolean isValueType() {
		return false;
	}
	
	@Deprecated
	public boolean isVoidType() {
		return false;
	}
	
	@Deprecated
	public boolean isExternalType() {
		return false;
	}

	@Deprecated
	public boolean isNodeType() {
		return false;
	}
	
	@Deprecated
	public boolean isConstructorType() {
		return false;
	}

	@Deprecated
	public boolean isAbstractDataType() {
		return false;
	}
	
	@Deprecated
	public boolean isNumberType() {
		return false;
	}

	@Deprecated
	public boolean isMapType() {
		return false;
	}
	
	@Deprecated
	public boolean isBoolType() {
		return false;
	}
	
	@Deprecated
	public boolean isParameterType() {
		return false;
	}

	@Deprecated
	public boolean isDateTimeType() {
		return false;
	}
	
	/**
	 * Build a real value. This method is supported by the DoubleTypes
	 
	 * @param f   the factory to use
	 * @param arg the double to wrap
	 * @return a real value
	 */
	public IValue make(IValueFactory f, double arg) {
		throw new IllegalOperationException("make double", this);
	}
	
	/**
	 * Build a real value. This method is supported by the DoubleTypes
	 
	 * @param f   the factory to use
	 * @param arg the float to wrap
	 * @return a real value
	 */
	public IValue make(IValueFactory f, float arg) {
		throw new IllegalOperationException("make double", this);
	}
	
	/**
	 * Build a real value. This method is supported by TreeNodeTypes that wrap
	 * a single double argument.
	 * 
	 * @param f   the factory to use
	 * @param arg the float to wrap
	 * @return a real value
	 */
	public IValue make(IValueFactory f, TypeStore store, float arg) {
		throw new IllegalOperationException("make real", this);
	}

	/**
	 * Build a real value. This method is supported by TreeNodeTypes that wrap
	 * a single double argument.
	 * 
	 * @param f   the factory to use
	 * @param arg the double to wrap
	 * @return a real value
	 */
	public IValue make(IValueFactory f, TypeStore store, double arg) {
		throw new IllegalOperationException("make real", this);
	}

	/**
	 * Build a integer value. This method is supported by IntegerTypes.
	 * 
	 * @param f   the factory to use
	 * @param arg the integer to wrap
	 * @return a integer value
	 */
	public IValue make(IValueFactory f, int arg) {
		throw new IllegalOperationException("make int", this);
	}
	
	/**
	 * Build a integer value. This method is supported by TreeNodeTypes 
	 * that wrap a single integer argument.
	 * 
	 * @param f   the factory to use
	 * @param arg the integer to wrap
	 * @return a integer value
	 */
	public IValue make(IValueFactory f, TypeStore store, int arg) {
		throw new IllegalOperationException("make int", this);
	}

	/**
	 * Build a integer value. This method is supported by TreeNodeTypes 
	 * that wrap a single integer argument.
	 * 
	 * @param f   the factory to use
	 * @param num the numerator
	 * @param denom the denominator
	 * @return a rational value
	 */
	public IValue make(IValueFactory f, int num, int denom) {
		throw new IllegalOperationException("make rational", this);
	}

	/**
	 * Build a integer value. This method is supported by TreeNodeTypes 
	 * that wrap a single integer argument.
	 * 
	 * @param f   the factory to use
	 * @param num the numerator
	 * @param denom the denominator
	 * @return a rational value
	 */
	public IValue make(IValueFactory f, TypeStore store, int num, int denom) {
		throw new IllegalOperationException("make rational", this);
	}

	/**
	 * Build a string value. This method is supported by the StringType.
	 * 
	 * @param f   the factory to use
	 * @param arg the string to wrap
	 * @return a string value
	 */
	public IValue make(IValueFactory f, String arg) {
		throw new IllegalOperationException("make string", this);
	}
	
	/**
	 * Build a string value. This method is supported by TreeNodeTypes 
	 * that wrap a single string value
	 * 
	 * @param f   the factory to use
	 * @param arg the string to wrap
	 * @return a tree node that wraps a single string
	 */
	public IValue make(IValueFactory f, TypeStore store, String arg) {
		throw new IllegalOperationException("make string", this);
	}

	/** 
	 * Build a value that does not have any children. This method is supported
	 * by list, set, tuple, relation and map, nullary trees and
	 * NamedTypes that are sub-types of any of the previous. 
	 * 
	 * @param f
	 * @return a new empty value of the type
	 */
	public IValue make(IValueFactory f) {
		throw new IllegalOperationException("make zero constructor", this);
	}

	/**
	 * Build a value that has a number of children. This method is supported by
	 * tuples types and tree node types with the correct arity, also lists
	 * relations and maps can be constructed with a fixed size.
	 * 
	 * @param f    factory to use
	 * @param args arguments to use
	 * @return a value of the appropriate type
	 */
	public IValue make(IValueFactory f, IValue...args) {
		throw new IllegalOperationException("apply to children", this);
	}
	
	public IValue make(IValueFactory f, TypeStore ts, IValue...args) {
		throw new IllegalOperationException("apply to children", this);
	}
	
	public IValue make(IValueFactory f, URI uri, int startOffset, int length,
			int startLine, int endLine, int startCol, int endCol) {
		throw new IllegalOperationException("make source location", this);
	} 
	
	public IValue make(IValueFactory f, URI uri) {
		throw new IllegalOperationException("make source location", this);
	} 
	
	public IValue make(IValueFactory f, String path, int startOffset, int length,
			int startLine, int endLine, int startCol, int endCol) {
		throw new IllegalOperationException("make source location", this);
	} 

	public IValue make(IValueFactory f, String name, IValue... children) {
		throw new IllegalOperationException("make tree constructor", this);
	}
	
	public IValue make(IValueFactory valueFactory, String name,
			IValue[] argValues, Map<String, IValue> keyArgValues) {
		throw new IllegalOperationException("make tree constructor", this);
	}
	
	public IValue make(IValueFactory f, TypeStore store, String name, IValue... children) {
		throw new IllegalOperationException("make tree constructor", this);
	}
	
	public IValue make(IValueFactory f, boolean arg) {
		throw new IllegalOperationException("make boolean", this);
	}
	
	public IValue make(IValueFactory f, TypeStore store, boolean arg) {
		throw new IllegalOperationException("make boolean", this);
	}

	/** 
	 * Build a date. This method is supported by DateTimeType.
	 * 
	 * @param f			Factory to use
	 * @param year		Year of the date
	 * @param month		Month of the date
	 * @param day		Day of the date
	 * 
	 * @return			The constructed date.
	 */
	public IValue make(IValueFactory f, int year, int month, int day) {
		throw new IllegalOperationException("make datetime", this);
	}
	
	/**
	 * Build a time. This method is supported by DateTimeType.
	 * 
	 * @param f				Factory to use
	 * @param hour			Hour of the time
	 * @param minute		Minute of the time
	 * @param second		Second of the time
	 * @param millisecond	Millisecond of the time
	 * 
	 * @return				The constructed time.
	 */
	public IValue make(IValueFactory f, int hour, int minute, int second, 
			int millisecond) {
		throw new IllegalOperationException("make datetime", this);		
	}
	
	/**
	 * Build a time. This method is supported by DateTimeType.
	 * 
	 * @param f				Factory to use
	 * @param hour			Hour of the time
	 * @param minute		Minute of the time
	 * @param second		Second of the time
	 * @param millisecond	Millisecond of the time
	 * @param hourOffset	Timezone offset in hours
	 * @param minuteOffset	Timezone offset in minutes
	 * 
	 * @return				The constructed time
	 */
	public IValue make(IValueFactory f, int hour, int minute, int second, 
			int millisecond, int hourOffset, int minuteOffset) {
		throw new IllegalOperationException("make datetime", this);		
	}
	
	/**
	 * Build a datetime. This method is supported by DateTimeType.
	 * 
	 * @param f				Factory to use
	 * @param year			Year of the date
	 * @param month			Month of the date
	 * @param day			Day of the date
	 * @param hour			Hour of the time
	 * @param minute		Minute of the time
	 * @param second		Second of the time
	 * @param millisecond	Millisecond of the time
	 * 
	 * @return				The constructed datetime
	 */
	public IValue make(IValueFactory f, int year, int month, int day, 
			int hour, int minute, int second, int millisecond) {
		throw new IllegalOperationException("make datetime", this);		
	}
	
	/**
	 * Build a datetime. This method is supported by DateTimeType.
	 * 
	 * @param f				Factory to use
	 * @param year			Year of the date
	 * @param month			Month of the date
	 * @param day			Day of the date
	 * @param hour			Hour of the time
	 * @param minute		Minute of the time
	 * @param second		Second of the time
	 * @param millisecond	Millisecond of the time
	 * @param hourOffset	Timezone offset in hours
	 * @param minuteOffset	Timezone offset in minutes
	 * 
	 * @return				The constructed datetime
	 */
	public IValue make(IValueFactory f, int year, int month, int day, int hour, 
			int minute, int second, int millisecond, int hourOffset, int minuteOffset) {
		throw new IllegalOperationException("make datetime", this);		
	}
	
	/**
	 * Build a datetime. This method is supported by DateTimeType.
	 * 
	 * @param f		Factory to use
	 * @param 		Datetime instant of this time
	 * 
	 * @return		The constructed datetime.
	 */
	public IValue make(IValueFactory f, long instant) {
		throw new IllegalOperationException("make datetime", this);
	}
	
	/**
	 * Return a writer object. This works for Lists, Sets, Relations and Maps.
	 * Caller is responsible for assigning the result to I{List,Set,Relation,Map}Writer
	 * variable.
	 * @param f   factory to use 
	 * @return a writer
	 */
	public <T extends IWriter> T writer(IValueFactory f) {
		throw new IllegalOperationException("writer", this);
	}

	/**
	 * For AliasTypes, return which basic type it hides.
	 * @return the first super type of this type that is not a AliasType.
	 */
	public Type getHiddenType() {
		throw new IllegalOperationException("getHiddenType", this);
	}

	/**
	 * For alias types and adt types return which type parameters there are.
	 * 
	 * @return void if there are no type parameters, or a tuple of type parameters otherwise.
	 */
	public Type getTypeParameters() {
		throw new IllegalOperationException("getTypeParameters", this);
	}
	
	/**
	 *  Compare against another type.
	 *  
	 *  A type is 'less' than another if it is a subtype, 'greater' if the other is
	 *  a subtype, or 'equal' if both are subtypes of each other.
	 * 
	 *  Note: this class has a natural ordering that is inconsistent with equals.
	 *  equals() on types is exact equality, which may be different from compareTo(o) == 0
	 *  
	 *  @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Type o) {
		if (isSubtypeOf(o)) {
			return -1;
		}
		else if (o.isSubtypeOf(this)) {
			return 1;
		}
		return 0;
	}

	public boolean hasKeywordArguments() {
		return false;
	}
	
//	public IValue[] getDefaults(){
//		throw new IllegalOperationException("getDefaults", this);
//	}

	public int getPositionalArity() {
		throw new IllegalOperationException("getIndexOfFirstDefault", this);
	}



//	public IValue getFieldDefault(int i) {
//		throw new IllegalOperationException("getFieldDefault", this);
//	}

//	public IValue getFieldDefault(String fieldName) throws FactTypeUseException {
//		throw new IllegalOperationException("getFieldDefault", this);
//	}
//
//	public boolean hasFieldDefault(String fieldName) {
//		throw new IllegalOperationException("hasFieldDefault", this);
//	}

	
}