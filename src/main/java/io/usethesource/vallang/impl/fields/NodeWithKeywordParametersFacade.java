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

import java.util.Iterator;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.capsule.Map;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IWithKeywordParameters;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.visitors.IValueVisitor;

public class NodeWithKeywordParametersFacade implements INode {
    protected final INode content;
    protected final Map.Immutable<String, IValue> parameters;

    public NodeWithKeywordParametersFacade(final INode content, final Map.Immutable<String, IValue> parameters) {
        this.content = content;
        this.parameters = parameters;
    }

    @Override
    public Type getType() {
        return content.getType();
    }

    @Override
    public INode setChildren(IValue[] childArray) {
        return content.setChildren(childArray).asWithKeywordParameters().setParameters(parameters);
    }

    @Override
    public <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
        return v.visitNode(this);
    }

    @Override
    public IValue get(int i) {
        return content.get(i);
    }

    @Override
    public INode set(int i, IValue newChild) {
        INode newContent = content.set(i, newChild);
        return new NodeWithKeywordParametersFacade(newContent, parameters); // TODO: introduce wrap() here as well
    }

    @Override
    public int arity() {
        return content.arity();
    }

    @Override
    public String toString() {
        return defaultToString();
    }

    @Override
    public String getName() {
        return content.getName();
    }

    @Override
    public Iterable<IValue> getChildren() {
        return content.getChildren();
    }

    @Override
    public Iterator<IValue> iterator() {
        return content.iterator();
    }

    @Override
    public INode replace(int first, int second, int end, IList repl) {
        INode newContent = content.replace(first, second, end, repl);
        return new NodeWithKeywordParametersFacade(newContent, parameters); // TODO: introduce wrap() here as well
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if(o == this) {
            return true;
        }
        if(o == null) {
            return false;
        }

        if(o.getClass() == getClass()){
            NodeWithKeywordParametersFacade other = (NodeWithKeywordParametersFacade) o;

            return content.equals(other.content) && parameters.equals(other.parameters);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return 15551 + 7 * content.hashCode() + 11 * parameters.hashCode();
    }

    @Override
    public boolean mayHaveKeywordParameters() {
        return true;
    }

    @Override
    public IWithKeywordParameters<? extends INode> asWithKeywordParameters() {
        return new AbstractDefaultWithKeywordParameters<INode>(content, parameters) {
            @Override
            protected INode wrap(INode content, Map.Immutable<String, IValue> parameters) {
                return new NodeWithKeywordParametersFacade(content, parameters);
            }
        };
    }

}
