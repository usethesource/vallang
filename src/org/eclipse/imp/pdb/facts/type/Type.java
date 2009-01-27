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

import java.util.Iterator;
import java.util.Map;

import org.eclipse.imp.pdb.facts.ISourceRange;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.IWriter;

/**
 * This class is the abstract implementation for all types. Types
 * are ordered in a partially ordered type hierarchy with 'value'
 * as the largest type and 'void' as the smallest. Each type represents
 * a set of values. Named types are aliases (i.e. they are a sub-type
 * of the type they hide and vice versa). 
 * <p>
 * Users of this API will generally use the interface of @{link Type} and
 * {@link TypeFactory}, the other classes in this package are not part of the
 * interface. To construct {@link IValue}'s, use the 'make' methods of 
 * @{link Type}.
 * <p>
 * Technical detail: since void is a sub-type of all types and named types 
 * may be sub-types of any types, a Java encoding of the hierarchy using
 * single inheritance will not work. Therefore, all methods of all types are
 * present on this abstract class Type. void and named type implement all
 * methods, while the other methods implement only the relevant methods.
 * Calling a method that is not present on any of the specific types will
 * lead to a @{link FactTypeError} exception. 
 */
public abstract class Type implements Iterable<Type> {
	/**
	 * Retrieve the type of elements in a set or a relation.
	 * @return type of elements
	 */
	public Type getElementType() {
		throw new FactTypeError("This type does not have elements");
	}
	
	/**
	 * Retrieve the key type of a map
	 * @return key type
	 */
	public Type getKeyType() {
		throw new FactTypeError("This type does not have a key");
	}

	/**
	 * Retrieve the value type of a map
	 * @return value type
	 */
	public Type getValueType() {
		throw new FactTypeError("This type does not have a value");
	}
	
	/**
	 * Retrieve the name of a named type, a tree node type or 
	 * a parameter type.
	 *  
	 * @return name of the type
	 */
	public String getName() {
		throw new FactTypeError("This type does not have a name");
	}
	
	/**
	 * Retrieve the type of a field of a tuple type, a relation type
	 * or a tree node type.
	 * 
	 * @param i index of the field to retrieve
	 * @return type of the field at index i
	 */
	public Type getFieldType(int i) {
		throw new FactTypeError("This type does not have fields");
	}

	/**
	 * Retrieve the type of a field of a tuple type, a relation type
	 * or a tree node type.
	 * <p>
	 * @param fieldName label of the field to retrieve
	 * @return type of the field at index i
	 * @throws FactTypeError when the type has no field labels (tuples
	 *         and relations optionally have field labels).
	 */
	public Type getFieldType(String fieldName) throws FactTypeError {
		throw new FactTypeError("This type does not have fields");
	}

	/**
	 * Retrieve the field types of a tree node type or a relation, 
	 * represented as a tuple type.
	 * 
	 * @return a tuple type representing the field types
	 */
	public Type getFieldTypes() {
		throw new FactTypeError("This type does not have fields");
	}
	
	/**
	 * Retrieve the field name at a certain index for a tuple type,
	 * a relation type or a tree node type.
	 * 
	 * @param i index of the field name to retrieve
	 * @return the field name at index i
	 * @throws FactTypeError when this type does not have field labels.
	 *         Tuples and relations optionally have field labels.
	 */
	public String getFieldName(int i) {
		throw new FactTypeError("This type does not have fields");
	}
	
	/**
	 * Retrieve a field index for a certain label for a tuple type,
	 * a relation type or a tree node type.
	 * 
	 * @param fieldName name of the field to retrieve
	 * @return the index of fieldName
	 */
	public int getFieldIndex(String fieldName)  {
		throw new FactTypeError("This type does not have fields");
	}

	/**
	 * Return the type that a named type is an alias for.
	 * @return type
	 */
	public Type getHiddenType() {
		throw new FactTypeError("This type is not a named type");
	}

	/**
	 * Retrieve the width, a.k.a. arity, of a tuple, a relation or 
	 * a tree node type.
	 * @return the arity
	 */
	public int getArity() {
		throw new FactTypeError("This type does not have an arity");
	}
	
	/**
	 * Compose tuples or relation types
	 * @param other
	 * @return a new type that represent the composition
	 */
	public Type compose(Type other) {
		throw new FactTypeError("This type does not a compose operation");
	}
	
