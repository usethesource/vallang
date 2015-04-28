/*******************************************************************************
 * Copyright (c) 2013-2015 CWI
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

import static org.eclipse.imp.pdb.facts.util.AbstractSpecialisedImmutableMap.entryOf;

import java.text.DecimalFormat;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("rawtypes")
public class TrieMap_5Bits_Untyped_Spec0To8<K, V> implements ImmutableMap<K, V> {

	@SuppressWarnings("unchecked")
	private static final TrieMap_5Bits_Untyped_Spec0To8 EMPTY_MAP = new TrieMap_5Bits_Untyped_Spec0To8(
					CompactMapNode.EMPTY_NODE, 0, 0);

	private static final boolean DEBUG = false;

	private final AbstractMapNode<K, V> rootNode;
	private final int hashCode;
	private final int cachedSize;

	TrieMap_5Bits_Untyped_Spec0To8(AbstractMapNode<K, V> rootNode, int hashCode, int cachedSize) {
		this.rootNode = rootNode;
		this.hashCode = hashCode;
		this.cachedSize = cachedSize;
		if (DEBUG) {
			assert checkHashCodeAndSize(hashCode, cachedSize);
		}
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> ImmutableMap<K, V> of() {
		return TrieMap_5Bits_Untyped_Spec0To8.EMPTY_MAP;
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> ImmutableMap<K, V> of(Object... keyValuePairs) {
		if (keyValuePairs.length % 2 != 0) {
			throw new IllegalArgumentException(
							"Length of argument list is uneven: no key/value pairs.");
		}

		ImmutableMap<K, V> result = TrieMap_5Bits_Untyped_Spec0To8.EMPTY_MAP;

		for (int i = 0; i < keyValuePairs.length; i += 2) {
			final K key = (K) keyValuePairs[i];
			final V val = (V) keyValuePairs[i + 1];

			result = result.__put(key, val);
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> TransientMap<K, V> transientOf() {
		return TrieMap_5Bits_Untyped_Spec0To8.EMPTY_MAP.asTransient();
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> TransientMap<K, V> transientOf(Object... keyValuePairs) {
		if (keyValuePairs.length % 2 != 0) {
			throw new IllegalArgumentException(
							"Length of argument list is uneven: no key/value pairs.");
		}

		final TransientMap<K, V> result = TrieMap_5Bits_Untyped_Spec0To8.EMPTY_MAP.asTransient();

		for (int i = 0; i < keyValuePairs.length; i += 2) {
			final K key = (K) keyValuePairs[i];
			final V val = (V) keyValuePairs[i + 1];

			result.__put(key, val);
		}

		return result;
	}

	private boolean checkHashCodeAndSize(final int targetHash, final int targetSize) {
		int hash = 0;
		int size = 0;

		for (Iterator<Map.Entry<K, V>> it = entryIterator(); it.hasNext();) {
			final Map.Entry<K, V> entry = it.next();
			final K key = entry.getKey();
			final V val = entry.getValue();

			hash += key.hashCode() ^ val.hashCode();
			size += 1;
		}

		return hash == targetHash && size == targetSize;
	}

	public static final int transformHashCode(final int hash) {
		return hash;
	}

	public boolean containsKey(final Object o) {
		try {
			@SuppressWarnings("unchecked")
			final K key = (K) o;
			return rootNode.containsKey(key, transformHashCode(key.hashCode()), 0);
		} catch (ClassCastException unused) {
			return false;
		}
	}

	public boolean containsKeyEquivalent(final Object o, final Comparator<Object> cmp) {
		try {
			@SuppressWarnings("unchecked")
			final K key = (K) o;
			return rootNode.containsKey(key, transformHashCode(key.hashCode()), 0, cmp);
		} catch (ClassCastException unused) {
			return false;
		}
	}

	public boolean containsValue(final Object o) {
		for (Iterator<V> iterator = valueIterator(); iterator.hasNext();) {
			if (iterator.next().equals(o)) {
				return true;
			}
		}
		return false;
	}

	public boolean containsValueEquivalent(final Object o, final Comparator<Object> cmp) {
		for (Iterator<V> iterator = valueIterator(); iterator.hasNext();) {
			if (cmp.compare(iterator.next(), o) == 0) {
				return true;
			}
		}
		return false;
	}

	public V get(final Object o) {
		try {
			@SuppressWarnings("unchecked")
			final K key = (K) o;
			final Optional<V> result = rootNode
							.findByKey(key, transformHashCode(key.hashCode()), 0);

			if (result.isPresent()) {
				return result.get();
			} else {
				return null;
			}
		} catch (ClassCastException unused) {
			return null;
		}
	}

	public V getEquivalent(final Object o, final Comparator<Object> cmp) {
		try {
			@SuppressWarnings("unchecked")
			final K key = (K) o;
			final Optional<V> result = rootNode.findByKey(key, transformHashCode(key.hashCode()),
							0, cmp);

			if (result.isPresent()) {
				return result.get();
			} else {
				return null;
			}
		} catch (ClassCastException unused) {
			return null;
		}
	}

	public ImmutableMap<K, V> __put(final K key, final V val) {
		final int keyHash = key.hashCode();
		final MapResult<K, V> details = MapResult.unchanged();

		final CompactMapNode<K, V> newRootNode = rootNode.updated(null, key, val,
						transformHashCode(keyHash), 0, details);

		if (details.isModified()) {
			if (details.hasReplacedValue()) {
				final int valHashOld = details.getReplacedValue().hashCode();
				final int valHashNew = val.hashCode();

				return new TrieMap_5Bits_Untyped_Spec0To8<K, V>(newRootNode, hashCode
								+ ((keyHash ^ valHashNew)) - ((keyHash ^ valHashOld)), cachedSize);
			}

			final int valHash = val.hashCode();
			return new TrieMap_5Bits_Untyped_Spec0To8<K, V>(newRootNode, hashCode
							+ ((keyHash ^ valHash)), cachedSize + 1);
		}

		return this;
	}

	public ImmutableMap<K, V> __putEquivalent(final K key, final V val, final Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final MapResult<K, V> details = MapResult.unchanged();

		final CompactMapNode<K, V> newRootNode = rootNode.updated(null, key, val,
						transformHashCode(keyHash), 0, details, cmp);

		if (details.isModified()) {
			if (details.hasReplacedValue()) {
				final int valHashOld = details.getReplacedValue().hashCode();
				final int valHashNew = val.hashCode();

				return new TrieMap_5Bits_Untyped_Spec0To8<K, V>(newRootNode, hashCode
								+ ((keyHash ^ valHashNew)) - ((keyHash ^ valHashOld)), cachedSize);
			}

			final int valHash = val.hashCode();
			return new TrieMap_5Bits_Untyped_Spec0To8<K, V>(newRootNode, hashCode
							+ ((keyHash ^ valHash)), cachedSize + 1);
		}

		return this;
	}

	public ImmutableMap<K, V> __putAll(final Map<? extends K, ? extends V> map) {
		final TransientMap<K, V> tmpTransient = this.asTransient();
		tmpTransient.__putAll(map);
		return tmpTransient.freeze();
	}

	public ImmutableMap<K, V> __putAllEquivalent(final Map<? extends K, ? extends V> map,
					final Comparator<Object> cmp) {
		final TransientMap<K, V> tmpTransient = this.asTransient();
		tmpTransient.__putAllEquivalent(map, cmp);
		return tmpTransient.freeze();
	}

	public ImmutableMap<K, V> __remove(final K key) {
		final int keyHash = key.hashCode();
		final MapResult<K, V> details = MapResult.unchanged();

		final CompactMapNode<K, V> newRootNode = rootNode.removed(null, key,
						transformHashCode(keyHash), 0, details);

		if (details.isModified()) {
			assert details.hasReplacedValue();
			final int valHash = details.getReplacedValue().hashCode();
			return new TrieMap_5Bits_Untyped_Spec0To8<K, V>(newRootNode, hashCode
							- ((keyHash ^ valHash)), cachedSize - 1);
		}

		return this;
	}

	public ImmutableMap<K, V> __removeEquivalent(final K key, final Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final MapResult<K, V> details = MapResult.unchanged();

		final CompactMapNode<K, V> newRootNode = rootNode.removed(null, key,
						transformHashCode(keyHash), 0, details, cmp);

		if (details.isModified()) {
			assert details.hasReplacedValue();
			final int valHash = details.getReplacedValue().hashCode();
			return new TrieMap_5Bits_Untyped_Spec0To8<K, V>(newRootNode, hashCode
							- ((keyHash ^ valHash)), cachedSize - 1);
		}

		return this;
	}

	public V put(final K key, final V val) {
		throw new UnsupportedOperationException();
	}

	public void putAll(final Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public V remove(final Object key) {
		throw new UnsupportedOperationException();
	}

	public int size() {
		return cachedSize;
	}

	public boolean isEmpty() {
		return cachedSize == 0;
	}

	public Iterator<K> keyIterator() {
		return new MapKeyIterator<>(rootNode);
	}

	public Iterator<V> valueIterator() {
		return new MapValueIterator<>(rootNode);
	}

	public Iterator<Map.Entry<K, V>> entryIterator() {
		return new MapEntryIterator<>(rootNode);
	}

	@Override
	public Set<K> keySet() {
		Set<K> keySet = null;

		if (keySet == null) {
			keySet = new AbstractSet<K>() {
				@Override
				public Iterator<K> iterator() {
					return TrieMap_5Bits_Untyped_Spec0To8.this.keyIterator();
				}

				@Override
				public int size() {
					return TrieMap_5Bits_Untyped_Spec0To8.this.size();
				}

				@Override
				public boolean isEmpty() {
					return TrieMap_5Bits_Untyped_Spec0To8.this.isEmpty();
				}

				@Override
				public void clear() {
					TrieMap_5Bits_Untyped_Spec0To8.this.clear();
				}

				@Override
				public boolean contains(Object k) {
					return TrieMap_5Bits_Untyped_Spec0To8.this.containsKey(k);
				}
			};
		}

		return keySet;
	}

	@Override
	public Collection<V> values() {
		Collection<V> values = null;

		if (values == null) {
			values = new AbstractCollection<V>() {
				@Override
				public Iterator<V> iterator() {
					return TrieMap_5Bits_Untyped_Spec0To8.this.valueIterator();
				}

				@Override
				public int size() {
					return TrieMap_5Bits_Untyped_Spec0To8.this.size();
				}

				@Override
				public boolean isEmpty() {
					return TrieMap_5Bits_Untyped_Spec0To8.this.isEmpty();
				}

				@Override
				public void clear() {
					TrieMap_5Bits_Untyped_Spec0To8.this.clear();
				}

				@Override
				public boolean contains(Object v) {
					return TrieMap_5Bits_Untyped_Spec0To8.this.containsValue(v);
				}
			};
		}

		return values;
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		Set<java.util.Map.Entry<K, V>> entrySet = null;

		if (entrySet == null) {
			entrySet = new AbstractSet<java.util.Map.Entry<K, V>>() {
				@Override
				public Iterator<java.util.Map.Entry<K, V>> iterator() {
					return new Iterator<Map.Entry<K, V>>() {
						private final Iterator<Map.Entry<K, V>> i = entryIterator();

						@Override
						public boolean hasNext() {
							return i.hasNext();
						}

						@Override
						public Map.Entry<K, V> next() {
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
					return TrieMap_5Bits_Untyped_Spec0To8.this.size();
				}

				@Override
				public boolean isEmpty() {
					return TrieMap_5Bits_Untyped_Spec0To8.this.isEmpty();
				}

				@Override
				public void clear() {
					TrieMap_5Bits_Untyped_Spec0To8.this.clear();
				}

				@Override
				public boolean contains(Object k) {
					return TrieMap_5Bits_Untyped_Spec0To8.this.containsKey(k);
				}
			};
		}

		return entrySet;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (other == null) {
			return false;
		}

		if (other instanceof TrieMap_5Bits_Untyped_Spec0To8) {
			TrieMap_5Bits_Untyped_Spec0To8<?, ?> that = (TrieMap_5Bits_Untyped_Spec0To8<?, ?>) other;

			if (this.cachedSize != that.cachedSize) {
				return false;
			}

			if (this.hashCode != that.hashCode) {
				return false;
			}

			return rootNode.equals(that.rootNode);
		} else if (other instanceof Map) {
			Map that = (Map) other;

			if (this.size() != that.size())
				return false;

			for (@SuppressWarnings("unchecked")
			Iterator<Map.Entry> it = that.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = it.next();

				try {
					@SuppressWarnings("unchecked")
					final K key = (K) entry.getKey();
					final Optional<V> result = rootNode.findByKey(key,
									transformHashCode(key.hashCode()), 0);

					if (!result.isPresent()) {
						return false;
					} else {
						@SuppressWarnings("unchecked")
						final V val = (V) entry.getValue();

						if (!result.get().equals(val)) {
							return false;
						}
					}
				} catch (ClassCastException unused) {
					return false;
				}
			}

			return true;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean isTransientSupported() {
		return true;
	}

	@Override
	public TransientMap<K, V> asTransient() {
		return new TransientTrieMap_5Bits_Untyped_Spec0To8<K, V>(this);
	}

	/*
	 * For analysis purposes only.
	 */
	protected AbstractMapNode<K, V> getRootNode() {
		return rootNode;
	}

	/*
	 * For analysis purposes only.
	 */
	protected Iterator<AbstractMapNode<K, V>> nodeIterator() {
		return new TrieMap_5Bits_Untyped_Spec0To8NodeIterator<>(rootNode);
	}

	/*
	 * For analysis purposes only.
	 */
	protected int getNodeCount() {
		final Iterator<AbstractMapNode<K, V>> it = nodeIterator();
		int sumNodes = 0;

		for (; it.hasNext(); it.next()) {
			sumNodes += 1;
		}

		return sumNodes;
	}

	/*
	 * For analysis purposes only. Payload X Node
	 */
	protected int[][] arityCombinationsHistogram() {
		final Iterator<AbstractMapNode<K, V>> it = nodeIterator();
		final int[][] sumArityCombinations = new int[33][33];

		while (it.hasNext()) {
			final AbstractMapNode<K, V> node = it.next();
			sumArityCombinations[node.payloadArity()][node.nodeArity()] += 1;
		}

		return sumArityCombinations;
	}

	/*
	 * For analysis purposes only.
	 */
	protected int[] arityHistogram() {
		final int[][] sumArityCombinations = arityCombinationsHistogram();
		final int[] sumArity = new int[33];

		final int maxArity = 32; // TODO: factor out constant

		for (int j = 0; j <= maxArity; j++) {
			for (int maxRestArity = maxArity - j, k = 0; k <= maxRestArity - j; k++) {
				sumArity[j + k] += sumArityCombinations[j][k];
			}
		}

		return sumArity;
	}

	/*
	 * For analysis purposes only.
	 */
	public void printStatistics() {
		final int[][] sumArityCombinations = arityCombinationsHistogram();
		final int[] sumArity = arityHistogram();
		final int sumNodes = getNodeCount();

		final int[] cumsumArity = new int[33];
		for (int cumsum = 0, i = 0; i < 33; i++) {
			cumsum += sumArity[i];
			cumsumArity[i] = cumsum;
		}

		final float threshhold = 0.01f; // for printing results
		for (int i = 0; i < 33; i++) {
			float arityPercentage = (float) (sumArity[i]) / sumNodes;
			float cumsumArityPercentage = (float) (cumsumArity[i]) / sumNodes;

			if (arityPercentage != 0 && arityPercentage >= threshhold) {
				// details per level
				StringBuilder bldr = new StringBuilder();
				int max = i;
				for (int j = 0; j <= max; j++) {
					for (int k = max - j; k <= max - j; k++) {
						float arityCombinationsPercentage = (float) (sumArityCombinations[j][k])
										/ sumNodes;

						if (arityCombinationsPercentage != 0
										&& arityCombinationsPercentage >= threshhold) {
							bldr.append(String.format("%d/%d: %s, ", j, k, new DecimalFormat(
											"0.00%").format(arityCombinationsPercentage)));
						}
					}
				}
				final String detailPercentages = bldr.toString();

				// overview
				System.out.println(String.format("%2d: %s\t[cumsum = %s]\t%s", i,
								new DecimalFormat("0.00%").format(arityPercentage),
								new DecimalFormat("0.00%").format(cumsumArityPercentage),
								detailPercentages));
			}
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

	static final class MapResult<K, V> {
		private V replacedValue;
		private boolean isModified;
		private boolean isReplaced;

		// update: inserted/removed single element, element count changed
		public void modified() {
			this.isModified = true;
		}

		public void updated(V replacedValue) {
			this.replacedValue = replacedValue;
			this.isModified = true;
			this.isReplaced = true;
		}

		// update: neither element, nor element count changed
		public static <K, V> MapResult<K, V> unchanged() {
			return new MapResult<>();
		}

		private MapResult() {
		}

		public boolean isModified() {
			return isModified;
		}

		public boolean hasReplacedValue() {
			return isReplaced;
		}

		public V getReplacedValue() {
			return replacedValue;
		}
	}

	protected static interface INode<K, V> {
	}

	protected static abstract class AbstractMapNode<K, V> implements INode<K, V> {

		static final int TUPLE_LENGTH = 2;

		abstract boolean containsKey(final K key, final int keyHash, final int shift);

		abstract boolean containsKey(final K key, final int keyHash, final int shift,
						final Comparator<Object> cmp);

		abstract Optional<V> findByKey(final K key, final int keyHash, final int shift);

		abstract Optional<V> findByKey(final K key, final int keyHash, final int shift,
						final Comparator<Object> cmp);

		abstract CompactMapNode<K, V> updated(final AtomicReference<Thread> mutator, final K key,
						final V val, final int keyHash, final int shift,
						final MapResult<K, V> details);

		abstract CompactMapNode<K, V> updated(final AtomicReference<Thread> mutator, final K key,
						final V val, final int keyHash, final int shift,
						final MapResult<K, V> details, final Comparator<Object> cmp);

		abstract CompactMapNode<K, V> removed(final AtomicReference<Thread> mutator, final K key,
						final int keyHash, final int shift, final MapResult<K, V> details);

		abstract CompactMapNode<K, V> removed(final AtomicReference<Thread> mutator, final K key,
						final int keyHash, final int shift, final MapResult<K, V> details,
						final Comparator<Object> cmp);

		static final boolean isAllowedToEdit(AtomicReference<Thread> x, AtomicReference<Thread> y) {
			return x != null && y != null && (x == y || x.get() == y.get());
		}

		abstract boolean hasNodes();

		abstract int nodeArity();

		abstract AbstractMapNode<K, V> getNode(final int index);

		@Deprecated
		Iterator<? extends AbstractMapNode<K, V>> nodeIterator() {
			return new Iterator<AbstractMapNode<K, V>>() {

				int nextIndex = 0;
				final int nodeArity = AbstractMapNode.this.nodeArity();

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				@Override
				public AbstractMapNode<K, V> next() {
					if (!hasNext())
						throw new NoSuchElementException();
					return AbstractMapNode.this.getNode(nextIndex++);
				}

				@Override
				public boolean hasNext() {
					return nextIndex < nodeArity;
				}
			};
		}

		abstract boolean hasPayload();

		abstract int payloadArity();

		abstract K getKey(final int index);

		abstract V getValue(final int index);

		abstract Map.Entry<K, V> getKeyValueEntry(final int index);

		@Deprecated
		abstract boolean hasSlots();

		abstract int slotArity();

		abstract Object getSlot(final int index);

		/**
		 * The arity of this trie node (i.e. number of values and nodes stored
		 * on this level).
		 * 
		 * @return sum of nodes and values stored within
		 */

		int arity() {
			return payloadArity() + nodeArity();
		}

		int size() {
			final Iterator<K> it = new MapKeyIterator<>(this);

			int size = 0;
			while (it.hasNext()) {
				size += 1;
				it.next();
			}

			return size;
		}
	}

	protected static abstract class CompactMapNode<K, V> extends AbstractMapNode<K, V> {

		static final int HASH_CODE_LENGTH = 32;

		static final int BIT_PARTITION_SIZE = 5;
		static final int BIT_PARTITION_MASK = 0b11111;

		static final int mask(final int keyHash, final int shift) {
			return (keyHash >>> shift) & BIT_PARTITION_MASK;
		}

		static final int bitpos(final int mask) {
			return (int) (1 << mask);
		}

		abstract int nodeMap();

		abstract int dataMap();

		static final byte SIZE_EMPTY = 0b00;
		static final byte SIZE_ONE = 0b01;
		static final byte SIZE_MORE_THAN_ONE = 0b10;

		/**
		 * Abstract predicate over a node's size. Value can be either
		 * {@value #SIZE_EMPTY}, {@value #SIZE_ONE}, or
		 * {@value #SIZE_MORE_THAN_ONE}.
		 * 
		 * @return size predicate
		 */
		abstract byte sizePredicate();

		@Override
		abstract CompactMapNode<K, V> getNode(final int index);

		boolean nodeInvariant() {
			boolean inv1 = (size() - payloadArity() >= 2 * (arity() - payloadArity()));
			boolean inv2 = (this.arity() == 0) ? sizePredicate() == SIZE_EMPTY : true;
			boolean inv3 = (this.arity() == 1 && payloadArity() == 1) ? sizePredicate() == SIZE_ONE
							: true;
			boolean inv4 = (this.arity() >= 2) ? sizePredicate() == SIZE_MORE_THAN_ONE : true;

			boolean inv5 = (this.nodeArity() >= 0) && (this.payloadArity() >= 0)
							&& ((this.payloadArity() + this.nodeArity()) == this.arity());

			return inv1 && inv2 && inv3 && inv4 && inv5;
		}

		abstract CompactMapNode<K, V> copyAndSetValue(final AtomicReference<Thread> mutator,
						final int bitpos, final V val);

		abstract CompactMapNode<K, V> copyAndInsertValue(final AtomicReference<Thread> mutator,
						final int bitpos, final K key, final V val);

		abstract CompactMapNode<K, V> copyAndRemoveValue(final AtomicReference<Thread> mutator,
						final int bitpos);

		abstract CompactMapNode<K, V> copyAndSetNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node);

		abstract CompactMapNode<K, V> copyAndMigrateFromInlineToNode(
						final AtomicReference<Thread> mutator, final int bitpos,
						final CompactMapNode<K, V> node);

		abstract CompactMapNode<K, V> copyAndMigrateFromNodeToInline(
						final AtomicReference<Thread> mutator, final int bitpos,
						final CompactMapNode<K, V> node);

		CompactMapNode<K, V> removeInplaceValueAndConvertToSpecializedNode(
						final AtomicReference<Thread> mutator, final int bitpos) {
			throw new UnsupportedOperationException();
		}

		static final <K, V> CompactMapNode<K, V> mergeTwoKeyValPairs(final K key0, final V val0,
						final int keyHash0, final K key1, final V val1, final int keyHash1,
						final int shift) {
			assert !(key0.equals(key1));

			if (shift >= HASH_CODE_LENGTH) {
				// throw new
				// IllegalStateException("Hash collision not yet fixed.");
				return new HashCollisionMapNode_5Bits_Untyped_Spec0To8<>(keyHash0,
								(K[]) new Object[] { key0, key1 },
								(V[]) new Object[] { val0, val1 });
			}

			final int mask0 = mask(keyHash0, shift);
			final int mask1 = mask(keyHash1, shift);

			if (mask0 != mask1) {
				// both nodes fit on same level
				final int dataMap = (int) (bitpos(mask0) | bitpos(mask1));

				if (mask0 < mask1) {
					return nodeOf(null, (int) 0, dataMap, key0, val0, key1, val1);
				} else {
					return nodeOf(null, (int) 0, dataMap, key1, val1, key0, val0);
				}
			} else {
				final CompactMapNode<K, V> node = mergeTwoKeyValPairs(key0, val0, keyHash0, key1,
								val1, keyHash1, shift + BIT_PARTITION_SIZE);
				// values fit on next level

				final int nodeMap = bitpos(mask0);
				return nodeOf(null, nodeMap, (int) 0, node);
			}
		}

		static final CompactMapNode EMPTY_NODE;

		static {

			EMPTY_NODE = new Map0To0Node_5Bits_Untyped_Spec0To8<>(null, (int) 0, (int) 0);

		};

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object[] nodes) {
			return new BitmapIndexedMapNode<>(mutator, nodeMap, dataMap, nodes);
		}

		@SuppressWarnings("unchecked")
		static final <K, V> CompactMapNode<K, V> nodeOf(AtomicReference<Thread> mutator) {
			return EMPTY_NODE;
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap) {
			return EMPTY_NODE;
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0) {
			return new Map0To1Node_5Bits_Untyped_Spec0To8<>(mutator, nodeMap, dataMap, slot0);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0, final Object slot1) {
			return new Map0To2Node_5Bits_Untyped_Spec0To8<>(mutator, nodeMap, dataMap, slot0, slot1);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2) {
			return new Map0To3Node_5Bits_Untyped_Spec0To8<>(mutator, nodeMap, dataMap, slot0,
							slot1, slot2);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3) {
			return new Map0To4Node_5Bits_Untyped_Spec0To8<>(mutator, nodeMap, dataMap, slot0,
							slot1, slot2, slot3);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4) {
			return new Map0To5Node_5Bits_Untyped_Spec0To8<>(mutator, nodeMap, dataMap, slot0,
							slot1, slot2, slot3, slot4);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5) {
			return new Map0To6Node_5Bits_Untyped_Spec0To8<>(mutator, nodeMap, dataMap, slot0,
							slot1, slot2, slot3, slot4, slot5);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6) {
			return new Map0To7Node_5Bits_Untyped_Spec0To8<>(mutator, nodeMap, dataMap, slot0,
							slot1, slot2, slot3, slot4, slot5, slot6);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6,
						final Object slot7) {
			return new Map0To8Node_5Bits_Untyped_Spec0To8<>(mutator, nodeMap, dataMap, slot0,
							slot1, slot2, slot3, slot4, slot5, slot6, slot7);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6,
						final Object slot7, final Object slot8) {
			return new Map0To9Node_5Bits_Untyped_Spec0To8<>(mutator, nodeMap, dataMap, slot0,
							slot1, slot2, slot3, slot4, slot5, slot6, slot7, slot8);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6,
						final Object slot7, final Object slot8, final Object slot9) {
			return new Map0To10Node_5Bits_Untyped_Spec0To8<>(mutator, nodeMap, dataMap, slot0,
							slot1, slot2, slot3, slot4, slot5, slot6, slot7, slot8, slot9);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6,
						final Object slot7, final Object slot8, final Object slot9,
						final Object slot10) {
			return new Map0To11Node_5Bits_Untyped_Spec0To8<>(mutator, nodeMap, dataMap, slot0,
							slot1, slot2, slot3, slot4, slot5, slot6, slot7, slot8, slot9, slot10);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6,
						final Object slot7, final Object slot8, final Object slot9,
						final Object slot10, final Object slot11) {
			return new Map0To12Node_5Bits_Untyped_Spec0To8<>(mutator, nodeMap, dataMap, slot0,
							slot1, slot2, slot3, slot4, slot5, slot6, slot7, slot8, slot9, slot10,
							slot11);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6,
						final Object slot7, final Object slot8, final Object slot9,
						final Object slot10, final Object slot11, final Object slot12) {
			return new Map0To13Node_5Bits_Untyped_Spec0To8<>(mutator, nodeMap, dataMap, slot0,
							slot1, slot2, slot3, slot4, slot5, slot6, slot7, slot8, slot9, slot10,
							slot11, slot12);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6,
						final Object slot7, final Object slot8, final Object slot9,
						final Object slot10, final Object slot11, final Object slot12,
						final Object slot13) {
			return new Map0To14Node_5Bits_Untyped_Spec0To8<>(mutator, nodeMap, dataMap, slot0,
							slot1, slot2, slot3, slot4, slot5, slot6, slot7, slot8, slot9, slot10,
							slot11, slot12, slot13);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6,
						final Object slot7, final Object slot8, final Object slot9,
						final Object slot10, final Object slot11, final Object slot12,
						final Object slot13, final Object slot14) {
			return new Map0To15Node_5Bits_Untyped_Spec0To8<>(mutator, nodeMap, dataMap, slot0,
							slot1, slot2, slot3, slot4, slot5, slot6, slot7, slot8, slot9, slot10,
							slot11, slot12, slot13, slot14);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6,
						final Object slot7, final Object slot8, final Object slot9,
						final Object slot10, final Object slot11, final Object slot12,
						final Object slot13, final Object slot14, final Object slot15) {
			return new Map0To16Node_5Bits_Untyped_Spec0To8<>(mutator, nodeMap, dataMap, slot0,
							slot1, slot2, slot3, slot4, slot5, slot6, slot7, slot8, slot9, slot10,
							slot11, slot12, slot13, slot14, slot15);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6,
						final Object slot7, final Object slot8, final Object slot9,
						final Object slot10, final Object slot11, final Object slot12,
						final Object slot13, final Object slot14, final Object slot15,
						final Object slot16) {
			return nodeOf(mutator, nodeMap, dataMap, new Object[] { slot0, slot1, slot2, slot3,
							slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
							slot13, slot14, slot15, slot16 });
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6,
						final Object slot7, final Object slot8, final Object slot9,
						final Object slot10, final Object slot11, final Object slot12,
						final Object slot13, final Object slot14, final Object slot15,
						final Object slot16, final Object slot17) {
			return nodeOf(mutator, nodeMap, dataMap, new Object[] { slot0, slot1, slot2, slot3,
							slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
							slot13, slot14, slot15, slot16, slot17 });
		}

		static final int index(final int bitmap, final int bitpos) {
			return java.lang.Integer.bitCount(bitmap & (bitpos - 1));
		}

		static final int index(final int bitmap, final int mask, final int bitpos) {
			return (bitmap == -1) ? mask : index(bitmap, bitpos);
		}

		int dataIndex(final int bitpos) {
			return java.lang.Integer.bitCount(dataMap() & (bitpos - 1));
		}

		int nodeIndex(final int bitpos) {
			return java.lang.Integer.bitCount(nodeMap() & (bitpos - 1));
		}

		CompactMapNode<K, V> nodeAt(final int bitpos) {
			return getNode(nodeIndex(bitpos));
		}

		boolean containsKey(final K key, final int keyHash, final int shift) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			final int dataMap = dataMap();
			if ((dataMap & bitpos) != 0) {
				final int index = index(dataMap, mask, bitpos);
				return getKey(index).equals(key);
			}

			final int nodeMap = nodeMap();
			if ((nodeMap & bitpos) != 0) {
				final int index = index(nodeMap, mask, bitpos);
				return getNode(index).containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			}

			return false;
		}

		boolean containsKey(final K key, final int keyHash, final int shift,
						final Comparator<Object> cmp) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			final int dataMap = dataMap();
			if ((dataMap & bitpos) != 0) {
				final int index = index(dataMap, mask, bitpos);
				return cmp.compare(getKey(index), key) == 0;
			}

			final int nodeMap = nodeMap();
			if ((nodeMap & bitpos) != 0) {
				final int index = index(nodeMap, mask, bitpos);
				return getNode(index).containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		Optional<V> findByKey(final K key, final int keyHash, final int shift) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int index = dataIndex(bitpos);
				if (getKey(index).equals(key)) {
					final V result = getValue(index);

					return Optional.of(result);
				}

				return Optional.empty();
			}

			if ((nodeMap() & bitpos) != 0) { // node (not value)
				final AbstractMapNode<K, V> subNode = nodeAt(bitpos);

				return subNode.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			}

			return Optional.empty();
		}

		Optional<V> findByKey(final K key, final int keyHash, final int shift,
						final Comparator<Object> cmp) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int index = dataIndex(bitpos);
				if (cmp.compare(getKey(index), key) == 0) {
					final V result = getValue(index);

					return Optional.of(result);
				}

				return Optional.empty();
			}

			if ((nodeMap() & bitpos) != 0) { // node (not value)
				final AbstractMapNode<K, V> subNode = nodeAt(bitpos);

				return subNode.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		CompactMapNode<K, V> updated(final AtomicReference<Thread> mutator, final K key,
						final V val, final int keyHash, final int shift,
						final MapResult<K, V> details) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int dataIndex = dataIndex(bitpos);
				final K currentKey = getKey(dataIndex);

				if (currentKey.equals(key)) {
					final V currentVal = getValue(dataIndex);

					if (currentVal.equals(val)) {
						return this;
					} else {
						// update mapping
						details.updated(currentVal);
						return copyAndSetValue(mutator, bitpos, val);
					}
				} else {
					final V currentVal = getValue(dataIndex);
					final CompactMapNode<K, V> subNodeNew = mergeTwoKeyValPairs(currentKey,
									currentVal, transformHashCode(currentKey.hashCode()), key, val,
									keyHash, shift + BIT_PARTITION_SIZE);

					details.modified();
					return copyAndMigrateFromInlineToNode(mutator, bitpos, subNodeNew);
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactMapNode<K, V> subNode = nodeAt(bitpos);
				final CompactMapNode<K, V> subNodeNew = subNode.updated(mutator, key, val, keyHash,
								shift + BIT_PARTITION_SIZE, details);

				if (details.isModified()) {
					return copyAndSetNode(mutator, bitpos, subNodeNew);
				} else {
					return this;
				}
			} else {
				// no value
				details.modified();
				return copyAndInsertValue(mutator, bitpos, key, val);
			}
		}

		CompactMapNode<K, V> updated(final AtomicReference<Thread> mutator, final K key,
						final V val, final int keyHash, final int shift,
						final MapResult<K, V> details, final Comparator<Object> cmp) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int dataIndex = dataIndex(bitpos);
				final K currentKey = getKey(dataIndex);

				if (cmp.compare(currentKey, key) == 0) {
					final V currentVal = getValue(dataIndex);

					if (cmp.compare(currentVal, val) == 0) {
						return this;
					} else {
						// update mapping
						details.updated(currentVal);
						return copyAndSetValue(mutator, bitpos, val);
					}
				} else {
					final V currentVal = getValue(dataIndex);
					final CompactMapNode<K, V> subNodeNew = mergeTwoKeyValPairs(currentKey,
									currentVal, transformHashCode(currentKey.hashCode()), key, val,
									keyHash, shift + BIT_PARTITION_SIZE);

					details.modified();
					return copyAndMigrateFromInlineToNode(mutator, bitpos, subNodeNew);
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactMapNode<K, V> subNode = nodeAt(bitpos);
				final CompactMapNode<K, V> subNodeNew = subNode.updated(mutator, key, val, keyHash,
								shift + BIT_PARTITION_SIZE, details, cmp);

				if (details.isModified()) {
					return copyAndSetNode(mutator, bitpos, subNodeNew);
				} else {
					return this;
				}
			} else {
				// no value
				details.modified();
				return copyAndInsertValue(mutator, bitpos, key, val);
			}
		}

		CompactMapNode<K, V> removed(final AtomicReference<Thread> mutator, final K key,
						final int keyHash, final int shift, final MapResult<K, V> details) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int dataIndex = dataIndex(bitpos);

				if (getKey(dataIndex).equals(key)) {
					final V currentVal = getValue(dataIndex);
					details.updated(currentVal);

					if (this.payloadArity() == 2 && this.nodeArity() == 0) {
						/*
						 * Create new node with remaining pair. The new node
						 * will a) either become the new root returned, or b)
						 * unwrapped and inlined during returning.
						 */
						final int newDataMap = (shift == 0) ? (int) (dataMap() ^ bitpos)
										: bitpos(mask(keyHash, 0));

						if (dataIndex == 0) {
							return CompactMapNode.<K, V> nodeOf(mutator, (int) 0, newDataMap,
											getKey(1), getValue(1));
						} else {
							return CompactMapNode.<K, V> nodeOf(mutator, (int) 0, newDataMap,
											getKey(0), getValue(0));
						}
					} else if (this.arity() == 9) {
						return removeInplaceValueAndConvertToSpecializedNode(mutator, bitpos);
					} else {
						return copyAndRemoveValue(mutator, bitpos);
					}
				} else {
					return this;
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactMapNode<K, V> subNode = nodeAt(bitpos);
				final CompactMapNode<K, V> subNodeNew = subNode.removed(mutator, key, keyHash,
								shift + BIT_PARTITION_SIZE, details);

				if (!details.isModified()) {
					return this;
				}

				switch (subNodeNew.sizePredicate()) {
				case 0: {
					throw new IllegalStateException("Sub-node must have at least one element.");
				}
				case 1: {
					// inline value (move to front)
					details.modified();
					return copyAndMigrateFromNodeToInline(mutator, bitpos, subNodeNew);
				}
				default: {
					// modify current node (set replacement node)
					return copyAndSetNode(mutator, bitpos, subNodeNew);
				}
				}
			}

			return this;
		}

		CompactMapNode<K, V> removed(final AtomicReference<Thread> mutator, final K key,
						final int keyHash, final int shift, final MapResult<K, V> details,
						final Comparator<Object> cmp) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int dataIndex = dataIndex(bitpos);

				if (cmp.compare(getKey(dataIndex), key) == 0) {
					final V currentVal = getValue(dataIndex);
					details.updated(currentVal);

					if (this.payloadArity() == 2 && this.nodeArity() == 0) {
						/*
						 * Create new node with remaining pair. The new node
						 * will a) either become the new root returned, or b)
						 * unwrapped and inlined during returning.
						 */
						final int newDataMap = (shift == 0) ? (int) (dataMap() ^ bitpos)
										: bitpos(mask(keyHash, 0));

						if (dataIndex == 0) {
							return CompactMapNode.<K, V> nodeOf(mutator, (int) 0, newDataMap,
											getKey(1), getValue(1));
						} else {
							return CompactMapNode.<K, V> nodeOf(mutator, (int) 0, newDataMap,
											getKey(0), getValue(0));
						}
					} else if (this.arity() == 9) {
						return removeInplaceValueAndConvertToSpecializedNode(mutator, bitpos);
					} else {
						return copyAndRemoveValue(mutator, bitpos);
					}
				} else {
					return this;
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactMapNode<K, V> subNode = nodeAt(bitpos);
				final CompactMapNode<K, V> subNodeNew = subNode.removed(mutator, key, keyHash,
								shift + BIT_PARTITION_SIZE, details, cmp);

				if (!details.isModified()) {
					return this;
				}

				switch (subNodeNew.sizePredicate()) {
				case 0: {
					throw new IllegalStateException("Sub-node must have at least one element.");
				}
				case 1: {
					// inline value (move to front)
					details.modified();
					return copyAndMigrateFromNodeToInline(mutator, bitpos, subNodeNew);
				}
				default: {
					// modify current node (set replacement node)
					return copyAndSetNode(mutator, bitpos, subNodeNew);
				}
				}
			}

			return this;
		}

		/**
		 * @return 0 <= mask <= 2^BIT_PARTITION_SIZE - 1
		 */
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

				map = (int) (map >> 1);
				mask += 1;
			}

			assert cnt1 != i_th;
			throw new RuntimeException("Called with invalid arguments.");
		}

		@Override
		public String toString() {
			final StringBuilder bldr = new StringBuilder();
			bldr.append('[');

			for (byte i = 0; i < payloadArity(); i++) {
				final byte pos = recoverMask(dataMap(), (byte) (i + 1));
				bldr.append(String.format("@%d: %s", pos, getKey(i), getValue(i)));

				if (!((i + 1) == payloadArity())) {
					bldr.append(", ");
				}
			}

			if (payloadArity() > 0 && nodeArity() > 0) {
				bldr.append(", ");
			}

			for (byte i = 0; i < nodeArity(); i++) {
				final byte pos = recoverMask(nodeMap(), (byte) (i + 1));
				bldr.append(String.format("@%d: %s", pos, getNode(i)));

				if (!((i + 1) == nodeArity())) {
					bldr.append(", ");
				}
			}

			bldr.append(']');
			return bldr.toString();
		}

	}

	protected static abstract class CompactMixedMapNode<K, V> extends CompactMapNode<K, V> {

		private final int nodeMap;
		private final int dataMap;

		CompactMixedMapNode(final AtomicReference<Thread> mutator, final int nodeMap,
						final int dataMap) {
			this.nodeMap = nodeMap;
			this.dataMap = dataMap;
		}

		@Override
		public int nodeMap() {
			return nodeMap;
		}

		@Override
		public int dataMap() {
			return dataMap;
		}

	}

	protected static abstract class CompactNodesOnlyMapNode<K, V> extends CompactMapNode<K, V> {

		private final int nodeMap;

		CompactNodesOnlyMapNode(final AtomicReference<Thread> mutator, final int nodeMap,
						final int dataMap) {
			this.nodeMap = nodeMap;
		}

		@Override
		public int nodeMap() {
			return nodeMap;
		}

		@Override
		public int dataMap() {
			return 0;
		}

	}

	protected static abstract class CompactValuesOnlyMapNode<K, V> extends CompactMapNode<K, V> {

		private final int dataMap;

		CompactValuesOnlyMapNode(final AtomicReference<Thread> mutator, final int nodeMap,
						final int dataMap) {
			this.dataMap = dataMap;
		}

		@Override
		public int nodeMap() {
			return 0;
		}

		@Override
		public int dataMap() {
			return dataMap;
		}

	}

	protected static abstract class CompactEmptyMapNode<K, V> extends CompactMapNode<K, V> {

		CompactEmptyMapNode(final AtomicReference<Thread> mutator, final int nodeMap,
						final int dataMap) {
		}

		@Override
		public int nodeMap() {
			return 0;
		}

		@Override
		public int dataMap() {
			return 0;
		}

	}

	private static final class BitmapIndexedMapNode<K, V> extends CompactMixedMapNode<K, V> {

		final AtomicReference<Thread> mutator;
		final Object[] nodes;

		private BitmapIndexedMapNode(final AtomicReference<Thread> mutator, final int nodeMap,
						final int dataMap, final Object[] nodes) {
			super(mutator, nodeMap, dataMap);

			this.mutator = mutator;
			this.nodes = nodes;

			if (DEBUG) {

				assert (TUPLE_LENGTH * java.lang.Integer.bitCount(dataMap)
								+ java.lang.Integer.bitCount(nodeMap) == nodes.length);

				for (int i = 0; i < TUPLE_LENGTH * payloadArity(); i++) {
					assert ((nodes[i] instanceof CompactMapNode) == false);
				}
				for (int i = TUPLE_LENGTH * payloadArity(); i < nodes.length; i++) {
					assert ((nodes[i] instanceof CompactMapNode) == true);
				}
			}

			assert arity() > 8;
			assert nodeInvariant();
		}

		@SuppressWarnings("unchecked")
		@Override
		K getKey(final int index) {
			return (K) nodes[TUPLE_LENGTH * index];
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(final int index) {
			return (V) nodes[TUPLE_LENGTH * index + 1];
		}

		Map.Entry<K, V> getKeyValueEntry(final int index) {
			return entryOf((K) nodes[TUPLE_LENGTH * index], (V) nodes[TUPLE_LENGTH * index + 1]);
		}

		@SuppressWarnings("unchecked")
		@Override
		CompactMapNode<K, V> getNode(final int index) {
			return (CompactMapNode<K, V>) nodes[nodes.length - 1 - index];
		}

		@Override
		boolean hasPayload() {
			return dataMap() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount(dataMap());
		}

		@Override
		boolean hasNodes() {
			return nodeMap() != 0;
		}

		@Override
		int nodeArity() {
			return java.lang.Integer.bitCount(nodeMap());
		}

		@Override
		Object getSlot(final int index) {
			return nodes[index];
		}

		@Override
		boolean hasSlots() {
			return nodes.length != 0;
		}

		@Override
		int slotArity() {
			return nodes.length;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 0;
			result = prime * result + ((int) dataMap());
			result = prime * result + ((int) dataMap());
			result = prime * result + Arrays.hashCode(nodes);
			return result;
		}

		@Override
		public boolean equals(final Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			BitmapIndexedMapNode<?, ?> that = (BitmapIndexedMapNode<?, ?>) other;
			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}
			if (!Arrays.equals(nodes, that.nodes)) {
				return false;
			}
			return true;
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(final AtomicReference<Thread> mutator,
						final int bitpos, final V val) {
			final int idx = TUPLE_LENGTH * dataIndex(bitpos) + 1;

			if (isAllowedToEdit(this.mutator, mutator)) {
				// no copying if already editable
				this.nodes[idx] = val;
				return this;
			} else {
				final Object[] src = this.nodes;
				final Object[] dst = (Object[]) new Object[src.length];

				// copy 'src' and set 1 element(s) at position 'idx'
				System.arraycopy(src, 0, dst, 0, src.length);
				dst[idx + 0] = val;

				return nodeOf(mutator, nodeMap(), dataMap(), dst);
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {

			final int idx = this.nodes.length - 1 - nodeIndex(bitpos);

			if (isAllowedToEdit(this.mutator, mutator)) {
				// no copying if already editable
				this.nodes[idx] = node;
				return this;
			} else {
				final Object[] src = this.nodes;
				final Object[] dst = (Object[]) new Object[src.length];

				// copy 'src' and set 1 element(s) at position 'idx'
				System.arraycopy(src, 0, dst, 0, src.length);
				dst[idx + 0] = node;

				return nodeOf(mutator, nodeMap(), dataMap(), dst);
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(final AtomicReference<Thread> mutator,
						final int bitpos, final K key, final V val) {
			final int idx = TUPLE_LENGTH * dataIndex(bitpos);

			final Object[] src = this.nodes;
			final Object[] dst = (Object[]) new Object[src.length + 2];

			// copy 'src' and insert 2 element(s) at position 'idx'
			System.arraycopy(src, 0, dst, 0, idx);
			dst[idx + 0] = key;
			dst[idx + 1] = val;
			System.arraycopy(src, idx, dst, idx + 2, src.length - idx);

			return nodeOf(mutator, nodeMap(), (int) (dataMap() | bitpos), dst);
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(final AtomicReference<Thread> mutator,
						final int bitpos) {
			final int idx = TUPLE_LENGTH * dataIndex(bitpos);

			final Object[] src = this.nodes;
			final Object[] dst = (Object[]) new Object[src.length - 2];

			// copy 'src' and remove 2 element(s) at position 'idx'
			System.arraycopy(src, 0, dst, 0, idx);
			System.arraycopy(src, idx + 2, dst, idx, src.length - idx - 2);

			return nodeOf(mutator, nodeMap(), (int) (dataMap() ^ bitpos), dst);
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {

			final int idxOld = TUPLE_LENGTH * dataIndex(bitpos);
			final int idxNew = this.nodes.length - TUPLE_LENGTH - nodeIndex(bitpos);

			final Object[] src = this.nodes;
			final Object[] dst = new Object[src.length - 2 + 1];

			// copy 'src' and remove 2 element(s) at position 'idxOld' and
			// insert 1 element(s) at position 'idxNew' (TODO: carefully test)
			assert idxOld <= idxNew;
			System.arraycopy(src, 0, dst, 0, idxOld);
			System.arraycopy(src, idxOld + 2, dst, idxOld, idxNew - idxOld);
			dst[idxNew + 0] = node;
			System.arraycopy(src, idxNew + 2, dst, idxNew + 1, src.length - idxNew - 2);

			return nodeOf(mutator, (int) (nodeMap() | bitpos), (int) (dataMap() ^ bitpos), dst);
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {

			final int idxOld = this.nodes.length - 1 - nodeIndex(bitpos);
			final int idxNew = dataIndex(bitpos);

			final Object[] src = this.nodes;
			final Object[] dst = new Object[src.length - 1 + 2];

			// copy 'src' and remove 1 element(s) at position 'idxOld' and
			// insert 2 element(s) at position 'idxNew' (TODO: carefully test)
			assert idxOld >= idxNew;
			System.arraycopy(src, 0, dst, 0, idxNew);
			dst[idxNew + 0] = node.getKey(0);
			dst[idxNew + 1] = node.getValue(0);
			System.arraycopy(src, idxNew, dst, idxNew + 2, idxOld - idxNew);
			System.arraycopy(src, idxOld + 1, dst, idxOld + 2, src.length - idxOld - 1);

			return nodeOf(mutator, (int) (nodeMap() ^ bitpos), (int) (dataMap() | bitpos), dst);
		}

		@Override
		CompactMapNode<K, V> removeInplaceValueAndConvertToSpecializedNode(
						final AtomicReference<Thread> mutator, final int bitpos) {
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (payloadArity()) { // 0 <= payloadArity <= 9 // or ts.nMax
			case 1: {

				switch (valIndex) {
				case 0: {

					break;
				}
				default:
					throw new IllegalStateException("Index out of range.");
				}

				final CompactMapNode<K, V> node1 = getNode(0);
				final CompactMapNode<K, V> node2 = getNode(1);
				final CompactMapNode<K, V> node3 = getNode(2);
				final CompactMapNode<K, V> node4 = getNode(3);
				final CompactMapNode<K, V> node5 = getNode(4);
				final CompactMapNode<K, V> node6 = getNode(5);
				final CompactMapNode<K, V> node7 = getNode(6);
				final CompactMapNode<K, V> node8 = getNode(7);

				return nodeOf(mutator, nodeMap, dataMap, node1, node2, node3, node4, node5, node6,
								node7, node8);

			}
			case 2: {
				K key1;
				V val1;

				switch (valIndex) {
				case 0: {

					key1 = getKey(1);
					val1 = getValue(1);
					break;
				}
				case 1: {

					key1 = getKey(0);
					val1 = getValue(0);
					break;
				}
				default:
					throw new IllegalStateException("Index out of range.");
				}

				final CompactMapNode<K, V> node1 = getNode(0);
				final CompactMapNode<K, V> node2 = getNode(1);
				final CompactMapNode<K, V> node3 = getNode(2);
				final CompactMapNode<K, V> node4 = getNode(3);
				final CompactMapNode<K, V> node5 = getNode(4);
				final CompactMapNode<K, V> node6 = getNode(5);
				final CompactMapNode<K, V> node7 = getNode(6);

				return nodeOf(mutator, nodeMap, dataMap, key1, val1, node1, node2, node3, node4,
								node5, node6, node7);

			}
			case 3: {
				K key1;
				V val1;
				K key2;
				V val2;

				switch (valIndex) {
				case 0: {

					key1 = getKey(1);
					val1 = getValue(1);
					key2 = getKey(2);
					val2 = getValue(2);
					break;
				}
				case 1: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(2);
					val2 = getValue(2);
					break;
				}
				case 2: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					break;
				}
				default:
					throw new IllegalStateException("Index out of range.");
				}

				final CompactMapNode<K, V> node1 = getNode(0);
				final CompactMapNode<K, V> node2 = getNode(1);
				final CompactMapNode<K, V> node3 = getNode(2);
				final CompactMapNode<K, V> node4 = getNode(3);
				final CompactMapNode<K, V> node5 = getNode(4);
				final CompactMapNode<K, V> node6 = getNode(5);

				return nodeOf(mutator, nodeMap, dataMap, key1, val1, key2, val2, node1, node2,
								node3, node4, node5, node6);

			}
			case 4: {
				K key1;
				V val1;
				K key2;
				V val2;
				K key3;
				V val3;

				switch (valIndex) {
				case 0: {

					key1 = getKey(1);
					val1 = getValue(1);
					key2 = getKey(2);
					val2 = getValue(2);
					key3 = getKey(3);
					val3 = getValue(3);
					break;
				}
				case 1: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(2);
					val2 = getValue(2);
					key3 = getKey(3);
					val3 = getValue(3);
					break;
				}
				case 2: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(3);
					val3 = getValue(3);
					break;
				}
				case 3: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					break;
				}
				default:
					throw new IllegalStateException("Index out of range.");
				}

				final CompactMapNode<K, V> node1 = getNode(0);
				final CompactMapNode<K, V> node2 = getNode(1);
				final CompactMapNode<K, V> node3 = getNode(2);
				final CompactMapNode<K, V> node4 = getNode(3);
				final CompactMapNode<K, V> node5 = getNode(4);

				return nodeOf(mutator, nodeMap, dataMap, key1, val1, key2, val2, key3, val3, node1,
								node2, node3, node4, node5);

			}
			case 5: {
				K key1;
				V val1;
				K key2;
				V val2;
				K key3;
				V val3;
				K key4;
				V val4;

				switch (valIndex) {
				case 0: {

					key1 = getKey(1);
					val1 = getValue(1);
					key2 = getKey(2);
					val2 = getValue(2);
					key3 = getKey(3);
					val3 = getValue(3);
					key4 = getKey(4);
					val4 = getValue(4);
					break;
				}
				case 1: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(2);
					val2 = getValue(2);
					key3 = getKey(3);
					val3 = getValue(3);
					key4 = getKey(4);
					val4 = getValue(4);
					break;
				}
				case 2: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(3);
					val3 = getValue(3);
					key4 = getKey(4);
					val4 = getValue(4);
					break;
				}
				case 3: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					key4 = getKey(4);
					val4 = getValue(4);
					break;
				}
				case 4: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					key4 = getKey(3);
					val4 = getValue(3);
					break;
				}
				default:
					throw new IllegalStateException("Index out of range.");
				}

				final CompactMapNode<K, V> node1 = getNode(0);
				final CompactMapNode<K, V> node2 = getNode(1);
				final CompactMapNode<K, V> node3 = getNode(2);
				final CompactMapNode<K, V> node4 = getNode(3);

				return nodeOf(mutator, nodeMap, dataMap, key1, val1, key2, val2, key3, val3, key4,
								val4, node1, node2, node3, node4);

			}
			case 6: {
				K key1;
				V val1;
				K key2;
				V val2;
				K key3;
				V val3;
				K key4;
				V val4;
				K key5;
				V val5;

				switch (valIndex) {
				case 0: {

					key1 = getKey(1);
					val1 = getValue(1);
					key2 = getKey(2);
					val2 = getValue(2);
					key3 = getKey(3);
					val3 = getValue(3);
					key4 = getKey(4);
					val4 = getValue(4);
					key5 = getKey(5);
					val5 = getValue(5);
					break;
				}
				case 1: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(2);
					val2 = getValue(2);
					key3 = getKey(3);
					val3 = getValue(3);
					key4 = getKey(4);
					val4 = getValue(4);
					key5 = getKey(5);
					val5 = getValue(5);
					break;
				}
				case 2: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(3);
					val3 = getValue(3);
					key4 = getKey(4);
					val4 = getValue(4);
					key5 = getKey(5);
					val5 = getValue(5);
					break;
				}
				case 3: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					key4 = getKey(4);
					val4 = getValue(4);
					key5 = getKey(5);
					val5 = getValue(5);
					break;
				}
				case 4: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					key4 = getKey(3);
					val4 = getValue(3);
					key5 = getKey(5);
					val5 = getValue(5);
					break;
				}
				case 5: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					key4 = getKey(3);
					val4 = getValue(3);
					key5 = getKey(4);
					val5 = getValue(4);
					break;
				}
				default:
					throw new IllegalStateException("Index out of range.");
				}

				final CompactMapNode<K, V> node1 = getNode(0);
				final CompactMapNode<K, V> node2 = getNode(1);
				final CompactMapNode<K, V> node3 = getNode(2);

				return nodeOf(mutator, nodeMap, dataMap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, node1, node2, node3);

			}
			case 7: {
				K key1;
				V val1;
				K key2;
				V val2;
				K key3;
				V val3;
				K key4;
				V val4;
				K key5;
				V val5;
				K key6;
				V val6;

				switch (valIndex) {
				case 0: {

					key1 = getKey(1);
					val1 = getValue(1);
					key2 = getKey(2);
					val2 = getValue(2);
					key3 = getKey(3);
					val3 = getValue(3);
					key4 = getKey(4);
					val4 = getValue(4);
					key5 = getKey(5);
					val5 = getValue(5);
					key6 = getKey(6);
					val6 = getValue(6);
					break;
				}
				case 1: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(2);
					val2 = getValue(2);
					key3 = getKey(3);
					val3 = getValue(3);
					key4 = getKey(4);
					val4 = getValue(4);
					key5 = getKey(5);
					val5 = getValue(5);
					key6 = getKey(6);
					val6 = getValue(6);
					break;
				}
				case 2: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(3);
					val3 = getValue(3);
					key4 = getKey(4);
					val4 = getValue(4);
					key5 = getKey(5);
					val5 = getValue(5);
					key6 = getKey(6);
					val6 = getValue(6);
					break;
				}
				case 3: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					key4 = getKey(4);
					val4 = getValue(4);
					key5 = getKey(5);
					val5 = getValue(5);
					key6 = getKey(6);
					val6 = getValue(6);
					break;
				}
				case 4: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					key4 = getKey(3);
					val4 = getValue(3);
					key5 = getKey(5);
					val5 = getValue(5);
					key6 = getKey(6);
					val6 = getValue(6);
					break;
				}
				case 5: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					key4 = getKey(3);
					val4 = getValue(3);
					key5 = getKey(4);
					val5 = getValue(4);
					key6 = getKey(6);
					val6 = getValue(6);
					break;
				}
				case 6: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					key4 = getKey(3);
					val4 = getValue(3);
					key5 = getKey(4);
					val5 = getValue(4);
					key6 = getKey(5);
					val6 = getValue(5);
					break;
				}
				default:
					throw new IllegalStateException("Index out of range.");
				}

				final CompactMapNode<K, V> node1 = getNode(0);
				final CompactMapNode<K, V> node2 = getNode(1);

				return nodeOf(mutator, nodeMap, dataMap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, node1, node2);

			}
			case 8: {
				K key1;
				V val1;
				K key2;
				V val2;
				K key3;
				V val3;
				K key4;
				V val4;
				K key5;
				V val5;
				K key6;
				V val6;
				K key7;
				V val7;

				switch (valIndex) {
				case 0: {

					key1 = getKey(1);
					val1 = getValue(1);
					key2 = getKey(2);
					val2 = getValue(2);
					key3 = getKey(3);
					val3 = getValue(3);
					key4 = getKey(4);
					val4 = getValue(4);
					key5 = getKey(5);
					val5 = getValue(5);
					key6 = getKey(6);
					val6 = getValue(6);
					key7 = getKey(7);
					val7 = getValue(7);
					break;
				}
				case 1: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(2);
					val2 = getValue(2);
					key3 = getKey(3);
					val3 = getValue(3);
					key4 = getKey(4);
					val4 = getValue(4);
					key5 = getKey(5);
					val5 = getValue(5);
					key6 = getKey(6);
					val6 = getValue(6);
					key7 = getKey(7);
					val7 = getValue(7);
					break;
				}
				case 2: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(3);
					val3 = getValue(3);
					key4 = getKey(4);
					val4 = getValue(4);
					key5 = getKey(5);
					val5 = getValue(5);
					key6 = getKey(6);
					val6 = getValue(6);
					key7 = getKey(7);
					val7 = getValue(7);
					break;
				}
				case 3: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					key4 = getKey(4);
					val4 = getValue(4);
					key5 = getKey(5);
					val5 = getValue(5);
					key6 = getKey(6);
					val6 = getValue(6);
					key7 = getKey(7);
					val7 = getValue(7);
					break;
				}
				case 4: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					key4 = getKey(3);
					val4 = getValue(3);
					key5 = getKey(5);
					val5 = getValue(5);
					key6 = getKey(6);
					val6 = getValue(6);
					key7 = getKey(7);
					val7 = getValue(7);
					break;
				}
				case 5: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					key4 = getKey(3);
					val4 = getValue(3);
					key5 = getKey(4);
					val5 = getValue(4);
					key6 = getKey(6);
					val6 = getValue(6);
					key7 = getKey(7);
					val7 = getValue(7);
					break;
				}
				case 6: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					key4 = getKey(3);
					val4 = getValue(3);
					key5 = getKey(4);
					val5 = getValue(4);
					key6 = getKey(5);
					val6 = getValue(5);
					key7 = getKey(7);
					val7 = getValue(7);
					break;
				}
				case 7: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					key4 = getKey(3);
					val4 = getValue(3);
					key5 = getKey(4);
					val5 = getValue(4);
					key6 = getKey(5);
					val6 = getValue(5);
					key7 = getKey(6);
					val7 = getValue(6);
					break;
				}
				default:
					throw new IllegalStateException("Index out of range.");
				}

				final CompactMapNode<K, V> node1 = getNode(0);

				return nodeOf(mutator, nodeMap, dataMap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key7, val7, node1);

			}
			case 9: {
				K key1;
				V val1;
				K key2;
				V val2;
				K key3;
				V val3;
				K key4;
				V val4;
				K key5;
				V val5;
				K key6;
				V val6;
				K key7;
				V val7;
				K key8;
				V val8;

				switch (valIndex) {
				case 0: {

					key1 = getKey(1);
					val1 = getValue(1);
					key2 = getKey(2);
					val2 = getValue(2);
					key3 = getKey(3);
					val3 = getValue(3);
					key4 = getKey(4);
					val4 = getValue(4);
					key5 = getKey(5);
					val5 = getValue(5);
					key6 = getKey(6);
					val6 = getValue(6);
					key7 = getKey(7);
					val7 = getValue(7);
					key8 = getKey(8);
					val8 = getValue(8);
					break;
				}
				case 1: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(2);
					val2 = getValue(2);
					key3 = getKey(3);
					val3 = getValue(3);
					key4 = getKey(4);
					val4 = getValue(4);
					key5 = getKey(5);
					val5 = getValue(5);
					key6 = getKey(6);
					val6 = getValue(6);
					key7 = getKey(7);
					val7 = getValue(7);
					key8 = getKey(8);
					val8 = getValue(8);
					break;
				}
				case 2: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(3);
					val3 = getValue(3);
					key4 = getKey(4);
					val4 = getValue(4);
					key5 = getKey(5);
					val5 = getValue(5);
					key6 = getKey(6);
					val6 = getValue(6);
					key7 = getKey(7);
					val7 = getValue(7);
					key8 = getKey(8);
					val8 = getValue(8);
					break;
				}
				case 3: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					key4 = getKey(4);
					val4 = getValue(4);
					key5 = getKey(5);
					val5 = getValue(5);
					key6 = getKey(6);
					val6 = getValue(6);
					key7 = getKey(7);
					val7 = getValue(7);
					key8 = getKey(8);
					val8 = getValue(8);
					break;
				}
				case 4: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					key4 = getKey(3);
					val4 = getValue(3);
					key5 = getKey(5);
					val5 = getValue(5);
					key6 = getKey(6);
					val6 = getValue(6);
					key7 = getKey(7);
					val7 = getValue(7);
					key8 = getKey(8);
					val8 = getValue(8);
					break;
				}
				case 5: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					key4 = getKey(3);
					val4 = getValue(3);
					key5 = getKey(4);
					val5 = getValue(4);
					key6 = getKey(6);
					val6 = getValue(6);
					key7 = getKey(7);
					val7 = getValue(7);
					key8 = getKey(8);
					val8 = getValue(8);
					break;
				}
				case 6: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					key4 = getKey(3);
					val4 = getValue(3);
					key5 = getKey(4);
					val5 = getValue(4);
					key6 = getKey(5);
					val6 = getValue(5);
					key7 = getKey(7);
					val7 = getValue(7);
					key8 = getKey(8);
					val8 = getValue(8);
					break;
				}
				case 7: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					key4 = getKey(3);
					val4 = getValue(3);
					key5 = getKey(4);
					val5 = getValue(4);
					key6 = getKey(5);
					val6 = getValue(5);
					key7 = getKey(6);
					val7 = getValue(6);
					key8 = getKey(8);
					val8 = getValue(8);
					break;
				}
				case 8: {

					key1 = getKey(0);
					val1 = getValue(0);
					key2 = getKey(1);
					val2 = getValue(1);
					key3 = getKey(2);
					val3 = getValue(2);
					key4 = getKey(3);
					val4 = getValue(3);
					key5 = getKey(4);
					val5 = getValue(4);
					key6 = getKey(5);
					val6 = getValue(5);
					key7 = getKey(6);
					val7 = getValue(6);
					key8 = getKey(7);
					val8 = getValue(7);
					break;
				}
				default:
					throw new IllegalStateException("Index out of range.");
				}

				return nodeOf(mutator, nodeMap, dataMap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key7, val7, key8, val8);

			}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}
	}

	private static final class HashCollisionMapNode_5Bits_Untyped_Spec0To8<K, V> extends
					CompactMapNode<K, V> {
		private final K[] keys;
		private final V[] vals;
		private final int hash;

		HashCollisionMapNode_5Bits_Untyped_Spec0To8(final int hash, final K[] keys, final V[] vals) {
			this.keys = keys;
			this.vals = vals;
			this.hash = hash;

			assert payloadArity() >= 2;
		}

		boolean containsKey(final K key, final int keyHash, final int shift) {
			if (this.hash == keyHash) {
				for (K k : keys) {
					if (k.equals(key)) {
						return true;
					}
				}
			}
			return false;
		}

		boolean containsKey(final K key, final int keyHash, final int shift,
						final Comparator<Object> cmp) {
			if (this.hash == keyHash) {
				for (K k : keys) {
					if (cmp.compare(k, key) == 0) {
						return true;
					}
				}
			}
			return false;
		}

		Optional<V> findByKey(final K key, final int keyHash, final int shift) {
			for (int i = 0; i < keys.length; i++) {
				final K _key = keys[i];
				if (key.equals(_key)) {
					final V val = vals[i];
					return Optional.of(val);
				}
			}
			return Optional.empty();
		}

		Optional<V> findByKey(final K key, final int keyHash, final int shift,
						final Comparator<Object> cmp) {
			for (int i = 0; i < keys.length; i++) {
				final K _key = keys[i];
				if (cmp.compare(key, _key) == 0) {
					final V val = vals[i];
					return Optional.of(val);
				}
			}
			return Optional.empty();
		}

		CompactMapNode<K, V> updated(final AtomicReference<Thread> mutator, final K key,
						final V val, final int keyHash, final int shift,
						final MapResult<K, V> details) {
			assert this.hash == keyHash;

			for (int idx = 0; idx < keys.length; idx++) {
				if (keys[idx].equals(key)) {
					final V currentVal = vals[idx];

					if (currentVal.equals(val)) {
						return this;
					} else {
						// add new mapping
						final V[] src = this.vals;
						@SuppressWarnings("unchecked")
						final V[] dst = (V[]) new Object[src.length];

						// copy 'src' and set 1 element(s) at position 'idx'
						System.arraycopy(src, 0, dst, 0, src.length);
						dst[idx + 0] = val;

						final CompactMapNode<K, V> thisNew = new HashCollisionMapNode_5Bits_Untyped_Spec0To8<>(
										this.hash, this.keys, dst);

						details.updated(currentVal);
						return thisNew;
					}
				}
			}

			@SuppressWarnings("unchecked")
			final K[] keysNew = (K[]) new Object[this.keys.length + 1];

			// copy 'this.keys' and insert 1 element(s) at position
			// 'keys.length'
			System.arraycopy(this.keys, 0, keysNew, 0, keys.length);
			keysNew[keys.length + 0] = key;
			System.arraycopy(this.keys, keys.length, keysNew, keys.length + 1, this.keys.length
							- keys.length);

			@SuppressWarnings("unchecked")
			final V[] valsNew = (V[]) new Object[this.vals.length + 1];

			// copy 'this.vals' and insert 1 element(s) at position
			// 'vals.length'
			System.arraycopy(this.vals, 0, valsNew, 0, vals.length);
			valsNew[vals.length + 0] = val;
			System.arraycopy(this.vals, vals.length, valsNew, vals.length + 1, this.vals.length
							- vals.length);

			details.modified();
			return new HashCollisionMapNode_5Bits_Untyped_Spec0To8<>(keyHash, keysNew, valsNew);
		}

		CompactMapNode<K, V> updated(final AtomicReference<Thread> mutator, final K key,
						final V val, final int keyHash, final int shift,
						final MapResult<K, V> details, final Comparator<Object> cmp) {
			assert this.hash == keyHash;

			for (int idx = 0; idx < keys.length; idx++) {
				if (cmp.compare(keys[idx], key) == 0) {
					final V currentVal = vals[idx];

					if (cmp.compare(currentVal, val) == 0) {
						return this;
					} else {
						// add new mapping
						final V[] src = this.vals;
						@SuppressWarnings("unchecked")
						final V[] dst = (V[]) new Object[src.length];

						// copy 'src' and set 1 element(s) at position 'idx'
						System.arraycopy(src, 0, dst, 0, src.length);
						dst[idx + 0] = val;

						final CompactMapNode<K, V> thisNew = new HashCollisionMapNode_5Bits_Untyped_Spec0To8<>(
										this.hash, this.keys, dst);

						details.updated(currentVal);
						return thisNew;
					}
				}
			}

			@SuppressWarnings("unchecked")
			final K[] keysNew = (K[]) new Object[this.keys.length + 1];

			// copy 'this.keys' and insert 1 element(s) at position
			// 'keys.length'
			System.arraycopy(this.keys, 0, keysNew, 0, keys.length);
			keysNew[keys.length + 0] = key;
			System.arraycopy(this.keys, keys.length, keysNew, keys.length + 1, this.keys.length
							- keys.length);

			@SuppressWarnings("unchecked")
			final V[] valsNew = (V[]) new Object[this.vals.length + 1];

			// copy 'this.vals' and insert 1 element(s) at position
			// 'vals.length'
			System.arraycopy(this.vals, 0, valsNew, 0, vals.length);
			valsNew[vals.length + 0] = val;
			System.arraycopy(this.vals, vals.length, valsNew, vals.length + 1, this.vals.length
							- vals.length);

			details.modified();
			return new HashCollisionMapNode_5Bits_Untyped_Spec0To8<>(keyHash, keysNew, valsNew);
		}

		CompactMapNode<K, V> removed(final AtomicReference<Thread> mutator, final K key,
						final int keyHash, final int shift, final MapResult<K, V> details) {
			for (int idx = 0; idx < keys.length; idx++) {
				if (keys[idx].equals(key)) {
					final V currentVal = vals[idx];
					details.updated(currentVal);

					if (this.arity() == 1) {
						return nodeOf(mutator);
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node
						 * will be a) either be the new root returned, or b)
						 * unwrapped and inlined.
						 */
						final K theOtherKey = (idx == 0) ? keys[1] : keys[0];
						final V theOtherVal = (idx == 0) ? vals[1] : vals[0];
						return CompactMapNode.<K, V> nodeOf(mutator).updated(mutator, theOtherKey,
										theOtherVal, keyHash, 0, details);
					} else {
						@SuppressWarnings("unchecked")
						final K[] keysNew = (K[]) new Object[this.keys.length - 1];

						// copy 'this.keys' and remove 1 element(s) at position
						// 'idx'
						System.arraycopy(this.keys, 0, keysNew, 0, idx);
						System.arraycopy(this.keys, idx + 1, keysNew, idx, this.keys.length - idx
										- 1);

						@SuppressWarnings("unchecked")
						final V[] valsNew = (V[]) new Object[this.vals.length - 1];

						// copy 'this.vals' and remove 1 element(s) at position
						// 'idx'
						System.arraycopy(this.vals, 0, valsNew, 0, idx);
						System.arraycopy(this.vals, idx + 1, valsNew, idx, this.vals.length - idx
										- 1);

						return new HashCollisionMapNode_5Bits_Untyped_Spec0To8<>(keyHash, keysNew,
										valsNew);
					}
				}
			}
			return this;
		}

		CompactMapNode<K, V> removed(final AtomicReference<Thread> mutator, final K key,
						final int keyHash, final int shift, final MapResult<K, V> details,
						final Comparator<Object> cmp) {
			for (int idx = 0; idx < keys.length; idx++) {
				if (cmp.compare(keys[idx], key) == 0) {
					final V currentVal = vals[idx];
					details.updated(currentVal);

					if (this.arity() == 1) {
						return nodeOf(mutator);
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node
						 * will be a) either be the new root returned, or b)
						 * unwrapped and inlined.
						 */
						final K theOtherKey = (idx == 0) ? keys[1] : keys[0];
						final V theOtherVal = (idx == 0) ? vals[1] : vals[0];
						return CompactMapNode.<K, V> nodeOf(mutator).updated(mutator, theOtherKey,
										theOtherVal, keyHash, 0, details, cmp);
					} else {
						@SuppressWarnings("unchecked")
						final K[] keysNew = (K[]) new Object[this.keys.length - 1];

						// copy 'this.keys' and remove 1 element(s) at position
						// 'idx'
						System.arraycopy(this.keys, 0, keysNew, 0, idx);
						System.arraycopy(this.keys, idx + 1, keysNew, idx, this.keys.length - idx
										- 1);

						@SuppressWarnings("unchecked")
						final V[] valsNew = (V[]) new Object[this.vals.length - 1];

						// copy 'this.vals' and remove 1 element(s) at position
						// 'idx'
						System.arraycopy(this.vals, 0, valsNew, 0, idx);
						System.arraycopy(this.vals, idx + 1, valsNew, idx, this.vals.length - idx
										- 1);

						return new HashCollisionMapNode_5Bits_Untyped_Spec0To8<>(keyHash, keysNew,
										valsNew);
					}
				}
			}
			return this;
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
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
			return payloadArity();
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		K getKey(final int index) {
			return keys[index];
		}

		@Override
		V getValue(final int index) {
			return vals[index];
		}

		Map.Entry<K, V> getKeyValueEntry(final int index) {
			return entryOf(keys[index], vals[index]);
		}

		@Override
		public CompactMapNode<K, V> getNode(int index) {
			throw new IllegalStateException("Is leaf node.");
		}

		@Override
		Object getSlot(final int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		boolean hasSlots() {
			throw new UnsupportedOperationException();
		}

		@Override
		int slotArity() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 0;
			result = prime * result + hash;
			result = prime * result + Arrays.hashCode(keys);
			result = prime * result + Arrays.hashCode(vals);
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

			HashCollisionMapNode_5Bits_Untyped_Spec0To8<?, ?> that = (HashCollisionMapNode_5Bits_Untyped_Spec0To8<?, ?>) other;

			if (hash != that.hash) {
				return false;
			}

			if (arity() != that.arity()) {
				return false;
			}

			/*
			 * Linear scan for each key, because of arbitrary element order.
			 */
			outerLoop: for (int i = 0; i < that.payloadArity(); i++) {
				final Object otherKey = that.getKey(i);
				final Object otherVal = that.getValue(i);

				for (int j = 0; j < keys.length; j++) {
					final K key = keys[j];
					final V val = vals[j];

					if (key.equals(otherKey) && val.equals(otherVal)) {
						continue outerLoop;
					}
				}
				return false;
			}

			return true;
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(final AtomicReference<Thread> mutator,
						final int bitpos, final V val) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(final AtomicReference<Thread> mutator,
						final int bitpos, final K key, final V val) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(final AtomicReference<Thread> mutator,
						final int bitpos) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactMapNode<K, V> removeInplaceValueAndConvertToSpecializedNode(
						final AtomicReference<Thread> mutator, final int bitpos) {
			throw new UnsupportedOperationException();
		}

		@Override
		int nodeMap() {
			throw new UnsupportedOperationException();
		}

		@Override
		int dataMap() {
			throw new UnsupportedOperationException();
		}

	}

	/**
	 * Iterator skeleton that uses a fixed stack in depth.
	 */
	private static abstract class AbstractMapIterator<K, V> {

		private static final int MAX_DEPTH = 7;

		protected int currentValueCursor;
		protected int currentValueLength;
		protected AbstractMapNode<K, V> currentValueNode;

		private int currentStackLevel = -1;
		private final int[] nodeCursorsAndLengths = new int[MAX_DEPTH * 2];

		@SuppressWarnings("unchecked")
		AbstractMapNode<K, V>[] nodes = new AbstractMapNode[MAX_DEPTH];

		AbstractMapIterator(AbstractMapNode<K, V> rootNode) {
			if (rootNode.hasNodes()) {
				currentStackLevel = 0;

				nodes[0] = rootNode;
				nodeCursorsAndLengths[0] = 0;
				nodeCursorsAndLengths[1] = rootNode.nodeArity();
			}

			if (rootNode.hasPayload()) {
				currentValueNode = rootNode;
				currentValueCursor = 0;
				currentValueLength = rootNode.payloadArity();
			}
		}

		/*
		 * search for next node that contains values
		 */
		private boolean searchNextValueNode() {
			while (currentStackLevel >= 0) {
				final int currentCursorIndex = currentStackLevel * 2;
				final int currentLengthIndex = currentCursorIndex + 1;

				final int nodeCursor = nodeCursorsAndLengths[currentCursorIndex];
				final int nodeLength = nodeCursorsAndLengths[currentLengthIndex];

				if (nodeCursor < nodeLength) {
					final AbstractMapNode<K, V> nextNode = nodes[currentStackLevel]
									.getNode(nodeCursor);
					nodeCursorsAndLengths[currentCursorIndex]++;

					if (nextNode.hasNodes()) {
						/*
						 * put node on next stack level for depth-first
						 * traversal
						 */
						final int nextStackLevel = ++currentStackLevel;
						final int nextCursorIndex = nextStackLevel * 2;
						final int nextLengthIndex = nextCursorIndex + 1;

						nodes[nextStackLevel] = nextNode;
						nodeCursorsAndLengths[nextCursorIndex] = 0;
						nodeCursorsAndLengths[nextLengthIndex] = nextNode.nodeArity();
					}

					if (nextNode.hasPayload()) {
						/*
						 * found next node that contains values
						 */
						currentValueNode = nextNode;
						currentValueCursor = 0;
						currentValueLength = nextNode.payloadArity();
						return true;
					}
				} else {
					currentStackLevel--;
				}
			}

			return false;
		}

		public boolean hasNext() {
			if (currentValueCursor < currentValueLength) {
				return true;
			} else {
				return searchNextValueNode();
			}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	protected static class MapKeyIterator<K, V> extends AbstractMapIterator<K, V> implements
					Iterator<K> {

		MapKeyIterator(AbstractMapNode<K, V> rootNode) {
			super(rootNode);
		}

		@Override
		public K next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			} else {
				return currentValueNode.getKey(currentValueCursor++);
			}
		}

	}

	protected static class MapValueIterator<K, V> extends AbstractMapIterator<K, V> implements
					Iterator<V> {

		MapValueIterator(AbstractMapNode<K, V> rootNode) {
			super(rootNode);
		}

		@Override
		public V next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			} else {
				return currentValueNode.getValue(currentValueCursor++);
			}
		}

	}

	protected static class MapEntryIterator<K, V> extends AbstractMapIterator<K, V> implements
					Iterator<Map.Entry<K, V>> {

		MapEntryIterator(AbstractMapNode<K, V> rootNode) {
			super(rootNode);
		}

		@Override
		public Map.Entry<K, V> next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			} else {
				return currentValueNode.getKeyValueEntry(currentValueCursor++);
			}
		}

	}

	/**
	 * Iterator that first iterates over inlined-values and then continues depth
	 * first recursively.
	 */
	private static class TrieMap_5Bits_Untyped_Spec0To8NodeIterator<K, V> implements
					Iterator<AbstractMapNode<K, V>> {

		final Deque<Iterator<? extends AbstractMapNode<K, V>>> nodeIteratorStack;

		TrieMap_5Bits_Untyped_Spec0To8NodeIterator(AbstractMapNode<K, V> rootNode) {
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
		public AbstractMapNode<K, V> next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			AbstractMapNode<K, V> innerNode = nodeIteratorStack.peek().next();

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

	static final class TransientTrieMap_5Bits_Untyped_Spec0To8<K, V> implements TransientMap<K, V> {
		final private AtomicReference<Thread> mutator;
		private AbstractMapNode<K, V> rootNode;
		private int hashCode;
		private int cachedSize;

		TransientTrieMap_5Bits_Untyped_Spec0To8(
						TrieMap_5Bits_Untyped_Spec0To8<K, V> trieMap_5Bits_Untyped_Spec0To8) {
			this.mutator = new AtomicReference<Thread>(Thread.currentThread());
			this.rootNode = trieMap_5Bits_Untyped_Spec0To8.rootNode;
			this.hashCode = trieMap_5Bits_Untyped_Spec0To8.hashCode;
			this.cachedSize = trieMap_5Bits_Untyped_Spec0To8.cachedSize;
			if (DEBUG) {
				assert checkHashCodeAndSize(hashCode, cachedSize);
			}
		}

		private boolean checkHashCodeAndSize(final int targetHash, final int targetSize) {
			int hash = 0;
			int size = 0;

			for (Iterator<Map.Entry<K, V>> it = entryIterator(); it.hasNext();) {
				final Map.Entry<K, V> entry = it.next();
				final K key = entry.getKey();
				final V val = entry.getValue();

				hash += key.hashCode() ^ val.hashCode();
				size += 1;
			}

			return hash == targetHash && size == targetSize;
		}

		public V put(final K key, final V val) {
			throw new UnsupportedOperationException();
		}

		public void putAll(final Map<? extends K, ? extends V> m) {
			throw new UnsupportedOperationException();
		}

		public void clear() {
			throw new UnsupportedOperationException();
		}

		public V remove(final Object key) {
			throw new UnsupportedOperationException();
		}

		public boolean containsKey(final Object o) {
			try {
				@SuppressWarnings("unchecked")
				final K key = (K) o;
				return rootNode.containsKey(key, transformHashCode(key.hashCode()), 0);
			} catch (ClassCastException unused) {
				return false;
			}
		}

		public boolean containsKeyEquivalent(final Object o, final Comparator<Object> cmp) {
			try {
				@SuppressWarnings("unchecked")
				final K key = (K) o;
				return rootNode.containsKey(key, transformHashCode(key.hashCode()), 0, cmp);
			} catch (ClassCastException unused) {
				return false;
			}
		}

		public boolean containsValue(final Object o) {
			for (Iterator<V> iterator = valueIterator(); iterator.hasNext();) {
				if (iterator.next().equals(o)) {
					return true;
				}
			}
			return false;
		}

		public boolean containsValueEquivalent(final Object o, final Comparator<Object> cmp) {
			for (Iterator<V> iterator = valueIterator(); iterator.hasNext();) {
				if (cmp.compare(iterator.next(), o) == 0) {
					return true;
				}
			}
			return false;
		}

		public V get(final Object o) {
			try {
				@SuppressWarnings("unchecked")
				final K key = (K) o;
				final Optional<V> result = rootNode.findByKey(key,
								transformHashCode(key.hashCode()), 0);

				if (result.isPresent()) {
					return result.get();
				} else {
					return null;
				}
			} catch (ClassCastException unused) {
				return null;
			}
		}

		public V getEquivalent(final Object o, final Comparator<Object> cmp) {
			try {
				@SuppressWarnings("unchecked")
				final K key = (K) o;
				final Optional<V> result = rootNode.findByKey(key,
								transformHashCode(key.hashCode()), 0, cmp);

				if (result.isPresent()) {
					return result.get();
				} else {
					return null;
				}
			} catch (ClassCastException unused) {
				return null;
			}
		}

		public V __put(final K key, final V val) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final MapResult<K, V> details = MapResult.unchanged();

			final CompactMapNode<K, V> newRootNode = rootNode.updated(mutator, key, val,
							transformHashCode(keyHash), 0, details);

			if (details.isModified()) {
				if (details.hasReplacedValue()) {
					final V old = details.getReplacedValue();

					final int valHashOld = old.hashCode();
					final int valHashNew = val.hashCode();

					rootNode = newRootNode;
					hashCode = hashCode + (keyHash ^ valHashNew) - (keyHash ^ valHashOld);

					if (DEBUG) {
						assert checkHashCodeAndSize(hashCode, cachedSize);
					}
					return details.getReplacedValue();
				} else {
					final int valHashNew = val.hashCode();
					rootNode = newRootNode;
					hashCode += (keyHash ^ valHashNew);
					cachedSize += 1;

					if (DEBUG) {
						assert checkHashCodeAndSize(hashCode, cachedSize);
					}
					return null;
				}
			}

			if (DEBUG) {
				assert checkHashCodeAndSize(hashCode, cachedSize);
			}
			return null;
		}

		public V __putEquivalent(final K key, final V val, final Comparator<Object> cmp) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final MapResult<K, V> details = MapResult.unchanged();

			final CompactMapNode<K, V> newRootNode = rootNode.updated(mutator, key, val,
							transformHashCode(keyHash), 0, details, cmp);

			if (details.isModified()) {
				if (details.hasReplacedValue()) {
					final V old = details.getReplacedValue();

					final int valHashOld = old.hashCode();
					final int valHashNew = val.hashCode();

					rootNode = newRootNode;
					hashCode = hashCode + (keyHash ^ valHashNew) - (keyHash ^ valHashOld);

					if (DEBUG) {
						assert checkHashCodeAndSize(hashCode, cachedSize);
					}
					return details.getReplacedValue();
				} else {
					final int valHashNew = val.hashCode();
					rootNode = newRootNode;
					hashCode += (keyHash ^ valHashNew);
					cachedSize += 1;

					if (DEBUG) {
						assert checkHashCodeAndSize(hashCode, cachedSize);
					}
					return null;
				}
			}

			if (DEBUG) {
				assert checkHashCodeAndSize(hashCode, cachedSize);
			}
			return null;
		}

		public boolean __putAll(final Map<? extends K, ? extends V> map) {
			boolean modified = false;

			for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
				final boolean isPresent = this.containsKey(entry.getKey());
				final V replaced = this.__put(entry.getKey(), entry.getValue());

				if (!isPresent || replaced != null) {
					modified = true;
				}
			}

			return modified;
		}

		public boolean __putAllEquivalent(final Map<? extends K, ? extends V> map,
						final Comparator<Object> cmp) {
			boolean modified = false;

			for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
				final boolean isPresent = this.containsKeyEquivalent(entry.getKey(), cmp);
				final V replaced = this.__putEquivalent(entry.getKey(), entry.getValue(), cmp);

				if (!isPresent || replaced != null) {
					modified = true;
				}
			}

			return modified;
		}

		public V __remove(final K key) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final MapResult<K, V> details = MapResult.unchanged();

			final CompactMapNode<K, V> newRootNode = rootNode.removed(mutator, key,
							transformHashCode(keyHash), 0, details);

			if (details.isModified()) {
				assert details.hasReplacedValue();
				final int valHash = details.getReplacedValue().hashCode();

				rootNode = newRootNode;
				hashCode = hashCode - (keyHash ^ valHash);
				cachedSize = cachedSize - 1;

				if (DEBUG) {
					assert checkHashCodeAndSize(hashCode, cachedSize);
				}
				return details.getReplacedValue();
			}

			if (DEBUG) {
				assert checkHashCodeAndSize(hashCode, cachedSize);
			}

			return null;
		}

		public V __removeEquivalent(final K key, final Comparator<Object> cmp) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final MapResult<K, V> details = MapResult.unchanged();

			final CompactMapNode<K, V> newRootNode = rootNode.removed(mutator, key,
							transformHashCode(keyHash), 0, details, cmp);

			if (details.isModified()) {
				assert details.hasReplacedValue();
				final int valHash = details.getReplacedValue().hashCode();

				rootNode = newRootNode;
				hashCode = hashCode - (keyHash ^ valHash);
				cachedSize = cachedSize - 1;

				if (DEBUG) {
					assert checkHashCodeAndSize(hashCode, cachedSize);
				}
				return details.getReplacedValue();
			}

			if (DEBUG) {
				assert checkHashCodeAndSize(hashCode, cachedSize);
			}

			return null;
		}

		public int size() {
			return cachedSize;
		}

		public boolean isEmpty() {
			return cachedSize == 0;
		}

		public Iterator<K> keyIterator() {
			return new TransientMapKeyIterator<>(this);
		}

		public Iterator<V> valueIterator() {
			return new TransientMapValueIterator<>(this);
		}

		public Iterator<Map.Entry<K, V>> entryIterator() {
			return new TransientMapEntryIterator<>(this);
		}

		public static class TransientMapKeyIterator<K, V> extends MapKeyIterator<K, V> {
			final TransientTrieMap_5Bits_Untyped_Spec0To8<K, V> collection;
			K lastKey;

			public TransientMapKeyIterator(
							final TransientTrieMap_5Bits_Untyped_Spec0To8<K, V> collection) {
				super(collection.rootNode);
				this.collection = collection;
			}

			public K next() {
				return lastKey = super.next();
			}

			public void remove() {
				// TODO: test removal at iteration rigorously
				collection.__remove(lastKey);
			}
		}

		public static class TransientMapValueIterator<K, V> extends MapValueIterator<K, V> {
			final TransientTrieMap_5Bits_Untyped_Spec0To8<K, V> collection;

			public TransientMapValueIterator(
							final TransientTrieMap_5Bits_Untyped_Spec0To8<K, V> collection) {
				super(collection.rootNode);
				this.collection = collection;
			}

			public V next() {
				return super.next();
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		}

		public static class TransientMapEntryIterator<K, V> extends MapEntryIterator<K, V> {
			final TransientTrieMap_5Bits_Untyped_Spec0To8<K, V> collection;

			public TransientMapEntryIterator(
							final TransientTrieMap_5Bits_Untyped_Spec0To8<K, V> collection) {
				super(collection.rootNode);
				this.collection = collection;
			}

			public Map.Entry<K, V> next() {
				return super.next();
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		}

		@Override
		public Set<K> keySet() {
			Set<K> keySet = null;

			if (keySet == null) {
				keySet = new AbstractSet<K>() {
					@Override
					public Iterator<K> iterator() {
						return TransientTrieMap_5Bits_Untyped_Spec0To8.this.keyIterator();
					}

					@Override
					public int size() {
						return TransientTrieMap_5Bits_Untyped_Spec0To8.this.size();
					}

					@Override
					public boolean isEmpty() {
						return TransientTrieMap_5Bits_Untyped_Spec0To8.this.isEmpty();
					}

					@Override
					public void clear() {
						TransientTrieMap_5Bits_Untyped_Spec0To8.this.clear();
					}

					@Override
					public boolean contains(Object k) {
						return TransientTrieMap_5Bits_Untyped_Spec0To8.this.containsKey(k);
					}
				};
			}

			return keySet;
		}

		@Override
		public Collection<V> values() {
			Collection<V> values = null;

			if (values == null) {
				values = new AbstractCollection<V>() {
					@Override
					public Iterator<V> iterator() {
						return TransientTrieMap_5Bits_Untyped_Spec0To8.this.valueIterator();
					}

					@Override
					public int size() {
						return TransientTrieMap_5Bits_Untyped_Spec0To8.this.size();
					}

					@Override
					public boolean isEmpty() {
						return TransientTrieMap_5Bits_Untyped_Spec0To8.this.isEmpty();
					}

					@Override
					public void clear() {
						TransientTrieMap_5Bits_Untyped_Spec0To8.this.clear();
					}

					@Override
					public boolean contains(Object v) {
						return TransientTrieMap_5Bits_Untyped_Spec0To8.this.containsValue(v);
					}
				};
			}

			return values;
		}

		@Override
		public Set<java.util.Map.Entry<K, V>> entrySet() {
			Set<java.util.Map.Entry<K, V>> entrySet = null;

			if (entrySet == null) {
				entrySet = new AbstractSet<java.util.Map.Entry<K, V>>() {
					@Override
					public Iterator<java.util.Map.Entry<K, V>> iterator() {
						return new Iterator<Map.Entry<K, V>>() {
							private final Iterator<Map.Entry<K, V>> i = entryIterator();

							@Override
							public boolean hasNext() {
								return i.hasNext();
							}

							@Override
							public Map.Entry<K, V> next() {
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
						return TransientTrieMap_5Bits_Untyped_Spec0To8.this.size();
					}

					@Override
					public boolean isEmpty() {
						return TransientTrieMap_5Bits_Untyped_Spec0To8.this.isEmpty();
					}

					@Override
					public void clear() {
						TransientTrieMap_5Bits_Untyped_Spec0To8.this.clear();
					}

					@Override
					public boolean contains(Object k) {
						return TransientTrieMap_5Bits_Untyped_Spec0To8.this.containsKey(k);
					}
				};
			}

			return entrySet;
		}

		@Override
		public boolean equals(final Object other) {
			if (other == this) {
				return true;
			}
			if (other == null) {
				return false;
			}

			if (other instanceof TransientTrieMap_5Bits_Untyped_Spec0To8) {
				TransientTrieMap_5Bits_Untyped_Spec0To8<?, ?> that = (TransientTrieMap_5Bits_Untyped_Spec0To8<?, ?>) other;

				if (this.cachedSize != that.cachedSize) {
					return false;
				}

				if (this.hashCode != that.hashCode) {
					return false;
				}

				return rootNode.equals(that.rootNode);
			} else if (other instanceof Map) {
				Map that = (Map) other;

				if (this.size() != that.size())
					return false;

				for (@SuppressWarnings("unchecked")
				Iterator<Map.Entry> it = that.entrySet().iterator(); it.hasNext();) {
					Map.Entry entry = it.next();

					try {
						@SuppressWarnings("unchecked")
						final K key = (K) entry.getKey();
						final Optional<V> result = rootNode.findByKey(key,
										transformHashCode(key.hashCode()), 0);

						if (!result.isPresent()) {
							return false;
						} else {
							@SuppressWarnings("unchecked")
							final V val = (V) entry.getValue();

							if (!result.get().equals(val)) {
								return false;
							}
						}
					} catch (ClassCastException unused) {
						return false;
					}
				}

				return true;
			}

			return false;
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public ImmutableMap<K, V> freeze() {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			mutator.set(null);
			return new TrieMap_5Bits_Untyped_Spec0To8<K, V>(rootNode, hashCode, cachedSize);
		}
	}

	private static final class Map0To0Node_5Bits_Untyped_Spec0To8<K, V> extends
					CompactMixedMapNode<K, V> {

		Map0To0Node_5Bits_Untyped_Spec0To8(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap) {
			super(mutator, nodeMap, dataMap);

			assert nodeInvariant();
		}

		@Override
		boolean hasSlots() {
			return false;
		}

		@Override
		int slotArity() {
			return 0;
		}

		@Override
		Object getSlot(final int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@SuppressWarnings("unchecked")
		@Override
		K getKey(int index) {
			return (K) getSlot(TUPLE_LENGTH * index);
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(int index) {
			return (V) getSlot(TUPLE_LENGTH * index + 1);
		}

		@SuppressWarnings("unchecked")
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf((K) getSlot(TUPLE_LENGTH * index), (V) getSlot(TUPLE_LENGTH * index + 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		public CompactMapNode<K, V> getNode(int index) {
			final int offset = TUPLE_LENGTH * payloadArity();
			return (CompactMapNode<K, V>) getSlot(offset + index);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			final int offset = TUPLE_LENGTH * payloadArity();
			final Object[] nodes = new Object[0 - offset];

			for (int i = 0; i < 0 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 0;
		}

		@Override
		int nodeArity() {
			return 0 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount(dataMap());
		}

		@Override
		byte sizePredicate() {
			if (this.nodeArity() == 0 && this.payloadArity() == 0) {
				return SIZE_EMPTY;
			} else if (this.nodeArity() == 0 && this.payloadArity() == 1) {
				return SIZE_ONE;
			} else {
				return SIZE_MORE_THAN_ONE;
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final int bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, final int bitpos,
						final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final int bitpos) {
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final int bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = TUPLE_LENGTH * (payloadArity() - 1) + nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() | bitpos);
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() ^ bitpos);
			final int dataMap = (int) (this.dataMap() | bitpos);

			final K key = node.getKey(0);
			final V val = node.getValue(0);

			switch (bitIndex) {
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public int hashCode() {
			int result = 1;

			return result;
		}

		@Override
		public boolean equals(final Object other) {
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

	}

	private static final class Map0To1Node_5Bits_Untyped_Spec0To8<K, V> extends
					CompactMixedMapNode<K, V> {

		private final Object slot0;

		Map0To1Node_5Bits_Untyped_Spec0To8(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;

			assert nodeInvariant();
		}

		@Override
		boolean hasSlots() {
			return true;
		}

		@Override
		int slotArity() {
			return 1;
		}

		@Override
		Object getSlot(final int index) {
			switch (index) {
			case 0:
				return slot0;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		K getKey(int index) {
			return (K) getSlot(TUPLE_LENGTH * index);
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(int index) {
			return (V) getSlot(TUPLE_LENGTH * index + 1);
		}

		@SuppressWarnings("unchecked")
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf((K) getSlot(TUPLE_LENGTH * index), (V) getSlot(TUPLE_LENGTH * index + 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		public CompactMapNode<K, V> getNode(int index) {
			final int offset = TUPLE_LENGTH * payloadArity();
			return (CompactMapNode<K, V>) getSlot(offset + index);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			final int offset = TUPLE_LENGTH * payloadArity();
			final Object[] nodes = new Object[1 - offset];

			for (int i = 0; i < 1 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 1;
		}

		@Override
		int nodeArity() {
			return 1 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount(dataMap());
		}

		@Override
		byte sizePredicate() {
			if (this.nodeArity() == 0 && this.payloadArity() == 0) {
				return SIZE_EMPTY;
			} else if (this.nodeArity() == 0 && this.payloadArity() == 1) {
				return SIZE_ONE;
			} else {
				return SIZE_MORE_THAN_ONE;
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final int bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, final int bitpos,
						final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final int bitpos) {
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final int bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = TUPLE_LENGTH * (payloadArity() - 1) + nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() | bitpos);
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() ^ bitpos);
			final int dataMap = (int) (this.dataMap() | bitpos);

			final K key = node.getKey(0);
			final V val = node.getValue(0);

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((int) nodeMap());
			result = prime * result + ((int) dataMap());
			result = prime * result + slot0.hashCode();
			return result;
		}

		@Override
		public boolean equals(final Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Map0To1Node_5Bits_Untyped_Spec0To8<?, ?> that = (Map0To1Node_5Bits_Untyped_Spec0To8<?, ?>) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(slot0.equals(that.slot0))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To2Node_5Bits_Untyped_Spec0To8<K, V> extends
					CompactMixedMapNode<K, V> {

		private final Object slot0;
		private final Object slot1;

		Map0To2Node_5Bits_Untyped_Spec0To8(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0, final Object slot1) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;

			assert nodeInvariant();
		}

		@Override
		boolean hasSlots() {
			return true;
		}

		@Override
		int slotArity() {
			return 2;
		}

		@Override
		Object getSlot(final int index) {
			switch (index) {
			case 0:
				return slot0;
			case 1:
				return slot1;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		K getKey(int index) {
			return (K) getSlot(TUPLE_LENGTH * index);
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(int index) {
			return (V) getSlot(TUPLE_LENGTH * index + 1);
		}

		@SuppressWarnings("unchecked")
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf((K) getSlot(TUPLE_LENGTH * index), (V) getSlot(TUPLE_LENGTH * index + 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		public CompactMapNode<K, V> getNode(int index) {
			final int offset = TUPLE_LENGTH * payloadArity();
			return (CompactMapNode<K, V>) getSlot(offset + index);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			final int offset = TUPLE_LENGTH * payloadArity();
			final Object[] nodes = new Object[2 - offset];

			for (int i = 0; i < 2 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 2;
		}

		@Override
		int nodeArity() {
			return 2 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount(dataMap());
		}

		@Override
		byte sizePredicate() {
			if (this.nodeArity() == 0 && this.payloadArity() == 0) {
				return SIZE_EMPTY;
			} else if (this.nodeArity() == 0 && this.payloadArity() == 1) {
				return SIZE_ONE;
			} else {
				return SIZE_MORE_THAN_ONE;
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final int bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, final int bitpos,
						final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final int bitpos) {
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final int bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = TUPLE_LENGTH * (payloadArity() - 1) + nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() | bitpos);
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() ^ bitpos);
			final int dataMap = (int) (this.dataMap() | bitpos);

			final K key = node.getKey(0);
			final V val = node.getValue(0);

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot1);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((int) nodeMap());
			result = prime * result + ((int) dataMap());
			result = prime * result + slot0.hashCode();
			result = prime * result + slot1.hashCode();
			return result;
		}

		@Override
		public boolean equals(final Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Map0To2Node_5Bits_Untyped_Spec0To8<?, ?> that = (Map0To2Node_5Bits_Untyped_Spec0To8<?, ?>) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(slot0.equals(that.slot0))) {
				return false;
			}
			if (!(slot1.equals(that.slot1))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To3Node_5Bits_Untyped_Spec0To8<K, V> extends
					CompactMixedMapNode<K, V> {

		private final Object slot0;
		private final Object slot1;
		private final Object slot2;

		Map0To3Node_5Bits_Untyped_Spec0To8(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;

			assert nodeInvariant();
		}

		@Override
		boolean hasSlots() {
			return true;
		}

		@Override
		int slotArity() {
			return 3;
		}

		@Override
		Object getSlot(final int index) {
			switch (index) {
			case 0:
				return slot0;
			case 1:
				return slot1;
			case 2:
				return slot2;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		K getKey(int index) {
			return (K) getSlot(TUPLE_LENGTH * index);
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(int index) {
			return (V) getSlot(TUPLE_LENGTH * index + 1);
		}

		@SuppressWarnings("unchecked")
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf((K) getSlot(TUPLE_LENGTH * index), (V) getSlot(TUPLE_LENGTH * index + 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		public CompactMapNode<K, V> getNode(int index) {
			final int offset = TUPLE_LENGTH * payloadArity();
			return (CompactMapNode<K, V>) getSlot(offset + index);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			final int offset = TUPLE_LENGTH * payloadArity();
			final Object[] nodes = new Object[3 - offset];

			for (int i = 0; i < 3 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 3;
		}

		@Override
		int nodeArity() {
			return 3 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount(dataMap());
		}

		@Override
		byte sizePredicate() {
			if (this.nodeArity() == 0 && this.payloadArity() == 0) {
				return SIZE_EMPTY;
			} else if (this.nodeArity() == 0 && this.payloadArity() == 1) {
				return SIZE_ONE;
			} else {
				return SIZE_MORE_THAN_ONE;
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final int bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, final int bitpos,
						final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final int bitpos) {
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final int bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = TUPLE_LENGTH * (payloadArity() - 1) + nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() | bitpos);
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, node, slot2);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() ^ bitpos);
			final int dataMap = (int) (this.dataMap() | bitpos);

			final K key = node.getKey(0);
			final V val = node.getValue(0);

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot1, slot2);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot2);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((int) nodeMap());
			result = prime * result + ((int) dataMap());
			result = prime * result + slot0.hashCode();
			result = prime * result + slot1.hashCode();
			result = prime * result + slot2.hashCode();
			return result;
		}

		@Override
		public boolean equals(final Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Map0To3Node_5Bits_Untyped_Spec0To8<?, ?> that = (Map0To3Node_5Bits_Untyped_Spec0To8<?, ?>) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(slot0.equals(that.slot0))) {
				return false;
			}
			if (!(slot1.equals(that.slot1))) {
				return false;
			}
			if (!(slot2.equals(that.slot2))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To4Node_5Bits_Untyped_Spec0To8<K, V> extends
					CompactMixedMapNode<K, V> {

		private final Object slot0;
		private final Object slot1;
		private final Object slot2;
		private final Object slot3;

		Map0To4Node_5Bits_Untyped_Spec0To8(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;

			assert nodeInvariant();
		}

		@Override
		boolean hasSlots() {
			return true;
		}

		@Override
		int slotArity() {
			return 4;
		}

		@Override
		Object getSlot(final int index) {
			switch (index) {
			case 0:
				return slot0;
			case 1:
				return slot1;
			case 2:
				return slot2;
			case 3:
				return slot3;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		K getKey(int index) {
			return (K) getSlot(TUPLE_LENGTH * index);
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(int index) {
			return (V) getSlot(TUPLE_LENGTH * index + 1);
		}

		@SuppressWarnings("unchecked")
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf((K) getSlot(TUPLE_LENGTH * index), (V) getSlot(TUPLE_LENGTH * index + 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		public CompactMapNode<K, V> getNode(int index) {
			final int offset = TUPLE_LENGTH * payloadArity();
			return (CompactMapNode<K, V>) getSlot(offset + index);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			final int offset = TUPLE_LENGTH * payloadArity();
			final Object[] nodes = new Object[4 - offset];

			for (int i = 0; i < 4 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 4;
		}

		@Override
		int nodeArity() {
			return 4 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount(dataMap());
		}

		@Override
		byte sizePredicate() {
			if (this.nodeArity() == 0 && this.payloadArity() == 0) {
				return SIZE_EMPTY;
			} else if (this.nodeArity() == 0 && this.payloadArity() == 1) {
				return SIZE_ONE;
			} else {
				return SIZE_MORE_THAN_ONE;
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final int bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, final int bitpos,
						final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final int bitpos) {
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final int bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = TUPLE_LENGTH * (payloadArity() - 1) + nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() | bitpos);
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, node, slot2, slot3);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot2, node, slot3);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() ^ bitpos);
			final int dataMap = (int) (this.dataMap() | bitpos);

			final K key = node.getKey(0);
			final V val = node.getValue(0);

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot1, slot2, slot3);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot2, slot3);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot3);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot3);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((int) nodeMap());
			result = prime * result + ((int) dataMap());
			result = prime * result + slot0.hashCode();
			result = prime * result + slot1.hashCode();
			result = prime * result + slot2.hashCode();
			result = prime * result + slot3.hashCode();
			return result;
		}

		@Override
		public boolean equals(final Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Map0To4Node_5Bits_Untyped_Spec0To8<?, ?> that = (Map0To4Node_5Bits_Untyped_Spec0To8<?, ?>) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(slot0.equals(that.slot0))) {
				return false;
			}
			if (!(slot1.equals(that.slot1))) {
				return false;
			}
			if (!(slot2.equals(that.slot2))) {
				return false;
			}
			if (!(slot3.equals(that.slot3))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To5Node_5Bits_Untyped_Spec0To8<K, V> extends
					CompactMixedMapNode<K, V> {

		private final Object slot0;
		private final Object slot1;
		private final Object slot2;
		private final Object slot3;
		private final Object slot4;

		Map0To5Node_5Bits_Untyped_Spec0To8(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;

			assert nodeInvariant();
		}

		@Override
		boolean hasSlots() {
			return true;
		}

		@Override
		int slotArity() {
			return 5;
		}

		@Override
		Object getSlot(final int index) {
			switch (index) {
			case 0:
				return slot0;
			case 1:
				return slot1;
			case 2:
				return slot2;
			case 3:
				return slot3;
			case 4:
				return slot4;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		K getKey(int index) {
			return (K) getSlot(TUPLE_LENGTH * index);
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(int index) {
			return (V) getSlot(TUPLE_LENGTH * index + 1);
		}

		@SuppressWarnings("unchecked")
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf((K) getSlot(TUPLE_LENGTH * index), (V) getSlot(TUPLE_LENGTH * index + 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		public CompactMapNode<K, V> getNode(int index) {
			final int offset = TUPLE_LENGTH * payloadArity();
			return (CompactMapNode<K, V>) getSlot(offset + index);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			final int offset = TUPLE_LENGTH * payloadArity();
			final Object[] nodes = new Object[5 - offset];

			for (int i = 0; i < 5 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 5;
		}

		@Override
		int nodeArity() {
			return 5 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount(dataMap());
		}

		@Override
		byte sizePredicate() {
			if (this.nodeArity() == 0 && this.payloadArity() == 0) {
				return SIZE_EMPTY;
			} else if (this.nodeArity() == 0 && this.payloadArity() == 1) {
				return SIZE_ONE;
			} else {
				return SIZE_MORE_THAN_ONE;
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final int bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, final int bitpos,
						final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final int bitpos) {
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final int bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = TUPLE_LENGTH * (payloadArity() - 1) + nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() | bitpos);
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, node, slot2, slot3, slot4);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot2, node, slot3, slot4);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, node, slot4);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot4);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() ^ bitpos);
			final int dataMap = (int) (this.dataMap() | bitpos);

			final K key = node.getKey(0);
			final V val = node.getValue(0);

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot1, slot2, slot3, slot4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot2, slot3, slot4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot3, slot4);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot3, slot4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot4);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((int) nodeMap());
			result = prime * result + ((int) dataMap());
			result = prime * result + slot0.hashCode();
			result = prime * result + slot1.hashCode();
			result = prime * result + slot2.hashCode();
			result = prime * result + slot3.hashCode();
			result = prime * result + slot4.hashCode();
			return result;
		}

		@Override
		public boolean equals(final Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Map0To5Node_5Bits_Untyped_Spec0To8<?, ?> that = (Map0To5Node_5Bits_Untyped_Spec0To8<?, ?>) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(slot0.equals(that.slot0))) {
				return false;
			}
			if (!(slot1.equals(that.slot1))) {
				return false;
			}
			if (!(slot2.equals(that.slot2))) {
				return false;
			}
			if (!(slot3.equals(that.slot3))) {
				return false;
			}
			if (!(slot4.equals(that.slot4))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To6Node_5Bits_Untyped_Spec0To8<K, V> extends
					CompactMixedMapNode<K, V> {

		private final Object slot0;
		private final Object slot1;
		private final Object slot2;
		private final Object slot3;
		private final Object slot4;
		private final Object slot5;

		Map0To6Node_5Bits_Untyped_Spec0To8(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
			this.slot5 = slot5;

			assert nodeInvariant();
		}

		@Override
		boolean hasSlots() {
			return true;
		}

		@Override
		int slotArity() {
			return 6;
		}

		@Override
		Object getSlot(final int index) {
			switch (index) {
			case 0:
				return slot0;
			case 1:
				return slot1;
			case 2:
				return slot2;
			case 3:
				return slot3;
			case 4:
				return slot4;
			case 5:
				return slot5;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		K getKey(int index) {
			return (K) getSlot(TUPLE_LENGTH * index);
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(int index) {
			return (V) getSlot(TUPLE_LENGTH * index + 1);
		}

		@SuppressWarnings("unchecked")
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf((K) getSlot(TUPLE_LENGTH * index), (V) getSlot(TUPLE_LENGTH * index + 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		public CompactMapNode<K, V> getNode(int index) {
			final int offset = TUPLE_LENGTH * payloadArity();
			return (CompactMapNode<K, V>) getSlot(offset + index);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			final int offset = TUPLE_LENGTH * payloadArity();
			final Object[] nodes = new Object[6 - offset];

			for (int i = 0; i < 6 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 6;
		}

		@Override
		int nodeArity() {
			return 6 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount(dataMap());
		}

		@Override
		byte sizePredicate() {
			if (this.nodeArity() == 0 && this.payloadArity() == 0) {
				return SIZE_EMPTY;
			} else if (this.nodeArity() == 0 && this.payloadArity() == 1) {
				return SIZE_ONE;
			} else {
				return SIZE_MORE_THAN_ONE;
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final int bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, final int bitpos,
						final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final int bitpos) {
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final int bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = TUPLE_LENGTH * (payloadArity() - 1) + nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() | bitpos);
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, node, slot2, slot3, slot4, slot5);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot2, node, slot3, slot4, slot5);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, node, slot4, slot5);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, node, slot5);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot4, slot5);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, node, slot5);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() ^ bitpos);
			final int dataMap = (int) (this.dataMap() | bitpos);

			final K key = node.getKey(0);
			final V val = node.getValue(0);

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot1, slot2, slot3, slot4,
									slot5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot2, slot3, slot4,
									slot5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot3, slot4,
									slot5);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot3, slot4,
									slot5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot4,
									slot5);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot4,
									slot5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot5);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot5);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((int) nodeMap());
			result = prime * result + ((int) dataMap());
			result = prime * result + slot0.hashCode();
			result = prime * result + slot1.hashCode();
			result = prime * result + slot2.hashCode();
			result = prime * result + slot3.hashCode();
			result = prime * result + slot4.hashCode();
			result = prime * result + slot5.hashCode();
			return result;
		}

		@Override
		public boolean equals(final Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Map0To6Node_5Bits_Untyped_Spec0To8<?, ?> that = (Map0To6Node_5Bits_Untyped_Spec0To8<?, ?>) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(slot0.equals(that.slot0))) {
				return false;
			}
			if (!(slot1.equals(that.slot1))) {
				return false;
			}
			if (!(slot2.equals(that.slot2))) {
				return false;
			}
			if (!(slot3.equals(that.slot3))) {
				return false;
			}
			if (!(slot4.equals(that.slot4))) {
				return false;
			}
			if (!(slot5.equals(that.slot5))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To7Node_5Bits_Untyped_Spec0To8<K, V> extends
					CompactMixedMapNode<K, V> {

		private final Object slot0;
		private final Object slot1;
		private final Object slot2;
		private final Object slot3;
		private final Object slot4;
		private final Object slot5;
		private final Object slot6;

		Map0To7Node_5Bits_Untyped_Spec0To8(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
			this.slot5 = slot5;
			this.slot6 = slot6;

			assert nodeInvariant();
		}

		@Override
		boolean hasSlots() {
			return true;
		}

		@Override
		int slotArity() {
			return 7;
		}

		@Override
		Object getSlot(final int index) {
			switch (index) {
			case 0:
				return slot0;
			case 1:
				return slot1;
			case 2:
				return slot2;
			case 3:
				return slot3;
			case 4:
				return slot4;
			case 5:
				return slot5;
			case 6:
				return slot6;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		K getKey(int index) {
			return (K) getSlot(TUPLE_LENGTH * index);
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(int index) {
			return (V) getSlot(TUPLE_LENGTH * index + 1);
		}

		@SuppressWarnings("unchecked")
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf((K) getSlot(TUPLE_LENGTH * index), (V) getSlot(TUPLE_LENGTH * index + 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		public CompactMapNode<K, V> getNode(int index) {
			final int offset = TUPLE_LENGTH * payloadArity();
			return (CompactMapNode<K, V>) getSlot(offset + index);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			final int offset = TUPLE_LENGTH * payloadArity();
			final Object[] nodes = new Object[7 - offset];

			for (int i = 0; i < 7 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 7;
		}

		@Override
		int nodeArity() {
			return 7 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount(dataMap());
		}

		@Override
		byte sizePredicate() {
			if (this.nodeArity() == 0 && this.payloadArity() == 0) {
				return SIZE_EMPTY;
			} else if (this.nodeArity() == 0 && this.payloadArity() == 1) {
				return SIZE_ONE;
			} else {
				return SIZE_MORE_THAN_ONE;
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final int bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, final int bitpos,
						final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final int bitpos) {
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final int bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = TUPLE_LENGTH * (payloadArity() - 1) + nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() | bitpos);
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, node, slot2, slot3, slot4, slot5,
									slot6);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot2, node, slot3, slot4, slot5,
									slot6);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, node, slot4, slot5,
									slot6);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, node, slot5,
									slot6);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, node,
									slot6);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot4, slot5,
									slot6);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, node, slot5,
									slot6);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, node,
									slot6);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node,
									slot6);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() ^ bitpos);
			final int dataMap = (int) (this.dataMap() | bitpos);

			final K key = node.getKey(0);
			final V val = node.getValue(0);

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot1, slot2, slot3, slot4,
									slot5, slot6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot2, slot3, slot4,
									slot5, slot6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot3, slot4,
									slot5, slot6);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot3, slot4,
									slot5, slot6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot4,
									slot5, slot6);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot4,
									slot5, slot6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot5, slot6);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot5, slot6);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot5, slot6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot6);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot6);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 6:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((int) nodeMap());
			result = prime * result + ((int) dataMap());
			result = prime * result + slot0.hashCode();
			result = prime * result + slot1.hashCode();
			result = prime * result + slot2.hashCode();
			result = prime * result + slot3.hashCode();
			result = prime * result + slot4.hashCode();
			result = prime * result + slot5.hashCode();
			result = prime * result + slot6.hashCode();
			return result;
		}

		@Override
		public boolean equals(final Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Map0To7Node_5Bits_Untyped_Spec0To8<?, ?> that = (Map0To7Node_5Bits_Untyped_Spec0To8<?, ?>) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(slot0.equals(that.slot0))) {
				return false;
			}
			if (!(slot1.equals(that.slot1))) {
				return false;
			}
			if (!(slot2.equals(that.slot2))) {
				return false;
			}
			if (!(slot3.equals(that.slot3))) {
				return false;
			}
			if (!(slot4.equals(that.slot4))) {
				return false;
			}
			if (!(slot5.equals(that.slot5))) {
				return false;
			}
			if (!(slot6.equals(that.slot6))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To8Node_5Bits_Untyped_Spec0To8<K, V> extends
					CompactMixedMapNode<K, V> {

		private final Object slot0;
		private final Object slot1;
		private final Object slot2;
		private final Object slot3;
		private final Object slot4;
		private final Object slot5;
		private final Object slot6;
		private final Object slot7;

		Map0To8Node_5Bits_Untyped_Spec0To8(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6,
						final Object slot7) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
			this.slot5 = slot5;
			this.slot6 = slot6;
			this.slot7 = slot7;

			assert nodeInvariant();
		}

		@Override
		boolean hasSlots() {
			return true;
		}

		@Override
		int slotArity() {
			return 8;
		}

		@Override
		Object getSlot(final int index) {
			switch (index) {
			case 0:
				return slot0;
			case 1:
				return slot1;
			case 2:
				return slot2;
			case 3:
				return slot3;
			case 4:
				return slot4;
			case 5:
				return slot5;
			case 6:
				return slot6;
			case 7:
				return slot7;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		K getKey(int index) {
			return (K) getSlot(TUPLE_LENGTH * index);
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(int index) {
			return (V) getSlot(TUPLE_LENGTH * index + 1);
		}

		@SuppressWarnings("unchecked")
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf((K) getSlot(TUPLE_LENGTH * index), (V) getSlot(TUPLE_LENGTH * index + 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		public CompactMapNode<K, V> getNode(int index) {
			final int offset = TUPLE_LENGTH * payloadArity();
			return (CompactMapNode<K, V>) getSlot(offset + index);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			final int offset = TUPLE_LENGTH * payloadArity();
			final Object[] nodes = new Object[8 - offset];

			for (int i = 0; i < 8 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 8;
		}

		@Override
		int nodeArity() {
			return 8 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount(dataMap());
		}

		@Override
		byte sizePredicate() {
			if (this.nodeArity() == 0 && this.payloadArity() == 0) {
				return SIZE_EMPTY;
			} else if (this.nodeArity() == 0 && this.payloadArity() == 1) {
				return SIZE_ONE;
			} else {
				return SIZE_MORE_THAN_ONE;
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final int bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, final int bitpos,
						final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final int bitpos) {
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final int bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = TUPLE_LENGTH * (payloadArity() - 1) + nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() | bitpos);
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, node, slot2, slot3, slot4, slot5,
									slot6, slot7);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot2, node, slot3, slot4, slot5,
									slot6, slot7);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, node, slot4, slot5,
									slot6, slot7);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, node, slot5,
									slot6, slot7);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, node,
									slot6, slot7);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									node, slot7);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot4, slot5,
									slot6, slot7);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, node, slot5,
									slot6, slot7);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, node,
									slot6, slot7);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									node, slot7);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node,
									slot6, slot7);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									node, slot7);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() ^ bitpos);
			final int dataMap = (int) (this.dataMap() | bitpos);

			final K key = node.getKey(0);
			final V val = node.getValue(0);

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot2, slot3, slot4,
									slot5, slot6, slot7);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot3, slot4,
									slot5, slot6, slot7);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot3, slot4,
									slot5, slot6, slot7);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot4,
									slot5, slot6, slot7);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot4,
									slot5, slot6, slot7);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot5, slot6, slot7);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot5, slot6, slot7);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot5, slot6, slot7);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot6, slot7);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot6, slot7);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot6, slot7);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 6:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot7);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot7);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot7);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot7);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 7:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((int) nodeMap());
			result = prime * result + ((int) dataMap());
			result = prime * result + slot0.hashCode();
			result = prime * result + slot1.hashCode();
			result = prime * result + slot2.hashCode();
			result = prime * result + slot3.hashCode();
			result = prime * result + slot4.hashCode();
			result = prime * result + slot5.hashCode();
			result = prime * result + slot6.hashCode();
			result = prime * result + slot7.hashCode();
			return result;
		}

		@Override
		public boolean equals(final Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Map0To8Node_5Bits_Untyped_Spec0To8<?, ?> that = (Map0To8Node_5Bits_Untyped_Spec0To8<?, ?>) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(slot0.equals(that.slot0))) {
				return false;
			}
			if (!(slot1.equals(that.slot1))) {
				return false;
			}
			if (!(slot2.equals(that.slot2))) {
				return false;
			}
			if (!(slot3.equals(that.slot3))) {
				return false;
			}
			if (!(slot4.equals(that.slot4))) {
				return false;
			}
			if (!(slot5.equals(that.slot5))) {
				return false;
			}
			if (!(slot6.equals(that.slot6))) {
				return false;
			}
			if (!(slot7.equals(that.slot7))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To9Node_5Bits_Untyped_Spec0To8<K, V> extends
					CompactMixedMapNode<K, V> {

		private final Object slot0;
		private final Object slot1;
		private final Object slot2;
		private final Object slot3;
		private final Object slot4;
		private final Object slot5;
		private final Object slot6;
		private final Object slot7;
		private final Object slot8;

		Map0To9Node_5Bits_Untyped_Spec0To8(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6,
						final Object slot7, final Object slot8) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
			this.slot5 = slot5;
			this.slot6 = slot6;
			this.slot7 = slot7;
			this.slot8 = slot8;

			assert nodeInvariant();
		}

		@Override
		boolean hasSlots() {
			return true;
		}

		@Override
		int slotArity() {
			return 9;
		}

		@Override
		Object getSlot(final int index) {
			switch (index) {
			case 0:
				return slot0;
			case 1:
				return slot1;
			case 2:
				return slot2;
			case 3:
				return slot3;
			case 4:
				return slot4;
			case 5:
				return slot5;
			case 6:
				return slot6;
			case 7:
				return slot7;
			case 8:
				return slot8;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		K getKey(int index) {
			return (K) getSlot(TUPLE_LENGTH * index);
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(int index) {
			return (V) getSlot(TUPLE_LENGTH * index + 1);
		}

		@SuppressWarnings("unchecked")
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf((K) getSlot(TUPLE_LENGTH * index), (V) getSlot(TUPLE_LENGTH * index + 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		public CompactMapNode<K, V> getNode(int index) {
			final int offset = TUPLE_LENGTH * payloadArity();
			return (CompactMapNode<K, V>) getSlot(offset + index);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			final int offset = TUPLE_LENGTH * payloadArity();
			final Object[] nodes = new Object[9 - offset];

			for (int i = 0; i < 9 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 9;
		}

		@Override
		int nodeArity() {
			return 9 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount(dataMap());
		}

		@Override
		byte sizePredicate() {
			if (this.nodeArity() == 0 && this.payloadArity() == 0) {
				return SIZE_EMPTY;
			} else if (this.nodeArity() == 0 && this.payloadArity() == 1) {
				return SIZE_ONE;
			} else {
				return SIZE_MORE_THAN_ONE;
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final int bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, final int bitpos,
						final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final int bitpos) {
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final int bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = TUPLE_LENGTH * (payloadArity() - 1) + nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() | bitpos);
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, node, slot2, slot3, slot4, slot5,
									slot6, slot7, slot8);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot2, node, slot3, slot4, slot5,
									slot6, slot7, slot8);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, node, slot4, slot5,
									slot6, slot7, slot8);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, node, slot5,
									slot6, slot7, slot8);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, node,
									slot6, slot7, slot8);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									node, slot7, slot8);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, node, slot8);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot4, slot5,
									slot6, slot7, slot8);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, node, slot5,
									slot6, slot7, slot8);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, node,
									slot6, slot7, slot8);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									node, slot7, slot8);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, node, slot8);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node,
									slot6, slot7, slot8);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									node, slot7, slot8);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, node, slot8);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, node, slot8);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() ^ bitpos);
			final int dataMap = (int) (this.dataMap() | bitpos);

			final K key = node.getKey(0);
			final V val = node.getValue(0);

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot3, slot4,
									slot5, slot6, slot7, slot8);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot3, slot4,
									slot5, slot6, slot7, slot8);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot4,
									slot5, slot6, slot7, slot8);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot4,
									slot5, slot6, slot7, slot8);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot5, slot6, slot7, slot8);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot5, slot6, slot7, slot8);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot5, slot6, slot7, slot8);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot6, slot7, slot8);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot6, slot7, slot8);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot6, slot7, slot8);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 6:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot7, slot8);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot7, slot8);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot7, slot8);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot7, slot8);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 7:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot8);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot8);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot8);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot8);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 8:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((int) nodeMap());
			result = prime * result + ((int) dataMap());
			result = prime * result + slot0.hashCode();
			result = prime * result + slot1.hashCode();
			result = prime * result + slot2.hashCode();
			result = prime * result + slot3.hashCode();
			result = prime * result + slot4.hashCode();
			result = prime * result + slot5.hashCode();
			result = prime * result + slot6.hashCode();
			result = prime * result + slot7.hashCode();
			result = prime * result + slot8.hashCode();
			return result;
		}

		@Override
		public boolean equals(final Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Map0To9Node_5Bits_Untyped_Spec0To8<?, ?> that = (Map0To9Node_5Bits_Untyped_Spec0To8<?, ?>) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(slot0.equals(that.slot0))) {
				return false;
			}
			if (!(slot1.equals(that.slot1))) {
				return false;
			}
			if (!(slot2.equals(that.slot2))) {
				return false;
			}
			if (!(slot3.equals(that.slot3))) {
				return false;
			}
			if (!(slot4.equals(that.slot4))) {
				return false;
			}
			if (!(slot5.equals(that.slot5))) {
				return false;
			}
			if (!(slot6.equals(that.slot6))) {
				return false;
			}
			if (!(slot7.equals(that.slot7))) {
				return false;
			}
			if (!(slot8.equals(that.slot8))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To10Node_5Bits_Untyped_Spec0To8<K, V> extends
					CompactMixedMapNode<K, V> {

		private final Object slot0;
		private final Object slot1;
		private final Object slot2;
		private final Object slot3;
		private final Object slot4;
		private final Object slot5;
		private final Object slot6;
		private final Object slot7;
		private final Object slot8;
		private final Object slot9;

		Map0To10Node_5Bits_Untyped_Spec0To8(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6,
						final Object slot7, final Object slot8, final Object slot9) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
			this.slot5 = slot5;
			this.slot6 = slot6;
			this.slot7 = slot7;
			this.slot8 = slot8;
			this.slot9 = slot9;

			assert nodeInvariant();
		}

		@Override
		boolean hasSlots() {
			return true;
		}

		@Override
		int slotArity() {
			return 10;
		}

		@Override
		Object getSlot(final int index) {
			switch (index) {
			case 0:
				return slot0;
			case 1:
				return slot1;
			case 2:
				return slot2;
			case 3:
				return slot3;
			case 4:
				return slot4;
			case 5:
				return slot5;
			case 6:
				return slot6;
			case 7:
				return slot7;
			case 8:
				return slot8;
			case 9:
				return slot9;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		K getKey(int index) {
			return (K) getSlot(TUPLE_LENGTH * index);
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(int index) {
			return (V) getSlot(TUPLE_LENGTH * index + 1);
		}

		@SuppressWarnings("unchecked")
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf((K) getSlot(TUPLE_LENGTH * index), (V) getSlot(TUPLE_LENGTH * index + 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		public CompactMapNode<K, V> getNode(int index) {
			final int offset = TUPLE_LENGTH * payloadArity();
			return (CompactMapNode<K, V>) getSlot(offset + index);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			final int offset = TUPLE_LENGTH * payloadArity();
			final Object[] nodes = new Object[10 - offset];

			for (int i = 0; i < 10 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 10;
		}

		@Override
		int nodeArity() {
			return 10 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount(dataMap());
		}

		@Override
		byte sizePredicate() {
			if (this.nodeArity() == 0 && this.payloadArity() == 0) {
				return SIZE_EMPTY;
			} else if (this.nodeArity() == 0 && this.payloadArity() == 1) {
				return SIZE_ONE;
			} else {
				return SIZE_MORE_THAN_ONE;
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final int bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, final int bitpos,
						final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final int bitpos) {
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final int bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = TUPLE_LENGTH * (payloadArity() - 1) + nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() | bitpos);
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, node, slot2, slot3, slot4, slot5,
									slot6, slot7, slot8, slot9);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot2, node, slot3, slot4, slot5,
									slot6, slot7, slot8, slot9);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, node, slot4, slot5,
									slot6, slot7, slot8, slot9);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, node, slot5,
									slot6, slot7, slot8, slot9);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, node,
									slot6, slot7, slot8, slot9);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									node, slot7, slot8, slot9);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, node, slot8, slot9);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, node, slot9);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot4, slot5,
									slot6, slot7, slot8, slot9);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, node, slot5,
									slot6, slot7, slot8, slot9);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, node,
									slot6, slot7, slot8, slot9);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									node, slot7, slot8, slot9);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, node, slot8, slot9);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, node, slot9);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node,
									slot6, slot7, slot8, slot9);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									node, slot7, slot8, slot9);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, node, slot8, slot9);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, node, slot9);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, node, slot8, slot9);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, node, slot9);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (bitIndex) {
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() ^ bitpos);
			final int dataMap = (int) (this.dataMap() | bitpos);

			final K key = node.getKey(0);
			final V val = node.getValue(0);

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot4,
									slot5, slot6, slot7, slot8, slot9);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot4,
									slot5, slot6, slot7, slot8, slot9);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot5, slot6, slot7, slot8, slot9);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot5, slot6, slot7, slot8, slot9);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot5, slot6, slot7, slot8, slot9);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot6, slot7, slot8, slot9);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot6, slot7, slot8, slot9);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot6, slot7, slot8, slot9);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 6:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot7, slot8, slot9);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot7, slot8, slot9);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot7, slot8, slot9);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot7, slot8, slot9);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 7:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot8, slot9);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot8, slot9);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot8, slot9);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot8, slot9);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 8:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot9);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot9);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot9);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot9);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot9);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 9:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((int) nodeMap());
			result = prime * result + ((int) dataMap());
			result = prime * result + slot0.hashCode();
			result = prime * result + slot1.hashCode();
			result = prime * result + slot2.hashCode();
			result = prime * result + slot3.hashCode();
			result = prime * result + slot4.hashCode();
			result = prime * result + slot5.hashCode();
			result = prime * result + slot6.hashCode();
			result = prime * result + slot7.hashCode();
			result = prime * result + slot8.hashCode();
			result = prime * result + slot9.hashCode();
			return result;
		}

		@Override
		public boolean equals(final Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Map0To10Node_5Bits_Untyped_Spec0To8<?, ?> that = (Map0To10Node_5Bits_Untyped_Spec0To8<?, ?>) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(slot0.equals(that.slot0))) {
				return false;
			}
			if (!(slot1.equals(that.slot1))) {
				return false;
			}
			if (!(slot2.equals(that.slot2))) {
				return false;
			}
			if (!(slot3.equals(that.slot3))) {
				return false;
			}
			if (!(slot4.equals(that.slot4))) {
				return false;
			}
			if (!(slot5.equals(that.slot5))) {
				return false;
			}
			if (!(slot6.equals(that.slot6))) {
				return false;
			}
			if (!(slot7.equals(that.slot7))) {
				return false;
			}
			if (!(slot8.equals(that.slot8))) {
				return false;
			}
			if (!(slot9.equals(that.slot9))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To11Node_5Bits_Untyped_Spec0To8<K, V> extends
					CompactMixedMapNode<K, V> {

		private final Object slot0;
		private final Object slot1;
		private final Object slot2;
		private final Object slot3;
		private final Object slot4;
		private final Object slot5;
		private final Object slot6;
		private final Object slot7;
		private final Object slot8;
		private final Object slot9;
		private final Object slot10;

		Map0To11Node_5Bits_Untyped_Spec0To8(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6,
						final Object slot7, final Object slot8, final Object slot9,
						final Object slot10) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
			this.slot5 = slot5;
			this.slot6 = slot6;
			this.slot7 = slot7;
			this.slot8 = slot8;
			this.slot9 = slot9;
			this.slot10 = slot10;

			assert nodeInvariant();
		}

		@Override
		boolean hasSlots() {
			return true;
		}

		@Override
		int slotArity() {
			return 11;
		}

		@Override
		Object getSlot(final int index) {
			switch (index) {
			case 0:
				return slot0;
			case 1:
				return slot1;
			case 2:
				return slot2;
			case 3:
				return slot3;
			case 4:
				return slot4;
			case 5:
				return slot5;
			case 6:
				return slot6;
			case 7:
				return slot7;
			case 8:
				return slot8;
			case 9:
				return slot9;
			case 10:
				return slot10;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		K getKey(int index) {
			return (K) getSlot(TUPLE_LENGTH * index);
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(int index) {
			return (V) getSlot(TUPLE_LENGTH * index + 1);
		}

		@SuppressWarnings("unchecked")
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf((K) getSlot(TUPLE_LENGTH * index), (V) getSlot(TUPLE_LENGTH * index + 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		public CompactMapNode<K, V> getNode(int index) {
			final int offset = TUPLE_LENGTH * payloadArity();
			return (CompactMapNode<K, V>) getSlot(offset + index);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			final int offset = TUPLE_LENGTH * payloadArity();
			final Object[] nodes = new Object[11 - offset];

			for (int i = 0; i < 11 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 11;
		}

		@Override
		int nodeArity() {
			return 11 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount(dataMap());
		}

		@Override
		byte sizePredicate() {
			if (this.nodeArity() == 0 && this.payloadArity() == 0) {
				return SIZE_EMPTY;
			} else if (this.nodeArity() == 0 && this.payloadArity() == 1) {
				return SIZE_ONE;
			} else {
				return SIZE_MORE_THAN_ONE;
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final int bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, final int bitpos,
						final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9, slot10);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9, slot10);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val, slot10);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final int bitpos) {
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final int bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = TUPLE_LENGTH * (payloadArity() - 1) + nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() | bitpos);
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, node, slot2, slot3, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot2, node, slot3, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, node, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, node, slot5,
									slot6, slot7, slot8, slot9, slot10);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, node,
									slot6, slot7, slot8, slot9, slot10);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									node, slot7, slot8, slot9, slot10);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, node, slot8, slot9, slot10);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, node, slot9, slot10);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, node, slot10);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, node, slot5,
									slot6, slot7, slot8, slot9, slot10);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, node,
									slot6, slot7, slot8, slot9, slot10);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									node, slot7, slot8, slot9, slot10);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, node, slot8, slot9, slot10);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, node, slot9, slot10);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, node, slot10);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node,
									slot6, slot7, slot8, slot9, slot10);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									node, slot7, slot8, slot9, slot10);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, node, slot8, slot9, slot10);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, node, slot9, slot10);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, node, slot10);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, node, slot8, slot9, slot10);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, node, slot9, slot10);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, node, slot10);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (bitIndex) {
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, node, slot10);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() ^ bitpos);
			final int dataMap = (int) (this.dataMap() | bitpos);

			final K key = node.getKey(0);
			final V val = node.getValue(0);

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot5, slot6, slot7, slot8, slot9, slot10);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot5, slot6, slot7, slot8, slot9, slot10);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot5, slot6, slot7, slot8, slot9, slot10);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot6, slot7, slot8, slot9, slot10);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot6, slot7, slot8, slot9, slot10);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot6, slot7, slot8, slot9, slot10);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 6:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot7, slot8, slot9, slot10);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot7, slot8, slot9, slot10);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot7, slot8, slot9, slot10);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot7, slot8, slot9, slot10);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 7:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot8, slot9, slot10);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot8, slot9, slot10);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot8, slot9, slot10);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot8, slot9, slot10);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 8:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot9, slot10);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot9, slot10);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot9, slot10);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot9, slot10);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot9, slot10);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 9:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot10);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot10);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot10);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot10);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot10);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 10:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((int) nodeMap());
			result = prime * result + ((int) dataMap());
			result = prime * result + slot0.hashCode();
			result = prime * result + slot1.hashCode();
			result = prime * result + slot2.hashCode();
			result = prime * result + slot3.hashCode();
			result = prime * result + slot4.hashCode();
			result = prime * result + slot5.hashCode();
			result = prime * result + slot6.hashCode();
			result = prime * result + slot7.hashCode();
			result = prime * result + slot8.hashCode();
			result = prime * result + slot9.hashCode();
			result = prime * result + slot10.hashCode();
			return result;
		}

		@Override
		public boolean equals(final Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Map0To11Node_5Bits_Untyped_Spec0To8<?, ?> that = (Map0To11Node_5Bits_Untyped_Spec0To8<?, ?>) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(slot0.equals(that.slot0))) {
				return false;
			}
			if (!(slot1.equals(that.slot1))) {
				return false;
			}
			if (!(slot2.equals(that.slot2))) {
				return false;
			}
			if (!(slot3.equals(that.slot3))) {
				return false;
			}
			if (!(slot4.equals(that.slot4))) {
				return false;
			}
			if (!(slot5.equals(that.slot5))) {
				return false;
			}
			if (!(slot6.equals(that.slot6))) {
				return false;
			}
			if (!(slot7.equals(that.slot7))) {
				return false;
			}
			if (!(slot8.equals(that.slot8))) {
				return false;
			}
			if (!(slot9.equals(that.slot9))) {
				return false;
			}
			if (!(slot10.equals(that.slot10))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To12Node_5Bits_Untyped_Spec0To8<K, V> extends
					CompactMixedMapNode<K, V> {

		private final Object slot0;
		private final Object slot1;
		private final Object slot2;
		private final Object slot3;
		private final Object slot4;
		private final Object slot5;
		private final Object slot6;
		private final Object slot7;
		private final Object slot8;
		private final Object slot9;
		private final Object slot10;
		private final Object slot11;

		Map0To12Node_5Bits_Untyped_Spec0To8(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6,
						final Object slot7, final Object slot8, final Object slot9,
						final Object slot10, final Object slot11) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
			this.slot5 = slot5;
			this.slot6 = slot6;
			this.slot7 = slot7;
			this.slot8 = slot8;
			this.slot9 = slot9;
			this.slot10 = slot10;
			this.slot11 = slot11;

			assert nodeInvariant();
		}

		@Override
		boolean hasSlots() {
			return true;
		}

		@Override
		int slotArity() {
			return 12;
		}

		@Override
		Object getSlot(final int index) {
			switch (index) {
			case 0:
				return slot0;
			case 1:
				return slot1;
			case 2:
				return slot2;
			case 3:
				return slot3;
			case 4:
				return slot4;
			case 5:
				return slot5;
			case 6:
				return slot6;
			case 7:
				return slot7;
			case 8:
				return slot8;
			case 9:
				return slot9;
			case 10:
				return slot10;
			case 11:
				return slot11;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		K getKey(int index) {
			return (K) getSlot(TUPLE_LENGTH * index);
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(int index) {
			return (V) getSlot(TUPLE_LENGTH * index + 1);
		}

		@SuppressWarnings("unchecked")
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf((K) getSlot(TUPLE_LENGTH * index), (V) getSlot(TUPLE_LENGTH * index + 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		public CompactMapNode<K, V> getNode(int index) {
			final int offset = TUPLE_LENGTH * payloadArity();
			return (CompactMapNode<K, V>) getSlot(offset + index);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			final int offset = TUPLE_LENGTH * payloadArity();
			final Object[] nodes = new Object[12 - offset];

			for (int i = 0; i < 12 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 12;
		}

		@Override
		int nodeArity() {
			return 12 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount(dataMap());
		}

		@Override
		byte sizePredicate() {
			if (this.nodeArity() == 0 && this.payloadArity() == 0) {
				return SIZE_EMPTY;
			} else if (this.nodeArity() == 0 && this.payloadArity() == 1) {
				return SIZE_ONE;
			} else {
				return SIZE_MORE_THAN_ONE;
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final int bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, final int bitpos,
						final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9, slot10, slot11);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9, slot10, slot11);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val, slot10, slot11);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final int bitpos) {
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final int bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = TUPLE_LENGTH * (payloadArity() - 1) + nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() | bitpos);
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, node, slot2, slot3, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot2, node, slot3, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, node, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, node, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, node,
									slot6, slot7, slot8, slot9, slot10, slot11);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									node, slot7, slot8, slot9, slot10, slot11);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, node, slot8, slot9, slot10, slot11);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, node, slot9, slot10, slot11);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, node, slot10, slot11);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, node, slot11);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, node, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, node,
									slot6, slot7, slot8, slot9, slot10, slot11);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									node, slot7, slot8, slot9, slot10, slot11);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, node, slot8, slot9, slot10, slot11);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, node, slot9, slot10, slot11);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, node, slot10, slot11);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, node, slot11);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node,
									slot6, slot7, slot8, slot9, slot10, slot11);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									node, slot7, slot8, slot9, slot10, slot11);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, node, slot8, slot9, slot10, slot11);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, node, slot9, slot10, slot11);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, node, slot10, slot11);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, node, slot11);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, slot11, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, node, slot8, slot9, slot10, slot11);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, node, slot9, slot10, slot11);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, node, slot10, slot11);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, node, slot11);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, slot11, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (bitIndex) {
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, node, slot10, slot11);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, node, slot11);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, slot11, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (bitIndex) {
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() ^ bitpos);
			final int dataMap = (int) (this.dataMap() | bitpos);

			final K key = node.getKey(0);
			final V val = node.getValue(0);

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot6, slot7, slot8, slot9, slot10, slot11);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot6, slot7, slot8, slot9, slot10, slot11);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot6, slot7, slot8, slot9, slot10, slot11);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 6:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot7, slot8, slot9, slot10, slot11);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot7, slot8, slot9, slot10, slot11);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot7, slot8, slot9, slot10, slot11);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot7, slot8, slot9, slot10, slot11);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 7:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot8, slot9, slot10, slot11);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot8, slot9, slot10, slot11);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot8, slot9, slot10, slot11);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot8, slot9, slot10, slot11);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 8:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot9, slot10, slot11);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot9, slot10, slot11);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot9, slot10, slot11);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot9, slot10, slot11);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot9, slot10, slot11);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 9:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot10, slot11);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot10, slot11);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot10, slot11);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot10, slot11);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot10, slot11);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 10:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot11);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot11);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9, slot11);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9, slot11);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9, slot11);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val, slot11);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 11:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9, slot10);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9, slot10);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val, slot10);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((int) nodeMap());
			result = prime * result + ((int) dataMap());
			result = prime * result + slot0.hashCode();
			result = prime * result + slot1.hashCode();
			result = prime * result + slot2.hashCode();
			result = prime * result + slot3.hashCode();
			result = prime * result + slot4.hashCode();
			result = prime * result + slot5.hashCode();
			result = prime * result + slot6.hashCode();
			result = prime * result + slot7.hashCode();
			result = prime * result + slot8.hashCode();
			result = prime * result + slot9.hashCode();
			result = prime * result + slot10.hashCode();
			result = prime * result + slot11.hashCode();
			return result;
		}

		@Override
		public boolean equals(final Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Map0To12Node_5Bits_Untyped_Spec0To8<?, ?> that = (Map0To12Node_5Bits_Untyped_Spec0To8<?, ?>) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(slot0.equals(that.slot0))) {
				return false;
			}
			if (!(slot1.equals(that.slot1))) {
				return false;
			}
			if (!(slot2.equals(that.slot2))) {
				return false;
			}
			if (!(slot3.equals(that.slot3))) {
				return false;
			}
			if (!(slot4.equals(that.slot4))) {
				return false;
			}
			if (!(slot5.equals(that.slot5))) {
				return false;
			}
			if (!(slot6.equals(that.slot6))) {
				return false;
			}
			if (!(slot7.equals(that.slot7))) {
				return false;
			}
			if (!(slot8.equals(that.slot8))) {
				return false;
			}
			if (!(slot9.equals(that.slot9))) {
				return false;
			}
			if (!(slot10.equals(that.slot10))) {
				return false;
			}
			if (!(slot11.equals(that.slot11))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To13Node_5Bits_Untyped_Spec0To8<K, V> extends
					CompactMixedMapNode<K, V> {

		private final Object slot0;
		private final Object slot1;
		private final Object slot2;
		private final Object slot3;
		private final Object slot4;
		private final Object slot5;
		private final Object slot6;
		private final Object slot7;
		private final Object slot8;
		private final Object slot9;
		private final Object slot10;
		private final Object slot11;
		private final Object slot12;

		Map0To13Node_5Bits_Untyped_Spec0To8(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6,
						final Object slot7, final Object slot8, final Object slot9,
						final Object slot10, final Object slot11, final Object slot12) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
			this.slot5 = slot5;
			this.slot6 = slot6;
			this.slot7 = slot7;
			this.slot8 = slot8;
			this.slot9 = slot9;
			this.slot10 = slot10;
			this.slot11 = slot11;
			this.slot12 = slot12;

			assert nodeInvariant();
		}

		@Override
		boolean hasSlots() {
			return true;
		}

		@Override
		int slotArity() {
			return 13;
		}

		@Override
		Object getSlot(final int index) {
			switch (index) {
			case 0:
				return slot0;
			case 1:
				return slot1;
			case 2:
				return slot2;
			case 3:
				return slot3;
			case 4:
				return slot4;
			case 5:
				return slot5;
			case 6:
				return slot6;
			case 7:
				return slot7;
			case 8:
				return slot8;
			case 9:
				return slot9;
			case 10:
				return slot10;
			case 11:
				return slot11;
			case 12:
				return slot12;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		K getKey(int index) {
			return (K) getSlot(TUPLE_LENGTH * index);
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(int index) {
			return (V) getSlot(TUPLE_LENGTH * index + 1);
		}

		@SuppressWarnings("unchecked")
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf((K) getSlot(TUPLE_LENGTH * index), (V) getSlot(TUPLE_LENGTH * index + 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		public CompactMapNode<K, V> getNode(int index) {
			final int offset = TUPLE_LENGTH * payloadArity();
			return (CompactMapNode<K, V>) getSlot(offset + index);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			final int offset = TUPLE_LENGTH * payloadArity();
			final Object[] nodes = new Object[13 - offset];

			for (int i = 0; i < 13 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 13;
		}

		@Override
		int nodeArity() {
			return 13 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount(dataMap());
		}

		@Override
		byte sizePredicate() {
			if (this.nodeArity() == 0 && this.payloadArity() == 0) {
				return SIZE_EMPTY;
			} else if (this.nodeArity() == 0 && this.payloadArity() == 1) {
				return SIZE_ONE;
			} else {
				return SIZE_MORE_THAN_ONE;
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final int bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11, slot12);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11, slot12);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val, slot12);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, final int bitpos,
						final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9, slot10, slot11, slot12);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val, slot10, slot11, slot12);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, key, val, slot12);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final int bitpos) {
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11, slot12);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11, slot12);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot12);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final int bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11, slot12);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11, slot12);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11, slot12);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11, slot12);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11, slot12);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot12);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = TUPLE_LENGTH * (payloadArity() - 1) + nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() | bitpos);
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, node, slot2, slot3, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot2, node, slot3, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, node, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, node, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, node,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									node, slot7, slot8, slot9, slot10, slot11, slot12);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, node, slot8, slot9, slot10, slot11, slot12);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, node, slot9, slot10, slot11, slot12);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, node, slot10, slot11, slot12);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, node, slot11, slot12);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, node, slot12);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, node, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, node,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									node, slot7, slot8, slot9, slot10, slot11, slot12);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, node, slot8, slot9, slot10, slot11, slot12);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, node, slot9, slot10, slot11, slot12);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, node, slot10, slot11, slot12);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, node, slot11, slot12);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, node, slot12);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									node, slot7, slot8, slot9, slot10, slot11, slot12);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, node, slot8, slot9, slot10, slot11, slot12);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, node, slot9, slot10, slot11, slot12);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, node, slot10, slot11, slot12);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, node, slot11, slot12);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, slot11, node, slot12);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, node, slot8, slot9, slot10, slot11, slot12);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, node, slot9, slot10, slot11, slot12);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, node, slot10, slot11, slot12);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, node, slot11, slot12);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, slot11, node, slot12);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, slot11, slot12, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (bitIndex) {
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, node, slot10, slot11, slot12);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, node, slot11, slot12);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, slot11, node, slot12);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, slot11, slot12, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (bitIndex) {
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, node, slot12);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot12, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() ^ bitpos);
			final int dataMap = (int) (this.dataMap() | bitpos);

			final K key = node.getKey(0);
			final V val = node.getValue(0);

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 6:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot7, slot8, slot9, slot10, slot11, slot12);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot7, slot8, slot9, slot10, slot11, slot12);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot7, slot8, slot9, slot10, slot11, slot12);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot7, slot8, slot9, slot10, slot11, slot12);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 7:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot8, slot9, slot10, slot11, slot12);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot8, slot9, slot10, slot11, slot12);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot8, slot9, slot10, slot11, slot12);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot8, slot9, slot10, slot11, slot12);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 8:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot9, slot10, slot11, slot12);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot9, slot10, slot11, slot12);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot9, slot10, slot11, slot12);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot9, slot10, slot11, slot12);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot9, slot10, slot11, slot12);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 9:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot10, slot11, slot12);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot10, slot11, slot12);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot10, slot11, slot12);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot10, slot11, slot12);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot10, slot11, slot12);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 10:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot11, slot12);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot11, slot12);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9, slot11, slot12);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9, slot11, slot12);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9, slot11, slot12);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val, slot11, slot12);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 11:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot12);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot12);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot12);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9, slot10, slot12);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9, slot10, slot12);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val, slot10, slot12);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 12:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9, slot10, slot11);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9, slot10, slot11);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val, slot10, slot11);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, key, val);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((int) nodeMap());
			result = prime * result + ((int) dataMap());
			result = prime * result + slot0.hashCode();
			result = prime * result + slot1.hashCode();
			result = prime * result + slot2.hashCode();
			result = prime * result + slot3.hashCode();
			result = prime * result + slot4.hashCode();
			result = prime * result + slot5.hashCode();
			result = prime * result + slot6.hashCode();
			result = prime * result + slot7.hashCode();
			result = prime * result + slot8.hashCode();
			result = prime * result + slot9.hashCode();
			result = prime * result + slot10.hashCode();
			result = prime * result + slot11.hashCode();
			result = prime * result + slot12.hashCode();
			return result;
		}

		@Override
		public boolean equals(final Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Map0To13Node_5Bits_Untyped_Spec0To8<?, ?> that = (Map0To13Node_5Bits_Untyped_Spec0To8<?, ?>) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(slot0.equals(that.slot0))) {
				return false;
			}
			if (!(slot1.equals(that.slot1))) {
				return false;
			}
			if (!(slot2.equals(that.slot2))) {
				return false;
			}
			if (!(slot3.equals(that.slot3))) {
				return false;
			}
			if (!(slot4.equals(that.slot4))) {
				return false;
			}
			if (!(slot5.equals(that.slot5))) {
				return false;
			}
			if (!(slot6.equals(that.slot6))) {
				return false;
			}
			if (!(slot7.equals(that.slot7))) {
				return false;
			}
			if (!(slot8.equals(that.slot8))) {
				return false;
			}
			if (!(slot9.equals(that.slot9))) {
				return false;
			}
			if (!(slot10.equals(that.slot10))) {
				return false;
			}
			if (!(slot11.equals(that.slot11))) {
				return false;
			}
			if (!(slot12.equals(that.slot12))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To14Node_5Bits_Untyped_Spec0To8<K, V> extends
					CompactMixedMapNode<K, V> {

		private final Object slot0;
		private final Object slot1;
		private final Object slot2;
		private final Object slot3;
		private final Object slot4;
		private final Object slot5;
		private final Object slot6;
		private final Object slot7;
		private final Object slot8;
		private final Object slot9;
		private final Object slot10;
		private final Object slot11;
		private final Object slot12;
		private final Object slot13;

		Map0To14Node_5Bits_Untyped_Spec0To8(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6,
						final Object slot7, final Object slot8, final Object slot9,
						final Object slot10, final Object slot11, final Object slot12,
						final Object slot13) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
			this.slot5 = slot5;
			this.slot6 = slot6;
			this.slot7 = slot7;
			this.slot8 = slot8;
			this.slot9 = slot9;
			this.slot10 = slot10;
			this.slot11 = slot11;
			this.slot12 = slot12;
			this.slot13 = slot13;

			assert nodeInvariant();
		}

		@Override
		boolean hasSlots() {
			return true;
		}

		@Override
		int slotArity() {
			return 14;
		}

		@Override
		Object getSlot(final int index) {
			switch (index) {
			case 0:
				return slot0;
			case 1:
				return slot1;
			case 2:
				return slot2;
			case 3:
				return slot3;
			case 4:
				return slot4;
			case 5:
				return slot5;
			case 6:
				return slot6;
			case 7:
				return slot7;
			case 8:
				return slot8;
			case 9:
				return slot9;
			case 10:
				return slot10;
			case 11:
				return slot11;
			case 12:
				return slot12;
			case 13:
				return slot13;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		K getKey(int index) {
			return (K) getSlot(TUPLE_LENGTH * index);
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(int index) {
			return (V) getSlot(TUPLE_LENGTH * index + 1);
		}

		@SuppressWarnings("unchecked")
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf((K) getSlot(TUPLE_LENGTH * index), (V) getSlot(TUPLE_LENGTH * index + 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		public CompactMapNode<K, V> getNode(int index) {
			final int offset = TUPLE_LENGTH * payloadArity();
			return (CompactMapNode<K, V>) getSlot(offset + index);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			final int offset = TUPLE_LENGTH * payloadArity();
			final Object[] nodes = new Object[14 - offset];

			for (int i = 0; i < 14 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 14;
		}

		@Override
		int nodeArity() {
			return 14 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount(dataMap());
		}

		@Override
		byte sizePredicate() {
			if (this.nodeArity() == 0 && this.payloadArity() == 0) {
				return SIZE_EMPTY;
			} else if (this.nodeArity() == 0 && this.payloadArity() == 1) {
				return SIZE_ONE;
			} else {
				return SIZE_MORE_THAN_ONE;
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final int bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11, slot12, slot13);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11, slot12, slot13);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val, slot12, slot13);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, final int bitpos,
						final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9, slot10, slot11, slot12,
								slot13);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val, slot10, slot11, slot12,
								slot13);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, key, val, slot12,
								slot13);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, key,
								val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final int bitpos) {
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11, slot12, slot13);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11, slot12, slot13);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot12, slot13);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final int bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11, slot12, slot13);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11, slot12, slot13);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11, slot12, slot13);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11, slot12, slot13);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot12, slot13);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot13);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = TUPLE_LENGTH * (payloadArity() - 1) + nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() | bitpos);
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, node, slot2, slot3, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot2, node, slot3, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, node, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, node, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, node,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									node, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, node, slot8, slot9, slot10, slot11, slot12, slot13);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, node, slot9, slot10, slot11, slot12, slot13);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, node, slot10, slot11, slot12, slot13);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, node, slot11, slot12, slot13);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, node, slot12, slot13);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, node, slot13);
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, slot13, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, node, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, node,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									node, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, node, slot8, slot9, slot10, slot11, slot12, slot13);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, node, slot9, slot10, slot11, slot12, slot13);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, node, slot10, slot11, slot12, slot13);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, node, slot11, slot12, slot13);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, node, slot12, slot13);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, node, slot13);
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, slot13, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									node, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, node, slot8, slot9, slot10, slot11, slot12, slot13);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, node, slot9, slot10, slot11, slot12, slot13);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, node, slot10, slot11, slot12, slot13);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, node, slot11, slot12, slot13);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, slot11, node, slot12, slot13);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, node, slot13);
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, slot13, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, node, slot8, slot9, slot10, slot11, slot12, slot13);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, node, slot9, slot10, slot11, slot12, slot13);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, node, slot10, slot11, slot12, slot13);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, node, slot11, slot12, slot13);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, slot11, node, slot12, slot13);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, slot11, slot12, node, slot13);
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, slot11, slot12, slot13, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (bitIndex) {
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, node, slot10, slot11, slot12, slot13);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, node, slot11, slot12, slot13);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, slot11, node, slot12, slot13);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, slot11, slot12, node, slot13);
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, slot11, slot12, slot13, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (bitIndex) {
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, node, slot12, slot13);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot12, node, slot13);
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot12, slot13, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 6:
				switch (bitIndex) {
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() ^ bitpos);
			final int dataMap = (int) (this.dataMap() | bitpos);

			final K key = node.getKey(0);
			final V val = node.getValue(0);

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 6:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 7:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot8, slot9, slot10, slot11, slot12,
									slot13);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot8, slot9, slot10, slot11, slot12,
									slot13);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot8, slot9, slot10, slot11, slot12,
									slot13);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot8, slot9, slot10, slot11, slot12,
									slot13);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 8:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot9, slot10, slot11, slot12,
									slot13);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot9, slot10, slot11, slot12,
									slot13);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot9, slot10, slot11, slot12,
									slot13);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot9, slot10, slot11, slot12,
									slot13);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot9, slot10, slot11, slot12,
									slot13);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 9:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot10, slot11, slot12,
									slot13);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot10, slot11, slot12,
									slot13);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot10, slot11, slot12,
									slot13);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot10, slot11, slot12,
									slot13);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot10, slot11, slot12,
									slot13);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 10:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot11, slot12,
									slot13);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot11, slot12,
									slot13);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9, slot11, slot12,
									slot13);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9, slot11, slot12,
									slot13);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9, slot11, slot12,
									slot13);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val, slot11, slot12,
									slot13);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 11:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot12,
									slot13);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot12,
									slot13);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot12,
									slot13);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9, slot10, slot12,
									slot13);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9, slot10, slot12,
									slot13);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val, slot10, slot12,
									slot13);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 12:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot13);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot13);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot13);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9, slot10, slot11,
									slot13);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9, slot10, slot11,
									slot13);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val, slot10, slot11,
									slot13);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, key, val,
									slot13);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 13:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9, slot10, slot11,
									slot12);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val, slot10, slot11,
									slot12);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, key, val,
									slot12);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((int) nodeMap());
			result = prime * result + ((int) dataMap());
			result = prime * result + slot0.hashCode();
			result = prime * result + slot1.hashCode();
			result = prime * result + slot2.hashCode();
			result = prime * result + slot3.hashCode();
			result = prime * result + slot4.hashCode();
			result = prime * result + slot5.hashCode();
			result = prime * result + slot6.hashCode();
			result = prime * result + slot7.hashCode();
			result = prime * result + slot8.hashCode();
			result = prime * result + slot9.hashCode();
			result = prime * result + slot10.hashCode();
			result = prime * result + slot11.hashCode();
			result = prime * result + slot12.hashCode();
			result = prime * result + slot13.hashCode();
			return result;
		}

		@Override
		public boolean equals(final Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Map0To14Node_5Bits_Untyped_Spec0To8<?, ?> that = (Map0To14Node_5Bits_Untyped_Spec0To8<?, ?>) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(slot0.equals(that.slot0))) {
				return false;
			}
			if (!(slot1.equals(that.slot1))) {
				return false;
			}
			if (!(slot2.equals(that.slot2))) {
				return false;
			}
			if (!(slot3.equals(that.slot3))) {
				return false;
			}
			if (!(slot4.equals(that.slot4))) {
				return false;
			}
			if (!(slot5.equals(that.slot5))) {
				return false;
			}
			if (!(slot6.equals(that.slot6))) {
				return false;
			}
			if (!(slot7.equals(that.slot7))) {
				return false;
			}
			if (!(slot8.equals(that.slot8))) {
				return false;
			}
			if (!(slot9.equals(that.slot9))) {
				return false;
			}
			if (!(slot10.equals(that.slot10))) {
				return false;
			}
			if (!(slot11.equals(that.slot11))) {
				return false;
			}
			if (!(slot12.equals(that.slot12))) {
				return false;
			}
			if (!(slot13.equals(that.slot13))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To15Node_5Bits_Untyped_Spec0To8<K, V> extends
					CompactMixedMapNode<K, V> {

		private final Object slot0;
		private final Object slot1;
		private final Object slot2;
		private final Object slot3;
		private final Object slot4;
		private final Object slot5;
		private final Object slot6;
		private final Object slot7;
		private final Object slot8;
		private final Object slot9;
		private final Object slot10;
		private final Object slot11;
		private final Object slot12;
		private final Object slot13;
		private final Object slot14;

		Map0To15Node_5Bits_Untyped_Spec0To8(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6,
						final Object slot7, final Object slot8, final Object slot9,
						final Object slot10, final Object slot11, final Object slot12,
						final Object slot13, final Object slot14) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
			this.slot5 = slot5;
			this.slot6 = slot6;
			this.slot7 = slot7;
			this.slot8 = slot8;
			this.slot9 = slot9;
			this.slot10 = slot10;
			this.slot11 = slot11;
			this.slot12 = slot12;
			this.slot13 = slot13;
			this.slot14 = slot14;

			assert nodeInvariant();
		}

		@Override
		boolean hasSlots() {
			return true;
		}

		@Override
		int slotArity() {
			return 15;
		}

		@Override
		Object getSlot(final int index) {
			switch (index) {
			case 0:
				return slot0;
			case 1:
				return slot1;
			case 2:
				return slot2;
			case 3:
				return slot3;
			case 4:
				return slot4;
			case 5:
				return slot5;
			case 6:
				return slot6;
			case 7:
				return slot7;
			case 8:
				return slot8;
			case 9:
				return slot9;
			case 10:
				return slot10;
			case 11:
				return slot11;
			case 12:
				return slot12;
			case 13:
				return slot13;
			case 14:
				return slot14;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		K getKey(int index) {
			return (K) getSlot(TUPLE_LENGTH * index);
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(int index) {
			return (V) getSlot(TUPLE_LENGTH * index + 1);
		}

		@SuppressWarnings("unchecked")
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf((K) getSlot(TUPLE_LENGTH * index), (V) getSlot(TUPLE_LENGTH * index + 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		public CompactMapNode<K, V> getNode(int index) {
			final int offset = TUPLE_LENGTH * payloadArity();
			return (CompactMapNode<K, V>) getSlot(offset + index);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			final int offset = TUPLE_LENGTH * payloadArity();
			final Object[] nodes = new Object[15 - offset];

			for (int i = 0; i < 15 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 15;
		}

		@Override
		int nodeArity() {
			return 15 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount(dataMap());
		}

		@Override
		byte sizePredicate() {
			if (this.nodeArity() == 0 && this.payloadArity() == 0) {
				return SIZE_EMPTY;
			} else if (this.nodeArity() == 0 && this.payloadArity() == 1) {
				return SIZE_ONE;
			} else {
				return SIZE_MORE_THAN_ONE;
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final int bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11, slot12, slot13, slot14);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val, slot12, slot13, slot14);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, val, slot14);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, final int bitpos,
						final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val, slot10, slot11, slot12,
								slot13, slot14);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, key, val, slot12,
								slot13, slot14);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, key,
								val, slot14);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final int bitpos) {
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11, slot12, slot13, slot14);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot12, slot13, slot14);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot14);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final int bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11, slot12, slot13, slot14);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11, slot12, slot13, slot14);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11, slot12, slot13, slot14);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot12, slot13, slot14);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot13, slot14);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot14);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = TUPLE_LENGTH * (payloadArity() - 1) + nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() | bitpos);
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, node, slot2, slot3, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot2, node, slot3, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, node, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, node, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, node,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
									slot14);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
									slot14);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
									slot14);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
									slot14);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
									slot14);
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
									slot14);
				case 13:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
									node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, node, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, node,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
									slot14);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
									slot14);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
									slot14);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
									slot14);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
									slot14);
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
									slot14);
				case 13:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
									node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
									slot14);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
									slot14);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
									slot14);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
									slot14);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
									slot14);
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
									slot14);
				case 13:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
									node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, node, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, node, slot9, slot10, slot11, slot12, slot13,
									slot14);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, node, slot10, slot11, slot12, slot13,
									slot14);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, node, slot11, slot12, slot13,
									slot14);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, slot11, node, slot12, slot13,
									slot14);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, slot11, slot12, node, slot13,
									slot14);
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, slot11, slot12, slot13, node,
									slot14);
				case 13:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
									node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (bitIndex) {
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, node, slot10, slot11, slot12, slot13,
									slot14);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, node, slot11, slot12, slot13,
									slot14);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, slot11, node, slot12, slot13,
									slot14);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, slot11, slot12, node, slot13,
									slot14);
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, slot11, slot12, slot13, node,
									slot14);
				case 13:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, slot11, slot12, slot13, slot14,
									node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (bitIndex) {
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, node, slot12, slot13, slot14);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot12, node, slot13, slot14);
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot12, slot13, node, slot14);
				case 13:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot12, slot13, slot14, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 6:
				switch (bitIndex) {
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, node, slot14);
				case 13:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot14, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() ^ bitpos);
			final int dataMap = (int) (this.dataMap() | bitpos);

			final K key = node.getKey(0);
			final V val = node.getValue(0);

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 6:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 7:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 8:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot9, slot10, slot11, slot12,
									slot13, slot14);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot9, slot10, slot11, slot12,
									slot13, slot14);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot9, slot10, slot11, slot12,
									slot13, slot14);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot9, slot10, slot11, slot12,
									slot13, slot14);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot9, slot10, slot11, slot12,
									slot13, slot14);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 9:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot10, slot11, slot12,
									slot13, slot14);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot10, slot11, slot12,
									slot13, slot14);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot10, slot11, slot12,
									slot13, slot14);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot10, slot11, slot12,
									slot13, slot14);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot10, slot11, slot12,
									slot13, slot14);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 10:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot11, slot12,
									slot13, slot14);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot11, slot12,
									slot13, slot14);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9, slot11, slot12,
									slot13, slot14);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9, slot11, slot12,
									slot13, slot14);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9, slot11, slot12,
									slot13, slot14);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val, slot11, slot12,
									slot13, slot14);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 11:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot12,
									slot13, slot14);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot12,
									slot13, slot14);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot12,
									slot13, slot14);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9, slot10, slot12,
									slot13, slot14);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9, slot10, slot12,
									slot13, slot14);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val, slot10, slot12,
									slot13, slot14);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 12:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot13, slot14);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot13, slot14);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot13, slot14);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9, slot10, slot11,
									slot13, slot14);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9, slot10, slot11,
									slot13, slot14);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val, slot10, slot11,
									slot13, slot14);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, key, val,
									slot13, slot14);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 13:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12, slot14);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12, slot14);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12, slot14);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12, slot14);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9, slot10, slot11,
									slot12, slot14);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val, slot10, slot11,
									slot12, slot14);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, key, val,
									slot12, slot14);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 14:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12, slot13);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12, slot13);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12, slot13);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12, slot13);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9, slot10, slot11,
									slot12, slot13);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val, slot10, slot11,
									slot12, slot13);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, key, val,
									slot12, slot13);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, key, val);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((int) nodeMap());
			result = prime * result + ((int) dataMap());
			result = prime * result + slot0.hashCode();
			result = prime * result + slot1.hashCode();
			result = prime * result + slot2.hashCode();
			result = prime * result + slot3.hashCode();
			result = prime * result + slot4.hashCode();
			result = prime * result + slot5.hashCode();
			result = prime * result + slot6.hashCode();
			result = prime * result + slot7.hashCode();
			result = prime * result + slot8.hashCode();
			result = prime * result + slot9.hashCode();
			result = prime * result + slot10.hashCode();
			result = prime * result + slot11.hashCode();
			result = prime * result + slot12.hashCode();
			result = prime * result + slot13.hashCode();
			result = prime * result + slot14.hashCode();
			return result;
		}

		@Override
		public boolean equals(final Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Map0To15Node_5Bits_Untyped_Spec0To8<?, ?> that = (Map0To15Node_5Bits_Untyped_Spec0To8<?, ?>) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(slot0.equals(that.slot0))) {
				return false;
			}
			if (!(slot1.equals(that.slot1))) {
				return false;
			}
			if (!(slot2.equals(that.slot2))) {
				return false;
			}
			if (!(slot3.equals(that.slot3))) {
				return false;
			}
			if (!(slot4.equals(that.slot4))) {
				return false;
			}
			if (!(slot5.equals(that.slot5))) {
				return false;
			}
			if (!(slot6.equals(that.slot6))) {
				return false;
			}
			if (!(slot7.equals(that.slot7))) {
				return false;
			}
			if (!(slot8.equals(that.slot8))) {
				return false;
			}
			if (!(slot9.equals(that.slot9))) {
				return false;
			}
			if (!(slot10.equals(that.slot10))) {
				return false;
			}
			if (!(slot11.equals(that.slot11))) {
				return false;
			}
			if (!(slot12.equals(that.slot12))) {
				return false;
			}
			if (!(slot13.equals(that.slot13))) {
				return false;
			}
			if (!(slot14.equals(that.slot14))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To16Node_5Bits_Untyped_Spec0To8<K, V> extends
					CompactMixedMapNode<K, V> {

		private final Object slot0;
		private final Object slot1;
		private final Object slot2;
		private final Object slot3;
		private final Object slot4;
		private final Object slot5;
		private final Object slot6;
		private final Object slot7;
		private final Object slot8;
		private final Object slot9;
		private final Object slot10;
		private final Object slot11;
		private final Object slot12;
		private final Object slot13;
		private final Object slot14;
		private final Object slot15;

		Map0To16Node_5Bits_Untyped_Spec0To8(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object slot0,
						final Object slot1, final Object slot2, final Object slot3,
						final Object slot4, final Object slot5, final Object slot6,
						final Object slot7, final Object slot8, final Object slot9,
						final Object slot10, final Object slot11, final Object slot12,
						final Object slot13, final Object slot14, final Object slot15) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
			this.slot5 = slot5;
			this.slot6 = slot6;
			this.slot7 = slot7;
			this.slot8 = slot8;
			this.slot9 = slot9;
			this.slot10 = slot10;
			this.slot11 = slot11;
			this.slot12 = slot12;
			this.slot13 = slot13;
			this.slot14 = slot14;
			this.slot15 = slot15;

			assert nodeInvariant();
		}

		@Override
		boolean hasSlots() {
			return true;
		}

		@Override
		int slotArity() {
			return 16;
		}

		@Override
		Object getSlot(final int index) {
			switch (index) {
			case 0:
				return slot0;
			case 1:
				return slot1;
			case 2:
				return slot2;
			case 3:
				return slot3;
			case 4:
				return slot4;
			case 5:
				return slot5;
			case 6:
				return slot6;
			case 7:
				return slot7;
			case 8:
				return slot8;
			case 9:
				return slot9;
			case 10:
				return slot10;
			case 11:
				return slot11;
			case 12:
				return slot12;
			case 13:
				return slot13;
			case 14:
				return slot14;
			case 15:
				return slot15;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		K getKey(int index) {
			return (K) getSlot(TUPLE_LENGTH * index);
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(int index) {
			return (V) getSlot(TUPLE_LENGTH * index + 1);
		}

		@SuppressWarnings("unchecked")
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf((K) getSlot(TUPLE_LENGTH * index), (V) getSlot(TUPLE_LENGTH * index + 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		public CompactMapNode<K, V> getNode(int index) {
			final int offset = TUPLE_LENGTH * payloadArity();
			return (CompactMapNode<K, V>) getSlot(offset + index);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			final int offset = TUPLE_LENGTH * payloadArity();
			final Object[] nodes = new Object[16 - offset];

			for (int i = 0; i < 16 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 16;
		}

		@Override
		int nodeArity() {
			return 16 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount(dataMap());
		}

		@Override
		byte sizePredicate() {
			if (this.nodeArity() == 0 && this.payloadArity() == 0) {
				return SIZE_EMPTY;
			} else if (this.nodeArity() == 0 && this.payloadArity() == 1) {
				return SIZE_ONE;
			} else {
				return SIZE_MORE_THAN_ONE;
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final int bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11, slot12, slot13, slot14,
								slot15);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val, slot12, slot13, slot14,
								slot15);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, val, slot14,
								slot15);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, final int bitpos,
						final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val, slot10, slot11, slot12,
								slot13, slot14, slot15);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, key, val, slot12,
								slot13, slot14, slot15);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, key,
								val, slot14, slot15);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final int bitpos) {
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap());
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11, slot12, slot13, slot14, slot15);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot12, slot13, slot14, slot15);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot14, slot15);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final int bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final int nodeMap = this.nodeMap();
			final int dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11, slot12, slot13, slot14,
								slot15);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11, slot12, slot13, slot14,
								slot15);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot12, slot13, slot14,
								slot15);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot13, slot14,
								slot15);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot14,
								slot15);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot15);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = TUPLE_LENGTH * (payloadArity() - 1) + nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() | bitpos);
			final int dataMap = (int) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, node, slot2, slot3, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot2, node, slot3, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, node, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, node, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, node,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
									slot14, slot15);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
									slot14, slot15);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
									slot14, slot15);
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
									slot14, slot15);
				case 13:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
									node, slot15);
				case 14:
					return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
									slot15, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot4, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, node, slot5,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, node,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
									slot14, slot15);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
									slot14, slot15);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
									slot14, slot15);
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
									slot14, slot15);
				case 13:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
									node, slot15);
				case 14:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
									slot15, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node,
									slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
									slot14, slot15);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
									slot14, slot15);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
									slot14, slot15);
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
									slot14, slot15);
				case 13:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
									node, slot15);
				case 14:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6,
									slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
									slot15, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, node, slot8, slot9, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, node, slot9, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, node, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, node, slot11, slot12, slot13,
									slot14, slot15);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, slot11, node, slot12, slot13,
									slot14, slot15);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, slot11, slot12, node, slot13,
									slot14, slot15);
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, slot11, slot12, slot13, node,
									slot14, slot15);
				case 13:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
									node, slot15);
				case 14:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
									slot15, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (bitIndex) {
				case 8:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, node, slot10, slot11, slot12, slot13,
									slot14, slot15);
				case 9:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, node, slot11, slot12, slot13,
									slot14, slot15);
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, slot11, node, slot12, slot13,
									slot14, slot15);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, slot11, slot12, node, slot13,
									slot14, slot15);
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, slot11, slot12, slot13, node,
									slot14, slot15);
				case 13:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, slot11, slot12, slot13, slot14,
									node, slot15);
				case 14:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot10, slot11, slot12, slot13, slot14,
									slot15, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (bitIndex) {
				case 10:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, node, slot12, slot13,
									slot14, slot15);
				case 11:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot12, node, slot13,
									slot14, slot15);
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot12, slot13, node,
									slot14, slot15);
				case 13:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot12, slot13, slot14,
									node, slot15);
				case 14:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot12, slot13, slot14,
									slot15, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 6:
				switch (bitIndex) {
				case 12:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, node,
									slot14, slot15);
				case 13:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot14,
									node, slot15);
				case 14:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot14,
									slot15, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 7:
				switch (bitIndex) {
				case 14:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = dataIndex(bitpos);

			final int nodeMap = (int) (this.nodeMap() ^ bitpos);
			final int dataMap = (int) (this.dataMap() | bitpos);

			final K key = node.getKey(0);
			final V val = node.getValue(0);

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 6:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 7:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot8, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 8:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot9, slot10, slot11, slot12,
									slot13, slot14, slot15);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 9:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot10, slot11, slot12,
									slot13, slot14, slot15);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot10, slot11, slot12,
									slot13, slot14, slot15);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot10, slot11, slot12,
									slot13, slot14, slot15);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot10, slot11, slot12,
									slot13, slot14, slot15);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot10, slot11, slot12,
									slot13, slot14, slot15);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 10:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot11, slot12,
									slot13, slot14, slot15);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot11, slot12,
									slot13, slot14, slot15);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9, slot11, slot12,
									slot13, slot14, slot15);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9, slot11, slot12,
									slot13, slot14, slot15);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9, slot11, slot12,
									slot13, slot14, slot15);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val, slot11, slot12,
									slot13, slot14, slot15);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 11:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot12,
									slot13, slot14, slot15);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot12,
									slot13, slot14, slot15);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot12,
									slot13, slot14, slot15);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9, slot10, slot12,
									slot13, slot14, slot15);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9, slot10, slot12,
									slot13, slot14, slot15);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val, slot10, slot12,
									slot13, slot14, slot15);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 12:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot13, slot14, slot15);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot13, slot14, slot15);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot13, slot14, slot15);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9, slot10, slot11,
									slot13, slot14, slot15);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9, slot10, slot11,
									slot13, slot14, slot15);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val, slot10, slot11,
									slot13, slot14, slot15);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, key, val,
									slot13, slot14, slot15);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 13:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12, slot14, slot15);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12, slot14, slot15);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12, slot14, slot15);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12, slot14, slot15);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9, slot10, slot11,
									slot12, slot14, slot15);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val, slot10, slot11,
									slot12, slot14, slot15);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, key, val,
									slot12, slot14, slot15);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 14:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12, slot13, slot15);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12, slot13, slot15);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12, slot13, slot15);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12, slot13, slot15);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9, slot10, slot11,
									slot12, slot13, slot15);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val, slot10, slot11,
									slot12, slot13, slot15);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, key, val,
									slot12, slot13, slot15);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, key, val, slot15);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 15:
				switch (valIndex) {
				case 0:
					return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12, slot13, slot14);
				case 1:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12, slot13, slot14);
				case 2:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
									slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12, slot13, slot14);
				case 3:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, key, val, slot6, slot7, slot8, slot9, slot10, slot11,
									slot12, slot13, slot14);
				case 4:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, key, val, slot8, slot9, slot10, slot11,
									slot12, slot13, slot14);
				case 5:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, key, val, slot10, slot11,
									slot12, slot13, slot14);
				case 6:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, key, val,
									slot12, slot13, slot14);
				case 7:
					return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
									slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
									slot13, key, val, slot14);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((int) nodeMap());
			result = prime * result + ((int) dataMap());
			result = prime * result + slot0.hashCode();
			result = prime * result + slot1.hashCode();
			result = prime * result + slot2.hashCode();
			result = prime * result + slot3.hashCode();
			result = prime * result + slot4.hashCode();
			result = prime * result + slot5.hashCode();
			result = prime * result + slot6.hashCode();
			result = prime * result + slot7.hashCode();
			result = prime * result + slot8.hashCode();
			result = prime * result + slot9.hashCode();
			result = prime * result + slot10.hashCode();
			result = prime * result + slot11.hashCode();
			result = prime * result + slot12.hashCode();
			result = prime * result + slot13.hashCode();
			result = prime * result + slot14.hashCode();
			result = prime * result + slot15.hashCode();
			return result;
		}

		@Override
		public boolean equals(final Object other) {
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
			Map0To16Node_5Bits_Untyped_Spec0To8<?, ?> that = (Map0To16Node_5Bits_Untyped_Spec0To8<?, ?>) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(slot0.equals(that.slot0))) {
				return false;
			}
			if (!(slot1.equals(that.slot1))) {
				return false;
			}
			if (!(slot2.equals(that.slot2))) {
				return false;
			}
			if (!(slot3.equals(that.slot3))) {
				return false;
			}
			if (!(slot4.equals(that.slot4))) {
				return false;
			}
			if (!(slot5.equals(that.slot5))) {
				return false;
			}
			if (!(slot6.equals(that.slot6))) {
				return false;
			}
			if (!(slot7.equals(that.slot7))) {
				return false;
			}
			if (!(slot8.equals(that.slot8))) {
				return false;
			}
			if (!(slot9.equals(that.slot9))) {
				return false;
			}
			if (!(slot10.equals(that.slot10))) {
				return false;
			}
			if (!(slot11.equals(that.slot11))) {
				return false;
			}
			if (!(slot12.equals(that.slot12))) {
				return false;
			}
			if (!(slot13.equals(that.slot13))) {
				return false;
			}
			if (!(slot14.equals(that.slot14))) {
				return false;
			}
			if (!(slot15.equals(that.slot15))) {
				return false;
			}

			return true;
		}

	}

}