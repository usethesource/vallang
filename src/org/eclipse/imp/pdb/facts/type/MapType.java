/*******************************************************************************
* Copyright (c) 2008 CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.pdb.facts.type;

public final class MapType extends Type {
    /*package*/ Type fKeyType;
    /*package*/ Type fValueType;
    

    /*package*/ MapType(Type keyType, Type valueType) {
    	fKeyType= keyType;
    	fValueType = valueType;
    }
    
    public Type getKeyType() {
    	return fKeyType;
    }
    
    public Type getValueType() {
    	return fValueType;
    }

    @Override
    public boolean isMapType() {
    	return true;
    }
  
    @Override
    public boolean isSubtypeOf(Type other) {
        if (other == this || other.isValueType()) {
        	return true;
        }
        else if (other.isMapType()) {
        	MapType o = (MapType) other;
        	
        	return fKeyType.isSubtypeOf(o.fKeyType) && fValueType.isSubtypeOf(o.fValueType);
        }
        
        return false;
    }

    @Override
    public Type lub(Type other) {
    	if (other.isSubtypeOf(this)) {
    		return this;
    	}
    	else if (other.isMapType()) {
    		MapType o = (MapType) other;
        	
    		return TypeFactory.getInstance().mapType(fKeyType.lub(o.fKeyType),
    				                                 fValueType.lub(o.fValueType));
    	}
    	else if (other.isNamedType()) {
    		return other.lub(this);
    	}
    	
    	return TypeFactory.getInstance().valueType();
    }

    
    @Override
    public String getTypeDescriptor() {
        return toString();
    }

    @Override
    public int hashCode() {
        return 56509 + 3511 * fKeyType.hashCode() + 1171 * fValueType.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MapType)) {
            return false;
        }
        MapType other= (MapType) obj;
        // N.B.: The element type must have been created and canonicalized before any
        // attempt to manipulate the outer type (i.e. SetType), so we can use object
        // identity here for the fEltType.
        return fKeyType == other.fKeyType && fValueType == other.fValueType;
    }

    @Override
    public String toString() {
        return "map[" + fKeyType.getTypeDescriptor() + "," + fValueType.getTypeDescriptor() + "]";
    }
}
