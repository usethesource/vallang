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

public final class SetType extends Type {
    /*package*/ Type fEltType;

    /*package*/ SetType(Type eltType) {
    	fEltType= eltType;
    }
    
    public Type getElementType() {
    	return fEltType;
    }

    @Override
    public boolean isSetType() {
    	return true;
    }
  
    @Override
    public boolean isSubtypeOf(Type other) {
        if (other == this || other.isValueType()) {
        	return true;
        }
        else if (other.isSetType()) {
        	SetType o = (SetType) other;
        	
        	return fEltType.isSubtypeOf(o.fEltType);
        }
        else if (other.isRelationType()) {
        	RelationType o = (RelationType) other;
        	
        	if (fEltType.isTupleType()) {
        		TupleType t = (TupleType) fEltType;
        		
        		return t.isSubtypeOf(o.fTupleType);
        	}
        }
        
        
        return false;
    }

    @Override
    public Type lub(Type other) {
    	if (other.isSubtypeOf(this)) {
    		return this;
    	}
    	else if (other.isSetType()) {
    		SetType o = (SetType) other;
        	
    		return TypeFactory.getInstance().setTypeOf(fEltType.lub(o.fEltType));
    	}
    	else if (other.isRelationType()) {
    		RelationType o = (RelationType) other;
    		Type lub = fEltType.lub(o.fTupleType);
    		
    		if (lub.isTupleType()) {
				return TypeFactory.getInstance().relType((TupleType) lub);
    		}
    		
            // The upper bound on tuples of different arity is just ValueType,
            // so if the set's element type was tuple, but the arity didn't match,
            // just return set[Value].
            // N.B.: fEltType.lub(o.fEltType) would compute Value, so the below is just an optimization.
            return TypeFactory.getInstance().setTypeOf(TypeFactory.getInstance().valueType());
    	}
    	else if (other.isNamedType()) {
    		return lub(((NamedType) other).getSuperType());
    	}
    	
    	return TypeFactory.getInstance().valueType();
    }

    
    @Override
    public String getTypeDescriptor() {
        return toString();
    }

    @Override
    public int hashCode() {
        return 56509 + 3511 * fEltType.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SetType)) {
            return false;
        }
        SetType other= (SetType) obj;
        // N.B.: The element type must have been created and canonicalized before any
        // attempt to manipulate the outer type (i.e. SetType), so we can use object
        // identity here for the fEltType.
        return fEltType == other.fEltType;
    }

    @Override
    public String toString() {
        return "set[" + fEltType.getTypeDescriptor() + "]";
    }
}
