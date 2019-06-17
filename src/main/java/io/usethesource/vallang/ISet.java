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

import io.usethesource.vallang.visitors.IValueVisitor;

public interface ISet extends ICollection<ISet> {
	
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

            if (!deleted && e.isEqual(elem)) {
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
    public default boolean isEqual(IValue other) {
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
                    // function contains() calls isEqual() but used O(n) time
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
    public default boolean contains(IValue e) {
        // the loop might seem weird but due to the (deprecated)
        // semantics of node annotations we must check each element
        // for _deep_ equality. This is a big source of inefficiency
        // and one of the reasons why the semantics of annotations is
        // deprecated for "keyword parameters".
        for (IValue v : this) {
            if (v.isEqual(e)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * @param element
     * @return true if this is an element of the set according to the semantics of `equals`
     */
    @Deprecated
    public default boolean containsEqualElement(IValue e) {
       
       
        return false;
    }
    
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
    
    public default boolean defaultEquals(Object that) {
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
                outer:for (IValue v1 : this) {
                    
                    // the extra loop might seem weird but due to the (deprecated)
                    // semantics of node annotations we must check each element
                    // for _deep_ equality. This is a big source of inefficiency
                    // and one of the reasons why the semantics of annotations is
                    // deprecated for "keyword parameters".
                    
                    for (IValue v2 : set2) {
                        if (v2.equals(v1)) {
                            continue outer;
                        }

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
            protected final ISet set = ISet.this;
            
            @Override
            public String toString() {
                return set.toString();
            }   

            @Override
            public ISet asContainer() {
                return set;
            }
        };
    }
}
