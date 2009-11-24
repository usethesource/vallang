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

import java.math.BigDecimal;
import java.math.BigInteger;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.impl.ICanBecomeABigInteger;
import org.eclipse.imp.pdb.facts.impl.fast.BigIntegerValue;
import org.eclipse.imp.pdb.facts.impl.fast.IntegerValue;
import org.eclipse.imp.pdb.facts.impl.util.sharing.IShareable;

/**
 * Specialized implementation for shareable integer values that fall outside the 32-bit range.
 * 
 * @author Arnold Lankamp
 */
public class SharedBigIntegerValue extends BigIntegerValue implements IShareable{
	
	public SharedBigIntegerValue(BigInteger bigInteger){
		super(bigInteger);
	}

	public IReal toReal(){
		return SharedValueFactory.getInstance().real(new BigDecimal(value));
	}
	
	public IInteger add(IInteger other){
		BigInteger result = value.add(((ICanBecomeABigInteger) other).toBigInteger());
		
		int length = result.bitLength();
		if(length <= 31){
			return SharedValueFactory.getInstance().integer(result.intValue());
		}
		
		return SharedValueFactory.getInstance().integer(result);
	}
	
	public IInteger subtract(IInteger other){
		BigInteger result = value.subtract(((ICanBecomeABigInteger) other).toBigInteger());
		
		int length = result.bitLength();
		if(length <= 31){
			return SharedValueFactory.getInstance().integer(result.intValue());
		}
		
		return SharedValueFactory.getInstance().integer(result);
	}
	
	public IInteger multiply(IInteger other){
		BigInteger result = value.multiply(((ICanBecomeABigInteger) other).toBigInteger());
		// The result of this operation can never fit in a 32-bit integer, so no need to check.
		return SharedValueFactory.getInstance().integer(result);
	}
	
	public IInteger divide(IInteger other){
		BigInteger result = value.divide(((ICanBecomeABigInteger) other).toBigInteger());
		
		int length = result.bitLength();
		if(length <= 31){
			return SharedValueFactory.getInstance().integer(result.intValue());
		}
		
		return SharedValueFactory.getInstance().integer(result);
	}
	
	public IInteger mod(IInteger other){
		BigInteger result = value.mod(((ICanBecomeABigInteger) other).toBigInteger());
		
		if(other instanceof IntegerValue){
			int integerResult = result.intValue();
			return SharedValueFactory.getInstance().integer(integerResult);
		}
		
		return SharedValueFactory.getInstance().integer(result);
	}
	
	public IInteger remainder(IInteger other){
		BigInteger result = value.remainder(((ICanBecomeABigInteger) other).toBigInteger());
		
		if(other instanceof IntegerValue){
			int integerResult = result.intValue();
			return SharedValueFactory.getInstance().integer(integerResult);
		}
		
		return SharedValueFactory.getInstance().integer(result);
	}

	public IInteger negate(){
		return SharedValueFactory.getInstance().integer(value.negate());
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
	
	public boolean equivalent(IShareable shareable){
		return super.equals(shareable);
	}
	
	public boolean equals(Object o){
		return (this == o);
	}
}
