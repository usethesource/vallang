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

import java.util.Map;

import org.eclipse.imp.pdb.facts.ISourceRange;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.IWriter;

public abstract class Type {
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
		else if (other.isNamedType()) {
			return other.lub(this);
		}
		else {
			return TypeFactory.getInstance().valueType();
		}
	}

	

	public boolean isSubtypeOf(Type other) {
		// this is the default implementation. Subclasses should override
		// to take their immediate super types into account.
		Type base = other.getBaseType();
		return other.isValueType() || base.isValueType() || base == this;
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
	
	public Type getBaseType() {
		return this;
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

	public boolean isNamedType() {
		return false;
	}

	public boolean isValueType() {
		return false;
	}
	
	public boolean isVoidType() {
		return false;
	}

	public boolean isTreeType() {
		return false;
	}
	
	public boolean isTreeNodeType() {
		return false;
	}

	public boolean isNamedTreeType() {
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