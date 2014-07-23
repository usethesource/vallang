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
public class TrieMapSpecializationWithUntypedVariables<K, V> extends AbstractImmutableMap<K, V> {

	@SuppressWarnings("unchecked")
	private static final TrieMapSpecializationWithUntypedVariables EMPTY_MAP = new TrieMapSpecializationWithUntypedVariables(CompactMapNode.EMPTY_NODE, 0, 0);

	private static final boolean DEBUG = false;

	private final AbstractMapNode<K, V> rootNode;
	private final int hashCode;
	private final int cachedSize;

	TrieMapSpecializationWithUntypedVariables(AbstractMapNode<K, V> rootNode, int hashCode, int cachedSize) {
		this.rootNode = rootNode;
		this.hashCode = hashCode;
		this.cachedSize = cachedSize;
		if (DEBUG) {
			assert checkHashCodeAndSize(hashCode, cachedSize);
		}
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> ImmutableMap<K, V> of() {
		return TrieMapSpecializationWithUntypedVariables.EMPTY_MAP;
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> ImmutableMap<K, V> of(Object... keyValuePairs) {
		if (keyValuePairs.length % 2 != 0) {
			throw new IllegalArgumentException(
							"Length of argument list is uneven: no key/value pairs.");
		}

		ImmutableMap<K, V> result = TrieMapSpecializationWithUntypedVariables.EMPTY_MAP;

		for (int i = 0; i < keyValuePairs.length; i += 2) {
			final K key = (K) keyValuePairs[i];
			final V val = (V) keyValuePairs[i + 1];

			result = result.__put(key, val);
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> TransientMap<K, V> transientOf() {
		return TrieMapSpecializationWithUntypedVariables.EMPTY_MAP.asTransient();
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> TransientMap<K, V> transientOf(Object... keyValuePairs) {
		if (keyValuePairs.length % 2 != 0) {
			throw new IllegalArgumentException(
							"Length of argument list is uneven: no key/value pairs.");
		}

		final TransientMap<K, V> result = TrieMapSpecializationWithUntypedVariables.EMPTY_MAP.asTransient();

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
	public TrieMapSpecializationWithUntypedVariables<K, V> __put(final K key, final V val) {
		final int keyHash = key.hashCode();
		final Result<K, V, ? extends CompactMapNode<K, V>> result = rootNode.updated(null, key,
						val, keyHash, 0);

		if (result.isModified()) {

			if (result.hasReplacedValue()) {
				final int valHashOld = result.getReplacedValue().hashCode();
				final int valHashNew = val.hashCode();

				return new TrieMapSpecializationWithUntypedVariables<K, V>(result.getNode(), hashCode + (keyHash ^ valHashNew)
								- (keyHash ^ valHashOld), cachedSize);
			}

			final int valHash = val.hashCode();
			return new TrieMapSpecializationWithUntypedVariables<K, V>(result.getNode(), hashCode + (keyHash ^ valHash),
							cachedSize + 1);

		}

		return this;
	}

	@Override
	public TrieMapSpecializationWithUntypedVariables<K, V> __putEquivalent(final K key, final V val, Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final Result<K, V, ? extends CompactMapNode<K, V>> result = rootNode.updated(null, key,
						val, keyHash, 0, cmp);

		if (result.isModified()) {

			if (result.hasReplacedValue()) {
				final int valHashOld = result.getReplacedValue().hashCode();
				final int valHashNew = val.hashCode();

				return new TrieMapSpecializationWithUntypedVariables<K, V>(result.getNode(), hashCode + (keyHash ^ valHashNew)
								- (keyHash ^ valHashOld), cachedSize);
			}

			final int valHash = val.hashCode();
			return new TrieMapSpecializationWithUntypedVariables<K, V>(result.getNode(), hashCode + (keyHash ^ valHash),
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
	public TrieMapSpecializationWithUntypedVariables<K, V> __remove(final K key) {
		final int keyHash = key.hashCode();
		final Result<K, V, ? extends CompactMapNode<K, V>> result = rootNode.removed(null, key,
						keyHash, 0);

		if (result.isModified()) {

			// TODO: carry deleted value in result
			// assert result.hasReplacedValue();
			// final int valHash = result.getReplacedValue().hashCode();

			final int valHash = rootNode.findByKey(key, keyHash, 0).get().hashCode();

			return new TrieMapSpecializationWithUntypedVariables<K, V>(result.getNode(), hashCode - (keyHash ^ valHash),
							cachedSize - 1);

		}

		return this;
	}

	@Override
	public TrieMapSpecializationWithUntypedVariables<K, V> __removeEquivalent(final K key, Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final Result<K, V, ? extends CompactMapNode<K, V>> result = rootNode.removed(null, key,
						keyHash, 0, cmp);

		if (result.isModified()) {

			// TODO: carry deleted value in result
			// assert result.hasReplacedValue();
			// final int valHash = result.getReplacedValue().hashCode();

			final int valHash = rootNode.findByKey(key, keyHash, 0, cmp).get().hashCode();

			return new TrieMapSpecializationWithUntypedVariables<K, V>(result.getNode(), hashCode - (keyHash ^ valHash),
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
					return TrieMapSpecializationWithUntypedVariables.this.size();
				}

				@Override
				public boolean isEmpty() {
					return TrieMapSpecializationWithUntypedVariables.this.isEmpty();
				}

				@SuppressWarnings("deprecation")
				@Override
				public void clear() {
					TrieMapSpecializationWithUntypedVariables.this.clear();
				}

				@Override
				public boolean contains(Object k) {
					return TrieMapSpecializationWithUntypedVariables.this.containsKey(k);
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

		if (other instanceof TrieMapSpecializationWithUntypedVariables) {
			TrieMapSpecializationWithUntypedVariables<?, ?> that = (TrieMapSpecializationWithUntypedVariables<?, ?>) other;

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
					return nodeOf(null, (short) 0, dataMap, key0, val0, key1, val1);
				} else {
					return nodeOf(null, (short) 0, dataMap, key1, val1, key0, val0);
				}
			} else {
				// values fit on next level
				final CompactMapNode<K, V> node = mergeNodes(key0, val0, keyHash0, key1, val1,
								keyHash1, shift + BIT_PARTITION_SIZE);

				final short nodeMap = (short) (1L << mask0);
				return nodeOf(null, nodeMap, (short) 0, node);
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
				return nodeOf(null, nodeMap, dataMap, key1, val1, node0);
			} else {
				// values fit on next level
				final CompactMapNode<K, V> node = mergeNodes(node0, keyHash0, key1, val1, keyHash1,
								shift + BIT_PARTITION_SIZE);

				final short nodeMap = (short) (1L << mask0);
				return nodeOf(null, nodeMap, (short) 0, node);
			}
		}

		static final CompactMapNode EMPTY_NODE;

		static {
			EMPTY_NODE = new Map0To0Node<>(null, (short) 0, (short) 0);
		};

		// TODO: consolidate and remove
		static final <K, V> CompactMapNode<K, V> nodeOf(AtomicReference<Thread> mutator) {
			return nodeOf(mutator, (short) 0, (short) 0);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap) {
			return EMPTY_NODE;
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0) {
			return new Map0To1Node<>(mutator, nodeMap, dataMap, slot0);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1) {
			return new Map0To2Node<>(mutator, nodeMap, dataMap, slot0, slot1);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2) {
			return new Map0To3Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3) {
			return new Map0To4Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4) {
			return new Map0To5Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5) {
			return new Map0To6Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6) {
			return new Map0To7Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7) {
			return new Map0To8Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8) {
			return new Map0To9Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9) {
			return new Map0To10Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10) {
			return new Map0To11Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11) {
			return new Map0To12Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12) {
			return new Map0To13Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13) {
			return new Map0To14Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14) {
			return new Map0To15Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
							slot14);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15) {
			return new Map0To16Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
							slot14, slot15);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16) {
			return new Map0To17Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
							slot14, slot15, slot16);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17) {
			return new Map0To18Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
							slot14, slot15, slot16, slot17);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18) {
			return new Map0To19Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
							slot14, slot15, slot16, slot17, slot18);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19) {
			return new Map0To20Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
							slot14, slot15, slot16, slot17, slot18, slot19);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20) {
			return new Map0To21Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
							slot14, slot15, slot16, slot17, slot18, slot19, slot20);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21) {
			return new Map0To22Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
							slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21, final java.lang.Object slot22) {
			return new Map0To23Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
							slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21, final java.lang.Object slot22,
						final java.lang.Object slot23) {
			return new Map0To24Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
							slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
							slot23);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21, final java.lang.Object slot22,
						final java.lang.Object slot23, final java.lang.Object slot24) {
			return new Map0To25Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
							slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
							slot23, slot24);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21, final java.lang.Object slot22,
						final java.lang.Object slot23, final java.lang.Object slot24,
						final java.lang.Object slot25) {
			return new Map0To26Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
							slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
							slot23, slot24, slot25);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21, final java.lang.Object slot22,
						final java.lang.Object slot23, final java.lang.Object slot24,
						final java.lang.Object slot25, final java.lang.Object slot26) {
			return new Map0To27Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
							slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
							slot23, slot24, slot25, slot26);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21, final java.lang.Object slot22,
						final java.lang.Object slot23, final java.lang.Object slot24,
						final java.lang.Object slot25, final java.lang.Object slot26,
						final java.lang.Object slot27) {
			return new Map0To28Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
							slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
							slot23, slot24, slot25, slot26, slot27);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21, final java.lang.Object slot22,
						final java.lang.Object slot23, final java.lang.Object slot24,
						final java.lang.Object slot25, final java.lang.Object slot26,
						final java.lang.Object slot27, final java.lang.Object slot28) {
			return new Map0To29Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
							slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
							slot23, slot24, slot25, slot26, slot27, slot28);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21, final java.lang.Object slot22,
						final java.lang.Object slot23, final java.lang.Object slot24,
						final java.lang.Object slot25, final java.lang.Object slot26,
						final java.lang.Object slot27, final java.lang.Object slot28,
						final java.lang.Object slot29) {
			return new Map0To30Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
							slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
							slot23, slot24, slot25, slot26, slot27, slot28, slot29);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21, final java.lang.Object slot22,
						final java.lang.Object slot23, final java.lang.Object slot24,
						final java.lang.Object slot25, final java.lang.Object slot26,
						final java.lang.Object slot27, final java.lang.Object slot28,
						final java.lang.Object slot29, final java.lang.Object slot30) {
			return new Map0To31Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
							slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
							slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
		}

		static final <K, V> CompactMapNode<K, V> nodeOf(final AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21, final java.lang.Object slot22,
						final java.lang.Object slot23, final java.lang.Object slot24,
						final java.lang.Object slot25, final java.lang.Object slot26,
						final java.lang.Object slot27, final java.lang.Object slot28,
						final java.lang.Object slot29, final java.lang.Object slot30,
						final java.lang.Object slot31) {
			return new Map0To32Node<>(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4,
							slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
							slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
							slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
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

					final CompactMapNode<K, V> thisNew = copyAndRemoveValue(mutator, bitpos)
									.copyAndInsertNode(mutator, bitpos, nodeNew);

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

					final CompactMapNode<K, V> thisNew = copyAndRemoveValue(mutator, bitpos)
									.copyAndInsertNode(mutator, bitpos, nodeNew);

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
					// inline value (move to front)
					// final CompactMapNode<K, V> thisNew =
					// copyAndMigrateFromNodeToInline(mutator, bitpos,
					// subNodeNew);
					final CompactMapNode<K, V> thisNew = copyAndRemoveNode(mutator, bitpos)
									.copyAndInsertValue(mutator, bitpos, subNodeNew.getKey(0),
													subNodeNew.getValue(0));

					return Result.modified(thisNew);
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
					// inline value (move to front)
					// final CompactMapNode<K, V> thisNew =
					// copyAndMigrateFromNodeToInline(mutator, bitpos,
					// subNodeNew);
					final CompactMapNode<K, V> thisNew = copyAndRemoveNode(mutator, bitpos)
									.copyAndInsertValue(mutator, bitpos, subNodeNew.getKey(0),
													subNodeNew.getValue(0));

					return Result.modified(thisNew);
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

	private static abstract class CompactNodesOnlyMapNode<K, V> extends CompactMapNode<K, V> {

		private final short nodeMap;

		CompactNodesOnlyMapNode(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap) {
			this.nodeMap = nodeMap;
		}

		@Override
		public short nodeMap() {
			return nodeMap;
		}

		@Override
		public short dataMap() {
			return 0;
		}

	}

	private static abstract class CompactValuesOnlyMapNode<K, V> extends CompactMapNode<K, V> {

		private final short dataMap;

		CompactValuesOnlyMapNode(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap) {
			this.dataMap = dataMap;
		}

		@Override
		public short nodeMap() {
			return 0;
		}

		@Override
		public short dataMap() {
			return dataMap;
		}

	}

	private static abstract class CompactEmptyMapNode<K, V> extends CompactMapNode<K, V> {

		CompactEmptyMapNode(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap) {
		}

		@Override
		public short nodeMap() {
			return 0;
		}

		@Override
		public short dataMap() {
			return 0;
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

			return ArrayKeyValueIterator.of(keysAndVals);
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

		TransientTrieMap(TrieMapSpecializationWithUntypedVariables<K, V> trieMap) {
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
			return new TrieMapSpecializationWithUntypedVariables<K, V>(rootNode, hashCode, cachedSize);
		}
	}

	private static final class Map0To0Node<K, V> extends CompactMixedMapNode<K, V> {

		Map0To0Node(final AtomicReference<Thread> mutator, final short nodeMap, final short dataMap) {
			super(mutator, nodeMap, dataMap);

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
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

	}

	private static final class Map0To1Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;

		Map0To1Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap);
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
			Map0To1Node<?, ?> that = (Map0To1Node<?, ?>) other;

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

	private static final class Map0To2Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;

		Map0To2Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

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
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0);
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
			Map0To2Node<?, ?> that = (Map0To2Node<?, ?>) other;

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

	private static final class Map0To3Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;

		Map0To3Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

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
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1);
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
			Map0To3Node<?, ?> that = (Map0To3Node<?, ?>) other;

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

	private static final class Map0To4Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;

		Map0To4Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

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
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

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
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2);
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
			Map0To4Node<?, ?> that = (Map0To4Node<?, ?>) other;

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

	private static final class Map0To5Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;

		Map0To5Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

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
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

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
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3);
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
			Map0To5Node<?, ?> that = (Map0To5Node<?, ?>) other;

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

	private static final class Map0To6Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;

		Map0To6Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5) {
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
		java.lang.Object getSlot(int index) {
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
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

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
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

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
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4);
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
			Map0To6Node<?, ?> that = (Map0To6Node<?, ?>) other;

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

	private static final class Map0To7Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;

		Map0To7Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6) {
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
		java.lang.Object getSlot(int index) {
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
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

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
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

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
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5);
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
			Map0To7Node<?, ?> that = (Map0To7Node<?, ?>) other;

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

	private static final class Map0To8Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;

		Map0To8Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7) {
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
		java.lang.Object getSlot(int index) {
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
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

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
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

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
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6);
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
			Map0To8Node<?, ?> that = (Map0To8Node<?, ?>) other;

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

	private static final class Map0To9Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;

		Map0To9Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8) {
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
		java.lang.Object getSlot(int index) {
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
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

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
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

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
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7);
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
			Map0To9Node<?, ?> that = (Map0To9Node<?, ?>) other;

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

	private static final class Map0To10Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;

		Map0To10Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9) {
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
		java.lang.Object getSlot(int index) {
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
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

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
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

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
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8);
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
			Map0To10Node<?, ?> that = (Map0To10Node<?, ?>) other;

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

	private static final class Map0To11Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;

		Map0To11Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10) {
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
		java.lang.Object getSlot(int index) {
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
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

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
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

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
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9);
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
			Map0To11Node<?, ?> that = (Map0To11Node<?, ?>) other;

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

	private static final class Map0To12Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;

		Map0To12Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11) {
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
		java.lang.Object getSlot(int index) {
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
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

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
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

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
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10, slot11);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10, slot11);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10, slot11);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10, slot11);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10, slot11);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot11);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10);
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
			Map0To12Node<?, ?> that = (Map0To12Node<?, ?>) other;

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

	private static final class Map0To13Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;
		private final java.lang.Object slot12;

		Map0To13Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12) {
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
		java.lang.Object getSlot(int index) {
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
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

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
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

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
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10, slot11, slot12);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10, slot11, slot12);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10, slot11, slot12);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10, slot11, slot12);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10, slot11, slot12);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot11, slot12);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot12);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11, slot12);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11, slot12);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11, slot12);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11, slot12);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11, slot12);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot12);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11);
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
			Map0To13Node<?, ?> that = (Map0To13Node<?, ?>) other;

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

	private static final class Map0To14Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;
		private final java.lang.Object slot12;
		private final java.lang.Object slot13;

		Map0To14Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13) {
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
		java.lang.Object getSlot(int index) {
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
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

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
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

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
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10, slot11, slot12, slot13);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10, slot11, slot12, slot13);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10, slot11, slot12, slot13);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot11, slot12, slot13);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot12, slot13);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot13);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11, slot12, slot13);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11, slot12, slot13);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11, slot12, slot13);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11, slot12, slot13);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot12, slot13);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot13);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12);
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
			Map0To14Node<?, ?> that = (Map0To14Node<?, ?>) other;

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

	private static final class Map0To15Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;
		private final java.lang.Object slot12;
		private final java.lang.Object slot13;
		private final java.lang.Object slot14;

		Map0To15Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14) {
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
		java.lang.Object getSlot(int index) {
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
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

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
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

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
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
								slot14);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
								slot14);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
								slot14);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
								slot14);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
								slot14);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot14);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11, slot12, slot13, slot14);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11, slot12, slot13, slot14);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11, slot12, slot13, slot14);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot12, slot13, slot14);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot13, slot14);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot14);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13);
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
			Map0To15Node<?, ?> that = (Map0To15Node<?, ?>) other;

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

	private static final class Map0To16Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;
		private final java.lang.Object slot12;
		private final java.lang.Object slot13;
		private final java.lang.Object slot14;
		private final java.lang.Object slot15;

		Map0To16Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15) {
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
		java.lang.Object getSlot(int index) {
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
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

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
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

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
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

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
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
								slot14, slot15);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
								slot14, slot15);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
								slot14, slot15);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
								slot14, slot15);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot14, slot15);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot15);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11, slot12, slot13, slot14, slot15);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11, slot12, slot13, slot14, slot15);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11, slot12, slot13, slot14, slot15);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot12, slot13, slot14, slot15);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot13, slot14, slot15);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot14, slot15);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot15);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14);
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
			Map0To16Node<?, ?> that = (Map0To16Node<?, ?>) other;

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

	private static final class Map0To17Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;
		private final java.lang.Object slot12;
		private final java.lang.Object slot13;
		private final java.lang.Object slot14;
		private final java.lang.Object slot15;
		private final java.lang.Object slot16;

		Map0To17Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16) {
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
			this.slot16 = slot16;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			case 16:
				return slot16;
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
			final Object[] nodes = new Object[17 - offset];

			for (int i = 0; i < 17 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 17;
		}

		@Override
		int nodeArity() {
			return 17 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val, slot12, slot13, slot14,
								slot15, slot16);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, val, slot14,
								slot15, slot16);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								val, slot16);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, key, val, slot12,
								slot13, slot14, slot15, slot16);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, key,
								val, slot14, slot15, slot16);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, key, val, slot16);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot12, slot13, slot14, slot15, slot16);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot14, slot15, slot16);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot16);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot12, slot13, slot14,
								slot15, slot16);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot13, slot14,
								slot15, slot16);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot14,
								slot15, slot16);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot15, slot16);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot16);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
								slot14, slot15, slot16);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
								slot14, slot15, slot16);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
								slot14, slot15, slot16);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot14, slot15, slot16);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot15, slot16);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot16);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11, slot12, slot13, slot14, slot15,
								slot16);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot12, slot13, slot14, slot15,
								slot16);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot13, slot14, slot15,
								slot16);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot14, slot15,
								slot16);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot15,
								slot16);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot16);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15);
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
			result = prime * result + slot16.hashCode();
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
			Map0To17Node<?, ?> that = (Map0To17Node<?, ?>) other;

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
			if (!(slot16.equals(that.slot16))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To18Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;
		private final java.lang.Object slot12;
		private final java.lang.Object slot13;
		private final java.lang.Object slot14;
		private final java.lang.Object slot15;
		private final java.lang.Object slot16;
		private final java.lang.Object slot17;

		Map0To18Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17) {
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
			this.slot16 = slot16;
			this.slot17 = slot17;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			case 16:
				return slot16;
			case 17:
				return slot17;
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
			final Object[] nodes = new Object[18 - offset];

			for (int i = 0; i < 18 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 18;
		}

		@Override
		int nodeArity() {
			return 18 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, val, slot14,
								slot15, slot16, slot17);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								val, slot16, slot17);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, key, val, slot12,
								slot13, slot14, slot15, slot16, slot17);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, key,
								val, slot14, slot15, slot16, slot17);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, key, val, slot16, slot17);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot12, slot13, slot14, slot15, slot16,
								slot17);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot14, slot15, slot16,
								slot17);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot16,
								slot17);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot13, slot14,
								slot15, slot16, slot17);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot14,
								slot15, slot16, slot17);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot15, slot16, slot17);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot16, slot17);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot17);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
								slot14, slot15, slot16, slot17);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
								slot14, slot15, slot16, slot17);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot14, slot15, slot16, slot17);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot15, slot16, slot17);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot16, slot17);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot17);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot12, slot13, slot14, slot15,
								slot16, slot17);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot13, slot14, slot15,
								slot16, slot17);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot14, slot15,
								slot16, slot17);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot15,
								slot16, slot17);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot16, slot17);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot17);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16);
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
			result = prime * result + slot16.hashCode();
			result = prime * result + slot17.hashCode();
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
			Map0To18Node<?, ?> that = (Map0To18Node<?, ?>) other;

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
			if (!(slot16.equals(that.slot16))) {
				return false;
			}
			if (!(slot17.equals(that.slot17))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To19Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;
		private final java.lang.Object slot12;
		private final java.lang.Object slot13;
		private final java.lang.Object slot14;
		private final java.lang.Object slot15;
		private final java.lang.Object slot16;
		private final java.lang.Object slot17;
		private final java.lang.Object slot18;

		Map0To19Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18) {
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
			this.slot16 = slot16;
			this.slot17 = slot17;
			this.slot18 = slot18;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			case 16:
				return slot16;
			case 17:
				return slot17;
			case 18:
				return slot18;
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
			final Object[] nodes = new Object[19 - offset];

			for (int i = 0; i < 19 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 19;
		}

		@Override
		int nodeArity() {
			return 19 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, val, slot14,
								slot15, slot16, slot17, slot18);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								val, slot16, slot17, slot18);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, val, slot18);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, key, val, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, key,
								val, slot14, slot15, slot16, slot17, slot18);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, key, val, slot16, slot17, slot18);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, key, val, slot18);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot12, slot13, slot14, slot15, slot16,
								slot17, slot18);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot14, slot15, slot16,
								slot17, slot18);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot16,
								slot17, slot18);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot18);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot14,
								slot15, slot16, slot17, slot18);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot15, slot16, slot17, slot18);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot16, slot17, slot18);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot17, slot18);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot18);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
								slot14, slot15, slot16, slot17, slot18);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot14, slot15, slot16, slot17, slot18);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot15, slot16, slot17, slot18);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot16, slot17, slot18);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot17, slot18);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot18);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot13, slot14, slot15,
								slot16, slot17, slot18);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot14, slot15,
								slot16, slot17, slot18);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot15,
								slot16, slot17, slot18);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot16, slot17, slot18);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot17, slot18);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot18);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
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
			result = prime * result + slot16.hashCode();
			result = prime * result + slot17.hashCode();
			result = prime * result + slot18.hashCode();
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
			Map0To19Node<?, ?> that = (Map0To19Node<?, ?>) other;

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
			if (!(slot16.equals(that.slot16))) {
				return false;
			}
			if (!(slot17.equals(that.slot17))) {
				return false;
			}
			if (!(slot18.equals(that.slot18))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To20Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;
		private final java.lang.Object slot12;
		private final java.lang.Object slot13;
		private final java.lang.Object slot14;
		private final java.lang.Object slot15;
		private final java.lang.Object slot16;
		private final java.lang.Object slot17;
		private final java.lang.Object slot18;
		private final java.lang.Object slot19;

		Map0To20Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19) {
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
			this.slot16 = slot16;
			this.slot17 = slot17;
			this.slot18 = slot18;
			this.slot19 = slot19;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			case 16:
				return slot16;
			case 17:
				return slot17;
			case 18:
				return slot18;
			case 19:
				return slot19;
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
			final Object[] nodes = new Object[20 - offset];

			for (int i = 0; i < 20 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 20;
		}

		@Override
		int nodeArity() {
			return 20 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, val, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								val, slot16, slot17, slot18, slot19);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, val, slot18, slot19);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, key, val, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, key,
								val, slot14, slot15, slot16, slot17, slot18, slot19);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, key, val, slot16, slot17, slot18, slot19);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, key, val, slot18, slot19);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot12, slot13, slot14, slot15, slot16,
								slot17, slot18, slot19);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot14, slot15, slot16,
								slot17, slot18, slot19);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot16,
								slot17, slot18, slot19);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot18, slot19);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot15, slot16, slot17, slot18, slot19);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot16, slot17, slot18, slot19);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot17, slot18, slot19);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot18, slot19);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot19);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot14, slot15, slot16, slot17, slot18, slot19);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot15, slot16, slot17, slot18, slot19);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot16, slot17, slot18, slot19);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot17, slot18, slot19);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot18, slot19);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot19);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot14, slot15,
								slot16, slot17, slot18, slot19);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot15,
								slot16, slot17, slot18, slot19);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot16, slot17, slot18, slot19);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot17, slot18, slot19);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot18, slot19);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot19);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18);
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
			result = prime * result + slot16.hashCode();
			result = prime * result + slot17.hashCode();
			result = prime * result + slot18.hashCode();
			result = prime * result + slot19.hashCode();
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
			Map0To20Node<?, ?> that = (Map0To20Node<?, ?>) other;

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
			if (!(slot16.equals(that.slot16))) {
				return false;
			}
			if (!(slot17.equals(that.slot17))) {
				return false;
			}
			if (!(slot18.equals(that.slot18))) {
				return false;
			}
			if (!(slot19.equals(that.slot19))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To21Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;
		private final java.lang.Object slot12;
		private final java.lang.Object slot13;
		private final java.lang.Object slot14;
		private final java.lang.Object slot15;
		private final java.lang.Object slot16;
		private final java.lang.Object slot17;
		private final java.lang.Object slot18;
		private final java.lang.Object slot19;
		private final java.lang.Object slot20;

		Map0To21Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20) {
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
			this.slot16 = slot16;
			this.slot17 = slot17;
			this.slot18 = slot18;
			this.slot19 = slot19;
			this.slot20 = slot20;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			case 16:
				return slot16;
			case 17:
				return slot17;
			case 18:
				return slot18;
			case 19:
				return slot19;
			case 20:
				return slot20;
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
			final Object[] nodes = new Object[21 - offset];

			for (int i = 0; i < 21 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 21;
		}

		@Override
		int nodeArity() {
			return 21 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, val, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								val, slot16, slot17, slot18, slot19, slot20);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, val, slot18, slot19, slot20);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, val, slot20);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, key, val, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, key,
								val, slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, key, val, slot16, slot17, slot18, slot19, slot20);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, key, val, slot18, slot19, slot20);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, key, val, slot20);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot12, slot13, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot16,
								slot17, slot18, slot19, slot20);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot18, slot19, slot20);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot20);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot16, slot17, slot18, slot19, slot20);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot17, slot18, slot19, slot20);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot18, slot19, slot20);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot19, slot20);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot20);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot15, slot16, slot17, slot18, slot19, slot20);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot16, slot17, slot18, slot19, slot20);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot17, slot18, slot19, slot20);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot18, slot19, slot20);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot19, slot20);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot20);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot15,
								slot16, slot17, slot18, slot19, slot20);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot16, slot17, slot18, slot19, slot20);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot17, slot18, slot19, slot20);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot18, slot19, slot20);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot19, slot20);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot20);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
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
			result = prime * result + slot16.hashCode();
			result = prime * result + slot17.hashCode();
			result = prime * result + slot18.hashCode();
			result = prime * result + slot19.hashCode();
			result = prime * result + slot20.hashCode();
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
			Map0To21Node<?, ?> that = (Map0To21Node<?, ?>) other;

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
			if (!(slot16.equals(that.slot16))) {
				return false;
			}
			if (!(slot17.equals(that.slot17))) {
				return false;
			}
			if (!(slot18.equals(that.slot18))) {
				return false;
			}
			if (!(slot19.equals(that.slot19))) {
				return false;
			}
			if (!(slot20.equals(that.slot20))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To22Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;
		private final java.lang.Object slot12;
		private final java.lang.Object slot13;
		private final java.lang.Object slot14;
		private final java.lang.Object slot15;
		private final java.lang.Object slot16;
		private final java.lang.Object slot17;
		private final java.lang.Object slot18;
		private final java.lang.Object slot19;
		private final java.lang.Object slot20;
		private final java.lang.Object slot21;

		Map0To22Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21) {
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
			this.slot16 = slot16;
			this.slot17 = slot17;
			this.slot18 = slot18;
			this.slot19 = slot19;
			this.slot20 = slot20;
			this.slot21 = slot21;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			case 16:
				return slot16;
			case 17:
				return slot17;
			case 18:
				return slot18;
			case 19:
				return slot19;
			case 20:
				return slot20;
			case 21:
				return slot21;
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
			final Object[] nodes = new Object[22 - offset];

			for (int i = 0; i < 22 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 22;
		}

		@Override
		int nodeArity() {
			return 22 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, val, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								val, slot16, slot17, slot18, slot19, slot20, slot21);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, val, slot18, slot19, slot20, slot21);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, val, slot20, slot21);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, key, val, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, key,
								val, slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, key, val, slot16, slot17, slot18, slot19, slot20, slot21);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, key, val, slot18, slot19, slot20, slot21);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, key, val, slot20, slot21);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot12, slot13, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot16,
								slot17, slot18, slot19, slot20, slot21);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot18, slot19, slot20, slot21);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot20, slot21);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot16, slot17, slot18, slot19, slot20, slot21);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot17, slot18, slot19, slot20, slot21);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot18, slot19, slot20, slot21);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot19, slot20, slot21);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot20, slot21);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot21);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot16, slot17, slot18, slot19, slot20, slot21);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot17, slot18, slot19, slot20, slot21);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot18, slot19, slot20, slot21);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot19, slot20, slot21);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot20, slot21);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node, slot21);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot16, slot17, slot18, slot19, slot20, slot21);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot17, slot18, slot19, slot20, slot21);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot18, slot19, slot20, slot21);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot19, slot20, slot21);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot20, slot21);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot21);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20);
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
			result = prime * result + slot16.hashCode();
			result = prime * result + slot17.hashCode();
			result = prime * result + slot18.hashCode();
			result = prime * result + slot19.hashCode();
			result = prime * result + slot20.hashCode();
			result = prime * result + slot21.hashCode();
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
			Map0To22Node<?, ?> that = (Map0To22Node<?, ?>) other;

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
			if (!(slot16.equals(that.slot16))) {
				return false;
			}
			if (!(slot17.equals(that.slot17))) {
				return false;
			}
			if (!(slot18.equals(that.slot18))) {
				return false;
			}
			if (!(slot19.equals(that.slot19))) {
				return false;
			}
			if (!(slot20.equals(that.slot20))) {
				return false;
			}
			if (!(slot21.equals(that.slot21))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To23Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;
		private final java.lang.Object slot12;
		private final java.lang.Object slot13;
		private final java.lang.Object slot14;
		private final java.lang.Object slot15;
		private final java.lang.Object slot16;
		private final java.lang.Object slot17;
		private final java.lang.Object slot18;
		private final java.lang.Object slot19;
		private final java.lang.Object slot20;
		private final java.lang.Object slot21;
		private final java.lang.Object slot22;

		Map0To23Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21, final java.lang.Object slot22) {
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
			this.slot16 = slot16;
			this.slot17 = slot17;
			this.slot18 = slot18;
			this.slot19 = slot19;
			this.slot20 = slot20;
			this.slot21 = slot21;
			this.slot22 = slot22;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			case 16:
				return slot16;
			case 17:
				return slot17;
			case 18:
				return slot18;
			case 19:
				return slot19;
			case 20:
				return slot20;
			case 21:
				return slot21;
			case 22:
				return slot22;
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
			final Object[] nodes = new Object[23 - offset];

			for (int i = 0; i < 23 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 23;
		}

		@Override
		int nodeArity() {
			return 23 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, val, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								val, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, val, slot18, slot19, slot20, slot21, slot22);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, val, slot20, slot21, slot22);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, val, slot22);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, key, val, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, key,
								val, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, key, val, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, key, val, slot18, slot19, slot20, slot21,
								slot22);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, key, val, slot20, slot21,
								slot22);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, key, val,
								slot22);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot12, slot13, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot18, slot19, slot20, slot21, slot22);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot20, slot21, slot22);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot22);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot17, slot18, slot19, slot20, slot21, slot22);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot18, slot19, slot20, slot21, slot22);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot19, slot20, slot21, slot22);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot20, slot21, slot22);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot21, slot22);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node, slot22);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot17, slot18, slot19, slot20, slot21,
								slot22);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot18, slot19, slot20, slot21,
								slot22);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot19, slot20, slot21,
								slot22);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot20, slot21,
								slot22);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node, slot21,
								slot22);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, node,
								slot22);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot17, slot18, slot19, slot20, slot21, slot22);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot18, slot19, slot20, slot21, slot22);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot19, slot20, slot21, slot22);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot20, slot21, slot22);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot21, slot22);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot22);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
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
			result = prime * result + slot16.hashCode();
			result = prime * result + slot17.hashCode();
			result = prime * result + slot18.hashCode();
			result = prime * result + slot19.hashCode();
			result = prime * result + slot20.hashCode();
			result = prime * result + slot21.hashCode();
			result = prime * result + slot22.hashCode();
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
			Map0To23Node<?, ?> that = (Map0To23Node<?, ?>) other;

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
			if (!(slot16.equals(that.slot16))) {
				return false;
			}
			if (!(slot17.equals(that.slot17))) {
				return false;
			}
			if (!(slot18.equals(that.slot18))) {
				return false;
			}
			if (!(slot19.equals(that.slot19))) {
				return false;
			}
			if (!(slot20.equals(that.slot20))) {
				return false;
			}
			if (!(slot21.equals(that.slot21))) {
				return false;
			}
			if (!(slot22.equals(that.slot22))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To24Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;
		private final java.lang.Object slot12;
		private final java.lang.Object slot13;
		private final java.lang.Object slot14;
		private final java.lang.Object slot15;
		private final java.lang.Object slot16;
		private final java.lang.Object slot17;
		private final java.lang.Object slot18;
		private final java.lang.Object slot19;
		private final java.lang.Object slot20;
		private final java.lang.Object slot21;
		private final java.lang.Object slot22;
		private final java.lang.Object slot23;

		Map0To24Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21, final java.lang.Object slot22,
						final java.lang.Object slot23) {
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
			this.slot16 = slot16;
			this.slot17 = slot17;
			this.slot18 = slot18;
			this.slot19 = slot19;
			this.slot20 = slot20;
			this.slot21 = slot21;
			this.slot22 = slot22;
			this.slot23 = slot23;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			case 16:
				return slot16;
			case 17:
				return slot17;
			case 18:
				return slot18;
			case 19:
				return slot19;
			case 20:
				return slot20;
			case 21:
				return slot21;
			case 22:
				return slot22;
			case 23:
				return slot23;
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
			final Object[] nodes = new Object[24 - offset];

			for (int i = 0; i < 24 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 24;
		}

		@Override
		int nodeArity() {
			return 24 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, val, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								val, slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, val, slot18, slot19, slot20, slot21, slot22, slot23);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, val, slot20, slot21, slot22, slot23);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, val, slot22, slot23);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, key, val, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, key,
								val, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, key, val, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, key, val, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, key, val, slot20, slot21,
								slot22, slot23);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, key, val,
								slot22, slot23);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot12, slot13, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot18, slot19, slot20, slot21, slot22, slot23);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot20, slot21, slot22, slot23);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot22, slot23);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot19, slot20, slot21, slot22,
								slot23);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot20, slot21, slot22,
								slot23);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot21, slot22,
								slot23);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node, slot22,
								slot23);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, node,
								slot23);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot18, slot19, slot20, slot21,
								slot22, slot23);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot19, slot20, slot21,
								slot22, slot23);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot20, slot21,
								slot22, slot23);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node, slot21,
								slot22, slot23);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, node,
								slot22, slot23);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								node, slot23);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot17, slot18, slot19, slot20, slot21, slot22, slot23);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot18, slot19, slot20, slot21, slot22, slot23);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot19, slot20, slot21, slot22, slot23);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot20, slot21, slot22, slot23);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot21, slot22, slot23);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot22, slot23);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot23);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22);
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
			result = prime * result + slot16.hashCode();
			result = prime * result + slot17.hashCode();
			result = prime * result + slot18.hashCode();
			result = prime * result + slot19.hashCode();
			result = prime * result + slot20.hashCode();
			result = prime * result + slot21.hashCode();
			result = prime * result + slot22.hashCode();
			result = prime * result + slot23.hashCode();
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
			Map0To24Node<?, ?> that = (Map0To24Node<?, ?>) other;

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
			if (!(slot16.equals(that.slot16))) {
				return false;
			}
			if (!(slot17.equals(that.slot17))) {
				return false;
			}
			if (!(slot18.equals(that.slot18))) {
				return false;
			}
			if (!(slot19.equals(that.slot19))) {
				return false;
			}
			if (!(slot20.equals(that.slot20))) {
				return false;
			}
			if (!(slot21.equals(that.slot21))) {
				return false;
			}
			if (!(slot22.equals(that.slot22))) {
				return false;
			}
			if (!(slot23.equals(that.slot23))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To25Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;
		private final java.lang.Object slot12;
		private final java.lang.Object slot13;
		private final java.lang.Object slot14;
		private final java.lang.Object slot15;
		private final java.lang.Object slot16;
		private final java.lang.Object slot17;
		private final java.lang.Object slot18;
		private final java.lang.Object slot19;
		private final java.lang.Object slot20;
		private final java.lang.Object slot21;
		private final java.lang.Object slot22;
		private final java.lang.Object slot23;
		private final java.lang.Object slot24;

		Map0To25Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21, final java.lang.Object slot22,
						final java.lang.Object slot23, final java.lang.Object slot24) {
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
			this.slot16 = slot16;
			this.slot17 = slot17;
			this.slot18 = slot18;
			this.slot19 = slot19;
			this.slot20 = slot20;
			this.slot21 = slot21;
			this.slot22 = slot22;
			this.slot23 = slot23;
			this.slot24 = slot24;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			case 16:
				return slot16;
			case 17:
				return slot17;
			case 18:
				return slot18;
			case 19:
				return slot19;
			case 20:
				return slot20;
			case 21:
				return slot21;
			case 22:
				return slot22;
			case 23:
				return slot23;
			case 24:
				return slot24;
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
			final Object[] nodes = new Object[25 - offset];

			for (int i = 0; i < 25 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 25;
		}

		@Override
		int nodeArity() {
			return 25 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, val, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								val, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, val, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, val, slot20, slot21, slot22,
								slot23, slot24);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, val, slot22,
								slot23, slot24);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								val, slot24);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, key, val, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, key,
								val, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, key, val, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, key, val, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, key, val, slot20, slot21,
								slot22, slot23, slot24);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, key, val,
								slot22, slot23, slot24);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, key, val, slot24);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot12, slot13, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot18, slot19, slot20, slot21, slot22, slot23, slot24);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot20, slot21, slot22, slot23, slot24);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot22, slot23, slot24);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot24);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot20, slot21, slot22,
								slot23, slot24);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot21, slot22,
								slot23, slot24);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node, slot22,
								slot23, slot24);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, node,
								slot23, slot24);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								node, slot24);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot19, slot20, slot21,
								slot22, slot23, slot24);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot20, slot21,
								slot22, slot23, slot24);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node, slot21,
								slot22, slot23, slot24);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, node,
								slot22, slot23, slot24);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								node, slot23, slot24);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, node, slot24);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot19, slot20, slot21, slot22, slot23,
								slot24);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot20, slot21, slot22, slot23,
								slot24);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot21, slot22, slot23,
								slot24);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot22, slot23,
								slot24);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot23,
								slot24);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot24);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
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
			result = prime * result + slot16.hashCode();
			result = prime * result + slot17.hashCode();
			result = prime * result + slot18.hashCode();
			result = prime * result + slot19.hashCode();
			result = prime * result + slot20.hashCode();
			result = prime * result + slot21.hashCode();
			result = prime * result + slot22.hashCode();
			result = prime * result + slot23.hashCode();
			result = prime * result + slot24.hashCode();
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
			Map0To25Node<?, ?> that = (Map0To25Node<?, ?>) other;

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
			if (!(slot16.equals(that.slot16))) {
				return false;
			}
			if (!(slot17.equals(that.slot17))) {
				return false;
			}
			if (!(slot18.equals(that.slot18))) {
				return false;
			}
			if (!(slot19.equals(that.slot19))) {
				return false;
			}
			if (!(slot20.equals(that.slot20))) {
				return false;
			}
			if (!(slot21.equals(that.slot21))) {
				return false;
			}
			if (!(slot22.equals(that.slot22))) {
				return false;
			}
			if (!(slot23.equals(that.slot23))) {
				return false;
			}
			if (!(slot24.equals(that.slot24))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To26Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;
		private final java.lang.Object slot12;
		private final java.lang.Object slot13;
		private final java.lang.Object slot14;
		private final java.lang.Object slot15;
		private final java.lang.Object slot16;
		private final java.lang.Object slot17;
		private final java.lang.Object slot18;
		private final java.lang.Object slot19;
		private final java.lang.Object slot20;
		private final java.lang.Object slot21;
		private final java.lang.Object slot22;
		private final java.lang.Object slot23;
		private final java.lang.Object slot24;
		private final java.lang.Object slot25;

		Map0To26Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21, final java.lang.Object slot22,
						final java.lang.Object slot23, final java.lang.Object slot24,
						final java.lang.Object slot25) {
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
			this.slot16 = slot16;
			this.slot17 = slot17;
			this.slot18 = slot18;
			this.slot19 = slot19;
			this.slot20 = slot20;
			this.slot21 = slot21;
			this.slot22 = slot22;
			this.slot23 = slot23;
			this.slot24 = slot24;
			this.slot25 = slot25;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			case 16:
				return slot16;
			case 17:
				return slot17;
			case 18:
				return slot18;
			case 19:
				return slot19;
			case 20:
				return slot20;
			case 21:
				return slot21;
			case 22:
				return slot22;
			case 23:
				return slot23;
			case 24:
				return slot24;
			case 25:
				return slot25;
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
			final Object[] nodes = new Object[26 - offset];

			for (int i = 0; i < 26 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 26;
		}

		@Override
		int nodeArity() {
			return 26 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, val, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								val, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, val, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, val, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, val, slot22,
								slot23, slot24, slot25);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								val, slot24, slot25);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, key, val, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, key,
								val, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, key, val, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, key, val, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, key, val, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, key, val,
								slot22, slot23, slot24, slot25);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, key, val, slot24, slot25);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot12, slot13, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot20, slot21, slot22, slot23, slot24,
								slot25);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot22, slot23, slot24,
								slot25);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot24,
								slot25);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot21, slot22,
								slot23, slot24, slot25);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node, slot22,
								slot23, slot24, slot25);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, node,
								slot23, slot24, slot25);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								node, slot24, slot25);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, node, slot25);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot20, slot21,
								slot22, slot23, slot24, slot25);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node, slot21,
								slot22, slot23, slot24, slot25);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, node,
								slot22, slot23, slot24, slot25);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								node, slot23, slot24, slot25);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, node, slot24, slot25);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, node, slot25);
			case 26:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot20, slot21, slot22, slot23,
								slot24, slot25);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot21, slot22, slot23,
								slot24, slot25);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot22, slot23,
								slot24, slot25);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot23,
								slot24, slot25);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot24, slot25);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot25);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24);
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
			result = prime * result + slot16.hashCode();
			result = prime * result + slot17.hashCode();
			result = prime * result + slot18.hashCode();
			result = prime * result + slot19.hashCode();
			result = prime * result + slot20.hashCode();
			result = prime * result + slot21.hashCode();
			result = prime * result + slot22.hashCode();
			result = prime * result + slot23.hashCode();
			result = prime * result + slot24.hashCode();
			result = prime * result + slot25.hashCode();
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
			Map0To26Node<?, ?> that = (Map0To26Node<?, ?>) other;

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
			if (!(slot16.equals(that.slot16))) {
				return false;
			}
			if (!(slot17.equals(that.slot17))) {
				return false;
			}
			if (!(slot18.equals(that.slot18))) {
				return false;
			}
			if (!(slot19.equals(that.slot19))) {
				return false;
			}
			if (!(slot20.equals(that.slot20))) {
				return false;
			}
			if (!(slot21.equals(that.slot21))) {
				return false;
			}
			if (!(slot22.equals(that.slot22))) {
				return false;
			}
			if (!(slot23.equals(that.slot23))) {
				return false;
			}
			if (!(slot24.equals(that.slot24))) {
				return false;
			}
			if (!(slot25.equals(that.slot25))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To27Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;
		private final java.lang.Object slot12;
		private final java.lang.Object slot13;
		private final java.lang.Object slot14;
		private final java.lang.Object slot15;
		private final java.lang.Object slot16;
		private final java.lang.Object slot17;
		private final java.lang.Object slot18;
		private final java.lang.Object slot19;
		private final java.lang.Object slot20;
		private final java.lang.Object slot21;
		private final java.lang.Object slot22;
		private final java.lang.Object slot23;
		private final java.lang.Object slot24;
		private final java.lang.Object slot25;
		private final java.lang.Object slot26;

		Map0To27Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21, final java.lang.Object slot22,
						final java.lang.Object slot23, final java.lang.Object slot24,
						final java.lang.Object slot25, final java.lang.Object slot26) {
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
			this.slot16 = slot16;
			this.slot17 = slot17;
			this.slot18 = slot18;
			this.slot19 = slot19;
			this.slot20 = slot20;
			this.slot21 = slot21;
			this.slot22 = slot22;
			this.slot23 = slot23;
			this.slot24 = slot24;
			this.slot25 = slot25;
			this.slot26 = slot26;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			case 16:
				return slot16;
			case 17:
				return slot17;
			case 18:
				return slot18;
			case 19:
				return slot19;
			case 20:
				return slot20;
			case 21:
				return slot21;
			case 22:
				return slot22;
			case 23:
				return slot23;
			case 24:
				return slot24;
			case 25:
				return slot25;
			case 26:
				return slot26;
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
			final Object[] nodes = new Object[27 - offset];

			for (int i = 0; i < 27 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 27;
		}

		@Override
		int nodeArity() {
			return 27 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, val, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								val, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, val, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, val, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, val, slot22,
								slot23, slot24, slot25, slot26);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								val, slot24, slot25, slot26);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, val, slot26);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, key, val, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, key,
								val, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, key, val, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, key, val, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, key, val, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, key, val,
								slot22, slot23, slot24, slot25, slot26);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, key, val, slot24, slot25, slot26);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, key, val, slot26);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot12, slot13, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot22, slot23, slot24,
								slot25, slot26);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot24,
								slot25, slot26);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot26);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node, slot22,
								slot23, slot24, slot25, slot26);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, node,
								slot23, slot24, slot25, slot26);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								node, slot24, slot25, slot26);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, node, slot25, slot26);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, node, slot26);
			case 26:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node, slot21,
								slot22, slot23, slot24, slot25, slot26);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, node,
								slot22, slot23, slot24, slot25, slot26);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								node, slot23, slot24, slot25, slot26);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, node, slot24, slot25, slot26);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, node, slot25, slot26);
			case 26:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, node, slot26);
			case 27:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot21, slot22, slot23,
								slot24, slot25, slot26);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot22, slot23,
								slot24, slot25, slot26);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot23,
								slot24, slot25, slot26);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot24, slot25, slot26);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot25, slot26);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot26);
			case 26:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
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
			result = prime * result + slot16.hashCode();
			result = prime * result + slot17.hashCode();
			result = prime * result + slot18.hashCode();
			result = prime * result + slot19.hashCode();
			result = prime * result + slot20.hashCode();
			result = prime * result + slot21.hashCode();
			result = prime * result + slot22.hashCode();
			result = prime * result + slot23.hashCode();
			result = prime * result + slot24.hashCode();
			result = prime * result + slot25.hashCode();
			result = prime * result + slot26.hashCode();
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
			Map0To27Node<?, ?> that = (Map0To27Node<?, ?>) other;

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
			if (!(slot16.equals(that.slot16))) {
				return false;
			}
			if (!(slot17.equals(that.slot17))) {
				return false;
			}
			if (!(slot18.equals(that.slot18))) {
				return false;
			}
			if (!(slot19.equals(that.slot19))) {
				return false;
			}
			if (!(slot20.equals(that.slot20))) {
				return false;
			}
			if (!(slot21.equals(that.slot21))) {
				return false;
			}
			if (!(slot22.equals(that.slot22))) {
				return false;
			}
			if (!(slot23.equals(that.slot23))) {
				return false;
			}
			if (!(slot24.equals(that.slot24))) {
				return false;
			}
			if (!(slot25.equals(that.slot25))) {
				return false;
			}
			if (!(slot26.equals(that.slot26))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To28Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;
		private final java.lang.Object slot12;
		private final java.lang.Object slot13;
		private final java.lang.Object slot14;
		private final java.lang.Object slot15;
		private final java.lang.Object slot16;
		private final java.lang.Object slot17;
		private final java.lang.Object slot18;
		private final java.lang.Object slot19;
		private final java.lang.Object slot20;
		private final java.lang.Object slot21;
		private final java.lang.Object slot22;
		private final java.lang.Object slot23;
		private final java.lang.Object slot24;
		private final java.lang.Object slot25;
		private final java.lang.Object slot26;
		private final java.lang.Object slot27;

		Map0To28Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21, final java.lang.Object slot22,
						final java.lang.Object slot23, final java.lang.Object slot24,
						final java.lang.Object slot25, final java.lang.Object slot26,
						final java.lang.Object slot27) {
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
			this.slot16 = slot16;
			this.slot17 = slot17;
			this.slot18 = slot18;
			this.slot19 = slot19;
			this.slot20 = slot20;
			this.slot21 = slot21;
			this.slot22 = slot22;
			this.slot23 = slot23;
			this.slot24 = slot24;
			this.slot25 = slot25;
			this.slot26 = slot26;
			this.slot27 = slot27;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			case 16:
				return slot16;
			case 17:
				return slot17;
			case 18:
				return slot18;
			case 19:
				return slot19;
			case 20:
				return slot20;
			case 21:
				return slot21;
			case 22:
				return slot22;
			case 23:
				return slot23;
			case 24:
				return slot24;
			case 25:
				return slot25;
			case 26:
				return slot26;
			case 27:
				return slot27;
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
			final Object[] nodes = new Object[28 - offset];

			for (int i = 0; i < 28 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 28;
		}

		@Override
		int nodeArity() {
			return 28 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, val, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								val, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, val, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, val, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, val, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								val, slot24, slot25, slot26, slot27);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, val, slot26, slot27);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, key, val, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, key,
								val, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, key, val, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, key, val, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, key, val, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, key, val,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, key, val, slot24, slot25, slot26, slot27);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, key, val, slot26, slot27);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot12, slot13, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot22, slot23, slot24,
								slot25, slot26, slot27);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot24,
								slot25, slot26, slot27);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot26, slot27);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, node,
								slot23, slot24, slot25, slot26, slot27);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								node, slot24, slot25, slot26, slot27);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, node, slot25, slot26, slot27);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, node, slot26, slot27);
			case 26:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, node, slot27);
			case 27:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, node,
								slot22, slot23, slot24, slot25, slot26, slot27);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								node, slot23, slot24, slot25, slot26, slot27);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, node, slot24, slot25, slot26, slot27);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, node, slot25, slot26, slot27);
			case 26:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, node, slot26, slot27);
			case 27:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, node, slot27);
			case 28:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot22, slot23,
								slot24, slot25, slot26, slot27);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot23,
								slot24, slot25, slot26, slot27);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot24, slot25, slot26, slot27);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot25, slot26, slot27);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot26, slot27);
			case 26:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot27);
			case 27:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26);
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
			result = prime * result + slot16.hashCode();
			result = prime * result + slot17.hashCode();
			result = prime * result + slot18.hashCode();
			result = prime * result + slot19.hashCode();
			result = prime * result + slot20.hashCode();
			result = prime * result + slot21.hashCode();
			result = prime * result + slot22.hashCode();
			result = prime * result + slot23.hashCode();
			result = prime * result + slot24.hashCode();
			result = prime * result + slot25.hashCode();
			result = prime * result + slot26.hashCode();
			result = prime * result + slot27.hashCode();
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
			Map0To28Node<?, ?> that = (Map0To28Node<?, ?>) other;

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
			if (!(slot16.equals(that.slot16))) {
				return false;
			}
			if (!(slot17.equals(that.slot17))) {
				return false;
			}
			if (!(slot18.equals(that.slot18))) {
				return false;
			}
			if (!(slot19.equals(that.slot19))) {
				return false;
			}
			if (!(slot20.equals(that.slot20))) {
				return false;
			}
			if (!(slot21.equals(that.slot21))) {
				return false;
			}
			if (!(slot22.equals(that.slot22))) {
				return false;
			}
			if (!(slot23.equals(that.slot23))) {
				return false;
			}
			if (!(slot24.equals(that.slot24))) {
				return false;
			}
			if (!(slot25.equals(that.slot25))) {
				return false;
			}
			if (!(slot26.equals(that.slot26))) {
				return false;
			}
			if (!(slot27.equals(that.slot27))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To29Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;
		private final java.lang.Object slot12;
		private final java.lang.Object slot13;
		private final java.lang.Object slot14;
		private final java.lang.Object slot15;
		private final java.lang.Object slot16;
		private final java.lang.Object slot17;
		private final java.lang.Object slot18;
		private final java.lang.Object slot19;
		private final java.lang.Object slot20;
		private final java.lang.Object slot21;
		private final java.lang.Object slot22;
		private final java.lang.Object slot23;
		private final java.lang.Object slot24;
		private final java.lang.Object slot25;
		private final java.lang.Object slot26;
		private final java.lang.Object slot27;
		private final java.lang.Object slot28;

		Map0To29Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21, final java.lang.Object slot22,
						final java.lang.Object slot23, final java.lang.Object slot24,
						final java.lang.Object slot25, final java.lang.Object slot26,
						final java.lang.Object slot27, final java.lang.Object slot28) {
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
			this.slot16 = slot16;
			this.slot17 = slot17;
			this.slot18 = slot18;
			this.slot19 = slot19;
			this.slot20 = slot20;
			this.slot21 = slot21;
			this.slot22 = slot22;
			this.slot23 = slot23;
			this.slot24 = slot24;
			this.slot25 = slot25;
			this.slot26 = slot26;
			this.slot27 = slot27;
			this.slot28 = slot28;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			case 16:
				return slot16;
			case 17:
				return slot17;
			case 18:
				return slot18;
			case 19:
				return slot19;
			case 20:
				return slot20;
			case 21:
				return slot21;
			case 22:
				return slot22;
			case 23:
				return slot23;
			case 24:
				return slot24;
			case 25:
				return slot25;
			case 26:
				return slot26;
			case 27:
				return slot27;
			case 28:
				return slot28;
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
			final Object[] nodes = new Object[29 - offset];

			for (int i = 0; i < 29 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 29;
		}

		@Override
		int nodeArity() {
			return 29 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, val, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								val, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, val, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, val, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, val, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								val, slot24, slot25, slot26, slot27, slot28);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, val, slot26, slot27, slot28);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, val, slot28);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, key, val, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, key,
								val, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, key, val, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, key, val, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, key, val, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, key, val,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, key, val, slot24, slot25, slot26, slot27, slot28);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, key, val, slot26, slot27, slot28);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, key, val, slot28);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot12, slot13, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot24,
								slot25, slot26, slot27, slot28);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot26, slot27, slot28);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot28);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, node,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								node, slot24, slot25, slot26, slot27, slot28);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, node, slot25, slot26, slot27, slot28);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, node, slot26, slot27, slot28);
			case 26:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, node, slot27, slot28);
			case 27:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, node, slot28);
			case 28:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, node,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								node, slot23, slot24, slot25, slot26, slot27, slot28);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, node, slot24, slot25, slot26, slot27, slot28);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, node, slot25, slot26, slot27, slot28);
			case 26:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, node, slot26, slot27, slot28);
			case 27:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, node, slot27, slot28);
			case 28:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, node, slot28);
			case 29:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot23,
								slot24, slot25, slot26, slot27, slot28);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot24, slot25, slot26, slot27, slot28);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot25, slot26, slot27, slot28);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot26, slot27, slot28);
			case 26:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot27, slot28);
			case 27:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot28);
			case 28:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
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
			result = prime * result + slot16.hashCode();
			result = prime * result + slot17.hashCode();
			result = prime * result + slot18.hashCode();
			result = prime * result + slot19.hashCode();
			result = prime * result + slot20.hashCode();
			result = prime * result + slot21.hashCode();
			result = prime * result + slot22.hashCode();
			result = prime * result + slot23.hashCode();
			result = prime * result + slot24.hashCode();
			result = prime * result + slot25.hashCode();
			result = prime * result + slot26.hashCode();
			result = prime * result + slot27.hashCode();
			result = prime * result + slot28.hashCode();
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
			Map0To29Node<?, ?> that = (Map0To29Node<?, ?>) other;

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
			if (!(slot16.equals(that.slot16))) {
				return false;
			}
			if (!(slot17.equals(that.slot17))) {
				return false;
			}
			if (!(slot18.equals(that.slot18))) {
				return false;
			}
			if (!(slot19.equals(that.slot19))) {
				return false;
			}
			if (!(slot20.equals(that.slot20))) {
				return false;
			}
			if (!(slot21.equals(that.slot21))) {
				return false;
			}
			if (!(slot22.equals(that.slot22))) {
				return false;
			}
			if (!(slot23.equals(that.slot23))) {
				return false;
			}
			if (!(slot24.equals(that.slot24))) {
				return false;
			}
			if (!(slot25.equals(that.slot25))) {
				return false;
			}
			if (!(slot26.equals(that.slot26))) {
				return false;
			}
			if (!(slot27.equals(that.slot27))) {
				return false;
			}
			if (!(slot28.equals(that.slot28))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To30Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;
		private final java.lang.Object slot12;
		private final java.lang.Object slot13;
		private final java.lang.Object slot14;
		private final java.lang.Object slot15;
		private final java.lang.Object slot16;
		private final java.lang.Object slot17;
		private final java.lang.Object slot18;
		private final java.lang.Object slot19;
		private final java.lang.Object slot20;
		private final java.lang.Object slot21;
		private final java.lang.Object slot22;
		private final java.lang.Object slot23;
		private final java.lang.Object slot24;
		private final java.lang.Object slot25;
		private final java.lang.Object slot26;
		private final java.lang.Object slot27;
		private final java.lang.Object slot28;
		private final java.lang.Object slot29;

		Map0To30Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21, final java.lang.Object slot22,
						final java.lang.Object slot23, final java.lang.Object slot24,
						final java.lang.Object slot25, final java.lang.Object slot26,
						final java.lang.Object slot27, final java.lang.Object slot28,
						final java.lang.Object slot29) {
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
			this.slot16 = slot16;
			this.slot17 = slot17;
			this.slot18 = slot18;
			this.slot19 = slot19;
			this.slot20 = slot20;
			this.slot21 = slot21;
			this.slot22 = slot22;
			this.slot23 = slot23;
			this.slot24 = slot24;
			this.slot25 = slot25;
			this.slot26 = slot26;
			this.slot27 = slot27;
			this.slot28 = slot28;
			this.slot29 = slot29;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			case 16:
				return slot16;
			case 17:
				return slot17;
			case 18:
				return slot18;
			case 19:
				return slot19;
			case 20:
				return slot20;
			case 21:
				return slot21;
			case 22:
				return slot22;
			case 23:
				return slot23;
			case 24:
				return slot24;
			case 25:
				return slot25;
			case 26:
				return slot26;
			case 27:
				return slot27;
			case 28:
				return slot28;
			case 29:
				return slot29;
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
			final Object[] nodes = new Object[30 - offset];

			for (int i = 0; i < 30 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 30;
		}

		@Override
		int nodeArity() {
			return 30 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, val, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								val, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, val, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, val, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, val, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								val, slot24, slot25, slot26, slot27, slot28, slot29);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, val, slot26, slot27, slot28, slot29);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, val, slot28, slot29);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() | bitpos);

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, key, val, slot0, slot1, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27, slot28,
								slot29);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, key, val, slot2, slot3,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27, slot28,
								slot29);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, key, val,
								slot4, slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27, slot28,
								slot29);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								key, val, slot6, slot7, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27, slot28,
								slot29);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, key, val, slot8, slot9, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27, slot28,
								slot29);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, key, val, slot10, slot11, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27, slot28,
								slot29);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, key, val, slot12,
								slot13, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27, slot28,
								slot29);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, key,
								val, slot14, slot15, slot16, slot17, slot18, slot19, slot20,
								slot21, slot22, slot23, slot24, slot25, slot26, slot27, slot28,
								slot29);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, key, val, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, key, val, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, key, val, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, key, val,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, key, val, slot24, slot25, slot26, slot27, slot28, slot29);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, key, val, slot26, slot27, slot28, slot29);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, key, val, slot28, slot29);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot12, slot13, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28, slot29);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28, slot29);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28, slot29);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28, slot29);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28, slot29);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28, slot29);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot24,
								slot25, slot26, slot27, slot28, slot29);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot26, slot27, slot28, slot29);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot28, slot29);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, node,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								node, slot24, slot25, slot26, slot27, slot28, slot29);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, node, slot25, slot26, slot27, slot28, slot29);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, node, slot26, slot27, slot28, slot29);
			case 26:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, node, slot27, slot28, slot29);
			case 27:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, node, slot28, slot29);
			case 28:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, node, slot29);
			case 29:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, node,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								node, slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, node, slot24, slot25, slot26, slot27, slot28, slot29);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, node, slot25, slot26, slot27, slot28, slot29);
			case 26:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, node, slot26, slot27, slot28, slot29);
			case 27:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, node, slot27, slot28, slot29);
			case 28:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, node, slot28, slot29);
			case 29:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, node, slot29);
			case 30:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot24, slot25, slot26, slot27, slot28, slot29);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot25, slot26, slot27, slot28, slot29);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot26, slot27, slot28, slot29);
			case 26:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot27, slot28, slot29);
			case 27:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot28, slot29);
			case 28:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot29);
			case 29:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28);
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
			result = prime * result + slot16.hashCode();
			result = prime * result + slot17.hashCode();
			result = prime * result + slot18.hashCode();
			result = prime * result + slot19.hashCode();
			result = prime * result + slot20.hashCode();
			result = prime * result + slot21.hashCode();
			result = prime * result + slot22.hashCode();
			result = prime * result + slot23.hashCode();
			result = prime * result + slot24.hashCode();
			result = prime * result + slot25.hashCode();
			result = prime * result + slot26.hashCode();
			result = prime * result + slot27.hashCode();
			result = prime * result + slot28.hashCode();
			result = prime * result + slot29.hashCode();
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
			Map0To30Node<?, ?> that = (Map0To30Node<?, ?>) other;

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
			if (!(slot16.equals(that.slot16))) {
				return false;
			}
			if (!(slot17.equals(that.slot17))) {
				return false;
			}
			if (!(slot18.equals(that.slot18))) {
				return false;
			}
			if (!(slot19.equals(that.slot19))) {
				return false;
			}
			if (!(slot20.equals(that.slot20))) {
				return false;
			}
			if (!(slot21.equals(that.slot21))) {
				return false;
			}
			if (!(slot22.equals(that.slot22))) {
				return false;
			}
			if (!(slot23.equals(that.slot23))) {
				return false;
			}
			if (!(slot24.equals(that.slot24))) {
				return false;
			}
			if (!(slot25.equals(that.slot25))) {
				return false;
			}
			if (!(slot26.equals(that.slot26))) {
				return false;
			}
			if (!(slot27.equals(that.slot27))) {
				return false;
			}
			if (!(slot28.equals(that.slot28))) {
				return false;
			}
			if (!(slot29.equals(that.slot29))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To31Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;
		private final java.lang.Object slot12;
		private final java.lang.Object slot13;
		private final java.lang.Object slot14;
		private final java.lang.Object slot15;
		private final java.lang.Object slot16;
		private final java.lang.Object slot17;
		private final java.lang.Object slot18;
		private final java.lang.Object slot19;
		private final java.lang.Object slot20;
		private final java.lang.Object slot21;
		private final java.lang.Object slot22;
		private final java.lang.Object slot23;
		private final java.lang.Object slot24;
		private final java.lang.Object slot25;
		private final java.lang.Object slot26;
		private final java.lang.Object slot27;
		private final java.lang.Object slot28;
		private final java.lang.Object slot29;
		private final java.lang.Object slot30;

		Map0To31Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21, final java.lang.Object slot22,
						final java.lang.Object slot23, final java.lang.Object slot24,
						final java.lang.Object slot25, final java.lang.Object slot26,
						final java.lang.Object slot27, final java.lang.Object slot28,
						final java.lang.Object slot29, final java.lang.Object slot30) {
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
			this.slot16 = slot16;
			this.slot17 = slot17;
			this.slot18 = slot18;
			this.slot19 = slot19;
			this.slot20 = slot20;
			this.slot21 = slot21;
			this.slot22 = slot22;
			this.slot23 = slot23;
			this.slot24 = slot24;
			this.slot25 = slot25;
			this.slot26 = slot26;
			this.slot27 = slot27;
			this.slot28 = slot28;
			this.slot29 = slot29;
			this.slot30 = slot30;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			case 16:
				return slot16;
			case 17:
				return slot17;
			case 18:
				return slot18;
			case 19:
				return slot19;
			case 20:
				return slot20;
			case 21:
				return slot21;
			case 22:
				return slot22;
			case 23:
				return slot23;
			case 24:
				return slot24;
			case 25:
				return slot25;
			case 26:
				return slot26;
			case 27:
				return slot27;
			case 28:
				return slot28;
			case 29:
				return slot29;
			case 30:
				return slot30;
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
			final Object[] nodes = new Object[31 - offset];

			for (int i = 0; i < 31 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 31;
		}

		@Override
		int nodeArity() {
			return 31 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, val, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								val, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, val, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, val, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, val, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								val, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, val, slot26, slot27, slot28, slot29, slot30);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, val, slot28, slot29, slot30);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, val, slot30);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			throw new IllegalStateException();
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot12, slot13, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28, slot29, slot30);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28, slot29, slot30);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28, slot29, slot30);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28, slot29, slot30);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28, slot29, slot30);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28, slot29, slot30);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot24,
								slot25, slot26, slot27, slot28, slot29, slot30);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot26, slot27, slot28, slot29, slot30);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot28, slot29, slot30);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot30);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, node,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								node, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, node, slot25, slot26, slot27, slot28, slot29, slot30);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, node, slot26, slot27, slot28, slot29, slot30);
			case 26:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, node, slot27, slot28, slot29, slot30);
			case 27:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, node, slot28, slot29, slot30);
			case 28:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, node, slot29, slot30);
			case 29:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, node, slot30);
			case 30:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() | bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot0, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot1, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot2, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot3, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot4,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot5, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot7, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot8, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot9, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot10, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot11, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot12, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot13,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot14, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot15, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot16, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot17, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot18, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot19, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot20, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node, slot21,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, node,
								slot22, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								node, slot23, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, node, slot24, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, node, slot25, slot26, slot27, slot28, slot29,
								slot30);
			case 26:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, node, slot26, slot27, slot28, slot29,
								slot30);
			case 27:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, node, slot27, slot28, slot29,
								slot30);
			case 28:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, node, slot28, slot29,
								slot30);
			case 29:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, node, slot29,
								slot30);
			case 30:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, node,
								slot30);
			case 31:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot25, slot26, slot27, slot28, slot29, slot30);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot26, slot27, slot28, slot29, slot30);
			case 26:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot27, slot28, slot29, slot30);
			case 27:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot28, slot29, slot30);
			case 28:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot29, slot30);
			case 29:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot30);
			case 30:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
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
			result = prime * result + slot16.hashCode();
			result = prime * result + slot17.hashCode();
			result = prime * result + slot18.hashCode();
			result = prime * result + slot19.hashCode();
			result = prime * result + slot20.hashCode();
			result = prime * result + slot21.hashCode();
			result = prime * result + slot22.hashCode();
			result = prime * result + slot23.hashCode();
			result = prime * result + slot24.hashCode();
			result = prime * result + slot25.hashCode();
			result = prime * result + slot26.hashCode();
			result = prime * result + slot27.hashCode();
			result = prime * result + slot28.hashCode();
			result = prime * result + slot29.hashCode();
			result = prime * result + slot30.hashCode();
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
			Map0To31Node<?, ?> that = (Map0To31Node<?, ?>) other;

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
			if (!(slot16.equals(that.slot16))) {
				return false;
			}
			if (!(slot17.equals(that.slot17))) {
				return false;
			}
			if (!(slot18.equals(that.slot18))) {
				return false;
			}
			if (!(slot19.equals(that.slot19))) {
				return false;
			}
			if (!(slot20.equals(that.slot20))) {
				return false;
			}
			if (!(slot21.equals(that.slot21))) {
				return false;
			}
			if (!(slot22.equals(that.slot22))) {
				return false;
			}
			if (!(slot23.equals(that.slot23))) {
				return false;
			}
			if (!(slot24.equals(that.slot24))) {
				return false;
			}
			if (!(slot25.equals(that.slot25))) {
				return false;
			}
			if (!(slot26.equals(that.slot26))) {
				return false;
			}
			if (!(slot27.equals(that.slot27))) {
				return false;
			}
			if (!(slot28.equals(that.slot28))) {
				return false;
			}
			if (!(slot29.equals(that.slot29))) {
				return false;
			}
			if (!(slot30.equals(that.slot30))) {
				return false;
			}

			return true;
		}

	}

	private static final class Map0To32Node<K, V> extends CompactMixedMapNode<K, V> {

		private final java.lang.Object slot0;
		private final java.lang.Object slot1;
		private final java.lang.Object slot2;
		private final java.lang.Object slot3;
		private final java.lang.Object slot4;
		private final java.lang.Object slot5;
		private final java.lang.Object slot6;
		private final java.lang.Object slot7;
		private final java.lang.Object slot8;
		private final java.lang.Object slot9;
		private final java.lang.Object slot10;
		private final java.lang.Object slot11;
		private final java.lang.Object slot12;
		private final java.lang.Object slot13;
		private final java.lang.Object slot14;
		private final java.lang.Object slot15;
		private final java.lang.Object slot16;
		private final java.lang.Object slot17;
		private final java.lang.Object slot18;
		private final java.lang.Object slot19;
		private final java.lang.Object slot20;
		private final java.lang.Object slot21;
		private final java.lang.Object slot22;
		private final java.lang.Object slot23;
		private final java.lang.Object slot24;
		private final java.lang.Object slot25;
		private final java.lang.Object slot26;
		private final java.lang.Object slot27;
		private final java.lang.Object slot28;
		private final java.lang.Object slot29;
		private final java.lang.Object slot30;
		private final java.lang.Object slot31;

		Map0To32Node(final AtomicReference<Thread> mutator, final short nodeMap,
						final short dataMap, final java.lang.Object slot0,
						final java.lang.Object slot1, final java.lang.Object slot2,
						final java.lang.Object slot3, final java.lang.Object slot4,
						final java.lang.Object slot5, final java.lang.Object slot6,
						final java.lang.Object slot7, final java.lang.Object slot8,
						final java.lang.Object slot9, final java.lang.Object slot10,
						final java.lang.Object slot11, final java.lang.Object slot12,
						final java.lang.Object slot13, final java.lang.Object slot14,
						final java.lang.Object slot15, final java.lang.Object slot16,
						final java.lang.Object slot17, final java.lang.Object slot18,
						final java.lang.Object slot19, final java.lang.Object slot20,
						final java.lang.Object slot21, final java.lang.Object slot22,
						final java.lang.Object slot23, final java.lang.Object slot24,
						final java.lang.Object slot25, final java.lang.Object slot26,
						final java.lang.Object slot27, final java.lang.Object slot28,
						final java.lang.Object slot29, final java.lang.Object slot30,
						final java.lang.Object slot31) {
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
			this.slot16 = slot16;
			this.slot17 = slot17;
			this.slot18 = slot18;
			this.slot19 = slot19;
			this.slot20 = slot20;
			this.slot21 = slot21;
			this.slot22 = slot22;
			this.slot23 = slot23;
			this.slot24 = slot24;
			this.slot25 = slot25;
			this.slot26 = slot26;
			this.slot27 = slot27;
			this.slot28 = slot28;
			this.slot29 = slot29;
			this.slot30 = slot30;
			this.slot31 = slot31;

			assert nodeInvariant();
		}

		@Override
		java.lang.Object getSlot(int index) {
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
			case 16:
				return slot16;
			case 17:
				return slot17;
			case 18:
				return slot18;
			case 19:
				return slot19;
			case 20:
				return slot20;
			case 21:
				return slot21;
			case 22:
				return slot22;
			case 23:
				return slot23;
			case 24:
				return slot24;
			case 25:
				return slot25;
			case 26:
				return slot26;
			case 27:
				return slot27;
			case 28:
				return slot28;
			case 29:
				return slot29;
			case 30:
				return slot30;
			case 31:
				return slot31;
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
			final Object[] nodes = new Object[32 - offset];

			for (int i = 0; i < 32 - offset; i++) {
				// assert ((getSlot(offset + i) instanceof AbstractMapNode) ==
				// true);
				nodes[i] = getSlot(offset + i);
			}

			return (Iterator) ArrayIterator.of(nodes);
		}

		@Override
		boolean hasNodes() {
			return TUPLE_LENGTH * payloadArity() != 32;
		}

		@Override
		int nodeArity() {
			return 32 - TUPLE_LENGTH * payloadArity();
		}

		@Override
		boolean hasPayload() {
			return payloadArity() != 0;
		}

		@Override
		int payloadArity() {
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFFFF));
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, final short bitpos,
						final V val) {
			final int idx = dataIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot0, val, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, val, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, val,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, val, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, val, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, val, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, val, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								val, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, val, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, val, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, val, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								val, slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, val, slot26, slot27, slot28, slot29, slot30, slot31);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, val, slot28, slot29, slot30, slot31);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, val, slot30, slot31);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key, final V val) {
			throw new IllegalStateException();
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int valIndex = dataIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap());
			final short dataMap = (short) (this.dataMap() ^ bitpos);

			switch (valIndex) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot2, slot3, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot4, slot5, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot6, slot7,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot10, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot12, slot13, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot14, slot15, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot16,
								slot17, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot18, slot19, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot20, slot21, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot22, slot23, slot24,
								slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot24,
								slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot26, slot27, slot28, slot29, slot30, slot31);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot28, slot29, slot30, slot31);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot30, slot31);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = this.nodeMap();
			final short dataMap = this.dataMap();

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, node, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, node, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, node, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, node, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, node, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, node,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								node, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, node, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, node, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, node, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, node, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, node, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, node, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, node, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, node,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								node, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, node, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, node, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, node, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, node, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, node, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, node, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, node,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								node, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, node, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, node, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 26:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, node, slot27, slot28, slot29, slot30,
								slot31);
			case 27:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, node, slot28, slot29, slot30,
								slot31);
			case 28:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, node, slot29, slot30,
								slot31);
			case 29:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, node, slot30,
								slot31);
			case 30:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, node,
								slot31);
			case 31:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * payloadArity() + nodeIndex(bitpos);

			final short nodeMap = (short) (this.nodeMap() ^ bitpos);
			final short dataMap = (short) (this.dataMap());

			switch (idx) {
			case 0:
				return nodeOf(mutator, nodeMap, dataMap, slot1, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 1:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot2, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 2:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot3, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 3:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot4, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 4:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot5, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 5:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot6,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 6:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 7:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 8:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 9:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30,
								slot31);
			case 10:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot11, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 11:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot12, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 12:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot13, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 13:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot14, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 14:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot15,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 15:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot16, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 16:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot17, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 17:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot18, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 18:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot19, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 19:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot20, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 20:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot21, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 21:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot22, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 22:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot23,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 23:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot24, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 24:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot25, slot26, slot27, slot28, slot29, slot30, slot31);
			case 25:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot26, slot27, slot28, slot29, slot30, slot31);
			case 26:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot27, slot28, slot29, slot30, slot31);
			case 27:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot28, slot29, slot30, slot31);
			case 28:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot29, slot30, slot31);
			case 29:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot30, slot31);
			case 30:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot31);
			case 31:
				return nodeOf(mutator, nodeMap, dataMap, slot0, slot1, slot2, slot3, slot4, slot5,
								slot6, slot7, slot8, slot9, slot10, slot11, slot12, slot13, slot14,
								slot15, slot16, slot17, slot18, slot19, slot20, slot21, slot22,
								slot23, slot24, slot25, slot26, slot27, slot28, slot29, slot30);
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
			result = prime * result + slot16.hashCode();
			result = prime * result + slot17.hashCode();
			result = prime * result + slot18.hashCode();
			result = prime * result + slot19.hashCode();
			result = prime * result + slot20.hashCode();
			result = prime * result + slot21.hashCode();
			result = prime * result + slot22.hashCode();
			result = prime * result + slot23.hashCode();
			result = prime * result + slot24.hashCode();
			result = prime * result + slot25.hashCode();
			result = prime * result + slot26.hashCode();
			result = prime * result + slot27.hashCode();
			result = prime * result + slot28.hashCode();
			result = prime * result + slot29.hashCode();
			result = prime * result + slot30.hashCode();
			result = prime * result + slot31.hashCode();
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
			Map0To32Node<?, ?> that = (Map0To32Node<?, ?>) other;

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
			if (!(slot16.equals(that.slot16))) {
				return false;
			}
			if (!(slot17.equals(that.slot17))) {
				return false;
			}
			if (!(slot18.equals(that.slot18))) {
				return false;
			}
			if (!(slot19.equals(that.slot19))) {
				return false;
			}
			if (!(slot20.equals(that.slot20))) {
				return false;
			}
			if (!(slot21.equals(that.slot21))) {
				return false;
			}
			if (!(slot22.equals(that.slot22))) {
				return false;
			}
			if (!(slot23.equals(that.slot23))) {
				return false;
			}
			if (!(slot24.equals(that.slot24))) {
				return false;
			}
			if (!(slot25.equals(that.slot25))) {
				return false;
			}
			if (!(slot26.equals(that.slot26))) {
				return false;
			}
			if (!(slot27.equals(that.slot27))) {
				return false;
			}
			if (!(slot28.equals(that.slot28))) {
				return false;
			}
			if (!(slot29.equals(that.slot29))) {
				return false;
			}
			if (!(slot30.equals(that.slot30))) {
				return false;
			}
			if (!(slot31.equals(that.slot31))) {
				return false;
			}

			return true;
		}

	}

}