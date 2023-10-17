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

package io.usethesource.vallang;

import io.usethesource.vallang.visitors.IValueVisitor;

public interface IInteger extends INumber {
    @Override
    default int getMatchFingerprint() {
        if (signum() == 0) {
            return 104431; /* "int".hashCode() */
        }
        else {
            return hashCode();
        }
    }

	/**
	 * @return this + other;
	 */
    @Override
    public IInteger add(IInteger other);
    
    /**
     * @return this - other;
     */
    @Override
    public IInteger subtract(IInteger other);
    
    /**
     * @return this * other;
     */
    @Override
    public IInteger multiply(IInteger other);
    
    /**
     * @return this / other;
     */
    public IInteger divide(IInteger other);

    /**
     * @return this / other (exact divison);
     */
    public IRational divide(IRational other);

    /**
     * @return this rem other, which is the remainder after dividing this by other.
     * This may be a negative number.
     */
    public IInteger remainder(IInteger other);
    
    /**
     * @return -1 * this;
     */
    @Override
    public IInteger negate();
    
    /**
     * @return this % other, which is always a positive IInteger
     */
    public IInteger mod(IInteger other);
    
    /**
     * @return an IReal that is equal to this IInteger
     */
    @Override
    public IReal  toReal(int precision);
    
    /**
     * @return true iff this < other
     */
    @Override
    public IBool less(IInteger other);
    
    /**
     * @return true iff this > other
     */
    @Override
    public IBool greater(IInteger other);
 
    /**
     * @return true iff this <= other
     */
    @Override
    public IBool lessEqual(IInteger other);
    
    /**
     * @return true iff this >= other
     */
    @Override
    public IBool greaterEqual(IInteger other);
    
    /**
     * @return the value of the IInteger represent as a string of decimal numbers in ASCII encoding.
     */
    public String getStringRepresentation();
    
    /**
     * @return the two's complement representation of this integer in the minimum
     * amount of required bytes and in big-endian order. 
     */
    public byte[] getTwosComplementRepresentation();
    
    /**
     * Converts this IInteger to an int. If it
     * does not fit an ArithmeticException is thrown.
     * 
     * Use doubleValue() instead, if you are not sure if the
     * result will fit in an int.
     * 
     */
    public int intValue() throws ArithmeticException;
    
    /**
     * Converts this IInteger to a long.  
     * If it does not fit an ArithmeticException is thrown.
     *
     * Use doubleValue() instead, if you are not sure if the
     * result will fit in a long.
     * 
     */
    public long longValue() throws ArithmeticException;
    
    /**
     * Converts this IInteger to a double.  
     * The conversion may lose precision, and will yield +/- Inf
     * if the magnitude is too large for a double.
     */
    public double doubleValue();
    
    /**
     * Compares two integers
     * @param other
     * @return -1 if receiver is less than other, 0 is receiver is equal, 1 if receiver is larger
     */
    public int compare(IInteger other);
    
    /**
     * @return return -1, 0 or 1 iff this integer is less than, equal to or greater than zero.
     */
    @Override
    public int signum();

    /**
     * @return absolute value of this integer
     */
    @Override
    public IInteger abs();
	
	@Override
	public default <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
	    return v.visitInteger(this);
	}
}
