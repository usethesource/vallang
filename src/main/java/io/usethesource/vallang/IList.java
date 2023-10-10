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
import java.util.Random;

import org.checkerframework.checker.interning.qual.EqualsMethod;
import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.visitors.IValueVisitor;

public interface IList extends ICollection<IList> {
    
    @Override
    default int getMatchFingerprint() {
        return 3322014; // "list".hashCode()
    }

    /**
     * @return the number of elements in the list
     */
    @Override
    public default int size() {
        return length();
    }
    
    /**
     * Return the length of the list == size();
     */
    public int length();
    
    /**
     * @return a new list with all elements in reverse order
     */
    public default IList reverse() {
        IListWriter w = writer();
        for (IValue e : this) {
            w.insert(e);
        }
        return w.done();
    }
    
    /**
     * @param rand the random generator to use for the shuffling. If the same seed is set, the same shuffling should happen.
     * @return a new list with all the elements randomly shuffled.
     */
    public default IList shuffle(Random rand) {
        IListWriter w = writer();
        w.appendAll(this); // add everything
        // we use Fisher–Yates shuffle (or Knuth shuffle)
        // unbiased and linear time (incase of random access)
        for (int i = length() - 1; i >= 1; i--) {
            w.replaceAt(i, w.replaceAt(rand.nextInt(i + 1), w.get(i)));
        }
        return w.done();
    }
    
    /**
     * @return an IListWriter for the current list implementation
     */
    @Override
    public IListWriter writer();
    
    /**
     * Appends an element to the end of the list
     * 
     * @param e the new element
     * @return a new list with the element at the end
     */
    default IList append(IValue e) {
        IListWriter w = writer();
        w.appendAll(this);
        w.append(e);

        return w.done();
    }
    
    /**
     * Inserts an element in front of the list
     * @param e the new element
     * @return a new list with the element in front
     */
    default IList insert(IValue e) {
        IListWriter w = writer();
        w.appendAll(this);
        w.insert(e);

        return w.done();
    }
    
    /**
     * Concatenates this list with another
     * @param o another list
     * @return a concatenated list with the elements of the 
     *         receiver before the elements of o.
     */
    default IList concat(IList o) {
        IListWriter w = writer();
        w.appendAll(this);
        w.appendAll(o);
        return w.done();
    }
    
    /**
     * Replaces the value of the ith element in the list with a new value
     * @param i index to replace a value at.
     * @param e the new value to replace the old one
     * @return a new list with the element replaced
     * @throws FactTypeUseException when the type of the element is not a subtype of the element type
     * @throws IndexOutOfBoundsException when the i < 0 or i >= IList.length()
     */
    public default IList put(int i, IValue e) {
        IListWriter w = writer();
        w.appendAll(this);
        w.replaceAt(i, e);
        return w.done();
    }
    
    /**
     * Replaces the value of the elements first, second ... end in the list with the elements in the list r
     * Expected:
     * - support for negative indices
     *  - support for the case begin > end
     * @param first index to start replacement (inclusive)
     * @param second index of second element
     * @param end index to end replacement (exclusive)
     * @param repl the new values to replace the old one
     * @return a new list with the elements replaced
     * @throws FactTypeUseException when the type of the element is not a subtype of the element type
     * @throws IndexOutOfBoundsException when the b < 0 or b >= IList.length() or e < 0 or e > IList.length()
     */
    public default IList replace(int first, int second, int end, IList repl) {
        IListWriter result = writer();

        int rlen = repl.length();
        int increment = Math.abs(second - first);

        if (first < end) {
            int listIndex = 0;
            // Before begin
            while (listIndex < first) {
                result.append( get(listIndex++));
            }
            int replIndex = 0;
            boolean wrapped = false;
            // Between begin and end
            while (listIndex < end) {
                result.append(repl.get(replIndex++));
                if (replIndex == rlen) {
                    replIndex = 0;
                    wrapped = true;
                }
                listIndex++; // skip the replaced element
                for (int j = 1; j < increment && listIndex < end; j++) {
                    result.append(get(listIndex++));
                }
            }
            if (!wrapped) {
                while (replIndex < rlen) {
                    result.append(repl.get(replIndex++));
                }
            }
            // After end
            int dlen = length();
            while (listIndex < dlen) {
                result.append(get(listIndex++));
            }
        } else {
            // Before begin (from right to left)
            int listIndex = length() - 1;
            while (listIndex > first) {
                result.insert(get(listIndex--));
            }
            // Between begin (right) and end (left)
            int replIndex = 0;
            boolean wrapped = false;
            while (listIndex > end) {
                result.insert(repl.get(replIndex++));
                if (replIndex == repl.length()) {
                    replIndex = 0;
                    wrapped = true;
                }
                listIndex--; // skip the replaced element
                for (int j = 1; j < increment && listIndex > end; j++) {
                    result.insert(get(listIndex--));
                }
            }
            if (!wrapped) {
                while (replIndex < rlen) {
                    result.insert(repl.get(replIndex++));
                }
            }
            // Left of end
            while (listIndex >= 0) {
                result.insert(get(listIndex--));
            }
        }

        return result.done();
    }
    
    /**
     * Return the ith element of the list.
     * 
     * @param i
     * @return the ith element of the list
     * @throws IndexOutOfBoundsException when i < 0 or i >= IList.length
     */
    public IValue get(int i) throws IndexOutOfBoundsException;
    
