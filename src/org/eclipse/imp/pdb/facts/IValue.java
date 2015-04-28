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

import org.eclipse.imp.pdb.facts.io.StandardTextWriter;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;


public interface IValue {
	/** 
	 * @return the {@link Type} of a value
	 */
    Type getType();
    
    /**
     * Execute the {@link IValueVisitor} on the current node
     * 
     * @param
     */
    <T, E extends Throwable> T accept(IValueVisitor<T,E> v) throws E;
    
    /**
     * Warning: you may not want to use this method. The semantics of this 
     * equals is left entirely to the implementation, although it should follow
     * the contract as documented by Java's {@link Object} class.
     * <br>
     * This method computes object equality for use in implementations of IValue.
     * This equals method may implement a much more discriminating kind
     * of equals than you need, for example one where the types of two values are not
     * only comparable but exactly equal. 
     * <br>
     * For logical equality of two values, use {@link #isEqual(IValue)}.
     * 
     * @param other object to compare to
     * @return true iff the other object is equal to the receiver object
     */
    public boolean equals(Object other);
    
    /**
     * Compute logical equality of two values, which means that the data they
     * represent is equal and their types are comparable (one is a sub-type of the other).
     * 
     * @param other value to compare to
     * @return true iff the the contents of the receiver is equal to the contents
     *         of the other, and their types are comparable (one is a sub-type of the other).
     */
    public boolean isEqual(IValue other);
    
    /**
     * Prints the value to a string using the {@link StandardTextWriter}
     */
    public String toString();
    
    /**
     * @return if this {@link IValue} object can be annotated
     */
    @Deprecated
    public boolean isAnnotatable();
    
    /**
     * Creates a view that exposes the {@link IAnnotatable} annotation API. 
     * 
     * @return an {@link IAnnotatable} view on this {@link IValue} object 
     */
    @Deprecated
    public IAnnotatable<? extends IValue> asAnnotatable();
    
    /**
     * @return if this {@link IValue} object may have keyword parameters
     */
    public boolean mayHaveKeywordParameters();    
    
    /**
     * Creates a view that exposes the {@link IWithKeywordParameters} annotation API. 
     * 
     * @return an {@link IAnnotatable} view on this {@link IValue} object 
     */
    public IWithKeywordParameters<? extends IValue> asWithKeywordParameters();
}
