/*******************************************************************************
 * Copyright (c) 2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package org.eclipse.imp.pdb.facts.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractSpecialisedImmutableMap<K, V> implements ImmutableMap<K,V>  {
	@SuppressWarnings("rawtypes")
	private static ImmutableMap EMPTY_MAP = new Map0();

	@SuppressWarnings("unchecked")
	public static <K, V> ImmutableMap<K, V> mapOf() {
		return EMPTY_MAP;
	}
	
	public static <K, V> ImmutableMap<K, V> mapOf(K key1, V val1) {
		return new Map1AndEntry<K, V>(key1, val1);
	}

	public static <K, V> ImmutableMap<K, V> mapOf(K key1, V val1, K key2, V val2) {
		return new Map2<K, V>(key1, val1, key2, val2);
	}

	public static <K, V> ImmutableMap<K, V> mapOf(K key1, V val1, K key2, V val2, K key3, V val3) {
		return new Map3<K, V>(key1, val1, key2, val2, key3, val3);
	}
	
	public static <K, V> ImmutableMap<K, V> mapOf(K key1, V val1, K key2, V val2, K key3, V val3, K key4, V val4) {
		return new Map4<K, V>(key1, val1, key2, val2, key3, val3, key4, val4);
	}

	public static <K, V> ImmutableMap<K, V> mapOf(K key1, V val1, K key2, V val2, K key3, V val3, K key4, V val4, K key5, V val5) {
		return new Map5<K, V>(key1, val1, key2, val2, key3, val3, key4, val4, key5, val5);
	}
	
	public static <K, V> ImmutableMap<K, V> mapOf(K key1, V val1, K key2, V val2, K key3, V val3, K key4, V val4, K key5, V val5, K key6, V val6) {
		return new CopyOnWriteImmutableMap<K, V>(key1, val1, key2, val2, key3, val3, key4, val4, key5, val5, key6, val6);
	}
	
	public static <K, V> ImmutableMap<K, V> mapOf(Map<K, V> map) {
		if (map instanceof AbstractSpecialisedImmutableMap) {
			return (ImmutableMap<K, V>) map;
		} else {
			return flatten(map);
		}
	}
	
	@SafeVarargs
	protected static <K, V> ImmutableMap<K, V> flatten(Map<? extends K, ? extends V>... maps) {
		return CopyOnWriteImmutableMap.flatten(maps);
	}
	
	@Override
	public boolean isEmpty() {
		return size() == 0;
	}
	
	@Override
	public V remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public V put(K key, V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		 throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == this) return true;
		if (other == null) return false;

		if (other instanceof Map) {
			try {
				@SuppressWarnings("unchecked")
				Map<K, V> that = (Map<K, V>) other;

				if (this.size() == that.size()) {
					for (Entry<K, V> e : this.entrySet()) {
						if (!that.containsKey(e.getKey()))
							return false;
						if (!Objects.equals(e.getValue(), that.get(e.getKey())))
							return false;
					}
					return true;
				}
			} catch (ClassCastException unused) {
				return false;
			}
		}

		return false;
	}
}

class Map0<K, V> extends AbstractSpecialisedImmutableMap<K, V> {
	Map0() {
	}

	@Override
	public boolean containsValue(Object val) {
		return false;
	}

	@Override
	public boolean containsKey(Object key) {
		return false;
	}

	@Override
	public V get(Object key) {
		return null;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return Collections.emptySet();
	}

	@Override
	public Set<K> keySet() {
		return Collections.emptySet();
	}

	@Override
	public Collection<V> values() {
		return Collections.emptySet();
	}
	
	@Override
	public ImmutableMap<K, V> __put(K key, V val) {
		return new Map1AndEntry<K, V>(key, val);
	}

	@Override
	public ImmutableMap<K, V> __remove(K key) {
		return this;
	}

	@Override
	public ImmutableMap<K, V> __putAll(Map<? extends K, ? extends V> map) {
		return flatten(this, map);
	}
		
	@Override
	public String toString() {
		return "{}";
	}

}

class Map1AndEntry<K, V> extends AbstractSpecialisedImmutableMap<K, V> implements Map.Entry<K, V>, Cloneable {
	private final K key1;
	private final V val1;

	Map1AndEntry(K key1, V val1) {
		this.key1 = key1;
		this.val1 = val1;
	}

	@Override
	public boolean containsValue(Object val) {
		if (val.equals(val1))
			return true;
		else
			return false;
	}

	@Override
	public boolean containsKey(Object key) {
		if (key.equals(key1))
			return true;
		else
			return false;
	}

	@Override
	public V get(Object key) {
		if (key.equals(key1))
			return val1;
		else
			return null;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public K getKey() {
		return key1;
	}

	@Override
	public V getValue() {
		return val1;
	}

	@Override
	public V setValue(V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return Collections.singleton((Entry<K, V>) this);
	}

	@Override
	public Set<K> keySet() {
		return Collections.singleton(key1);
	}

	@Override
	public Collection<V> values() {
		return Collections.singleton(val1);
	}
	
	@Override
	public ImmutableMap<K, V> __put(K key, V val) {
		if (key.equals(key1))
			return mapOf(key, val);
		else
			return mapOf(key1, val1, key, val);
	}

	@Override
	public ImmutableMap<K, V> __remove(K key) {
		if (key.equals(key1))
			return mapOf();
		else
			return this;
	}
	
	@Override
	public ImmutableMap<K, V> __putAll(Map<? extends K, ? extends V> map) {
		return flatten(this, map);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	@Override
	public int hashCode() {
		return (Objects.hashCode(key1) ^ Objects.hashCode(val1));
	}

	/*
	 * TODO: String representation is incorrect if seen as {@link Map.Entry}.
	 * Always prints string as set view.
	 */
	@Override
	public String toString() {
		return String.format("{%s=%s}", key1, val1);
	}
}

