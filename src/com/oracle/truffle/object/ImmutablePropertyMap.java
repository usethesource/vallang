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
package com.oracle.truffle.object;

import static java.lang.System.arraycopy;
import static com.oracle.truffle.object.ImmutablePropertyMap.Node.*;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.oracle.truffle.api.object.Property;

public final class ImmutablePropertyMap implements ImmutableMap<Object, Property> {

	private static final Node EMPTY_NODE = new BitmapIndexedNode(0, 0, new Object[] {},
			new int[] {});

	private static final ImmutablePropertyMap EMPTY_MAP = new ImmutablePropertyMap(EMPTY_NODE, 0, 0);

	private static final boolean DEBUG = false;

	private final Node rootNode;
	private final int cachedSize;

	/*
	 * This field and subsequent IDs stored in nodes are not part of the object
	 * identity.
	 */
	private final int nextSequenceId;

	private ImmutablePropertyMap(Node rootNode, int cachedSize, int nextSequenceId) {
		this.rootNode = rootNode;
		this.cachedSize = cachedSize;
		this.nextSequenceId = nextSequenceId;

		if (DEBUG) {
			assert checkSize(cachedSize);
		}
	}

	public static final ImmutableMap<Object, Property> of() {
		return EMPTY_MAP;
	}

	private boolean checkSize(final int targetSize) {
		int size = 0;

		for (Iterator<Property> it = new ElementIterator(rootNode); it.hasNext(); size++) {
		}

		return size == targetSize;
	}

	private static final Object extractKey(final Property element) {
		return element.getKey();
	}

	public static final int transformHashCode(final int hash) {
		return hash;
	}

	@Override
	public boolean containsKey(final Object key) {
		try {
			final int keyHash = key.hashCode();

			return rootNode.containsKey(key, transformHashCode(keyHash), 0);
		} catch (ClassCastException unused) {
			return false;
		}
	}

	@Override
	public boolean containsValue(final Object o) {
		try {
			final Property element = (Property) o;
			final int keyHash = extractKey(element).hashCode();

			return rootNode.containsValue(element, transformHashCode(keyHash), 0);
		} catch (ClassCastException unused) {
			return false;
		}
	}

