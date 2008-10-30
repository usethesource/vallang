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

package org.eclipse.imp.pdb.facts.impl;

import org.eclipse.imp.pdb.facts.IDouble;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.NamedType;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/*package*/ class DoubleValue extends Value implements IDouble {
    private final double fValue;

    /*package*/ DoubleValue(double value) {
        super(TypeFactory.getInstance().doubleType());
        fValue= value;
    }

    /*package*/ DoubleValue(NamedType type, double d) {
		super(type);
		fValue = d;
	}

	public double getValue() {
        return fValue;
    }

    @Override
    public String toString() {
        return Double.toString(fValue);
    }
    
    @Override
    public boolean equals(Object o) {
    	if (o instanceof DoubleValue) {
    		return ((DoubleValue) o).fValue == fValue;
    	}
    	return false;
    }
    
    @Override
    public int hashCode() {
    	long bits = Double.doubleToLongBits(fValue);
    	return (int)(bits ^ (bits >>> 32));
    }
    
    public IValue accept(IValueVisitor v) throws VisitorException {
    	return v.visitDouble(this);
    };
    
    @Override
    protected Object clone() throws CloneNotSupportedException {
    	return new DoubleValue(fValue);
    }
}
