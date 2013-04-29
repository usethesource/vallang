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

import org.eclipse.imp.pdb.facts.IRational;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.type.TypeLattice.IKind;

/*package*/ final class RationalType extends Type {
    private final static RationalType sInstance= new RationalType();

    public static RationalType getInstance() {
        return sInstance;
    }

    private RationalType() {
    	super();
    }

    @Override
    public boolean isRationalType() {
    	return true;
    }
    
    /**
     * Should never need to be called; there should be only one instance of IntegerType
     */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof RationalType);
    }

    @Override
    public int hashCode() {
        return 212873;
    }
    
    @Override
    public String toString() {
        return "rat";
    }

    @Override
    protected IKind getKind() {
      return new TypeLattice.Rational();
    }
    
    @Override
    protected boolean acceptSubtype(IKind kind) {
      return kind.subRational(this);
    }
    
    @Override
    protected Type acceptLub(IKind kind) {
      return kind.lubRational(this);
    }
    
    @Override
    public <T> T accept(ITypeVisitor<T> visitor) {
    	return visitor.visitRational(this);
    }
    
    @Override
    public IRational make(IValueFactory f, int num, int denom) {
    	return f.rational(num, denom);
    }
    
    @Override
    public IRational make(IValueFactory f, TypeStore store, int num, int denom) {
    	return make(f, num, denom);
    }
}
