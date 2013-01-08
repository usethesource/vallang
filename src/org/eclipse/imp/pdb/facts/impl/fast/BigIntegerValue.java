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
 * Specialized implementation for integer values that fall outside the 32-bit range.
 * 
 * @author Arnold Lankamp
 */
/*package*/ class BigIntegerValue extends AbstractNumberValue implements IInteger, ICanBecomeABigInteger{
	private final static Type INTEGER_TYPE = TypeFactory.getInstance().integerType();
	
	protected final BigInteger value;
	
	/*package*/ BigIntegerValue(BigInteger value){
		super();
		if(value.equals(BigInteger.ZERO))
			value = BigInteger.ZERO;
		if(value.equals(BigInteger.ONE))
			value = BigInteger.ONE;

		this.value = value;
	}
	
	public IInteger toInteger() {
		return this;
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

	public double doubleValue(){
		return value.doubleValue();
	}

	public IReal toReal(){
		return ValueFactory.getInstance().real(new BigDecimal(value));
	}

	public IRational toRational(){
		return new RationalValue(this, IntegerValue.INTEGER_ONE);
	}
	public byte[] getTwosComplementRepresentation(){
		return value.toByteArray();
	}
	
	public BigInteger toBigInteger(){
		return value;
	}
	
	public IInteger add(IInteger other){
		BigInteger o = ((ICanBecomeABigInteger) other).toBigInteger();
		BigInteger result = value.add(o);
		if(result == value)
			return this;
		else if(result == o)
			return other;
		int length = result.bitLength();
		if(length <= 31){
			return ValueFactory.getInstance().integer(result.intValue());
		}
		
		return ValueFactory.getInstance().integer(result);
	}
	
	public IReal add(IReal other) {
		return (IReal) other.add(this);
	}
	
	public IRational add(IRational other) {
		return (IRational) other.add(this);
	}
	
	public IInteger subtract(IInteger other){
		BigInteger result = value.subtract(((ICanBecomeABigInteger) other).toBigInteger());
		if(result == value)
			return this;
		
		int length = result.bitLength();
		if(length <= 31){
			return ValueFactory.getInstance().integer(result.intValue());
		}
		
		return ValueFactory.getInstance().integer(result);
	}
	
	public INumber subtract(IReal other) {
		return toReal().subtract(other);
	}

	public INumber subtract(IRational other) {
		return toRational().subtract(other);
	}
	
	public IInteger multiply(IInteger other){
		BigInteger o = ((ICanBecomeABigInteger) other).toBigInteger();
		BigInteger result = value.multiply(o);
		if(result == value)
			return this;
		else if(result == o)
			return other;
		// The result of this operation can never fit in a 32-bit integer, so no need to check.
		return ValueFactory.getInstance().integer(result);
	}
	
	public IReal multiply(IReal other) {
		return (IReal) other.multiply(this);
	}
	
	public IRational multiply(IRational other) {
		return (IRational) other.multiply(this);
	}
	public IInteger divide(IInteger other){
		BigInteger result = value.divide(((ICanBecomeABigInteger) other).toBigInteger());
		if(result == value)
			return this;
		
		int length = result.bitLength();
		if(length <= 31){
			return ValueFactory.getInstance().integer(result.intValue());
		}
		
		return ValueFactory.getInstance().integer(result);
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
	
	public IBool equal(IInteger other){
    return ValueFactory.getInstance().bool(compare(other) == 0);
  }
  
  public IBool equal(IReal other) {
    return other.equal(this);
  }
  
  public IBool equal(IRational other) {
    return other.equal(this);
  }
  
	public IBool greater(IInteger other){
		return ValueFactory.getInstance().bool(compare(other) > 0);
	}
	
	public IBool greater(IReal other) {
		return other.less(this);
	}
	
	public IBool greater(IRational other) {
		return other.less(this);
	}
	
	public IBool greaterEqual(IInteger other){
		return ValueFactory.getInstance().bool(compare(other) >= 0);
	}

	public IBool greaterEqual(IReal other) {
		return other.lessEqual(this);
	}
	
	public IBool greaterEqual(IRational other) {
		return other.lessEqual(this);
	}
	
	public IBool less(IInteger other){
		return ValueFactory.getInstance().bool(compare(other) < 0);
	}
	
	public IBool less(IReal other) {
		return other.greater(this);
	}
	
	public IBool less(IRational other) {
		return other.greater(this);
	}
	
	public IBool lessEqual(IInteger other){
		return ValueFactory.getInstance().bool(compare(other) <= 0);
	}
	
	public IBool lessEqual(IReal other) {
		return other.greaterEqual(this);
	}
	
	public IBool lessEqual(IRational other) {
		return other.greaterEqual(this);
	}
	
	public int compare(IInteger other){
		return value.compareTo(((ICanBecomeABigInteger) other).toBigInteger());
	}
	
	public int compare(INumber other) {
		if (other.getType().isIntegerType()) {
			return compare(other.toInteger());
		}
		else if (other.getType().isRationalType()) {
			return toRational().compare(other);
		}
		else {
			return toReal().compare(other);
		}
	}
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
		return v.visitInteger(this);
	}
	
	public int hashCode(){
		return value.hashCode();
	}
	
	public boolean equals(Object o){
		if(o == null) return false;
		else if(o == this) return true;
		
		if(o.getClass() == getClass()){
			BigIntegerValue otherInteger = (BigIntegerValue) o;
			return value.equals(otherInteger.value);
		}
		
		return false;
	}
	
	public boolean isEqual(IValue other){
		return equals(other);
	}
	
	public String getStringRepresentation(){
		return value.toString();
	}

	public int signum() {
		return value.signum();
	}
	
	public IInteger abs() {
		return new BigIntegerValue(value.abs());
	}

}
