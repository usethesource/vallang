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
public class TrieSet_5Bits_IntKey extends AbstractSet<java.lang.Integer> implements
				ImmutableSet<java.lang.Integer> {

	@SuppressWarnings("unchecked")
	private static final TrieSet_5Bits_IntKey EMPTY_SET = new TrieSet_5Bits_IntKey(
					CompactSetNode.EMPTY_NODE, 0, 0);

	private static final boolean DEBUG = false;

	private final AbstractSetNode rootNode;
	private final int hashCode;
	private final int cachedSize;

	TrieSet_5Bits_IntKey(AbstractSetNode rootNode, int hashCode, int cachedSize) {
		this.rootNode = rootNode;
		this.hashCode = hashCode;
		this.cachedSize = cachedSize;
		if (DEBUG) {
			assert checkHashCodeAndSize(hashCode, cachedSize);
		}
	}

	@SuppressWarnings("unchecked")
	public static final ImmutableSet<java.lang.Integer> of() {
		return TrieSet_5Bits_IntKey.EMPTY_SET;
	}

	@SuppressWarnings("unchecked")
	public static final ImmutableSet<java.lang.Integer> of(int... keys) {
		ImmutableSet<java.lang.Integer> result = TrieSet_5Bits_IntKey.EMPTY_SET;

		for (final int key : keys) {
			result = result.__insert(key);
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public static final TransientSet<java.lang.Integer> transientOf() {
		return TrieSet_5Bits_IntKey.EMPTY_SET.asTransient();
	}

	@SuppressWarnings("unchecked")
	public static final TransientSet<java.lang.Integer> transientOf(int... keys) {
		final TransientSet<java.lang.Integer> result = TrieSet_5Bits_IntKey.EMPTY_SET.asTransient();

		for (final int key : keys) {
			result.__insert(key);
		}

		return result;
	}

	private boolean checkHashCodeAndSize(final int targetHash, final int targetSize) {
		int hash = 0;
		int size = 0;

		for (Iterator<java.lang.Integer> it = keyIterator(); it.hasNext();) {
			final int key = it.next();

			hash += (int) key;
			size += 1;
		}

		return hash == targetHash && size == targetSize;
	}

	@Override
	public TrieSet_5Bits_IntKey __insert(final java.lang.Integer key) {
		final int keyHash = key.hashCode();
		final Result details = Result.unchanged();

		final CompactSetNode newRootNode = rootNode.updated(null, key, keyHash, 0, details);

		if (details.isModified()) {

			return new TrieSet_5Bits_IntKey(newRootNode, hashCode + keyHash, cachedSize + 1);

		}

		return this;
	}

	@Override
	public TrieSet_5Bits_IntKey __insertEquivalent(final java.lang.Integer key,
					final Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final Result details = Result.unchanged();

		final CompactSetNode newRootNode = rootNode.updated(null, key, keyHash, 0, details, cmp);

		if (details.isModified()) {

			return new TrieSet_5Bits_IntKey(newRootNode, hashCode + keyHash, cachedSize + 1);

		}

		return this;
	}

	@Override
	public ImmutableSet<java.lang.Integer> __remove(final java.lang.Integer key) {
		final int keyHash = key.hashCode();
		final Result details = Result.unchanged();

		final CompactSetNode newRootNode = rootNode.removed(null, key, keyHash, 0, details);

		if (details.isModified()) {

			return new TrieSet_5Bits_IntKey(newRootNode, hashCode - keyHash, cachedSize - 1);

		}

		return this;
	}

	@Override
	public ImmutableSet<java.lang.Integer> __removeEquivalent(final java.lang.Integer key,
					final Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final Result details = Result.unchanged();

		final CompactSetNode newRootNode = rootNode.removed(null, key, keyHash, 0, details, cmp);

		if (details.isModified()) {

			return new TrieSet_5Bits_IntKey(newRootNode, hashCode - keyHash, cachedSize - 1);

		}

		return this;
	}

	@Override
	public boolean contains(final java.lang.Object o) {
		try {
			@SuppressWarnings("unchecked")
			final int key = (int) o;
			return rootNode.containsKey(key, (int) key, 0);
		} catch (ClassCastException unused) {
			return false;
		}
	}

	@Override
	public boolean containsEquivalent(final java.lang.Object o, final Comparator<Object> cmp) {
		try {
			@SuppressWarnings("unchecked")
			final int key = (int) o;
			return rootNode.containsKey(key, (int) key, 0, cmp);
		} catch (ClassCastException unused) {
			return false;
		}
	}

	@Override
	public java.lang.Integer get(final java.lang.Object o) {
		try {
			@SuppressWarnings("unchecked")
			final int key = (int) o;
			final Optional<java.lang.Integer> result = rootNode.findByKey(key, (int) key, 0);

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
	public java.lang.Integer getEquivalent(final java.lang.Object o, final Comparator<Object> cmp) {
		try {
			@SuppressWarnings("unchecked")
			final int key = (int) o;
			final Optional<java.lang.Integer> result = rootNode.findByKey(key, (int) key, 0, cmp);

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
	public ImmutableSet<java.lang.Integer> __insertAll(
					final ImmutableSet<? extends java.lang.Integer> set) {
		TransientSet<java.lang.Integer> tmp = asTransient();
		tmp.__insertAll(set);
		return tmp.freeze();
	}

	@Override
	public ImmutableSet<java.lang.Integer> __insertAllEquivalent(
					final ImmutableSet<? extends java.lang.Integer> set,
					final Comparator<Object> cmp) {
		TransientSet<java.lang.Integer> tmp = asTransient();
		tmp.__insertAllEquivalent(set, cmp);
		return tmp.freeze();
	}

	@Override
	public ImmutableSet<java.lang.Integer> __retainAll(
					final ImmutableSet<? extends java.lang.Integer> set) {
		TransientSet<java.lang.Integer> tmp = asTransient();
		tmp.__retainAll(set);
		return tmp.freeze();
	}

	@Override
	public ImmutableSet<java.lang.Integer> __retainAllEquivalent(
					final ImmutableSet<? extends java.lang.Integer> set,
					final Comparator<Object> cmp) {
		TransientSet<java.lang.Integer> tmp = asTransient();
		tmp.__retainAllEquivalent(set, cmp);
		return tmp.freeze();
	}

	@Override
	public ImmutableSet<java.lang.Integer> __removeAll(
					final ImmutableSet<? extends java.lang.Integer> set) {
		TransientSet<java.lang.Integer> tmp = asTransient();
		tmp.__removeAll(set);
		return tmp.freeze();
	}

	@Override
	public ImmutableSet<java.lang.Integer> __removeAllEquivalent(
					final ImmutableSet<? extends java.lang.Integer> set,
					final Comparator<Object> cmp) {
		TransientSet<java.lang.Integer> tmp = asTransient();
		tmp.__removeAllEquivalent(set, cmp);
		return tmp.freeze();
	}

	@Override
	public boolean add(final java.lang.Integer key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(final java.lang.Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(final Collection<? extends java.lang.Integer> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		for (Object item : c) {
			if (!contains(item)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean containsAllEquivalent(final Collection<?> c, final Comparator<Object> cmp) {
		for (Object item : c) {
			if (!containsEquivalent(item, cmp)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int size() {
		return cachedSize;
	}

	@Override
	public Iterator<java.lang.Integer> iterator() {
		return keyIterator();
	}

	@Override
	public SupplierIterator<java.lang.Integer, java.lang.Integer> keyIterator() {
		return new SetKeyIterator(rootNode);
	}

	@Override
	public boolean isTransientSupported() {
		return true;
	}

	@Override
	public TransientSet<java.lang.Integer> asTransient() {
		return new TransientTrieSet_5Bits_IntKey(this);
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

		if (other instanceof TrieSet_5Bits_IntKey) {
			TrieSet_5Bits_IntKey that = (TrieSet_5Bits_IntKey) other;

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
	protected AbstractSetNode getRootNode() {
		return rootNode;
	}

	/*
	 * For analysis purposes only.
	 */
	protected Iterator<AbstractSetNode> nodeIterator() {
		return new TrieSet_5Bits_IntKeyNodeIterator(rootNode);
	}

	/*
	 * For analysis purposes only.
	 */
	protected int getNodeCount() {
		final Iterator<AbstractSetNode> it = nodeIterator();
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
		final Iterator<AbstractSetNode> it = nodeIterator();
		final int[][] sumArityCombinations = new int[33][33];

		while (it.hasNext()) {
			final AbstractSetNode node = it.next();
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

	static final class Result {
		private int replacedValue;
		private boolean isModified;
		private boolean isReplaced;

		// update: inserted/removed single element, element count changed
		public void modified() {
			this.isModified = true;
		}

		public void updated(int replacedValue) {
			this.replacedValue = replacedValue;
			this.isModified = true;
			this.isReplaced = true;
		}

		// update: neither element, nor element count changed
		public static Result unchanged() {
			return new Result();
		}

		private Result() {
		}

		public boolean isModified() {
			return isModified;
		}

		public boolean hasReplacedValue() {
			return isReplaced;
		}

		public int getReplacedValue() {
			return replacedValue;
		}
	}

	protected static abstract class AbstractNode<K, V> {
	}

	protected static abstract class AbstractSetNode extends
					AbstractNode<java.lang.Integer, java.lang.Void> {

		static final int TUPLE_LENGTH = 1;

		abstract boolean containsKey(final int key, final int keyHash, final int shift);

		abstract boolean containsKey(final int key, final int keyHash, final int shift,
						final Comparator<Object> cmp);

		abstract Optional<java.lang.Integer> findByKey(final int key, final int keyHash,
						final int shift);

		abstract Optional<java.lang.Integer> findByKey(final int key, final int keyHash,
						final int shift, final Comparator<Object> cmp);

		abstract CompactSetNode updated(final AtomicReference<Thread> mutator, final int key,
						final int keyHash, final int shift, final Result details);

		abstract CompactSetNode updated(final AtomicReference<Thread> mutator, final int key,
						final int keyHash, final int shift, final Result details,
						final Comparator<Object> cmp);

		abstract CompactSetNode removed(final AtomicReference<Thread> mutator, final int key,
						final int keyHash, final int shift, final Result details);

		abstract CompactSetNode removed(final AtomicReference<Thread> mutator, final int key,
						final int keyHash, final int shift, final Result details,
						final Comparator<Object> cmp);

		static final boolean isAllowedToEdit(AtomicReference<Thread> x, AtomicReference<Thread> y) {
			return x != null && y != null && (x == y || x.get() == y.get());
		}

		abstract AbstractSetNode getNode(final int index);

		abstract boolean hasNodes();

		abstract int nodeArity();

		@Deprecated
		Iterator<? extends AbstractSetNode> nodeIterator() {
			return new Iterator<AbstractSetNode>() {

				int nextIndex = 0;

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				@Override
				public AbstractSetNode next() {
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

		abstract int getKey(final int index);

		abstract boolean hasPayload();

		abstract int payloadArity();

		@Deprecated
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
			final SupplierIterator<java.lang.Integer, java.lang.Integer> it = new SetKeyIterator(
							this);

			int size = 0;
			while (it.hasNext()) {
				size += 1;
				it.next();
			}

			return size;
		}

	}

	private static abstract class CompactSetNode extends AbstractSetNode {

		static final int BIT_PARTITION_SIZE = 5;
		static final int BIT_PARTITION_MASK = 0b11111;

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
		abstract CompactSetNode getNode(final int index);

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

		abstract CompactSetNode copyAndInsertValue(AtomicReference<Thread> mutator,
						final int bitpos, final int key);

		abstract CompactSetNode copyAndRemoveValue(AtomicReference<Thread> mutator, final int bitpos);

		abstract CompactSetNode copyAndSetNode(AtomicReference<Thread> mutator, final int bitpos,
						CompactSetNode node);

		abstract CompactSetNode copyAndMigrateFromInlineToNode(
						final AtomicReference<Thread> mutator, final int bitpos,
						final CompactSetNode node);

		abstract CompactSetNode copyAndMigrateFromNodeToInline(
						final AtomicReference<Thread> mutator, final int bitpos,
						final CompactSetNode node);

		/*
		 * TODO: specialize removed(..) to remove this method from this
		 * interface
		 */

		@SuppressWarnings("unchecked")
		static final CompactSetNode mergeNodes(final int key0, int keyHash0, final int key1,
						int keyHash1, int shift) {
			assert !(key0 == key1);

			if (keyHash0 == keyHash1) {
				return new HashCollisionSetNode_5Bits_IntKey(keyHash0, (int[]) new int[] { key0,
								key1 });
			}

			final int mask0 = (keyHash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (keyHash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level
				final int dataMap = (int) (1L << mask0 | 1L << mask1);

				if (mask0 < mask1) {
					return nodeOf(null, (int) 0, dataMap, new Object[] { key0, key1 }, (byte) 2);
				} else {
					return nodeOf(null, (int) 0, dataMap, new Object[] { key1, key0 }, (byte) 2);
				}
			} else {
				// values fit on next level
				final CompactSetNode node = mergeNodes(key0, keyHash0, key1, keyHash1, shift
								+ BIT_PARTITION_SIZE);

				final int nodeMap = (int) (1L << mask0);
				return nodeOf(null, nodeMap, (int) 0, new Object[] { node }, (byte) 0);
			}
		}

		static final CompactSetNode mergeNodes(CompactSetNode node0, int keyHash0, final int key1,
						int keyHash1, int shift) {
			final int mask0 = (keyHash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (keyHash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level
				final int nodeMap = (int) (1L << mask0);
				final int dataMap = (int) (1L << mask1);

				// store values before node
				return nodeOf(null, nodeMap, dataMap, new Object[] { key1, node0 }, (byte) 1);
			} else {
				// values fit on next level
				final CompactSetNode node = mergeNodes(node0, keyHash0, key1, keyHash1, shift
								+ BIT_PARTITION_SIZE);

				final int nodeMap = (int) (1L << mask0);
				return nodeOf(null, nodeMap, (int) 0, new Object[] { node }, (byte) 0);
			}
		}

		static final CompactSetNode EMPTY_NODE;

		static {
			EMPTY_NODE = new BitmapIndexedSetNode(null, (int) 0, (int) 0, new Object[] {}, (byte) 0);
		};

		static final CompactSetNode nodeOf(AtomicReference<Thread> mutator, final int nodeMap,
						final int dataMap, Object[] nodes, byte payloadArity) {
			return new BitmapIndexedSetNode(mutator, nodeMap, dataMap, nodes, payloadArity);
		}

		@SuppressWarnings("unchecked")
		static final CompactSetNode nodeOf(AtomicReference<Thread> mutator) {
			return EMPTY_NODE;
		}

		static final CompactSetNode nodeOf(AtomicReference<Thread> mutator, final int nodeMap,
						final int dataMap, final int key) {
			assert nodeMap == 0;
			return nodeOf(mutator, (int) 0, dataMap, new Object[] { key }, (byte) 1);
		}

		final int dataIndex(final int bitpos) {
			return java.lang.Integer.bitCount(dataMap() & (bitpos - 1));
		}

		final int nodeIndex(final int bitpos) {
			return java.lang.Integer.bitCount(nodeMap() & (bitpos - 1));
		}

		int keyAt(final int bitpos) {
			return getKey(dataIndex(bitpos));
		}

		CompactSetNode nodeAt(final int bitpos) {
			return getNode(nodeIndex(bitpos));
		}

		@Override
		boolean containsKey(final int key, final int keyHash, final int shift) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (int) (1L << mask);

			if ((dataMap() & bitpos) != 0) {
				return keyAt(bitpos) == key;
			}

			if ((nodeMap() & bitpos) != 0) {
				return nodeAt(bitpos).containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			}

			return false;
		}

		@Override
		boolean containsKey(final int key, final int keyHash, final int shift,
						final Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (int) (1L << mask);

			if ((dataMap() & bitpos) != 0) {
				return keyAt(bitpos) == key;
			}

			if ((nodeMap() & bitpos) != 0) {
				return nodeAt(bitpos).containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<java.lang.Integer> findByKey(final int key, final int keyHash, final int shift) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (int) (1L << mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				if (keyAt(bitpos) == key) {
					final int _key = keyAt(bitpos);

					return Optional.of(_key);
				}

				return Optional.empty();
			}

			if ((nodeMap() & bitpos) != 0) { // node (not value)
				final AbstractSetNode subNode = nodeAt(bitpos);

				return subNode.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			}

			return Optional.empty();
		}

		@Override
		Optional<java.lang.Integer> findByKey(final int key, final int keyHash, final int shift,
						final Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (int) (1L << mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				if (keyAt(bitpos) == key) {
					final int _key = keyAt(bitpos);

					return Optional.of(_key);
				}

				return Optional.empty();
			}

			if ((nodeMap() & bitpos) != 0) { // node (not value)
				final AbstractSetNode subNode = nodeAt(bitpos);

				return subNode.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@Override
		CompactSetNode updated(final AtomicReference<Thread> mutator, final int key,
						final int keyHash, final int shift, final Result details) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (int) (1L << mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int dataIndex = dataIndex(bitpos);
				final int currentKey = getKey(dataIndex);

				if (currentKey == key) {
					return this;
				} else {

					final CompactSetNode subNodeNew = mergeNodes(currentKey, (int) currentKey, key,
									keyHash, shift + BIT_PARTITION_SIZE);

					details.modified();
					return copyAndMigrateFromInlineToNode(mutator, bitpos, subNodeNew);

				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactSetNode subNode = nodeAt(bitpos);
				final CompactSetNode subNodeNew = subNode.updated(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, details);

				if (details.isModified()) {
					return copyAndSetNode(mutator, bitpos, subNodeNew);
				} else {
					return this;
				}
			} else {
				// no value
				details.modified();
				return copyAndInsertValue(mutator, bitpos, key);
			}
		}

		@Override
		CompactSetNode updated(final AtomicReference<Thread> mutator, final int key,
						final int keyHash, final int shift, final Result details,
						final Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (int) (1L << mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int dataIndex = dataIndex(bitpos);
				final int currentKey = getKey(dataIndex);

				if (currentKey == key) {
					return this;
				} else {

					final CompactSetNode subNodeNew = mergeNodes(currentKey, (int) currentKey, key,
									keyHash, shift + BIT_PARTITION_SIZE);

					details.modified();
					return copyAndMigrateFromInlineToNode(mutator, bitpos, subNodeNew);

				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactSetNode subNode = nodeAt(bitpos);
				final CompactSetNode subNodeNew = subNode.updated(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, details, cmp);

				if (details.isModified()) {
					return copyAndSetNode(mutator, bitpos, subNodeNew);
				} else {
					return this;
				}
			} else {
				// no value
				details.modified();
				return copyAndInsertValue(mutator, bitpos, key);
			}
		}

		@Override
		CompactSetNode removed(final AtomicReference<Thread> mutator, final int key,
						final int keyHash, final int shift, final Result details) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (int) (1L << mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int dataIndex = dataIndex(bitpos);

				if (getKey(dataIndex) == key) {
					details.modified();

					if (this.payloadArity() == 2 && this.nodeArity() == 0) {
						/*
						 * Create new node with remaining pair. The new node
						 * will a) either become the new root returned, or b)
						 * unwrapped and inlined during returning.
						 */
						final int newDataMap = (shift == 0) ? (int) (dataMap() ^ bitpos)
										: (int) (1L << (keyHash & BIT_PARTITION_MASK));

						if (dataIndex == 0) {
							return CompactSetNode.nodeOf(mutator, (int) 0, newDataMap, getKey(1));
						} else {
							return CompactSetNode.nodeOf(mutator, (int) 0, newDataMap, getKey(0));
						}
					} else {
						return copyAndRemoveValue(mutator, bitpos);
					}
				} else {
					return this;
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactSetNode subNode = nodeAt(bitpos);
				final CompactSetNode subNodeNew = subNode.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, details);

				if (!details.isModified()) {
					return this;
				}

				switch (subNodeNew.sizePredicate()) {
				case 0: {
					throw new IllegalStateException("Sub-node must have at least one element.");
				}
				case 1: {
					if (this.payloadArity() == 0 && this.nodeArity() == 1) {
						// escalate (singleton or empty) result
						return subNodeNew;
					} else {
						// inline value (move to front)
						return copyAndMigrateFromNodeToInline(mutator, bitpos, subNodeNew);
					}
				}
				default: {
					// modify current node (set replacement node)
					return copyAndSetNode(mutator, bitpos, subNodeNew);
				}
				}
			}

			return this;
		}

		@Override
		CompactSetNode removed(final AtomicReference<Thread> mutator, final int key,
						final int keyHash, final int shift, final Result details,
						final Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (int) (1L << mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int dataIndex = dataIndex(bitpos);

				if (getKey(dataIndex) == key) {
					details.modified();

					if (this.payloadArity() == 2 && this.nodeArity() == 0) {
						/*
						 * Create new node with remaining pair. The new node
						 * will a) either become the new root returned, or b)
						 * unwrapped and inlined during returning.
						 */
						final int newDataMap = (shift == 0) ? (int) (dataMap() ^ bitpos)
										: (int) (1L << (keyHash & BIT_PARTITION_MASK));

						if (dataIndex == 0) {
							return CompactSetNode.nodeOf(mutator, (int) 0, newDataMap, getKey(1));
						} else {
							return CompactSetNode.nodeOf(mutator, (int) 0, newDataMap, getKey(0));
						}
					} else {
						return copyAndRemoveValue(mutator, bitpos);
					}
				} else {
					return this;
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactSetNode subNode = nodeAt(bitpos);
				final CompactSetNode subNodeNew = subNode.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, details, cmp);

				if (!details.isModified()) {
					return this;
				}

				switch (subNodeNew.sizePredicate()) {
				case 0: {
					throw new IllegalStateException("Sub-node must have at least one element.");
				}
				case 1: {
					if (this.payloadArity() == 0 && this.nodeArity() == 1) {
						// escalate (singleton or empty) result
						return subNodeNew;
					} else {
						// inline value (move to front)
						return copyAndMigrateFromNodeToInline(mutator, bitpos, subNodeNew);
					}
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

	private static abstract class CompactMixedSetNode extends CompactSetNode {

		private final int nodeMap;
		private final int dataMap;

		CompactMixedSetNode(final AtomicReference<Thread> mutator, final int nodeMap,
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

	private static final class BitmapIndexedSetNode extends CompactMixedSetNode {
		private AtomicReference<Thread> mutator;

		private Object[] nodes;
		final private byte payloadArity;

		BitmapIndexedSetNode(AtomicReference<Thread> mutator, final int nodeMap, final int dataMap,
						Object[] nodes, byte payloadArity) {
			super(mutator, nodeMap, dataMap);

			assert (TUPLE_LENGTH * java.lang.Integer.bitCount(dataMap)
							+ java.lang.Integer.bitCount(nodeMap) == nodes.length);

			this.mutator = mutator;

			this.nodes = nodes;
			this.payloadArity = payloadArity;

			assert (payloadArity == java.lang.Integer.bitCount(dataMap));
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

		@Override
		int getKey(int index) {
			return (int) nodes[TUPLE_LENGTH * index];
		}

		@SuppressWarnings("unchecked")
		@Override
		public CompactSetNode getNode(int index) {
			final int offset = TUPLE_LENGTH * payloadArity;
			return (CompactSetNode) nodes[offset + index];
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactSetNode> nodeIterator() {
			final int offset = TUPLE_LENGTH * payloadArity;

			for (int i = offset; i < nodes.length - offset; i++) {
				assert ((nodes[i] instanceof AbstractSetNode) == true);
			}

			return (Iterator) ArrayIterator.of(nodes, offset, nodes.length - offset);
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
			BitmapIndexedSetNode that = (BitmapIndexedSetNode) other;
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
		CompactSetNode copyAndSetNode(AtomicReference<Thread> mutator, final int bitpos,
						CompactSetNode node) {
			final int idx = TUPLE_LENGTH * payloadArity + nodeIndex(bitpos);

			if (isAllowedToEdit(this.mutator, mutator)) {
				// no copying if already editable
				this.nodes[idx] = node;
				return this;
			} else {
				final java.lang.Object[] src = this.nodes;
				final java.lang.Object[] dst = (java.lang.Object[]) new Object[src.length];

				// copy 'src' and set 1 element(s) at position 'idx'
				System.arraycopy(src, 0, dst, 0, src.length);
				dst[idx + 0] = node;

				return nodeOf(mutator, nodeMap(), dataMap(), dst, payloadArity);
			}
		}

		@Override
		CompactSetNode copyAndInsertValue(AtomicReference<Thread> mutator, final int bitpos,
						final int key) {
			final int idx = TUPLE_LENGTH * dataIndex(bitpos);

			final java.lang.Object[] src = this.nodes;
			final java.lang.Object[] dst = (java.lang.Object[]) new Object[src.length + 1];

			// copy 'src' and insert 1 element(s) at position 'idx'
			System.arraycopy(src, 0, dst, 0, idx);
			dst[idx + 0] = key;
			System.arraycopy(src, idx, dst, idx + 1, src.length - idx);

			return nodeOf(mutator, nodeMap(), (int) (dataMap() | bitpos), dst,
							(byte) (payloadArity + 1));
		}

		@Override
		CompactSetNode copyAndRemoveValue(AtomicReference<Thread> mutator, final int bitpos) {
			final int idx = TUPLE_LENGTH * dataIndex(bitpos);

			final java.lang.Object[] src = this.nodes;
			final java.lang.Object[] dst = (java.lang.Object[]) new Object[src.length - 1];

			// copy 'src' and remove 1 element(s) at position 'idx'
			System.arraycopy(src, 0, dst, 0, idx);
			System.arraycopy(src, idx + 1, dst, idx, src.length - idx - 1);

			return nodeOf(mutator, nodeMap(), (int) (dataMap() ^ bitpos), dst,
							(byte) (payloadArity - 1));
		}

		@Override
		CompactSetNode copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						final int bitpos, CompactSetNode node) {
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

			return nodeOf(mutator, (int) (nodeMap() | bitpos), (int) (dataMap() ^ bitpos), dst,
							(byte) (payloadArity - 1));
		}

		@Override
		CompactSetNode copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						final int bitpos, CompactSetNode node) {
			final int idxOld = TUPLE_LENGTH * payloadArity + nodeIndex(bitpos);
			final int idxNew = dataIndex(bitpos);

			final java.lang.Object[] src = this.nodes;
			final java.lang.Object[] dst = new Object[src.length - 1 + 1];

			// copy 'src' and remove 1 element(s) at position 'idxOld' and
			// insert 1 element(s) at position 'idxNew' (TODO: carefully test)
			assert idxOld >= idxNew;
			System.arraycopy(src, 0, dst, 0, idxNew);
			dst[idxNew + 0] = node.getKey(0);
			System.arraycopy(src, idxNew, dst, idxNew + 1, idxOld - idxNew);
			System.arraycopy(src, idxOld + 1, dst, idxOld + 1, src.length - idxOld - 1);

			return nodeOf(mutator, (int) (nodeMap() ^ bitpos), (int) (dataMap() | bitpos), dst,
							(byte) (payloadArity + 1));
		}

	}

	private static final class HashCollisionSetNode_5Bits_IntKey extends CompactSetNode {
		private final int[] keys;

		private final int hash;

		HashCollisionSetNode_5Bits_IntKey(final int hash, final int[] keys) {
			this.keys = keys;

			this.hash = hash;

			assert payloadArity() >= 2;
		}

		@Override
		boolean containsKey(final int key, final int keyHash, final int shift) {

			if (this.hash == keyHash) {
				for (int k : keys) {
					if (k == key) {
						return true;
					}
				}
			}
			return false;

		}

		@Override
		boolean containsKey(final int key, final int keyHash, final int shift,
						final Comparator<Object> cmp) {

			if (this.hash == keyHash) {
				for (int k : keys) {
					if (k == key) {
						return true;
					}
				}
			}
			return false;

		}

		@Override
		Optional<java.lang.Integer> findByKey(final int key, final int keyHash, final int shift) {

			for (int i = 0; i < keys.length; i++) {
				final int _key = keys[i];
				if (key == _key) {
					return Optional.of(_key);
				}
			}
			return Optional.empty();

		}

		@Override
		Optional<java.lang.Integer> findByKey(final int key, final int keyHash, final int shift,
						final Comparator<Object> cmp) {

			for (int i = 0; i < keys.length; i++) {
				final int _key = keys[i];
				if (key == _key) {
					return Optional.of(_key);
				}
			}
			return Optional.empty();

		}

		@Override
		CompactSetNode updated(final AtomicReference<Thread> mutator, final int key,
						final int keyHash, final int shift, final Result details) {
			if (this.hash != keyHash) {
				details.modified();
				return mergeNodes(this, this.hash, key, keyHash, shift);
			}

			for (int idx = 0; idx < keys.length; idx++) {
				if (keys[idx] == key) {

					return this;

				}
			}

			final int[] keysNew = new int[this.keys.length + 1];

			// copy 'this.keys' and insert 1 element(s) at position
			// 'keys.length'
			System.arraycopy(this.keys, 0, keysNew, 0, keys.length);
			keysNew[keys.length + 0] = key;
			System.arraycopy(this.keys, keys.length, keysNew, keys.length + 1, this.keys.length
							- keys.length);

			details.modified();
			return new HashCollisionSetNode_5Bits_IntKey(keyHash, keysNew);
		}

		@Override
		CompactSetNode updated(final AtomicReference<Thread> mutator, final int key,
						final int keyHash, final int shift, final Result details,
						final Comparator<Object> cmp) {
			if (this.hash != keyHash) {
				details.modified();
				return mergeNodes(this, this.hash, key, keyHash, shift);
			}

			for (int idx = 0; idx < keys.length; idx++) {
				if (keys[idx] == key) {

					return this;

				}
			}

			final int[] keysNew = new int[this.keys.length + 1];

			// copy 'this.keys' and insert 1 element(s) at position
			// 'keys.length'
			System.arraycopy(this.keys, 0, keysNew, 0, keys.length);
			keysNew[keys.length + 0] = key;
			System.arraycopy(this.keys, keys.length, keysNew, keys.length + 1, this.keys.length
							- keys.length);

			details.modified();
			return new HashCollisionSetNode_5Bits_IntKey(keyHash, keysNew);
		}

		@Override
		CompactSetNode removed(final AtomicReference<Thread> mutator, final int key,
						final int keyHash, final int shift, final Result details) {

			for (int idx = 0; idx < keys.length; idx++) {
				if (keys[idx] == key) {

					if (this.arity() == 1) {
						return nodeOf(mutator);
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node
						 * will be a) either be the new root returned, or b)
						 * unwrapped and inlined.
						 */
						final int theOtherKey = (idx == 0) ? keys[1] : keys[0];

						return CompactSetNode.nodeOf(mutator).updated(mutator, theOtherKey,
										keyHash, 0, details);
					} else {
						final int[] keysNew = new int[this.keys.length - 1];

						// copy 'this.keys' and remove 1 element(s) at position
						// 'idx'
						System.arraycopy(this.keys, 0, keysNew, 0, idx);
						System.arraycopy(this.keys, idx + 1, keysNew, idx, this.keys.length - idx
										- 1);

						return new HashCollisionSetNode_5Bits_IntKey(keyHash, keysNew);
					}
				}
			}
			return this;

		}

		@Override
		CompactSetNode removed(final AtomicReference<Thread> mutator, final int key,
						final int keyHash, final int shift, final Result details,
						final Comparator<Object> cmp) {

			for (int idx = 0; idx < keys.length; idx++) {
				if (keys[idx] == key) {

					if (this.arity() == 1) {
						return nodeOf(mutator);
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node
						 * will be a) either be the new root returned, or b)
						 * unwrapped and inlined.
						 */
						final int theOtherKey = (idx == 0) ? keys[1] : keys[0];

						return CompactSetNode.nodeOf(mutator).updated(mutator, theOtherKey,
										keyHash, 0, details, cmp);
					} else {
						final int[] keysNew = new int[this.keys.length - 1];

						// copy 'this.keys' and remove 1 element(s) at position
						// 'idx'
						System.arraycopy(this.keys, 0, keysNew, 0, idx);
						System.arraycopy(this.keys, idx + 1, keysNew, idx, this.keys.length - idx
										- 1);

						return new HashCollisionSetNode_5Bits_IntKey(keyHash, keysNew);
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
		int getKey(int index) {
			return keys[index];
		}

		@Override
		public CompactSetNode getNode(int index) {
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

			HashCollisionSetNode_5Bits_IntKey that = (HashCollisionSetNode_5Bits_IntKey) other;

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
				final int otherKey = that.getKey(i);

				for (int j = 0; j < keys.length; j++) {
					final int key = keys[j];

					if (key == otherKey) {
						continue outerLoop;
					}
				}
				return false;

			}

			return true;
		}

		@Override
		CompactSetNode copyAndInsertValue(AtomicReference<Thread> mutator, final int bitpos,
						final int key) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactSetNode copyAndRemoveValue(AtomicReference<Thread> mutator, final int bitpos) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactSetNode copyAndSetNode(AtomicReference<Thread> mutator, final int bitpos,
						CompactSetNode node) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactSetNode copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactSetNode node) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactSetNode copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactSetNode node) {
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
	private static abstract class AbstractSetIterator {

		// TODO: verify maximum deepness
		private static final int MAX_DEPTH = 8;

		protected int currentValueCursor;
		protected int currentValueLength;
		protected AbstractSetNode currentValueNode;

		private int currentStackLevel;
		private final int[] nodeCursorsAndLengths = new int[MAX_DEPTH * 2];

		@SuppressWarnings("unchecked")
		AbstractSetNode[] nodes = new AbstractSetNode[MAX_DEPTH];

		AbstractSetIterator(AbstractSetNode rootNode) {
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
						final AbstractSetNode nextNode = nodes[currentStackLevel]
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

	private static final class SetKeyIterator extends AbstractSetIterator implements
					SupplierIterator<java.lang.Integer, java.lang.Integer> {

		SetKeyIterator(AbstractSetNode rootNode) {
			super(rootNode);
		}

		@Override
		public java.lang.Integer next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			} else {
				return currentValueNode.getKey(currentValueCursor++);
			}
		}

		@Override
		public java.lang.Integer get() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Iterator that first iterates over inlined-values and then continues depth
	 * first recursively.
	 */
	private static class TrieSet_5Bits_IntKeyNodeIterator implements Iterator<AbstractSetNode> {

		final Deque<Iterator<? extends AbstractSetNode>> nodeIteratorStack;

		TrieSet_5Bits_IntKeyNodeIterator(AbstractSetNode rootNode) {
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
		public AbstractSetNode next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			AbstractSetNode innerNode = nodeIteratorStack.peek().next();

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

	static final class TransientTrieSet_5Bits_IntKey extends AbstractSet<java.lang.Integer>
					implements TransientSet<java.lang.Integer> {
		final private AtomicReference<Thread> mutator;
		private AbstractSetNode rootNode;
		private int hashCode;
		private int cachedSize;

		TransientTrieSet_5Bits_IntKey(TrieSet_5Bits_IntKey trieSet_5Bits_IntKey) {
			this.mutator = new AtomicReference<Thread>(Thread.currentThread());
			this.rootNode = trieSet_5Bits_IntKey.rootNode;
			this.hashCode = trieSet_5Bits_IntKey.hashCode;
			this.cachedSize = trieSet_5Bits_IntKey.cachedSize;
			if (DEBUG) {
				assert checkHashCodeAndSize(hashCode, cachedSize);
			}
		}

		private boolean checkHashCodeAndSize(final int targetHash, final int targetSize) {
			int hash = 0;
			int size = 0;

			for (Iterator<java.lang.Integer> it = keyIterator(); it.hasNext();) {
				final int key = it.next();

				hash += (int) key;
				size += 1;
			}

			return hash == targetHash && size == targetSize;
		}

		@Override
		public boolean contains(Object o) {
			try {
				@SuppressWarnings("unchecked")
				final int key = (int) o;
				return rootNode.containsKey(key, (int) key, 0);
			} catch (ClassCastException unused) {
				return false;
			}
		}

		@Override
		public boolean containsEquivalent(Object o, Comparator<Object> cmp) {
			try {
				@SuppressWarnings("unchecked")
				final int key = (int) o;
				return rootNode.containsKey(key, (int) key, 0, cmp);
			} catch (ClassCastException unused) {
				return false;
			}
		}

		@Override
		public java.lang.Integer get(Object o) {
			try {
				@SuppressWarnings("unchecked")
				final int key = (int) o;
				final Optional<java.lang.Integer> result = rootNode.findByKey(key, (int) key, 0);

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
		public java.lang.Integer getEquivalent(Object o, Comparator<Object> cmp) {
			try {
				@SuppressWarnings("unchecked")
				final int key = (int) o;
				final Optional<java.lang.Integer> result = rootNode.findByKey(key, (int) key, 0,
								cmp);

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
		public boolean __insert(final java.lang.Integer key) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final Result details = Result.unchanged();

			final CompactSetNode newRootNode = rootNode.updated(mutator, key, keyHash, 0, details);

			if (details.isModified()) {
				rootNode = newRootNode;

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
		public boolean __insertEquivalent(final java.lang.Integer key, final Comparator<Object> cmp) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final Result details = Result.unchanged();

			final CompactSetNode newRootNode = rootNode.updated(mutator, key, keyHash, 0, details,
							cmp);

			if (details.isModified()) {
				rootNode = newRootNode;

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
		public boolean __insertAll(final ImmutableSet<? extends java.lang.Integer> set) {
			boolean modified = false;

			for (final int key : set) {
				modified |= __insert(key);
			}

			return modified;
		}

		@Override
		public boolean __insertAllEquivalent(final ImmutableSet<? extends java.lang.Integer> set,
						final Comparator<Object> cmp) {
			boolean modified = false;

			for (final int key : set) {
				modified |= __insertEquivalent(key, cmp);
			}

			return modified;
		}

		@Override
		public boolean __removeAll(final ImmutableSet<? extends java.lang.Integer> set) {
			boolean modified = false;

			for (final int key : set) {
				modified |= __remove(key);
			}

			return modified;
		}

		@Override
		public boolean __removeAllEquivalent(final ImmutableSet<? extends java.lang.Integer> set,
						final Comparator<Object> cmp) {
			boolean modified = false;

			for (final int key : set) {
				modified |= __removeEquivalent(key, cmp);
			}

			return modified;
		}

		@Override
		public boolean __remove(final java.lang.Integer key) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");

			}

			final int keyHash = key.hashCode();
			final Result details = Result.unchanged();

			final CompactSetNode newRootNode = rootNode.removed(mutator, key, keyHash, 0, details);

			if (details.isModified()) {

				rootNode = newRootNode;
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
		public boolean __removeEquivalent(final java.lang.Integer key, Comparator<Object> cmp) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final Result details = Result.unchanged();

			final CompactSetNode newRootNode = rootNode.removed(mutator, key, keyHash, 0, details,
							cmp);

			if (details.isModified()) {

				rootNode = newRootNode;
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
		public boolean __retainAll(ImmutableSet<? extends java.lang.Integer> set) {
			boolean modified = false;

			Iterator<java.lang.Integer> thisIterator = iterator();
			while (thisIterator.hasNext()) {
				if (!set.contains(thisIterator.next())) {
					thisIterator.remove();
					modified = true;
				}
			}

			return modified;
		}

		@Override
		public boolean __retainAllEquivalent(ImmutableSet<? extends java.lang.Integer> set,
						Comparator<Object> cmp) {
			boolean modified = false;

			Iterator<java.lang.Integer> thisIterator = iterator();
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
		public Iterator<java.lang.Integer> iterator() {
			return keyIterator();
		}

		@Override
		public SupplierIterator<java.lang.Integer, java.lang.Integer> keyIterator() {
			return new TransientSetKeyIterator(this);
		}

		/**
		 * Iterator that first iterates over inlined-values and then continues
		 * depth first recursively.
		 */
		private static class TransientSetKeyIterator extends AbstractSetIterator implements
						SupplierIterator<java.lang.Integer, java.lang.Integer> {

			final TransientTrieSet_5Bits_IntKey transientTrieSet_5Bits_IntKey;
			java.lang.Integer lastKey;

			TransientSetKeyIterator(TransientTrieSet_5Bits_IntKey transientTrieSet_5Bits_IntKey) {
				super(transientTrieSet_5Bits_IntKey.rootNode);
				this.transientTrieSet_5Bits_IntKey = transientTrieSet_5Bits_IntKey;
			}

			@Override
			public java.lang.Integer next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				} else {
					lastKey = currentValueNode.getKey(currentValueCursor++);
					return lastKey;
				}
			}

			@Override
			public java.lang.Integer get() {
				throw new UnsupportedOperationException();
			}

			/*
			 * TODO: test removal with iteration rigorously
			 */
			@Override
			public void remove() {
				boolean success = transientTrieSet_5Bits_IntKey.__remove(lastKey);

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

			if (other instanceof TransientTrieSet_5Bits_IntKey) {
				TransientTrieSet_5Bits_IntKey that = (TransientTrieSet_5Bits_IntKey) other;

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
		public ImmutableSet<java.lang.Integer> freeze() {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			mutator.set(null);
			return new TrieSet_5Bits_IntKey(rootNode, hashCode, cachedSize);
		}
	}

}