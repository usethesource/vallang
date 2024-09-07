/*******************************************************************************
* Copyright (c) 2019 NWO-I CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Jurgen J. Vinju - initial implementation
*******************************************************************************/
package io.usethesource.vallang;

import java.util.Iterator;
import java.util.function.Function;

import io.usethesource.vallang.exceptions.IllegalOperationException;
import io.usethesource.vallang.type.Type;

/**
 * Provides Relational Calculus operators to an existing ICollection.
 * This interface provides a generic implementation for all operators,
 * which should be specialized by implementations that know how to optimize
 * these operations with specialized data-structures.
 *
 * @param <C> a collection value type like ISet or IList
 */
public interface IRelation<C extends ICollection<C>> extends Iterable<IValue> {

    @Override
    default Iterator<IValue> iterator() {
        return asContainer().iterator();
    }

    /**
     * Relational composition works only on binary relations. It matches the second column
     * of the receiver with the first column of the given relation.
     * TODO: generalize to n-ary. Implementing classes should specialize
     * this generic implementation for more efficiency.
     *
     * @param that is the given relation
     * @return a new binary relation with first column of the accepting relation and the second column
     *        of the given relation, containing only tuples where the last column of the receiver
     *        matches the first column of the given relation.
     */
    public default C compose(IRelation<C> that) {
        C thisContainer = this.asContainer();
        C thatContainer = that.asContainer();
        Type thisElementType = thisContainer.getElementType();
        Type thatElementType = thatContainer.getElementType();

        if (thisElementType.isBottom()) {
            return thisContainer;
        }

        if (thatElementType.isBottom()) {
            return thatContainer;
        }

        if (thisElementType.getArity() != 2 || thatElementType.getArity() != 2) {
            throw new IllegalOperationException("Incompatible types for composition.", thisElementType, thatElementType);
        }

        if (!thisElementType.getFieldType(1).comparable(thatElementType.getFieldType(0))) {
            return asContainer().empty();
        }

        IWriter<C> w = writer();

        for (IValue elem1 : this) {
            ITuple tuple1 = (ITuple) elem1;
            for (IValue elem2 : that) {
                ITuple tuple2 = (ITuple) elem2;
                if (tuple1.get(1).equals(tuple2.get(0))) {
                    w.appendTuple(tuple1.get(0), tuple2.get(1));
                }
            }
        }

        return w.done();
    }

    /**
     * @return the transitive non-reflexive closure of a binary relation
     * @throws UnsupportedOperationException when the receiver is not a binary relation
     */
    public default C closure() {
        if (!isBinary()) {
            throw new UnsupportedOperationException("relation is not binary");
        }

        C next = this.asContainer();
        IRelation<C> result;

        do {
            result = next.asRelation();
            next = result.compose(result).union(next);
        } while (!next.equals(result.asContainer()));

        return next;
    }

    /**
     * @return the transitive reflexive closure of a binary relation
     * @throws UnsupportedOperationException when the receiver is not a binary relation
     */
    public default C closureStar() {
        IWriter<C> w = writer();

        for (IValue val : carrier()) {
            w.appendTuple(val, val);
        }
        w.appendAll(closure());

        return w.done();
    }

    /**
     * @return the number of columns in the relation
     */
    default int arity() {
        return asContainer().getElementType().getArity();
    }

    /**
     * @return a new empty relation with the current factory
     */
    default C empty() {
        return asContainer().empty();
    }

    /**
     * Reduces an n-ary relation to fewer columns, given by the fields to select.
     * @param fields to select from the relation, index starts at 0
     * @return a new relation with only the columns selected by the fields parameter
     */
    default C project(int... fields) {
        IWriter<C> w = writer();

        for (IValue v : this) {
            w.append(((ITuple) v).select(fields));
        }

        return w.done();
    }

    /**
     * Compute the carrier set of an n-ary relation.
     * @return a container with all the elements of all tuples in the relation.
     */
    public default C carrier() {
        IWriter<C> w = writer().unique();

        for (IValue t : this) {
            w.appendAll((ITuple) t);
        }

        return w.done();
    }

    /**
     * @return a container with the first elements of all tuples in the relation
     */
    public default C domain() {
        IWriter<C> w = asContainer().writer();

        for (IValue elem : this) {
            w.insert(((ITuple) elem).get(0));
        }

        return w.done();
    }

    /**
     * @return a container with the last elements of all tuples in the relation
     */
    public default C range() {
        int columnIndex = arity() - 1;
        IWriter<C> w = writer();

        for (IValue elem : this) {
            w.insert(((ITuple) elem).get(columnIndex));
        }

        return w.done();
    }

    /**
     * Lookup the values by the first column in the relation
     * @return a set of the second elements of all tuples where the first column is
     *         equal to the given key, without the first column.
     *
     * TODO: generalize to producing n-ary tuples.
     */
    public default C index(IValue key) {
        C set1 = asContainer();
        Type elementType = getElementType();

        if (elementType.isBottom()) {
            return set1.empty();
        }

        int valueArity = elementType.getArity() - 1;

        Function<ITuple, IValue> mapper;
        if (valueArity == 0) {
            mapper = t -> t.get(1);
        }
        else {
            int[] newTupleIndex = new int[valueArity];
            for (int k = 1; k <= valueArity; k++) {
                newTupleIndex[k - 1] =  k;
            }
            mapper = t -> t.select(newTupleIndex);
        }

        IWriter<C> result = writer();
        for (IValue val : this) {
            ITuple tup = (ITuple) val;
            if (tup.get(0).equals(key)) {
                result.insert(mapper.apply(tup));
            }
        }

        return result.done();
    }

    /**
     * @return the original container this IRelation<C> is wrapping.
     */
    public C asContainer();

    /**
     * @return a fresh writer for the kind of container this IRelation is wrapping
     */
    public default IWriter<C> writer() {
        return asContainer().writer();
    }

    /**
     * @return the element type of the container this IRelation is wrapping
     */
    public default Type getElementType() {
        return asContainer().getElementType();
    }

    /**
     * @return true iff the current relation is binary (has two columns)
     */
    default boolean isBinary() {
        return getElementType().isBottom() || getElementType().getArity() == 2;
    }
}
