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

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;

/**
 * A type for values that are either ints or reals
 */
/*package*/ final class NumberType extends Type {
	private final static NumberType sInstance= new NumberType();
	
	protected static class Subtype extends DefaultSubtype {
	  @Override
	  public Boolean visitNumber(Type type) {
	    return true;
	  }
	}
	
    public static NumberType getInstance() {
        return sInstance;
    }

    @Override
    public boolean isNumberType() {
    	return true;
    }
    
    @Override
    public String toString() {
        return "num";
    }
    
    @Override
    protected DefaultSubtype getSubtype() {
      return new Subtype();
    }
     
    @Override
    public Type lub(Type other) {
    	if (other == this) {
    		return this;
    	}
    	if (other.isIntegerType() || other.isRealType() || other.isRationalType()) {
    		return this;
    	}
    	return super.lub(other);
    }
    
    /**
     * Should never be called, NodeType is a singleton 
     */
    @Override
    public boolean equals(Object o) {
        return (o instanceof NumberType);
    }
    
    @Override
    public int hashCode() {
    	return 133020331;
    }
    
    @Override
    public <T> T accept(ITypeVisitor<T> visitor) {
    	return visitor.visitNumber(this);
    }
    
    @Override
    public IValue make(IValueFactory f, double arg) {
    	return TypeFactory.getInstance().realType().make(f, arg);
    }
    
    @Override
    public IValue make(IValueFactory f, float arg) {
    	return TypeFactory.getInstance().realType().make(f, arg);
    }
    
    @Override
    public IValue make(IValueFactory f, int arg) {
    	return TypeFactory.getInstance().integerType().make(f, arg);
    }
    
    @Override
    public IValue make(IValueFactory f, int num, int denom) {
    	return TypeFactory.getInstance().rationalType().make(f, num, denom);
    }
    
    @Override
    public IValue make(IValueFactory f, TypeStore s, double arg) {
    	return TypeFactory.getInstance().realType().make(f, arg);
    }
    
    @Override
    public IValue make(IValueFactory f, TypeStore s, float arg) {
    	return TypeFactory.getInstance().realType().make(f, arg);
    }
    
    @Override
    public IValue make(IValueFactory f, TypeStore s, int arg) {
    	return TypeFactory.getInstance().integerType().make(f, arg);
    }

    @Override
    public IValue make(IValueFactory f, TypeStore s, int num, int denom) {
    	return TypeFactory.getInstance().integerType().make(f, num, denom);
    }
}
