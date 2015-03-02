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

import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndInsert;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndMoveToBack;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndMoveToFront;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndRemove;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndSet;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("rawtypes")
public class TrieSetGPCEDynamic<K> extends AbstractImmutableSet<K> {

	@SuppressWarnings("unchecked")
	private static final TrieSetGPCEDynamic EMPTY_INPLACE_INDEX_SET = new TrieSetGPCEDynamic(
					CompactSetNode.EMPTY_INPLACE_INDEX_NODE, 0, 0);

	private static final boolean USE_SPECIALIAZIONS = false;
	private static final boolean USE_STACK_ITERATOR = true; // does not effect
															// TransientSet

	private final AbstractSetNode<K> rootNode;
	private final int hashCode;
	private final int cachedSize;

	TrieSetGPCEDynamic(AbstractSetNode<K> rootNode, int hashCode, int cachedSize) {
		this.rootNode = rootNode;
		this.hashCode = hashCode;
		this.cachedSize = cachedSize;
	}

	@SuppressWarnings("unchecked")
	public static final <K> ImmutableSet<K> of() {
		return TrieSetGPCEDynamic.EMPTY_INPLACE_INDEX_SET;
	}

	@SuppressWarnings("unchecked")
	public static final <K> ImmutableSet<K> of(K... keys) {
		ImmutableSet<K> result = TrieSetGPCEDynamic.EMPTY_INPLACE_INDEX_SET;

		for (K item : keys) {
			result = result.__insert(item);
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public static final <K> TransientSet<K> transientOf() {
		return TrieSetGPCEDynamic.EMPTY_INPLACE_INDEX_SET.asTransient();
	}

	@SafeVarargs
	@SuppressWarnings("unchecked")
	public static final <K> TransientSet<K> transientOf(K... keys) {
		final TransientSet<K> result = TrieSetGPCEDynamic.EMPTY_INPLACE_INDEX_SET.asTransient();

		for (K item : keys) {
			result.__insert(item);
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	protected static final <K> Comparator<K> equalityComparator() {
		return EqualityUtils.getDefaultEqualityComparator();
	}

	private boolean invariant() {
		int _hash = 0;
		int _count = 0;

		for (SupplierIterator<K, K> it = keyIterator(); it.hasNext();) {
			final K key = it.next();

			_hash += key.hashCode();
			_count += 1;
		}

		return this.hashCode == _hash && this.cachedSize == _count;
	}

	@Override
	public TrieSetGPCEDynamic<K> __insert(K k) {
		return __insertEquivalent(k, equalityComparator());
	}

	@Override
	public TrieSetGPCEDynamic<K> __insertEquivalent(K key, Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final Result<K, Void, ? extends AbstractSetNode<K>> result = rootNode.updated(null, key,
						keyHash, null, 0, cmp);

		if (result.isModified()) {
			return new TrieSetGPCEDynamic<K>(result.getNode(), hashCode + keyHash, cachedSize + 1);
		}

		return this;
	}

	@Override
	public ImmutableSet<K> __insertAll(Set<? extends K> set) {
		return __insertAllEquivalent(set, equalityComparator());
	}

	@Override
	public ImmutableSet<K> __insertAllEquivalent(Set<? extends K> set, Comparator<Object> cmp) {
		TransientSet<K> tmp = asTransient();
		tmp.__insertAllEquivalent(set, cmp);
		return tmp.freeze();
	}

	@Override
	public ImmutableSet<K> __retainAll(Set<? extends K> set) {
		TransientSet<K> tmp = asTransient();
		tmp.__retainAll(set);
		return tmp.freeze();
	}

	@Override
	public ImmutableSet<K> __retainAllEquivalent(TransientSet<? extends K> set,
					Comparator<Object> cmp) {
		TransientSet<K> tmp = asTransient();
		tmp.__retainAllEquivalent(set, cmp);
		return tmp.freeze();
	}

	@Override
	public TrieSetGPCEDynamic<K> __remove(K k) {
		return __removeEquivalent(k, equalityComparator());
	}

	@Override
	public TrieSetGPCEDynamic<K> __removeEquivalent(K key, Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final Result<K, Void, ? extends AbstractSetNode<K>> result = rootNode.removed(null, key,
						keyHash, 0, cmp);

		if (result.isModified()) {
			// TODO: carry deleted value in result
			// assert result.hasReplacedValue();
			// final int valHash = result.getReplacedValue().hashCode();

			return new TrieSetGPCEDynamic<K>(result.getNode(), hashCode - keyHash, cachedSize - 1);
		}

		return this;
	}

	@Override
	public ImmutableSet<K> __removeAll(Set<? extends K> set) {
		return __removeAllEquivalent(set, equalityComparator());
	}

	@Override
	public ImmutableSet<K> __removeAllEquivalent(Set<? extends K> set, Comparator<Object> cmp) {
		TransientSet<K> tmp = asTransient();
		tmp.__removeAllEquivalent(set, cmp);
		return tmp.freeze();
	}

	@Override
	public boolean contains(Object o) {
		return rootNode.containsKey(o, o.hashCode(), 0, equalityComparator());
	}

	@Override
	public boolean containsEquivalent(Object o, Comparator<Object> cmp) {
		return rootNode.containsKey(o, o.hashCode(), 0, cmp);
	}

	@Override
	public K get(Object key) {
		return getEquivalent(key, equalityComparator());
	}

	@Override
	public K getEquivalent(Object key, Comparator<Object> cmp) {
		final Optional<K> result = rootNode.findByKey(key, key.hashCode(), 0, cmp);

		if (result.isPresent()) {
			return result.get();
		} else {
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
		return new TrieSetIteratorWithFixedWidthStack<>(rootNode);
	}

	// @Override
	// public SupplierIterator<K, K> keyIterator() {
	// return new TrieSetIteratorWithFixedWidthStack<>(rootNode);
	// }

	private static class TrieSetIteratorWithFixedWidthStack<K> implements SupplierIterator<K, K> {
		int valueIndex;
		int valueLength;
		AbstractSetNode<K> valueNode;

		K lastValue = null;

		int stackLevel;

		int[] indexAndLength = new int[7 * 2];

		@SuppressWarnings("unchecked")
		AbstractSetNode<K>[] nodes = new AbstractSetNode[7];

		TrieSetIteratorWithFixedWidthStack(AbstractSetNode<K> rootNode) {
			stackLevel = 0;

			valueNode = rootNode;

			valueIndex = 0;
			valueLength = rootNode.payloadArity();

			nodes[0] = rootNode;
			indexAndLength[0] = 0;
			indexAndLength[1] = rootNode.nodeArity();
		}

		@Override
		public boolean hasNext() {
			if (valueIndex < valueLength) {
				return true;
			}

			while (true) {
				final int nodeIndex = indexAndLength[2 * stackLevel];
				final int nodeLength = indexAndLength[2 * stackLevel + 1];

				if (nodeIndex < nodeLength) {
					final AbstractSetNode<K> nextNode = nodes[stackLevel].getNode(nodeIndex);
					indexAndLength[2 * stackLevel] = (nodeIndex + 1);

					final int nextNodeValueArity = nextNode.payloadArity();
					final int nextNodeNodeArity = nextNode.nodeArity();

					if (nextNodeNodeArity != 0) {
						stackLevel++;

						nodes[stackLevel] = nextNode;
						indexAndLength[2 * stackLevel] = 0;
						indexAndLength[2 * stackLevel + 1] = nextNode.nodeArity();
					}

					if (nextNodeValueArity != 0) {
						valueNode = nextNode;
						valueIndex = 0;
						valueLength = nextNodeValueArity;
						return true;
					}
				} else {
					if (stackLevel == 0) {
						return false;
					}

					stackLevel--;
				}
			}
		}

		@Override
		public K next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			} else {
				K lastKey = valueNode.getKey(valueIndex);
				lastValue = lastKey;

				valueIndex += 1;

				return lastKey;
			}
		}

		@Override
		public K get() {
			if (lastValue == null) {
				throw new NoSuchElementException();
			} else {
				K tmp = lastValue;
				lastValue = null;

				return tmp;
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public boolean isTransientSupported() {
		return true;
	}

	@Override
	public TransientSet<K> asTransient() {
		return new TransientTrieSet<K>(this);
	}

	static final class TransientTrieSet<K> extends AbstractSet<K> implements TransientSet<K> {
		final private AtomicReference<Thread> mutator;
		private AbstractSetNode<K> rootNode;
		private int hashCode;
		private int cachedSize;

		TransientTrieSet(TrieSetGPCEDynamic<K> TrieSet) {
			this.mutator = new AtomicReference<Thread>(Thread.currentThread());
			this.rootNode = TrieSet.rootNode;
			this.hashCode = TrieSet.hashCode;
			this.cachedSize = TrieSet.cachedSize;
		}

		// TODO: merge with TrieSet invariant (as function)
		private boolean invariant() {
			int _hash = 0;

			for (SupplierIterator<K, K> it = keyIterator(); it.hasNext();) {
				final K key = it.next();

				_hash += key.hashCode();
			}

			return this.hashCode == _hash;
		}

		@Override
		public boolean contains(Object o) {
			return rootNode.containsKey(o, o.hashCode(), 0, equalityComparator());
		}

		@Override
		public boolean containsEquivalent(Object o, Comparator<Object> cmp) {
			return rootNode.containsKey(o, o.hashCode(), 0, cmp);
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
		public K get(Object key) {
			return getEquivalent(key, equalityComparator());
		}

		@Override
		public K getEquivalent(Object key, Comparator<Object> cmp) {
			final Optional<K> result = rootNode.findByKey(key, key.hashCode(), 0, cmp);

			if (result.isPresent()) {
				return result.get();
			} else {
				return null;
			}
		}

		@Override
		public boolean __insert(K k) {
			return __insertEquivalent(k, equalityComparator());
		}

		@Override
		public boolean __insertEquivalent(K key, Comparator<Object> cmp) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final Result<K, Void, ? extends AbstractSetNode<K>> result = rootNode.updated(mutator,
							key, keyHash, null, 0, cmp);

			if (result.isModified()) {
				rootNode = result.getNode();

				hashCode += keyHash;
				cachedSize += 1;

				return true;
			}

			return false;
		}

		@Override
		public boolean __retainAll(Set<? extends K> set) {
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
		public boolean __retainAllEquivalent(TransientSet<? extends K> set, Comparator<Object> cmp) {
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
		public boolean __remove(K k) {
			return __removeEquivalent(k, equalityComparator());
		}

		@Override
		public boolean __removeEquivalent(K key, Comparator<Object> cmp) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final Result<K, Void, ? extends AbstractSetNode<K>> result = rootNode.removed(mutator,
							key, keyHash, 0, cmp);

			if (result.isModified()) {
				// TODO: carry deleted value in result
				// assert result.hasReplacedValue();
				// final int valHash = result.getReplacedValue().hashCode();

				rootNode = result.getNode();
				hashCode -= keyHash;
				cachedSize -= 1;

				return true;
			}

			return false;
		}

		@Override
		public boolean __removeAll(Set<? extends K> set) {
			return __removeAllEquivalent(set, equalityComparator());
		}

		@Override
		public boolean __removeAllEquivalent(Set<? extends K> set, Comparator<Object> cmp) {
			boolean modified = false;

			for (K key : set) {
				modified |= __removeEquivalent(key, cmp);
			}

			return modified;
		}

		@Override
		public boolean __insertAll(Set<? extends K> set) {
			return __insertAllEquivalent(set, equalityComparator());
		}

		@Override
		public boolean __insertAllEquivalent(Set<? extends K> set, Comparator<Object> cmp) {
			boolean modified = false;

			for (K key : set) {
				modified |= __insertEquivalent(key, cmp);
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

		// TODO: test; declare in transient interface
		// @Override
		@Override
		public SupplierIterator<K, K> keyIterator() {
			return new TransientTrieSetIterator<K>(this);
		}

		/**
		 * Iterator that first iterates over inlined-values and then continues
		 * depth first recursively.
		 */
		// TODO: test
		private static class TransientTrieSetIterator<K> extends
						TrieSetIteratorWithFixedWidthStack<K> {

			final TransientTrieSet<K> transientTrieSet;
			K lastKey;

			TransientTrieSetIterator(TransientTrieSet<K> transientTrieSet) {
				super(transientTrieSet.rootNode);
				this.transientTrieSet = transientTrieSet;
			}

			@Override
			public K next() {
				lastKey = super.next();
				return lastKey;
			}

			@Override
			public void remove() {
				transientTrieSet.__remove(lastKey);
			}
		}

		@Override
		public boolean equals(Object o) {
			return rootNode.equals(o);
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public String toString() {
			return rootNode.toString();
		}

		@Override
		public ImmutableSet<K> freeze() {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			mutator.set(null);
			return new TrieSetGPCEDynamic<K>(rootNode, hashCode, cachedSize);
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

	protected static abstract class AbstractNode<K, V> {

	}

	protected static abstract class AbstractSetNode<K> extends AbstractNode<K, Void> {

		protected static final int BIT_PARTITION_SIZE = 5;
		protected static final int BIT_PARTITION_MASK = 0x1f;

		abstract boolean containsKey(Object key, int keyHash, int shift);

		abstract boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp);

		abstract Optional<K> findByKey(Object key, int keyHash, int shift);

		abstract Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp);

		abstract Result<K, Void, ? extends AbstractSetNode<K>> updated(
						AtomicReference<Thread> mutator, K key, int keyHash, Void val, int shift);

		abstract Result<K, Void, ? extends AbstractSetNode<K>> updated(
						AtomicReference<Thread> mutator, K key, int keyHash, Void val, int shift,
						Comparator<Object> cmp);

		abstract Result<K, Void, ? extends AbstractSetNode<K>> removed(
						AtomicReference<Thread> mutator, K key, int keyHash, int shift);

		abstract Result<K, Void, ? extends AbstractSetNode<K>> removed(
						AtomicReference<Thread> mutator, K key, int keyHash, int shift,
						Comparator<Object> cmp);

		static final boolean isAllowedToEdit(AtomicReference<Thread> x, AtomicReference<Thread> y) {
			return x != null && y != null && (x == y || x.get() == y.get());
		}

		abstract K getKey(int index);

		abstract AbstractSetNode<K> getNode(int index);

		abstract boolean hasNodes();

		abstract Iterator<? extends AbstractSetNode<K>> nodeIterator();

		abstract int nodeArity();

		abstract boolean hasPayload();

		abstract SupplierIterator<K, K> payloadIterator();

		abstract int payloadArity();

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
			final SupplierIterator<K, K> it = new TrieSetIteratorWithFixedWidthStack<>(this);

			int size = 0;
			while (it.hasNext()) {
				size += 1;
				it.next();
			}

			return size;
		}
	}

	private static abstract class CompactSetNode<K> extends AbstractSetNode<K> {
		static final byte SIZE_EMPTY = 0b00;
		static final byte SIZE_ONE = 0b01;
		static final byte SIZE_MORE_THAN_ONE = 0b10;

		@Override
		abstract Result<K, Void, ? extends CompactSetNode<K>> updated(
						AtomicReference<Thread> mutator, K key, int keyHash, Void val, int shift);

		@Override
		abstract Result<K, Void, ? extends CompactSetNode<K>> updated(
						AtomicReference<Thread> mutator, K key, int keyHash, Void val, int shift,
						Comparator<Object> cmp);

		@Override
		abstract Result<K, Void, ? extends CompactSetNode<K>> removed(
						AtomicReference<Thread> mutator, K key, int hash, int shift);

		@Override
		abstract Result<K, Void, ? extends CompactSetNode<K>> removed(
						AtomicReference<Thread> mutator, K key, int hash, int shift,
						Comparator<Object> cmp);

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
		abstract K headKey();

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

		@SuppressWarnings("unchecked")
		static final CompactSetNode EMPTY_INPLACE_INDEX_NODE;

		static {
			EMPTY_INPLACE_INDEX_NODE = new BitmapIndexedSetNode<>(null, 0, 0, new Object[] {},
							(byte) 0);
		};

		static final <K> CompactSetNode<K> valNodeOf(AtomicReference<Thread> mutator, int bitmap,
						int valmap, Object[] nodes, byte valueArity) {
			return new BitmapIndexedSetNode<>(mutator, bitmap, valmap, nodes, valueArity);
		}

		@SuppressWarnings("unchecked")
		static final <K> CompactSetNode<K> mergeNodes(K key0, int keyHash0, K key1, int keyHash1,
						int shift) {
			assert key0.equals(key1) == false;

			if (keyHash0 == keyHash1) {
				return new HashCollisionSetNode<>(keyHash0, (K[]) new Object[] { key0, key1 });
			}

			final int mask0 = (keyHash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (keyHash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level

				final int valmap = 1 << mask0 | 1 << mask1;

				if (mask0 < mask1) {
					return valNodeOf(null, valmap, valmap, new Object[] { key0, key1 }, (byte) 2);
				} else {
					return valNodeOf(null, valmap, valmap, new Object[] { key1, key0 }, (byte) 2);
				}
			} else {
				// values fit on next level
				final CompactSetNode<K> node = mergeNodes(key0, keyHash0, key1, keyHash1, shift
								+ BIT_PARTITION_SIZE);

				final int bitmap = 1 << mask0;
				return valNodeOf(null, bitmap, 0, new Object[] { node }, (byte) 0);
			}
		}

		static final <K> CompactSetNode<K> mergeNodes(CompactSetNode<K> node0, int keyHash0,
						K key1, int keyHash1, int shift) {
			final int mask0 = (keyHash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (keyHash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level

				final int bitmap = 1 << mask0 | 1 << mask1;
				final int valmap = 1 << mask1;

				// store values before node
				return valNodeOf(null, bitmap, valmap, new Object[] { key1, node0 }, (byte) 1);
			} else {
				// values fit on next level
				final CompactSetNode<K> node = mergeNodes(node0, keyHash0, key1, keyHash1, shift
								+ BIT_PARTITION_SIZE);

				final int bitmap = 1 << mask0;
				return valNodeOf(null, bitmap, 0, new Object[] { node }, (byte) 0);
			}
		}
	}

	private static final class BitmapIndexedSetNode<K> extends CompactSetNode<K> {
		private AtomicReference<Thread> mutator;

		private Object[] nodes;
		final private int bitmap;
		final private int valmap;
		final private byte payloadArity;

		BitmapIndexedSetNode(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes, byte payloadArity) {
			this.mutator = mutator;

			this.nodes = nodes;
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.payloadArity = payloadArity;
		}

		final int bitIndex(int bitpos) {
			return payloadArity + Integer.bitCount((bitmap ^ valmap) & (bitpos - 1));
		}

		final int valIndex(int bitpos) {
			return Integer.bitCount(valmap & (bitpos - 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		boolean containsKey(Object key, int hash, int shift) {
			final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) {
				return nodes[valIndex(bitpos)].equals(key);
			}

			if ((bitmap & bitpos) != 0) {
				return ((AbstractSetNode<K>) nodes[bitIndex(bitpos)]).containsKey(key, hash, shift
								+ BIT_PARTITION_SIZE);
			}

			return false;
		}

		@SuppressWarnings("unchecked")
		@Override
		boolean containsKey(Object key, int hash, int shift, Comparator<Object> cmp) {
			final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) {
				return cmp.compare(nodes[valIndex(bitpos)], key) == 0;
			}

			if ((bitmap & bitpos) != 0) {
				return ((AbstractSetNode<K>) nodes[bitIndex(bitpos)]).containsKey(key, hash, shift
								+ BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		@SuppressWarnings("unchecked")
		Optional<K> findByKey(Object key, int keyHash, int shift) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (nodes[valIndex].equals(key)) {
					final K _key = (K) nodes[valIndex];
					return Optional.of(_key);
				}

				return Optional.empty();
			}

			if ((bitmap & bitpos) != 0) { // node (not value)
				final AbstractSetNode<K> subNode = ((AbstractSetNode<K>) nodes[bitIndex(bitpos)]);

				return subNode.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			}

			return Optional.empty();
		}

		@Override
		@SuppressWarnings("unchecked")
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (cmp.compare(nodes[valIndex], key) == 0) {
					final K _key = (K) nodes[valIndex];
					return Optional.of(_key);
				}

				return Optional.empty();
			}

			if ((bitmap & bitpos) != 0) { // node (not value)
				final AbstractSetNode<K> subNode = ((AbstractSetNode<K>) nodes[bitIndex(bitpos)]);

				return subNode.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator,
						K key, int keyHash, Void val, int shift) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				final Object currentKey = nodes[valIndex];

				if (currentKey.equals(key)) {
					return Result.unchanged(this);
				} else {
					final CompactSetNode<K> nodeNew = mergeNodes((K) nodes[valIndex],
									nodes[valIndex].hashCode(), key, keyHash, shift
													+ BIT_PARTITION_SIZE);

					final int offset = (payloadArity - 1);
					final int index = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
									& (bitpos - 1));

					final Object[] editableNodes = copyAndMoveToBack(this.nodes, valIndex, offset
									+ index, nodeNew);

					final CompactSetNode<K> thisNew = CompactSetNode.<K> valNodeOf(mutator, bitmap
									| bitpos, valmap & ~bitpos, editableNodes,
									(byte) (payloadArity - 1));

					return Result.modified(thisNew);
				}
			} else if ((bitmap & bitpos) != 0) { // node (not value)
				final int bitIndex = bitIndex(bitpos);
				final CompactSetNode<K> subNode = (CompactSetNode<K>) nodes[bitIndex];

				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = subNode.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (!nestedResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactSetNode<K> thisNew;

				// modify current node (set replacement node)
				if (isAllowedToEdit(this.mutator, mutator)) {
					// no copying if already editable
					this.nodes[bitIndex] = nestedResult.getNode();
					thisNew = this;
				} else {
					final Object[] editableNodes = copyAndSet(this.nodes, bitIndex,
									nestedResult.getNode());

					thisNew = CompactSetNode.<K> valNodeOf(mutator, bitmap, valmap, editableNodes,
									payloadArity);
				}

				return Result.modified(thisNew);
			} else {
				// no value
				final Object[] editableNodes = copyAndInsert(this.nodes, valIndex(bitpos), key);

				final CompactSetNode<K> thisNew = CompactSetNode
								.<K> valNodeOf(mutator, bitmap | bitpos, valmap | bitpos,
												editableNodes, (byte) (payloadArity + 1));

				return Result.modified(thisNew);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator,
						K key, int keyHash, Void val, int shift, Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				final Object currentKey = nodes[valIndex];

				if (cmp.compare(currentKey, key) == 0) {
					return Result.unchanged(this);
				} else {
					final CompactSetNode<K> nodeNew = mergeNodes((K) nodes[valIndex],
									nodes[valIndex].hashCode(), key, keyHash, shift
													+ BIT_PARTITION_SIZE);

					final int offset = (payloadArity - 1);
					final int index = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
									& (bitpos - 1));

					final Object[] editableNodes = copyAndMoveToBack(this.nodes, valIndex, offset
									+ index, nodeNew);

					final CompactSetNode<K> thisNew = CompactSetNode.<K> valNodeOf(mutator, bitmap
									| bitpos, valmap & ~bitpos, editableNodes,
									(byte) (payloadArity - 1));

					return Result.modified(thisNew);
				}
			} else if ((bitmap & bitpos) != 0) { // node (not value)
				final int bitIndex = bitIndex(bitpos);
				final CompactSetNode<K> subNode = (CompactSetNode<K>) nodes[bitIndex];

				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = subNode.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!nestedResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactSetNode<K> thisNew;

				// modify current node (set replacement node)
				if (isAllowedToEdit(this.mutator, mutator)) {
					// no copying if already editable
					this.nodes[bitIndex] = nestedResult.getNode();
					thisNew = this;
				} else {
					final Object[] editableNodes = copyAndSet(this.nodes, bitIndex,
									nestedResult.getNode());

					thisNew = CompactSetNode.<K> valNodeOf(mutator, bitmap, valmap, editableNodes,
									payloadArity);
				}

				return Result.modified(thisNew);
			} else {
				// no value
				final Object[] editableNodes = copyAndInsert(this.nodes, valIndex(bitpos), key);

				final CompactSetNode<K> thisNew = CompactSetNode
								.<K> valNodeOf(mutator, bitmap | bitpos, valmap | bitpos,
												editableNodes, (byte) (payloadArity + 1));

				return Result.modified(thisNew);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator,
						K key, int keyHash, int shift) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (nodes[valIndex].equals(key)) {
					if (!USE_SPECIALIAZIONS && this.payloadArity() == 2 && this.nodeArity() == 0) {
						/*
						 * Create new node with remaining pair. The new node
						 * will a) either become the new root returned, or b)
						 * unwrapped and inlined during returning.
						 */
						final CompactSetNode<K> thisNew;
						final int newValmap = (shift == 0) ? this.valmap & ~bitpos
										: 1 << (keyHash & BIT_PARTITION_MASK);

						if (valIndex == 0) {
							thisNew = CompactSetNode.<K> valNodeOf(mutator, newValmap, newValmap,
											new Object[] { nodes[2], nodes[3] }, (byte) (1));
						} else {
							thisNew = CompactSetNode.<K> valNodeOf(mutator, newValmap, newValmap,
											new Object[] { nodes[0], nodes[1] }, (byte) (1));
						}

						return Result.modified(thisNew);
					} else {
						final Object[] editableNodes = copyAndRemove(this.nodes, valIndex);

						final CompactSetNode<K> thisNew = CompactSetNode.<K> valNodeOf(mutator,
										this.bitmap & ~bitpos, this.valmap & ~bitpos,
										editableNodes, (byte) (payloadArity - 1));

						return Result.modified(thisNew);
					}
				} else {
					return Result.unchanged(this);
				}
			} else if ((bitmap & bitpos) != 0) { // node (not value)
				final int bitIndex = bitIndex(bitpos);
				final CompactSetNode<K> subNode = (CompactSetNode<K>) nodes[bitIndex];
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = subNode.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (!nestedResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactSetNode<K> subNodeNew = nestedResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case 0: {
					if (!USE_SPECIALIAZIONS && this.payloadArity() == 0 && this.nodeArity() == 1) {
						// escalate (singleton or empty) result
						return nestedResult;
					} else {
						// remove node
						final Object[] editableNodes = copyAndRemove(this.nodes, bitIndex);

						final CompactSetNode<K> thisNew = CompactSetNode.<K> valNodeOf(mutator,
										bitmap & ~bitpos, valmap, editableNodes, payloadArity);

						return Result.modified(thisNew);
					}
				}
				case 1: {
					if (!USE_SPECIALIAZIONS && this.payloadArity() == 0 && this.nodeArity() == 1) {
						// escalate (singleton or empty) result
						return nestedResult;
					} else {
						// inline value (move to front)
						final int valIndexNew = Integer.bitCount((valmap | bitpos) & (bitpos - 1));

						final Object[] editableNodes = copyAndMoveToFront(this.nodes, bitIndex,
										valIndexNew, subNodeNew.headKey());

						final CompactSetNode<K> thisNew = CompactSetNode.<K> valNodeOf(mutator,
										bitmap, valmap | bitpos, editableNodes,
										(byte) (payloadArity + 1));

						return Result.modified(thisNew);
					}
				}
				default: {
					// modify current node (set replacement node)
					if (isAllowedToEdit(this.mutator, mutator)) {
						// no copying if already editable
						this.nodes[bitIndex] = subNodeNew;
						return Result.modified(this);
					} else {
						final Object[] editableNodes = copyAndSet(this.nodes, bitIndex, subNodeNew);

						final CompactSetNode<K> thisNew = CompactSetNode.<K> valNodeOf(mutator,
										bitmap, valmap, editableNodes, payloadArity);

						return Result.modified(thisNew);
					}
				}
				}
			}

			return Result.unchanged(this);
		}

		@SuppressWarnings("unchecked")
		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator,
						K key, int keyHash, int shift, Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (cmp.compare(nodes[valIndex], key) == 0) {
					if (!USE_SPECIALIAZIONS && this.payloadArity() == 2 && this.nodeArity() == 0) {
						/*
						 * Create new node with remaining pair. The new node
						 * will a) either become the new root returned, or b)
						 * unwrapped and inlined during returning.
						 */
						final CompactSetNode<K> thisNew;
						final int newValmap = (shift == 0) ? this.valmap & ~bitpos
										: 1 << (keyHash & BIT_PARTITION_MASK);

						if (valIndex == 0) {
							thisNew = CompactSetNode.<K> valNodeOf(mutator, newValmap, newValmap,
											new Object[] { nodes[2], nodes[3] }, (byte) (1));
						} else {
							thisNew = CompactSetNode.<K> valNodeOf(mutator, newValmap, newValmap,
											new Object[] { nodes[0], nodes[1] }, (byte) (1));
						}

						return Result.modified(thisNew);
					} else {
						final Object[] editableNodes = copyAndRemove(this.nodes, valIndex);

						final CompactSetNode<K> thisNew = CompactSetNode.<K> valNodeOf(mutator,
										this.bitmap & ~bitpos, this.valmap & ~bitpos,
										editableNodes, (byte) (payloadArity - 1));

						return Result.modified(thisNew);
					}
				} else {
					return Result.unchanged(this);
				}
			} else if ((bitmap & bitpos) != 0) { // node (not value)
				final int bitIndex = bitIndex(bitpos);
				final CompactSetNode<K> subNode = (CompactSetNode<K>) nodes[bitIndex];
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = subNode.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!nestedResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactSetNode<K> subNodeNew = nestedResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case 0: {
					if (!USE_SPECIALIAZIONS && this.payloadArity() == 0 && this.nodeArity() == 1) {
						// escalate (singleton or empty) result
						return nestedResult;
					} else {
						// remove node
						final Object[] editableNodes = copyAndRemove(this.nodes, bitIndex);

						final CompactSetNode<K> thisNew = CompactSetNode.<K> valNodeOf(mutator,
										bitmap & ~bitpos, valmap, editableNodes, payloadArity);

						return Result.modified(thisNew);
					}
				}
				case 1: {
					if (!USE_SPECIALIAZIONS && this.payloadArity() == 0 && this.nodeArity() == 1) {
						// escalate (singleton or empty) result
						return nestedResult;
					} else {
						// inline value (move to front)
						final int valIndexNew = Integer.bitCount((valmap | bitpos) & (bitpos - 1));

						final Object[] editableNodes = copyAndMoveToFront(this.nodes, bitIndex,
										valIndexNew, subNodeNew.headKey());

						final CompactSetNode<K> thisNew = CompactSetNode.<K> valNodeOf(mutator,
										bitmap, valmap | bitpos, editableNodes,
										(byte) (payloadArity + 1));

						return Result.modified(thisNew);
					}
				}
				default: {
					// modify current node (set replacement node)
					if (isAllowedToEdit(this.mutator, mutator)) {
						// no copying if already editable
						this.nodes[bitIndex] = subNodeNew;
						return Result.modified(this);
					} else {
						final Object[] editableNodes = copyAndSet(this.nodes, bitIndex, subNodeNew);

						final CompactSetNode<K> thisNew = CompactSetNode.<K> valNodeOf(mutator,
										bitmap, valmap, editableNodes, payloadArity);

						return Result.modified(thisNew);
					}
				}
				}
			}

			return Result.unchanged(this);
		}

		// returns 0 <= mask <= 31
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

		@SuppressWarnings("unchecked")
		@Override
		K getKey(int index) {
			return (K) nodes[index];
		}

		@SuppressWarnings("unchecked")
		@Override
		public AbstractSetNode<K> getNode(int index) {
			final int offset = payloadArity;
			return (AbstractSetNode<K>) nodes[offset + index];
		}

		@Override
		SupplierIterator<K, K> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(nodes, 0, payloadArity);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<AbstractSetNode<K>> nodeIterator() {
			final int offset = payloadArity;

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
			return payloadArity != nodes.length;
		}

		@Override
		int nodeArity() {
			return nodes.length - payloadArity;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 0;
			result = prime * result + bitmap;
			result = prime * result + valmap;
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
			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}
			if (!Arrays.equals(nodes, that.nodes)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return Arrays.toString(nodes);
		}

		@Override
		byte sizePredicate() {
			if (USE_SPECIALIAZIONS) {
				return SIZE_MORE_THAN_ONE;
			} else {
				if (this.nodeArity() == 0 && this.payloadArity == 0) {
					return SIZE_EMPTY;
				} else if (this.nodeArity() == 0 && this.payloadArity == 1) {
					return SIZE_ONE;
				} else {
					return SIZE_MORE_THAN_ONE;
				}
			}
		}
	}

	// TODO: replace by immutable cons list
	private static final class HashCollisionSetNode<K> extends CompactSetNode<K> {
		private final K[] keys;
		private final int hash;

		HashCollisionSetNode(int hash, K[] keys) {
			this.keys = keys;
			this.hash = hash;

			assert payloadArity() >= 2;
		}

		@Override
		SupplierIterator<K, K> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(keys);
		}

		@Override
		public String toString() {
			return Arrays.toString(keys);
		}

		@Override
		Iterator<CompactSetNode<K>> nodeIterator() {
			return Collections.emptyIterator();
		}

		@Override
		K headKey() {
			assert hasPayload();
			return keys[0];
		}

		@Override
		public boolean containsKey(Object key, int keyHash, int shift) {
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
		public boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			if (this.hash == keyHash) {
				for (K k : keys) {
					if (cmp.compare(k, key) == 0) {
						return true;
					}
				}
			}
			return false;
		}

		/**
		 * Inserts an object if not yet present. Note, that this implementation
		 * always returns a new immutable {@link TrieSetGPCEDynamic} instance.
		 */
		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator,
						K key, int keyHash, Void val, int shift, Comparator<Object> cmp) {
			if (this.hash != keyHash) {
				return Result.modified(mergeNodes(this, this.hash, key, keyHash, shift));
			}

			if (containsKey(key, keyHash, shift, cmp)) {
				return Result.unchanged(this);
			}

			@SuppressWarnings("unchecked")
			final K[] keysNew = (K[]) copyAndInsert(keys, keys.length, key);
			return Result.modified(new HashCollisionSetNode<>(keyHash, keysNew));
		}

		/**
		 * Removes an object if present. Note, that this implementation always
		 * returns a new immutable {@link TrieSetGPCEDynamic} instance.
		 */
		@SuppressWarnings("unchecked")
		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator,
						K key, int keyHash, int shift, Comparator<Object> cmp) {
			for (int i = 0; i < keys.length; i++) {
				if (cmp.compare(keys[i], key) == 0) {
					if (this.arity() == 1) {
						return Result.modified(EMPTY_INPLACE_INDEX_NODE);
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node
						 * will be a) either be the new root returned, or b)
						 * unwrapped and inlined.
						 */
						final K theOtherKey = (i == 0) ? keys[1] : keys[0];
						return EMPTY_INPLACE_INDEX_NODE.updated(mutator, theOtherKey, keyHash,
										null, 0, cmp);
					} else {
						return Result.modified(new HashCollisionSetNode<>(keyHash,
										(K[]) copyAndRemove(keys, i)));
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

			HashCollisionSetNode<?> that = (HashCollisionSetNode<?>) other;

			if (hash != that.hash) {
				return false;
			}

			if (arity() != that.arity()) {
				return false;
			}

			/*
			 * Linear scan for each key, because of arbitrary element order.
			 */
			outerLoop: for (SupplierIterator<?, ?> it = that.payloadIterator(); it.hasNext();) {
				final Object otherKey = it.next();

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
		Optional<K> findByKey(Object key, int hash, int shift) {
			for (int i = 0; i < keys.length; i++) {
				final K _key = keys[i];
				if (key.equals(_key)) {
					return Optional.of(_key);
				}
			}
			return Optional.empty();
		}

		@Override
		Optional<K> findByKey(Object key, int hash, int shift, Comparator<Object> cmp) {
			for (int i = 0; i < keys.length; i++) {
				final K _key = keys[i];
				if (cmp.compare(key, _key) == 0) {
					return Optional.of(_key);
				}
			}
			return Optional.empty();
		}

		// TODO: generate instead of delegate
		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator,
						K key, int keyHash, Void val, int shift) {
			return updated(mutator, key, keyHash, val, shift,
							EqualityUtils.getDefaultEqualityComparator());
		}

		// TODO: generate instead of delegate
		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator,
						K key, int keyHash, int shift) {
			return removed(mutator, key, keyHash, shift,
							EqualityUtils.getDefaultEqualityComparator());
		}

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

		if (other instanceof TrieSetGPCEDynamic) {
			TrieSetGPCEDynamic that = (TrieSetGPCEDynamic) other;

			if (this.size() != that.size()) {
				return false;
			}

			return rootNode.equals(that.rootNode);
		}

		return super.equals(other);
	}

}
