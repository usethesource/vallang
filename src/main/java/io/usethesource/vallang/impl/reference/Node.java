/*******************************************************************************
 * Copyright (c) 2007-2013 IBM Corporation & CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package io.usethesource.vallang.impl.reference;

import java.util.Iterator;

import io.usethesource.capsule.util.iterator.ArrayIterator;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Naive implementation of an untyped tree node, using array of children.
 */
/*package*/ class Node implements INode {
    protected static final Type VALUE_TYPE = TypeFactory.getInstance().valueType();

    @Override
    public Type getType() {
        return fType;
    }

    protected final Type fType;
    protected final IValue[] fChildren;
    protected final String fName;
    protected int fHash = -1;

    /*package*/ Node(String name, IValue[] children) {
        super();
        fType = TypeFactory.getInstance().nodeType();
        fName = name;
        fChildren = children.clone();
    }

    protected Node(String name, Type type, IValue[] children) {
        super();
        fType = type;
        fName = name;
        fChildren = children.clone();
    }

    /*package*/ Node(String name) {
        this(name, new IValue[0]);
    }

    /**
     * Replaces a child
     * @param other
     * @param index
     * @param newChild
     */
    protected Node(Node other, int index, IValue newChild) {
        super();
        fType = other.fType;
        fName = other.fName;
        fChildren = other.fChildren.clone();
        fChildren[index] = newChild;
    }

    @Override
    public INode setChildren(IValue[] childArray) {
        return new Node(fName, childArray);
    }

    @Override
    public int arity() {
        return fChildren.length;
    }

    @Override
    public IValue get(int i) throws IndexOutOfBoundsException {
        try {
            return fChildren[i];
        }
        catch (ArrayIndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException("Node node does not have child at pos " + i);
        }
    }

    @Override
    public Iterable<IValue> getChildren() {
        return this;
    }

    @Override
    public String getName() {
        return fName;
    }

    @Override
    public  INode set(int i, IValue newChild) throws IndexOutOfBoundsException {
        try {
            return new Node(this, i, newChild);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException("Node node does not have child at pos " + i);
        }
    }

    @Override
    public Iterator<IValue> iterator() {
        return ArrayIterator.of(fChildren);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        else if(obj == null) {
            return false;
        }
        else if (getClass() == obj.getClass()) {
            Node other = (Node) obj;

            if (fType != other.fType) {
                return false;
            }

            if (fChildren.length != other.fChildren.length) {
                return false;
            }

            if (fName == other.fName || (fName != null && fName.equals(other.fName))) {
                for (int i = 0; i < fChildren.length; i++) {
                    if (!fChildren[i].equals(other.fChildren[i])) {
                        return false;
                    }
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return defaultToString();
    }

    public int computeHashCode() {
        int hash = fName.hashCode();

        for (int i = 0; i < fChildren.length; i++) {
            hash = (hash << 1) ^ (hash >> 1) ^ fChildren[i].hashCode();
        }
        return hash;
    }

    @Override
    public int hashCode() {
        if (fHash == -1) {
            fHash = computeHashCode();
        }
        return fHash;
    }
}
