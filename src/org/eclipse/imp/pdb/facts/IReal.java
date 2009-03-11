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
*******************************************************************************/

package org.eclipse.imp.pdb.facts;


public interface IReal extends IValue {
	/**
	 * @return this + other;
	 */
    IReal add(IReal other);
    
    /**
     * @return this - other;
     */
    IReal subtract(IReal other);
    
    /**
     * @return this * other;
     */
    IReal multiply(IReal other);
    
    /**
     * Divides a real. When the resulting number infinitely progresses, the
     * precision is rounded to 80*80 digits (approximately one page of digits).
     * The rounding method used is "half up", which means to the nearest number
     * and when at equal distance to the nearest number upward.
     * 
     * @return this / other;
     */
    IReal divide(IReal other);
    
    /**
     * @return this number rounded down to the nearest integer number that is
     * less than this number.
     */
    IReal floor();
    
    /**
     * @return this number rounded to the nearest integer number.
     */
    IReal round();
    
    /**
     * @return the integer value nearest to this number
     */
    IInteger toInteger();
    
    /**
     * @return true iff this < other
     */
    IBool less(IReal other);
    
    /**
     * @return true iff this > other
     */
    IBool greater(IReal other);
    
    /**
     * @return true iff this <= other
     */
    IBool lessEqual(IReal other);
    
    /**
     * @return true iff this >= other
     */
    IBool greaterEqual(IReal other);
    
    /**
     * @return this real represented as a string of decimal characters in scientific notation
     */
    String getStringRepresentation();
    
    /**
     * @return -1 * this
     */
    IReal negate();
    
    /**
     * Compares two doubles
     * @param other
     * @return -1 if receiver is less than other, 0 is receiver is equal, 1 if receiver is larger
     */
    int compare(IReal other);
    
    /**
     * Converts this IReal to a double. Precision may be less.
     * If the number does not fit, it will return
     * {@link Double#NEGATIVE_INFINITY} or {@link Double#POSITIVE_INFINITY}
     * when the number is smaller or larger, respectively.
     */
    double doubleValue();

    /**
     * Converts this IReal to a float. Precision may be less.
     * If the number does not fit, it will return
     * {@link Float#NEGATIVE_INFINITY} or {@link Float#POSITIVE_INFINITY}
     * when the number is smaller or larger, respectively.
     */
    float floatValue();
    
    /**
     * @return the number of digits in this.{@link #unscaled()}
     */
    int precision();
    
    /**
     * The scale is the exponent of 10 with which the base is multiplied
     * as in scientific notation. I.e. in 1 * 10<sup>3</sup>, the scale is 3.
     * 
     * @return the scale of the real
     */
    int scale();
    
    /**
     * Returns the unscaled value of this real as an integer. This is useful
     * for serialization purposes among other things.
     * 
     * @return this * 10<sup>this.scale()</sup>
     */
    IInteger unscaled();
}
