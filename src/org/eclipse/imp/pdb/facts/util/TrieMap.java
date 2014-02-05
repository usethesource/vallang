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

import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("rawtypes")
public class TrieMap<K,V> extends AbstractImmutableMap<K,V> {

	@SuppressWarnings("unchecked")
	private static final TrieMap EMPTY = new TrieMap(AbstractNode.EMPTY_NODE);
	
	final private AbstractNode<K,V> rootNode;
	
	TrieMap(AbstractNode<K,V> rootNode) {
		this.rootNode = rootNode;
	}	
	
	@SuppressWarnings("unchecked")
	public static final <K,V> ImmutableMap<K,V> of() {
		return TrieMap.EMPTY;
	}	
	
	@SuppressWarnings("unchecked")
	public static final <K,V> TransientMap<K,V> transientOf() {
		return TrieMap.EMPTY.asTransient();
	}	
		
	@SuppressWarnings("unchecked")
	protected static final <K> Comparator<K> equalityComparator() {
		return EqualityUtils.getDefaultEqualityComparator();
	}
	
	private boolean invarint() {		
//		int _hash = 0; 
//		for (K key : this) {
//			_hash += key.hashCode();
//		}
//		return this.hashCode == _hash;
		return false;
	}
	
	@Override
	public TrieMap<K,V> __put(K k, V v) {
		return __putEquivalent(k, v, equalityComparator());
	}

	@Override
	public TrieMap<K,V> __putEquivalent(K k, V v, Comparator<Object> cmp) {
		AbstractNode.Result<K,V> result = rootNode.updated(k, k.hashCode(), v, 0, cmp);
		return (result.isModified()) ? new TrieMap<K,V>(result.getNode()) : this;
	}
	
	@Override
	public ImmutableMap<K,V> __putAll(Map<? extends K, ? extends V> map) {
		return __putAllEquivalent(map, equalityComparator());
	}	

	@Override
	public ImmutableMap<K,V> __putAllEquivalent(Map<? extends K, ? extends V> map, Comparator<Object> cmp) {
		TransientMap<K,V> tmp = asTransient(); 		
		tmp.__putAllEquivalent(map, cmp);		
		return tmp.freeze();
	}
	
	@Override
	public TrieMap<K,V> __remove(K k) {
		return __removeEquivalent(k, equalityComparator());
	}

	@Override
	public TrieMap<K,V> __removeEquivalent(K k, Comparator<Object> cmp) {
		AbstractNode.Result<K,V> result = rootNode.removed(k, k.hashCode(), 0, cmp);
		return (result.isModified()) ? new TrieMap<K,V>(result.getNode()) : this;
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
		AbstractNode.Optional<Map.Entry<K,V>> result = rootNode.findByKey(key, key.hashCode(), 0, cmp);
		
		if (result.isPresent()) {
			return result.get().getValue();
		} else {
			return null;
		}
	}	

	@Override
	public int size() {
		return rootNode.size();
	}

	@Override
	public Iterator<Map.Entry<K, V>> entryIterator() {
		return new TrieMapEntryIterator(rootNode);
	}

	@Override
	public Iterator<K> keyIterator() {
		return new TrieMapKeyIterator(rootNode);
	}
	
	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		Set<java.util.Map.Entry<K, V>> entrySet = null;
		
