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

import org.eclipse.imp.pdb.facts.ISourceRange;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;

public abstract class Type {
	/**
	 * @return the least upper bound type of the receiver and the argument type
	 */
	public abstract Type lub(Type other);

	public abstract boolean isSubtypeOf(Type other);

	public abstract IValue accept(ITypeVisitor visitor);

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

	public boolean isObjectType() {
		return false;
	}

	public boolean isTreeNodeType() {
		return false;
	}

	public boolean isTreeSortType() {
		return false;
	}

	public boolean isMapType() {
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

	public IValue make(IValueFactory f, IValue...args) {
		throw new FactTypeError(
				"This type does not have an array or variable argument list constructor: " + this);
	}
	
	/**
	 * Build a value that has a number of children. This method is supported by tuple and
	 * tree, set, list, relation and NamedTypes that are subtypes of any of the previous.
	 * 
	 * @param f
	 * @param arg0
	 * @return
	 */
	public IValue make(IValueFactory f, IValue first, IValue... rest) {
		throw new FactTypeError(
				"This type does not have an array or variable argument list constructor: " + this);
	}

	public <T> IValue make(IValueFactory f, T arg) {
		throw new FactTypeError("This type is not an ObjectType: " + this);
	}

	public IValue make(IValueFactory f, String path, ISourceRange range) {
		throw new FactTypeError("This type is not a SourceLocationType: "
				+ this);
	}

	public IValue make(IValueFactory f, int startOffset, int length,
			int startLine, int endLine, int startCol, int endCol) {
		throw new FactTypeError("This type is not a SourceRangeType: " + this);
	}
}
