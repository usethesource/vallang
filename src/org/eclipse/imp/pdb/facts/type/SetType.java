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

import java.util.Map;

import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;

/*package*/ class SetType extends Type {
	protected static class Subtype extends ValueSubtype {
	  private final Type fEltType;

    public Subtype(Type eltType) {
	    this.fEltType = eltType;
    }
    
    @Override
    public ValueSubtype visitSet(Type type) {
      setLub(TF.setType(fEltType.lub(type.getElementType())));
      setSubtype(fEltType.isSubtypeOf(type.getElementType()));
      return this;
    }
    
    @Override
    public ValueSubtype visitRelationType(Type type) {
      return visitSet(type);
    }
  }

  protected final Type fEltType;

    /*package*/ SetType(Type eltType) {
    	fEltType= eltType;
    }
    
    @Override
    public Type getElementType() {
    	return fEltType;
    }

    @Override
    public boolean isSetType() {
    	return true;
    }

    @Override
    protected ValueSubtype getSubtype() {
      return new Subtype(fEltType);
    }

    @Override
    public Type carrier() {
    	return this;
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

    @Override
	public IValue make(IValueFactory f) {
		return f.set(fEltType);
	}
	
    @Override
	public IValue make(IValueFactory f, IValue... elems) {
		return f.set(elems);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ISetWriter writer(IValueFactory f) {
		return f.setWriter(fEltType);
	}
	
	@Override
	public boolean match(Type matched, Map<Type, Type> bindings)
			throws FactTypeUseException {
		return super.match(matched, bindings)
				&& getElementType().match(matched.getElementType(), bindings);
	}
	
	@Override
	public Type instantiate(Map<Type, Type> bindings) {
		return TypeFactory.getInstance().setType(getElementType().instantiate(bindings));
	}
}

