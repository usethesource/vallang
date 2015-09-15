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

import java.lang.reflect.Field;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@SuppressWarnings("rawtypes")
public class TrieMap_Heterogeneous_BleedingEdge implements ImmutableMap<Object, Object> {

	@SuppressWarnings("unchecked")
	private static final TrieMap_Heterogeneous_BleedingEdge EMPTY_MAP = new TrieMap_Heterogeneous_BleedingEdge(
			CompactMapNode.EMPTY_NODE, 0, 0);

	private static final boolean DEBUG = false;

	private final AbstractMapNode rootNode;
	private final int hashCode;
	private final int cachedSize;

	TrieMap_Heterogeneous_BleedingEdge(AbstractMapNode rootNode, int hashCode, int cachedSize) {
		this.rootNode = rootNode;
		this.hashCode = hashCode;
		this.cachedSize = cachedSize;
		if (DEBUG) {
			assert checkHashCodeAndSize(hashCode, cachedSize);
		}
	}

	@SuppressWarnings("unchecked")
	public static final ImmutableMap<Object, Object> of() {
		return TrieMap_Heterogeneous_BleedingEdge.EMPTY_MAP;
	}

	@SuppressWarnings("unchecked")
	public static final ImmutableMap<Object, Object> of(Object... keyValuePairs) {
		if (keyValuePairs.length % 2 != 0) {
			throw new IllegalArgumentException("Length of argument list is uneven: no key/value pairs.");
		}

		ImmutableMap<Object, Object> result = TrieMap_Heterogeneous_BleedingEdge.EMPTY_MAP;

		for (int i = 0; i < keyValuePairs.length; i += 2) {
			final Object key = (Object) keyValuePairs[i];
			final Object val = (Object) keyValuePairs[i + 1];

			result = result.__put(key, val);
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public static final TransientMap<Object, Object> transientOf() {
		return TrieMap_Heterogeneous_BleedingEdge.EMPTY_MAP.asTransient();
	}

	@SuppressWarnings("unchecked")
	public static final TransientMap<Object, Object> transientOf(Object... keyValuePairs) {
		if (keyValuePairs.length % 2 != 0) {
			throw new IllegalArgumentException("Length of argument list is uneven: no key/value pairs.");
		}

		final TransientMap<Object, Object> result = TrieMap_Heterogeneous_BleedingEdge.EMPTY_MAP.asTransient();

		for (int i = 0; i < keyValuePairs.length; i += 2) {
			final Object key = (Object) keyValuePairs[i];
			final Object val = (Object) keyValuePairs[i + 1];

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

	public boolean containsKey(final Object o) {
		try {
			@SuppressWarnings("unchecked")
			final Object key = (Object) o;
			return rootNode.containsKey(key, transformHashCode(key.hashCode()), 0);
		} catch (ClassCastException unused) {
			return false;
		}
	}

	public boolean containsKeyEquivalent(final Object o, final Comparator<Object> cmp) {
		try {
			@SuppressWarnings("unchecked")
			final Object key = (Object) o;
			return rootNode.containsKey(key, transformHashCode(key.hashCode()), 0, cmp);
		} catch (ClassCastException unused) {
			return false;
		}
	}

	public boolean containsValue(final Object o) {
		for (Iterator<Object> iterator = valueIterator(); iterator.hasNext();) {
			if (iterator.next().equals(o)) {
				return true;
			}
		}
		return false;
	}

	public boolean containsValueEquivalent(final Object o, final Comparator<Object> cmp) {
		for (Iterator<Object> iterator = valueIterator(); iterator.hasNext();) {
			if (cmp.compare(iterator.next(), o) == 0) {
				return true;
			}
		}
		return false;
	}

	public Object get(final Object o) {
		try {
			@SuppressWarnings("unchecked")
			final Object key = (Object) o;
			final Optional<Object> result = rootNode.findByKey(key, transformHashCode(key.hashCode()), 0);

			if (result.isPresent()) {
				return result.get();
			} else {
				return null;
			}
		} catch (ClassCastException unused) {
			return null;
		}
	}

	public Object getEquivalent(final Object o, final Comparator<Object> cmp) {
		try {
			@SuppressWarnings("unchecked")
			final Object key = (Object) o;
			final Optional<Object> result = rootNode.findByKey(key, transformHashCode(key.hashCode()), 0, cmp);

			if (result.isPresent()) {
				return result.get();
			} else {
				return null;
			}
		} catch (ClassCastException unused) {
			return null;
		}
	}

	public ImmutableMap<Object, Object> __put(final Object key, final Object val) {
		final int keyHash = key.hashCode();
		final MapResult details = MapResult.unchanged();

		final CompactMapNode newRootNode = rootNode.updated(null, key, val, transformHashCode(keyHash), 0, details);

		if (details.isModified()) {
			if (details.hasReplacedValue()) {
				final int valHashOld = details.getReplacedValue().hashCode();
				final int valHashNew = val.hashCode();

				return new TrieMap_Heterogeneous_BleedingEdge(newRootNode,
						hashCode + ((keyHash ^ valHashNew)) - ((keyHash ^ valHashOld)), cachedSize);
			}

			final int valHash = val.hashCode();
			return new TrieMap_Heterogeneous_BleedingEdge(newRootNode, hashCode + ((keyHash ^ valHash)),
					cachedSize + 1);
		}

		return this;
	}

	public ImmutableMap<Object, Object> __putEquivalent(final Object key, final Object val,
			final Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final MapResult details = MapResult.unchanged();

		final CompactMapNode newRootNode = rootNode.updated(null, key, val, transformHashCode(keyHash), 0, details,
				cmp);

		if (details.isModified()) {
			if (details.hasReplacedValue()) {
				final int valHashOld = details.getReplacedValue().hashCode();
				final int valHashNew = val.hashCode();

				return new TrieMap_Heterogeneous_BleedingEdge(newRootNode,
						hashCode + ((keyHash ^ valHashNew)) - ((keyHash ^ valHashOld)), cachedSize);
			}

			final int valHash = val.hashCode();
			return new TrieMap_Heterogeneous_BleedingEdge(newRootNode, hashCode + ((keyHash ^ valHash)),
					cachedSize + 1);
		}

		return this;
	}

	public ImmutableMap<Object, Object> __putAll(final Map<? extends Object, ? extends Object> map) {
		final TransientMap<Object, Object> tmpTransient = this.asTransient();
		tmpTransient.__putAll(map);
		return tmpTransient.freeze();
	}

	public ImmutableMap<Object, Object> __putAllEquivalent(final Map<? extends Object, ? extends Object> map,
			final Comparator<Object> cmp) {
		final TransientMap<Object, Object> tmpTransient = this.asTransient();
		tmpTransient.__putAllEquivalent(map, cmp);
		return tmpTransient.freeze();
	}

	public ImmutableMap<Object, Object> __remove(final Object key) {
		final int keyHash = key.hashCode();
		final MapResult details = MapResult.unchanged();

		final CompactMapNode newRootNode = rootNode.removed(null, key, transformHashCode(keyHash), 0, details);

		if (details.isModified()) {
			assert details.hasReplacedValue();
			final int valHash = details.getReplacedValue().hashCode();
			return new TrieMap_Heterogeneous_BleedingEdge(newRootNode, hashCode - ((keyHash ^ valHash)),
					cachedSize - 1);
		}

		return this;
	}

	public ImmutableMap<Object, Object> __removeEquivalent(final Object key, final Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final MapResult details = MapResult.unchanged();

		final CompactMapNode newRootNode = rootNode.removed(null, key, transformHashCode(keyHash), 0, details, cmp);

		if (details.isModified()) {
			assert details.hasReplacedValue();
			final int valHash = details.getReplacedValue().hashCode();
			return new TrieMap_Heterogeneous_BleedingEdge(newRootNode, hashCode - ((keyHash ^ valHash)),
					cachedSize - 1);
		}

		return this;
	}

	public Object put(final Object key, final Object val) {
		throw new UnsupportedOperationException();
	}

	public void putAll(final Map<? extends Object, ? extends Object> m) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public Object remove(final Object key) {
		throw new UnsupportedOperationException();
	}

	public int size() {
		return cachedSize;
	}

	public boolean isEmpty() {
		return cachedSize == 0;
	}

	public Iterator<Object> keyIterator() {
		return new MapKeyIterator(rootNode);
	}

	public Iterator<Object> valueIterator() {
		return new MapValueIterator(rootNode);
	}

	public Iterator<Map.Entry<Object, Object>> entryIterator() {
		return new MapEntryIterator(rootNode);
	}

	@Override
	public Set<Object> keySet() {
		Set<Object> keySet = null;

		if (keySet == null) {
			keySet = new AbstractSet<Object>() {
				@Override
				public Iterator<Object> iterator() {
					return TrieMap_Heterogeneous_BleedingEdge.this.keyIterator();
				}

				@Override
				public int size() {
					return TrieMap_Heterogeneous_BleedingEdge.this.size();
				}

				@Override
				public boolean isEmpty() {
					return TrieMap_Heterogeneous_BleedingEdge.this.isEmpty();
				}

				@Override
				public void clear() {
					TrieMap_Heterogeneous_BleedingEdge.this.clear();
				}

				@Override
				public boolean contains(Object k) {
					return TrieMap_Heterogeneous_BleedingEdge.this.containsKey(k);
				}
			};
		}

		return keySet;
	}

	@Override
	public Collection<Object> values() {
		Collection<Object> values = null;

		if (values == null) {
			values = new AbstractCollection<Object>() {
				@Override
				public Iterator<Object> iterator() {
					return TrieMap_Heterogeneous_BleedingEdge.this.valueIterator();
				}

				@Override
				public int size() {
					return TrieMap_Heterogeneous_BleedingEdge.this.size();
				}

				@Override
				public boolean isEmpty() {
					return TrieMap_Heterogeneous_BleedingEdge.this.isEmpty();
				}

				@Override
				public void clear() {
					TrieMap_Heterogeneous_BleedingEdge.this.clear();
				}

				@Override
				public boolean contains(Object v) {
					return TrieMap_Heterogeneous_BleedingEdge.this.containsValue(v);
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
					return TrieMap_Heterogeneous_BleedingEdge.this.size();
				}

				@Override
				public boolean isEmpty() {
					return TrieMap_Heterogeneous_BleedingEdge.this.isEmpty();
				}

				@Override
				public void clear() {
					TrieMap_Heterogeneous_BleedingEdge.this.clear();
				}

				@Override
				public boolean contains(Object k) {
					return TrieMap_Heterogeneous_BleedingEdge.this.containsKey(k);
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

		if (other instanceof TrieMap_Heterogeneous_BleedingEdge) {
			TrieMap_Heterogeneous_BleedingEdge that = (TrieMap_Heterogeneous_BleedingEdge) other;

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
					final Object key = (Object) entry.getKey();
					final Optional<Object> result = rootNode.findByKey(key, transformHashCode(key.hashCode()), 0);

					if (!result.isPresent()) {
						return false;
					} else {
						@SuppressWarnings("unchecked")
						final Object val = (Object) entry.getValue();

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
	public TransientMap<Object, Object> asTransient() {
		return new TransientTrieMap_Heterogeneous_BleedingEdge(this);
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
		return new TrieMap_Heterogeneous_BleedingEdgeNodeIterator(rootNode);
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
		final int[][] sumArityCombinations = new int[9][9];

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
		final int[] sumArity = new int[9];

		final int maxArity = 8; // TODO: factor out constant

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

		final int[] cumsumArity = new int[9];
		for (int cumsum = 0, i = 0; i < 9; i++) {
			cumsum += sumArity[i];
			cumsumArity[i] = cumsum;
		}

		final float threshhold = 0.01f; // for printing results
		for (int i = 0; i < 9; i++) {
			float arityPercentage = (float) (sumArity[i]) / sumNodes;
			float cumsumArityPercentage = (float) (cumsumArity[i]) / sumNodes;

			if (arityPercentage != 0 && arityPercentage >= threshhold) {
				// details per level
				StringBuilder bldr = new StringBuilder();
				int max = i;
				for (int j = 0; j <= max; j++) {
					for (int k = max - j; k <= max - j; k++) {
						float arityCombinationsPercentage = (float) (sumArityCombinations[j][k]) / sumNodes;

						if (arityCombinationsPercentage != 0 && arityCombinationsPercentage >= threshhold) {
							bldr.append(String.format("%d/%d: %s, ", j, k,
									new DecimalFormat("0.00%").format(arityCombinationsPercentage)));
						}
					}
				}
				final String detailPercentages = bldr.toString();

				// overview
				System.out.println(String.format("%2d: %s\t[cumsum = %s]\t%s", i,
						new DecimalFormat("0.00%").format(arityPercentage),
						new DecimalFormat("0.00%").format(cumsumArityPercentage), detailPercentages));
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

	protected static interface INode<K, V> {
	}

	protected static abstract class AbstractMapNode implements INode<Object, Object> {

		protected static final sun.misc.Unsafe unsafe;

		static {
			try {
				Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
				field.setAccessible(true);
				unsafe = (sun.misc.Unsafe) field.get(null);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		static final int TUPLE_LENGTH = 2;

		abstract boolean containsKey(final Object key, final int keyHash, final int shift);

		abstract boolean containsKey(final Object key, final int keyHash, final int shift,
				final Comparator<Object> cmp);

		abstract Optional<Object> findByKey(final Object key, final int keyHash, final int shift);

		abstract Optional<Object> findByKey(final Object key, final int keyHash, final int shift,
				final Comparator<Object> cmp);

		abstract CompactMapNode updated(final AtomicReference<Thread> mutator, final Object key, final Object val,
				final int keyHash, final int shift, final MapResult details);

		abstract CompactMapNode updated(final AtomicReference<Thread> mutator, final Object key, final Object val,
				final int keyHash, final int shift, final MapResult details, final Comparator<Object> cmp);

		abstract CompactMapNode removed(final AtomicReference<Thread> mutator, final Object key, final int keyHash,
				final int shift, final MapResult details);

		abstract CompactMapNode removed(final AtomicReference<Thread> mutator, final Object key, final int keyHash,
				final int shift, final MapResult details, final Comparator<Object> cmp);

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

		abstract Object getRareKey(final int index);

		abstract Object getRareValue(final int index);

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
			final Iterator<Object> it = new MapKeyIterator(this);

			int size = 0;
			while (it.hasNext()) {
				size += 1;
				it.next();
			}

			return size;
		}
	}

	private abstract static class CompactMapNode extends AbstractMapNode {

		static final Class[][] initializeSpecializationsByContentAndNodes() {
			Class[][] next = new Class[9][9];

			try {
				for (int m = 0; m <= 8; m++) {
					for (int n = 0; n <= 8; n++) {
						int mNext = m;
						int nNext = n;

						if (mNext < 0 || mNext > 8 || nNext < 0 || nNext > 8 || nNext + mNext > 8) {
							next[m][n] = null;
						} else {
							next[m][n] = Class.forName(String.format(
									"org.eclipse.imp.pdb.facts.util.TrieMap_Heterogeneous_BleedingEdge$Map%dTo%dNode_Heterogeneous_BleedingEdge",
									mNext, nNext));
						}
					}
				}
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}

			return next;
		}

		static final Class[][] specializationsByContentAndNodes = initializeSpecializationsByContentAndNodes();

		static final long globalNodeMapOffset = Map0To2Node_Heterogeneous_BleedingEdge.rawMap1Offset;

		static final long globalDataMapOffset = Map0To2Node_Heterogeneous_BleedingEdge.rawMap2Offset;

		static final long initializeGlobalArrayOffsetsOffset() {
			final Class<Map0To2Node_Heterogeneous_BleedingEdge> dstClass = Map0To2Node_Heterogeneous_BleedingEdge.class;

			try {
				return unsafe.staticFieldOffset(dstClass.getDeclaredField("arrayOffsets"));
			} catch (NoSuchFieldException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}

		static final long globalArrayOffsetsOffset = initializeGlobalArrayOffsetsOffset();

		static final long initializeArrayBase() {
			final Class<Map0To2Node_Heterogeneous_BleedingEdge> dstClass = Map0To2Node_Heterogeneous_BleedingEdge.class;

			try {
				final long[] dstArrayOffsets = (long[]) unsafe.getObject(dstClass, globalArrayOffsetsOffset);

				// assuems that both are of type Object and next to each other
				// in memory
				return dstArrayOffsets[0];
			} catch (SecurityException e) {
				throw new RuntimeException(e);
			}
		}

		static final long arrayBase = initializeArrayBase();

		static final long initializeAddressSize() {
			final Class<Map0To2Node_Heterogeneous_BleedingEdge> dstClass = Map0To2Node_Heterogeneous_BleedingEdge.class;

			try {
				final long[] dstArrayOffsets = (long[]) unsafe.getObject(dstClass, globalArrayOffsetsOffset);

				// assuems that both are of type Object and next to each other
				// in memory
				return dstArrayOffsets[1] - dstArrayOffsets[0];
			} catch (SecurityException e) {
				throw new RuntimeException(e);
			}
		}

		static final long addressSize = initializeAddressSize();

		static final int hashCodeLength() {
			return 32;
		}

		static final int bitPartitionSize() {
			return 3;
		}

		static final int bitPartitionMask() {
			return 0b111;
		}

		static final int mask(final int keyHash, final int shift) {
			return (keyHash >>> shift) & bitPartitionMask();
		}

		static final byte bitpos(final int mask) {
			return (byte) (1 << mask);
		}

		abstract byte nodeMap();

		abstract byte dataMap();

		abstract byte rareMap();

		abstract byte rawMap1();

		abstract byte rawMap2();

		static final boolean isRare(final Object o) {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		static final boolean isRare(final Object o0, final Object o1) {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		static final boolean isRare(final byte bitpos) {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		enum ContentType {
			KEY, VAL, RARE_KEY, RARE_VAL, NODE, SLOT
		}

		int logicalToPhysicalIndex(final ContentType type, final int index) {
			final int physicalIndex;

			switch (type) {
			case KEY:
				physicalIndex = TUPLE_LENGTH * index;
				break;
			case VAL:
				physicalIndex = TUPLE_LENGTH * index + 1;
				break;
			case RARE_KEY:
				physicalIndex = TUPLE_LENGTH * index + TUPLE_LENGTH * java.lang.Integer.bitCount(dataMap());
				break;
			case RARE_VAL:
				physicalIndex = TUPLE_LENGTH * index + TUPLE_LENGTH * java.lang.Integer.bitCount(dataMap()) + 1;
				break;
			case NODE:
				physicalIndex = slotArity() - 1 - index;
				break;
			case SLOT:
				physicalIndex = index;
				break;
			default:
				throw new IllegalStateException("Cases not exhausted?");
			}

			return physicalIndex;
		}

		static final byte sizeEmpty() {
			return 0b0;
		}

		static final byte sizeOne() {
			return 0b1;
		}

		static final byte sizeMoreThanOne() {
			return 0b10;
		}

		abstract byte sizePredicate();

		@Override

		CompactMapNode getNode(final int index) {
			try {
				final long[] arrayOffsets = (long[]) unsafe.getObject(this.getClass(),
						unsafe.staticFieldOffset(this.getClass().getDeclaredField("arrayOffsets")));
				return (CompactMapNode) unsafe.getObject(this,
						arrayOffsets[logicalToPhysicalIndex(ContentType.NODE, index)]);
			} catch (NoSuchFieldException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}

		boolean nodeInvariant() {
			boolean inv1 = (size() - payloadArity() >= 2 * (arity() - payloadArity()));
			boolean inv2 = (this.arity() == 0) ? sizePredicate() == sizeEmpty() : true;
			boolean inv3 = (this.arity() == 1 && payloadArity() == 1) ? sizePredicate() == sizeOne() : true;
			boolean inv4 = (this.arity() >= 2) ? sizePredicate() == sizeMoreThanOne() : true;
			boolean inv5 = (this.nodeArity() >= 0) && (this.payloadArity() >= 0)
					&& ((this.payloadArity() + this.nodeArity()) == this.arity());

			return inv1 && inv2 && inv3 && inv4 && inv5;
		}

		static final long[] arrayOffsets(final Class clazz, final String[] fieldNames) {
			try {
				long[] arrayOffsets = new long[fieldNames.length];

				for (int i = 0; i < fieldNames.length; i++) {
					arrayOffsets[i] = unsafe.objectFieldOffset(clazz.getDeclaredField(fieldNames[i]));
				}

				return arrayOffsets;
			} catch (NoSuchFieldException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}

		static final long bitmapOffset(final Class clazz, final String bitmapName) {
			try {
				List<Class> bottomUpHierarchy = new LinkedList<>();

				Class currentClass = clazz;
				while (currentClass != null) {
					bottomUpHierarchy.add(currentClass);
					currentClass = currentClass.getSuperclass();
				}

				final java.util.Optional<Field> bitmapNameField = bottomUpHierarchy.stream()
						.flatMap(hierarchyClass -> Stream.of(hierarchyClass.getDeclaredFields()))
						.filter(f -> f.getName().equals(bitmapName)).findFirst();

				if (bitmapNameField.isPresent()) {
					return unsafe.objectFieldOffset(bitmapNameField.get());
				} else {
					return sun.misc.Unsafe.INVALID_FIELD_OFFSET;
				}
			} catch (SecurityException e) {
				throw new RuntimeException(e);
			}
		}

		CompactMapNode copyAndSetValue(final AtomicReference<Thread> mutator, final byte bitpos, final Object val) {
			try {
				final int valIdx = dataIndex(bitpos);

				final Class srcClass = this.getClass();
				final Class dstClass = specializationsByContentAndNodes[payloadArity()][nodeArity()];

				if (dstClass == null) {
					throw new RuntimeException(
							String.format("[%s] No new specialization [payloadArity=%d, nodeArity=%d].",
									srcClass.getName(), payloadArity(), nodeArity()));
				}

				final CompactMapNode src = this;
				final CompactMapNode dst = (CompactMapNode) (unsafe.allocateInstance(dstClass));

				final long[] srcArrayOffsets = (long[]) unsafe.getObject(srcClass, globalArrayOffsetsOffset);

				final long[] dstArrayOffsets = (long[]) unsafe.getObject(dstClass, globalArrayOffsetsOffset);

				// copy and update bitmaps
				final byte newNodeMap = unsafe.getByte(src, globalNodeMapOffset);
				unsafe.putByte(dst, globalNodeMapOffset, newNodeMap);

				// copy and update bitmaps
				final byte newDataMap = unsafe.getByte(src, globalDataMapOffset);
				unsafe.putByte(dst, globalDataMapOffset, newDataMap);

				// copy payload range
				for (int i = 0; i < valIdx; i++) {
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.KEY, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.KEY, i)]));
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.VAL, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.VAL, i)]));
				}

				unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.KEY, valIdx)],
						unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.KEY, valIdx)]));
				unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.VAL, valIdx)], val);

				for (int i = valIdx + 1; i < payloadArity(); i++) {
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.KEY, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.KEY, i)]));
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.VAL, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.VAL, i)]));
				}

				// copy node range
				for (int i = 0; i < nodeArity(); i++) {
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.NODE, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.NODE, i)]));
				}

				return dst;
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			}
		}

		CompactMapNode copyAndInsertValue(final AtomicReference<Thread> mutator, final byte bitpos, final Object key,
				final Object val) {
			try {
				final int valIdx = dataIndex(bitpos);

				final Class srcClass = this.getClass();
				final Class dstClass = specializationsByContentAndNodes[payloadArity() + 1][nodeArity()];

				if (dstClass == null) {
					throw new RuntimeException(
							String.format("[%s] No new specialization [payloadArity=%d, nodeArity=%d].",
									srcClass.getName(), payloadArity(), nodeArity()));
				}

				final CompactMapNode src = this;
				final CompactMapNode dst = (CompactMapNode) (unsafe.allocateInstance(dstClass));

				final long[] srcArrayOffsets = (long[]) unsafe.getObject(srcClass, globalArrayOffsetsOffset);

				final long[] dstArrayOffsets = (long[]) unsafe.getObject(dstClass, globalArrayOffsetsOffset);

				// copy and update bitmaps
				final byte newNodeMap = unsafe.getByte(src, globalNodeMapOffset);
				unsafe.putByte(dst, globalNodeMapOffset, newNodeMap);

				// copy and update bitmaps
				final byte newDataMap = (byte) (unsafe.getByte(src, globalDataMapOffset) | bitpos);
				unsafe.putByte(dst, globalDataMapOffset, newDataMap);

				// copy payload range
				for (int i = 0; i < valIdx; i++) {
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.KEY, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.KEY, i)]));
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.VAL, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.VAL, i)]));
				}

				unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.KEY, valIdx)], key);
				unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.VAL, valIdx)], val);

				for (int i = valIdx; i < payloadArity(); i++) {
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.KEY, i + 1)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.KEY, i)]));
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.VAL, i + 1)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.VAL, i)]));
				}

				// copy node range
				for (int i = 0; i < nodeArity(); i++) {
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.NODE, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.NODE, i)]));
				}

				return dst;
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			}
		}

		CompactMapNode copyAndRemoveValue(final AtomicReference<Thread> mutator, final byte bitpos) {
			try {
				final int valIdx = dataIndex(bitpos);

				final Class srcClass = this.getClass();
				final Class dstClass = specializationsByContentAndNodes[payloadArity() - 1][nodeArity()];

				if (dstClass == null) {
					throw new RuntimeException(
							String.format("[%s] No new specialization [payloadArity=%d, nodeArity=%d].",
									srcClass.getName(), payloadArity(), nodeArity()));
				}

				final CompactMapNode src = this;
				final CompactMapNode dst = (CompactMapNode) (unsafe.allocateInstance(dstClass));

				final long[] srcArrayOffsets = (long[]) unsafe.getObject(srcClass, globalArrayOffsetsOffset);

				final long[] dstArrayOffsets = (long[]) unsafe.getObject(dstClass, globalArrayOffsetsOffset);

				// copy and update bitmaps
				final byte newNodeMap = unsafe.getByte(src, globalNodeMapOffset);
				unsafe.putByte(dst, globalNodeMapOffset, newNodeMap);

				// copy and update bitmaps
				final byte newDataMap = (byte) (unsafe.getByte(src, globalDataMapOffset) ^ bitpos);
				unsafe.putByte(dst, globalDataMapOffset, newDataMap);

				// copy payload range
				for (int i = 0; i < valIdx; i++) {
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.KEY, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.KEY, i)]));
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.VAL, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.VAL, i)]));
				}
				for (int i = valIdx + 1; i < payloadArity(); i++) {
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.KEY, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.KEY, i + 1)]));
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.VAL, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.VAL, i + 1)]));
				}

				// copy node range
				for (int i = 0; i < nodeArity(); i++) {
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.NODE, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.NODE, i)]));
				}

				return dst;
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			}
		}

		CompactMapNode copyAndSetNode(final AtomicReference<Thread> mutator, final byte bitpos,
				final CompactMapNode node) {
			try {
				final int idx = nodeIndex(bitpos);

				final Class srcClass = this.getClass();
				final Class dstClass = specializationsByContentAndNodes[payloadArity()][nodeArity()];

				if (dstClass == null) {
					throw new RuntimeException(
							String.format("[%s] No new specialization [payloadArity=%d, nodeArity=%d].",
									srcClass.getName(), payloadArity(), nodeArity()));
				}

				final CompactMapNode src = this;
				final CompactMapNode dst = (CompactMapNode) (unsafe.allocateInstance(dstClass));

				final long[] srcArrayOffsets = (long[]) unsafe.getObject(srcClass, globalArrayOffsetsOffset);

				final long[] dstArrayOffsets = (long[]) unsafe.getObject(dstClass, globalArrayOffsetsOffset);

				// copy and update bitmaps
				final byte newNodeMap = unsafe.getByte(src, globalNodeMapOffset);
				unsafe.putByte(dst, globalNodeMapOffset, newNodeMap);

				// copy and update bitmaps
				final byte newDataMap = unsafe.getByte(src, globalDataMapOffset);
				unsafe.putByte(dst, globalDataMapOffset, newDataMap);

				// copy payload range
				for (int i = 0; i < payloadArity(); i++) {
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.KEY, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.KEY, i)]));
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.VAL, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.VAL, i)]));
				}

				// copy node range
				for (int i = 0; i < idx; i++) {
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.NODE, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.NODE, i)]));
				}

				unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.NODE, idx)], node);

				for (int i = idx + 1; i < nodeArity(); i++) {
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.NODE, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.NODE, i)]));
				}

				return dst;
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			}
		}

		CompactMapNode copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator, final byte bitpos,
				final CompactMapNode node) {
			try {
				final int idxOld = dataIndex(bitpos);
				final int idxNew = nodeIndex(bitpos);

				final Class srcClass = this.getClass();
				final Class dstClass = specializationsByContentAndNodes[payloadArity() - 1][nodeArity() + 1];

				if (dstClass == null) {
					throw new RuntimeException(
							String.format("[%s] No new specialization [payloadArity=%d, nodeArity=%d].",
									srcClass.getName(), payloadArity(), nodeArity()));
				}

				final CompactMapNode src = this;
				final CompactMapNode dst = (CompactMapNode) (unsafe.allocateInstance(dstClass));

				final long[] srcArrayOffsets = (long[]) unsafe.getObject(srcClass, globalArrayOffsetsOffset);

				final long[] dstArrayOffsets = (long[]) unsafe.getObject(dstClass, globalArrayOffsetsOffset);

				// copy and update bitmaps
				final byte newNodeMap = (byte) (unsafe.getByte(src, globalNodeMapOffset) | bitpos);
				unsafe.putByte(dst, globalNodeMapOffset, newNodeMap);

				// copy and update bitmaps
				final byte newDataMap = (byte) (unsafe.getByte(src, globalDataMapOffset) ^ bitpos);
				unsafe.putByte(dst, globalDataMapOffset, newDataMap);

				// copy payload range
				for (int i = 0; i < idxOld; i++) {
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.KEY, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.KEY, i)]));
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.VAL, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.VAL, i)]));
				}

				for (int i = idxOld + 1; i < payloadArity(); i++) {
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.KEY, i - 1)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.KEY, i)]));
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.VAL, i - 1)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.VAL, i)]));
				}

				// copy node range
				for (int i = 0; i < idxNew; i++) {
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.NODE, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.NODE, i)]));
				}

				unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.NODE, idxNew)], node);

				for (int i = idxNew; i < nodeArity(); i++) {
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.NODE, i + 1)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.NODE, i)]));
				}

				return dst;
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			}
		}

		CompactMapNode copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator, final byte bitpos,
				final CompactMapNode node) {
			try {
				final int idxOld = nodeIndex(bitpos);
				final int idxNew = dataIndex(bitpos);

				final Class srcClass = this.getClass();
				final Class dstClass = specializationsByContentAndNodes[payloadArity() + 1][nodeArity() - 1];

				if (dstClass == null) {
					throw new RuntimeException(
							String.format("[%s] No new specialization [payloadArity=%d, nodeArity=%d].",
									srcClass.getName(), payloadArity(), nodeArity()));
				}

				final CompactMapNode src = this;
				final CompactMapNode dst = (CompactMapNode) (unsafe.allocateInstance(dstClass));

				final long[] srcArrayOffsets = (long[]) unsafe.getObject(srcClass, globalArrayOffsetsOffset);

				final long[] dstArrayOffsets = (long[]) unsafe.getObject(dstClass, globalArrayOffsetsOffset);

				// copy and update bitmaps
				final byte newNodeMap = (byte) (unsafe.getByte(src, globalNodeMapOffset) ^ bitpos);
				unsafe.putByte(dst, globalNodeMapOffset, newNodeMap);

				// copy and update bitmaps
				final byte newDataMap = (byte) (unsafe.getByte(src, globalDataMapOffset) | bitpos);
				unsafe.putByte(dst, globalDataMapOffset, newDataMap);

				// copy payload range
				for (int i = 0; i < idxNew; i++) {
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.KEY, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.KEY, i)]));
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.VAL, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.VAL, i)]));
				}

				unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.KEY, idxNew)],
						node.getKey(0));
				unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.VAL, idxNew)],
						node.getValue(0));

				for (int i = idxNew; i < payloadArity(); i++) {
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.KEY, i + 1)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.KEY, i)]));
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.VAL, i + 1)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.VAL, i)]));
				}

				// copy node range
				for (int i = 0; i < idxOld; i++) {
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.NODE, i)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.NODE, i)]));
				}
				for (int i = idxOld + 1; i < nodeArity(); i++) {
					unsafe.putObject(dst, dstArrayOffsets[dst.logicalToPhysicalIndex(ContentType.NODE, i - 1)],
							unsafe.getObject(src, srcArrayOffsets[src.logicalToPhysicalIndex(ContentType.NODE, i)]));
				}

				return dst;
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			}
		}

		static final CompactMapNode mergeTwoKeyValPairs(final Object key0, final Object val0, final int keyHash0,
				final Object key1, final Object val1, final int keyHash1, final int shift) {
			assert!(key0.equals(key1));

			if (shift >= hashCodeLength()) {
				// throw new IllegalStateException("Hash collision not yet
				// fixed.");
				return new HashCollisionMapNode_Heterogeneous_BleedingEdge(keyHash0,
						(Object[]) new Object[] { key0, key1 }, (Object[]) new Object[] { val0, val1 });
			}

			final int mask0 = mask(keyHash0, shift);
			final int mask1 = mask(keyHash1, shift);

			if (mask0 != mask1) {
				// both nodes fit on same level
				final byte dataMap = (byte) (bitpos(mask0) | bitpos(mask1));

				if (mask0 < mask1) {
					return nodeOf0x2(null, (byte) 0, dataMap, key0, val0, key1, val1);
				} else {
					return nodeOf0x2(null, (byte) 0, dataMap, key1, val1, key0, val0);
				}
			} else {
				final CompactMapNode node = mergeTwoKeyValPairs(key0, val0, keyHash0, key1, val1, keyHash1,
						shift + bitPartitionSize());
				// values fit on next level

				final byte nodeMap = bitpos(mask0);
				return nodeOf1x0(null, nodeMap, (byte) 0, node);
			}
		}

		protected static final CompactMapNode EMPTY_NODE = new Map0To0Node_Heterogeneous_BleedingEdge(null, (byte) 0,
				(byte) 0);

		static final int index(final byte bitmap, final byte bitpos) {
			return java.lang.Integer.bitCount((int) (bitmap & 0xFF) & (bitpos - 1));
		}

		static final int index(final byte bitmap, final int mask, final byte bitpos) {
			return ((int) (bitmap & 0xFF) == -1) ? mask : index(bitmap, bitpos);
		}

		int dataIndex(final byte bitpos) {
			return java.lang.Integer.bitCount((int) (dataMap() & 0xFF) & (bitpos - 1));
		}

		int nodeIndex(final byte bitpos) {
			return java.lang.Integer.bitCount((int) (nodeMap() & 0xFF) & (bitpos - 1));
		}

		int rareIndex(final byte bitpos) {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		CompactMapNode nodeAt(final byte bitpos) {
			return getNode(nodeIndex(bitpos));
		}

		static final byte recoverMask(int map, final byte i_th) {
			assert 1 <= i_th && i_th <= 8;

			byte cnt1 = 0;
			byte mask = 0;

			while (mask < 8) {
				if ((map & 0x01) == 0x01) {
					cnt1 += 1;

					if (cnt1 == i_th) {
						return mask;
					}
				}

				map = (byte) (map >> 1);
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
				bldr.append(
						String.format("@%d<#%d,#%d>", pos, Objects.hashCode(getKey(i)), Objects.hashCode(getValue(i))));
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

		static final CompactMapNode nodeOf1x0(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object slot0) {
			try {
				final Class<Map0To1Node_Heterogeneous_BleedingEdge> dstClass = Map0To1Node_Heterogeneous_BleedingEdge.class;

				final Map0To1Node_Heterogeneous_BleedingEdge dst = (Map0To1Node_Heterogeneous_BleedingEdge) (unsafe
						.allocateInstance(dstClass));

				unsafe.putByte(dst, globalNodeMapOffset, nodeMap);
				unsafe.putByte(dst, globalDataMapOffset, dataMap);

				// works in presence of padding
				long offset = arrayBase;
				unsafe.putObject(dst, offset, slot0);
				offset += addressSize;

				return dst;
			} catch (InstantiationException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}

		static final CompactMapNode nodeOf0x1(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1) {
			try {
				final Class<Map1To0Node_Heterogeneous_BleedingEdge> dstClass = Map1To0Node_Heterogeneous_BleedingEdge.class;

				final Map1To0Node_Heterogeneous_BleedingEdge dst = (Map1To0Node_Heterogeneous_BleedingEdge) (unsafe
						.allocateInstance(dstClass));

				unsafe.putByte(dst, globalNodeMapOffset, nodeMap);
				unsafe.putByte(dst, globalDataMapOffset, dataMap);

				// works in presence of padding
				long offset = arrayBase;
				unsafe.putObject(dst, offset, key1);
				offset += addressSize;
				unsafe.putObject(dst, offset, val1);
				offset += addressSize;

				return dst;
			} catch (InstantiationException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}

		static final CompactMapNode nodeOf0x2(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2) {
			try {
				final Class<Map2To0Node_Heterogeneous_BleedingEdge> dstClass = Map2To0Node_Heterogeneous_BleedingEdge.class;

				final Map2To0Node_Heterogeneous_BleedingEdge dst = (Map2To0Node_Heterogeneous_BleedingEdge) (unsafe
						.allocateInstance(dstClass));

				unsafe.putByte(dst, globalNodeMapOffset, nodeMap);
				unsafe.putByte(dst, globalDataMapOffset, dataMap);

				// works in presence of padding
				long offset = arrayBase;
				unsafe.putObject(dst, offset, key1);
				offset += addressSize;
				unsafe.putObject(dst, offset, val1);
				offset += addressSize;
				unsafe.putObject(dst, offset, key2);
				offset += addressSize;
				unsafe.putObject(dst, offset, val2);
				offset += addressSize;

				return dst;
			} catch (InstantiationException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}

		@Override

		Object getKey(final int index) {
			try {
				final long[] arrayOffsets = (long[]) unsafe.getObject(this.getClass(),
						unsafe.staticFieldOffset(this.getClass().getDeclaredField("arrayOffsets")));
				return (Object) unsafe.getObject(this, arrayOffsets[logicalToPhysicalIndex(ContentType.KEY, index)]);
			} catch (NoSuchFieldException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}

		@Override

		Map.Entry<Object, Object> getKeyValueEntry(final int index) {
			return entryOf(getKey(index), getValue(index));
		}

		@Override

		CompactMapNode updated(final AtomicReference<Thread> mutator, final Object key, final Object val,
				final int keyHash, final int shift, final MapResult details) {
			final int mask = mask(keyHash, shift);
			final byte bitpos = bitpos(mask);

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
							transformHashCode(currentKey.hashCode()), key, val, keyHash, shift + bitPartitionSize());

					details.modified();
					return copyAndMigrateFromInlineToNode(mutator, bitpos, subNodeNew);
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactMapNode subNode = nodeAt(bitpos);
				final CompactMapNode subNodeNew = subNode.updated(mutator, key, val, keyHash,
						shift + bitPartitionSize(), details);

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

		boolean containsKey(final Object key, final int keyHash, final int shift, final Comparator<Object> cmp) {
			final int mask = mask(keyHash, shift);
			final byte bitpos = bitpos(mask);

			final byte dataMap = dataMap();
			if ((dataMap & bitpos) != 0) {
				final int index = index(dataMap, mask, bitpos);
				return cmp.compare(getKey(index), key) == 0;
			}

			final byte nodeMap = nodeMap();
			if ((nodeMap & bitpos) != 0) {
				final int index = index(nodeMap, mask, bitpos);
				return getNode(index).containsKey(key, keyHash, shift + bitPartitionSize(), cmp);
			}

			return false;
		}

		@Override

		Optional<Object> findByKey(final Object key, final int keyHash, final int shift, final Comparator<Object> cmp) {
			final int mask = mask(keyHash, shift);
			final byte bitpos = bitpos(mask);

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

		Object getSlot(final int index) {
			try {
				final long[] arrayOffsets = (long[]) unsafe.getObject(this.getClass(),
						unsafe.staticFieldOffset(this.getClass().getDeclaredField("arrayOffsets")));
				return (Object) unsafe.getObject(this, arrayOffsets[logicalToPhysicalIndex(ContentType.SLOT, index)]);
			} catch (NoSuchFieldException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}

		@Override

		Optional<Object> findByKey(final Object key, final int keyHash, final int shift) {
			final int mask = mask(keyHash, shift);
			final byte bitpos = bitpos(mask);

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

		Object getRareValue(final int index) {
			try {
				final long[] arrayOffsets = (long[]) unsafe.getObject(this.getClass(),
						unsafe.staticFieldOffset(this.getClass().getDeclaredField("arrayOffsets")));
				return (Object) unsafe.getObject(this,
						arrayOffsets[logicalToPhysicalIndex(ContentType.RARE_VAL, index)]);
			} catch (NoSuchFieldException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}

		@Override

		Object getValue(final int index) {
			try {
				final long[] arrayOffsets = (long[]) unsafe.getObject(this.getClass(),
						unsafe.staticFieldOffset(this.getClass().getDeclaredField("arrayOffsets")));
				return (Object) unsafe.getObject(this, arrayOffsets[logicalToPhysicalIndex(ContentType.VAL, index)]);
			} catch (NoSuchFieldException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}

		@Override

		CompactMapNode removed(final AtomicReference<Thread> mutator, final Object key, final int keyHash,
				final int shift, final MapResult details, final Comparator<Object> cmp) {
			final int mask = mask(keyHash, shift);
			final byte bitpos = bitpos(mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int dataIndex = dataIndex(bitpos);

				if (cmp.compare(getKey(dataIndex), key) == 0) {
					final Object currentVal = getValue(dataIndex);
					details.updated(currentVal);

					if (this.payloadArity() == 2 && this.nodeArity() == 0) {
						/*
						 * Create new node with remaining pair. The new node
						 * will a) either become the new root returned, or b)
						 * unwrapped and inlined during returning.
						 */
						final byte newDataMap = (shift == 0) ? (byte) (dataMap() ^ bitpos) : bitpos(mask(keyHash, 0));

						return nodeOf0x1(mutator, (byte) (0), newDataMap, getKey(1 - dataIndex),
								getValue(1 - dataIndex));
					} else {
						return copyAndRemoveValue(mutator, bitpos);
					}
				} else {
					return this;
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactMapNode subNode = nodeAt(bitpos);
				final CompactMapNode subNodeNew = subNode.removed(mutator, key, keyHash, shift + bitPartitionSize(),
						details, cmp);

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

		@Override

		boolean containsKey(final Object key, final int keyHash, final int shift) {
			final int mask = mask(keyHash, shift);
			final byte bitpos = bitpos(mask);

			final byte dataMap = dataMap();
			if ((dataMap & bitpos) != 0) {
				final int index = index(dataMap, mask, bitpos);
				return getKey(index).equals(key);
			}

			final byte nodeMap = nodeMap();
			if ((nodeMap & bitpos) != 0) {
				final int index = index(nodeMap, mask, bitpos);
				return getNode(index).containsKey(key, keyHash, shift + bitPartitionSize());
			}

			return false;
		}

		@Override

		Object getRareKey(final int index) {
			try {
				final long[] arrayOffsets = (long[]) unsafe.getObject(this.getClass(),
						unsafe.staticFieldOffset(this.getClass().getDeclaredField("arrayOffsets")));
				return (Object) unsafe.getObject(this,
						arrayOffsets[logicalToPhysicalIndex(ContentType.RARE_KEY, index)]);
			} catch (NoSuchFieldException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}

		@Override

		CompactMapNode updated(final AtomicReference<Thread> mutator, final Object key, final Object val,
				final int keyHash, final int shift, final MapResult details, final Comparator<Object> cmp) {
			final int mask = mask(keyHash, shift);
			final byte bitpos = bitpos(mask);

			if ((dataMap() & bitpos) != 0) { // inplace value
				final int dataIndex = dataIndex(bitpos);
				final Object currentKey = getKey(dataIndex);

				if (cmp.compare(currentKey, key) == 0) {
					final Object currentVal = getValue(dataIndex);

					// update mapping
					details.updated(currentVal);
					return copyAndSetValue(mutator, bitpos, val);
				} else {
					final Object currentVal = getValue(dataIndex);
					final CompactMapNode subNodeNew = mergeTwoKeyValPairs(currentKey, currentVal,
							transformHashCode(currentKey.hashCode()), key, val, keyHash, shift + bitPartitionSize());

					details.modified();
					return copyAndMigrateFromInlineToNode(mutator, bitpos, subNodeNew);
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactMapNode subNode = nodeAt(bitpos);
				final CompactMapNode subNodeNew = subNode.updated(mutator, key, val, keyHash,
						shift + bitPartitionSize(), details, cmp);

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

		CompactMapNode removed(final AtomicReference<Thread> mutator, final Object key, final int keyHash,
				final int shift, final MapResult details) {
			final int mask = mask(keyHash, shift);
			final byte bitpos = bitpos(mask);

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
						final byte newDataMap = (shift == 0) ? (byte) (dataMap() ^ bitpos) : bitpos(mask(keyHash, 0));

						return nodeOf0x1(mutator, (byte) (0), newDataMap, getKey(1 - dataIndex),
								getValue(1 - dataIndex));
					} else {
						return copyAndRemoveValue(mutator, bitpos);
					}
				} else {
					return this;
				}
			} else if ((nodeMap() & bitpos) != 0) { // node (not value)
				final CompactMapNode subNode = nodeAt(bitpos);
				final CompactMapNode subNodeNew = subNode.removed(mutator, key, keyHash, shift + bitPartitionSize(),
						details);

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

	}

	private abstract static class CompactEmptyMapNode extends CompactMapNode {

		private CompactEmptyMapNode(final AtomicReference<Thread> mutator) {

		}

		@Override

		byte nodeMap() {
			return 0;
		}

		@Override

		byte dataMap() {
			return 0;
		}

	}

	private abstract static class CompactNodesOnlyMapNode extends CompactMapNode {

		private CompactNodesOnlyMapNode(final AtomicReference<Thread> mutator, final byte nodeMap) {
			this.nodeMap = nodeMap;
		}

		@Override

		byte nodeMap() {
			return nodeMap;
		}

		private final byte nodeMap;

		@Override

		byte dataMap() {
			return 0;
		}

	}

	private abstract static class CompactValuesOnlyMapNode extends CompactMapNode {

		private CompactValuesOnlyMapNode(final AtomicReference<Thread> mutator, final byte dataMap) {
			this.dataMap = dataMap;
		}

		@Override

		byte nodeMap() {
			return 0;
		}

		@Override

		byte dataMap() {
			return dataMap;
		}

		private final byte dataMap;

	}

	private abstract static class CompactMixedMapNode extends CompactMapNode {

		private CompactMixedMapNode(final AtomicReference<Thread> mutator, final byte nodeMap, final byte dataMap) {
			this.nodeMap = nodeMap;
			this.dataMap = dataMap;
		}

		@Override

		byte nodeMap() {
			return nodeMap;
		}

		private final byte nodeMap;

		@Override

		byte dataMap() {
			return dataMap;
		}

		private final byte dataMap;

	}

	static final class FeatureFlags {
		public static final long SUPPORTS_NOTHING = 0;
		public static final long SUPPORTS_NODES = 1 << 0;
		public static final long SUPPORTS_PAYLOAD = 1 << 1;
	}

	private abstract static class CompactHeterogeneousMapNode extends CompactMapNode {

		abstract byte rawMap1();

		abstract byte rawMap2();

		@Override

		byte nodeMap() {
			return (byte) (rawMap1() ^ rareMap());
		}

		@Override

		byte dataMap() {
			return (byte) (rawMap2() ^ rareMap());
		}

		@Override

		byte rareMap() {
			return (byte) (rawMap1() & rawMap2());
		}

	}

	private abstract static class CompactEmptyHeterogeneousMapNode extends CompactHeterogeneousMapNode {

		private CompactEmptyHeterogeneousMapNode(final AtomicReference<Thread> mutator) {

		}

		@Override

		byte rawMap1() {
			return 0;
		}

		@Override

		byte rawMap2() {
			return 0;
		}

	}

	private abstract static class CompactNodesOnlyHeterogeneousMapNode extends CompactHeterogeneousMapNode {

		private CompactNodesOnlyHeterogeneousMapNode(final AtomicReference<Thread> mutator, final byte rawMap1) {
			this.rawMap1 = rawMap1;
		}

		@Override

		byte rawMap1() {
			return rawMap1;
		}

		private final byte rawMap1;

		@Override

		byte rawMap2() {
			return 0;
		}

	}

	private abstract static class CompactValuesOnlyHeterogeneousMapNode extends CompactHeterogeneousMapNode {

		private CompactValuesOnlyHeterogeneousMapNode(final AtomicReference<Thread> mutator, final byte rawMap1,
				final byte rawMap2) {
			this.rawMap1 = rawMap1;
			this.rawMap2 = rawMap2;
		}

		@Override

		byte rawMap1() {
			return rawMap1;
		}

		private final byte rawMap1;

		@Override

		byte rawMap2() {
			return rawMap2;
		}

		private final byte rawMap2;

	}

	private abstract static class CompactMixedHeterogeneousMapNode extends CompactHeterogeneousMapNode {

		private CompactMixedHeterogeneousMapNode(final AtomicReference<Thread> mutator, final byte rawMap1,
				final byte rawMap2) {
			this.rawMap1 = rawMap1;
			this.rawMap2 = rawMap2;
		}

		@Override

		byte rawMap1() {
			return rawMap1;
		}

		private final byte rawMap1;

		@Override

		byte rawMap2() {
			return rawMap2;
		}

		private final byte rawMap2;

	}

	private static final class HashCollisionMapNode_Heterogeneous_BleedingEdge extends CompactMapNode {
		private final Object[] keys;
		private final Object[] vals;
		private final int hash;

		HashCollisionMapNode_Heterogeneous_BleedingEdge(final int hash, final Object[] keys, final Object[] vals) {
			this.keys = keys;
			this.vals = vals;
			this.hash = hash;

			assert payloadArity() >= 2;
		}

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

		boolean containsKey(final Object key, final int keyHash, final int shift, final Comparator<Object> cmp) {
			if (this.hash == keyHash) {
				for (Object k : keys) {
					if (cmp.compare(k, key) == 0) {
						return true;
					}
				}
			}
			return false;
		}

		Optional<Object> findByKey(final Object key, final int keyHash, final int shift) {
			for (int i = 0; i < keys.length; i++) {
				final Object _key = keys[i];
				if (key.equals(_key)) {
					final Object val = vals[i];
					return Optional.of(val);
				}
			}
			return Optional.empty();
		}

		Optional<Object> findByKey(final Object key, final int keyHash, final int shift, final Comparator<Object> cmp) {
			for (int i = 0; i < keys.length; i++) {
				final Object _key = keys[i];
				if (cmp.compare(key, _key) == 0) {
					final Object val = vals[i];
					return Optional.of(val);
				}
			}
			return Optional.empty();
		}

		CompactMapNode updated(final AtomicReference<Thread> mutator, final Object key, final Object val,
				final int keyHash, final int shift, final MapResult details) {
			assert this.hash == keyHash;

			for (int idx = 0; idx < keys.length; idx++) {
				if (keys[idx].equals(key)) {
					final Object currentVal = vals[idx];

					if (currentVal.equals(val)) {
						return this;
					} else {
						// add new mapping
						final Object[] src = this.vals;
						final Object[] dst = (Object[]) new Object[src.length];

						// copy 'src' and set 1 element(s) at position 'idx'
						System.arraycopy(src, 0, dst, 0, src.length);
						dst[idx + 0] = val;

						final CompactMapNode thisNew = new HashCollisionMapNode_Heterogeneous_BleedingEdge(this.hash,
								this.keys, dst);

						details.updated(currentVal);
						return thisNew;
					}
				}
			}

			final Object[] keysNew = (Object[]) new Object[this.keys.length + 1];

			// copy 'this.keys' and insert 1 element(s) at position
			// 'keys.length'
			System.arraycopy(this.keys, 0, keysNew, 0, keys.length);
			keysNew[keys.length + 0] = key;
			System.arraycopy(this.keys, keys.length, keysNew, keys.length + 1, this.keys.length - keys.length);

			final Object[] valsNew = (Object[]) new Object[this.vals.length + 1];

			// copy 'this.vals' and insert 1 element(s) at position
			// 'vals.length'
			System.arraycopy(this.vals, 0, valsNew, 0, vals.length);
			valsNew[vals.length + 0] = val;
			System.arraycopy(this.vals, vals.length, valsNew, vals.length + 1, this.vals.length - vals.length);

			details.modified();
			return new HashCollisionMapNode_Heterogeneous_BleedingEdge(keyHash, keysNew, valsNew);
		}

		CompactMapNode updated(final AtomicReference<Thread> mutator, final Object key, final Object val,
				final int keyHash, final int shift, final MapResult details, final Comparator<Object> cmp) {
			assert this.hash == keyHash;

			for (int idx = 0; idx < keys.length; idx++) {
				if (cmp.compare(keys[idx], key) == 0) {
					final Object currentVal = vals[idx];

					if (cmp.compare(currentVal, val) == 0) {
						return this;
					} else {
						// add new mapping
						final Object[] src = this.vals;
						final Object[] dst = (Object[]) new Object[src.length];

						// copy 'src' and set 1 element(s) at position 'idx'
						System.arraycopy(src, 0, dst, 0, src.length);
						dst[idx + 0] = val;

						final CompactMapNode thisNew = new HashCollisionMapNode_Heterogeneous_BleedingEdge(this.hash,
								this.keys, dst);

						details.updated(currentVal);
						return thisNew;
					}
				}
			}

			final Object[] keysNew = (Object[]) new Object[this.keys.length + 1];

			// copy 'this.keys' and insert 1 element(s) at position
			// 'keys.length'
			System.arraycopy(this.keys, 0, keysNew, 0, keys.length);
			keysNew[keys.length + 0] = key;
			System.arraycopy(this.keys, keys.length, keysNew, keys.length + 1, this.keys.length - keys.length);

			final Object[] valsNew = (Object[]) new Object[this.vals.length + 1];

			// copy 'this.vals' and insert 1 element(s) at position
			// 'vals.length'
			System.arraycopy(this.vals, 0, valsNew, 0, vals.length);
			valsNew[vals.length + 0] = val;
			System.arraycopy(this.vals, vals.length, valsNew, vals.length + 1, this.vals.length - vals.length);

			details.modified();
			return new HashCollisionMapNode_Heterogeneous_BleedingEdge(keyHash, keysNew, valsNew);
		}

		CompactMapNode removed(final AtomicReference<Thread> mutator, final Object key, final int keyHash,
				final int shift, final MapResult details) {
			for (int idx = 0; idx < keys.length; idx++) {
				if (keys[idx].equals(key)) {
					final Object currentVal = vals[idx];
					details.updated(currentVal);

					if (this.arity() == 1) {
						return EMPTY_NODE;
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node
						 * will be a) either be the new root returned, or b)
						 * unwrapped and inlined.
						 */
						final Object theOtherKey = (idx == 0) ? keys[1] : keys[0];
						final Object theOtherVal = (idx == 0) ? vals[1] : vals[0];
						return EMPTY_NODE.updated(mutator, theOtherKey, theOtherVal, keyHash, 0, details);
					} else {
						final Object[] keysNew = (Object[]) new Object[this.keys.length - 1];

						// copy 'this.keys' and remove 1 element(s) at position
						// 'idx'
						System.arraycopy(this.keys, 0, keysNew, 0, idx);
						System.arraycopy(this.keys, idx + 1, keysNew, idx, this.keys.length - idx - 1);

						final Object[] valsNew = (Object[]) new Object[this.vals.length - 1];

						// copy 'this.vals' and remove 1 element(s) at position
						// 'idx'
						System.arraycopy(this.vals, 0, valsNew, 0, idx);
						System.arraycopy(this.vals, idx + 1, valsNew, idx, this.vals.length - idx - 1);

						return new HashCollisionMapNode_Heterogeneous_BleedingEdge(keyHash, keysNew, valsNew);
					}
				}
			}
			return this;
		}

		CompactMapNode removed(final AtomicReference<Thread> mutator, final Object key, final int keyHash,
				final int shift, final MapResult details, final Comparator<Object> cmp) {
			for (int idx = 0; idx < keys.length; idx++) {
				if (cmp.compare(keys[idx], key) == 0) {
					final Object currentVal = vals[idx];
					details.updated(currentVal);

					if (this.arity() == 1) {
						return EMPTY_NODE;
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node
						 * will be a) either be the new root returned, or b)
						 * unwrapped and inlined.
						 */
						final Object theOtherKey = (idx == 0) ? keys[1] : keys[0];
						final Object theOtherVal = (idx == 0) ? vals[1] : vals[0];
						return EMPTY_NODE.updated(mutator, theOtherKey, theOtherVal, keyHash, 0, details, cmp);
					} else {
						final Object[] keysNew = (Object[]) new Object[this.keys.length - 1];

						// copy 'this.keys' and remove 1 element(s) at position
						// 'idx'
						System.arraycopy(this.keys, 0, keysNew, 0, idx);
						System.arraycopy(this.keys, idx + 1, keysNew, idx, this.keys.length - idx - 1);

						final Object[] valsNew = (Object[]) new Object[this.vals.length - 1];

						// copy 'this.vals' and remove 1 element(s) at position
						// 'idx'
						System.arraycopy(this.vals, 0, valsNew, 0, idx);
						System.arraycopy(this.vals, idx + 1, valsNew, idx, this.vals.length - idx - 1);

						return new HashCollisionMapNode_Heterogeneous_BleedingEdge(keyHash, keysNew, valsNew);
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
			return sizeMoreThanOne();
		}

		@Override
		Object getKey(final int index) {
			return keys[index];
		}

		@Override
		Object getValue(final int index) {
			return vals[index];
		}

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

			HashCollisionMapNode_Heterogeneous_BleedingEdge that = (HashCollisionMapNode_Heterogeneous_BleedingEdge) other;

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
		CompactMapNode copyAndSetValue(final AtomicReference<Thread> mutator, final byte bitpos, final Object val) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactMapNode copyAndInsertValue(final AtomicReference<Thread> mutator, final byte bitpos, final Object key,
				final Object val) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactMapNode copyAndRemoveValue(final AtomicReference<Thread> mutator, final byte bitpos) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactMapNode copyAndSetNode(final AtomicReference<Thread> mutator, final byte bitpos,
				final CompactMapNode node) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactMapNode copyAndMigrateFromInlineToNode(final AtomicReference<Thread> mutator, final byte bitpos,
				final CompactMapNode node) {
			throw new UnsupportedOperationException();
		}

		@Override
		CompactMapNode copyAndMigrateFromNodeToInline(final AtomicReference<Thread> mutator, final byte bitpos,
				final CompactMapNode node) {
			throw new UnsupportedOperationException();
		}

		@Override
		byte nodeMap() {
			throw new UnsupportedOperationException();
		}

		@Override
		byte dataMap() {
			throw new UnsupportedOperationException();
		}

		@Override
		byte rareMap() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		byte rawMap1() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		byte rawMap2() {
			// TODO Auto-generated method stub
			return 0;
		}

	}

	/**
	 * Iterator skeleton that uses a fixed stack in depth.
	 */
	private static abstract class AbstractMapIterator {

		private static final int MAX_DEPTH = 11;

		protected int currentValueCursor;
		protected int currentValueLength;
		protected AbstractMapNode currentValueNode;

		private int currentStackLevel = -1;
		private final int[] nodeCursorsAndLengths = new int[MAX_DEPTH * 2];

		@SuppressWarnings("unchecked")
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

	protected static class MapKeyIterator extends AbstractMapIterator implements Iterator<Object> {

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

	protected static class MapValueIterator extends AbstractMapIterator implements Iterator<Object> {

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

	protected static class MapEntryIterator extends AbstractMapIterator implements Iterator<Map.Entry<Object, Object>> {

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
	private static class TrieMap_Heterogeneous_BleedingEdgeNodeIterator implements Iterator<AbstractMapNode> {

		final Deque<Iterator<? extends AbstractMapNode>> nodeIteratorStack;

		TrieMap_Heterogeneous_BleedingEdgeNodeIterator(AbstractMapNode rootNode) {
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

	static final class TransientTrieMap_Heterogeneous_BleedingEdge implements TransientMap<Object, Object> {
		final private AtomicReference<Thread> mutator;
		private AbstractMapNode rootNode;
		private int hashCode;
		private int cachedSize;

		TransientTrieMap_Heterogeneous_BleedingEdge(
				TrieMap_Heterogeneous_BleedingEdge trieMap_Heterogeneous_BleedingEdge) {
			this.mutator = new AtomicReference<Thread>(Thread.currentThread());
			this.rootNode = trieMap_Heterogeneous_BleedingEdge.rootNode;
			this.hashCode = trieMap_Heterogeneous_BleedingEdge.hashCode;
			this.cachedSize = trieMap_Heterogeneous_BleedingEdge.cachedSize;
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

		public Object put(final Object key, final Object val) {
			throw new UnsupportedOperationException();
		}

		public void putAll(final Map<? extends Object, ? extends Object> m) {
			throw new UnsupportedOperationException();
		}

		public void clear() {
			throw new UnsupportedOperationException();
		}

		public Object remove(final Object key) {
			throw new UnsupportedOperationException();
		}

		public boolean containsKey(final Object o) {
			try {
				@SuppressWarnings("unchecked")
				final Object key = (Object) o;
				return rootNode.containsKey(key, transformHashCode(key.hashCode()), 0);
			} catch (ClassCastException unused) {
				return false;
			}
		}

		public boolean containsKeyEquivalent(final Object o, final Comparator<Object> cmp) {
			try {
				@SuppressWarnings("unchecked")
				final Object key = (Object) o;
				return rootNode.containsKey(key, transformHashCode(key.hashCode()), 0, cmp);
			} catch (ClassCastException unused) {
				return false;
			}
		}

		public boolean containsValue(final Object o) {
			for (Iterator<Object> iterator = valueIterator(); iterator.hasNext();) {
				if (iterator.next().equals(o)) {
					return true;
				}
			}
			return false;
		}

		public boolean containsValueEquivalent(final Object o, final Comparator<Object> cmp) {
			for (Iterator<Object> iterator = valueIterator(); iterator.hasNext();) {
				if (cmp.compare(iterator.next(), o) == 0) {
					return true;
				}
			}
			return false;
		}

		public Object get(final Object o) {
			try {
				@SuppressWarnings("unchecked")
				final Object key = (Object) o;
				final Optional<Object> result = rootNode.findByKey(key, transformHashCode(key.hashCode()), 0);

				if (result.isPresent()) {
					return result.get();
				} else {
					return null;
				}
			} catch (ClassCastException unused) {
				return null;
			}
		}

		public Object getEquivalent(final Object o, final Comparator<Object> cmp) {
			try {
				@SuppressWarnings("unchecked")
				final Object key = (Object) o;
				final Optional<Object> result = rootNode.findByKey(key, transformHashCode(key.hashCode()), 0, cmp);

				if (result.isPresent()) {
					return result.get();
				} else {
					return null;
				}
			} catch (ClassCastException unused) {
				return null;
			}
		}

		public Object __put(final Object key, final Object val) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final MapResult details = MapResult.unchanged();

			final CompactMapNode newRootNode = rootNode.updated(mutator, key, val, transformHashCode(keyHash), 0,
					details);

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

		public Object __putEquivalent(final Object key, final Object val, final Comparator<Object> cmp) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final MapResult details = MapResult.unchanged();

			final CompactMapNode newRootNode = rootNode.updated(mutator, key, val, transformHashCode(keyHash), 0,
					details, cmp);

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

		public Object __remove(final Object key) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final MapResult details = MapResult.unchanged();

			final CompactMapNode newRootNode = rootNode.removed(mutator, key, transformHashCode(keyHash), 0, details);

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

		public Object __removeEquivalent(final Object key, final Comparator<Object> cmp) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final MapResult details = MapResult.unchanged();

			final CompactMapNode newRootNode = rootNode.removed(mutator, key, transformHashCode(keyHash), 0, details,
					cmp);

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

		public Iterator<Object> keyIterator() {
			return new TransientMapKeyIterator(this);
		}

		public Iterator<Object> valueIterator() {
			return new TransientMapValueIterator(this);
		}

		public Iterator<Map.Entry<Object, Object>> entryIterator() {
			return new TransientMapEntryIterator(this);
		}

		public static class TransientMapKeyIterator extends MapKeyIterator {
			final TransientTrieMap_Heterogeneous_BleedingEdge collection;
			Object lastKey;

			public TransientMapKeyIterator(final TransientTrieMap_Heterogeneous_BleedingEdge collection) {
				super(collection.rootNode);
				this.collection = collection;
			}

			public Object next() {
				return lastKey = super.next();
			}

			public void remove() {
				// TODO: test removal at iteration rigorously
				collection.__remove(lastKey);
			}
		}

		public static class TransientMapValueIterator extends MapValueIterator {
			final TransientTrieMap_Heterogeneous_BleedingEdge collection;

			public TransientMapValueIterator(final TransientTrieMap_Heterogeneous_BleedingEdge collection) {
				super(collection.rootNode);
				this.collection = collection;
			}

			public Object next() {
				return super.next();
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		}

		public static class TransientMapEntryIterator extends MapEntryIterator {
			final TransientTrieMap_Heterogeneous_BleedingEdge collection;

			public TransientMapEntryIterator(final TransientTrieMap_Heterogeneous_BleedingEdge collection) {
				super(collection.rootNode);
				this.collection = collection;
			}

			public Map.Entry<Object, Object> next() {
				return super.next();
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		}

		@Override
		public Set<Object> keySet() {
			Set<Object> keySet = null;

			if (keySet == null) {
				keySet = new AbstractSet<Object>() {
					@Override
					public Iterator<Object> iterator() {
						return TransientTrieMap_Heterogeneous_BleedingEdge.this.keyIterator();
					}

					@Override
					public int size() {
						return TransientTrieMap_Heterogeneous_BleedingEdge.this.size();
					}

					@Override
					public boolean isEmpty() {
						return TransientTrieMap_Heterogeneous_BleedingEdge.this.isEmpty();
					}

					@Override
					public void clear() {
						TransientTrieMap_Heterogeneous_BleedingEdge.this.clear();
					}

					@Override
					public boolean contains(Object k) {
						return TransientTrieMap_Heterogeneous_BleedingEdge.this.containsKey(k);
					}
				};
			}

			return keySet;
		}

		@Override
		public Collection<Object> values() {
			Collection<Object> values = null;

			if (values == null) {
				values = new AbstractCollection<Object>() {
					@Override
					public Iterator<Object> iterator() {
						return TransientTrieMap_Heterogeneous_BleedingEdge.this.valueIterator();
					}

					@Override
					public int size() {
						return TransientTrieMap_Heterogeneous_BleedingEdge.this.size();
					}

					@Override
					public boolean isEmpty() {
						return TransientTrieMap_Heterogeneous_BleedingEdge.this.isEmpty();
					}

					@Override
					public void clear() {
						TransientTrieMap_Heterogeneous_BleedingEdge.this.clear();
					}

					@Override
					public boolean contains(Object v) {
						return TransientTrieMap_Heterogeneous_BleedingEdge.this.containsValue(v);
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
						return TransientTrieMap_Heterogeneous_BleedingEdge.this.size();
					}

					@Override
					public boolean isEmpty() {
						return TransientTrieMap_Heterogeneous_BleedingEdge.this.isEmpty();
					}

					@Override
					public void clear() {
						TransientTrieMap_Heterogeneous_BleedingEdge.this.clear();
					}

					@Override
					public boolean contains(Object k) {
						return TransientTrieMap_Heterogeneous_BleedingEdge.this.containsKey(k);
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

			if (other instanceof TransientTrieMap_Heterogeneous_BleedingEdge) {
				TransientTrieMap_Heterogeneous_BleedingEdge that = (TransientTrieMap_Heterogeneous_BleedingEdge) other;

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
						final Object key = (Object) entry.getKey();
						final Optional<Object> result = rootNode.findByKey(key, transformHashCode(key.hashCode()), 0);

						if (!result.isPresent()) {
							return false;
						} else {
							@SuppressWarnings("unchecked")
							final Object val = (Object) entry.getValue();

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
		public ImmutableMap<Object, Object> freeze() {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			mutator.set(null);
			return new TrieMap_Heterogeneous_BleedingEdge(rootNode, hashCode, cachedSize);
		}
	}

	private static class Map0To0Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map0To0Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map0To0Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map0To0Node_Heterogeneous_BleedingEdge.class, new String[] {});

		private Map0To0Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap) {
			super(mutator, nodeMap, dataMap);
		}

		@Override

		int payloadArity() {
			return 0;
		}

		@Override

		boolean hasNodes() {
			return false;
		}

		@Override

		boolean hasSlots() {
			return false;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 0;
		}

		@Override

		int slotArity() {
			return 0;
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

		@Override

		boolean hasPayload() {
			return false;
		}

		@Override

		byte sizePredicate() {
			return sizeEmpty();
		}

	}

	private static class Map0To1Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map0To1Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map0To1Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map0To1Node_Heterogeneous_BleedingEdge.class,
				new String[] { "slot0" });

		private final Object slot0;

		private Map0To1Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object slot0) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
		}

		@Override

		int payloadArity() {
			return 0;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 1;
		}

		@Override

		int slotArity() {
			return 1;
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
			Map0To1Node_Heterogeneous_BleedingEdge that = (Map0To1Node_Heterogeneous_BleedingEdge) other;

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

		@Override

		boolean hasPayload() {
			return false;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map0To2Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map0To2Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map0To2Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map0To2Node_Heterogeneous_BleedingEdge.class,
				new String[] { "slot0", "slot1" });

		private final Object slot0;

		private final Object slot1;

		private Map0To2Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object slot0, final Object slot1) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
		}

		@Override

		int payloadArity() {
			return 0;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 2;
		}

		@Override

		int slotArity() {
			return 2;
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
			Map0To2Node_Heterogeneous_BleedingEdge that = (Map0To2Node_Heterogeneous_BleedingEdge) other;

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

		@Override

		boolean hasPayload() {
			return false;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map0To3Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map0To3Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map0To3Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map0To3Node_Heterogeneous_BleedingEdge.class,
				new String[] { "slot0", "slot1", "slot2" });

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private Map0To3Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object slot0, final Object slot1, final Object slot2) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
		}

		@Override

		int payloadArity() {
			return 0;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 3;
		}

		@Override

		int slotArity() {
			return 3;
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
			Map0To3Node_Heterogeneous_BleedingEdge that = (Map0To3Node_Heterogeneous_BleedingEdge) other;

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

		@Override

		boolean hasPayload() {
			return false;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map0To4Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map0To4Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map0To4Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map0To4Node_Heterogeneous_BleedingEdge.class,
				new String[] { "slot0", "slot1", "slot2", "slot3" });

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private final Object slot3;

		private Map0To4Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object slot0, final Object slot1, final Object slot2, final Object slot3) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
		}

		@Override

		int payloadArity() {
			return 0;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 4;
		}

		@Override

		int slotArity() {
			return 4;
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
			Map0To4Node_Heterogeneous_BleedingEdge that = (Map0To4Node_Heterogeneous_BleedingEdge) other;

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

		@Override

		boolean hasPayload() {
			return false;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map0To5Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map0To5Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map0To5Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map0To5Node_Heterogeneous_BleedingEdge.class,
				new String[] { "slot0", "slot1", "slot2", "slot3", "slot4" });

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private final Object slot3;

		private final Object slot4;

		private Map0To5Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object slot0, final Object slot1, final Object slot2, final Object slot3,
				final Object slot4) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
		}

		@Override

		int payloadArity() {
			return 0;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 5;
		}

		@Override

		int slotArity() {
			return 5;
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
			Map0To5Node_Heterogeneous_BleedingEdge that = (Map0To5Node_Heterogeneous_BleedingEdge) other;

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

		@Override

		boolean hasPayload() {
			return false;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map0To6Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map0To6Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map0To6Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map0To6Node_Heterogeneous_BleedingEdge.class,
				new String[] { "slot0", "slot1", "slot2", "slot3", "slot4", "slot5" });

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private final Object slot3;

		private final Object slot4;

		private final Object slot5;

		private Map0To6Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object slot0, final Object slot1, final Object slot2, final Object slot3,
				final Object slot4, final Object slot5) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
			this.slot5 = slot5;
		}

		@Override

		int payloadArity() {
			return 0;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 6;
		}

		@Override

		int slotArity() {
			return 6;
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
			Map0To6Node_Heterogeneous_BleedingEdge that = (Map0To6Node_Heterogeneous_BleedingEdge) other;

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

		@Override

		boolean hasPayload() {
			return false;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map0To7Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map0To7Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map0To7Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map0To7Node_Heterogeneous_BleedingEdge.class,
				new String[] { "slot0", "slot1", "slot2", "slot3", "slot4", "slot5", "slot6" });

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private final Object slot3;

		private final Object slot4;

		private final Object slot5;

		private final Object slot6;

		private Map0To7Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object slot0, final Object slot1, final Object slot2, final Object slot3,
				final Object slot4, final Object slot5, final Object slot6) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
			this.slot5 = slot5;
			this.slot6 = slot6;
		}

		@Override

		int payloadArity() {
			return 0;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 7;
		}

		@Override

		int slotArity() {
			return 7;
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
			Map0To7Node_Heterogeneous_BleedingEdge that = (Map0To7Node_Heterogeneous_BleedingEdge) other;

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

		@Override

		boolean hasPayload() {
			return false;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map0To8Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map0To8Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map0To8Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map0To8Node_Heterogeneous_BleedingEdge.class,
				new String[] { "slot0", "slot1", "slot2", "slot3", "slot4", "slot5", "slot6", "slot7" });

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private final Object slot3;

		private final Object slot4;

		private final Object slot5;

		private final Object slot6;

		private final Object slot7;

		private Map0To8Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object slot0, final Object slot1, final Object slot2, final Object slot3,
				final Object slot4, final Object slot5, final Object slot6, final Object slot7) {
			super(mutator, nodeMap, dataMap);
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
			this.slot5 = slot5;
			this.slot6 = slot6;
			this.slot7 = slot7;
		}

		@Override

		int payloadArity() {
			return 0;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 8;
		}

		@Override

		int slotArity() {
			return 8;
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
			Map0To8Node_Heterogeneous_BleedingEdge that = (Map0To8Node_Heterogeneous_BleedingEdge) other;

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

		@Override

		boolean hasPayload() {
			return false;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map1To0Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map1To0Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map1To0Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map1To0Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1" });

		private final Object key1;

		private final Object val1;

		private Map1To0Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
		}

		@Override

		int payloadArity() {
			return 1;
		}

		@Override

		boolean hasNodes() {
			return false;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 0;
		}

		@Override

		int slotArity() {
			return 2;
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
			Map1To0Node_Heterogeneous_BleedingEdge that = (Map1To0Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}

			return true;
		}

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeOne();
		}

	}

	private static class Map1To1Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map1To1Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map1To1Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map1To1Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "slot0" });

		private final Object key1;

		private final Object val1;

		private final Object slot0;

		private Map1To1Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object slot0) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.slot0 = slot0;
		}

		@Override

		int payloadArity() {
			return 1;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 1;
		}

		@Override

		int slotArity() {
			return 3;
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
			Map1To1Node_Heterogeneous_BleedingEdge that = (Map1To1Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(slot0.equals(that.slot0))) {
				return false;
			}

			return true;
		}

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map1To2Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map1To2Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map1To2Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map1To2Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "slot0", "slot1" });

		private final Object key1;

		private final Object val1;

		private final Object slot0;

		private final Object slot1;

		private Map1To2Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object slot0, final Object slot1) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.slot0 = slot0;
			this.slot1 = slot1;
		}

		@Override

		int payloadArity() {
			return 1;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 2;
		}

		@Override

		int slotArity() {
			return 4;
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
			Map1To2Node_Heterogeneous_BleedingEdge that = (Map1To2Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map1To3Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map1To3Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map1To3Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map1To3Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "slot0", "slot1", "slot2" });

		private final Object key1;

		private final Object val1;

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private Map1To3Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object slot0, final Object slot1,
				final Object slot2) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
		}

		@Override

		int payloadArity() {
			return 1;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 3;
		}

		@Override

		int slotArity() {
			return 5;
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
			Map1To3Node_Heterogeneous_BleedingEdge that = (Map1To3Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map1To4Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map1To4Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map1To4Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map1To4Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "slot0", "slot1", "slot2", "slot3" });

		private final Object key1;

		private final Object val1;

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private final Object slot3;

		private Map1To4Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object slot0, final Object slot1,
				final Object slot2, final Object slot3) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
		}

		@Override

		int payloadArity() {
			return 1;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 4;
		}

		@Override

		int slotArity() {
			return 6;
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
			Map1To4Node_Heterogeneous_BleedingEdge that = (Map1To4Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map1To5Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map1To5Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map1To5Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map1To5Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "slot0", "slot1", "slot2", "slot3", "slot4" });

		private final Object key1;

		private final Object val1;

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private final Object slot3;

		private final Object slot4;

		private Map1To5Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object slot0, final Object slot1,
				final Object slot2, final Object slot3, final Object slot4) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
		}

		@Override

		int payloadArity() {
			return 1;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 5;
		}

		@Override

		int slotArity() {
			return 7;
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
			Map1To5Node_Heterogeneous_BleedingEdge that = (Map1To5Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map1To6Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map1To6Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map1To6Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map1To6Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "slot0", "slot1", "slot2", "slot3", "slot4", "slot5" });

		private final Object key1;

		private final Object val1;

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private final Object slot3;

		private final Object slot4;

		private final Object slot5;

		private Map1To6Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object slot0, final Object slot1,
				final Object slot2, final Object slot3, final Object slot4, final Object slot5) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
			this.slot5 = slot5;
		}

		@Override

		int payloadArity() {
			return 1;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 6;
		}

		@Override

		int slotArity() {
			return 8;
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
			Map1To6Node_Heterogeneous_BleedingEdge that = (Map1To6Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map1To7Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map1To7Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map1To7Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map1To7Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "slot0", "slot1", "slot2", "slot3", "slot4", "slot5", "slot6" });

		private final Object key1;

		private final Object val1;

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private final Object slot3;

		private final Object slot4;

		private final Object slot5;

		private final Object slot6;

		private Map1To7Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object slot0, final Object slot1,
				final Object slot2, final Object slot3, final Object slot4, final Object slot5, final Object slot6) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
			this.slot5 = slot5;
			this.slot6 = slot6;
		}

		@Override

		int payloadArity() {
			return 1;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 7;
		}

		@Override

		int slotArity() {
			return 9;
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
			Map1To7Node_Heterogeneous_BleedingEdge that = (Map1To7Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map2To0Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map2To0Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map2To0Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map2To0Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private Map2To0Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
		}

		@Override

		int payloadArity() {
			return 2;
		}

		@Override

		boolean hasNodes() {
			return false;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 0;
		}

		@Override

		int slotArity() {
			return 4;
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
			Map2To0Node_Heterogeneous_BleedingEdge that = (Map2To0Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}

			return true;
		}

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map2To1Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map2To1Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map2To1Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map2To1Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "slot0" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object slot0;

		private Map2To1Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object slot0) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.slot0 = slot0;
		}

		@Override

		int payloadArity() {
			return 2;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 1;
		}

		@Override

		int slotArity() {
			return 5;
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
			Map2To1Node_Heterogeneous_BleedingEdge that = (Map2To1Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(slot0.equals(that.slot0))) {
				return false;
			}

			return true;
		}

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map2To2Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map2To2Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map2To2Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map2To2Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "slot0", "slot1" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object slot0;

		private final Object slot1;

		private Map2To2Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object slot0, final Object slot1) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.slot0 = slot0;
			this.slot1 = slot1;
		}

		@Override

		int payloadArity() {
			return 2;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 2;
		}

		@Override

		int slotArity() {
			return 6;
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
			Map2To2Node_Heterogeneous_BleedingEdge that = (Map2To2Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map2To3Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map2To3Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map2To3Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map2To3Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "slot0", "slot1", "slot2" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private Map2To3Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object slot0, final Object slot1, final Object slot2) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
		}

		@Override

		int payloadArity() {
			return 2;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 3;
		}

		@Override

		int slotArity() {
			return 7;
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
			Map2To3Node_Heterogeneous_BleedingEdge that = (Map2To3Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map2To4Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map2To4Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map2To4Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map2To4Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "slot0", "slot1", "slot2", "slot3" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private final Object slot3;

		private Map2To4Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object slot0, final Object slot1, final Object slot2, final Object slot3) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
		}

		@Override

		int payloadArity() {
			return 2;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 4;
		}

		@Override

		int slotArity() {
			return 8;
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
			Map2To4Node_Heterogeneous_BleedingEdge that = (Map2To4Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map2To5Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map2To5Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map2To5Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map2To5Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "slot0", "slot1", "slot2", "slot3", "slot4" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private final Object slot3;

		private final Object slot4;

		private Map2To5Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object slot0, final Object slot1, final Object slot2, final Object slot3, final Object slot4) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
		}

		@Override

		int payloadArity() {
			return 2;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 5;
		}

		@Override

		int slotArity() {
			return 9;
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
			Map2To5Node_Heterogeneous_BleedingEdge that = (Map2To5Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map2To6Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map2To6Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map2To6Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map2To6Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "slot0", "slot1", "slot2", "slot3", "slot4", "slot5" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private final Object slot3;

		private final Object slot4;

		private final Object slot5;

		private Map2To6Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object slot0, final Object slot1, final Object slot2, final Object slot3, final Object slot4,
				final Object slot5) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
			this.slot5 = slot5;
		}

		@Override

		int payloadArity() {
			return 2;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 6;
		}

		@Override

		int slotArity() {
			return 10;
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
			Map2To6Node_Heterogeneous_BleedingEdge that = (Map2To6Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map3To0Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map3To0Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map3To0Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map3To0Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "key3", "val3" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private Map3To0Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
		}

		@Override

		int payloadArity() {
			return 3;
		}

		@Override

		boolean hasNodes() {
			return false;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 0;
		}

		@Override

		int slotArity() {
			return 6;
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
			Map3To0Node_Heterogeneous_BleedingEdge that = (Map3To0Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
				return false;
			}

			return true;
		}

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map3To1Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map3To1Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map3To1Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map3To1Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "key3", "val3", "slot0" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private final Object slot0;

		private Map3To1Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3, final Object slot0) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.slot0 = slot0;
		}

		@Override

		int payloadArity() {
			return 3;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 1;
		}

		@Override

		int slotArity() {
			return 7;
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
			Map3To1Node_Heterogeneous_BleedingEdge that = (Map3To1Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
				return false;
			}
			if (!(slot0.equals(that.slot0))) {
				return false;
			}

			return true;
		}

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map3To2Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map3To2Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map3To2Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map3To2Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "key3", "val3", "slot0", "slot1" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private final Object slot0;

		private final Object slot1;

		private Map3To2Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3, final Object slot0, final Object slot1) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.slot0 = slot0;
			this.slot1 = slot1;
		}

		@Override

		int payloadArity() {
			return 3;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 2;
		}

		@Override

		int slotArity() {
			return 8;
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
			Map3To2Node_Heterogeneous_BleedingEdge that = (Map3To2Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map3To3Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map3To3Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map3To3Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map3To3Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "key3", "val3", "slot0", "slot1", "slot2" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private Map3To3Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3, final Object slot0, final Object slot1, final Object slot2) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
		}

		@Override

		int payloadArity() {
			return 3;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 3;
		}

		@Override

		int slotArity() {
			return 9;
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
			Map3To3Node_Heterogeneous_BleedingEdge that = (Map3To3Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map3To4Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map3To4Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map3To4Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map3To4Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "key3", "val3", "slot0", "slot1", "slot2", "slot3" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private final Object slot3;

		private Map3To4Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3, final Object slot0, final Object slot1, final Object slot2,
				final Object slot3) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
		}

		@Override

		int payloadArity() {
			return 3;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 4;
		}

		@Override

		int slotArity() {
			return 10;
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
			Map3To4Node_Heterogeneous_BleedingEdge that = (Map3To4Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map3To5Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map3To5Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map3To5Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map3To5Node_Heterogeneous_BleedingEdge.class, new String[] {
				"key1", "val1", "key2", "val2", "key3", "val3", "slot0", "slot1", "slot2", "slot3", "slot4" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private final Object slot3;

		private final Object slot4;

		private Map3To5Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3, final Object slot0, final Object slot1, final Object slot2,
				final Object slot3, final Object slot4) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
			this.slot4 = slot4;
		}

		@Override

		int payloadArity() {
			return 3;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 5;
		}

		@Override

		int slotArity() {
			return 11;
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
			Map3To5Node_Heterogeneous_BleedingEdge that = (Map3To5Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map4To0Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map4To0Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map4To0Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map4To0Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "key3", "val3", "key4", "val4" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private final Object key4;

		private final Object val4;

		private Map4To0Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3, final Object key4, final Object val4) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
		}

		@Override

		int payloadArity() {
			return 4;
		}

		@Override

		boolean hasNodes() {
			return false;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 0;
		}

		@Override

		int slotArity() {
			return 8;
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
			Map4To0Node_Heterogeneous_BleedingEdge that = (Map4To0Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
				return false;
			}
			if (!(key4.equals(that.key4))) {
				return false;
			}
			if (!(val4.equals(that.val4))) {
				return false;
			}

			return true;
		}

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map4To1Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map4To1Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map4To1Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map4To1Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "key3", "val3", "key4", "val4", "slot0" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private final Object key4;

		private final Object val4;

		private final Object slot0;

		private Map4To1Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3, final Object key4, final Object val4, final Object slot0) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.slot0 = slot0;
		}

		@Override

		int payloadArity() {
			return 4;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 1;
		}

		@Override

		int slotArity() {
			return 9;
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
			Map4To1Node_Heterogeneous_BleedingEdge that = (Map4To1Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
				return false;
			}
			if (!(key4.equals(that.key4))) {
				return false;
			}
			if (!(val4.equals(that.val4))) {
				return false;
			}
			if (!(slot0.equals(that.slot0))) {
				return false;
			}

			return true;
		}

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map4To2Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map4To2Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map4To2Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map4To2Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "key3", "val3", "key4", "val4", "slot0", "slot1" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private final Object key4;

		private final Object val4;

		private final Object slot0;

		private final Object slot1;

		private Map4To2Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3, final Object key4, final Object val4, final Object slot0,
				final Object slot1) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.slot0 = slot0;
			this.slot1 = slot1;
		}

		@Override

		int payloadArity() {
			return 4;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 2;
		}

		@Override

		int slotArity() {
			return 10;
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
			Map4To2Node_Heterogeneous_BleedingEdge that = (Map4To2Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
				return false;
			}
			if (!(key4.equals(that.key4))) {
				return false;
			}
			if (!(val4.equals(that.val4))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map4To3Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map4To3Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map4To3Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map4To3Node_Heterogeneous_BleedingEdge.class, new String[] {
				"key1", "val1", "key2", "val2", "key3", "val3", "key4", "val4", "slot0", "slot1", "slot2" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private final Object key4;

		private final Object val4;

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private Map4To3Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3, final Object key4, final Object val4, final Object slot0,
				final Object slot1, final Object slot2) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
		}

		@Override

		int payloadArity() {
			return 4;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 3;
		}

		@Override

		int slotArity() {
			return 11;
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
			Map4To3Node_Heterogeneous_BleedingEdge that = (Map4To3Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
				return false;
			}
			if (!(key4.equals(that.key4))) {
				return false;
			}
			if (!(val4.equals(that.val4))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map4To4Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map4To4Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map4To4Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map4To4Node_Heterogeneous_BleedingEdge.class, new String[] {
				"key1", "val1", "key2", "val2", "key3", "val3", "key4", "val4", "slot0", "slot1", "slot2", "slot3" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private final Object key4;

		private final Object val4;

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private final Object slot3;

		private Map4To4Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3, final Object key4, final Object val4, final Object slot0,
				final Object slot1, final Object slot2, final Object slot3) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
			this.slot3 = slot3;
		}

		@Override

		int payloadArity() {
			return 4;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 4;
		}

		@Override

		int slotArity() {
			return 12;
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
			Map4To4Node_Heterogeneous_BleedingEdge that = (Map4To4Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
				return false;
			}
			if (!(key4.equals(that.key4))) {
				return false;
			}
			if (!(val4.equals(that.val4))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map5To0Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map5To0Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map5To0Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map5To0Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "key3", "val3", "key4", "val4", "key5", "val5" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private final Object key4;

		private final Object val4;

		private final Object key5;

		private final Object val5;

		private Map5To0Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3, final Object key4, final Object val4, final Object key5,
				final Object val5) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.key5 = key5;
			this.val5 = val5;
		}

		@Override

		int payloadArity() {
			return 5;
		}

		@Override

		boolean hasNodes() {
			return false;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 0;
		}

		@Override

		int slotArity() {
			return 10;
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
			Map5To0Node_Heterogeneous_BleedingEdge that = (Map5To0Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
				return false;
			}
			if (!(key4.equals(that.key4))) {
				return false;
			}
			if (!(val4.equals(that.val4))) {
				return false;
			}
			if (!(key5.equals(that.key5))) {
				return false;
			}
			if (!(val5.equals(that.val5))) {
				return false;
			}

			return true;
		}

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map5To1Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map5To1Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map5To1Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map5To1Node_Heterogeneous_BleedingEdge.class, new String[] {
				"key1", "val1", "key2", "val2", "key3", "val3", "key4", "val4", "key5", "val5", "slot0" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private final Object key4;

		private final Object val4;

		private final Object key5;

		private final Object val5;

		private final Object slot0;

		private Map5To1Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3, final Object key4, final Object val4, final Object key5,
				final Object val5, final Object slot0) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.key5 = key5;
			this.val5 = val5;
			this.slot0 = slot0;
		}

		@Override

		int payloadArity() {
			return 5;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 1;
		}

		@Override

		int slotArity() {
			return 11;
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
			Map5To1Node_Heterogeneous_BleedingEdge that = (Map5To1Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
				return false;
			}
			if (!(key4.equals(that.key4))) {
				return false;
			}
			if (!(val4.equals(that.val4))) {
				return false;
			}
			if (!(key5.equals(that.key5))) {
				return false;
			}
			if (!(val5.equals(that.val5))) {
				return false;
			}
			if (!(slot0.equals(that.slot0))) {
				return false;
			}

			return true;
		}

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map5To2Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map5To2Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map5To2Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map5To2Node_Heterogeneous_BleedingEdge.class, new String[] {
				"key1", "val1", "key2", "val2", "key3", "val3", "key4", "val4", "key5", "val5", "slot0", "slot1" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private final Object key4;

		private final Object val4;

		private final Object key5;

		private final Object val5;

		private final Object slot0;

		private final Object slot1;

		private Map5To2Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3, final Object key4, final Object val4, final Object key5,
				final Object val5, final Object slot0, final Object slot1) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.key5 = key5;
			this.val5 = val5;
			this.slot0 = slot0;
			this.slot1 = slot1;
		}

		@Override

		int payloadArity() {
			return 5;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 2;
		}

		@Override

		int slotArity() {
			return 12;
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
			Map5To2Node_Heterogeneous_BleedingEdge that = (Map5To2Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
				return false;
			}
			if (!(key4.equals(that.key4))) {
				return false;
			}
			if (!(val4.equals(that.val4))) {
				return false;
			}
			if (!(key5.equals(that.key5))) {
				return false;
			}
			if (!(val5.equals(that.val5))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map5To3Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map5To3Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map5To3Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map5To3Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "key3", "val3", "key4", "val4", "key5", "val5", "slot0",
						"slot1", "slot2" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private final Object key4;

		private final Object val4;

		private final Object key5;

		private final Object val5;

		private final Object slot0;

		private final Object slot1;

		private final Object slot2;

		private Map5To3Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3, final Object key4, final Object val4, final Object key5,
				final Object val5, final Object slot0, final Object slot1, final Object slot2) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.key5 = key5;
			this.val5 = val5;
			this.slot0 = slot0;
			this.slot1 = slot1;
			this.slot2 = slot2;
		}

		@Override

		int payloadArity() {
			return 5;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 3;
		}

		@Override

		int slotArity() {
			return 13;
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
			Map5To3Node_Heterogeneous_BleedingEdge that = (Map5To3Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
				return false;
			}
			if (!(key4.equals(that.key4))) {
				return false;
			}
			if (!(val4.equals(that.val4))) {
				return false;
			}
			if (!(key5.equals(that.key5))) {
				return false;
			}
			if (!(val5.equals(that.val5))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map6To0Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map6To0Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map6To0Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map6To0Node_Heterogeneous_BleedingEdge.class, new String[] {
				"key1", "val1", "key2", "val2", "key3", "val3", "key4", "val4", "key5", "val5", "key6", "val6" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private final Object key4;

		private final Object val4;

		private final Object key5;

		private final Object val5;

		private final Object key6;

		private final Object val6;

		private Map6To0Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3, final Object key4, final Object val4, final Object key5,
				final Object val5, final Object key6, final Object val6) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.key5 = key5;
			this.val5 = val5;
			this.key6 = key6;
			this.val6 = val6;
		}

		@Override

		int payloadArity() {
			return 6;
		}

		@Override

		boolean hasNodes() {
			return false;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 0;
		}

		@Override

		int slotArity() {
			return 12;
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
			Map6To0Node_Heterogeneous_BleedingEdge that = (Map6To0Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
				return false;
			}
			if (!(key4.equals(that.key4))) {
				return false;
			}
			if (!(val4.equals(that.val4))) {
				return false;
			}
			if (!(key5.equals(that.key5))) {
				return false;
			}
			if (!(val5.equals(that.val5))) {
				return false;
			}
			if (!(key6.equals(that.key6))) {
				return false;
			}
			if (!(val6.equals(that.val6))) {
				return false;
			}

			return true;
		}

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map6To1Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map6To1Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map6To1Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map6To1Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "key3", "val3", "key4", "val4", "key5", "val5", "key6",
						"val6", "slot0" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private final Object key4;

		private final Object val4;

		private final Object key5;

		private final Object val5;

		private final Object key6;

		private final Object val6;

		private final Object slot0;

		private Map6To1Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3, final Object key4, final Object val4, final Object key5,
				final Object val5, final Object key6, final Object val6, final Object slot0) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.key5 = key5;
			this.val5 = val5;
			this.key6 = key6;
			this.val6 = val6;
			this.slot0 = slot0;
		}

		@Override

		int payloadArity() {
			return 6;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 1;
		}

		@Override

		int slotArity() {
			return 13;
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
			Map6To1Node_Heterogeneous_BleedingEdge that = (Map6To1Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
				return false;
			}
			if (!(key4.equals(that.key4))) {
				return false;
			}
			if (!(val4.equals(that.val4))) {
				return false;
			}
			if (!(key5.equals(that.key5))) {
				return false;
			}
			if (!(val5.equals(that.val5))) {
				return false;
			}
			if (!(key6.equals(that.key6))) {
				return false;
			}
			if (!(val6.equals(that.val6))) {
				return false;
			}
			if (!(slot0.equals(that.slot0))) {
				return false;
			}

			return true;
		}

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map6To2Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map6To2Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map6To2Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map6To2Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "key3", "val3", "key4", "val4", "key5", "val5", "key6",
						"val6", "slot0", "slot1" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private final Object key4;

		private final Object val4;

		private final Object key5;

		private final Object val5;

		private final Object key6;

		private final Object val6;

		private final Object slot0;

		private final Object slot1;

		private Map6To2Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3, final Object key4, final Object val4, final Object key5,
				final Object val5, final Object key6, final Object val6, final Object slot0, final Object slot1) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.key5 = key5;
			this.val5 = val5;
			this.key6 = key6;
			this.val6 = val6;
			this.slot0 = slot0;
			this.slot1 = slot1;
		}

		@Override

		int payloadArity() {
			return 6;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 2;
		}

		@Override

		int slotArity() {
			return 14;
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
			Map6To2Node_Heterogeneous_BleedingEdge that = (Map6To2Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
				return false;
			}
			if (!(key4.equals(that.key4))) {
				return false;
			}
			if (!(val4.equals(that.val4))) {
				return false;
			}
			if (!(key5.equals(that.key5))) {
				return false;
			}
			if (!(val5.equals(that.val5))) {
				return false;
			}
			if (!(key6.equals(that.key6))) {
				return false;
			}
			if (!(val6.equals(that.val6))) {
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

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map7To0Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map7To0Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map7To0Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map7To0Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "key3", "val3", "key4", "val4", "key5", "val5", "key6",
						"val6", "key7", "val7" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private final Object key4;

		private final Object val4;

		private final Object key5;

		private final Object val5;

		private final Object key6;

		private final Object val6;

		private final Object key7;

		private final Object val7;

		private Map7To0Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3, final Object key4, final Object val4, final Object key5,
				final Object val5, final Object key6, final Object val6, final Object key7, final Object val7) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.key5 = key5;
			this.val5 = val5;
			this.key6 = key6;
			this.val6 = val6;
			this.key7 = key7;
			this.val7 = val7;
		}

		@Override

		int payloadArity() {
			return 7;
		}

		@Override

		boolean hasNodes() {
			return false;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 0;
		}

		@Override

		int slotArity() {
			return 14;
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
			Map7To0Node_Heterogeneous_BleedingEdge that = (Map7To0Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
				return false;
			}
			if (!(key4.equals(that.key4))) {
				return false;
			}
			if (!(val4.equals(that.val4))) {
				return false;
			}
			if (!(key5.equals(that.key5))) {
				return false;
			}
			if (!(val5.equals(that.val5))) {
				return false;
			}
			if (!(key6.equals(that.key6))) {
				return false;
			}
			if (!(val6.equals(that.val6))) {
				return false;
			}
			if (!(key7.equals(that.key7))) {
				return false;
			}
			if (!(val7.equals(that.val7))) {
				return false;
			}

			return true;
		}

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map7To1Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map7To1Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map7To1Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map7To1Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "key3", "val3", "key4", "val4", "key5", "val5", "key6",
						"val6", "key7", "val7", "slot0" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private final Object key4;

		private final Object val4;

		private final Object key5;

		private final Object val5;

		private final Object key6;

		private final Object val6;

		private final Object key7;

		private final Object val7;

		private final Object slot0;

		private Map7To1Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3, final Object key4, final Object val4, final Object key5,
				final Object val5, final Object key6, final Object val6, final Object key7, final Object val7,
				final Object slot0) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.key5 = key5;
			this.val5 = val5;
			this.key6 = key6;
			this.val6 = val6;
			this.key7 = key7;
			this.val7 = val7;
			this.slot0 = slot0;
		}

		@Override

		int payloadArity() {
			return 7;
		}

		@Override

		boolean hasNodes() {
			return true;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 1;
		}

		@Override

		int slotArity() {
			return 15;
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
			Map7To1Node_Heterogeneous_BleedingEdge that = (Map7To1Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
				return false;
			}
			if (!(key4.equals(that.key4))) {
				return false;
			}
			if (!(val4.equals(that.val4))) {
				return false;
			}
			if (!(key5.equals(that.key5))) {
				return false;
			}
			if (!(val5.equals(that.val5))) {
				return false;
			}
			if (!(key6.equals(that.key6))) {
				return false;
			}
			if (!(val6.equals(that.val6))) {
				return false;
			}
			if (!(key7.equals(that.key7))) {
				return false;
			}
			if (!(val7.equals(that.val7))) {
				return false;
			}
			if (!(slot0.equals(that.slot0))) {
				return false;
			}

			return true;
		}

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

	private static class Map8To0Node_Heterogeneous_BleedingEdge extends CompactMixedHeterogeneousMapNode {

		static final long rawMap1Offset = bitmapOffset(Map8To0Node_Heterogeneous_BleedingEdge.class, "rawMap1");

		static final long rawMap2Offset = bitmapOffset(Map8To0Node_Heterogeneous_BleedingEdge.class, "rawMap2");

		static final long[] arrayOffsets = arrayOffsets(Map8To0Node_Heterogeneous_BleedingEdge.class,
				new String[] { "key1", "val1", "key2", "val2", "key3", "val3", "key4", "val4", "key5", "val5", "key6",
						"val6", "key7", "val7", "key8", "val8" });

		private final Object key1;

		private final Object val1;

		private final Object key2;

		private final Object val2;

		private final Object key3;

		private final Object val3;

		private final Object key4;

		private final Object val4;

		private final Object key5;

		private final Object val5;

		private final Object key6;

		private final Object val6;

		private final Object key7;

		private final Object val7;

		private final Object key8;

		private final Object val8;

		private Map8To0Node_Heterogeneous_BleedingEdge(final AtomicReference<Thread> mutator, final byte nodeMap,
				final byte dataMap, final Object key1, final Object val1, final Object key2, final Object val2,
				final Object key3, final Object val3, final Object key4, final Object val4, final Object key5,
				final Object val5, final Object key6, final Object val6, final Object key7, final Object val7,
				final Object key8, final Object val8) {
			super(mutator, nodeMap, dataMap);
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.key5 = key5;
			this.val5 = val5;
			this.key6 = key6;
			this.val6 = val6;
			this.key7 = key7;
			this.val7 = val7;
			this.key8 = key8;
			this.val8 = val8;
		}

		@Override

		int payloadArity() {
			return 8;
		}

		@Override

		boolean hasNodes() {
			return false;
		}

		@Override

		boolean hasSlots() {
			return true;
		}

		@Override

		public int hashCode() {
			throw new UnsupportedOperationException(); // TODO: to implement
		}

		@Override

		int nodeArity() {
			return 0;
		}

		@Override

		int slotArity() {
			return 16;
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
			Map8To0Node_Heterogeneous_BleedingEdge that = (Map8To0Node_Heterogeneous_BleedingEdge) other;

			if (nodeMap() != that.nodeMap()) {
				return false;
			}
			if (dataMap() != that.dataMap()) {
				return false;
			}

			if (!(key1.equals(that.key1))) {
				return false;
			}
			if (!(val1.equals(that.val1))) {
				return false;
			}
			if (!(key2.equals(that.key2))) {
				return false;
			}
			if (!(val2.equals(that.val2))) {
				return false;
			}
			if (!(key3.equals(that.key3))) {
				return false;
			}
			if (!(val3.equals(that.val3))) {
				return false;
			}
			if (!(key4.equals(that.key4))) {
				return false;
			}
			if (!(val4.equals(that.val4))) {
				return false;
			}
			if (!(key5.equals(that.key5))) {
				return false;
			}
			if (!(val5.equals(that.val5))) {
				return false;
			}
			if (!(key6.equals(that.key6))) {
				return false;
			}
			if (!(val6.equals(that.val6))) {
				return false;
			}
			if (!(key7.equals(that.key7))) {
				return false;
			}
			if (!(val7.equals(that.val7))) {
				return false;
			}
			if (!(key8.equals(that.key8))) {
				return false;
			}
			if (!(val8.equals(that.val8))) {
				return false;
			}

			return true;
		}

		@Override

		boolean hasPayload() {
			return true;
		}

		@Override

		byte sizePredicate() {
			return sizeMoreThanOne();
		}

	}

}