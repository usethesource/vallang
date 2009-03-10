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

import java.math.BigDecimal;
import java.math.BigInteger;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/*package*/ class IntegerValue extends Value implements IInteger {
    private final BigInteger fValue;

    /*package*/ IntegerValue(int i) {
        super(TypeFactory.getInstance().integerType());
        fValue= BigInteger.valueOf(i);
    }
    
    /*package*/ IntegerValue(long i) {
        super(TypeFactory.getInstance().integerType());
        fValue= BigInteger.valueOf(i);
    }
    
    /*package*/ IntegerValue(BigInteger i) {
        super(TypeFactory.getInstance().integerType());
        fValue= i;
    }
    
    /*package*/ IntegerValue(byte[] a) {
		this(new BigInteger(a));
	}

	@Override
    public boolean equals(Object o) {
    	if (getClass() == o.getClass()) {
    		return fValue.equals(((IntegerValue) o).fValue);
    	}
    	return false;
    }
    
    public IInteger add(IInteger other) {
    	return new IntegerValue(fValue.add(((IntegerValue) other).fValue));
    }
    
    public IInteger negate() {
    	return new IntegerValue(fValue.negate());
    }
    
    public IInteger subtract(IInteger other) {
    	return new IntegerValue(fValue.subtract(((IntegerValue) other).fValue));
    }
    
    public IInteger multiply(IInteger other) {
    	return new IntegerValue(fValue.multiply(((IntegerValue) other).fValue));
    }
    
    public IInteger divide(IInteger other) {
    	return new IntegerValue(fValue.divide(((IntegerValue) other).fValue));
    }
    
    public IInteger remainder(IInteger other) {
    	return new IntegerValue(fValue.remainder(((IntegerValue) other).fValue));
    }
    
    public IInteger mod(IInteger other) {
    	return new IntegerValue(fValue.mod(((IntegerValue) other).fValue));
    }
    
    public IBool less(IInteger other) {
    	return new BoolValue(compare(other) < 0);
    }
    
    public IBool lessEqual(IInteger other) {
    	return new BoolValue(compare(other) <= 0);
    }
    
    public IBool greater(IInteger other) {
    	return new BoolValue(compare(other) > 0);
    }
    
    public IBool greaterEqual(IInteger other) {
    	return new BoolValue(compare(other) >= 0);
    }
    
    public IReal toReal() {
    	return new RealValue(new BigDecimal(fValue));
    }
    
    public int compare(IInteger other) {
    	return fValue.compareTo(((IntegerValue) other).fValue);
    }
    
    @Override
    public int hashCode() {
    	return fValue.hashCode();
    }
    
    public <T> T accept(IValueVisitor<T> v) throws VisitorException {
    	return v.visitInteger(this);
    }
    
    public String getStringRepresentation() {
    	return fValue.toString();
    }
    
    public byte[] getTwosComplementRepresentation() {
    	return fValue.toByteArray();
    }
    
    public int intValue() {
    	return fValue.intValue();
    }
    
    
    public long longValue() {
    	return fValue.longValue();
    }
}
