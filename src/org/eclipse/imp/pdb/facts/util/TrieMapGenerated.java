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

import static org.eclipse.imp.pdb.facts.util.AbstractSpecialisedImmutableJdkMap.mapOf;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndInsert;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndInsertPair;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndMoveToBackPair;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndMoveToFrontPair;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndRemove;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndRemovePair;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndSet;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

//import objectexplorer.ObjectGraphMeasurer.Footprint;
//
//import com.google.common.base.Predicate;
//import com.google.common.base.Predicates;

@SuppressWarnings("rawtypes")
public class TrieMapGenerated<K, V> extends AbstractImmutableMap<K, V> {

	@SuppressWarnings("unchecked")
	private static final TrieMapGenerated EMPTY_INPLACE_INDEX_MAP = new TrieMapGenerated(
					CompactNode.EMPTY_INPLACE_INDEX_NODE, 0, 0);

	final private AbstractNode<K, V> rootNode;
	private final int hashCode;
	private final int cachedSize;

	TrieMapGenerated(AbstractNode<K, V> rootNode, int hashCode, int cachedSize) {
		this.rootNode = rootNode;
		this.hashCode = hashCode;
		this.cachedSize = cachedSize;
		assert invariant();
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> ImmutableMap<K, V> of() {
		return TrieMapGenerated.EMPTY_INPLACE_INDEX_MAP;
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> TransientMap<K, V> transientOf() {
		return TrieMapGenerated.EMPTY_INPLACE_INDEX_MAP.asTransient();
	}

	@SuppressWarnings("unchecked")
	protected static final <K> Comparator<K> equalityComparator() {
		return EqualityUtils.getDefaultEqualityComparator();
	}

	private boolean invariant() {
		int _hash = 0;
		int _count = 0;

		for (SupplierIterator<K, V> it = keyIterator(); it.hasNext();) {
			final K key = it.next();
			final V val = it.get();

			_hash += key.hashCode() ^ val.hashCode();
			_count += 1;
		}

		return this.hashCode == _hash && this.cachedSize == _count;
	}

	@Override
	public TrieMapGenerated<K, V> __put(K k, V v) {
		return __putEquivalent(k, v, equalityComparator());
	}

	@Override
	public TrieMapGenerated<K, V> __putEquivalent(K key, V val, Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final AbstractNode.Result<K, V> result = rootNode.updated(null, key, keyHash, val, 0, cmp);

		if (result.isModified()) {
			if (result.hasReplacedValue()) {
				final int valHashOld = result.getReplacedValue().hashCode();
				final int valHashNew = val.hashCode();

				return new TrieMapGenerated<K, V>(result.getNode(), hashCode
								+ (keyHash ^ valHashNew) - (keyHash ^ valHashOld), cachedSize);
			}

			final int valHash = val.hashCode();
			return new TrieMapGenerated<K, V>(result.getNode(), hashCode + (keyHash ^ valHash),
							cachedSize + 1);
		}

		return this;
	}

	@Override
	public ImmutableMap<K, V> __putAll(Map<? extends K, ? extends V> map) {
		return __putAllEquivalent(map, equalityComparator());
	}

	@Override
	public ImmutableMap<K, V> __putAllEquivalent(Map<? extends K, ? extends V> map,
					Comparator<Object> cmp) {
		TransientMap<K, V> tmp = asTransient();
		tmp.__putAllEquivalent(map, cmp);
		return tmp.freeze();
	}

	@Override
	public TrieMapGenerated<K, V> __remove(K k) {
		return __removeEquivalent(k, equalityComparator());
	}

	@Override
	public TrieMapGenerated<K, V> __removeEquivalent(K key, Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final AbstractNode.Result<K, V> result = rootNode.removed(null, key, keyHash, 0, cmp);

		if (result.isModified()) {
			// TODO: carry deleted value in result
			// assert result.hasReplacedValue();
			// final int valHash = result.getReplacedValue().hashCode();

			final int valHash = rootNode.findByKey(key, keyHash, 0, cmp).get().getValue()
							.hashCode();

			return new TrieMapGenerated<K, V>(result.getNode(), hashCode - (keyHash ^ valHash),
							cachedSize - 1);
		}

		return this;
	}

	@Override
	public boolean containsKey(Object o) {
		return rootNode.containsKey(o, o.hashCode(), 0, equalityComparator());
	}

	@Override
	public boolean containsKeyEquivalent(Object o, Comparator<Object> cmp) {
		return rootNode.containsKey(o, o.hashCode(), 0, cmp);
	}

	@Override
	public boolean containsValue(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsValueEquivalent(Object o, Comparator<Object> cmp) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V get(Object key) {
		return getEquivalent(key, equalityComparator());
	}

	@Override
	public V getEquivalent(Object key, Comparator<Object> cmp) {
		final AbstractNode.Optional<Map.Entry<K, V>> result = rootNode.findByKey(key,
						key.hashCode(), 0, cmp);

		if (result.isPresent()) {
			return result.get().getValue();
		} else {
			return null;
		}
	}

	@Override
	public int size() {
		return cachedSize;
	}

	@Override
	public SupplierIterator<K, V> keyIterator() {
		return new TrieMapIterator<>(rootNode);
	}

	@Override
	public Iterator<V> valueIterator() {
		return new TrieMapValueIterator<>(new TrieMapIterator<>(rootNode));
	}

	@Override
	public Iterator<Map.Entry<K, V>> entryIterator() {
		return new TrieMapEntryIterator<>(new TrieMapIterator<>(rootNode));
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		Set<java.util.Map.Entry<K, V>> entrySet = null;

		if (entrySet == null) {
			entrySet = new AbstractSet<java.util.Map.Entry<K, V>>() {
				public Iterator<java.util.Map.Entry<K, V>> iterator() {
					return new Iterator<Entry<K, V>>() {
						private final Iterator<Entry<K, V>> i = entryIterator();

						public boolean hasNext() {
							return i.hasNext();
						}

						public Entry<K, V> next() {
							return i.next();
						}

						public void remove() {
							i.remove();
						}
					};
				}

				public int size() {
					return TrieMapGenerated.this.size();
				}

				public boolean isEmpty() {
					return TrieMapGenerated.this.isEmpty();
				}

				public void clear() {
					TrieMapGenerated.this.clear();
				}

				public boolean contains(Object k) {
					return TrieMapGenerated.this.containsKey(k);
				}
			};
		}
		return entrySet;
	}

	/**
	 * Iterator that first iterates over inlined-values and then continues depth
	 * first recursively.
	 */
	private static class TrieMapIterator<K, V> implements SupplierIterator<K, V> {

		final Deque<Iterator<AbstractNode<K, V>>> nodeIteratorStack;
		SupplierIterator<K, V> valueIterator;

		TrieMapIterator(AbstractNode<K, V> rootNode) {
			if (rootNode.hasValues()) {
				valueIterator = rootNode.valueIterator();
			} else {
				valueIterator = EmptySupplierIterator.emptyIterator();
			}

			nodeIteratorStack = new ArrayDeque<>();
			if (rootNode.hasNodes()) {
				nodeIteratorStack.push(rootNode.nodeIterator());
			}
		}

		@Override
		public boolean hasNext() {
			while (true) {
				if (valueIterator.hasNext()) {
					return true;
				} else {
					if (nodeIteratorStack.isEmpty()) {
						return false;
					} else {
						if (nodeIteratorStack.peek().hasNext()) {
							AbstractNode<K, V> innerNode = nodeIteratorStack.peek().next();

							if (innerNode.hasValues())
								valueIterator = innerNode.valueIterator();

							if (innerNode.hasNodes()) {
								nodeIteratorStack.push(innerNode.nodeIterator());
							}
							continue;
						} else {
							nodeIteratorStack.pop();
							continue;
						}
					}
				}
			}
		}

		@Override
		public K next() {
			if (!hasNext())
				throw new NoSuchElementException();

			return valueIterator.next();
		}

		@Override
		public V get() {
			return valueIterator.get();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private static class EmptySupplierIterator<K, V> implements SupplierIterator<K, V> {

		private static final SupplierIterator EMPTY_ITERATOR = new EmptySupplierIterator();

		@SuppressWarnings("unchecked")
		static <K, V> SupplierIterator<K, V> emptyIterator() {
			return EMPTY_ITERATOR;
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public K next() {
			throw new NoSuchElementException();
		}

		@Override
		public V get() {
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private static class TrieMapEntryIterator<K, V> implements Iterator<Map.Entry<K, V>> {
		private final SupplierIterator<K, V> iterator;

		TrieMapEntryIterator(SupplierIterator<K, V> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@SuppressWarnings("unchecked")
		@Override
		public Map.Entry<K, V> next() {
			final K key = iterator.next();
			final V val = iterator.get();
			return (java.util.Map.Entry<K, V>) mapOf(key, val);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private static class TrieMapValueIterator<K, V> implements Iterator<V> {
		private final SupplierIterator<K, V> iterator;

		TrieMapValueIterator(SupplierIterator<K, V> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public V next() {
			iterator.next();
			return iterator.get();
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
	public TransientMap<K, V> asTransient() {
		return new TransientTrieMap<K, V>(this);
	}

	static final class TransientTrieMap<K, V> implements TransientMap<K, V> {
		final private AtomicReference<Thread> mutator;
		private AbstractNode<K, V> rootNode;
		private int hashCode;
		private int cachedSize;

		TransientTrieMap(TrieMapGenerated<K, V> trieMap) {
			this.mutator = new AtomicReference<Thread>(Thread.currentThread());
			this.rootNode = trieMap.rootNode;
			this.hashCode = trieMap.hashCode;
			this.cachedSize = trieMap.cachedSize;
			assert invariant();
		}

		// TODO: merge with TrieMap invariant (as function)
		private boolean invariant() {
			int _hash = 0;

			for (SupplierIterator<K, V> it = keyIterator(); it.hasNext();) {
				final K key = it.next();
				final V val = it.get();

				_hash += key.hashCode() ^ val.hashCode();
			}

			return this.hashCode == _hash;
		}

		@Override
		public boolean containsKey(Object o) {
			return rootNode.containsKey(o, o.hashCode(), 0, equalityComparator());
		}

		@Override
		public boolean containsKeyEquivalent(Object o, Comparator<Object> cmp) {
			return rootNode.containsKey(o, o.hashCode(), 0, cmp);
		}

		@Override
		public V get(Object key) {
			return getEquivalent(key, equalityComparator());
		}

		@Override
		public V getEquivalent(Object key, Comparator<Object> cmp) {
			AbstractNode.Optional<Map.Entry<K, V>> result = rootNode.findByKey(key, key.hashCode(),
							0, cmp);

			if (result.isPresent()) {
				return result.get().getValue();
			} else {
				return null;
			}
		}

		@Override
		public V __put(K k, V v) {
			return __putEquivalent(k, v, equalityComparator());
		}

		@Override
		public V __putEquivalent(K key, V val, Comparator<Object> cmp) {
			if (mutator.get() == null)
				throw new IllegalStateException("Transient already frozen.");

			final int keyHash = key.hashCode();
			final AbstractNode.Result<K, V> result = rootNode.updated(mutator, key, keyHash, val,
							0, cmp);

			if (result.isModified()) {
				rootNode = result.getNode();

				if (result.hasReplacedValue()) {
					final V old = result.getReplacedValue();

					final int valHashOld = old.hashCode();
					final int valHashNew = val.hashCode();

					hashCode += keyHash ^ valHashNew;
					hashCode -= keyHash ^ valHashOld;
					// cachedSize remains same

					assert invariant();
					return old;
				} else {
					final int valHashNew = val.hashCode();

					hashCode += keyHash ^ valHashNew;
					cachedSize += 1;

					assert invariant();
					return null;
				}
			}

			assert invariant();
			return null;
		}

		@Override
		public boolean __remove(K k) {
			return __removeEquivalent(k, equalityComparator());
		}

		@Override
		public boolean __removeEquivalent(K key, Comparator<Object> cmp) {
			if (mutator.get() == null)
				throw new IllegalStateException("Transient already frozen.");

			final int keyHash = key.hashCode();
			final AbstractNode.Result<K, V> result = rootNode
							.removed(mutator, key, keyHash, 0, cmp);

			if (result.isModified()) {
				// TODO: carry deleted value in result
				// assert result.hasReplacedValue();
				// final int valHash = result.getReplacedValue().hashCode();

				final int valHash = rootNode.findByKey(key, keyHash, 0, cmp).get().getValue()
								.hashCode();

				rootNode = result.getNode();
				hashCode -= keyHash ^ valHash;
				cachedSize -= 1;

				assert invariant();
				return true;
			}

			assert invariant();
			return false;
		}

		@Override
		public boolean __putAll(Map<? extends K, ? extends V> map) {
			return __putAllEquivalent(map, equalityComparator());
		}

		@Override
		public boolean __putAllEquivalent(Map<? extends K, ? extends V> map, Comparator<Object> cmp) {
			boolean modified = false;

			for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
				final boolean isPresent = containsKeyEquivalent(entry.getKey(), cmp);
				final V replaced = __putEquivalent(entry.getKey(), entry.getValue(), cmp);

				if (!isPresent || replaced != null) {
					modified = true;
				}
			}

			return modified;
		}

		// TODO: test; declare in transient interface
		// @Override
		public SupplierIterator<K, V> keyIterator() {
			return new TransientTrieMapIterator<K, V>(this);
		}

		/**
		 * Iterator that first iterates over inlined-values and then continues
		 * depth first recursively.
		 */
		// TODO: test
		private static class TransientTrieMapIterator<K, V> extends TrieMapIterator<K, V> {

			final TransientTrieMap<K, V> transientTrieMap;
			K lastKey;

			TransientTrieMapIterator(TransientTrieMap<K, V> transientTrieMap) {
				super(transientTrieMap.rootNode);
				this.transientTrieMap = transientTrieMap;
			}

			@Override
			public K next() {
				lastKey = super.next();
				return lastKey;
			}

			@Override
			public void remove() {
				transientTrieMap.__remove(lastKey);
			}
		}

		@Override
		public boolean equals(Object o) {
			return rootNode.equals(o);
		}

		@Override
		public int hashCode() {
			return rootNode.hashCode();
		}

		@Override
		public String toString() {
			return rootNode.toString();
		}

		@Override
		public ImmutableMap<K, V> freeze() {
			if (mutator.get() == null)
				throw new IllegalStateException("Transient already frozen.");

			mutator.set(null);
			return new TrieMapGenerated<K, V>(rootNode, hashCode, cachedSize);
		}
	}

	private static abstract class AbstractNode<K, V> {

		protected static final int BIT_PARTITION_SIZE = 5;
		protected static final int BIT_PARTITION_MASK = 0x1f;

		abstract boolean containsKey(Object key, int keyHash, int shift,
						Comparator<Object> comparator);

		abstract Optional<Map.Entry<K, V>> findByKey(Object key, int hash, int shift,
						Comparator<Object> cmp);

		abstract Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val,
						int shift, Comparator<Object> cmp);

		abstract Result<K, V> removed(AtomicReference<Thread> mutator, K key, int hash, int shift,
						Comparator<Object> cmp);

		static boolean isAllowedToEdit(AtomicReference<Thread> x, AtomicReference<Thread> y) {
			return x != null && y != null && (x == y || x.get() == y.get());
		}

		@SuppressWarnings("unchecked")
		static <K, V> Map.Entry<K, V> entryOf(final K key, final V val) {
			return (java.util.Map.Entry<K, V>) mapOf(key, val);
		}

		abstract boolean hasNodes();

		abstract Iterator<AbstractNode<K, V>> nodeIterator();

		abstract int nodeArity();

		abstract boolean hasValues();

		abstract SupplierIterator<K, V> valueIterator();

		abstract int valueArity();

		// abstract K getValue(int index);
		//
		// abstract public AbstractNode<K, V> getNode(int index);

		/**
		 * The arity of this trie node (i.e. number of values and nodes stored
		 * on this level).
		 * 
		 * @return sum of nodes and values stored within
		 */
		int arity() {
			return valueArity() + nodeArity();
		}

		/**
		 * The total number of elements contained by this (sub)tree.
		 * 
		 * @return element count
		 */
		abstract int size();

		static class Result<T1, T2> {
			private final Object result;
			private final T2 replacedValue;
			private final boolean isModified;

			// update: inserted/removed single element, element count changed
			public static <T1, T2> Result<T1, T2> modified(AbstractNode<T1, T2> node) {
				return new Result<>(node, null, true);
			}

			// update: replaced single mapping, but element count unchanged
			public static <T1, T2> Result<T1, T2> updated(AbstractNode<T1, T2> node,
							T2 replacedValue) {
				return new Result<>(node, replacedValue, true);
			}

			// update: neither element, nor element count changed
			public static <T1, T2> Result<T1, T2> unchanged(AbstractNode<T1, T2> node) {
				return new Result<>(node, null, false);
			}

			private Result(AbstractNode<T1, T2> node, T2 replacedValue, boolean isMutated) {
				this.result = node;
				this.replacedValue = replacedValue;
				this.isModified = isMutated;
			}

			@SuppressWarnings("unchecked")
			public AbstractNode<T1, T2> getNode() {
				return (AbstractNode<T1, T2>) result;
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
			private final static Optional EMPTY = new Optional() {
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
				return (Optional<T>) EMPTY;
			}

			static <T> Optional<T> of(T value) {
				return new Value<T>(value);
			}

			abstract boolean isPresent();

			abstract T get();

			private static class Value<T> extends Optional<T> {
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
	}

	private static abstract class CompactNode<K, V> extends AbstractNode<K, V> {
		static final byte SIZE_EMPTY = 0b00;
		static final byte SIZE_ONE = 0b01;
		static final byte SIZE_MORE_THAN_ONE = 0b10;

		/**
		 * Returns the first key stored within this node.
		 * 
		 * @return first key
		 */
		abstract K headKey();

		/**
		 * Returns the first value stored within this node.
		 * 
		 * @return first value
		 */
		abstract V headVal();

		boolean nodeInvariant() {
			// boolean inv1 = (size() - valueArity() >= 2 * (arity() -
			// valueArity()));
			boolean inv2 = (this.arity() == 0) ? size() == SIZE_EMPTY : true;
			boolean inv3 = (this.arity() == 1 && valueArity() == 1) ? size() == SIZE_ONE : true;
			boolean inv4 = (this.arity() >= 2) ? size() == SIZE_MORE_THAN_ONE : true;

			boolean inv5 = (this.nodeArity() >= 0) && (this.valueArity() >= 0)
							&& ((this.valueArity() + this.nodeArity()) == this.arity());

			return inv2 && inv3 && inv4 && inv5;
		}

		// static final CompactNode EMPTY_INPLACE_INDEX_NODE = new
		// InplaceIndexNode(0, 0,
		// new CompactNode[0], SIZE_EMPTY);

		static final CompactNode EMPTY_INPLACE_INDEX_NODE = new Value0Index0Node();

		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos,
						AbstractNode<K, V> node) {
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
		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator) {
			return (AbstractNode<K, V>) EMPTY_INPLACE_INDEX_NODE;
		}

		// manually added
		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte npos1, AbstractNode<K, V> node1, byte npos2,
						AbstractNode<K, V> node2, byte npos3, AbstractNode<K, V> node3, byte npos4,
						AbstractNode<K, V> node4) {
			final int bitmap = (1 << pos1) | (1 << npos1) | (1 << npos2) | (1 << npos3)
							| (1 << npos4);
			final int valmap = (1 << pos1);

			return new InplaceIndexNodeWith1Value<>(mutator, bitmap, valmap, new Object[] { key1,
							val1, node1, node2, node3, node4 });
		}

		// manually added
		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte pos2, K key2, V val2, byte pos3, K key3, V val3,
						byte npos1, AbstractNode<K, V> node1, byte npos2, AbstractNode<K, V> node2) {
			final int bitmap = (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << npos1)
							| (1 << npos2);
			final int valmap = (1 << pos1) | (1 << pos2) | (1 << pos3);

			return new InplaceIndexNodeWith3Values<>(mutator, bitmap, valmap, new Object[] { key1,
							val1, key2, val2, key3, val3, node1, node2 });
		}

		// manually added
		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte pos2, K key2, V val2, byte npos1,
						AbstractNode<K, V> node1, byte npos2, AbstractNode<K, V> node2, byte npos3,
						AbstractNode<K, V> node3) {
			final int bitmap = (1 << pos1) | (1 << pos2) | (1 << npos1) | (1 << npos2)
							| (1 << npos3);
			final int valmap = (1 << pos1) | (1 << pos2);

			return new InplaceIndexNodeWith2Values<>(mutator, bitmap, valmap, new Object[] { key1,
							val1, key2, val2, node1, node2, node3 });
		}

		// manually added
		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte pos2, K key2, V val2, byte pos3, K key3, V val3,
						byte pos4, K key4, V val4, byte npos1, AbstractNode<K, V> node1) {
			final int bitmap = (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << pos4) | (1 << npos1);
			final int valmap = (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << pos4);

			return new InplaceIndexNodeWith4Values<>(mutator, bitmap, valmap, new Object[] { key1,
							val1, key2, val2, key3, val3, key4, val4, node1 });
		}

		// manually added
		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte pos2, K key2, V val2, byte pos3, K key3, V val3,
						byte pos4, K key4, V val4, byte pos5, K key5, V val5) {
			final int bitmap = (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << pos4) | (1 << pos5);
			final int valmap = bitmap;

			// TODO: separate between pure value and pure node nodes
			return new InplaceIndexNodeWith5Values<>(mutator, bitmap, valmap, new Object[] { key1,
							val1, key2, val2, key3, val3, key4, val4, key5, val5 });
		}

		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte npos1,
						AbstractNode<K, V> node1, byte npos2, AbstractNode<K, V> node2) {
			return new Value0Index2Node<>(mutator, npos1, node1, npos2, node2);
		}

		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte npos1,
						AbstractNode<K, V> node1, byte npos2, AbstractNode<K, V> node2, byte npos3,
						AbstractNode<K, V> node3) {
			return new Value0Index3Node<>(mutator, npos1, node1, npos2, node2, npos3, node3);
		}

		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte npos1,
						AbstractNode<K, V> node1, byte npos2, AbstractNode<K, V> node2, byte npos3,
						AbstractNode<K, V> node3, byte npos4, AbstractNode<K, V> node4) {
			return new Value0Index4Node<>(mutator, npos1, node1, npos2, node2, npos3, node3, npos4,
							node4);
		}

		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1) {
			return new Value1Index0Node<>(mutator, pos1, key1, val1);
		}

		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte npos1, AbstractNode<K, V> node1) {
			return new Value1Index1Node<>(mutator, pos1, key1, val1, npos1, node1);
		}

		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte npos1, AbstractNode<K, V> node1, byte npos2,
						AbstractNode<K, V> node2) {
			return new Value1Index2Node<>(mutator, pos1, key1, val1, npos1, node1, npos2, node2);
		}

		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte npos1, AbstractNode<K, V> node1, byte npos2,
						AbstractNode<K, V> node2, byte npos3, AbstractNode<K, V> node3) {
			return new Value1Index3Node<>(mutator, pos1, key1, val1, npos1, node1, npos2, node2,
							npos3, node3);
		}

		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte pos2, K key2, V val2) {
			return new Value2Index0Node<>(mutator, pos1, key1, val1, pos2, key2, val2);
		}

		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte pos2, K key2, V val2, byte npos1,
						AbstractNode<K, V> node1) {
			return new Value2Index1Node<>(mutator, pos1, key1, val1, pos2, key2, val2, npos1, node1);
		}

		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte pos2, K key2, V val2, byte npos1,
						AbstractNode<K, V> node1, byte npos2, AbstractNode<K, V> node2) {
			return new Value2Index2Node<>(mutator, pos1, key1, val1, pos2, key2, val2, npos1,
							node1, npos2, node2);
		}

		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte pos2, K key2, V val2, byte pos3, K key3, V val3) {
			return new Value3Index0Node<>(mutator, pos1, key1, val1, pos2, key2, val2, pos3, key3,
							val3);
		}

		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte pos2, K key2, V val2, byte pos3, K key3, V val3,
						byte npos1, AbstractNode<K, V> node1) {
			return new Value3Index1Node<>(mutator, pos1, key1, val1, pos2, key2, val2, pos3, key3,
							val3, npos1, node1);
		}

		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos1,
						K key1, V val1, byte pos2, K key2, V val2, byte pos3, K key3, V val3,
						byte pos4, K key4, V val4) {
			return new Value4Index0Node<>(mutator, pos1, key1, val1, pos2, key2, val2, pos3, key3,
							val3, pos4, key4, val4);
		}

		static <K, V> AbstractNode<K, V> valNodeOf(AtomicReference<Thread> mutator, int bitmap,
						int valmap, Object[] nodes, byte valueArity) {
			switch (valueArity) {
			case 0:
				return new InplaceIndexNodeWith0Values<>(mutator, bitmap, valmap, nodes);
			case 1:
				return new InplaceIndexNodeWith1Value<>(mutator, bitmap, valmap, nodes);
			case 2:
				return new InplaceIndexNodeWith2Values<>(mutator, bitmap, valmap, nodes);
			case 3:
				return new InplaceIndexNodeWith3Values<>(mutator, bitmap, valmap, nodes);
			case 4:
				return new InplaceIndexNodeWith4Values<>(mutator, bitmap, valmap, nodes);
			case 5:
				return new InplaceIndexNodeWith5Values<>(mutator, bitmap, valmap, nodes);
			case 6:
				return new InplaceIndexNodeWith6Values<>(mutator, bitmap, valmap, nodes);
			case 7:
				return new InplaceIndexNodeWith7Values<>(mutator, bitmap, valmap, nodes);
			case 8:
				return new InplaceIndexNodeWith8Values<>(mutator, bitmap, valmap, nodes);
			case 9:
				return new InplaceIndexNodeWith9Values<>(mutator, bitmap, valmap, nodes);
			case 10:
				return new InplaceIndexNodeWith10Values<>(mutator, bitmap, valmap, nodes);
			case 11:
				return new InplaceIndexNodeWith11Values<>(mutator, bitmap, valmap, nodes);
			case 12:
				return new InplaceIndexNodeWith12Values<>(mutator, bitmap, valmap, nodes);
			case 13:
				return new InplaceIndexNodeWith13Values<>(mutator, bitmap, valmap, nodes);
			case 14:
				return new InplaceIndexNodeWith14Values<>(mutator, bitmap, valmap, nodes);
			case 15:
				return new InplaceIndexNodeWith15Values<>(mutator, bitmap, valmap, nodes);
			case 16:
				return new InplaceIndexNodeWith16Values<>(mutator, bitmap, valmap, nodes);
			case 17:
				return new InplaceIndexNodeWith17Values<>(mutator, bitmap, valmap, nodes);
			case 18:
				return new InplaceIndexNodeWith18Values<>(mutator, bitmap, valmap, nodes);
			case 19:
				return new InplaceIndexNodeWith19Values<>(mutator, bitmap, valmap, nodes);
			case 20:
				return new InplaceIndexNodeWith20Values<>(mutator, bitmap, valmap, nodes);
			case 21:
				return new InplaceIndexNodeWith21Values<>(mutator, bitmap, valmap, nodes);
			case 22:
				return new InplaceIndexNodeWith22Values<>(mutator, bitmap, valmap, nodes);
			case 23:
				return new InplaceIndexNodeWith23Values<>(mutator, bitmap, valmap, nodes);
			case 24:
				return new InplaceIndexNodeWith24Values<>(mutator, bitmap, valmap, nodes);
			case 25:
				return new InplaceIndexNodeWith25Values<>(mutator, bitmap, valmap, nodes);
			case 26:
				return new InplaceIndexNodeWith26Values<>(mutator, bitmap, valmap, nodes);
			case 27:
				return new InplaceIndexNodeWith27Values<>(mutator, bitmap, valmap, nodes);
			case 28:
				return new InplaceIndexNodeWith28Values<>(mutator, bitmap, valmap, nodes);
			case 29:
				return new InplaceIndexNodeWith29Values<>(mutator, bitmap, valmap, nodes);
			case 30:
				return new InplaceIndexNodeWith30Values<>(mutator, bitmap, valmap, nodes);
			case 31:
				return new InplaceIndexNodeWith31Values<>(mutator, bitmap, valmap, nodes);
			case 32:
				return new InplaceIndexNodeWith32Values<>(mutator, bitmap, valmap, nodes);

			default:
				throw new IllegalStateException("Value arity out of range.");
			}
		}

		@SuppressWarnings("unchecked")
		static <K, V> CompactNode<K, V> mergeNodes(K key0, int keyHash0, V val0, K key1,
						int keyHash1, V val1, int shift) {
			assert key0.equals(key1) == false;

			if (keyHash0 == keyHash1)
				return new InplaceHashCollisionNode<>(keyHash0, (K[]) new Object[] { key0, key1 },
								(V[]) new Object[] { val0, val1 });

			final int mask0 = (keyHash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (keyHash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level
				final Object[] nodes = new Object[4];

				if (mask0 < mask1) {
					nodes[0] = key0;
					nodes[1] = val0;
					nodes[2] = key1;
					nodes[3] = val1;

					return (CompactNode) valNodeOf(null, (byte) mask0, key0, val0, (byte) mask1,
									key1, val1);
				} else {
					nodes[0] = key1;
					nodes[1] = val1;
					nodes[2] = key0;
					nodes[3] = val0;

					return (CompactNode) valNodeOf(null, (byte) mask1, key1, val1, (byte) mask0,
									key0, val0);
				}
			} else {
				// values fit on next level
				final CompactNode<K, V> node = mergeNodes(key0, keyHash0, val0, key1, keyHash1,
								val1, shift + BIT_PARTITION_SIZE);

				return (CompactNode) valNodeOf(null, (byte) mask0, node);
			}
		}

		@SuppressWarnings("unchecked")
		static <K, V> CompactNode<K, V> mergeNodes(CompactNode<K, V> node0, int keyHash0, K key1,
						int keyHash1, V val1, int shift) {
			final int mask0 = (keyHash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (keyHash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level
				final Object[] nodes = new Object[3];

				// store values before node
				nodes[0] = key1;
				nodes[1] = val1;
				nodes[2] = node0;

				return (CompactNode) valNodeOf(null, (byte) mask1, key1, val1, (byte) mask0, node0);
			} else {
				// values fit on next level
				final CompactNode<K, V> node = mergeNodes(node0, keyHash0, key1, keyHash1, val1,
								shift + BIT_PARTITION_SIZE);

				return (CompactNode) valNodeOf(null, (byte) mask0, node);
			}
		}
	}

	private abstract static class AbstractInplaceIndexNode<K, V> extends CompactNode<K, V> {
		// private AtomicReference<Thread> mutator;

		private int bitmap;
		private int valmap;
		private Object[] nodes;

		AbstractInplaceIndexNode(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			assert (2 * Integer.bitCount(valmap) + Integer.bitCount(bitmap ^ valmap) == nodes.length);

			// this.mutator = mutator;

			this.bitmap = bitmap;
			this.valmap = valmap;
			this.nodes = nodes;

			assert (valueArity() == Integer.bitCount(valmap));
			assert (valueArity() >= 2 || nodeArity() >= 1); // =
															// SIZE_MORE_THAN_ONE

			for (int i = 0; i < 2 * valueArity(); i++)
				assert ((nodes[i] instanceof AbstractNode) == false);

			for (int i = 2 * valueArity(); i < nodes.length; i++)
				assert ((nodes[i] instanceof AbstractNode) == true);

			// assert invariant
			assert nodeInvariant();
		}

		final int bitIndex(int bitpos) {
			return 2 * valueArity() + Integer.bitCount((bitmap ^ valmap) & (bitpos - 1));
		}

		final int valIndex(int bitpos) {
			return 2 * Integer.bitCount(valmap & (bitpos - 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		boolean containsKey(Object key, int hash, int shift, Comparator<Object> cmp) {
			final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0)
				return cmp.compare(nodes[valIndex(bitpos)], key) == 0;

			if ((bitmap & bitpos) != 0)
				return ((AbstractNode<K, V>) nodes[bitIndex(bitpos)]).containsKey(key, hash, shift
								+ BIT_PARTITION_SIZE, cmp);

			return false;
		}

		@SuppressWarnings("unchecked")
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (cmp.compare(nodes[valIndex], key) == 0) {
					final K _key = (K) nodes[valIndex];
					final V _val = (V) nodes[valIndex + 1];

					final Map.Entry<K, V> entry = (java.util.Map.Entry<K, V>) mapOf(_key, _val);
					return Optional.of(entry);
				}

				return Optional.empty();
			}

			if ((bitmap & bitpos) != 0) { // node (not value)
				final AbstractNode<K, V> subNode = ((AbstractNode<K, V>) nodes[bitIndex(bitpos)]);

				return subNode.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift,
						Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (cmp.compare(nodes[valIndex], key) == 0) {

					if (cmp.compare(nodes[valIndex + 1], val) == 0)
						return Result.unchanged(this);

					// update mapping (TODO: inline update if mutator is set)
					final Object[] nodesNew = copyAndSet(nodes, valIndex + 1, val);
					return Result.updated(CompactNode.<K, V> valNodeOf(mutator, bitmap, valmap,
									nodesNew, (byte) valueArity()), (V) nodes[valIndex + 1]);
				}

				final AbstractNode<K, V> nodeNew = mergeNodes((K) nodes[valIndex],
								nodes[valIndex].hashCode(), (V) nodes[valIndex + 1], key, keyHash,
								val, shift + BIT_PARTITION_SIZE);

				final int offset = 2 * (valueArity() - 1);
				final int index = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
								& (bitpos - 1));

				final Object[] editableNodes = copyAndMoveToBackPair(this.nodes, valIndex, offset
								+ index, nodeNew);

				final AbstractNode<K, V> thisNew = CompactNode.<K, V> valNodeOf(mutator, bitmap
								| bitpos, valmap & ~bitpos, editableNodes,
								(byte) (valueArity() - 1));

				return Result.modified(thisNew);
			}

			if ((bitmap & bitpos) != 0) { // node (not value)
				final int bitIndex = bitIndex(bitpos);
				final AbstractNode<K, V> subNode = (AbstractNode<K, V>) nodes[bitIndex];

				final Result<K, V> subNodeResult = subNode.updated(mutator, key, keyHash, val,
								shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew;

				// // modify current node (set replacement node)
				// if (isAllowedToEdit(this.mutator, mutator)) {
				// // no copying if already editable
				// this.nodes[bitIndex] = subNodeResult.getNode();
				// thisNew = this;
				// } else {
				final Object[] editableNodes = copyAndSet(this.nodes, bitIndex,
								subNodeResult.getNode());

				thisNew = CompactNode.<K, V> valNodeOf(mutator, bitmap, valmap, editableNodes,
								(byte) valueArity());
				// }

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			}

			// no value
			final Object[] editableNodes = copyAndInsertPair(this.nodes, valIndex(bitpos), key, val);

			final AbstractNode<K, V> thisNew = CompactNode.<K, V> valNodeOf(mutator, bitmap
							| bitpos, valmap | bitpos, editableNodes, (byte) (valueArity() + 1));

			return Result.modified(thisNew);
		}

		@SuppressWarnings("unchecked")
		@Override
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift,
						Comparator<Object> comparator) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (comparator.compare(nodes[valIndex], key) != 0)
					return Result.unchanged(this);

				if (this.arity() == 5) {
					switch (this.valueArity()) { // 0 <= valueArity <= 5
					case 1: {
						final int nmap = ((bitmap & ~bitpos) ^ (valmap & ~bitpos));
						final byte npos1 = recoverMask(nmap, (byte) 1);
						final byte npos2 = recoverMask(nmap, (byte) 2);
						final byte npos3 = recoverMask(nmap, (byte) 3);
						final byte npos4 = recoverMask(nmap, (byte) 4);
						final AbstractNode<K, V> node1 = (AbstractNode<K, V>) nodes[valueArity() + 0];
						final AbstractNode<K, V> node2 = (AbstractNode<K, V>) nodes[valueArity() + 1];
						final AbstractNode<K, V> node3 = (AbstractNode<K, V>) nodes[valueArity() + 2];
						final AbstractNode<K, V> node4 = (AbstractNode<K, V>) nodes[valueArity() + 3];

						return Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2,
										npos3, node3, npos4, node4));
					}
					case 2: {
						final int map = (valmap & ~bitpos);
						final byte pos1 = recoverMask(map, (byte) 1);
						final K key1;
						final V val1;

						final int nmap = ((bitmap & ~bitpos) ^ (valmap & ~bitpos));
						final byte npos1 = recoverMask(nmap, (byte) 1);
						final byte npos2 = recoverMask(nmap, (byte) 2);
						final byte npos3 = recoverMask(nmap, (byte) 3);
						final AbstractNode<K, V> node1 = (AbstractNode<K, V>) nodes[valueArity() + 0];
						final AbstractNode<K, V> node2 = (AbstractNode<K, V>) nodes[valueArity() + 1];
						final AbstractNode<K, V> node3 = (AbstractNode<K, V>) nodes[valueArity() + 2];

						if (mask < pos1) {
							key1 = (K) nodes[2];
							val1 = (V) nodes[3];
						} else {
							key1 = (K) nodes[0];
							val1 = (V) nodes[1];
						}

						return Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1,
										npos2, node2, npos3, node3));
					}
					case 3: {
						final int map = (valmap & ~bitpos);
						final byte pos1 = recoverMask(map, (byte) 1);
						final byte pos2 = recoverMask(map, (byte) 2);
						final K key1;
						final K key2;
						final V val1;
						final V val2;

						final int nmap = ((bitmap & ~bitpos) ^ (valmap & ~bitpos));
						final byte npos1 = recoverMask(nmap, (byte) 1);
						final byte npos2 = recoverMask(nmap, (byte) 2);
						final AbstractNode<K, V> node1 = (AbstractNode<K, V>) nodes[valueArity() + 0];
						final AbstractNode<K, V> node2 = (AbstractNode<K, V>) nodes[valueArity() + 1];

						if (mask < pos1) {
							key1 = (K) nodes[2];
							val1 = (V) nodes[3];
							key2 = (K) nodes[4];
							val2 = (V) nodes[5];
						} else if (mask < pos2) {
							key1 = (K) nodes[0];
							val1 = (V) nodes[1];
							key2 = (K) nodes[4];
							val2 = (V) nodes[5];
						} else {
							key1 = (K) nodes[0];
							val1 = (V) nodes[1];
							key2 = (K) nodes[2];
							val2 = (V) nodes[3];
						}

						return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2,
										val2, npos1, node1, npos2, node2));
					}
					case 4: {
						final int map = (valmap & ~bitpos);
						final byte pos1 = recoverMask(map, (byte) 1);
						final byte pos2 = recoverMask(map, (byte) 2);
						final byte pos3 = recoverMask(map, (byte) 3);
						final K key1;
						final K key2;
						final K key3;
						final V val1;
						final V val2;
						final V val3;

						final int nmap = ((bitmap & ~bitpos) ^ (valmap & ~bitpos));
						final byte npos1 = recoverMask(nmap, (byte) 1);
						final AbstractNode<K, V> node1 = (AbstractNode<K, V>) nodes[valueArity() + 0];

						if (mask < pos1) {
							key1 = (K) nodes[2];
							val1 = (V) nodes[3];
							key2 = (K) nodes[4];
							val2 = (V) nodes[5];
							key3 = (K) nodes[6];
							val3 = (V) nodes[7];
						} else if (mask < pos2) {
							key1 = (K) nodes[0];
							val1 = (V) nodes[1];
							key2 = (K) nodes[4];
							val2 = (V) nodes[5];
							key3 = (K) nodes[6];
							val3 = (V) nodes[7];
						} else if (mask < pos3) {
							key1 = (K) nodes[0];
							val1 = (V) nodes[1];
							key2 = (K) nodes[2];
							val2 = (V) nodes[3];
							key3 = (K) nodes[6];
							val3 = (V) nodes[7];
						} else {
							key1 = (K) nodes[0];
							val1 = (V) nodes[1];
							key2 = (K) nodes[2];
							val2 = (V) nodes[3];
							key3 = (K) nodes[4];
							val3 = (V) nodes[5];
						}

						return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2,
										val2, pos3, key3, val3, npos1, node1));
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
						final V val1;
						final V val2;
						final V val3;
						final V val4;

						if (mask < pos1) {
							key1 = (K) nodes[2];
							val1 = (V) nodes[3];
							key2 = (K) nodes[4];
							val2 = (V) nodes[5];
							key3 = (K) nodes[6];
							val3 = (V) nodes[7];
							key4 = (K) nodes[8];
							val4 = (V) nodes[9];
						} else if (mask < pos2) {
							key1 = (K) nodes[0];
							val1 = (V) nodes[1];
							key2 = (K) nodes[4];
							val2 = (V) nodes[5];
							key3 = (K) nodes[6];
							val3 = (V) nodes[7];
							key4 = (K) nodes[8];
							val4 = (V) nodes[9];
						} else if (mask < pos3) {
							key1 = (K) nodes[0];
							val1 = (V) nodes[1];
							key2 = (K) nodes[2];
							val2 = (V) nodes[3];
							key3 = (K) nodes[6];
							val3 = (V) nodes[7];
							key4 = (K) nodes[8];
							val4 = (V) nodes[9];
						} else if (mask < pos4) {
							key1 = (K) nodes[0];
							val1 = (V) nodes[1];
							key2 = (K) nodes[2];
							val2 = (V) nodes[3];
							key3 = (K) nodes[4];
							val3 = (V) nodes[5];
							key4 = (K) nodes[8];
							val4 = (V) nodes[9];
						} else {
							key1 = (K) nodes[0];
							val1 = (V) nodes[1];
							key2 = (K) nodes[2];
							val2 = (V) nodes[3];
							key3 = (K) nodes[4];
							val3 = (V) nodes[5];
							key4 = (K) nodes[6];
							val4 = (V) nodes[7];
						}

						return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2,
										val2, pos3, key3, val3, pos4, key4, val4));
					}
					}
				} else {
					final Object[] editableNodes = copyAndRemovePair(this.nodes, valIndex);

					final AbstractNode<K, V> thisNew = CompactNode.<K, V> valNodeOf(mutator,
									this.bitmap & ~bitpos, this.valmap & ~bitpos, editableNodes,
									(byte) (valueArity() - 1));

					return Result.modified(thisNew);
				}
			}

			if ((bitmap & bitpos) != 0) { // node (not value)
				final int bitIndex = bitIndex(bitpos);
				final AbstractNode<K, V> subNode = (AbstractNode<K, V>) nodes[bitIndex];
				final Result<K, V> subNodeResult = subNode.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, comparator);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				// TODO: rework result that it can take CompactNode instead of
				// AbstractNode
				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				switch (subNodeNew.size()) {
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
							final K key2 = (K) nodes[2];
							final K key3 = (K) nodes[4];
							final K key4 = (K) nodes[6];
							final V val1 = (V) nodes[1];
							final V val2 = (V) nodes[3];
							final V val3 = (V) nodes[5];
							final V val4 = (V) nodes[7];

							return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2,
											val2, pos3, key3, val3, pos4, key4, val4));
						}
						case 2: {
							final int map = valmap;
							final byte pos1 = recoverMask(map, (byte) 1);
							final byte pos2 = recoverMask(map, (byte) 2);
							final byte pos3 = recoverMask(map, (byte) 3);
							final K key1 = (K) nodes[0];
							final K key2 = (K) nodes[2];
							final K key3 = (K) nodes[4];
							final V val1 = (V) nodes[1];
							final V val2 = (V) nodes[3];
							final V val3 = (V) nodes[5];

							final int nmap = ((bitmap & ~bitpos) ^ valmap);
							final byte npos1 = recoverMask(nmap, (byte) 1);
							final AbstractNode<K, V> node1;

							if (mask < npos1) {
								node1 = (AbstractNode<K, V>) nodes[valueArity() + 1];
							} else {
								node1 = (AbstractNode<K, V>) nodes[valueArity() + 0];
							}

							return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2,
											val2, pos3, key3, val3, npos1, node1));
						}
						case 3: {
							final int map = valmap;
							final byte pos1 = recoverMask(map, (byte) 1);
							final byte pos2 = recoverMask(map, (byte) 2);
							final K key1 = (K) nodes[0];
							final K key2 = (K) nodes[2];
							final V val1 = (V) nodes[1];
							final V val2 = (V) nodes[3];

							final int nmap = ((bitmap & ~bitpos) ^ valmap);
							final byte npos1 = recoverMask(nmap, (byte) 1);
							final byte npos2 = recoverMask(nmap, (byte) 2);
							final AbstractNode<K, V> node1;
							final AbstractNode<K, V> node2;

							if (mask < npos1) {
								node1 = (AbstractNode<K, V>) nodes[valueArity() + 1];
								node2 = (AbstractNode<K, V>) nodes[valueArity() + 2];
							} else if (mask < npos2) {
								node1 = (AbstractNode<K, V>) nodes[valueArity() + 0];
								node2 = (AbstractNode<K, V>) nodes[valueArity() + 2];
							} else {
								node1 = (AbstractNode<K, V>) nodes[valueArity() + 0];
								node2 = (AbstractNode<K, V>) nodes[valueArity() + 1];
							}

							return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2,
											val2, npos1, node1, npos2, node2));
						}
						case 4: {
							final int map = valmap;
							final byte pos1 = recoverMask(map, (byte) 1);
							final K key1 = (K) nodes[0];
							final V val1 = (V) nodes[1];

							final int nmap = ((bitmap & ~bitpos) ^ valmap);
							final byte npos1 = recoverMask(nmap, (byte) 1);
							final byte npos2 = recoverMask(nmap, (byte) 2);
							final byte npos3 = recoverMask(nmap, (byte) 3);
							final AbstractNode<K, V> node1;
							final AbstractNode<K, V> node2;
							final AbstractNode<K, V> node3;

							if (mask < npos1) {
								node1 = (AbstractNode<K, V>) nodes[valueArity() + 1];
								node2 = (AbstractNode<K, V>) nodes[valueArity() + 2];
								node3 = (AbstractNode<K, V>) nodes[valueArity() + 3];
							} else if (mask < npos2) {
								node1 = (AbstractNode<K, V>) nodes[valueArity() + 0];
								node2 = (AbstractNode<K, V>) nodes[valueArity() + 2];
								node3 = (AbstractNode<K, V>) nodes[valueArity() + 3];
							} else if (mask < npos3) {
								node1 = (AbstractNode<K, V>) nodes[valueArity() + 0];
								node2 = (AbstractNode<K, V>) nodes[valueArity() + 1];
								node3 = (AbstractNode<K, V>) nodes[valueArity() + 3];
							} else {
								node1 = (AbstractNode<K, V>) nodes[valueArity() + 0];
								node2 = (AbstractNode<K, V>) nodes[valueArity() + 1];
								node3 = (AbstractNode<K, V>) nodes[valueArity() + 2];
							}

							return Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1,
											node1, npos2, node2, npos3, node3));
						}
						case 5: {
							final int nmap = ((bitmap & ~bitpos) ^ valmap);
							final byte npos1 = recoverMask(nmap, (byte) 1);
							final byte npos2 = recoverMask(nmap, (byte) 2);
							final byte npos3 = recoverMask(nmap, (byte) 3);
							final byte npos4 = recoverMask(nmap, (byte) 4);
							final AbstractNode<K, V> node1;
							final AbstractNode<K, V> node2;
							final AbstractNode<K, V> node3;
							final AbstractNode<K, V> node4;

							if (mask < npos1) {
								node1 = (AbstractNode<K, V>) nodes[valueArity() + 1];
								node2 = (AbstractNode<K, V>) nodes[valueArity() + 2];
								node3 = (AbstractNode<K, V>) nodes[valueArity() + 3];
								node4 = (AbstractNode<K, V>) nodes[valueArity() + 4];
							} else if (mask < npos2) {
								node1 = (AbstractNode<K, V>) nodes[valueArity() + 0];
								node2 = (AbstractNode<K, V>) nodes[valueArity() + 2];
								node3 = (AbstractNode<K, V>) nodes[valueArity() + 3];
								node4 = (AbstractNode<K, V>) nodes[valueArity() + 4];
							} else if (mask < npos3) {
								node1 = (AbstractNode<K, V>) nodes[valueArity() + 0];
								node2 = (AbstractNode<K, V>) nodes[valueArity() + 1];
								node3 = (AbstractNode<K, V>) nodes[valueArity() + 3];
								node4 = (AbstractNode<K, V>) nodes[valueArity() + 4];
							} else if (mask < npos4) {
								node1 = (AbstractNode<K, V>) nodes[valueArity() + 0];
								node2 = (AbstractNode<K, V>) nodes[valueArity() + 1];
								node3 = (AbstractNode<K, V>) nodes[valueArity() + 2];
								node4 = (AbstractNode<K, V>) nodes[valueArity() + 4];
							} else {
								node1 = (AbstractNode<K, V>) nodes[valueArity() + 0];
								node2 = (AbstractNode<K, V>) nodes[valueArity() + 1];
								node3 = (AbstractNode<K, V>) nodes[valueArity() + 2];
								node4 = (AbstractNode<K, V>) nodes[valueArity() + 3];
							}

							return Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2,
											npos3, node3, npos4, node4));
						}
						}
					} else {
						final Object[] editableNodes = copyAndRemovePair(this.nodes, bitIndex);

						final AbstractNode<K, V> thisNew = CompactNode.<K, V> valNodeOf(mutator,
										bitmap & ~bitpos, valmap, editableNodes,
										(byte) valueArity());

						return Result.modified(thisNew);
					}
				}
				case 1: {
					// inline value (move to front)
					final int valIndexNew = Integer.bitCount((valmap | bitpos) & (bitpos - 1));

					final Object[] editableNodes = copyAndMoveToFrontPair(this.nodes, bitIndex,
									valIndexNew, subNodeNew.headKey(), subNodeNew.headVal());

					final AbstractNode<K, V> thisNew = CompactNode.<K, V> valNodeOf(mutator,
									bitmap, valmap | bitpos, editableNodes,
									(byte) (valueArity() + 1));

					return Result.modified(thisNew);
				}
				default: {
					// // modify current node (set replacement node)
					// if (isAllowedToEdit(this.mutator, mutator)) {
					// // no copying if already editable
					// this.nodes[bitIndex] = subNodeNew;
					// return Result.modified(this);
					// } else {
					final Object[] editableNodes = copyAndSet(this.nodes, bitIndex, subNodeNew);

					final AbstractNode<K, V> thisNew = CompactNode.<K, V> valNodeOf(mutator,
									bitmap, valmap, editableNodes, (byte) valueArity());

					return Result.modified(thisNew);
					// }
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

			throw new RuntimeException("Called with invalid arguments."); // cnt1
																			// !=
																			// i_th
		}

		// InplaceIndexNode<K, V> editAndInsertPair(AtomicReference<Thread>
		// mutator, int index,
		// Object keyNew, Object valNew) {
		// final Object[] editableNodes = copyAndInsertPair(this.nodes, index,
		// keyNew, valNew);
		//
		// if (isAllowedToEdit(this.mutator, mutator)) {
		// this.nodes = editableNodes;
		// return this;
		// }
		//
		// return new InplaceIndexNode<>(mutator, editableNodes);
		// }
		//
		// InplaceIndexNode<K, V> editAndRemovePair(AtomicReference<Thread>
		// mutator, int index) {
		// final Object[] editableNodes = copyAndRemovePair(this.nodes, index);
		//
		// if (isAllowedToEdit(this.mutator, mutator)) {
		// this.nodes = editableNodes;
		// return this;
		// }
		//
		// return new InplaceIndexNode<>(mutator, editableNodes);
		// }
		//
		// InplaceIndexNode<K, V> editAndSet(AtomicReference<Thread> mutator,
		// int index,
		// Object elementNew) {
		// if (isAllowedToEdit(this.mutator, mutator)) {
		// // no copying if already editable
		// this.nodes[index] = elementNew;
		// return this;
		// } else {
		// final Object[] editableNodes = copyAndSet(this.nodes, index,
		// elementNew);
		// return new InplaceIndexNode<>(mutator, editableNodes);
		// }
		// }
		//
		// InplaceIndexNode<K, V> editAndMoveToBackPair(AtomicReference<Thread>
		// mutator, int indexOld,
		// int indexNew, Object elementNew) {
		// final Object[] editableNodes = copyAndMoveToBackPair(this.nodes,
		// indexOld, indexNew,
		// elementNew);
		//
		// if (isAllowedToEdit(this.mutator, mutator)) {
		// this.nodes = editableNodes;
		// return this;
		// }
		//
		// return new InplaceIndexNode<>(mutator, editableNodes);
		// }
		//
		// InplaceIndexNode<K, V> editAndMoveToFrontPair(AtomicReference<Thread>
		// mutator,
		// int indexOld, int indexNew, Object keyNew, Object valNew) {
		// final Object[] editableNodes = copyAndMoveToFrontPair(this.nodes,
		// indexOld, indexNew,
		// keyNew, valNew);
		//
		// if (isAllowedToEdit(this.mutator, mutator)) {
		// this.nodes = editableNodes;
		// return this;
		// }
		//
		// return new InplaceIndexNode<>(mutator, editableNodes);
		// }

		@Override
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(nodes, 0, 2 * valueArity());
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<AbstractNode<K, V>> nodeIterator() {
			final int offset = 2 * valueArity();

			for (int i = offset; i < nodes.length - offset; i++)
				assert ((nodes[i] instanceof AbstractNode) == true);

			return (Iterator) ArrayIterator.of(nodes, offset, nodes.length - offset);
		}

		@SuppressWarnings("unchecked")
		@Override
		K headKey() {
			assert hasValues();
			return (K) nodes[0];
		}

		@SuppressWarnings("unchecked")
		@Override
		V headVal() {
			assert hasValues();
			return (V) nodes[1];
		}

		@Override
		boolean hasValues() {
			return valueArity() != 0;
		}

		@Override
		abstract int valueArity();

		@Override
		boolean hasNodes() {
			return 2 * valueArity() != nodes.length;
		}

		@Override
		int nodeArity() {
			return nodes.length - 2 * valueArity();
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
			AbstractInplaceIndexNode<?, ?> that = (AbstractInplaceIndexNode<?, ?>) other;
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
		int size() {
			return SIZE_MORE_THAN_ONE;
		}
	}

	private static final class InplaceIndexNodeWith0Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith0Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 0;
		}

	}

	private static final class InplaceIndexNodeWith1Value<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith1Value(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 1;
		}

	}

	private static final class InplaceIndexNodeWith2Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith2Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 2;
		}

	}

	private static final class InplaceIndexNodeWith3Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith3Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 3;
		}

	}

	private static final class InplaceIndexNodeWith4Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith4Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 4;
		}

	}

	private static final class InplaceIndexNodeWith5Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith5Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 5;
		}

	}

	private static final class InplaceIndexNodeWith6Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith6Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 6;
		}

	}

	private static final class InplaceIndexNodeWith7Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith7Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 7;
		}

	}

	private static final class InplaceIndexNodeWith8Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith8Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 8;
		}

	}

	private static final class InplaceIndexNodeWith9Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith9Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 9;
		}

	}

	private static final class InplaceIndexNodeWith10Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith10Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 10;
		}

	}

	private static final class InplaceIndexNodeWith11Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith11Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 11;
		}

	}

	private static final class InplaceIndexNodeWith12Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith12Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 12;
		}

	}

	private static final class InplaceIndexNodeWith13Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith13Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 13;
		}

	}

	private static final class InplaceIndexNodeWith14Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith14Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 14;
		}

	}

	private static final class InplaceIndexNodeWith15Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith15Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 15;
		}

	}

	private static final class InplaceIndexNodeWith16Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith16Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 16;
		}

	}

	private static final class InplaceIndexNodeWith17Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith17Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 17;
		}

	}

	private static final class InplaceIndexNodeWith18Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith18Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 18;
		}

	}

	private static final class InplaceIndexNodeWith19Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith19Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 19;
		}

	}

	private static final class InplaceIndexNodeWith20Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith20Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 20;
		}

	}

	private static final class InplaceIndexNodeWith21Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith21Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 21;
		}

	}

	private static final class InplaceIndexNodeWith22Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith22Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 22;
		}

	}

	private static final class InplaceIndexNodeWith23Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith23Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 23;
		}

	}

	private static final class InplaceIndexNodeWith24Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith24Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 24;
		}

	}

	private static final class InplaceIndexNodeWith25Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith25Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 25;
		}

	}

	private static final class InplaceIndexNodeWith26Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith26Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 26;
		}

	}

	private static final class InplaceIndexNodeWith27Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith27Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 27;
		}

	}

	private static final class InplaceIndexNodeWith28Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith28Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 28;
		}

	}

	private static final class InplaceIndexNodeWith29Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith29Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 29;
		}

	}

	private static final class InplaceIndexNodeWith30Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith30Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 30;
		}

	}

	private static final class InplaceIndexNodeWith31Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith31Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 31;
		}

	}

	private static final class InplaceIndexNodeWith32Values<K, V> extends
					AbstractInplaceIndexNode<K, V> {

		InplaceIndexNodeWith32Values(AtomicReference<Thread> mutator, int bitmap, int valmap,
						Object[] nodes) {
			super(mutator, bitmap, valmap, nodes);
		}

		@Override
		int valueArity() {
			return 32;
		}

	}

	// TODO: replace by immutable cons list
	private static final class InplaceHashCollisionNode<K, V> extends CompactNode<K, V> {
		private final K[] keys;
		private final V[] vals;
		private final int hash;

		InplaceHashCollisionNode(int hash, K[] keys, V[] vals) {
			this.keys = keys;
			this.vals = vals;
			this.hash = hash;
		}

		@Override
		SupplierIterator<K, V> valueIterator() {
			// TODO: change representation of keys and values
			assert keys.length == vals.length;

			final Object[] keysAndVals = new Object[keys.length + vals.length];
			for (int i = 0; i < keys.length; i++) {
				keysAndVals[2 * i] = keys[i];
				keysAndVals[2 * i + 1] = vals[i];
			}

			return ArrayKeyValueIterator.of(keysAndVals);
		}

		@Override
		public String toString() {
			final Object[] keysAndVals = new Object[keys.length + vals.length];
			for (int i = 0; i < keys.length; i++) {
				keysAndVals[2 * i] = keys[i];
				keysAndVals[2 * i + 1] = vals[i];
			}
			return Arrays.toString(keysAndVals);
		}

		@Override
		Iterator<AbstractNode<K, V>> nodeIterator() {
			return Collections.emptyIterator();
		}

		@Override
		K headKey() {
			assert hasValues();
			return keys[0];
		}

		@Override
		V headVal() {
			assert hasValues();
			return vals[0];
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
		@SuppressWarnings("unchecked")
		@Override
		Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift,
						Comparator<Object> cmp) {
			if (this.hash != keyHash)
				return Result.modified(mergeNodes((CompactNode<K, V>) this, this.hash, key,
								keyHash, val, shift));

			if (containsKey(key, keyHash, shift, cmp))
				return Result.unchanged(this);

			final K[] keysNew = (K[]) copyAndInsert(keys, keys.length, key);
			final V[] valsNew = (V[]) copyAndInsert(vals, vals.length, val);
			return Result.modified(new InplaceHashCollisionNode<>(keyHash, keysNew, valsNew));
		}

		/**
		 * Removes an object if present. Note, that this implementation always
		 * returns a new immutable {@link TrieSet} instance.
		 */
		@SuppressWarnings("unchecked")
		@Override
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift,
						Comparator<Object> cmp) {
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
						final V theOtherVal = (i == 0) ? vals[1] : vals[0];
						return EMPTY_INPLACE_INDEX_NODE.updated(mutator, theOtherKey, keyHash,
										theOtherVal, 0, cmp);
					} else {
						return Result.modified(new InplaceHashCollisionNode<>(keyHash,
										(K[]) copyAndRemove(keys, i), (V[]) copyAndRemove(vals, i)));
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
		int size() {
			return valueArity();
		}

		// @Override
		// K getValue(int index) {
		// return keys[index];
		// }
		//
		// @Override
		// public AbstractNode<K, V> getNode(int index) {
		// throw new IllegalStateException("Is leaf node.");
		// }

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

			InplaceHashCollisionNode<?, ?> that = (InplaceHashCollisionNode<?, ?>) other;

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
				final Object otherVal = it.next();

				for (int i = 0; i < keys.length; i++) {
					final K key = keys[i];
					final V val = vals[i];

					if (key.equals(otherKey) && val.equals(otherVal)) {
						continue outerLoop;
					}
				}
				return false;
			}

			return true;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int hash, int shift, Comparator<Object> cmp) {
			for (int i = 0; i < keys.length; i++) {
				final K _key = keys[i];
				if (cmp.compare(key, _key) == 0) {
					final V _val = vals[i];
					return Optional.of(entryOf(_key, _val));
				}
			}
			return Optional.empty();
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((rootNode == null) ? 0 : rootNode.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (other == null)
			return false;

		if (other instanceof TrieMapGenerated) {
			TrieMapGenerated that = (TrieMapGenerated) other;

			if (this.size() != that.size())
				return false;

			return rootNode.equals(that.rootNode);
		}

		return super.equals(other);
	}

	public void printStats() {
//		final Iterator<AbstractNode<K, V>> it = new TrieMapNodeIterator<>(rootNode);
//
//		final Path file = Paths.get("tree-node-stats.csv");
//		final List<String> lines = new ArrayList<>();
//		lines.add("arity,valueArity,nodeArity,size");
//
//		while (it.hasNext()) {
//			final AbstractNode<K, V> node = it.next();
//
//			// it.next();
//			// final AbstractNode<K, V> node =
//			// CompactNode.EMPTY_INPLACE_INDEX_NODE;
//
//			Predicate<Object> isRoot = new Predicate<Object>() {
//				@Override
//				public boolean apply(Object arg0) {
//					return arg0 == node;
//				}
//			};
//
//			Predicate<Object> jointPredicate = Predicates.or(isRoot, Predicates.not(Predicates.or(
//							Predicates.instanceOf(AbstractNode.class),
//							Predicates.instanceOf(Integer.class))));
//
//			long memoryInBytes = objectexplorer.MemoryMeasurer.measureBytes(node, jointPredicate);
//			Footprint memoryFootprint = objectexplorer.ObjectGraphMeasurer.measure(node,
//							jointPredicate);
//
//			// if (node.arity() == 32) {
//			final int pointers = 2 * node.valueArity() + node.nodeArity();
//
//			final String statString = String
//							.format("arity=%d [values=%d, nodes=%d]\n%d bytes [%1.1f bytes per pointer]\n%s\n",
//											node.arity(), node.valueArity(), node.nodeArity(),
//											memoryInBytes, (float) memoryInBytes / pointers,
//											memoryFootprint);
//
//			System.out.println(statString);
//
//			final String statFileString = String.format("%d,%d,%d,%d", node.arity(),
//							node.valueArity(), node.nodeArity(), memoryInBytes);
//			lines.add(statFileString);
//			// }
//		}
//
//		Predicate<Object> totalPredicate = Predicates.not(Predicates.instanceOf(Integer.class));
//
//		long totalMemoryInBytes = objectexplorer.MemoryMeasurer.measureBytes(rootNode,
//						totalPredicate);
//		Footprint totalMemoryFootprint = objectexplorer.ObjectGraphMeasurer.measure(rootNode,
//						totalPredicate);
//
//		final String totalStatString = String.format(
//						"size=%d\n%d bytes [%1.1f bytes per key-value-pair; min 8.0 bytes]\n%s\n",
//						cachedSize, totalMemoryInBytes, (float) totalMemoryInBytes / cachedSize,
//						totalMemoryFootprint);
//
//		System.out.println(totalStatString);
//
//		// write stats to file
//		try {
//			Files.write(file, lines, StandardCharsets.UTF_8);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}

	/**
	 * Iterator that first iterates over inlined-values and then continues depth
	 * first recursively.
	 */
	private static class TrieMapNodeIterator<K, V> implements Iterator<AbstractNode<K, V>> {

		final Deque<Iterator<AbstractNode<K, V>>> nodeIteratorStack;

		TrieMapNodeIterator(AbstractNode<K, V> rootNode) {
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
		public AbstractNode<K, V> next() {
			if (!hasNext())
				throw new NoSuchElementException();

			AbstractNode<K, V> innerNode = nodeIteratorStack.peek().next();

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

	private abstract static class AbstractSingletonNode<K, V> extends CompactNode<K, V> {

		protected abstract byte npos1();

		protected final AbstractNode<K, V> node1;

		AbstractSingletonNode(AbstractNode<K, V> node1) {
			this.node1 = node1;
		}

		@Override
		Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				final Result<K, V> subNodeResult = node1.updated(mutator, key, keyHash, val, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			}

			// no value
			final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, npos1(), node1);
			return Result.modified(thisNew);

		}

		@Override
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				final Result<K, V> subNodeResult = node1.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				switch (subNodeNew.size()) {
				case SIZE_EMPTY:
				case SIZE_ONE:
					// escalate (singleton or empty) result
					return subNodeResult;

				case SIZE_MORE_THAN_ONE:
					// modify current node (set replacement node)
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, subNodeNew);
					return Result.modified(thisNew);

				default:
					throw new IllegalStateException("Invalid size state.");
				}
			}

			return Result.unchanged(this);
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
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<AbstractNode<K, V>> nodeIterator() {
			return ArrayIterator.<AbstractNode<K, V>> of(new AbstractNode[] { node1 });
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
		SupplierIterator<K, V> valueIterator() {
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
		int size() {
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
			AbstractSingletonNode<?, ?> that = (AbstractSingletonNode<?, ?>) other;

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

		@Override
		V headVal() {
			throw new UnsupportedOperationException("No value in this kind of node.");
		}

	}

	private static final class SingletonNodeAtMask0Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 0;
		}

		SingletonNodeAtMask0Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask1Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 1;
		}

		SingletonNodeAtMask1Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask2Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 2;
		}

		SingletonNodeAtMask2Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask3Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 3;
		}

		SingletonNodeAtMask3Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask4Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 4;
		}

		SingletonNodeAtMask4Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask5Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 5;
		}

		SingletonNodeAtMask5Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask6Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 6;
		}

		SingletonNodeAtMask6Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask7Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 7;
		}

		SingletonNodeAtMask7Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask8Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 8;
		}

		SingletonNodeAtMask8Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask9Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 9;
		}

		SingletonNodeAtMask9Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask10Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 10;
		}

		SingletonNodeAtMask10Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask11Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 11;
		}

		SingletonNodeAtMask11Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask12Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 12;
		}

		SingletonNodeAtMask12Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask13Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 13;
		}

		SingletonNodeAtMask13Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask14Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 14;
		}

		SingletonNodeAtMask14Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask15Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 15;
		}

		SingletonNodeAtMask15Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask16Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 16;
		}

		SingletonNodeAtMask16Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask17Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 17;
		}

		SingletonNodeAtMask17Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask18Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 18;
		}

		SingletonNodeAtMask18Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask19Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 19;
		}

		SingletonNodeAtMask19Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask20Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 20;
		}

		SingletonNodeAtMask20Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask21Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 21;
		}

		SingletonNodeAtMask21Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask22Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 22;
		}

		SingletonNodeAtMask22Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask23Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 23;
		}

		SingletonNodeAtMask23Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask24Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 24;
		}

		SingletonNodeAtMask24Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask25Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 25;
		}

		SingletonNodeAtMask25Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask26Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 26;
		}

		SingletonNodeAtMask26Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask27Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 27;
		}

		SingletonNodeAtMask27Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask28Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 28;
		}

		SingletonNodeAtMask28Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask29Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 29;
		}

		SingletonNodeAtMask29Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask30Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 30;
		}

		SingletonNodeAtMask30Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonNodeAtMask31Node<K, V> extends AbstractSingletonNode<K, V> {

		@Override
		protected byte npos1() {
			return 31;
		}

		SingletonNodeAtMask31Node(AbstractNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class Value0Index0Node<K, V> extends CompactNode<K, V> {

		Value0Index0Node() {
		}

		@Override
		Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			// no value

			final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, key, val);
			return Result.modified(thisNew);

		}

		@Override
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift,
						Comparator<Object> cmp) {

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {

			return Optional.empty();
		}

		@Override
		Iterator<AbstractNode<K, V>> nodeIterator() {
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
		SupplierIterator<K, V> valueIterator() {
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
		int size() {
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

		@Override
		V headVal() {
			throw new UnsupportedOperationException("No value in this kind of node.");
		}

	}

	private static final class Value0Index2Node<K, V> extends CompactNode<K, V> {
		private final byte npos1;
		private final AbstractNode<K, V> node1;
		private final byte npos2;
		private final AbstractNode<K, V> node2;

		Value0Index2Node(AtomicReference<Thread> mutator, byte npos1, AbstractNode<K, V> node1,
						byte npos2, AbstractNode<K, V> node2) {
			this.npos1 = npos1;
			this.node1 = node1;
			this.npos2 = npos2;
			this.node2 = node2;
		}

		@Override
		Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				final Result<K, V> subNodeResult = node1.updated(mutator, key, keyHash, val, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
								subNodeResult.getNode(), npos2, node2);

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			} else if (mask == npos2) {
				final Result<K, V> subNodeResult = node2.updated(mutator, key, keyHash, val, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask,
								subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			}

			// no value

			final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, npos1, node1,
							npos2, node2);
			return Result.modified(thisNew);

		}

		@Override
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				final Result<K, V> subNodeResult = node1.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				assert this.arity() >= 2;
				assert subNodeNew.size() >= 1;

				switch (subNodeNew.size()) {
				// case 0:
				// // remove node
				// return Result.modified(valNodeOf(mutator, npos2, node2));

				case 1:
					// inline value

					if (mask < npos1) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), npos2, node2);
						return Result.modified(thisNew);
					}

				default:
					// modify current node (set replacement node)

					final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, subNodeNew, npos2,
									node2);

					return Result.modified(thisNew);
				}
			} else if (mask == npos2) {
				final Result<K, V> subNodeResult = node2.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				assert this.arity() >= 2;
				assert subNodeNew.size() >= 1;

				switch (subNodeNew.size()) {
				// case 0:
				// // remove node
				// return Result.modified(valNodeOf(mutator, npos1, node1));

				case 1:
					// inline value

					if (mask < npos1) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), npos1, node1);
						return Result.modified(thisNew);
					}

				default:
					// modify current node (set replacement node)

					final AbstractNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask,
									subNodeNew);

					return Result.modified(thisNew);
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
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
		Iterator<AbstractNode<K, V>> nodeIterator() {
			return ArrayIterator.<AbstractNode<K, V>> of(new AbstractNode[] { node1, node2 });
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
		SupplierIterator<K, V> valueIterator() {
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
		int size() {
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
			Value0Index2Node<?, ?> that = (Value0Index2Node<?, ?>) other;

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
			return String.format("[%s, %s]", node1, node2);
		}

		@Override
		K headKey() {
			throw new UnsupportedOperationException("No key in this kind of node.");
		}

		@Override
		V headVal() {
			throw new UnsupportedOperationException("No value in this kind of node.");
		}

	}

	private static final class Value0Index3Node<K, V> extends CompactNode<K, V> {
		private final byte npos1;
		private final AbstractNode<K, V> node1;
		private final byte npos2;
		private final AbstractNode<K, V> node2;
		private final byte npos3;
		private final AbstractNode<K, V> node3;

		Value0Index3Node(AtomicReference<Thread> mutator, byte npos1, AbstractNode<K, V> node1,
						byte npos2, AbstractNode<K, V> node2, byte npos3, AbstractNode<K, V> node3) {
			this.npos1 = npos1;
			this.node1 = node1;
			this.npos2 = npos2;
			this.node2 = node2;
			this.npos3 = npos3;
			this.node3 = node3;
		}

		@Override
		Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				final Result<K, V> subNodeResult = node1.updated(mutator, key, keyHash, val, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
								subNodeResult.getNode(), npos2, node2, npos3, node3);

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			} else if (mask == npos2) {
				final Result<K, V> subNodeResult = node2.updated(mutator, key, keyHash, val, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask,
								subNodeResult.getNode(), npos3, node3);

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			} else if (mask == npos3) {
				final Result<K, V> subNodeResult = node3.updated(mutator, key, keyHash, val, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
								mask, subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			}

			// no value

			final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, npos1, node1,
							npos2, node2, npos3, node3);
			return Result.modified(thisNew);

		}

		@Override
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				final Result<K, V> subNodeResult = node1.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				assert this.arity() >= 2;
				assert subNodeNew.size() >= 1;

				switch (subNodeNew.size()) {
				// case 0:
				// // remove node
				// return Result.modified(valNodeOf(mutator, npos2, node2,
				// npos3, node3));

				case 1:
					// inline value

					if (mask < npos1) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), npos2, node2,
										npos3, node3);
						return Result.modified(thisNew);
					}

				default:
					// modify current node (set replacement node)

					final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, subNodeNew, npos2,
									node2, npos3, node3);

					return Result.modified(thisNew);
				}
			} else if (mask == npos2) {
				final Result<K, V> subNodeResult = node2.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				assert this.arity() >= 2;
				assert subNodeNew.size() >= 1;

				switch (subNodeNew.size()) {
				// case 0:
				// // remove node
				// return Result.modified(valNodeOf(mutator, npos1, node1,
				// npos3, node3));

				case 1:
					// inline value

					if (mask < npos1) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), npos1, node1,
										npos3, node3);
						return Result.modified(thisNew);
					}

				default:
					// modify current node (set replacement node)

					final AbstractNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask,
									subNodeNew, npos3, node3);

					return Result.modified(thisNew);
				}
			} else if (mask == npos3) {
				final Result<K, V> subNodeResult = node3.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				assert this.arity() >= 2;
				assert subNodeNew.size() >= 1;

				switch (subNodeNew.size()) {
				// case 0:
				// // remove node
				// return Result.modified(valNodeOf(mutator, npos1, node1,
				// npos2, node2));

				case 1:
					// inline value

					if (mask < npos1) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), npos1, node1,
										npos2, node2);
						return Result.modified(thisNew);
					}

				default:
					// modify current node (set replacement node)

					final AbstractNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2,
									node2, mask, subNodeNew);

					return Result.modified(thisNew);
				}
			}

			return Result.unchanged(this);
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
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
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
		Iterator<AbstractNode<K, V>> nodeIterator() {
			return ArrayIterator
							.<AbstractNode<K, V>> of(new AbstractNode[] { node1, node2, node3 });
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
		SupplierIterator<K, V> valueIterator() {
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
		int size() {
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
			Value0Index3Node<?, ?> that = (Value0Index3Node<?, ?>) other;

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
			return String.format("[%s, %s, %s]", node1, node2, node3);
		}

		@Override
		K headKey() {
			throw new UnsupportedOperationException("No key in this kind of node.");
		}

		@Override
		V headVal() {
			throw new UnsupportedOperationException("No value in this kind of node.");
		}

	}

	private static final class Value0Index4Node<K, V> extends CompactNode<K, V> {
		private final byte npos1;
		private final AbstractNode<K, V> node1;
		private final byte npos2;
		private final AbstractNode<K, V> node2;
		private final byte npos3;
		private final AbstractNode<K, V> node3;
		private final byte npos4;
		private final AbstractNode<K, V> node4;

		Value0Index4Node(AtomicReference<Thread> mutator, byte npos1, AbstractNode<K, V> node1,
						byte npos2, AbstractNode<K, V> node2, byte npos3, AbstractNode<K, V> node3,
						byte npos4, AbstractNode<K, V> node4) {
			this.npos1 = npos1;
			this.node1 = node1;
			this.npos2 = npos2;
			this.node2 = node2;
			this.npos3 = npos3;
			this.node3 = node3;
			this.npos4 = npos4;
			this.node4 = node4;
		}

		@Override
		Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				final Result<K, V> subNodeResult = node1.updated(mutator, key, keyHash, val, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
								subNodeResult.getNode(), npos2, node2, npos3, node3, npos4, node4);

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			} else if (mask == npos2) {
				final Result<K, V> subNodeResult = node2.updated(mutator, key, keyHash, val, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask,
								subNodeResult.getNode(), npos3, node3, npos4, node4);

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			} else if (mask == npos3) {
				final Result<K, V> subNodeResult = node3.updated(mutator, key, keyHash, val, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
								mask, subNodeResult.getNode(), npos4, node4);

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			} else if (mask == npos4) {
				final Result<K, V> subNodeResult = node4.updated(mutator, key, keyHash, val, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
								npos3, node3, mask, subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			}

			// no value

			final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, npos1, node1,
							npos2, node2, npos3, node3, npos4, node4);
			return Result.modified(thisNew);

		}

		@Override
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1) {
				final Result<K, V> subNodeResult = node1.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				assert this.arity() >= 2;
				assert subNodeNew.size() >= 1;

				switch (subNodeNew.size()) {
				// case 0:
				// // remove node
				// return Result.modified(valNodeOf(mutator, npos2, node2,
				// npos3, node3, npos4, node4));

				case 1:
					// inline value

					if (mask < npos1) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), npos2, node2,
										npos3, node3, npos4, node4);
						return Result.modified(thisNew);
					}

				default:
					// modify current node (set replacement node)

					final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, subNodeNew, npos2,
									node2, npos3, node3, npos4, node4);

					return Result.modified(thisNew);
				}
			} else if (mask == npos2) {
				final Result<K, V> subNodeResult = node2.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				assert this.arity() >= 2;
				assert subNodeNew.size() >= 1;

				switch (subNodeNew.size()) {
				// case 0:
				// // remove node
				// return Result.modified(valNodeOf(mutator, npos1, node1,
				// npos3, node3, npos4, node4));

				case 1:
					// inline value

					if (mask < npos1) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), npos1, node1,
										npos3, node3, npos4, node4);
						return Result.modified(thisNew);
					}

				default:
					// modify current node (set replacement node)

					final AbstractNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask,
									subNodeNew, npos3, node3, npos4, node4);

					return Result.modified(thisNew);
				}
			} else if (mask == npos3) {
				final Result<K, V> subNodeResult = node3.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				assert this.arity() >= 2;
				assert subNodeNew.size() >= 1;

				switch (subNodeNew.size()) {
				// case 0:
				// // remove node
				// return Result.modified(valNodeOf(mutator, npos1, node1,
				// npos2, node2, npos4, node4));

				case 1:
					// inline value

					if (mask < npos1) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), npos1, node1,
										npos2, node2, npos4, node4);
						return Result.modified(thisNew);
					}

				default:
					// modify current node (set replacement node)

					final AbstractNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2,
									node2, mask, subNodeNew, npos4, node4);

					return Result.modified(thisNew);
				}
			} else if (mask == npos4) {
				final Result<K, V> subNodeResult = node4.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				assert this.arity() >= 2;
				assert subNodeNew.size() >= 1;

				switch (subNodeNew.size()) {
				// case 0:
				// // remove node
				// return Result.modified(valNodeOf(mutator, npos1, node1,
				// npos2, node2, npos3, node3));

				case 1:
					// inline value

					if (mask < npos1) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), npos1, node1,
										npos2, node2, npos3, node3);
						return Result.modified(thisNew);
					}

				default:
					// modify current node (set replacement node)

					final AbstractNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2,
									node2, npos3, node3, mask, subNodeNew);

					return Result.modified(thisNew);
				}
			}

			return Result.unchanged(this);
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
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
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
		Iterator<AbstractNode<K, V>> nodeIterator() {
			return ArrayIterator.<AbstractNode<K, V>> of(new AbstractNode[] { node1, node2, node3,
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
		SupplierIterator<K, V> valueIterator() {
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
		int size() {
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
			Value0Index4Node<?, ?> that = (Value0Index4Node<?, ?>) other;

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
			return String.format("[%s, %s, %s, %s]", node1, node2, node3, node4);
		}

		@Override
		K headKey() {
			throw new UnsupportedOperationException("No key in this kind of node.");
		}

		@Override
		V headVal() {
			throw new UnsupportedOperationException("No value in this kind of node.");
		}

	}

	private static final class Value1Index0Node<K, V> extends CompactNode<K, V> {
		private final byte pos1;
		private final K key1;
		private final V val1;

		Value1Index0Node(AtomicReference<Thread> mutator, byte pos1, K key1, V val1) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;
		}

		@Override
		Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (cmp.compare(val, val1) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(valNodeOf(mutator, mask, key, val), val1);
				}

				// merge into node
				final AbstractNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, node);
				return Result.modified(thisNew);

			}

			// no value

			if (mask < pos1) {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, pos1, key1,
								val1);
				return Result.modified(thisNew);
			}

			else {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, key,
								val);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(CompactNode.<K, V> valNodeOf(mutator));
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			}

			return Optional.empty();
		}

		@Override
		Iterator<AbstractNode<K, V>> nodeIterator() {
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
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1 });
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
		int size() {
			return SIZE_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();
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
			Value1Index0Node<?, ?> that = (Value1Index0Node<?, ?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s=%s]", key1, val1);
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		V headVal() {
			return val1;
		}

	}

	private static final class Value1Index1Node<K, V> extends CompactNode<K, V> {
		private final byte pos1;
		private final K key1;
		private final V val1;
		private final byte npos1;
		private final AbstractNode<K, V> node1;

		Value1Index1Node(AtomicReference<Thread> mutator, byte pos1, K key1, V val1, byte npos1,
						AbstractNode<K, V> node1) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;
			this.npos1 = npos1;
			this.node1 = node1;
		}

		@Override
		Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (cmp.compare(val, val1) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(valNodeOf(mutator, mask, key, val, npos1, node1), val1);
				}

				// merge into node
				final AbstractNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, node, npos1, node1);
					return Result.modified(thisNew);
				}

				else {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == npos1) {
				final Result<K, V> subNodeResult = node1.updated(mutator, key, keyHash, val, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
								subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			}

			// no value

			if (mask < pos1) {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, pos1, key1,
								val1, npos1, node1);
				return Result.modified(thisNew);
			}

			else {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, key,
								val, npos1, node1);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, npos1, node1));
			} else if (mask == npos1) {
				final Result<K, V> subNodeResult = node1.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				assert this.arity() >= 2;
				assert subNodeNew.size() >= 1;

				switch (subNodeNew.size()) {
				// case 0:
				// // remove node
				// return Result.modified(valNodeOf(mutator, pos1, key1, val1));

				case 1:
					// inline value

					if (mask < npos1) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), pos1, key1,
										val1);
						return Result.modified(thisNew);
					}

					else {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										mask, subNodeNew.headKey(), subNodeNew.headVal());
						return Result.modified(thisNew);
					}

				default:
					// modify current node (set replacement node)

					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
									subNodeNew);

					return Result.modified(thisNew);
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<AbstractNode<K, V>> nodeIterator() {
			return ArrayIterator.<AbstractNode<K, V>> of(new AbstractNode[] { node1 });
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
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1 });
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
		int size() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();
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
			Value1Index1Node<?, ?> that = (Value1Index1Node<?, ?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
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
			return String.format("[%s=%s, %s]", key1, val1, node1);
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		V headVal() {
			return val1;
		}

	}

	private static final class Value1Index2Node<K, V> extends CompactNode<K, V> {
		private final byte pos1;
		private final K key1;
		private final V val1;
		private final byte npos1;
		private final AbstractNode<K, V> node1;
		private final byte npos2;
		private final AbstractNode<K, V> node2;

		Value1Index2Node(AtomicReference<Thread> mutator, byte pos1, K key1, V val1, byte npos1,
						AbstractNode<K, V> node1, byte npos2, AbstractNode<K, V> node2) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;
			this.npos1 = npos1;
			this.node1 = node1;
			this.npos2 = npos2;
			this.node2 = node2;
		}

		@Override
		Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (cmp.compare(val, val1) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, mask, key, val, npos1, node1, npos2, node2),
									val1);
				}

				// merge into node
				final AbstractNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, node, npos1, node1,
									npos2, node2);
					return Result.modified(thisNew);
				}

				else if (mask < npos2) {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask, node,
									npos2, node2);
					return Result.modified(thisNew);
				}

				else {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2,
									node2, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == npos1) {
				final Result<K, V> subNodeResult = node1.updated(mutator, key, keyHash, val, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
								subNodeResult.getNode(), npos2, node2);

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			} else if (mask == npos2) {
				final Result<K, V> subNodeResult = node2.updated(mutator, key, keyHash, val, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
								node1, mask, subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			}

			// no value

			if (mask < pos1) {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, pos1, key1,
								val1, npos1, node1, npos2, node2);
				return Result.modified(thisNew);
			}

			else {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, key,
								val, npos1, node1, npos2, node2);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2));
			} else if (mask == npos1) {
				final Result<K, V> subNodeResult = node1.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				assert this.arity() >= 2;
				assert subNodeNew.size() >= 1;

				switch (subNodeNew.size()) {
				// case 0:
				// // remove node
				// return Result.modified(valNodeOf(mutator, pos1, key1, val1,
				// npos2, node2));

				case 1:
					// inline value

					if (mask < npos1) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), pos1, key1,
										val1, npos2, node2);
						return Result.modified(thisNew);
					}

					else {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										mask, subNodeNew.headKey(), subNodeNew.headVal(), npos2,
										node2);
						return Result.modified(thisNew);
					}

				default:
					// modify current node (set replacement node)

					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
									subNodeNew, npos2, node2);

					return Result.modified(thisNew);
				}
			} else if (mask == npos2) {
				final Result<K, V> subNodeResult = node2.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				assert this.arity() >= 2;
				assert subNodeNew.size() >= 1;

				switch (subNodeNew.size()) {
				// case 0:
				// // remove node
				// return Result.modified(valNodeOf(mutator, pos1, key1, val1,
				// npos1, node1));

				case 1:
					// inline value

					if (mask < npos1) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), pos1, key1,
										val1, npos1, node1);
						return Result.modified(thisNew);
					}

					else {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										mask, subNodeNew.headKey(), subNodeNew.headVal(), npos1,
										node1);
						return Result.modified(thisNew);
					}

				default:
					// modify current node (set replacement node)

					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
									node1, mask, subNodeNew);

					return Result.modified(thisNew);
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<AbstractNode<K, V>> nodeIterator() {
			return ArrayIterator.<AbstractNode<K, V>> of(new AbstractNode[] { node1, node2 });
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
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1 });
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
		int size() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();
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
			Value1Index2Node<?, ?> that = (Value1Index2Node<?, ?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
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
			return String.format("[%s=%s, %s, %s]", key1, val1, node1, node2);
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		V headVal() {
			return val1;
		}

	}

	private static final class Value1Index3Node<K, V> extends CompactNode<K, V> {
		private final byte pos1;
		private final K key1;
		private final V val1;
		private final byte npos1;
		private final AbstractNode<K, V> node1;
		private final byte npos2;
		private final AbstractNode<K, V> node2;
		private final byte npos3;
		private final AbstractNode<K, V> node3;

		Value1Index3Node(AtomicReference<Thread> mutator, byte pos1, K key1, V val1, byte npos1,
						AbstractNode<K, V> node1, byte npos2, AbstractNode<K, V> node2, byte npos3,
						AbstractNode<K, V> node3) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;
			this.npos1 = npos1;
			this.node1 = node1;
			this.npos2 = npos2;
			this.node2 = node2;
			this.npos3 = npos3;
			this.node3 = node3;
		}

		@Override
		Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (cmp.compare(val, val1) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, mask, key, val, npos1, node1, npos2, node2,
													npos3, node3), val1);
				}

				// merge into node
				final AbstractNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, node, npos1, node1,
									npos2, node2, npos3, node3);
					return Result.modified(thisNew);
				}

				else if (mask < npos2) {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask, node,
									npos2, node2, npos3, node3);
					return Result.modified(thisNew);
				}

				else if (mask < npos3) {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2,
									node2, mask, node, npos3, node3);
					return Result.modified(thisNew);
				}

				else {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2,
									node2, npos3, node3, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == npos1) {
				final Result<K, V> subNodeResult = node1.updated(mutator, key, keyHash, val, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
								subNodeResult.getNode(), npos2, node2, npos3, node3);

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			} else if (mask == npos2) {
				final Result<K, V> subNodeResult = node2.updated(mutator, key, keyHash, val, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
								node1, mask, subNodeResult.getNode(), npos3, node3);

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			} else if (mask == npos3) {
				final Result<K, V> subNodeResult = node3.updated(mutator, key, keyHash, val, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
								node1, npos2, node2, mask, subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			}

			// no value

			if (mask < pos1) {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, pos1, key1,
								val1, npos1, node1, npos2, node2, npos3, node3);
				return Result.modified(thisNew);
			}

			else {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, key,
								val, npos1, node1, npos2, node2, npos3, node3);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2, npos3, node3));
			} else if (mask == npos1) {
				final Result<K, V> subNodeResult = node1.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				assert this.arity() >= 2;
				assert subNodeNew.size() >= 1;

				switch (subNodeNew.size()) {
				// case 0:
				// // remove node
				// return Result.modified(valNodeOf(mutator, pos1, key1, val1,
				// npos2, node2, npos3, node3));

				case 1:
					// inline value

					if (mask < npos1) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), pos1, key1,
										val1, npos2, node2, npos3, node3);
						return Result.modified(thisNew);
					}

					else {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										mask, subNodeNew.headKey(), subNodeNew.headVal(), npos2,
										node2, npos3, node3);
						return Result.modified(thisNew);
					}

				default:
					// modify current node (set replacement node)

					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
									subNodeNew, npos2, node2, npos3, node3);

					return Result.modified(thisNew);
				}
			} else if (mask == npos2) {
				final Result<K, V> subNodeResult = node2.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				assert this.arity() >= 2;
				assert subNodeNew.size() >= 1;

				switch (subNodeNew.size()) {
				// case 0:
				// // remove node
				// return Result.modified(valNodeOf(mutator, pos1, key1, val1,
				// npos1, node1, npos3, node3));

				case 1:
					// inline value

					if (mask < npos1) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), pos1, key1,
										val1, npos1, node1, npos3, node3);
						return Result.modified(thisNew);
					}

					else {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										mask, subNodeNew.headKey(), subNodeNew.headVal(), npos1,
										node1, npos3, node3);
						return Result.modified(thisNew);
					}

				default:
					// modify current node (set replacement node)

					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
									node1, mask, subNodeNew, npos3, node3);

					return Result.modified(thisNew);
				}
			} else if (mask == npos3) {
				final Result<K, V> subNodeResult = node3.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				assert this.arity() >= 2;
				assert subNodeNew.size() >= 1;

				switch (subNodeNew.size()) {
				// case 0:
				// // remove node
				// return Result.modified(valNodeOf(mutator, pos1, key1, val1,
				// npos1, node1, npos2, node2));

				case 1:
					// inline value

					if (mask < npos1) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), pos1, key1,
										val1, npos1, node1, npos2, node2);
						return Result.modified(thisNew);
					}

					else {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										mask, subNodeNew.headKey(), subNodeNew.headVal(), npos1,
										node1, npos2, node2);
						return Result.modified(thisNew);
					}

				default:
					// modify current node (set replacement node)

					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
									node1, npos2, node2, mask, subNodeNew);

					return Result.modified(thisNew);
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos3) {
				return node3.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
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
		Iterator<AbstractNode<K, V>> nodeIterator() {
			return ArrayIterator
							.<AbstractNode<K, V>> of(new AbstractNode[] { node1, node2, node3 });
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
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1 });
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
		int size() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();
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
			Value1Index3Node<?, ?> that = (Value1Index3Node<?, ?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
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
			return String.format("[%s=%s, %s, %s, %s]", key1, val1, node1, node2, node3);
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		V headVal() {
			return val1;
		}

	}

	private static final class Value2Index0Node<K, V> extends CompactNode<K, V> {
		private final byte pos1;
		private final K key1;
		private final V val1;
		private final byte pos2;
		private final K key2;
		private final V val2;

		Value2Index0Node(AtomicReference<Thread> mutator, byte pos1, K key1, V val1, byte pos2,
						K key2, V val2) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;
			this.pos2 = pos2;
			this.key2 = key2;
			this.val2 = val2;
		}

		@Override
		Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (cmp.compare(val, val1) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(valNodeOf(mutator, mask, key, val, pos2, key2, val2),
									val1);
				}

				// merge into node
				final AbstractNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos2, key2, val2, mask, node);
				return Result.modified(thisNew);

			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					if (cmp.compare(val, val2) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(valNodeOf(mutator, pos1, key1, val1, mask, key, val),
									val2);
				}

				// merge into node
				final AbstractNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, node);
				return Result.modified(thisNew);

			}

			// no value

			if (mask < pos1) {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, pos1, key1,
								val1, pos2, key2, val2);
				return Result.modified(thisNew);
			}

			else if (mask < pos2) {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, key,
								val, pos2, key2, val2);
				return Result.modified(thisNew);
			}

			else {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, mask, key, val);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos2, key2, val2));
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, val1));
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return true;
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(entryOf(key2, val2));
			}

			return Optional.empty();
		}

		@Override
		Iterator<AbstractNode<K, V>> nodeIterator() {
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
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2 });
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
		int size() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();
			result = prime * result + pos2;
			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();
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
			Value2Index0Node<?, ?> that = (Value2Index0Node<?, ?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (pos2 != that.pos2) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s=%s, %s=%s]", key1, val1, key2, val2);
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		V headVal() {
			return val1;
		}

	}

	private static final class Value2Index1Node<K, V> extends CompactNode<K, V> {
		private final byte pos1;
		private final K key1;
		private final V val1;
		private final byte pos2;
		private final K key2;
		private final V val2;
		private final byte npos1;
		private final AbstractNode<K, V> node1;

		Value2Index1Node(AtomicReference<Thread> mutator, byte pos1, K key1, V val1, byte pos2,
						K key2, V val2, byte npos1, AbstractNode<K, V> node1) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;
			this.pos2 = pos2;
			this.key2 = key2;
			this.val2 = val2;
			this.npos1 = npos1;
			this.node1 = node1;
		}

		@Override
		Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (cmp.compare(val, val1) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, mask, key, val, pos2, key2, val2, npos1,
													node1), val1);
				}

				// merge into node
				final AbstractNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos2, key2, val2, mask,
									node, npos1, node1);
					return Result.modified(thisNew);
				}

				else {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos2, key2, val2, npos1,
									node1, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					if (cmp.compare(val, val2) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, pos1, key1, val1, mask, key, val, npos1,
													node1), val2);
				}

				// merge into node
				final AbstractNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
									node, npos1, node1);
					return Result.modified(thisNew);
				}

				else {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
									node1, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == npos1) {
				final Result<K, V> subNodeResult = node1.updated(mutator, key, keyHash, val, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, mask, subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			}

			// no value

			if (mask < pos1) {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, pos1, key1,
								val1, pos2, key2, val2, npos1, node1);
				return Result.modified(thisNew);
			}

			else if (mask < pos2) {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, key,
								val, pos2, key2, val2, npos1, node1);
				return Result.modified(thisNew);
			}

			else {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, mask, key, val, npos1, node1);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos2, key2, val2, npos1, node1));
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1));
			} else if (mask == npos1) {
				final Result<K, V> subNodeResult = node1.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				assert this.arity() >= 2;
				assert subNodeNew.size() >= 1;

				switch (subNodeNew.size()) {
				// case 0:
				// // remove node
				// return Result.modified(valNodeOf(mutator, pos1, key1, val1,
				// pos2, key2, val2));

				case 1:
					// inline value

					if (mask < npos1) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), pos1, key1,
										val1, pos2, key2, val2);
						return Result.modified(thisNew);
					}

					else if (mask < pos2) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										mask, subNodeNew.headKey(), subNodeNew.headVal(), pos2,
										key2, val2);
						return Result.modified(thisNew);
					}

					else {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										pos2, key2, val2, mask, subNodeNew.headKey(),
										subNodeNew.headVal());
						return Result.modified(thisNew);
					}

				default:
					// modify current node (set replacement node)

					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2,
									key2, val2, mask, subNodeNew);

					return Result.modified(thisNew);
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return true;
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(entryOf(key2, val2));
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<AbstractNode<K, V>> nodeIterator() {
			return ArrayIterator.<AbstractNode<K, V>> of(new AbstractNode[] { node1 });
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
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2 });
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
		int size() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();
			result = prime * result + pos2;
			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();
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
			Value2Index1Node<?, ?> that = (Value2Index1Node<?, ?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (pos2 != that.pos2) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
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
			return String.format("[%s=%s, %s=%s, %s]", key1, val1, key2, val2, node1);
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		V headVal() {
			return val1;
		}

	}

	private static final class Value2Index2Node<K, V> extends CompactNode<K, V> {
		private final byte pos1;
		private final K key1;
		private final V val1;
		private final byte pos2;
		private final K key2;
		private final V val2;
		private final byte npos1;
		private final AbstractNode<K, V> node1;
		private final byte npos2;
		private final AbstractNode<K, V> node2;

		Value2Index2Node(AtomicReference<Thread> mutator, byte pos1, K key1, V val1, byte pos2,
						K key2, V val2, byte npos1, AbstractNode<K, V> node1, byte npos2,
						AbstractNode<K, V> node2) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;
			this.pos2 = pos2;
			this.key2 = key2;
			this.val2 = val2;
			this.npos1 = npos1;
			this.node1 = node1;
			this.npos2 = npos2;
			this.node2 = node2;
		}

		@Override
		Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (cmp.compare(val, val1) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, mask, key, val, pos2, key2, val2, npos1,
													node1, npos2, node2), val1);
				}

				// merge into node
				final AbstractNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos2, key2, val2, mask,
									node, npos1, node1, npos2, node2);
					return Result.modified(thisNew);
				}

				else if (mask < npos2) {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos2, key2, val2, npos1,
									node1, mask, node, npos2, node2);
					return Result.modified(thisNew);
				}

				else {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos2, key2, val2, npos1,
									node1, npos2, node2, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					if (cmp.compare(val, val2) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, pos1, key1, val1, mask, key, val, npos1,
													node1, npos2, node2), val2);
				}

				// merge into node
				final AbstractNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
									node, npos1, node1, npos2, node2);
					return Result.modified(thisNew);
				}

				else if (mask < npos2) {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
									node1, mask, node, npos2, node2);
					return Result.modified(thisNew);
				}

				else {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
									node1, npos2, node2, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == npos1) {
				final Result<K, V> subNodeResult = node1.updated(mutator, key, keyHash, val, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, mask, subNodeResult.getNode(), npos2, node2);

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			} else if (mask == npos2) {
				final Result<K, V> subNodeResult = node2.updated(mutator, key, keyHash, val, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, npos1, node1, mask, subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			}

			// no value

			if (mask < pos1) {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, pos1, key1,
								val1, pos2, key2, val2, npos1, node1, npos2, node2);
				return Result.modified(thisNew);
			}

			else if (mask < pos2) {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, key,
								val, pos2, key2, val2, npos1, node1, npos2, node2);
				return Result.modified(thisNew);
			}

			else {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, mask, key, val, npos1, node1, npos2, node2);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos2, key2, val2, npos1, node1, npos2,
								node2));
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1, npos2,
								node2));
			} else if (mask == npos1) {
				final Result<K, V> subNodeResult = node1.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				assert this.arity() >= 2;
				assert subNodeNew.size() >= 1;

				switch (subNodeNew.size()) {
				// case 0:
				// // remove node
				// return Result.modified(valNodeOf(mutator, pos1, key1, val1,
				// pos2, key2, val2, npos2, node2));

				case 1:
					// inline value

					if (mask < npos1) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), pos1, key1,
										val1, pos2, key2, val2, npos2, node2);
						return Result.modified(thisNew);
					}

					else if (mask < pos2) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										mask, subNodeNew.headKey(), subNodeNew.headVal(), pos2,
										key2, val2, npos2, node2);
						return Result.modified(thisNew);
					}

					else {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										pos2, key2, val2, mask, subNodeNew.headKey(),
										subNodeNew.headVal(), npos2, node2);
						return Result.modified(thisNew);
					}

				default:
					// modify current node (set replacement node)

					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2,
									key2, val2, mask, subNodeNew, npos2, node2);

					return Result.modified(thisNew);
				}
			} else if (mask == npos2) {
				final Result<K, V> subNodeResult = node2.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				assert this.arity() >= 2;
				assert subNodeNew.size() >= 1;

				switch (subNodeNew.size()) {
				// case 0:
				// // remove node
				// return Result.modified(valNodeOf(mutator, pos1, key1, val1,
				// pos2, key2, val2, npos1, node1));

				case 1:
					// inline value

					if (mask < npos1) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), pos1, key1,
										val1, pos2, key2, val2, npos1, node1);
						return Result.modified(thisNew);
					}

					else if (mask < pos2) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										mask, subNodeNew.headKey(), subNodeNew.headVal(), pos2,
										key2, val2, npos1, node1);
						return Result.modified(thisNew);
					}

					else {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										pos2, key2, val2, mask, subNodeNew.headKey(),
										subNodeNew.headVal(), npos1, node1);
						return Result.modified(thisNew);
					}

				default:
					// modify current node (set replacement node)

					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2,
									key2, val2, npos1, node1, mask, subNodeNew);

					return Result.modified(thisNew);
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return true;
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(entryOf(key2, val2));
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<AbstractNode<K, V>> nodeIterator() {
			return ArrayIterator.<AbstractNode<K, V>> of(new AbstractNode[] { node1, node2 });
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
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2 });
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
		int size() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();
			result = prime * result + pos2;
			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();
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
			Value2Index2Node<?, ?> that = (Value2Index2Node<?, ?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (pos2 != that.pos2) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
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
			return String.format("[%s=%s, %s=%s, %s, %s]", key1, val1, key2, val2, node1, node2);
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		V headVal() {
			return val1;
		}

	}

	private static final class Value3Index0Node<K, V> extends CompactNode<K, V> {
		private final byte pos1;
		private final K key1;
		private final V val1;
		private final byte pos2;
		private final K key2;
		private final V val2;
		private final byte pos3;
		private final K key3;
		private final V val3;

		Value3Index0Node(AtomicReference<Thread> mutator, byte pos1, K key1, V val1, byte pos2,
						K key2, V val2, byte pos3, K key3, V val3) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;
			this.pos2 = pos2;
			this.key2 = key2;
			this.val2 = val2;
			this.pos3 = pos3;
			this.key3 = key3;
			this.val3 = val3;
		}

		@Override
		Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (cmp.compare(val, val1) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, mask, key, val, pos2, key2, val2, pos3,
													key3, val3), val1);
				}

				// merge into node
				final AbstractNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos2, key2, val2, pos3, key3,
								val3, mask, node);
				return Result.modified(thisNew);

			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					if (cmp.compare(val, val2) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, pos1, key1, val1, mask, key, val, pos3,
													key3, val3), val2);
				}

				// merge into node
				final AbstractNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos3, key3,
								val3, mask, node);
				return Result.modified(thisNew);

			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					if (cmp.compare(val, val3) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, mask,
													key, val), val3);
				}

				// merge into node
				final AbstractNode<K, V> node = mergeNodes(key3, key3.hashCode(), val3, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, mask, node);
				return Result.modified(thisNew);

			}

			// no value

			if (mask < pos1) {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, pos1, key1,
								val1, pos2, key2, val2, pos3, key3, val3);
				return Result.modified(thisNew);
			}

			else if (mask < pos2) {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, key,
								val, pos2, key2, val2, pos3, key3, val3);
				return Result.modified(thisNew);
			}

			else if (mask < pos3) {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, mask, key, val, pos3, key3, val3);
				return Result.modified(thisNew);
			}

			else {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, pos3, key3, val3, mask, key, val);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos2, key2, val2, pos3, key3, val3));
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos3, key3, val3));
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2));
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
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
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(entryOf(key2, val2));
			} else if (mask == pos3 && cmp.compare(key, key3) == 0) {
				return Optional.of(entryOf(key3, val3));
			}

			return Optional.empty();
		}

		@Override
		Iterator<AbstractNode<K, V>> nodeIterator() {
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
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3 });
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
		int size() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();
			result = prime * result + pos2;
			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();
			result = prime * result + pos3;
			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();
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
			Value3Index0Node<?, ?> that = (Value3Index0Node<?, ?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (pos2 != that.pos2) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (pos3 != that.pos3) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s=%s, %s=%s, %s=%s]", key1, val1, key2, val2, key3, val3);
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		V headVal() {
			return val1;
		}

	}

	private static final class Value3Index1Node<K, V> extends CompactNode<K, V> {
		private final byte pos1;
		private final K key1;
		private final V val1;
		private final byte pos2;
		private final K key2;
		private final V val2;
		private final byte pos3;
		private final K key3;
		private final V val3;
		private final byte npos1;
		private final AbstractNode<K, V> node1;

		Value3Index1Node(AtomicReference<Thread> mutator, byte pos1, K key1, V val1, byte pos2,
						K key2, V val2, byte pos3, K key3, V val3, byte npos1,
						AbstractNode<K, V> node1) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;
			this.pos2 = pos2;
			this.key2 = key2;
			this.val2 = val2;
			this.pos3 = pos3;
			this.key3 = key3;
			this.val3 = val3;
			this.npos1 = npos1;
			this.node1 = node1;
		}

		@Override
		Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (cmp.compare(val, val1) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, mask, key, val, pos2, key2, val2, pos3,
													key3, val3, npos1, node1), val1);
				}

				// merge into node
				final AbstractNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos2, key2, val2, pos3,
									key3, val3, mask, node, npos1, node1);
					return Result.modified(thisNew);
				}

				else {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos2, key2, val2, pos3,
									key3, val3, npos1, node1, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					if (cmp.compare(val, val2) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, pos1, key1, val1, mask, key, val, pos3,
													key3, val3, npos1, node1), val2);
				}

				// merge into node
				final AbstractNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos3,
									key3, val3, mask, node, npos1, node1);
					return Result.modified(thisNew);
				}

				else {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos3,
									key3, val3, npos1, node1, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					if (cmp.compare(val, val3) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, mask,
													key, val, npos1, node1), val3);
				}

				// merge into node
				final AbstractNode<K, V> node = mergeNodes(key3, key3.hashCode(), val3, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				if (mask < npos1) {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2,
									key2, val2, mask, node, npos1, node1);
					return Result.modified(thisNew);
				}

				else {
					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2,
									key2, val2, npos1, node1, mask, node);
					return Result.modified(thisNew);
				}

			} else if (mask == npos1) {
				final Result<K, V> subNodeResult = node1.updated(mutator, key, keyHash, val, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, pos3, key3, val3, mask, subNodeResult.getNode());

				if (subNodeResult.hasReplacedValue())
					return Result.updated(thisNew, subNodeResult.getReplacedValue());

				return Result.modified(thisNew);
			}

			// no value

			if (mask < pos1) {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, pos1, key1,
								val1, pos2, key2, val2, pos3, key3, val3, npos1, node1);
				return Result.modified(thisNew);
			}

			else if (mask < pos2) {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, key,
								val, pos2, key2, val2, pos3, key3, val3, npos1, node1);
				return Result.modified(thisNew);
			}

			else if (mask < pos3) {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, mask, key, val, pos3, key3, val3, npos1, node1);
				return Result.modified(thisNew);
			}

			else {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, pos3, key3, val3, mask, key, val, npos1, node1);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos2, key2, val2, pos3, key3, val3,
								npos1, node1));
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos3, key3, val3,
								npos1, node1));
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
								npos1, node1));
			} else if (mask == npos1) {
				final Result<K, V> subNodeResult = node1.removed(mutator, key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();

				assert this.arity() >= 2;
				assert subNodeNew.size() >= 1;

				switch (subNodeNew.size()) {
				// case 0:
				// // remove node
				// return Result.modified(valNodeOf(mutator, pos1, key1, val1,
				// pos2, key2, val2, pos3, key3, val3));

				case 1:
					// inline value

					if (mask < npos1) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask,
										subNodeNew.headKey(), subNodeNew.headVal(), pos1, key1,
										val1, pos2, key2, val2, pos3, key3, val3);
						return Result.modified(thisNew);
					}

					else if (mask < pos2) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										mask, subNodeNew.headKey(), subNodeNew.headVal(), pos2,
										key2, val2, pos3, key3, val3);
						return Result.modified(thisNew);
					}

					else if (mask < pos3) {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										pos2, key2, val2, mask, subNodeNew.headKey(),
										subNodeNew.headVal(), pos3, key3, val3);
						return Result.modified(thisNew);
					}

					else {
						final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1,
										pos2, key2, val2, pos3, key3, val3, mask,
										subNodeNew.headKey(), subNodeNew.headVal());
						return Result.modified(thisNew);
					}

				default:
					// modify current node (set replacement node)

					final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2,
									key2, val2, pos3, key3, val3, mask, subNodeNew);

					return Result.modified(thisNew);
				}
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return true;
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return true;
			} else if (mask == pos3 && cmp.compare(key, key3) == 0) {
				return true;
			} else if (mask == npos1) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@Override
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(entryOf(key2, val2));
			} else if (mask == pos3 && cmp.compare(key, key3) == 0) {
				return Optional.of(entryOf(key3, val3));
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<AbstractNode<K, V>> nodeIterator() {
			return ArrayIterator.<AbstractNode<K, V>> of(new AbstractNode[] { node1 });
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
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3 });
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
		int size() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();
			result = prime * result + pos2;
			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();
			result = prime * result + pos3;
			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();
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
			Value3Index1Node<?, ?> that = (Value3Index1Node<?, ?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (pos2 != that.pos2) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (pos3 != that.pos3) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
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
			return String.format("[%s=%s, %s=%s, %s=%s, %s]", key1, val1, key2, val2, key3, val3,
							node1);
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		V headVal() {
			return val1;
		}

	}

	private static final class Value4Index0Node<K, V> extends CompactNode<K, V> {
		private final byte pos1;
		private final K key1;
		private final V val1;
		private final byte pos2;
		private final K key2;
		private final V val2;
		private final byte pos3;
		private final K key3;
		private final V val3;
		private final byte pos4;
		private final K key4;
		private final V val4;

		Value4Index0Node(AtomicReference<Thread> mutator, byte pos1, K key1, V val1, byte pos2,
						K key2, V val2, byte pos3, K key3, V val3, byte pos4, K key4, V val4) {
			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;
			this.pos2 = pos2;
			this.key2 = key2;
			this.val2 = val2;
			this.pos3 = pos3;
			this.key3 = key3;
			this.val3 = val3;
			this.pos4 = pos4;
			this.key4 = key4;
			this.val4 = val4;
		}

		@Override
		Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (cmp.compare(val, val1) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, mask, key, val, pos2, key2, val2, pos3,
													key3, val3, pos4, key4, val4), val1);
				}

				// merge into node
				final AbstractNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos2, key2, val2, pos3, key3,
								val3, pos4, key4, val4, mask, node);
				return Result.modified(thisNew);

			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					if (cmp.compare(val, val2) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, pos1, key1, val1, mask, key, val, pos3,
													key3, val3, pos4, key4, val4), val2);
				}

				// merge into node
				final AbstractNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos3, key3,
								val3, pos4, key4, val4, mask, node);
				return Result.modified(thisNew);

			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					if (cmp.compare(val, val3) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, mask,
													key, val, pos4, key4, val4), val3);
				}

				// merge into node
				final AbstractNode<K, V> node = mergeNodes(key3, key3.hashCode(), val3, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, pos4, key4, val4, mask, node);
				return Result.modified(thisNew);

			} else if (mask == pos4) {
				if (cmp.compare(key, key4) == 0) {
					if (cmp.compare(val, val4) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					return Result.updated(
									valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3,
													key3, val3, mask, key, val), val4);
				}

				// merge into node
				final AbstractNode<K, V> node = mergeNodes(key4, key4.hashCode(), val4, key,
								keyHash, val, shift + BIT_PARTITION_SIZE);

				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, pos3, key3, val3, mask, node);
				return Result.modified(thisNew);

			}

			// no value

			if (mask < pos1) {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, mask, key, val, pos1, key1,
								val1, pos2, key2, val2, pos3, key3, val3, pos4, key4, val4);
				return Result.modified(thisNew);
			}

			else if (mask < pos2) {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask, key,
								val, pos2, key2, val2, pos3, key3, val3, pos4, key4, val4);
				return Result.modified(thisNew);
			}

			else if (mask < pos3) {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, mask, key, val, pos3, key3, val3, pos4, key4, val4);
				return Result.modified(thisNew);
			}

			else if (mask < pos4) {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, pos3, key3, val3, mask, key, val, pos4, key4, val4);
				return Result.modified(thisNew);
			}

			else {
				final AbstractNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2, key2,
								val2, pos3, key3, val3, pos4, key4, val4, mask, key, val);
				return Result.modified(thisNew);
			}

		}

		@Override
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1) {
				if (cmp.compare(key, key1) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos2, key2, val2, pos3, key3, val3, pos4,
								key4, val4));
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos3, key3, val3, pos4,
								key4, val4));
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos4,
								key4, val4));
			} else if (mask == pos4) {
				if (cmp.compare(key, key4) != 0) {
					return Result.unchanged(this);
				}

				// remove pair
				return Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3,
								key3, val3));
			}

			return Result.unchanged(this);
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
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
		Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(entryOf(key2, val2));
			} else if (mask == pos3 && cmp.compare(key, key3) == 0) {
				return Optional.of(entryOf(key3, val3));
			} else if (mask == pos4 && cmp.compare(key, key4) == 0) {
				return Optional.of(entryOf(key4, val4));
			}

			return Optional.empty();
		}

		@Override
		Iterator<AbstractNode<K, V>> nodeIterator() {
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
		SupplierIterator<K, V> valueIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3,
							key4, val4 });
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
		int size() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pos1;
			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();
			result = prime * result + pos2;
			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();
			result = prime * result + pos3;
			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();
			result = prime * result + pos4;
			result = prime * result + key4.hashCode();
			result = prime * result + val4.hashCode();
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
			Value4Index0Node<?, ?> that = (Value4Index0Node<?, ?>) other;

			if (pos1 != that.pos1) {
				return false;
			}
			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (pos2 != that.pos2) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (pos3 != that.pos3) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
				return false;
			}
			if (pos4 != that.pos4) {
				return false;
			}
			if (!key4.equals(that.key4)) {
				return false;
			}
			if (!val4.equals(that.val4)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("[%s=%s, %s=%s, %s=%s, %s=%s]", key1, val1, key2, val2, key3,
							val3, key4, val4);
		}

		@Override
		K headKey() {
			return key1;
		}

		@Override
		V headVal() {
			return val1;
		}

	}

}
