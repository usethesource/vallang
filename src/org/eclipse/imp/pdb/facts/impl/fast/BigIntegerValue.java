/*******************************************************************************
* Copyright (c) 2009 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Arnold Lankamp - interfaces and implementation
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.fast;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.ICanBecomeABigInteger;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * Specialized implementation for integer values that fall outside the 32-bit range.
 * 
 * @author Arnold Lankamp
 */
public class BigIntegerValue implements IInteger, ICanBecomeABigInteger{
	private final static Type INTEGER_TYPE = TypeFactory.getInstance().integerType();
	
	protected final BigInteger value;
	
	public BigIntegerValue(BigInteger value){
		super();
		
		this.value = value;
	}

	public Type getType(){
		return INTEGER_TYPE;
	}

	public int intValue(){
		return value.intValue();
	}

	public long longValue(){
		return value.longValue();
	}

	public IReal toReal(){
		return ValueFactory.getInstance().real(new BigDecimal(value));
	}

	public byte[] getTwosComplementRepresentation(){
		return value.toByteArray();
	}
	
	public BigInteger toBigInteger(){
		return value;
	}
	
	public IInteger add(IInteger other){
		BigInteger result = value.add(((ICanBecomeABigInteger) other).toBigInteger());
		
		int length = result.bitLength();
		if(length <= 32){
			if(length < 32 || result.compareTo(BigInteger.ZERO) != -1){
				return ValueFactory.getInstance().integer(result.intValue());
			}
		}
		
		return ValueFactory.getInstance().integer(result);
	}
	
	public IInteger subtract(IInteger other){
		BigInteger result = value.subtract(((ICanBecomeABigInteger) other).toBigInteger());
		
		int length = result.bitLength();
		if(length <= 32){
			if(length < 32 || result.compareTo(BigInteger.ZERO) != -1){
				return ValueFactory.getInstance().integer(result.intValue());
			}
		}
		
		return ValueFactory.getInstance().integer(result);
	}
	
	public IInteger multiply(IInteger other){
		BigInteger result = value.multiply(((ICanBecomeABigInteger) other).toBigInteger());
		// The result of this operation can never fit in a 32-bit integer, so no need to check.
		return ValueFactory.getInstance().integer(result);
	}
	
	public IInteger divide(IInteger other){
		BigInteger result = value.divide(((ICanBecomeABigInteger) other).toBigInteger());
		
		int length = result.bitLength();
		if(length <= 32){
			if(length < 32 || result.compareTo(BigInteger.ZERO) != -1){
				return ValueFactory.getInstance().integer(result.intValue());
			}
		}
		
		return ValueFactory.getInstance().integer(result);
	}
	
	public IInteger mod(IInteger other){
		BigInteger result = value.mod(((ICanBecomeABigInteger) other).toBigInteger());
		
		if(other instanceof IntegerValue){
			int integerResult = result.intValue();
			return ValueFactory.getInstance().integer(integerResult);
		}
		
		return ValueFactory.getInstance().integer(result);
	}
	
	public IInteger remainder(IInteger other){
		BigInteger result = value.remainder(((ICanBecomeABigInteger) other).toBigInteger());
		
		if(other instanceof IntegerValue){
			int integerResult = result.intValue();
			return ValueFactory.getInstance().integer(integerResult);
		}
		
		return ValueFactory.getInstance().integer(result);
	}
	
	public IInteger negate(){
		return ValueFactory.getInstance().integer(value.negate());
	}
	
	public IBool greater(IInteger other){
		return ValueFactory.getInstance().bool(compare(other) > 0);
	}
	
	public IBool greaterEqual(IInteger other){
		return ValueFactory.getInstance().bool(compare(other) >= 0);
	}
	
	public IBool less(IInteger other){
		return ValueFactory.getInstance().bool(compare(other) < 0);
	}
	
	public IBool lessEqual(IInteger other){
		return ValueFactory.getInstance().bool(compare(other) <= 0);
	}
	
	public int compare(IInteger other){
		return value.compareTo(((ICanBecomeABigInteger) other).toBigInteger());
	}
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
		return v.visitInteger(this);
	}
	
	public int hashCode(){
		return value.hashCode();
	}
	
	public boolean equals(Object o){
		if(o == null) return false;
		
		if(o.getClass() == getClass()){
			BigIntegerValue otherInteger = (BigIntegerValue) o;
			return value.equals(otherInteger.value);
		}
		
		return false;
	}
	
	public boolean isEqual(IValue other){
		return equals(other);
	}
	
	public String toString(){
		return value.toString();
	}
	
	public String getStringRepresentation(){
		return toString();
	}
}
