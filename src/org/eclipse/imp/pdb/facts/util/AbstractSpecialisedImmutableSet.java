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

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractSpecialisedImmutableSet<K> extends
		AbstractCollection<K> implements ImmutableSet<K> {

	@SuppressWarnings("rawtypes")
	private static ImmutableSet EMPTY_SET = new Set0();

	@SuppressWarnings("unchecked")
	public static <K> ImmutableSet<K> setOf() {
		return EMPTY_SET;
	}
	
	public static <K> ImmutableSet<K> setOf(K key1) {
		return new Set1<K>(key1);
	}

	public static <K> ImmutableSet<K> setOf(K key1, K key2) {
		return new Set2<K>(key1, key2);
	}

	public static <K> ImmutableSet<K> setOf(K key1, K key2, K key3) {
		return new Set3<K>(key1, key2, key3);
	}
	
	public static <K> ImmutableSet<K> setOf(K key1, K key2, K key3, K key4) {
		return new Set4<K>(key1, key2, key3, key4);
	}

	public static <K> ImmutableSet<K> setOf(K key1, K key2, K key3, K key4, K key5) {
		return new Set5<K>(key1, key2, key3, key4, key5);
	}
	
	public static <K> ImmutableSet<K> setOf(K key1, K key2, K key3, K key4, K key5, K key6) {
		return new CopyOnWriteImmutableSet<K>(key1, key2, key3, key4, key5, key6);
	}
	
	public static <K> ImmutableSet<K> setOf(Set<K> set) {
		if (set instanceof AbstractSpecialisedImmutableSet) {
			return (ImmutableSet<K>) set;
		} else {
			return flatten(set);
		}
	}
	
	@SafeVarargs
	protected static <K> ImmutableSet<K> flatten(Set<? extends K>... sets) {
		return CopyOnWriteImmutableSet.flatten(sets);
	}
	
	@Override
	public boolean add(K k) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends K> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean containsEquivalent(Object o, Comparator<Object> cmp) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not yet implemented.");
	}

	@Override
	public ImmutableSet<K> __insertEquivalent(K e, Comparator<Object> cmp) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not yet implemented.");
	}

	@Override
	public ImmutableSet<K> __insertAllEquivalent(Set<? extends K> set,
			Comparator<Object> cmp) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not yet implemented.");
	}

	@Override
	public ImmutableSet<K> __removeEquivalent(K e, Comparator<Object> cmp) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not yet implemented.");
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == this) return true;
		if (other == null) return false;

		if (other instanceof Set) {
			try {
				@SuppressWarnings("unchecked")
				Set<K> that = (Set<K>) other;

				if (this.size() == that.size()) {
					for (K e : this)
						if (!that.contains(e))
							return false;					
					return true;
				}
			} catch (ClassCastException unused) {
				return false;
			}
		}

		return false;
	}
}

class Set0<K> extends AbstractSpecialisedImmutableSet<K> {
	Set0() {
	}

	@Override
	public boolean contains(Object val) {
		return false;
	}

	@Override
	public int size() {
		return 0;
	}
	
	@Override
	public Iterator<K> iterator() {
		return Collections.emptyIterator();
	}
	
	@Override
	public ImmutableSet<K> __insert(K key) {
		return new Set1<K>(key);
	}

	@Override
	public ImmutableSet<K> __remove(K key) {
		return this;
	}

	@Override
	public ImmutableSet<K> __insertAll(Set<? extends K> set) {
		return flatten(this, set);
	}
		
	@Override
	public String toString() {
		return "{}";
	}
}

class Set1<K> extends AbstractSpecialisedImmutableSet<K> implements Cloneable {
	private final K key1;

	Set1(K key1) {
		this.key1 = key1;
	}

	@Override
	public boolean contains(Object key) {
		if (key.equals(key1))
			return true;
		else
			return false;
	}

	@Override
	public int size() {
		return 1;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<K> iterator() {
		return ArrayIterator.of((K[]) new Object[] { key1 });
	}
	
	@Override
	public ImmutableSet<K> __insert(K key) {
		if (key.equals(key1))
			return setOf(key);
		else
			return setOf(key1, key);
	}

	@Override
	public ImmutableSet<K> __remove(K key) {
		if (key.equals(key1))
			return setOf();
		else
			return this;
	}

	@Override
	public ImmutableSet<K> __insertAll(Set<? extends K> set) {
		return flatten(this, set);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(key1);
	}

	@Override
	public String toString() {
		return String.format("{%s=%s}", key1);
	}
}

class Set2<K> extends AbstractSpecialisedImmutableSet<K> implements Cloneable {
	private final K key1;
	private final K key2;

	Set2(K key1, K key2) {
		if (key1.equals(key2))
			throw new IllegalArgumentException(
					"Duplicate keys are not allowed in specialised set."); 
		
		this.key1 = key1;
		this.key2 = key2;
	}
	
	@Override
	public boolean contains(Object key) {
		if (key.equals(key1))
			return true;
		else if (key.equals(key2))
			return true;
		else
			return false;
	}