	/**
	 * Iterate over fields of the type 
	 */
	public Iterator<Type> iterator() {
		throw new FactTypeError("This type does not have an iterator");
	}
	 
	/**
	 * Select fields from tuples and relation
	 * @param fields
	 * @return a new tuple or relation type with the selected fields
	 */
	public Type select(int... fields) {
		throw new FactTypeError("This type does not have a select");
	}
	
	/**
	 * Select fields from tuples and relation
	 * @param fields
	 * @return a new tuple or relation type with the selected fields
	 */
	public Type select(String... names) {
		throw new FactTypeError("This type does not have a select");
	}
	
	/**
	 * For a constructor, return the algebraic data-type it constructs
	 * @return a type
	 */
	public Type getAbstractDataType() {
		throw new FactTypeError("This type does not have an abstract data-type.");
	}
	
	/**
	 * For an alias type, return which type it aliases.
	 * @return a type
	 */ 
	public Type getAliased() {
		throw new FactTypeError("This type is not an alias for anything");
	}
	
	/**
	 * For a parameter type, return its bound
	 * @return a type
	 */
	public Type getBound() {
		throw new FactTypeError("This type does not have a bound");
	}
	
	/**
	 * For a tuple type or a relation type, determine whether the
	 * fields are labelled or not.
	 * @return iff the fields of a type or relation have been labelled
	 */
	public boolean hasFieldNames() {
		throw new FactTypeError("This type never has field names");
	}
	
	/**
	 * For a AbstractDataType or a ConstructorType, return whether a certain
	 * annotation label was declared.
	 * 
	 * @param label
	 * @return true iff this type has an annotation named label declared for it. 
	 */
	public boolean declaresAnnotation(String label) {
		return false;
	}
	
	public Type getAnnotationType(String label) throws FactTypeError {
		throw new FactTypeError("This type does not have support for annotations");
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
		else if (other.isAbstractDataType() && other.isDefinedBy(this)) {
			return other;
		}
		else {
			return TypeFactory.getInstance().valueType();
		}
	}


