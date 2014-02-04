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
		public boolean __put(E e, V v) {
			return __putEquivalent(e, v, equalityComparator());
		}

		@Override
		public boolean __putEquivalent(E e, V v, Comparator<Object> cmp) {
			if (mutator.get() == null)
				throw new IllegalStateException("Transient already frozen.");
			
			AbstractNode.Result<E,V> result = rootNode.updated(mutator, e, e.hashCode(), v, 0, cmp);

			if (result.isModified()) {
				rootNode = result.getNode();
				return true;
			} else {
				return false;
			}
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
				modified |= __putEquivalent(entry.getKey(), entry.getValue(), cmp);
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

//	@Override
//	public int hashCode() {
//		// TODO: specialize
//	}

	// NOTE: does not work because both trees don't have a single canonical representation.
//	@Override
//	public boolean equals(Object other) {
//		if (null == other) {
//			return false;
//		}
//		if (this == other) {
//			return true;
//		}
//		if (getClass() != other.getClass()) {
//			return false;
//		}
//		
//		TrieMap that = (TrieMap) other;			
//		if (cachedSize != that.cachedSize) {
//			return false;
//		}
//		if (!rootNode.equals(that.rootNode)) {
//			return false;
//		}
//		return true;
//	}	

	private static abstract class AbstractNode<K,V> {

		protected static final int BIT_PARTITION_SIZE = 5;
		protected static final int BIT_PARTITION_MASK = 0x1f;
		
		@SuppressWarnings("unchecked")
		protected static final AbstractNode EMPTY_NODE = new IndexNode(0, new AbstractNode[0], 0);
		
		abstract boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> comparator);
		
		abstract Optional<Map.Entry<K,V>> findByKey(Object key, int hash, int shift, Comparator<Object> cmp);
		
		abstract Result<K,V> updated(K key, int keyHash, V val, int shift, Comparator<Object> cmp);
		
		abstract Result<K,V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift, Comparator<Object> cmp);
		
		abstract Result<K,V> removed(K key, int hash, int shift, Comparator<Object> comparator);
		
		abstract Result<K,V> removed(AtomicReference<Thread> mutator, K key, int hash, int shift, Comparator<Object> comparator);	
		
		abstract boolean isLeaf();
		
//		abstract boolean hasValues();
//		abstract Iterator<K> valueIterator();
		
