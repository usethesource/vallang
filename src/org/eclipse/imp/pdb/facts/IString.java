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
     * @param other
     * @return
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
     * @return
     */
    IString substring(int start, int end);
    
    /**
     * Compares two strings lexicographically
     * @param other
     * @return -1 if receiver is less than other, 0 is receiver is equal, 1 if receiver is larger
     */
    int compare(IString other);
    
    /**
     * Returns the Unicode character at the given index.
     * @param index
     * @return
     */
    int charAt(int index);
}
