package io.usethesource.vallang;

import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.Type;

public interface IWriter<T extends ICollection<T>> extends Iterable<IValue> {
    /**
     * @return the type of the container after it is done().
     */
    public Type computeType();
    
    /**
     * Insert several elements
     * @param value array of elements to insert
     */
	 public void insert(IValue... value);
	 
	 /**
	  * Append several elements
	  * @param value array of elements to append
	  */
	 public default void append(IValue... value) {
	     insert(value);
	 }
	 
	 /**
	  * Append elements at the end.
	  * 
	  * @param value array of elements to append
	  * @throws FactTypeUseException when done() was called before or when the elements have an incompatible type.
	  */
	 public default void appendAll(Iterable<? extends IValue> collection) {
	     for (IValue v : collection) {
             append(v);
         }
	 }
	    
	 /**
	  * Insert a tuple made of the given fields
	  * @param fields
	  */
	 public void insertTuple(IValue... fields);
	 
	 /**
      * Append a tuple made of the given fields
      * @param fields
      */
	 public default void appendTuple(IValue... fields) {
	     insertTuple(fields);
	 }
	 
	 /**
	  * Inserts all elements of an iterable
	  * @param collection
	  * @throws FactTypeUseException
	  */
	 default void insertAll(Iterable<? extends IValue> collection) {
	     for (IValue v : collection) {
	         insert(v);
	     }
	 }
	 
	 public T done();
}