    /**
     * Compute a sublist.
     * 
     * @param offset inclusive start index of the sublist
     * @param length number of elements in the resulting list
     * @return a new list that contains this[offset] until this[offset+length-1].
     */
    default IList sublist(int offset, int length) {
        if (offset < 0 || length < 0 || offset + length > length()) {
            throw new IndexOutOfBoundsException();
        }
        
        IListWriter w = writer();
        for (int i = offset; i < offset + length; i++) {
            w.append(get(i));
        }
        return w.done();
    }
    
    /**
     * @return true iff the list is non-empty
     */
    public boolean isEmpty();
    
    /**
     * @param e
     * @return true iff e is an element of the list
     */
    public default boolean contains(IValue e) {
        for (IValue v : this) {
            if (v.equals(e)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Removes the first occurrence of an element, i.e. the
     * element with the lowest index that is present in the list,
     * if present at all.
     * @param e
     * @return a new list, with one element removed.
     */
    public default IList delete(IValue v) {
        IListWriter w = writer();

        boolean deleted = false;
        
        for (IValue e : this) {
            if (!deleted && e.equals(v)) {
                deleted = true; // skip first occurrence
            } else {
                w.append(e);
            }
        }
        return w.done();
    }

    /**
     * Removes the element at index <code>i</code>.
     * 
     * @param i
     * @return a new list with one element removed.
     */
    public default IList delete(int index) {
        IListWriter w = writer();

        int currentIndex = 0;
        boolean deleted = false;
        
        for (Iterator<IValue> iterator = iterator(); iterator.hasNext(); currentIndex++) {
            IValue e = iterator.next();

            if (!deleted && index == currentIndex) {
                deleted = true; // skip first occurrence
            } else {
                w.append(e);
            }
        }
        return w.done();
    }
    
    /**
     * Carthesian product of two lists.
     * 
     * @param l
     * @return a new list relation containing the product
     */
    @Override
    public default IList product(IList l) {
        IWriter<IList> w = writer();

        for (IValue t1 : this) {
            for (IValue t2 : l) {
                w.appendTuple(t1, t2);
            }
        }

        return w.done();
    }
    
    /**
     * Intersection of two lists
     * @param l
     * @return a new list that is the intersection
     */
    public default IList intersect(IList l) {
        IWriter<IList> w = writer();

        for (IValue v : this) {
            if (l.contains(v)) {
                w.append(v);
            }
        }

        return w.done();
    }
    
    /**
     * Difference of two lists
     * @param l
     * @return a new list that is the intersection
     */
    public default IList subtract(IList l) {
        IWriter<IList> w = writer();
        for (IValue v : this) {
            if (l.contains(v)) {
                l = l.delete(v);
            } else {
                w.append(v);
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

        if (other instanceof IList) {
            IList list2 = (IList) other;

            if (length() == list2.length()) {
                final Iterator<IValue> it1 = iterator();
                final Iterator<IValue> it2 = list2.iterator();

                while (it1.hasNext() && it2.hasNext()) {
                    // call to IValue.isEqual(IValue)
                    if (!it1.next().match(it2.next())) {
                        return false;
                    }
                }

                assert (!it1.hasNext() && !it2.hasNext());
                return true;
            }
        }

        return false;
    }
    
    /**
     * @return true if this list is a sublist of list l
     */
    public default boolean isSubListOf(IList l) {
        int j = 0;
        nextValue: for (IValue elm : this) {
            while (j < l.length()) {
                if (elm.equals(l.get(j))) {
                    j++;
                    continue nextValue;
                } else {
                    j++;
                }
            }
            return false;
        }
        return true;
    }

    @EqualsMethod
    public default boolean defaultEquals(@Nullable Object other) {
        if (other == this) {
            return true;
        }
        
        if (other == null) {
            return false;
        }

        if (other instanceof IList) {
            IList list2 = (IList) other;

            if (isEmpty() && list2.isEmpty()) {
                return true;
            }
            
            if (getType() != list2.getType())
                return false;

            if (hashCode() != list2.hashCode()) {
                return false;
            }

            if (length() == list2.length()) {

                final Iterator<IValue> it1 = iterator();
                final Iterator<IValue> it2 = list2.iterator();

                while (it1.hasNext() && it2.hasNext()) {
                    if (!it1.next().equals(it2.next())) {
                        return false;
                    }
                }

                assert (!it1.hasNext() && !it2.hasNext());
                return true;
            }
        }

        return false;
    }
    
    default int defaultHashCode() {
        int hash = 0;

        for (IValue element : this) {
            hash = (hash << 1) ^ element.hashCode();
        }

        return hash;
    }
    
    @Override
    default <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
        return v.visitList(this);
    }
    
    @Override
    default IRelation<IList> asRelation() {
        if (!getType().isListRelation()) {
            throw new UnsupportedOperationException(getType() + " is not a relation");
        }
        
        return new IRelation<IList>() {
            @Override
            public String toString() {
                return IList.this.toString();
            }

            @Override
            public IList asContainer() {
                return IList.this;
            }
            
            @Override
            public IWriter<IList> writer() {
               return IList.this.writer();
            }
        };
    }
}