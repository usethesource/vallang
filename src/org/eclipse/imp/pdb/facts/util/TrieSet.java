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
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("rawtypes")
public class TrieSet<K> extends AbstractImmutableSet<K> {

	@SuppressWarnings("unchecked")
	private static final TrieSet EMPTY_INPLACE_INDEX_SET = new TrieSet(
					CompactNode.EMPTY_INPLACE_INDEX_NODE, 0, 0);

	private final AbstractNode<K> rootNode;
	private final int hashCode;
	private final int cachedSize;

	TrieSet(AbstractNode<K> rootNode, int hashCode, int cachedSize) {
		this.rootNode = rootNode;
		this.hashCode = hashCode;
		this.cachedSize = cachedSize;
		assert invariant();
	}

	@SuppressWarnings("unchecked")
	public static final <K> ImmutableSet<K> of() {
		return TrieSet.EMPTY_INPLACE_INDEX_SET;
	}

	@SuppressWarnings("unchecked")
	public static final <K> ImmutableSet<K> of(K... keys) {
		ImmutableSet<K> result = TrieSet.EMPTY_INPLACE_INDEX_SET;
		
		for (K item : keys) {
			result = result.__insert(item);
		}
		
		return result;
	}		
	
	@SuppressWarnings("unchecked")
	public static final <K> TransientSet<K> transientOf() {
		return TrieSet.EMPTY_INPLACE_INDEX_SET.asTransient();
	}
	
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public static final <K> TransientSet<K> transientOf(K... keys) {
		final TransientSet<K> result = TrieSet.EMPTY_INPLACE_INDEX_SET.asTransient();		
		
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
	public TrieSet<K> __insert(K k) {
		return __insertEquivalent(k, equalityComparator());
	}

	@Override
	public TrieSet<K> __insertEquivalent(K key, Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final Result<K, Void, ? extends AbstractNode<K>> result = rootNode.updated(null, key,
						keyHash, 0, cmp);

		if (result.isModified()) {
			return new TrieSet<K>(result.getNode(), hashCode + keyHash, cachedSize + 1);
		}

		return this;
	}

	@Override
	public ImmutableSet<K> __insertAll(ImmutableSet<? extends K> set) {
		return __insertAllEquivalent(set, equalityComparator());
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
		return __retainAllEquivalent(set, equalityComparator());
	}

	@Override
	public ImmutableSet<K> __retainAllEquivalent(ImmutableSet<? extends K> set,
					Comparator<Object> cmp) {
		TransientSet<K> tmp = asTransient();
		tmp.__retainAllEquivalent(set, cmp);
		return tmp.freeze();
	}

	@Override
	public TrieSet<K> __remove(K k) {
		return __removeEquivalent(k, equalityComparator());
	}

	@Override
	public TrieSet<K> __removeEquivalent(K key, Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final Result<K, Void, ? extends AbstractNode<K>> result = rootNode.removed(null, key,
						keyHash, 0, cmp);

		if (result.isModified()) {
			// TODO: carry deleted value in result
			// assert result.hasReplacedValue();
			// final int valHash = result.getReplacedValue().hashCode();

			return new TrieSet<K>(result.getNode(), hashCode - keyHash, cachedSize - 1);
		}

		return this;
	}

	@Override
	public ImmutableSet<K> __removeAll(ImmutableSet<? extends K> set) {
		return __removeAllEquivalent(set, equalityComparator());
	}

	@Override
	public ImmutableSet<K> __removeAllEquivalent(ImmutableSet<? extends K> set,
					Comparator<Object> cmp) {
		TransientSet<K> tmp = asTransient();
		tmp.__removeAllEquivalent(set, cmp);
		return tmp.freeze();
	}

	@Override
	public boolean contains(Object o) {
		return rootNode.contains(o, o.hashCode(), 0, equalityComparator());
	}

	@Override
	public boolean containsEquivalent(Object o, Comparator<Object> cmp) {
		return rootNode.contains(o, o.hashCode(), 0, cmp);
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
		AbstractNode<K> valueNode;

		K lastValue = null;

		int stackLevel;

		int[] indexAndLength = new int[7 * 2];

		@SuppressWarnings("unchecked")
		AbstractNode<K>[] nodes = new AbstractNode[7];

		TrieSetIteratorWithFixedWidthStack(AbstractNode<K> rootNode) {
			stackLevel = 0;

			valueNode = rootNode;

			valueIndex = 0;
			valueLength = rootNode.valueArity();

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
					final AbstractNode<K> nextNode = nodes[stackLevel].getNode(nodeIndex);
					indexAndLength[2 * stackLevel] = (nodeIndex + 1);

					final int nextNodeValueArity = nextNode.valueArity();
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
		private AbstractNode<K> rootNode;
		private int hashCode;
		private int cachedSize;

		TransientTrieSet(TrieSet<K> TrieSet) {
			this.mutator = new AtomicReference<Thread>(Thread.currentThread());
			this.rootNode = TrieSet.rootNode;
			this.hashCode = TrieSet.hashCode;
			this.cachedSize = TrieSet.cachedSize;
			assert invariant();
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
			return rootNode.contains(o, o.hashCode(), 0, equalityComparator());
		}

		@Override
		public boolean containsEquivalent(Object o, Comparator<Object> cmp) {
			return rootNode.contains(o, o.hashCode(), 0, cmp);
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
			final Result<K, Void, ? extends AbstractNode<K>> result = rootNode.updated(mutator,
							key, keyHash, 0, cmp);

			if (result.isModified()) {
				rootNode = result.getNode();

				hashCode += keyHash;
				cachedSize += 1;

				assert invariant();
				return true;
			}

			assert invariant();
			return false;
		}

		@Override
		public boolean __retainAll(ImmutableSet<? extends K> set) {
			return __retainAllEquivalent(set, equalityComparator());
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
		public boolean __remove(K k) {
			return __removeEquivalent(k, equalityComparator());
		}

		@Override
		public boolean __removeEquivalent(K key, Comparator<Object> cmp) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final Result<K, Void, ? extends AbstractNode<K>> result = rootNode.removed(mutator,
							key, keyHash, 0, cmp);

			if (result.isModified()) {
				// TODO: carry deleted value in result
				// assert result.hasReplacedValue();
				// final int valHash = result.getReplacedValue().hashCode();

				rootNode = result.getNode();
				hashCode -= keyHash;
				cachedSize -= 1;

				assert invariant();
				return true;
			}

			assert invariant();
			return false;
		}

		@Override
		public boolean __removeAll(ImmutableSet<? extends K> set) {
			return __removeAllEquivalent(set, equalityComparator());
		}

		@Override
		public boolean __removeAllEquivalent(ImmutableSet<? extends K> set, Comparator<Object> cmp) {
			boolean modified = false;

			for (K key : set) {
				modified |= __removeEquivalent(key, cmp);
			}

			return modified;
		}

		@Override
		public boolean __insertAll(ImmutableSet<? extends K> set) {
			return __insertAllEquivalent(set, equalityComparator());
		}

		@Override
		public boolean __insertAllEquivalent(ImmutableSet<? extends K> set, Comparator<Object> cmp) {
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
			return new TrieSet<K>(rootNode, hashCode, cachedSize);
		}
	}

	static final class Result<T1, T2, N extends AbstractNode<T1>> {
		private final N result;
		private final T2 replacedValue;
		private final boolean isModified;

		// update: inserted/removed single element, element count changed
		public static <T1, T2, N extends AbstractNode<T1>> Result<T1, T2, N> modified(N node) {
			return new Result<>(node, null, true);
		}

		// update: replaced single mapping, but element count unchanged
		public static <T1, T2, N extends AbstractNode<T1>> Result<T1, T2, N> updated(N node,
						T2 replacedValue) {
			return new Result<>(node, replacedValue, true);
		}

		// update: neither element, nor element count changed
		public static <T1, T2, N extends AbstractNode<T1>> Result<T1, T2, N> unchanged(N node) {
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

	private static abstract class AbstractNode<K> {

		protected static final int BIT_PARTITION_SIZE = 5;
		protected static final int BIT_PARTITION_MASK = 0x1f;

		abstract boolean contains(Object key, int keyHash, int shift, Comparator<Object> comparator);

		abstract Optional<K> findByKey(Object key, int hash, int shift, Comparator<Object> cmp);

		abstract Result<K, Void, ? extends AbstractNode<K>> updated(
						AtomicReference<Thread> mutator, K key, int keyHash, int shift,
						Comparator<Object> cmp);

		abstract Result<K, Void, ? extends AbstractNode<K>> removed(
						AtomicReference<Thread> mutator, K key, int hash, int shift,
						Comparator<Object> cmp);

		static final boolean isAllowedToEdit(AtomicReference<Thread> x, AtomicReference<Thread> y) {
			return x != null && y != null && (x == y || x.get() == y.get());
		}

		abstract K getKey(int index);

		abstract AbstractNode<K> getNode(int index);

		abstract boolean hasNodes();

		abstract Iterator<? extends AbstractNode<K>> nodeIterator();

		abstract int nodeArity();

		abstract boolean hasValues();

		abstract SupplierIterator<K, K> valueIterator();

		abstract int valueArity();

		/**
		 * The arity of this trie node (i.e. number of values and nodes stored
		 * on this level).
		 * 
		 * @return sum of nodes and values stored within
		 */
		int arity() {
			return valueArity() + nodeArity();
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

	private static abstract class CompactNode<K> extends AbstractNode<K> {
		static final byte SIZE_EMPTY = 0b00;
		static final byte SIZE_ONE = 0b01;
		static final byte SIZE_MORE_THAN_ONE = 0b10;

		@Override
		abstract Result<K, Void, ? extends CompactNode<K>> updated(AtomicReference<Thread> mutator,
						K key, int keyHash, int shift, Comparator<Object> cmp);

		@Override
		abstract Result<K, Void, ? extends CompactNode<K>> removed(AtomicReference<Thread> mutator,
						K key, int hash, int shift, Comparator<Object> cmp);

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
			boolean inv1 = (size() - valueArity() >= 2 * (arity() - valueArity()));
			boolean inv2 = (this.arity() == 0) ? sizePredicate() == SIZE_EMPTY : true;
			boolean inv3 = (this.arity() == 1 && valueArity() == 1) ? sizePredicate() == SIZE_ONE
							: true;
			boolean inv4 = (this.arity() >= 2) ? sizePredicate() == SIZE_MORE_THAN_ONE : true;

			boolean inv5 = (this.nodeArity() >= 0) && (this.valueArity() >= 0)
							&& ((this.valueArity() + this.nodeArity()) == this.arity());

			return inv1 && inv2 && inv3 && inv4 && inv5;
		}

		static final CompactNode EMPTY_INPLACE_INDEX_NODE = new Value0Index0Node();

		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator, byte pos,
						CompactNode<K> node) {
			switch (pos) {
			case 0:
				return new SingletonNodeAtMask0Node<>(node);
			case 1:
				return new SingletonNodeAtMask1Node<>(node);
			case 2:
				return new SingletonNodeAtMask2Node<>(node);
			case 3:
				return new SingletonNodeAtMask3Node<>(node);
			case 4:
				return new SingletonNodeAtMask4Node<>(node);
			case 5:
				return new SingletonNodeAtMask5Node<>(node);
			case 6:
				return new SingletonNodeAtMask6Node<>(node);
			case 7:
				return new SingletonNodeAtMask7Node<>(node);
			case 8:
				return new SingletonNodeAtMask8Node<>(node);
			case 9:
				return new SingletonNodeAtMask9Node<>(node);
			case 10:
				return new SingletonNodeAtMask10Node<>(node);
			case 11:
				return new SingletonNodeAtMask11Node<>(node);
			case 12:
				return new SingletonNodeAtMask12Node<>(node);
			case 13:
				return new SingletonNodeAtMask13Node<>(node);
			case 14:
				return new SingletonNodeAtMask14Node<>(node);
			case 15:
				return new SingletonNodeAtMask15Node<>(node);
			case 16:
				return new SingletonNodeAtMask16Node<>(node);
			case 17:
				return new SingletonNodeAtMask17Node<>(node);
			case 18:
				return new SingletonNodeAtMask18Node<>(node);
			case 19:
				return new SingletonNodeAtMask19Node<>(node);
			case 20:
				return new SingletonNodeAtMask20Node<>(node);
			case 21:
				return new SingletonNodeAtMask21Node<>(node);
			case 22:
				return new SingletonNodeAtMask22Node<>(node);
			case 23:
				return new SingletonNodeAtMask23Node<>(node);
			case 24:
				return new SingletonNodeAtMask24Node<>(node);
			case 25:
				return new SingletonNodeAtMask25Node<>(node);
			case 26:
				return new SingletonNodeAtMask26Node<>(node);
			case 27:
				return new SingletonNodeAtMask27Node<>(node);
			case 28:
				return new SingletonNodeAtMask28Node<>(node);
			case 29:
				return new SingletonNodeAtMask29Node<>(node);
			case 30:
				return new SingletonNodeAtMask30Node<>(node);
			case 31:
				return new SingletonNodeAtMask31Node<>(node);

			default:
				throw new IllegalStateException("Position out of range.");
			}
		}

		@SuppressWarnings("unchecked")
		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator) {
			return EMPTY_INPLACE_INDEX_NODE;
		}

		// manually added
		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, byte npos1, CompactNode<K> node1, byte npos2, CompactNode<K> node2,
						byte npos3, CompactNode<K> node3, byte npos4, CompactNode<K> node4) {
			final int bitmap = (1 << pos1) | (1 << npos1) | (1 << npos2) | (1 << npos3)
							| (1 << npos4);
			final int valmap = (1 << pos1);

			return new MixedIndexNode<>(mutator, bitmap, valmap, new Object[] { key1, node1, node2,
							node3, node4 }, (byte) 1);
		}

		// manually added
		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, byte pos2, K key2, byte pos3, K key3, byte npos1,
						CompactNode<K> node1, byte npos2, CompactNode<K> node2) {
			final int bitmap = (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << npos1)
							| (1 << npos2);
			final int valmap = (1 << pos1) | (1 << pos2) | (1 << pos3);

			return new MixedIndexNode<>(mutator, bitmap, valmap, new Object[] { key1, key2, key3,
							node1, node2 }, (byte) 3);
		}

		// manually added
		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, byte pos2, K key2, byte npos1, CompactNode<K> node1, byte npos2,
						CompactNode<K> node2, byte npos3, CompactNode<K> node3) {
			final int bitmap = (1 << pos1) | (1 << pos2) | (1 << npos1) | (1 << npos2)
							| (1 << npos3);
			final int valmap = (1 << pos1) | (1 << pos2);

			return new MixedIndexNode<>(mutator, bitmap, valmap, new Object[] { key1, key2, node1,
							node2, node3 }, (byte) 2);
		}

		// manually added
		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, byte pos2, K key2, byte pos3, K key3, byte pos4, K key4,
						byte npos1, CompactNode<K> node1) {
			final int bitmap = (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << pos4) | (1 << npos1);
			final int valmap = (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << pos4);

			return new MixedIndexNode<>(mutator, bitmap, valmap, new Object[] { key1, key2, key3,
							key4, node1 }, (byte) 4);
		}

