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

import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.oracle.truffle.api.object.Property;

public class IndexedImmutablePropertySet implements
		IndexedImmutableSet<Property> {

	private static final IndexedImmutablePropertySet EMPTY_SET = new IndexedImmutablePropertySet(
			CompactSetNode.EMPTY_NODE, 0, 0);

	private static final boolean DEBUG = false;

	private final AbstractSetNode rootNode;
	private final int hashCode;
	private final int cachedSize;

	IndexedImmutablePropertySet(AbstractSetNode rootNode, int hashCode,
			int cachedSize) {
		this.rootNode = rootNode;
		this.hashCode = hashCode;
		this.cachedSize = cachedSize;
		if (DEBUG) {
			assert checkHashCodeAndSize(hashCode, cachedSize);
		}
	}

	public static final IndexedImmutableSet<Property> of() {
		return IndexedImmutablePropertySet.EMPTY_SET;
	}

	public static final IndexedImmutableSet<Property> of(Property... keys) {
		IndexedImmutableSet<Property> result = IndexedImmutablePropertySet.EMPTY_SET;

		for (final Property key : keys) {
			result = result.__insert(key);
		}

		return result;
	}

	private boolean checkHashCodeAndSize(final int targetHash,
			final int targetSize) {
		int hash = 0;
		int size = 0;

		for (Iterator<Property> it = keyIterator(); it.hasNext();) {
			final Property key = it.next();

			hash += key.hashCode();
			size += 1;
		}

		return hash == targetHash && size == targetSize;
	}

	public static final Property extractKey(final Property key) {
		return key;
	}

	public static final int transformHashCode(final int hash) {
		return hash;
	}

	public boolean contains(final Object o) {
		try {
			final Property key = (Property) o;
			return rootNode.contains(key, transformHashCode(key.hashCode()), 0);
		} catch (ClassCastException unused) {
			return false;
		}
	}

	public Property get(final Object o) {
		try {
			final Property key = (Property) o;
			final Optional<Property> result = rootNode.findByKey(key,
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

	public IndexedImmutableSet<Property> __insert(final Property key) {
		final int keyHash = key.hashCode();
		final SetResult details = SetResult.unchanged();

		final CompactSetNode newRootNode = rootNode.updated(null, key,
				transformHashCode(keyHash), 0, details);

		if (details.isModified()) {
			return new IndexedImmutablePropertySet(newRootNode, hashCode
					+ keyHash, cachedSize + 1);
		}

		return this;
	}

	public IndexedImmutableSet<Property> __remove(final Property key) {
		final int keyHash = key.hashCode();
		final SetResult details = SetResult.unchanged();

		final CompactSetNode newRootNode = rootNode.removed(null, key,
				transformHashCode(keyHash), 0, details);

		if (details.isModified()) {
			return new IndexedImmutablePropertySet(newRootNode, hashCode
					- keyHash, cachedSize - 1);
		}

		return this;
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public boolean add(final Property key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends Property> c) {
		throw new UnsupportedOperationException();
	}

	public boolean remove(final Object key) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll(final Collection<?> c) {
		throw new UnsupportedOperationException();
	}

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

	public int size() {
		return cachedSize;
	}

	public boolean isEmpty() {
		return cachedSize == 0;
	}

	public Iterator<Property> iterator() {
		return keyIterator();
	}

	public Iterator<Property> keyIterator() {
		return new ElementIterator(rootNode);
	}

	@Override
	public Object[] toArray() {
		Object[] array = new Object[cachedSize];

		int idx = 0;
		for (Property key : this) {
			array[idx++] = key;
		}

		return array;
	}

	@Override
	public <T> T[] toArray(final T[] a) {
		List<Property> list = new ArrayList<Property>(cachedSize);

		for (Property key : this) {
			list.add(key);
		}

		return list.toArray(a);
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (other == null) {
			return false;
		}

		if (other instanceof IndexedImmutablePropertySet) {
			IndexedImmutablePropertySet that = (IndexedImmutablePropertySet) other;

			if (this.cachedSize != that.cachedSize) {
				return false;
			}

			if (this.hashCode != that.hashCode) {
				return false;
			}

			return rootNode.equals(that.rootNode);
		} else if (other instanceof Set) {
			Set that = (Set) other;

			if (this.size() != that.size())
				return false;

			return containsAll(that);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return hashCode;
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
		return new NodeIterator(rootNode);
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
							bldr.append(String
									.format("%d/%d: %s, ",
											j,
											k,
											new DecimalFormat("0.00%")
													.format(arityCombinationsPercentage)));
						}
					}
				}
				final String detailPercentages = bldr.toString();

				// overview
				System.out.println(String.format("%2d: %s\t[cumsum = %s]\t%s",
						i, new DecimalFormat("0.00%").format(arityPercentage),
						new DecimalFormat("0.00%")
								.format(cumsumArityPercentage),
						detailPercentages));
			}
		}
	}

	static final class SetResult {
		private Property replacedValue;
		private boolean isModified;
		private boolean isReplaced;

		// update: inserted/removed single element, element count changed
		public void modified() {
			this.isModified = true;
		}

		public void updated(Property replacedValue) {
			this.replacedValue = replacedValue;
			this.isModified = true;
			this.isReplaced = true;
		}

		// update: neither element, nor element count changed
		public static SetResult unchanged() {
			return new SetResult();
		}

		private SetResult() {
		}

		public boolean isModified() {
			return isModified;
		}

		public boolean hasReplacedValue() {
			return isReplaced;
		}

		public Property getReplacedValue() {
			return replacedValue;
		}
	}

	protected static interface INode<K, V> {
	}

	protected static abstract class AbstractSetNode implements
			INode<Property, java.lang.Void> {

		abstract boolean contains(final Property key, final int keyHash,
				final int shift);

		abstract Optional<Property> findByKey(final Property key,
				final int keyHash, final int shift);

		abstract CompactSetNode updated(final AtomicReference<Thread> mutator,
				final Property key, final int keyHash, final int shift,
				final SetResult details);

		abstract CompactSetNode removed(final AtomicReference<Thread> mutator,
				final Property key, final int keyHash, final int shift,
				final SetResult details);

		static final boolean isAllowedToEdit(AtomicReference<Thread> x,
				AtomicReference<Thread> y) {
			return x != null && y != null && (x == y || x.get() == y.get());
		}

		abstract boolean hasNodes();

		abstract int nodeArity();

		abstract AbstractSetNode getNode(final int index);

		@Deprecated
		Iterator<? extends AbstractSetNode> nodeIterator() {
			return new Iterator<AbstractSetNode>() {

				int nextIndex = 0;
				final int nodeArity = AbstractSetNode.this.nodeArity();

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
					return nextIndex < nodeArity;
				}
			};
		}

		abstract boolean hasPayload();

		abstract int payloadArity();

		abstract Property getKey(final int index);

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
			final Iterator<Property> it = new ElementIterator(this);

			int size = 0;
			while (it.hasNext()) {
				size += 1;
				it.next();
			}

			return size;
		}
	}

	protected static abstract class CompactSetNode extends AbstractSetNode {

		static final int hashCodeLength() {
			return 32;
		}

		static final int bitPartitionSize() {
			return 5;
		}

		static final int bitPartitionMask() {
			return 0b11111;
		}

		static final int mask(final int keyHash, final int shift) {
			return (keyHash >>> shift) & bitPartitionMask();
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

		boolean nodeInvariant() {
			boolean inv1 = (size() - payloadArity() >= 2 * (arity() - payloadArity()));
			boolean inv2 = (this.arity() == 0) ? sizePredicate() == SIZE_EMPTY
					: true;
			boolean inv3 = (this.arity() == 1 && payloadArity() == 1) ? sizePredicate() == SIZE_ONE
					: true;
			boolean inv4 = (this.arity() >= 2) ? sizePredicate() == SIZE_MORE_THAN_ONE
					: true;

			boolean inv5 = (this.nodeArity() >= 0)
					&& (this.payloadArity() >= 0)
					&& ((this.payloadArity() + this.nodeArity()) == this
							.arity());

			return inv1 && inv2 && inv3 && inv4 && inv5;
		}

		@Override
		abstract CompactSetNode getNode(final int index);

		abstract CompactSetNode copyAndInsertValue(
				final AtomicReference<Thread> mutator, final int bitpos,
				final Property key);

		abstract CompactSetNode copyAndRemoveValue(
				final AtomicReference<Thread> mutator, final int bitpos);

		abstract CompactSetNode copyAndSetNode(
				final AtomicReference<Thread> mutator, final int bitpos,
				final CompactSetNode node);

		abstract CompactSetNode copyAndMigrateFromInlineToNode(
				final AtomicReference<Thread> mutator, final int bitpos,
				final CompactSetNode node);

		abstract CompactSetNode copyAndMigrateFromNodeToInline(
				final AtomicReference<Thread> mutator, final int bitpos,
				final CompactSetNode node);

		static final CompactSetNode mergeTwoKeyValPairs(final Property key0,
				final int keyHash0, final Property key1, final int keyHash1,
				final int shift) {
			assert !(key0.equals(key1));

			if (shift >= hashCodeLength()) {
				// throw new
				// IllegalStateException("Hash collision not yet fixed.");
				return new HashCollisionNode(keyHash0,
						(Property[]) new Object[] { key0, key1 });
			}

			final int mask0 = mask(keyHash0, shift);
			final int mask1 = mask(keyHash1, shift);

			if (mask0 != mask1) {
				// both nodes fit on same level
				final int dataMap = (int) (bitpos(mask0) | bitpos(mask1));

				if (mask0 < mask1) {
					return nodeOf((int) (0), dataMap,
							new Object[] { key0, key1 });
				} else {
					return nodeOf((int) (0), dataMap,
							new Object[] { key1, key0 });
				}
			} else {
				final CompactSetNode node = mergeTwoKeyValPairs(key0, keyHash0,
						key1, keyHash1, shift + bitPartitionSize());
				// values fit on next level

				final int nodeMap = bitpos(mask0);
				return nodeOf(nodeMap, (int) (0), new Object[] { node });
			}
		}

		private static final CompactSetNode initializeEMPTY_NODE() {
			return new BitmapIndexedNode((int) (0), (int) (0), new Object[] {});
		}

		protected static final CompactSetNode EMPTY_NODE = initializeEMPTY_NODE();

		static final CompactSetNode nodeOf(final int nodeMap,
				final int dataMap, final Object[] nodes) {
			return new BitmapIndexedNode(nodeMap, dataMap, nodes);
		}

		static final CompactSetNode nodeOf0x0(final int nodeMap,
				final int dataMap) {
			return EMPTY_NODE;
		}

		static final CompactSetNode nodeOf1x0(final int nodeMap,
				final int dataMap, final CompactSetNode node1) {
			return nodeOf(nodeMap, dataMap, new Object[] { node1 });
		}

		static final CompactSetNode nodeOf0x1(final int nodeMap,
				final int dataMap, final Property key1) {
			return nodeOf(nodeMap, dataMap, new Object[] { key1 });
		}

		static final int index(final int bitmap, final int bitpos) {
			return java.lang.Integer.bitCount(bitmap & (bitpos - 1));
		}

		static final int index(final int bitmap, final int mask,
				final int bitpos) {
			return (bitmap == -1) ? mask : index(bitmap, bitpos);
		}

		int dataIndex(final int bitpos) {
			return java.lang.Integer.bitCount(dataMap() & (bitpos - 1));
		}

		int nodeIndex(final int bitpos) {
			return java.lang.Integer.bitCount(nodeMap() & (bitpos - 1));
		}

		CompactSetNode nodeAt(final int bitpos) {
			return getNode(nodeIndex(bitpos));
		}

		boolean contains(final Property key, final int keyHash, final int shift) {
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
				return getNode(index).contains(key, keyHash,
						shift + bitPartitionSize());
			}

			return false;
		}

		Optional<Property> findByKey(final Property key, final int keyHash,
				final int shift) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int index = dataIndex(bitpos);
				if (getKey(index).equals(key)) {
					return Optional.of(getKey(index));
				}

				return Optional.empty();
			}

			if ((nodeMap() & bitpos) != 0) { // node (not value)
				final AbstractSetNode subNode = nodeAt(bitpos);

				return subNode.findByKey(key, keyHash, shift
						+ bitPartitionSize());
			}

			return Optional.empty();
		}

		CompactSetNode updated(final AtomicReference<Thread> mutator,
				final Property key, final int keyHash, final int shift,
				final SetResult details) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int dataIndex = dataIndex(bitpos);
				final Property currentKey = getKey(dataIndex);

				if (currentKey.equals(key)) {
					return this;
				} else {
					final CompactSetNode subNodeNew = mergeTwoKeyValPairs(
							currentKey,
							transformHashCode(currentKey.hashCode()), key,
							keyHash, shift + bitPartitionSize());

					details.modified();
					return copyAndMigrateFromInlineToNode(mutator, bitpos,
							subNodeNew);
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactSetNode subNode = nodeAt(bitpos);
				final CompactSetNode subNodeNew = subNode.updated(mutator, key,
						keyHash, shift + bitPartitionSize(), details);

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

		CompactSetNode removed(final AtomicReference<Thread> mutator,
				final Property key, final int keyHash, final int shift,
				final SetResult details) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int dataIndex = dataIndex(bitpos);

				if (getKey(dataIndex).equals(key)) {
					details.modified();

					if (this.payloadArity() == 2 && this.nodeArity() == 0) {
						/*
						 * Create new node with remaining pair. The new node
						 * will a) either become the new root returned, or b)
						 * unwrapped and inlined during returning.
						 */
						final int newDataMap = (shift == 0) ? (int) (dataMap() ^ bitpos)
								: bitpos(mask(keyHash, 0));

						return nodeOf0x1(0, newDataMap, getKey(1 - dataIndex));
					} else {
						return copyAndRemoveValue(mutator, bitpos);
					}
				} else {
					return this;
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactSetNode subNode = nodeAt(bitpos);
				final CompactSetNode subNodeNew = subNode.removed(mutator, key,
						keyHash, shift + bitPartitionSize(), details);

				if (!details.isModified()) {
					return this;
				}

				switch (subNodeNew.sizePredicate()) {
				case 0: {
					throw new IllegalStateException(
							"Sub-node must have at least one element.");
				}
				case 1: {
					if (this.payloadArity() == 0 && this.nodeArity() == 1) {
						// escalate (singleton or empty) result
						return subNodeNew;
					} else {
						// inline value (move to front)
						return copyAndMigrateFromNodeToInline(mutator, bitpos,
								subNodeNew);
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
				bldr.append(String.format("@%d<#%d>", pos,
						Objects.hashCode(getKey(i))));

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

	protected static abstract class CompactMixedSetNode extends CompactSetNode {

		private final int nodeMap;
		private final int dataMap;

		CompactMixedSetNode(final AtomicReference<Thread> mutator,
				final int nodeMap, final int dataMap) {
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

	private static final class BitmapIndexedNode extends CompactMixedSetNode {

		final Object[] nodes;

		private BitmapIndexedNode(final int nodeMap, final int dataMap,
				final Object[] nodes) {
			super(null, nodeMap, dataMap);

			this.nodes = nodes;

			if (DEBUG) {

				assert (java.lang.Integer.bitCount(dataMap)
						+ java.lang.Integer.bitCount(nodeMap) == nodes.length);

				for (int i = 0; i < payloadArity(); i++) {
					assert ((nodes[i] instanceof CompactSetNode) == false);
				}
				for (int i = payloadArity(); i < nodes.length; i++) {
					assert ((nodes[i] instanceof CompactSetNode) == true);
				}
			}

			assert nodeInvariant();
		}

		@Override
		Property getKey(final int index) {
			return (Property) nodes[index];
		}

		@Override
		CompactSetNode getNode(final int index) {
			return (CompactSetNode) nodes[nodes.length - 1 - index];
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<? extends AbstractSetNode> nodeIterator() {
			final int length = nodeArity();
			final int offset = nodes.length - length;

			if (DEBUG) {
				for (int i = offset; i < offset + length; i++) {
					assert ((nodes[i] instanceof AbstractSetNode) == true);
				}
			}

			return (Iterator) ArrayIterator.of(nodes, offset, length);
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
			BitmapIndexedNode that = (BitmapIndexedNode) other;
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
			if (this.nodeArity() == 0) {
				switch (this.payloadArity()) {
				case 0:
					return SIZE_EMPTY;
				case 1:
					return SIZE_ONE;
				default:
					return SIZE_MORE_THAN_ONE;
				}
			} else {
				return SIZE_MORE_THAN_ONE;
			}
		}

		@Override
		CompactSetNode copyAndSetNode(final AtomicReference<Thread> mutator,
				final int bitpos, final CompactSetNode node) {

			final int idx = this.nodes.length - 1 - nodeIndex(bitpos);

			final Object[] src = this.nodes;
			final Object[] dst = (Object[]) new Object[src.length];

			// copy 'src' and set 1 element(s) at position 'idx'
			System.arraycopy(src, 0, dst, 0, src.length);
			dst[idx + 0] = node;

			return nodeOf(nodeMap(), dataMap(), dst);

		}

		@Override
		CompactSetNode copyAndInsertValue(
				final AtomicReference<Thread> mutator, final int bitpos,
				final Property key) {
			final int idx = dataIndex(bitpos);

			final Object[] src = this.nodes;
			final Object[] dst = (Object[]) new Object[src.length + 1];

			// copy 'src' and insert 1 element(s) at position 'idx'
			System.arraycopy(src, 0, dst, 0, idx);
			dst[idx + 0] = key;
			System.arraycopy(src, idx, dst, idx + 1, src.length - idx);

			return nodeOf(nodeMap(), (int) (dataMap() | bitpos), dst);
		}

		@Override
		CompactSetNode copyAndRemoveValue(
				final AtomicReference<Thread> mutator, final int bitpos) {
			final int idx = dataIndex(bitpos);

			final Object[] src = this.nodes;
			final Object[] dst = (Object[]) new Object[src.length - 1];

			// copy 'src' and remove 1 element(s) at position 'idx'
			System.arraycopy(src, 0, dst, 0, idx);
			System.arraycopy(src, idx + 1, dst, idx, src.length - idx - 1);

			return nodeOf(nodeMap(), (int) (dataMap() ^ bitpos), dst);
		}

		@Override
		CompactSetNode copyAndMigrateFromInlineToNode(
				final AtomicReference<Thread> mutator, final int bitpos,
				final CompactSetNode node) {

			final int idxOld = dataIndex(bitpos);
			final int idxNew = this.nodes.length - 1 - nodeIndex(bitpos);

			final Object[] src = this.nodes;
			final Object[] dst = new Object[src.length - 1 + 1];

			// copy 'src' and remove 1 element(s) at position 'idxOld' and
			// insert 1 element(s) at position 'idxNew' (TODO: carefully test)
			assert idxOld <= idxNew;
			System.arraycopy(src, 0, dst, 0, idxOld);
			System.arraycopy(src, idxOld + 1, dst, idxOld, idxNew - idxOld);
			dst[idxNew + 0] = node;
			System.arraycopy(src, idxNew + 1, dst, idxNew + 1, src.length
					- idxNew - 1);

			return nodeOf((int) (nodeMap() | bitpos),
					(int) (dataMap() ^ bitpos), dst);
		}

		@Override
		CompactSetNode copyAndMigrateFromNodeToInline(
				final AtomicReference<Thread> mutator, final int bitpos,
				final CompactSetNode node) {

			final int idxOld = this.nodes.length - 1 - nodeIndex(bitpos);
			final int idxNew = dataIndex(bitpos);

			final Object[] src = this.nodes;
			final Object[] dst = new Object[src.length - 1 + 1];

			// copy 'src' and remove 1 element(s) at position 'idxOld' and
			// insert 1 element(s) at position 'idxNew' (TODO: carefully test)
			assert idxOld >= idxNew;
			System.arraycopy(src, 0, dst, 0, idxNew);
			dst[idxNew + 0] = node.getKey(0);
			System.arraycopy(src, idxNew, dst, idxNew + 1, idxOld - idxNew);
			System.arraycopy(src, idxOld + 1, dst, idxOld + 1, src.length
					- idxOld - 1);

			return nodeOf((int) (nodeMap() ^ bitpos),
					(int) (dataMap() | bitpos), dst);
		}

	}

	private static final class HashCollisionNode extends CompactSetNode {
		private final Property[] keys;

		private final int hash;

		HashCollisionNode(final int hash, final Property[] keys) {
			this.keys = keys;

			this.hash = hash;

			assert payloadArity() >= 2;
		}

		boolean contains(final Property key, final int keyHash, final int shift) {
			if (this.hash == keyHash) {
				for (Property k : keys) {
					if (k.equals(key)) {
						return true;
					}
				}
			}
			return false;
		}

		Optional<Property> findByKey(final Property key, final int keyHash,
				final int shift) {
			for (int i = 0; i < keys.length; i++) {
				final Property _key = keys[i];
				if (key.equals(_key)) {
					return Optional.of(_key);
				}
			}
			return Optional.empty();
		}

		CompactSetNode updated(final AtomicReference<Thread> mutator,
				final Property key, final int keyHash, final int shift,
				final SetResult details) {
			assert this.hash == keyHash;

			for (int idx = 0; idx < keys.length; idx++) {
				if (keys[idx].equals(key)) {
					return this;
				}
			}

			final Property[] keysNew = (Property[]) new Object[this.keys.length + 1];

			// copy 'this.keys' and insert 1 element(s) at position
			// 'keys.length'
			System.arraycopy(this.keys, 0, keysNew, 0, keys.length);
			keysNew[keys.length + 0] = key;
			System.arraycopy(this.keys, keys.length, keysNew, keys.length + 1,
					this.keys.length - keys.length);

			details.modified();
			return new HashCollisionNode(keyHash, keysNew);
		}

		CompactSetNode removed(final AtomicReference<Thread> mutator,
				final Property key, final int keyHash, final int shift,
				final SetResult details) {
			for (int idx = 0; idx < keys.length; idx++) {
				if (keys[idx].equals(key)) {
					details.modified();

					if (this.arity() == 1) {
						return EMPTY_NODE;
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node
						 * will be a) either be the new root returned, or b)
						 * unwrapped and inlined.
						 */
						final Property theOtherKey = (idx == 0) ? keys[1]
								: keys[0];

						return EMPTY_NODE.updated(mutator, theOtherKey,
								keyHash, 0, details);
					} else {
						final Property[] keysNew = (Property[]) new Object[this.keys.length - 1];

						// copy 'this.keys' and remove 1 element(s) at position
						// 'idx'
						System.arraycopy(this.keys, 0, keysNew, 0, idx);
						System.arraycopy(this.keys, idx + 1, keysNew, idx,
								this.keys.length - idx - 1);

						return new HashCollisionNode(keyHash, keysNew);
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
		Property getKey(final int index) {
			return keys[index];
		}

		@Override
		public CompactSetNode getNode(int index) {
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

			HashCollisionNode that = (HashCollisionNode) other;

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

				for (int j = 0; j < keys.length; j++) {
					final Property key = keys[j];

					if (key.equals(otherKey)) {
						continue outerLoop;
					}
				}
				return false;

			}

			return true;
		}

		@Override
		CompactSetNode copyAndInsertValue(
				final AtomicReference<Thread> mutator, final int bitpos,
				final Property key) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactSetNode copyAndRemoveValue(
				final AtomicReference<Thread> mutator, final int bitpos) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactSetNode copyAndSetNode(final AtomicReference<Thread> mutator,
				final int bitpos, final CompactSetNode node) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactSetNode copyAndMigrateFromInlineToNode(
				final AtomicReference<Thread> mutator, final int bitpos,
				final CompactSetNode node) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactSetNode copyAndMigrateFromNodeToInline(
				final AtomicReference<Thread> mutator, final int bitpos,
				final CompactSetNode node) {
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
	private static abstract class AbstractIterator {

		private static final int MAX_DEPTH = 7;

		protected int currentValueCursor;
		protected int currentValueLength;
		protected AbstractSetNode currentValueNode;

		private int currentStackLevel = -1;
		private final int[] nodeCursorsAndLengths = new int[MAX_DEPTH * 2];

		AbstractSetNode[] nodes = new AbstractSetNode[MAX_DEPTH];

		AbstractIterator(AbstractSetNode rootNode) {
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
					final AbstractSetNode nextNode = nodes[currentStackLevel]
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
						nodeCursorsAndLengths[nextLengthIndex] = nextNode
								.nodeArity();
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

	protected static class ElementIterator extends AbstractIterator implements
			Iterator<Property> {

		ElementIterator(AbstractSetNode rootNode) {
			super(rootNode);
		}

		@Override
		public Property next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			} else {
				return currentValueNode.getKey(currentValueCursor++);
			}
		}

	}

	/**
	 * Iterator that first iterates over inlined-values and then continues depth
	 * first recursively.
	 */
	private static class NodeIterator implements Iterator<AbstractSetNode> {

		final Deque<Iterator<? extends AbstractSetNode>> nodeIteratorStack;

		NodeIterator(AbstractSetNode rootNode) {
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

}