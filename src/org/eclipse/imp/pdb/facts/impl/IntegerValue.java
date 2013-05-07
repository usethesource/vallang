/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

 *******************************************************************************/

package org.eclipse.imp.pdb.facts.impl;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.INumber;
import org.eclipse.imp.pdb.facts.IRational;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/*package*/public class IntegerValue extends AbstractNumberValue implements IInteger, ICanBecomeABigInteger {
  private final BigInteger fValue;
  public static final IntegerValue INTEGER_ONE = new IntegerValue(1);

  /* package */IntegerValue(int i) {
    super(TypeFactory.getInstance().integerType());
    fValue = BigInteger.valueOf(i);
  }

  /* package */IntegerValue(long i) {
    super(TypeFactory.getInstance().integerType());
    fValue = BigInteger.valueOf(i);
  }

  /* package */IntegerValue(BigInteger i) {
    super(TypeFactory.getInstance().integerType());
    fValue = i;
  }

  /* package */IntegerValue(byte[] a) {
    this(new BigInteger(a));
  }

  public IInteger toInteger() {
    return this;
  }

  public IRational toRational() {
    return new RationalValue(this, INTEGER_ONE);
  }

  @Override
  public boolean equals(Object o) {
	  if(this == o) {
		  return true;
	  }
	  else if(o == null) {
		  return false;
	  }
	  else if (getClass() == o.getClass()) {
      return fValue.equals(((IntegerValue) o).fValue);
    }
    return false;
  }

  public IInteger add(IInteger other) {
    return new IntegerValue(fValue.add(((IntegerValue) other).fValue));
  }

  public IReal add(IReal other) {
    return (IReal) other.add(this);
  }

  public IRational add(IRational other) {
    return (IRational) other.add(this);
  }

  public IInteger negate() {
    return new IntegerValue(fValue.negate());
  }

  public IInteger subtract(IInteger other) {
    return new IntegerValue(fValue.subtract(((IntegerValue) other).fValue));
  }

  public INumber subtract(IReal other) {
    return toReal().subtract(other);
  }

  public INumber subtract(IRational other) {
    return toRational().subtract(other);
  }

  public IInteger multiply(IInteger other) {
    return new IntegerValue(fValue.multiply(((IntegerValue) other).fValue));
  }

  public IReal multiply(IReal other) {
    return (IReal) other.multiply(this);
  }

  public IRational multiply(IRational other) {
    return (IRational) other.multiply(this);
  }

  public IInteger divide(IInteger other) {
    return new IntegerValue(fValue.divide(((IntegerValue) other).fValue));
  }

  public IRational divide(IRational other) {
    return toRational().divide(other);
  }

  public INumber divide(IInteger other, int precision) {
    return toReal().divide(other, precision);
  }

  public IReal divide(IReal other, int precision) {
    return toReal().divide(other, precision);
  }

  public INumber divide(IRational other, int precision) {
    return toReal().divide(other, precision);
  }

  public IInteger remainder(IInteger other) {
    return new IntegerValue(fValue.remainder(((IntegerValue) other).fValue));
  }

  public IInteger mod(IInteger other) {
    return new IntegerValue(fValue.mod(((IntegerValue) other).fValue));
  }

  public IBool less(IInteger other) {
    return BoolValue.getBoolValue(compare(other) < 0);
  }

  public IBool less(IReal other) {
    return other.greater(this);
  }

  public IBool less(IRational other) {
    return other.greater(this);
  }

  public IBool lessEqual(IInteger other) {
    return BoolValue.getBoolValue(compare(other) <= 0);
  }

  public IBool lessEqual(IReal other) {
    return other.greaterEqual(this);
  }

  public IBool lessEqual(IRational other) {
    return other.greaterEqual(this);
  }

  public IBool equal(IInteger other) {
    return BoolValue.getBoolValue(compare(other) == 0);
  }

  public IBool equal(IReal other) {
    return other.equal(this);
  }

  public IBool equal(IRational other) {
    return other.equal(this);
  }

  public IBool greater(IInteger other) {
    return BoolValue.getBoolValue(compare(other) > 0);
  }

  public IBool greater(IReal other) {
    return other.less(this);
  }

  public IBool greater(IRational other) {
    return other.less(this);
  }

  public IBool greaterEqual(IInteger other) {
    return BoolValue.getBoolValue(compare(other) >= 0);
  }

  public IBool greaterEqual(IReal other) {
    return other.lessEqual(this);
  }

  public IBool greaterEqual(IRational other) {
    return other.lessEqual(this);
  }

  public IReal toReal() {
    return new RealValue(new BigDecimal(fValue));
  }

  public int compare(IInteger other) {
    return fValue.compareTo(((IntegerValue) other).fValue);
  }

  public int compare(INumber other) {
    if (isIntegerType(other)) {
      return compare(other.toInteger());
    } else if (other.getType().equivalent(TypeFactory.getInstance().rationalType())) {
      int compare = other.compare(this);
      return compare == 0 ? 0 : (compare < 0 ? 1 : -1); // negating compare would be unsafe
    }
    return toReal().compare(other);
  }

  @Override
  public int hashCode() {
    return fValue.hashCode();
  }

  public <T> T accept(IValueVisitor<T> v) throws VisitorException {
    return v.visitInteger(this);
  }

  public String getStringRepresentation() {
    return fValue.toString();
  }

  public byte[] getTwosComplementRepresentation() {
    return fValue.toByteArray();
  }

  public int intValue() {
    return fValue.intValue();
  }

  public long longValue() {
    return fValue.longValue();
  }

  public double doubleValue() {
	    return fValue.doubleValue();
  }

  public int signum() {
    return fValue.signum();
  }

  public IInteger abs() {
    return new IntegerValue(fValue.abs());
  }

  public BigInteger toBigInteger() {
    return fValue;
  }
}
