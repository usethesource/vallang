/*******************************************************************************
 * Copyright (c) 2008, 2012 CWI
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
package org.eclipse.imp.pdb.facts;

import java.util.Iterator;
import java.util.Map.Entry;

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
     * Adds a new entry to the map, mapping the key to value. If the
     * key existed before, the old value will be lost.
     * @param key   
     * @param value
     * @return a copy of the map with the new key/value mapping
     */
    public IMap put(IValue key, IValue value);
    
    /**
     * @param key
     * @return the value that is mapped to this key, or null if no such value exists
     */
    public IValue get(IValue key);

    /**
     * Determine whether a certain key exists in this map.
     * @param key
     * @return true iff there is a value mapped to this key
     */
    public boolean containsKey(IValue key);
    
    /**
     * Determine whether a certain value exists in this map.
     * @param value 
     * @return true iff there is at least one key that maps to the given value.
     */
    public boolean containsValue(IValue value);

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
     * If the value type of this map is a sub-type of the key type of the other, construct
     * a new map that represents the composition which maps keys of this map to values of
     * the other map.
     * @param other other map to compose with
     * @return a new map that represent the composition of this with the other map
     */
    public IMap compose(IMap other);
    
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
	 * Checks if the <code>other</code> map is defined for every key that is
	 * present in the receiver object.
	 * 
	 * @param other
	 * @return true iff all for every key of the receiver there exists an entry
	 *         in the other map.
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
