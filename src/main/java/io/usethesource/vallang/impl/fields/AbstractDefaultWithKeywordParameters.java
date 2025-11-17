/*******************************************************************************
 * Copyright (c) 2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package io.usethesource.vallang.impl.fields;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import io.usethesource.capsule.util.collection.AbstractSpecialisedImmutableMap;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IWithKeywordParameters;


/**
 * A generic wrapper for an {@link IValue} that associates keyword parameters to it.
 *
 * @param <T> the interface over which this parameter wrapper closes
 */
public abstract class AbstractDefaultWithKeywordParameters<T extends IValue> implements IWithKeywordParameters<T> {
    protected final T content;
    protected final io.usethesource.capsule.Map.Immutable<String, IValue> parameters;

    /**
     * Creates an {@link IWithKeywordParameters} view on {@link #content} with already
     * provided {@link #parameters}.
     *
     * @param content
     *            is the wrapped object that supports keywod fields
     * @param parameters
     *            is the map of fields associated to {@link #content}
     */
    public AbstractDefaultWithKeywordParameters(T content, io.usethesource.capsule.Map.Immutable<String, IValue> parameters) {
        this.content = content;
        this.parameters = parameters;
    }

    /**
     * Wraps {@link #content} with other parameters. This methods is mandatory
     * because of PDB's immutable value nature: Once parameters are modified, a
     * new immutable view is returned.
     *
     * @param content
     *            is the wrapped object that supports keyword fields
     * @param annotations
     *            is the map of fields associated to {@link #content}
     * @return a new representations of {@link #content} with associated
     *         {@link #parameters}
     */
    protected abstract T wrap(final T content, final io.usethesource.capsule.Map.Immutable<String, IValue> parameters);

    @Override
    public String toString() {
        return content.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X extends @Nullable IValue> X getParameter(String label) {
        return (X) parameters.get(label);
    }

    @Override
    public T setParameter(String label, IValue newValue) {
        return wrap(content, parameters.__put(label, newValue));
    }

    @Override
    public T unsetParameter(String label) {
        io.usethesource.capsule.Map.Immutable<String, IValue> removed = parameters.__remove(label);

        if (removed.isEmpty()) {
            return content;
        }
        return wrap(content, removed);
    }

    @Override
    public T unsetAll() {
        return content;
    }

    @Override
    @SuppressWarnings("contracts.conditional.postcondition") // CF has a bug around super classes with EnsuresNonNullIf
    @EnsuresNonNullIf(expression="getParameter(#1)", result=true)
    public boolean hasParameter(String label) {
        return parameters.containsKey(label);
    }

    @Override
    public boolean hasParameters() {
        return !parameters.isEmpty();
    }

    @Override
    @Pure
    public Set<String> getParameterNames() {
        return Collections.unmodifiableSet(parameters.keySet());
    }

    @Override
    public Map<String,IValue> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public int hashCode() {
        return 91 + content.hashCode() * 13 + 101 * parameters.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == null) {
            return false;
        }

        if (!getClass().equals(other.getClass())) {
            return false;
        }

        AbstractDefaultWithKeywordParameters<? extends IValue> o = (AbstractDefaultWithKeywordParameters<?>) other;

        if (!content.equals(o.content)) {
            return false;
        }

        if (parameters.size() != o.parameters.size()) {
            return false;
        }

        // TODO: there should be a faster way for this
        for (String key : parameters.keySet()) {
            if (!parameters.get(key).equals(o.getParameter(key))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public <U extends IWithKeywordParameters<? extends IValue>> boolean equalParameters(U other) {
        if (!(other instanceof AbstractDefaultWithKeywordParameters<?>)) {
            return false;
        }

        AbstractDefaultWithKeywordParameters<? extends IValue> o = (AbstractDefaultWithKeywordParameters<?>) other;

        if (parameters.size() != o.parameters.size()) {
            return false;
        }

        for (String key : parameters.keySet()) {
            IValue parameter = getParameter(key);
            if (parameter == null && o.getParameter(key) != null) {
                return false;
            }
            else if (parameter != null && !parameter.equals(o.getParameter(key))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public T setParameters(Map<String, IValue> params) {
        if (params.isEmpty()) {
            return content;
        }
        return wrap(content, AbstractSpecialisedImmutableMap.mapOf(params));
    }

    /**
     * This method is only to be used by internal methods, such as testing and fast iterators
     */
    public io.usethesource.capsule.Map.Immutable<String, IValue> internalGetParameters() {
        return parameters;
    }
}
