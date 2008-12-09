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



/**
 * A Parameter Type can be used to represent an abstract type,
 * i.e. a type that needs to be instantiated with an actual type
 * later.
 */
public class ParameterType extends Type {
	/* package */ final String fName;
	/* package */ final Type fBound;
	
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
	
	public Type getBound() {
		return fBound;
	}
	
	public String getName() {
		return fName;
	}
	
	@Override
	public Type getBaseType() {
		return fBound;
	}
	
	@Override
	public boolean isSubtypeOf(Type other) {
		if (other == this) {
			return true;
		}
		else {
			return fBound.isSubtypeOf(other);
		}
	}

	@Override
	public Type lub(Type other) {
		if (other == this) {
			return this;
		}
		else {
			return getBound().lub(other);
		}
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

	
}
