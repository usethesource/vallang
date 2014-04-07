/*******************************************************************************
 * Copyright (c) 2013-2014 CWI
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

import static org.eclipse.imp.pdb.facts.util.AbstractSpecialisedImmutableJdkMap.mapOf;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndInsert;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndInsertPair;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndMoveToBackPair;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndMoveToFrontPair;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndRemove;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndRemovePair;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndSet;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/*
 * Used for {@code #printStats()}
 **/
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.List;
//
//import objectexplorer.ObjectGraphMeasurer.Footprint;
//
//import com.google.common.base.Predicate;
//import com.google.common.base.Predicates;

@SuppressWarnings("rawtypes")
public class TrieMap<K, V> extends AbstractImmutableMap<K, V> {

	@SuppressWarnings("unchecked")
	private static final TrieMap EMPTY_INPLACE_INDEX_MAP = new TrieMap(
					CompactNode.EMPTY_INPLACE_INDEX_NODE, 0, 0);

	private final AbstractNode<K, V> rootNode;
	private final int hashCode;
	private final int cachedSize;

	TrieMap(AbstractNode<K, V> rootNode, int hashCode, int cachedSize) {
		this.rootNode = rootNode;
		this.hashCode = hashCode;
		this.cachedSize = cachedSize;
		assert invariant();
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> ImmutableMap<K, V> of() {
		return TrieMap.EMPTY_INPLACE_INDEX_MAP;
	}
	
	@SuppressWarnings("unchecked")
	public static final <K, V> ImmutableMap<K, V> of(Object... keyValuePairs) {
		if (keyValuePairs.length % 2 != 0) {
			throw new IllegalArgumentException(
							"Length of argument list is uneven: no key/value pairs.");
		}
		
		TrieMap<K, V> result = TrieMap.EMPTY_INPLACE_INDEX_MAP;
		
		for (int i = 0; i < keyValuePairs.length; i += 2) {
			final K key = (K) keyValuePairs[i];
			final V val = (V) keyValuePairs[i+1];
			
			result = result.__put(key, val);
		}
		
		return result;
	}	

	@SuppressWarnings("unchecked")
	public static final <K, V> TransientMap<K, V> transientOf() {
		return TrieMap.EMPTY_INPLACE_INDEX_MAP.asTransient();
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> TransientMap<K, V> transientOf(Object... keyValuePairs) {
		if (keyValuePairs.length % 2 != 0) {
			throw new IllegalArgumentException(
							"Length of argument list is uneven: no key/value pairs.");
		}
		
		TransientMap<K, V> result = TrieMap.EMPTY_INPLACE_INDEX_MAP.asTransient();
		
		for (int i = 0; i < keyValuePairs.length; i += 2) {
			final K key = (K) keyValuePairs[i];
			final V val = (V) keyValuePairs[i+1];
			
			result.__put(key, val);
		}
		
		return result;
	}	
	
	@SuppressWarnings("unchecked")
	protected static final <K> Comparator<K> equalityComparator() {
		return EqualityUtils.getDefaultEqualityComparator();
	}

	private boolean invariant() {
		int _hash = 0;
		int _count = 0;

		for (SupplierIterator<K, V> it = keyIterator(); it.hasNext();) {
			final K key = it.next();
			final V val = it.get();

			_hash += key.hashCode() ^ val.hashCode();
			_count += 1;
		}

		return this.hashCode == _hash && this.cachedSize == _count;
	}

	@Override
	public TrieMap<K, V> __put(K k, V v) {
		return __putEquivalent(k, v, equalityComparator());
	}

	@Override
	public TrieMap<K, V> __putEquivalent(K key, V val, Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final Result<K, V, ? extends AbstractNode<K, V>> result = rootNode.updated(null, key,
						keyHash, val, 0, cmp);

		if (result.isModified()) {
			if (result.hasReplacedValue()) {
				final int valHashOld = result.getReplacedValue().hashCode();
				final int valHashNew = val.hashCode();

				return new TrieMap<K, V>(result.getNode(), hashCode
								+ (keyHash ^ valHashNew) - (keyHash ^ valHashOld), cachedSize);
			}

			final int valHash = val.hashCode();
			return new TrieMap<K, V>(result.getNode(), hashCode + (keyHash ^ valHash),
							cachedSize + 1);
		}

		return this;
	}

	@Override
	public ImmutableMap<K, V> __putAll(Map<? extends K, ? extends V> map) {
		return __putAllEquivalent(map, equalityComparator());
	}

	@Override
	public ImmutableMap<K, V> __putAllEquivalent(Map<? extends K, ? extends V> map,
					Comparator<Object> cmp) {
		TransientMap<K, V> tmp = asTransient();
		tmp.__putAllEquivalent(map, cmp);
		return tmp.freeze();
	}

	@Override
	public TrieMap<K, V> __remove(K k) {
		return __removeEquivalent(k, equalityComparator());
	}

	@Override
	public TrieMap<K, V> __removeEquivalent(K key, Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final Result<K, V, ? extends AbstractNode<K, V>> result = rootNode.removed(null, key,
						keyHash, 0, cmp);

		if (result.isModified()) {
			// TODO: carry deleted value in result
			// assert result.hasReplacedValue();
			// final int valHash = result.getReplacedValue().hashCode();

			final int valHash = rootNode.findByKey(key, keyHash, 0, cmp).get().getValue()
							.hashCode();

			return new TrieMap<K, V>(result.getNode(), hashCode - (keyHash ^ valHash),
							cachedSize - 1);
		}

		return this;
	}

	@Override
	public boolean containsKey(Object o) {
		return rootNode.containsKey(o, o.hashCode(), 0, equalityComparator());
	}

	@Override
	public boolean containsKeyEquivalent(Object o, Comparator<Object> cmp) {
		return rootNode.containsKey(o, o.hashCode(), 0, cmp);
	}

	@Override
	public boolean containsValue(Object o) {
		return containsValueEquivalent(o, equalityComparator());
	}

	@Override
	public boolean containsValueEquivalent(Object o, Comparator<Object> cmp) {
		for (Iterator<V> iterator = valueIterator(); iterator.hasNext();) {
			if (cmp.compare(iterator.next(), o) == 0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public V get(Object key) {
		return getEquivalent(key, equalityComparator());
	}

	@Override
	public V getEquivalent(Object key, Comparator<Object> cmp) {
		final Optional<Map.Entry<K, V>> result = rootNode.findByKey(key, key.hashCode(), 0, cmp);

		if (result.isPresent()) {
			return result.get().getValue();
		} else {
			return null;
		}
	}

	@Override
	public int size() {
		return cachedSize;
	}

	@Override
	public SupplierIterator<K, V> keyIterator() {
		return new TrieMapIteratorWithFixedWidthStack<>(rootNode);
	}

	@Override
	public Iterator<V> valueIterator() {
		return new TrieMapValueIterator<>(new TrieMapIteratorWithFixedWidthStack<>(rootNode));
	}

	@Override
	public Iterator<Map.Entry<K, V>> entryIterator() {
		return new TrieMapEntryIterator<>(new TrieMapIteratorWithFixedWidthStack<>(rootNode));
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		Set<java.util.Map.Entry<K, V>> entrySet = null;

		if (entrySet == null) {
			entrySet = new AbstractSet<java.util.Map.Entry<K, V>>() {
				@Override
				public Iterator<java.util.Map.Entry<K, V>> iterator() {
					return new Iterator<Entry<K, V>>() {
						private final Iterator<Entry<K, V>> i = entryIterator();

						@Override
						public boolean hasNext() {
							return i.hasNext();
						}

						@Override
						public Entry<K, V> next() {
							return i.next();
						}

						@Override
						public void remove() {
							i.remove();
						}
					};
				}

				@Override
				public int size() {
					return TrieMap.this.size();
				}

				@Override
				public boolean isEmpty() {
					return TrieMap.this.isEmpty();
				}

				@Override
				public void clear() {
					TrieMap.this.clear();
				}

				@Override
				public boolean contains(Object k) {
					return TrieMap.this.containsKey(k);
				}
			};
		}
		return entrySet;
	}

	private static class TrieMapIteratorWithFixedWidthStack<K, V> implements SupplierIterator<K, V> {
		int valueIndex;
		int valueLength;
		AbstractNode<K, V> valueNode;

		V lastValue = null;

		int stackLevel;

		int[] indexAndLength = new int[7 * 2];

		@SuppressWarnings("unchecked")
		AbstractNode<K, V>[] nodes = new AbstractNode[7];

		TrieMapIteratorWithFixedWidthStack(AbstractNode<K, V> rootNode) {
			stackLevel = 0;

			valueNode = rootNode;

			valueIndex = 0;
			valueLength = rootNode.valueArity();

			nodes[0] = rootNode;
			indexAndLength[0] = 0;
			indexAndLength[1] = rootNode.nodeArity();
		}

		@Override
		public boolean hasNext() {
			if (valueIndex < valueLength) {
				return true;
			}

			while (true) {
				final int nodeIndex = indexAndLength[2 * stackLevel];
				final int nodeLength = indexAndLength[2 * stackLevel + 1];

				if (nodeIndex < nodeLength) {
					final AbstractNode<K, V> nextNode = nodes[stackLevel].getNode(nodeIndex);
					indexAndLength[2 * stackLevel] = (nodeIndex + 1);

					final int nextNodeValueArity = nextNode.valueArity();
					final int nextNodeNodeArity = nextNode.nodeArity();

					if (nextNodeNodeArity != 0) {
						stackLevel++;

						nodes[stackLevel] = nextNode;
						indexAndLength[2 * stackLevel] = 0;
						indexAndLength[2 * stackLevel + 1] = nextNode.nodeArity();
					}

					if (nextNodeValueArity != 0) {
						valueNode = nextNode;
						valueIndex = 0;
						valueLength = nextNodeValueArity;
						return true;
					}
				} else {
					if (stackLevel == 0) {
						return false;
					}

					stackLevel--;
				}
			}
		}

		@Override
		public K next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			} else {
				K lastKey = valueNode.getKey(valueIndex);
				lastValue = valueNode.getValue(valueIndex);

				valueIndex += 1;

				return lastKey;
			}
		}

		@Override
		public V get() {
			if (lastValue == null) {
				throw new NoSuchElementException();
			} else {
				V tmp = lastValue;
				lastValue = null;

				return tmp;
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/*
	 * TODO/NOTE: This iterator is also reused by the TransientTrieMap.
	 */
	private static class TrieMapEntryIterator<K, V> implements Iterator<Map.Entry<K, V>> {
		private final SupplierIterator<K, V> iterator;

		TrieMapEntryIterator(SupplierIterator<K, V> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@SuppressWarnings("unchecked")
		@Override
		public Map.Entry<K, V> next() {
			final K key = iterator.next();
			final V val = iterator.get();
			return (java.util.Map.Entry<K, V>) mapOf(key, val);
		}

		@Override
		public void remove() {
			iterator.remove();
		}
	}

	/*
	 * TODO/NOTE: This iterator is also reused by the TransientTrieMap.
	 */
	private static class TrieMapValueIterator<K, V> implements Iterator<V> {
		private final SupplierIterator<K, V> iterator;

		TrieMapValueIterator(SupplierIterator<K, V> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public V next() {
			iterator.next();
			return iterator.get();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public boolean isTransientSupported() {
		return true;
	}

	@Override
	public TransientMap<K, V> asTransient() {
		return new TransientTrieMap<K, V>(this);
	}

	static final class TransientTrieMap<K, V> extends AbstractMap<K, V> implements TransientMap<K, V> {
		final private AtomicReference<Thread> mutator;
		private AbstractNode<K, V> rootNode;
		private int hashCode;
		private int cachedSize;

		TransientTrieMap(TrieMap<K, V> trieMap) {
			this.mutator = new AtomicReference<Thread>(Thread.currentThread());
			this.rootNode = trieMap.rootNode;
			this.hashCode = trieMap.hashCode;
			this.cachedSize = trieMap.cachedSize;
			assert invariant();
		}

		// TODO: merge with TrieMap invariant (as function)
		private boolean invariant() {
			int _hash = 0;

			for (SupplierIterator<K, V> it = keyIterator(); it.hasNext();) {
				final K key = it.next();
				final V val = it.get();

				_hash += key.hashCode() ^ val.hashCode();
			}

			return this.hashCode == _hash;
		}

		@Override
		public boolean containsKey(Object o) {
			return rootNode.containsKey(o, o.hashCode(), 0, equalityComparator());
		}

		@Override
		public boolean containsKeyEquivalent(Object o, Comparator<Object> cmp) {
			return rootNode.containsKey(o, o.hashCode(), 0, cmp);
		}

		@Override
		public V get(Object key) {
			return getEquivalent(key, equalityComparator());
		}

		@Override
		public V getEquivalent(Object key, Comparator<Object> cmp) {
			final Optional<Map.Entry<K, V>> result = rootNode
							.findByKey(key, key.hashCode(), 0, cmp);

			if (result.isPresent()) {
				return result.get().getValue();
			} else {
				return null;
			}
		}

		@Override
		public V __put(K k, V v) {
			return __putEquivalent(k, v, equalityComparator());
		}

		@Override
		public V __putEquivalent(K key, V val, Comparator<Object> cmp) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final Result<K, V, ? extends AbstractNode<K, V>> result = rootNode.updated(mutator,
							key, keyHash, val, 0, cmp);

			if (result.isModified()) {
				rootNode = result.getNode();

				if (result.hasReplacedValue()) {
					final V old = result.getReplacedValue();

					final int valHashOld = old.hashCode();
					final int valHashNew = val.hashCode();

					hashCode += keyHash ^ valHashNew;
					hashCode -= keyHash ^ valHashOld;
					// cachedSize remains same

					assert invariant();
					return old;
				} else {
					final int valHashNew = val.hashCode();

					hashCode += keyHash ^ valHashNew;
					cachedSize += 1;

					assert invariant();
					return null;
				}
			}

			assert invariant();
			return null;
		}

		@Override
		public boolean __remove(K k) {
			return __removeEquivalent(k, equalityComparator());
		}

		@Override
		public boolean __removeEquivalent(K key, Comparator<Object> cmp) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final Result<K, V, ? extends AbstractNode<K, V>> result = rootNode.removed(mutator,
							key, keyHash, 0, cmp);

			if (result.isModified()) {
				// TODO: carry deleted value in result
				// assert result.hasReplacedValue();
				// final int valHash = result.getReplacedValue().hashCode();

				final int valHash = rootNode.findByKey(key, keyHash, 0, cmp).get().getValue()
								.hashCode();

				rootNode = result.getNode();
				hashCode -= keyHash ^ valHash;
				cachedSize -= 1;

				assert invariant();
				return true;
			}

			assert invariant();
			return false;
		}

		@Override
		public boolean __putAll(Map<? extends K, ? extends V> map) {
			return __putAllEquivalent(map, equalityComparator());
		}

		@Override
		public boolean __putAllEquivalent(Map<? extends K, ? extends V> map, Comparator<Object> cmp) {
			boolean modified = false;

			for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
				final boolean isPresent = containsKeyEquivalent(entry.getKey(), cmp);
				final V replaced = __putEquivalent(entry.getKey(), entry.getValue(), cmp);

				if (!isPresent || replaced != null) {
					modified = true;
				}
			}

			return modified;
		}

		@Override
		public Set<java.util.Map.Entry<K, V>> entrySet() {
			Set<java.util.Map.Entry<K, V>> entrySet = null;

			if (entrySet == null) {
				entrySet = new AbstractSet<java.util.Map.Entry<K, V>>() {
					@Override
					public Iterator<java.util.Map.Entry<K, V>> iterator() {
						return new Iterator<Entry<K, V>>() {
							private final Iterator<Entry<K, V>> i = entryIterator();

							@Override
							public boolean hasNext() {
								return i.hasNext();
							}

							@Override
							public Entry<K, V> next() {
								return i.next();
							}

							@Override
							public void remove() {
								i.remove();
							}
						};
					}

					@Override
					public int size() {
						return TransientTrieMap.this.size();
					}

					@Override
					public boolean isEmpty() {
						return TransientTrieMap.this.isEmpty();
					}

					@Override
					public void clear() {
						TransientTrieMap.this.clear();
					}

					@Override
					public boolean contains(Object k) {
						return TransientTrieMap.this.containsKey(k);
					}
				};
			}
			return entrySet;
		}		
		
		@Override
		public SupplierIterator<K, V> keyIterator() {
			return new TransientTrieMapIterator<K, V>(this);
		}

		@Override
		public Iterator<V> valueIterator() {
			return new TrieMapValueIterator<>(keyIterator());
		}

		@Override
		public Iterator<Map.Entry<K, V>> entryIterator() {
			return new TrieMapEntryIterator<>(keyIterator());
		}		
		
		/**
		 * Iterator that first iterates over inlined-values and then continues
		 * depth first recursively.
		 */
		// TODO: test
		private static class TransientTrieMapIterator<K, V> extends
						TrieMapIteratorWithFixedWidthStack<K, V> {

			final TransientTrieMap<K, V> transientTrieMap;
			K lastKey;

			TransientTrieMapIterator(TransientTrieMap<K, V> transientTrieMap) {
				super(transientTrieMap.rootNode);
				this.transientTrieMap = transientTrieMap;
			}

			@Override
			public K next() {
				lastKey = super.next();
				return lastKey;
			}

			@Override
			public void remove() {
				transientTrieMap.__remove(lastKey);
			}
		}

		@Override
		public boolean equals(Object o) {
			return rootNode.equals(o);
		}

		@Override
		public int hashCode() {
			return rootNode.hashCode();
		}

		@Override
		public String toString() {
			return rootNode.toString();
		}

		@Override
		public ImmutableMap<K, V> freeze() {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			mutator.set(null);
			return new TrieMap<K, V>(rootNode, hashCode, cachedSize);
		}
	}

	static final class Result<T1, T2, N extends AbstractNode<T1, T2>> {
		private final N result;
		private final T2 replacedValue;
		private final boolean isModified;

		// update: inserted/removed single element, element count changed
		public static <T1, T2, N extends AbstractNode<T1, T2>> Result<T1, T2, N> modified(N node) {
			return new Result<>(node, null, true);
		}

		// update: replaced single mapping, but element count unchanged
		public static <T1, T2, N extends AbstractNode<T1, T2>> Result<T1, T2, N> updated(N node,
						T2 replacedValue) {
			return new Result<>(node, replacedValue, true);
		}

		// update: neither element, nor element count changed
		public static <T1, T2, N extends AbstractNode<T1, T2>> Result<T1, T2, N> unchanged(N node) {
			return new Result<>(node, null, false);
		}

		private Result(N node, T2 replacedValue, boolean isMutated) {
			this.result = node;
			this.replacedValue = replacedValue;
			this.isModified = isMutated;
		}

		public N getNode() {
			return result;
		}

		public boolean isModified() {
			return isModified;
		}

		public boolean hasReplacedValue() {
			return replacedValue != null;
		}

		public T2 getReplacedValue() {
			return replacedValue;
		}
	}

	abstract static class Optional<T> {
		private static final Optional EMPTY = new Optional() {
			@Override
			boolean isPresent() {
				return false;
			}

			@Override
			Object get() {
				return null;
			}
		};

		@SuppressWarnings("unchecked")
		static <T> Optional<T> empty() {
			return EMPTY;
		}

		static <T> Optional<T> of(T value) {
			return new Value<T>(value);
		}

		abstract boolean isPresent();

		abstract T get();

		private static final class Value<T> extends Optional<T> {
			private final T value;

			private Value(T value) {
				this.value = value;
			}

			@Override
			boolean isPresent() {
				return true;
			}

			@Override
			T get() {
				return value;
			}
		}
	}

	private static abstract class AbstractNode<K, V> {

		protected static final int BIT_PARTITION_SIZE = 5;
		protected static final int BIT_PARTITION_MASK = 0x1f;

		abstract boolean containsKey(Object key, int keyHash, int shift,
						Comparator<Object> comparator);

		abstract Optional<Map.Entry<K, V>> findByKey(Object key, int hash, int shift,
						Comparator<Object> cmp);

		abstract Result<K, V, ? extends AbstractNode<K, V>> updated(
						AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift,
						Comparator<Object> cmp);

		abstract Result<K, V, ? extends AbstractNode<K, V>> removed(
						AtomicReference<Thread> mutator, K key, int hash, int shift,
						Comparator<Object> cmp);

		static final boolean isAllowedToEdit(AtomicReference<Thread> x, AtomicReference<Thread> y) {
			return x != null && y != null && (x == y || x.get() == y.get());
		}

		@SuppressWarnings("unchecked")
		static final <K, V> Map.Entry<K, V> entryOf(final K key, final V val) {
			return (java.util.Map.Entry<K, V>) mapOf(key, val);
		}

		abstract K getKey(int index);

		abstract V getValue(int index);

		abstract AbstractNode<K, V> getNode(int index);

		abstract boolean hasNodes();

		abstract Iterator<? extends AbstractNode<K, V>> nodeIterator();

		abstract int nodeArity();

		abstract boolean hasValues();

		abstract SupplierIterator<K, V> valueIterator();

		abstract int valueArity();

		/**
		 * The arity of this trie node (i.e. number of values and nodes stored
		 * on this level).
		 * 
		 * @return sum of nodes and values stored within
		 */
		int arity() {
			return valueArity() + nodeArity();
		}

		int size() {
			final SupplierIterator<K, V> it = new TrieMapIteratorWithFixedWidthStack<>(this);

			int size = 0;
			while (it.hasNext()) {
				size += 1;
				it.next();
			}

			return size;
		}
	}

	private static abstract class CompactNode<K, V> extends AbstractNode<K, V> {
		static final byte SIZE_EMPTY = 0b00;
		static final byte SIZE_ONE = 0b01;
		static final byte SIZE_MORE_THAN_ONE = 0b10;

		@Override
		abstract Result<K, V, ? extends CompactNode<K, V>> updated(AtomicReference<Thread> mutator,
						K key, int keyHash, V val, int shift, Comparator<Object> cmp);

		@Override
		abstract Result<K, V, ? extends CompactNode<K, V>> removed(AtomicReference<Thread> mutator,
						K key, int hash, int shift, Comparator<Object> cmp);

		/**
		 * Abstract predicate over a node's size. Value can be either
		 * {@value #SIZE_EMPTY}, {@value #SIZE_ONE}, or
		 * {@value #SIZE_MORE_THAN_ONE}.
		 * 
		 * @return size predicate
		 */
		abstract byte sizePredicate();

		/**
		 * Returns the first key stored within this node.
		 * 
		 * @return first key
		 */
		abstract K headKey();

		/**
		 * Returns the first value stored within this node.
		 * 
		 * @return first value
		 */
		abstract V headVal();

		boolean nodeInvariant() {
			boolean inv1 = (size() - valueArity() >= 2 * (arity() - valueArity()));
			boolean inv2 = (this.arity() == 0) ? sizePredicate() == SIZE_EMPTY : true;
			boolean inv3 = (this.arity() == 1 && valueArity() == 1) ? sizePredicate() == SIZE_ONE
							: true;
			boolean inv4 = (this.arity() >= 2) ? sizePredicate() == SIZE_MORE_THAN_ONE : true;

			boolean inv5 = (this.nodeArity() >= 0) && (this.valueArity() >= 0)
							&& ((this.valueArity() + this.nodeArity()) == this.arity());

			return inv1 && inv2 && inv3 && inv4 && inv5;
		}

		static final CompactNode EMPTY_INPLACE_INDEX_NODE = new Value0Index0Node();

		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos,
						CompactNode<K, V> node) {
			switch (pos) {
			case 0:
				return new SingletonNodeAtMask0Node<>(node);
			case 1:
				return new SingletonNodeAtMask1Node<>(node);
			case 2:
				return new SingletonNodeAtMask2Node<>(node);
			case 3:
				return new SingletonNodeAtMask3Node<>(node);
			case 4:
				return new SingletonNodeAtMask4Node<>(node);
			case 5:
				return new SingletonNodeAtMask5Node<>(node);
			case 6:
				return new SingletonNodeAtMask6Node<>(node);
			case 7:
				return new SingletonNodeAtMask7Node<>(node);
			case 8:
				return new SingletonNodeAtMask8Node<>(node);
			case 9:
				return new SingletonNodeAtMask9Node<>(node);
			case 10:
				return new SingletonNodeAtMask10Node<>(node);
			case 11:
				return new SingletonNodeAtMask11Node<>(node);
			case 12:
				return new SingletonNodeAtMask12Node<>(node);
			case 13:
				return new SingletonNodeAtMask13Node<>(node);
			case 14:
				return new SingletonNodeAtMask14Node<>(node);
			case 15:
				return new SingletonNodeAtMask15Node<>(node);
			case 16:
				return new SingletonNodeAtMask16Node<>(node);
			case 17:
				return new SingletonNodeAtMask17Node<>(node);
			case 18:
				return new SingletonNodeAtMask18Node<>(node);
			case 19:
				return new SingletonNodeAtMask19Node<>(node);
			case 20:
				return new SingletonNodeAtMask20Node<>(node);
			case 21:
				return new SingletonNodeAtMask21Node<>(node);
			case 22:
				return new SingletonNodeAtMask22Node<>(node);
			case 23:
				return new SingletonNodeAtMask23Node<>(node);
			case 24:
				return new SingletonNodeAtMask24Node<>(node);
			case 25:
				return new SingletonNodeAtMask25Node<>(node);
			case 26:
				return new SingletonNodeAtMask26Node<>(node);
			case 27:
				return new SingletonNodeAtMask27Node<>(node);
			case 28:
				return new SingletonNodeAtMask28Node<>(node);
			case 29:
				return new SingletonNodeAtMask29Node<>(node);
			case 30:
				return new SingletonNodeAtMask30Node<>(node);
			case 31:
				return new SingletonNodeAtMask31Node<>(node);

			default:
				throw new IllegalStateException("Position out of range.");
			}
		}

		@SuppressWarnings("unchecked")
		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator) {
			return EMPTY_INPLACE_INDEX_NODE;
		}

		// manually added
		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte npos1, CompactNode<K, V> node1, byte npos2,
						CompactNode<K, V> node2, byte npos3, CompactNode<K, V> node3, byte npos4,
						CompactNode<K, V> node4) {
			final int bitmap = (1 << pos1) | (1 << npos1) | (1 << npos2) | (1 << npos3)
							| (1 << npos4);
			final int valmap = (1 << pos1);

			return new MixedIndexNode<>(mutator, bitmap, valmap, new Object[] { key1, val1, node1,
							node2, node3, node4 }, (byte) 1);
		}

		// manually added
		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte pos2, K key2, V val2, byte pos3, K key3, V val3,
						byte npos1, CompactNode<K, V> node1, byte npos2, CompactNode<K, V> node2) {
			final int bitmap = (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << npos1)
							| (1 << npos2);
			final int valmap = (1 << pos1) | (1 << pos2) | (1 << pos3);

			return new MixedIndexNode<>(mutator, bitmap, valmap, new Object[] { key1, val1, key2,
							val2, key3, val3, node1, node2 }, (byte) 3);
		}

		// manually added
		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte pos2, K key2, V val2, byte npos1,
						CompactNode<K, V> node1, byte npos2, CompactNode<K, V> node2, byte npos3,
						CompactNode<K, V> node3) {
			final int bitmap = (1 << pos1) | (1 << pos2) | (1 << npos1) | (1 << npos2)
							| (1 << npos3);
			final int valmap = (1 << pos1) | (1 << pos2);

			return new MixedIndexNode<>(mutator, bitmap, valmap, new Object[] { key1, val1, key2,
							val2, node1, node2, node3 }, (byte) 2);
		}

		// manually added
		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte pos2, K key2, V val2, byte pos3, K key3, V val3,
						byte pos4, K key4, V val4, byte npos1, CompactNode<K, V> node1) {
			final int bitmap = (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << pos4) | (1 << npos1);
			final int valmap = (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << pos4);

			return new MixedIndexNode<>(mutator, bitmap, valmap, new Object[] { key1, val1, key2,
							val2, key3, val3, key4, val4, node1 }, (byte) 4);
		}

		// manually added
		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte pos2, K key2, V val2, byte pos3, K key3, V val3,
						byte pos4, K key4, V val4, byte pos5, K key5, V val5) {
			final int valmap = (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << pos4) | (1 << pos5);

			return new MixedIndexNode<>(mutator, valmap, valmap, new Object[] { key1, val1, key2,
							val2, key3, val3, key4, val4, key5, val5 }, (byte) 5);
		}

		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator,
						byte npos1, CompactNode<K, V> node1, byte npos2, CompactNode<K, V> node2) {
			return new Value0Index2Node<>(mutator, npos1, node1, npos2, node2);
		}

		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator,
						byte npos1, CompactNode<K, V> node1, byte npos2, CompactNode<K, V> node2,
						byte npos3, CompactNode<K, V> node3) {
			return new Value0Index3Node<>(mutator, npos1, node1, npos2, node2, npos3, node3);
		}

		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator,
						byte npos1, CompactNode<K, V> node1, byte npos2, CompactNode<K, V> node2,
						byte npos3, CompactNode<K, V> node3, byte npos4, CompactNode<K, V> node4) {
			return new Value0Index4Node<>(mutator, npos1, node1, npos2, node2, npos3, node3, npos4,
							node4);
		}

		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1) {
			return new Value1Index0Node<>(mutator, pos1, key1, val1);
		}

		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte npos1, CompactNode<K, V> node1) {
			return new Value1Index1Node<>(mutator, pos1, key1, val1, npos1, node1);
		}

		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte npos1, CompactNode<K, V> node1, byte npos2,
						CompactNode<K, V> node2) {
			return new Value1Index2Node<>(mutator, pos1, key1, val1, npos1, node1, npos2, node2);
		}

		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte npos1, CompactNode<K, V> node1, byte npos2,
						CompactNode<K, V> node2, byte npos3, CompactNode<K, V> node3) {
			return new Value1Index3Node<>(mutator, pos1, key1, val1, npos1, node1, npos2, node2,
							npos3, node3);
		}

		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte pos2, K key2, V val2) {
			return new Value2Index0Node<>(mutator, pos1, key1, val1, pos2, key2, val2);
		}

		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte pos2, K key2, V val2, byte npos1,
						CompactNode<K, V> node1) {
			return new Value2Index1Node<>(mutator, pos1, key1, val1, pos2, key2, val2, npos1, node1);
		}

		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte pos2, K key2, V val2, byte npos1,
						CompactNode<K, V> node1, byte npos2, CompactNode<K, V> node2) {
			return new Value2Index2Node<>(mutator, pos1, key1, val1, pos2, key2, val2, npos1,
							node1, npos2, node2);
		}

		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte pos2, K key2, V val2, byte pos3, K key3, V val3) {
			return new Value3Index0Node<>(mutator, pos1, key1, val1, pos2, key2, val2, pos3, key3,
							val3);
		}

		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte pos2, K key2, V val2, byte pos3, K key3, V val3,
						byte npos1, CompactNode<K, V> node1) {
			return new Value3Index1Node<>(mutator, pos1, key1, val1, pos2, key2, val2, pos3, key3,
							val3, npos1, node1);
		}

		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte pos2, K key2, V val2, byte pos3, K key3, V val3,
						byte pos4, K key4, V val4) {
			return new Value4Index0Node<>(mutator, pos1, key1, val1, pos2, key2, val2, pos3, key3,
							val3, pos4, key4, val4);
		}

		static final <K, V> CompactNode<K, V> valNodeOf(AtomicReference<Thread> mutator,
						int bitmap, int valmap, Object[] nodes, byte valueArity) {
			return new MixedIndexNode<>(mutator, bitmap, valmap, nodes, valueArity);
		}

		@SuppressWarnings("unchecked")
		static final <K, V> CompactNode<K, V> mergeNodes(K key0, int keyHash0, V val0, K key1,
						int keyHash1, V val1, int shift) {
			assert key0.equals(key1) == false;

			if (keyHash0 == keyHash1) {
				return new HashCollisionNode<>(keyHash0, (K[]) new Object[] { key0, key1 },
								(V[]) new Object[] { val0, val1 });
			}

			final int mask0 = (keyHash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (keyHash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level
				final Object[] nodes = new Object[4];

				if (mask0 < mask1) {
					nodes[0] = key0;
					nodes[1] = val0;
					nodes[2] = key1;
					nodes[3] = val1;

					return valNodeOf(null, (byte) mask0, key0, val0, (byte) mask1, key1, val1);
				} else {
					nodes[0] = key1;
					nodes[1] = val1;
					nodes[2] = key0;
					nodes[3] = val0;

					return valNodeOf(null, (byte) mask1, key1, val1, (byte) mask0, key0, val0);
				}
			} else {
				// values fit on next level
				final CompactNode<K, V> node = mergeNodes(key0, keyHash0, val0, key1, keyHash1,
								val1, shift + BIT_PARTITION_SIZE);

				return valNodeOf(null, (byte) mask0, node);
			}
		}

		static final <K, V> CompactNode<K, V> mergeNodes(CompactNode<K, V> node0, int keyHash0,
						K key1, int keyHash1, V val1, int shift) {
			final int mask0 = (keyHash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (keyHash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level
				final Object[] nodes = new Object[3];

				// store values before node
				nodes[0] = key1;
				nodes[1] = val1;
				nodes[2] = node0;

				return valNodeOf(null, (byte) mask1, key1, val1, (byte) mask0, node0);
			} else {
				// values fit on next level
				final CompactNode<K, V> node = mergeNodes(node0, keyHash0, key1, keyHash1, val1,
								shift + BIT_PARTITION_SIZE);

				return valNodeOf(null, (byte) mask0, node);
			}
		}
	}

	private static final class MixedIndexNode<K, V> extends CompactNode<K, V> {
		private AtomicReference<Thread> mutator;

		private Object[] nodes;
		final private int bitmap;
		final private int valmap;
		final private byte valueArity;

		MixedIndexNode(AtomicReference<Thread> mutator, int bitmap, int valmap, Object[] nodes,
						byte valueArity) {
			assert (2 * Integer.bitCount(valmap) + Integer.bitCount(bitmap ^ valmap) == nodes.length);

			this.mutator = mutator;

			this.nodes = nodes;
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.valueArity = valueArity;

			assert (valueArity == Integer.bitCount(valmap));
			assert (valueArity() >= 2 || nodeArity() >= 1); // =
															// SIZE_MORE_THAN_ONE

			// for (int i = 0; i < 2 * valueArity; i++)
			// assert ((nodes[i] instanceof CompactNode) == false);
			//
			// for (int i = 2 * valueArity; i < nodes.length; i++)
			// assert ((nodes[i] instanceof CompactNode) == true);

			// assert invariant
			assert nodeInvariant();
		}

		final int bitIndex(int bitpos) {
			return 2 * valueArity + Integer.bitCount((bitmap ^ valmap) & (bitpos - 1));
		}

		final int valIndex(int bitpos) {
			return 2 * Integer.bitCount(valmap & (bitpos - 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		boolean containsKey(Object key, int hash, int shift, Comparator<Object> cmp) {
			final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) {
				return cmp.compare(nodes[valIndex(bitpos)], key) == 0;
			}

			if ((bitmap & bitpos) != 0) {
				return ((AbstractNode<K, V>) nodes[bitIndex(bitpos)]).containsKey(key, hash, shift
								+ BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		@SuppressWarnings("unchecked")
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (cmp.compare(nodes[valIndex], key) == 0) {
					final K _key = (K) nodes[valIndex];
					final V _val = (V) nodes[valIndex + 1];

					final Map.Entry<K, V> entry = (java.util.Map.Entry<K, V>) mapOf(_key, _val);
					return Optional.of(entry);
				}

				return Optional.empty();
			}

			if ((bitmap & bitpos) != 0) { // node (not value)
				final AbstractNode<K, V> subNode = ((AbstractNode<K, V>) nodes[bitIndex(bitpos)]);

				return subNode.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Result<K, V, ? extends CompactNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				final Object currentKey = nodes[valIndex];

				if (cmp.compare(currentKey, key) == 0) {

					final Object currentVal = nodes[valIndex + 1];

					if (cmp.compare(currentVal, val) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					final CompactNode<K, V> thisNew;

					if (isAllowedToEdit(this.mutator, mutator)) {
						// no copying if already editable
						this.nodes[valIndex + 1] = val;
						thisNew = this;
					} else {
						final Object[] editableNodes = copyAndSet(this.nodes, valIndex + 1, val);

						thisNew = CompactNode.<K, V> valNodeOf(mutator, bitmap, valmap,
										editableNodes, valueArity);
					}

					return Result.updated(thisNew, (V) currentVal);
				} else {
					final CompactNode<K, V> nodeNew = mergeNodes((K) nodes[valIndex],
									nodes[valIndex].hashCode(), (V) nodes[valIndex + 1], key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					final int offset = 2 * (valueArity - 1);
					final int index = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
									& (bitpos - 1));

					final Object[] editableNodes = copyAndMoveToBackPair(this.nodes, valIndex,
									offset + index, nodeNew);

					final CompactNode<K, V> thisNew = CompactNode.<K, V> valNodeOf(mutator, bitmap
									| bitpos, valmap & ~bitpos, editableNodes,
									(byte) (valueArity - 1));

					return Result.modified(thisNew);
				}
			} else if ((bitmap & bitpos) != 0) { // node (not value)
				final int bitIndex = bitIndex(bitpos);
				final CompactNode<K, V> subNode = (CompactNode<K, V>) nodes[bitIndex];

				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = subNode.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew;

				// modify current node (set replacement node)
				if (isAllowedToEdit(this.mutator, mutator)) {
					// no copying if already editable
					this.nodes[bitIndex] = subNodeResult.getNode();
					thisNew = this;
				} else {
					final Object[] editableNodes = copyAndSet(this.nodes, bitIndex,
									subNodeResult.getNode());

					thisNew = CompactNode.<K, V> valNodeOf(mutator, bitmap, valmap, editableNodes,
									valueArity);
				}

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else {
				// no value
				final Object[] editableNodes = copyAndInsertPair(this.nodes, valIndex(bitpos), key,
								val);

				final CompactNode<K, V> thisNew = CompactNode.<K, V> valNodeOf(mutator, bitmap
								| bitpos, valmap | bitpos, editableNodes, (byte) (valueArity + 1));

				return Result.modified(thisNew);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Result<K, V, ? extends CompactNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> comparator) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (comparator.compare(nodes[valIndex], key) != 0) {
					return Result.unchanged(this);
				}

				if (this.arity() == 5) {
					switch (this.valueArity) { // 0 <= valueArity <= 5
					case 1: {
						final int nmap = ((bitmap & ~bitpos) ^ (valmap & ~bitpos));
						final byte npos1 = recoverMask(nmap, (byte) 1);
						final byte npos2 = recoverMask(nmap, (byte) 2);
						final byte npos3 = recoverMask(nmap, (byte) 3);
						final byte npos4 = recoverMask(nmap, (byte) 4);
						final CompactNode<K, V> node1 = (CompactNode<K, V>) nodes[valueArity + 0];
						final CompactNode<K, V> node2 = (CompactNode<K, V>) nodes[valueArity + 1];
						final CompactNode<K, V> node3 = (CompactNode<K, V>) nodes[valueArity + 2];
						final CompactNode<K, V> node4 = (CompactNode<K, V>) nodes[valueArity + 3];

						return Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2,
										npos3, node3, npos4, node4));
					}
					case 2: {
						final int map = (valmap & ~bitpos);
						final byte pos1 = recoverMask(map, (byte) 1);
						final K key1;
						final V val1;

						final int nmap = ((bitmap & ~bitpos) ^ (valmap & ~bitpos));
						final byte npos1 = recoverMask(nmap, (byte) 1);
						final byte npos2 = recoverMask(nmap, (byte) 2);
						final byte npos3 = recoverMask(nmap, (byte) 3);
						final CompactNode<K, V> node1 = (CompactNode<K, V>) nodes[valueArity + 0];
						final CompactNode<K, V> node2 = (CompactNode<K, V>) nodes[valueArity + 1];
						final CompactNode<K, V> node3 = (CompactNode<K, V>) nodes[valueArity + 2];

						if (mask < pos1) {
							key1 = (K) nodes[2];
							val1 = (V) nodes[3];
						} else {
							key1 = (K) nodes[0];
							val1 = (V) nodes[1];
						}

						return Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1,
										npos2, node2, npos3, node3));
					}
					case 3: {
						final int map = (valmap & ~bitpos);
						final byte pos1 = recoverMask(map, (byte) 1);
						final byte pos2 = recoverMask(map, (byte) 2);
						final K key1;
						final K key2;
						final V val1;
						final V val2;

						final int nmap = ((bitmap & ~bitpos) ^ (valmap & ~bitpos));
						final byte npos1 = recoverMask(nmap, (byte) 1);
						final byte npos2 = recoverMask(nmap, (byte) 2);
						final CompactNode<K, V> node1 = (CompactNode<K, V>) nodes[valueArity + 0];
						final CompactNode<K, V> node2 = (CompactNode<K, V>) nodes[valueArity + 1];

						if (mask < pos1) {
							key1 = (K) nodes[2];
							val1 = (V) nodes[3];
							key2 = (K) nodes[4];
							val2 = (V) nodes[5];
						} else if (mask < pos2) {
							key1 = (K) nodes[0];
							val1 = (V) nodes[1];
							key2 = (K) nodes[4];
							val2 = (V) nodes[5];
						} else {
							key1 = (K) nodes[0];
							val1 = (V) nodes[1];
							key2 = (K) nodes[2];
							val2 = (V) nodes[3];
						}

						return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2,
										val2, npos1, node1, npos2, node2));
					}
					case 4: {
						final int map = (valmap & ~bitpos);
						final byte pos1 = recoverMask(map, (byte) 1);
						final byte pos2 = recoverMask(map, (byte) 2);
						final byte pos3 = recoverMask(map, (byte) 3);
						final K key1;
						final K key2;
						final K key3;
						final V val1;
						final V val2;
						final V val3;

						final int nmap = ((bitmap & ~bitpos) ^ (valmap & ~bitpos));
						final byte npos1 = recoverMask(nmap, (byte) 1);
						final CompactNode<K, V> node1 = (CompactNode<K, V>) nodes[valueArity + 0];

						if (mask < pos1) {
							key1 = (K) nodes[2];
							val1 = (V) nodes[3];
							key2 = (K) nodes[4];
							val2 = (V) nodes[5];
							key3 = (K) nodes[6];
							val3 = (V) nodes[7];
						} else if (mask < pos2) {
							key1 = (K) nodes[0];
							val1 = (V) nodes[1];
							key2 = (K) nodes[4];
							val2 = (V) nodes[5];
							key3 = (K) nodes[6];
							val3 = (V) nodes[7];
						} else if (mask < pos3) {
							key1 = (K) nodes[0];
							val1 = (V) nodes[1];
							key2 = (K) nodes[2];
							val2 = (V) nodes[3];
							key3 = (K) nodes[6];
							val3 = (V) nodes[7];
						} else {
							key1 = (K) nodes[0];
							val1 = (V) nodes[1];
							key2 = (K) nodes[2];
							val2 = (V) nodes[3];
							key3 = (K) nodes[4];
							val3 = (V) nodes[5];
						}

						return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2,
										val2, pos3, key3, val3, npos1, node1));
					}
					case 5: {
						final int map = (valmap & ~bitpos);
						final byte pos1 = recoverMask(map, (byte) 1);
						final byte pos2 = recoverMask(map, (byte) 2);
						final byte pos3 = recoverMask(map, (byte) 3);
						final byte pos4 = recoverMask(map, (byte) 4);
						final K key1;
						final K key2;
						final K key3;
						final K key4;
						final V val1;
						final V val2;
						final V val3;
						final V val4;

						if (mask < pos1) {
							key1 = (K) nodes[2];
							val1 = (V) nodes[3];
							key2 = (K) nodes[4];
							val2 = (V) nodes[5];
							key3 = (K) nodes[6];
							val3 = (V) nodes[7];
							key4 = (K) nodes[8];
							val4 = (V) nodes[9];
						} else if (mask < pos2) {
							key1 = (K) nodes[0];
							val1 = (V) nodes[1];
							key2 = (K) nodes[4];
							val2 = (V) nodes[5];
							key3 = (K) nodes[6];
							val3 = (V) nodes[7];
							key4 = (K) nodes[8];
							val4 = (V) nodes[9];
						} else if (mask < pos3) {
							key1 = (K) nodes[0];
							val1 = (V) nodes[1];
							key2 = (K) nodes[2];
							val2 = (V) nodes[3];
							key3 = (K) nodes[6];
							val3 = (V) nodes[7];
							key4 = (K) nodes[8];
							val4 = (V) nodes[9];
						} else if (mask < pos4) {
							key1 = (K) nodes[0];
							val1 = (V) nodes[1];
							key2 = (K) nodes[2];
							val2 = (V) nodes[3];
							key3 = (K) nodes[4];
							val3 = (V) nodes[5];
							key4 = (K) nodes[8];
							val4 = (V) nodes[9];
						} else {
							key1 = (K) nodes[0];
							val1 = (V) nodes[1];
							key2 = (K) nodes[2];
							val2 = (V) nodes[3];
							key3 = (K) nodes[4];
							val3 = (V) nodes[5];
							key4 = (K) nodes[6];
							val4 = (V) nodes[7];
						}

						return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2,
										val2, pos3, key3, val3, pos4, key4, val4));
					}
					}
				} else {
					final Object[] editableNodes = copyAndRemovePair(this.nodes, valIndex);

					final CompactNode<K, V> thisNew = CompactNode.<K, V> valNodeOf(mutator,
									this.bitmap & ~bitpos, this.valmap & ~bitpos, editableNodes,
									(byte) (valueArity - 1));

					return Result.modified(thisNew);
				}
			} else if ((bitmap & bitpos) != 0) { // node (not value)
				final int bitIndex = bitIndex(bitpos);
				final CompactNode<K, V> subNode = (CompactNode<K, V>) nodes[bitIndex];
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = subNode.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, comparator);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case 0: {
					// remove node
					if (this.arity() == 5) {
						switch (this.nodeArity()) {
						case 1: {
							final int map = valmap;
							final byte pos1 = recoverMask(map, (byte) 1);
							final byte pos2 = recoverMask(map, (byte) 2);
							final byte pos3 = recoverMask(map, (byte) 3);
							final byte pos4 = recoverMask(map, (byte) 4);
							final K key1 = (K) nodes[0];
							final K key2 = (K) nodes[2];
							final K key3 = (K) nodes[4];
							final K key4 = (K) nodes[6];
							final V val1 = (V) nodes[1];
							final V val2 = (V) nodes[3];
							final V val3 = (V) nodes[5];
							final V val4 = (V) nodes[7];

							return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2,
											val2, pos3, key3, val3, pos4, key4, val4));
						}
						case 2: {
							final int map = valmap;
							final byte pos1 = recoverMask(map, (byte) 1);
							final byte pos2 = recoverMask(map, (byte) 2);
							final byte pos3 = recoverMask(map, (byte) 3);
							final K key1 = (K) nodes[0];
							final K key2 = (K) nodes[2];
							final K key3 = (K) nodes[4];
							final V val1 = (V) nodes[1];
							final V val2 = (V) nodes[3];
							final V val3 = (V) nodes[5];

							final int nmap = ((bitmap & ~bitpos) ^ valmap);
							final byte npos1 = recoverMask(nmap, (byte) 1);
							final CompactNode<K, V> node1;

							if (mask < npos1) {
								node1 = (CompactNode<K, V>) nodes[valueArity + 1];
							} else {
								node1 = (CompactNode<K, V>) nodes[valueArity + 0];
							}

							return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2,
											val2, pos3, key3, val3, npos1, node1));
						}
						case 3: {
							final int map = valmap;
							final byte pos1 = recoverMask(map, (byte) 1);
							final byte pos2 = recoverMask(map, (byte) 2);
							final K key1 = (K) nodes[0];
							final K key2 = (K) nodes[2];
							final V val1 = (V) nodes[1];
							final V val2 = (V) nodes[3];

							final int nmap = ((bitmap & ~bitpos) ^ valmap);
							final byte npos1 = recoverMask(nmap, (byte) 1);
							final byte npos2 = recoverMask(nmap, (byte) 2);
							final CompactNode<K, V> node1;
							final CompactNode<K, V> node2;

							if (mask < npos1) {
								node1 = (CompactNode<K, V>) nodes[valueArity + 1];
								node2 = (CompactNode<K, V>) nodes[valueArity + 2];
							} else if (mask < npos2) {
								node1 = (CompactNode<K, V>) nodes[valueArity + 0];
								node2 = (CompactNode<K, V>) nodes[valueArity + 2];
							} else {
								node1 = (CompactNode<K, V>) nodes[valueArity + 0];
								node2 = (CompactNode<K, V>) nodes[valueArity + 1];
							}

							return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2,
											val2, npos1, node1, npos2, node2));
						}
						case 4: {
							final int map = valmap;
							final byte pos1 = recoverMask(map, (byte) 1);
							final K key1 = (K) nodes[0];
							final V val1 = (V) nodes[1];

							final int nmap = ((bitmap & ~bitpos) ^ valmap);
							final byte npos1 = recoverMask(nmap, (byte) 1);
							final byte npos2 = recoverMask(nmap, (byte) 2);
							final byte npos3 = recoverMask(nmap, (byte) 3);
							final CompactNode<K, V> node1;
							final CompactNode<K, V> node2;
							final CompactNode<K, V> node3;

							if (mask < npos1) {
								node1 = (CompactNode<K, V>) nodes[valueArity + 1];
								node2 = (CompactNode<K, V>) nodes[valueArity + 2];
								node3 = (CompactNode<K, V>) nodes[valueArity + 3];
							} else if (mask < npos2) {
								node1 = (CompactNode<K, V>) nodes[valueArity + 0];
								node2 = (CompactNode<K, V>) nodes[valueArity + 2];
								node3 = (CompactNode<K, V>) nodes[valueArity + 3];
							} else if (mask < npos3) {
								node1 = (CompactNode<K, V>) nodes[valueArity + 0];
								node2 = (CompactNode<K, V>) nodes[valueArity + 1];
								node3 = (CompactNode<K, V>) nodes[valueArity + 3];
							} else {
								node1 = (CompactNode<K, V>) nodes[valueArity + 0];
								node2 = (CompactNode<K, V>) nodes[valueArity + 1];
								node3 = (CompactNode<K, V>) nodes[valueArity + 2];
							}

							return Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1,
											node1, npos2, node2, npos3, node3));
						}
						case 5: {
							final int nmap = ((bitmap & ~bitpos) ^ valmap);
							final byte npos1 = recoverMask(nmap, (byte) 1);
							final byte npos2 = recoverMask(nmap, (byte) 2);
							final byte npos3 = recoverMask(nmap, (byte) 3);
							final byte npos4 = recoverMask(nmap, (byte) 4);
							final CompactNode<K, V> node1;
							final CompactNode<K, V> node2;
							final CompactNode<K, V> node3;
							final CompactNode<K, V> node4;

							if (mask < npos1) {
								node1 = (CompactNode<K, V>) nodes[valueArity + 1];
								node2 = (CompactNode<K, V>) nodes[valueArity + 2];
								node3 = (CompactNode<K, V>) nodes[valueArity + 3];
								node4 = (CompactNode<K, V>) nodes[valueArity + 4];
							} else if (mask < npos2) {
								node1 = (CompactNode<K, V>) nodes[valueArity + 0];
								node2 = (CompactNode<K, V>) nodes[valueArity + 2];
								node3 = (CompactNode<K, V>) nodes[valueArity + 3];
								node4 = (CompactNode<K, V>) nodes[valueArity + 4];
							} else if (mask < npos3) {
								node1 = (CompactNode<K, V>) nodes[valueArity + 0];
								node2 = (CompactNode<K, V>) nodes[valueArity + 1];
								node3 = (CompactNode<K, V>) nodes[valueArity + 3];
								node4 = (CompactNode<K, V>) nodes[valueArity + 4];
							} else if (mask < npos4) {
								node1 = (CompactNode<K, V>) nodes[valueArity + 0];
								node2 = (CompactNode<K, V>) nodes[valueArity + 1];
								node3 = (CompactNode<K, V>) nodes[valueArity + 2];
								node4 = (CompactNode<K, V>) nodes[valueArity + 4];
							} else {
								node1 = (CompactNode<K, V>) nodes[valueArity + 0];
								node2 = (CompactNode<K, V>) nodes[valueArity + 1];
								node3 = (CompactNode<K, V>) nodes[valueArity + 2];
								node4 = (CompactNode<K, V>) nodes[valueArity + 3];
							}

							return Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2,
											npos3, node3, npos4, node4));
						}
						}
					} else {
						final Object[] editableNodes = copyAndRemovePair(this.nodes, bitIndex);

						final CompactNode<K, V> thisNew = CompactNode.<K, V> valNodeOf(mutator,
										bitmap & ~bitpos, valmap, editableNodes, valueArity);

						return Result.modified(thisNew);
					}
				}
				case 1: {
					// inline value (move to front)
					final int valIndexNew = Integer.bitCount((valmap | bitpos) & (bitpos - 1));

					final Object[] editableNodes = copyAndMoveToFrontPair(this.nodes, bitIndex,
									valIndexNew, subNodeNew.headKey(), subNodeNew.headVal());

					final CompactNode<K, V> thisNew = CompactNode.<K, V> valNodeOf(mutator, bitmap,
									valmap | bitpos, editableNodes, (byte) (valueArity + 1));

					return Result.modified(thisNew);
				}
				default: {
					// modify current node (set replacement node)
					if (isAllowedToEdit(this.mutator, mutator)) {
						// no copying if already editable
						this.nodes[bitIndex] = subNodeNew;
						return Result.modified(this);
					} else {
						final Object[] editableNodes = copyAndSet(this.nodes, bitIndex, subNodeNew);

						final CompactNode<K, V> thisNew = CompactNode.<K, V> valNodeOf(mutator,
										bitmap, valmap, editableNodes, valueArity);

						return Result.modified(thisNew);
					}
				}
				}
			}

			return Result.unchanged(this);
		}

		// returns 0 <= mask <= 31
		static byte recoverMask(int map, byte i_th) {
			assert 1 <= i_th && i_th <= 32;

			byte cnt1 = 0;
			byte mask = 0;

			while (mask < 32) {
				if ((map & 0x01) == 0x01) {
					cnt1 += 1;

					if (cnt1 == i_th) {
						return mask;
					}
				}

				map = map >> 1;
				mask += 1;
			}

			assert cnt1 != i_th;
			throw new RuntimeException("Called with invalid arguments.");
		}

		@SuppressWarnings("unchecked")
		@Override
		K getKey(int index) {
			return (K) nodes[2 * index];
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(int index) {
			return (V) nodes[2 * index + 1];
		}

		@SuppressWarnings("unchecked")
		@Override
		public AbstractNode<K, V> getNode(int index) {
			final int offset = 2 * valueArity;
			return (AbstractNode<K, V>) nodes[offset + index];
		}

		@Override
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(nodes, 0, 2 * valueArity);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<AbstractNode<K, V>> nodeIterator() {
			final int offset = 2 * valueArity;

			for (int i = offset; i < nodes.length - offset; i++) {
				assert ((nodes[i] instanceof AbstractNode) == true);
			}

			return (Iterator) ArrayIterator.of(nodes, offset, nodes.length - offset);
		}

		@SuppressWarnings("unchecked")
		@Override
		K headKey() {
			assert hasValues();
			return (K) nodes[0];
		}

		@SuppressWarnings("unchecked")
		@Override
		V headVal() {
			assert hasValues();
			return (V) nodes[1];
		}

		@Override
		boolean hasValues() {
			return valueArity != 0;
		}

		@Override
		int valueArity() {
			return valueArity;
		}

		@Override
		boolean hasNodes() {
			return 2 * valueArity != nodes.length;
		}

		@Override
		int nodeArity() {
			return nodes.length - 2 * valueArity;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 0;
			result = prime * result + bitmap;
			result = prime * result + valmap;
			result = prime * result + Arrays.hashCode(nodes);
			return result;
		}

		@Override
		public boolean equals(Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			MixedIndexNode<?, ?> that = (MixedIndexNode<?, ?>) other;
			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}
			if (!Arrays.equals(nodes, that.nodes)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return Arrays.toString(nodes);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}
	}

	// TODO: replace by immutable cons list
	private static final class HashCollisionNode<K, V> extends CompactNode<K, V> {
		private final K[] keys;
		private final V[] vals;
		private final int hash;

		HashCollisionNode(int hash, K[] keys, V[] vals) {
			this.keys = keys;
			this.vals = vals;
			this.hash = hash;

			assert valueArity() >= 2;
		}

		@Override
		SupplierIterator<K, V> valueIterator() {
			// TODO: change representation of keys and values
			assert keys.length == vals.length;

			final Object[] keysAndVals = new Object[keys.length + vals.length];
			for (int i = 0; i < keys.length; i++) {
				keysAndVals[2 * i] = keys[i];
				keysAndVals[2 * i + 1] = vals[i];
			}

			return ArrayKeyValueIterator.of(keysAndVals);
		}

		@Override
		public String toString() {
			final Object[] keysAndVals = new Object[keys.length + vals.length];
			for (int i = 0; i < keys.length; i++) {
				keysAndVals[2 * i] = keys[i];
				keysAndVals[2 * i + 1] = vals[i];
			}
			return Arrays.toString(keysAndVals);
		}

		@Override
		Iterator<CompactNode<K, V>> nodeIterator() {
			return Collections.emptyIterator();
		}

		@Override
		K headKey() {
			assert hasValues();
			return keys[0];
		}

		@Override
		V headVal() {
			assert hasValues();
			return vals[0];
		}

		@Override
		public boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			if (this.hash == keyHash) {
				for (K k : keys) {
					if (cmp.compare(k, key) == 0) {
						return true;
					}
				}
			}
			return false;
		}

		/**
		 * Inserts an object if not yet present. Note, that this implementation
		 * always returns a new immutable {@link TrieMap} instance.
		 */
		@Override
		Result<K, V, ? extends CompactNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			if (this.hash != keyHash) {
				return Result.modified(mergeNodes(this, this.hash, key, keyHash, val, shift));
			}

			for (int i = 0; i < keys.length; i++) {
				if (cmp.compare(keys[i], key) == 0) {
					
					final V currentVal = vals[i];
					
					if (cmp.compare(currentVal, val) == 0) {
						return Result.unchanged(this);
					}
					
					final CompactNode<K, V> thisNew;
					
//					// update mapping
//					if (isAllowedToEdit(this.mutator, mutator)) {
//						// no copying if already editable
//						this.vals[i] = val;
//						thisNew = this;
//					} else {
						@SuppressWarnings("unchecked")
						final V[] editableVals = (V[]) copyAndSet(this.vals, i, val);

						thisNew = new HashCollisionNode<>(this.hash, this.keys, editableVals);
//					}

					return Result.updated(thisNew, currentVal);						
				}
			}
			
			// no value
			@SuppressWarnings("unchecked")
			final K[] keysNew = (K[]) copyAndInsert(keys, keys.length, key);
			@SuppressWarnings("unchecked")
			final V[] valsNew = (V[]) copyAndInsert(vals, vals.length, val);
			return Result.modified(new HashCollisionNode<>(keyHash, keysNew, valsNew));
		}

		/**
		 * Removes an object if present. Note, that this implementation always
		 * returns a new immutable {@link TrieMap} instance.
		 */
		@SuppressWarnings("unchecked")
		@Override
		Result<K, V, ? extends CompactNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			for (int i = 0; i < keys.length; i++) {
				if (cmp.compare(keys[i], key) == 0) {
					if (this.arity() == 1) {
						return Result.modified(CompactNode.<K, V> valNodeOf(mutator));
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node
						 * will be a) either be the new root returned, or b)
						 * unwrapped and inlined.
						 */
						final K theOtherKey = (i == 0) ? keys[1] : keys[0];
						final V theOtherVal = (i == 0) ? vals[1] : vals[0];
						return CompactNode.<K, V> valNodeOf(mutator).updated(mutator, theOtherKey,
										keyHash, theOtherVal, 0, cmp);
					} else {
						return Result.modified(new HashCollisionNode<>(keyHash,
										(K[]) copyAndRemove(keys, i), (V[]) copyAndRemove(vals, i)));
					}
				}
			}
			return Result.unchanged(this);
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return keys.length;
		}

		@Override
		boolean hasNodes() {
			return false;
		}

		@Override
		int nodeArity() {
			return 0;
		}

		@Override
		int arity() {
			return valueArity();
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		K getKey(int index) {
			return keys[index];
		}

		@Override
		V getValue(int index) {
			return vals[index];
		}

		@Override
		public CompactNode<K, V> getNode(int index) {
			throw new IllegalStateException("Is leaf node.");
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 0;
			result = prime * result + hash;
			result = prime * result + Arrays.hashCode(keys);
			return result;
		}

		@Override
		public boolean equals(Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}

			HashCollisionNode<?, ?> that = (HashCollisionNode<?, ?>) other;

			if (hash != that.hash) {
				return false;
			}

			if (arity() != that.arity()) {
				return false;
			}

			/*
			 * Linear scan for each key, because of arbitrary element order.
			 */
			outerLoop: for (SupplierIterator<?, ?> it = that.valueIterator(); it.hasNext();) {
				final Object otherKey = it.next();
				final Object otherVal = it.next();

				for (int i = 0; i < keys.length; i++) {
					final K key = keys[i];
					final V val = vals[i];

					if (key.equals(otherKey) && val.equals(otherVal)) {
						continue outerLoop;
					}
				}
				return false;
			}

			return true;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int hash, int shift, Comparator<Object> cmp) {
			for (int i = 0; i < keys.length; i++) {
				final K _key = keys[i];
				if (cmp.compare(key, _key) == 0) {
					final V _val = vals[i];
					return Optional.of(entryOf(_key, _val));
				}
			}
			return Optional.empty();
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((rootNode == null) ? 0 : rootNode.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (other == null) {
			return false;
		}

		if (other instanceof TrieMap) {
			TrieMap that = (TrieMap) other;

			if (this.size() != that.size()) {
				return false;
			}

			return rootNode.equals(that.rootNode);
		}

		return super.equals(other);
	}

	public void printStats() {
//		final Iterator<AbstractNode<K, V>> it = new TrieMapNodeIterator<>(rootNode);
//
//		final Path file = Paths.get("tree-node-stats.csv");
//		final List<String> lines = new ArrayList<>();
//		lines.add("arity,valueArity,nodeArity,size");
//
//		while (it.hasNext()) {
//			final AbstractNode<K, V> node = it.next();
//
//			Predicate<Object> isRoot = new Predicate<Object>() {
//				@Override
//				public boolean apply(Object arg0) {
//					return arg0 == node;
//				}
//			};
//
//			Predicate<Object> jointPredicate = Predicates.or(isRoot, Predicates.not(Predicates.or(
//							Predicates.instanceOf(AbstractNode.class),
//							Predicates.instanceOf(Integer.class))));
//
//			long memoryInBytes = objectexplorer.MemoryMeasurer.measureBytes(node, jointPredicate);
//			Footprint memoryFootprint = objectexplorer.ObjectGraphMeasurer.measure(node,
//							jointPredicate);
//
//			final int pointers = 2 * node.valueArity() + node.nodeArity();
//
//			final String statString = String
//							.format("arity=%d [values=%d, nodes=%d]\n%d bytes [%1.1f bytes per pointer]\n%s\n",
//											node.arity(), node.valueArity(), node.nodeArity(),
//											memoryInBytes, (float) memoryInBytes / pointers,
//											memoryFootprint);
//
//			System.out.println(statString);
//
//			final String statFileString = String.format("%d,%d,%d,%d", node.arity(),
//							node.valueArity(), node.nodeArity(), memoryInBytes);
//			lines.add(statFileString);
//		}
//
//		Predicate<Object> totalPredicate = Predicates.not(Predicates.instanceOf(Integer.class));
//
//		long totalMemoryInBytes = objectexplorer.MemoryMeasurer.measureBytes(rootNode,
//						totalPredicate);
//		Footprint totalMemoryFootprint = objectexplorer.ObjectGraphMeasurer.measure(rootNode,
//						totalPredicate);
//
//		final String totalStatString = String.format(
//						"size=%d\n%d bytes [%1.1f bytes per key-value-pair; min 8.0 bytes]\n%s\n",
//						cachedSize, totalMemoryInBytes, (float) totalMemoryInBytes / cachedSize,
//						totalMemoryFootprint);
//
//		System.out.println(totalStatString);
//
//		// write stats to file
//		try {
//			Files.write(file, lines, StandardCharsets.UTF_8);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}

	/**
	 * Iterator that first iterates over inlined-values and then continues depth
	 * first recursively.
	 */
	@SuppressWarnings("unused")
	private static class TrieMapNodeIterator<K, V> implements Iterator<AbstractNode<K, V>> {

		final Deque<Iterator<? extends AbstractNode<K, V>>> nodeIteratorStack;

		TrieMapNodeIterator(AbstractNode<K, V> rootNode) {
			nodeIteratorStack = new ArrayDeque<>();
			nodeIteratorStack.push(Collections.singleton(rootNode).iterator());
		}

		@Override
		public boolean hasNext() {
			while (true) {
				if (nodeIteratorStack.isEmpty()) {
					return false;
				} else {
					if (nodeIteratorStack.peek().hasNext()) {
						return true;
					} else {
						nodeIteratorStack.pop();
						continue;
					}
				}
			}
		}

		@Override
		public AbstractNode<K, V> next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			AbstractNode<K, V> innerNode = nodeIteratorStack.peek().next();

			if (innerNode.hasNodes()) {
				nodeIteratorStack.push(innerNode.nodeIterator());
			}

			return innerNode;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private abstract static class AbstractSingletonNode<K, V> extends CompactNode<K, V> {

		protected abstract byte npos1();

		protected final CompactNode<K, V> node1;

		AbstractSingletonNode(CompactNode<K, V> node1) {
			this.node1 = node1;
		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			}

			// no value
			final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, npos1(), node1);
			return Result.modified(thisNew);
		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_EMPTY:
				case SIZE_ONE:
					// escalate (singleton or empty) result
					return subNodeResult;

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)
					final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, subNodeNew);
					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Invalid size state.");
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@Override
		K getKey(int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		V getValue(int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		public AbstractNode<K, V> getNode(int index) {
			if (index == 0) {
				return node1;
			} else {
				throw new IndexOutOfBoundsException();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<AbstractNode<K, V>> nodeIterator() {
			return ArrayIterator.<AbstractNode<K, V>> of(new AbstractNode[] { node1 });
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 1;
		}

		@Override
		SupplierIterator<K, V> valueIterator() {
			return EmptySupplierIterator.emptyIterator();
		}

		@Override
		boolean hasValues() {
			return false;
		}

		@Override
		int valueArity() {
			return 0;
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + npos1();
			result = prime * result + node1.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			AbstractSingletonNode<?, ?> that = (AbstractSingletonNode<?, ?>) other;

			if (!node1.equals(that.node1)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s]", node1);
		}

		@Override
		K headKey() {
			throw new UnsupportedOperationException("No key in this kind of node.");
		}

		@Override
		V headVal() {
			throw new UnsupportedOperationException("No value in this kind of node.");
		}

	}

	private static final class SingletonNodeAtMask0Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 0;
		}

		SingletonNodeAtMask0Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask1Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 1;
		}

		SingletonNodeAtMask1Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask2Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 2;
		}

		SingletonNodeAtMask2Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask3Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 3;
		}

		SingletonNodeAtMask3Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask4Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 4;
		}

		SingletonNodeAtMask4Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask5Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 5;
		}

		SingletonNodeAtMask5Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask6Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 6;
		}

		SingletonNodeAtMask6Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask7Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 7;
		}

		SingletonNodeAtMask7Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask8Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 8;
		}

		SingletonNodeAtMask8Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask9Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 9;
		}

		SingletonNodeAtMask9Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask10Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 10;
		}

		SingletonNodeAtMask10Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask11Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 11;
		}

		SingletonNodeAtMask11Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask12Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 12;
		}

		SingletonNodeAtMask12Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask13Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 13;
		}

		SingletonNodeAtMask13Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask14Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 14;
		}

		SingletonNodeAtMask14Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask15Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 15;
		}

		SingletonNodeAtMask15Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask16Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 16;
		}

		SingletonNodeAtMask16Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask17Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 17;
		}

		SingletonNodeAtMask17Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask18Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 18;
		}

		SingletonNodeAtMask18Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask19Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 19;
		}

		SingletonNodeAtMask19Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask20Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 20;
		}

		SingletonNodeAtMask20Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask21Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 21;
		}

		SingletonNodeAtMask21Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask22Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 22;
		}

		SingletonNodeAtMask22Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask23Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 23;
		}

		SingletonNodeAtMask23Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask24Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 24;
		}

		SingletonNodeAtMask24Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask25Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 25;
		}

		SingletonNodeAtMask25Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask26Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 26;
		}

		SingletonNodeAtMask26Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask27Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 27;
		}

		SingletonNodeAtMask27Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask28Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 28;
		}

		SingletonNodeAtMask28Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask29Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 29;
		}

		SingletonNodeAtMask29Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask30Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 30;
		}

		SingletonNodeAtMask30Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask31Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 31;
		}

		SingletonNodeAtMask31Node(CompactNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class Value0Index0Node<K, V> extends CompactNode<K, V> {

		Value0Index0Node() {
		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			// no value

			final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, key, val);
			return Result.modified(thisNew);

		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {

			return Optional.empty();
		}

		@Override
		Iterator<CompactNode<K, V>> nodeIterator() {
			return Collections.emptyIterator();
		}

		@Override
		boolean hasNodes() {
			return false;
		}

		@Override
		int nodeArity() {
			return 0;
		}

		@Override
		SupplierIterator<K, V> valueIterator() {
			return EmptySupplierIterator.emptyIterator();
		}

		@Override
		boolean hasValues() {
			return false;
		}

		@Override
		int valueArity() {
			return 0;
		}

		@Override
		K getKey(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		V getValue(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		public CompactNode<K, V> getNode(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		byte sizePredicate() {
			return SIZE_EMPTY;
		}

		@Override
		public int hashCode() {
			return 31;
		}

		@Override
		public boolean equals(Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return "[]";
		}

		@Override
		K headKey() {
			throw new UnsupportedOperationException("No key in this kind of node.");
		}

		@Override
		V headVal() {
			throw new UnsupportedOperationException("No value in this kind of node.");
		}

	}

	private static final class Value0Index2Node<K, V> extends CompactNode<K, V> {
		private final byte npos1;
		private final CompactNode<K, V> node1;
		private final byte npos2;
		private final CompactNode<K, V> node2;

		Value0Index2Node(AtomicReference<Thread> mutator, byte npos1, CompactNode<K, V> node1,
						byte npos2, CompactNode<K, V> node2) {
			this.npos1 = npos1;
			this.node1 = node1;
			this.npos2 = npos2;
			this.node2 = node2;
			assert nodeInvariant();
		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, subNodeResult.getNode(),
								npos2, node2);

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask,
								subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			}

			// no value

			final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, npos1, node1,
							npos2, node2);
			return Result.modified(thisNew);

		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE:
					// inline value

					if (mask < npos1) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), npos2, node2);
						return Result.modified(thisNew);
					}

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)

					final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, subNodeNew, npos2,
									node2);

					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE:
					// inline value

					if (mask < npos1) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), npos1, node1);
						return Result.modified(thisNew);
					}

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)

					final CompactNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask,
									subNodeNew);

					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactNode<K, V>> nodeIterator() {
			return ArrayIterator.<CompactNode<K, V>> of(new CompactNode[] { node1, node2 });
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 2;
		}

		@Override
		SupplierIterator<K, V> valueIterator() {
			return EmptySupplierIterator.emptyIterator();
		}

		@Override
		boolean hasValues() {
			return false;
		}

		@Override
		int valueArity() {
			return 0;
		}

		@Override
		K getKey(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		V getValue(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		public CompactNode<K, V> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + npos1;
			result = prime * result + node1.hashCode();
			result = prime * result + npos2;
			result = prime * result + node2.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Value0Index2Node<?, ?> that = (Value0Index2Node<?, ?>) other;

			if (npos1 != that.npos1) {
				return false;
			}
			if (npos2 != that.npos2) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			if (!node2.equals(that.node2)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s, %s]", node1, node2);
		}

		@Override
		K headKey() {
			throw new UnsupportedOperationException("No key in this kind of node.");
		}

		@Override
		V headVal() {
			throw new UnsupportedOperationException("No value in this kind of node.");
		}

	}

	private static final class Value0Index3Node<K, V> extends CompactNode<K, V> {
		private final byte npos1;
		private final CompactNode<K, V> node1;
		private final byte npos2;
		private final CompactNode<K, V> node2;
		private final byte npos3;
		private final CompactNode<K, V> node3;

		Value0Index3Node(AtomicReference<Thread> mutator, byte npos1, CompactNode<K, V> node1,
						byte npos2, CompactNode<K, V> node2, byte npos3, CompactNode<K, V> node3) {
			this.npos1 = npos1;
			this.node1 = node1;
			this.npos2 = npos2;
			this.node2 = node2;
			this.npos3 = npos3;
			this.node3 = node3;
			assert nodeInvariant();
		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, subNodeResult.getNode(),
								npos2, node2, npos3, node3);

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask,
								subNodeResult.getNode(), npos3, node3);

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else if (mask == npos3) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node3.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
								mask, subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			}

			// no value

			final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, npos1, node1,
							npos2, node2, npos3, node3);
			return Result.modified(thisNew);

		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE:
					// inline value

					if (mask < npos1) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), npos2, node2,
										npos3, node3);
						return Result.modified(thisNew);
					}

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)

					final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, subNodeNew, npos2,
									node2, npos3, node3);

					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE:
					// inline value

					if (mask < npos1) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), npos1, node1,
										npos3, node3);
						return Result.modified(thisNew);
					}

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)

					final CompactNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask,
									subNodeNew, npos3, node3);

					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
			} else if (mask == npos3) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node3.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE:
					// inline value

					if (mask < npos1) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), npos1, node1,
										npos2, node2);
						return Result.modified(thisNew);
					}

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)

					final CompactNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2,
									node2, mask, subNodeNew);

					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos3) {
				return node3.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos3) {
				return node3.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactNode<K, V>> nodeIterator() {
			return ArrayIterator.<CompactNode<K, V>> of(new CompactNode[] { node1, node2, node3 });
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 3;
		}

		@Override
		SupplierIterator<K, V> valueIterator() {
			return EmptySupplierIterator.emptyIterator();
		}

		@Override
		boolean hasValues() {
			return false;
		}

		@Override
		int valueArity() {
			return 0;
		}

		@Override
		K getKey(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		V getValue(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		public CompactNode<K, V> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;
			case 2:
				return node3;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + npos1;
			result = prime * result + node1.hashCode();
			result = prime * result + npos2;
			result = prime * result + node2.hashCode();
			result = prime * result + npos3;
			result = prime * result + node3.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Value0Index3Node<?, ?> that = (Value0Index3Node<?, ?>) other;

			if (npos1 != that.npos1) {
				return false;
			}
			if (npos2 != that.npos2) {
				return false;
			}
			if (npos3 != that.npos3) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			if (!node2.equals(that.node2)) {
				return false;
			}
			if (!node3.equals(that.node3)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s, %s, %s]", node1, node2, node3);
		}

		@Override
		K headKey() {
			throw new UnsupportedOperationException("No key in this kind of node.");
		}

		@Override
		V headVal() {
			throw new UnsupportedOperationException("No value in this kind of node.");
		}

	}

	private static final class Value0Index4Node<K, V> extends CompactNode<K, V> {
		private final byte npos1;
		private final CompactNode<K, V> node1;
		private final byte npos2;
		private final CompactNode<K, V> node2;
		private final byte npos3;
		private final CompactNode<K, V> node3;
		private final byte npos4;
		private final CompactNode<K, V> node4;

		Value0Index4Node(AtomicReference<Thread> mutator, byte npos1, CompactNode<K, V> node1,
						byte npos2, CompactNode<K, V> node2, byte npos3, CompactNode<K, V> node3,
						byte npos4, CompactNode<K, V> node4) {
			this.npos1 = npos1;
			this.node1 = node1;
			this.npos2 = npos2;
			this.node2 = node2;
			this.npos3 = npos3;
			this.node3 = node3;
			this.npos4 = npos4;
			this.node4 = node4;
			assert nodeInvariant();
		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, subNodeResult.getNode(),
								npos2, node2, npos3, node3, npos4, node4);

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask,
								subNodeResult.getNode(), npos3, node3, npos4, node4);

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else if (mask == npos3) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node3.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
								mask, subNodeResult.getNode(), npos4, node4);

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else if (mask == npos4) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node4.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
								npos3, node3, mask, subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			}

			// no value

			final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, npos1, node1,
							npos2, node2, npos3, node3, npos4, node4);
			return Result.modified(thisNew);

		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE:
					// inline value

					if (mask < npos1) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), npos2, node2,
										npos3, node3, npos4, node4);
						return Result.modified(thisNew);
					}

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)

					final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, subNodeNew, npos2,
									node2, npos3, node3, npos4, node4);

					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE:
					// inline value

					if (mask < npos1) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), npos1, node1,
										npos3, node3, npos4, node4);
						return Result.modified(thisNew);
					}

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)

					final CompactNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask,
									subNodeNew, npos3, node3, npos4, node4);

					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
			} else if (mask == npos3) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node3.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE:
					// inline value

					if (mask < npos1) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), npos1, node1,
										npos2, node2, npos4, node4);
						return Result.modified(thisNew);
					}

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)

					final CompactNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2,
									node2, mask, subNodeNew, npos4, node4);

					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
			} else if (mask == npos4) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node4.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE:
					// inline value

					if (mask < npos1) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), npos1, node1,
										npos2, node2, npos3, node3);
						return Result.modified(thisNew);
					}

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)

					final CompactNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2,
									node2, npos3, node3, mask, subNodeNew);

					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos3) {
				return node3.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos4) {
				return node4.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos3) {
				return node3.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos4) {
				return node4.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactNode<K, V>> nodeIterator() {
			return ArrayIterator.<CompactNode<K, V>> of(new CompactNode[] { node1, node2, node3,
							node4 });
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 4;
		}

		@Override
		SupplierIterator<K, V> valueIterator() {
			return EmptySupplierIterator.emptyIterator();
		}

		@Override
		boolean hasValues() {
			return false;
		}

		@Override
		int valueArity() {
			return 0;
		}

		@Override
		K getKey(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		V getValue(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		public CompactNode<K, V> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;
			case 2:
				return node3;
			case 3:
				return node4;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + npos1;
			result = prime * result + node1.hashCode();
			result = prime * result + npos2;
			result = prime * result + node2.hashCode();
			result = prime * result + npos3;
			result = prime * result + node3.hashCode();
			result = prime * result + npos4;
			result = prime * result + node4.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Value0Index4Node<?, ?> that = (Value0Index4Node<?, ?>) other;

			if (npos1 != that.npos1) {
				return false;
			}
			if (npos2 != that.npos2) {
				return false;
			}
			if (npos3 != that.npos3) {
				return false;
			}
			if (npos4 != that.npos4) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			if (!node2.equals(that.node2)) {
				return false;
			}
			if (!node3.equals(that.node3)) {
				return false;
			}
			if (!node4.equals(that.node4)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s, %s, %s, %s]", node1, node2, node3, node4);
		}

		@Override
		K headKey() {
			throw new UnsupportedOperationException("No key in this kind of node.");
		}

		@Override
		V headVal() {
			throw new UnsupportedOperationException("No value in this kind of node.");
		}

	}

	private static final class Value1Index0Node<K, V> extends CompactNode<K, V> {
		private final byte pos1;
		private final K key1;
		private final V val1;

		Value1Index0Node(AtomicReference<Thread> mutator, byte pos1, K key1, V val1) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;
			assert nodeInvariant();
		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (cmp.compare(val, val1) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(valNodeOf(mutator, mask, key, val), val1);
				}

				// merge into node
				final CompactNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, node);
				return Result.modified(thisNew);

			}

			// no value

			if (mask < pos1) {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, pos1, key1,
								val1);
				return Result.modified(thisNew);
			}

			else {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, key,
								val);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(CompactNode.<K, V> valNodeOf(mutator));
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			}

			return Optional.empty();
		}

		@Override
		Iterator<CompactNode<K, V>> nodeIterator() {
			return Collections.emptyIterator();
		}

		@Override
		boolean hasNodes() {
			return false;
		}

		@Override
		int nodeArity() {
			return 0;
		}

		@Override
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1 });
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return 1;
		}

		@Override
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		V getValue(int index) {
			switch (index) {
			case 0:
				return val1;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public CompactNode<K, V> getNode(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		byte sizePredicate() {
			return SIZE_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Value1Index0Node<?, ?> that = (Value1Index0Node<?, ?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s=%s]", key1, val1);
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		V headVal() {
			return val1;
		}

	}

	private static final class Value1Index1Node<K, V> extends CompactNode<K, V> {
		private final byte pos1;
		private final K key1;
		private final V val1;
		private final byte npos1;
		private final CompactNode<K, V> node1;

		Value1Index1Node(AtomicReference<Thread> mutator, byte pos1, K key1, V val1, byte npos1,
						CompactNode<K, V> node1) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;
			this.npos1 = npos1;
			this.node1 = node1;
			assert nodeInvariant();
		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (cmp.compare(val, val1) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(valNodeOf(mutator, mask, key, val, npos1, node1), val1);
				}

				// merge into node
				final CompactNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, node, npos1, node1);
					return Result.modified(thisNew);
				}

				else {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
								subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			}

			// no value

			if (mask < pos1) {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, pos1, key1,
								val1, npos1, node1);
				return Result.modified(thisNew);
			}

			else {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, key,
								val, npos1, node1);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, npos1, node1));
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE:
					// inline value

					if (mask < npos1) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), pos1, key1,
										val1);
						return Result.modified(thisNew);
					}

					else {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										mask, subNodeNew.headKey(), subNodeNew.headVal());
						return Result.modified(thisNew);
					}

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)

					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
									subNodeNew);

					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactNode<K, V>> nodeIterator() {
			return ArrayIterator.<CompactNode<K, V>> of(new CompactNode[] { node1 });
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 1;
		}

		@Override
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1 });
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return 1;
		}

		@Override
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		V getValue(int index) {
			switch (index) {
			case 0:
				return val1;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public CompactNode<K, V> getNode(int index) {
			switch (index) {
			case 0:
				return node1;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();
			result = prime * result + npos1;
			result = prime * result + node1.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Value1Index1Node<?, ?> that = (Value1Index1Node<?, ?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (npos1 != that.npos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s=%s, %s]", key1, val1, node1);
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		V headVal() {
			return val1;
		}

	}

	private static final class Value1Index2Node<K, V> extends CompactNode<K, V> {
		private final byte pos1;
		private final K key1;
		private final V val1;
		private final byte npos1;
		private final CompactNode<K, V> node1;
		private final byte npos2;
		private final CompactNode<K, V> node2;

		Value1Index2Node(AtomicReference<Thread> mutator, byte pos1, K key1, V val1, byte npos1,
						CompactNode<K, V> node1, byte npos2, CompactNode<K, V> node2) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;
			this.npos1 = npos1;
			this.node1 = node1;
			this.npos2 = npos2;
			this.node2 = node2;
			assert nodeInvariant();
		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (cmp.compare(val, val1) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, mask, key, val, npos1, node1, npos2, node2),
									val1);
				}

				// merge into node
				final CompactNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, node, npos1, node1,
									npos2, node2);
					return Result.modified(thisNew);
				}

				else if (mask < npos2) {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask, node,
									npos2, node2);
					return Result.modified(thisNew);
				}

				else {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2,
									node2, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
								subNodeResult.getNode(), npos2, node2);

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
								node1, mask, subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			}

			// no value

			if (mask < pos1) {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, pos1, key1,
								val1, npos1, node1, npos2, node2);
				return Result.modified(thisNew);
			}

			else {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, key,
								val, npos1, node1, npos2, node2);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2));
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE:
					// inline value

					if (mask < npos1) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), pos1, key1,
										val1, npos2, node2);
						return Result.modified(thisNew);
					}

					else {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										mask, subNodeNew.headKey(), subNodeNew.headVal(), npos2,
										node2);
						return Result.modified(thisNew);
					}

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)

					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
									subNodeNew, npos2, node2);

					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE:
					// inline value

					if (mask < npos1) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), pos1, key1,
										val1, npos1, node1);
						return Result.modified(thisNew);
					}

					else {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										mask, subNodeNew.headKey(), subNodeNew.headVal(), npos1,
										node1);
						return Result.modified(thisNew);
					}

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)

					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
									node1, mask, subNodeNew);

					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactNode<K, V>> nodeIterator() {
			return ArrayIterator.<CompactNode<K, V>> of(new CompactNode[] { node1, node2 });
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 2;
		}

		@Override
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1 });
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return 1;
		}

		@Override
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		V getValue(int index) {
			switch (index) {
			case 0:
				return val1;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public CompactNode<K, V> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();
			result = prime * result + npos1;
			result = prime * result + node1.hashCode();
			result = prime * result + npos2;
			result = prime * result + node2.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Value1Index2Node<?, ?> that = (Value1Index2Node<?, ?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (npos1 != that.npos1) {
				return false;
			}
			if (npos2 != that.npos2) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			if (!node2.equals(that.node2)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s=%s, %s, %s]", key1, val1, node1, node2);
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		V headVal() {
			return val1;
		}

	}

	private static final class Value1Index3Node<K, V> extends CompactNode<K, V> {
		private final byte pos1;
		private final K key1;
		private final V val1;
		private final byte npos1;
		private final CompactNode<K, V> node1;
		private final byte npos2;
		private final CompactNode<K, V> node2;
		private final byte npos3;
		private final CompactNode<K, V> node3;

		Value1Index3Node(AtomicReference<Thread> mutator, byte pos1, K key1, V val1, byte npos1,
						CompactNode<K, V> node1, byte npos2, CompactNode<K, V> node2, byte npos3,
						CompactNode<K, V> node3) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;
			this.npos1 = npos1;
			this.node1 = node1;
			this.npos2 = npos2;
			this.node2 = node2;
			this.npos3 = npos3;
			this.node3 = node3;
			assert nodeInvariant();
		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (cmp.compare(val, val1) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, mask, key, val, npos1, node1, npos2, node2,
													npos3, node3), val1);
				}

				// merge into node
				final CompactNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, node, npos1, node1,
									npos2, node2, npos3, node3);
					return Result.modified(thisNew);
				}

				else if (mask < npos2) {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask, node,
									npos2, node2, npos3, node3);
					return Result.modified(thisNew);
				}

				else if (mask < npos3) {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2,
									node2, mask, node, npos3, node3);
					return Result.modified(thisNew);
				}

				else {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2,
									node2, npos3, node3, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
								subNodeResult.getNode(), npos2, node2, npos3, node3);

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
								node1, mask, subNodeResult.getNode(), npos3, node3);

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else if (mask == npos3) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node3.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
								node1, npos2, node2, mask, subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			}

			// no value

			if (mask < pos1) {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, pos1, key1,
								val1, npos1, node1, npos2, node2, npos3, node3);
				return Result.modified(thisNew);
			}

			else {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, key,
								val, npos1, node1, npos2, node2, npos3, node3);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2, npos3, node3));
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE:
					// inline value

					if (mask < npos1) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), pos1, key1,
										val1, npos2, node2, npos3, node3);
						return Result.modified(thisNew);
					}

					else {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										mask, subNodeNew.headKey(), subNodeNew.headVal(), npos2,
										node2, npos3, node3);
						return Result.modified(thisNew);
					}

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)

					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
									subNodeNew, npos2, node2, npos3, node3);

					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE:
					// inline value

					if (mask < npos1) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), pos1, key1,
										val1, npos1, node1, npos3, node3);
						return Result.modified(thisNew);
					}

					else {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										mask, subNodeNew.headKey(), subNodeNew.headVal(), npos1,
										node1, npos3, node3);
						return Result.modified(thisNew);
					}

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)

					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
									node1, mask, subNodeNew, npos3, node3);

					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
			} else if (mask == npos3) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node3.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE:
					// inline value

					if (mask < npos1) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), pos1, key1,
										val1, npos1, node1, npos2, node2);
						return Result.modified(thisNew);
					}

					else {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										mask, subNodeNew.headKey(), subNodeNew.headVal(), npos1,
										node1, npos2, node2);
						return Result.modified(thisNew);
					}

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)

					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
									node1, npos2, node2, mask, subNodeNew);

					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos3) {
				return node3.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos3) {
				return node3.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactNode<K, V>> nodeIterator() {
			return ArrayIterator.<CompactNode<K, V>> of(new CompactNode[] { node1, node2, node3 });
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 3;
		}

		@Override
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1 });
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return 1;
		}

		@Override
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		V getValue(int index) {
			switch (index) {
			case 0:
				return val1;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public CompactNode<K, V> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;
			case 2:
				return node3;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();
			result = prime * result + npos1;
			result = prime * result + node1.hashCode();
			result = prime * result + npos2;
			result = prime * result + node2.hashCode();
			result = prime * result + npos3;
			result = prime * result + node3.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Value1Index3Node<?, ?> that = (Value1Index3Node<?, ?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (npos1 != that.npos1) {
				return false;
			}
			if (npos2 != that.npos2) {
				return false;
			}
			if (npos3 != that.npos3) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			if (!node2.equals(that.node2)) {
				return false;
			}
			if (!node3.equals(that.node3)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s=%s, %s, %s, %s]", key1, val1, node1, node2, node3);
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		V headVal() {
			return val1;
		}

	}

	private static final class Value2Index0Node<K, V> extends CompactNode<K, V> {
		private final byte pos1;
		private final K key1;
		private final V val1;
		private final byte pos2;
		private final K key2;
		private final V val2;

		Value2Index0Node(AtomicReference<Thread> mutator, byte pos1, K key1, V val1, byte pos2,
						K key2, V val2) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;
			this.pos2 = pos2;
			this.key2 = key2;
			this.val2 = val2;
			assert nodeInvariant();
		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (cmp.compare(val, val1) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(valNodeOf(mutator, mask, key, val, pos2, key2, val2),
									val1);
				}

				// merge into node
				final CompactNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos2, key2, val2, mask, node);
				return Result.modified(thisNew);

			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					if (cmp.compare(val, val2) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(valNodeOf(mutator, pos1, key1, val1, mask, key, val),
									val2);
				}

				// merge into node
				final CompactNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, node);
				return Result.modified(thisNew);

			}

			// no value

			if (mask < pos1) {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, pos1, key1,
								val1, pos2, key2, val2);
				return Result.modified(thisNew);
			}

			else if (mask < pos2) {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, key,
								val, pos2, key2, val2);
				return Result.modified(thisNew);
			}

			else {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, mask, key, val);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				/*
				 * Create root node with singleton element. This node will
				 * be a) either be the new root returned, or b) unwrapped
				 * and inlined.
				 */
				return Result.modified(valNodeOf(mutator,
								(byte) (key2.hashCode() & BIT_PARTITION_MASK), key2, val2));
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				/*
				 * Create root node with singleton element. This node will
				 * be a) either be the new root returned, or b) unwrapped
				 * and inlined.
				 */
				return Result.modified(valNodeOf(mutator,
								(byte) (key1.hashCode() & BIT_PARTITION_MASK), key1, val1));
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return true;
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(entryOf(key2, val2));
			}

			return Optional.empty();
		}

		@Override
		Iterator<CompactNode<K, V>> nodeIterator() {
			return Collections.emptyIterator();
		}

		@Override
		boolean hasNodes() {
			return false;
		}

		@Override
		int nodeArity() {
			return 0;
		}

		@Override
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2 });
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return 2;
		}

		@Override
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;
			case 1:
				return key2;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		V getValue(int index) {
			switch (index) {
			case 0:
				return val1;
			case 1:
				return val2;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public CompactNode<K, V> getNode(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();
			result = prime * result + pos2;
			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Value2Index0Node<?, ?> that = (Value2Index0Node<?, ?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (pos2 != that.pos2) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s=%s, %s=%s]", key1, val1, key2, val2);
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		V headVal() {
			return val1;
		}

	}

	private static final class Value2Index1Node<K, V> extends CompactNode<K, V> {
		private final byte pos1;
		private final K key1;
		private final V val1;
		private final byte pos2;
		private final K key2;
		private final V val2;
		private final byte npos1;
		private final CompactNode<K, V> node1;

		Value2Index1Node(AtomicReference<Thread> mutator, byte pos1, K key1, V val1, byte pos2,
						K key2, V val2, byte npos1, CompactNode<K, V> node1) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;
			this.pos2 = pos2;
			this.key2 = key2;
			this.val2 = val2;
			this.npos1 = npos1;
			this.node1 = node1;
			assert nodeInvariant();
		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (cmp.compare(val, val1) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, mask, key, val, pos2, key2, val2, npos1,
													node1), val1);
				}

				// merge into node
				final CompactNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos2, key2, val2, mask,
									node, npos1, node1);
					return Result.modified(thisNew);
				}

				else {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos2, key2, val2, npos1,
									node1, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					if (cmp.compare(val, val2) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, pos1, key1, val1, mask, key, val, npos1,
													node1), val2);
				}

				// merge into node
				final CompactNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
									node, npos1, node1);
					return Result.modified(thisNew);
				}

				else {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
									node1, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, mask, subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			}

			// no value

			if (mask < pos1) {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, pos1, key1,
								val1, pos2, key2, val2, npos1, node1);
				return Result.modified(thisNew);
			}

			else if (mask < pos2) {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, key,
								val, pos2, key2, val2, npos1, node1);
				return Result.modified(thisNew);
			}

			else {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, mask, key, val, npos1, node1);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos2, key2, val2, npos1, node1));
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1));
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE:
					// inline value

					if (mask < npos1) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), pos1, key1,
										val1, pos2, key2, val2);
						return Result.modified(thisNew);
					}

					else if (mask < pos2) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										mask, subNodeNew.headKey(), subNodeNew.headVal(), pos2,
										key2, val2);
						return Result.modified(thisNew);
					}

					else {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										pos2, key2, val2, mask, subNodeNew.headKey(),
										subNodeNew.headVal());
						return Result.modified(thisNew);
					}

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)

					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2,
									key2, val2, mask, subNodeNew);

					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return true;
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(entryOf(key2, val2));
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactNode<K, V>> nodeIterator() {
			return ArrayIterator.<CompactNode<K, V>> of(new CompactNode[] { node1 });
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 1;
		}

		@Override
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2 });
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return 2;
		}

		@Override
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;
			case 1:
				return key2;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		V getValue(int index) {
			switch (index) {
			case 0:
				return val1;
			case 1:
				return val2;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public CompactNode<K, V> getNode(int index) {
			switch (index) {
			case 0:
				return node1;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();
			result = prime * result + pos2;
			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();
			result = prime * result + npos1;
			result = prime * result + node1.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Value2Index1Node<?, ?> that = (Value2Index1Node<?, ?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (pos2 != that.pos2) {
				return false;
			}
			if (npos1 != that.npos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s=%s, %s=%s, %s]", key1, val1, key2, val2, node1);
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		V headVal() {
			return val1;
		}

	}

	private static final class Value2Index2Node<K, V> extends CompactNode<K, V> {
		private final byte pos1;
		private final K key1;
		private final V val1;
		private final byte pos2;
		private final K key2;
		private final V val2;
		private final byte npos1;
		private final CompactNode<K, V> node1;
		private final byte npos2;
		private final CompactNode<K, V> node2;

		Value2Index2Node(AtomicReference<Thread> mutator, byte pos1, K key1, V val1, byte pos2,
						K key2, V val2, byte npos1, CompactNode<K, V> node1, byte npos2,
						CompactNode<K, V> node2) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;
			this.pos2 = pos2;
			this.key2 = key2;
			this.val2 = val2;
			this.npos1 = npos1;
			this.node1 = node1;
			this.npos2 = npos2;
			this.node2 = node2;
			assert nodeInvariant();
		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (cmp.compare(val, val1) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, mask, key, val, pos2, key2, val2, npos1,
													node1, npos2, node2), val1);
				}

				// merge into node
				final CompactNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos2, key2, val2, mask,
									node, npos1, node1, npos2, node2);
					return Result.modified(thisNew);
				}

				else if (mask < npos2) {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos2, key2, val2, npos1,
									node1, mask, node, npos2, node2);
					return Result.modified(thisNew);
				}

				else {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos2, key2, val2, npos1,
									node1, npos2, node2, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					if (cmp.compare(val, val2) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, pos1, key1, val1, mask, key, val, npos1,
													node1, npos2, node2), val2);
				}

				// merge into node
				final CompactNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
									node, npos1, node1, npos2, node2);
					return Result.modified(thisNew);
				}

				else if (mask < npos2) {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
									node1, mask, node, npos2, node2);
					return Result.modified(thisNew);
				}

				else {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
									node1, npos2, node2, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, mask, subNodeResult.getNode(), npos2, node2);

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, npos1, node1, mask, subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			}

			// no value

			if (mask < pos1) {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, pos1, key1,
								val1, pos2, key2, val2, npos1, node1, npos2, node2);
				return Result.modified(thisNew);
			}

			else if (mask < pos2) {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, key,
								val, pos2, key2, val2, npos1, node1, npos2, node2);
				return Result.modified(thisNew);
			}

			else {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, mask, key, val, npos1, node1, npos2, node2);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos2, key2, val2, npos1, node1, npos2,
								node2));
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1, npos2,
								node2));
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE:
					// inline value

					if (mask < npos1) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), pos1, key1,
										val1, pos2, key2, val2, npos2, node2);
						return Result.modified(thisNew);
					}

					else if (mask < pos2) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										mask, subNodeNew.headKey(), subNodeNew.headVal(), pos2,
										key2, val2, npos2, node2);
						return Result.modified(thisNew);
					}

					else {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										pos2, key2, val2, mask, subNodeNew.headKey(),
										subNodeNew.headVal(), npos2, node2);
						return Result.modified(thisNew);
					}

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)

					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2,
									key2, val2, mask, subNodeNew, npos2, node2);

					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE:
					// inline value

					if (mask < npos1) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), pos1, key1,
										val1, pos2, key2, val2, npos1, node1);
						return Result.modified(thisNew);
					}

					else if (mask < pos2) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										mask, subNodeNew.headKey(), subNodeNew.headVal(), pos2,
										key2, val2, npos1, node1);
						return Result.modified(thisNew);
					}

					else {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										pos2, key2, val2, mask, subNodeNew.headKey(),
										subNodeNew.headVal(), npos1, node1);
						return Result.modified(thisNew);
					}

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)

					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2,
									key2, val2, npos1, node1, mask, subNodeNew);

					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return true;
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(entryOf(key2, val2));
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactNode<K, V>> nodeIterator() {
			return ArrayIterator.<CompactNode<K, V>> of(new CompactNode[] { node1, node2 });
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 2;
		}

		@Override
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2 });
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return 2;
		}

		@Override
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;
			case 1:
				return key2;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		V getValue(int index) {
			switch (index) {
			case 0:
				return val1;
			case 1:
				return val2;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public CompactNode<K, V> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();
			result = prime * result + pos2;
			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();
			result = prime * result + npos1;
			result = prime * result + node1.hashCode();
			result = prime * result + npos2;
			result = prime * result + node2.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Value2Index2Node<?, ?> that = (Value2Index2Node<?, ?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (pos2 != that.pos2) {
				return false;
			}
			if (npos1 != that.npos1) {
				return false;
			}
			if (npos2 != that.npos2) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			if (!node2.equals(that.node2)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s=%s, %s=%s, %s, %s]", key1, val1, key2, val2, node1, node2);
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		V headVal() {
			return val1;
		}

	}

	private static final class Value3Index0Node<K, V> extends CompactNode<K, V> {
		private final byte pos1;
		private final K key1;
		private final V val1;
		private final byte pos2;
		private final K key2;
		private final V val2;
		private final byte pos3;
		private final K key3;
		private final V val3;

		Value3Index0Node(AtomicReference<Thread> mutator, byte pos1, K key1, V val1, byte pos2,
						K key2, V val2, byte pos3, K key3, V val3) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;
			this.pos2 = pos2;
			this.key2 = key2;
			this.val2 = val2;
			this.pos3 = pos3;
			this.key3 = key3;
			this.val3 = val3;
			assert nodeInvariant();
		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (cmp.compare(val, val1) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, mask, key, val, pos2, key2, val2, pos3,
													key3, val3), val1);
				}

				// merge into node
				final CompactNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos2, key2, val2, pos3, key3,
								val3, mask, node);
				return Result.modified(thisNew);

			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					if (cmp.compare(val, val2) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, pos1, key1, val1, mask, key, val, pos3,
													key3, val3), val2);
				}

				// merge into node
				final CompactNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos3, key3,
								val3, mask, node);
				return Result.modified(thisNew);

			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					if (cmp.compare(val, val3) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, mask,
													key, val), val3);
				}

				// merge into node
				final CompactNode<K, V> node = mergeNodes(key3, key3.hashCode(), val3, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, mask, node);
				return Result.modified(thisNew);

			}

			// no value

			if (mask < pos1) {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, pos1, key1,
								val1, pos2, key2, val2, pos3, key3, val3);
				return Result.modified(thisNew);
			}

			else if (mask < pos2) {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, key,
								val, pos2, key2, val2, pos3, key3, val3);
				return Result.modified(thisNew);
			}

			else if (mask < pos3) {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, mask, key, val, pos3, key3, val3);
				return Result.modified(thisNew);
			}

			else {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, pos3, key3, val3, mask, key, val);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos2, key2, val2, pos3, key3, val3));
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos3, key3, val3));
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2));
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return true;
			} else if (mask == pos3 && cmp.compare(key, key3) == 0) {
				return true;
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(entryOf(key2, val2));
			} else if (mask == pos3 && cmp.compare(key, key3) == 0) {
				return Optional.of(entryOf(key3, val3));
			}

			return Optional.empty();
		}

		@Override
		Iterator<CompactNode<K, V>> nodeIterator() {
			return Collections.emptyIterator();
		}

		@Override
		boolean hasNodes() {
			return false;
		}

		@Override
		int nodeArity() {
			return 0;
		}

		@Override
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3 });
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return 3;
		}

		@Override
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;
			case 1:
				return key2;
			case 2:
				return key3;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		V getValue(int index) {
			switch (index) {
			case 0:
				return val1;
			case 1:
				return val2;
			case 2:
				return val3;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public CompactNode<K, V> getNode(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();
			result = prime * result + pos2;
			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();
			result = prime * result + pos3;
			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Value3Index0Node<?, ?> that = (Value3Index0Node<?, ?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (pos2 != that.pos2) {
				return false;
			}
			if (pos3 != that.pos3) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s=%s, %s=%s, %s=%s]", key1, val1, key2, val2, key3, val3);
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		V headVal() {
			return val1;
		}

	}

	private static final class Value3Index1Node<K, V> extends CompactNode<K, V> {
		private final byte pos1;
		private final K key1;
		private final V val1;
		private final byte pos2;
		private final K key2;
		private final V val2;
		private final byte pos3;
		private final K key3;
		private final V val3;
		private final byte npos1;
		private final CompactNode<K, V> node1;

		Value3Index1Node(AtomicReference<Thread> mutator, byte pos1, K key1, V val1, byte pos2,
						K key2, V val2, byte pos3, K key3, V val3, byte npos1,
						CompactNode<K, V> node1) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;
			this.pos2 = pos2;
			this.key2 = key2;
			this.val2 = val2;
			this.pos3 = pos3;
			this.key3 = key3;
			this.val3 = val3;
			this.npos1 = npos1;
			this.node1 = node1;
			assert nodeInvariant();
		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (cmp.compare(val, val1) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, mask, key, val, pos2, key2, val2, pos3,
													key3, val3, npos1, node1), val1);
				}

				// merge into node
				final CompactNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos2, key2, val2, pos3,
									key3, val3, mask, node, npos1, node1);
					return Result.modified(thisNew);
				}

				else {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos2, key2, val2, pos3,
									key3, val3, npos1, node1, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					if (cmp.compare(val, val2) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, pos1, key1, val1, mask, key, val, pos3,
													key3, val3, npos1, node1), val2);
				}

				// merge into node
				final CompactNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos3,
									key3, val3, mask, node, npos1, node1);
					return Result.modified(thisNew);
				}

				else {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos3,
									key3, val3, npos1, node1, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					if (cmp.compare(val, val3) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, mask,
													key, val, npos1, node1), val3);
				}

				// merge into node
				final CompactNode<K, V> node = mergeNodes(key3, key3.hashCode(), val3, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2,
									key2, val2, mask, node, npos1, node1);
					return Result.modified(thisNew);
				}

				else {
					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2,
									key2, val2, npos1, node1, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, pos3, key3, val3, mask, subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			}

			// no value

			if (mask < pos1) {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, pos1, key1,
								val1, pos2, key2, val2, pos3, key3, val3, npos1, node1);
				return Result.modified(thisNew);
			}

			else if (mask < pos2) {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, key,
								val, pos2, key2, val2, pos3, key3, val3, npos1, node1);
				return Result.modified(thisNew);
			}

			else if (mask < pos3) {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, mask, key, val, pos3, key3, val3, npos1, node1);
				return Result.modified(thisNew);
			}

			else {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, pos3, key3, val3, mask, key, val, npos1, node1);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos2, key2, val2, pos3, key3, val3,
								npos1, node1));
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos3, key3, val3,
								npos1, node1));
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
								npos1, node1));
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactNode<K, V>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K, V> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE:
					// inline value

					if (mask < npos1) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), pos1, key1,
										val1, pos2, key2, val2, pos3, key3, val3);
						return Result.modified(thisNew);
					}

					else if (mask < pos2) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										mask, subNodeNew.headKey(), subNodeNew.headVal(), pos2,
										key2, val2, pos3, key3, val3);
						return Result.modified(thisNew);
					}

					else if (mask < pos3) {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										pos2, key2, val2, mask, subNodeNew.headKey(),
										subNodeNew.headVal(), pos3, key3, val3);
						return Result.modified(thisNew);
					}

					else {
						final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										pos2, key2, val2, pos3, key3, val3, mask,
										subNodeNew.headKey(), subNodeNew.headVal());
						return Result.modified(thisNew);
					}

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)

					final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2,
									key2, val2, pos3, key3, val3, mask, subNodeNew);

					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return true;
			} else if (mask == pos3 && cmp.compare(key, key3) == 0) {
				return true;
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(entryOf(key2, val2));
			} else if (mask == pos3 && cmp.compare(key, key3) == 0) {
				return Optional.of(entryOf(key3, val3));
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactNode<K, V>> nodeIterator() {
			return ArrayIterator.<CompactNode<K, V>> of(new CompactNode[] { node1 });
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 1;
		}

		@Override
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3 });
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return 3;
		}

		@Override
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;
			case 1:
				return key2;
			case 2:
				return key3;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		V getValue(int index) {
			switch (index) {
			case 0:
				return val1;
			case 1:
				return val2;
			case 2:
				return val3;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public CompactNode<K, V> getNode(int index) {
			switch (index) {
			case 0:
				return node1;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();
			result = prime * result + pos2;
			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();
			result = prime * result + pos3;
			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();
			result = prime * result + npos1;
			result = prime * result + node1.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Value3Index1Node<?, ?> that = (Value3Index1Node<?, ?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (pos2 != that.pos2) {
				return false;
			}
			if (pos3 != that.pos3) {
				return false;
			}
			if (npos1 != that.npos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s=%s, %s=%s, %s=%s, %s]", key1, val1, key2, val2, key3, val3,
							node1);
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		V headVal() {
			return val1;
		}

	}

	private static final class Value4Index0Node<K, V> extends CompactNode<K, V> {
		private final byte pos1;
		private final K key1;
		private final V val1;
		private final byte pos2;
		private final K key2;
		private final V val2;
		private final byte pos3;
		private final K key3;
		private final V val3;
		private final byte pos4;
		private final K key4;
		private final V val4;

		Value4Index0Node(AtomicReference<Thread> mutator, byte pos1, K key1, V val1, byte pos2,
						K key2, V val2, byte pos3, K key3, V val3, byte pos4, K key4, V val4) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;
			this.pos2 = pos2;
			this.key2 = key2;
			this.val2 = val2;
			this.pos3 = pos3;
			this.key3 = key3;
			this.val3 = val3;
			this.pos4 = pos4;
			this.key4 = key4;
			this.val4 = val4;
			assert nodeInvariant();
		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (cmp.compare(val, val1) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, mask, key, val, pos2, key2, val2, pos3,
													key3, val3, pos4, key4, val4), val1);
				}

				// merge into node
				final CompactNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos2, key2, val2, pos3, key3,
								val3, pos4, key4, val4, mask, node);
				return Result.modified(thisNew);

			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					if (cmp.compare(val, val2) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, pos1, key1, val1, mask, key, val, pos3,
													key3, val3, pos4, key4, val4), val2);
				}

				// merge into node
				final CompactNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos3, key3,
								val3, pos4, key4, val4, mask, node);
				return Result.modified(thisNew);

			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					if (cmp.compare(val, val3) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, mask,
													key, val, pos4, key4, val4), val3);
				}

				// merge into node
				final CompactNode<K, V> node = mergeNodes(key3, key3.hashCode(), val3, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, pos4, key4, val4, mask, node);
				return Result.modified(thisNew);

			} else if (mask == pos4) {
				if (cmp.compare(key, key4) == 0) {
					if (cmp.compare(val, val4) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3,
													key3, val3, mask, key, val), val4);
				}

				// merge into node
				final CompactNode<K, V> node = mergeNodes(key4, key4.hashCode(), val4, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, pos3, key3, val3, mask, node);
				return Result.modified(thisNew);

			}

			// no value

			if (mask < pos1) {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, pos1, key1,
								val1, pos2, key2, val2, pos3, key3, val3, pos4, key4, val4);
				return Result.modified(thisNew);
			}

			else if (mask < pos2) {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, key,
								val, pos2, key2, val2, pos3, key3, val3, pos4, key4, val4);
				return Result.modified(thisNew);
			}

			else if (mask < pos3) {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, mask, key, val, pos3, key3, val3, pos4, key4, val4);
				return Result.modified(thisNew);
			}

			else if (mask < pos4) {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, pos3, key3, val3, mask, key, val, pos4, key4, val4);
				return Result.modified(thisNew);
			}

			else {
				final CompactNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, pos3, key3, val3, pos4, key4, val4, mask, key, val);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, V, ? extends CompactNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos2, key2, val2, pos3, key3, val3, pos4,
								key4, val4));
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos3, key3, val3, pos4,
								key4, val4));
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos4,
								key4, val4));
			} else if (mask == pos4) {
				if (cmp.compare(key, key4) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3,
								key3, val3));
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return true;
			} else if (mask == pos3 && cmp.compare(key, key3) == 0) {
				return true;
			} else if (mask == pos4 && cmp.compare(key, key4) == 0) {
				return true;
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(entryOf(key2, val2));
			} else if (mask == pos3 && cmp.compare(key, key3) == 0) {
				return Optional.of(entryOf(key3, val3));
			} else if (mask == pos4 && cmp.compare(key, key4) == 0) {
				return Optional.of(entryOf(key4, val4));
			}

			return Optional.empty();
		}

		@Override
		Iterator<CompactNode<K, V>> nodeIterator() {
			return Collections.emptyIterator();
		}

		@Override
		boolean hasNodes() {
			return false;
		}

		@Override
		int nodeArity() {
			return 0;
		}

		@Override
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3,
							key4, val4 });
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return 4;
		}

		@Override
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;
			case 1:
				return key2;
			case 2:
				return key3;
			case 3:
				return key4;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		V getValue(int index) {
			switch (index) {
			case 0:
				return val1;
			case 1:
				return val2;
			case 2:
				return val3;
			case 3:
				return val4;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public CompactNode<K, V> getNode(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();
			result = prime * result + pos2;
			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();
			result = prime * result + pos3;
			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();
			result = prime * result + pos4;
			result = prime * result + key4.hashCode();
			result = prime * result + val4.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Value4Index0Node<?, ?> that = (Value4Index0Node<?, ?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (pos2 != that.pos2) {
				return false;
			}
			if (pos3 != that.pos3) {
				return false;
			}
			if (pos4 != that.pos4) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!key4.equals(that.key4)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
				return false;
			}
			if (!val4.equals(that.val4)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s=%s, %s=%s, %s=%s, %s=%s]", key1, val1, key2, val2, key3,
							val3, key4, val4);
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		V headVal() {
			return val1;
		}

	}

}
