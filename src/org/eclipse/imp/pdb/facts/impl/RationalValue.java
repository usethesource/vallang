/*******************************************************************************
* Copyright (c) 2011 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Anya Helene Bagge - initial implementation
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl;

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

public class RationalValue extends AbstractNumberValue implements IRational {
	public static final Type RATIONAL_TYPE = TypeFactory.getInstance().rationalType();

	protected final IInteger num;
	protected final IInteger denom;

	public RationalValue(IInteger num, IInteger denom) {
		super(RATIONAL_TYPE);
		if(denom.signum() < 0) {
			num = num.negate();
			denom = denom.negate();
		}
		// normalize infinites
		if(denom.signum() == 0) {
			if(num.signum() > 0)
				num = intOne();
			else if(num.signum() < 0)
				num = intOne().negate();
			else
				throw new ArithmeticException("Illegal fraction 0/0");

		}
		else if(num.signum() == 0) {
			denom = intOne();
		}
		else {
			IInteger gcd = gcd(num, denom);
			while(gcd.compare(intOne()) != 0) {
				num = num.divide(gcd);
				denom = denom.divide(gcd);
				gcd = gcd(num, denom);
			}
		}
		this.num = num;
		this.denom = denom;
	}
	
	public IRational add(IRational other) {
		// (num*other.denom + denom*other.num) / denom*other.denom
		return toRational(
				num.multiply(other.denominator()).add(denom.multiply(other.numerator())),
					denom.multiply(other.denominator()));
	}

	public IReal add(IReal other) {
		return toReal().add(other);
	}

	public INumber add(IInteger other) {
		return toRational(num.add(other.multiply(denom)), denom);
	}

	public IRational subtract(IRational other) {
		// (num*other.denom - denom*other.num) / denom*other.denom
		return toRational(
				num.multiply(other.denominator()).subtract(denom.multiply(other.numerator())),
					denom.multiply(other.denominator()));
	}

	public INumber subtract(IReal other) {
		return toReal().subtract(other);
	}

	public INumber subtract(IInteger other) {
		return toRational(num.subtract(other.multiply(denom)), denom);
	}

	public IRational multiply(IRational other) {
		return toRational(num.multiply(other.numerator()),
				denom.multiply(other.denominator()));
	}

	public IReal multiply(IReal other) {
		return toReal().multiply(other);
	}

	public INumber multiply(IInteger other) {
		return toRational(num.multiply(other), denom);
	}

	// TODO: should we perhaps drop this and only have the other divide?
	// or vice-versa?
	public IRational divide(IRational other) {
		return toRational(num.multiply(other.denominator()),
				denom.multiply(other.numerator()));
	}

	public IReal divide(IReal other, int precision) {
		return toReal().divide(other, precision);
	}

	public IRational divide(IInteger other, int precision) {
		return divide(other); // forget precision
	}

	public IRational divide(IInteger other) {
		return toRational(num, denom.multiply(other));
	}

	
	public INumber divide(IRational other, int precision) {
		return toRational(num.multiply(other.denominator()),
				denom.multiply(other.numerator()));
	}

	public IBool less(IRational other) {
		return BoolValue.getBoolValue(compare(other) < 0);
	}

	public IBool less(IReal other) {
		return other.greater(this);
	}

	public IBool less(IInteger other) {
		return less(other.toRational());
	}

	public IBool greater(IRational other) {
		return BoolValue.getBoolValue(compare(other) > 0);
	}

	public IBool greater(IReal other) {
		return other.less(this);
	}

	public IBool greater(IInteger other) {
		return greater(other.toRational());
	}
	
	public IBool equal(IRational other) {
    return BoolValue.getBoolValue(compare(other) == 0);
  }

  public IBool equal(IReal other) {
    return other.equal(this);
  }

  public IBool equal(IInteger other) {
    return equal(other.toRational());
  }

	public IBool lessEqual(IRational other) {
		return BoolValue.getBoolValue(compare(other) <= 0);
	}

	public IBool lessEqual(IReal other) {
		return other.greaterEqual(this);
	}

	public IBool lessEqual(IInteger other) {
		return lessEqual(other.toRational());
	}

	public IBool greaterEqual(IRational other) {
		return BoolValue.getBoolValue(compare(other) >= 0);
	}

	public IBool greaterEqual(IReal other) {
		return other.lessEqual(this);
	}

	public IBool greaterEqual(IInteger other) {
		return greaterEqual(other.toRational());
	}

	public boolean isEqual(IValue other) {
		return equals(other);
	}

	public boolean equals(Object o) {
		if(o == null) return false;
		if(o == this) return true;
		if(o.getClass() == getClass()){
			RationalValue other = (RationalValue) o;
			return num.equals(other.num) && denom.equals(other.denom); 
		}
		else if(o instanceof IInteger)
			return equals(((IInteger) o).toRational());
		else if(o instanceof IReal)
			return toReal().isEqual((IReal)o);

		
		return false;
	}

	public int compare(INumber other) {
		if(other.getType().isIntegerType()) {
			IInteger div = num.divide(denom);
			IInteger rem = num.remainder(denom);
			if(div.compare(other) != 0)
				return div.compare(other);
			else
				return rem.signum();
		}
		else if(other.getType().isRationalType()){
			IRational diff = subtract((IRational)other);
			return diff.signum();
		}
		else
			return toReal().compare(other);
	}

	public Type getType() {
		return RATIONAL_TYPE;
	}

	public <T> T accept(IValueVisitor<T> v) throws VisitorException {
		return v.visitRational(this);
	}

	public IRational negate() {
		return toRational(num.negate(), denom);
	}

	public IReal toReal() {
		IReal r1 = num.toReal();
		IReal r2 = denom.toReal();
		r1 = r1.divide(r2, r1.precision());
		return r1;
	}

	public IInteger toInteger() {
		return num.divide(denom);
	}

	public String getStringRepresentation() {
		return num.getStringRepresentation() + "r" + (denom.equals(intOne()) ? "" : denom.getStringRepresentation());
	}
	
	public int compare(IRational other) {
		IRational diff = subtract(other);
		return diff.signum();
	}

	public int signum() {
		return num.signum();
	}

	public IRational abs() {
		return toRational(num.abs(), denom);
	}

	public IInteger floor() {
		return num.divide(denom);
	}

	public IInteger round() {
		return toReal().round().toInteger();
	}

	public IRational toRational() {
		return this;
	}

	public IRational toRational(IInteger n, IInteger d) {
		return new RationalValue(n, d);
	}

	public IRational remainder(IRational other) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode() {
		if(denom.equals(intOne()))
			return num.hashCode();
		else {
			final int prime = 31;
			int result = 1;
			result = prime * result + num.hashCode();
			result = prime * result + denom.hashCode();
			return result;
		}
	}

	public IInteger numerator() {
		return num;
	}

	public IInteger denominator() {
		return denom;
	}

	public IInteger remainder() {
		return num.remainder(denom);
	}

	protected IInteger gcd(IInteger n, IInteger d) {
		n = n.abs();
		d = d.abs();
		while(d.signum() > 0) {
			IInteger tmp = d;
			d = n.mod(d);
			n = tmp;
		}
		return n;
	}
	protected IInteger intOne() {
		return IntegerValue.INTEGER_ONE;
	}

	public double doubleValue() {
		return num.doubleValue() / denom.doubleValue();
	}
}