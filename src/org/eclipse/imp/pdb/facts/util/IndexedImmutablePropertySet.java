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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.oracle.truffle.api.object.Property;

public class IndexedImmutablePropertySet implements
		IndexedImmutableSet<Property> {

	private static final IndexedImmutablePropertySet EMPTY_SET = new IndexedImmutablePropertySet(
			CompactNode.EMPTY_NODE, 0, 0);

	private static final boolean DEBUG = false;

	private final AbstractNode rootNode;
	private final int hashCode;
	private final int cachedSize;

	IndexedImmutablePropertySet(AbstractNode rootNode, int hashCode,
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

		for (Iterator<Property> it = new ElementIterator(rootNode); it
				.hasNext();) {
			final Property key = it.next();

			hash += key.hashCode();
			size += 1;
		}

		return hash == targetHash && size == targetSize;
	}

	private static final Object extractKey(final Property element) {
		return element.getKey();
	}

	public static final int transformHashCode(final int hash) {
		return hash;
	}

	@Override
	public boolean contains(final Object o) {
		try {
			final Property key = (Property) o;
			return rootNode.contains(key, transformHashCode(key.hashCode()), 0);
		} catch (ClassCastException unused) {
			return false;
		}
	}

	@Override
	public Property get(final Object o) {
		try {
			final Property key = (Property) o;
			final Optional<Property> result = rootNode.find(key,
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

	@Override
	public IndexedImmutableSet<Property> __insert(final Property key) {
		final int keyHash = key.hashCode();
		final ModificationResult details = ModificationResult.unchanged();

		final CompactNode newRootNode = rootNode.updated(key,
				transformHashCode(keyHash), 0, details);

		if (details.isModified()) {
			return new IndexedImmutablePropertySet(newRootNode, hashCode
					+ keyHash, cachedSize + 1);
		}

		return this;
	}

	@Override
	public IndexedImmutableSet<Property> __remove(final Property key) {
		final int keyHash = key.hashCode();
		final ModificationResult details = ModificationResult.unchanged();

		final CompactNode newRootNode = rootNode.removed(key,
				transformHashCode(keyHash), 0, details);

		if (details.isModified()) {
			return new IndexedImmutablePropertySet(newRootNode, hashCode
					- keyHash, cachedSize - 1);
		}

		return this;
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean add(final Property key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends Property> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(final Object key) {
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
	public int size() {
		return cachedSize;
	}

	@Override
	public boolean isEmpty() {
		return cachedSize == 0;
	}

	@Override
	public Iterator<Property> iterator() {
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
	public int hashCode() {
		return hashCode;
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

	static final class ModificationResult {
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
		public static ModificationResult unchanged() {
			return new ModificationResult();
		}

		private ModificationResult() {
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

	protected static abstract class AbstractNode {

		abstract boolean contains(final Property key, final int keyHash,
				final int shift);

		abstract Optional<Property> find(final Property key, final int keyHash,
				final int shift);

		abstract CompactNode updated(final Property key, final int keyHash,
				final int shift, final ModificationResult details);

		abstract CompactNode removed(final Property key, final int keyHash,
				final int shift, final ModificationResult details);

		abstract boolean hasNodes();

		abstract int nodeArity();

		abstract AbstractNode getNode(final int index);

		abstract boolean hasElements();

		abstract int elementArity();

		abstract Property getElement(final int index);

	}

	protected static abstract class CompactNode extends AbstractNode {

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
			return 1 << mask;
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

		@Deprecated
		// Only used in nodeInvariant()
		int arity() {
			return elementArity() + nodeArity();
		}

		@Deprecated
		// Only used in nodeInvariant()
		int size() {
			final Iterator<Property> it = new ElementIterator(this);

			int size = 0;
			while (it.hasNext()) {
				size += 1;
				it.next();
			}

			return size;
		}

		boolean nodeInvariant() {
			boolean inv1 = (size() - elementArity() >= 2 * (arity() - elementArity()));
			boolean inv2 = (this.arity() == 0) ? sizePredicate() == SIZE_EMPTY
					: true;
			boolean inv3 = (this.arity() == 1 && elementArity() == 1) ? sizePredicate() == SIZE_ONE
					: true;
			boolean inv4 = (this.arity() >= 2) ? sizePredicate() == SIZE_MORE_THAN_ONE
					: true;

			boolean inv5 = (this.nodeArity() >= 0)
					&& (this.elementArity() >= 0)
					&& ((this.elementArity() + this.nodeArity()) == this
							.arity());

			return inv1 && inv2 && inv3 && inv4 && inv5;
		}

		@Override
		abstract CompactNode getNode(final int index);

		abstract CompactNode copyAndInsertValue(final int bitpos,
				final Property key);

		abstract CompactNode copyAndRemoveValue(final int bitpos);

		abstract CompactNode copyAndSetNode(final int bitpos,
				final CompactNode node);

		abstract CompactNode copyAndMigrateFromInlineToNode(final int bitpos,
				final CompactNode node);

		abstract CompactNode copyAndMigrateFromNodeToInline(final int bitpos,
				final CompactNode node);

		static final CompactNode mergeTwoElements(final Property key0,
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
				final int dataMap = bitpos(mask0) | bitpos(mask1);

				if (mask0 < mask1) {
					return new BitmapIndexedNode(0, dataMap, new Object[] {
							key0, key1 });
				} else {
					return new BitmapIndexedNode(0, dataMap, new Object[] {
							key1, key0 });
				}
			} else {
				final CompactNode node = mergeTwoElements(key0, keyHash0, key1,
						keyHash1, shift + bitPartitionSize());
				// values fit on next level

				final int nodeMap = bitpos(mask0);
				return new BitmapIndexedNode(nodeMap, 0, new Object[] { node });
			}
		}

		protected static final CompactNode EMPTY_NODE = new BitmapIndexedNode(
				0, 0, new Object[] {});

		static final int index(final int bitmap, final int bitpos) {
			return java.lang.Integer.bitCount(bitmap & (bitpos - 1));
		}

		static final int index(final int bitmap, final int mask,
				final int bitpos) {
			return (bitmap == -1) ? mask : index(bitmap, bitpos);
		}

		@Override
		boolean contains(final Property key, final int keyHash, final int shift) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			final int dataMap = dataMap();
			if ((dataMap & bitpos) != 0) {
				final int index = index(dataMap, mask, bitpos);
				return getElement(index).equals(key);
			}

			final int nodeMap = nodeMap();
			if ((nodeMap & bitpos) != 0) {
				final int index = index(nodeMap, mask, bitpos);
				return getNode(index).contains(key, keyHash,
						shift + bitPartitionSize());
			}

			return false;
		}

		@Override
		Optional<Property> find(final Property key, final int keyHash,
				final int shift) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			final int dataMap = dataMap();
			if ((dataMap & bitpos) != 0) {
				final int index = index(dataMap, mask, bitpos);
				if (getElement(index).equals(key)) {
					return Optional.of(getElement(index));
				}

				return Optional.empty();
			}

			final int nodeMap = nodeMap();
			if ((nodeMap & bitpos) != 0) {
				final int index = index(nodeMap, mask, bitpos);
				return getNode(index).find(key, keyHash,
						shift + bitPartitionSize());
			}

			return Optional.empty();
		}

		@Override
		CompactNode updated(final Property key, final int keyHash,
				final int shift, final ModificationResult details) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int dataIndex = index(dataMap(), bitpos);
				final Property currentKey = getElement(dataIndex);

				if (currentKey.equals(key)) {
					return this;
				} else {
					final CompactNode subNodeNew = mergeTwoElements(currentKey,
							transformHashCode(currentKey.hashCode()), key,
							keyHash, shift + bitPartitionSize());

					details.modified();
					return copyAndMigrateFromInlineToNode(bitpos, subNodeNew);
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final int nodeIndex = index(nodeMap(), bitpos);

				final CompactNode subNode = getNode(nodeIndex);
				final CompactNode subNodeNew = subNode.updated(key, keyHash,
						shift + bitPartitionSize(), details);

				if (details.isModified()) {
					return copyAndSetNode(bitpos, subNodeNew);
				} else {
					return this;
				}
			} else {
				// no value
				details.modified();
				return copyAndInsertValue(bitpos, key);
			}
		}

		@Override
		CompactNode removed(final Property key, final int keyHash,
				final int shift, final ModificationResult details) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int dataIndex = index(dataMap(), bitpos);

				if (getElement(dataIndex).equals(key)) {
					details.modified();

					if (this.elementArity() == 2 && this.nodeArity() == 0) {
						/*
						 * Create new node with remaining pair. The new node
						 * will a) either become the new root returned, or b)
						 * unwrapped and inlined during returning.
						 */
						final int newDataMap = (shift == 0) ? (int) (dataMap() ^ bitpos)
								: bitpos(mask(keyHash, 0));

						return new BitmapIndexedNode(0, newDataMap,
								new Object[] { getElement(1 - dataIndex) });
					} else {
						return copyAndRemoveValue(bitpos);
					}
				} else {
					return this;
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final int nodeIndex = index(nodeMap(), bitpos);

				final CompactNode subNode = getNode(nodeIndex);
				final CompactNode subNodeNew = subNode.removed(key, keyHash,
						shift + bitPartitionSize(), details);

				if (!details.isModified()) {
					return this;
				}

				switch (subNodeNew.sizePredicate()) {
				case 0: {
					throw new IllegalStateException(
							"Sub-node must have at least one element.");
				}
				case 1: {
					if (this.elementArity() == 0 && this.nodeArity() == 1) {
						// escalate (singleton or empty) result
						return subNodeNew;
					} else {
						// inline value (move to front)
						return copyAndMigrateFromNodeToInline(bitpos,
								subNodeNew);
					}
				}
				default: {
					// modify current node (set replacement node)
					return copyAndSetNode(bitpos, subNodeNew);
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

				map = map >> 1;
				mask += 1;
			}

			assert cnt1 != i_th;
			throw new RuntimeException("Called with invalid arguments.");
		}

		@Override
		public String toString() {
			final StringBuilder bldr = new StringBuilder();
			bldr.append('[');

			for (byte i = 0; i < elementArity(); i++) {
				final byte pos = recoverMask(dataMap(), (byte) (i + 1));
				bldr.append(String.format("@%d<#%d>", pos,
						Objects.hashCode(getElement(i))));

				if (!((i + 1) == elementArity())) {
					bldr.append(", ");
				}
			}

			if (elementArity() > 0 && nodeArity() > 0) {
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

	private static final class BitmapIndexedNode extends CompactNode {

		private final int nodeMap;
		private final int dataMap;
		final Object[] nodes;

		private BitmapIndexedNode(final int nodeMap, final int dataMap,
				final Object[] nodes) {

			this.nodeMap = nodeMap;
			this.dataMap = dataMap;
			this.nodes = nodes;

			if (DEBUG) {

				assert (java.lang.Integer.bitCount(dataMap)
						+ java.lang.Integer.bitCount(nodeMap) == nodes.length);

				for (int i = 0; i < elementArity(); i++) {
					assert ((nodes[i] instanceof CompactNode) == false);
				}
				for (int i = elementArity(); i < nodes.length; i++) {
					assert ((nodes[i] instanceof CompactNode) == true);
				}
			}

			assert nodeInvariant();
		}

		@Override
		public int nodeMap() {
			return nodeMap;
		}

		@Override
		public int dataMap() {
			return dataMap;
		}

		@Override
		Property getElement(final int index) {
			return (Property) nodes[index];
		}

		@Override
		CompactNode getNode(final int index) {
			return (CompactNode) nodes[nodes.length - 1 - index];
		}

		@Override
		boolean hasElements() {
			return dataMap() != 0;
		}

		@Override
		int elementArity() {
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
		public int hashCode() {
			final int prime = 31;
			int result = 0;
			result = prime * result + (dataMap());
			result = prime * result + (dataMap());
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
				switch (this.elementArity()) {
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
		CompactNode copyAndSetNode(final int bitpos, final CompactNode node) {

			final int idx = this.nodes.length - 1 - index(nodeMap, bitpos);

			final Object[] src = this.nodes;
			final Object[] dst = new Object[src.length];

			// copy 'src' and set 1 element(s) at position 'idx'
			System.arraycopy(src, 0, dst, 0, src.length);
			dst[idx] = node;

			return new BitmapIndexedNode(nodeMap(), dataMap(), dst);
		}

		@Override
		CompactNode copyAndInsertValue(final int bitpos, final Property key) {
			final int idx = index(dataMap, bitpos);

			final Object[] src = this.nodes;
			final Object[] dst = new Object[src.length + 1];

			// copy 'src' and insert 1 element(s) at position 'idx'
			System.arraycopy(src, 0, dst, 0, idx);
			dst[idx] = key;
			System.arraycopy(src, idx, dst, idx + 1, src.length - idx);

			return new BitmapIndexedNode(nodeMap(), dataMap() | bitpos, dst);
		}

		@Override
		CompactNode copyAndRemoveValue(final int bitpos) {
			final int idx = index(dataMap, bitpos);

			final Object[] src = this.nodes;
			final Object[] dst = new Object[src.length - 1];

			// copy 'src' and remove 1 element(s) at position 'idx'
			System.arraycopy(src, 0, dst, 0, idx);
			System.arraycopy(src, idx + 1, dst, idx, src.length - idx - 1);

			return new BitmapIndexedNode(nodeMap(), dataMap() ^ bitpos, dst);
		}

		@Override
		CompactNode copyAndMigrateFromInlineToNode(final int bitpos,
				final CompactNode node) {

			final int idxOld = index(dataMap, bitpos);
			final int idxNew = this.nodes.length - 1 - index(nodeMap, bitpos);

			final Object[] src = this.nodes;
			final Object[] dst = new Object[src.length - 1 + 1];

			// copy 'src' and remove 1 element(s) at position 'idxOld' and
			// insert 1 element(s) at position 'idxNew' (TODO: carefully test)
			assert idxOld <= idxNew;
			System.arraycopy(src, 0, dst, 0, idxOld);
			System.arraycopy(src, idxOld + 1, dst, idxOld, idxNew - idxOld);
			dst[idxNew] = node;
			System.arraycopy(src, idxNew + 1, dst, idxNew + 1, src.length
					- idxNew - 1);

			return new BitmapIndexedNode(nodeMap() | bitpos,
					dataMap() ^ bitpos, dst);
		}

		@Override
		CompactNode copyAndMigrateFromNodeToInline(final int bitpos,
				final CompactNode node) {

			final int idxOld = this.nodes.length - 1 - index(nodeMap, bitpos);
			final int idxNew = index(dataMap, bitpos);

			final Object[] src = this.nodes;
			final Object[] dst = new Object[src.length - 1 + 1];

			// copy 'src' and remove 1 element(s) at position 'idxOld' and
			// insert 1 element(s) at position 'idxNew' (TODO: carefully test)
			assert idxOld >= idxNew;
			System.arraycopy(src, 0, dst, 0, idxNew);
			dst[idxNew] = node.getElement(0);
			System.arraycopy(src, idxNew, dst, idxNew + 1, idxOld - idxNew);
			System.arraycopy(src, idxOld + 1, dst, idxOld + 1, src.length
					- idxOld - 1);

			return new BitmapIndexedNode(nodeMap() ^ bitpos,
					dataMap() | bitpos, dst);
		}

	}

	private static final class HashCollisionNode extends CompactNode {
		private final Property[] elements;

		private final int hash;

		HashCollisionNode(final int hash, final Property[] keys) {
			this.elements = keys;

			this.hash = hash;

			assert elementArity() >= 2;
		}

		@Override
		boolean contains(final Property key, final int keyHash, final int shift) {
			if (this.hash == keyHash) {
				for (Property k : elements) {
					if (k.equals(key)) {
						return true;
					}
				}
			}
			return false;
		}

		@Override
		Optional<Property> find(final Property key, final int keyHash,
				final int shift) {
			for (int i = 0; i < elements.length; i++) {
				final Property _key = elements[i];
				if (key.equals(_key)) {
					return Optional.of(_key);
				}
			}
			return Optional.empty();
		}

		@Override
		CompactNode updated(final Property key, final int keyHash,
				final int shift, final ModificationResult details) {
			assert this.hash == keyHash;

			for (int idx = 0; idx < elements.length; idx++) {
				if (elements[idx].equals(key)) {
					return this;
				}
			}

			final Property[] keysNew = (Property[]) new Object[this.elements.length + 1];

			// copy 'this.keys' and insert 1 element(s) at position
			// 'keys.length'
			System.arraycopy(this.elements, 0, keysNew, 0, elements.length);
			keysNew[elements.length] = key;
			System.arraycopy(this.elements, elements.length, keysNew,
					elements.length + 1, this.elements.length - elements.length);

			details.modified();
			return new HashCollisionNode(keyHash, keysNew);
		}

		@Override
		CompactNode removed(final Property key, final int keyHash,
				final int shift, final ModificationResult details) {
			for (int idx = 0; idx < elements.length; idx++) {
				if (elements[idx].equals(key)) {
					details.modified();

					if (this.arity() == 1) {
						return EMPTY_NODE;
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node
						 * will be a) either be the new root returned, or b)
						 * unwrapped and inlined.
						 */
						final Property theOtherKey = (idx == 0) ? elements[1]
								: elements[0];

						return EMPTY_NODE.updated(theOtherKey, keyHash, 0,
								details);
					} else {
						final Property[] keysNew = (Property[]) new Object[this.elements.length - 1];

						// copy 'this.keys' and remove 1 element(s) at position
						// 'idx'
						System.arraycopy(this.elements, 0, keysNew, 0, idx);
						System.arraycopy(this.elements, idx + 1, keysNew, idx,
								this.elements.length - idx - 1);

						return new HashCollisionNode(keyHash, keysNew);
					}
				}
			}
			return this;
		}

		@Override
		boolean hasElements() {
			return true;
		}

		@Override
		int elementArity() {
			return elements.length;
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
			return elementArity();
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		Property getElement(final int index) {
			return elements[index];
		}

		@Override
		public CompactNode getNode(int index) {
			throw new IllegalStateException("Is leaf node.");
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 0;
			result = prime * result + hash;
			result = prime * result + Arrays.hashCode(elements);
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
			outerLoop: for (int i = 0; i < that.elementArity(); i++) {
				final Object otherKey = that.getElement(i);

				for (int j = 0; j < elements.length; j++) {
					final Property key = elements[j];

					if (key.equals(otherKey)) {
						continue outerLoop;
					}
				}
				return false;

			}

			return true;
		}

		@Override
		CompactNode copyAndInsertValue(final int bitpos, final Property key) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactNode copyAndRemoveValue(final int bitpos) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactNode copyAndSetNode(final int bitpos, final CompactNode node) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactNode copyAndMigrateFromInlineToNode(final int bitpos,
				final CompactNode node) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactNode copyAndMigrateFromNodeToInline(final int bitpos,
				final CompactNode node) {
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
		protected AbstractNode currentValueNode;

		private int currentStackLevel = -1;
		private final int[] nodeCursorsAndLengths = new int[MAX_DEPTH * 2];

		AbstractNode[] nodes = new AbstractNode[MAX_DEPTH];

		AbstractIterator(AbstractNode rootNode) {
			if (rootNode.hasNodes()) {
				currentStackLevel = 0;

				nodes[0] = rootNode;
				nodeCursorsAndLengths[0] = 0;
				nodeCursorsAndLengths[1] = rootNode.nodeArity();
			}

			if (rootNode.hasElements()) {
				currentValueNode = rootNode;
				currentValueCursor = 0;
				currentValueLength = rootNode.elementArity();
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
					final AbstractNode nextNode = nodes[currentStackLevel]
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

					if (nextNode.hasElements()) {
						/*
						 * found next node that contains values
						 */
						currentValueNode = nextNode;
						currentValueCursor = 0;
						currentValueLength = nextNode.elementArity();
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

		ElementIterator(AbstractNode rootNode) {
			super(rootNode);
		}

		@Override
		public Property next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			} else {
				return currentValueNode.getElement(currentValueCursor++);
			}
		}

	}

}