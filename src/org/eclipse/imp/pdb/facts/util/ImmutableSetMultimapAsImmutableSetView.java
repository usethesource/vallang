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
import java.util.Set;
import java.util.function.BiFunction;

/*
 * (K, V) -> T
 * 
 * Wrapping kev-value pair to tuple
 */
public class ImmutableSetMultimapAsImmutableSetView<K, V, T> implements ImmutableSet<T> {

	final ImmutableSetMultimap<K, V> multimap;

	final BiFunction<K, V, T> tupleOf;

	final BiFunction<T, Integer, Object> tupleElementAt;

	// protected ImmutableSetMultimapAsImmutableSetView() {
	// multimap = TrieSetMultimap_BleedingEdge.<K, V> of();
	//
	// tupleOf = AbstractSpecialisedImmutableMap::entryOf;
	//
	// tupleElementAt = (tuple, position) -> {
	// switch (position) {
	// case 0:
	// return tuple.getKey();
	// case 1:
	// return tuple.getValue();
	// default:
	// throw new IllegalStateException();
	// }
	// };
	// }

	public ImmutableSetMultimapAsImmutableSetView(ImmutableSetMultimap<K, V> multimap,
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
		return multimap.tupleIterator(tupleOf);
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
	}

	@Override
	public <T> T[] toArray(T[] a) {
		throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
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
		try {
			T tuple = (T) o;

			@SuppressWarnings("unchecked")
			final K key = (K) tupleElementAt.apply(tuple, 0);
			@SuppressWarnings("unchecked")
			final V val = (V) tupleElementAt.apply(tuple, 1);

			return multimap.containsEntry(key, val);
		} catch (ClassCastException | ArrayIndexOutOfBoundsException e) {
			// not a tuple or not at least two elements
			return false;
		}
	}

