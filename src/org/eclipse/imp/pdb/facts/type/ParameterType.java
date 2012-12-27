/*******************************************************************************
* Copyright (c) 2008 CWI.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Jurgen Vinju - initial API and implementation
*******************************************************************************/

package org.eclipse.imp.pdb.facts.type;

import java.util.Map;

import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;



/**
 * A Parameter Type can be used to represent an abstract type,
 * i.e. a type that needs to be instantiated with an actual type
 * later.
 */
/*package*/ final class ParameterType extends Type {
	private final String fName;
	private final Type fBound;
	
	/* package */ ParameterType(String name, Type bound) {
		fName = name;
		fBound = bound;
	}
	
	/* package */ ParameterType(String name) {
		fName = name;
		fBound = TypeFactory.getInstance().valueType();
	}
	
	@Override
	public boolean isParameterType() {
		return true;
	}
	
	@Override
	public boolean isValueType() {
		return fBound.isValueType();
	}
	
	@Override
	public boolean isBoolType() {
		return fBound.isBoolType();
	}
	
	@Override
	public boolean isRealType() {
		return fBound.isRealType();
	}
	
	@Override
	public boolean isIntegerType() {
		return fBound.isIntegerType();
	}
	
	@Override
	public boolean isListType() {
		return fBound.isListType();
	}
	
	@Override
	public boolean isMapType() {
		return fBound.isMapType();
	}
	
	@Override
	public boolean isAbstractDataType() {
		return fBound.isAbstractDataType();
	}
	
	@Override
	public boolean isAliasType() {
		return fBound.isAliasType();
	}
	
	@Override
	public boolean isRelationType() {
		return fBound.isRelationType();
	}
	
	@Override
	public boolean isListRelationType() {
		return fBound.isListRelationType();
	}
	
	@Override
	public boolean isSetType() {
		return fBound.isSetType();
	}
	
	@Override
	public boolean isSourceLocationType() {
		return fBound.isSourceLocationType();
	}
	
	@Override
	public boolean isSourceRangeType() {
		return fBound.isSourceRangeType();
	}
	
	@Override
	public boolean isStringType() {
		return fBound.isStringType();
	}
	
	@Override
	public boolean isConstructorType() {
		return fBound.isConstructorType();
	}
	
	@Override
	public boolean isNodeType() {
		return fBound.isNodeType();
	}
	
	@Override
	public boolean isTupleType() {
		return fBound.isTupleType();
	}
	
	@Override
	public boolean isVoidType() {
		return fBound.isVoidType();
	}
	
	@Override
	public Type getBound() {
		return fBound;
	}
	
	@Override
	public String getName() {
		return fName;
	}
	
	@Override
	public int getArity(){
		return fBound.getArity();
	}
	
	@Override
	public Type getFieldType(int i){
		return fBound.getFieldType(i);
	}
	
	@Override
	public String[] getFieldNames(){
		return fBound.getFieldNames();
	}
	
	@Override
	public boolean isSubtypeOf(Type other) {
		if (other == this) {
			return true;
		}
		
		return fBound.isSubtypeOf(other);
	}

	@Override
	public Type lub(Type other) {
		if (other == this) {
			return this;
		}
		
		return getBound().lub(other);
	}
	
	@Override
	public String toString() {
		return fBound.isValueType() ? "&" + fName : "&" + fName + "<:" + fBound.toString();
	}
	
	@Override
	public int hashCode() {
		return 49991 + 49831 * fName.hashCode() + 133020331 * fBound.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof ParameterType) {
			ParameterType other = (ParameterType) o;
			return fName.equals(other.fName) && fBound == other.fBound;
		}
		return false;
	}
	
	@Override
	public <T> T accept(ITypeVisitor<T> visitor) {
		return visitor.visitParameter(this);
	}

	@Override
	public boolean match(Type matched, Map<Type, Type> bindings)
			throws FactTypeUseException {
		if (!super.match(matched, bindings)) {
			return false;
		}
		
		Type earlier = bindings.get(this);
		if (earlier != null) {
			Type lub = earlier.lub(matched);
			if (!lub.isSubtypeOf(getBound())) {
				return false;
			}
			
			bindings.put(this, lub);
		}
		else {
			bindings.put(this, matched);
		}
		
		return true;
	}
	
	@Override
	public Type instantiate(Map<Type, Type> bindings) {
		Type result = bindings.get(this);
		return result != null ? result : this;
	}
}