class Map2<K, V> extends AbstractSpecialisedImmutableMap<K, V> implements Cloneable {
	private final K key1;
	private final V val1;
	private final K key2;
	private final V val2;

	Map2(K key1, V val1, K key2, V val2) {
		if (key1.equals(key2))
			throw new IllegalArgumentException(
					"Duplicate keys are not allowed in specialised map."); 
		
		this.key1 = key1;
		this.val1 = val1;
		this.key2 = key2;
		this.val2 = val2;
	}

	@Override
	public boolean containsValue(Object val) {
		if (val.equals(val1))
			return true;
		else if (val.equals(val2))
			return true;
		else
			return false;
	}

	@Override
	public boolean containsKey(Object key) {
		if (key.equals(key1))
			return true;
		else if (key.equals(key2))
			return true;
		else
			return false;
	}

	@Override
	public V get(Object key) {
		if (key.equals(key1))
			return val1;
		else if (key.equals(key2))
			return val2;
		else
			return null;
	}

	@Override
	public int size() {
		return 2;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new HashSet<Entry<K, V>>(
				Arrays.asList(
						new Map1AndEntry<>(key1, val1),
						new Map1AndEntry<>(key2, val2)));
	}

	@Override
	public Set<K> keySet() {
		return new HashSet<K>(
				Arrays.asList(key1, key2));
	}

	@Override
	public Collection<V> values() {
		return new HashSet<V>(
				Arrays.asList(val1, val2));
	}
	
	@Override
	public ImmutableMap<K, V> __put(K key, V val) {
		if (key.equals(key1))
			return mapOf(key, val, key2, val2);
		else if (key.equals(key2))
			return mapOf(key1, val1, key, val);
		else
			return mapOf(key1, val1, key2, val2, key, val);
	}

	@Override
	public ImmutableMap<K, V> __remove(K key) {
		if (key.equals(key1))
			return mapOf(key2, val2);
		else if (key.equals(key2))
			return mapOf(key1, val1);
		else
			return this;
	}
	
