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

/*package*/ final class RealType extends Type {
	private final static RealType sInstance = new RealType();

	public static RealType getInstance() {
		return sInstance;
	}

	private RealType() {
		super();
	}

	@Override
	public boolean isRealType() {
		return true;
	}
	
	/**
	 * Should never need to be called; there should be only one instance of
	 * IntegerType
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof RealType);
	}

	@Override
	public int hashCode() {
		return 84121;
	}

	@Override
    public boolean isSubtypeOf(Type other) {
    	if (other == this) {
    		return true;
    	}
    	if (other.isNumberType()) {
    		return true;
    	}
    	
    	return super.isSubtypeOf(other);
    }
    
    @Override
    public Type lub(Type other) {
    	if (other == this) {
    		return this;
    	}
    	if (!other.isVoidType() && (other.isNumberType() || other.isIntegerType())) {
    		return TypeFactory.getInstance().numberType();
    	}
    	return super.lub(other);
    }
    
	@Override
	public String toString() {
		return "real";
	}
	
	@Override
	public <T> T accept(ITypeVisitor<T> visitor) {
		return visitor.visitReal(this);
	}
	
	@Override
	public IValue make(IValueFactory f, double arg) {
		return f.real(arg);
	}
	
	@Override
	public IValue make(IValueFactory f, TypeStore store, double arg) {
		return make(f, arg);
	}
	
	@Override
	public IValue make(IValueFactory f, float arg) {
		return f.real(arg);
	}
	
	@Override
	public IValue make(IValueFactory f, TypeStore store, float arg) {
		return make(f, arg);
	}
}
