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

import java.math.BigInteger;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.INumber;
import org.eclipse.imp.pdb.facts.IRational;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.ICanBecomeABigInteger;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * Implementation for IInteger.
 * <br /><br />
 * Integer values that fall outside the 32-bit range will be store in BigIntegerValue instead.
 * 
 * @author Arnold Lankamp
 */
public class IntegerValue extends AbstractNumberValue implements IInteger, ICanBecomeABigInteger{
	private final static Type INTEGER_TYPE = TypeFactory.getInstance().integerType();
	
	private final static int SEVEN_BITS_MASK = 0x0000007f;
	private final static int FIFTEEN_BITS_MASK = 0x00007fff;
	private final static int TWENTYTHREE_BITS_MASK = 0x007fffff;
	public final static IntegerValue INTEGER_ONE = new IntegerValue(1);
	protected final int value;
	
	public IntegerValue(int value){
		super();
		this.value = value;
	}

	public IInteger toInteger() {
		return this;
	}
	
	public Type getType(){
		return INTEGER_TYPE;
	}
	
	public int intValue(){
		return value;
	}
	
	public long longValue(){
		return value;
	}
	
	public double doubleValue(){
		return value;
	}
	
	public IReal toReal(){
		return ValueFactory.getInstance().real(value);
	}
	
	public byte[] getTwosComplementRepresentation(){
		if((value & SEVEN_BITS_MASK) == value){
			byte[] data = new byte[1];
			data[0] = (byte) (value & 0x7f);
			return data;
		}else if((value & FIFTEEN_BITS_MASK) == value){
			byte[] data = new byte[2];
			data[0] = (byte) ((value >> 8) & 0x7f);
			data[1] = (byte) (value & 0xff);
			return data;
		}else if((value & TWENTYTHREE_BITS_MASK) == value){
			byte[] data = new byte[3];
			data[0] = (byte) ((value >> 16) & 0x7f);
			data[1] = (byte) ((value >> 8) & 0xff);
			data[2] = (byte) (value & 0xff);
			return data;
		}
		
		byte[] data = new byte[4];
		data[0] = (byte) ((value >> 24) & 0xff);
		data[1] = (byte) ((value >> 16) & 0xff);
		data[2] = (byte) ((value >> 8) & 0xff);
		data[3] = (byte) (value & 0xff);
		return data;
	}
	
	public BigInteger toBigInteger(){
		return new BigInteger(getTwosComplementRepresentation());
	}
	
	public boolean isEqual(IValue other) {
	  return equals(other);
	}
	
	public IInteger add(IInteger other){
		if(value == 0)
			return other;
		
		if(other instanceof BigIntegerValue){
			return other.add(this);
		}
		
		int otherIntValue = other.intValue();

		if(otherIntValue == 0)
			return this;
		
		int result = value + otherIntValue;
		if((value < 0) && (otherIntValue < 0) && (result >= 0)){// Overflow -> positive.
			byte[] intValueData = new byte[5];
			intValueData[0] = (byte) 0xff;
			intValueData[1] = (byte)((result >>> 24) & 0xff);
			intValueData[2] = (byte)((result >>> 16) & 0xff);
			intValueData[3] = (byte)((result >>> 8) & 0xff);
			intValueData[4] = (byte)(result & 0xff);
			
			return ValueFactory.getInstance().integer(new BigInteger(intValueData));
		}else if((value > 0) && (otherIntValue > 0) && (result < 0)){// Overflow -> negative.
			byte[] intValueData = new byte[5];
			intValueData[0] = 0;
			intValueData[1] = (byte)((result >>> 24) & 0xff);
			intValueData[2] = (byte)((result >>> 16) & 0xff);
			intValueData[3] = (byte)((result >>> 8) & 0xff);
			intValueData[4] = (byte)(result & 0xff);
			
			return ValueFactory.getInstance().integer(new BigInteger(intValueData));
		}
		
		return ValueFactory.getInstance().integer(result);
	}

