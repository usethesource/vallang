/*******************************************************************************
 * Copyright (c) 2007-2012 IBM Corporation, CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Robert Fuhrer (rfuhrer@watson.ibm.com) 
 *    Jurgen Vinju (Jurgen.Vinju@cwi.nl)
 *    Davy Landman - added mathematical functions
 *******************************************************************************/

package org.eclipse.imp.pdb.facts.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.INumber;
import org.eclipse.imp.pdb.facts.IRational;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.impl.util.BigDecimalCalculations;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/*package*/class RealValue extends AbstractNumberValue implements IReal {
 
  private final BigDecimal fValue;

  /* package */RealValue(double value) {
    super(TypeFactory.getInstance().realType());
    fValue = BigDecimal.valueOf(value);
  }
  
  /* package */RealValue(double value, int precision) {
	    super(TypeFactory.getInstance().realType());
	    fValue = new BigDecimal(value, new MathContext(precision));
	  }

  /* package */RealValue(float value) {
    super(TypeFactory.getInstance().realType());
    fValue = BigDecimal.valueOf(value);
  }
  
  /* package */RealValue(float value, int precision) {
	    super(TypeFactory.getInstance().realType());
	    fValue = new BigDecimal(value, new MathContext(precision));
	  }

  /* package */RealValue(BigDecimal value) {
    super(TypeFactory.getInstance().realType());
    fValue = value;
  }
  
  /* package */RealValue(BigDecimal value, int precision) {
	    super(TypeFactory.getInstance().realType());
	    fValue = new BigDecimal(value.toEngineeringString(), new MathContext(precision));
	  }
  
  /* package */ RealValue(String s){
	  super(TypeFactory.getInstance().realType());
	  fValue =  new BigDecimal(s);
  }
  
  /* package */ RealValue(String s, int precision){
	  super(TypeFactory.getInstance().realType());
	  fValue =  new BigDecimal(s, new MathContext(precision));
  }

  public IReal toReal() {
    return this;
  }

  public IReal negate() {
    return new RealValue(fValue.negate(),BaseValueFactory.PRECISION);
  }

  public IReal add(IReal other) {
    return new RealValue(fValue.add(((RealValue) other).fValue), BaseValueFactory.PRECISION);
  }

  public IReal add(IInteger other) {
    return add(other.toReal());
  }

  public IReal add(IRational other) {
    return add(other.toReal());
  }

  public IReal subtract(IReal other) {
    return new RealValue(fValue.subtract(((RealValue) other).fValue), BaseValueFactory.PRECISION);
  }

  public IReal subtract(IInteger other) {
    return subtract(other.toReal());
  }

  public IReal subtract(IRational other) {
    return subtract(other.toReal());
  }

  public IReal multiply(IReal other) {
	// int precision = Math.min(Math.max(fValue.precision(), other.precision()), BaseValueFactory.PRECISION);
	// MathContext mc = new MathContext(precision, RoundingMode.HALF_UP);
    return new RealValue(fValue.multiply(((RealValue) other).fValue));
  }

  public IReal multiply(IInteger other) {
    return multiply(other.toReal());
  }

  public IReal multiply(IRational other) {
    return multiply(other.toReal());
  }

  public IReal divide(IReal other, int precision) {
    // make sure the precision is *at least* the same as that of the arguments
	precision = Math.max(Math.max(fValue.precision(), other.precision()), precision);
	MathContext mc = new MathContext(precision, RoundingMode.HALF_UP);
    return new RealValue(fValue.divide(((RealValue) other).fValue, mc));
  }

  public IReal divide(IInteger other, int precision) {
    return divide(other.toReal(), precision);
  }

  public IReal divide(IRational other, int precision) {
    return divide(other.toReal(), precision);
  }

  public IReal round() {
    return new RealValue(fValue.setScale(0, RoundingMode.HALF_UP));
  }

  public IReal floor() {
    return new RealValue(fValue.setScale(0, RoundingMode.FLOOR));
  }

  public IInteger toInteger() {
    return new IntegerValue(fValue.toBigInteger());
  }

  public IRational toRational() {
    throw new UnsupportedOperationException();
  }

  public IBool less(IReal other) {
    return BoolValue.getBoolValue(compare(other) < 0);
  }

  public IBool less(IInteger other) {
    return less(other.toReal());
  }

  public IBool less(IRational other) {
    return less(other.toReal());
  }

  public IBool lessEqual(IReal other) {
    return BoolValue.getBoolValue(compare(other) <= 0);
  }

  public IBool lessEqual(IInteger other) {
    return lessEqual(other.toReal());
  }

  public IBool lessEqual(IRational other) {
    return lessEqual(other.toReal());
  }

  public IBool equal(IReal other) {
    return BoolValue.getBoolValue(compare(other) == 0);
  }

  public IBool equal(IInteger other) {
    return equal(other.toReal());
  }

  public IBool equal(IRational other) {
    return equal(other.toReal());
  }
  
  public IBool greater(IReal other) {
    return BoolValue.getBoolValue(compare(other) > 0);
  }

  public IBool greater(IInteger other) {
    return greater(other.toReal());
  }

  public IBool greater(IRational other) {
    return greater(other.toReal());
  }

  public IBool greaterEqual(IReal other) {
    return BoolValue.getBoolValue(compare(other) >= 0);
  }

  public IBool greaterEqual(IInteger other) {
    return greaterEqual(other.toReal());
  }

  public IBool greaterEqual(IRational other) {
    return greaterEqual(other.toReal());
  }

  public int compare(IReal other) {
    return fValue.compareTo(((RealValue) other).fValue);
  }

  public int compare(INumber other) {
    return compare(other.toReal());
  }

  @Override
  public boolean equals(Object o) {
    if (getClass() == o.getClass()) {
      return fValue.compareTo(((RealValue) o).fValue) == 0;
    }
    return false;
  }

  @Override
  public int hashCode() {
    // Java BigDecimals have a bug, their even though 3.0 and 3.00 are equal,
    // their hashCode() is not, which is against the equals/hashCode() contract.
    // To work around this, we use this simple trick here which is correct but
    // might lead to many collisions.
    return Double.valueOf(fValue.doubleValue()).hashCode();
  }

  public <T> T accept(IValueVisitor<T> v) throws VisitorException {
    return v.visitReal(this);
  }

  public String getStringRepresentation() {
    StringBuilder sb = new StringBuilder();
    String decimalString = fValue.toString();
    sb.append(decimalString);
    if (decimalString.indexOf(".") == -1)
      sb.append(".");
    return sb.toString();
  }

  public double doubleValue() {
    return fValue.doubleValue();
  }

  public float floatValue() {
    return fValue.floatValue();
  }

  public int precision() {
    return fValue.precision();
  }


  public int scale() {
    return fValue.scale();
  }

  public IInteger unscaled() {
    return new IntegerValue(fValue.unscaledValue());
  }

  public IReal abs() {
    return new RealValue(fValue.abs(), BaseValueFactory.PRECISION);
  }

  public int signum() {
    return fValue.signum();
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
    return new RealValue(BigDecimalCalculations.ln(fValue, precision));
  }

  public IReal sqrt(int precision) {
    return new RealValue(BigDecimalCalculations.sqrt(fValue, precision));
  }

  public IReal nroot(IInteger n, int precision) {
    return new RealValue(BigDecimalCalculations.intRoot(fValue, n.longValue(), precision));
  }

  public IReal exp(int precision) {
    return new RealValue(BigDecimalCalculations.exp(fValue, precision));
  }

  public IReal pow(IInteger power) {
    return new RealValue(fValue.pow(power.intValue()));
  }

  public IReal tan(int precision) {
    return new RealValue(BigDecimalCalculations.tan(fValue, precision));
  }

  public IReal sin(int precision) {
    return new RealValue(BigDecimalCalculations.sin(fValue, precision));
  }

  public IReal cos(int precision) {
    return new RealValue(BigDecimalCalculations.cos(fValue, precision));
  }

  public static IReal pi(int precision) {
    if (precision < 0 || precision > 1000)
      throw new IllegalArgumentException("PI max precision is 1000");
    return new RealValue(BigDecimalCalculations.PI.setScale(precision, BigDecimal.ROUND_HALF_EVEN));
  }

  public static IReal e(int precision) {
    if (precision < 0 || precision > 1000)
      throw new IllegalArgumentException("E max precision is 1000");
    return new RealValue(BigDecimalCalculations.E.setScale(precision, BigDecimal.ROUND_HALF_EVEN));
  }

}