	@Override
	public ImmutableMap<K, V> __putAll(Map<? extends K, ? extends V> map) {
		return flatten(this, map);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	@Override
	public int hashCode() {
		return (Objects.hashCode(key1) ^ Objects.hashCode(val1))
				+ (Objects.hashCode(key2) ^ Objects.hashCode(val2));
	}	
	
	@Override
	public String toString() {
		return String.format("{%s=%s, %s=%s}", key1, val1, key2, val2);
	}
}

class Map3<K, V> extends AbstractSpecialisedImmutableMap<K, V> implements Cloneable {
	private final K key1;
	private final V val1;
	private final K key2;
	private final V val2;
	private final K key3;
	private final V val3;

	Map3(K key1, V val1, K key2, V val2, K key3, V val3) {
		if (key1.equals(key2) 
				|| key1.equals(key3) 
				|| key2.equals(key3))
			throw new IllegalArgumentException(
					"Duplicate keys are not allowed in specialised map."); 
		
		this.key1 = key1;
		this.val1 = val1;
		this.key2 = key2;
		this.val2 = val2;
		this.key3 = key3;
		this.val3 = val3;
	}

	@Override
	public boolean containsValue(Object val) {
		if (val.equals(val1))
			return true;
		else if (val.equals(val2))
			return true;
		else if (val.equals(val3))
			return true;
		else
			return false;
	}

	@Override
	public boolean containsKey(Object key) {
		if (key.equals(key1))
			return true;
		else if (key.equals(key2))
			return true;
		else if (key.equals(key3))
			return true;
		else
			return false;
	}

	@Override
	public V get(Object key) {
		if (key.equals(key1))
			return val1;
		else if (key.equals(key2))
			return val2;
		else if (key.equals(key3))
			return val3;
		else
			return null;
	}

	@Override
	public int size() {
		return 3;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new HashSet<Entry<K, V>>(
				Arrays.asList(
						new Map1AndEntry<>(key1, val1),
						new Map1AndEntry<>(key2, val2),
						new Map1AndEntry<>(key3, val3)));
	}

	@Override
	public Set<K> keySet() {
		return new HashSet<K>(
				Arrays.asList(key1, key2, key3));
	}

	@Override
	public Collection<V> values() {
		return new HashSet<V>(
				Arrays.asList(val1, val2, val3));
	}
	
	@Override
	public ImmutableMap<K, V> __put(K key, V val) {
		if (key.equals(key1))
			return mapOf(key, val, key2, val2, key3, val3);
		else if (key.equals(key2))
			return mapOf(key1, val1, key, val, key3, val3);
		else if (key.equals(key3))
			return mapOf(key1, val1, key2, val2, key, val);
		else {
			return mapOf(key1, val1, key2, val2, key3, val3, key, val);
		}
	}

	@Override
	public ImmutableMap<K, V> __remove(K key) {
		if (key.equals(key1))
			return mapOf(key2, val2, key3, val3);
		else if (key.equals(key2))
			return mapOf(key1, val1, key3, val3);
		else if (key.equals(key3))
			return mapOf(key1, val1, key2, val2);
		else
			return this;
	}

	@Override
	public ImmutableMap<K, V> __putAll(Map<? extends K, ? extends V> map) {
		return flatten(this, map);
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	@Override
	public int hashCode() {
		return (Objects.hashCode(key1) ^ Objects.hashCode(val1))
				+ (Objects.hashCode(key2) ^ Objects.hashCode(val2))
				+ (Objects.hashCode(key3) ^ Objects.hashCode(val3));
	}		
	
	@Override
	public String toString() {
		return String.format("{%s=%s, %s=%s, %s=%s}", key1, val1, key2, val2, key3, val3);
	}
}

class Map4<K, V> extends AbstractSpecialisedImmutableMap<K, V> implements Cloneable {
	private final K key1;
	private final V val1;
	private final K key2;
	private final V val2;
	private final K key3;
	private final V val3;
	private final K key4;
	private final V val4;
	
