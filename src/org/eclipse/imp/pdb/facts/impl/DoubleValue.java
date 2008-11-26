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

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IDouble;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/*package*/ class DoubleValue extends Value implements IDouble {
    private final double fValue;

    /*package*/ DoubleValue(double value) {
        super(TypeFactory.getInstance().doubleType());
        fValue= value;
    }
    
    private DoubleValue(DoubleValue other, String label, IValue anno) {
    	super(other, label, anno);
    	fValue = other.fValue;
    }

	public double getValue() {
        return fValue;
    }

    @Override
    public String toString() {
        return Double.toString(fValue);
    }
    
    public IDouble add(IDouble other) {
    	return new DoubleValue(fValue + other.getValue());
    }
    
    public IDouble subtract(IDouble other) {
    	return new DoubleValue(fValue - other.getValue());
    }
    
    public IDouble multiply(IDouble other) {
    	return new DoubleValue(fValue * other.getValue());
    }
    
    public IDouble divide(IDouble other) {
    	return new DoubleValue(fValue / other.getValue());
    }
    
    public IInteger round() {
    	return new IntegerValue((int) Math.round(fValue));
    }
    
    public IInteger floor() {
    	return new IntegerValue((int) Math.floor(fValue));
    }
    
    public IBool less(IDouble other) {
    	return new BoolValue(fValue < other.getValue());
    }
    
    public IBool lessEqual(IDouble other) {
    	return new BoolValue(fValue <= other.getValue());
    }
    
    public IBool greater(IDouble other) {
    	return new BoolValue(fValue > other.getValue());
    }
    
    public IBool greaterEqual(IDouble other) {
    	return new BoolValue(fValue >= other.getValue());
    }
    
    public int compare(IDouble other) {
    	if (fValue < other.getValue()) {
    		return -1;
    	}
    	else if (fValue > other.getValue()) {
    		return 1;
    	}
    	else {
    		return 0;
    	}
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
    
    public <T> T accept(IValueVisitor<T> v) throws VisitorException {
    	return v.visitDouble(this);
    };
    
    @Override
    protected IValue clone(String label, IValue anno) {
    	return new DoubleValue(this, label, anno);
    }
}
