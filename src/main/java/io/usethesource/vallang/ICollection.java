package io.usethesource.vallang;

import io.usethesource.vallang.type.Type;

public interface ICollection<T extends ICollection<T>> extends IValue, Iterable<IValue> {
    /**
     * @return the most concrete type (least upper bound) of the elements' types
     */
    Type getElementType();
    
    /**
     * @return true iff this container has no elements
     */
    boolean isEmpty();
    
    /**
     * @return an empty collection
     */
    T empty();
    
    /**
     * @return an empty writer for this collection kind
     */
    IWriter<T> writer();

    /**
     * @return the arity of the collection (the number of elements in it)
     */
    int size();
    
    /**
     * @return this collection with an IRelation interface
     * @throws IllegalOperationException when the container does not contain all tuples of the same arity
     */
    IRelation<T> asRelation();
}
