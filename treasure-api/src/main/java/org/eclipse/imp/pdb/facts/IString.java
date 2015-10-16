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

package org.eclipse.imp.pdb.facts;

public interface IString extends IValue {
	/**
	 * @return the Java string that this string represents
	 */
    String getValue();

    /**
     * Concatenates two strings
     */
    IString concat(IString other);
    
    /**
     * Reverses a string
     */
    IString reverse();

    /**
     * Computes the length of the string 
     * @return amount of Unicode characters 
     */
    int length();
    
    /**
     * Computes a substring
     *  
     * @param start the inclusive start index
     * @param end   the exclusive end index
     */
    IString substring(int start, int end);
    
    /**
     * Computes a substring
     *  
     * @param start the inclusive start index
     */
    IString substring(int start);
    
    /**
     * Compares two strings lexicographically
     * @param other
     * @return -1 if receiver is less than other, 0 is receiver is equal, 1 if receiver is larger
     */
    int compare(IString other);
    
    /**
     * Returns the Unicode character at the given index.
     * @param index an index into the string
     * @return the Unicode character (in UTF-32)
     */
    int charAt(int index);
    
    /**
     * Replace the characters first, second ... end.
     * Expected:
     * - support for negative indices
     * - support for the case begin > end
     * @param first  inclusive index of first element
     * @param second index of second element
     * @param end	 exclusive end index
     * @param repl	the replacement string
     * @param start the inclusive  start index
     * @return
     */
    IString replace(int first, int second, int end, IString repl);
}
