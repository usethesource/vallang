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
package org.eclipse.imp.pdb.facts.impl.shared;

import java.math.BigInteger;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.impl.ICanBecomeABigInteger;
import org.eclipse.imp.pdb.facts.impl.fast.IntegerValue;
import org.eclipse.imp.pdb.facts.impl.util.sharing.IShareable;

/**
 * Implementation of shareable integers.
 * 
 * @author Arnold Lankamp
 */
public class SharedIntegerValue extends IntegerValue implements IShareable{
	
	protected SharedIntegerValue(int value){
		super(value);
	}
	
	public IReal toReal(){
		return SharedValueFactory.getInstance().real(value);
	}
	
	public IInteger add(IInteger other){
		if(other instanceof SharedBigIntegerValue){
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
			
			return SharedValueFactory.getInstance().integer(new BigInteger(intValueData));
		}else if((value > 0) && (otherIntValue > 0) && (result < 0)){// Overflow -> negative.
			byte[] intValueData = new byte[5];
			intValueData[0] = 0;
			intValueData[1] = (byte)((result >>> 24) & 0xff);
			intValueData[2] = (byte)((result >>> 16) & 0xff);
			intValueData[3] = (byte)((result >>> 8) & 0xff);
			intValueData[4] = (byte)(result & 0xff);
			
			return SharedValueFactory.getInstance().integer(new BigInteger(intValueData));
		}
		
		return SharedValueFactory.getInstance().integer(result);
	}
	
	public IInteger subtract(IInteger other){
		if(other instanceof SharedBigIntegerValue){
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
			
			return SharedValueFactory.getInstance().integer(new BigInteger(intValueData));
		}else if((value > 0) && (otherIntValue < 0) && (result < 0)){// Overflow -> negative.
			byte[] intValueData = new byte[5];
			intValueData[0] = 0;
			intValueData[1] = (byte)((result >>> 24) & 0xff);
			intValueData[2] = (byte)((result >>> 16) & 0xff);
			intValueData[3] = (byte)((result >>> 8) & 0xff);
			intValueData[4] = (byte)(result & 0xff);
			
			return SharedValueFactory.getInstance().integer(new BigInteger(intValueData));
		}
		
		return SharedValueFactory.getInstance().integer(result);
	}
	
	public IInteger multiply(IInteger other){
		if(other instanceof SharedBigIntegerValue){
			return other.add(this);
		}
		
		int otherIntValue = other.intValue();
		boolean resultIsPositive = ((((value ^ otherIntValue) ^ 0x80000000) & 0x80000000) == 0x80000000);
		if(resultIsPositive){
			int div = Integer.MAX_VALUE / otherIntValue;
			if((value > 0)){
				if(value <= div){
					return SharedValueFactory.getInstance().integer(value * other.intValue());
				}
			}else{
				if(value >= div){
					return SharedValueFactory.getInstance().integer(value * other.intValue());
				}
			}
		}else{
			int div = Integer.MIN_VALUE / otherIntValue;
			if((value > 0)){
				if(value <= div){
					return SharedValueFactory.getInstance().integer(value * other.intValue());
				}
			}else{
				if(value >= div){
					return SharedValueFactory.getInstance().integer(value * other.intValue());
				}
			}
		}
		
		return SharedValueFactory.getInstance().integer(toBigInteger().multiply(((ICanBecomeABigInteger) other).toBigInteger()));
	}
	
	public IInteger divide(IInteger other){
		if(other instanceof SharedBigIntegerValue){
			return SharedValueFactory.getInstance().integer(toBigInteger().divide(((ICanBecomeABigInteger) other).toBigInteger()));
		}
		
		return SharedValueFactory.getInstance().integer(value / other.intValue());
	}
	
	public IInteger mod(IInteger other){
		if(other instanceof SharedBigIntegerValue){
			if(value < 0){
				return SharedValueFactory.getInstance().integer((~value) + 1);
			}
			return this;
		}
		
		int newValue = value % other.intValue();
		newValue = newValue >= 0 ? newValue : ((~newValue) + 1);
		
		return SharedValueFactory.getInstance().integer(newValue);
	}
	
	public IInteger remainder(IInteger other){
		if(other instanceof SharedBigIntegerValue){
			return this;
		}
		
		return SharedValueFactory.getInstance().integer(value % other.intValue());
	}
	
	public IInteger negate(){
		return SharedValueFactory.getInstance().integer((~value) + 1);
	}


	public IBool greater(IInteger other){
		return SharedValueFactory.getInstance().bool(compare(other) > 0);
	}

	public IBool greaterEqual(IInteger other){
		return SharedValueFactory.getInstance().bool(compare(other) >= 0);
	}

	public IBool less(IInteger other){
		return SharedValueFactory.getInstance().bool(compare(other) < 0);
	}

	public IBool lessEqual(IInteger other){
		return SharedValueFactory.getInstance().bool(compare(other) <= 0);
	}
	
	public int compare(IInteger other){
		if(other instanceof SharedBigIntegerValue){
			return ~(other.compare(this));
		}
		
		if(value > other.intValue()) return 1;
		if(value < other.intValue()) return -1;
		
		return 0;
	}
	
	public boolean equivalent(IShareable shareable){
		return super.equals(shareable);
	}
	
	public boolean equals(Object o){
		return (this == o);
	}
}
