/*******************************************************************************
* Copyright (c) 2009 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Arnold Lankamp - interfaces and implementation
*    Davy Landman - added mathematical functions 
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.shared;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.impl.fast.BigDecimalValue;
import org.eclipse.imp.pdb.facts.impl.util.BigDecimalCalculations;
import org.eclipse.imp.pdb.facts.impl.util.sharing.IShareable;

/**
 * Implementation of shareable reals.
 * 
 * @author Arnold Lankamp
 */
public class SharedBigDecimalValue extends BigDecimalValue implements IShareable{
	
	public SharedBigDecimalValue(BigDecimal bigDecimal){
		super(bigDecimal);
	}
	
	public IInteger toInteger(){
		return SharedValueFactory.getInstance().integer(value.toBigInteger());
	}
	
	public IReal floor(){
		return SharedValueFactory.getInstance().real(value.setScale(0, RoundingMode.FLOOR));
	}
	
	public IReal round(){
		return SharedValueFactory.getInstance().real(value.setScale(0, RoundingMode.HALF_UP));
	}
	
	public IReal add(IReal other){
		return SharedValueFactory.getInstance().real(value.add(((SharedBigDecimalValue) other).value));
	}
	
	public IReal subtract(IReal other){
		return SharedValueFactory.getInstance().real(value.subtract(((SharedBigDecimalValue) other).value));
	}
	
	public IReal multiply(IReal other){
		return SharedValueFactory.getInstance().real(value.multiply(((SharedBigDecimalValue) other).value));
	}
	
	public IReal divide(IReal other, int precision){
		MathContext mc = new MathContext(precision, RoundingMode.HALF_UP);
		// make sure the precision is *at least* the same as that of the arguments
		precision = Math.max(Math.max(value.precision(), other.precision()), precision);
		return SharedValueFactory.getInstance().real(value.divide(((SharedBigDecimalValue) other).value, mc));
	}
	
	public IReal negate(){
		return SharedValueFactory.getInstance().real(value.negate());
	}
	
	public IInteger unscaled(){
		return SharedValueFactory.getInstance().integer(value.unscaledValue());
	}
	
	public IBool greater(IReal other){
		return SharedValueFactory.getInstance().bool(compare(other) > 0);
	}
	
	public IBool greaterEqual(IReal other){
		return SharedValueFactory.getInstance().bool(compare(other) >= 0);
	}
	
	public IBool less(IReal other){
		return SharedValueFactory.getInstance().bool(compare(other) < 0);
	}
	
	public IBool lessEqual(IReal other){
		return SharedValueFactory.getInstance().bool(compare(other) <= 0);
	}
	
	public boolean equivalent(IShareable shareable){
		return super.equals(shareable);
	}
	
	public boolean equals(Object o){
		return (this == o);
	}
	
	public IReal log(IInteger base, int precision) {
		return log(base.toReal(), precision);
	}
	
	public IReal log(IReal base, int precision) {
		IReal lnBase = base.ln(precision + 1);
		IReal lnThis = this.ln(precision + 1);
		return lnThis.divide(lnBase, precision);
	}

	public IReal ln(int precision) {
		return SharedValueFactory.getInstance().real(BigDecimalCalculations.ln(value, precision));
	}

	public IReal sqrt(int precision) {
		return SharedValueFactory.getInstance().real(BigDecimalCalculations.sqrt(value, precision));
	}

	public IReal nroot(IInteger n, int precision) {
		return SharedValueFactory.getInstance().real(BigDecimalCalculations.intRoot(value, n.longValue(), precision));
	}
	
	public IReal exp(int precision) {
		return SharedValueFactory.getInstance().real(BigDecimalCalculations.exp(value, precision));
	}

	public IReal pow(IInteger power) {
		return SharedValueFactory.getInstance().real(value.pow(power.intValue()));
	}

	public IReal tan(int precision) {
		return SharedValueFactory.getInstance().real(BigDecimalCalculations.tan(value, precision));
	}

	public IReal sin(int precision) {
		return SharedValueFactory.getInstance().real(BigDecimalCalculations.sin(value, precision));
	}

	public IReal cos(int precision) {
		return SharedValueFactory.getInstance().real(BigDecimalCalculations.cos(value, precision));
	}
	
	public static IReal pi(int precision) {
		if (precision < 0 || precision > 1000)
			throw new IllegalArgumentException("PI max precision is 1000");
		return SharedValueFactory.getInstance().real(BigDecimalCalculations.PI.setScale(precision, BigDecimal.ROUND_HALF_EVEN));
	}
	
	public static IReal e(int precision) {
		if (precision < 0 || precision > 1000)
			throw new IllegalArgumentException("E max precision is 1000");
		return SharedValueFactory.getInstance().real(BigDecimalCalculations.E.setScale(precision, BigDecimal.ROUND_HALF_EVEN));
	}	
}