	@Override
	public int size() {
		return 2;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<K> iterator() {
		return ArrayIterator.of((K[]) new Object[] { key1, key2 });
	}	
	
	@Override
	public ImmutableSet<K> __insert(K key) {
		if (key.equals(key1))
			return setOf(key, key2);
		else if (key.equals(key2))
			return setOf(key1, key);
		else
			return setOf(key1, key2, key);
	}

	@Override
	public ImmutableSet<K> __remove(K key) {
		if (key.equals(key1))
			return setOf(key2);
		else if (key.equals(key2))
			return setOf(key1);
		else
			return this;
	}
	
	@Override
	public ImmutableSet<K> __insertAll(Set<? extends K> set) {
		return flatten(this, set);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(key1)
				+ Objects.hashCode(key2);
	}	
	
	@Override
	public String toString() {
		return String.format("{%s=%s, %s=%s}", key1, key2);
	}
}

class Set3<K> extends AbstractSpecialisedImmutableSet<K> implements Cloneable {
	private final K key1;
	private final K key2;
	private final K key3;

	Set3(K key1, K key2, K key3) {
		if (key1.equals(key2) 
				|| key1.equals(key3) 
				|| key2.equals(key3))
			throw new IllegalArgumentException(
					"Duplicate keys are not allowed in specialised set."); 
		
		this.key1 = key1;
		this.key2 = key2;
		this.key3 = key3;
	}

