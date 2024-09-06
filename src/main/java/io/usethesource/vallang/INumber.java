/*******************************************************************************
* Copyright (c) 2010 CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   Jurgen Vinju (Jurgen.Vinju@cwi.nl) - initial API and implementation
*******************************************************************************/
package io.usethesource.vallang;

public abstract interface INumber extends IValue {
    /**
     * Returns an integer if both arguments are integer, and a real otherwise
     * @return this + other
     */
    public INumber add(INumber other);
    
    /**
     * @return this + other
     */
    public IReal   add(IReal other);
    
    /**
     * Returns an integer if both arguments are integer, and a real otherwise
     * @return this + other
     */
    public INumber add(IInteger other);

    /**
     * Returns a rational if both arguments are rationals or integers, and a real otherwise
     * @return this + other
     */
    public INumber add(IRational other);

    /**
     * Returns an integer if both arguments are integer, and a real otherwise
     * @return this - other;
     */
    public INumber subtract(INumber other);
    
    /**
     * @return this - other;
     */
    public INumber subtract(IReal other);
    
    /**
     * Returns an integer if both arguments are integer, and a real otherwise
     * @return this - other;
     */
    public INumber subtract(IInteger other);
    
    /**
     * @return this - other;
     */
    public INumber subtract(IRational other);

    /**
     * Returns an integer if both arguments are integer, and a real otherwise
     * @return this * other;
     */
    public INumber multiply(INumber other);
    
    /**
     * @return this * other;
     */
    public IReal multiply(IReal other);
    
    /**
     * Returns an integer if both arguments are integer, and a real otherwise
     * @return this * other;
     */
    public INumber multiply(IInteger other);

    /**
     * Returns a rational if both arguments are rationals/integer, and a real otherwise
     * @return this * other;
     */
    public INumber multiply(IRational other);

    /**
     * Integer division if both the receiver and the argument are integers, and real division otherwise
     * @return this / other 
     */
    public INumber divide(INumber other, int precision);
    
    /**
     * @return this / other 
     */
    public IReal divide(IReal other, int precision);
    
    /**
     * Integer division if both the receiver and the argument are integers, and real division otherwise
     * @return this / other 
     */
    public INumber divide(IInteger other, int precision);

    /**
     * Rational division if both the receiver and the argument are integers/rationals, 
     * real division otherwise
     * @return this / other 
     */
    public INumber divide(IRational other, int precision);

    /**
     * Returns an integer if both arguments are integer, and a real otherwise
     * @return -1 * this;
     */
    public INumber negate();
    
    /**
     * @param precision the precision of the result. This parameter may be ignored if another source of an accurate precision is available.
     * @return an IReal that is equal to this INumber
     */
    public IReal  toReal(int precision);
    
    /**
     * @return an IInteger (truncated if it was a real value)
     */
    public IInteger toInteger();

    /**
     * @return an IRational (truncated if it was a real value)
     */
    public IRational toRational();

    /**
     * @return true iff the numbers are equal
     */
    public IBool equal(INumber other);
    
    /**
     * @return true iff the numbers are equal
     */
    public IBool equal(IInteger other);
    
    /**
     * @return true iff the numbers are equal
     */
    public IBool equal(IReal other);
    
    /**
     * @return true iff the numbers are equal
     */
    public IBool equal(IRational other);
    
    /**
     * @return true iff this < other
     */
    public IBool less(INumber other);
    
    /**
     * @return true iff this < other
     */
    public IBool less(IReal other);
    
    /**
     * @return true iff this < other
     */
    public IBool less(IInteger other);

    /**
     * @return true iff this < other
     */
    public IBool less(IRational other);

    /**
     * @return true iff this > other
     */
    public IBool greater(INumber other);
    
    /**
     * @return true iff this > other
     */
    public IBool greater(IReal other);
    
    /**
     * @return true iff this > other
     */
    public IBool greater(IInteger other);

    /**
     * @return true iff this > other
     */
    public IBool greater(IRational other);

    /**
     * @return true iff this <= other
     */
    public IBool lessEqual(INumber other);
    
    /**
     * @return true iff this <= other
     */
    public IBool lessEqual(IReal other);
    
    /**
     * @return true iff this <= other
     */
    public IBool lessEqual(IInteger other);

    /**
     * @return true iff this <= other
     */
    public IBool lessEqual(IRational other);

    /**
     * @return true iff this >= other
     */
    public IBool greaterEqual(INumber other);
    
    /**
     * @return true iff this >= other
     */
    public IBool greaterEqual(IReal other);
    
    /**
     * @return true iff this >= other
     */
    public IBool greaterEqual(IInteger other);

    /**
     * @return true iff this >= other
     */
    public IBool greaterEqual(IRational other);
    
    /**
     * Returns an integer if the receiver was an integer, and a real otherwise
     * @return absolute value of this number
     */
    public INumber abs();
    
    /**
     * Compares two numbers
     * @param other
     * @return -1 if receiver is less than other, 0 is receiver is equal, 1 if receiver is larger
     */
    public int compare(INumber other);
    
    /**
     * @return return -1, 0 or 1 iff this integer is less than, equal to or greater than zero.
     */
    public int signum();
}
