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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class TrieMap_Heterogeneous implements ImmutableMap<Object, Object> {

	private static final TrieMap_Heterogeneous EMPTY_MAP = new TrieMap_Heterogeneous(
					CompactMapNode.emptyTrieNodeConstant(), 0, 0);

	private static final boolean DEBUG = false;

	private final AbstractMapNode rootNode;
	private final int hashCode;
	private final int cachedSize;

	TrieMap_Heterogeneous(AbstractMapNode rootNode, int hashCode, int cachedSize) {
		this.rootNode = rootNode;
		this.hashCode = hashCode;
		this.cachedSize = cachedSize;
		if (DEBUG) {
			assert checkHashCodeAndSize(hashCode, cachedSize);
		}
	}

	public static final ImmutableMap of() {
		return TrieMap_Heterogeneous.EMPTY_MAP;
	}

	public static final ImmutableMap of(Object... keyValuePairs) {
		if (keyValuePairs.length % 2 != 0) {
			throw new IllegalArgumentException(
							"Length of argument list is uneven: no key/value pairs.");
		}

		ImmutableMap result = TrieMap_Heterogeneous.EMPTY_MAP;

		for (int i = 0; i < keyValuePairs.length; i += 2) {
			final Object key = keyValuePairs[i];
			final Object val = keyValuePairs[i + 1];

			result = result.__put(key, val);
		}

		return result;
	}

	public static final TransientMap transientOf() {
		return TrieMap_Heterogeneous.EMPTY_MAP.asTransient();
	}

	public static final TransientMap transientOf(Object... keyValuePairs) {
		if (keyValuePairs.length % 2 != 0) {
			throw new IllegalArgumentException(
							"Length of argument list is uneven: no key/value pairs.");
		}

		final TransientMap result = TrieMap_Heterogeneous.EMPTY_MAP.asTransient();

		for (int i = 0; i < keyValuePairs.length; i += 2) {
			final Object key = keyValuePairs[i];
			final Object val = keyValuePairs[i + 1];

			result.__put(key, val);
		}

		return result;
	}

	private boolean checkHashCodeAndSize(final int targetHash, final int targetSize) {
		int hash = 0;
		int size = 0;

		for (Iterator<Map.Entry<Object, Object>> it = entryIterator(); it.hasNext();) {
			final Map.Entry<Object, Object> entry = it.next();
			final Object key = entry.getKey();
			final Object val = entry.getValue();

			hash += key.hashCode() ^ val.hashCode();
			size += 1;
		}

		return hash == targetHash && size == targetSize;
	}

	public static final int transformHashCode(final int hash) {
		return hash;
	}

	@Override
	public boolean containsKey(final Object o) {
		try {

			final Object key = o;
			return rootNode.containsKey(key, transformHashCode(key.hashCode()), 0);
		} catch (ClassCastException unused) {
			return false;
		}
	}

	@Override
	public boolean containsKeyEquivalent(final Object o, final Comparator<Object> cmp) {
		try {

			final Object key = o;
			return rootNode.containsKey(key, transformHashCode(key.hashCode()), 0, cmp);
		} catch (ClassCastException unused) {
			return false;
		}
	}

	@Override
	public boolean containsValue(final Object o) {
		for (Iterator iterator = valueIterator(); iterator.hasNext();) {
			if (iterator.next().equals(o)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsValueEquivalent(final Object o, final Comparator<Object> cmp) {
		for (Iterator iterator = valueIterator(); iterator.hasNext();) {
			if (cmp.compare(iterator.next(), o) == 0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Object get(final Object o) {
		try {

			final Object key = o;
			final Optional result = rootNode.findByKey(key, transformHashCode(key.hashCode()), 0);

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
	public Object getEquivalent(final Object o, final Comparator<Object> cmp) {
		try {

			final Object key = o;
			final Optional result = rootNode.findByKey(key, transformHashCode(key.hashCode()), 0,
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
	public ImmutableMap __put(final Object key, final Object val) {
		final int keyHash = key.hashCode();
		final MapResult details = MapResult.unchanged();

		final CompactMapNode newRootNode = rootNode.updated(null, key, val,
						transformHashCode(keyHash), 0, details);

		if (details.isModified()) {
			if (details.hasReplacedValue()) {
				final int valHashOld = details.getReplacedValue().hashCode();
				final int valHashNew = val.hashCode();

				return new TrieMap_Heterogeneous(newRootNode, hashCode + ((keyHash ^ valHashNew))
								- ((keyHash ^ valHashOld)), cachedSize);
			}

			final int valHash = val.hashCode();
			return new TrieMap_Heterogeneous(newRootNode, hashCode + ((keyHash ^ valHash)),
							cachedSize + 1);
		}

		return this;
	}

	@Override
	public ImmutableMap __putEquivalent(final Object key, final Object val,
					final Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final MapResult details = MapResult.unchanged();

		final CompactMapNode newRootNode = rootNode.updated(null, key, val,
						transformHashCode(keyHash), 0, details, cmp);

		if (details.isModified()) {
			if (details.hasReplacedValue()) {
				final int valHashOld = details.getReplacedValue().hashCode();
				final int valHashNew = val.hashCode();

				return new TrieMap_Heterogeneous(newRootNode, hashCode + ((keyHash ^ valHashNew))
								- ((keyHash ^ valHashOld)), cachedSize);
			}

			final int valHash = val.hashCode();
			return new TrieMap_Heterogeneous(newRootNode, hashCode + ((keyHash ^ valHash)),
							cachedSize + 1);
		}

		return this;
	}

	@Override
	public ImmutableMap __putAll(final Map<? extends Object, ? extends Object> map) {
		final TransientMap tmpTransient = this.asTransient();
		tmpTransient.__putAll(map);
		return tmpTransient.freeze();
	}

	@Override
	public ImmutableMap __putAllEquivalent(final Map<? extends Object, ? extends Object> map,
					final Comparator<Object> cmp) {
		final TransientMap tmpTransient = this.asTransient();
		tmpTransient.__putAllEquivalent(map, cmp);
		return tmpTransient.freeze();
	}

	@Override
	public ImmutableMap __remove(final Object key) {
		final int keyHash = key.hashCode();
		final MapResult details = MapResult.unchanged();

		final CompactMapNode newRootNode = rootNode.removed(null, key, transformHashCode(keyHash),
						0, details);

		if (details.isModified()) {
			assert details.hasReplacedValue();
			final int valHash = details.getReplacedValue().hashCode();
			return new TrieMap_Heterogeneous(newRootNode, hashCode - ((keyHash ^ valHash)),
							cachedSize - 1);
		}

		return this;
	}

	@Override
	public ImmutableMap __removeEquivalent(final Object key, final Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final MapResult details = MapResult.unchanged();

		final CompactMapNode newRootNode = rootNode.removed(null, key, transformHashCode(keyHash),
						0, details, cmp);

		if (details.isModified()) {
			assert details.hasReplacedValue();
			final int valHash = details.getReplacedValue().hashCode();
			return new TrieMap_Heterogeneous(newRootNode, hashCode - ((keyHash ^ valHash)),
							cachedSize - 1);
		}

		return this;
	}

	@Override
	public Object put(final Object key, final Object val) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(final Map<? extends Object, ? extends Object> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object remove(final Object key) {
		throw new UnsupportedOperationException();
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
	public Iterator keyIterator() {
		return new MapKeyIterator(rootNode);
	}

	@Override
	public Iterator valueIterator() {
		return new MapValueIterator(rootNode);
	}

	@Override
	public Iterator<Map.Entry<Object, Object>> entryIterator() {
		return new MapEntryIterator(rootNode);
	}

	@Override
	public Set keySet() {
		Set keySet = null;

		if (keySet == null) {
			keySet = new AbstractSet() {
				@Override
				public Iterator iterator() {
					return TrieMap_Heterogeneous.this.keyIterator();
				}

				@Override
				public int size() {
					return TrieMap_Heterogeneous.this.size();
				}

				@Override
				public boolean isEmpty() {
					return TrieMap_Heterogeneous.this.isEmpty();
				}

				@Override
				public void clear() {
					TrieMap_Heterogeneous.this.clear();
				}

				@Override
				public boolean contains(Object k) {
					return TrieMap_Heterogeneous.this.containsKey(k);
				}
			};
		}

		return keySet;
	}

	@Override
	public Collection values() {
		Collection values = null;

		if (values == null) {
			values = new AbstractCollection() {
				@Override
				public Iterator iterator() {
					return TrieMap_Heterogeneous.this.valueIterator();
				}

				@Override
				public int size() {
					return TrieMap_Heterogeneous.this.size();
				}

				@Override
				public boolean isEmpty() {
					return TrieMap_Heterogeneous.this.isEmpty();
				}

				@Override
				public void clear() {
					TrieMap_Heterogeneous.this.clear();
				}

				@Override
				public boolean contains(Object v) {
					return TrieMap_Heterogeneous.this.containsValue(v);
				}
			};
		}

		return values;
	}

	@Override
	public Set<java.util.Map.Entry<Object, Object>> entrySet() {
		Set<java.util.Map.Entry<Object, Object>> entrySet = null;

		if (entrySet == null) {
			entrySet = new AbstractSet<java.util.Map.Entry<Object, Object>>() {
				@Override
				public Iterator<java.util.Map.Entry<Object, Object>> iterator() {
					return new Iterator<Map.Entry<Object, Object>>() {
						private final Iterator<Map.Entry<Object, Object>> i = entryIterator();

						@Override
						public boolean hasNext() {
							return i.hasNext();
						}

						@Override
						public Map.Entry<Object, Object> next() {
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
					return TrieMap_Heterogeneous.this.size();
				}

				@Override
				public boolean isEmpty() {
					return TrieMap_Heterogeneous.this.isEmpty();
				}

				@Override
				public void clear() {
					TrieMap_Heterogeneous.this.clear();
				}

				@Override
				public boolean contains(Object k) {
					return TrieMap_Heterogeneous.this.containsKey(k);
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

		if (other instanceof TrieMap_Heterogeneous) {
			TrieMap_Heterogeneous that = (TrieMap_Heterogeneous) other;

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

			for (Iterator<Map.Entry<Object, Object>> it = that.entrySet().iterator(); it.hasNext();) {
				Map.Entry<Object, Object> entry = it.next();

				try {

					final Object key = entry.getKey();
					final Optional result = rootNode.findByKey(key,
									transformHashCode(key.hashCode()), 0);

					if (!result.isPresent()) {
						return false;
					} else {

						final Object val = entry.getValue();

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
	public TransientMap asTransient() {
		return new TransientTrieMap_BleedingEdge(this);
	}

	/*
	 * For analysis purposes only.
	 */
	protected AbstractMapNode getRootNode() {
		return rootNode;
	}

	/*
	 * For analysis purposes only.
	 */
	protected Iterator<AbstractMapNode> nodeIterator() {
		return new TrieMap_BleedingEdgeNodeIterator(rootNode);
	}

	/*
	 * For analysis purposes only.
	 */
	protected int getNodeCount() {
		final Iterator<AbstractMapNode> it = nodeIterator();
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
		final Iterator<AbstractMapNode> it = nodeIterator();
		final int[][] sumArityCombinations = new int[33][33];

		while (it.hasNext()) {
			final AbstractMapNode node = it.next();
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

	static final class MapResult {
		private Object replacedValue;
		private boolean isModified;
		private boolean isReplaced;

		// update: inserted/removed single element, element count changed
		public void modified() {
			this.isModified = true;
		}

		public void updated(Object replacedValue) {
			this.replacedValue = replacedValue;
			this.isModified = true;
			this.isReplaced = true;
		}

		// update: neither element, nor element count changed
		public static MapResult unchanged() {
			return new MapResult();
		}

		private MapResult() {
		}

		public boolean isModified() {
			return isModified;
		}

		public boolean hasReplacedValue() {
			return isReplaced;
		}

		public Object getReplacedValue() {
			return replacedValue;
		}
	}

	protected static interface INode {
	}

	protected static abstract class AbstractMapNode implements INode {

		static final int TUPLE_LENGTH = 2;

		abstract boolean containsKey(final Object key, final int keyHash, final int shift);

		abstract boolean containsKey(final Object key, final int keyHash, final int shift,
						final Comparator<Object> cmp);

		abstract Optional findByKey(final Object key, final int keyHash, final int shift);

		abstract Optional findByKey(final Object key, final int keyHash, final int shift,
						final Comparator<Object> cmp);

		abstract CompactMapNode updated(final AtomicReference<Thread> mutator, final Object key,
						final Object val, final int keyHash, final int shift,
						final MapResult details);

		abstract CompactMapNode updated(final AtomicReference<Thread> mutator, final Object key,
						final Object val, final int keyHash, final int shift,
						final MapResult details, final Comparator<Object> cmp);

		abstract CompactMapNode removed(final AtomicReference<Thread> mutator, final Object key,
						final int keyHash, final int shift, final MapResult details);

		abstract CompactMapNode removed(final AtomicReference<Thread> mutator, final Object key,
						final int keyHash, final int shift, final MapResult details,
						final Comparator<Object> cmp);

		static final boolean isAllowedToEdit(AtomicReference<Thread> x, AtomicReference<Thread> y) {
			return x != null && y != null && (x == y || x.get() == y.get());
		}

		abstract boolean hasNodes();

		abstract int nodeArity();

		abstract AbstractMapNode getNode(final int index);

		@Deprecated
		Iterator<? extends AbstractMapNode> nodeIterator() {
			return new Iterator<AbstractMapNode>() {

				int nextIndex = 0;
				final int nodeArity = AbstractMapNode.this.nodeArity();

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				@Override
				public AbstractMapNode next() {
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

		abstract Object getKey(final int index);

		abstract Object getValue(final int index);

		abstract Map.Entry<Object, Object> getKeyValueEntry(final int index);

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
			final Iterator it = new MapKeyIterator(this);

			int size = 0;
			while (it.hasNext()) {
				size += 1;
				it.next();
			}

			return size;
		}
	}

	protected static abstract class CompactMapNode extends AbstractMapNode {

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

		abstract int rareMap();

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
		abstract CompactMapNode getNode(final int index);

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

		abstract CompactMapNode copyAndSetValue(final AtomicReference<Thread> mutator,
						final int bitpos, final Object val);

		abstract CompactMapNode copyAndInsertValue(final AtomicReference<Thread> mutator,
						final int bitpos, final Object key, final Object val);

		abstract CompactMapNode copyAndRemoveValue(final AtomicReference<Thread> mutator,
						final int bitpos);

		abstract CompactMapNode copyAndSetNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode node);

		abstract CompactMapNode copyAndMigrateFromInlineToNode(
						final AtomicReference<Thread> mutator, final int bitpos,
						final CompactMapNode node);

		abstract CompactMapNode copyAndMigrateFromNodeToInline(
						final AtomicReference<Thread> mutator, final int bitpos,
						final CompactMapNode node);

		static final CompactMapNode mergeTwoKeyValPairs(final Object key0, final Object val0,
						final int keyHash0, final Object key1, final Object val1,
						final int keyHash1, final int shift) {
			assert !(key0.equals(key1));

			if (shift >= hashCodeLength()) {
				// throw new
				// IllegalStateException("Hash collision not yet fixed.");
				return new HashCollisionMapNode_BleedingEdge(keyHash0, new Object[] { key0, key1 },
								new Object[] { val0, val1 });
			}

			final int mask0 = mask(keyHash0, shift);
			final int mask1 = mask(keyHash1, shift);

			if (mask0 != mask1) {
				// both nodes fit on same level
				final int dataMap = bitpos(mask0) | bitpos(mask1);

				if (mask0 < mask1) {
					return nodeOf(null, (0), dataMap, new Object[] { key0, val0, key1, val1 });
				} else {
					return nodeOf(null, (0), dataMap, new Object[] { key1, val1, key0, val0 });
				}
			} else {
				final CompactMapNode node = mergeTwoKeyValPairs(key0, val0, keyHash0, key1, val1,
								keyHash1, shift + bitPartitionSize());
				// values fit on next level

				final int nodeMap = bitpos(mask0);
				return nodeOf(null, nodeMap, (0), new Object[] { node });
			}
		}

		static final CompactMapNode emptyTrieNodeConstant() {
			return EMPTY_NODE;
		}

		static final CompactMapNode EMPTY_NODE;

		static {

			EMPTY_NODE = new BitmapIndexedMapNode(null, (0), (0), new Object[] {});

		};

		static final CompactMapNode nodeOf(final AtomicReference<Thread> mutator,
						final int nodeMap, final int dataMap, final Object[] nodes) {
			return new BitmapIndexedMapNode(mutator, nodeMap, dataMap, nodes);
		}

		static final CompactMapNode nodeOf(AtomicReference<Thread> mutator) {
			return EMPTY_NODE;
		}

		static final CompactMapNode nodeOf(AtomicReference<Thread> mutator, final int nodeMap,
						final int dataMap, final Object key, final Object val) {
			assert nodeMap == 0;
			return nodeOf(mutator, (0), dataMap, new Object[] { key, val });
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

		CompactMapNode nodeAt(final int bitpos) {
			return getNode(nodeIndex(bitpos));
		}

		@Override
		boolean containsKey(final Object key, final int keyHash, final int shift) {
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
				return getNode(index).containsKey(key, keyHash, shift + bitPartitionSize());
			}

			return false;
		}

		@Override
		boolean containsKey(final Object key, final int keyHash, final int shift,
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
				return getNode(index).containsKey(key, keyHash, shift + bitPartitionSize(), cmp);
			}

			return false;
		}

		@Override
		Optional findByKey(final Object key, final int keyHash, final int shift) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int index = dataIndex(bitpos);
				if (getKey(index).equals(key)) {
					final Object result = getValue(index);

					return Optional.of(result);
				}

				return Optional.empty();
			}

			if ((nodeMap() & bitpos) != 0) { // node (not value)
				final AbstractMapNode subNode = nodeAt(bitpos);

				return subNode.findByKey(key, keyHash, shift + bitPartitionSize());
			}

			return Optional.empty();
		}

		@Override
		Optional findByKey(final Object key, final int keyHash, final int shift,
						final Comparator<Object> cmp) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int index = dataIndex(bitpos);
				if (cmp.compare(getKey(index), key) == 0) {
					final Object result = getValue(index);

					return Optional.of(result);
				}

				return Optional.empty();
			}

			if ((nodeMap() & bitpos) != 0) { // node (not value)
				final AbstractMapNode subNode = nodeAt(bitpos);

				return subNode.findByKey(key, keyHash, shift + bitPartitionSize(), cmp);
			}

			return Optional.empty();
		}

		@Override
		CompactMapNode updated(final AtomicReference<Thread> mutator, final Object key,
						final Object val, final int keyHash, final int shift,
						final MapResult details) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int dataIndex = dataIndex(bitpos);
				final Object currentKey = getKey(dataIndex);

				if (currentKey.equals(key)) {
					final Object currentVal = getValue(dataIndex);

					// update mapping
					details.updated(currentVal);
					return copyAndSetValue(mutator, bitpos, val);
				} else {
					final Object currentVal = getValue(dataIndex);
					final CompactMapNode subNodeNew = mergeTwoKeyValPairs(currentKey, currentVal,
									transformHashCode(currentKey.hashCode()), key, val, keyHash,
									shift + bitPartitionSize());

					details.modified();
					return copyAndMigrateFromInlineToNode(mutator, bitpos, subNodeNew);
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactMapNode subNode = nodeAt(bitpos);
				final CompactMapNode subNodeNew = subNode.updated(mutator, key, val, keyHash, shift
								+ bitPartitionSize(), details);

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

		@Override
		CompactMapNode updated(final AtomicReference<Thread> mutator, final Object key,
						final Object val, final int keyHash, final int shift,
						final MapResult details, final Comparator<Object> cmp) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactMapNode removed(final AtomicReference<Thread> mutator, final Object key,
						final int keyHash, final int shift, final MapResult details) {
			final int mask = mask(keyHash, shift);
			final int bitpos = bitpos(mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int dataIndex = dataIndex(bitpos);

				if (getKey(dataIndex).equals(key)) {
					final Object currentVal = getValue(dataIndex);
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
							return CompactMapNode.nodeOf(mutator, 0, newDataMap, getKey(1),
											getValue(1));
						} else {
							return CompactMapNode.nodeOf(mutator, 0, newDataMap, getKey(0),
											getValue(0));
						}
					} else {
						return copyAndRemoveValue(mutator, bitpos);
					}
				} else {
					return this;
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactMapNode subNode = nodeAt(bitpos);
				final CompactMapNode subNodeNew = subNode.removed(mutator, key, keyHash, shift
								+ bitPartitionSize(), details);

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
		CompactMapNode removed(final AtomicReference<Thread> mutator, final Object key,
						final int keyHash, final int shift, final MapResult details,
						final Comparator<Object> cmp) {
			throw new UnsupportedOperationException();
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

			for (byte i = 0; i < payloadArity(); i++) {
				final byte pos = recoverMask(dataMap(), (byte) (i + 1));
				bldr.append(String.format("@%d<#%d,#%d>", pos, Objects.hashCode(getKey(i)),
								Objects.hashCode(getValue(i))));

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

	protected static abstract class CompactMixedMapNode extends CompactMapNode {

		private final int dataMap;
		private final int eitherMap;

		CompactMixedMapNode(final AtomicReference<Thread> mutator, final int maybeMap,
						final int dataMap) {
			this.eitherMap = maybeMap;
			this.dataMap = dataMap;
		}

		@Override
		public int dataMap() {
			return dataMap;
		}

		@Override
		public int nodeMap() {
			return eitherMap ^ rareMap();
		}

		@Override
		public int rareMap() {
			return eitherMap & dataMap;
		}

	}

	private static final class BitmapIndexedMapNode extends CompactMixedMapNode {

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

			assert nodeInvariant();
		}

		@Override
		Object getKey(final int index) {
			return nodes[TUPLE_LENGTH * index];
		}

		@Override
		Object getValue(final int index) {
			return nodes[TUPLE_LENGTH * index + 1];
		}

		@Override
		Map.Entry<Object, Object> getKeyValueEntry(final int index) {
			return entryOf(nodes[TUPLE_LENGTH * index], nodes[TUPLE_LENGTH * index + 1]);
		}

		@Override
		CompactMapNode getNode(final int index) {
			return (CompactMapNode) nodes[nodes.length - 1 - index];
		}

		@Override
		Iterator<? extends AbstractMapNode> nodeIterator() {
			final int length = nodeArity();
			final int offset = nodes.length - length;

			if (DEBUG) {
				for (int i = offset; i < offset + length; i++) {
					assert ((nodes[i] instanceof AbstractMapNode) == true);
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
			BitmapIndexedMapNode that = (BitmapIndexedMapNode) other;
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
		CompactMapNode copyAndSetValue(final AtomicReference<Thread> mutator, final int bitpos,
						final Object val) {
			final int idx = TUPLE_LENGTH * dataIndex(bitpos) + 1;

			if (isAllowedToEdit(this.mutator, mutator)) {
				// no copying if already editable
				this.nodes[idx] = val;
				return this;
			} else {
				final Object[] src = this.nodes;
				final Object[] dst = new Object[src.length];

				// copy 'src' and set 1 element(s) at position 'idx'
				System.arraycopy(src, 0, dst, 0, src.length);
				dst[idx + 0] = val;

				return nodeOf(mutator, nodeMap(), dataMap(), dst);
			}
		}

		@Override
		CompactMapNode copyAndSetNode(final AtomicReference<Thread> mutator, final int bitpos,
						final CompactMapNode node) {

			final int idx = this.nodes.length - 1 - nodeIndex(bitpos);

			if (isAllowedToEdit(this.mutator, mutator)) {
				// no copying if already editable
				this.nodes[idx] = node;
				return this;
			} else {
				final Object[] src = this.nodes;
				final Object[] dst = new Object[src.length];

				// copy 'src' and set 1 element(s) at position 'idx'
				System.arraycopy(src, 0, dst, 0, src.length);
				dst[idx + 0] = node;

				return nodeOf(mutator, nodeMap(), dataMap(), dst);
			}
		}

		@Override
		CompactMapNode copyAndInsertValue(final AtomicReference<Thread> mutator, final int bitpos,
						final Object key, final Object val) {
			final int idx = TUPLE_LENGTH * dataIndex(bitpos);

			final Object[] src = this.nodes;
			final Object[] dst = new Object[src.length + 2];

			// copy 'src' and insert 2 element(s) at position 'idx'
			System.arraycopy(src, 0, dst, 0, idx);
			dst[idx + 0] = key;
			dst[idx + 1] = val;
			System.arraycopy(src, idx, dst, idx + 2, src.length - idx);

			return nodeOf(mutator, nodeMap(), dataMap() | bitpos, dst);
		}

		@Override
		CompactMapNode copyAndRemoveValue(final AtomicReference<Thread> mutator, final int bitpos) {
			final int idx = TUPLE_LENGTH * dataIndex(bitpos);

			final Object[] src = this.nodes;
			final Object[] dst = new Object[src.length - 2];

			// copy 'src' and remove 2 element(s) at position 'idx'
			System.arraycopy(src, 0, dst, 0, idx);
			System.arraycopy(src, idx + 2, dst, idx, src.length - idx - 2);

			return nodeOf(mutator, nodeMap(), dataMap() ^ bitpos, dst);
		}

		@Override
		CompactMapNode copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode node) {

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

			return nodeOf(mutator, nodeMap() | bitpos, dataMap() ^ bitpos, dst);
		}

		@Override
		CompactMapNode copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode node) {

			final int idxOld = this.nodes.length - 1 - nodeIndex(bitpos);
			final int idxNew = TUPLE_LENGTH * dataIndex(bitpos);

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

			return nodeOf(mutator, nodeMap() ^ bitpos, dataMap() | bitpos, dst);
		}

	}

	private static final class HashCollisionMapNode_BleedingEdge extends CompactMapNode {
		private final Object[] keys;
		private final Object[] vals;
		private final int hash;

		HashCollisionMapNode_BleedingEdge(final int hash, final Object[] keys, final Object[] vals) {
			this.keys = keys;
			this.vals = vals;
			this.hash = hash;

			assert payloadArity() >= 2;
		}

		@Override
		boolean containsKey(final Object key, final int keyHash, final int shift) {
			if (this.hash == keyHash) {
				for (Object k : keys) {
					if (k.equals(key)) {
						return true;
					}
				}
			}
			return false;
		}

		@Override
		boolean containsKey(final Object key, final int keyHash, final int shift,
						final Comparator<Object> cmp) {
			if (this.hash == keyHash) {
				for (Object k : keys) {
					if (cmp.compare(k, key) == 0) {
						return true;
					}
				}
			}
			return false;
		}

		@Override
		Optional findByKey(final Object key, final int keyHash, final int shift) {
			for (int i = 0; i < keys.length; i++) {
				final Object _key = keys[i];
				if (key.equals(_key)) {
					final Object val = vals[i];
					return Optional.of(val);
				}
			}
			return Optional.empty();
		}

		@Override
		Optional findByKey(final Object key, final int keyHash, final int shift,
						final Comparator<Object> cmp) {
			for (int i = 0; i < keys.length; i++) {
				final Object _key = keys[i];
				if (cmp.compare(key, _key) == 0) {
					final Object val = vals[i];
					return Optional.of(val);
				}
			}
			return Optional.empty();
		}

		@Override
		CompactMapNode updated(final AtomicReference<Thread> mutator, final Object key,
						final Object val, final int keyHash, final int shift,
						final MapResult details) {
			assert this.hash == keyHash;

			for (int idx = 0; idx < keys.length; idx++) {
				if (keys[idx].equals(key)) {
					final Object currentVal = vals[idx];

					if (currentVal.equals(val)) {
						return this;
					} else {
						// add new mapping
						final Object[] src = this.vals;

						final Object[] dst = new Object[src.length];

						// copy 'src' and set 1 element(s) at position 'idx'
						System.arraycopy(src, 0, dst, 0, src.length);
						dst[idx + 0] = val;

						final CompactMapNode thisNew = new HashCollisionMapNode_BleedingEdge(
										this.hash, this.keys, dst);

						details.updated(currentVal);
						return thisNew;
					}
				}
			}

			final Object[] keysNew = new Object[this.keys.length + 1];

			// copy 'this.keys' and insert 1 element(s) at position
			// 'keys.length'
			System.arraycopy(this.keys, 0, keysNew, 0, keys.length);
			keysNew[keys.length + 0] = key;
			System.arraycopy(this.keys, keys.length, keysNew, keys.length + 1, this.keys.length
							- keys.length);

			final Object[] valsNew = new Object[this.vals.length + 1];

			// copy 'this.vals' and insert 1 element(s) at position
			// 'vals.length'
			System.arraycopy(this.vals, 0, valsNew, 0, vals.length);
			valsNew[vals.length + 0] = val;
			System.arraycopy(this.vals, vals.length, valsNew, vals.length + 1, this.vals.length
							- vals.length);

			details.modified();
			return new HashCollisionMapNode_BleedingEdge(keyHash, keysNew, valsNew);
		}

		@Override
		CompactMapNode updated(final AtomicReference<Thread> mutator, final Object key,
						final Object val, final int keyHash, final int shift,
						final MapResult details, final Comparator<Object> cmp) {
			assert this.hash == keyHash;

			for (int idx = 0; idx < keys.length; idx++) {
				if (cmp.compare(keys[idx], key) == 0) {
					final Object currentVal = vals[idx];

					if (cmp.compare(currentVal, val) == 0) {
						return this;
					} else {
						// add new mapping
						final Object[] src = this.vals;

						final Object[] dst = new Object[src.length];

						// copy 'src' and set 1 element(s) at position 'idx'
						System.arraycopy(src, 0, dst, 0, src.length);
						dst[idx + 0] = val;

						final CompactMapNode thisNew = new HashCollisionMapNode_BleedingEdge(
										this.hash, this.keys, dst);

						details.updated(currentVal);
						return thisNew;
					}
				}
			}

			final Object[] keysNew = new Object[this.keys.length + 1];

			// copy 'this.keys' and insert 1 element(s) at position
			// 'keys.length'
			System.arraycopy(this.keys, 0, keysNew, 0, keys.length);
			keysNew[keys.length + 0] = key;
			System.arraycopy(this.keys, keys.length, keysNew, keys.length + 1, this.keys.length
							- keys.length);

			final Object[] valsNew = new Object[this.vals.length + 1];

			// copy 'this.vals' and insert 1 element(s) at position
			// 'vals.length'
			System.arraycopy(this.vals, 0, valsNew, 0, vals.length);
			valsNew[vals.length + 0] = val;
			System.arraycopy(this.vals, vals.length, valsNew, vals.length + 1, this.vals.length
							- vals.length);

			details.modified();
			return new HashCollisionMapNode_BleedingEdge(keyHash, keysNew, valsNew);
		}

		@Override
		CompactMapNode removed(final AtomicReference<Thread> mutator, final Object key,
						final int keyHash, final int shift, final MapResult details) {
			for (int idx = 0; idx < keys.length; idx++) {
				if (keys[idx].equals(key)) {
					final Object currentVal = vals[idx];
					details.updated(currentVal);

					if (this.arity() == 1) {
						return nodeOf(mutator);
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node
						 * will be a) either be the new root returned, or b)
						 * unwrapped and inlined.
						 */
						final Object theOtherKey = (idx == 0) ? keys[1] : keys[0];
						final Object theOtherVal = (idx == 0) ? vals[1] : vals[0];
						return CompactMapNode.nodeOf(mutator).updated(mutator, theOtherKey,
										theOtherVal, keyHash, 0, details);
					} else {

						final Object[] keysNew = new Object[this.keys.length - 1];

						// copy 'this.keys' and remove 1 element(s) at position
						// 'idx'
						System.arraycopy(this.keys, 0, keysNew, 0, idx);
						System.arraycopy(this.keys, idx + 1, keysNew, idx, this.keys.length - idx
										- 1);

						final Object[] valsNew = new Object[this.vals.length - 1];

						// copy 'this.vals' and remove 1 element(s) at position
						// 'idx'
						System.arraycopy(this.vals, 0, valsNew, 0, idx);
						System.arraycopy(this.vals, idx + 1, valsNew, idx, this.vals.length - idx
										- 1);

						return new HashCollisionMapNode_BleedingEdge(keyHash, keysNew, valsNew);
					}
				}
			}
			return this;
		}

		@Override
		CompactMapNode removed(final AtomicReference<Thread> mutator, final Object key,
						final int keyHash, final int shift, final MapResult details,
						final Comparator<Object> cmp) {
			for (int idx = 0; idx < keys.length; idx++) {
				if (cmp.compare(keys[idx], key) == 0) {
					final Object currentVal = vals[idx];
					details.updated(currentVal);

					if (this.arity() == 1) {
						return nodeOf(mutator);
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node
						 * will be a) either be the new root returned, or b)
						 * unwrapped and inlined.
						 */
						final Object theOtherKey = (idx == 0) ? keys[1] : keys[0];
						final Object theOtherVal = (idx == 0) ? vals[1] : vals[0];
						return CompactMapNode.nodeOf(mutator).updated(mutator, theOtherKey,
										theOtherVal, keyHash, 0, details, cmp);
					} else {

						final Object[] keysNew = new Object[this.keys.length - 1];

						// copy 'this.keys' and remove 1 element(s) at position
						// 'idx'
						System.arraycopy(this.keys, 0, keysNew, 0, idx);
						System.arraycopy(this.keys, idx + 1, keysNew, idx, this.keys.length - idx
										- 1);

						final Object[] valsNew = new Object[this.vals.length - 1];

						// copy 'this.vals' and remove 1 element(s) at position
						// 'idx'
						System.arraycopy(this.vals, 0, valsNew, 0, idx);
						System.arraycopy(this.vals, idx + 1, valsNew, idx, this.vals.length - idx
										- 1);

						return new HashCollisionMapNode_BleedingEdge(keyHash, keysNew, valsNew);
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
		Object getKey(final int index) {
			return keys[index];
		}

		@Override
		Object getValue(final int index) {
			return vals[index];
		}

		@Override
		Map.Entry<Object, Object> getKeyValueEntry(final int index) {
			return entryOf(keys[index], vals[index]);
		}

		@Override
		public CompactMapNode getNode(int index) {
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

			HashCollisionMapNode_BleedingEdge that = (HashCollisionMapNode_BleedingEdge) other;

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
					final Object key = keys[j];
					final Object val = vals[j];

					if (key.equals(otherKey) && val.equals(otherVal)) {
						continue outerLoop;
					}
				}
				return false;
			}

			return true;
		}

		@Override
		CompactMapNode copyAndSetValue(final AtomicReference<Thread> mutator, final int bitpos,
						final Object val) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactMapNode copyAndInsertValue(final AtomicReference<Thread> mutator, final int bitpos,
						final Object key, final Object val) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactMapNode copyAndRemoveValue(final AtomicReference<Thread> mutator, final int bitpos) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactMapNode copyAndSetNode(final AtomicReference<Thread> mutator, final int bitpos,
						final CompactMapNode node) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactMapNode copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode node) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactMapNode copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator,
						final int bitpos, final CompactMapNode node) {
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

		@Override
		int rareMap() {
			throw new UnsupportedOperationException();
		}

	}

	/**
	 * Iterator skeleton that uses a fixed stack in depth.
	 */
	private static abstract class AbstractMapIterator {

		private static final int MAX_DEPTH = 7;

		protected int currentValueCursor;
		protected int currentValueLength;
		protected AbstractMapNode currentValueNode;

		private int currentStackLevel = -1;
		private final int[] nodeCursorsAndLengths = new int[MAX_DEPTH * 2];

		AbstractMapNode[] nodes = new AbstractMapNode[MAX_DEPTH];

		AbstractMapIterator(AbstractMapNode rootNode) {
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
					final AbstractMapNode nextNode = nodes[currentStackLevel].getNode(nodeCursor);
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

	protected static class MapKeyIterator extends AbstractMapIterator implements Iterator {

		MapKeyIterator(AbstractMapNode rootNode) {
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

	protected static class MapValueIterator extends AbstractMapIterator implements Iterator {

		MapValueIterator(AbstractMapNode rootNode) {
			super(rootNode);
		}

		@Override
		public Object next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			} else {
				return currentValueNode.getValue(currentValueCursor++);
			}
		}

	}

	protected static class MapEntryIterator extends AbstractMapIterator implements
					Iterator<Map.Entry<Object, Object>> {

		MapEntryIterator(AbstractMapNode rootNode) {
			super(rootNode);
		}

		@Override
		public Map.Entry<Object, Object> next() {
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
	private static class TrieMap_BleedingEdgeNodeIterator implements Iterator<AbstractMapNode> {

		final Deque<Iterator<? extends AbstractMapNode>> nodeIteratorStack;

		TrieMap_BleedingEdgeNodeIterator(AbstractMapNode rootNode) {
			nodeIteratorStack = new ArrayDeque();
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
		public AbstractMapNode next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			AbstractMapNode innerNode = nodeIteratorStack.peek().next();

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

	static final class TransientTrieMap_BleedingEdge implements TransientMap<Object, Object> {
		final private AtomicReference<Thread> mutator;
		private AbstractMapNode rootNode;
		private int hashCode;
		private int cachedSize;

		TransientTrieMap_BleedingEdge(TrieMap_Heterogeneous trieMap_BleedingEdge) {
			this.mutator = new AtomicReference<Thread>(Thread.currentThread());
			this.rootNode = trieMap_BleedingEdge.rootNode;
			this.hashCode = trieMap_BleedingEdge.hashCode;
			this.cachedSize = trieMap_BleedingEdge.cachedSize;
			if (DEBUG) {
				assert checkHashCodeAndSize(hashCode, cachedSize);
			}
		}

		private boolean checkHashCodeAndSize(final int targetHash, final int targetSize) {
			int hash = 0;
			int size = 0;

			for (Iterator<Map.Entry<Object, Object>> it = entryIterator(); it.hasNext();) {
				final Map.Entry<Object, Object> entry = it.next();
				final Object key = entry.getKey();
				final Object val = entry.getValue();

				hash += key.hashCode() ^ val.hashCode();
				size += 1;
			}

			return hash == targetHash && size == targetSize;
		}

		@Override
		public Object put(final Object key, final Object val) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void putAll(final Map<? extends Object, ? extends Object> m) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object remove(final Object key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean containsKey(final Object o) {
			try {

				final Object key = o;
				return rootNode.containsKey(key, transformHashCode(key.hashCode()), 0);
			} catch (ClassCastException unused) {
				return false;
			}
		}

		@Override
		public boolean containsKeyEquivalent(final Object o, final Comparator<Object> cmp) {
			try {

				final Object key = o;
				return rootNode.containsKey(key, transformHashCode(key.hashCode()), 0, cmp);
			} catch (ClassCastException unused) {
				return false;
			}
		}

		@Override
		public boolean containsValue(final Object o) {
			for (Iterator iterator = valueIterator(); iterator.hasNext();) {
				if (iterator.next().equals(o)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean containsValueEquivalent(final Object o, final Comparator<Object> cmp) {
			for (Iterator iterator = valueIterator(); iterator.hasNext();) {
				if (cmp.compare(iterator.next(), o) == 0) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Object get(final Object o) {
			try {

				final Object key = o;
				final Optional result = rootNode.findByKey(key, transformHashCode(key.hashCode()),
								0);

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
		public Object getEquivalent(final Object o, final Comparator<Object> cmp) {
			try {

				final Object key = o;
				final Optional result = rootNode.findByKey(key, transformHashCode(key.hashCode()),
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

		@Override
		public Object __put(final Object key, final Object val) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final MapResult details = MapResult.unchanged();

			final CompactMapNode newRootNode = rootNode.updated(mutator, key, val,
							transformHashCode(keyHash), 0, details);

			if (details.isModified()) {
				if (details.hasReplacedValue()) {
					final Object old = details.getReplacedValue();

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

		@Override
		public Object __putEquivalent(final Object key, final Object val,
						final Comparator<Object> cmp) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final MapResult details = MapResult.unchanged();

			final CompactMapNode newRootNode = rootNode.updated(mutator, key, val,
							transformHashCode(keyHash), 0, details, cmp);

			if (details.isModified()) {
				if (details.hasReplacedValue()) {
					final Object old = details.getReplacedValue();

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

		@Override
		public boolean __putAll(final Map<? extends Object, ? extends Object> map) {
			boolean modified = false;

			for (Map.Entry<? extends Object, ? extends Object> entry : map.entrySet()) {
				final boolean isPresent = this.containsKey(entry.getKey());
				final Object replaced = this.__put(entry.getKey(), entry.getValue());

				if (!isPresent || replaced != null) {
					modified = true;
				}
			}

			return modified;
		}

		@Override
		public boolean __putAllEquivalent(final Map<? extends Object, ? extends Object> map,
						final Comparator<Object> cmp) {
			boolean modified = false;

			for (Map.Entry<? extends Object, ? extends Object> entry : map.entrySet()) {
				final boolean isPresent = this.containsKeyEquivalent(entry.getKey(), cmp);
				final Object replaced = this.__putEquivalent(entry.getKey(), entry.getValue(), cmp);

				if (!isPresent || replaced != null) {
					modified = true;
				}
			}

			return modified;
		}

		@Override
		public Object __remove(final Object key) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final MapResult details = MapResult.unchanged();

			final CompactMapNode newRootNode = rootNode.removed(mutator, key,
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

		@Override
		public Object __removeEquivalent(final Object key, final Comparator<Object> cmp) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final MapResult details = MapResult.unchanged();

			final CompactMapNode newRootNode = rootNode.removed(mutator, key,
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

		@Override
		public int size() {
			return cachedSize;
		}

		@Override
		public boolean isEmpty() {
			return cachedSize == 0;
		}

		@Override
		public Iterator keyIterator() {
			return new TransientMapKeyIterator(this);
		}

		@Override
		public Iterator valueIterator() {
			return new TransientMapValueIterator(this);
		}

		@Override
		public Iterator<Map.Entry<Object, Object>> entryIterator() {
			return new TransientMapEntryIterator(this);
		}

		public static class TransientMapKeyIterator extends MapKeyIterator {
			final TransientTrieMap_BleedingEdge collection;
			Object lastKey;

			public TransientMapKeyIterator(final TransientTrieMap_BleedingEdge collection) {
				super(collection.rootNode);
				this.collection = collection;
			}

			@Override
			public Object next() {
				return lastKey = super.next();
			}

			@Override
			public void remove() {
				// TODO: test removal at iteration rigorously
				collection.__remove(lastKey);
			}
		}

		public static class TransientMapValueIterator extends MapValueIterator {
			final TransientTrieMap_BleedingEdge collection;

			public TransientMapValueIterator(final TransientTrieMap_BleedingEdge collection) {
				super(collection.rootNode);
				this.collection = collection;
			}

			@Override
			public Object next() {
				return super.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		}

		public static class TransientMapEntryIterator extends MapEntryIterator {
			final TransientTrieMap_BleedingEdge collection;

			public TransientMapEntryIterator(final TransientTrieMap_BleedingEdge collection) {
				super(collection.rootNode);
				this.collection = collection;
			}

			@Override
			public Map.Entry<Object, Object> next() {
				return super.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		}

		@Override
		public Set keySet() {
			Set keySet = null;

			if (keySet == null) {
				keySet = new AbstractSet() {
					@Override
					public Iterator iterator() {
						return TransientTrieMap_BleedingEdge.this.keyIterator();
					}

					@Override
					public int size() {
						return TransientTrieMap_BleedingEdge.this.size();
					}

					@Override
					public boolean isEmpty() {
						return TransientTrieMap_BleedingEdge.this.isEmpty();
					}

					@Override
					public void clear() {
						TransientTrieMap_BleedingEdge.this.clear();
					}

					@Override
					public boolean contains(Object k) {
						return TransientTrieMap_BleedingEdge.this.containsKey(k);
					}
				};
			}

			return keySet;
		}

		@Override
		public Collection values() {
			Collection values = null;

			if (values == null) {
				values = new AbstractCollection() {
					@Override
					public Iterator iterator() {
						return TransientTrieMap_BleedingEdge.this.valueIterator();
					}

					@Override
					public int size() {
						return TransientTrieMap_BleedingEdge.this.size();
					}

					@Override
					public boolean isEmpty() {
						return TransientTrieMap_BleedingEdge.this.isEmpty();
					}

					@Override
					public void clear() {
						TransientTrieMap_BleedingEdge.this.clear();
					}

					@Override
					public boolean contains(Object v) {
						return TransientTrieMap_BleedingEdge.this.containsValue(v);
					}
				};
			}

			return values;
		}

		@Override
		public Set<java.util.Map.Entry<Object, Object>> entrySet() {
			Set<java.util.Map.Entry<Object, Object>> entrySet = null;

			if (entrySet == null) {
				entrySet = new AbstractSet<java.util.Map.Entry<Object, Object>>() {
					@Override
					public Iterator<java.util.Map.Entry<Object, Object>> iterator() {
						return new Iterator<Map.Entry<Object, Object>>() {
							private final Iterator<Map.Entry<Object, Object>> i = entryIterator();

							@Override
							public boolean hasNext() {
								return i.hasNext();
							}

							@Override
							public Map.Entry<Object, Object> next() {
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
						return TransientTrieMap_BleedingEdge.this.size();
					}

					@Override
					public boolean isEmpty() {
						return TransientTrieMap_BleedingEdge.this.isEmpty();
					}

					@Override
					public void clear() {
						TransientTrieMap_BleedingEdge.this.clear();
					}

					@Override
					public boolean contains(Object k) {
						return TransientTrieMap_BleedingEdge.this.containsKey(k);
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

			if (other instanceof TransientTrieMap_BleedingEdge) {
				TransientTrieMap_BleedingEdge that = (TransientTrieMap_BleedingEdge) other;

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

				for (Iterator<Map.Entry<Object, Object>> it = that.entrySet().iterator(); it
								.hasNext();) {
					Map.Entry<Object, Object> entry = it.next();

					try {

						final Object key = entry.getKey();
						final Optional result = rootNode.findByKey(key,
										transformHashCode(key.hashCode()), 0);

						if (!result.isPresent()) {
							return false;
						} else {

							final Object val = entry.getValue();

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
		public ImmutableMap freeze() {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			mutator.set(null);
			return new TrieMap_Heterogeneous(rootNode, hashCode, cachedSize);
		}
	}

}