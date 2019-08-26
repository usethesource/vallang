/*******************************************************************************
* Copyright (c) 2007 IBM Corporation, 2017 Centrum Wiskunde & Informatica
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
*    Jurgen Vinju (Jurgen.Vinju@cwi.nl) - maintenance and extensions

*******************************************************************************/

package io.usethesource.vallang;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.io.StandardTextWriter;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.visitors.IValueVisitor;


public interface IValue {
    public static final TypeFactory TF = TypeFactory.getInstance();
    
	/** 
	 * @return the {@link Type} of a value
	 */
    public Type getType();
    
    /**
     * Execute the {@link IValueVisitor} on the current node
     * 
     * @param v the visitor to dispatch to
     */
    public <T, E extends Throwable> T accept(IValueVisitor<T,E> v) throws E;
    
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
    public boolean equals(@Nullable Object other);
    
    /**
     * Another notion of equality which ignores secondary information:
     * namely: keyword parameters either top-level or deeply nested. 
     * 
     * The intention is for this notion of equality to match what the notion of pattern
     * matching is in the Rascal language. In this case the receiver acts
     * as a "pattern" for the argument.
     * 
     * The following theories hold:
     * 
     * a.equals(b) ==> a.match(b)  // and not the inverse
     * 
     * match() is an equivalence relation
     * 
     * Note that match() is linear in the size of the smallest of the
     * two values.
     * 
     * @param other value to compare to
     * @return true iff the other value is equal to the other value, while
     *         ignoring all non-structural details such the presence of keyword 
     *         parameters
     */
    public default boolean match(IValue other) {
        return equals(other);
    }
    
    /**
     * Prints the value to a string using the {@link StandardTextWriter}
     */
    public String toString();
    
    /**
     * @return if this {@link IValue} object may have keyword parameters
     */
    public default boolean mayHaveKeywordParameters() {
        return false;
    }
    
    /**
     * Creates a view that exposes the {@link IWithKeywordParameters} annotation API. 
     * 
     * @return an {@link IWithKeywordParameters} view on this {@link IValue} object 
     */
    public default IWithKeywordParameters<? extends IValue> asWithKeywordParameters() {
        throw new UnsupportedOperationException(getType() + " does not support keyword parameters.");
    }
    
    /**
     * This is how all IValue implementations should be printed.
     * @return the literal expression format of the value as a string
     */
    public default String defaultToString() {
        return StandardTextWriter.valueToString(this);
    }
}
