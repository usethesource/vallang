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

package org.eclipse.imp.pdb.facts;


public interface IRational extends INumber {
	/**
	 * @return this + other;
	 */
    IRational add(IRational other);
    
    /**
     * @return this - other;
     */
    IRational subtract(IRational other);
    
    /**
     * @return this * other;
     */
    IRational multiply(IRational other);
    
    /**
     * @return this / other;
     */
    IRational divide(IRational other);
    
    /**
     * @return this / other;
     */
    IRational divide(IInteger other);
    
    /**
     * @return this rem other, which is the remainder after dividing this by other.
     * This may be a negative number.
     */
    IRational remainder(IRational other);
    
    /**
     * @return -1 * this;
     */
    IRational negate();
    
    /**
     * @return an IReal that approximates this IRational
     */
    IReal  toReal(int precision);

    /**
     * @return a double that approximates this IRational
     */
    double doubleValue();


    /**
     * @return the integer value nearest to this number
     * (equal to numerator()/denominator())
     */
    IInteger toInteger();

    /**
     * @return true iff this < other
     */
    IBool less(IRational other);
    
    /**
     * @return true iff this > other
     */
    IBool greater(IRational other);
 
    /**
     * @return true iff this <= other
     */
    IBool lessEqual(IRational other);
    
    /**
     * @return true iff this >= other
     */
    IBool greaterEqual(IRational other);
    
    /**
     * @return the value of the IRational represent as a string of decimal numbers in ASCII encoding.
     */
    String getStringRepresentation();
    
    /**
     * @return The rational's numerator
     */
    IInteger numerator();
    
    
    
    /**
     * @return The rational's denominator
     */
    IInteger denominator();

    /**
     * numerator() == (toInteger() * denominator()) + remainder()
     *  
     * @return numerator() % denominator()
     */
    IInteger remainder();

    /**
     * Compares two rationals
     * @param other
     * @return -1 if receiver is less than other, 0 is receiver is equal, 1 if receiver is larger
     */
    int compare(IRational other);
    
    /**
     * @return return -1, 0 or 1 iff this rational is less than, equal to or greater than zero.
     */
    int signum();

    /**
     * @return absolute value of this rational
     */
	IRational abs();
	
    /**
     * @return this number rounded down to the nearest integer number that is
     * less than this number.
     */
    IInteger floor();
    
    /**
     * @return this number rounded to the nearest integer number.
     */
    IInteger round();
    
}