	@Override
	public boolean contains(Object key) {
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
	public int size() {
		return 3;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<K> iterator() {
		return ArrayIterator.of((K[]) new Object[] { key1, key2, key3 });
	}	
	
	@Override
	public ImmutableSet<K> __insert(K key) {
		if (key.equals(key1))
			return setOf(key, key2, key3);
		else if (key.equals(key2))
			return setOf(key1, key, key3);
		else if (key.equals(key3))
			return setOf(key1, key2, key);
		else {
			return setOf(key1, key2, key3, key);
		}
	}

	@Override
	public ImmutableSet<K> __remove(K key) {
		if (key.equals(key1))
			return setOf(key2, key3);
		else if (key.equals(key2))
			return setOf(key1, key3);
		else if (key.equals(key3))
			return setOf(key1, key2);
		else
			return this;
	}

	@Override
	public ImmutableSet<K> __insertAll(Set<? extends K> set) {
		return flatten(this, set);
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(key1)
				+ Objects.hashCode(key2)
				+ Objects.hashCode(key3);
	}
	
	@Override
	public String toString() {
		return String.format("{%s=%s, %s=%s, %s=%s}", key1, key2, key3);
	}
}

class Set4<K> extends AbstractSpecialisedImmutableSet<K> implements Cloneable {
	private final K key1;
	private final K key2;
	private final K key3;
	private final K key4;
	
	Set4(K key1, K key2, K key3, K key4) {
		if (key1.equals(key2) 
				|| key1.equals(key3) 
				|| key1.equals(key4)
				|| key2.equals(key3) 
				|| key2.equals(key4) 
				|| key3.equals(key4))
			throw new IllegalArgumentException(
					"Duplicate keys are not allowed in specialised set."); 
		
		this.key1 = key1;
		this.key2 = key2;
		this.key3 = key3;
		this.key4 = key4;
	}

	@Override
	public boolean contains(Object key) {
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
	public int size() {
		return 4;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<K> iterator() {
		return ArrayIterator.of((K[]) new Object[] { key1, key2, key3, key4 });
	}
	
	@Override
	public ImmutableSet<K> __insert(K key) {
		if (key.equals(key1))
			return setOf(key, key2, key3, key4);
		else if (key.equals(key2))
			return setOf(key1, key, key3, key4);
		else if (key.equals(key3))
			return setOf(key1, key2, key, key4);
		else if (key.equals(key4))
			return setOf(key1, key2, key3, key);
		else {
			return setOf(key1, key2, key3, key4, key);
		}
	}

	@Override
	public ImmutableSet<K> __remove(K key) {
		if (key.equals(key1))
			return setOf(key2, key3, key4);
		else if (key.equals(key2))
			return setOf(key1, key3, key4);
		else if (key.equals(key3))
			return setOf(key1, key2, key4);
		else if (key.equals(key4))
			return setOf(key1, key2, key3);
		else
			return this;
	}

	@Override
	public ImmutableSet<K> __insertAll(Set<? extends K> set) {
		return flatten(this, set);
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(key1)
				+ Objects.hashCode(key2)
				+ Objects.hashCode(key3)
				+ Objects.hashCode(key4);
	}
	
	@Override
	public String toString() {
		return String.format("{%s=%s, %s=%s, %s=%s, %s=%s}", key1, key2, key3, key4);
	}
}

class Set5<K> extends AbstractSpecialisedImmutableSet<K> implements Cloneable {
	private final K key1;
	private final K key2;
	private final K key3;
	private final K key4;
	private final K key5;
	
	Set5(K key1, K key2, K key3, K key4, K key5) {
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
					"Duplicate keys are not allowed in specialised set."); 
		
		this.key1 = key1;
		this.key2 = key2;
		this.key3 = key3;
		this.key4 = key4;
		this.key5 = key5;
	}

	@Override
	public boolean contains(Object key) {
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
	public int size() {
		return 5;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<K> iterator() {
		return ArrayIterator.of((K[]) new Object[] { key1, key2, key3, key4, key5 });
	}
	
	@Override
	public ImmutableSet<K> __insert(K key) {
		if (key.equals(key1))
			return setOf(key, key2, key3, key4, key5);
		else if (key.equals(key2))
			return setOf(key1, key, key3, key4, key5);
		else if (key.equals(key3))
			return setOf(key1, key2, key, key4, key5);
		else if (key.equals(key4))
			return setOf(key1, key2, key3, key, key5);
		else if (key.equals(key5))
			return setOf(key1, key2, key3, key4, key);
		else {
			return setOf(key1, key2, key3, key4, key5, key);
		}
	}

	@Override
	public ImmutableSet<K> __remove(K key) {
		if (key.equals(key1))
			return setOf(key2, key3, key4, key5);
		else if (key.equals(key2))
			return setOf(key1, key3, key4, key5);
		else if (key.equals(key3))
			return setOf(key1, key2, key4, key5);
		else if (key.equals(key4))
			return setOf(key1, key2, key3, key5);
		else if (key.equals(key5))
			return setOf(key1, key2, key3, key4);
		else
			return this;
	}
	
	@Override
	public ImmutableSet<K> __insertAll(Set<? extends K> set) {
		return flatten(this, set);
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(key1)
				+ Objects.hashCode(key2)
				+ Objects.hashCode(key3)
				+ Objects.hashCode(key4)
				+ Objects.hashCode(key5);
	}			
	
	@Override
	public String toString() {
		return String.format("{%s=%s, %s=%s, %s=%s, %s=%s, %s=%s}", key1, key2, key3, key4, key5);
	}
}

/**
 * A {@link ImmutableSet} implementation that wraps an arbitrary {@link Set}. On
 * modification, the whole Set while be cloned.
 * 
 * Do not construct a {@link CopyOnWriteImmutableSet) with a mutable that might 
 * be modified outside this container.
 * 
 * @param <K>
 *            the type of keys maintained by this Set
 */
class CopyOnWriteImmutableSet<K> implements ImmutableSet<K> {
	
	private final Set<K> content;
	
	CopyOnWriteImmutableSet(K key1, K key2, K key3, K key4, K key5, K key6) {
		this.content = new HashSet<>(8);
		
		this.content.add(key1);
		this.content.add(key2);
		this.content.add(key3);
		this.content.add(key4);
		this.content.add(key5);
		this.content.add(key6);
	}	
	
	@SafeVarargs
	protected static <K> ImmutableSet<K> flatten(Set<? extends K>... sets) {
		final Set<K> newContent = 
				new HashSet<>();
				
		for (Set<? extends K> set : sets)
			newContent.addAll(set);
		
		return new CopyOnWriteImmutableSet<K>(newContent);
	}
		
	CopyOnWriteImmutableSet(Set<K> content) {
		this.content = content;
	}
	
	@Override
	public boolean add(K k) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends K> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean containsEquivalent(Object o, Comparator<Object> cmp) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not yet implemented.");
	}

	@Override
	public ImmutableSet<K> __insertEquivalent(K e, Comparator<Object> cmp) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not yet implemented.");
	}

	@Override
	public ImmutableSet<K> __insertAllEquivalent(Set<? extends K> set,
			Comparator<Object> cmp) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not yet implemented.");
	}

	@Override
	public ImmutableSet<K> __removeEquivalent(K e, Comparator<Object> cmp) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not yet implemented.");
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
	public Iterator<K> iterator() {
		return Collections.unmodifiableSet(content).iterator();
	}

	@Override
	public boolean contains(Object key) {
		return content.contains(key);
	}

	public boolean containsAll(Collection<?> c) {
		return content.containsAll(c);
	}
	
	public Object[] toArray() {
		return content.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return content.toArray(a);
	}

	@Override
	public ImmutableSet<K> __insert(K keyue) {
		final Set<K> newContent = 
				new HashSet<>(content);
		
		newContent.add(keyue);
		
		return new CopyOnWriteImmutableSet<K>(newContent);		
	}

	@Override
	public ImmutableSet<K> __remove(K key) {
		final Set<K> newContent = 
				new HashSet<>(content);
		
		newContent.remove(key);
		
		return new CopyOnWriteImmutableSet<K>(newContent);		
	}
	
	@Override
	public ImmutableSet<K> __insertAll(Set<? extends K> set) {
		final Set<K> newContent = 
				new HashSet<>(content);
		
		newContent.addAll(set);
		
		return new CopyOnWriteImmutableSet<K>(newContent);		
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