		if (entrySet == null) {
            entrySet = new AbstractSet<java.util.Map.Entry<K, V>>() {
                public Iterator<java.util.Map.Entry<K, V>> iterator() {
                    return new Iterator<Entry<K,V>>() {
                        private final Iterator<Entry<K,V>> i = entryIterator();

						public boolean hasNext() {
							return i.hasNext();
						}

						public Entry<K,V> next() {
							return i.next();
						}

						public void remove() {
							i.remove();
						}
                    };
                }

                public int size() {
                    return TrieMap.this.size();
                }

                public boolean isEmpty() {
                    return TrieMap.this.isEmpty();
                }

                public void clear() {
                    TrieMap.this.clear();
                }

                public boolean contains(Object k) {
                    return TrieMap.this.containsKey(k);
                }
            };
        }
        return entrySet;	
	}	
		
	/**
	 * Iterator that first iterates over inlined-values and then
	 * continues depth first recursively.
	 */
	private static class TrieMapEntryIterator implements Iterator {

		final Deque<Iterator<AbstractNode>> nodeIterationStack;
		AbstractNode next;
		
		TrieMapEntryIterator(AbstractNode rootNode) {			
			next = null;
			nodeIterationStack = new ArrayDeque<>();
			
			if (rootNode.isLeaf()) {
				next = rootNode;
			} else if (rootNode.size() > 0) {
				nodeIterationStack.push(rootNode.nodeIterator());
			}
		}

		@Override
		public boolean hasNext() {
			while (true) {
				if (next != null) {
					return true;
				} else {					
					if (nodeIterationStack.isEmpty()) {
						return false;
					} else {
						if (nodeIterationStack.peek().hasNext()) {
							AbstractNode innerNode = nodeIterationStack.peek().next();						
							
							if (innerNode.isLeaf())
								next = innerNode;
							else if (innerNode.size() > 0) {
								nodeIterationStack.push(innerNode.nodeIterator());
							}
							continue;
						} else {
							nodeIterationStack.pop();
							continue;
						}
					}
				}
			}
		}

		@Override
		public Object next() {
			if (!hasNext()) throw new NoSuchElementException();
	
			Object result = next;
			next = null;
			
			return result;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private static class TrieMapKeyIterator extends TrieMapEntryIterator {
		TrieMapKeyIterator(AbstractNode rootNode) {
			super(rootNode);
		}
		
		@Override
		public Object next() {
			final Map.Entry result = (Map.Entry) super.next();
			return result.getKey();			
		}
	}

	@Override
	public boolean isTransientSupported() {
		return true;
	}

	@Override
	public TransientMap<K,V> asTransient() {
		return new TransientTrieMap<K,V>(this);
	}
	
	static final class TransientTrieMap<E,V> implements TransientMap<E,V> {		
		final private AtomicReference<Thread> mutator;		
		private AbstractNode<E,V> rootNode;
				
		TransientTrieMap(TrieMap<E,V> trieMap) {
			this.mutator = new AtomicReference<Thread>(Thread.currentThread());
			this.rootNode = trieMap.rootNode;
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
			AbstractNode.Optional<Map.Entry<E,V>> result = rootNode.findByKey(key, key.hashCode(), 0, cmp);
			
			if (result.isPresent()) {
				return result.get().getValue();
			} else {
				return null;
			}
		}		
		
		@Override
		public V __put(E e, V v) {
			return __putEquivalent(e, v, equalityComparator());
		}

		@Override
		public V __putEquivalent(E e, V v, Comparator<Object> cmp) {
			if (mutator.get() == null)
				throw new IllegalStateException("Transient already frozen.");
			
			AbstractNode.Result<E,V> result = rootNode.updated(mutator, e, e.hashCode(), v, 0, cmp);

			if (result.isModified()) {
				rootNode = result.getNode();
				
				if (result.hasReplacedValue()) {
					return result.getReplacedValue();
				}
			}
			
			return null; // either not modified or key-value pair inserted 
		}

		@Override
		public boolean __remove(E e) {
			return __removeEquivalent(e, equalityComparator());
		}

		@Override
		public boolean __removeEquivalent(E e, Comparator<Object> cmp) {
			if (mutator.get() == null)
				throw new IllegalStateException("Transient already frozen.");
			
			AbstractNode.Result<E,V> result = rootNode.removed(mutator, (E) e, e.hashCode(), 0, cmp);

			if (result.isModified()) {
				rootNode = result.getNode();
				return true;
			} else {
				return false;
			}
		}

		@Override
		public boolean __putAll(Map<? extends E, ? extends V> map) {
			return __putAllEquivalent(map, equalityComparator());
		}

		@Override
		public boolean __putAllEquivalent(Map<? extends E, ? extends V> map,
				Comparator<Object> cmp) {
			boolean modified = false;
			
			for (Entry<? extends E, ? extends V> entry : map.entrySet()) {
				final boolean isPresent = containsKeyEquivalent(entry.getKey(), cmp);
				final V replaced = __putEquivalent(entry.getKey(), entry.getValue(), cmp);
				
				if (!isPresent || replaced != null) {
					modified = true;
				}
			}
			
			return modified;	
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
		public ImmutableMap<E,V> freeze() {
			if (mutator.get() == null)
				throw new IllegalStateException("Transient already frozen.");
			
			mutator.set(null);
			return new TrieMap<E,V>(rootNode);
		}		
	}

	private static abstract class AbstractNode<K,V> {

		protected static final int BIT_PARTITION_SIZE = 5;
		protected static final int BIT_PARTITION_MASK = 0x1f;
		
		@SuppressWarnings("unchecked")
		
		// TODO: ensure proper separation between index and inplaceIndex nodes		
		protected static final AbstractNode EMPTY_NODE = null; // new IndexNode(0, new AbstractNode[0], 0);
		
		abstract boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> comparator);
		
		abstract Optional<Map.Entry<K,V>> findByKey(Object key, int hash, int shift, Comparator<Object> cmp);
		
		abstract Result<K,V> updated(K key, int keyHash, V val, int shift, Comparator<Object> cmp);
		
		abstract Result<K,V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift, Comparator<Object> cmp);
		
		abstract Result<K,V> removed(K key, int hash, int shift, Comparator<Object> comparator);
		
		abstract Result<K,V> removed(AtomicReference<Thread> mutator, K key, int hash, int shift, Comparator<Object> comparator);	
		
		abstract boolean isLeaf();
		
		abstract boolean hasValues();

		abstract Iterator<K> valueIterator();

		abstract int valueArity();

		abstract boolean hasNodes();

		abstract Iterator<AbstractNode<K, V>> nodeIterator();

		abstract int nodeArity();
		
		abstract K getValue(int index);
		
		abstract public AbstractNode<K, V> getNode(int index);
			
		/**
		 * The arity of this trie node (i.e. number of values and nodes stored on this level).
		 * @return sum of nodes and values stored within
		 */
		abstract int arity();
		
		/**
		 * The total number of elements contained by this (sub)tree.
		 * @return element count
		 */
		abstract int size();

		/**
		 * Returns the first value stored within this node.
		 * 
		 * @return first value
		 */
		abstract K head(); // TODO: pair!
		
		boolean nodeInvariant() {
			return (size() - valueArity() >= 2 * (arity() - valueArity()));
		}
		
		static <K, V> AbstractNode<K, V>[] arraycopyAndSet(AbstractNode<K, V>[] array, int index, AbstractNode<K, V> elementNew) {
			final AbstractNode<K, V>[] arrayNew = new AbstractNode[array.length];
			System.arraycopy(array, 0, arrayNew, 0, array.length);
			arrayNew[index] = elementNew;
			return arrayNew;
		}

		static <K, V> AbstractNode<K, V>[] arraycopyAndInsert(AbstractNode<K, V>[] array, int index, AbstractNode<K, V> elementNew) {
			final AbstractNode<K, V>[] arrayNew = new AbstractNode[array.length + 1];
			System.arraycopy(array, 0, arrayNew, 0, index);
			arrayNew[index] = elementNew;
			System.arraycopy(array, index, arrayNew, index + 1, array.length - index);
			return arrayNew;
		}

		static <K, V> AbstractNode<K, V>[] arraycopyAndRemove(AbstractNode<K, V>[] array, int index) {
			final AbstractNode<K, V>[] arrayNew = new AbstractNode[array.length - 1];
			System.arraycopy(array, 0, arrayNew, 0, index);
			System.arraycopy(array, index + 1, arrayNew, index, array.length - index - 1);
			return arrayNew;
		}	
		
		static <K,V> AbstractNode<K,V> mergeNodes(AbstractNode<K, V> node0, int hash0, AbstractNode<K, V> node1, int hash1, int shift) {
			final int mask0 = (hash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (hash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level
				final int bitmap = (1 << mask0) | (1 << mask1);			
				final AbstractNode<K, V>[] nodes = new AbstractNode[2];

				if (mask0 < mask1) {
					nodes[0] = node0;
					nodes[1] = node1;
				} else {
					nodes[0] = node1;
					nodes[1] = node0;
				}

				return new IndexNode<>(bitmap, nodes, node0.size() + node1.size());
			} else {
				// values fit on next level
				final int bitmap = (1 << mask0);
				final AbstractNode<K, V> node = mergeNodes(node0, hash0, node1, hash1, shift + BIT_PARTITION_SIZE);

				return new IndexNode<>(bitmap, node, node.size());
			}
		}
		
		static class Result<T1,T2> {
			private final Object result;
			private final T2 replacedValue;
			private final boolean isModified;

			// update: inserted/removed single element, element count changed
			public static <T1,T2> Result<T1, T2> modified(AbstractNode<T1,T2> node) {
				return new Result<>(node, null, true);
			}
			
			// update: replaced single mapping, but element count unchanged
			public static <T1,T2> Result<T1, T2> updated(AbstractNode<T1,T2> node, T2 replacedValue) {
				return new Result<>(node, replacedValue, true);
			}			
			
			// update: neither element, nor element count changed
			public static <T1,T2> Result<T1, T2> unchanged(AbstractNode<T1,T2> node) {
				return new Result<>(node, null, false);
			}
			
			private Result(AbstractNode<T1,T2> node, T2 replacedValue, boolean isMutated) {
				this.result = node;
				this.replacedValue = replacedValue;
				this.isModified = isMutated;
			}
			
			@SuppressWarnings("unchecked")
			public AbstractNode<T1,T2> getNode() {
				return (AbstractNode<T1,T2>) result;
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
	
	private static final class InplaceIndexNode<K, V> extends AbstractNode<K, V> {
		private AtomicReference<Thread> mutator;

		private int bitmap;
		private int valmap;
		private Object[] nodes;
		private int cachedSize;
		private int cachedValmapBitCount;

		InplaceIndexNode(AtomicReference<Thread> mutator, int bitmap, int valmap, Object[] nodes, int cachedSize) {
			assert (Integer.bitCount(bitmap) == nodes.length);

			this.mutator = mutator;

			this.bitmap = bitmap;
			this.valmap = valmap;
			this.nodes = nodes;
			this.cachedSize = cachedSize;

			this.cachedValmapBitCount = Integer.bitCount(valmap);
		
			// assert invariant
			assert nodeInvariant();
		}

		InplaceIndexNode(AtomicReference<Thread> mutator, Object[] nodes) {
			this.mutator = mutator;
			this.nodes = nodes;
		}

		InplaceIndexNode(int bitmap, int valmap, Object[] nodes, int cachedSize) {
			this(null, bitmap, valmap, nodes, cachedSize);
		}

		InplaceIndexNode(int bitmap, int valmap, Object node, int cachedSize) {
			this(bitmap, valmap, new Object[] { node }, cachedSize);
		}
		
		final int bitIndex(int bitpos) {
			return cachedValmapBitCount + Integer.bitCount((bitmap ^ valmap) & (bitpos - 1));
		}

		final int valIndex(int bitpos) {
			return Integer.bitCount(valmap & (bitpos - 1));
		}

		private void updateMetadata(int bitmap, int valmap, int cachedSize, int cachedValmapBitCount) {
			assert (Integer.bitCount(bitmap) == nodes.length);

			this.bitmap = bitmap;
			this.valmap = valmap;
			this.cachedSize = cachedSize;
			this.cachedValmapBitCount = cachedValmapBitCount;
			
			// assert invariant
			assert nodeInvariant();
		}

		@Override
		public boolean containsKey(Object key, int hash, int shift, Comparator<Object> cmp) {
			final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0)
				return cmp.compare(nodes[valIndex(bitpos)], key) == 0;

			if ((bitmap & bitpos) != 0)
				return ((AbstractNode<K, V>) nodes[bitIndex(bitpos)]).containsKey(key, hash,
						shift + BIT_PARTITION_SIZE, cmp);

			return false;
		}

		@Override
		public Result<K, V> updated(K key, int keyHash, V val, int shift, Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (cmp.compare(nodes[valIndex], key) == 0)
					return Result.unchanged(this);

				final AbstractNode<K, V> nodeNew = mergeNodes(nodes[valIndex], nodes[valIndex].hashCode(), key, keyHash,
						shift + BIT_PARTITION_SIZE);

				final int offset = cachedValmapBitCount - 1;
				final int index = Integer.bitCount((bitmap ^ (valmap & ~bitpos)) & (bitpos - 1));
				final Object[] nodesNew = copyAndMoveToBack(nodes, valIndex, offset + index, nodeNew);

				return Result.modified(new InplaceIndexNode<K, V>(bitmap, valmap & ~bitpos, nodesNew, cachedSize + 1));
			}

			if ((bitmap & bitpos) != 0) { // node (not value)
				final int bitIndex = bitIndex(bitpos);

				final AbstractNode<K, V> subNode = (AbstractNode<K, V>) nodes[bitIndex];
				final Result<K, V> subNodeResult = subNode.updated(key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final Object[] nodesNew = copyAndSet(nodes, bitIndex, subNodeResult.getNode());
				return Result.modified(new InplaceIndexNode<K, V>(bitmap, valmap, nodesNew, cachedSize + 1));
			}

			// no value
			Object[] nodesNew = copyAndInsert(nodes, valIndex(bitpos), key);
			return Result.modified(new InplaceIndexNode<K, V>(bitmap | bitpos, valmap | bitpos, nodesNew, cachedSize + 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		public Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift,
				Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (cmp.compare(nodes[valIndex], key) == 0)
					return Result.unchanged(this);

				final AbstractNode<K, V> nodeNew = mergeNodes(nodes[valIndex], nodes[valIndex].hashCode(), key, keyHash,
						shift + BIT_PARTITION_SIZE);

				final int offset = cachedValmapBitCount - 1;
				final int index = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos)) & (bitpos - 1));

				InplaceIndexNode<K, V> editableNode = editAndMoveToBack(mutator, valIndex, offset + index, nodeNew);
				editableNode
						.updateMetadata(bitmap | bitpos, valmap & ~bitpos, cachedSize + 1, cachedValmapBitCount - 1);
				return Result.modified(editableNode);
			}

			if ((bitmap & bitpos) != 0) { // node (not value)
				final int bitIndex = bitIndex(bitpos);
				final AbstractNode<K, V> subNode = (AbstractNode<K, V>) nodes[bitIndex];

				final Result<K, V> subNodeResult = subNode.updated(mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE,
						cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				InplaceIndexNode<K, V> editableNode = editAndSet(mutator, bitIndex, subNodeResult.getNode());
				editableNode.updateMetadata(bitmap, valmap, cachedSize + 1, cachedValmapBitCount);
				return Result.modified(editableNode);
			}

			// no value
			InplaceIndexNode<K, V> editableNode = editAndInsert(mutator, valIndex(bitpos), key);
			editableNode.updateMetadata(bitmap | bitpos, valmap | bitpos, cachedSize + 1, cachedValmapBitCount + 1);
			return Result.modified(editableNode);
		}

		InplaceIndexNode<K, V> editAndInsert(AtomicReference<Thread> mutator, int index, Object elementNew) {
			Object[] editableNodes = copyAndInsert(this.nodes, index, elementNew);

			if (this.mutator == mutator) {
				this.nodes = editableNodes;
				return this;
			} else {
				return new InplaceIndexNode<>(mutator, editableNodes);
			}
		}

		InplaceIndexNode<K, V> editAndRemove(AtomicReference<Thread> mutator, int index) {
			Object[] editableNodes = copyAndRemove(this.nodes, index);

			if (this.mutator == mutator) {
				this.nodes = editableNodes;
				return this;
			} else {
				return new InplaceIndexNode<>(mutator, editableNodes);
			}
		}

		InplaceIndexNode<K, V> editAndSet(AtomicReference<Thread> mutator, int index, Object elementNew) {
			if (this.mutator == mutator) {
				// no copying if already editable
				this.nodes[index] = elementNew;
				return this;
			} else {
				final Object[] editableNodes = copyAndSet(this.nodes, index, elementNew);
				return new InplaceIndexNode<>(mutator, editableNodes);
			}
		}

		InplaceIndexNode<K, V> editAndMoveToBack(AtomicReference<Thread> mutator, int indexOld, int indexNew,
				Object elementNew) {
			Object[] editableNodes = copyAndMoveToBack(this.nodes, indexOld, indexNew, elementNew);

			if (this.mutator == mutator) {
				this.nodes = editableNodes;
				return this;
			} else {
				return new InplaceIndexNode<>(mutator, editableNodes);
			}
		}

		InplaceIndexNode<K, V> editAndMoveToFront(AtomicReference<Thread> mutator, int indexOld, int indexNew,
				Object elementNew) {
			Object[] editableNodes = copyAndMoveToFront(this.nodes, indexOld, indexNew, elementNew);

			if (this.mutator == mutator) {
				this.nodes = editableNodes;
				return this;
			} else {
				return new InplaceIndexNode<>(mutator, editableNodes);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public Result<K, V> removed(K key, int keyHash, int shift, Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (cmp.compare(nodes[valIndex], key) != 0)
					return Result.unchanged(this);

				if (this.arity() == 1) {
					return Result.modified(EMPTY_NODE);
				} else if (this.arity() == 2 && valmap == bitmap) {
					/*
					 * Create root node with singleton element. This node will
					 * be a) either be the new root returned, or b) unwrapped
					 * and inlined.
					 */
					final K theOtherKey = (K) ((valIndex == 0) ? nodes[1] : nodes[0]);
					final K theOtherVal = null;
					return EMPTY_NODE.updated(theOtherKey, theOtherKey.hashCode(), theOtherVal, 0, cmp);
				} else {
					final Object[] nodesNew = copyAndRemove(nodes, valIndex);
					return Result.modified(new InplaceIndexNode<K, V>(bitmap & ~bitpos, valmap & ~bitpos, nodesNew,
							cachedSize - 1));
				}
			}

			if ((bitmap & bitpos) != 0) { // node (not value)
				final int bitIndex = bitIndex(bitpos);
				final AbstractNode<K, V> subNode = (AbstractNode<K, V>) nodes[bitIndex];
				final Result<K, V> subNodeResult = subNode.removed(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> subNodeNew = subNodeResult.getNode();

				if (this.arity() == 1) {
					switch (subNodeNew.size()) {
					case 0:
					case 1:
						// escalate (singleton or empty) result
						return subNodeResult;

					default:
						// modify current node (set replacement node)
						final Object[] nodesNew = copyAndSet(nodes, bitIndex, subNodeNew);
						return Result.modified(new InplaceIndexNode<K, V>(bitmap, valmap, nodesNew, cachedSize - 1));
					}
				} else {
					assert this.arity() >= 2;

					switch (subNodeNew.size()) {
					case 0:
						// remove node
						final Object[] nodesNew0 = copyAndRemove(nodes, bitIndex);
						return Result.modified(new InplaceIndexNode<K, V>(bitmap & ~bitpos, valmap, nodesNew0,
								cachedSize - 1));

					case 1:
						// inline value (move to front)
						final int valIndexNew = Integer.bitCount((valmap | bitpos) & (bitpos - 1));

						final Object[] nodesNew1 = copyAndMoveToFront(nodes, bitIndex, valIndexNew, subNodeNew.head());
						return Result.modified(new InplaceIndexNode<K, V>(bitmap, valmap | bitpos, nodesNew1,
								cachedSize - 1));

					default:
						// modify current node (set replacement node)
						final Object[] nodesNew = copyAndSet(nodes, bitIndex, subNodeNew);
						return Result.modified(new InplaceIndexNode<K, V>(bitmap, valmap, nodesNew, cachedSize - 1));
					}
				}
			}

			return Result.unchanged(this);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Result<K, V> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift,
				Comparator<Object> comparator) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (comparator.compare(nodes[valIndex], key) != 0)
					return Result.unchanged(this);

				if (this.arity() == 1) {
					return Result.modified(EMPTY_NODE);
				} else if (this.arity() == 2 && valmap == bitmap) {
					/*
					 * Create root node with singleton element. This node will
					 * be a) either be the new root returned, or b) unwrapped
					 * and inlined.
					 */
					final K theOtherKey = (K) ((valIndex == 0) ? nodes[1] : nodes[0]);
					final K theOtherVal = null;
					return EMPTY_NODE.updated(theOtherKey, theOtherKey.hashCode(), theOtherVal, 0, comparator);
				} else {
					InplaceIndexNode<K, V> editableNode = editAndRemove(mutator, valIndex);
					editableNode.updateMetadata(this.bitmap & ~bitpos, this.valmap & ~bitpos, cachedSize - 1,
							cachedValmapBitCount - 1);
					return Result.modified(editableNode);
				}
			}

			if ((bitmap & bitpos) != 0) { // node (not value)
				final int bitIndex = bitIndex(bitpos);
				final AbstractNode<K, V> subNode = (AbstractNode<K, V>) nodes[bitIndex];
				final Result<K, V> subNodeResult = subNode.removed(key, keyHash, shift + BIT_PARTITION_SIZE, comparator);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				final AbstractNode<K, V> subNodeNew = subNodeResult.getNode();

				if (this.arity() == 1) {
					switch (subNodeNew.size()) {
					case 0:
					case 1:
						// escalate (singleton or empty) result
						return subNodeResult;

					default:
						// modify current node (set replacement node)
						InplaceIndexNode<K, V> editableNode = editAndSet(mutator, bitIndex, subNodeNew);
						editableNode.updateMetadata(bitmap, valmap, cachedSize - 1, cachedValmapBitCount);
						return Result.modified(editableNode);
					}
				} else {
					assert this.arity() >= 2;

					switch (subNodeNew.size()) {
					case 0:
						// remove node
						InplaceIndexNode<K, V> editableNode0 = editAndRemove(mutator, bitIndex);
						editableNode0.updateMetadata(bitmap & ~bitpos, valmap, cachedSize - 1, cachedValmapBitCount);
						return Result.modified(editableNode0);

					case 1:
						// inline value (move to front)
						final int valIndexNew = Integer.bitCount((valmap | bitpos) & (bitpos - 1));

						InplaceIndexNode<K, V> editableNode1 = editAndMoveToFront(mutator, bitIndex, valIndexNew,
								subNodeNew.head());
						editableNode1.updateMetadata(bitmap, valmap | bitpos, cachedSize - 1, cachedValmapBitCount + 1);
						return Result.modified(editableNode1);

					default:
						// modify current node (set replacement node)
						InplaceIndexNode<K, V> editableNode2 = editAndSet(mutator, bitIndex, subNodeNew);
						editableNode2.updateMetadata(bitmap, valmap, cachedSize - 1, cachedValmapBitCount);
						return Result.modified(editableNode2);
					}
				}
			}

			return Result.unchanged(this);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<K> valueIterator() {
			return (Iterator<K>) ArrayIterator.of(nodes, 0, cachedValmapBitCount);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		Iterator<AbstractNode<K, V>> nodeIterator() {
			return (Iterator) ArrayIterator.of(nodes, cachedValmapBitCount, nodes.length - cachedValmapBitCount);
		}

		@SuppressWarnings("unchecked")
		@Override
		K head() {
			assert hasValues();
			return (K) nodes[0];
		}

		@Override
		boolean hasValues() {
			return cachedValmapBitCount != 0;
		}

		@Override
		int valueArity() {
			return cachedValmapBitCount;
		}

		@Override
		boolean hasNodes() {
			return cachedValmapBitCount != nodes.length;
		}

		@Override
		int nodeArity() {
			return nodes.length - cachedValmapBitCount;
		}

//		@SuppressWarnings("unchecked")
//		@Override
//		K getValue(int index) {
//			return (K) nodes[index];
//		}
//
//		@SuppressWarnings("unchecked")
//		@Override
//		public AbstractNode<K, V> getNode(int index) {
//			return (AbstractNode<K, V>) nodes[cachedValmapBitCount + index];
//		}	
		
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
			InplaceIndexNode<?,?> that = (InplaceIndexNode<?,?>) other;
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
		int arity() {
			return nodes.length;
		}

		@Override
		int size() {
			return cachedSize;
		}
	}	
	
	private static final class InplaceHashCollisionNode<K, V> extends AbstractNode<K, V> {
		private final K[] keys;
		private final int hash;

		InplaceHashCollisionNode(int hash, K[] keys) {
			this.keys = keys;
			this.hash = hash;
		}

		@Override
		Iterator<K> valueIterator() {
			return ArrayIterator.of(keys);
		}

		@Override
		Iterator<AbstractNode<K, V>> nodeIterator() {
			return Collections.emptyIterator();
		}

		@Override
		K head() {
			assert hasValues();
			return keys[0];
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
		Result<K, V> updated(K key, int keyHash, V val, int shift, Comparator<Object> cmp) {
			if (this.hash != keyHash)
				return Result.modified(mergeNodes((AbstractNode<K, V>) this, this.hash, key, keyHash, shift));

			if (contains(key, keyHash, shift, cmp))
				return Result.unchanged(this);

			final K[] keysNew = (K[]) copyAndInsert(keys, keys.length, key);
			return Result.modified(new InplaceHashCollisionNode<>(keyHash, keysNew));
		}

		@Override
		Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift, Comparator<Object> cmp) {
			return updated(key, keyHash, val, shift, cmp);
		}

		/**
		 * Removes an object if present. Note, that this implementation always
		 * returns a new immutable {@link TrieSet} instance.
		 */
		@SuppressWarnings("unchecked")
		@Override
		Result<K, V> removed(K key, int keyHash, V val, int shift, Comparator<Object> cmp) {
			for (int i = 0; i < keys.length; i++) {
				if (cmp.compare(keys[i], key) == 0) {
					if (this.arity() == 1) {
						return Result.modified(EMPTY_NODE);
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node will
						 * be a) either be the new root returned, or b) unwrapped
						 * and inlined.
						 */
						final K theOther = (i == 0) ? keys[1] : keys[0];
						return EMPTY_NODE.updated(theOther, keyHash, 0, cmp);
					} else {
						return Result.modified(new InplaceHashCollisionNode<>(keyHash, (K[]) copyAndRemove(keys, i)));
					}
				}
			}
			return Result.unchanged(this);
		}

		@Override
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int hash, int shift, Comparator<Object> cmp) {
			return removed(key, hash, shift, cmp);
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
		K getValue(int index) {
			return keys[index];
		}
		
		@Override
		public AbstractNode<K, V> getNode(int index) {
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
			// TODO: steps by 2
			throw new RuntimeException();
			
			if (null == other) {
				return false;
			}
			if (this == other) {
				return true;
			}
			if (getClass() != other.getClass()) {
				return false;
			}
		
			InplaceHashCollisionNode<?,?> that = (InplaceHashCollisionNode<?,?>) other;
		
			if (hash != that.hash) {
				return false;
			}
			
			if (arity() != that.arity()) {
				return false;
			}
		
			/*
			 * Linear scan for each key, because of arbitrary element order.
			 */
			final Comparator<Object> cmp = equalityComparator();
			for (Iterator<?> it = that.valueIterator(); it.hasNext();) {
				final Object otherKey = it.next();
				
				for (Object key : keys) {
					if (cmp.compare(key, otherKey) == 0) {
						continue;
					}
				}
				return false;
			}
		
			return true;
		}

		@Override
		int arity() {
			return keys.length;
		}

		@Override
		int size() {
			return keys.length;
		}
	}
	
	private static final class IndexNode<K,V> extends AbstractNode<K,V> {

		private AtomicReference<Thread> mutator;

		private int bitmap;
		private AbstractNode<K, V>[] nodes;
		private int cachedSize;
			
		IndexNode(AtomicReference<Thread> mutator, int bitmap, AbstractNode<K, V>[] nodes, int cachedSize) {
			assert (Integer.bitCount(bitmap) == nodes.length);

			this.mutator = mutator;			
			this.bitmap = bitmap;
			this.nodes = nodes;
			this.cachedSize = cachedSize;
		}
			
		IndexNode(AtomicReference<Thread> mutator, AbstractNode<K, V>[] nodes) {
			this.mutator = mutator;
			this.nodes = nodes;
		}
		
		IndexNode(int bitmap, AbstractNode<K, V>[] nodes, int cachedSize) {
			this(null, bitmap, nodes, cachedSize);
		}

		IndexNode(int bitmap, AbstractNode<K, V> node, int cachedSize) {
			this(bitmap, new AbstractNode[] { node }, cachedSize);
		}

		final int index(int bitpos) {
			return Integer.bitCount(bitmap & (bitpos - 1));
		}
			
		private void updateMetadata(int bitmap, int cachedSize) {
			this.bitmap = bitmap;
			this.cachedSize = cachedSize; 
		}
		
		@Override
		public boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> comparator) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((bitmap & bitpos) != 0) {			
				return nodes[index(bitpos)].containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, comparator);
			} else {
				return false;
			}
		}
		
		Optional<Map.Entry<K,V>> findByKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((bitmap & bitpos) != 0) {			
				return nodes[index(bitpos)].findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else {
				return Optional.empty();
			}			
		}

		@Override
		public Result<K,V> updated(K key, int keyHash, V val, int shift, Comparator<Object> comparator) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);
			final int index = index(bitpos);

			if ((bitmap & bitpos) == 0) { // no value
				AbstractNode<K, V>[] nodesReplacement = arraycopyAndInsert(nodes, index, new LeafNode<K, V>(key, keyHash, val));
				return Result.modified(new IndexNode<>(bitmap | bitpos, nodesReplacement, cachedSize + 1));
			} else { // nested node
				final AbstractNode<K,V> subNode = (AbstractNode<K,V>) nodes[index];

				// immutable copy subNode
				final Result<K,V> subNodeResult = subNode.updated(key, keyHash, val, shift + BIT_PARTITION_SIZE, comparator);
				final AbstractNode<K,V> subNodeReplacement = subNodeResult.getNode();

				if (subNode == subNodeReplacement)
					return Result.unchanged(this);

				final AbstractNode<K, V>[] nodesReplacement = arraycopyAndSet(nodes, index, subNodeReplacement);
				
				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(new IndexNode<K, V>(bitmap, nodesReplacement, cachedSize), subNodeResult.getReplacedValue());					
				} else {
					return Result.modified(new IndexNode<K, V>(bitmap, nodesReplacement, cachedSize + 1));
				}
			}
		}
		
		IndexNode<K,V> editAndInsert(AtomicReference<Thread> mutator, int index, AbstractNode elementNew) {		
			AbstractNode<K, V>[] editableNodes = arraycopyAndInsert(this.nodes, index, elementNew);
			
			if (this.mutator == mutator) {
				this.nodes = editableNodes;
				return this;
			} else {
				return new IndexNode<>(mutator, editableNodes);
			}
		}
		
		IndexNode<K,V> editAndSet(AtomicReference<Thread> mutator, int index, AbstractNode<K, V> elementNew) {
			if (this.mutator == mutator) {
				nodes[index] = elementNew;
				return this;
			} else {
				final AbstractNode<K, V>[] editableNodes = arraycopyAndSet(this.nodes, index, elementNew);
				return new IndexNode<>(mutator, editableNodes);
			}
		}
		
		IndexNode<K,V> editAndRemove(AtomicReference<Thread> mutator, int index) {
			AbstractNode<K, V>[] editableNodes = arraycopyAndRemove(this.nodes, index);
			
			if (this.mutator == mutator) {
				this.nodes = editableNodes;
				return this;
			} else {
				return new IndexNode<K, V>(mutator, editableNodes);
			}
		}
		
		@Override
		public Result<K,V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift, Comparator<Object> comparator) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);
			final int index = index(bitpos);

			if ((bitmap & bitpos) == 0) { // no value
				IndexNode<K,V> editableNode = editAndInsert(mutator, index, new LeafNode<K, V>(key, keyHash, val));
				editableNode.updateMetadata(bitmap | bitpos, cachedSize + 1);			
				return Result.modified(editableNode);
			} else { // nested node
				AbstractNode<K,V> subNode = (AbstractNode<K,V>) nodes[index];
										
				Result<K, V> resultNode = subNode.updated(mutator, key,
						keyHash, val, shift + BIT_PARTITION_SIZE, comparator);
				
				if (resultNode.isModified()) {
					IndexNode<K,V> editableNode = editAndSet(mutator, index, resultNode.getNode());
					if (resultNode.hasReplacedValue()) {					
						editableNode.updateMetadata(bitmap, cachedSize);
						return Result.updated(editableNode, resultNode.getReplacedValue());
					} else {
						editableNode.updateMetadata(bitmap, cachedSize + 1);
						return Result.modified(editableNode);
					}
				} else {
					return Result.unchanged(subNode);
				}
			}
		}

		@Override
		public Result<K,V> removed(K key, int hash, int shift, Comparator<Object> comparator) {
			final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);
			final int bitIndex = index(bitpos);

			if ((bitmap & bitpos) == 0) { // no value
				return Result.unchanged(this);
			} else { // nested node
				final AbstractNode<K,V> subNode = (AbstractNode<K,V>) nodes[bitIndex];
				
				final Result<K,V> result = subNode.removed(key, hash, shift + BIT_PARTITION_SIZE, comparator);	
				
				if (!result.isModified())
					return Result.unchanged(this);

				// TODO: optimization if singleton element node is returned
				final AbstractNode<K,V> subNodeReplacement = result.getNode();
				
				if (this.arity() == 1 && (subNodeReplacement.size() == 0 || subNodeReplacement.size() == 1)) {
					// escalate (singleton or empty) result
					return result;
				} else {
					// modify current node (set replacement node)
					final AbstractNode<K,V>[] nodesReplacement = arraycopyAndSet(nodes, bitIndex, subNodeReplacement);
					return Result.modified(new IndexNode<>(bitmap, nodesReplacement, cachedSize - 1));
				}		
			}
		}
		
		@Override
		public Result<K,V> removed(AtomicReference<Thread> mutator, K key, int hash, int shift, Comparator<Object> comparator) {
			final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);
			final int index = index(bitpos);

			if ((bitmap & bitpos) == 0) { // no value
				return Result.unchanged(this);
			} else { // nested node
				final AbstractNode<K,V> subNode = (AbstractNode<K,V>) nodes[index];
		
				Result<K, V> resultNode = subNode.removed(mutator, key, hash,
						shift + BIT_PARTITION_SIZE, comparator);
				
				if (resultNode.isModified()) {
					IndexNode<K,V> editableNode = editAndSet(mutator, index, resultNode.getNode());
					editableNode.updateMetadata(bitmap, cachedSize - 1);
					return Result.modified(editableNode);
				} else {
					return Result.unchanged(subNode);
				}
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 0;
			result = prime * result + bitmap;
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
			IndexNode that = (IndexNode) other;
			if (cachedSize != that.cachedSize) {
				return false;
			}
			if (bitmap != that.bitmap) {
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
		int arity() {
			return nodes.length;
		}

		@Override
		int size() {
			return cachedSize;
		}

		@Override
		boolean isLeaf() {
			return false;
		}

		@Override
		Iterator<AbstractNode<K,V>> nodeIterator() {
			return ArrayIterator.of(nodes);
		}

		@Override
		boolean hasValues() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		Iterator<K> valueIterator() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		int valueArity() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		boolean hasNodes() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		int nodeArity() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		K getValue(int index) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public AbstractNode<K, V> getNode(int index) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		K head() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	private static final class LeafNode<K, V> extends AbstractNode<K, V> implements Map.Entry<K, V> {

		private final K key;
		private final V val;
		private final int keyHash;

		LeafNode(K key, int keyHash, V val) {
			this.key = key;
			this.val = val;
			this.keyHash = keyHash;
		}

		@Override
		Result<K,V> updated(K key, int keyHash, V val, int shift, Comparator<Object> cmp) {
			if (this.keyHash != keyHash)
				// insert (no collision)
				return Result.modified(mergeNodes(this, this.keyHash, new LeafNode<K, V>(key, keyHash, val), keyHash, shift));

			if (cmp.compare(this.key, key) != 0)
				// insert (hash collision)
				return Result.modified(new LeafHashCollisionNode<K, V>(keyHash, new LeafNode[]{this, new LeafNode<K, V>(key, keyHash, val)}));
			
			if (cmp.compare(this.val, val) != 0)
				// value replaced
				return Result.updated(new LeafNode<K, V>(key, keyHash, val), val);
			
			return Result.unchanged(this);
		}

		@Override
		Result<K,V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift, Comparator<Object> cmp) {
			return updated(key, keyHash, val, shift, cmp);
		}
		
		@Override
		Result<K,V> removed(K key, int hash, int shift, Comparator<Object> cmp) {
			if (cmp.compare(this.key, key) == 0) {
				return Result.modified(EMPTY_NODE);
			} else {
				return Result.unchanged(this);
			}
		}
		
		@Override
		Result<K,V> removed(AtomicReference<Thread> mutator, K key, int hash, int shift, Comparator<Object> cmp) {
			return removed(key, hash, shift, cmp);
		}
		
		@Override
		boolean containsKey(Object key, int hash, int shift, Comparator<Object> cmp) {
			return this.keyHash == hash
					&& cmp.compare(this.key, key) == 0;
		}
		
		@Override
		Optional<Map.Entry<K,V>> findByKey(Object key, int hash, int shift, Comparator<Object> cmp) {
			if (this.keyHash == hash
					&& cmp.compare(this.key, key) == 0) {
				return Optional.of((Map.Entry<K,V>) this);
			} else {
				return Optional.empty();				
			}
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return val;
		}

		@Override
		public V setValue(V value) {
			throw new UnsupportedOperationException();
		}

		@Override
		int arity() {
			return 1;
		}
		
		@Override
		public int size() {
			return 1;
		}
		
		@Override
		boolean isLeaf() {
			return true;
		}

		@Override
		Iterator<AbstractNode<K, V>> nodeIterator() {
			return Collections.emptyIterator();
		}
		
		@Override
		public String toString() {
			return key + "=" + val;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = keyHash;
			result = prime * result + key.hashCode();
			result = prime * result + ((val == null) ? 0 : val.hashCode());
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
			LeafNode that = (LeafNode) other;
			if (keyHash != that.keyHash) {
				return false;
			}
			if (!key.equals(that.key)) {
				return false;
			}
			if (!Objects.equals(val, that.val)) {
				return false;
			}
			return true;
		}

		@Override
		boolean hasValues() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		Iterator<K> valueIterator() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		int valueArity() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		boolean hasNodes() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		int nodeArity() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		K getValue(int index) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public AbstractNode<K, V> getNode(int index) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		K head() {
			// TODO Auto-generated method stub
			return null;
		}	
	}	
	
	private static final class LeafHashCollisionNode<K, V> extends AbstractNode<K, V> {
		private final AbstractNode<K,V>[] leafs;
		private final int hash;

		LeafHashCollisionNode(int hash, AbstractNode<K, V>[] leafs) {
			this.leafs = leafs;
			this.hash = hash;
		}
		
		@Override
		boolean containsKey(Object key, int hash, int shift, Comparator<Object> cmp) {
			for (AbstractNode<K,V> node : leafs) {
				if (node.containsKey(key, hash, shift + BIT_PARTITION_SIZE, cmp)) // TODO: increase BIT_PARTITION_SIZE?
					return true;
			}
			return false;
		}
		
		@Override
		Optional<Map.Entry<K,V>> findByKey(Object key, int hash, int shift, Comparator<Object> cmp) {
			for (AbstractNode<K,V> node : leafs) {
				final Optional<Map.Entry<K,V>> queryResult = node.findByKey(key, hash, shift + BIT_PARTITION_SIZE, cmp); // TODO: increase BIT_PARTITION_SIZE?

				if (queryResult.isPresent()) {
					return queryResult;
				}
			}
			return Optional.empty();			
		}
		
		
		/**
		 * Inserts an object if not yet present. Note, that this implementation always
		 * returns a new immutable {@link TrieMap} instance.
		 */
		@Override
		Result<K,V> updated(K key, int keyHash, V val, int shift, Comparator<Object> cmp) {
			if (this.hash != keyHash)
				return Result.modified(mergeNodes(
						this, this.hash, new LeafNode<K, V>(key, keyHash, val), keyHash, shift)); // add one

			for (int i = 0; i < leafs.length; i++) {
				AbstractNode<K,V> node = leafs[i];
				if (node.containsKey(key, hash, shift + BIT_PARTITION_SIZE, cmp)) { // TODO: increase BIT_PARTITION_SIZE?
					final AbstractNode<K, V>[] leafsNew = arraycopyAndSet(leafs, i, new LeafNode<K, V>(key, keyHash, val));
					return Result.updated(new LeafHashCollisionNode<K, V>(keyHash, leafsNew), ((LeafNode<K, V>) node).getValue()); // replace one
				}
			}
			
			final AbstractNode<K, V>[] leafsNew = arraycopyAndInsert(leafs, leafs.length, new LeafNode<K, V>(key, keyHash, val));
			return Result.modified(new LeafHashCollisionNode<>(keyHash, leafsNew)); // add one			
		}

		@Override
		Result<K,V> updated(AtomicReference<Thread> mutator, K key, int keyHash, 
				V val, int shift, Comparator<Object> cmp) {	
			return updated(key, keyHash, val, shift, cmp);
		}

		/**
		 * Removes an object if present. Note, that this implementation always
		 * returns a new immutable {@link TrieMap} instance.
		 */
		@Override
		Result<K,V> removed(K key, int hash, int shift, Comparator comparator) {
			for (int i = 0; i < leafs.length; i++) {
				if (comparator.compare(leafs[i], key) == 0) {
					if (this.arity() == 1) {
						return Result.modified(EMPTY_NODE);
					} else if (this.arity() == 2) {
						final AbstractNode<K, V> theOther = (i == 0) ? leafs[1] : leafs[0];
						return Result.modified(theOther);
					} else {
						return Result.modified(new LeafHashCollisionNode<>(hash, arraycopyAndRemove(leafs, i)));
					}
				}
			}
			return Result.unchanged(this);
		}
		
		@Override
		Result<K,V> removed(AtomicReference<Thread> mutator, K key, int hash,
				int shift, Comparator<Object> comparator) {
			return removed(key, hash, shift, comparator);
		}	

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 0;
			result = prime * result + hash;
			result = prime * result + Arrays.hashCode(leafs);
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

			LeafHashCollisionNode that = (LeafHashCollisionNode) other;
			
			if (hash != that.hash) {
				return false;
			}
			
			if (leafs.length != that.leafs.length) {
				return false;
			}
			
			outer:
			for (AbstractNode thisLeaf : this.leafs) {
				for (AbstractNode thatLeaf : that.leafs) {				
					if (thisLeaf.equals(thatLeaf)) {
						continue outer; 
					} else {
						return false;
					}		
				}
			}
			
			return true;
		}

		@Override
		int arity() {
			return leafs.length;
		}

		@Override
		int size() {
			return leafs.length;
		}
		
		@Override
		boolean isLeaf() {
			return false;
		}

		@Override
		Iterator<AbstractNode<K,V>> nodeIterator() {
			return ArrayIterator.<AbstractNode<K,V>>of(leafs);
		}

		@Override
		boolean hasValues() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		Iterator<K> valueIterator() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		int valueArity() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		boolean hasNodes() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		int nodeArity() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		K getValue(int index) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public AbstractNode<K, V> getNode(int index) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		K head() {
			// TODO Auto-generated method stub
			return null;
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
		
		if (other instanceof TrieMap) {
			TrieMap that = (TrieMap) other;

			if (this.size() != that.size())
				return false;

			return rootNode.equals(that.rootNode);
		}
		
		return super.equals(other);
	}
	
}
