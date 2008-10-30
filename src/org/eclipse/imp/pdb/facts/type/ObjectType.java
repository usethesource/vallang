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

public class ObjectType<T> extends Type {
    /*package*/ Class<T> fClass;
    
    /*package*/ ObjectType(Class<T> clazz) {
    	fClass = clazz;
	}
    
	@Override
	public String getTypeDescriptor() {
		return toString();
	}

	@Override
	public boolean isSubtypeOf(Type other) {
		if (other == TypeFactory.getInstance().valueType()) {
			return true;
		}
		else {
			return other == this;
		}
	}

	@Override
	public Type lub(Type other) {
		if (other == this) {
			return this;
		}
		else if (other.isNamedType()) {
    		return lub(((NamedType) other).getSuperType());
    	}
		
		return TypeFactory.getInstance().valueType();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ObjectType)) {
			return false;
		}
		
		ObjectType<T> other = (ObjectType<T>) o;
		
		return other.fClass.equals(fClass);
	}
	
	@Override
	public int hashCode() {
		return 722222227 + 323232323 * fClass.hashCode();
	}
	
	@Override
	public String toString() {
		return "<class: " + fClass.getCanonicalName() + ">";
	}
	
	public boolean isObjectType() {
		return true;
	}

	public boolean checkClass(Class<T> clazz) {
		return fClass.equals(clazz);
	}
}
