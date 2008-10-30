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

public class NamedType extends Type {
	/* package */ String fName;
	/* package */ Type fSuperType;
	
	/* package */ NamedType(String name, Type superType) {
		fName = name;
		fSuperType = superType;
	}
	
	@Override
	public boolean isNamedType() {
		return true;
	}
	
	public Type getSuperType() {
		return fSuperType;
	}
	
	/**
	 * @return the first super type of this type that is not a NamedType.
	 */
	public Type getBaseType() {
		Type baseType = fSuperType;
		while (baseType.isNamedType()) {
			baseType = ((NamedType) baseType).getSuperType();
		}
		return baseType;
	}

	@Override
	public String getTypeDescriptor() {
            return "<" + fName +  " = " + fSuperType.toString() + ">";
	}

	@Override
	public boolean isSubtypeOf(Type other) {
		if (other == this || other.isValueType()) {
			return true;
		}
		
		return fSuperType.isSubtypeOf(other);
	}

	@Override
	public Type lub(Type other) {
		if (other == this) {
			return this;
		}
		else if (other.isSubtypeOf(this)) {
			return this;
		}
		else if (this.isSubtypeOf(other)) {
			return other;
		}
		else {
			return getBaseType().lub(other.getBaseType());
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

}
