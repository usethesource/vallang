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
import java.math.MathContext;
import java.math.RoundingMode;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * Implementation of IReal.
 * 
 * @author Arnold Lankamp
 */
public class BigDecimalValue extends Value implements IReal{
	private final static Type DOUBLE_TYPE = TypeFactory.getInstance().realType();
	
	protected final static MathContext mc = MathContext.DECIMAL128; // More then good enough.
	
	protected final BigDecimal value;
	
	protected BigDecimalValue(BigDecimal value){
		super();
		
		this.value = value;
	}

	public Type getType(){
		return DOUBLE_TYPE;
	}
	
	public float floatValue(){
		return value.floatValue();
	}
	
	public double doubleValue(){
		return value.doubleValue();
	}
	
	public IInteger toInteger(){
		return ValueFactory.getInstance().integer(value.toBigInteger());
	}
	
	public IReal floor(){
		return ValueFactory.getInstance().real(value.setScale(0, RoundingMode.FLOOR));
	}
	
	public IReal round(){
		return ValueFactory.getInstance().real(value.setScale(0, RoundingMode.HALF_UP));
	}
	
	public IReal add(IReal other){
		return ValueFactory.getInstance().real(value.add(((BigDecimalValue) other).value, mc));
	}
	
	public IReal subtract(IReal other){
		return ValueFactory.getInstance().real(value.subtract(((BigDecimalValue) other).value, mc));
	}
	
	public IReal multiply(IReal other){
		return ValueFactory.getInstance().real(value.multiply(((BigDecimalValue) other).value, mc));
	}
	
	public IReal divide(IReal other, int precision){
		return ValueFactory.getInstance().real(value.divide(((BigDecimalValue) other).value, mc));
	}
	
	public IReal negate(){
		return ValueFactory.getInstance().real(value.negate());
	}
	
	public int precision(){
		return value.precision();
	}
	
	public int scale(){
		return value.scale();
	}
	
	public IInteger unscaled(){
		return ValueFactory.getInstance().integer(value.unscaledValue());
	}
	
	public IBool greater(IReal other){
		return ValueFactory.getInstance().bool(compare(other) > 0);
	}
	
	public IBool greaterEqual(IReal other){
		return ValueFactory.getInstance().bool(compare(other) >= 0);
	}
	
	public IBool less(IReal other){
		return ValueFactory.getInstance().bool(compare(other) < 0);
	}
	
	public IBool lessEqual(IReal other){
		return ValueFactory.getInstance().bool(compare(other) <= 0);
	}
	
	public int compare(IReal other){
		return value.compareTo(((BigDecimalValue) other).value);
	}
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
		return v.visitReal(this);
	}
	
	public int hashCode(){
		// BigDecimals don't generate consistent hashcodes for things that are actually 'equal'.
		// This code rectifies this problem.
		long bits = Double.doubleToLongBits(value.doubleValue());
		return (int) (bits ^ (bits >>> 32));
	}
	
	public boolean equals(Object o){
		if(o == null) return false;
		
		if(o.getClass() == getClass()){
			BigDecimalValue otherDouble = (BigDecimalValue) o;
			return (value.compareTo(otherDouble.value) == 0);
		}
		
		return false;
	}
	
	public boolean isEqual(IValue value){
		return equals(value);
	}
	
	public String getStringRepresentation(){
		return value.toString();
	}
}
