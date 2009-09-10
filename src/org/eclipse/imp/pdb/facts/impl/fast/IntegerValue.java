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
public class IntegerValue extends Value implements IInteger, ICanBecomeABigInteger{
	private final static Type INTEGER_TYPE = TypeFactory.getInstance().integerType();
	
	private final static int EIGTH_BITS_MASK = 0x000000ff;
	private final static int SIXTEEN_BITS_MASK = 0x0000ffff;
	private final static int TWENTYFOUR_BITS_MASK = 0x00ffffff;
	
	protected final int value;
	
	protected IntegerValue(int value){
		super();
		
		this.value = value;
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
	
	public IReal toReal(){
		return ValueFactory.getInstance().real(value);
	}
	
	public byte[] getTwosComplementRepresentation(){
		if((value & EIGTH_BITS_MASK) == value){
			byte[] data = new byte[1];
			data[0] = (byte) (value & 0xff);
			return data;
		}else if((value & SIXTEEN_BITS_MASK) == value){
			byte[] data = new byte[2];
			data[0] = (byte) ((value >> 8) & 0xff);
			data[1] = (byte) (value & 0xff);
			return data;
		}else if((value & TWENTYFOUR_BITS_MASK) == value){
			byte[] data = new byte[3];
			data[0] = (byte) ((value >> 16) & 0xff);
			data[1] = (byte) ((value >> 8) & 0xff);
			data[2] = (byte) (value & 0xff);
			return data;
		}else{
			byte[] data = new byte[4];
			data[0] = (byte) ((value >> 24) & 0xff);
			data[1] = (byte) ((value >> 16) & 0xff);
			data[2] = (byte) ((value >> 8) & 0xff);
			data[3] = (byte) (value & 0xff);
			return data;
		}
	}
	
	public BigInteger toBigInteger(){
		return new BigInteger(getTwosComplementRepresentation());
	}
	
	public IInteger add(IInteger other){
		if(other instanceof BigIntegerValue){
			return other.add(this);
		}
		
		int otherIntValue = other.intValue();
		
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
	
	public IInteger subtract(IInteger other){
		if(other instanceof BigIntegerValue){
			return other.subtract(this.negate());
		}
		
		int otherIntValue = other.intValue();
		
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
	
	public IInteger multiply(IInteger other){
		if(value == 0) return this;
		
		if(other instanceof BigIntegerValue){
			return other.multiply(this);
		}
		
		int otherIntValue = other.intValue();
		if(otherIntValue == 0) return other;
		
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
	
	public IInteger divide(IInteger other){
		if(other instanceof BigIntegerValue){
			return ValueFactory.getInstance().integer(toBigInteger().divide(((ICanBecomeABigInteger) other).toBigInteger()));
		}
		
		return ValueFactory.getInstance().integer(value / other.intValue());
	}
	
	public IInteger mod(IInteger other){
		if(other instanceof BigIntegerValue){
			if(value < 0){
				return ValueFactory.getInstance().integer((~value) + 1);
			}
			return this;
		}
		
		int newValue = value % other.intValue();
		newValue = newValue >= 0 ? newValue : ((~newValue) + 1);
		
		return ValueFactory.getInstance().integer(newValue);
	}
	
	public IInteger remainder(IInteger other){
		if(other instanceof BigIntegerValue){
			return this;
		}
		
		return ValueFactory.getInstance().integer(value % other.intValue());
	}
	
	public IInteger negate(){
		return ValueFactory.getInstance().integer((~value) + 1);
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
		if(other instanceof BigIntegerValue){
			return ((~other.compare(this)) + 1);
		}
		
		if(value > other.intValue()) return 1;
		if(value < other.intValue()) return -1;
		
		return 0;
	}
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
		return v.visitInteger(this);
	}
	
	public int hashCode(){
		return value;
	}
	
	public boolean equals(Object o){
		if(o == null) return false;
		
		if(o.getClass() == getClass()){
			IntegerValue otherInteger = (IntegerValue) o;
			return (value == otherInteger.value);
		}
		
		return false;
	}
	
	public boolean isEqual(IValue value){
		return equals(value);
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
}