	public IRational add(IRational other) {
		return (IRational ) other.add(this);
	}

	public IReal add(IReal other) {
		return (IReal) other.add(this);
	}
	    
	public INumber subtract(IReal other) {
		return toReal().subtract(other);
	}
	 
	public IInteger subtract(IInteger other){
		if(value == 0)
			return other.negate();
		
		if(other instanceof BigIntegerValue){
			return other.negate().subtract(this.negate());
		}
		
		int otherIntValue = other.intValue();

		if(otherIntValue == 0)
			return this;
		
		int result = value - otherIntValue;
		if((value < 0) && (otherIntValue > 0) && (result > 0)){// Overflow -> positive.
			byte[] intValueData = new byte[5];
			intValueData[0] = (byte) 0xff;
			intValueData[1] = (byte)((result >>> 24) & 0xff);
			intValueData[2] = (byte)((result >>> 16) & 0xff);
			intValueData[3] = (byte)((result >>> 8) & 0xff);
			intValueData[4] = (byte)(result & 0xff);
			
			return ValueFactory.getInstance().integer(new BigInteger(intValueData));
		}else if((value > 0) && (otherIntValue < 0) && (result < 0)){// Overflow -> negative.
			byte[] intValueData = new byte[5];
			intValueData[0] = 0;
			intValueData[1] = (byte)((result >>> 24) & 0xff);
			intValueData[2] = (byte)((result >>> 16) & 0xff);
			intValueData[3] = (byte)((result >>> 8) & 0xff);
			intValueData[4] = (byte)(result & 0xff);
			
			return ValueFactory.getInstance().integer(new BigInteger(intValueData));
		}
		
		return ValueFactory.getInstance().integer(result);
	}
	
	public IRational subtract(IRational other) {
		return toRational().subtract(other);
	}

	public IInteger multiply(IInteger other){
		if(value == 0)
			return this;
		if(value == 1)
			return other;
		
		if(other instanceof BigIntegerValue){
			return other.multiply(this);
		}
		
		int otherIntValue = other.intValue();
		if(otherIntValue == 0) return other;
		if(otherIntValue == 1) return this;
		
		boolean resultIsPositive = ((((value ^ otherIntValue) ^ 0x80000000) & 0x80000000) == 0x80000000);
		if(resultIsPositive){
			int div = Integer.MAX_VALUE / otherIntValue;
			if((value > 0)){
				if(value <= div){
					return ValueFactory.getInstance().integer(value * other.intValue());
				}
			}else{
				if(value >= div){
					return ValueFactory.getInstance().integer(value * other.intValue());
				}
			}
		}else{
			int div = Integer.MIN_VALUE / otherIntValue;
			if((value > 0)){
				if(value <= div){
					return ValueFactory.getInstance().integer(value * other.intValue());
				}
			}else{
				if(value >= div){
					return ValueFactory.getInstance().integer(value * other.intValue());
				}
			}
		}
		
		return ValueFactory.getInstance().integer(toBigInteger().multiply(((ICanBecomeABigInteger) other).toBigInteger()));
	}

	public IRational multiply(IRational other) {
    	return (IRational) other.multiply(this);
	}

	 public IReal multiply(IReal other) {
	    	return (IReal) other.multiply(this);
	 }
	
	public IInteger divide(IInteger other){
		if(value == 0)
			return this;
		if(other instanceof BigIntegerValue){
			return ValueFactory.getInstance().integer(toBigInteger().divide(((ICanBecomeABigInteger) other).toBigInteger()));
		}
		
		int otherIntValue = other.intValue();
		if(otherIntValue == 1)
			return this;
		return ValueFactory.getInstance().integer(value / otherIntValue);
	}
	

	public IRational divide(IRational other) {
		return toRational().divide(other);
	}

	public INumber divide(IInteger other, int precision) {
		return toReal().divide(other, precision);
	}