//		abstract boolean hasNodes();
		abstract Iterator<AbstractNode<K,V>> nodeIterator();
		
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
			private final boolean isModified;
			private final int sizeDelta;
			
			public static <T1,T2> Result<T1, T2> fromModified(AbstractNode<T1,T2> node, int sizeDelta) {
				return new Result<>(node, true, sizeDelta);
			}
			
			public static <T1,T2> Result<T1, T2> fromUnchanged(AbstractNode<T1,T2> node) {
				return new Result<>(node, false, 0);
			}
			
			private Result(AbstractNode<T1,T2> node, boolean isMutated, int sizeDelta) {
				this.result = node;
				this.isModified = isMutated;
				this.sizeDelta = sizeDelta;
			}
			
			@SuppressWarnings("unchecked")
			public AbstractNode<T1,T2> getNode() {
				return (AbstractNode<T1,T2>) result;
			}
						
			public boolean isModified() {
				return isModified;
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

	private static final class IndexNode<K,V> extends AbstractNode<K,V> {

		private AtomicReference<Thread> mutator;

		private int bitmap;
		private AbstractNode<K,V>[] nodes;
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

			if ((bitmap & bitpos) == 0) {
				AbstractNode<K, V>[] nodesReplacement = arraycopyAndInsert(nodes, index, new LeafNode<K, V>(key, keyHash, val));
				return Result.fromModified(new IndexNode<>(bitmap | bitpos, nodesReplacement, cachedSize + 1), 1);
			} else {
				// it's a TrieMap node, not a inplace value
				final AbstractNode<K,V> subNode = (AbstractNode<K,V>) nodes[index];

				// immutable copy subNode
				final Result<K,V> subNodeResult = subNode.updated(key, keyHash, val, shift + BIT_PARTITION_SIZE, comparator);
				final AbstractNode<K,V> subNodeReplacement = subNodeResult.getNode();

				if (subNode == subNodeReplacement)
					return Result.fromUnchanged(this);

				final AbstractNode<K, V>[] nodesReplacement = arraycopyAndSet(nodes, index, subNodeReplacement);

				return Result.fromModified(new IndexNode<K, V>(
						bitmap, nodesReplacement, cachedSize + subNodeResult.sizeDelta), subNodeResult.sizeDelta);		
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
				IndexNode<K,V> editableNode = editAndInsert(mutator, index, new LeafNode(key, keyHash, val));
				editableNode.updateMetadata(bitmap | bitpos, cachedSize + 1);			
				return Result.fromModified(editableNode, 1);
			} else { // node (not value)
				@SuppressWarnings("unchecked")
				AbstractNode<K,V> subNode = (AbstractNode<K,V>) nodes[index];
										
				Result resultNode = subNode.updated(mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, comparator);
				
				if (resultNode.isModified()) {
					IndexNode<K,V> editableNode = editAndSet(mutator, index, resultNode.getNode());
					editableNode.updateMetadata(bitmap, cachedSize + resultNode.sizeDelta);
					return Result.fromModified(editableNode, resultNode.sizeDelta);
				} else {
					return Result.fromUnchanged(subNode);
				}
			}
		}

		@Override
		public Result<K,V> removed(K key, int hash, int shift, Comparator<Object> comparator) {
			final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);
			final int bitIndex = index(bitpos);

			if ((bitmap & bitpos) == 0) {
				return Result.fromUnchanged(this);
			} else {
				// it's a TrieMap node, not a inplace value
				@SuppressWarnings("unchecked")
				final AbstractNode<K,V> subNode = (AbstractNode<K,V>) nodes[bitIndex];
				
				final Result<K,V> result = subNode.removed(key, hash, shift + BIT_PARTITION_SIZE, comparator);	
				
				if (!result.isModified())
					return Result.fromUnchanged(this);

				// TODO: optimization if singleton element node is returned
				final AbstractNode<K,V> subNodeReplacement = result.getNode();
				final AbstractNode<K,V>[] nodesReplacement = arraycopyAndSet(nodes, bitIndex, subNodeReplacement);
				return Result.fromModified(new IndexNode<>(bitmap, nodesReplacement, cachedSize - 1), -1);
			}
		}
		
		@Override
		public Result<K,V> removed(AtomicReference<Thread> mutator, K key, int hash, int shift, Comparator<Object> comparator) {
			final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);
			final int index = index(bitpos);

			if ((bitmap & bitpos) == 0) {
				return Result.fromUnchanged(this);
			} else {
				// it's a TrieMap node, not a inplace value
				@SuppressWarnings("unchecked")
				final AbstractNode<K,V> subNode = (AbstractNode<K,V>) nodes[index];
		
				Result resultNode = subNode.removed(mutator, key, hash, shift + BIT_PARTITION_SIZE, comparator);
				
				if (resultNode.isModified()) {
					IndexNode<K,V> editableNode = editAndSet(mutator, index, resultNode.getNode());
					editableNode.updateMetadata(bitmap, cachedSize - 1);
					return Result.fromModified(editableNode, -1);
				} else {
					return Result.fromUnchanged(subNode);
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
				return Result.fromModified(mergeNodes(this, this.keyHash, new LeafNode<K, V>(key, keyHash, val), keyHash, shift), 1); // insert (no collision)

			if (cmp.compare(this.key, key) != 0)
				return Result.fromModified(new HashCollisionNode<K, V>(keyHash, new LeafNode[]{this, new LeafNode<K, V>(key, keyHash, val)}), 1); // insert (hash collision)
			else
				return Result.fromModified(new LeafNode<K, V>(key, keyHash, val), 0); // replace 
		}

		@Override
		Result<K,V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift, Comparator<Object> cmp) {
			return updated(key, keyHash, val, shift, cmp);
		}
		
		@Override
		Result<K,V> removed(K key, int hash, int shift, Comparator<Object> cmp) {
			if (cmp.compare(this.key, key) == 0) {
				return Result.fromModified(EMPTY_NODE, -1);
			} else {
				return Result.fromUnchanged(this);
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
	}	
	
	@SuppressWarnings("rawtypes")
	private static final class HashCollisionNode<K,V> extends AbstractNode<K,V> {
		private final AbstractNode<K,V>[] leafs;
		private final int hash;

		HashCollisionNode(int hash, AbstractNode<K, V>[] leafs) {
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
				return Result.fromModified(mergeNodes(
						this, this.hash, new LeafNode<K, V>(key, keyHash, val), keyHash, shift), 1); // add one

			for (int i = 0; i < leafs.length; i++) {
				AbstractNode<K,V> node = leafs[i];
				if (node.containsKey(key, hash, shift + BIT_PARTITION_SIZE, cmp)) { // TODO: increase BIT_PARTITION_SIZE?
					final AbstractNode<K, V>[] leafsNew = arraycopyAndSet(leafs, i, new LeafNode<K, V>(key, keyHash, val));
					return Result.fromModified(new HashCollisionNode<K, V>(keyHash, leafsNew), 0); // replace one
				}
			}
			
			final AbstractNode<K, V>[] leafsNew = arraycopyAndInsert(leafs, leafs.length, new LeafNode<K, V>(key, keyHash, val));
			return Result.fromModified(new HashCollisionNode<>(keyHash, leafsNew), 1); // add one			
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
		@SuppressWarnings("unchecked")
		@Override
		Result<K,V> removed(K key, int hash, int shift, Comparator comparator) {
			// TODO: optimize in general
			// TODO: optimization if singleton element node is returned

			for (int i = 0; i < leafs.length; i++) {
				if (comparator.compare(leafs[i], key) == 0)
					return Result.fromModified(new HashCollisionNode<>(
							hash, arraycopyAndRemove(leafs, i)), -1);
			}
			return Result.fromUnchanged(this);
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

			HashCollisionNode that = (HashCollisionNode) other;
			
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