	Map4(K key1, V val1, K key2, V val2, K key3, V val3, K key4, V val4) {
		if (key1.equals(key2) 
				|| key1.equals(key3) 
				|| key1.equals(key4)
				|| key2.equals(key3) 
				|| key2.equals(key4) 
				|| key3.equals(key4))
			throw new IllegalArgumentException(
					"Duplicate keys are not allowed in specialised map."); 
		
		this.key1 = key1;
		this.val1 = val1;
		this.key2 = key2;
		this.val2 = val2;
		this.key3 = key3;
		this.val3 = val3;
		this.key4 = key4;
		this.val4 = val4;
	}

	@Override
	public boolean containsValue(Object val) {
		if (val.equals(val1))
			return true;
		else if (val.equals(val2))
			return true;
		else if (val.equals(val3))
			return true;
		else if (val.equals(val4))
			return true;
		else
			return false;
	}

	@Override
	public boolean containsKey(Object key) {
		if (key.equals(key1))
			return true;
		else if (key.equals(key2))
			return true;
		else if (key.equals(key3))
			return true;
		else if (key.equals(key4))
			return true;
		else
			return false;
	}

	@Override
	public V get(Object key) {
		if (key.equals(key1))
			return val1;
		else if (key.equals(key2))
			return val2;
		else if (key.equals(key3))
			return val3;
		else if (key.equals(key4))
			return val4;
		else
			return null;
	}

	@Override
	public int size() {
		return 4;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new HashSet<Entry<K, V>>(
				Arrays.asList(
						new Map1AndEntry<>(key1, val1),
						new Map1AndEntry<>(key2, val2),
						new Map1AndEntry<>(key3, val3),
						new Map1AndEntry<>(key4, val4)));
	}

	@Override
	public Set<K> keySet() {
		return new HashSet<K>(
				Arrays.asList(key1, key2, key3, key4));
	}

	@Override
	public Collection<V> values() {
		return new HashSet<V>(
				Arrays.asList(val1, val2, val3, val4));
	}
	
	@Override
	public ImmutableMap<K, V> __put(K key, V val) {
		if (key.equals(key1))
			return mapOf(key, val, key2, val2, key3, val3, key4, val4);
		else if (key.equals(key2))
			return mapOf(key1, val1, key, val, key3, val3, key4, val4);
		else if (key.equals(key3))
			return mapOf(key1, val1, key2, val2, key, val, key4, val4);
		else if (key.equals(key4))
			return mapOf(key1, val1, key2, val2, key3, val3, key, val);
		else {
			return mapOf(key1, val1, key2, val2, key3, val3, key4, val4, key, val);
		}
	}

	@Override
	public ImmutableMap<K, V> __remove(K key) {
		if (key.equals(key1))
			return mapOf(key2, val2, key3, val3, key4, val4);
		else if (key.equals(key2))
			return mapOf(key1, val1, key3, val3, key4, val4);
		else if (key.equals(key3))
			return mapOf(key1, val1, key2, val2, key4, val4);
		else if (key.equals(key4))
			return mapOf(key1, val1, key2, val2, key3, val3);
		else
			return this;
	}

	@Override
	public ImmutableMap<K, V> __putAll(Map<? extends K, ? extends V> map) {
		return flatten(this, map);
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	@Override
	public int hashCode() {
		return (Objects.hashCode(key1) ^ Objects.hashCode(val1))
				+ (Objects.hashCode(key2) ^ Objects.hashCode(val2))
				+ (Objects.hashCode(key3) ^ Objects.hashCode(val3))
				+ (Objects.hashCode(key4) ^ Objects.hashCode(val4));
	}		
	
	@Override
	public String toString() {
		return String.format("{%s=%s, %s=%s, %s=%s, %s=%s}", key1, val1, key2, val2, key3, val3, key4, val4);
	}
}

class Map5<K, V> extends AbstractSpecialisedImmutableMap<K, V> implements Cloneable {
	private final K key1;
	private final V val1;
	private final K key2;
	private final V val2;
	private final K key3;
	private final V val3;
	private final K key4;
	private final V val4;
	private final K key5;
	private final V val5;
	