	public INumber divide(IRational other, int precision) {
		return toReal().divide(other, precision);
	}

	public IReal divide(IReal other, int precision) {
		return toReal().divide(other, precision);
	}
	
	public IInteger mod(IInteger other){
		if(other instanceof BigIntegerValue){
			if(value < 0){
				BigInteger m = ((BigIntegerValue)other).toBigInteger();
				// i.e. -1 % m = m + (-1)
				BigInteger res = m.add(toBigInteger());
				return ValueFactory.getInstance().integer(res);
			}
			return this;
		}
		int otherVal = other.intValue();
		int newValue = value % other.intValue();
		newValue = newValue >= 0 ? newValue : newValue + otherVal;
		return ValueFactory.getInstance().integer(newValue);
	}
	
	public IInteger remainder(IInteger other){
		if(other instanceof BigIntegerValue){
			return this;
		}
		
		return ValueFactory.getInstance().integer(value % other.intValue());
	}
	
	public IInteger negate(){
		if(value == 0)
			return this;
		else
			return ValueFactory.getInstance().integer((~((long) value)) + 1);
	}
	
	public IBool equal(IInteger other){
	  return ValueFactory.getInstance().bool(compare(other) == 0);
	}

	public IBool equal(IRational other) {
	  return other.equal(this);
	}

	public IBool equal(IReal other) {
	  return other.equal(this);
	}

	public IBool greater(IInteger other){
		return ValueFactory.getInstance().bool(compare(other) > 0);
	}

	public IBool greater(IRational other) {
    	return other.lessEqual(this);
	}
	 
	public IBool greater(IReal other) {
    	return other.lessEqual(this);
	}
    
	public IBool greaterEqual(IInteger other){
		return ValueFactory.getInstance().bool(compare(other) >= 0);
	}

	public IBool greaterEqual(IRational other) {
		return other.less(this);
	}

	public IBool greaterEqual(IReal other) {
	  return ValueFactory.getInstance().bool(compare(other) >= 0);
	}
	 
	public IBool less(IInteger other){
		return ValueFactory.getInstance().bool(compare(other) < 0);
	}
	
	public IBool less(IRational other) {
		return other.greaterEqual(this);
	}
	
	public IBool less(IReal other) {
		return other.greaterEqual(this);
    }

	public IBool lessEqual(IInteger other){
		return ValueFactory.getInstance().bool(compare(other) <= 0);
	}
	
	public IBool lessEqual(IRational other) {
		return other.greater(this);
	}
	
	public IBool lessEqual(IReal other) {
		return other.greater(this);
	}
	 
	public int compare(IInteger other){
		if(other instanceof BigIntegerValue){
			return ((~other.compare(this)) + 1);
		}
		
		if(value > other.intValue()) return 1;
		if(value < other.intValue()) return -1;
		
		return 0;
	}
	
	public int compare(INumber other) {
		if (other.getType().isIntegerType()) {
			return compare(other.toInteger());
		}
		return toReal().compare(other);
	}
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
		return v.visitInteger(this);
	}
	
	public int hashCode(){
		return value;
	}
	
	public boolean equals(Object o){
		if(o == null) return false;
		else if(o == this) return true;
		
		if(o.getClass() == getClass()){
			IntegerValue otherInteger = (IntegerValue) o;
			return (value == otherInteger.value);
		}
		else if(o instanceof IRational)
			return ((IRational)o).equals(this);
		else if(o instanceof IReal)
			return ((IReal)o).equals(this);
		
		return false;
	}
	
	public String getStringRepresentation(){
		return Integer.toString(value);
	}

	public int signum() {
		return value < 0 ? -1 : (value == 0 ? 0 : 1);
	}
	
	public IInteger abs() {
		return new IntegerValue(Math.abs(value));
	}

	public IRational toRational() {
		return new RationalValue(this, INTEGER_ONE);
	}
}
