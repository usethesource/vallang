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
}
