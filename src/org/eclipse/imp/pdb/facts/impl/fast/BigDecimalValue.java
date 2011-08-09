/*******************************************************************************
* Copyright (c) 2009 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Arnold Lankamp - interfaces and implementation
*    Jurgen Vinju - extensions and fixes
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.fast;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.INumber;
import org.eclipse.imp.pdb.facts.IRational;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

public class BigDecimalValue extends AbstractNumberValue implements IReal {
	private final static Type DOUBLE_TYPE = TypeFactory.getInstance().realType();
	
	protected final BigDecimal value;
	
	protected BigDecimalValue(BigDecimal value){
		super();
		
		this.value = value;
	}

	public IReal abs() {
		return new BigDecimalValue(value.abs());
	}
	
	public IReal toReal() {
		return this;
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
	
	public IRational toRational(){
    	throw new UnsupportedOperationException();
	}
	
	public IReal floor(){
		return ValueFactory.getInstance().real(value.setScale(0, RoundingMode.FLOOR));
	}
	
	public IReal round(){
		return ValueFactory.getInstance().real(value.setScale(0, RoundingMode.HALF_UP));
	}
	
	public IReal add(IReal other){
		return ValueFactory.getInstance().real(value.add(((BigDecimalValue) other).value));
	}
	
	public INumber add(IInteger other) {
		return add(other.toReal());
	}
	
	public INumber add(IRational other) {
		return add(other.toReal());
	}
	
	public IReal subtract(IReal other){
		return ValueFactory.getInstance().real(value.subtract(((BigDecimalValue) other).value));
	}
	
	public INumber subtract(IInteger other) {
		return subtract(other.toReal());
	}
	
	public INumber subtract(IRational other) {
		return subtract(other.toReal());
	}
	
	public IReal multiply(IReal other){
		return ValueFactory.getInstance().real(value.multiply(((BigDecimalValue) other).value));
	}
	
	public INumber multiply(IInteger other) {
		return multiply(other.toReal());
	}
	
	public INumber multiply(IRational other) {
		return multiply(other.toReal());
	}
	
	public IReal divide(IReal other, int precision){
		// make sure the precision is *at least* the same as that of the arguments
		precision = Math.max(Math.max(value.precision(), other.precision()), precision);
		MathContext mc = new MathContext(precision, RoundingMode.HALF_UP);
		return ValueFactory.getInstance().real(value.divide(((BigDecimalValue) other).value, mc));
	}
	
	public IReal divide(IInteger other, int precision) {
		return divide(other.toReal(), precision);
	}
	
	public IReal divide(IRational other, int precision) {
		return divide(other.toReal(), precision);
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
	
	public IBool greater(IInteger other) {
		return greater(other.toReal());
	}
	
	public IBool greater(IRational other) {
		return greater(other.toReal());
	}
	
	public IBool greaterEqual(IReal other){
		return ValueFactory.getInstance().bool(compare(other) >= 0);
	}
	
	public IBool greaterEqual(IInteger other) {
		return greaterEqual(other.toReal());
	}
	
	public IBool greaterEqual(IRational other) {
		return greaterEqual(other.toReal());
	}
	
	
	public IBool less(IReal other){
		return ValueFactory.getInstance().bool(compare(other) < 0);
	}
	
	public IBool less(IInteger other) {
		return less(other.toReal());
	}
	
	public IBool less(IRational other) {
		return less(other.toReal());
	}
	
	public IBool lessEqual(IReal other){
		return ValueFactory.getInstance().bool(compare(other) <= 0);
	}
	
	public IBool lessEqual(IInteger other) {
		return lessEqual(other.toReal());
	}
	
	public IBool lessEqual(IRational other) {
		return lessEqual(other.toReal());
	}
	
	public int compare(IReal other){
		return value.compareTo(((BigDecimalValue) other).value);
	}
	
	public int compare(INumber other) {
		return compare(other.toReal());
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
			return (value.equals(otherDouble.value));
		}
		
		return false;
	}
	
	public boolean isEqual(IValue o){
		if(o == null) return false;

		if(o.getClass() == getClass()){
			BigDecimalValue otherDouble = (BigDecimalValue) o;
			return (value.compareTo(otherDouble.value) == 0);
		}
		
		return false; 
	}
	
	public String getStringRepresentation(){
		StringBuilder sb = new StringBuilder();
		String decimalString = value.toString();
		sb.append(decimalString);
		if(decimalString.indexOf(".") == -1) sb.append(".");
		return sb.toString();
	}
	
	public int signum() {
		return value.signum();
	}

}
