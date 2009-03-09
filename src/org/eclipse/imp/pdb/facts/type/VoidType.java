/*******************************************************************************
* Copyright (c) 2008 CWI.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Jurgen Vinju - jurgen@vinju.org

*******************************************************************************/

package org.eclipse.imp.pdb.facts.type;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.imp.pdb.facts.exceptions.IllegalOperationException;


/** 
 * The void type represents an empty collection of values. I.e. it 
 * is a subtype of all types, the bottom of the type hierarchy.
 * 
 * This type does not have any values with it naturally and can,
 * for example, be used to elegantly initialize computations that 
 * involve least upper bounds.
 */
/*package*/ final class VoidType extends Type {

	private static class InstanceHolder {
		static VoidType sInstance = new VoidType(); 
	}
	
	private VoidType() { }
	
	@Override
	public <T> T accept(ITypeVisitor<T> visitor) {
		return visitor.visitVoid(this);
	}

	@Override
	public boolean isSubtypeOf(Type other) {
		// the empty set is a subset of all sets,
		// and so the void type is a subtype of all types.
		return true;
	}

	@Override
	public boolean isVoidType() {
		return true;
	}
	
	@Override
	public Type lub(Type other) {
		return other;
	}
	
	@Override
	public String toString() {
		return "void";
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof VoidType;
	}
	
	@Override
	public int hashCode() {
		return 199; 
	}

	public static VoidType getInstance() {
		return InstanceHolder.sInstance;
	}
	
	@Override
	public int getArity() {
		return 0;
	}
	
	@Override
	public Type getAliased() {
		return this;
	}
	
	@Override
	public Type getElementType() {
		return this;
	}
	
	@Override
	public int getFieldIndex(String fieldName) {
		throw new IllegalOperationException("getFieldIndex", this);
	}
	
	@Override
	public boolean hasFieldNames() {
		return false;
	}
	
	@Override
	public String getFieldName(int i) {
		return null;
	}
	
	@Override
	public Type select(int... fields) {
		return this;
	}
	
	@Override
	public Type select(String... names) {
		return this;
	}
	
	@Override
	public Type getAbstractDataType() {
		return this;
	}
	
	@Override
	public Type getBound() {
		return this;
	}
	
	@Override
	public Type getTypeParameters() {
		return this;
	}
	
	@Override
	public Type getFieldType(int i) {
		return this;
	}
	
	@Override
	public Type getFieldType(String fieldName) {
		return this;
	}
	
	@Override
	public Type getFieldTypes() {
		return this;
	}
	
	@Override
	public Type getKeyType() {
		return this;
	}
	
	@Override
	public Type getValueType() {
		return this;
	}

	@Override
	public boolean isTupleType() {
		return true;
	}
	
	@Override
	public boolean isBoolType() {
		return true;
	}
	
	@Override
	public boolean isRealType() {
		return true;
	}
	
	@Override
	public boolean isIntegerType() {
		return true;
	}
	
	@Override
	public boolean isListType() {
		return true;
	}
	
	@Override
	public boolean isMapType() {
		return true;
	}
	
	@Override
	public boolean isAbstractDataType() {
		return true;
	}
	
	@Override
	public boolean isAliasType() {
		return true;
	}
	
	@Override
	public boolean isParameterType() {
		return true;
	}
	
	@Override
	public boolean isRelationType() {
		return true;
	}
	
	@Override
	public boolean isSetType() {
		return true;
	}
	
	@Override
	public boolean isSourceLocationType() {
		return true;
	}
	
	@Override
	public boolean isSourceRangeType() {
		return true;
	}
	
	@Override
	public boolean isStringType() {
		return true;
	}
	
	@Override
	public boolean isConstructorType() {
		return true;
	}
	
	@Override
	public boolean isNodeType() {
		return true;
	}
	
	@Override
	public boolean isValueType() {
		return true;
	}
	
	@Override
	public boolean comparable(Type other) {
		return true;
	}
	
	@Override
	public Type compose(Type other) {
		return this;
	}
	
	@Override
	public String getName() {
		return "";
	}
	
	@Override
	public Type instantiate(TypeStore store, Map<Type, Type> bindings) {
		return this;
	}
	
	@Override
	public Type getHiddenType() {
		return this;
	}
	
	@Override
	public Iterator<Type> iterator() {
		return new Iterator<Type>() {
			boolean once = false;
			public boolean hasNext() {
				return !once;
			}

			public Type next() {
				once = true;
				return VoidType.this;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}

