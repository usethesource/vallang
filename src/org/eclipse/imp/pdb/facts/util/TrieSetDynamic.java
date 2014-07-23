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

import java.text.DecimalFormat;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("rawtypes")
public class TrieSetDynamic<K> extends AbstractImmutableSet<K> {

	@SuppressWarnings("unchecked")
	private static final TrieSetDynamic EMPTY_SET = new TrieSetDynamic(CompactSetNode.EMPTY_NODE, 0, 0);

	private static final boolean DEBUG = false;

	private final AbstractSetNode<K> rootNode;
	private final int hashCode;
	private final int cachedSize;

	TrieSetDynamic(AbstractSetNode<K> rootNode, int hashCode, int cachedSize) {
		this.rootNode = rootNode;
		this.hashCode = hashCode;
		this.cachedSize = cachedSize;
		if (DEBUG) {
			assert checkHashCodeAndSize(hashCode, cachedSize);
		}
	}

	@SuppressWarnings("unchecked")
	public static final <K> ImmutableSet<K> of() {
		return TrieSetDynamic.EMPTY_SET;
	}

	@SuppressWarnings("unchecked")
	public static final <K> ImmutableSet<K> of(K... keys) {
		ImmutableSet<K> result = TrieSetDynamic.EMPTY_SET;

		for (final K key : keys) {
			result = result.__insert(key);
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public static final <K> TransientSet<K> transientOf() {
		return TrieSetDynamic.EMPTY_SET.asTransient();
	}

	@SuppressWarnings("unchecked")
	public static final <K> TransientSet<K> transientOf(K... keys) {
		final TransientSet<K> result = TrieSetDynamic.EMPTY_SET.asTransient();

		for (final K key : keys) {
			result.__insert(key);
		}

		return result;
	}

	private boolean checkHashCodeAndSize(final int targetHash, final int targetSize) {
		int hash = 0;
		int size = 0;

		for (Iterator<K> it = keyIterator(); it.hasNext();) {
			final K key = it.next();

			hash += key.hashCode();
			size += 1;
		}

		return hash == targetHash && size == targetSize;
	}

	@Override
	public TrieSetDynamic<K> __insert(final K key) {
		final int keyHash = key.hashCode();
		final Result<K, Void, ? extends CompactSetNode<K>> result = rootNode.updated(null, key,
						keyHash, 0);

		if (result.isModified()) {

			return new TrieSetDynamic<K>(result.getNode(), hashCode + keyHash, cachedSize + 1);

		}

		return this;
	}

	@Override
	public TrieSetDynamic<K> __insertEquivalent(final K key, Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final Result<K, Void, ? extends CompactSetNode<K>> result = rootNode.updated(null, key,
						keyHash, 0, cmp);

		if (result.isModified()) {

			return new TrieSetDynamic<K>(result.getNode(), hashCode + keyHash, cachedSize + 1);

		}

		return this;
	}

	@Override
	public ImmutableSet<K> __insertAll(ImmutableSet<? extends K> set) {
		TransientSet<K> tmp = asTransient();
		tmp.__insertAll(set);
		return tmp.freeze();
	}

	@Override
	public ImmutableSet<K> __insertAllEquivalent(ImmutableSet<? extends K> set,
					Comparator<Object> cmp) {
		TransientSet<K> tmp = asTransient();
		tmp.__insertAllEquivalent(set, cmp);
		return tmp.freeze();
	}

	@Override
	public ImmutableSet<K> __retainAll(ImmutableSet<? extends K> set) {
		TransientSet<K> tmp = asTransient();
		tmp.__retainAll(set);
		return tmp.freeze();
	}

	@Override
	public ImmutableSet<K> __retainAllEquivalent(ImmutableSet<? extends K> set,
					Comparator<Object> cmp) {
		TransientSet<K> tmp = asTransient();
		tmp.__retainAllEquivalent(set, cmp);
		return tmp.freeze();
	}

	@Override
	public ImmutableSet<K> __removeAll(ImmutableSet<? extends K> set) {
		TransientSet<K> tmp = asTransient();
		tmp.__removeAll(set);
		return tmp.freeze();
	}

	@Override
	public ImmutableSet<K> __removeAllEquivalent(ImmutableSet<? extends K> set,
					Comparator<Object> cmp) {
		TransientSet<K> tmp = asTransient();
		tmp.__removeAllEquivalent(set, cmp);
		return tmp.freeze();
	}

	@Override
	public TrieSetDynamic<K> __remove(final K key) {
		final int keyHash = key.hashCode();
		final Result<K, Void, ? extends CompactSetNode<K>> result = rootNode.removed(null, key,
						keyHash, 0);

		if (result.isModified()) {

			return new TrieSetDynamic<K>(result.getNode(), hashCode - keyHash, cachedSize - 1);

		}

		return this;
	}

	@Override
	public TrieSetDynamic<K> __removeEquivalent(final K key, Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final Result<K, Void, ? extends CompactSetNode<K>> result = rootNode.removed(null, key,
						keyHash, 0, cmp);

		if (result.isModified()) {

			return new TrieSetDynamic<K>(result.getNode(), hashCode - keyHash, cachedSize - 1);

		}

		return this;
	}

	@Override
	public boolean contains(Object o) {
		try {
			final K key = (K) o;
			return rootNode.containsKey(key, key.hashCode(), 0);
		} catch (ClassCastException unused) {
			return false;
		}
	}

	@Override
	public boolean containsEquivalent(Object o, Comparator<Object> cmp) {
		try {
			final K key = (K) o;
			return rootNode.containsKey(key, key.hashCode(), 0, cmp);
		} catch (ClassCastException unused) {
			return false;
		}
	}

	@Override
	public K get(Object o) {
		try {
			final K key = (K) o;
			final Optional<K> result = rootNode.findByKey(key, key.hashCode(), 0);

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
	public K getEquivalent(Object o, Comparator<Object> cmp) {
		try {
			final K key = (K) o;
			final Optional<K> result = rootNode.findByKey(key, key.hashCode(), 0, cmp);

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
	public Iterator<K> iterator() {
		return keyIterator();
	}

	@Override
	public SupplierIterator<K, K> keyIterator() {
		return new SetKeyIterator<>(rootNode);
	}

	@Override
	public boolean isTransientSupported() {
		return true;
	}

	@Override
	public TransientSet<K> asTransient() {
		return new TransientTrieSet<K>(this);
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

		if (other instanceof TrieSetDynamic) {
			TrieSetDynamic<?> that = (TrieSetDynamic<?>) other;

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
	protected AbstractSetNode<K> getRootNode() {
		return rootNode;
	}

	/*
	 * For analysis purposes only.
	 */
	protected Iterator<AbstractSetNode<K>> nodeIterator() {
		return new TrieSetNodeIterator<>(rootNode);
	}

	/*
	 * For analysis purposes only.
	 */
	protected int getNodeCount() {
		final Iterator<AbstractSetNode<K>> it = nodeIterator();
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
		final Iterator<AbstractSetNode<K>> it = nodeIterator();
		final int[][] sumArityCombinations = new int[17][17];

		while (it.hasNext()) {
			final AbstractSetNode<K> node = it.next();
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

	protected static abstract class AbstractSetNode<K> extends AbstractNode<K, java.lang.Void> {

		static final int TUPLE_LENGTH = 1;

		abstract boolean containsKey(final K key, int keyHash, int shift);

		abstract boolean containsKey(final K key, int keyHash, int shift, Comparator<Object> cmp);

		abstract Optional<K> findByKey(final K key, int keyHash, int shift);

		abstract Optional<K> findByKey(final K key, int keyHash, int shift, Comparator<Object> cmp);

		abstract Result<K, Void, ? extends CompactSetNode<K>> updated(
						AtomicReference<Thread> mutator, final K key, int keyHash, int shift);

		abstract Result<K, Void, ? extends CompactSetNode<K>> updated(
						AtomicReference<Thread> mutator, final K key, int keyHash, int shift,
						Comparator<Object> cmp);

		abstract Result<K, Void, ? extends CompactSetNode<K>> removed(
						AtomicReference<Thread> mutator, final K key, int keyHash, int shift);

		abstract Result<K, Void, ? extends CompactSetNode<K>> removed(
						AtomicReference<Thread> mutator, final K key, int keyHash, int shift,
						Comparator<Object> cmp);

		static final boolean isAllowedToEdit(AtomicReference<Thread> x, AtomicReference<Thread> y) {
			return x != null && y != null && (x == y || x.get() == y.get());
		}

		abstract K getKey(int index);

		abstract AbstractSetNode<K> getNode(int index);

		abstract boolean hasNodes();

		@Deprecated
		Iterator<? extends AbstractSetNode<K>> nodeIterator() {
			return new Iterator<AbstractSetNode<K>>() {

				int nextIndex = 0;

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				@Override
				public AbstractSetNode<K> next() {
					if (!hasNext())
						throw new NoSuchElementException();
					return AbstractSetNode.this.getNode(nextIndex++);
				}

				@Override
				public boolean hasNext() {
					return nextIndex < AbstractSetNode.this.nodeArity();
				}
			};
		}

		abstract int nodeArity();

		abstract boolean hasPayload();

		@Deprecated
		SupplierIterator<K, K> payloadIterator() {
			return new SupplierIterator<K, K>() {

				int nextIndex = 0;

				@Override
				public K get() {
					if (nextIndex == 0 || nextIndex > AbstractSetNode.this.payloadArity()) {
						throw new NoSuchElementException();
					}

					return AbstractSetNode.this.getKey(nextIndex - 1);
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				@Override
				public K next() {
					if (!hasNext())
						throw new NoSuchElementException();
					return AbstractSetNode.this.getKey(nextIndex++);
				}

				@Override
				public boolean hasNext() {
					return nextIndex < AbstractSetNode.this.payloadArity();
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
			final SupplierIterator<K, K> it = new SetKeyIterator<>(this);

			int size = 0;
			while (it.hasNext()) {
				size += 1;
				it.next();
			}

			return size;
		}
	}

	private static abstract class CompactSetNode<K> extends AbstractSetNode<K> {

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

		@Override
		abstract CompactSetNode<K> getNode(int index);

		@Deprecated
		@Override
		Iterator<? extends CompactSetNode<K>> nodeIterator() {
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

		abstract CompactSetNode<K> copyAndInsertValue(AtomicReference<Thread> mutator,
						final short bitpos, final K key);

		abstract CompactSetNode<K> copyAndRemoveValue(AtomicReference<Thread> mutator,
						final short bitpos);

		abstract CompactSetNode<K> copyAndSetNode(AtomicReference<Thread> mutator,
						final short bitpos, CompactSetNode<K> node);

		CompactSetNode<K> copyAndInsertNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactSetNode<K> node) {
			throw new UnsupportedOperationException();
		}

		CompactSetNode<K> copyAndRemoveNode(AtomicReference<Thread> mutator, final short bitpos) {
			throw new UnsupportedOperationException();
		}

		CompactSetNode<K> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						final short bitpos, CompactSetNode<K> node) {
			throw new UnsupportedOperationException();
		}

		CompactSetNode<K> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						final short bitpos, CompactSetNode<K> node) {
			throw new UnsupportedOperationException();
		}

		@SuppressWarnings("unchecked")
		static final <K> CompactSetNode<K> mergeNodes(final K key0, int keyHash0, final K key1,
						int keyHash1, int shift) {
			assert !(key0.equals(key1));

			if (keyHash0 == keyHash1) {
				return new HashCollisionSetNode<>(keyHash0, (K[]) new Object[] { key0, key1 });
			}

			final int mask0 = (keyHash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (keyHash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level
				final short dataMap = (short) (1L << mask0 | 1L << mask1);

				if (mask0 < mask1) {
					return nodeOf(null, (short) 0, dataMap, new Object[] { key0, key1 }, (byte) 2);
				} else {
					return nodeOf(null, (short) 0, dataMap, new Object[] { key1, key0 }, (byte) 2);
				}
			} else {
				// values fit on next level
				final CompactSetNode<K> node = mergeNodes(key0, keyHash0, key1, keyHash1, shift
								+ BIT_PARTITION_SIZE);

				final short nodeMap = (short) (1L << mask0);
				return nodeOf(null, nodeMap, (short) 0, new Object[] { node }, (byte) 0);
			}
		}

		static final <K> CompactSetNode<K> mergeNodes(CompactSetNode<K> node0, int keyHash0,
						final K key1, int keyHash1, int shift) {
			final int mask0 = (keyHash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (keyHash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level
				final short nodeMap = (short) (1L << mask0);
				final short dataMap = (short) (1L << mask1);

				// store values before node
				return nodeOf(null, nodeMap, dataMap, new Object[] { key1, node0 }, (byte) 1);
			} else {
				// values fit on next level
				final CompactSetNode<K> node = mergeNodes(node0, keyHash0, key1, keyHash1, shift
								+ BIT_PARTITION_SIZE);

				final short nodeMap = (short) (1L << mask0);
				return nodeOf(null, nodeMap, (short) 0, new Object[] { node }, (byte) 0);
			}
		}

		static final CompactSetNode EMPTY_NODE;

		static {
			EMPTY_NODE = new BitmapIndexedSetNode<>(null, (short) 0, (short) 0, new Object[] {},
							(byte) 0);
		};

		static final <K> CompactSetNode<K> nodeOf(AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, Object[] nodes, byte payloadArity) {
			return new BitmapIndexedSetNode<>(mutator, nodeMap, dataMap, nodes, payloadArity);
		}

		@SuppressWarnings("unchecked")
		static final <K> CompactSetNode<K> nodeOf(AtomicReference<Thread> mutator) {
			return EMPTY_NODE;
		}

		static final <K> CompactSetNode<K> nodeOf(AtomicReference<Thread> mutator,
						final short nodeMap, final short dataMap, final K key) {
			assert nodeMap == 0;
			return nodeOf(mutator, (short) 0, dataMap, new Object[] { key }, (byte) 1);
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

		CompactSetNode<K> nodeAt(final short bitpos) {
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
		Optional<K> findByKey(final K key, int keyHash, int shift) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final short bitpos = (short) (1L << mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				if (keyAt(bitpos).equals(key)) {
					final K _key = keyAt(bitpos);

					return Optional.of(_key);
				}

				return Optional.empty();
			}

			if ((nodeMap() & bitpos) != 0) { // node (not value)
				final AbstractSetNode<K> subNode = nodeAt(bitpos);

				return subNode.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			}

			return Optional.empty();
		}

		@Override
		Optional<K> findByKey(final K key, int keyHash, int shift, Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final short bitpos = (short) (1L << mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				if (cmp.compare(keyAt(bitpos), key) == 0) {
					final K _key = keyAt(bitpos);

					return Optional.of(_key);
				}

				return Optional.empty();
			}

			if ((nodeMap() & bitpos) != 0) { // node (not value)
				final AbstractSetNode<K> subNode = nodeAt(bitpos);

				return subNode.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator,
						final K key, int keyHash, int shift) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final short bitpos = (short) (1L << mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final K currentKey = keyAt(bitpos);

				if (currentKey.equals(key)) {
					return Result.unchanged(this);
				} else {
					final CompactSetNode<K> nodeNew = mergeNodes(keyAt(bitpos), keyAt(bitpos)
									.hashCode(), key, keyHash, shift + BIT_PARTITION_SIZE);

					final CompactSetNode<K> thisNew = copyAndMigrateFromInlineToNode(mutator,
									bitpos, nodeNew);

					return Result.modified(thisNew);
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactSetNode<K> subNode = nodeAt(bitpos);

				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = subNode.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (!nestedResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactSetNode<K> thisNew = copyAndSetNode(mutator, bitpos,
								nestedResult.getNode());

				return Result.modified(thisNew);
			} else {
				// no value
				final CompactSetNode<K> thisNew = copyAndInsertValue(mutator, bitpos, key);

				return Result.modified(thisNew);
			}
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator,
						final K key, int keyHash, int shift, Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final short bitpos = (short) (1L << mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final K currentKey = keyAt(bitpos);

				if (cmp.compare(currentKey, key) == 0) {
					return Result.unchanged(this);
				} else {
					final CompactSetNode<K> nodeNew = mergeNodes(keyAt(bitpos), keyAt(bitpos)
									.hashCode(), key, keyHash, shift + BIT_PARTITION_SIZE);

					final CompactSetNode<K> thisNew = copyAndMigrateFromInlineToNode(mutator,
									bitpos, nodeNew);

					return Result.modified(thisNew);
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactSetNode<K> subNode = nodeAt(bitpos);

				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = subNode.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!nestedResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactSetNode<K> thisNew = copyAndSetNode(mutator, bitpos,
								nestedResult.getNode());

				return Result.modified(thisNew);
			} else {
				// no value
				final CompactSetNode<K> thisNew = copyAndInsertValue(mutator, bitpos, key);

				return Result.modified(thisNew);
			}
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator,
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
						final CompactSetNode<K> thisNew;
						final short newDataMap = (shift == 0) ? (short) (dataMap() ^ bitpos)
										: (short) (1L << (keyHash & BIT_PARTITION_MASK));

						if (dataIndex(bitpos) == 0) {
							thisNew = CompactSetNode.<K> nodeOf(mutator, (short) 0, newDataMap,
											getKey(1));
						} else {
							thisNew = CompactSetNode.<K> nodeOf(mutator, (short) 0, newDataMap,
											getKey(0));
						}

						return Result.modified(thisNew);
					} else {
						final CompactSetNode<K> thisNew = copyAndRemoveValue(mutator, bitpos);

						return Result.modified(thisNew);
					}
				} else {
					return Result.unchanged(this);
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactSetNode<K> subNode = nodeAt(bitpos);
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = subNode.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (!nestedResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactSetNode<K> subNodeNew = nestedResult.getNode();

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
						final CompactSetNode<K> thisNew = copyAndMigrateFromNodeToInline(mutator,
										bitpos, subNodeNew);
						// final CompactSetNode<K> thisNew =
						// copyAndRemoveNode(mutator,
						// bitpos).copyAndInsertValue(mutator, bitpos,
						// subNodeNew.getKey(0));

						return Result.modified(thisNew);
					}
				}
				default: {
					// modify current node (set replacement node)
					final CompactSetNode<K> thisNew = copyAndSetNode(mutator, bitpos, subNodeNew);

					return Result.modified(thisNew);
				}
				}
			}

			return Result.unchanged(this);
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator,
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
						final CompactSetNode<K> thisNew;
						final short newDataMap = (shift == 0) ? (short) (dataMap() ^ bitpos)
										: (short) (1L << (keyHash & BIT_PARTITION_MASK));

						if (dataIndex(bitpos) == 0) {
							thisNew = CompactSetNode.<K> nodeOf(mutator, (short) 0, newDataMap,
											getKey(1));
						} else {
							thisNew = CompactSetNode.<K> nodeOf(mutator, (short) 0, newDataMap,
											getKey(0));
						}

						return Result.modified(thisNew);
					} else {
						final CompactSetNode<K> thisNew = copyAndRemoveValue(mutator, bitpos);

						return Result.modified(thisNew);
					}
				} else {
					return Result.unchanged(this);
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactSetNode<K> subNode = nodeAt(bitpos);
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = subNode.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!nestedResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactSetNode<K> subNodeNew = nestedResult.getNode();

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
						final CompactSetNode<K> thisNew = copyAndMigrateFromNodeToInline(mutator,
										bitpos, subNodeNew);
						// final CompactSetNode<K> thisNew =
						// copyAndRemoveNode(mutator,
						// bitpos).copyAndInsertValue(mutator, bitpos,
						// subNodeNew.getKey(0));

						return Result.modified(thisNew);
					}
				}
				default: {
					// modify current node (set replacement node)
					final CompactSetNode<K> thisNew = copyAndSetNode(mutator, bitpos, subNodeNew);

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
				bldr.append(String.format("@%d: ", pos, getKey(i)));

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

	private static abstract class CompactMixedSetNode<K> extends CompactSetNode<K> {

		private final short nodeMap;
		private final short dataMap;

		CompactMixedSetNode(final AtomicReference<Thread> mutator, final short nodeMap,
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

	private static final class BitmapIndexedSetNode<K> extends CompactMixedSetNode<K> {
		private AtomicReference<Thread> mutator;

		private Object[] nodes;
		final private byte payloadArity;

		BitmapIndexedSetNode(AtomicReference<Thread> mutator, final short nodeMap,
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
		public CompactSetNode<K> getNode(int index) {
			final int offset = TUPLE_LENGTH * payloadArity;
			return (CompactSetNode<K>) nodes[offset + index];
		}

		@Override
		SupplierIterator<K, K> payloadIterator() {
			return ArrayKeyValueIterator.of(nodes, 0, TUPLE_LENGTH * payloadArity);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactSetNode<K>> nodeIterator() {
			final int offset = TUPLE_LENGTH * payloadArity;

			for (int i = offset; i < nodes.length - offset; i++) {
				assert ((nodes[i] instanceof AbstractSetNode) == true);
			}

			return (Iterator) ArrayIterator.of(nodes, offset, nodes.length - offset);
		}

		@SuppressWarnings("unchecked")
		@Override
		K headKey() {
			assert hasPayload();
			return (K) nodes[0];
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
			BitmapIndexedSetNode<?> that = (BitmapIndexedSetNode<?>) other;
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
		CompactSetNode<K> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactSetNode<K> node) {
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
		CompactSetNode<K> copyAndInsertValue(AtomicReference<Thread> mutator, final short bitpos,
						final K key) {
			final int idx = TUPLE_LENGTH * dataIndex(bitpos);

			final java.lang.Object[] src = this.nodes;
			@SuppressWarnings("unchecked")
			final java.lang.Object[] dst = (java.lang.Object[]) new Object[src.length + 1];

			// copy 'src' and insert 1 element(s) at position 'idx'
			System.arraycopy(src, 0, dst, 0, idx);
			dst[idx + 0] = key;
			System.arraycopy(src, idx, dst, idx + 1, src.length - idx);

			return nodeOf(mutator, nodeMap(), (short) (dataMap() | bitpos), dst,
							(byte) (payloadArity + 1));
		}

		@Override
		CompactSetNode<K> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			final int idx = TUPLE_LENGTH * dataIndex(bitpos);

			final java.lang.Object[] src = this.nodes;
			@SuppressWarnings("unchecked")
			final java.lang.Object[] dst = (java.lang.Object[]) new Object[src.length - 1];

			// copy 'src' and remove 1 element(s) at position 'idx'
			System.arraycopy(src, 0, dst, 0, idx);
			System.arraycopy(src, idx + 1, dst, idx, src.length - idx - 1);

			return nodeOf(mutator, nodeMap(), (short) (dataMap() ^ bitpos), dst,
							(byte) (payloadArity - 1));
		}

		@Override
		CompactSetNode<K> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						final short bitpos, CompactSetNode<K> node) {
			final int idxOld = TUPLE_LENGTH * dataIndex(bitpos);
			final int idxNew = TUPLE_LENGTH * (payloadArity - 1) + nodeIndex(bitpos);

			final java.lang.Object[] src = this.nodes;
			final java.lang.Object[] dst = new Object[src.length - 1 + 1];

			// copy 'src' and remove 1 element(s) at position 'idxOld' and
			// insert 1 element(s) at position 'idxNew' (TODO: carefully test)
			assert idxOld <= idxNew;
			System.arraycopy(src, 0, dst, 0, idxOld);
			System.arraycopy(src, idxOld + 1, dst, idxOld, idxNew - idxOld);
			dst[idxNew + 0] = node;
			System.arraycopy(src, idxNew + 1, dst, idxNew + 1, src.length - idxNew - 1);

			return nodeOf(mutator, (short) (nodeMap() | bitpos), (short) (dataMap() ^ bitpos), dst,
							(byte) (payloadArity - 1));
		}

		@Override
		CompactSetNode<K> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						final short bitpos, CompactSetNode<K> node) {
			final int idxOld = TUPLE_LENGTH * payloadArity + nodeIndex(bitpos);
			final int idxNew = dataIndex(bitpos);

			final java.lang.Object[] src = this.nodes;
			final java.lang.Object[] dst = new Object[src.length - 1 + 1];

			// copy 'src' and remove 1 element(s) at position 'idxOld' and
			// insert 1 element(s) at position 'idxNew' (TODO: carefully test)
			assert idxOld >= idxNew;
			System.arraycopy(src, 0, dst, 0, idxNew);
			dst[idxNew + 0] = node.headKey();
			System.arraycopy(src, idxNew, dst, idxNew + 1, idxOld - idxNew);
			System.arraycopy(src, idxOld + 1, dst, idxOld + 1, src.length - idxOld - 1);

			return nodeOf(mutator, (short) (nodeMap() ^ bitpos), (short) (dataMap() | bitpos), dst,
							(byte) (payloadArity + 1));
		}
	}

	private static final class HashCollisionSetNode<K> extends CompactSetNode<K> {
		private final K[] keys;

		private final int hash;

		HashCollisionSetNode(final int hash, final K[] keys) {
			this.keys = keys;

			this.hash = hash;

			assert payloadArity() >= 2;
		}

		@Override
		SupplierIterator<K, K> payloadIterator() {
			final Object[] keysAndVals = new Object[2 * keys.length];
			for (int i = 0; i < keys.length; i++) {
				keysAndVals[2 * i] = keys[i];
				keysAndVals[2 * i + 1] = keys[i];
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
		Optional<K> findByKey(final K key, int hash, int shift) {

			for (int i = 0; i < keys.length; i++) {
				final K _key = keys[i];
				if (key.equals(_key)) {
					return Optional.of(_key);
				}
			}
			return Optional.empty();

		}

		@Override
		Optional<K> findByKey(final K key, int hash, int shift, Comparator<Object> cmp) {

			for (int i = 0; i < keys.length; i++) {
				final K _key = keys[i];
				if (cmp.compare(key, _key) == 0) {
					return Optional.of(_key);
				}
			}
			return Optional.empty();

		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator,
						final K key, int keyHash, int shift) {
			if (this.hash != keyHash) {
				return Result.modified(mergeNodes(this, this.hash, key, keyHash, shift));
			}

			for (int idx = 0; idx < keys.length; idx++) {
				if (keys[idx].equals(key)) {

					return Result.unchanged(this);

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

			return Result.modified(new HashCollisionSetNode<>(keyHash, keysNew));
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator,
						final K key, int keyHash, int shift, Comparator<Object> cmp) {
			if (this.hash != keyHash) {
				return Result.modified(mergeNodes(this, this.hash, key, keyHash, shift));
			}

			for (int idx = 0; idx < keys.length; idx++) {
				if (cmp.compare(keys[idx], key) == 0) {

					return Result.unchanged(this);

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

			return Result.modified(new HashCollisionSetNode<>(keyHash, keysNew));
		}

		@SuppressWarnings("unchecked")
		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator,
						final K key, int keyHash, int shift) {

			for (int idx = 0; idx < keys.length; idx++) {
				if (keys[idx].equals(key)) {
					if (this.arity() == 1) {
						return Result.modified(CompactSetNode.<K> nodeOf(mutator));
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node
						 * will be a) either be the new root returned, or b)
						 * unwrapped and inlined.
						 */
						final K theOtherKey = (idx == 0) ? keys[1] : keys[0];

						return CompactSetNode.<K> nodeOf(mutator).updated(mutator, theOtherKey,
										keyHash, 0);
					} else {
						@SuppressWarnings("unchecked")
						final K[] keysNew = (K[]) new Object[this.keys.length - 1];

						// copy 'this.keys' and remove 1 element(s) at position
						// 'idx'
						System.arraycopy(this.keys, 0, keysNew, 0, idx);
						System.arraycopy(this.keys, idx + 1, keysNew, idx, this.keys.length - idx
										- 1);

						return Result.modified(new HashCollisionSetNode<>(keyHash, keysNew));
					}
				}
			}
			return Result.unchanged(this);

		}

		@SuppressWarnings("unchecked")
		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator,
						final K key, int keyHash, int shift, Comparator<Object> cmp) {

			for (int idx = 0; idx < keys.length; idx++) {
				if (cmp.compare(keys[idx], key) == 0) {
					if (this.arity() == 1) {
						return Result.modified(CompactSetNode.<K> nodeOf(mutator));
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node
						 * will be a) either be the new root returned, or b)
						 * unwrapped and inlined.
						 */
						final K theOtherKey = (idx == 0) ? keys[1] : keys[0];

						return CompactSetNode.<K> nodeOf(mutator).updated(mutator, theOtherKey,
										keyHash, 0, cmp);
					} else {
						@SuppressWarnings("unchecked")
						final K[] keysNew = (K[]) new Object[this.keys.length - 1];

						// copy 'this.keys' and remove 1 element(s) at position
						// 'idx'
						System.arraycopy(this.keys, 0, keysNew, 0, idx);
						System.arraycopy(this.keys, idx + 1, keysNew, idx, this.keys.length - idx
										- 1);

						return Result.modified(new HashCollisionSetNode<>(keyHash, keysNew));
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
		public CompactSetNode<K> getNode(int index) {
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

			HashCollisionSetNode<K> that = (HashCollisionSetNode<K>) other;

			if (hash != that.hash) {
				return false;
			}

			if (arity() != that.arity()) {
				return false;
			}

			/*
			 * Linear scan for each key, because of arbitrary element order.
			 */
			outerLoop: for (SupplierIterator<K, K> it = that.payloadIterator(); it.hasNext();) {
				final K otherKey = it.next();

				for (int i = 0; i < keys.length; i++) {
					final K key = keys[i];

					if (key.equals(otherKey)) {
						continue outerLoop;
					}
				}
				return false;

			}

			return true;
		}

		@Override
		CompactSetNode<K> copyAndInsertValue(AtomicReference<Thread> mutator, final short bitpos,
						final K key) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactSetNode<K> copyAndRemoveValue(AtomicReference<Thread> mutator, final short bitpos) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactSetNode<K> copyAndSetNode(AtomicReference<Thread> mutator, final short bitpos,
						CompactSetNode<K> node) {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Iterator skeleton that uses a fixed stack in depth.
	 */
	private static abstract class AbstractSetIterator<K> {

		// TODO: verify maximum deepness
		private static final int MAX_DEPTH = 10;

		protected int currentValueCursor;
		protected int currentValueLength;
		protected AbstractSetNode<K> currentValueNode;

		private int currentStackLevel;
		private final int[] nodeCursorsAndLengths = new int[MAX_DEPTH * 2];

		@SuppressWarnings("unchecked")
		AbstractSetNode<K>[] nodes = new AbstractSetNode[MAX_DEPTH];

		AbstractSetIterator(AbstractSetNode<K> rootNode) {
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
						final AbstractSetNode<K> nextNode = nodes[currentStackLevel]
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

	private static final class SetKeyIterator<K> extends AbstractSetIterator<K> implements
					SupplierIterator<K, K> {

		SetKeyIterator(AbstractSetNode<K> rootNode) {
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
		public K get() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Iterator that first iterates over inlined-values and then continues depth
	 * first recursively.
	 */
	private static class TrieSetNodeIterator<K> implements Iterator<AbstractSetNode<K>> {

		final Deque<Iterator<? extends AbstractSetNode<K>>> nodeIteratorStack;

		TrieSetNodeIterator(AbstractSetNode<K> rootNode) {
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
		public AbstractSetNode<K> next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			AbstractSetNode<K> innerNode = nodeIteratorStack.peek().next();

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

	static final class TransientTrieSet<K> extends AbstractSet<K> implements TransientSet<K> {
		final private AtomicReference<Thread> mutator;
		private AbstractSetNode<K> rootNode;
		private int hashCode;
		private int cachedSize;

		TransientTrieSet(TrieSetDynamic<K> trieSet) {
			this.mutator = new AtomicReference<Thread>(Thread.currentThread());
			this.rootNode = trieSet.rootNode;
			this.hashCode = trieSet.hashCode;
			this.cachedSize = trieSet.cachedSize;
			if (DEBUG) {
				assert checkHashCodeAndSize(hashCode, cachedSize);
			}
		}

		private boolean checkHashCodeAndSize(final int targetHash, final int targetSize) {
			int hash = 0;
			int size = 0;

			for (Iterator<K> it = keyIterator(); it.hasNext();) {
				final K key = it.next();

				hash += key.hashCode();
				size += 1;
			}

			return hash == targetHash && size == targetSize;
		}

		@Override
		public boolean contains(Object o) {
			try {
				final K key = (K) o;
				return rootNode.containsKey(key, key.hashCode(), 0);
			} catch (ClassCastException unused) {
				return false;
			}
		}

		@Override
		public boolean containsEquivalent(Object o, Comparator<Object> cmp) {
			try {
				final K key = (K) o;
				return rootNode.containsKey(key, key.hashCode(), 0, cmp);
			} catch (ClassCastException unused) {
				return false;
			}
		}

		@Override
		public K get(Object o) {
			try {
				final K key = (K) o;
				final Optional<K> result = rootNode.findByKey(key, key.hashCode(), 0);

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
		public K getEquivalent(Object o, Comparator<Object> cmp) {
			try {
				final K key = (K) o;
				final Optional<K> result = rootNode.findByKey(key, key.hashCode(), 0, cmp);

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
		public boolean __insert(final K key) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final Result<K, Void, ? extends CompactSetNode<K>> result = rootNode.updated(mutator,
							key, keyHash, 0);

			if (result.isModified()) {
				rootNode = result.getNode();

				hashCode += keyHash;
				cachedSize += 1;

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
		public boolean __insertEquivalent(final K key, final Comparator<Object> cmp) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final Result<K, Void, ? extends CompactSetNode<K>> result = rootNode.updated(mutator,
							key, keyHash, 0, cmp);

			if (result.isModified()) {
				rootNode = result.getNode();

				hashCode += keyHash;
				cachedSize += 1;

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
		public boolean __insertAll(final ImmutableSet<? extends K> set) {
			boolean modified = false;

			for (final K key : set) {
				modified |= __insert(key);
			}

			return modified;
		}

		@Override
		public boolean __insertAllEquivalent(final ImmutableSet<? extends K> set,
						final Comparator<Object> cmp) {
			boolean modified = false;

			for (final K key : set) {
				modified |= __insertEquivalent(key, cmp);
			}

			return modified;
		}

		@Override
		public boolean __removeAll(final ImmutableSet<? extends K> set) {
			boolean modified = false;

			for (final K key : set) {
				modified |= __remove(key);
			}

			return modified;
		}

		@Override
		public boolean __removeAllEquivalent(final ImmutableSet<? extends K> set,
						final Comparator<Object> cmp) {
			boolean modified = false;

			for (final K key : set) {
				modified |= __removeEquivalent(key, cmp);
			}

			return modified;
		}

		@Override
		public boolean __remove(final K key) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");

			}

			final int keyHash = key.hashCode();
			final Result<K, Void, ? extends CompactSetNode<K>> result = rootNode.removed(mutator,
							key, keyHash, 0);

			if (result.isModified()) {

				rootNode = result.getNode();
				hashCode -= keyHash;
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
			final Result<K, Void, ? extends CompactSetNode<K>> result = rootNode.removed(mutator,
							key, keyHash, 0, cmp);

			if (result.isModified()) {

				rootNode = result.getNode();
				hashCode -= keyHash;
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
		public boolean containsAll(Collection<?> c) {
			for (Object item : c) {
				if (!contains(item)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean containsAllEquivalent(Collection<?> c, Comparator<Object> cmp) {
			for (Object item : c) {
				if (!containsEquivalent(item, cmp)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean __retainAll(ImmutableSet<? extends K> set) {
			boolean modified = false;

			Iterator<K> thisIterator = iterator();
			while (thisIterator.hasNext()) {
				if (!set.contains(thisIterator.next())) {
					thisIterator.remove();
					modified = true;
				}
			}

			return modified;
		}

		@Override
		public boolean __retainAllEquivalent(ImmutableSet<? extends K> set, Comparator<Object> cmp) {
			boolean modified = false;

			Iterator<K> thisIterator = iterator();
			while (thisIterator.hasNext()) {
				if (!set.containsEquivalent(thisIterator.next(), cmp)) {
					thisIterator.remove();
					modified = true;
				}
			}

			return modified;
		}

		@Override
		public int size() {
			return cachedSize;
		}

		@Override
		public Iterator<K> iterator() {
			return keyIterator();
		}

		@Override
		public SupplierIterator<K, K> keyIterator() {
			return new TransientSetKeyIterator<>(this);
		}

		/**
		 * Iterator that first iterates over inlined-values and then continues
		 * depth first recursively.
		 */
		private static class TransientSetKeyIterator<K> extends AbstractSetIterator<K> implements
						SupplierIterator<K, K> {

			final TransientTrieSet<K> transientTrieSet;
			K lastKey;

			TransientSetKeyIterator(TransientTrieSet<K> transientTrieSet) {
				super(transientTrieSet.rootNode);
				this.transientTrieSet = transientTrieSet;
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
			public K get() {
				throw new UnsupportedOperationException();
			}

			/*
			 * TODO: test removal with iteration rigorously
			 */
			@Override
			public void remove() {
				boolean success = transientTrieSet.__remove(lastKey);

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

			if (other instanceof TransientTrieSet) {
				TransientTrieSet<?> that = (TransientTrieSet<?>) other;

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
		public ImmutableSet<K> freeze() {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			mutator.set(null);
			return new TrieSetDynamic<K>(rootNode, hashCode, cachedSize);
		}
	}

}