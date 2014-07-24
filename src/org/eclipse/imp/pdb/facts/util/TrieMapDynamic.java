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

import static org.eclipse.imp.pdb.facts.util.AbstractSpecialisedImmutableMap.entryOf;

import java.text.DecimalFormat;
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

@SuppressWarnings("rawtypes")
public class TrieMapDynamic<K, V> extends AbstractImmutableMap<K, V> {

	@SuppressWarnings("unchecked")
	private static final TrieMapDynamic EMPTY_MAP = new TrieMapDynamic(CompactMapNode.EMPTY_NODE, 0, 0);

	private static final boolean DEBUG = false;

	private final AbstractMapNode<K, V> rootNode;
	private final int hashCode;
	private final int cachedSize;

	TrieMapDynamic(AbstractMapNode<K, V> rootNode, int hashCode, int cachedSize) {
		this.rootNode = rootNode;
		this.hashCode = hashCode;
		this.cachedSize = cachedSize;
		if (DEBUG) {
			assert checkHashCodeAndSize(hashCode, cachedSize);
		}
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> ImmutableMap<K, V> of() {
		return TrieMapDynamic.EMPTY_MAP;
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> ImmutableMap<K, V> of(Object... keyValuePairs) {
		if (keyValuePairs.length % 2 != 0) {
			throw new IllegalArgumentException(
							"Length of argument list is uneven: no key/value pairs.");
		}

		ImmutableMap<K, V> result = TrieMapDynamic.EMPTY_MAP;

		for (int i = 0; i < keyValuePairs.length; i += 2) {
			final K key = (K) keyValuePairs[i];
			final V val = (V) keyValuePairs[i + 1];

			result = result.__put(key, val);
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> TransientMap<K, V> transientOf() {
		return TrieMapDynamic.EMPTY_MAP.asTransient();
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> TransientMap<K, V> transientOf(Object... keyValuePairs) {
		if (keyValuePairs.length % 2 != 0) {
			throw new IllegalArgumentException(
							"Length of argument list is uneven: no key/value pairs.");
		}

		final TransientMap<K, V> result = TrieMapDynamic.EMPTY_MAP.asTransient();

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

	@Override
	public TrieMapDynamic<K, V> __put(final K key, final V val) {
		final int keyHash = key.hashCode();
		final Result<K, V, ? extends CompactMapNode<K, V>> result = rootNode.updated(null, key,
						val, keyHash, 0);

		if (result.isModified()) {

			if (result.hasReplacedValue()) {
				final int valHashOld = result.getReplacedValue().hashCode();
				final int valHashNew = val.hashCode();

				return new TrieMapDynamic<K, V>(result.getNode(), hashCode + (keyHash ^ valHashNew)
								- (keyHash ^ valHashOld), cachedSize);
			}

			final int valHash = val.hashCode();
			return new TrieMapDynamic<K, V>(result.getNode(), hashCode + (keyHash ^ valHash),
							cachedSize + 1);

		}

		return this;
	}

	@Override
	public TrieMapDynamic<K, V> __putEquivalent(final K key, final V val, Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final Result<K, V, ? extends CompactMapNode<K, V>> result = rootNode.updated(null, key,
						val, keyHash, 0, cmp);

		if (result.isModified()) {

			if (result.hasReplacedValue()) {
				final int valHashOld = result.getReplacedValue().hashCode();
				final int valHashNew = val.hashCode();

				return new TrieMapDynamic<K, V>(result.getNode(), hashCode + (keyHash ^ valHashNew)
								- (keyHash ^ valHashOld), cachedSize);
			}

			final int valHash = val.hashCode();
			return new TrieMapDynamic<K, V>(result.getNode(), hashCode + (keyHash ^ valHash),
							cachedSize + 1);

		}

		return this;
	}

	@Override
	public ImmutableMap<K, V> __putAll(Map<? extends K, ? extends V> map) {
		TransientMap<K, V> tmp = asTransient();
		tmp.__putAll(map);
		return tmp.freeze();
	}

	@Override
	public ImmutableMap<K, V> __putAllEquivalent(Map<? extends K, ? extends V> map,
					Comparator<Object> cmp) {
		TransientMap<K, V> tmp = asTransient();
		tmp.__putAllEquivalent(map, cmp);
		return tmp.freeze();
	}

	@Override
	public TrieMapDynamic<K, V> __remove(final K key) {
		final int keyHash = key.hashCode();
		final Result<K, V, ? extends CompactMapNode<K, V>> result = rootNode.removed(null, key,
						keyHash, 0);

		if (result.isModified()) {

			// TODO: carry deleted value in result
			// assert result.hasReplacedValue();
			// final int valHash = result.getReplacedValue().hashCode();

			final int valHash = rootNode.findByKey(key, keyHash, 0).get().hashCode();

			return new TrieMapDynamic<K, V>(result.getNode(), hashCode - (keyHash ^ valHash),
							cachedSize - 1);

		}

		return this;
	}

	@Override
	public TrieMapDynamic<K, V> __removeEquivalent(final K key, Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final Result<K, V, ? extends CompactMapNode<K, V>> result = rootNode.removed(null, key,
						keyHash, 0, cmp);

		if (result.isModified()) {

			// TODO: carry deleted value in result
			// assert result.hasReplacedValue();
			// final int valHash = result.getReplacedValue().hashCode();

			final int valHash = rootNode.findByKey(key, keyHash, 0, cmp).get().hashCode();

			return new TrieMapDynamic<K, V>(result.getNode(), hashCode - (keyHash ^ valHash),
							cachedSize - 1);

		}

		return this;
	}

	@Override
	public boolean containsKey(Object o) {
		try {
			final K key = (K) o;
			return rootNode.containsKey(key, key.hashCode(), 0);
		} catch (ClassCastException unused) {
			return false;
		}
	}

	@Override
	public boolean containsKeyEquivalent(Object o, Comparator<Object> cmp) {
		try {
			final K key = (K) o;
			return rootNode.containsKey(key, key.hashCode(), 0, cmp);
		} catch (ClassCastException unused) {
			return false;
		}
	}

	@Override
	public boolean containsValue(Object o) {
		for (Iterator<V> iterator = valueIterator(); iterator.hasNext();) {
			if (iterator.next().equals(o)) {
				return true;
			}
		}
		return false;
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
	public V get(Object o) {
		try {
			final K key = (K) o;
			final Optional<V> result = rootNode.findByKey(key, key.hashCode(), 0);

			if (result.isPresent()) {
				return result.get();
			} else {
				return null;
			}
		} catch (ClassCastException unused) {
			return null;
		}
	}

	@Override
	public V getEquivalent(Object o, Comparator<Object> cmp) {
		try {
			final K key = (K) o;
			final Optional<V> result = rootNode.findByKey(key, key.hashCode(), 0, cmp);

			if (result.isPresent()) {
				return result.get();
			} else {
				return null;
			}
		} catch (ClassCastException unused) {
			return null;
		}
	}

	@Override
	public int size() {
		return cachedSize;
	}

	@Override
	public SupplierIterator<K, V> keyIterator() {
		return new MapKeyIterator<>(rootNode);
	}

	@Override
	public Iterator<V> valueIterator() {
		return new MapValueIterator<>(rootNode);
	}

	@Override
	public Iterator<Map.Entry<K, V>> entryIterator() {
		return new MapEntryIterator<>(rootNode);
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
					return TrieMapDynamic.this.size();
				}

				@Override
				public boolean isEmpty() {
					return TrieMapDynamic.this.isEmpty();
				}

				@SuppressWarnings("deprecation")
				@Override
				public void clear() {
					TrieMapDynamic.this.clear();
				}

				@Override
				public boolean contains(Object k) {
					return TrieMapDynamic.this.containsKey(k);
				}
			};
		}
		return entrySet;
	}

	@Override
	public boolean isTransientSupported() {
		return true;
	}

	@Override
	public TransientMap<K, V> asTransient() {
		return new TransientTrieMap<K, V>(this);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (other == null) {
			return false;
		}

		if (other instanceof TrieMapDynamic) {
			TrieMapDynamic<?, ?> that = (TrieMapDynamic<?, ?>) other;

			if (this.size() != that.size()) {
				return false;
			}

			return rootNode.equals(that.rootNode);
		}

		return super.equals(other);
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
		return new TrieMapNodeIterator<>(rootNode);
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
		final int[][] sumArityCombinations = new int[17][17];

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
		final int[] sumArity = new int[17];

		final int maxArity = 16; // TODO: factor out constant

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

		final int[] cumsumArity = new int[17];
		for (int cumsum = 0, i = 0; i < 17; i++) {
			cumsum += sumArity[i];
			cumsumArity[i] = cumsum;
		}

		final float threshhold = 0.01f; // for printing results
		for (int i = 0; i < 17; i++) {
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

	protected static abstract class AbstractNode<K, V> {
	}

	protected static abstract class AbstractMapNode<K, V> extends AbstractNode<K, V> {

		static final int TUPLE_LENGTH = 2;

		abstract boolean containsKey(final K key, int keyHash, int shift);

		abstract boolean containsKey(final K key, int keyHash, int shift, Comparator<Object> cmp);

		abstract Optional<V> findByKey(final K key, int keyHash, int shift);

		abstract Optional<V> findByKey(final K key, int keyHash, int shift, Comparator<Object> cmp);

		abstract Result<K, V, ? extends CompactMapNode<K, V>> updated(
						AtomicReference<Thread> mutator, final K key, final V val, int keyHash,
						int shift);

		abstract Result<K, V, ? extends CompactMapNode<K, V>> updated(
						AtomicReference<Thread> mutator, final K key, final V val, int keyHash,
						int shift, Comparator<Object> cmp);

		abstract Result<K, V, ? extends CompactMapNode<K, V>> removed(
						AtomicReference<Thread> mutator, final K key, int keyHash, int shift);

		abstract Result<K, V, ? extends CompactMapNode<K, V>> removed(
						AtomicReference<Thread> mutator, final K key, int keyHash, int shift,
						Comparator<Object> cmp);

		static final boolean isAllowedToEdit(AtomicReference<Thread> x, AtomicReference<Thread> y) {
			return x != null && y != null && (x == y || x.get() == y.get());
		}

		abstract K getKey(int index);

		abstract V getValue(int index);

		abstract java.util.Map.Entry<K, V> getKeyValueEntry(int index);

		abstract AbstractMapNode<K, V> getNode(int index);

		abstract boolean hasNodes();

		@Deprecated
		Iterator<? extends AbstractMapNode<K, V>> nodeIterator() {
			return new Iterator<AbstractMapNode<K, V>>() {

				int nextIndex = 0;

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
					return nextIndex < AbstractMapNode.this.nodeArity();
				}
			};
		}

		abstract int nodeArity();

		abstract boolean hasPayload();

		@Deprecated
		SupplierIterator<K, V> payloadIterator() {
			return new SupplierIterator<K, V>() {

				int nextIndex = 0;

				@Override
				public V get() {
					if (nextIndex == 0 || nextIndex > AbstractMapNode.this.payloadArity()) {
						throw new NoSuchElementException();
					}

					return AbstractMapNode.this.getValue(nextIndex - 1);
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				@Override
				public K next() {
					if (!hasNext())
						throw new NoSuchElementException();
					return AbstractMapNode.this.getKey(nextIndex++);
				}

				@Override
				public boolean hasNext() {
					return nextIndex < AbstractMapNode.this.payloadArity();
				}
			};
		}

		abstract int payloadArity();

		abstract java.lang.Object getSlot(int index);

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
			final SupplierIterator<K, V> it = new MapKeyIterator<>(this);

			int size = 0;
			while (it.hasNext()) {
				size += 1;
				it.next();
			}

			return size;
		}
	}

	private static abstract class CompactMapNode<K, V> extends AbstractMapNode<K, V> {

		protected static final int BIT_PARTITION_SIZE = 4;
		protected static final int BIT_PARTITION_MASK = 0b1111;

		short nodeMap() {
			throw new UnsupportedOperationException();
		}

		short dataMap() {
			throw new UnsupportedOperationException();
		}

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

		/**
		 * Returns the first key stored within this node.
		 * 
		 * @return first key
		 */
		@Deprecated
		K headKey() {
			return getKey(0);
		}

		/**
		 * Returns the first value stored within this node.
		 * 
		 * @return first value
		 */
		@Deprecated
		V headVal() {
			return getValue(0);
		}

		@Override
		abstract CompactMapNode<K, V> getNode(int index);

		@Deprecated
		@Override
		Iterator<? extends CompactMapNode<K, V>> nodeIterator() {
			throw new UnsupportedOperationException();
		}

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

		abstract CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator,
						final short bitpos, final V val);

		abstract CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val);

		abstract CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator,
						final short bitpos);

		abstract CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator,
						final short bitpos, CompactMapNode<K, V> node);

		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			throw new UnsupportedOperationException();
		}

		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			throw new UnsupportedOperationException();
		}

		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						final short bitpos, CompactMapNode<K, V> node) {
			throw new UnsupportedOperationException();
		}

		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						final short bitpos, CompactMapNode<K, V> node) {
			throw new UnsupportedOperationException();
		}

		@SuppressWarnings("unchecked")
		static final <K, V> CompactMapNode<K, V> mergeNodes(final K key0, final V val0,
						int keyHash0, final K key1, final V val1, int keyHash1, int shift) {
			assert !(key0.equals(key1));

			if (keyHash0 == keyHash1) {
				return new HashCollisionMapNode<>(keyHash0, (K[]) new Object[] { key0, key1 },
								(V[]) new Object[] { val0, val1 });
			}

			final int mask0 = (keyHash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (keyHash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level
				final short dataMap = (short) (1L << mask0 | 1L << mask1);

				if (mask0 < mask1) {
					return nodeOf(null, (short) 0, dataMap,
									new Object[] { key0, val0, key1, val1 }, (byte) 2);
				} else {
					return nodeOf(null, (short) 0, dataMap,
									new Object[] { key1, val1, key0, val0 }, (byte) 2);
				}
			} else {
				// values fit on next level
				final CompactMapNode<K, V> node = mergeNodes(key0, val0, keyHash0, key1, val1,
								keyHash1, shift + BIT_PARTITION_SIZE);

				final short nodeMap = (short) (1L << mask0);
				return nodeOf(null, nodeMap, (short) 0, new Object[] { node }, (byte) 0);
			}
		}

		static final <K, V> CompactMapNode<K, V> mergeNodes(CompactMapNode<K, V> node0,
						int keyHash0, final K key1, final V val1, int keyHash1, int shift) {
			final int mask0 = (keyHash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (keyHash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level
				final short nodeMap = (short) (1L << mask0);
				final short dataMap = (short) (1L << mask1);

				// store values before node
				return nodeOf(null, nodeMap, dataMap, new Object[] { key1, val1, node0 }, (byte) 1);
			} else {
				// values fit on next level
				final CompactMapNode<K, V> node = mergeNodes(node0, keyHash0, key1, val1, keyHash1,
								shift + BIT_PARTITION_SIZE);

				final short nodeMap = (short) (1L << mask0);
				return nodeOf(null, nodeMap, (short) 0, new Object[] { node }, (byte) 0);
			}
		}

		static final CompactMapNode EMPTY_NODE;

		static {
			EMPTY_NODE = new BitmapIndexedMapNode<>(null, (short) 0, (short) 0, new Object[] {},
							(byte) 0);
		};

		static final <K, V> CompactMapNode<K, V> nodeOf(AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, Object[] nodes, byte payloadArity) {
			return new BitmapIndexedMapNode<>(mutator, nodeMap, dataMap, nodes, payloadArity);
		}

		@SuppressWarnings("unchecked")
		static final <K, V> CompactMapNode<K, V> nodeOf(AtomicReference<Thread> mutator) {
			return EMPTY_NODE;
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final K key, final V val) {
			assert nodeMap == 0;
			return nodeOf(mutator, (short) 0, dataMap, new Object[] { key, val }, (byte) 1);
		}

		final int dataIndex(final short bitpos) {
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF) & (bitpos - 1));
		}

		final int nodeIndex(final short bitpos) {
			return java.lang.Integer.bitCount((int) (nodeMap() & 0xFFFF) & (bitpos - 1));
		}

		K keyAt(final short bitpos) {
			return getKey(dataIndex(bitpos));
		}

		V valAt(final short bitpos) {
			return getValue(dataIndex(bitpos));
		}

		CompactMapNode<K, V> nodeAt(final short bitpos) {
			return getNode(nodeIndex(bitpos));
		}

		@Override
		boolean containsKey(final K key, int keyHash, int shift) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final short bitpos = (short) (1L << mask);

			if ((dataMap() & bitpos) != 0) {
				return keyAt(bitpos).equals(key);
			}

			if ((nodeMap() & bitpos) != 0) {
				return nodeAt(bitpos).containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			}

			return false;
		}

		@Override
		boolean containsKey(final K key, int keyHash, int shift, Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final short bitpos = (short) (1L << mask);

			if ((dataMap() & bitpos) != 0) {
				return cmp.compare(keyAt(bitpos), key) == 0;
			}

			if ((nodeMap() & bitpos) != 0) {
				return nodeAt(bitpos).containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<V> findByKey(final K key, int keyHash, int shift) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final short bitpos = (short) (1L << mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				if (keyAt(bitpos).equals(key)) {
					final V _val = valAt(bitpos);

					return Optional.of(_val);
				}

				return Optional.empty();
			}

			if ((nodeMap() & bitpos) != 0) { // node (not value)
				final AbstractMapNode<K, V> subNode = nodeAt(bitpos);

				return subNode.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			}

			return Optional.empty();
		}

		@Override
		Optional<V> findByKey(final K key, int keyHash, int shift, Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final short bitpos = (short) (1L << mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				if (cmp.compare(keyAt(bitpos), key) == 0) {
					final V _val = valAt(bitpos);

					return Optional.of(_val);
				}

				return Optional.empty();
			}

			if ((nodeMap() & bitpos) != 0) { // node (not value)
				final AbstractMapNode<K, V> subNode = nodeAt(bitpos);

				return subNode.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator,
						final K key, final V val, int keyHash, int shift) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final short bitpos = (short) (1L << mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final K currentKey = keyAt(bitpos);

				if (currentKey.equals(key)) {
					final V currentVal = valAt(bitpos);

					if (currentVal.equals(val)) {
						return Result.unchanged(this);
					}

					// update mapping
					final CompactMapNode<K, V> thisNew = copyAndSetValue(mutator, bitpos, val);

					return Result.updated(thisNew, currentVal);
				} else {
					final CompactMapNode<K, V> nodeNew = mergeNodes(keyAt(bitpos), valAt(bitpos),
									keyAt(bitpos).hashCode(), key, val, keyHash, shift
													+ BIT_PARTITION_SIZE);

					final CompactMapNode<K, V> thisNew = copyAndMigrateFromInlineToNode(mutator,
									bitpos, nodeNew);

					return Result.modified(thisNew);
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactMapNode<K, V> subNode = nodeAt(bitpos);

				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = subNode.updated(
								mutator, key, val, keyHash, shift + BIT_PARTITION_SIZE);

				if (!nestedResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactMapNode<K, V> thisNew = copyAndSetNode(mutator, bitpos,
								nestedResult.getNode());

				if (nestedResult.hasReplacedValue()) {
					return Result.updated(thisNew, nestedResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else {
				// no value
				final CompactMapNode<K, V> thisNew = copyAndInsertValue(mutator, bitpos, key, val);

				return Result.modified(thisNew);
			}
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator,
						final K key, final V val, int keyHash, int shift, Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final short bitpos = (short) (1L << mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final K currentKey = keyAt(bitpos);

				if (cmp.compare(currentKey, key) == 0) {
					final V currentVal = valAt(bitpos);

					if (cmp.compare(currentVal, val) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					final CompactMapNode<K, V> thisNew = copyAndSetValue(mutator, bitpos, val);

					return Result.updated(thisNew, currentVal);
				} else {
					final CompactMapNode<K, V> nodeNew = mergeNodes(keyAt(bitpos), valAt(bitpos),
									keyAt(bitpos).hashCode(), key, val, keyHash, shift
													+ BIT_PARTITION_SIZE);

					final CompactMapNode<K, V> thisNew = copyAndMigrateFromInlineToNode(mutator,
									bitpos, nodeNew);

					return Result.modified(thisNew);
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactMapNode<K, V> subNode = nodeAt(bitpos);

				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = subNode.updated(
								mutator, key, val, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!nestedResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactMapNode<K, V> thisNew = copyAndSetNode(mutator, bitpos,
								nestedResult.getNode());

				if (nestedResult.hasReplacedValue()) {
					return Result.updated(thisNew, nestedResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else {
				// no value
				final CompactMapNode<K, V> thisNew = copyAndInsertValue(mutator, bitpos, key, val);

				return Result.modified(thisNew);
			}
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator,
						final K key, int keyHash, int shift) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final short bitpos = (short) (1L << mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				if (keyAt(bitpos).equals(key)) {
					if (this.payloadArity() == 2 && this.nodeArity() == 0) {
						/*
						 * Create new node with remaining pair. The new node
						 * will a) either become the new root returned, or b)
						 * unwrapped and inlined during returning.
						 */
						final CompactMapNode<K, V> thisNew;
						final short newDataMap = (shift == 0) ? (short) (dataMap() ^ bitpos)
										: (short) (1L << (keyHash & BIT_PARTITION_MASK));

						if (dataIndex(bitpos) == 0) {
							thisNew = CompactMapNode.<K, V> nodeOf(mutator, (short) 0, newDataMap,
											getKey(1), getValue(1));
						} else {
							thisNew = CompactMapNode.<K, V> nodeOf(mutator, (short) 0, newDataMap,
											getKey(0), getValue(0));
						}

						return Result.modified(thisNew);
					} else {
						final CompactMapNode<K, V> thisNew = copyAndRemoveValue(mutator, bitpos);

						return Result.modified(thisNew);
					}
				} else {
					return Result.unchanged(this);
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactMapNode<K, V> subNode = nodeAt(bitpos);
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = subNode.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (!nestedResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactMapNode<K, V> subNodeNew = nestedResult.getNode();

				if (subNodeNew.sizePredicate() == 0) {
					throw new IllegalStateException("Sub-node must have at least one element.");
				}
				assert subNodeNew.sizePredicate() > 0;

				switch (subNodeNew.sizePredicate()) {
				case 1: {
					if (this.payloadArity() == 0 && this.nodeArity() == 1) {
						// escalate (singleton or empty) result
						return nestedResult;
					} else {
						// inline value (move to front)
						final CompactMapNode<K, V> thisNew = copyAndMigrateFromNodeToInline(
										mutator, bitpos, subNodeNew);
						// final CompactMapNode<K, V> thisNew =
						// copyAndRemoveNode(mutator,
						// bitpos).copyAndInsertValue(mutator, bitpos,
						// subNodeNew.getKey(0), subNodeNew.getValue(0));

						return Result.modified(thisNew);
					}
				}
				default: {
					// modify current node (set replacement node)
					final CompactMapNode<K, V> thisNew = copyAndSetNode(mutator, bitpos, subNodeNew);

					return Result.modified(thisNew);
				}
				}
			}

			return Result.unchanged(this);
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator,
						final K key, int keyHash, int shift, Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final short bitpos = (short) (1L << mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				if (cmp.compare(keyAt(bitpos), key) == 0) {
					if (this.payloadArity() == 2 && this.nodeArity() == 0) {
						/*
						 * Create new node with remaining pair. The new node
						 * will a) either become the new root returned, or b)
						 * unwrapped and inlined during returning.
						 */
						final CompactMapNode<K, V> thisNew;
						final short newDataMap = (shift == 0) ? (short) (dataMap() ^ bitpos)
										: (short) (1L << (keyHash & BIT_PARTITION_MASK));

						if (dataIndex(bitpos) == 0) {
							thisNew = CompactMapNode.<K, V> nodeOf(mutator, (short) 0, newDataMap,
											getKey(1), getValue(1));
						} else {
							thisNew = CompactMapNode.<K, V> nodeOf(mutator, (short) 0, newDataMap,
											getKey(0), getValue(0));
						}

						return Result.modified(thisNew);
					} else {
						final CompactMapNode<K, V> thisNew = copyAndRemoveValue(mutator, bitpos);

						return Result.modified(thisNew);
					}
				} else {
					return Result.unchanged(this);
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactMapNode<K, V> subNode = nodeAt(bitpos);
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = subNode.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!nestedResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactMapNode<K, V> subNodeNew = nestedResult.getNode();

				if (subNodeNew.sizePredicate() == 0) {
					throw new IllegalStateException("Sub-node must have at least one element.");
				}
				assert subNodeNew.sizePredicate() > 0;

				switch (subNodeNew.sizePredicate()) {
				case 1: {
					if (this.payloadArity() == 0 && this.nodeArity() == 1) {
						// escalate (singleton or empty) result
						return nestedResult;
					} else {
						// inline value (move to front)
						final CompactMapNode<K, V> thisNew = copyAndMigrateFromNodeToInline(
										mutator, bitpos, subNodeNew);
						// final CompactMapNode<K, V> thisNew =
						// copyAndRemoveNode(mutator,
						// bitpos).copyAndInsertValue(mutator, bitpos,
						// subNodeNew.getKey(0), subNodeNew.getValue(0));

						return Result.modified(thisNew);
					}
				}
				default: {
					// modify current node (set replacement node)
					final CompactMapNode<K, V> thisNew = copyAndSetNode(mutator, bitpos, subNodeNew);

					return Result.modified(thisNew);
				}
				}
			}

			return Result.unchanged(this);
		}

		/**
		 * @return 0 <= mask <= 2^BIT_PARTITION_SIZE - 1
		 */
		static byte recoverMask(short map, byte i_th) {
			assert 1 <= i_th && i_th <= 16;

			byte cnt1 = 0;
			byte mask = 0;

			while (mask < 16) {
				if ((map & 0x01) == 0x01) {
					cnt1 += 1;

					if (cnt1 == i_th) {
						return mask;
					}
				}

				map = (short) (map >> 1);
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

	private static abstract class CompactMixedMapNode<K, V> extends CompactMapNode<K, V> {

		private final short nodeMap;
		private final short dataMap;

		CompactMixedMapNode(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap) {
			this.nodeMap = nodeMap;
			this.dataMap = dataMap;
		}

		@Override
		public short nodeMap() {
			return nodeMap;
		}

		@Override
		public short dataMap() {
			return dataMap;
		}

	}

	private static final class BitmapIndexedMapNode<K, V> extends CompactMixedMapNode<K, V> {
		private AtomicReference<Thread> mutator;

		private Object[] nodes;
		final private byte payloadArity;

		BitmapIndexedMapNode(AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, Object[] nodes, byte payloadArity) {
			super(mutator, nodeMap, dataMap);

			assert (TUPLE_LENGTH * java.lang.Integer.bitCount((int) (dataMap & 0xFFFF))
							+ java.lang.Integer.bitCount((int) (nodeMap & 0xFFFF)) == nodes.length);

			this.mutator = mutator;

			this.nodes = nodes;
			this.payloadArity = payloadArity;

			assert (payloadArity == java.lang.Integer.bitCount((int) (dataMap & 0xFFFF)));
			// assert (payloadArity() >= 2 || nodeArity() >= 1); // =
			// // SIZE_MORE_THAN_ONE

			// for (int i = 0; i < TUPLE_LENGTH * payloadArity; i++)
			// assert ((nodes[i] instanceof CompactNode) == false);
			//
			// for (int i = TUPLE_LENGTH * payloadArity; i < nodes.length; i++)
			// assert ((nodes[i] instanceof CompactNode) == true);

			// assert invariant
			assert nodeInvariant();
		}

		@SuppressWarnings("unchecked")
		@Override
		K getKey(int index) {
			return (K) nodes[TUPLE_LENGTH * index];
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(int index) {
			return (V) nodes[TUPLE_LENGTH * index + 1];
		}

		@SuppressWarnings("unchecked")
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf((K) nodes[TUPLE_LENGTH * index], (V) nodes[TUPLE_LENGTH * index + 1]);
		}

		@SuppressWarnings("unchecked")
		@Override
		public CompactMapNode<K, V> getNode(int index) {
			final int offset = TUPLE_LENGTH * payloadArity;
			return (CompactMapNode<K, V>) nodes[offset + index];
		}

		@Override
		SupplierIterator<K, V> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(nodes, 0, TUPLE_LENGTH * payloadArity);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			final int offset = TUPLE_LENGTH * payloadArity;

			for (int i = offset; i < nodes.length - offset; i++) {
				assert ((nodes[i] instanceof AbstractMapNode) == true);
			}

			return (Iterator) ArrayIterator.of(nodes, offset, nodes.length - offset);
		}

		@SuppressWarnings("unchecked")
		@Override
		K headKey() {
			assert hasPayload();
			return (K) nodes[0];
		}

		@SuppressWarnings("unchecked")
		@Override
		V headVal() {
			assert hasPayload();
			return (V) nodes[1];
		}

		@Override
		boolean hasPayload() {
			return payloadArity != 0;
		}

		@Override
		int payloadArity() {
			return payloadArity;
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity != nodes.length;
		}

		@Override
		int nodeArity() {
			return nodes.length - TUPLE_LENGTH * payloadArity;
		}

		@Override
		java.lang.Object getSlot(int index) {
			throw new UnsupportedOperationException();
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
			if (this.nodeArity() == 0 && this.payloadArity == 0) {
				return SIZE_EMPTY;
			} else if (this.nodeArity() == 0 && this.payloadArity == 1) {
				return SIZE_ONE;
			} else {
				return SIZE_MORE_THAN_ONE;
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = TUPLE_LENGTH * dataIndex(bitpos) + 1;

			if (isAllowedToEdit(this.mutator, mutator)) {
				// no copying if already editable
				this.nodes[idx] = val;
				return this;
			} else {
				final java.lang.Object[] src = this.nodes;
				@SuppressWarnings("unchecked")
				final java.lang.Object[] dst = (java.lang.Object[]) new Object[src.length];

				// copy 'src' and set 1 element(s) at position 'idx'
				System.arraycopy(src, 0, dst, 0, src.length);
				dst[idx + 0] = val;

				return nodeOf(mutator, nodeMap(), dataMap(), dst, payloadArity);
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity + nodeIndex(bitpos);

			if (isAllowedToEdit(this.mutator, mutator)) {
				// no copying if already editable
				this.nodes[idx] = node;
				return this;
			} else {
				final java.lang.Object[] src = this.nodes;
				@SuppressWarnings("unchecked")
				final java.lang.Object[] dst = (java.lang.Object[]) new Object[src.length];

				// copy 'src' and set 1 element(s) at position 'idx'
				System.arraycopy(src, 0, dst, 0, src.length);
				dst[idx + 0] = node;

				return nodeOf(mutator, nodeMap(), dataMap(), dst, payloadArity);
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = TUPLE_LENGTH * dataIndex(bitpos);

			final java.lang.Object[] src = this.nodes;
			@SuppressWarnings("unchecked")
			final java.lang.Object[] dst = (java.lang.Object[]) new Object[src.length + 2];

			// copy 'src' and insert 2 element(s) at position 'idx'
			System.arraycopy(src, 0, dst, 0, idx);
			dst[idx + 0] = key;
			dst[idx + 1] = val;
			System.arraycopy(src, idx, dst, idx + 2, src.length - idx);

			return nodeOf(mutator, nodeMap(), (short) (dataMap() | bitpos), dst,
							(byte) (payloadArity + 1));
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * dataIndex(bitpos);

			final java.lang.Object[] src = this.nodes;
			@SuppressWarnings("unchecked")
			final java.lang.Object[] dst = (java.lang.Object[]) new Object[src.length - 2];

			// copy 'src' and remove 2 element(s) at position 'idx'
			System.arraycopy(src, 0, dst, 0, idx);
			System.arraycopy(src, idx + 2, dst, idx, src.length - idx - 2);

			return nodeOf(mutator, nodeMap(), (short) (dataMap() ^ bitpos), dst,
							(byte) (payloadArity - 1));
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						final short bitpos, CompactMapNode<K, V> node) {
			final int idxOld = TUPLE_LENGTH * dataIndex(bitpos);
			final int idxNew = TUPLE_LENGTH * (payloadArity - 1) + nodeIndex(bitpos);

			final java.lang.Object[] src = this.nodes;
			final java.lang.Object[] dst = new Object[src.length - 2 + 1];

			// copy 'src' and remove 2 element(s) at position 'idxOld' and
			// insert 1 element(s) at position 'idxNew' (TODO: carefully test)
			assert idxOld <= idxNew;
			System.arraycopy(src, 0, dst, 0, idxOld);
			System.arraycopy(src, idxOld + 2, dst, idxOld, idxNew - idxOld);
			dst[idxNew + 0] = node;
			System.arraycopy(src, idxNew + 2, dst, idxNew + 1, src.length - idxNew - 2);

			return nodeOf(mutator, (short) (nodeMap() | bitpos), (short) (dataMap() ^ bitpos), dst,
							(byte) (payloadArity - 1));
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						final short bitpos, CompactMapNode<K, V> node) {
			final int idxOld = TUPLE_LENGTH * payloadArity + nodeIndex(bitpos);
			final int idxNew = dataIndex(bitpos);

			final java.lang.Object[] src = this.nodes;
			final java.lang.Object[] dst = new Object[src.length - 1 + 2];

			// copy 'src' and remove 1 element(s) at position 'idxOld' and
			// insert 2 element(s) at position 'idxNew' (TODO: carefully test)
			assert idxOld >= idxNew;
			System.arraycopy(src, 0, dst, 0, idxNew);
			dst[idxNew + 0] = node.headKey();
			dst[idxNew + 1] = node.headVal();
			System.arraycopy(src, idxNew, dst, idxNew + 2, idxOld - idxNew);
			System.arraycopy(src, idxOld + 1, dst, idxOld + 2, src.length - idxOld - 1);

			return nodeOf(mutator, (short) (nodeMap() ^ bitpos), (short) (dataMap() | bitpos), dst,
							(byte) (payloadArity + 1));
		}
	}

	private static final class HashCollisionMapNode<K, V> extends CompactMapNode<K, V> {
		private final K[] keys;
		private final V[] vals;
		private final int hash;

		HashCollisionMapNode(final int hash, final K[] keys, final V[] vals) {
			this.keys = keys;
			this.vals = vals;
			this.hash = hash;

			assert payloadArity() >= 2;
		}

		@Override
		SupplierIterator<K, V> payloadIterator() {
			// TODO: change representation of keys and values
			assert keys.length == vals.length;

			final Object[] keysAndVals = new Object[keys.length + vals.length];
			for (int i = 0; i < keys.length; i++) {
				keysAndVals[2 * i] = keys[i];
				keysAndVals[2 * i + 1] = vals[i];
			}

			return ArrayKeyValueSupplierIterator.of(keysAndVals);
		}

		@Override
		public boolean containsKey(final K key, int keyHash, int shift) {

			if (this.hash == keyHash) {
				for (K k : keys) {
					if (k.equals(key)) {
						return true;
					}
				}
			}
			return false;

		}

		@Override
		public boolean containsKey(final K key, int keyHash, int shift, Comparator<Object> cmp) {

			if (this.hash == keyHash) {
				for (K k : keys) {
					if (cmp.compare(k, key) == 0) {
						return true;
					}
				}
			}
			return false;

		}

		@Override
		Optional<V> findByKey(final K key, int hash, int shift) {

			for (int i = 0; i < keys.length; i++) {
				final K _key = keys[i];
				if (key.equals(_key)) {
					final V _val = vals[i];
					return Optional.of(_val);
				}
			}
			return Optional.empty();

		}

		@Override
		Optional<V> findByKey(final K key, int hash, int shift, Comparator<Object> cmp) {

			for (int i = 0; i < keys.length; i++) {
				final K _key = keys[i];
				if (cmp.compare(key, _key) == 0) {
					final V _val = vals[i];
					return Optional.of(_val);
				}
			}
			return Optional.empty();

		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator,
						final K key, final V val, int keyHash, int shift) {
			if (this.hash != keyHash) {
				return Result.modified(mergeNodes(this, this.hash, key, val, keyHash, shift));
			}

			for (int idx = 0; idx < keys.length; idx++) {
				if (keys[idx].equals(key)) {

					final V currentVal = vals[idx];

					if (currentVal.equals(val)) {
						return Result.unchanged(this);
					}

					final V[] src = this.vals;
					@SuppressWarnings("unchecked")
					final V[] dst = (V[]) new Object[src.length];

					// copy 'src' and set 1 element(s) at position 'idx'
					System.arraycopy(src, 0, dst, 0, src.length);
					dst[idx + 0] = val;

					final CompactMapNode<K, V> thisNew = new HashCollisionMapNode<>(this.hash,
									this.keys, dst);

					return Result.updated(thisNew, currentVal);

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

			return Result.modified(new HashCollisionMapNode<>(keyHash, keysNew, valsNew));
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator,
						final K key, final V val, int keyHash, int shift, Comparator<Object> cmp) {
			if (this.hash != keyHash) {
				return Result.modified(mergeNodes(this, this.hash, key, val, keyHash, shift));
			}

			for (int idx = 0; idx < keys.length; idx++) {
				if (cmp.compare(keys[idx], key) == 0) {

					final V currentVal = vals[idx];

					if (cmp.compare(currentVal, val) == 0) {
						return Result.unchanged(this);
					}

					final V[] src = this.vals;
					@SuppressWarnings("unchecked")
					final V[] dst = (V[]) new Object[src.length];

					// copy 'src' and set 1 element(s) at position 'idx'
					System.arraycopy(src, 0, dst, 0, src.length);
					dst[idx + 0] = val;

					final CompactMapNode<K, V> thisNew = new HashCollisionMapNode<>(this.hash,
									this.keys, dst);

					return Result.updated(thisNew, currentVal);

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

			return Result.modified(new HashCollisionMapNode<>(keyHash, keysNew, valsNew));
		}

		@SuppressWarnings("unchecked")
		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator,
						final K key, int keyHash, int shift) {

			for (int idx = 0; idx < keys.length; idx++) {
				if (keys[idx].equals(key)) {
					if (this.arity() == 1) {
						return Result.modified(CompactMapNode.<K, V> nodeOf(mutator));
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node
						 * will be a) either be the new root returned, or b)
						 * unwrapped and inlined.
						 */
						final K theOtherKey = (idx == 0) ? keys[1] : keys[0];
						final V theOtherVal = (idx == 0) ? vals[1] : vals[0];
						return CompactMapNode.<K, V> nodeOf(mutator).updated(mutator, theOtherKey,
										theOtherVal, keyHash, 0);
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

						return Result.modified(new HashCollisionMapNode<>(keyHash, keysNew, valsNew));
					}
				}
			}
			return Result.unchanged(this);

		}

		@SuppressWarnings("unchecked")
		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator,
						final K key, int keyHash, int shift, Comparator<Object> cmp) {

			for (int idx = 0; idx < keys.length; idx++) {
				if (cmp.compare(keys[idx], key) == 0) {
					if (this.arity() == 1) {
						return Result.modified(CompactMapNode.<K, V> nodeOf(mutator));
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node
						 * will be a) either be the new root returned, or b)
						 * unwrapped and inlined.
						 */
						final K theOtherKey = (idx == 0) ? keys[1] : keys[0];
						final V theOtherVal = (idx == 0) ? vals[1] : vals[0];
						return CompactMapNode.<K, V> nodeOf(mutator).updated(mutator, theOtherKey,
										theOtherVal, keyHash, 0, cmp);
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

						return Result.modified(new HashCollisionMapNode<>(keyHash, keysNew, valsNew));
					}
				}
			}
			return Result.unchanged(this);

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
		K getKey(int index) {
			return keys[index];
		}

		@Override
		V getValue(int index) {
			return vals[index];
		}

		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf(keys[index], vals[index]);
		}

		@Override
		public CompactMapNode<K, V> getNode(int index) {
			throw new IllegalStateException("Is leaf node.");
		}

		@Override
		java.lang.Object getSlot(int index) {
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

			HashCollisionMapNode<K, V> that = (HashCollisionMapNode<K, V>) other;

			if (hash != that.hash) {
				return false;
			}

			if (arity() != that.arity()) {
				return false;
			}

			/*
			 * Linear scan for each key, because of arbitrary element order.
			 */
			outerLoop: for (SupplierIterator<K, V> it = that.payloadIterator(); it.hasNext();) {
				final K otherKey = it.next();
				@SuppressWarnings("deprecation")
				final V otherVal = it.get();

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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Iterator skeleton that uses a fixed stack in depth.
	 */
	private static abstract class AbstractMapIterator<K, V> {

		// TODO: verify maximum deepness
		private static final int MAX_DEPTH = 10;

		protected int currentValueCursor;
		protected int currentValueLength;
		protected AbstractMapNode<K, V> currentValueNode;

		private int currentStackLevel;
		private final int[] nodeCursorsAndLengths = new int[MAX_DEPTH * 2];

		@SuppressWarnings("unchecked")
		AbstractMapNode<K, V>[] nodes = new AbstractMapNode[MAX_DEPTH];

		AbstractMapIterator(AbstractMapNode<K, V> rootNode) {
			currentStackLevel = 0;

			currentValueNode = rootNode;
			currentValueCursor = 0;
			currentValueLength = rootNode.payloadArity();

			nodes[0] = rootNode;
			nodeCursorsAndLengths[0] = 0;
			nodeCursorsAndLengths[1] = rootNode.nodeArity();
		}

		public boolean hasNext() {
			if (currentValueCursor < currentValueLength) {
				return true;
			} else {
				/*
				 * search for next node that contains values
				 */
				while (currentStackLevel >= 0) {
					final int currentCursorIndex = currentStackLevel * 2;
					final int currentLengthIndex = currentCursorIndex + 1;

					final int nodeCursor = nodeCursorsAndLengths[currentCursorIndex];
					final int nodeLength = nodeCursorsAndLengths[currentLengthIndex];

					if (nodeCursor < nodeLength) {
						final AbstractMapNode<K, V> nextNode = nodes[currentStackLevel]
										.getNode(nodeCursor);
						nodeCursorsAndLengths[currentCursorIndex]++;

						final int nextValueLength = nextNode.payloadArity();
						final int nextNodeLength = nextNode.nodeArity();

						if (nextNodeLength > 0) {
							/*
							 * put node on next stack level for depth-first
							 * traversal
							 */
							final int nextStackLevel = ++currentStackLevel;
							final int nextCursorIndex = nextStackLevel * 2;
							final int nextLengthIndex = nextCursorIndex + 1;

							nodes[nextStackLevel] = nextNode;
							nodeCursorsAndLengths[nextCursorIndex] = 0;
							nodeCursorsAndLengths[nextLengthIndex] = nextNodeLength;
						}

						if (nextValueLength != 0) {
							/*
							 * found for next node that contains values
							 */
							currentValueNode = nextNode;
							currentValueCursor = 0;
							currentValueLength = nextValueLength;
							return true;
						}
					} else {
						currentStackLevel--;
					}
				}
			}

			return false;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private static final class MapKeyIterator<K, V> extends AbstractMapIterator<K, V> implements
					SupplierIterator<K, V> {

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

		@Override
		public V get() {
			throw new UnsupportedOperationException();
		}
	}

	private static final class MapValueIterator<K, V> extends AbstractMapIterator<K, V> implements
					SupplierIterator<V, K> {

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

		@Override
		public K get() {
			throw new UnsupportedOperationException();
		}
	}

	private static final class MapEntryIterator<K, V> extends AbstractMapIterator<K, V> implements
					SupplierIterator<Map.Entry<K, V>, K> {

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

		@Override
		public K get() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Iterator that first iterates over inlined-values and then continues depth
	 * first recursively.
	 */
	private static class TrieMapNodeIterator<K, V> implements Iterator<AbstractMapNode<K, V>> {

		final Deque<Iterator<? extends AbstractMapNode<K, V>>> nodeIteratorStack;

		TrieMapNodeIterator(AbstractMapNode<K, V> rootNode) {
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

	static final class TransientTrieMap<K, V> extends AbstractMap<K, V> implements
					TransientMap<K, V> {
		final private AtomicReference<Thread> mutator;
		private AbstractMapNode<K, V> rootNode;
		private int hashCode;
		private int cachedSize;

		TransientTrieMap(TrieMapDynamic<K, V> trieMap) {
			this.mutator = new AtomicReference<Thread>(Thread.currentThread());
			this.rootNode = trieMap.rootNode;
			this.hashCode = trieMap.hashCode;
			this.cachedSize = trieMap.cachedSize;
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

		@Override
		public boolean containsKey(Object o) {
			try {
				final K key = (K) o;
				return rootNode.containsKey(key, key.hashCode(), 0);
			} catch (ClassCastException unused) {
				return false;
			}
		}

		@Override
		public boolean containsKeyEquivalent(Object o, Comparator<Object> cmp) {
			try {
				final K key = (K) o;
				return rootNode.containsKey(key, key.hashCode(), 0, cmp);
			} catch (ClassCastException unused) {
				return false;
			}
		}

		@Override
		public V get(Object o) {
			try {
				final K key = (K) o;
				final Optional<V> result = rootNode.findByKey(key, key.hashCode(), 0);

				if (result.isPresent()) {
					return result.get();
				} else {
					return null;
				}
			} catch (ClassCastException unused) {
				return null;
			}
		}

		@Override
		public V getEquivalent(Object o, Comparator<Object> cmp) {
			try {
				final K key = (K) o;
				final Optional<V> result = rootNode.findByKey(key, key.hashCode(), 0, cmp);

				if (result.isPresent()) {
					return result.get();
				} else {
					return null;
				}
			} catch (ClassCastException unused) {
				return null;
			}
		}

		@Override
		public V __put(final K key, final V val) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final Result<K, V, ? extends CompactMapNode<K, V>> result = rootNode.updated(mutator,
							key, val, keyHash, 0);

			if (result.isModified()) {
				rootNode = result.getNode();

				if (result.hasReplacedValue()) {
					final V old = result.getReplacedValue();

					final int valHashOld = old.hashCode();
					final int valHashNew = val.hashCode();

					hashCode += keyHash ^ valHashNew;
					hashCode -= keyHash ^ valHashOld;
					// cachedSize remains same

					if (DEBUG) {
						assert checkHashCodeAndSize(hashCode, cachedSize);
					}
					return old;
				} else {
					final int valHashNew = val.hashCode();

					hashCode += keyHash ^ valHashNew;
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

		@Override
		public V __putEquivalent(final K key, final V val, final Comparator<Object> cmp) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final Result<K, V, ? extends CompactMapNode<K, V>> result = rootNode.updated(mutator,
							key, val, keyHash, 0, cmp);

			if (result.isModified()) {
				rootNode = result.getNode();

				if (result.hasReplacedValue()) {
					final V old = result.getReplacedValue();

					final int valHashOld = old.hashCode();
					final int valHashNew = val.hashCode();

					hashCode += keyHash ^ valHashNew;
					hashCode -= keyHash ^ valHashOld;
					// cachedSize remains same

					if (DEBUG) {
						assert checkHashCodeAndSize(hashCode, cachedSize);
					}
					return old;
				} else {
					final int valHashNew = val.hashCode();

					hashCode += keyHash ^ valHashNew;
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

		@Override
		public boolean __putAll(final Map<? extends K, ? extends V> map) {
			boolean modified = false;

			for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
				final boolean isPresent = containsKey(entry.getKey());
				final V replaced = __put(entry.getKey(), entry.getValue());

				if (!isPresent || replaced != null) {
					modified = true;
				}
			}

			return modified;
		}

		@Override
		public boolean __putAllEquivalent(final Map<? extends K, ? extends V> map,
						final Comparator<Object> cmp) {
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
		public boolean __remove(final K key) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");

			}

			final int keyHash = key.hashCode();
			final Result<K, V, ? extends CompactMapNode<K, V>> result = rootNode.removed(mutator,
							key, keyHash, 0);

			if (result.isModified()) {

				// TODO: carry deleted value in result
				// assert result.hasReplacedValue();
				// final int valHash = result.getReplacedValue().hashCode();

				final int valHash = rootNode.findByKey(key, keyHash, 0).get().hashCode();

				rootNode = result.getNode();
				hashCode -= keyHash ^ valHash;
				cachedSize -= 1;

				if (DEBUG) {
					assert checkHashCodeAndSize(hashCode, cachedSize);
				}
				return true;

			}

			if (DEBUG) {
				assert checkHashCodeAndSize(hashCode, cachedSize);
			}
			return false;
		}

		@Override
		public boolean __removeEquivalent(final K key, Comparator<Object> cmp) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final Result<K, V, ? extends CompactMapNode<K, V>> result = rootNode.removed(mutator,
							key, keyHash, 0, cmp);

			if (result.isModified()) {

				// TODO: carry deleted value in result
				// assert result.hasReplacedValue();
				// final int valHash = result.getReplacedValue().hashCode();

				final int valHash = rootNode.findByKey(key, keyHash, 0, cmp).get().hashCode();

				rootNode = result.getNode();
				hashCode -= keyHash ^ valHash;
				cachedSize -= 1;

				if (DEBUG) {
					assert checkHashCodeAndSize(hashCode, cachedSize);
				}
				return true;

			}

			if (DEBUG) {
				assert checkHashCodeAndSize(hashCode, cachedSize);
			}
			return false;

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
		public int size() {
			return cachedSize;
		}

		@Override
		public SupplierIterator<K, V> keyIterator() {
			return new TransientMapKeyIterator<>(this);
		}

		@Override
		public Iterator<V> valueIterator() {
			// return new TrieMapValueIterator<>(keyIterator());
			return new MapValueIterator<>(rootNode); // TODO: iterator does not
														// support removal
		}

		@Override
		public Iterator<Map.Entry<K, V>> entryIterator() {
			// return new TrieMapEntryIterator<>(keyIterator());
			return new MapEntryIterator<>(rootNode); // TODO: iterator does not
														// support removal
		}

		/**
		 * Iterator that first iterates over inlined-values and then continues
		 * depth first recursively.
		 */
		private static class TransientMapKeyIterator<K, V> extends AbstractMapIterator<K, V>
						implements SupplierIterator<K, V> {

			final TransientTrieMap<K, V> transientTrieMap;
			K lastKey;

			TransientMapKeyIterator(TransientTrieMap<K, V> transientTrieMap) {
				super(transientTrieMap.rootNode);
				this.transientTrieMap = transientTrieMap;
			}

			@Override
			public K next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				} else {
					lastKey = currentValueNode.getKey(currentValueCursor++);
					return lastKey;
				}
			}

			@Override
			public V get() {
				throw new UnsupportedOperationException();
			}

			/*
			 * TODO: test removal with iteration rigorously
			 */
			@Override
			public void remove() {
				boolean success = transientTrieMap.__remove(lastKey);

				if (!success) {
					throw new IllegalStateException("Key from iteration couldn't be deleted.");
				}
			}
		}

		@Override
		public boolean equals(Object other) {
			if (other == this) {
				return true;
			}
			if (other == null) {
				return false;
			}

			if (other instanceof TransientTrieMap) {
				TransientTrieMap<?, ?> that = (TransientTrieMap<?, ?>) other;

				if (this.size() != that.size()) {
					return false;
				}

				return rootNode.equals(that.rootNode);
			}

			return super.equals(other);
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
			return new TrieMapDynamic<K, V>(rootNode, hashCode, cachedSize);
		}
	}

}