	@Override
	public Property get(final Object key) {
		try {
			final int keyHash = key.hashCode();

			final Optional<Property> result = rootNode.find(key, transformHashCode(keyHash), 0);

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
	public ImmutableMap<Object, Property> copyAndPut(final Object key, final Property element) {
		if (key != extractKey(element)) {
			throw new IllegalArgumentException("Key must reference equal extracted key of tuple.");
		}

		final int keyHash = extractKey(element).hashCode();
		final UpdateReport report = new UpdateReport();

		final Node newRootNode = rootNode.updated(key, element,
				transformHashCode(keyHash), nextSequenceId, 0, report);

		if (report.isTrieModified()) {
			return new ImmutablePropertyMap(newRootNode, cachedSize + 1, nextSequenceId + 1);
		}

		return this;
	}

	@Override
	public ImmutableMap<Object, Property> copyAndRemove(final Object key) {
		final int keyHash = key.hashCode();
		final UpdateReport report = new UpdateReport();

		final Node newRootNode = rootNode.removed(key, transformHashCode(keyHash), 0, report);

		if (report.isTrieModified()) {
			return new ImmutablePropertyMap(newRootNode, cachedSize - 1, nextSequenceId);
		}

		return this;
	}

	@Override
	public int size() {
		return cachedSize;
	}

	@Override
	public boolean isEmpty() {
		return cachedSize == 0;
	}

	public Iterator<Object> keyIterator() {
		return new KeyIterator(rootNode);
	}

	public Iterator<Property> valueIterator() {
		return new ElementIterator(rootNode);
	}

	public Iterator<Map.Entry<Object, Property>> entryIterator() {
		return new EntryIterator(rootNode);
	}

	@Override
	public Set<Object> keySet() {
		Set<Object> keySet = null;

		if (keySet == null) {
			keySet = new AbstractSet<Object>() {
				@Override
				public Iterator<Object> iterator() {
					return ImmutablePropertyMap.this.keyIterator();
				}

				@Override
				public int size() {
					return ImmutablePropertyMap.this.size();
				}

				@Override
				public boolean isEmpty() {
					return ImmutablePropertyMap.this.isEmpty();
				}

				@Override
				public void clear() {
					ImmutablePropertyMap.this.clear();
				}

				@Override
				public boolean contains(Object key) {
					return ImmutablePropertyMap.this.containsKey(key);
				}
			};
		}

		return keySet;
	}

	@Override
	public Collection<Property> values() {
		Collection<Property> values = null;

		if (values == null) {
			values = new AbstractCollection<Property>() {
				@Override
				public Iterator<Property> iterator() {
					return ImmutablePropertyMap.this.valueIterator();
				}

				@Override
				public int size() {
					return ImmutablePropertyMap.this.size();
				}

				@Override
				public boolean isEmpty() {
					return ImmutablePropertyMap.this.isEmpty();
				}

				@Override
				public void clear() {
					ImmutablePropertyMap.this.clear();
				}

				@Override
				public boolean contains(Object value) {
					return ImmutablePropertyMap.this.containsValue(value);
				}
			};
		}

		return values;
	}

	@Override
	public Set<Map.Entry<Object, Property>> entrySet() {
		Set<Map.Entry<Object, Property>> entrySet = null;

		if (entrySet == null) {
			entrySet = new AbstractSet<Map.Entry<Object, Property>>() {
				@Override
				public Iterator<Map.Entry<Object, Property>> iterator() {
					return ImmutablePropertyMap.this.entryIterator();
				}

				@Override
				public int size() {
					return ImmutablePropertyMap.this.size();
				}

				@Override
				public boolean isEmpty() {
					return ImmutablePropertyMap.this.isEmpty();
				}

				@Override
				public void clear() {
					ImmutablePropertyMap.this.clear();
				}

				@Override
				public boolean contains(Object key) {
					return ImmutablePropertyMap.this.containsKey(key);
				}
			};
		}

		return entrySet;
	}

	@Override
	public int hashCode() {
		int hash = 0;

		for (Iterator<Property> it = valueIterator(); it.hasNext();) {
			final Property element = it.next();
			hash += (extractKey(element).hashCode() ^ element.hashCode());
		}

		return hash;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (other == null) {
			return false;
		}
		if (getClass() != other.getClass()) {
			return false;
		}

		ImmutablePropertyMap that = (ImmutablePropertyMap) other;

		if (this.cachedSize != that.cachedSize) {
			return false;
		}

		return rootNode.equals(that.rootNode);
	}

	private static final class UpdateReport {

		private boolean isModified;

		// // update: neither element, nor element count changed
		public UpdateReport() {
			this.isModified = false;
		}

		// update: inserted/removed single element, element count changed
		public void setTrieModified() {
			this.isModified = true;
		}

		public boolean isTrieModified() {
			return isModified;
		}

	}

	static interface Node {

		boolean containsKey(final Object key, final int keyHash, final int shift);

		boolean containsValue(final Property element, final int keyHash, final int shift);

		Optional<Property> find(final Object key, final int keyHash, final int shift);

		Node updated(final Object key, final Property element, final int keyHash, int sequenceId,
				final int shift, final UpdateReport report);

		Node removed(final Object key, final int keyHash, final int shift, final UpdateReport report);

		boolean hasNodes();

		int nodeArity();

		Node getNode(final int index);

		boolean hasElements();

		int elementArity();

		Property getElement(final int index);

		default Object getKey(final int index) {
			return extractKey(getElement(index));
		}

		int getSequenceId(final int index);

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
		byte sizePredicate();

		/*************************/
		/*** UTILITY FUNCTIONS ***/
		/*************************/

		static int hashCodeLength() {
			return 32;
		}

		static int bitPartitionSize() {
			return 5;
		}

		static int bitPartitionMask() {
			return 0b11111;
		}

		static int mask(final int keyHash, final int shift) {
			return (keyHash >>> shift) & bitPartitionMask();
		}

		static int bitpos(final int mask) {
			return 1 << mask;
		}

		static int index(final int bitmap, final int bitpos) {
			return java.lang.Integer.bitCount(bitmap & (bitpos - 1));
		}

		static int index(final int bitmap, final int mask, final int bitpos) {
			return (bitmap == -1) ? mask : index(bitmap, bitpos);
		}

		static Node mergeTwoElements(final Property element0, final int keyHash0, int sequenceId0,
				final Property element1, final int keyHash1, int sequenceId1, final int shift) {
			Object key0 = extractKey(element0);
			Object key1 = extractKey(element1);
			assert !(key0.equals(key1));

			if (shift >= hashCodeLength()) {
				return new HashCollisionNode(keyHash0, element0, sequenceId0, element1, sequenceId1);
			}

			final int mask0 = mask(keyHash0, shift);
			final int mask1 = mask(keyHash1, shift);

			if (mask0 != mask1) {
				// both nodes fit on same level
				final int dataMap = bitpos(mask0) | bitpos(mask1);

				if (mask0 < mask1) {
					return BitmapIndexedNode.newElementTuple(dataMap, element0, sequenceId0,
							element1, sequenceId1);
				} else {
					return BitmapIndexedNode.newElementTuple(dataMap, element1, sequenceId1,
							element0, sequenceId0);
				}
			} else {
				final Node node = mergeTwoElements(element0, keyHash0, sequenceId0, element1,
						keyHash1, sequenceId1, shift + bitPartitionSize());
				// values fit on next level
				final int nodeMap = bitpos(mask0);

				return BitmapIndexedNode.newSubnodeSingleton(nodeMap, node);
			}
		}

	}

	private static final class BitmapIndexedNode implements Node {

		private final int nodeMap;
		private final int dataMap;

		private final Object[] nodes;
		private final int[] indices;

		private BitmapIndexedNode(final int nodeMap, final int dataMap, final Object[] nodes,
				int[] indices) {

			this.nodeMap = nodeMap;
			this.dataMap = dataMap;

			this.nodes = nodes;
			this.indices = indices;

			if (DEBUG) {

				assert (java.lang.Integer.bitCount(dataMap) + java.lang.Integer.bitCount(nodeMap) == nodes.length);

				for (int i = 0; i < elementArity(); i++) {
					assert ((nodes[i] instanceof Node) == false);
				}
				for (int i = elementArity(); i < nodes.length; i++) {
					assert ((nodes[i] instanceof Node) == true);
				}
			}

			assert nodeInvariant();
		}

		static final BitmapIndexedNode newElementSingleton(int dataMap, Property element0,
				int index0) {
			return new BitmapIndexedNode(0, dataMap, new Property[] { element0 },
					new int[] { index0 });
		}

		static final BitmapIndexedNode newElementTuple(int dataMap, Property element0, int index0,
				Property element1, int index1) {
			return new BitmapIndexedNode(0, dataMap, new Property[] { element0, element1 },
					new int[] { index0, index1 });
		}

		static final BitmapIndexedNode newSubnodeSingleton(int nodeMap, Node subNode) {
			return new BitmapIndexedNode(nodeMap, 0, new Object[] { subNode }, new int[] {});
		}

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
			boolean inv2 = (this.arity() == 0) ? sizePredicate() == SIZE_EMPTY : true;
			boolean inv3 = (this.arity() == 1 && elementArity() == 1) ? sizePredicate() == SIZE_ONE
					: true;
			boolean inv4 = (this.arity() >= 2) ? sizePredicate() == SIZE_MORE_THAN_ONE : true;

			boolean inv5 = (this.nodeArity() >= 0) && (this.elementArity() >= 0)
					&& ((this.elementArity() + this.nodeArity()) == this.arity());

			return inv1 && inv2 && inv3 && inv4 && inv5;
		}

		@Override
		public Property getElement(final int index) {
			return (Property) nodes[index];
		}

		@Override
		public int getSequenceId(final int index) {
			return indices[index];
		}

		@Override
		public Node getNode(final int index) {
			return (Node) nodes[nodes.length - 1 - index];
		}

		@Override
		public boolean hasElements() {
			return dataMap != 0;
		}

		@Override
		public int elementArity() {
			return (dataMap == 0) ? 0 : java.lang.Integer.bitCount(dataMap);
		}

		@Override
		public boolean hasNodes() {
			return nodeMap != 0;
		}

		@Override
		public int nodeArity() {
			return (nodeMap == 0) ? 0 : java.lang.Integer.bitCount(nodeMap);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 0;
			result = prime * result + (dataMap);
			result = prime * result + (dataMap);
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
			if (nodeMap != that.nodeMap) {
				return false;
			}
			if (dataMap != that.dataMap) {
				return false;
			}
			if (!Arrays.equals(nodes, that.nodes)) {
				return false;
			}
			return true;
		}

		/**
		 * @return 0 <= mask <= 2^BIT_PARTITION_SIZE - 1
		 */
		private static byte recoverMask(int map, byte i_th) {
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
				final byte pos = recoverMask(dataMap, (byte) (i + 1));
				bldr.append(String.format("@%d<#%d>", pos,
						Objects.hashCode(extractKey(getElement(i)))));

				if (!((i + 1) == elementArity())) {
					bldr.append(", ");
				}
			}

			if (elementArity() > 0 && nodeArity() > 0) {
				bldr.append(", ");
			}

			for (byte i = 0; i < nodeArity(); i++) {
				final byte pos = recoverMask(nodeMap, (byte) (i + 1));
				bldr.append(String.format("@%d: %s", pos, getNode(i)));

				if (!((i + 1) == nodeArity())) {
					bldr.append(", ");
				}
			}

			bldr.append(']');
			return bldr.toString();
		}

		@Override
		public byte sizePredicate() {
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

		Node copyAndSetNode(final int bitpos, final Node node) {
			final int idx = this.nodes.length - 1 - index(nodeMap, bitpos);

			final Object[] newNodes = new Object[nodes.length];

			// copy 'nodes' and update 1 element(s) at position 'idx'
			arraycopy(nodes, 0, newNodes, 0, nodes.length);
			newNodes[idx] = node;

			return new BitmapIndexedNode(nodeMap, dataMap, newNodes, indices);
		}

		Node copyAndInsertValue(final int bitpos, final Property element, int sequenceId) {
			final int idx = index(dataMap, bitpos);

			final Object[] newNodes = new Object[nodes.length + 1];

			// copy 'nodes' and insert 1 element(s) at position 'idx'
			arraycopy(nodes, 0, newNodes, 0, idx);
			newNodes[idx] = element;
			arraycopy(nodes, idx, newNodes, idx + 1, nodes.length - idx);

			final int[] newIndices = new int[indices.length + 1];

			// copy 'indices' and insert 1 element(s) at position 'idx'
			arraycopy(indices, 0, newIndices, 0, idx);
			newIndices[idx] = sequenceId;
			arraycopy(indices, idx, newIndices, idx + 1, indices.length - idx);

			return new BitmapIndexedNode(nodeMap, dataMap | bitpos, newNodes, newIndices);
		}

		Node copyAndSetValue(final int bitpos, final Property element, int sequenceId) {
			final int idx = index(dataMap, bitpos);

			final Object[] newNodes = new Object[nodes.length];

			// copy 'nodes' and set element(s) at position 'idx'
			arraycopy(nodes, 0, newNodes, 0, nodes.length);
			newNodes[idx] = element;

			final int[] newIndices = new int[indices.length];

			// copy 'indices' and set 1 element(s) at position 'idx'
			arraycopy(indices, 0, newIndices, 0, indices.length);
			newIndices[idx] = sequenceId;

			return new BitmapIndexedNode(nodeMap, dataMap, newNodes, newIndices);
		}
		
		Node copyAndRemoveValue(final int bitpos) {
			final int idx = index(dataMap, bitpos);

			final Object[] newNodes = new Object[nodes.length - 1];

			// copy 'nodes' and remove 1 element(s) at position 'idx'
			arraycopy(nodes, 0, newNodes, 0, idx);
			arraycopy(nodes, idx + 1, newNodes, idx, nodes.length - idx - 1);

			final int[] newIndices = new int[indices.length - 1];

			// copy 'indices' and remove 1 element(s) at position 'idx'
			arraycopy(indices, 0, newIndices, 0, idx);
			arraycopy(indices, idx + 1, newIndices, idx, indices.length - idx - 1);

			return new BitmapIndexedNode(nodeMap, dataMap ^ bitpos, newNodes, newIndices);
		}

		Node copyAndMigrateFromInlineToNode(final int bitpos, final Node node) {

			final int idxOld = index(dataMap, bitpos);
			final int idxNew = nodes.length - 1 - index(nodeMap, bitpos);

			final Object[] newNodes = new Object[nodes.length];

			// copy 'nodes' and remove 1 element(s) at position 'idxOld' and
			// insert 1 element(s) at position 'idxNew'
			assert idxOld <= idxNew;
			arraycopy(nodes, 0, newNodes, 0, idxOld);
			arraycopy(nodes, idxOld + 1, newNodes, idxOld, idxNew - idxOld);
			newNodes[idxNew] = node;
			arraycopy(nodes, idxNew + 1, newNodes, idxNew + 1, nodes.length - idxNew - 1);

			final int[] newIndices = new int[indices.length - 1];

			// copy 'indices' and remove 1 element(s) at position 'idxOld'
			arraycopy(indices, 0, newIndices, 0, idxOld);
			arraycopy(indices, idxOld + 1, newIndices, idxOld, indices.length - idxOld - 1);

			return new BitmapIndexedNode(nodeMap | bitpos, dataMap ^ bitpos, newNodes, newIndices);
		}

		Node copyAndMigrateFromNodeToInline(final int bitpos, final Node node) {

			final int idxOld = nodes.length - 1 - index(nodeMap, bitpos);
			final int idxNew = index(dataMap, bitpos);

			final Object[] newNodes = new Object[nodes.length];

			// copy 'nodes' and remove 1 element(s) at position 'idxOld' and
			// insert 1 element(s) at position 'idxNew'
			assert idxOld >= idxNew;
			arraycopy(nodes, 0, newNodes, 0, idxNew);
			newNodes[idxNew] = node.getElement(0);
			arraycopy(nodes, idxNew, newNodes, idxNew + 1, idxOld - idxNew);
			arraycopy(nodes, idxOld + 1, newNodes, idxOld + 1, nodes.length - idxOld - 1);

			final int[] newIndices = new int[indices.length + 1];

			// copy 'indices' and insert 1 element(s) at position 'idxNew'
			arraycopy(indices, 0, newIndices, 0, idxNew);
			newIndices[idxNew] = node.getSequenceId(0);
			arraycopy(indices, idxNew, newIndices, idxNew + 1, indices.length - idxNew);

			return new BitmapIndexedNode(nodeMap ^ bitpos, dataMap | bitpos, newNodes, newIndices);
		}

		@Override
		public boolean containsKey(final Object key, final int keyHash, final int shift) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap & bitpos) != 0) {
				final int index = index(dataMap, mask, bitpos);
				return getKey(index).equals(key);
			}

