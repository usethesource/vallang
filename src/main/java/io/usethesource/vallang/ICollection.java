package io.usethesource.vallang;

import io.usethesource.vallang.exceptions.IllegalOperationException;
import io.usethesource.vallang.type.Type;

public interface ICollection<T extends ICollection<T>> extends IValue, Iterable<IValue> {
    /**
     * @return the most concrete type (least upper bound) of the elements' types
     */
    public default Type getElementType() {
        return getType().getElementType();
    }
    
    /**
     * @return true iff this container has no elements
     */
    public boolean isEmpty();
    
    /**
     * @return an empty collection
     */
    public default T empty() {
        return writer().done();
    }
    
    /**
     * @return an empty writer for this collection kind
     */
    public IWriter<T> writer();

    /**
     * @return the arity of the collection (the number of elements in it)
     */
    public int size();
    
    /**
     * @return this collection with an IRelation interface
     * @throws IllegalOperationException when the container does not contain all tuples of the same arity
     */
    public IRelation<T> asRelation();
    
    /**
     * @return true iff the collection contains only tuples of a fixed width, if any.
     */
    public default boolean isRelation() {
        return getElementType().isFixedWidth();
    }
    
    /**
     * Computes the Cartesian product of two collections
     * @param that is another collection
     * @return a collection representing the Cartesian product of the receiver and the given collection
     */
    public default T product(T that) {
        IWriter<T> w = writer();

        if (that.isEmpty()) {
            return that;
        }
        
        for (IValue t1 : this) {
            for (IValue t2 : that) {
                w.appendTuple(t1, t2);
            }
        }

        return w.done();
    }
    
    /**
     * Computes the union of two collections, ignoring duplicates. The original duplicates in the receiver and the given container will dissappear as well.
     * 
     * @param that is the other collection 
     * @return a collection containing both the elements of the receiver and those of the given container, without introducing  duplicates.
     */
    public default T union(T that) {
        IWriter<T> w = writer().unique();
        w.appendAll(this);
        w.appendAll(that);
        return w.done();
    }
}
