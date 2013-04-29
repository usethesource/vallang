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

import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.type.TypeLattice.IKind;

/*package*/ final class IntegerType extends Type {
    private final static IntegerType sInstance= new IntegerType();

    public static IntegerType getInstance() {
        return sInstance;
    }

    private IntegerType() {
    	super();
    }

    @Override
    public boolean isIntegerType() {
    	return true;
    }
    
    /**
     * Should never need to be called; there should be only one instance of IntegerType
     */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof IntegerType);
    }

    @Override
    public int hashCode() {
        return 74843;
    }

    @Override
    public String toString() {
        return "int";
    }
    
    @Override
    protected IKind getKind() {
      return new TypeLattice.Integer();
    }
    
    @Override
    protected boolean acceptSubtype(IKind kind) {
      return kind.subInteger(this);
    }
    
    @Override
    protected Type acceptLub(IKind kind) {
      return kind.lubInteger(this);
    }
    
    @Override
    public <T> T accept(ITypeVisitor<T> visitor) {
    	return visitor.visitInteger(this);
    }
    
    @Override
    public IInteger make(IValueFactory f, int arg) {
    	return f.integer(arg);
    }
    
    @Override
    public IInteger make(IValueFactory f, TypeStore store, int arg) {
    	return make(f, arg);
    }
}
