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
package io.usethesource.vallang;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.visitors.IValueVisitor;

public interface IMap extends ICollection<IMap> {

    /**
     * Adds a new entry to the map, mapping the key to value. If the
     * key existed before, the old value will be lost.
     * @param key   
     * @param value
     * @return a copy of the map with the new key/value mapping
     */
    public default IMap put(IValue key, IValue value) {
        IMapWriter sw = writer();
        sw.putAll(this);
        sw.put(key, value);
        return sw.done();
    }
    
    /**
     * Remove all values with the given key (due to annotations there may be more
     * than one key to match in the map).
     * 
     * @param key
     * @return a map without entries that are isEqual to the key
     */
    public default IMap removeKey(IValue key) {
        IMapWriter sw = writer();
        
        for (Entry<IValue, IValue> c : (Iterable<Entry<IValue, IValue>>) () -> entryIterator()) {
            if (!c.getKey().isEqual(key)) {
                sw.put(c.getKey(), c.getValue());
            }
        }
        
        return sw.done();
    }

    /**
     * @param key
     * @return the value that is mapped to this key, or null if no such value exists
     */
    @Pure
    public @Nullable IValue get(IValue key);

    /**
     * Determine whether a certain key exists in this map.
     * @param key
     * @return true iff there is a value mapped to this key
     */
    @EnsuresNonNullIf(expression="get(#1)", result=true)
    @SuppressWarnings("contracts.conditional.postcondition.not.satisfied") // that's impossible to prove for the Checker Framework
    public default boolean containsKey(IValue key) {
        for (IValue cursor : this) {
            if (cursor.isEqual(key)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public default boolean isEqual(IValue other) {
        if (other == this) {
            return true;
        }

        if (other instanceof IMap) {
            IMap map2 = (IMap) other;

            if (size() == map2.size()) {

                for (IValue k1 : this) {
                    if (!containsKey(k1)) { 
                        return false;
                    } else {
                        IValue v1 = map2.get(k1);
                        if (v1 == null || !v1.isEqual(get(k1))) { 
                            return false;
                        }
                    }
                }

                return true;
            }
        }

        return false;
    }
    
    public default boolean defaultEquals(@Nullable Object other){
        if (other == this) {
            return true;
        }
        
        if (other == null) {
            return false;
        }

        if (other instanceof IMap) {
            IMap map2 = (IMap) other;

            if (getType() != map2.getType()) {
                return false;
            }

            if (hashCode() != map2.hashCode()) {
                return false;
            }

            if (size() == map2.size()) {

                outer:for (IValue k1 : map2) {
                    
                    // the loop might seem weird but due to the (deprecated)
                    // semantics of node annotations we must check each element
                    // for _deep_ equality. This is a big source of inefficiency
                    // and one of the reasons why the semantics of annotations is
                    // deprecated for "keyword parameters".
                    
                    for (IValue cursor : this) {
                        if (cursor.equals(k1)) {
                            // key was found, now check the value
                            IValue val2 = map2.get(k1);
                            if (val2 != null && !val2.equals(get(k1))) { // call to Object.equals(Object)
                                return false;
                            }
                            
                            continue outer;
                        }
                    }
                    return false;
                }
                return true;
            }
        }

        return false;   
    }
    
    public default int defaultHashCode() {
        int hash = 0;

        Iterator<IValue> keyIterator = iterator();
        while (keyIterator.hasNext()) {
            IValue key = keyIterator.next();
            hash ^= key.hashCode();
        }

        return hash;
    }
    
    @Override
    public default boolean match(IValue other) {
        if (other == this) {
            return true;
        }

        if (other instanceof IMap) {
            IMap map2 = (IMap) other;

            if (size() == map2.size()) {
                next:for (IValue k1 : this) {
                    IValue v1 = get(k1);

                    assert v1 != null : "@AssumeAssertion(nullness)";
                    
                    for (Iterator<IValue> iterator = map2.iterator(); iterator.hasNext();) {
                        IValue k2 = iterator.next();
                        if (k2.match(k1)) {
                            IValue v2 = map2.get(k2);

                            if (v2 == null || !v2.match(v1)) {
                                return false;
                            }
                            else {
                                continue next; // this key is co
                            }
                        }
                    }

                    return false; // no matching key found for k1
                }

            return true;
            }
        }

        return false;
    }
    
    /**
     * Determine whether a certain value exists in this map.
     * @param value 
     * @return true iff there is at least one key that maps to the given value.
     */
    public default boolean containsValue(IValue value) {
        for (IValue elem : (Iterable<IValue>) () -> valueIterator()) {
            if (elem.isEqual(value)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * @return the key type for this map
     */
    public default Type getKeyType() {
        return getType().getKeyType();
    }
    
    /**
     * @return the value type for this map
     */
    public default Type getValueType() {
        return getType().getValueType();
    }
    
    /**
     * Adds all key value pairs of the other map to this map (constructing a new one).
     * The values of the other map overwrite the values of this map.
     * @param other the other map to add to this one
     * @return a new map
     */
    public default IMap join(IMap other) {
        IMapWriter sw = writer();
        sw.putAll(this);
        sw.putAll(other);
        return sw.done();
    }
    
    /**
     * Removes all key-value pairs from this map where a key exists in the other map.
     * @param other
     * @return a new map with less key-value pairs.
     */
    public default IMap remove(IMap other) {
        IMapWriter sw = writer();
        
        for (Entry<IValue,IValue> entry : (Iterable<Entry<IValue, IValue>>) () -> entryIterator()) {
            if (!other.containsKey(entry.getKey())) {
                sw.put(entry.getKey(), entry.getValue());
            }
        }
        
        return sw.done();
    }
    
    /**
     * If the value type of this map is a sub-type of the key type of the other, construct
     * a new map that represents the composition which maps keys of this map to values of
     * the other map.
     * @param other other map to compose with
     * @return a new map that represent the composition of this with the other map
     */
    public default IMap compose(IMap other) {
        IMapWriter w = writer();

        for (Entry<IValue, IValue> e : (Iterable<Entry<IValue,IValue>>) () -> entryIterator()) {
            IValue value = other.get(e.getValue());
            if (value != null) {
                w.put(e.getKey(), value);
            }
        }

        return w.done();
    }
    
    @Override
    public IMapWriter writer();
    
    /**
     * Compute the common map (intersection) between this map and another. Any key-value
     * pair that is not present in the other will not be present in the result. This
     * means that if a key exists in both maps, but with a different value, the key
     * will not be present in the result. 
     * @param other
     * @return a new map containing the common pairs between the two maps.
     */
    public default IMap common(IMap other) {
        IMapWriter sw = writer();

        for (Entry<IValue, IValue> entry : (Iterable<Entry<IValue, IValue>>) () -> entryIterator()) {
            IValue thisKey = entry.getKey();
            IValue thisValue = entry.getValue();
            IValue otherValue = other.get(thisKey);
            
            if (otherValue != null && thisValue.equals(otherValue)) {
                sw.put(thisKey, thisValue);
            }
        }
        
        return sw.done();
    }
    
    /**
	 * Checks if the <code>other</code> map is defined for every key that is
	 * present in the receiver object.
	 * 
	 * @param other
	 * @return true iff all for every key of the receiver there exists an entry
	 *         in the other map.
	 */
    public default boolean isSubMap(IMap other) {
        for (Entry<IValue, IValue> entry : (Iterable<Entry<IValue, IValue>>) () -> entryIterator()) {
            IValue key = entry.getKey();
            
            if (!other.containsKey(key)) {
                return false;
            }
            
            if (!other.get(key).isEqual(entry.getValue())) {
                return false;
            }
        }

        return true;
    }
    
    /**
     * Repeated here for documentation purposes, this iterator
     * returns only the keys of the map, not its values.
     * 
     * @return an iterator over the keys of the map 
     */
    @Override
    public Iterator<IValue> iterator();
    
    /**
     * @return an iterator over the values of the map
     */
    public Iterator<IValue> valueIterator();
 
    /**
     * @return an iterator over the keys-value pairs of the map
     */
    public Iterator<Entry<IValue, IValue>> entryIterator();
    
    @Override
    default <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
        return v.visitMap(this);
    }
    
    @Override
    /**
     * a map should stream key/value tuples
     */
    abstract Stream<IValue> stream();
}
