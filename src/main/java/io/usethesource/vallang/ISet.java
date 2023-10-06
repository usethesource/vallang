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

package io.usethesource.vallang;

import java.util.Iterator;

import org.checkerframework.checker.interning.qual.EqualsMethod;
import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.visitors.IValueVisitor;

public interface ISet extends ICollection<ISet> {
	
    @Override
    default int getPatternMatchFingerprint() {
        return 113762; // "set".hashCode()
    }

    /**
     * Add an element to the set. 
     * @param element
     * @return a relation if the element type is a tuple type, a set otherwise
     */
    public default ISet insert(IValue element) {
        IWriter<ISet> sw = writer();
        sw.insertAll(this);
        sw.insert(element);
        return sw.done();
    }

    /**
     * Delete one element from the set.
     * @param elem
     * @return a set with one element removed, if present.
     */
    public default ISet delete(IValue elem) {
        IWriter<ISet> w = writer();

        boolean deleted = false;
        for (Iterator<IValue> iterator = iterator(); iterator.hasNext();) {
            IValue e = iterator.next();

            if (!deleted && e.equals(elem)) {
                deleted = true; // skip first occurrence
            } else {
                w.insert(e);
            }
        }
        return w.done();
    }
    
    /**
     * Computes the Cartesian product of two sets
     * @param set
     * @return a relation representing the Cartesian product
     */
    @Override
    public default ISet product(ISet that) {
        IWriter<ISet> w = writer();

        for (IValue t1 : this) {
            for (IValue t2 : that) {
                w.insertTuple(t1, t2);
            }
        }

        return w.done();
    }
    
    @Override
    public default boolean match(IValue other) {
        if (other == this) {
            return true;
        }
        
        if (other == null) {
            return false;
        }

        if (other instanceof ISet) {
            ISet set2 = (ISet) other;

            if (size() == set2.size()) {

                for (IValue v1 : this) {
                    // function containsMatch() calls match() but uses O(n) time
                    if (!set2.containsMatch(v1)) {
                        return false;
                    }
                }

                return true;
            }
        }

        return false;
    }
    
    /**
     * Compute the union of two sets. To be overridden
     * by implementations who know how to do this more efficiently.
     * @return the mathematical union of the two sets
     */
    @Override
    public default ISet union(ISet that) {
        if (this == that) {
            return this;
        }

        if (this.isEmpty()) {
            return that;
        }

        if (that.isEmpty()) {
            return this;
        }

        IWriter<ISet> w = writer();
        w.insertAll(this);
        w.insertAll(that);
        return w.done();
    }
    
    /**
     * Compute the intersection of two sets. To be overridden
     * by implementations who know how to do this more efficiently.
     * @return the mathematical intersection of the two sets
     */
    public default ISet intersect(ISet that) {
        if (this == that) {
            return this;
        }

        if (this.isEmpty()) {
            return this;
        }

        if (that.isEmpty()) {
            return that;
        }

        IWriter<ISet> w = writer();

        for (IValue v : this) {
            if (that.contains(v)) {
                w.insert(v);
            }
        }

        return w.done();
    }
    
    /**
     * Compute the difference of two sets by removing the elements of `that`
     * from the receiver. To be implemented more efficiently if possible
     * by implementations of ISet.
     * @return the mathematical difference of two sets
     */
    public default ISet subtract(ISet that) {
        if (this == that) {
            return writer().done();
        }

        if (this.isEmpty()) {
            return this;
        }

        if (that.isEmpty()) {
            return this;
        }

        IWriter<ISet> sw = writer();
        for (IValue a : this) {
            if (!that.contains(a)) {
                sw.insert(a);
            }
        }

        return sw.done();
    }
    
    /**
     * @param element
     * @return true if this is an element of the set
     */
    public boolean contains(IValue e);
    
    /**
     * @param element
     * @return true if this set contains an element that matches (IValue.match) an element in this set
     */
    public default boolean containsMatch(IValue e) {
        for (IValue v : this) {
            if (v.match(e)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * @param that other set to check for subset relation
     * @return true if all elements of this set are elements of the other.
     */
    public default boolean isSubsetOf(ISet that) {
        for (IValue elem : this) {
            if (!that.contains(elem)) {
                return false;
            }
        }
        return true;
    }

    /**
     * A default implementation of Object.hashCode() for use in implementations of ISet.
     */
    public default int defaultHashCode() {
        int hash = 0;

        Iterator<IValue> iterator = this.iterator();
        while (iterator.hasNext()) {
            IValue element = iterator.next();
            hash ^= element.hashCode();
        }

        return hash;
    }
    
    @EqualsMethod
    public default boolean defaultEquals(@Nullable Object that) {
        if (that == this) {
            return true;
        }
        
        if (that == null) {
            return false;
        }

        if (that instanceof ISet) {
            ISet set2 = (ISet) that;

            if (this.getType() != set2.getType()) {
                return false;
            }

            if (hashCode() != set2.hashCode()) {
                return false;
            }

            if (size() == set2.size()) {
                for (IValue v1 : this) {
                    if (!set2.contains(v1)) {
                        return false;
                    }
                }

                return true;
            }
        }

        return false;
    }
    
    @Override
    default <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
        return v.visitSet(this);
    }
    
    @Override
    default IRelation<ISet> asRelation() {
        if (!getType().isRelation()) {
            throw new UnsupportedOperationException(getType() + " is not a relation");
        }
        
        return new IRelation<ISet>() {
            @Override
            public String toString() {
                return ISet.this.toString();
            }   

            @Override
            public ISet asContainer() {
                return ISet.this;
            }
        };
    }
}
