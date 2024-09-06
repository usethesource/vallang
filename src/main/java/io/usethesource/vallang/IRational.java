/*******************************************************************************
* Copyright (c) 2011 IBM Corporation and CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Anya Helene Bagge (bagge@cwi.nl) - initial implementation
*******************************************************************************/

package io.usethesource.vallang;

import io.usethesource.vallang.visitors.IValueVisitor;

public interface IRational extends INumber {
    /**
     * @return this + other;
     */
    @Override
    public IRational add(IRational other);
    
    /**
     * @return this - other;
     */
    @Override
    public IRational subtract(IRational other);
    
    /**
     * @return this * other;
     */
    @Override
    public IRational multiply(IRational other);
    
    /**
     * @return this / other;
     */
    public IRational divide(IRational other);
    
    /**
     * @return this / other;
     */
    public IRational divide(IInteger other);
    
    /**
     * @return this rem other, which is the remainder after dividing this by other.
     * This may be a negative number.
     */
    public IRational remainder(IRational other);
    
    /**
     * @return -1 * this;
     */
    @Override
    public IRational negate();
    
    /**
     * @return an IReal that approximates this IRational
     */
    @Override
    public IReal toReal(int precision);

    /**
     * @return a double that approximates this IRational
     * If the number does not fit, plus or minus infinity may be returned.
     * Also the number that does fit may loose precision.
     */
    public double doubleValue();


    /**
     * @return the integer value nearest to this number
     * (equal to numerator()/denominator())
     */
    @Override
    public IInteger toInteger();

    /**
     * @return true iff this < other
     */
    @Override
    public IBool less(IRational other);
    
    /**
     * @return true iff this > other
     */
    @Override
    public IBool greater(IRational other);
 
    /**
     * @return true iff this <= other
     */
    @Override
    public IBool lessEqual(IRational other);
    
    /**
     * @return true iff this >= other
     */
    @Override
    public IBool greaterEqual(IRational other);
    
    /**
     * @return the value of the IRational represent as a string of decimal numbers in ASCII encoding.
     */
    public String getStringRepresentation();
    
    /**
     * @return The rational's numerator
     */
    public IInteger numerator();
    
    /**
     * @return The rational's denominator
     */
    public IInteger denominator();

    /**
     * numerator() == (toInteger() * denominator()) + remainder()
     *  
     * @return numerator() % denominator()
     */
    public IInteger remainder();

    /**
     * Compares two rationals
     * @param other
     * @return -1 if receiver is less than other, 0 is receiver is equal, 1 if receiver is larger
     */
    public int compare(IRational other);
    
    /**
     * @return return -1, 0 or 1 iff this rational is less than, equal to or greater than zero.
     */
    @Override
    public int signum();

    /**
     * @return absolute value of this rational
     */
    @Override
    public IRational abs();
    
    /**
     * @return this number rounded down to the nearest integer number that is
     * less than this number.
     */
    public IInteger floor();
    
    /**
     * @return this number rounded to the nearest integer number.
     */
    public IInteger round();
    
    @Override
    default <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
        return v.visitRational(this);
    }
    
}