	@Override
	public boolean containsEquivalent(Object o, Comparator<Object> cmp) {
		try {
			T tuple = (T) o;

			@SuppressWarnings("unchecked")
			final K key = (K) tupleElementAt.apply(tuple, 0);
			@SuppressWarnings("unchecked")
			final V val = (V) tupleElementAt.apply(tuple, 1);

			return multimap.containsEntryEquivalent(key, val, cmp);
		} catch (ClassCastException | ArrayIndexOutOfBoundsException e) {
			// not a tuple or not at least two elements
			return false;
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
	}

	@Override
	public boolean containsAllEquivalent(Collection<?> c, Comparator<Object> cmp) {
		throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
	}

	@Override
	public T get(Object o) {
		throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
	}

	@Override
	public T getEquivalent(Object o, Comparator<Object> cmp) {
		throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
	}

	@Override
	public ImmutableSet<T> __insert(T tuple) {
		@SuppressWarnings("unchecked")
		final K key = (K) tupleElementAt.apply(tuple, 0);
		@SuppressWarnings("unchecked")
		final V val = (V) tupleElementAt.apply(tuple, 1);

		final ImmutableSetMultimap<K, V> multimapNew = multimap.__put(key, val);

		if (multimapNew == multimap) {
			return this;
		} else {
			return new ImmutableSetMultimapAsImmutableSetView<>(multimapNew, tupleOf,
							tupleElementAt);
		}
	}

	@Override
	public ImmutableSet<T> __insertEquivalent(T tuple, Comparator<Object> cmp) {
		@SuppressWarnings("unchecked")
		final K key = (K) tupleElementAt.apply(tuple, 0);
		@SuppressWarnings("unchecked")
		final V val = (V) tupleElementAt.apply(tuple, 1);

		final ImmutableSetMultimap<K, V> multimapNew = multimap.__putEquivalent(key, val, cmp);

		if (multimapNew == multimap) {
			return this;
		} else {
			return new ImmutableSetMultimapAsImmutableSetView<>(multimapNew, tupleOf,
							tupleElementAt);
		}
	}

	@Override
	public ImmutableSet<T> __insertAll(Set<? extends T> set) {
		throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
	}

	@Override
	public ImmutableSet<T> __insertAllEquivalent(Set<? extends T> set, Comparator<Object> cmp) {
		throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
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
	public ImmutableSet<T> __removeEquivalent(T tuple, Comparator<Object> cmp) {
		@SuppressWarnings("unchecked")
		final K key = (K) tupleElementAt.apply(tuple, 0);
		@SuppressWarnings("unchecked")
		final V val = (V) tupleElementAt.apply(tuple, 1);

		final ImmutableSetMultimap<K, V> multimapNew = multimap.__removeEquivalent(key, val, cmp);

		return new ImmutableSetMultimapAsImmutableSetView<>(multimapNew, tupleOf, tupleElementAt);				
	}

	@Override
	public ImmutableSet<T> __removeAll(Set<? extends T> set) {
		throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
	}

	@Override
	public ImmutableSet<T> __removeAllEquivalent(Set<? extends T> set, Comparator<Object> cmp) {
		throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
	}

	@Override
	public ImmutableSet<T> __retainAll(Set<? extends T> set) {
		throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
	}

	@Override
	public ImmutableSet<T> __retainAllEquivalent(TransientSet<? extends T> set,
					Comparator<Object> cmp) {
		throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
	}

	@Override
	public Iterator<T> keyIterator() {
		return multimap.tupleIterator(tupleOf);
	}

	@Override
	public boolean isTransientSupported() {
		return true;
	}

	@Override
	public TransientSet<T> asTransient() {
		return new TransientSetMultimapAsTransientSetView<>(multimap.asTransient(), tupleOf,
						tupleElementAt);
	}

	static final class TransientSetMultimapAsTransientSetView<K, V, T> implements TransientSet<T> {

		final TransientSetMultimap<K, V> multimap;

		final BiFunction<K, V, T> tupleOf;

		final BiFunction<T, Integer, Object> tupleElementAt;

		public TransientSetMultimapAsTransientSetView(TransientSetMultimap<K, V> multimap,
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
			return multimap.tupleIterator(tupleOf);
		}

		@Override
		public Object[] toArray() {
			throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
		}

		@Override
		public <T> T[] toArray(T[] a) {
			throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
		}

		@Override
		public boolean add(T e) {
			throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
		}

		@Override
		public boolean addAll(Collection<? extends T> c) {
			throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
		}

		@Override
		public boolean contains(Object o) {
			try {
				T tuple = (T) o;

				@SuppressWarnings("unchecked")
				final K key = (K) tupleElementAt.apply(tuple, 0);
				@SuppressWarnings("unchecked")
				final V val = (V) tupleElementAt.apply(tuple, 1);

				return multimap.containsEntry(key, val);
			} catch (ClassCastException | ArrayIndexOutOfBoundsException e) {
				// not a tuple or not at least two elements
				return false;
			}
		}

		@Override
		public boolean containsEquivalent(Object o, Comparator<Object> cmp) {
			try {
				T tuple = (T) o;

				@SuppressWarnings("unchecked")
				final K key = (K) tupleElementAt.apply(tuple, 0);
				@SuppressWarnings("unchecked")
				final V val = (V) tupleElementAt.apply(tuple, 1);

				return multimap.containsEntryEquivalent(key, val, cmp);
			} catch (ClassCastException | ArrayIndexOutOfBoundsException e) {
				// not a tuple or not at least two elements
				return false;
			}
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
		}

		@Override
		public boolean containsAllEquivalent(Collection<?> c, Comparator<Object> cmp) {
			throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
		}

		@Override
		public T get(Object o) {
			throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
		}

		@Override
		public T getEquivalent(Object o, Comparator<Object> cmp) {
			throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
		}

		@Override
		public boolean __insert(T tuple) {
			@SuppressWarnings("unchecked")
			final K key = (K) tupleElementAt.apply(tuple, 0);
			@SuppressWarnings("unchecked")
			final V val = (V) tupleElementAt.apply(tuple, 1);

			return multimap.__put(key, val);
		}

		@Override
		public boolean __insertEquivalent(T tuple, Comparator<Object> cmp) {
			@SuppressWarnings("unchecked")
			final K key = (K) tupleElementAt.apply(tuple, 0);
			@SuppressWarnings("unchecked")
			final V val = (V) tupleElementAt.apply(tuple, 1);

			return multimap.__putEquivalent(key, val, cmp);
		}

		@Override
		public boolean __insertAll(Set<? extends T> set) {
			throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
		}

		@Override
		public boolean __insertAllEquivalent(Set<? extends T> set, Comparator<Object> cmp) {
			throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
		}

		@Override
		public boolean __remove(T tuple) {
			@SuppressWarnings("unchecked")
			final K key = (K) tupleElementAt.apply(tuple, 0);
			@SuppressWarnings("unchecked")
			final V val = (V) tupleElementAt.apply(tuple, 1);

			return multimap.__remove(key, val);			
		}

		@Override
		public boolean __removeEquivalent(T tuple, Comparator<Object> cmp) {
			@SuppressWarnings("unchecked")
			final K key = (K) tupleElementAt.apply(tuple, 0);
			@SuppressWarnings("unchecked")
			final V val = (V) tupleElementAt.apply(tuple, 1);

			return multimap.__removeEquivalent(key, val, cmp);			
		}

		@Override
		public boolean __removeAll(Set<? extends T> set) {
			throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
		}

		@Override
		public boolean __removeAllEquivalent(Set<? extends T> set, Comparator<Object> cmp) {
			throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
		}

		@Override
		public boolean __retainAll(Set<? extends T> set) {
			throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
		}

		@Override
		public boolean __retainAllEquivalent(TransientSet<? extends T> set, Comparator<Object> cmp) {
			throw new UnsupportedOperationException("Auto-generated method stub; not implemented yet.");
		}

		@Override
		public Iterator<T> keyIterator() {
			return multimap.tupleIterator(tupleOf);
		}

		@Override
		public ImmutableSet<T> freeze() {
			return new ImmutableSetMultimapAsImmutableSetView<>(multimap.freeze(), tupleOf,
							tupleElementAt);
		}

	}

}