			if ((nodeMap & bitpos) != 0) {
				final int index = index(nodeMap, mask, bitpos);
				return getNode(index).containsKey(key, keyHash, shift + bitPartitionSize());
			}

			return false;
		}

		@Override
		public boolean containsValue(final Property element, final int keyHash, final int shift) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap & bitpos) != 0) {
				final int index = index(dataMap, mask, bitpos);
				return getElement(index).equals(element);
			}

			if ((nodeMap & bitpos) != 0) {
				final int index = index(nodeMap, mask, bitpos);
				return getNode(index).containsValue(element, keyHash, shift + bitPartitionSize());
			}

			return false;
		}

		@Override
		public Optional<Property> find(final Object key, final int keyHash, final int shift) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap & bitpos) != 0) {
				final int index = index(dataMap, mask, bitpos);
				if (getKey(index).equals(key)) {
					return Optional.of(getElement(index));
				}

				return Optional.empty();
			}

			if ((nodeMap & bitpos) != 0) {
				final int index = index(nodeMap, mask, bitpos);
				return getNode(index).find(key, keyHash, shift + bitPartitionSize());
			}

			return Optional.empty();
		}

		@Override
		public Node updated(Object key, final Property element, final int keyHash,
				int sequenceId, final int shift, final UpdateReport report) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap & bitpos) != 0) { // inplace value
				final int dataIndex = index(dataMap, bitpos);

				if (getKey(dataIndex).equals(key)) {
					// update mapping
					report.setTrieModified();
					return copyAndSetValue(bitpos, element, sequenceId);
				} else {
					final Property currentElement = getElement(dataIndex);
					final int currentKeyHash = getKey(dataIndex).hashCode();
					final int currentSequenceId = getSequenceId(dataIndex);

					final Node subNodeNew = mergeTwoElements(currentElement,
							transformHashCode(currentKeyHash), currentSequenceId, element, keyHash,
							sequenceId, shift + bitPartitionSize());

					report.setTrieModified();
					return copyAndMigrateFromInlineToNode(bitpos, subNodeNew);
				}
			} else if ((nodeMap & bitpos) != 0) { // node (not value)
				final int nodeIndex = index(nodeMap, bitpos);

				final Node subNode = getNode(nodeIndex);
				final Node subNodeNew = subNode.updated(key, element, keyHash, sequenceId, shift
								+ bitPartitionSize(), report);

				if (report.isTrieModified()) {
					return copyAndSetNode(bitpos, subNodeNew);
				} else {
					return this;
				}
			} else {
				// no value
				report.setTrieModified();
				return copyAndInsertValue(bitpos, element, sequenceId);
			}
		}

		@Override
		public Node removed(final Object key, final int keyHash, final int shift,
				final UpdateReport report) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap & bitpos) != 0) { // inplace value
				final int dataIndex = index(dataMap, bitpos);

				if (getKey(dataIndex).equals(key)) {
					report.setTrieModified();

					if (this.elementArity() == 2 && this.nodeArity() == 0) {
						/*
						 * Create new node with remaining pair. The new node
						 * will a) either become the new root returned, or b)
						 * unwrapped and inlined during returning.
						 */
						final int newDataMap = (shift == 0) ? dataMap ^ bitpos : bitpos(mask(
								keyHash, 0));

						return BitmapIndexedNode.newElementSingleton(newDataMap,
								getElement(1 - dataIndex), getSequenceId(1 - dataIndex));
					} else {
						return copyAndRemoveValue(bitpos);
					}
				} else {
					return this;
				}
			} else if ((nodeMap & bitpos) != 0) { // node (not value)
				final int nodeIndex = index(nodeMap, bitpos);

				final Node subNode = getNode(nodeIndex);
				final Node subNodeNew = subNode.removed(key, keyHash, shift + bitPartitionSize(),
						report);

				if (!report.isTrieModified()) {
					return this;
				}

				if (subNodeNew.sizePredicate() == SIZE_ONE) {
					if (this.elementArity() == 0 && this.nodeArity() == 1) {
						// escalate (singleton or empty) result
						return subNodeNew;
					} else {
						// inline value (move to front)
						return copyAndMigrateFromNodeToInline(bitpos, subNodeNew);
					}
				} else {
					assert subNode.sizePredicate() == SIZE_MORE_THAN_ONE;

					// modify current node (set replacement node)
					return copyAndSetNode(bitpos, subNodeNew);
				}
			} else {
				// no value
				return this;
			}
		}

	}

	private static final class HashCollisionNode implements Node {

		private final int hash;
		private final Property[] elements;
		private final int[] indices;

		private HashCollisionNode(final int hash, final Property element0, int sequenceId0,
				final Property element1, int sequenceId1) {
			this.hash = hash;
			this.elements = new Property[] { element0, element1 };
			this.indices = new int[] { sequenceId0, sequenceId1 };
		}

		private HashCollisionNode(final int hash, final Property[] elements, final int[] indices) {
			if (elements.length <= 2) {
				throw new IllegalArgumentException("At least two elements are required.");
			}
			this.hash = hash;
			this.elements = elements;
			this.indices = indices;
		}

		@Override
		public boolean containsKey(final Object key, final int keyHash, final int shift) {
			return hash == keyHash && Stream.of(elements).anyMatch(e -> extractKey(e).equals(key));
		}

		@Override
		public boolean containsValue(final Property element, final int keyHash, final int shift) {
			return hash == keyHash && Stream.of(elements).anyMatch(e -> e.equals(element));
		}

		@Override
		public Optional<Property> find(final Object key, final int keyHash, final int shift) {
			return Stream.of(elements).filter(e -> extractKey(e).equals(key)).findAny();
		}

		// TODO: Object key = extractKey(element);
		@Override
		public Node updated(Object key, final Property element, final int keyHash,
				int sequenceId, final int shift, final UpdateReport report) {
			assert this.hash == keyHash;

			if (Stream.of(elements).anyMatch(e -> e.equals(element))) {
				return this;
			} else {
				final Property[] extendedElements = new Property[elements.length + 1];
				arraycopy(elements, 0, extendedElements, 0, elements.length);
				extendedElements[elements.length] = element;

				final int[] extendedIndices = new int[indices.length + 1];
				arraycopy(indices, 0, extendedIndices, 0, indices.length);
				extendedIndices[indices.length] = sequenceId;

				report.setTrieModified();
				return new HashCollisionNode(keyHash, extendedElements, extendedIndices);
			}
		}

		@Override
		public Node removed(final Object key, final int keyHash, final int shift,
				final UpdateReport report) {
			assert this.hash == keyHash;

			int indexOfKey = -1;

			for (int i = 0; i < elementArity() && indexOfKey == -1; i++) {
				if (getKey(i).equals(key)) {
					indexOfKey = i;
				}
			}

			if (indexOfKey == -1) {
				return this;
			} else {
				if (elements.length == 2) {
					/*
					 * Create root node with singleton element. This node will
					 * be a) either be the new root returned, or b) unwrapped
					 * and inlined.
					 */
					final int dataMap = bitpos(mask(hash, 0));

					report.setTrieModified();
					return BitmapIndexedNode.newElementSingleton(dataMap, elements[1 - indexOfKey],
							indices[1 - indexOfKey]);
				} else {
					final Property[] reducedElements = new Property[elements.length - 1];
					arraycopy(elements, 0, reducedElements, 0, indexOfKey);
					arraycopy(elements, indexOfKey + 1, reducedElements, indexOfKey,
							elements.length - indexOfKey - 1);

					final int[] reducedIndices = new int[indices.length - 1];
					arraycopy(indices, 0, reducedIndices, 0, indexOfKey);
					arraycopy(indices, indexOfKey + 1, reducedIndices, indexOfKey, indices.length
							- indexOfKey - 1);

					report.setTrieModified();
					return new HashCollisionNode(keyHash, reducedElements, reducedIndices);
				}
			}
		}

		@Override
		public boolean hasElements() {
			return true;
		}

		@Override
		public int elementArity() {
			return elements.length;
		}

		@Override
		public boolean hasNodes() {
			return false;
		}

		@Override
		public int nodeArity() {
			return 0;
		}

		@Override
		public byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public Property getElement(final int index) {
			return elements[index];
		}

		@Override
		public int getSequenceId(final int index) {
			return indices[index];
		}

		@Override
		public Node getNode(int index) {
			throw new UnsupportedOperationException(
					"Hash collision nodes are leaf nodes, without further sub-trees.");
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

			if (elementArity() != that.elementArity()) {
				return false;
			}

			/*
			 * Linear scan for each element, because of arbitrary element order.
			 */
			for (Property e1 : elements) {
				if (Stream.of(that.elements).noneMatch(e2 -> e1.equals(e2))) {
					return false;
				}
			}

			return true;
		}

	}

	/**
	 * Iterator skeleton that uses a fixed stack in depth.
	 */
	private static abstract class AbstractIterator {

		private static final int MAX_DEPTH = 7;

		protected int currentValueCursor;
		protected int currentValueLength;
		protected Node currentValueNode;

		private int currentStackLevel = -1;
		private final int[] nodeCursorsAndLengths = new int[MAX_DEPTH * 2];

		Node[] nodes = new Node[MAX_DEPTH];

		AbstractIterator(Node rootNode) {
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
					final Node nextNode = nodes[currentStackLevel].getNode(nodeCursor);
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

	private static final class KeyIterator extends AbstractIterator implements Iterator<Object> {

		KeyIterator(Node rootNode) {
			super(rootNode);
		}

		@Override
		public Object next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			} else {
				return currentValueNode.getKey(currentValueCursor++);
			}
		}
	}

	private static final class ElementIterator extends AbstractIterator implements
			Iterator<Property> {

		ElementIterator(Node rootNode) {
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

	private static final class EntryIterator extends AbstractIterator implements
			Iterator<Map.Entry<Object, Property>> {

		EntryIterator(Node rootNode) {
			super(rootNode);
		}

		@Override
		public Map.Entry<Object, Property> next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			} else {
				return new ImmutableMapEntry(currentValueNode.getElement(currentValueCursor++));
			}
		}
	}

	private static class ImmutableMapEntry implements Map.Entry<Object, Property> {

		private final Property backingProperty;

		ImmutableMapEntry(final Property property) {
			this.backingProperty = property;
		}

		@Override
		public Object getKey() {
			return extractKey(backingProperty);
		}

		@Override
		public Property getValue() {
			return backingProperty;
		}

		@Override
		public Property setValue(Property value) {
			throw new UnsupportedOperationException();
		}

	}

}