	Map5(K key1, V val1, K key2, V val2, K key3, V val3, K key4, V val4, K key5, V val5) {
		if (key1.equals(key2) 
				|| key1.equals(key3) 
				|| key1.equals(key4)
				|| key1.equals(key5)
				|| key2.equals(key3) 
				|| key2.equals(key4)
				|| key2.equals(key5)
				|| key3.equals(key4)
				|| key3.equals(key5)
				|| key4.equals(key5))
			throw new IllegalArgumentException(
					"Duplicate keys are not allowed in specialised map."); 
		
		this.key1 = key1;
		this.val1 = val1;
		this.key2 = key2;
		this.val2 = val2;
		this.key3 = key3;
		this.val3 = val3;
		this.key4 = key4;
		this.val4 = val4;
		this.key5 = key5;
		this.val5 = val5;
	}

	@Override
	public boolean containsValue(Object val) {
		if (val.equals(val1))
			return true;
		else if (val.equals(val2))
			return true;
		else if (val.equals(val3))
			return true;
		else if (val.equals(val4))
			return true;
		else if (val.equals(val5))
			return true;
		else
			return false;
	}

	@Override
	public boolean containsKey(Object key) {
		if (key.equals(key1))
			return true;
		else if (key.equals(key2))
			return true;
		else if (key.equals(key3))
			return true;
		else if (key.equals(key4))
			return true;
		else if (key.equals(key5))
			return true;
		else
			return false;
	}

	@Override
	public V get(Object key) {
		if (key.equals(key1))
			return val1;
		else if (key.equals(key2))
			return val2;
		else if (key.equals(key3))
			return val3;
		else if (key.equals(key4))
			return val4;
		else if (key.equals(key5))
			return val5;
		else
			return null;
	}

	@Override
	public int size() {
		return 5;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new HashSet<Entry<K, V>>(
				Arrays.asList(
						new Map1AndEntry<>(key1, val1),
						new Map1AndEntry<>(key2, val2),
						new Map1AndEntry<>(key3, val3),
						new Map1AndEntry<>(key4, val4),
						new Map1AndEntry<>(key5, val5)));
	}

	@Override
	public Set<K> keySet() {
		return new HashSet<K>(
				Arrays.asList(key1, key2, key3, key4, key5));
	}

	@Override
	public Collection<V> values() {
		return new HashSet<V>(
				Arrays.asList(val1, val2, val3, val4, val5));
	}
	
	@Override
	public ImmutableMap<K, V> __put(K key, V val) {
		if (key.equals(key1))
			return mapOf(key, val, key2, val2, key3, val3, key4, val4, key5, val5);
		else if (key.equals(key2))
			return mapOf(key1, val1, key, val, key3, val3, key4, val4, key5, val5);
		else if (key.equals(key3))
			return mapOf(key1, val1, key2, val2, key, val, key4, val4, key5, val5);
		else if (key.equals(key4))
			return mapOf(key1, val1, key2, val2, key3, val3, key, val, key5, val5);
		else if (key.equals(key5))
			return mapOf(key1, val1, key2, val2, key3, val3, key4, val4, key, val);
		else {
			return mapOf(key1, val1, key2, val2, key3, val3, key4, val4, key5, val5, key, val);
		}
	}

