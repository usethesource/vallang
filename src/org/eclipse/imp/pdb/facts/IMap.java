/*******************************************************************************
* Copyright (c) 2008 CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    jurgen@vinju.org

*******************************************************************************/

package org.eclipse.imp.pdb.facts;

import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.Type;

public interface IMap extends Iterable<IValue>, IValue {
	/**
	 * @return true iff the map is empty
	 */
    public boolean isEmpty();

    /**
     * @return the number of keys that have a mapped value in this map
     */
    public int size();

    /**
     * Adds a new entry to the map, mapping the key to value. If they
     * key existed before, the old value will be lost.
     * @param key   
     * @param value
     * @return
     * @throws FactTypeError when either the key or the value are not subtypes
     *         of the declared key and value types, respectively.
     */
    public IMap put(IValue key, IValue value) throws FactTypeError ;
    
    /**
     * @param key
     * @return the value that is mapped to this key, or null if no such value exists
     */
    public IValue get(IValue key);

    /**
     * Determine whether a certain key exists in this map.
     * @param key
     * @return true iff there is a value mapped to this key
     * @throws FactTypeError when this key type can not be in this map because its
     *         type is not a subtype of the key type of this map
     */
    public boolean containsKey(IValue key) throws FactTypeError ;
    
    /**
     * Determine whether a certain value exists in this map.
     * @param value 
     * @return true iff there is at least one key that maps to the given value.
     * @throws FactTypeError if the value given could not possibly be mapped in because
     *         its type is not a subtype of the value type of this map.
     */
    public boolean containsValue(IValue value) throws FactTypeError ;

    /**
     * @return the key type for this map
     */
    public Type getKeyType();
    
    /**
     * @return the value type for this map
     */
    public Type getValueType();
    
    /**
     * Adds all key value pairs of the other map to this map (constructing a new one).
     * The values of the other map overwrite the values of this map.
     * @param other the other map to add to this one
     * @return a new map
     */
    public IMap join(IMap other);
    
    /**
     * Removes all key-value pairs from this map where a key exists in the other map.
     * @param other
     * @return a new map with less key-value pairs.
     */
    public IMap remove(IMap other);
    
    /**
     * Compute the common map (intersection) between this map and another. Any key-value
     * pair that is not present in the other will not be present in the result. This
     * means that if a key exists in both maps, but with a different value, the key
     * will not be present in the result. 
     * @param other
     * @return a new map containing the common pairs between the two maps.
     */
    public IMap common(IMap other);
    
    /**
     * @param other
     * @return true iff all key values pairs of the receiver exist in the other map.
     */
    public boolean isSubMap(IMap other);
    
    /**
     * @return an iterator over the keys of the map 
     */
    public Iterator<IValue> iterator();
    
    /**
     * @return an iterator over the values of the map
     */
    public Iterator<IValue> valueIterator();
 
    /**
     * @return an iterator over the keys-value pairs of the map
     */
    public Iterator<Entry<IValue, IValue>> entryIterator();
}
