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
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;


public interface IValue  {
	/**
	 * Implements object equality. Do not use this method to compute logical
	 * or structural equality to an IValue, since this method will use type
	 * equality and not type compatibility. Also, this method will not ignore value 
	 * annotations. 
	 * <p>
	 * This method is commonly used internally by implementations of IValue and IValueFactory.
	 * 
	 * @param obj another value to compare to
	 * @return true iff the contents, the type and the annotations are equal
	 */
	@Override
	public boolean equals(Object obj);
	
	/**
	 * Computes logical equality with another value. Two values X and Y 
	 * are equal if and only if they represent equal contents. This concept of equal 
	 * contents is defined by each sub-type of IValue. However, annotations are always
	 * ignored and the types of two equal values can be different as long as they are
	 * comparable. 
	 * <p>
	 * Note that {@link #isIdentical(IValue)} may generally be faster than {@link #isEqual(IValue)}, but
	 * it is valuable to abstract from annotations to obtain separation of concerns.
	 * 
	 * @param other another value to compare to
	 * @return true iff the value is equal to the other value. (the types of
	 *         the values have to be compatible but not necessarily equal)
	 * @throws FactTypeError when the type of the other value is not comparable to the
	 *         type of this value
	 */
	boolean isEqual(IValue other) throws FactTypeError;
	
	/**
	 * Implements structural and logical equality with another value. Two values X and Y
	 * are identical if and only if they represent equal contents and all content has the
	 * same annotations. 
	 * <p>
	 * Use {@link #isEqual(IValue)} to test for equality while ignoring annotations.
	 * <p>
	 * Note that {@link #isIdentical(IValue)} may generally be faster than {@link #isEqual(IValue)}, but
	 * it is valuable to abstract from annotations to obtain separation of concerns.
	 * 
	 * @param other another value to compare to
	 * @return true iff the value and its annotations are identical to the other value
	 *         and its annotations. (the types of the values have to be compatible but not
	 *         necessarily equal)
	 * @throws FactTypeError when the type of the other value is not comparable to the
	 *         type of this value
	 */
	boolean isIdentical(IValue other) throws FactTypeError;
	
	/** 
	 * @return the Type of a value
	 */
    Type getType();
    
    /**
     * get the value of an annotation on this value
     * @param label
     * @return the value of the annotation labeled 'label' of this value, or null
     *         if it does not exist
     */
    IValue getAnnotation(String label);

    /**
     * set an annotation on this value
     * @param label
     * @param value
     * @return a new value with the annotation set
     */
    IValue setAnnotation(String label, IValue value);
    
    /**
     * determine whether a certain annotation is set on this value
     * @param label
     * @return true iff this value has an annotation with that label
     */
    boolean hasAnnotation(String label);
    
    /**
     * Execute the @see IValueVisitor on the current node
     * 
     * @param
     */
    <T> T accept(IValueVisitor<T> v) throws VisitorException;
}
