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

import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;

public class SetType extends Type {
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
        if (other.isSetType()) {
        	SetType o = (SetType) other;
        	
        	return fEltType.isSubtypeOf(o.fEltType);
        }
        else {
        	return super.isSubtypeOf(other);
        }
    }

    @Override
    public Type lub(Type other) {
    	if (other.isSetType()) {
    		SetType o = (SetType) other;
    		return TypeFactory.getInstance().setType(fEltType.lub(o.fEltType));
    	}
    	else {
    		return super.lub(other);
    	}
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
        return "set[" + fEltType + "]";
    }
    
    @Override
    public <T> T accept(ITypeVisitor<T> visitor) {
    	return visitor.visitSet(this);
    }

	public IValue make(IValueFactory f) {
		return f.set(fEltType);
	}
	
	public IValue make(IValueFactory f, IValue... elems) {
		return f.set(elems);
	}
	
	@Override
	public ISetWriter writer(IValueFactory f) {
		return f.setWriter(fEltType);
	}
}
