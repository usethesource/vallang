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

import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.Type;

public interface IList extends Iterable<IValue>, IValue {
	/**
	 * @return the type of the elements in the list
	 */
    public Type getElementType();
    
    /**
     * @return the number of elements in the list
     */
    public int length();
    
    /**
     * @return a new list with all elements in reverse order
     */
    public IList reverse();
    
    /**
     * Appends an element to the end of the list
     * 
     * @param e the new element
     * @return a new list with the element at the end
     */
    public IList append(IValue e);
    
    /**
     * Insers an element in front of the list
     * @param e the new element
     * @return a new list with the element in front
     */
    public IList insert(IValue e);
    
    /**
     * Concatenates this list with another
     * @param o another list
     * @return a concatenated list with the elements of the 
     *         receiver before the elements of o.
     */
    public IList concat(IList o);
    
    /**
     * Replaces the value of the ith element in the list with a new value
     * @param i index to replace a value at.
     * @param e the new value to replace the old one
     * @return a new list with the element replaced
     * @throws FactTypeError when the type of the element is not a subtype of the element type
     * @throws IndexOutOfBoundsException when the i < 0 or i >= IList.length()
     */
    public IList put(int i, IValue e) throws FactTypeError, IndexOutOfBoundsException;
    
    /**
     * Return the ith element of the list.
     * @param i
     * @return the ith element of the list
     * @throws IndexOutOfBoundsException when i < 0 or i >= IList.length
     */
    public IValue get(int i) throws IndexOutOfBoundsException;
    
    /**
     * @return true iff the list is non-empty
     */
    public boolean isEmpty();
}
