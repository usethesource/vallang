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
     * This method is used exclusively by code generated by the Rascal compiler,
     * or by the Rascal interpreter. The returned integer codes are opaque, although stable.
     * If you need to know what kind of value you have, use the IValueVisitor or the ITypeVisitor
     * interfaces and the `accept` methods on IValue and Type.
     *
     * @return an integer code that:
     *    * accurate reflects the identity of the top-level structure of this value
     *    * such that if pattern.match(this) ===> pattern.getPatternMatchFingerprint() == this.getPatternMatchFingerprint()
     *    * distinguishes maximally between different kinds of values
     *    * never makes the same or similar value have a different fingerprint
     */
    default int getMatchFingerprint() {
        return hashCode();
    }

     /**
     * This method is used exclusively by code generated by the Rascal compiler,
     *
     * @return an integer code that:
     *    * is guaranteed to be different from `getMatchFingerPrint`
     *    * is guaranteed to be constant
     *    * is guaranteed to be the same for every IValue
     */
    static int getDefaultMatchFingerprint() {
        return 0;
    }

    /**
     * Execute the {@link IValueVisitor} on the current node
     *
     * @param v the visitor to dispatch to
     */
    public <T, E extends Throwable> T accept(IValueVisitor<T,E> v) throws E;

    /**
     * This method computes logical equality between IValues. That means that it
     * may equalize objects implemented by different classes implementing the
     * same interface. It does uphold Java's equals/hashCode contract, nevertheless.
     *
     * <p>In particular, lazy intermediate representations of IList, ISet or IString would
     * lead to IValue objects being equal even though their internal structure differs greatly.
     * Implementors should think hard how to uphold equals/hashCode contract in such cases.</p>
     *
     * <p>A note on subtitutability: given that IValue's are immutable, and this equals method
     * implements an equivalence relation and that it upholds the hashCode/equals contract:
     * if equals(a,b) then a is substitutable for b and vice versa in _any_ context. This implies
     * full referential transparancy as well.</p>
     *
     * <p>On the computational complexity of equals: although equality is worst case linear,
     * implementations are expected to fail fast, and succeed fast. In particular the implementations
     * based on persistent and shared data-structures are expected to test for reference equality at every
     * level in a value. The expected/average complexity of equals should be around log(size), as a result.</p>
     *
     * @param other object to compare to
     * @return true iff the other value is logically equal to the receiver object
     */
    @Override
    public boolean equals(@Nullable Object other);

    /**
     * Another notion of equality which ignores secondary information:
     * namely, keyword parameters (either top-level or deeply nested).
     *
     * <p>The intention is for this notion of equality to match what the notion of pattern
     * matching is in the Rascal language. In this case the receiver acts
     * as a "pattern" for the argument.</p>
     *
     * <p>The following theories hold:
     *
     * <ul><li>. a.equals(b) ==> a.match(b)  // and not the inverse</li>
     *     <li> match() is an equivalence relation</li></ul>
     * </p>
     *
     * <p>Note that the match() algorithm is linear in the size of the smallest of the
     * two values, but implementations are expected to fail fast and succeed fast where
     * possible (checking for reference equality on the way).</p>
     *
     * @param other value to compare to
     * @return true iff the other value is equal to the other value, while
     *         ignoring all "non-structural" details such the presence of keyword
     *         parameters.
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
