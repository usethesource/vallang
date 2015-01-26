/*******************************************************************************
 * Copyright (c) 2015 CWI
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

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;

/*
 * (K, V) -> T
 * 
 * Wrapping kev-value pair to tuple
 */
public class ImmutableSetMultimapAsImmutableSetView<K, V, T extends Map.Entry<K, V>> implements
				ImmutableSet<T> {

	final ImmutableSetMultimap<K, V> multimap;

	final BiFunction<K, V, T> tupleOf;

	final BiFunction<T, Integer, Object> tupleElementAt;

	protected ImmutableSetMultimapAsImmutableSetView() {
		multimap = TrieSetMultimap_BleedingEdge.<K, V> of();

		tupleOf = AbstractSpecialisedImmutableMap::entryOf;

		tupleElementAt = (tuple, position) -> {
			switch (position) {
			case 0:
				return tuple.getKey();
			case 1:
				return tuple.getValue();
			default:
				throw new IllegalStateException();
			}
		};
	}

	private ImmutableSetMultimapAsImmutableSetView(ImmutableSetMultimap<K, V> multimap,
					BiFunction<K, V, T> tupleOf, BiFunction<T, Integer, Object> tupleElementAt) {
		this.multimap = multimap;
		this.tupleOf = tupleOf;
		this.tupleElementAt = tupleElementAt;
	}

	@Override
	public int size() {
		return multimap.size();
	}

	@Override
	public boolean isEmpty() {
		return multimap.isEmpty();
	}

	@Override
	public Iterator<T> iterator() {
		return this.keyIterator();
	}

	@Override
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean add(T tuple) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsEquivalent(Object o, Comparator<Object> cmp) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsAllEquivalent(Collection<?> c, Comparator<Object> cmp) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public T get(Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T getEquivalent(Object o, Comparator<Object> cmp) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImmutableSet<T> __insert(T tuple) {
		@SuppressWarnings("unchecked")
		final K key = (K) tupleElementAt.apply(tuple, 0);
		@SuppressWarnings("unchecked")
		final V val = (V) tupleElementAt.apply(tuple, 1);

		final ImmutableSetMultimap<K, V> multimapNew = multimap.__put(key, val);

		return new ImmutableSetMultimapAsImmutableSetView<>(multimapNew, tupleOf, tupleElementAt);
	}

	@Override
	public ImmutableSet<T> __insertEquivalent(T key, Comparator<Object> cmp) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImmutableSet<T> __insertAll(ImmutableSet<? extends T> set) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImmutableSet<T> __insertAllEquivalent(ImmutableSet<? extends T> set,
					Comparator<Object> cmp) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImmutableSet<T> __remove(T tuple) {
		@SuppressWarnings("unchecked")
		final K key = (K) tupleElementAt.apply(tuple, 0);
		@SuppressWarnings("unchecked")
		final V val = (V) tupleElementAt.apply(tuple, 1);

		final ImmutableSetMultimap<K, V> multimapNew = multimap.__remove(key, val);

		return new ImmutableSetMultimapAsImmutableSetView<>(multimapNew, tupleOf, tupleElementAt);
	}

	@Override
	public ImmutableSet<T> __removeEquivalent(T key, Comparator<Object> cmp) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImmutableSet<T> __removeAll(ImmutableSet<? extends T> set) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImmutableSet<T> __removeAllEquivalent(ImmutableSet<? extends T> set,
					Comparator<Object> cmp) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImmutableSet<T> __retainAll(ImmutableSet<? extends T> set) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImmutableSet<T> __retainAllEquivalent(ImmutableSet<? extends T> set,
					Comparator<Object> cmp) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<T> keyIterator() {
		return multimap.tupleIterator(tupleOf);
	}

	@Override
	public boolean isTransientSupported() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public TransientSet<T> asTransient() {
		// TODO Auto-generated method stub
		return null;
	}

}