	@Override
	public ImmutableMap<K, V> __remove(K key) {
		if (key.equals(key1))
			return mapOf(key2, val2, key3, val3, key4, val4, key5, val5);
		else if (key.equals(key2))
			return mapOf(key1, val1, key3, val3, key4, val4, key5, val5);
		else if (key.equals(key3))
			return mapOf(key1, val1, key2, val2, key4, val4, key5, val5);
		else if (key.equals(key4))
			return mapOf(key1, val1, key2, val2, key3, val3, key5, val5);
		else if (key.equals(key5))
			return mapOf(key1, val1, key2, val2, key3, val3, key4, val4);
		else
			return this;
	}

	@Override
	public ImmutableMap<K, V> __putAll(Map<? extends K, ? extends V> map) {
		return flatten(this, map);
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	@Override
	public int hashCode() {
		return (Objects.hashCode(key1) ^ Objects.hashCode(val1))
				+ (Objects.hashCode(key2) ^ Objects.hashCode(val2))
				+ (Objects.hashCode(key3) ^ Objects.hashCode(val3))
				+ (Objects.hashCode(key4) ^ Objects.hashCode(val4))
				+ (Objects.hashCode(key5) ^ Objects.hashCode(val5));
	}			
	
	@Override
	public String toString() {
		return String.format("{%s=%s, %s=%s, %s=%s, %s=%s, %s=%s}", key1, val1, key2, val2, key3, val3, key4, val4, key5, val5);
	}
}

/**
 * A {@link ImmutableMap} implementation that wraps an arbitrary {@link Map}. On
 * modification, the whole map while be cloned.
 * 
 * @param <K>
 *            the type of keys maintained by this map
 * @param <V>
 *            the type of mapped values
 */
class CopyOnWriteImmutableMap<K, V> implements ImmutableMap<K, V> {
	
	private final Map<K, V> content;
	
	CopyOnWriteImmutableMap(K key1, V val1, K key2, V val2, K key3, V val3, K key4, V val4, K key5, V val5, K key6, V val6) {
		this.content = new HashMap<>(8);
		
		this.content.put(key1, val1);
		this.content.put(key2, val2);
		this.content.put(key3, val3);
		this.content.put(key4, val4);
		this.content.put(key5, val5);
		this.content.put(key6, val6);
	}	
	
	@SafeVarargs
	protected static <K, V> ImmutableMap<K, V> flatten(Map<? extends K, ? extends V>... maps) {
		final Map<K, V> newContent = 
				new HashMap<>();
				
		for (Map<? extends K, ? extends V> map : maps)
			newContent.putAll(map);
		
		return new CopyOnWriteImmutableMap<K, V>(newContent);
	}
		
	CopyOnWriteImmutableMap(Map<K, V> content) {
		this.content = content;
	}

	@Override
	public V remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public V put(K key, V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		 throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return content.size();
	}

	@Override
	public boolean isEmpty() {
		return content.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return content.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return content.containsValue(value);
	}

	@Override
	public V get(Object key) {
		return content.get(key);
	}

	@Override
	public Set<K> keySet() {
		return content.keySet();
	}

	@Override
	public Collection<V> values() {
		return content.values();
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return content.entrySet();
	}

	@Override
	public ImmutableMap<K, V> __put(K key, V value) {
		final Map<K, V> newContent = 
				new HashMap<>(content);
		
		newContent.put(key, value);
		
		return new CopyOnWriteImmutableMap<K, V>(newContent);		
	}

	@Override
	public ImmutableMap<K, V> __remove(K key) {
		final Map<K, V> newContent = 
				new HashMap<>(content);
		
		newContent.remove(key);
		
		return new CopyOnWriteImmutableMap<K, V>(newContent);		
	}
	
	@Override
	public ImmutableMap<K, V> __putAll(Map<? extends K, ? extends V> map) {
		final Map<K, V> newContent = 
				new HashMap<>(content);
		
		newContent.putAll(map);
		
		return new CopyOnWriteImmutableMap<K, V>(newContent);		
	}
	
	@Override
	public int hashCode() {
		return content.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return content.equals(other);
	}

	@Override
	public String toString() {
		return content.toString();
	}
}
