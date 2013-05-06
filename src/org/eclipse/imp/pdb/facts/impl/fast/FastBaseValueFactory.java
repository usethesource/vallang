/*******************************************************************************
* Copyright (c) 2009, 2012-2013 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Jurgen Vinju - interface and implementation
*    Arnold Lankamp - implementation
*    Anya Helene Bagge - rational support, labeled maps and tuples
*    Davy Landman - added PI & E constants
*    Michael Steindorfer - extracted factory for numeric data
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.fast;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IRational;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.BaseValueFactory;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.util.ShareableHashMap;

/**
 * Extension of base value factory with optimized numeric representations.
 */
public abstract class FastBaseValueFactory extends BaseValueFactory {

	private final static String INTEGER_MAX_STRING = "2147483647";
	private final static String NEGATIVE_INTEGER_MAX_STRING = "-2147483648";
		
	@Override
	public IInteger integer(int value){
		return new IntegerValue(value);
	}

	@Override
	public IInteger integer(long value){
		if(((value & 0x000000007fffffffL) == value) || ((value & 0xffffffff80000000L) == 0xffffffff80000000L)){
			return integer((int) value);
		}else{
			byte[] valueData = new byte[8];
			valueData[0] = (byte) ((value >>> 56) & 0xff);
			valueData[1] = (byte) ((value >>> 48) & 0xff);
			valueData[2] = (byte) ((value >>> 40) & 0xff);
			valueData[3] = (byte) ((value >>> 32) & 0xff);
			valueData[4] = (byte) ((value >>> 24) & 0xff);
			valueData[5] = (byte) ((value >>> 16) & 0xff);
			valueData[6] = (byte) ((value >>> 8) & 0xff);
			valueData[7] = (byte) (value & 0xff);
			return integer(valueData);
		}
	}
	
	@Override
	public IInteger integer(String integerValue){
		if(integerValue.startsWith("-")){
			if(integerValue.length() < 11 || (integerValue.length() == 11 && integerValue.compareTo(NEGATIVE_INTEGER_MAX_STRING) <= 0)){
				return new IntegerValue(Integer.parseInt(integerValue));
			}
			return new BigIntegerValue(new BigInteger(integerValue));
		}
		
		if(integerValue.length() < 10 || (integerValue.length() == 10 && integerValue.compareTo(INTEGER_MAX_STRING) <= 0)){
			return new IntegerValue(Integer.parseInt(integerValue));
		}
		return new BigIntegerValue(new BigInteger(integerValue));
	}
	
	@Override
	public IInteger integer(byte[] integerData){
		if(integerData.length <= 4){
			int value = 0;
			for(int i = integerData.length - 1, j = 0; i >= 0; i--, j++){
				value |= ((integerData[i] & 0xff) << (j * 8));
			}
			
			return new IntegerValue(value);
		}
		return new BigIntegerValue(new BigInteger(integerData));
	}
	
	public IInteger integer(BigInteger value){
		if(value.bitLength() > 31){
			return new BigIntegerValue(value);
		}
		return new IntegerValue(value.intValue());
	}
	
	@Override
	public IRational rational(IInteger a, IInteger b) {
		return new RationalValue(a, b);
	}

	@Override
	public IReal real(double value){
		return new BigDecimalValue(BigDecimal.valueOf(value));
	}
	
	@Override
	public IReal real(double value, int p) {
		return new BigDecimalValue(new BigDecimal(value, new MathContext(p)));
	}
	
	@Override
	public IReal real(float value) {
		return new BigDecimalValue(BigDecimal.valueOf(value));
	}
	
	@Override
	public IReal real(String doubleValue){
		return new BigDecimalValue(new BigDecimal(doubleValue));
	}
	
	@Override
	public IReal real(String s, int p) throws NumberFormatException {
		return new BigDecimalValue(new BigDecimal(s, new MathContext(p)));
	}

	public IReal real(BigDecimal value){
		return new BigDecimalValue(value);
	}
	
	@Override
	public IReal pi(int precision) {
		return BigDecimalValue.pi(precision);
	}
	
	@Override
	public IReal e(int precision) {
		return BigDecimalValue.e(precision);
	}
	
	@Override
	public IString string(String value){
		return new StringValue(value);
	}
	
	protected Type inferInstantiatedTypeOfConstructor(final Type constructorType, final IValue... children) {
		Type instantiatedType;
		if (!constructorType.getAbstractDataType().isParameterized()) {
			instantiatedType = constructorType;
		} else {
			ShareableHashMap<Type, Type> bindings = new ShareableHashMap<>();
			TypeFactory tf = TypeFactory.getInstance();
			Type params = constructorType.getAbstractDataType().getTypeParameters();
			for (Type p : params) {
				if (p.isOpen()) {
					bindings.put(p, tf.voidType());
				}
			}
			constructorType.getFieldTypes().match(tf.tupleType(children), bindings);
			instantiatedType = constructorType.instantiate(bindings);
		}

		return instantiatedType;
	}	

	protected static void checkNull(Object... args) {
		for (Object a : args) {
			if (a == null) {
				throw new NullPointerException();
			}
		}
	}
	
	protected static void checkNull(java.util.Map<Object, Object> args) {
		for (java.util.Map.Entry<Object, Object> entry : args.entrySet()) {
			if (entry == null || entry.getKey() == null || entry.getValue() == null) {
				throw new NullPointerException();
			}
		}
	}
	
}
