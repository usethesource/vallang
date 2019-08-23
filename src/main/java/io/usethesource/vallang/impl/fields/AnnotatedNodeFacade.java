/*******************************************************************************
 * Copyright (c) 2013-2014 CWI
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
import io.usethesource.vallang.IAnnotatable;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IWithKeywordParameters;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.visitors.IValueVisitor;

public class AnnotatedNodeFacade implements INode {

    protected final INode content;
    protected final Map.Immutable<String, IValue> annotations;

    public AnnotatedNodeFacade(final INode content, final Map.Immutable<String, IValue> annotations) {
        this.content = content;
        this.annotations = annotations;
    }

    @SuppressWarnings("deprecation")
    @Override
    public INode setChildren(IValue[] childArray) {
        return content.setChildren(childArray).asAnnotatable().setAnnotations(annotations);
    }

    @Override
    public Type getType() {
        return content.getType();
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
        return new AnnotatedNodeFacade(newContent, annotations); // TODO: introduce wrap() here as well
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
        return new AnnotatedNodeFacade(newContent, annotations); // TODO: introduce wrap() here as well
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == this) {
            return true;
        }
        
        if (o == null) {
            return false;
        }

        if (o.getClass() == getClass()){
            AnnotatedNodeFacade other = (AnnotatedNodeFacade) o;

            return content.equals(other.content) &&
                    annotations.equals(other.annotations);
        }

        return false;
    }

    @Override
    public boolean isEqual(IValue other) {
        return content.isEqual(other);
    }

    @Override
    public boolean match(IValue other) {
        return content.match(other);
    }	

    @Override
    public int hashCode() {
        return content.hashCode();
    }

    @Override
    public boolean isAnnotatable() {
        return true;
    }

    @Override
    public IAnnotatable<? extends INode> asAnnotatable() {
        return new AbstractDefaultAnnotatable<INode>(content, annotations) {

            @Override
            protected INode wrap(INode content, Map.Immutable<String, IValue> annotations) {
                return new AnnotatedNodeFacade(content, annotations);
            }
        };
    }

    @Override
    public boolean mayHaveKeywordParameters() {
        return false;
    }

    @Override
    public IWithKeywordParameters<? extends INode> asWithKeywordParameters() {
        throw new UnsupportedOperationException("can not add keyword parameters to a node (" + content.getName() + ") which already has annotations");
    }
}
