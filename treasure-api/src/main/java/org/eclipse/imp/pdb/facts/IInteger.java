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


public interface IInteger extends INumber {
	/**
	 * @return this + other;
	 */
    IInteger add(IInteger other);
    
    /**
     * @return this - other;
     */
    IInteger subtract(IInteger other);
    
    /**
     * @return this * other;
     */
    IInteger multiply(IInteger other);
    
    /**
     * @return this / other;
     */
    IInteger divide(IInteger other);

    /**
     * @return this / other (exact divison);
     */
    IRational divide(IRational other);

    /**
     * @return this rem other, which is the remainder after dividing this by other.
     * This may be a negative number.
     */
    IInteger remainder(IInteger other);
    
    /**
     * @return -1 * this;
     */
    IInteger negate();
    
    /**
     * @return this % other, which is always a positive IInteger
     */
    IInteger mod(IInteger other);
    
    /**
     * @return an IReal that is equal to this IInteger
     */
    IReal  toReal(int precision);
    
    /**
     * @return true iff this < other
     */
    IBool less(IInteger other);
    
    /**
     * @return true iff this > other
     */
    IBool greater(IInteger other);
 
    /**
     * @return true iff this <= other
     */
    IBool lessEqual(IInteger other);
    
    /**
     * @return true iff this >= other
     */
    IBool greaterEqual(IInteger other);
    
    /**
     * @return the value of the IInteger represent as a string of decimal numbers in ASCII encoding.
     */
    String getStringRepresentation();
    
    /**
     * @return the two's complement representation of this integer in the minimum
     * amount of required bytes and in big-endian order. 
     */
    public byte[] getTwosComplementRepresentation();
    
    /**
     * Converts this IInteger to an int. Only the lower
     * 32 bits are used so the resulting int may be
     * smaller and the sign may change too.
     * 
     * Use doubleValue() instead, if you are not sure if the
     * result will fit in an int.
     * 
     */
    int intValue();
    
    /**
     * Converts this IInteger to a long.  
     * Only the lower 64 bits are used, so the resulting long may be 
     * smaller and the sign may change too.
     *
     * Use doubleValue() instead, if you are not sure if the
     * result will fit in a long.
     * 
     */
    long longValue();
    
    /**
     * Converts this IInteger to a double.  
     * The conversion may lose precision, and will yield +/- Inf
     * if the magnitude is too large for a double.
     */
    double doubleValue();
    
    /**
     * Compares two integers
     * @param other
     * @return -1 if receiver is less than other, 0 is receiver is equal, 1 if receiver is larger
     */
    int compare(IInteger other);
    
    /**
     * @return return -1, 0 or 1 iff this integer is less than, equal to or greater than zero.
     */
    int signum();

    /**
     * @return absolute value of this integer
     */
	IInteger abs();
}
