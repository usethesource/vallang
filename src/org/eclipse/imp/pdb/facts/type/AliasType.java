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
 * A AliasType is a named for a type, i.e. a type alias that can be
 * used to abstract from types that have a complex structure. Since a 
 * @{link Type} represents a set of values, a AliasType is defined to 
 * be an alias (because it represents the same set of values).
 * <p>
 * Detail: This API does not allow @{link IValue}'s of the basic types (int, 
 * double, etc) to be tagged with a AliasType. Instead the immediately
 * surrounding structure, such as a set or a relation will refer to the
 * AliasType.
 */
/*package*/ final class AliasType extends Type {
	/* package */ final String fName;
	/* package */ final Type fAliased;
	/* package */ final Type fParameters;
	
	/* package */ AliasType(String name, Type aliased) {
		fName = name;
		fAliased = aliased;
		fParameters = TypeFactory.getInstance().voidType();
	}
	
	/* package */ AliasType(String name, Type aliased, Type parameters) {
		fName = name;
		fAliased = aliased;
		fParameters = parameters;
	}
	
	@Override
	public boolean isAliasType() {
		return true;
	}
	
	@Override
	public boolean isParameterized() {
		return !fParameters.isVoidType();
	}
	
	@Override
	public String getName() {
		return fName;
	}
	
	@Override
	public Type getAliased() {
		return fAliased;
	}
	
	/**
	 * @return the first super type of this type that is not a AliasType.
	 */
	@Override
	public Type getHiddenType() {
		return fAliased;
	}

	@Override
	public boolean isSubtypeOf(Type other) {
		if (other == this) {
			return true;
		}
		else {
			return fAliased.isSubtypeOf(other);
		}
	}

	@Override
	public Type lub(Type other) {
		if (other == this) {
			return this;
		}
		else {
			return fAliased.lub(other);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(fName);
		if (!fParameters.isVoidType()) {
			sb.append("[");
			int idx= 0;
			for(Type elemType: fParameters) {
				if (idx++ > 0) {
					sb.append(",");
				}
				sb.append(elemType.toString());
			}
			sb.append("]");
		}
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		return 49991 + 49831 * fName.hashCode() + 67349 * fAliased.hashCode() + 1433 * fParameters.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof AliasType) {
			AliasType other = (AliasType) o;
			return fName.equals(other.fName) && fAliased == other.fAliased && fParameters == other.fParameters;
		}
		return false;
	}
	
	@Override
	public <T> T accept(ITypeVisitor<T> visitor) {
		return visitor.visitAlias(this);
	}
	
	@Override
	public IInteger make(IValueFactory f, int arg) {
		return (IInteger) fAliased.make(f, arg);
	}
	
	@Override
	public IValue make(IValueFactory f) {
		return fAliased.make(f);
	}
	
	@Override
	public IValue make(IValueFactory f, IValue... children) {
		return fAliased.make(f, children);
	}
	
	@Override
	public IValue make(IValueFactory f, String path, ISourceRange range) {
	    return fAliased.make(f, path, range);
	}
	
	@Override
	public IValue make(IValueFactory f,int startOffset, int length, int startLine, int endLine, int startCol, int endCol) {
		return fAliased.make(f, startOffset, length, startLine, endLine, startCol, endCol);
	}
	
	@Override
	public IValue make(IValueFactory f, double arg) {
		return fAliased.make(f, arg);
	}
	
	@Override
	public IValue make(IValueFactory f, String arg) {
		return fAliased.make(f, arg);
	}
	
	@Override
	public <T extends IWriter> T writer(IValueFactory f) {
		return fAliased.writer(f);
	}

	@Override
	public int getArity() {
		return fAliased.getArity();
	}
	
	@Override
	public Type getBound() {
		return fAliased.getBound();
	}
	
	@Override
	public Type getElementType() {
		return fAliased.getElementType();
	}
	
	@Override
	public int getFieldIndex(String fieldName) {
		return fAliased.getFieldIndex(fieldName);
	}
	
	@Override
	public String getFieldName(int i) {
		return fAliased.getFieldName(i);
	}
	
	@Override
	public Type getFieldType(int i) {
		return fAliased.getFieldType(i);
	}
	
	@Override
	public Type getFieldType(String fieldName) {
		return fAliased.getFieldType(fieldName);
	}
	
	@Override
	public Type getFieldTypes() {
		return fAliased.getFieldTypes();
	}
	
	@Override
	public Type getKeyType() {
		return fAliased.getKeyType();
	}
	
	@Override
	public Type getValueType() {
		return fAliased.getValueType();
	}

	@Override
	public boolean comparable(Type other) {
		return fAliased.comparable(other);
	}

	@Override
	public Type compose(Type other) {
		return fAliased.compose(other);
	}

	@Override
	public boolean hasFieldNames() {
		return fAliased.hasFieldNames();
	}

	@Override
	public Type instantiate(Map<Type, Type> bindings) {
		return new AliasType(fName, fAliased.instantiate(bindings), fParameters.instantiate(bindings));
	}
	
	@Override
	public void match(Type matched, Map<Type, Type> bindings)
			throws FactTypeError {
		super.match(matched, bindings);
		fAliased.match(matched, bindings);
	}

	@Override
	public boolean isBoolType() {
		return fAliased.isBoolType();
	}

	@Override
	public boolean isDoubleType() {
		return fAliased.isDoubleType();
	}

	@Override
	public boolean isIntegerType() {
		return fAliased.isIntegerType();
	}

	@Override
	public boolean isListType() {
		return fAliased.isListType();
	}

	@Override
	public boolean isMapType() {
		return fAliased.isMapType();
	}

	@Override
	public boolean isAbstractDataType() {
		return fAliased.isAbstractDataType();
	}

	@Override
	public boolean isParameterType() {
		return fAliased.isParameterType();
	}

	@Override
	public boolean isRelationType() {
		return fAliased.isRelationType();
	}

	@Override
	public boolean isSetType() {
		return fAliased.isSetType();
	}

	@Override
	public boolean isSourceLocationType() {
		return fAliased.isSourceLocationType();
	}

	@Override
	public boolean isSourceRangeType() {
		return fAliased.isSourceRangeType();
	}

	@Override
	public boolean isStringType() {
		return fAliased.isStringType();
	}

	@Override
	public boolean isConstructorType() {
		return fAliased.isConstructorType();
	}

	@Override
	public boolean isNodeType() {
		return fAliased.isNodeType();
	}

	@Override
	public boolean isTupleType() {
		return fAliased.isTupleType();
	}

	@Override
	public boolean isValueType() {
		return fAliased.isValueType();
	}

	@Override
	public boolean isVoidType() {
		return fAliased.isVoidType();
	}

	@Override
	public Iterator<Type> iterator() {
		return fAliased.iterator();
	}

	@Override
	public IValue make(IValueFactory f, boolean arg) {
		return fAliased.make(f, arg);
	}

	@Override
	public IValue make(IValueFactory f, String name, IValue... children) {
		return fAliased.make(f, name, children);
	}

	@Override
	public Type select(int... fields) {
		return fAliased.select(fields);
	}

	@Override
	public Type select(String... names) {
		return fAliased.select(names);
	}
}
