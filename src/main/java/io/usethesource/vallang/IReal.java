/*******************************************************************************
* Copyright (c) 2007,2009 IBM Corporation and CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
*    Jurgen Vinju (jurgen@vinju.org) - initial API and implementation
*    Davy Landman (davy.landamn@cwi.nl) - added log, ln, exp, pow, nroot, sqrt, sin, cos, and tan
*******************************************************************************/

package io.usethesource.vallang;

import io.usethesource.vallang.visitors.IValueVisitor;

public interface IReal extends INumber {
    @Override
    default int getMatchFingerprint() {
        int hash = hashCode();
        return hash == 0 ? 3496350 /* "real".hashCode() */ : hash;
    }

	/**
	 * @return this + other;
	 */
    @Override
    public IReal add(IReal other);
    
    /**
     * @return this - other;
     */
    @Override
    public IReal subtract(IReal other);
    
    /**
     * @return this * other;
     */
    @Override
    public IReal multiply(IReal other);
    
    /**
     * Divides a real with a specific precision
     * 
     * @return this / other;
     */
    @Override
    public IReal divide(IReal other, int precision);
    
    /**
     * @return this number rounded down to the nearest integer number that is
     * less than this number.
     */
    public IReal floor();
    
    /**
     * @return this number rounded to the nearest integer number.
     */
    public IReal round();
    
    /**
     * @return the integer value nearest to this number
     */
    @Override
    public IInteger toInteger();
    
    /**
     * @return true iff this < other
     */
    @Override
    public IBool less(IReal other);
    
    /**
     * @return true iff this > other
     */
    @Override
    public IBool greater(IReal other);
    
    /**
     * @return true iff this <= other
     */
    @Override
    public IBool lessEqual(IReal other);
    
    /**
     * @return true iff this >= other
     */
    @Override
    public IBool greaterEqual(IReal other);
    
    /**
     * @return this real represented as a string of decimal characters in scientific notation
     */
    public String getStringRepresentation();
    
    /**
     * @return -1 * this
     */
    @Override
    public IReal negate();
    
    /**
     * Compares two doubles
     * @param other
     * @return -1 if receiver is less than other, 0 is receiver is equal, 1 if receiver is larger
     */
    public int compare(IReal other);
    
    /**
     * Converts this IReal to a double. Precision may be less.
     * If the number does not fit, it will return
     * {@link Double#NEGATIVE_INFINITY} or {@link Double#POSITIVE_INFINITY}
     * when the number is smaller or larger, respectively.
     */
    public double doubleValue();

    /**
     * Converts this IReal to a float. Precision may be less.
     * If the number does not fit, it will return
     * {@link Float#NEGATIVE_INFINITY} or {@link Float#POSITIVE_INFINITY}
     * when the number is smaller or larger, respectively.
     */
    public float floatValue();
    
    /**
     * @return the number of digits in this.{@link #unscaled()}
     */
    public int precision();
    
    /**
     * The scale is the exponent of 10 with which the base is multiplied
     * as in scientific notation. I.e. in 1 * 10<sup>3</sup>, the scale is 3.
     * 
     * @return the scale of the real
     */
    public int scale();
    
    /**
     * Returns the unscaled value of this real as an integer. This is useful
     * for serialization purposes among other things.
     * 
     * @return this * 10<sup>this.scale()</sup>
     */
    public IInteger unscaled();

    /**
     * @return absolute value of this real
     */
    @Override
    public IReal abs();

	/**
	 * @return log<sub>base</sub>(this)
	 */
    public IReal log(IInteger base, int precision);
	
	/**
	 * @return log<sub>base</sub>(this)
	 */
	public IReal log(IReal base, int precision);
	
	/**
	 * @return natural log of this
	 */
	public IReal ln(int precision);
	
	/**
	 * @return square root of this
	 */
	public IReal sqrt(int precision);
	
	/**
	 * @return n-th root of this
	 */
	public IReal nroot(IInteger n, int precision);
	
	/**
	 * @return e<sup>this</sup>
	 */
	public IReal exp(int precision);
	
	/**
	 * @return this<sup>power</sup>
	 */
	public IReal pow(IInteger power);

	/**
	 * @return this<sup>power<sup> but for non natural numbers 
	 */
	public IReal pow(IReal power, int precision);
	
	/**
	 * @return tan(this)
	 */
	public IReal tan(int precision);
	
	/**
	 * @return sin(this)
	 */
	public IReal sin(int precision);
	
	/**
	 * @return cos(this)
	 */
	
	public IReal cos(int precision);
	
	@Override
	default <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
	    return v.visitReal(this);
	}

}