		// manually added
		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, byte pos2, K key2, byte pos3, K key3, byte pos4, K key4, byte pos5,
						K key5) {
			final int valmap = (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << pos4) | (1 << pos5);

			return new MixedIndexNode<>(mutator, valmap, valmap, new Object[] { key1, key2, key3,
							key4, key5 }, (byte) 5);
		}

		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator, byte npos1,
						CompactNode<K> node1, byte npos2, CompactNode<K> node2) {
			return new Value0Index2Node<>(mutator, npos1, node1, npos2, node2);
		}

		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator, byte npos1,
						CompactNode<K> node1, byte npos2, CompactNode<K> node2, byte npos3,
						CompactNode<K> node3) {
			return new Value0Index3Node<>(mutator, npos1, node1, npos2, node2, npos3, node3);
		}

		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator, byte npos1,
						CompactNode<K> node1, byte npos2, CompactNode<K> node2, byte npos3,
						CompactNode<K> node3, byte npos4, CompactNode<K> node4) {
			return new Value0Index4Node<>(mutator, npos1, node1, npos2, node2, npos3, node3, npos4,
							node4);
		}

		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator, byte pos1, K key1) {
			return new Value1Index0Node<>(mutator, pos1, key1);
		}

		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, byte npos1, CompactNode<K> node1) {
			return new Value1Index1Node<>(mutator, pos1, key1, npos1, node1);
		}

		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, byte npos1, CompactNode<K> node1, byte npos2, CompactNode<K> node2) {
			return new Value1Index2Node<>(mutator, pos1, key1, npos1, node1, npos2, node2);
		}

		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, byte npos1, CompactNode<K> node1, byte npos2, CompactNode<K> node2,
						byte npos3, CompactNode<K> node3) {
			return new Value1Index3Node<>(mutator, pos1, key1, npos1, node1, npos2, node2, npos3,
							node3);
		}

		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, byte pos2, K key2) {
			return new Value2Index0Node<>(mutator, pos1, key1, pos2, key2);
		}

		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, byte pos2, K key2, byte npos1, CompactNode<K> node1) {
			return new Value2Index1Node<>(mutator, pos1, key1, pos2, key2, npos1, node1);
		}

		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, byte pos2, K key2, byte npos1, CompactNode<K> node1, byte npos2,
						CompactNode<K> node2) {
			return new Value2Index2Node<>(mutator, pos1, key1, pos2, key2, npos1, node1, npos2,
							node2);
		}

		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, byte pos2, K key2, byte pos3, K key3) {
			return new Value3Index0Node<>(mutator, pos1, key1, pos2, key2, pos3, key3);
		}

		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, byte pos2, K key2, byte pos3, K key3, byte npos1,
						CompactNode<K> node1) {
			return new Value3Index1Node<>(mutator, pos1, key1, pos2, key2, pos3, key3, npos1, node1);
		}

		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, byte pos2, K key2, byte pos3, K key3, byte pos4, K key4) {
			return new Value4Index0Node<>(mutator, pos1, key1, pos2, key2, pos3, key3, pos4, key4);
		}

		static final <K> CompactNode<K> valNodeOf(AtomicReference<Thread> mutator, int bitmap,
						int valmap, Object[] nodes, byte valueArity) {
			return new MixedIndexNode<>(mutator, bitmap, valmap, nodes, valueArity);
		}

		@SuppressWarnings("unchecked")
		static final <K> CompactNode<K> mergeNodes(K key0, int keyHash0, K key1, int keyHash1,
						int shift) {
			assert key0.equals(key1) == false;

			if (keyHash0 == keyHash1) {
				return new HashCollisionNode<>(keyHash0, (K[]) new Object[] { key0, key1 });
			}

			final int mask0 = (keyHash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (keyHash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level
				final Object[] nodes = new Object[4];

				if (mask0 < mask1) {
					nodes[0] = key0;
					nodes[1] = key1;

					return valNodeOf(null, (byte) mask0, key0, (byte) mask1, key1);
				} else {
					nodes[0] = key1;
					nodes[1] = key0;

					return valNodeOf(null, (byte) mask1, key1, (byte) mask0, key0);
				}
			} else {
				// values fit on next level
				final CompactNode<K> node = mergeNodes(key0, keyHash0, key1, keyHash1, shift
								+ BIT_PARTITION_SIZE);

				return valNodeOf(null, (byte) mask0, node);
			}
		}

		static final <K> CompactNode<K> mergeNodes(CompactNode<K> node0, int keyHash0, K key1,
						int keyHash1, int shift) {
			final int mask0 = (keyHash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (keyHash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level
				final Object[] nodes = new Object[2];

				// store values before node
				nodes[0] = key1;
				nodes[1] = node0;

				return valNodeOf(null, (byte) mask1, key1, (byte) mask0, node0);
			} else {
				// values fit on next level
				final CompactNode<K> node = mergeNodes(node0, keyHash0, key1, keyHash1, shift
								+ BIT_PARTITION_SIZE);

				return valNodeOf(null, (byte) mask0, node);
			}
		}
	}

	private static final class MixedIndexNode<K> extends CompactNode<K> {
		private AtomicReference<Thread> mutator;

		private Object[] nodes;
		final private int bitmap;
		final private int valmap;
		final private byte valueArity;

		MixedIndexNode(AtomicReference<Thread> mutator, int bitmap, int valmap, Object[] nodes,
						byte valueArity) {
			assert (Integer.bitCount(valmap) + Integer.bitCount(bitmap ^ valmap) == nodes.length);

			this.mutator = mutator;

			this.nodes = nodes;
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.valueArity = valueArity;

			assert (valueArity == Integer.bitCount(valmap));
			assert (valueArity() >= 2 || nodeArity() >= 1); // =
															// SIZE_MORE_THAN_ONE

			// for (int i = 0; i < valueArity; i++)
			// assert ((nodes[i] instanceof CompactNode) == false);
			//
			// for (int i = valueArity; i < nodes.length; i++)
			// assert ((nodes[i] instanceof CompactNode) == true);

			// assert invariant
			assert nodeInvariant();
		}

		final int bitIndex(int bitpos) {
			return valueArity + Integer.bitCount((bitmap ^ valmap) & (bitpos - 1));
		}

		final int valIndex(int bitpos) {
			return Integer.bitCount(valmap & (bitpos - 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		boolean contains(Object key, int hash, int shift, Comparator<Object> cmp) {
			final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) {
				return cmp.compare(nodes[valIndex(bitpos)], key) == 0;
			}

			if ((bitmap & bitpos) != 0) {
				return ((AbstractNode<K>) nodes[bitIndex(bitpos)]).contains(key, hash, shift
								+ BIT_PARTITION_SIZE, cmp);
			}

			return false;
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
				final AbstractNode<K> subNode = ((AbstractNode<K>) nodes[bitIndex(bitpos)]);

				return subNode.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Result<K, Void, ? extends CompactNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				final Object currentKey = nodes[valIndex];

				if (cmp.compare(currentKey, key) == 0) {
					return Result.unchanged(this);
				} else {
					final CompactNode<K> nodeNew = mergeNodes((K) nodes[valIndex],
									nodes[valIndex].hashCode(), key, keyHash, shift
													+ BIT_PARTITION_SIZE);

					final int offset = (valueArity - 1);
					final int index = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
									& (bitpos - 1));

					final Object[] editableNodes = copyAndMoveToBack(this.nodes, valIndex, offset
									+ index, nodeNew);

					final CompactNode<K> thisNew = CompactNode.<K> valNodeOf(mutator, bitmap
									| bitpos, valmap & ~bitpos, editableNodes,
									(byte) (valueArity - 1));

					return Result.modified(thisNew);
				}
			} else if ((bitmap & bitpos) != 0) { // node (not value)
				final int bitIndex = bitIndex(bitpos);
				final CompactNode<K> subNode = (CompactNode<K>) nodes[bitIndex];

				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = subNode.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew;

				// modify current node (set replacement node)
				if (isAllowedToEdit(this.mutator, mutator)) {
					// no copying if already editable
					this.nodes[bitIndex] = subNodeResult.getNode();
					thisNew = this;
				} else {
					final Object[] editableNodes = copyAndSet(this.nodes, bitIndex,
									subNodeResult.getNode());

					thisNew = CompactNode.<K> valNodeOf(mutator, bitmap, valmap, editableNodes,
									valueArity);
				}

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else {
				// no value
				final Object[] editableNodes = copyAndInsert(this.nodes, valIndex(bitpos), key);

				final CompactNode<K> thisNew = CompactNode.<K> valNodeOf(mutator, bitmap | bitpos,
								valmap | bitpos, editableNodes, (byte) (valueArity + 1));

				return Result.modified(thisNew);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Result<K, Void, ? extends CompactNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> comparator) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (comparator.compare(nodes[valIndex], key) != 0) {
					return Result.unchanged(this);
				}

				if (this.arity() == 5) {
					switch (this.valueArity) { // 0 <= valueArity <= 5
					case 1: {
						final int nmap = ((bitmap & ~bitpos) ^ (valmap & ~bitpos));
						final byte npos1 = recoverMask(nmap, (byte) 1);
						final byte npos2 = recoverMask(nmap, (byte) 2);
						final byte npos3 = recoverMask(nmap, (byte) 3);
						final byte npos4 = recoverMask(nmap, (byte) 4);
						final CompactNode<K> node1 = (CompactNode<K>) nodes[valueArity + 0];
						final CompactNode<K> node2 = (CompactNode<K>) nodes[valueArity + 1];
						final CompactNode<K> node3 = (CompactNode<K>) nodes[valueArity + 2];
						final CompactNode<K> node4 = (CompactNode<K>) nodes[valueArity + 3];

						return Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2,
										npos3, node3, npos4, node4));
					}
					case 2: {
						final int map = (valmap & ~bitpos);
						final byte pos1 = recoverMask(map, (byte) 1);
						final K key1;

						final int nmap = ((bitmap & ~bitpos) ^ (valmap & ~bitpos));
						final byte npos1 = recoverMask(nmap, (byte) 1);
						final byte npos2 = recoverMask(nmap, (byte) 2);
						final byte npos3 = recoverMask(nmap, (byte) 3);
						final CompactNode<K> node1 = (CompactNode<K>) nodes[valueArity + 0];
						final CompactNode<K> node2 = (CompactNode<K>) nodes[valueArity + 1];
						final CompactNode<K> node3 = (CompactNode<K>) nodes[valueArity + 2];

						if (mask < pos1) {
							key1 = (K) nodes[1];
						} else {
							key1 = (K) nodes[0];
						}

						return Result.modified(valNodeOf(mutator, pos1, key1, npos1, node1, npos2,
										node2, npos3, node3));
					}
					case 3: {
						final int map = (valmap & ~bitpos);
						final byte pos1 = recoverMask(map, (byte) 1);
						final byte pos2 = recoverMask(map, (byte) 2);
						final K key1;
						final K key2;

						final int nmap = ((bitmap & ~bitpos) ^ (valmap & ~bitpos));
						final byte npos1 = recoverMask(nmap, (byte) 1);
						final byte npos2 = recoverMask(nmap, (byte) 2);
						final CompactNode<K> node1 = (CompactNode<K>) nodes[valueArity + 0];
						final CompactNode<K> node2 = (CompactNode<K>) nodes[valueArity + 1];

						if (mask < pos1) {
							key1 = (K) nodes[1];
							key2 = (K) nodes[2];
						} else if (mask < pos2) {
							key1 = (K) nodes[0];
							key2 = (K) nodes[2];
						} else {
							key1 = (K) nodes[0];
							key2 = (K) nodes[1];
						}

						return Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, npos1,
										node1, npos2, node2));
					}
					case 4: {
						final int map = (valmap & ~bitpos);
						final byte pos1 = recoverMask(map, (byte) 1);
						final byte pos2 = recoverMask(map, (byte) 2);
						final byte pos3 = recoverMask(map, (byte) 3);
						final K key1;
						final K key2;
						final K key3;

						final int nmap = ((bitmap & ~bitpos) ^ (valmap & ~bitpos));
						final byte npos1 = recoverMask(nmap, (byte) 1);
						final CompactNode<K> node1 = (CompactNode<K>) nodes[valueArity + 0];

						if (mask < pos1) {
							key1 = (K) nodes[1];
							key2 = (K) nodes[2];
							key3 = (K) nodes[3];
						} else if (mask < pos2) {
							key1 = (K) nodes[0];
							key2 = (K) nodes[2];
							key3 = (K) nodes[3];
						} else if (mask < pos3) {
							key1 = (K) nodes[0];
							key2 = (K) nodes[1];
							key3 = (K) nodes[3];
						} else {
							key1 = (K) nodes[0];
							key2 = (K) nodes[1];
							key3 = (K) nodes[2];
						}

						return Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, pos3,
										key3, npos1, node1));
					}
					case 5: {
						final int map = (valmap & ~bitpos);
						final byte pos1 = recoverMask(map, (byte) 1);
						final byte pos2 = recoverMask(map, (byte) 2);
						final byte pos3 = recoverMask(map, (byte) 3);
						final byte pos4 = recoverMask(map, (byte) 4);
						final K key1;
						final K key2;
						final K key3;
						final K key4;

						if (mask < pos1) {
							key1 = (K) nodes[1];
							key2 = (K) nodes[2];
							key3 = (K) nodes[3];
							key4 = (K) nodes[4];
						} else if (mask < pos2) {
							key1 = (K) nodes[0];
							key2 = (K) nodes[2];
							key3 = (K) nodes[3];
							key4 = (K) nodes[4];
						} else if (mask < pos3) {
							key1 = (K) nodes[0];
							key2 = (K) nodes[1];
							key3 = (K) nodes[3];
							key4 = (K) nodes[4];
						} else if (mask < pos4) {
							key1 = (K) nodes[0];
							key2 = (K) nodes[1];
							key3 = (K) nodes[2];
							key4 = (K) nodes[4];
						} else {
							key1 = (K) nodes[0];
							key2 = (K) nodes[1];
							key3 = (K) nodes[2];
							key4 = (K) nodes[3];
						}

						return Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, pos3,
										key3, pos4, key4));
					}
					}
				} else {
					final Object[] editableNodes = copyAndRemove(this.nodes, valIndex);

					final CompactNode<K> thisNew = CompactNode.<K> valNodeOf(mutator, this.bitmap
									& ~bitpos, this.valmap & ~bitpos, editableNodes,
									(byte) (valueArity - 1));

					return Result.modified(thisNew);
				}
			} else if ((bitmap & bitpos) != 0) { // node (not value)
				final int bitIndex = bitIndex(bitpos);
				final CompactNode<K> subNode = (CompactNode<K>) nodes[bitIndex];
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = subNode.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, comparator);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case 0: {
					// remove node
					if (this.arity() == 5) {
						switch (this.nodeArity()) {
						case 1: {
							final int map = valmap;
							final byte pos1 = recoverMask(map, (byte) 1);
							final byte pos2 = recoverMask(map, (byte) 2);
							final byte pos3 = recoverMask(map, (byte) 3);
							final byte pos4 = recoverMask(map, (byte) 4);
							final K key1 = (K) nodes[0];
							final K key2 = (K) nodes[1];
							final K key3 = (K) nodes[2];
							final K key4 = (K) nodes[3];

							return Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, pos3,
											key3, pos4, key4));
						}
						case 2: {
							final int map = valmap;
							final byte pos1 = recoverMask(map, (byte) 1);
							final byte pos2 = recoverMask(map, (byte) 2);
							final byte pos3 = recoverMask(map, (byte) 3);
							final K key1 = (K) nodes[0];
							final K key2 = (K) nodes[1];
							final K key3 = (K) nodes[2];

							final int nmap = ((bitmap & ~bitpos) ^ valmap);
							final byte npos1 = recoverMask(nmap, (byte) 1);
							final CompactNode<K> node1;

							if (mask < npos1) {
								node1 = (CompactNode<K>) nodes[valueArity + 1];
							} else {
								node1 = (CompactNode<K>) nodes[valueArity + 0];
							}

							return Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, pos3,
											key3, npos1, node1));
						}
						case 3: {
							final int map = valmap;
							final byte pos1 = recoverMask(map, (byte) 1);
							final byte pos2 = recoverMask(map, (byte) 2);
							final K key1 = (K) nodes[0];
							final K key2 = (K) nodes[1];

							final int nmap = ((bitmap & ~bitpos) ^ valmap);
							final byte npos1 = recoverMask(nmap, (byte) 1);
							final byte npos2 = recoverMask(nmap, (byte) 2);
							final CompactNode<K> node1;
							final CompactNode<K> node2;

							if (mask < npos1) {
								node1 = (CompactNode<K>) nodes[valueArity + 1];
								node2 = (CompactNode<K>) nodes[valueArity + 2];
							} else if (mask < npos2) {
								node1 = (CompactNode<K>) nodes[valueArity + 0];
								node2 = (CompactNode<K>) nodes[valueArity + 2];
							} else {
								node1 = (CompactNode<K>) nodes[valueArity + 0];
								node2 = (CompactNode<K>) nodes[valueArity + 1];
							}

							return Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2,
											npos1, node1, npos2, node2));
						}
						case 4: {
							final int map = valmap;
							final byte pos1 = recoverMask(map, (byte) 1);
							final K key1 = (K) nodes[0];

							final int nmap = ((bitmap & ~bitpos) ^ valmap);
							final byte npos1 = recoverMask(nmap, (byte) 1);
							final byte npos2 = recoverMask(nmap, (byte) 2);
							final byte npos3 = recoverMask(nmap, (byte) 3);
							final CompactNode<K> node1;
							final CompactNode<K> node2;
							final CompactNode<K> node3;

							if (mask < npos1) {
								node1 = (CompactNode<K>) nodes[valueArity + 1];
								node2 = (CompactNode<K>) nodes[valueArity + 2];
								node3 = (CompactNode<K>) nodes[valueArity + 3];
							} else if (mask < npos2) {
								node1 = (CompactNode<K>) nodes[valueArity + 0];
								node2 = (CompactNode<K>) nodes[valueArity + 2];
								node3 = (CompactNode<K>) nodes[valueArity + 3];
							} else if (mask < npos3) {
								node1 = (CompactNode<K>) nodes[valueArity + 0];
								node2 = (CompactNode<K>) nodes[valueArity + 1];
								node3 = (CompactNode<K>) nodes[valueArity + 3];
							} else {
								node1 = (CompactNode<K>) nodes[valueArity + 0];
								node2 = (CompactNode<K>) nodes[valueArity + 1];
								node3 = (CompactNode<K>) nodes[valueArity + 2];
							}

							return Result.modified(valNodeOf(mutator, pos1, key1, npos1, node1,
											npos2, node2, npos3, node3));
						}
						case 5: {
							final int nmap = ((bitmap & ~bitpos) ^ valmap);
							final byte npos1 = recoverMask(nmap, (byte) 1);
							final byte npos2 = recoverMask(nmap, (byte) 2);
							final byte npos3 = recoverMask(nmap, (byte) 3);
							final byte npos4 = recoverMask(nmap, (byte) 4);
							final CompactNode<K> node1;
							final CompactNode<K> node2;
							final CompactNode<K> node3;
							final CompactNode<K> node4;

							if (mask < npos1) {
								node1 = (CompactNode<K>) nodes[valueArity + 1];
								node2 = (CompactNode<K>) nodes[valueArity + 2];
								node3 = (CompactNode<K>) nodes[valueArity + 3];
								node4 = (CompactNode<K>) nodes[valueArity + 4];
							} else if (mask < npos2) {
								node1 = (CompactNode<K>) nodes[valueArity + 0];
								node2 = (CompactNode<K>) nodes[valueArity + 2];
								node3 = (CompactNode<K>) nodes[valueArity + 3];
								node4 = (CompactNode<K>) nodes[valueArity + 4];
							} else if (mask < npos3) {
								node1 = (CompactNode<K>) nodes[valueArity + 0];
								node2 = (CompactNode<K>) nodes[valueArity + 1];
								node3 = (CompactNode<K>) nodes[valueArity + 3];
								node4 = (CompactNode<K>) nodes[valueArity + 4];
							} else if (mask < npos4) {
								node1 = (CompactNode<K>) nodes[valueArity + 0];
								node2 = (CompactNode<K>) nodes[valueArity + 1];
								node3 = (CompactNode<K>) nodes[valueArity + 2];
								node4 = (CompactNode<K>) nodes[valueArity + 4];
							} else {
								node1 = (CompactNode<K>) nodes[valueArity + 0];
								node2 = (CompactNode<K>) nodes[valueArity + 1];
								node3 = (CompactNode<K>) nodes[valueArity + 2];
								node4 = (CompactNode<K>) nodes[valueArity + 3];
							}

							return Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2,
											npos3, node3, npos4, node4));
						}
						}
					} else {
						final Object[] editableNodes = copyAndRemove(this.nodes, bitIndex);

						final CompactNode<K> thisNew = CompactNode.<K> valNodeOf(mutator, bitmap
										& ~bitpos, valmap, editableNodes, valueArity);

						return Result.modified(thisNew);
					}
				}
				case 1: {
					// inline value (move to front)
					final int valIndexNew = Integer.bitCount((valmap | bitpos) & (bitpos - 1));

					final Object[] editableNodes = copyAndMoveToFront(this.nodes, bitIndex,
									valIndexNew, subNodeNew.headKey());

					final CompactNode<K> thisNew = CompactNode.<K> valNodeOf(mutator, bitmap,
									valmap | bitpos, editableNodes, (byte) (valueArity + 1));

					return Result.modified(thisNew);
				}
				default: {
					// modify current node (set replacement node)
					if (isAllowedToEdit(this.mutator, mutator)) {
						// no copying if already editable
						this.nodes[bitIndex] = subNodeNew;
						return Result.modified(this);
					} else {
						final Object[] editableNodes = copyAndSet(this.nodes, bitIndex, subNodeNew);

						final CompactNode<K> thisNew = CompactNode.<K> valNodeOf(mutator, bitmap,
										valmap, editableNodes, valueArity);

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
		public AbstractNode<K> getNode(int index) {
			final int offset = valueArity;
			return (AbstractNode<K>) nodes[offset + index];
		}

		@Override
		SupplierIterator<K, K> valueIterator() {
			return ArrayKeyValueIterator.of(nodes, 0, valueArity);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<AbstractNode<K>> nodeIterator() {
			final int offset = valueArity;

			for (int i = offset; i < nodes.length - offset; i++) {
				assert ((nodes[i] instanceof AbstractNode) == true);
			}

			return (Iterator) ArrayIterator.of(nodes, offset, nodes.length - offset);
		}

		@SuppressWarnings("unchecked")
		@Override
		K headKey() {
			assert hasValues();
			return (K) nodes[0];
		}

		@Override
		boolean hasValues() {
			return valueArity != 0;
		}

		@Override
		int valueArity() {
			return valueArity;
		}

		@Override
		boolean hasNodes() {
			return valueArity != nodes.length;
		}

		@Override
		int nodeArity() {
			return nodes.length - valueArity;
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
			MixedIndexNode<?> that = (MixedIndexNode<?>) other;
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
			return SIZE_MORE_THAN_ONE;
		}
	}

	// TODO: replace by immutable cons list
	private static final class HashCollisionNode<K> extends CompactNode<K> {
		private final K[] keys;
		private final int hash;

		HashCollisionNode(int hash, K[] keys) {
			this.keys = keys;
			this.hash = hash;

			assert valueArity() >= 2;
		}

		@Override
		SupplierIterator<K, K> valueIterator() {
			return ArrayKeyValueIterator.of(keys);
		}

		@Override
		public String toString() {
			return Arrays.toString(keys);
		}

		@Override
		Iterator<CompactNode<K>> nodeIterator() {
			return Collections.emptyIterator();
		}

		@Override
		K headKey() {
			assert hasValues();
			return keys[0];
		}

		@Override
		public boolean contains(Object key, int keyHash, int shift, Comparator<Object> cmp) {
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
		 * always returns a new immutable {@link TrieSet} instance.
		 */
		@Override
		Result<K, Void, ? extends CompactNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			if (this.hash != keyHash) {
				return Result.modified(mergeNodes(this, this.hash, key, keyHash, shift));
			}

			if (contains(key, keyHash, shift, cmp)) {
				return Result.unchanged(this);
			}

			@SuppressWarnings("unchecked")
			final K[] keysNew = (K[]) copyAndInsert(keys, keys.length, key);
			return Result.modified(new HashCollisionNode<>(keyHash, keysNew));
		}

		/**
		 * Removes an object if present. Note, that this implementation always
		 * returns a new immutable {@link TrieSet} instance.
		 */
		@SuppressWarnings("unchecked")
		@Override
		Result<K, Void, ? extends CompactNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			for (int i = 0; i < keys.length; i++) {
				if (cmp.compare(keys[i], key) == 0) {
					if (this.arity() == 1) {
						return Result.modified(CompactNode.<K> valNodeOf(mutator));
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node
						 * will be a) either be the new root returned, or b)
						 * unwrapped and inlined.
						 */
						final K theOtherKey = (i == 0) ? keys[1] : keys[0];
						return CompactNode.<K> valNodeOf(mutator).updated(mutator, theOtherKey,
										keyHash, 0, cmp);
					} else {
						return Result.modified(new HashCollisionNode<>(keyHash,
										(K[]) copyAndRemove(keys, i)));
					}
				}
			}
			return Result.unchanged(this);
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
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
			return valueArity();
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
		public CompactNode<K> getNode(int index) {
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

			HashCollisionNode<?> that = (HashCollisionNode<?>) other;

			if (hash != that.hash) {
				return false;
			}

			if (arity() != that.arity()) {
				return false;
			}

			/*
			 * Linear scan for each key, because of arbitrary element order.
			 */
			outerLoop: for (SupplierIterator<?, ?> it = that.valueIterator(); it.hasNext();) {
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
		Optional<K> findByKey(Object key, int hash, int shift, Comparator<Object> cmp) {
			for (int i = 0; i < keys.length; i++) {
				final K _key = keys[i];
				if (cmp.compare(key, _key) == 0) {
					return Optional.of(_key);
				}
			}
			return Optional.empty();
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

		if (other instanceof TrieSet) {
			TrieSet that = (TrieSet) other;

			if (this.size() != that.size()) {
				return false;
			}

			return rootNode.equals(that.rootNode);
		}

		return super.equals(other);
	}

	private abstract static class AbstractSingletonNode<K> extends CompactNode<K> {

		protected abstract byte npos1();

		protected final CompactNode<K> node1;

		AbstractSingletonNode(CompactNode<K> node1) {
			this.node1 = node1;
			assert nodeInvariant();
		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node1.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew = valNodeOf(mutator, mask, subNodeResult.getNode());

				return Result.modified(thisNew);
			}

			// no value
			final CompactNode<K> thisNew = valNodeOf(mutator, mask, key, npos1(), node1);
			return Result.modified(thisNew);
		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_EMPTY:
				case SIZE_ONE:
					// escalate (singleton or empty) result
					return subNodeResult;

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)
					final CompactNode<K> thisNew = valNodeOf(mutator, mask, subNodeNew);
					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Invalid size state.");
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean contains(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				return node1.contains(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@Override
		K getKey(int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		public AbstractNode<K> getNode(int index) {
			if (index == 0) {
				return node1;
			} else {
				throw new IndexOutOfBoundsException();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<AbstractNode<K>> nodeIterator() {
			return ArrayIterator.<AbstractNode<K>> of(new AbstractNode[] { node1 });
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 1;
		}

		@Override
		SupplierIterator<K, K> valueIterator() {
			return EmptySupplierIterator.emptyIterator();
		}

		@Override
		boolean hasValues() {
			return false;
		}

		@Override
		int valueArity() {
			return 0;
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + npos1();
			result = prime * result + node1.hashCode();
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
			AbstractSingletonNode<?> that = (AbstractSingletonNode<?>) other;

			if (!node1.equals(that.node1)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s]", node1);
		}

		@Override
		K headKey() {
			throw new UnsupportedOperationException("No key in this kind of node.");
		}

	}

	private static final class SingletonNodeAtMask0Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 0;
		}

		SingletonNodeAtMask0Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask1Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 1;
		}

		SingletonNodeAtMask1Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask2Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 2;
		}

		SingletonNodeAtMask2Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask3Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 3;
		}

		SingletonNodeAtMask3Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask4Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 4;
		}

		SingletonNodeAtMask4Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask5Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 5;
		}

		SingletonNodeAtMask5Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask6Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 6;
		}

		SingletonNodeAtMask6Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask7Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 7;
		}

		SingletonNodeAtMask7Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask8Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 8;
		}

		SingletonNodeAtMask8Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask9Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 9;
		}

		SingletonNodeAtMask9Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask10Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 10;
		}

		SingletonNodeAtMask10Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask11Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 11;
		}

		SingletonNodeAtMask11Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask12Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 12;
		}

		SingletonNodeAtMask12Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask13Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 13;
		}

		SingletonNodeAtMask13Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask14Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 14;
		}

		SingletonNodeAtMask14Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask15Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 15;
		}

		SingletonNodeAtMask15Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask16Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 16;
		}

		SingletonNodeAtMask16Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask17Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 17;
		}

		SingletonNodeAtMask17Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask18Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 18;
		}

		SingletonNodeAtMask18Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask19Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 19;
		}

		SingletonNodeAtMask19Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask20Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 20;
		}

		SingletonNodeAtMask20Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask21Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 21;
		}

		SingletonNodeAtMask21Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask22Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 22;
		}

		SingletonNodeAtMask22Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask23Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 23;
		}

		SingletonNodeAtMask23Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask24Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 24;
		}

		SingletonNodeAtMask24Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask25Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 25;
		}

		SingletonNodeAtMask25Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask26Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 26;
		}

		SingletonNodeAtMask26Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask27Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 27;
		}

		SingletonNodeAtMask27Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask28Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 28;
		}

		SingletonNodeAtMask28Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask29Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 29;
		}

		SingletonNodeAtMask29Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask30Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 30;
		}

		SingletonNodeAtMask30Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask31Node<K> extends AbstractSingletonNode<K> {

		@Override
		protected byte npos1() {
			return 31;
		}

		SingletonNodeAtMask31Node(CompactNode<K> node1) {
			super(node1);
		}

	}

	private static final class Value0Index0Node<K> extends CompactNode<K> {

		Value0Index0Node() {
		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			// no value

			final CompactNode<K> thisNew = valNodeOf(mutator, mask, key);
			return Result.modified(thisNew);

		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {

			return Result.unchanged(this);
		}

		@Override
		boolean contains(Object key, int keyHash, int shift, Comparator<Object> cmp) {

			return false;
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {

			return Optional.empty();
		}

		@Override
		Iterator<CompactNode<K>> nodeIterator() {
			return Collections.emptyIterator();
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
		SupplierIterator<K, K> valueIterator() {
			return EmptySupplierIterator.emptyIterator();
		}

		@Override
		boolean hasValues() {
			return false;
		}

		@Override
		int valueArity() {
			return 0;
		}

		@Override
		K getKey(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		public CompactNode<K> getNode(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		byte sizePredicate() {
			return SIZE_EMPTY;
		}

		@Override
		public int hashCode() {
			return 31;
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

		@Override
		public String toString() {
			return "[]";
		}

		@Override
		K headKey() {
			throw new UnsupportedOperationException("No key in this kind of node.");
		}

	}

	private static final class Value0Index2Node<K> extends CompactNode<K> {
		private final byte npos1;
		private final CompactNode<K> node1;
		private final byte npos2;
		private final CompactNode<K> node2;

		Value0Index2Node(AtomicReference<Thread> mutator, byte npos1, CompactNode<K> node1,
						byte npos2, CompactNode<K> node2) {
			this.npos1 = npos1;
			this.node1 = node1;
			this.npos2 = npos2;
			this.node2 = node2;
			assert nodeInvariant();
		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node1.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew = valNodeOf(mutator, mask, subNodeResult.getNode(),
								npos2, node2);

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node2.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew = valNodeOf(mutator, npos1, node1, mask,
								subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			}

			// no value

			final CompactNode<K> thisNew = valNodeOf(mutator, mask, key, npos1, node1, npos2, node2);
			return Result.modified(thisNew);

		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE: {
					// inline value

					final CompactNode<K> thisNew = valNodeOf(mutator, mask, subNodeNew.headKey(),
									npos2, node2);
					return Result.modified(thisNew);

				}
				case SIZE_MORE_THAN_ONE: {
					// modify current node (set replacement node)

					final CompactNode<K> thisNew = valNodeOf(mutator, mask, subNodeNew, npos2,
									node2);

					return Result.modified(thisNew);
				}
				default: {
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE: {
					// inline value

					final CompactNode<K> thisNew = valNodeOf(mutator, mask, subNodeNew.headKey(),
									npos1, node1);
					return Result.modified(thisNew);

				}
				case SIZE_MORE_THAN_ONE: {
					// modify current node (set replacement node)

					final CompactNode<K> thisNew = valNodeOf(mutator, npos1, node1, mask,
									subNodeNew);

					return Result.modified(thisNew);
				}
				default: {
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean contains(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.contains(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.contains(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactNode<K>> nodeIterator() {
			return ArrayIterator.<CompactNode<K>> of(new CompactNode[] { node1, node2 });
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 2;
		}

		@Override
		SupplierIterator<K, K> valueIterator() {
			return EmptySupplierIterator.emptyIterator();
		}

		@Override
		boolean hasValues() {
			return false;
		}

		@Override
		int valueArity() {
			return 0;
		}

		@Override
		K getKey(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		public CompactNode<K> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + npos1;
			result = prime * result + node1.hashCode();
			result = prime * result + npos2;
			result = prime * result + node2.hashCode();
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
			Value0Index2Node<?> that = (Value0Index2Node<?>) other;

			if (npos1 != that.npos1) {
				return false;
			}
			if (npos2 != that.npos2) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			if (!node2.equals(that.node2)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s, %s]", node1, node2);
		}

		@Override
		K headKey() {
			throw new UnsupportedOperationException("No key in this kind of node.");
		}

	}

	private static final class Value0Index3Node<K> extends CompactNode<K> {
		private final byte npos1;
		private final CompactNode<K> node1;
		private final byte npos2;
		private final CompactNode<K> node2;
		private final byte npos3;
		private final CompactNode<K> node3;

		Value0Index3Node(AtomicReference<Thread> mutator, byte npos1, CompactNode<K> node1,
						byte npos2, CompactNode<K> node2, byte npos3, CompactNode<K> node3) {
			this.npos1 = npos1;
			this.node1 = node1;
			this.npos2 = npos2;
			this.node2 = node2;
			this.npos3 = npos3;
			this.node3 = node3;
			assert nodeInvariant();
		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node1.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew = valNodeOf(mutator, mask, subNodeResult.getNode(),
								npos2, node2, npos3, node3);

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node2.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew = valNodeOf(mutator, npos1, node1, mask,
								subNodeResult.getNode(), npos3, node3);

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else if (mask == npos3) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node3.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2, mask,
								subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			}

			// no value

			final CompactNode<K> thisNew = valNodeOf(mutator, mask, key, npos1, node1, npos2,
							node2, npos3, node3);
			return Result.modified(thisNew);

		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE: {
					// inline value

					final CompactNode<K> thisNew = valNodeOf(mutator, mask, subNodeNew.headKey(),
									npos2, node2, npos3, node3);
					return Result.modified(thisNew);

				}
				case SIZE_MORE_THAN_ONE: {
					// modify current node (set replacement node)

					final CompactNode<K> thisNew = valNodeOf(mutator, mask, subNodeNew, npos2,
									node2, npos3, node3);

					return Result.modified(thisNew);
				}
				default: {
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE: {
					// inline value

					final CompactNode<K> thisNew = valNodeOf(mutator, mask, subNodeNew.headKey(),
									npos1, node1, npos3, node3);
					return Result.modified(thisNew);

				}
				case SIZE_MORE_THAN_ONE: {
					// modify current node (set replacement node)

					final CompactNode<K> thisNew = valNodeOf(mutator, npos1, node1, mask,
									subNodeNew, npos3, node3);

					return Result.modified(thisNew);
				}
				default: {
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
				}
			} else if (mask == npos3) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node3.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE: {
					// inline value

					final CompactNode<K> thisNew = valNodeOf(mutator, mask, subNodeNew.headKey(),
									npos1, node1, npos2, node2);
					return Result.modified(thisNew);

				}
				case SIZE_MORE_THAN_ONE: {
					// modify current node (set replacement node)

					final CompactNode<K> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
									mask, subNodeNew);

					return Result.modified(thisNew);
				}
				default: {
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean contains(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.contains(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.contains(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos3) {
				return node3.contains(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos3) {
				return node3.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactNode<K>> nodeIterator() {
			return ArrayIterator.<CompactNode<K>> of(new CompactNode[] { node1, node2, node3 });
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 3;
		}

		@Override
		SupplierIterator<K, K> valueIterator() {
			return EmptySupplierIterator.emptyIterator();
		}

		@Override
		boolean hasValues() {
			return false;
		}

		@Override
		int valueArity() {
			return 0;
		}

		@Override
		K getKey(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		public CompactNode<K> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;
			case 2:
				return node3;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + npos1;
			result = prime * result + node1.hashCode();
			result = prime * result + npos2;
			result = prime * result + node2.hashCode();
			result = prime * result + npos3;
			result = prime * result + node3.hashCode();
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
			Value0Index3Node<?> that = (Value0Index3Node<?>) other;

			if (npos1 != that.npos1) {
				return false;
			}
			if (npos2 != that.npos2) {
				return false;
			}
			if (npos3 != that.npos3) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			if (!node2.equals(that.node2)) {
				return false;
			}
			if (!node3.equals(that.node3)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s, %s, %s]", node1, node2, node3);
		}

		@Override
		K headKey() {
			throw new UnsupportedOperationException("No key in this kind of node.");
		}

	}

	private static final class Value0Index4Node<K> extends CompactNode<K> {
		private final byte npos1;
		private final CompactNode<K> node1;
		private final byte npos2;
		private final CompactNode<K> node2;
		private final byte npos3;
		private final CompactNode<K> node3;
		private final byte npos4;
		private final CompactNode<K> node4;

		Value0Index4Node(AtomicReference<Thread> mutator, byte npos1, CompactNode<K> node1,
						byte npos2, CompactNode<K> node2, byte npos3, CompactNode<K> node3,
						byte npos4, CompactNode<K> node4) {
			this.npos1 = npos1;
			this.node1 = node1;
			this.npos2 = npos2;
			this.node2 = node2;
			this.npos3 = npos3;
			this.node3 = node3;
			this.npos4 = npos4;
			this.node4 = node4;
			assert nodeInvariant();
		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node1.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew = valNodeOf(mutator, mask, subNodeResult.getNode(),
								npos2, node2, npos3, node3, npos4, node4);

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node2.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew = valNodeOf(mutator, npos1, node1, mask,
								subNodeResult.getNode(), npos3, node3, npos4, node4);

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else if (mask == npos3) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node3.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2, mask,
								subNodeResult.getNode(), npos4, node4);

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else if (mask == npos4) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node4.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
								npos3, node3, mask, subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			}

			// no value

			final CompactNode<K> thisNew = valNodeOf(mutator, mask, key, npos1, node1, npos2,
							node2, npos3, node3, npos4, node4);
			return Result.modified(thisNew);

		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE: {
					// inline value

					final CompactNode<K> thisNew = valNodeOf(mutator, mask, subNodeNew.headKey(),
									npos2, node2, npos3, node3, npos4, node4);
					return Result.modified(thisNew);

				}
				case SIZE_MORE_THAN_ONE: {
					// modify current node (set replacement node)

					final CompactNode<K> thisNew = valNodeOf(mutator, mask, subNodeNew, npos2,
									node2, npos3, node3, npos4, node4);

					return Result.modified(thisNew);
				}
				default: {
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE: {
					// inline value

					final CompactNode<K> thisNew = valNodeOf(mutator, mask, subNodeNew.headKey(),
									npos1, node1, npos3, node3, npos4, node4);
					return Result.modified(thisNew);

				}
				case SIZE_MORE_THAN_ONE: {
					// modify current node (set replacement node)

					final CompactNode<K> thisNew = valNodeOf(mutator, npos1, node1, mask,
									subNodeNew, npos3, node3, npos4, node4);

					return Result.modified(thisNew);
				}
				default: {
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
				}
			} else if (mask == npos3) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node3.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE: {
					// inline value

					final CompactNode<K> thisNew = valNodeOf(mutator, mask, subNodeNew.headKey(),
									npos1, node1, npos2, node2, npos4, node4);
					return Result.modified(thisNew);

				}
				case SIZE_MORE_THAN_ONE: {
					// modify current node (set replacement node)

					final CompactNode<K> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
									mask, subNodeNew, npos4, node4);

					return Result.modified(thisNew);
				}
				default: {
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
				}
			} else if (mask == npos4) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node4.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE: {
					// inline value

					final CompactNode<K> thisNew = valNodeOf(mutator, mask, subNodeNew.headKey(),
									npos1, node1, npos2, node2, npos3, node3);
					return Result.modified(thisNew);

				}
				case SIZE_MORE_THAN_ONE: {
					// modify current node (set replacement node)

					final CompactNode<K> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
									npos3, node3, mask, subNodeNew);

					return Result.modified(thisNew);
				}
				default: {
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean contains(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.contains(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.contains(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos3) {
				return node3.contains(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos4) {
				return node4.contains(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos3) {
				return node3.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos4) {
				return node4.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactNode<K>> nodeIterator() {
			return ArrayIterator
							.<CompactNode<K>> of(new CompactNode[] { node1, node2, node3, node4 });
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 4;
		}

		@Override
		SupplierIterator<K, K> valueIterator() {
			return EmptySupplierIterator.emptyIterator();
		}

		@Override
		boolean hasValues() {
			return false;
		}

		@Override
		int valueArity() {
			return 0;
		}

		@Override
		K getKey(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		public CompactNode<K> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;
			case 2:
				return node3;
			case 3:
				return node4;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + npos1;
			result = prime * result + node1.hashCode();
			result = prime * result + npos2;
			result = prime * result + node2.hashCode();
			result = prime * result + npos3;
			result = prime * result + node3.hashCode();
			result = prime * result + npos4;
			result = prime * result + node4.hashCode();
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
			Value0Index4Node<?> that = (Value0Index4Node<?>) other;

			if (npos1 != that.npos1) {
				return false;
			}
			if (npos2 != that.npos2) {
				return false;
			}
			if (npos3 != that.npos3) {
				return false;
			}
			if (npos4 != that.npos4) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			if (!node2.equals(that.node2)) {
				return false;
			}
			if (!node3.equals(that.node3)) {
				return false;
			}
			if (!node4.equals(that.node4)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s, %s, %s, %s]", node1, node2, node3, node4);
		}

		@Override
		K headKey() {
			throw new UnsupportedOperationException("No key in this kind of node.");
		}

	}

	private static final class Value1Index0Node<K> extends CompactNode<K> {
		private final byte pos1;
		private final K key1;

		Value1Index0Node(AtomicReference<Thread> mutator, byte pos1, K key1) {
			this.pos1 = pos1;
			this.key1 = key1;
			assert nodeInvariant();
		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					return Result.unchanged(this);
				}

				// merge into node
				final CompactNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash, shift
								+ BIT_PARTITION_SIZE);

				final CompactNode<K> thisNew = valNodeOf(mutator, mask, node);
				return Result.modified(thisNew);

			}

			// no value

			if (mask < pos1) {
				final CompactNode<K> thisNew = valNodeOf(mutator, mask, key, pos1, key1);
				return Result.modified(thisNew);
			}

			else {
				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask, key);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(CompactNode.<K> valNodeOf(mutator));
			}

			return Result.unchanged(this);
		}

		@Override
		boolean contains(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			}

			return false;
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(key1);
			}

			return Optional.empty();
		}

		@Override
		Iterator<CompactNode<K>> nodeIterator() {
			return Collections.emptyIterator();
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
		SupplierIterator<K, K> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, key1 });
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return 1;
		}

		@Override
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public CompactNode<K> getNode(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		byte sizePredicate() {
			return SIZE_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
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
			Value1Index0Node<?> that = (Value1Index0Node<?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s]", key1);
		}

		@Override
		K headKey() {
			return key1;
		}

	}

	private static final class Value1Index1Node<K> extends CompactNode<K> {
		private final byte pos1;
		private final K key1;
		private final byte npos1;
		private final CompactNode<K> node1;

		Value1Index1Node(AtomicReference<Thread> mutator, byte pos1, K key1, byte npos1,
						CompactNode<K> node1) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.npos1 = npos1;
			this.node1 = node1;
			assert nodeInvariant();
		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					return Result.unchanged(this);
				}

				// merge into node
				final CompactNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash, shift
								+ BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final CompactNode<K> thisNew = valNodeOf(mutator, mask, node, npos1, node1);
					return Result.modified(thisNew);
				}

				else {
					final CompactNode<K> thisNew = valNodeOf(mutator, npos1, node1, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node1.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask,
								subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			}

			// no value

			if (mask < pos1) {
				final CompactNode<K> thisNew = valNodeOf(mutator, mask, key, pos1, key1, npos1,
								node1);
				return Result.modified(thisNew);
			}

			else {
				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask, key, npos1,
								node1);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, npos1, node1));
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE: {
					// inline value

					if (mask < pos1) {
						final CompactNode<K> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), pos1, key1);
						return Result.modified(thisNew);
					}

					else {
						final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask,
										subNodeNew.headKey());
						return Result.modified(thisNew);
					}

				}
				case SIZE_MORE_THAN_ONE: {
					// modify current node (set replacement node)

					final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask, subNodeNew);

					return Result.modified(thisNew);
				}
				default: {
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean contains(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == npos1) {
				return node1.contains(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(key1);
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactNode<K>> nodeIterator() {
			return ArrayIterator.<CompactNode<K>> of(new CompactNode[] { node1 });
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 1;
		}

		@Override
		SupplierIterator<K, K> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, key1 });
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return 1;
		}

		@Override
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public CompactNode<K> getNode(int index) {
			switch (index) {
			case 0:
				return node1;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + npos1;
			result = prime * result + node1.hashCode();
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
			Value1Index1Node<?> that = (Value1Index1Node<?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (npos1 != that.npos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s, %s]", key1, node1);
		}

		@Override
		K headKey() {
			return key1;
		}

	}

	private static final class Value1Index2Node<K> extends CompactNode<K> {
		private final byte pos1;
		private final K key1;
		private final byte npos1;
		private final CompactNode<K> node1;
		private final byte npos2;
		private final CompactNode<K> node2;

		Value1Index2Node(AtomicReference<Thread> mutator, byte pos1, K key1, byte npos1,
						CompactNode<K> node1, byte npos2, CompactNode<K> node2) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.npos1 = npos1;
			this.node1 = node1;
			this.npos2 = npos2;
			this.node2 = node2;
			assert nodeInvariant();
		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					return Result.unchanged(this);
				}

				// merge into node
				final CompactNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash, shift
								+ BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final CompactNode<K> thisNew = valNodeOf(mutator, mask, node, npos1, node1,
									npos2, node2);
					return Result.modified(thisNew);
				}

				else if (mask < npos2) {
					final CompactNode<K> thisNew = valNodeOf(mutator, npos1, node1, mask, node,
									npos2, node2);
					return Result.modified(thisNew);
				}

				else {
					final CompactNode<K> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
									mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node1.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask,
								subNodeResult.getNode(), npos2, node2);

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node2.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, npos1, node1, mask,
								subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			}

			// no value

			if (mask < pos1) {
				final CompactNode<K> thisNew = valNodeOf(mutator, mask, key, pos1, key1, npos1,
								node1, npos2, node2);
				return Result.modified(thisNew);
			}

			else {
				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask, key, npos1,
								node1, npos2, node2);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2));
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE: {
					// inline value

					if (mask < pos1) {
						final CompactNode<K> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), pos1, key1, npos2, node2);
						return Result.modified(thisNew);
					}

					else {
						final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask,
										subNodeNew.headKey(), npos2, node2);
						return Result.modified(thisNew);
					}

				}
				case SIZE_MORE_THAN_ONE: {
					// modify current node (set replacement node)

					final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask, subNodeNew,
									npos2, node2);

					return Result.modified(thisNew);
				}
				default: {
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE: {
					// inline value

					if (mask < pos1) {
						final CompactNode<K> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), pos1, key1, npos1, node1);
						return Result.modified(thisNew);
					}

					else {
						final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask,
										subNodeNew.headKey(), npos1, node1);
						return Result.modified(thisNew);
					}

				}
				case SIZE_MORE_THAN_ONE: {
					// modify current node (set replacement node)

					final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, npos1, node1,
									mask, subNodeNew);

					return Result.modified(thisNew);
				}
				default: {
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean contains(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == npos1) {
				return node1.contains(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.contains(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(key1);
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactNode<K>> nodeIterator() {
			return ArrayIterator.<CompactNode<K>> of(new CompactNode[] { node1, node2 });
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 2;
		}

		@Override
		SupplierIterator<K, K> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, key1 });
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return 1;
		}

		@Override
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public CompactNode<K> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + npos1;
			result = prime * result + node1.hashCode();
			result = prime * result + npos2;
			result = prime * result + node2.hashCode();
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
			Value1Index2Node<?> that = (Value1Index2Node<?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (npos1 != that.npos1) {
				return false;
			}
			if (npos2 != that.npos2) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			if (!node2.equals(that.node2)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s, %s, %s]", key1, node1, node2);
		}

		@Override
		K headKey() {
			return key1;
		}

	}

	private static final class Value1Index3Node<K> extends CompactNode<K> {
		private final byte pos1;
		private final K key1;
		private final byte npos1;
		private final CompactNode<K> node1;
		private final byte npos2;
		private final CompactNode<K> node2;
		private final byte npos3;
		private final CompactNode<K> node3;

		Value1Index3Node(AtomicReference<Thread> mutator, byte pos1, K key1, byte npos1,
						CompactNode<K> node1, byte npos2, CompactNode<K> node2, byte npos3,
						CompactNode<K> node3) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.npos1 = npos1;
			this.node1 = node1;
			this.npos2 = npos2;
			this.node2 = node2;
			this.npos3 = npos3;
			this.node3 = node3;
			assert nodeInvariant();
		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					return Result.unchanged(this);
				}

				// merge into node
				final CompactNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash, shift
								+ BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final CompactNode<K> thisNew = valNodeOf(mutator, mask, node, npos1, node1,
									npos2, node2, npos3, node3);
					return Result.modified(thisNew);
				}

				else if (mask < npos2) {
					final CompactNode<K> thisNew = valNodeOf(mutator, npos1, node1, mask, node,
									npos2, node2, npos3, node3);
					return Result.modified(thisNew);
				}

				else if (mask < npos3) {
					final CompactNode<K> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
									mask, node, npos3, node3);
					return Result.modified(thisNew);
				}

				else {
					final CompactNode<K> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
									npos3, node3, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node1.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask,
								subNodeResult.getNode(), npos2, node2, npos3, node3);

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node2.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, npos1, node1, mask,
								subNodeResult.getNode(), npos3, node3);

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else if (mask == npos3) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node3.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, npos1, node1, npos2,
								node2, mask, subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			}

			// no value

			if (mask < pos1) {
				final CompactNode<K> thisNew = valNodeOf(mutator, mask, key, pos1, key1, npos1,
								node1, npos2, node2, npos3, node3);
				return Result.modified(thisNew);
			}

			else {
				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask, key, npos1,
								node1, npos2, node2, npos3, node3);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2, npos3, node3));
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE: {
					// inline value

					if (mask < pos1) {
						final CompactNode<K> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), pos1, key1, npos2, node2, npos3,
										node3);
						return Result.modified(thisNew);
					}

					else {
						final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask,
										subNodeNew.headKey(), npos2, node2, npos3, node3);
						return Result.modified(thisNew);
					}

				}
				case SIZE_MORE_THAN_ONE: {
					// modify current node (set replacement node)

					final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask, subNodeNew,
									npos2, node2, npos3, node3);

					return Result.modified(thisNew);
				}
				default: {
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE: {
					// inline value

					if (mask < pos1) {
						final CompactNode<K> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), pos1, key1, npos1, node1, npos3,
										node3);
						return Result.modified(thisNew);
					}

					else {
						final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask,
										subNodeNew.headKey(), npos1, node1, npos3, node3);
						return Result.modified(thisNew);
					}

				}
				case SIZE_MORE_THAN_ONE: {
					// modify current node (set replacement node)

					final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, npos1, node1,
									mask, subNodeNew, npos3, node3);

					return Result.modified(thisNew);
				}
				default: {
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
				}
			} else if (mask == npos3) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node3.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE: {
					// inline value

					if (mask < pos1) {
						final CompactNode<K> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), pos1, key1, npos1, node1, npos2,
										node2);
						return Result.modified(thisNew);
					}

					else {
						final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask,
										subNodeNew.headKey(), npos1, node1, npos2, node2);
						return Result.modified(thisNew);
					}

				}
				case SIZE_MORE_THAN_ONE: {
					// modify current node (set replacement node)

					final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, npos1, node1,
									npos2, node2, mask, subNodeNew);

					return Result.modified(thisNew);
				}
				default: {
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean contains(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == npos1) {
				return node1.contains(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.contains(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos3) {
				return node3.contains(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(key1);
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos3) {
				return node3.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactNode<K>> nodeIterator() {
			return ArrayIterator.<CompactNode<K>> of(new CompactNode[] { node1, node2, node3 });
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 3;
		}

		@Override
		SupplierIterator<K, K> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, key1 });
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return 1;
		}

		@Override
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public CompactNode<K> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;
			case 2:
				return node3;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + npos1;
			result = prime * result + node1.hashCode();
			result = prime * result + npos2;
			result = prime * result + node2.hashCode();
			result = prime * result + npos3;
			result = prime * result + node3.hashCode();
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
			Value1Index3Node<?> that = (Value1Index3Node<?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (npos1 != that.npos1) {
				return false;
			}
			if (npos2 != that.npos2) {
				return false;
			}
			if (npos3 != that.npos3) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			if (!node2.equals(that.node2)) {
				return false;
			}
			if (!node3.equals(that.node3)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s, %s, %s, %s]", key1, node1, node2, node3);
		}

		@Override
		K headKey() {
			return key1;
		}

	}

	private static final class Value2Index0Node<K> extends CompactNode<K> {
		private final byte pos1;
		private final K key1;
		private final byte pos2;
		private final K key2;

		Value2Index0Node(AtomicReference<Thread> mutator, byte pos1, K key1, byte pos2, K key2) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.pos2 = pos2;
			this.key2 = key2;
			assert nodeInvariant();
		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					return Result.unchanged(this);
				}

				// merge into node
				final CompactNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash, shift
								+ BIT_PARTITION_SIZE);

				final CompactNode<K> thisNew = valNodeOf(mutator, pos2, key2, mask, node);
				return Result.modified(thisNew);

			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					return Result.unchanged(this);
				}

				// merge into node
				final CompactNode<K> node = mergeNodes(key2, key2.hashCode(), key, keyHash, shift
								+ BIT_PARTITION_SIZE);

				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask, node);
				return Result.modified(thisNew);

			}

			// no value

			if (mask < pos1) {
				final CompactNode<K> thisNew = valNodeOf(mutator, mask, key, pos1, key1, pos2, key2);
				return Result.modified(thisNew);
			}

			else if (mask < pos2) {
				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask, key, pos2, key2);
				return Result.modified(thisNew);
			}

			else {
				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, mask, key);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				/*
				 * Create root node with singleton element. This node will be a)
				 * either be the new root returned, or b) unwrapped and inlined.
				 */
				return Result.modified(valNodeOf(mutator,
								(byte) (key2.hashCode() & BIT_PARTITION_MASK), key2));
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				/*
				 * Create root node with singleton element. This node will be a)
				 * either be the new root returned, or b) unwrapped and inlined.
				 */
				return Result.modified(valNodeOf(mutator,
								(byte) (key1.hashCode() & BIT_PARTITION_MASK), key1));
			}

			return Result.unchanged(this);
		}

		@Override
		boolean contains(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return true;
			}

			return false;
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(key1);
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(key2);
			}

			return Optional.empty();
		}

		@Override
		Iterator<CompactNode<K>> nodeIterator() {
			return Collections.emptyIterator();
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
		SupplierIterator<K, K> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, key1, key2, key2 });
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return 2;
		}

		@Override
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;
			case 1:
				return key2;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public CompactNode<K> getNode(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + pos2;
			result = prime * result + key2.hashCode();
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
			Value2Index0Node<?> that = (Value2Index0Node<?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (pos2 != that.pos2) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s, %s]", key1, key2);
		}

		@Override
		K headKey() {
			return key1;
		}

	}

	private static final class Value2Index1Node<K> extends CompactNode<K> {
		private final byte pos1;
		private final K key1;
		private final byte pos2;
		private final K key2;
		private final byte npos1;
		private final CompactNode<K> node1;

		Value2Index1Node(AtomicReference<Thread> mutator, byte pos1, K key1, byte pos2, K key2,
						byte npos1, CompactNode<K> node1) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.pos2 = pos2;
			this.key2 = key2;
			this.npos1 = npos1;
			this.node1 = node1;
			assert nodeInvariant();
		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					return Result.unchanged(this);
				}

				// merge into node
				final CompactNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash, shift
								+ BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final CompactNode<K> thisNew = valNodeOf(mutator, pos2, key2, mask, node,
									npos1, node1);
					return Result.modified(thisNew);
				}

				else {
					final CompactNode<K> thisNew = valNodeOf(mutator, pos2, key2, npos1, node1,
									mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					return Result.unchanged(this);
				}

				// merge into node
				final CompactNode<K> node = mergeNodes(key2, key2.hashCode(), key, keyHash, shift
								+ BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask, node,
									npos1, node1);
					return Result.modified(thisNew);
				}

				else {
					final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, npos1, node1,
									mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node1.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, mask,
								subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			}

			// no value

			if (mask < pos1) {
				final CompactNode<K> thisNew = valNodeOf(mutator, mask, key, pos1, key1, pos2,
								key2, npos1, node1);
				return Result.modified(thisNew);
			}

			else if (mask < pos2) {
				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask, key, pos2,
								key2, npos1, node1);
				return Result.modified(thisNew);
			}

			else {
				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, mask,
								key, npos1, node1);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos2, key2, npos1, node1));
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, npos1, node1));
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE: {
					// inline value

					if (mask < pos1) {
						final CompactNode<K> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), pos1, key1, pos2, key2);
						return Result.modified(thisNew);
					}

					else if (mask < pos2) {
						final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask,
										subNodeNew.headKey(), pos2, key2);
						return Result.modified(thisNew);
					}

					else {
						final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2,
										mask, subNodeNew.headKey());
						return Result.modified(thisNew);
					}

				}
				case SIZE_MORE_THAN_ONE: {
					// modify current node (set replacement node)

					final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, mask,
									subNodeNew);

					return Result.modified(thisNew);
				}
				default: {
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean contains(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return true;
			} else if (mask == npos1) {
				return node1.contains(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(key1);
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(key2);
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactNode<K>> nodeIterator() {
			return ArrayIterator.<CompactNode<K>> of(new CompactNode[] { node1 });
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 1;
		}

		@Override
		SupplierIterator<K, K> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, key1, key2, key2 });
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return 2;
		}

		@Override
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;
			case 1:
				return key2;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public CompactNode<K> getNode(int index) {
			switch (index) {
			case 0:
				return node1;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + pos2;
			result = prime * result + key2.hashCode();
			result = prime * result + npos1;
			result = prime * result + node1.hashCode();
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
			Value2Index1Node<?> that = (Value2Index1Node<?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (pos2 != that.pos2) {
				return false;
			}
			if (npos1 != that.npos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s, %s, %s]", key1, key2, node1);
		}

		@Override
		K headKey() {
			return key1;
		}

	}

	private static final class Value2Index2Node<K> extends CompactNode<K> {
		private final byte pos1;
		private final K key1;
		private final byte pos2;
		private final K key2;
		private final byte npos1;
		private final CompactNode<K> node1;
		private final byte npos2;
		private final CompactNode<K> node2;

		Value2Index2Node(AtomicReference<Thread> mutator, byte pos1, K key1, byte pos2, K key2,
						byte npos1, CompactNode<K> node1, byte npos2, CompactNode<K> node2) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.pos2 = pos2;
			this.key2 = key2;
			this.npos1 = npos1;
			this.node1 = node1;
			this.npos2 = npos2;
			this.node2 = node2;
			assert nodeInvariant();
		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					return Result.unchanged(this);
				}

				// merge into node
				final CompactNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash, shift
								+ BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final CompactNode<K> thisNew = valNodeOf(mutator, pos2, key2, mask, node,
									npos1, node1, npos2, node2);
					return Result.modified(thisNew);
				}

				else if (mask < npos2) {
					final CompactNode<K> thisNew = valNodeOf(mutator, pos2, key2, npos1, node1,
									mask, node, npos2, node2);
					return Result.modified(thisNew);
				}

				else {
					final CompactNode<K> thisNew = valNodeOf(mutator, pos2, key2, npos1, node1,
									npos2, node2, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					return Result.unchanged(this);
				}

				// merge into node
				final CompactNode<K> node = mergeNodes(key2, key2.hashCode(), key, keyHash, shift
								+ BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask, node,
									npos1, node1, npos2, node2);
					return Result.modified(thisNew);
				}

				else if (mask < npos2) {
					final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, npos1, node1,
									mask, node, npos2, node2);
					return Result.modified(thisNew);
				}

				else {
					final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, npos1, node1,
									npos2, node2, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node1.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, mask,
								subNodeResult.getNode(), npos2, node2);

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node2.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, npos1,
								node1, mask, subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			}

			// no value

			if (mask < pos1) {
				final CompactNode<K> thisNew = valNodeOf(mutator, mask, key, pos1, key1, pos2,
								key2, npos1, node1, npos2, node2);
				return Result.modified(thisNew);
			}

			else if (mask < pos2) {
				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask, key, pos2,
								key2, npos1, node1, npos2, node2);
				return Result.modified(thisNew);
			}

			else {
				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, mask,
								key, npos1, node1, npos2, node2);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos2, key2, npos1, node1, npos2, node2));
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, npos1, node1, npos2, node2));
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE: {
					// inline value

					if (mask < pos1) {
						final CompactNode<K> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), pos1, key1, pos2, key2, npos2, node2);
						return Result.modified(thisNew);
					}

					else if (mask < pos2) {
						final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask,
										subNodeNew.headKey(), pos2, key2, npos2, node2);
						return Result.modified(thisNew);
					}

					else {
						final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2,
										mask, subNodeNew.headKey(), npos2, node2);
						return Result.modified(thisNew);
					}

				}
				case SIZE_MORE_THAN_ONE: {
					// modify current node (set replacement node)

					final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, mask,
									subNodeNew, npos2, node2);

					return Result.modified(thisNew);
				}
				default: {
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE: {
					// inline value

					if (mask < pos1) {
						final CompactNode<K> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), pos1, key1, pos2, key2, npos1, node1);
						return Result.modified(thisNew);
					}

					else if (mask < pos2) {
						final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask,
										subNodeNew.headKey(), pos2, key2, npos1, node1);
						return Result.modified(thisNew);
					}

					else {
						final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2,
										mask, subNodeNew.headKey(), npos1, node1);
						return Result.modified(thisNew);
					}

				}
				case SIZE_MORE_THAN_ONE: {
					// modify current node (set replacement node)

					final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2,
									npos1, node1, mask, subNodeNew);

					return Result.modified(thisNew);
				}
				default: {
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean contains(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return true;
			} else if (mask == npos1) {
				return node1.contains(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.contains(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(key1);
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(key2);
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactNode<K>> nodeIterator() {
			return ArrayIterator.<CompactNode<K>> of(new CompactNode[] { node1, node2 });
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 2;
		}

		@Override
		SupplierIterator<K, K> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, key1, key2, key2 });
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return 2;
		}

		@Override
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;
			case 1:
				return key2;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public CompactNode<K> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + pos2;
			result = prime * result + key2.hashCode();
			result = prime * result + npos1;
			result = prime * result + node1.hashCode();
			result = prime * result + npos2;
			result = prime * result + node2.hashCode();
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
			Value2Index2Node<?> that = (Value2Index2Node<?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (pos2 != that.pos2) {
				return false;
			}
			if (npos1 != that.npos1) {
				return false;
			}
			if (npos2 != that.npos2) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			if (!node2.equals(that.node2)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s, %s, %s, %s]", key1, key2, node1, node2);
		}

		@Override
		K headKey() {
			return key1;
		}

	}

	private static final class Value3Index0Node<K> extends CompactNode<K> {
		private final byte pos1;
		private final K key1;
		private final byte pos2;
		private final K key2;
		private final byte pos3;
		private final K key3;

		Value3Index0Node(AtomicReference<Thread> mutator, byte pos1, K key1, byte pos2, K key2,
						byte pos3, K key3) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.pos2 = pos2;
			this.key2 = key2;
			this.pos3 = pos3;
			this.key3 = key3;
			assert nodeInvariant();
		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					return Result.unchanged(this);
				}

				// merge into node
				final CompactNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash, shift
								+ BIT_PARTITION_SIZE);

				final CompactNode<K> thisNew = valNodeOf(mutator, pos2, key2, pos3, key3, mask,
								node);
				return Result.modified(thisNew);

			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					return Result.unchanged(this);
				}

				// merge into node
				final CompactNode<K> node = mergeNodes(key2, key2.hashCode(), key, keyHash, shift
								+ BIT_PARTITION_SIZE);

				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos3, key3, mask,
								node);
				return Result.modified(thisNew);

			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					return Result.unchanged(this);
				}

				// merge into node
				final CompactNode<K> node = mergeNodes(key3, key3.hashCode(), key, keyHash, shift
								+ BIT_PARTITION_SIZE);

				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, mask,
								node);
				return Result.modified(thisNew);

			}

			// no value

			if (mask < pos1) {
				final CompactNode<K> thisNew = valNodeOf(mutator, mask, key, pos1, key1, pos2,
								key2, pos3, key3);
				return Result.modified(thisNew);
			}

			else if (mask < pos2) {
				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask, key, pos2,
								key2, pos3, key3);
				return Result.modified(thisNew);
			}

			else if (mask < pos3) {
				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, mask,
								key, pos3, key3);
				return Result.modified(thisNew);
			}

			else {
				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, pos3,
								key3, mask, key);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos2, key2, pos3, key3));
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, pos3, key3));
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2));
			}

			return Result.unchanged(this);
		}

		@Override
		boolean contains(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return true;
			} else if (mask == pos3 && cmp.compare(key, key3) == 0) {
				return true;
			}

			return false;
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(key1);
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(key2);
			} else if (mask == pos3 && cmp.compare(key, key3) == 0) {
				return Optional.of(key3);
			}

			return Optional.empty();
		}

		@Override
		Iterator<CompactNode<K>> nodeIterator() {
			return Collections.emptyIterator();
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
		SupplierIterator<K, K> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, key1, key2, key2, key3, key3 });
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return 3;
		}

		@Override
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;
			case 1:
				return key2;
			case 2:
				return key3;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public CompactNode<K> getNode(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + pos2;
			result = prime * result + key2.hashCode();
			result = prime * result + pos3;
			result = prime * result + key3.hashCode();
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
			Value3Index0Node<?> that = (Value3Index0Node<?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (pos2 != that.pos2) {
				return false;
			}
			if (pos3 != that.pos3) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s, %s, %s]", key1, key2, key3);
		}

		@Override
		K headKey() {
			return key1;
		}

	}

	private static final class Value3Index1Node<K> extends CompactNode<K> {
		private final byte pos1;
		private final K key1;
		private final byte pos2;
		private final K key2;
		private final byte pos3;
		private final K key3;
		private final byte npos1;
		private final CompactNode<K> node1;

		Value3Index1Node(AtomicReference<Thread> mutator, byte pos1, K key1, byte pos2, K key2,
						byte pos3, K key3, byte npos1, CompactNode<K> node1) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.pos2 = pos2;
			this.key2 = key2;
			this.pos3 = pos3;
			this.key3 = key3;
			this.npos1 = npos1;
			this.node1 = node1;
			assert nodeInvariant();
		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					return Result.unchanged(this);
				}

				// merge into node
				final CompactNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash, shift
								+ BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final CompactNode<K> thisNew = valNodeOf(mutator, pos2, key2, pos3, key3, mask,
									node, npos1, node1);
					return Result.modified(thisNew);
				}

				else {
					final CompactNode<K> thisNew = valNodeOf(mutator, pos2, key2, pos3, key3,
									npos1, node1, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					return Result.unchanged(this);
				}

				// merge into node
				final CompactNode<K> node = mergeNodes(key2, key2.hashCode(), key, keyHash, shift
								+ BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos3, key3, mask,
									node, npos1, node1);
					return Result.modified(thisNew);
				}

				else {
					final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos3, key3,
									npos1, node1, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					return Result.unchanged(this);
				}

				// merge into node
				final CompactNode<K> node = mergeNodes(key3, key3.hashCode(), key, keyHash, shift
								+ BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, mask,
									node, npos1, node1);
					return Result.modified(thisNew);
				}

				else {
					final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2,
									npos1, node1, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node1.updated(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, pos3,
								key3, mask, subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			}

			// no value

			if (mask < pos1) {
				final CompactNode<K> thisNew = valNodeOf(mutator, mask, key, pos1, key1, pos2,
								key2, pos3, key3, npos1, node1);
				return Result.modified(thisNew);
			}

			else if (mask < pos2) {
				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask, key, pos2,
								key2, pos3, key3, npos1, node1);
				return Result.modified(thisNew);
			}

			else if (mask < pos3) {
				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, mask,
								key, pos3, key3, npos1, node1);
				return Result.modified(thisNew);
			}

			else {
				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, pos3,
								key3, mask, key, npos1, node1);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos2, key2, pos3, key3, npos1, node1));
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, pos3, key3, npos1, node1));
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, npos1, node1));
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactNode<K>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_ONE: {
					// inline value

					if (mask < pos1) {
						final CompactNode<K> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), pos1, key1, pos2, key2, pos3, key3);
						return Result.modified(thisNew);
					}

					else if (mask < pos2) {
						final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask,
										subNodeNew.headKey(), pos2, key2, pos3, key3);
						return Result.modified(thisNew);
					}

					else if (mask < pos3) {
						final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2,
										mask, subNodeNew.headKey(), pos3, key3);
						return Result.modified(thisNew);
					}

					else {
						final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2,
										pos3, key3, mask, subNodeNew.headKey());
						return Result.modified(thisNew);
					}

				}
				case SIZE_MORE_THAN_ONE: {
					// modify current node (set replacement node)

					final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, pos3,
									key3, mask, subNodeNew);

					return Result.modified(thisNew);
				}
				default: {
					throw new IllegalStateException("Size predicate violates node invariant.");
				}
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean contains(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return true;
			} else if (mask == pos3 && cmp.compare(key, key3) == 0) {
				return true;
			} else if (mask == npos1) {
				return node1.contains(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(key1);
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(key2);
			} else if (mask == pos3 && cmp.compare(key, key3) == 0) {
				return Optional.of(key3);
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactNode<K>> nodeIterator() {
			return ArrayIterator.<CompactNode<K>> of(new CompactNode[] { node1 });
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 1;
		}

		@Override
		SupplierIterator<K, K> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, key1, key2, key2, key3, key3 });
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return 3;
		}

		@Override
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;
			case 1:
				return key2;
			case 2:
				return key3;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public CompactNode<K> getNode(int index) {
			switch (index) {
			case 0:
				return node1;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + pos2;
			result = prime * result + key2.hashCode();
			result = prime * result + pos3;
			result = prime * result + key3.hashCode();
			result = prime * result + npos1;
			result = prime * result + node1.hashCode();
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
			Value3Index1Node<?> that = (Value3Index1Node<?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (pos2 != that.pos2) {
				return false;
			}
			if (pos3 != that.pos3) {
				return false;
			}
			if (npos1 != that.npos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s, %s, %s, %s]", key1, key2, key3, node1);
		}

		@Override
		K headKey() {
			return key1;
		}

	}

	private static final class Value4Index0Node<K> extends CompactNode<K> {
		private final byte pos1;
		private final K key1;
		private final byte pos2;
		private final K key2;
		private final byte pos3;
		private final K key3;
		private final byte pos4;
		private final K key4;

		Value4Index0Node(AtomicReference<Thread> mutator, byte pos1, K key1, byte pos2, K key2,
						byte pos3, K key3, byte pos4, K key4) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.pos2 = pos2;
			this.key2 = key2;
			this.pos3 = pos3;
			this.key3 = key3;
			this.pos4 = pos4;
			this.key4 = key4;
			assert nodeInvariant();
		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					return Result.unchanged(this);
				}

				// merge into node
				final CompactNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash, shift
								+ BIT_PARTITION_SIZE);

				final CompactNode<K> thisNew = valNodeOf(mutator, pos2, key2, pos3, key3, pos4,
								key4, mask, node);
				return Result.modified(thisNew);

			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					return Result.unchanged(this);
				}

				// merge into node
				final CompactNode<K> node = mergeNodes(key2, key2.hashCode(), key, keyHash, shift
								+ BIT_PARTITION_SIZE);

				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos3, key3, pos4,
								key4, mask, node);
				return Result.modified(thisNew);

			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					return Result.unchanged(this);
				}

				// merge into node
				final CompactNode<K> node = mergeNodes(key3, key3.hashCode(), key, keyHash, shift
								+ BIT_PARTITION_SIZE);

				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, pos4,
								key4, mask, node);
				return Result.modified(thisNew);

			} else if (mask == pos4) {
				if (cmp.compare(key, key4) == 0) {
					return Result.unchanged(this);
				}

				// merge into node
				final CompactNode<K> node = mergeNodes(key4, key4.hashCode(), key, keyHash, shift
								+ BIT_PARTITION_SIZE);

				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, pos3,
								key3, mask, node);
				return Result.modified(thisNew);

			}

			// no value

			if (mask < pos1) {
				final CompactNode<K> thisNew = valNodeOf(mutator, mask, key, pos1, key1, pos2,
								key2, pos3, key3, pos4, key4);
				return Result.modified(thisNew);
			}

			else if (mask < pos2) {
				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask, key, pos2,
								key2, pos3, key3, pos4, key4);
				return Result.modified(thisNew);
			}

			else if (mask < pos3) {
				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, mask,
								key, pos3, key3, pos4, key4);
				return Result.modified(thisNew);
			}

			else if (mask < pos4) {
				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, pos3,
								key3, mask, key, pos4, key4);
				return Result.modified(thisNew);
			}

			else {
				final CompactNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, pos3,
								key3, pos4, key4, mask, key);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, Void, ? extends CompactNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos2, key2, pos3, key3, pos4, key4));
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, pos3, key3, pos4, key4));
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, pos4, key4));
			} else if (mask == pos4) {
				if (cmp.compare(key, key4) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, pos3, key3));
			}

			return Result.unchanged(this);
		}

		@Override
		boolean contains(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return true;
			} else if (mask == pos3 && cmp.compare(key, key3) == 0) {
				return true;
			} else if (mask == pos4 && cmp.compare(key, key4) == 0) {
				return true;
			}

			return false;
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(key1);
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(key2);
			} else if (mask == pos3 && cmp.compare(key, key3) == 0) {
				return Optional.of(key3);
			} else if (mask == pos4 && cmp.compare(key, key4) == 0) {
				return Optional.of(key4);
			}

			return Optional.empty();
		}

		@Override
		Iterator<CompactNode<K>> nodeIterator() {
			return Collections.emptyIterator();
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
		SupplierIterator<K, K> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, key1, key2, key2, key3, key3,
							key4, key4 });
		}

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		int valueArity() {
			return 4;
		}

		@Override
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;
			case 1:
				return key2;
			case 2:
				return key3;
			case 3:
				return key4;

			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		public CompactNode<K> getNode(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + pos2;
			result = prime * result + key2.hashCode();
			result = prime * result + pos3;
			result = prime * result + key3.hashCode();
			result = prime * result + pos4;
			result = prime * result + key4.hashCode();
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
			Value4Index0Node<?> that = (Value4Index0Node<?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (pos2 != that.pos2) {
				return false;
			}
			if (pos3 != that.pos3) {
				return false;
			}
			if (pos4 != that.pos4) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!key4.equals(that.key4)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s, %s, %s, %s]", key1, key2, key3, key4);
		}

		@Override
		K headKey() {
			return key1;
		}

	}

}
