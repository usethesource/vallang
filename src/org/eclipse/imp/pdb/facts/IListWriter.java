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


/**
 * This interface allows to gather the elements of a list efficiently and in a specific order.
 * When all elements have been gathered the done() method can be used to obtain an immutable IList
 * with the gathered elements in the specified order.
 * 
 * Note: implementations are not required to guarantee thread-safe access to the writer object.
 */
public interface IListWriter {
	
	/**
	 * Inserts elements in front, keeping the argument in order of appearance.
	 * 
	 * @param value an array of elements to insert in front.
	 * @throws FactTypeError when done() was called before or when the elements have an incompatible type.
	 */
    void insert(IValue... value) throws FactTypeError;
    
    /**
	 * Inserts elements at a specific position, keeping the argument in order of appearance.
	 * 
	 * @param index 
	 * @param value an array of elements to insert .
	 * @throws FactTypeError when done() was called before or when the elements have an incompatible type.
	 * @throws IndexOutOfBoundsException 
	 */
    void insertAt(int index, IValue... value) throws FactTypeError, IndexOutOfBoundsException;
    
    /**
	 * Inserts elements in front, keeping the argument in order of appearance.
	 * 
	 * @param elems an array of elements to insert
	 * @param start index to start copying elements from
	 * @param length amount of elements to copy from the array
	 * 
	 * @throws FactTypeError when done() was called before or when the elements have an incompatible type.
	 * @throws IndexOutOfBoundsException
	 */
    void insert(IValue[] elems, int start, int length) throws FactTypeError, IndexOutOfBoundsException;
    
    /**
	 * Inserts elements at a specific position, keeping the argument in order of appearance.
	 * 
	 * @param index place to insert elements at
	 * @param elems an array of elements to insert
	 * @param start index to start copying elements from
	 * @param length amount of elements to copy from the array
	 * 
	 * @throws FactTypeError when done() was called before or when the elements have an incompatible type.
	 * @throws IndexOutOfBoundsException
	 */
    void insertAt(int index, IValue[] elems, int start, int length) throws FactTypeError, IndexOutOfBoundsException;
    
    /**
     * Replaces an existing element at index in the list.
     * @param index the location where to replace the element
     * @param elem the new element
     * @throws FactTypeError when the type of the new element is not a subtype of the element type
     * @throws IndexOutOfBoundsException
     */
    void replaceAt(int index, IValue elem) throws FactTypeError, IndexOutOfBoundsException;
    /**
     * Append elements at the end.
     * 
     * @param value array of elements to append
     * @throws FactTypeError when done() was called before or when the elements have an incompatible type.
     */
    void append(IValue... value) throws FactTypeError;
    
    /**
     * Append elements at the end.
     * 
     * @param value array of elements to append
     * @throws FactTypeError when done() was called before or when the elements have an incompatible type.
     */
    void appendAll(Iterable<? extends IValue> collection) throws FactTypeError;
    
    /**
     * Finalize an immutable list. After this method none of the others may be called anymore.
     * @return an immutable IList
     */
    IList done();
}