	/**
	 * The sub-type relation. Value is the biggest type and void is
	 * the smallest. Value is the top and void is the bottom of the
	 * type hierarchy.
	 * 
	 * @param other
	 * @return true iff the receiver is a subtype of the other typ
	 */
	public boolean isSubtypeOf(Type other) {
		// this is the default implementation. Subclasses should override
		// to take their immediate super types into account.
		if (other.isValueType()) {
			return true;
		}
		if (other == this) {
			return true;
		}
		if (other.isAliasType()) {
			return isSubtypeOf(other.getAliased());
		}
		if (other.isAbstractDataType() && other.isDefinedBy(this)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Return whether an ADT is defined by a certain type (is extended by a certain by).
	 * @param type 
	 * @return true iff this ADT can be constructed using values of the given type.
	 */
	public boolean isDefinedBy(Type type) {
		return false;
	}
	
	/**
	 * Return whether an ADT or an alias Type has any type parameters
	 * @return true iff the type is parameterized
	 */
	public boolean isParameterized() {
		return false;
	}

	/**
	 * Compute whether this type is a subtype of the other or vice versa
	 * @param other type to compare to
	 * @return true iff the types are comparable.
	 */
	public boolean comparable(Type other) {
		return (other == this) || isSubtypeOf(other) || other.isSubtypeOf(this);
	}

	/**
	 * Computer whether this type is equivalent to another. 
	 * @param other type to compare to
	 * @return true iff the two types are sub-types of each-other;
	 */
	public boolean equivalent(Type other) {
		return (other == this) || (isSubtypeOf(other) && other.isSubtypeOf(this));
	}
	
	/**
	 * If this type has parameters and there are parameter types embedded in it,
	 * instantiate will replace the parameter types using the given bindings.
	 * 
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
	 * @throws FactTypeError when a pattern can not be matches because the
	 *         matched types do not fit the bounds of the parameter types,
	 *         or when a pattern simply can not be matched because of 
	 *         incompatibility.         
	 */
	public void match(Type matched, Map<Type, Type> bindings) throws FactTypeError {
		if (!matched.isSubtypeOf(this)) {
			throw new FactTypeError(this + " does not match " + matched);
		}
	}
	
	public abstract <T> T accept(ITypeVisitor<T> visitor);

	/**
	 * @return a type descriptor suitable for use in serialization, which can be
	 *         passed to @{link TypeFactory#getTypeByDescriptor()}
	 */
	public IValue getTypeDescriptor(IValueFactory factory) {
		return TypeDescriptorFactory.getInstance().toTypeDescriptor(factory, this);
	}
	
	public boolean isRelationType() {
		return false;
	}

	public boolean isSetType() {
		return false;
	}

	public boolean isTupleType() {
		return false;
	}

	public boolean isListType() {
		return false;
	}

	public boolean isIntegerType() {
		return false;
	}

	public boolean isDoubleType() {
		return false;
	}

	public boolean isStringType() {
		return false;
	}

	public boolean isSourceLocationType() {
		return false;
	}

	public boolean isSourceRangeType() {
		return false;
	}

	public boolean isAliasType() {
		return false;
	}

	public boolean isValueType() {
		return false;
	}
	
	public boolean isVoidType() {
		return false;
	}

	public boolean isNodeType() {
		return false;
	}
	
	public boolean isConstructorType() {
		return false;
	}

	public boolean isAbstractDataType() {
		return false;
	}

	public boolean isMapType() {
		return false;
	}
	
	public boolean isBoolType() {
		return false;
	}
	
	public boolean isParameterType() {
		return false;
	}

	/**
	 * Build a double value. This method is supported by the DoubleType
	 * and TreeNodeTypes with anonymous constructor names and NamedTypes which
	 * are subtypes of the previous two. 
	 * 
	 * @param f   the factory to use
	 * @param arg the double to wrap
	 * @return a double value
	 */
	public IValue make(IValueFactory f, double arg) {
		throw new FactTypeError("This is not a double: " + this);
	}

	/**
	 * Build a integer value. This method is supported by the DoubleType
	 * and TreeNodeTypes with anonymous constructor names and NamedTypes which
	 * are subtypes of the previous two.
	 * 
	 * @param f   the factory to use
	 * @param arg the integer to wrap
	 * @return a integer value
	 */
	public IValue make(IValueFactory f, int arg) {
		throw new FactTypeError("This is not an int: " + this);
	}

	/**
	 * Build a string value. This method is supported by the DoubleType
	 *  and TreeNodeTypes with anonymous constructor names and NamedTypes which
	 * are subtypes of the previous two.
	 * 
	 * @param f   the factory to use
	 * @param arg the integer to wrap
	 * @return a integer value
	 */
	public IValue make(IValueFactory f, String arg) {
		throw new FactTypeError("This is not a string: " + this);
	}

	/** 
	 * Build a value that does not have any children. This method is supported
	 * by list, set, tuple, relation and map, nullary trees and
	 * NamedTypes that are sub-types of any of the previous. 
	 * 
	 * @param f
	 * @return
	 */
	public IValue make(IValueFactory f) {
		throw new FactTypeError(
				"This type does not have a zero argument constructor: " + this);
	}

	/**
	 * Build a value that has a number of children. This method is supported by
	 * tuples types and tree node types with the correct arity, also lists
	 * relations and maps can be constructed with a fixed size.
	 * 
	 * @param f    factory to use
	 * @param args arguments to use
	 * @return a value of the apropriate type
	 */
	public IValue make(IValueFactory f, IValue...args) {
		throw new FactTypeError(
				"This type does not have an array or variable argument list constructor: " + this);
	}
	
	public IValue make(IValueFactory f, String path, ISourceRange range) {
		throw new FactTypeError("This type is not a SourceLocationType: "
				+ this);
	}

	public IValue make(IValueFactory f, int startOffset, int length,
			int startLine, int endLine, int startCol, int endCol) {
		throw new FactTypeError("This type is not a SourceRangeType: " + this);
	}

	public IValue make(IValueFactory f, String name, IValue... children) {
		throw new FactTypeError("This type is not a TreeSortType: " + this);
	}
	
	public IValue make(IValueFactory f, boolean arg) {
		throw new FactTypeError("This type is not a BoolType: " + this);
	}
	
	/**
	 * Return a writer object. This works for Lists, Sets, Relations and Maps.
	 * Caller is responsible for assigning the result to I{List,Set,Relation,Map}Writer
	 * variable.
	 * @param f   factory to use 
	 * @return a writer
	 */
	public <T extends IWriter> T writer(IValueFactory f) {
		throw new FactTypeError("This type does not provide a writer interface: " + this);
	}

	
}