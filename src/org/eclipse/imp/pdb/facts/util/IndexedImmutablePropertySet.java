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

import static java.lang.System.arraycopy;
import static org.eclipse.imp.pdb.facts.util.IndexedImmutablePropertySet.Node.*;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.oracle.truffle.api.object.Property;

/*
 * TODO: test default method behavior.
 */
public class IndexedImmutablePropertySet extends AbstractSet<Property> implements
		IndexedImmutableSet<Property> {

	private static final Node EMPTY_NODE = new BitmapIndexedNode(0, 0, new Object[] {});

	private static final IndexedImmutablePropertySet EMPTY_SET = new IndexedImmutablePropertySet(
			EMPTY_NODE, 0, 0);

	private static final boolean DEBUG = false;

	private final Node rootNode;
	private final int hashCode;
	private final int cachedSize;

	private IndexedImmutablePropertySet(Node rootNode, int hashCode, int cachedSize) {
		this.rootNode = rootNode;
		this.hashCode = hashCode;
		this.cachedSize = cachedSize;
		if (DEBUG) {
			assert checkHashCodeAndSize(hashCode, cachedSize);
		}
	}

	public static final IndexedImmutableSet<Property> of() {
		return EMPTY_SET;
	}

	public static final IndexedImmutableSet<Property> of(Property... elements) {
		IndexedImmutableSet<Property> result = EMPTY_SET;

		for (final Property element : elements) {
			result = result.__insert(element);
		}

		return result;
	}

	private boolean checkHashCodeAndSize(final int targetHash, final int targetSize) {
		int hash = 0;
		int size = 0;

		for (Iterator<Property> it = new ElementIterator(rootNode); it.hasNext();) {
			final Property element = it.next();

			hash += element.hashCode();
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
			final Property element = (Property) o;
			final int keyHash = extractKey(element).hashCode();

			return rootNode.contains(element, transformHashCode(keyHash), 0);
		} catch (ClassCastException unused) {
			return false;
		}
	}

	// @Override
	// public boolean containsAll(final Collection<?> c) {
	// for (Object item : c) {
	// if (!contains(item)) {
	// return false;
	// }
	// }
	// return true;
	// }

	@Override
	public Property get(final Object o) {
		try {
			final Property element = (Property) o;
			final int keyHash = extractKey(element).hashCode();

			final Optional<Property> result = rootNode.find(element, transformHashCode(keyHash), 0);

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
	public IndexedImmutableSet<Property> __insert(final Property element) {
		final int keyHash = extractKey(element).hashCode();
		final UpdateReport report = new UpdateReport();

		final Node newRootNode = rootNode.updated(element, transformHashCode(keyHash), 0, report);

		if (report.isTrieModified()) {
			return new IndexedImmutablePropertySet(newRootNode, hashCode + element.hashCode(),
					cachedSize + 1);
		}

		return this;
	}

	@Override
	public IndexedImmutableSet<Property> __remove(final Property element) {
		final int keyHash = extractKey(element).hashCode();
		final UpdateReport report = new UpdateReport();

		final Node newRootNode = rootNode.removed(element, transformHashCode(keyHash), 0, report);

		if (report.isTrieModified()) {
			return new IndexedImmutablePropertySet(newRootNode, hashCode - element.hashCode(),
					cachedSize - 1);
		}

		return this;
	}

	@Override
	public int size() {
		return cachedSize;
	}

	// @Override
	// public boolean isEmpty() {
	// return cachedSize == 0;
	// }

	@Override
	public Iterator<Property> iterator() {
		return new ElementIterator(rootNode);
	}

	// @Override
	// public Object[] toArray() {
	// Object[] array = new Object[cachedSize];
	//
	// int idx = 0;
	// for (Property element : this) {
	// array[idx++] = element;
	// }
	//
	// return array;
	// }
	//
	// @Override
	// public <T> T[] toArray(final T[] a) {
	// List<Property> list = new ArrayList<Property>(cachedSize);
	//
	// for (Property element : this) {
	// list.add(element);
	// }
	//
	// return list.toArray(a);
	// }

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

		boolean contains(final Property element, final int keyHash, final int shift);

		Optional<Property> find(final Property element, final int keyHash, final int shift);

		Node updated(final Property element, final int keyHash, final int shift,
				final UpdateReport report);

		Node removed(final Property element, final int keyHash, final int shift,
				final UpdateReport report);

		boolean hasNodes();

		int nodeArity();

		Node getNode(final int index);

		boolean hasElements();

		int elementArity();

		Property getElement(final int index);

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

		static Node mergeTwoElements(final Property element0, final int keyHash0,
				final Property element1, final int keyHash1, final int shift) {
			assert !(element0.equals(element1));

			if (shift >= hashCodeLength()) {
				return new HashCollisionNode(keyHash0, element0, element1);
			}

			final int mask0 = mask(keyHash0, shift);
			final int mask1 = mask(keyHash1, shift);

			if (mask0 != mask1) {
				// both nodes fit on same level
				final int dataMap = bitpos(mask0) | bitpos(mask1);

				if (mask0 < mask1) {
					return new BitmapIndexedNode(0, dataMap, new Object[] { element0, element1 });
				} else {
					return new BitmapIndexedNode(0, dataMap, new Object[] { element1, element0 });
				}
			} else {
				final Node node = mergeTwoElements(element0, keyHash0, element1, keyHash1, shift
						+ bitPartitionSize());
				// values fit on next level

				final int nodeMap = bitpos(mask0);
				return new BitmapIndexedNode(nodeMap, 0, new Object[] { node });
			}
		}

	}

	private static final class BitmapIndexedNode implements Node {

		private final int nodeMap;
		private final int dataMap;
		private final Object[] nodes;

		private BitmapIndexedNode(final int nodeMap, final int dataMap, final Object[] nodes) {

			this.nodeMap = nodeMap;
			this.dataMap = dataMap;
			this.nodes = nodes;

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

			final Object[] src = this.nodes;
			final Object[] dst = new Object[src.length];

			// copy 'src' and set 1 element(s) at position 'idx'
			arraycopy(src, 0, dst, 0, src.length);
			dst[idx] = node;

			return new BitmapIndexedNode(nodeMap, dataMap, dst);
		}

		Node copyAndInsertValue(final int bitpos, final Property element) {
			final int idx = index(dataMap, bitpos);

			final Object[] src = this.nodes;
			final Object[] dst = new Object[src.length + 1];

			// copy 'src' and insert 1 element(s) at position 'idx'
			arraycopy(src, 0, dst, 0, idx);
			dst[idx] = element;
			arraycopy(src, idx, dst, idx + 1, src.length - idx);

			return new BitmapIndexedNode(nodeMap, dataMap | bitpos, dst);
		}

		Node copyAndRemoveValue(final int bitpos) {
			final int idx = index(dataMap, bitpos);

			final Object[] src = this.nodes;
			final Object[] dst = new Object[src.length - 1];

			// copy 'src' and remove 1 element(s) at position 'idx'
			arraycopy(src, 0, dst, 0, idx);
			arraycopy(src, idx + 1, dst, idx, src.length - idx - 1);

			return new BitmapIndexedNode(nodeMap, dataMap ^ bitpos, dst);
		}

		Node copyAndMigrateFromInlineToNode(final int bitpos, final Node node) {

			final int idxOld = index(dataMap, bitpos);
			final int idxNew = this.nodes.length - 1 - index(nodeMap, bitpos);

			final Object[] src = this.nodes;
			final Object[] dst = new Object[src.length - 1 + 1];

			// copy 'src' and remove 1 element(s) at position 'idxOld' and
			// insert 1 element(s) at position 'idxNew'
			assert idxOld <= idxNew;
			arraycopy(src, 0, dst, 0, idxOld);
			arraycopy(src, idxOld + 1, dst, idxOld, idxNew - idxOld);
			dst[idxNew] = node;
			arraycopy(src, idxNew + 1, dst, idxNew + 1, src.length - idxNew - 1);

			return new BitmapIndexedNode(nodeMap | bitpos, dataMap ^ bitpos, dst);
		}

		Node copyAndMigrateFromNodeToInline(final int bitpos, final Node node) {

			final int idxOld = this.nodes.length - 1 - index(nodeMap, bitpos);
			final int idxNew = index(dataMap, bitpos);

			final Object[] src = this.nodes;
			final Object[] dst = new Object[src.length - 1 + 1];

			// copy 'src' and remove 1 element(s) at position 'idxOld' and
			// insert 1 element(s) at position 'idxNew'
			assert idxOld >= idxNew;
			arraycopy(src, 0, dst, 0, idxNew);
			dst[idxNew] = node.getElement(0);
			arraycopy(src, idxNew, dst, idxNew + 1, idxOld - idxNew);
			arraycopy(src, idxOld + 1, dst, idxOld + 1, src.length - idxOld - 1);

			return new BitmapIndexedNode(nodeMap ^ bitpos, dataMap | bitpos, dst);
		}

		@Override
		public boolean contains(final Property element, final int keyHash, final int shift) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap & bitpos) != 0) {
				final int index = index(dataMap, mask, bitpos);
				return getElement(index).equals(element);
			}

			if ((nodeMap & bitpos) != 0) {
				final int index = index(nodeMap, mask, bitpos);
				return getNode(index).contains(element, keyHash, shift + bitPartitionSize());
			}

			return false;
		}

		@Override
		public Optional<Property> find(final Property element, final int keyHash, final int shift) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap & bitpos) != 0) {
				final int index = index(dataMap, mask, bitpos);
				if (getElement(index).equals(element)) {
					return Optional.of(getElement(index));
				}

				return Optional.empty();
			}

			if ((nodeMap & bitpos) != 0) {
				final int index = index(nodeMap, mask, bitpos);
				return getNode(index).find(element, keyHash, shift + bitPartitionSize());
			}

			return Optional.empty();
		}

		@Override
		public Node updated(final Property element, final int keyHash, final int shift,
				final UpdateReport report) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap & bitpos) != 0) { // inplace value
				final int dataIndex = index(dataMap, bitpos);
				final Property currentElement = getElement(dataIndex);

				if (currentElement.equals(element)) {
					return this;
				} else {
					final int currentKeyHash = extractKey(currentElement).hashCode();
					final Node subNodeNew = mergeTwoElements(currentElement,
							transformHashCode(currentKeyHash), element, keyHash, shift
									+ bitPartitionSize());

					report.setTrieModified();
					return copyAndMigrateFromInlineToNode(bitpos, subNodeNew);
				}
			} else if ((nodeMap & bitpos) != 0) { // node (not value)
				final int nodeIndex = index(nodeMap, bitpos);

				final Node subNode = getNode(nodeIndex);
				final Node subNodeNew = subNode.updated(element, keyHash, shift
						+ bitPartitionSize(), report);

				if (report.isTrieModified()) {
					return copyAndSetNode(bitpos, subNodeNew);
				} else {
					return this;
				}
			} else {
				// no value
				report.setTrieModified();
				return copyAndInsertValue(bitpos, element);
			}
		}

		@Override
		public Node removed(final Property element, final int keyHash, final int shift,
				final UpdateReport report) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap & bitpos) != 0) { // inplace value
				final int dataIndex = index(dataMap, bitpos);

				if (getElement(dataIndex).equals(element)) {
					report.setTrieModified();

					if (this.elementArity() == 2 && this.nodeArity() == 0) {
						/*
						 * Create new node with remaining pair. The new node
						 * will a) either become the new root returned, or b)
						 * unwrapped and inlined during returning.
						 */
						final int newDataMap = (shift == 0) ? dataMap ^ bitpos : bitpos(mask(
								keyHash, 0));

						return new BitmapIndexedNode(0, newDataMap,
								new Object[] { getElement(1 - dataIndex) });
					} else {
						return copyAndRemoveValue(bitpos);
					}
				} else {
					return this;
				}
			} else if ((nodeMap & bitpos) != 0) { // node (not value)
				final int nodeIndex = index(nodeMap, bitpos);

				final Node subNode = getNode(nodeIndex);
				final Node subNodeNew = subNode.removed(element, keyHash, shift
						+ bitPartitionSize(), report);

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

		private HashCollisionNode(final int hash, final Property element0, final Property element1) {
			this.hash = hash;
			this.elements = new Property[] { element0, element1 };
		}

		private HashCollisionNode(final int hash, final Property[] elements) {
			if (elements.length <= 2) {
				throw new IllegalArgumentException("At least two elements are required.");
			}
			this.hash = hash;
			this.elements = elements;
		}

		@Override
		public boolean contains(final Property element, final int keyHash, final int shift) {
			return hash == keyHash && Stream.of(elements).anyMatch(e -> e.equals(element));
		}

		@Override
		public Optional<Property> find(final Property element, final int keyHash, final int shift) {
			return Stream.of(elements).filter(e -> e.equals(element)).findAny();
		}

		@Override
		public Node updated(final Property element, final int keyHash, final int shift,
				final UpdateReport report) {
			assert this.hash == keyHash;

			if (Stream.of(elements).anyMatch(e -> e.equals(element))) {
				return this;
			} else {
				final Property[] extendedElements = new Property[elements.length + 1];
				arraycopy(elements, 0, extendedElements, 0, elements.length);
				extendedElements[elements.length] = element;

				report.setTrieModified();
				return new HashCollisionNode(keyHash, extendedElements);
			}
		}

		@Override
		public Node removed(final Property element, final int keyHash, final int shift,
				final UpdateReport report) {
			if (Stream.of(elements).noneMatch(e -> e.equals(element))) {
				return this;
			} else {
				Property[] reducedElements = Stream.of(elements).filter(e -> !e.equals(element))
						.toArray(Property[]::new);

				if (reducedElements.length == 1) {
					/*
					 * Create root node with singleton element. This node will
					 * be a) either be the new root returned, or b) unwrapped
					 * and inlined.
					 */
					return EMPTY_NODE.updated(reducedElements[0], keyHash, 0, report);
				} else {
					report.setTrieModified();
					return new HashCollisionNode(keyHash, reducedElements);
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

}
