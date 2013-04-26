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

/*package*/ final class BoolType extends Type {
	private final static BoolType sInstance = new BoolType();

	/* package */static BoolType getInstance() {
		return sInstance;
	}

	private BoolType() {
		super();
	}

	@Override
	public boolean isBoolType() {
		return true;
	}
	
	/**
	 * Should never need to be called; there should be only one instance of
	 * IntegerType
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof BoolType);
	}

	@Override
	public int hashCode() {
		return 84121;
	}

	@Override
	public String toString() {
		return "bool";
	}
	
	@Override
	protected ValueSubtype getSubtype() {
	  return new ValueSubtype() {
	    @Override
	    public ValueSubtype visitBool(Type boolType) {
	      setLub(boolType);
        setSubtype(true);
        return this;
	    }
	  };
	}
	
	@Override
	public <T> T accept(ITypeVisitor<T> visitor) {
		return visitor.visitBool(this);
	}
	
	@Override
	public IValue make(IValueFactory f, boolean arg) {
		return f.bool(arg);
	}
	
	@Override
	public IValue make(IValueFactory f, TypeStore store, boolean arg) {
		return make(f, arg);
	}
}
