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
					CompactSetNode.EMPTY_INPLACE_INDEX_NODE, 0, 0);

	private static final boolean USE_SPECIALIAZIONS = true;
	private static final boolean USE_STACK_ITERATOR = true; // does not effect TransientSet
	
	private final AbstractSetNode<K> rootNode;
	private final int hashCode;
	private final int cachedSize;

	TrieSet(AbstractSetNode<K> rootNode, int hashCode, int cachedSize) {
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
		final Result<K, Void, ? extends AbstractSetNode<K>> result = rootNode.updated(null, key,
						keyHash, null, 0, cmp);

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
		final Result<K, Void, ? extends AbstractSetNode<K>> result = rootNode.removed(null, key,
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

		int[] indexAndLength = new int[16 * 2];

		@SuppressWarnings("unchecked")
		AbstractSetNode<K>[] nodes = new AbstractSetNode[16];

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
			final Result<K, Void, ? extends AbstractSetNode<K>> result = rootNode.removed(mutator,
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
		public int size() {
			return cachedSize;
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
		abstract Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator,
						K key, int keyHash, Void val, int shift, Comparator<Object> cmp);

		@Override
		abstract Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator,
						K key, int hash, int shift);

		@Override
		abstract Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator,
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
			if (USE_SPECIALIAZIONS) {
				EMPTY_INPLACE_INDEX_NODE = new Set0To0Node<>(null);
			} else {
				EMPTY_INPLACE_INDEX_NODE = new BitmapIndexedSetNode<>(null, 0, 0, new Object[] {},
								(byte) 0);
			}
		};		
		
		@SuppressWarnings("unchecked")
		static final <K> CompactSetNode<K> valNodeOf(AtomicReference<Thread> mutator) {
			return EMPTY_INPLACE_INDEX_NODE;
		}

		static final <K> CompactSetNode<K> valNodeOf(AtomicReference<Thread> mutator, int bitmap,
						int valmap, Object[] nodes, byte valueArity) {
			return new BitmapIndexedSetNode<>(mutator, bitmap, valmap, nodes, valueArity);
		}		
		
		static final <K> CompactSetNode<K> valNodeOf(AtomicReference<Thread> mutator, byte pos,
						CompactSetNode<K> node) {
			switch (pos) {
			case 0:
				return new SingletonSetNodeAtMask0Node<>(node);
			case 1:
				return new SingletonSetNodeAtMask1Node<>(node);
			case 2:
				return new SingletonSetNodeAtMask2Node<>(node);
			case 3:
				return new SingletonSetNodeAtMask3Node<>(node);
			case 4:
				return new SingletonSetNodeAtMask4Node<>(node);
			case 5:
				return new SingletonSetNodeAtMask5Node<>(node);
			case 6:
				return new SingletonSetNodeAtMask6Node<>(node);
			case 7:
				return new SingletonSetNodeAtMask7Node<>(node);
			case 8:
				return new SingletonSetNodeAtMask8Node<>(node);
			case 9:
				return new SingletonSetNodeAtMask9Node<>(node);
			case 10:
				return new SingletonSetNodeAtMask10Node<>(node);
			case 11:
				return new SingletonSetNodeAtMask11Node<>(node);
			case 12:
				return new SingletonSetNodeAtMask12Node<>(node);
			case 13:
				return new SingletonSetNodeAtMask13Node<>(node);
			case 14:
				return new SingletonSetNodeAtMask14Node<>(node);
			case 15:
				return new SingletonSetNodeAtMask15Node<>(node);
			case 16:
				return new SingletonSetNodeAtMask16Node<>(node);
			case 17:
				return new SingletonSetNodeAtMask17Node<>(node);
			case 18:
				return new SingletonSetNodeAtMask18Node<>(node);
			case 19:
				return new SingletonSetNodeAtMask19Node<>(node);
			case 20:
				return new SingletonSetNodeAtMask20Node<>(node);
			case 21:
				return new SingletonSetNodeAtMask21Node<>(node);
			case 22:
				return new SingletonSetNodeAtMask22Node<>(node);
			case 23:
				return new SingletonSetNodeAtMask23Node<>(node);
			case 24:
				return new SingletonSetNodeAtMask24Node<>(node);
			case 25:
				return new SingletonSetNodeAtMask25Node<>(node);
			case 26:
				return new SingletonSetNodeAtMask26Node<>(node);
			case 27:
				return new SingletonSetNodeAtMask27Node<>(node);
			case 28:
				return new SingletonSetNodeAtMask28Node<>(node);
			case 29:
				return new SingletonSetNodeAtMask29Node<>(node);
			case 30:
				return new SingletonSetNodeAtMask30Node<>(node);
			case 31:
				return new SingletonSetNodeAtMask31Node<>(node);

			default:
				throw new IllegalStateException("Position out of range.");
			}
		}
		
		static final <K> CompactSetNode<K> valNodeOf(final AtomicReference<Thread> mutator,
						final byte npos1, final CompactSetNode<K> node1, final byte npos2,
						final CompactSetNode<K> node2) {
			return new Set0To2Node<>(mutator, npos1, node1, npos2, node2);
		}

		static final <K> CompactSetNode<K> valNodeOf(final AtomicReference<Thread> mutator,
						final byte npos1, final CompactSetNode<K> node1, final byte npos2,
						final CompactSetNode<K> node2, final byte npos3,
						final CompactSetNode<K> node3) {
			return new Set0To3Node<>(mutator, npos1, node1, npos2, node2, npos3, node3);
		}

		static final <K> CompactSetNode<K> valNodeOf(final AtomicReference<Thread> mutator,
						final byte npos1, final CompactSetNode<K> node1, final byte npos2,
						final CompactSetNode<K> node2, final byte npos3,
						final CompactSetNode<K> node3, final byte npos4,
						final CompactSetNode<K> node4) {
			return new Set0To4Node<>(mutator, npos1, node1, npos2, node2, npos3, node3, npos4,
							node4);
		}

		static final <K> CompactSetNode<K> valNodeOf(final AtomicReference<Thread> mutator,
						final byte npos1, final CompactSetNode<K> node1, final byte npos2,
						final CompactSetNode<K> node2, final byte npos3,
						final CompactSetNode<K> node3, final byte npos4,
						final CompactSetNode<K> node4, final byte npos5,
						final CompactSetNode<K> node5) {
			final int bitmap = 0 | (1 << npos1) | (1 << npos2) | (1 << npos3) | (1 << npos4)
							| (1 << npos5);
			final int valmap = 0;

			return valNodeOf(mutator, bitmap, valmap, new Object[] { node1, node2, node3, node4,
							node5 }, (byte) 0);
		}

		static final <K> CompactSetNode<K> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1) {
			return new Set1To0Node<>(mutator, pos1, key1);
		}

		static final <K> CompactSetNode<K> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final byte npos1,
						final CompactSetNode<K> node1) {
			return new Set1To1Node<>(mutator, pos1, key1, npos1, node1);
		}

		static final <K> CompactSetNode<K> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final byte npos1,
						final CompactSetNode<K> node1, final byte npos2,
						final CompactSetNode<K> node2) {
			return new Set1To2Node<>(mutator, pos1, key1, npos1, node1, npos2, node2);
		}

		static final <K> CompactSetNode<K> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final byte npos1,
						final CompactSetNode<K> node1, final byte npos2,
						final CompactSetNode<K> node2, final byte npos3,
						final CompactSetNode<K> node3) {
			return new Set1To3Node<>(mutator, pos1, key1, npos1, node1, npos2, node2, npos3, node3);
		}

		static final <K> CompactSetNode<K> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final byte npos1,
						final CompactSetNode<K> node1, final byte npos2,
						final CompactSetNode<K> node2, final byte npos3,
						final CompactSetNode<K> node3, final byte npos4,
						final CompactSetNode<K> node4) {
			final int bitmap = 0 | (1 << pos1) | (1 << npos1) | (1 << npos2) | (1 << npos3)
							| (1 << npos4);
			final int valmap = 0 | (1 << pos1);

			return valNodeOf(mutator, bitmap, valmap, new Object[] { key1, node1, node2, node3,
							node4 }, (byte) 1);
		}

		static final <K> CompactSetNode<K> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final byte pos2, final K key2) {
			return new Set2To0Node<>(mutator, pos1, key1, pos2, key2);
		}

		static final <K> CompactSetNode<K> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final byte pos2, final K key2,
						final byte npos1, final CompactSetNode<K> node1) {
			return new Set2To1Node<>(mutator, pos1, key1, pos2, key2, npos1, node1);
		}

		static final <K> CompactSetNode<K> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final byte pos2, final K key2,
						final byte npos1, final CompactSetNode<K> node1, final byte npos2,
						final CompactSetNode<K> node2) {
			return new Set2To2Node<>(mutator, pos1, key1, pos2, key2, npos1, node1, npos2, node2);
		}

		static final <K> CompactSetNode<K> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final byte pos2, final K key2,
						final byte npos1, final CompactSetNode<K> node1, final byte npos2,
						final CompactSetNode<K> node2, final byte npos3,
						final CompactSetNode<K> node3) {
			final int bitmap = 0 | (1 << pos1) | (1 << pos2) | (1 << npos1) | (1 << npos2)
							| (1 << npos3);
			final int valmap = 0 | (1 << pos1) | (1 << pos2);

			return valNodeOf(mutator, bitmap, valmap, new Object[] { key1, key2, node1, node2,
							node3 }, (byte) 2);
		}

		static final <K> CompactSetNode<K> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final byte pos2, final K key2,
						final byte pos3, final K key3) {
			return new Set3To0Node<>(mutator, pos1, key1, pos2, key2, pos3, key3);
		}

		static final <K> CompactSetNode<K> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final byte pos2, final K key2,
						final byte pos3, final K key3, final byte npos1,
						final CompactSetNode<K> node1) {
			return new Set3To1Node<>(mutator, pos1, key1, pos2, key2, pos3, key3, npos1, node1);
		}

		static final <K> CompactSetNode<K> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final byte pos2, final K key2,
						final byte pos3, final K key3, final byte npos1,
						final CompactSetNode<K> node1, final byte npos2,
						final CompactSetNode<K> node2) {
			final int bitmap = 0 | (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << npos1)
							| (1 << npos2);
			final int valmap = 0 | (1 << pos1) | (1 << pos2) | (1 << pos3);

			return valNodeOf(mutator, bitmap, valmap,
							new Object[] { key1, key2, key3, node1, node2 }, (byte) 3);
		}

		static final <K> CompactSetNode<K> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final byte pos2, final K key2,
						final byte pos3, final K key3, final byte pos4, final K key4) {
			return new Set4To0Node<>(mutator, pos1, key1, pos2, key2, pos3, key3, pos4, key4);
		}

		static final <K> CompactSetNode<K> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final byte pos2, final K key2,
						final byte pos3, final K key3, final byte pos4, final K key4,
						final byte npos1, final CompactSetNode<K> node1) {
			final int bitmap = 0 | (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << pos4)
							| (1 << npos1);
			final int valmap = 0 | (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << pos4);

			return valNodeOf(mutator, bitmap, valmap,
							new Object[] { key1, key2, key3, key4, node1 }, (byte) 4);
		}

		static final <K> CompactSetNode<K> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final byte pos2, final K key2,
						final byte pos3, final K key3, final byte pos4, final K key4,
						final byte pos5, final K key5) {
			final int bitmap = 0 | (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << pos4)
							| (1 << pos5);
			final int valmap = 0 | (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << pos4)
							| (1 << pos5);

			return valNodeOf(mutator, bitmap, valmap,
							new Object[] { key1, key2, key3, key4, key5 }, (byte) 5);
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

				if (USE_SPECIALIAZIONS) {
					if (mask0 < mask1) {
						return valNodeOf(null, (byte) mask0, key0, (byte) mask1, key1);
					} else {
						return valNodeOf(null, (byte) mask1, key1, (byte) mask0, key0);
					}
				} else {
					final int valmap = 1 << mask0 | 1 << mask1;

					if (mask0 < mask1) {
						return valNodeOf(null, valmap, valmap, new Object[] { key0, key1 },
										(byte) 2);
					} else {
						return valNodeOf(null, valmap, valmap, new Object[] { key1, key0 },
										(byte) 2);
					}
				}
			} else {
				// values fit on next level
				final CompactSetNode<K> node = mergeNodes(key0, keyHash0, key1, keyHash1, shift
								+ BIT_PARTITION_SIZE);

				if (USE_SPECIALIAZIONS) {
					return valNodeOf(null, (byte) mask0, node);
				} else {
					final int bitmap = 1 << mask0;
					return valNodeOf(null, bitmap, 0, new Object[] { node }, (byte) 0);
				}
			}
		}

		static final <K> CompactSetNode<K> mergeNodes(CompactSetNode<K> node0, int keyHash0,
						K key1, int keyHash1, int shift) {
			final int mask0 = (keyHash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (keyHash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level

				if (USE_SPECIALIAZIONS) {
					// store values before node
					return valNodeOf(null, (byte) mask1, key1, (byte) mask0, node0);
				} else {
					final int bitmap = 1 << mask0 | 1 << mask1;
					final int valmap = 1 << mask1;

					// store values before node
					return valNodeOf(null, bitmap, valmap, new Object[] { key1, node0 }, (byte) 1);
				}
			} else {
				// values fit on next level
				final CompactSetNode<K> node = mergeNodes(node0, keyHash0, key1, keyHash1, shift
								+ BIT_PARTITION_SIZE);

				if (USE_SPECIALIAZIONS) {
					return valNodeOf(null, (byte) mask0, node);
				} else {
					final int bitmap = 1 << mask0;
					return valNodeOf(null, bitmap, 0, new Object[] { node }, (byte) 0);
				}
			}
		}
	}

	private static final class BitmapIndexedSetNode<K> extends CompactSetNode<K> {
		private AtomicReference<Thread> mutator;

		private Object[] nodes;
		final private int bitmap;
		final private int valmap;
		final private byte payloadArity;

		BitmapIndexedSetNode(AtomicReference<Thread> mutator, int bitmap, int valmap, Object[] nodes,
						byte payloadArity) {
			assert (Integer.bitCount(valmap) + Integer.bitCount(bitmap ^ valmap) == nodes.length);

			this.mutator = mutator;

			this.nodes = nodes;
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.payloadArity = payloadArity;

			assert (payloadArity == Integer.bitCount(valmap));
			assert (payloadArity() >= 2 || nodeArity() >= 1); // =
															// SIZE_MORE_THAN_ONE

			// for (int i = 0; i < valueArity; i++)
			// assert ((nodes[i] instanceof CompactSetNode) == false);
			//
			// for (int i = valueArity; i < nodes.length; i++)
			// assert ((nodes[i] instanceof CompactSetNode) == true);

			// assert invariant
			assert nodeInvariant();
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
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift) {
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
								thisNew = CompactSetNode.<K> valNodeOf(mutator, newValmap,
												newValmap, new Object[] { nodes[2], nodes[3] },
												(byte) (1));
							} else {
								thisNew = CompactSetNode.<K> valNodeOf(mutator, newValmap,
												newValmap, new Object[] { nodes[0], nodes[1] },
												(byte) (1));
							}
			
							return Result.modified(thisNew);
						} else if (USE_SPECIALIAZIONS && this.arity() == 5) {
							return Result.modified(removeInplaceValueAndConvertSpecializedNode(mask, bitpos));
						} else {
							final Object[] editableNodes = copyAndRemove(this.nodes, valIndex);
				
							final CompactSetNode<K> thisNew = CompactSetNode.<K> valNodeOf(mutator,
											this.bitmap & ~bitpos, this.valmap & ~bitpos, editableNodes,
											(byte) (payloadArity - 1));
				
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
						} else if (USE_SPECIALIAZIONS && this.arity() == 5) {
							return Result.modified(removeSubNodeAndConvertSpecializedNode(mask, bitpos));
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
							final int valIndex = valIndex(bitpos);
				
							final Object[] editableNodes = copyAndMoveToFront(this.nodes, bitIndex,
											valIndex, subNodeNew.headKey());
				
							final CompactSetNode<K> thisNew = CompactSetNode.<K> valNodeOf(mutator, bitmap,
											valmap | bitpos, editableNodes, (byte) (payloadArity + 1));
				
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
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift, Comparator<Object> cmp) {
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
								thisNew = CompactSetNode.<K> valNodeOf(mutator, newValmap,
												newValmap, new Object[] { nodes[2], nodes[3] },
												(byte) (1));
							} else {
								thisNew = CompactSetNode.<K> valNodeOf(mutator, newValmap,
												newValmap, new Object[] { nodes[0], nodes[1] },
												(byte) (1));
							}
			
							return Result.modified(thisNew);
						} else if (USE_SPECIALIAZIONS && this.arity() == 5) {
							return Result.modified(removeInplaceValueAndConvertSpecializedNode(mask, bitpos));
						} else {
							final Object[] editableNodes = copyAndRemove(this.nodes, valIndex);
				
							final CompactSetNode<K> thisNew = CompactSetNode.<K> valNodeOf(mutator,
											this.bitmap & ~bitpos, this.valmap & ~bitpos, editableNodes,
											(byte) (payloadArity - 1));
				
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
						} else if (USE_SPECIALIAZIONS && this.arity() == 5) {
							return Result.modified(removeSubNodeAndConvertSpecializedNode(mask, bitpos));
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
							final int valIndex = valIndex(bitpos);
				
							final Object[] editableNodes = copyAndMoveToFront(this.nodes, bitIndex,
											valIndex, subNodeNew.headKey());
				
							final CompactSetNode<K> thisNew = CompactSetNode.<K> valNodeOf(mutator, bitmap,
											valmap | bitpos, editableNodes, (byte) (payloadArity + 1));
				
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
		private CompactSetNode<K> removeInplaceValueAndConvertSpecializedNode(final int mask,
						final int bitpos) {
			switch (this.payloadArity) { // 0 <= payloadArity <= 5
			case 1: {
				final int nmap = ((bitmap & ~bitpos) ^ (valmap & ~bitpos));
				final byte npos1 = recoverMask(nmap, (byte) 1);
				final byte npos2 = recoverMask(nmap, (byte) 2);
				final byte npos3 = recoverMask(nmap, (byte) 3);
				final byte npos4 = recoverMask(nmap, (byte) 4);
				final CompactSetNode<K> node1 = (CompactSetNode<K>) nodes[payloadArity + 0];
				final CompactSetNode<K> node2 = (CompactSetNode<K>) nodes[payloadArity + 1];
				final CompactSetNode<K> node3 = (CompactSetNode<K>) nodes[payloadArity + 2];
				final CompactSetNode<K> node4 = (CompactSetNode<K>) nodes[payloadArity + 3];

				return valNodeOf(mutator, npos1, node1, npos2, node2, npos3, node3, npos4, node4);
			}
			case 2: {
				final int map = (valmap & ~bitpos);
				final byte pos1 = recoverMask(map, (byte) 1);
				final K key1;

				final int nmap = ((bitmap & ~bitpos) ^ (valmap & ~bitpos));
				final byte npos1 = recoverMask(nmap, (byte) 1);
				final byte npos2 = recoverMask(nmap, (byte) 2);
				final byte npos3 = recoverMask(nmap, (byte) 3);
				final CompactSetNode<K> node1 = (CompactSetNode<K>) nodes[payloadArity + 0];
				final CompactSetNode<K> node2 = (CompactSetNode<K>) nodes[payloadArity + 1];
				final CompactSetNode<K> node3 = (CompactSetNode<K>) nodes[payloadArity + 2];

				if (mask < pos1) {
					key1 = (K) nodes[1];
				} else {
					key1 = (K) nodes[0];
				}

				return valNodeOf(mutator, pos1, key1, npos1, node1, npos2, node2, npos3, node3);
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
				final CompactSetNode<K> node1 = (CompactSetNode<K>) nodes[payloadArity + 0];
				final CompactSetNode<K> node2 = (CompactSetNode<K>) nodes[payloadArity + 1];

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

				return valNodeOf(mutator, pos1, key1, pos2, key2, npos1, node1, npos2, node2);
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
				final CompactSetNode<K> node1 = (CompactSetNode<K>) nodes[payloadArity + 0];

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

				return valNodeOf(mutator, pos1, key1, pos2, key2, pos3, key3, npos1, node1);
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

				return valNodeOf(mutator, pos1, key1, pos2, key2, pos3, key3, pos4, key4);
			}
			default: {
				throw new IllegalStateException();
			}
			}				
		}

		@SuppressWarnings("unchecked")
		private CompactSetNode<K> removeSubNodeAndConvertSpecializedNode(final int mask,
						final int bitpos) {
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

				return valNodeOf(mutator, pos1, key1, pos2, key2, pos3, key3, pos4, key4);
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
				final CompactSetNode<K> node1;

				if (mask < npos1) {
					node1 = (CompactSetNode<K>) nodes[payloadArity + 1];
				} else {
					node1 = (CompactSetNode<K>) nodes[payloadArity + 0];
				}

				return valNodeOf(mutator, pos1, key1, pos2, key2, pos3, key3, npos1, node1);
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
				final CompactSetNode<K> node1;
				final CompactSetNode<K> node2;

				if (mask < npos1) {
					node1 = (CompactSetNode<K>) nodes[payloadArity + 1];
					node2 = (CompactSetNode<K>) nodes[payloadArity + 2];
				} else if (mask < npos2) {
					node1 = (CompactSetNode<K>) nodes[payloadArity + 0];
					node2 = (CompactSetNode<K>) nodes[payloadArity + 2];
				} else {
					node1 = (CompactSetNode<K>) nodes[payloadArity + 0];
					node2 = (CompactSetNode<K>) nodes[payloadArity + 1];
				}

				return valNodeOf(mutator, pos1, key1, pos2, key2, npos1, node1, npos2, node2);
			}
			case 4: {
				final int map = valmap;
				final byte pos1 = recoverMask(map, (byte) 1);
				final K key1 = (K) nodes[0];

				final int nmap = ((bitmap & ~bitpos) ^ valmap);
				final byte npos1 = recoverMask(nmap, (byte) 1);
				final byte npos2 = recoverMask(nmap, (byte) 2);
				final byte npos3 = recoverMask(nmap, (byte) 3);
				final CompactSetNode<K> node1;
				final CompactSetNode<K> node2;
				final CompactSetNode<K> node3;

				if (mask < npos1) {
					node1 = (CompactSetNode<K>) nodes[payloadArity + 1];
					node2 = (CompactSetNode<K>) nodes[payloadArity + 2];
					node3 = (CompactSetNode<K>) nodes[payloadArity + 3];
				} else if (mask < npos2) {
					node1 = (CompactSetNode<K>) nodes[payloadArity + 0];
					node2 = (CompactSetNode<K>) nodes[payloadArity + 2];
					node3 = (CompactSetNode<K>) nodes[payloadArity + 3];
				} else if (mask < npos3) {
					node1 = (CompactSetNode<K>) nodes[payloadArity + 0];
					node2 = (CompactSetNode<K>) nodes[payloadArity + 1];
					node3 = (CompactSetNode<K>) nodes[payloadArity + 3];
				} else {
					node1 = (CompactSetNode<K>) nodes[payloadArity + 0];
					node2 = (CompactSetNode<K>) nodes[payloadArity + 1];
					node3 = (CompactSetNode<K>) nodes[payloadArity + 2];
				}

				return valNodeOf(mutator, pos1, key1, npos1, node1, npos2, node2, npos3, node3);
			}
			case 5: {
				final int nmap = ((bitmap & ~bitpos) ^ valmap);
				final byte npos1 = recoverMask(nmap, (byte) 1);
				final byte npos2 = recoverMask(nmap, (byte) 2);
				final byte npos3 = recoverMask(nmap, (byte) 3);
				final byte npos4 = recoverMask(nmap, (byte) 4);
				final CompactSetNode<K> node1;
				final CompactSetNode<K> node2;
				final CompactSetNode<K> node3;
				final CompactSetNode<K> node4;

				if (mask < npos1) {
					node1 = (CompactSetNode<K>) nodes[payloadArity + 1];
					node2 = (CompactSetNode<K>) nodes[payloadArity + 2];
					node3 = (CompactSetNode<K>) nodes[payloadArity + 3];
					node4 = (CompactSetNode<K>) nodes[payloadArity + 4];
				} else if (mask < npos2) {
					node1 = (CompactSetNode<K>) nodes[payloadArity + 0];
					node2 = (CompactSetNode<K>) nodes[payloadArity + 2];
					node3 = (CompactSetNode<K>) nodes[payloadArity + 3];
					node4 = (CompactSetNode<K>) nodes[payloadArity + 4];
				} else if (mask < npos3) {
					node1 = (CompactSetNode<K>) nodes[payloadArity + 0];
					node2 = (CompactSetNode<K>) nodes[payloadArity + 1];
					node3 = (CompactSetNode<K>) nodes[payloadArity + 3];
					node4 = (CompactSetNode<K>) nodes[payloadArity + 4];
				} else if (mask < npos4) {
					node1 = (CompactSetNode<K>) nodes[payloadArity + 0];
					node2 = (CompactSetNode<K>) nodes[payloadArity + 1];
					node3 = (CompactSetNode<K>) nodes[payloadArity + 2];
					node4 = (CompactSetNode<K>) nodes[payloadArity + 4];
				} else {
					node1 = (CompactSetNode<K>) nodes[payloadArity + 0];
					node2 = (CompactSetNode<K>) nodes[payloadArity + 1];
					node3 = (CompactSetNode<K>) nodes[payloadArity + 2];
					node4 = (CompactSetNode<K>) nodes[payloadArity + 3];
				}

				return valNodeOf(mutator, npos1, node1, npos2, node2, npos3, node3, npos4, node4);
			}
			default: {
				throw new IllegalStateException();
			}			
			}
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
			return ArraySupplierIterator.of(nodes, 0, payloadArity);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<AbstractSetNode<K>> nodeIterator() {
			final int offset = payloadArity;

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
			return ArraySupplierIterator.of(keys);
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
		 * always returns a new immutable {@link TrieSet} instance.
		 */
		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift, Comparator<Object> cmp) {
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
		 * returns a new immutable {@link TrieSet} instance.
		 */
		@SuppressWarnings("unchecked")
		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			for (int i = 0; i < keys.length; i++) {
				if (cmp.compare(keys[i], key) == 0) {
					if (this.arity() == 1) {
						return Result.modified(CompactSetNode.<K> valNodeOf(mutator));
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node
						 * will be a) either be the new root returned, or b)
						 * unwrapped and inlined.
						 */
						final K theOtherKey = (i == 0) ? keys[1] : keys[0];
						return CompactSetNode.<K> valNodeOf(mutator).updated(mutator, theOtherKey,
										keyHash, null, 0, cmp);
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
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift) {
			return updated(mutator, key, keyHash, val, shift, EqualityUtils.getDefaultEqualityComparator());
		}

		// TODO: generate instead of delegate
		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			return removed(mutator, key, keyHash, shift, EqualityUtils.getDefaultEqualityComparator());
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

	private abstract static class AbstractSingletonSetNode<K> extends CompactSetNode<K> {

		protected abstract byte npos1();

		protected final CompactSetNode<K> node1;

		AbstractSingletonSetNode(CompactSetNode<K> node1) {
			this.node1 = node1;
			assert nodeInvariant();
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator,
						K key, int keyHash, Void val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				final Result<K, Void, ? extends CompactSetNode<K>> subNodeResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactSetNode<K> thisNew = valNodeOf(mutator, mask, subNodeResult.getNode());

				return Result.modified(thisNew);
			}

			// no value
			final CompactSetNode<K> thisNew = valNodeOf(mutator, mask, key, npos1(), node1);
			return Result.modified(thisNew);
		}		
		
		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator,
						K key, int keyHash, Void val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				final Result<K, Void, ? extends CompactSetNode<K>> subNodeResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactSetNode<K> thisNew = valNodeOf(mutator, mask, subNodeResult.getNode());

				return Result.modified(thisNew);
			}

			// no value
			final CompactSetNode<K> thisNew = valNodeOf(mutator, mask, key, npos1(), node1);
			return Result.modified(thisNew);
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				final Result<K, Void, ? extends CompactSetNode<K>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactSetNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_EMPTY:
				case SIZE_ONE:
					// escalate (singleton or empty) result
					return subNodeResult;

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)
					final CompactSetNode<K> thisNew = valNodeOf(mutator, mask, subNodeNew);
					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Invalid size state.");
				}
			}

			return Result.unchanged(this);
		}	
		
		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				final Result<K, Void, ? extends CompactSetNode<K>> subNodeResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactSetNode<K> subNodeNew = subNodeResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case SIZE_EMPTY:
				case SIZE_ONE:
					// escalate (singleton or empty) result
					return subNodeResult;

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)
					final CompactSetNode<K> thisNew = valNodeOf(mutator, mask, subNodeNew);
					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Invalid size state.");
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			}

			return false;
		}	
		
		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			}

			return Optional.empty();
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
		public AbstractSetNode<K> getNode(int index) {
			if (index == 0) {
				return node1;
			} else {
				throw new IndexOutOfBoundsException();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<AbstractSetNode<K>> nodeIterator() {
			return ArrayIterator.<AbstractSetNode<K>> of(new AbstractSetNode[] { node1 });
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
		SupplierIterator<K, K> payloadIterator() {
			return EmptySupplierIterator.emptyIterator();
		}

		@Override
		boolean hasPayload() {
			return false;
		}

		@Override
		int payloadArity() {
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
			AbstractSingletonSetNode<?> that = (AbstractSingletonSetNode<?>) other;

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

	private static final class SingletonSetNodeAtMask0Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 0;
		}

		SingletonSetNodeAtMask0Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask1Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 1;
		}

		SingletonSetNodeAtMask1Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask2Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 2;
		}

		SingletonSetNodeAtMask2Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask3Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 3;
		}

		SingletonSetNodeAtMask3Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask4Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 4;
		}

		SingletonSetNodeAtMask4Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask5Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 5;
		}

		SingletonSetNodeAtMask5Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask6Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 6;
		}

		SingletonSetNodeAtMask6Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask7Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 7;
		}

		SingletonSetNodeAtMask7Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask8Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 8;
		}

		SingletonSetNodeAtMask8Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask9Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 9;
		}

		SingletonSetNodeAtMask9Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask10Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 10;
		}

		SingletonSetNodeAtMask10Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask11Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 11;
		}

		SingletonSetNodeAtMask11Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask12Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 12;
		}

		SingletonSetNodeAtMask12Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask13Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 13;
		}

		SingletonSetNodeAtMask13Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask14Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 14;
		}

		SingletonSetNodeAtMask14Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask15Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 15;
		}

		SingletonSetNodeAtMask15Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask16Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 16;
		}

		SingletonSetNodeAtMask16Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask17Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 17;
		}

		SingletonSetNodeAtMask17Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask18Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 18;
		}

		SingletonSetNodeAtMask18Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask19Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 19;
		}

		SingletonSetNodeAtMask19Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask20Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 20;
		}

		SingletonSetNodeAtMask20Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask21Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 21;
		}

		SingletonSetNodeAtMask21Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask22Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 22;
		}

		SingletonSetNodeAtMask22Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask23Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 23;
		}

		SingletonSetNodeAtMask23Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask24Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 24;
		}

		SingletonSetNodeAtMask24Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask25Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 25;
		}

		SingletonSetNodeAtMask25Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask26Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 26;
		}

		SingletonSetNodeAtMask26Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask27Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 27;
		}

		SingletonSetNodeAtMask27Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask28Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 28;
		}

		SingletonSetNodeAtMask28Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask29Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 29;
		}

		SingletonSetNodeAtMask29Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask30Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 30;
		}

		SingletonSetNodeAtMask30Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class SingletonSetNodeAtMask31Node<K> extends AbstractSingletonSetNode<K> {

		@Override
		protected byte npos1() {
			return 31;
		}

		SingletonSetNodeAtMask31Node(CompactSetNode<K> node1) {
			super(node1);
		}

	}

	private static final class Set0To0Node<K> extends CompactSetNode<K> {

		Set0To0Node(final AtomicReference<Thread> mutator) {

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			return Result.modified(valNodeOf(mutator, mask, key));
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			return Result.modified(valNodeOf(mutator, mask, key));
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			return Result.unchanged(this);
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift) {
			return false;
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			return false;
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift) {
			return Optional.empty();
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactSetNode<K>> nodeIterator() {
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
		SupplierIterator<K, K> payloadIterator() {
			return EmptySupplierIterator.emptyIterator();
		}

		@Override
		boolean hasPayload() {
			return false;
		}

		@Override
		int payloadArity() {
			return 0;
		}

		@Override
		K headKey() {
			throw new UnsupportedOperationException("Node does not directly contain a key.");
		}

		@Override
		AbstractSetNode<K> getNode(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		K getKey(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		byte sizePredicate() {
			return SIZE_EMPTY;
		}

		@Override
		public int hashCode() {
			int result = 1;

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

			return true;
		}

		@Override
		public String toString() {
			return "[]";
		}

	}

	private static final class Set0To2Node<K> extends CompactSetNode<K> {

		private final byte npos1;
		private final CompactSetNode<K> node1;

		private final byte npos2;
		private final CompactSetNode<K> node2;

		Set0To2Node(final AtomicReference<Thread> mutator, final byte npos1,
						final CompactSetNode<K> node1, final byte npos2, final CompactSetNode<K> node2) {

			this.npos1 = npos1;
			this.node1 = node1;

			this.npos2 = npos2;
			this.node2 = node2;

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, mask, nestedResult.getNode(),
									npos2, node2);
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, npos1, node1, mask,
									nestedResult.getNode());
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, mask, nestedResult.getNode(),
									npos2, node2);
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, npos1, node1, mask,
									nestedResult.getNode());
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, mask, updatedNode, npos2, node2));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node2
						result = Result.modified(valNodeOf(mutator, npos1, node1, mask, updatedNode));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, mask, updatedNode, npos2, node2));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node2
						result = Result.modified(valNodeOf(mutator, npos1, node1, mask, updatedNode));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		private CompactSetNode<K> inlineValue(AtomicReference<Thread> mutator, byte mask, K key) {
			return valNodeOf(mutator, mask, key, npos1, node1, npos2, node2);
		}

		private CompactSetNode<K> removeNode1AndInlineValue(AtomicReference<Thread> mutator, byte mask,
						K key) {
			return valNodeOf(mutator, mask, key, npos2, node2);
		}

		private CompactSetNode<K> removeNode2AndInlineValue(AtomicReference<Thread> mutator, byte mask,
						K key) {
			return valNodeOf(mutator, mask, key, npos1, node1);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return false;
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else {
				return false;
			}
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return Optional.empty();
			}
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactSetNode<K>> nodeIterator() {
			return ArrayIterator.<CompactSetNode<K>> of(new CompactSetNode[] { node1, node2 });
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
		SupplierIterator<K, K> payloadIterator() {
			return EmptySupplierIterator.emptyIterator();
		}

		@Override
		boolean hasPayload() {
			return false;
		}

		@Override
		int payloadArity() {
			return 0;
		}

		@Override
		K headKey() {
			throw new UnsupportedOperationException("Node does not directly contain a key.");
		}

		@Override
		AbstractSetNode<K> getNode(int index) {
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
		K getKey(int index) {
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
			Set0To2Node<?> that = (Set0To2Node<?>) other;

			if (npos1 != that.npos1) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			if (npos2 != that.npos2) {
				return false;
			}
			if (!node2.equals(that.node2)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s, @%d: %s]", npos1, node1, npos2, node2);
		}

	}

	private static final class Set0To3Node<K> extends CompactSetNode<K> {

		private final byte npos1;
		private final CompactSetNode<K> node1;

		private final byte npos2;
		private final CompactSetNode<K> node2;

		private final byte npos3;
		private final CompactSetNode<K> node3;

		Set0To3Node(final AtomicReference<Thread> mutator, final byte npos1,
						final CompactSetNode<K> node1, final byte npos2, final CompactSetNode<K> node2,
						final byte npos3, final CompactSetNode<K> node3) {

			this.npos1 = npos1;
			this.node1 = node1;

			this.npos2 = npos2;
			this.node2 = node2;

			this.npos3 = npos3;
			this.node3 = node3;

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, mask, nestedResult.getNode(),
									npos2, node2, npos3, node3);
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, npos1, node1, mask,
									nestedResult.getNode(), npos3, node3);
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos3) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node3.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
									mask, nestedResult.getNode());
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, mask, nestedResult.getNode(),
									npos2, node2, npos3, node3);
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, npos1, node1, mask,
									nestedResult.getNode(), npos3, node3);
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos3) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node3.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
									mask, nestedResult.getNode());
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, mask, updatedNode, npos2, node2,
										npos3, node3));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node2
						result = Result.modified(valNodeOf(mutator, npos1, node1, mask, updatedNode,
										npos3, node3));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos3) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node3.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode3AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node3
						result = Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2, mask,
										updatedNode));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, mask, updatedNode, npos2, node2,
										npos3, node3));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node2
						result = Result.modified(valNodeOf(mutator, npos1, node1, mask, updatedNode,
										npos3, node3));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos3) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node3.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode3AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node3
						result = Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2, mask,
										updatedNode));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		private CompactSetNode<K> inlineValue(AtomicReference<Thread> mutator, byte mask, K key) {
			return valNodeOf(mutator, mask, key, npos1, node1, npos2, node2, npos3, node3);
		}

		private CompactSetNode<K> removeNode1AndInlineValue(AtomicReference<Thread> mutator, byte mask,
						K key) {
			return valNodeOf(mutator, mask, key, npos2, node2, npos3, node3);
		}

		private CompactSetNode<K> removeNode2AndInlineValue(AtomicReference<Thread> mutator, byte mask,
						K key) {
			return valNodeOf(mutator, mask, key, npos1, node1, npos3, node3);
		}

		private CompactSetNode<K> removeNode3AndInlineValue(AtomicReference<Thread> mutator, byte mask,
						K key) {
			return valNodeOf(mutator, mask, key, npos1, node1, npos2, node2);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos3) {
				return node3.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return false;
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos3) {
				return node3.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else {
				return false;
			}
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos3) {
				return node3.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return Optional.empty();
			}
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
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactSetNode<K>> nodeIterator() {
			return ArrayIterator.<CompactSetNode<K>> of(new CompactSetNode[] { node1, node2, node3 });
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
		SupplierIterator<K, K> payloadIterator() {
			return EmptySupplierIterator.emptyIterator();
		}

		@Override
		boolean hasPayload() {
			return false;
		}

		@Override
		int payloadArity() {
			return 0;
		}

		@Override
		K headKey() {
			throw new UnsupportedOperationException("Node does not directly contain a key.");
		}

		@Override
		AbstractSetNode<K> getNode(int index) {
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
		K getKey(int index) {
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
			Set0To3Node<?> that = (Set0To3Node<?>) other;

			if (npos1 != that.npos1) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			if (npos2 != that.npos2) {
				return false;
			}
			if (!node2.equals(that.node2)) {
				return false;
			}
			if (npos3 != that.npos3) {
				return false;
			}
			if (!node3.equals(that.node3)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s, @%d: %s, @%d: %s]", npos1, node1, npos2, node2, npos3,
							node3);
		}

	}

	private static final class Set0To4Node<K> extends CompactSetNode<K> {

		private final byte npos1;
		private final CompactSetNode<K> node1;

		private final byte npos2;
		private final CompactSetNode<K> node2;

		private final byte npos3;
		private final CompactSetNode<K> node3;

		private final byte npos4;
		private final CompactSetNode<K> node4;

		Set0To4Node(final AtomicReference<Thread> mutator, final byte npos1,
						final CompactSetNode<K> node1, final byte npos2, final CompactSetNode<K> node2,
						final byte npos3, final CompactSetNode<K> node3, final byte npos4,
						final CompactSetNode<K> node4) {

			this.npos1 = npos1;
			this.node1 = node1;

			this.npos2 = npos2;
			this.node2 = node2;

			this.npos3 = npos3;
			this.node3 = node3;

			this.npos4 = npos4;
			this.node4 = node4;

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, mask, nestedResult.getNode(),
									npos2, node2, npos3, node3, npos4, node4);
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, npos1, node1, mask,
									nestedResult.getNode(), npos3, node3, npos4, node4);
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos3) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node3.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
									mask, nestedResult.getNode(), npos4, node4);
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos4) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node4.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
									npos3, node3, mask, nestedResult.getNode());
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, mask, nestedResult.getNode(),
									npos2, node2, npos3, node3, npos4, node4);
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, npos1, node1, mask,
									nestedResult.getNode(), npos3, node3, npos4, node4);
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos3) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node3.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
									mask, nestedResult.getNode(), npos4, node4);
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos4) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node4.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
									npos3, node3, mask, nestedResult.getNode());
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, mask, updatedNode, npos2, node2,
										npos3, node3, npos4, node4));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node2
						result = Result.modified(valNodeOf(mutator, npos1, node1, mask, updatedNode,
										npos3, node3, npos4, node4));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos3) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node3.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode3AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node3
						result = Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2, mask,
										updatedNode, npos4, node4));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos4) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node4.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode4AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node4
						result = Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2, npos3,
										node3, mask, updatedNode));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, mask, updatedNode, npos2, node2,
										npos3, node3, npos4, node4));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node2
						result = Result.modified(valNodeOf(mutator, npos1, node1, mask, updatedNode,
										npos3, node3, npos4, node4));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos3) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node3.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode3AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node3
						result = Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2, mask,
										updatedNode, npos4, node4));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos4) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node4.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode4AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node4
						result = Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2, npos3,
										node3, mask, updatedNode));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		private CompactSetNode<K> inlineValue(AtomicReference<Thread> mutator, byte mask, K key) {
			return valNodeOf(mutator, mask, key, npos1, node1, npos2, node2, npos3, node3, npos4, node4);
		}

		private CompactSetNode<K> removeNode1AndInlineValue(AtomicReference<Thread> mutator, byte mask,
						K key) {
			return valNodeOf(mutator, mask, key, npos2, node2, npos3, node3, npos4, node4);
		}

		private CompactSetNode<K> removeNode2AndInlineValue(AtomicReference<Thread> mutator, byte mask,
						K key) {
			return valNodeOf(mutator, mask, key, npos1, node1, npos3, node3, npos4, node4);
		}

		private CompactSetNode<K> removeNode3AndInlineValue(AtomicReference<Thread> mutator, byte mask,
						K key) {
			return valNodeOf(mutator, mask, key, npos1, node1, npos2, node2, npos4, node4);
		}

		private CompactSetNode<K> removeNode4AndInlineValue(AtomicReference<Thread> mutator, byte mask,
						K key) {
			return valNodeOf(mutator, mask, key, npos1, node1, npos2, node2, npos3, node3);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos3) {
				return node3.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos4) {
				return node4.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return false;
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos3) {
				return node3.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos4) {
				return node4.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else {
				return false;
			}
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos3) {
				return node3.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos4) {
				return node4.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return Optional.empty();
			}
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
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactSetNode<K>> nodeIterator() {
			return ArrayIterator.<CompactSetNode<K>> of(new CompactSetNode[] { node1, node2, node3,
							node4 });
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
		SupplierIterator<K, K> payloadIterator() {
			return EmptySupplierIterator.emptyIterator();
		}

		@Override
		boolean hasPayload() {
			return false;
		}

		@Override
		int payloadArity() {
			return 0;
		}

		@Override
		K headKey() {
			throw new UnsupportedOperationException("Node does not directly contain a key.");
		}

		@Override
		AbstractSetNode<K> getNode(int index) {
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
		K getKey(int index) {
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
			Set0To4Node<?> that = (Set0To4Node<?>) other;

			if (npos1 != that.npos1) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			if (npos2 != that.npos2) {
				return false;
			}
			if (!node2.equals(that.node2)) {
				return false;
			}
			if (npos3 != that.npos3) {
				return false;
			}
			if (!node3.equals(that.node3)) {
				return false;
			}
			if (npos4 != that.npos4) {
				return false;
			}
			if (!node4.equals(that.node4)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s, @%d: %s, @%d: %s, @%d: %s]", npos1, node1, npos2, node2,
							npos3, node3, npos4, node4);
		}

	}

	private static final class Set1To0Node<K> extends CompactSetNode<K> {

		private final byte pos1;
		private final K key1;

		Set1To0Node(final AtomicReference<Thread> mutator, final byte pos1, final K key1) {

			this.pos1 = pos1;
			this.key1 = key1;

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, mask, node));
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, mask, node));
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					// remove key1, val1
					result = Result.modified(CompactSetNode.<K> valNodeOf(mutator));
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					// remove key1, val1
					result = Result.modified(CompactSetNode.<K> valNodeOf(mutator));
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		private CompactSetNode<K> inlineValue(AtomicReference<Thread> mutator, byte mask, K key) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, pos1, key1);
			} else {
				return valNodeOf(mutator, pos1, key1, mask, key);
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				return key.equals(key1);
			} else {
				return false;
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				return cmp.compare(key, key1) == 0;
			} else {
				return false;
			}
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && key.equals(key1)) {
				return Optional.of(key1);
			} else {
				return Optional.empty();
			}
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(key1);
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactSetNode<K>> nodeIterator() {
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
		SupplierIterator<K, K> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(new Object[] { key1, key1 });
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
			return 1;
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		AbstractSetNode<K> getNode(int index) {
			throw new IllegalStateException("Index out of range.");
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
			Set1To0Node<?> that = (Set1To0Node<?>) other;

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
			return String.format("[@%d: %s]", pos1, key1);
		}

	}

	private static final class Set1To1Node<K> extends CompactSetNode<K> {

		private final byte pos1;
		private final K key1;

		private final byte npos1;
		private final CompactSetNode<K> node1;

		Set1To1Node(final AtomicReference<Thread> mutator, final byte pos1, final K key1,
						final byte npos1, final CompactSetNode<K> node1) {

			this.pos1 = pos1;
			this.key1 = key1;

			this.npos1 = npos1;
			this.node1 = node1;

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, mask, node, npos1, node1));
					} else {
						result = Result.modified(valNodeOf(mutator, npos1, node1, mask, node));
					}
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask,
									nestedResult.getNode());
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, mask, node, npos1, node1));
					} else {
						result = Result.modified(valNodeOf(mutator, npos1, node1, mask, node));
					}
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask,
									nestedResult.getNode());
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, mask, updatedNode));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, mask, updatedNode));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		private CompactSetNode<K> inlineValue(AtomicReference<Thread> mutator, byte mask, K key) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, pos1, key1, npos1, node1);
			} else {
				return valNodeOf(mutator, pos1, key1, mask, key, npos1, node1);
			}
		}

		private CompactSetNode<K> removeNode1AndInlineValue(AtomicReference<Thread> mutator, byte mask,
						K key) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, pos1, key1);
			} else {
				return valNodeOf(mutator, pos1, key1, mask, key);
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				return key.equals(key1);
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return false;
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				return cmp.compare(key, key1) == 0;
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else {
				return false;
			}
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && key.equals(key1)) {
				return Optional.of(key1);
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return Optional.empty();
			}
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(key1);
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactSetNode<K>> nodeIterator() {
			return ArrayIterator.<CompactSetNode<K>> of(new CompactSetNode[] { node1 });
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
		SupplierIterator<K, K> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(new Object[] { key1, key1 });
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
			return 1;
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		AbstractSetNode<K> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			default:
				throw new IllegalStateException("Index out of range.");
			}
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
			Set1To1Node<?> that = (Set1To1Node<?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}

			if (npos1 != that.npos1) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s, @%d: %s]", pos1, key1, npos1, node1);
		}

	}

	private static final class Set1To2Node<K> extends CompactSetNode<K> {

		private final byte pos1;
		private final K key1;

		private final byte npos1;
		private final CompactSetNode<K> node1;

		private final byte npos2;
		private final CompactSetNode<K> node2;

		Set1To2Node(final AtomicReference<Thread> mutator, final byte pos1, final K key1,
						final byte npos1, final CompactSetNode<K> node1, final byte npos2,
						final CompactSetNode<K> node2) {

			this.pos1 = pos1;
			this.key1 = key1;

			this.npos1 = npos1;
			this.node1 = node1;

			this.npos2 = npos2;
			this.node2 = node2;

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, mask, node, npos1, node1, npos2,
										node2));
					} else if (mask < npos2) {
						result = Result.modified(valNodeOf(mutator, npos1, node1, mask, node, npos2,
										node2));
					} else {
						result = Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2, mask,
										node));
					}
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask,
									nestedResult.getNode(), npos2, node2);
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, pos1, key1, npos1, node1,
									mask, nestedResult.getNode());
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, mask, node, npos1, node1, npos2,
										node2));
					} else if (mask < npos2) {
						result = Result.modified(valNodeOf(mutator, npos1, node1, mask, node, npos2,
										node2));
					} else {
						result = Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2, mask,
										node));
					}
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask,
									nestedResult.getNode(), npos2, node2);
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, pos1, key1, npos1, node1,
									mask, nestedResult.getNode());
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, mask, updatedNode,
										npos2, node2));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node2
						result = Result.modified(valNodeOf(mutator, pos1, key1, npos1, node1, mask,
										updatedNode));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, mask, updatedNode,
										npos2, node2));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node2
						result = Result.modified(valNodeOf(mutator, pos1, key1, npos1, node1, mask,
										updatedNode));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		private CompactSetNode<K> inlineValue(AtomicReference<Thread> mutator, byte mask, K key) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, pos1, key1, npos1, node1, npos2, node2);
			} else {
				return valNodeOf(mutator, pos1, key1, mask, key, npos1, node1, npos2, node2);
			}
		}

		private CompactSetNode<K> removeNode1AndInlineValue(AtomicReference<Thread> mutator, byte mask,
						K key) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, pos1, key1, npos2, node2);
			} else {
				return valNodeOf(mutator, pos1, key1, mask, key, npos2, node2);
			}
		}

		private CompactSetNode<K> removeNode2AndInlineValue(AtomicReference<Thread> mutator, byte mask,
						K key) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, pos1, key1, npos1, node1);
			} else {
				return valNodeOf(mutator, pos1, key1, mask, key, npos1, node1);
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				return key.equals(key1);
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return false;
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				return cmp.compare(key, key1) == 0;
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else {
				return false;
			}
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && key.equals(key1)) {
				return Optional.of(key1);
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return Optional.empty();
			}
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
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactSetNode<K>> nodeIterator() {
			return ArrayIterator.<CompactSetNode<K>> of(new CompactSetNode[] { node1, node2 });
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
		SupplierIterator<K, K> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(new Object[] { key1, key1 });
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
			return 1;
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		AbstractSetNode<K> getNode(int index) {
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
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;
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
			Set1To2Node<?> that = (Set1To2Node<?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}

			if (npos1 != that.npos1) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			if (npos2 != that.npos2) {
				return false;
			}
			if (!node2.equals(that.node2)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s, @%d: %s, @%d: %s]", pos1, key1, npos1, node1, npos2, node2);
		}

	}

	private static final class Set1To3Node<K> extends CompactSetNode<K> {

		private final byte pos1;
		private final K key1;

		private final byte npos1;
		private final CompactSetNode<K> node1;

		private final byte npos2;
		private final CompactSetNode<K> node2;

		private final byte npos3;
		private final CompactSetNode<K> node3;

		Set1To3Node(final AtomicReference<Thread> mutator, final byte pos1, final K key1,
						final byte npos1, final CompactSetNode<K> node1, final byte npos2,
						final CompactSetNode<K> node2, final byte npos3, final CompactSetNode<K> node3) {

			this.pos1 = pos1;
			this.key1 = key1;

			this.npos1 = npos1;
			this.node1 = node1;

			this.npos2 = npos2;
			this.node2 = node2;

			this.npos3 = npos3;
			this.node3 = node3;

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, mask, node, npos1, node1, npos2,
										node2, npos3, node3));
					} else if (mask < npos2) {
						result = Result.modified(valNodeOf(mutator, npos1, node1, mask, node, npos2,
										node2, npos3, node3));
					} else if (mask < npos3) {
						result = Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2, mask,
										node, npos3, node3));
					} else {
						result = Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2, npos3,
										node3, mask, node));
					}
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask,
									nestedResult.getNode(), npos2, node2, npos3, node3);
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, pos1, key1, npos1, node1,
									mask, nestedResult.getNode(), npos3, node3);
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos3) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node3.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, pos1, key1, npos1, node1,
									npos2, node2, mask, nestedResult.getNode());
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, mask, node, npos1, node1, npos2,
										node2, npos3, node3));
					} else if (mask < npos2) {
						result = Result.modified(valNodeOf(mutator, npos1, node1, mask, node, npos2,
										node2, npos3, node3));
					} else if (mask < npos3) {
						result = Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2, mask,
										node, npos3, node3));
					} else {
						result = Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2, npos3,
										node3, mask, node));
					}
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, pos1, key1, mask,
									nestedResult.getNode(), npos2, node2, npos3, node3);
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, pos1, key1, npos1, node1,
									mask, nestedResult.getNode(), npos3, node3);
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos3) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node3.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, pos1, key1, npos1, node1,
									npos2, node2, mask, nestedResult.getNode());
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2, npos3,
									node3));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, mask, updatedNode,
										npos2, node2, npos3, node3));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node2
						result = Result.modified(valNodeOf(mutator, pos1, key1, npos1, node1, mask,
										updatedNode, npos3, node3));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos3) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node3.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode3AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node3
						result = Result.modified(valNodeOf(mutator, pos1, key1, npos1, node1, npos2,
										node2, mask, updatedNode));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2, npos3,
									node3));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, mask, updatedNode,
										npos2, node2, npos3, node3));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node2
						result = Result.modified(valNodeOf(mutator, pos1, key1, npos1, node1, mask,
										updatedNode, npos3, node3));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos3) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node3.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode3AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node3
						result = Result.modified(valNodeOf(mutator, pos1, key1, npos1, node1, npos2,
										node2, mask, updatedNode));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		private CompactSetNode<K> inlineValue(AtomicReference<Thread> mutator, byte mask, K key) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, pos1, key1, npos1, node1, npos2, node2, npos3,
								node3);
			} else {
				return valNodeOf(mutator, pos1, key1, mask, key, npos1, node1, npos2, node2, npos3,
								node3);
			}
		}

		private CompactSetNode<K> removeNode1AndInlineValue(AtomicReference<Thread> mutator, byte mask,
						K key) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, pos1, key1, npos2, node2, npos3, node3);
			} else {
				return valNodeOf(mutator, pos1, key1, mask, key, npos2, node2, npos3, node3);
			}
		}

		private CompactSetNode<K> removeNode2AndInlineValue(AtomicReference<Thread> mutator, byte mask,
						K key) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, pos1, key1, npos1, node1, npos3, node3);
			} else {
				return valNodeOf(mutator, pos1, key1, mask, key, npos1, node1, npos3, node3);
			}
		}

		private CompactSetNode<K> removeNode3AndInlineValue(AtomicReference<Thread> mutator, byte mask,
						K key) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, pos1, key1, npos1, node1, npos2, node2);
			} else {
				return valNodeOf(mutator, pos1, key1, mask, key, npos1, node1, npos2, node2);
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				return key.equals(key1);
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos3) {
				return node3.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return false;
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				return cmp.compare(key, key1) == 0;
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos3) {
				return node3.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else {
				return false;
			}
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && key.equals(key1)) {
				return Optional.of(key1);
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos3) {
				return node3.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return Optional.empty();
			}
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
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactSetNode<K>> nodeIterator() {
			return ArrayIterator.<CompactSetNode<K>> of(new CompactSetNode[] { node1, node2, node3 });
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
		SupplierIterator<K, K> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(new Object[] { key1, key1 });
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
			return 1;
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		AbstractSetNode<K> getNode(int index) {
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
		K getKey(int index) {
			switch (index) {
			case 0:
				return key1;
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
			Set1To3Node<?> that = (Set1To3Node<?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}

			if (npos1 != that.npos1) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			if (npos2 != that.npos2) {
				return false;
			}
			if (!node2.equals(that.node2)) {
				return false;
			}
			if (npos3 != that.npos3) {
				return false;
			}
			if (!node3.equals(that.node3)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s, @%d: %s, @%d: %s, @%d: %s]", pos1, key1, npos1, node1,
							npos2, node2, npos3, node3);
		}

	}

	private static final class Set2To0Node<K> extends CompactSetNode<K> {

		private final byte pos1;
		private final K key1;

		private final byte pos2;
		private final K key2;

		Set2To0Node(final AtomicReference<Thread> mutator, final byte pos1, final K key1,
						final byte pos2, final K key2) {

			this.pos1 = pos1;
			this.key1 = key1;

			this.pos2 = pos2;
			this.key2 = key2;

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos2, key2, mask, node));
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key2, key2.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, mask, node));
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos2, key2, mask, node));
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key2, key2.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, mask, node));
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					/*
					 * Create node with element key2. This node will a) either
					 * become the new root returned, or b) unwrapped and inlined.
					 */
					final byte pos2AtShiftZero = (shift == 0) ? pos2
									: (byte) (keyHash & BIT_PARTITION_MASK);
					result = Result.modified(valNodeOf(mutator, pos2AtShiftZero, key2));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					/*
					 * Create node with element key1. This node will a) either
					 * become the new root returned, or b) unwrapped and inlined.
					 */
					final byte pos1AtShiftZero = (shift == 0) ? pos1
									: (byte) (keyHash & BIT_PARTITION_MASK);
					result = Result.modified(valNodeOf(mutator, pos1AtShiftZero, key1));
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					/*
					 * Create node with element key2. This node will a) either
					 * become the new root returned, or b) unwrapped and inlined.
					 */
					final byte pos2AtShiftZero = (shift == 0) ? pos2
									: (byte) (keyHash & BIT_PARTITION_MASK);
					result = Result.modified(valNodeOf(mutator, pos2AtShiftZero, key2));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					/*
					 * Create node with element key1. This node will a) either
					 * become the new root returned, or b) unwrapped and inlined.
					 */
					final byte pos1AtShiftZero = (shift == 0) ? pos1
									: (byte) (keyHash & BIT_PARTITION_MASK);
					result = Result.modified(valNodeOf(mutator, pos1AtShiftZero, key1));
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		private CompactSetNode<K> inlineValue(AtomicReference<Thread> mutator, byte mask, K key) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, pos1, key1, pos2, key2);
			} else if (mask < pos2) {
				return valNodeOf(mutator, pos1, key1, mask, key, pos2, key2);
			} else {
				return valNodeOf(mutator, pos1, key1, pos2, key2, mask, key);
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				return key.equals(key1);
			} else if (mask == pos2) {
				return key.equals(key2);
			} else {
				return false;
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				return cmp.compare(key, key1) == 0;
			} else if (mask == pos2) {
				return cmp.compare(key, key2) == 0;
			} else {
				return false;
			}
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && key.equals(key1)) {
				return Optional.of(key1);
			} else if (mask == pos2 && key.equals(key2)) {
				return Optional.of(key2);
			} else {
				return Optional.empty();
			}
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(key1);
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(key2);
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactSetNode<K>> nodeIterator() {
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
		SupplierIterator<K, K> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(new Object[] { key1, key1, key2, key2 });
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
			return 2;
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		AbstractSetNode<K> getNode(int index) {
			throw new IllegalStateException("Index out of range.");
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
			Set2To0Node<?> that = (Set2To0Node<?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}

			if (pos2 != that.pos2) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s, @%d: %s]", pos1, key1, pos2, key2);
		}

	}

	private static final class Set2To1Node<K> extends CompactSetNode<K> {

		private final byte pos1;
		private final K key1;

		private final byte pos2;
		private final K key2;

		private final byte npos1;
		private final CompactSetNode<K> node1;

		Set2To1Node(final AtomicReference<Thread> mutator, final byte pos1, final K key1,
						final byte pos2, final K key2, final byte npos1, final CompactSetNode<K> node1) {

			this.pos1 = pos1;
			this.key1 = key1;

			this.pos2 = pos2;
			this.key2 = key2;

			this.npos1 = npos1;
			this.node1 = node1;

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos2, key2, mask, node, npos1,
										node1));
					} else {
						result = Result.modified(valNodeOf(mutator, pos2, key2, npos1, node1, mask,
										node));
					}
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key2, key2.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos1, key1, mask, node, npos1,
										node1));
					} else {
						result = Result.modified(valNodeOf(mutator, pos1, key1, npos1, node1, mask,
										node));
					}
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, mask,
									nestedResult.getNode());
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos2, key2, mask, node, npos1,
										node1));
					} else {
						result = Result.modified(valNodeOf(mutator, pos2, key2, npos1, node1, mask,
										node));
					}
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key2, key2.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos1, key1, mask, node, npos1,
										node1));
					} else {
						result = Result.modified(valNodeOf(mutator, pos1, key1, npos1, node1, mask,
										node));
					}
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, mask,
									nestedResult.getNode());
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, pos2, key2, npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					// remove key2, val2
					result = Result.modified(valNodeOf(mutator, pos1, key1, npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, mask,
										updatedNode));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, pos2, key2, npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					// remove key2, val2
					result = Result.modified(valNodeOf(mutator, pos1, key1, npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, mask,
										updatedNode));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		private CompactSetNode<K> inlineValue(AtomicReference<Thread> mutator, byte mask, K key) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, pos1, key1, pos2, key2, npos1, node1);
			} else if (mask < pos2) {
				return valNodeOf(mutator, pos1, key1, mask, key, pos2, key2, npos1, node1);
			} else {
				return valNodeOf(mutator, pos1, key1, pos2, key2, mask, key, npos1, node1);
			}
		}

		private CompactSetNode<K> removeNode1AndInlineValue(AtomicReference<Thread> mutator, byte mask,
						K key) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, pos1, key1, pos2, key2);
			} else if (mask < pos2) {
				return valNodeOf(mutator, pos1, key1, mask, key, pos2, key2);
			} else {
				return valNodeOf(mutator, pos1, key1, pos2, key2, mask, key);
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				return key.equals(key1);
			} else if (mask == pos2) {
				return key.equals(key2);
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return false;
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				return cmp.compare(key, key1) == 0;
			} else if (mask == pos2) {
				return cmp.compare(key, key2) == 0;
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else {
				return false;
			}
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && key.equals(key1)) {
				return Optional.of(key1);
			} else if (mask == pos2 && key.equals(key2)) {
				return Optional.of(key2);
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return Optional.empty();
			}
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
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactSetNode<K>> nodeIterator() {
			return ArrayIterator.<CompactSetNode<K>> of(new CompactSetNode[] { node1 });
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
		SupplierIterator<K, K> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(new Object[] { key1, key1, key2, key2 });
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
			return 2;
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		AbstractSetNode<K> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			default:
				throw new IllegalStateException("Index out of range.");
			}
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
			Set2To1Node<?> that = (Set2To1Node<?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}

			if (pos2 != that.pos2) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}

			if (npos1 != that.npos1) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s, @%d: %s, @%d: %s]", pos1, key1, pos2, key2, npos1, node1);
		}

	}

	private static final class Set2To2Node<K> extends CompactSetNode<K> {

		private final byte pos1;
		private final K key1;

		private final byte pos2;
		private final K key2;

		private final byte npos1;
		private final CompactSetNode<K> node1;

		private final byte npos2;
		private final CompactSetNode<K> node2;

		Set2To2Node(final AtomicReference<Thread> mutator, final byte pos1, final K key1,
						final byte pos2, final K key2, final byte npos1, final CompactSetNode<K> node1,
						final byte npos2, final CompactSetNode<K> node2) {

			this.pos1 = pos1;
			this.key1 = key1;

			this.pos2 = pos2;
			this.key2 = key2;

			this.npos1 = npos1;
			this.node1 = node1;

			this.npos2 = npos2;
			this.node2 = node2;

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos2, key2, mask, node, npos1,
										node1, npos2, node2));
					} else if (mask < npos2) {
						result = Result.modified(valNodeOf(mutator, pos2, key2, npos1, node1, mask,
										node, npos2, node2));
					} else {
						result = Result.modified(valNodeOf(mutator, pos2, key2, npos1, node1, npos2,
										node2, mask, node));
					}
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key2, key2.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos1, key1, mask, node, npos1,
										node1, npos2, node2));
					} else if (mask < npos2) {
						result = Result.modified(valNodeOf(mutator, pos1, key1, npos1, node1, mask,
										node, npos2, node2));
					} else {
						result = Result.modified(valNodeOf(mutator, pos1, key1, npos1, node1, npos2,
										node2, mask, node));
					}
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, mask,
									nestedResult.getNode(), npos2, node2);
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, npos1,
									node1, mask, nestedResult.getNode());
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos2, key2, mask, node, npos1,
										node1, npos2, node2));
					} else if (mask < npos2) {
						result = Result.modified(valNodeOf(mutator, pos2, key2, npos1, node1, mask,
										node, npos2, node2));
					} else {
						result = Result.modified(valNodeOf(mutator, pos2, key2, npos1, node1, npos2,
										node2, mask, node));
					}
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key2, key2.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos1, key1, mask, node, npos1,
										node1, npos2, node2));
					} else if (mask < npos2) {
						result = Result.modified(valNodeOf(mutator, pos1, key1, npos1, node1, mask,
										node, npos2, node2));
					} else {
						result = Result.modified(valNodeOf(mutator, pos1, key1, npos1, node1, npos2,
										node2, mask, node));
					}
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, mask,
									nestedResult.getNode(), npos2, node2);
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, npos1,
									node1, mask, nestedResult.getNode());
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, pos2, key2, npos1, node1, npos2, node2));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					// remove key2, val2
					result = Result.modified(valNodeOf(mutator, pos1, key1, npos1, node1, npos2, node2));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, mask,
										updatedNode, npos2, node2));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node2
						result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, npos1,
										node1, mask, updatedNode));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, pos2, key2, npos1, node1, npos2, node2));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					// remove key2, val2
					result = Result.modified(valNodeOf(mutator, pos1, key1, npos1, node1, npos2, node2));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, mask,
										updatedNode, npos2, node2));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node2
						result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, npos1,
										node1, mask, updatedNode));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		private CompactSetNode<K> inlineValue(AtomicReference<Thread> mutator, byte mask, K key) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, pos1, key1, pos2, key2, npos1, node1, npos2, node2);
			} else if (mask < pos2) {
				return valNodeOf(mutator, pos1, key1, mask, key, pos2, key2, npos1, node1, npos2, node2);
			} else {
				return valNodeOf(mutator, pos1, key1, pos2, key2, mask, key, npos1, node1, npos2, node2);
			}
		}

		private CompactSetNode<K> removeNode1AndInlineValue(AtomicReference<Thread> mutator, byte mask,
						K key) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, pos1, key1, pos2, key2, npos2, node2);
			} else if (mask < pos2) {
				return valNodeOf(mutator, pos1, key1, mask, key, pos2, key2, npos2, node2);
			} else {
				return valNodeOf(mutator, pos1, key1, pos2, key2, mask, key, npos2, node2);
			}
		}

		private CompactSetNode<K> removeNode2AndInlineValue(AtomicReference<Thread> mutator, byte mask,
						K key) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, pos1, key1, pos2, key2, npos1, node1);
			} else if (mask < pos2) {
				return valNodeOf(mutator, pos1, key1, mask, key, pos2, key2, npos1, node1);
			} else {
				return valNodeOf(mutator, pos1, key1, pos2, key2, mask, key, npos1, node1);
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				return key.equals(key1);
			} else if (mask == pos2) {
				return key.equals(key2);
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return false;
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				return cmp.compare(key, key1) == 0;
			} else if (mask == pos2) {
				return cmp.compare(key, key2) == 0;
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else {
				return false;
			}
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && key.equals(key1)) {
				return Optional.of(key1);
			} else if (mask == pos2 && key.equals(key2)) {
				return Optional.of(key2);
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return Optional.empty();
			}
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
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactSetNode<K>> nodeIterator() {
			return ArrayIterator.<CompactSetNode<K>> of(new CompactSetNode[] { node1, node2 });
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
		SupplierIterator<K, K> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(new Object[] { key1, key1, key2, key2 });
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
			return 2;
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		AbstractSetNode<K> getNode(int index) {
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
			Set2To2Node<?> that = (Set2To2Node<?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}

			if (pos2 != that.pos2) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}

			if (npos1 != that.npos1) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}
			if (npos2 != that.npos2) {
				return false;
			}
			if (!node2.equals(that.node2)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s, @%d: %s, @%d: %s, @%d: %s]", pos1, key1, pos2, key2, npos1,
							node1, npos2, node2);
		}

	}

	private static final class Set3To0Node<K> extends CompactSetNode<K> {

		private final byte pos1;
		private final K key1;

		private final byte pos2;
		private final K key2;

		private final byte pos3;
		private final K key3;

		Set3To0Node(final AtomicReference<Thread> mutator, final byte pos1, final K key1,
						final byte pos2, final K key2, final byte pos3, final K key3) {

			this.pos1 = pos1;
			this.key1 = key1;

			this.pos2 = pos2;
			this.key2 = key2;

			this.pos3 = pos3;
			this.key3 = key3;

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos2, key2, pos3, key3, mask, node));
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key2, key2.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, pos3, key3, mask, node));
				}
			} else if (mask == pos3) {
				if (key.equals(key3)) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key3, key3.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, mask, node));
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos2, key2, pos3, key3, mask, node));
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key2, key2.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, pos3, key3, mask, node));
				}
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key3, key3.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, mask, node));
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, pos2, key2, pos3, key3));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					// remove key2, val2
					result = Result.modified(valNodeOf(mutator, pos1, key1, pos3, key3));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos3) {
				if (key.equals(key3)) {
					// remove key3, val3
					result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2));
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, pos2, key2, pos3, key3));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					// remove key2, val2
					result = Result.modified(valNodeOf(mutator, pos1, key1, pos3, key3));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					// remove key3, val3
					result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2));
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		private CompactSetNode<K> inlineValue(AtomicReference<Thread> mutator, byte mask, K key) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, pos1, key1, pos2, key2, pos3, key3);
			} else if (mask < pos2) {
				return valNodeOf(mutator, pos1, key1, mask, key, pos2, key2, pos3, key3);
			} else if (mask < pos3) {
				return valNodeOf(mutator, pos1, key1, pos2, key2, mask, key, pos3, key3);
			} else {
				return valNodeOf(mutator, pos1, key1, pos2, key2, pos3, key3, mask, key);
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				return key.equals(key1);
			} else if (mask == pos2) {
				return key.equals(key2);
			} else if (mask == pos3) {
				return key.equals(key3);
			} else {
				return false;
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				return cmp.compare(key, key1) == 0;
			} else if (mask == pos2) {
				return cmp.compare(key, key2) == 0;
			} else if (mask == pos3) {
				return cmp.compare(key, key3) == 0;
			} else {
				return false;
			}
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && key.equals(key1)) {
				return Optional.of(key1);
			} else if (mask == pos2 && key.equals(key2)) {
				return Optional.of(key2);
			} else if (mask == pos3 && key.equals(key3)) {
				return Optional.of(key3);
			} else {
				return Optional.empty();
			}
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
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactSetNode<K>> nodeIterator() {
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
		SupplierIterator<K, K> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(new Object[] { key1, key1, key2, key2, key3, key3 });
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
			return 3;
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		AbstractSetNode<K> getNode(int index) {
			throw new IllegalStateException("Index out of range.");
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
			Set3To0Node<?> that = (Set3To0Node<?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}

			if (pos2 != that.pos2) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}

			if (pos3 != that.pos3) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s, @%d: %s, @%d: %s]", pos1, key1, pos2, key2, pos3, key3);
		}

	}

	private static final class Set3To1Node<K> extends CompactSetNode<K> {

		private final byte pos1;
		private final K key1;

		private final byte pos2;
		private final K key2;

		private final byte pos3;
		private final K key3;

		private final byte npos1;
		private final CompactSetNode<K> node1;

		Set3To1Node(final AtomicReference<Thread> mutator, final byte pos1, final K key1,
						final byte pos2, final K key2, final byte pos3, final K key3, final byte npos1,
						final CompactSetNode<K> node1) {

			this.pos1 = pos1;
			this.key1 = key1;

			this.pos2 = pos2;
			this.key2 = key2;

			this.pos3 = pos3;
			this.key3 = key3;

			this.npos1 = npos1;
			this.node1 = node1;

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos2, key2, pos3, key3, mask, node,
										npos1, node1));
					} else {
						result = Result.modified(valNodeOf(mutator, pos2, key2, pos3, key3, npos1,
										node1, mask, node));
					}
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key2, key2.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos1, key1, pos3, key3, mask, node,
										npos1, node1));
					} else {
						result = Result.modified(valNodeOf(mutator, pos1, key1, pos3, key3, npos1,
										node1, mask, node));
					}
				}
			} else if (mask == pos3) {
				if (key.equals(key3)) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key3, key3.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, mask, node,
										npos1, node1));
					} else {
						result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, npos1,
										node1, mask, node));
					}
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, pos3,
									key3, mask, nestedResult.getNode());
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos2, key2, pos3, key3, mask, node,
										npos1, node1));
					} else {
						result = Result.modified(valNodeOf(mutator, pos2, key2, pos3, key3, npos1,
										node1, mask, node));
					}
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key2, key2.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos1, key1, pos3, key3, mask, node,
										npos1, node1));
					} else {
						result = Result.modified(valNodeOf(mutator, pos1, key1, pos3, key3, npos1,
										node1, mask, node));
					}
				}
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key3, key3.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, mask, node,
										npos1, node1));
					} else {
						result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, npos1,
										node1, mask, node));
					}
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> thisNew = valNodeOf(mutator, pos1, key1, pos2, key2, pos3,
									key3, mask, nestedResult.getNode());
					result = Result.modified(thisNew);
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, pos2, key2, pos3, key3, npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					// remove key2, val2
					result = Result.modified(valNodeOf(mutator, pos1, key1, pos3, key3, npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos3) {
				if (key.equals(key3)) {
					// remove key3, val3
					result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, pos3, key3,
										mask, updatedNode));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, pos2, key2, pos3, key3, npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					// remove key2, val2
					result = Result.modified(valNodeOf(mutator, pos1, key1, pos3, key3, npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					// remove key3, val3
					result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, Void, ? extends CompactSetNode<K>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactSetNode<K> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, pos3, key3,
										mask, updatedNode));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		private CompactSetNode<K> inlineValue(AtomicReference<Thread> mutator, byte mask, K key) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, pos1, key1, pos2, key2, pos3, key3, npos1, node1);
			} else if (mask < pos2) {
				return valNodeOf(mutator, pos1, key1, mask, key, pos2, key2, pos3, key3, npos1, node1);
			} else if (mask < pos3) {
				return valNodeOf(mutator, pos1, key1, pos2, key2, mask, key, pos3, key3, npos1, node1);
			} else {
				return valNodeOf(mutator, pos1, key1, pos2, key2, pos3, key3, mask, key, npos1, node1);
			}
		}

		private CompactSetNode<K> removeNode1AndInlineValue(AtomicReference<Thread> mutator, byte mask,
						K key) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, pos1, key1, pos2, key2, pos3, key3);
			} else if (mask < pos2) {
				return valNodeOf(mutator, pos1, key1, mask, key, pos2, key2, pos3, key3);
			} else if (mask < pos3) {
				return valNodeOf(mutator, pos1, key1, pos2, key2, mask, key, pos3, key3);
			} else {
				return valNodeOf(mutator, pos1, key1, pos2, key2, pos3, key3, mask, key);
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				return key.equals(key1);
			} else if (mask == pos2) {
				return key.equals(key2);
			} else if (mask == pos3) {
				return key.equals(key3);
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return false;
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				return cmp.compare(key, key1) == 0;
			} else if (mask == pos2) {
				return cmp.compare(key, key2) == 0;
			} else if (mask == pos3) {
				return cmp.compare(key, key3) == 0;
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else {
				return false;
			}
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && key.equals(key1)) {
				return Optional.of(key1);
			} else if (mask == pos2 && key.equals(key2)) {
				return Optional.of(key2);
			} else if (mask == pos3 && key.equals(key3)) {
				return Optional.of(key3);
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return Optional.empty();
			}
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
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactSetNode<K>> nodeIterator() {
			return ArrayIterator.<CompactSetNode<K>> of(new CompactSetNode[] { node1 });
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
		SupplierIterator<K, K> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(new Object[] { key1, key1, key2, key2, key3, key3 });
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
			return 3;
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		AbstractSetNode<K> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			default:
				throw new IllegalStateException("Index out of range.");
			}
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
			Set3To1Node<?> that = (Set3To1Node<?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}

			if (pos2 != that.pos2) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}

			if (pos3 != that.pos3) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}

			if (npos1 != that.npos1) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s, @%d: %s, @%d: %s, @%d: %s]", pos1, key1, pos2, key2, pos3,
							key3, npos1, node1);
		}

	}

	private static final class Set4To0Node<K> extends CompactSetNode<K> {

		private final byte pos1;
		private final K key1;

		private final byte pos2;
		private final K key2;

		private final byte pos3;
		private final K key3;

		private final byte pos4;
		private final K key4;

		Set4To0Node(final AtomicReference<Thread> mutator, final byte pos1, final K key1,
						final byte pos2, final K key2, final byte pos3, final K key3, final byte pos4,
						final K key4) {

			this.pos1 = pos1;
			this.key1 = key1;

			this.pos2 = pos2;
			this.key2 = key2;

			this.pos3 = pos3;
			this.key3 = key3;

			this.pos4 = pos4;
			this.key4 = key4;

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos2, key2, pos3, key3, pos4, key4,
									mask, node));
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key2, key2.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, pos3, key3, pos4, key4,
									mask, node));
				}
			} else if (mask == pos3) {
				if (key.equals(key3)) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key3, key3.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, pos4, key4,
									mask, node));
				}
			} else if (mask == pos4) {
				if (key.equals(key4)) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key4, key4.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, pos3, key3,
									mask, node));
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, Void val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key1, key1.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos2, key2, pos3, key3, pos4, key4,
									mask, node));
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key2, key2.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, pos3, key3, pos4, key4,
									mask, node));
				}
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key3, key3.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, pos4, key4,
									mask, node));
				}
			} else if (mask == pos4) {
				if (cmp.compare(key, key4) == 0) {
					result = Result.unchanged(this);
				} else {
					// merge into node
					final CompactSetNode<K> node = mergeNodes(key4, key4.hashCode(), key, keyHash,
									shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, pos3, key3,
									mask, node));
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key));
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, pos2, key2, pos3, key3, pos4, key4));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					// remove key2, val2
					result = Result.modified(valNodeOf(mutator, pos1, key1, pos3, key3, pos4, key4));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos3) {
				if (key.equals(key3)) {
					// remove key3, val3
					result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, pos4, key4));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos4) {
				if (key.equals(key4)) {
					// remove key4, val4
					result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, pos3, key3));
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		@Override
		Result<K, Void, ? extends CompactSetNode<K>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, Void, ? extends CompactSetNode<K>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, pos2, key2, pos3, key3, pos4, key4));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					// remove key2, val2
					result = Result.modified(valNodeOf(mutator, pos1, key1, pos3, key3, pos4, key4));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					// remove key3, val3
					result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, pos4, key4));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos4) {
				if (cmp.compare(key, key4) == 0) {
					// remove key4, val4
					result = Result.modified(valNodeOf(mutator, pos1, key1, pos2, key2, pos3, key3));
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		private CompactSetNode<K> inlineValue(AtomicReference<Thread> mutator, byte mask, K key) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, pos1, key1, pos2, key2, pos3, key3, pos4, key4);
			} else if (mask < pos2) {
				return valNodeOf(mutator, pos1, key1, mask, key, pos2, key2, pos3, key3, pos4, key4);
			} else if (mask < pos3) {
				return valNodeOf(mutator, pos1, key1, pos2, key2, mask, key, pos3, key3, pos4, key4);
			} else if (mask < pos4) {
				return valNodeOf(mutator, pos1, key1, pos2, key2, pos3, key3, mask, key, pos4, key4);
			} else {
				return valNodeOf(mutator, pos1, key1, pos2, key2, pos3, key3, pos4, key4, mask, key);
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				return key.equals(key1);
			} else if (mask == pos2) {
				return key.equals(key2);
			} else if (mask == pos3) {
				return key.equals(key3);
			} else if (mask == pos4) {
				return key.equals(key4);
			} else {
				return false;
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				return cmp.compare(key, key1) == 0;
			} else if (mask == pos2) {
				return cmp.compare(key, key2) == 0;
			} else if (mask == pos3) {
				return cmp.compare(key, key3) == 0;
			} else if (mask == pos4) {
				return cmp.compare(key, key4) == 0;
			} else {
				return false;
			}
		}

		@Override
		Optional<K> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && key.equals(key1)) {
				return Optional.of(key1);
			} else if (mask == pos2 && key.equals(key2)) {
				return Optional.of(key2);
			} else if (mask == pos3 && key.equals(key3)) {
				return Optional.of(key3);
			} else if (mask == pos4 && key.equals(key4)) {
				return Optional.of(key4);
			} else {
				return Optional.empty();
			}
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
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactSetNode<K>> nodeIterator() {
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
		SupplierIterator<K, K> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(new Object[] { key1, key1, key2, key2, key3, key3, key4,
							key4 });
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
			return 4;
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		AbstractSetNode<K> getNode(int index) {
			throw new IllegalStateException("Index out of range.");
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
			Set4To0Node<?> that = (Set4To0Node<?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}

			if (pos2 != that.pos2) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}

			if (pos3 != that.pos3) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}

			if (pos4 != that.pos4) {
				return false;
			}
			if (!key4.equals(that.key4)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s, @%d: %s, @%d: %s, @%d: %s]", pos1, key1, pos2, key2, pos3,
							key3, pos4, key4);
		}

	}

}
