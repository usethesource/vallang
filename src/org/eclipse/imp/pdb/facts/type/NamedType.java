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

import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.ISourceRange;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.IWriter;

/**
 * A NamedType is a named for a type, i.e. a type alias that can be
 * used to abstract from types that have a complex structure. Since a 
 * @{link Type} represents a set of values, a NamedType is defined to 
 * be an alias (because it represents the same set of values).
 * <p>
 * Detail: This API does not allow @{link IValue}'s of the basic types (int, 
 * double, etc) to be tagged with a NamedType. Instead the immediately
 * surrounding structure, such as a set or a relation will refer to the
 * NamedType.
 */
/*package*/ final class NamedType extends Type {
	/* package */ final String fName;
	/* package */ final Type fSuperType;
	
	/* package */ NamedType(String name, Type superType) {
		fName = name;
		fSuperType = superType;
	}
	
	@Override
	public boolean isNamedType() {
		return true;
	}
	
	@Override
	public String getName() {
		return fName;
	}
	
	@Override
	public Type getSuperType() {
		return fSuperType;
	}
	
	/**
	 * @return the first super type of this type that is not a NamedType.
	 */
	@Override
	public Type getHiddenType() {
		return fSuperType;
	}

	@Override
	public boolean isSubtypeOf(Type other) {
		if (other == this) {
			return true;
		}
		else {
			return fSuperType.isSubtypeOf(other);
		}
	}

	@Override
	public Type lub(Type other) {
		if (other == this) {
			return this;
		}
		else {
			return fSuperType.lub(other);
		}
	}
	
	@Override
	public String toString() {
		return fName;
	}
	
	@Override
	public int hashCode() {
		return 49991 + 49831 * fName.hashCode() + 67349 * fSuperType.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof NamedType) {
			NamedType other = (NamedType) o;
			return fName.equals(other.fName) && fSuperType == other.fSuperType;
		}
		return false;
	}
	
	@Override
	public <T> T accept(ITypeVisitor<T> visitor) {
		return visitor.visitNamed(this);
	}
	
	@Override
	public IInteger make(IValueFactory f, int arg) {
		return (IInteger) fSuperType.make(f, arg);
	}
	
	@Override
	public IValue make(IValueFactory f) {
		return fSuperType.make(f);
	}
	
	@Override
	public IValue make(IValueFactory f, IValue... children) {
		return fSuperType.make(f, children);
	}
	
	@Override
	public IValue make(IValueFactory f, String path, ISourceRange range) {
	    return fSuperType.make(f, path, range);
	}
	
	@Override
	public IValue make(IValueFactory f,int startOffset, int length, int startLine, int endLine, int startCol, int endCol) {
		return fSuperType.make(f, startOffset, length, startLine, endLine, startCol, endCol);
	}
	
	@Override
	public IValue make(IValueFactory f, double arg) {
		return fSuperType.make(f, arg);
	}
	
	@Override
	public IValue make(IValueFactory f, String arg) {
		return fSuperType.make(f, arg);
	}
	
	@Override
	public <T extends IWriter> T writer(IValueFactory f) {
		return fSuperType.writer(f);
	}

	@Override
	public int getArity() {
		return fSuperType.getArity();
	}
	
	@Override
	public Type getBound() {
		return fSuperType.getBound();
	}
	
	@Override
	public Type getElementType() {
		return fSuperType.getElementType();
	}
	
	@Override
	public int getFieldIndex(String fieldName) {
		return fSuperType.getFieldIndex(fieldName);
	}
	
	@Override
	public String getFieldName(int i) {
		return fSuperType.getFieldName(i);
	}
	
	@Override
	public Type getFieldType(int i) {
		return fSuperType.getFieldType(i);
	}
	
	@Override
	public Type getFieldType(String fieldName) {
		return fSuperType.getFieldType(fieldName);
	}
	
	@Override
	public Type getFieldTypes() {
		return fSuperType.getFieldTypes();
	}
	
	@Override
	public Type getKeyType() {
		return fSuperType.getKeyType();
	}
	
	@Override
	public Type getValueType() {
		return fSuperType.getValueType();
	}

	@Override
	public boolean comparable(Type other) {
		return fSuperType.comparable(other);
	}

	@Override
	public Type compose(Type other) {
		return fSuperType.compose(other);
	}

	@Override
	public boolean hasFieldNames() {
		return fSuperType.hasFieldNames();
	}

	@Override
	public Type instantiate(Map<Type, Type> bindings) {
		return this;
	}

	@Override
	public boolean isBoolType() {
		return fSuperType.isBoolType();
	}

	@Override
	public boolean isDoubleType() {
		return fSuperType.isDoubleType();
	}

	@Override
	public boolean isIntegerType() {
		return fSuperType.isIntegerType();
	}

	@Override
	public boolean isListType() {
		return fSuperType.isListType();
	}

	@Override
	public boolean isMapType() {
		return fSuperType.isMapType();
	}

	@Override
	public boolean isNamedTreeType() {
		return fSuperType.isNamedTreeType();
	}

	@Override
	public boolean isParameterType() {
		return fSuperType.isParameterType();
	}

	@Override
	public boolean isRelationType() {
		return fSuperType.isRelationType();
	}

	@Override
	public boolean isSetType() {
		return fSuperType.isSetType();
	}

	@Override
	public boolean isSourceLocationType() {
		return fSuperType.isSourceLocationType();
	}

	@Override
	public boolean isSourceRangeType() {
		return fSuperType.isSourceRangeType();
	}

	@Override
	public boolean isStringType() {
		return fSuperType.isStringType();
	}

	@Override
	public boolean isTreeNodeType() {
		return fSuperType.isTreeNodeType();
	}

	@Override
	public boolean isTreeType() {
		return fSuperType.isTreeType();
	}

	@Override
	public boolean isTupleType() {
		return fSuperType.isTupleType();
	}

	@Override
	public boolean isValueType() {
		return fSuperType.isValueType();
	}

	@Override
	public boolean isVoidType() {
		return fSuperType.isVoidType();
	}

	@Override
	public Iterator<Type> iterator() {
		return fSuperType.iterator();
	}

	@Override
	public IValue make(IValueFactory f, boolean arg) {
		return fSuperType.make(f, arg);
	}

	@Override
	public IValue make(IValueFactory f, String name, IValue... children) {
		return fSuperType.make(f, name, children);
	}

	@Override
	public Type select(int... fields) {
		return fSuperType.select(fields);
	}

	@Override
	public Type select(String... names) {
		return fSuperType.select(names);
	}
}
