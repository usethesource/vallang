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

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("rawtypes")
public class TrieMap<K,V> extends AbstractImmutableMap<K,V> {

	@SuppressWarnings("unchecked")
	private static final TrieMap EMPTY = new TrieMap(AbstractNode.EMPTY_NODE);
	
	final private AbstractNode<K> rootNode;
	
	TrieMap(AbstractNode<K> rootNode) {
		this.rootNode = rootNode;
	}
	
//	@SafeVarargs
//	public static final <K,V> ImmutableMap<K,V> of(K... elements) {
//		@SuppressWarnings("unchecked")
//		ImmutableMap<K,V> result = TrieMap.EMPTY;
//		for (K k : elements) result = result.__insert(k);
//		return result;
//	}
//	
//	@SafeVarargs
//	public static final <K,V> TransientMap<K,V> transientOf(K... elements) {
//		TransientMap<K,V> transientSet = new TransientTrieMap<>(EMPTY);
//		for (K k : elements) 
//			transientSet.__insert(k);
//		return transientSet;
//	}
		
	@SuppressWarnings("unchecked")
	protected static final <K> Comparator<K> equalityComparator() {
		return EqualityUtils.getDefaultEqualityComparator();
	}
	
	@SuppressWarnings("unchecked")
	protected static final <K> Comparator<K> equivalenceComparator() {
		return EqualityUtils.getEquivalenceComparator();
	}
	
	@Override
	public TrieMap<K,V> __put(K k, V v) {
		return __putEquivalent(k, v, equalityComparator());
	}

	@Override
	public TrieMap<K,V> __putEquivalent(K k, V v, Comparator<Object> cmp) {
		AbstractNode.Result<K> result = rootNode.updated(k, k.hashCode(), 0, cmp);
		return (result.isModified()) ? new TrieMap<K,V>(result.getNode()) : this;
	}
	
	@Override
	public ImmutableMap<K,V> __putAll(Map<? extends K, ? extends V> map) {
		return __putAllEquivalent(map, equivalenceComparator());
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
		AbstractNode.Result<K> result = rootNode.removed(k, k.hashCode(), 0, cmp);
		return (result.isModified()) ? new TrieMap<K,V>(result.getNode()) : this;
	}

	@Override
	public boolean containsKey(Object o) {
		return rootNode.contains(o, o.hashCode(), 0, equivalenceComparator());
	}
	
	@Override
	public boolean containsKeyEquivalent(Object o, Comparator<Object> cmp) {
		return rootNode.contains(o, o.hashCode(), 0, cmp);
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
	public int size() {
		return rootNode.size();
	}

	@Override
	public Iterator<Map.Entry<K, V>> entryIterator() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}	
	
//	@SuppressWarnings("unchecked")
//	@Override
//	public Iterator<K> iterator() {
//		return new TrieMapIterator(rootNode);
//	}
	
	/**
	 * Iterator that first iterates over inlined-values and then
	 * continues depth first recursively.
	 */
	private static class TrieMapIterator implements Iterator {

		final Deque<Iterator<AbstractNode>> nodeIterationStack;
		Iterator<?> valueIterator;
		
		TrieMapIterator(AbstractNode rootNode) {			
			if (rootNode.hasValues()) {
				valueIterator = rootNode.valueIterator();
			} else {
				valueIterator = Collections.emptyIterator();
			}

			nodeIterationStack = new ArrayDeque<>();
			if (rootNode.hasNodes()) {
				nodeIterationStack.push(rootNode.nodeIterator());
			}
		}

		@Override
		public boolean hasNext() {
			while (true) {
				if (valueIterator.hasNext()) {
					return true;
				} else {						
					if (nodeIterationStack.isEmpty()) {
						return false;
					} else {
						if (nodeIterationStack.peek().hasNext()) {
							AbstractNode innerNode = nodeIterationStack.peek().next();						
							
							if (innerNode.hasValues())
								valueIterator = innerNode.valueIterator();
							
							if (innerNode.hasNodes()) {
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
			return valueIterator.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	// TODO: maybe move to ImmutableSet interface?
	public boolean isTransientSupported() {
		return true;
	}

	// TODO: maybe move to ImmutableSet interface?
	public TransientMap<K,V> asTransient() {
		return new TransientTrieMap<K,V>(this);
	}
	
	/*
	 * TODO: exchange TrieMap.equivalenceComparator() with standard equality operator
	 */
	static final class TransientTrieMap<E,V> implements TransientMap<E,V> {		
		final private AtomicReference<Thread> mutator;		
		private AbstractNode<E> rootNode;
				
		TransientTrieMap(TrieMap<E,V> trieMap) {
			this.mutator = new AtomicReference<Thread>(Thread.currentThread());
			this.rootNode = trieMap.rootNode;
		}

		@Override
		public boolean __put(E e, V v) {
			return __putEquivalent(e, v, TrieMap.equivalenceComparator());
		}

		@Override
		public boolean __putEquivalent(E e, V v, Comparator<Object> cmp) {
			AbstractNode.Result<E> result = rootNode.updated(mutator, e, e.hashCode(), 0, cmp);

			if (result.isModified()) {
				rootNode = result.getNode();
				return true;
			} else {
				return false;
			}
		}

		@Override
		public boolean __remove(E e) {
			return __removeEquivalent(e, TrieMap.equivalenceComparator());
		}

		@Override
		public boolean __removeEquivalent(E e, Comparator<Object> cmp) {
			AbstractNode.Result<E> result = rootNode.removed(mutator, (E) e, e.hashCode(), 0, cmp);

			if (result.isModified()) {
				rootNode = result.getNode();
				return true;
			} else {
				return false;
			}
		}

		@Override
		public boolean __putAll(Map<? extends E, ? extends V> map) {
			return __putAllEquivalent(map, TrieMap.equivalenceComparator());
		}

		@Override
		public boolean __putAllEquivalent(Map<? extends E, ? extends V> map,
				Comparator<Object> cmp) {
			throw new UnsupportedOperationException();
//			boolean modified = false;
//			
//			for (E e : map) {
//				modified |= __putEquivalent(e, cmp);
//			}
//			
//			return modified;	
		}	
		
//		@Override
//		public boolean removeAll(Collection<?> c) {
//			boolean modified = false;
	//
//			for (Object o : c)
//				modified |= remove(o);
//							
//			return modified;
//		}
	//
//		@Override
//		public boolean retainAll(Collection<?> c) {
//			throw new UnsupportedOperationException();
//		}
	//
//		@Override
//		public void clear() {
//			// allocated a new empty instance, because transient allows inplace modification.
//			content = new InplaceIndexNode<>(0, 0, new TrieMap[0], 0);
//		}	
	//
//		@Override
//		public Iterator<E> iterator() {
//			return content.iterator();
//		}
	//
//		@Override
//		public int size() {
//			return content.size();
//		}
	//
//		@Override
//		public boolean isEmpty() {
//			return content.isEmpty();
//		}
	//
//		@Override
//		public boolean contains(Object o) {
//			return content.contains(o);
//		}
	//	
//		@Override
//		public boolean containsEquivalent(Object o, Comparator<Object> cmp) {
//			// TODO Auto-generated method stub
//			return false;
//		}
	//
//		@Override
//		public Object[] toArray() {
//			return content.toArray();
//		}
	//
//		@Override
//		public <T> T[] toArray(T[] a) {
//			return content.toArray(a);
//		}
	//
//		@Override
//		public boolean containsAll(Collection<?> c) {
//			return content.containsAll(c);
//		}

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
		
		// TODO: ensure that only freezed once (like in IWriter implementations)
		@Override
		public ImmutableMap<E,V> freeze() {
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

	@SuppressWarnings("rawtypes")
	private static abstract class AbstractNode<K> {

		protected static final int BIT_PARTITION_SIZE = 5;
		protected static final int BIT_PARTITION_MASK = 0x1f;
		
		protected static final AbstractNode EMPTY_NODE = new InplaceIndexNode(0, 0, new Object[0], 0);
		
		abstract boolean contains(Object key, int hash, int shift, Comparator<Object> comparator);
		
		abstract Result<K> updated(K key, int hash, int shift, Comparator<Object> cmp);
		
		abstract Result<K> updated(AtomicReference<Thread> mutator, K key, int hash, int shift, Comparator<Object> cmp);
		
		abstract Result<K> removed(K key, int hash, int shift, Comparator<Object> comparator);
		
		abstract Result<K> removed(AtomicReference<Thread> mutator, K key, int hash, int shift, Comparator<Object> comparator);	
		
		abstract boolean hasValues();
		abstract Iterator<K> valueIterator();
		
		abstract boolean hasNodes();
		abstract Iterator<AbstractNode<K>> nodeIterator();
		
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

		
		@SuppressWarnings("unchecked")
		static <K> AbstractNode<K> mergeNodes(Object node0, int hash0, Object node1, int hash1, int shift) {
			assert (!(node0 instanceof AbstractNode));
			assert (!(node1 instanceof AbstractNode));

			if (hash0 == hash1)
				return new HashCollisionNode<>(hash0, (K[]) new Object[]{node0, node1});

			final int mask0 = (hash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (hash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level
				final int bitmap = (1 << mask0) | (1 << mask1);
				final int valmap = (1 << mask0) | (1 << mask1);
				final Object[] nodes = new Object[2];

				if (mask0 < mask1) {
					nodes[0] = node0;
					nodes[1] = node1;
				} else {
					nodes[0] = node1;
					nodes[1] = node0;
				}

				return new InplaceIndexNode<>(bitmap, valmap, nodes, 2);
			} else {
				// values fit on next level
				final int bitmap = (1 << mask0);
				final int valmap = 0;
				final AbstractNode node = mergeNodes(node0, hash0, node1, hash1, shift + BIT_PARTITION_SIZE);

				return new InplaceIndexNode<>(bitmap, valmap, node, 2);
			}
		}

		static <K> AbstractNode<K> mergeNodes(AbstractNode node0, int hash0, AbstractNode node1, int hash1, int shift) {
			final int mask0 = (hash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (hash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level
				final int bitmap = (1 << mask0) | (1 << mask1);
				final int valmap = 0;
				final Object[] nodes = new Object[2];

				if (mask0 < mask1) {
					nodes[0] = node0;
					nodes[1] = node1;
				} else {
					nodes[0] = node1;
					nodes[1] = node0;
				}

				return new InplaceIndexNode<>(bitmap, valmap, nodes, node0.size() + node1.size());
			} else {
				// values fit on next level
				final int bitmap = (1 << mask0);
				final int valmap = 0;
				final AbstractNode node = mergeNodes(node0, hash0, node1, hash1, shift + BIT_PARTITION_SIZE);

				return new InplaceIndexNode<>(bitmap, valmap, node, node.size());
			}
		}

		static <K> AbstractNode<K> mergeNodes(AbstractNode node0, int hash0, Object node1, int hash1, int shift) {
			assert (!(node1 instanceof AbstractNode));

			final int mask0 = (hash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (hash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level
				final int bitmap = (1 << mask0) | (1 << mask1);
				final int valmap = (1 << mask1);
				final Object[] nodes = new Object[2];

//				if (mask0 < mask1) {
//					nodes[0] = node0;
//					nodes[1] = node1;
//				} else {
					// inline node first
					nodes[0] = node1;
					nodes[1] = node0;
//				}

				return new InplaceIndexNode<>(bitmap, valmap, nodes, node0.size() + 1);
			} else {
				// values fit on next level
				final int bitmap = (1 << mask0);
				final int valmap = 0;
				final AbstractNode node = mergeNodes(node0, hash0, node1, hash1, shift + BIT_PARTITION_SIZE);

				return new InplaceIndexNode<>(bitmap, valmap, node, node.size());
			}
		}
		
		protected static class Result<T> {
			private final Object result;
			private final boolean isModified;
			
			public static <T> Result fromModified(AbstractNode<T> node) {
				return new Result<>(node, true);
			}
			
			public static <T> Result fromUnchanged(AbstractNode<T> node) {
				return new Result<>(node, false);
			}
			
			private Result(AbstractNode<T> node, boolean isMutated) {
				this.result = node;

				this.isModified = isMutated;
			}
			
			@SuppressWarnings("unchecked")
			public AbstractNode<T> getNode() {
				return (AbstractNode<T>) result;
			}
						
			public boolean isModified() {
				return isModified;
			}
		}		
	}

	@SuppressWarnings("rawtypes")
	private static final class InplaceIndexNode<K> extends AbstractNode<K> {

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
		}
			
		InplaceIndexNode(AtomicReference<Thread> mutator, Object[] nodes) {
			this.mutator = mutator;
			this.nodes = nodes;
		}
		
		InplaceIndexNode(int bitmap, int valmap, Object[] nodes, int cachedSize) {
			this(null, bitmap, valmap, nodes, cachedSize);
		}

		InplaceIndexNode(int bitmap, int valmap, Object node, int cachedSize) {
			this(bitmap, valmap, new Object[]{node}, cachedSize);
		}

		final int index(int bitpos) {
			return Integer.bitCount(bitmap & (bitpos - 1));
		}

		final int bitIndex(int bitpos) {
			return Integer.bitCount(valmap) + Integer.bitCount((bitmap ^ valmap) & (bitpos - 1));
		}
		
		final int valIndex(int bitpos) {
			return Integer.bitCount(valmap & (bitpos - 1));
		}
		
		private void updateMetadata(int bitmap, int valmap, int cachedSize, int cachedValmapBitCount) {
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.cachedSize = cachedSize;
			this.cachedValmapBitCount = cachedValmapBitCount; 
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public boolean contains(Object key, int hash, int shift, Comparator<Object> comparator) {
			final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((bitmap & bitpos) != 0) {			
				if ((valmap & bitpos) != 0) {
					return comparator.compare(nodes[valIndex(bitpos)], key) == 0;
				} else {
					return ((AbstractNode<K>) nodes[bitIndex(bitpos)]).contains(key, hash, shift + BIT_PARTITION_SIZE, comparator);
				}
			}
			return false;
		}

		@Override
		public Result<K> updated(K key, int hash, int shift, Comparator<Object> comparator) {
			final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);
			final int bitIndex = bitIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			if ((bitmap & bitpos) == 0) {
				Object[] nodesReplacement = ArrayUtils.arraycopyAndInsert(nodes, valIndex, key);
				return Result.fromModified(new InplaceIndexNode<>(
						bitmap | bitpos, valmap | bitpos, nodesReplacement, cachedSize + 1));
			}

			if ((valmap & bitpos) != 0) {
				// it's an inplace value
				if (comparator.compare(nodes[valIndex], key) == 0)
					return Result.fromUnchanged(this);

				final AbstractNode nodeNew = mergeNodes(nodes[valIndex], nodes[valIndex].hashCode(), key, hash, shift + BIT_PARTITION_SIZE);
				
				// immutable copy
				/** CODE DUPLCIATION **/
				final int bitIndexNewOffset = Integer.bitCount(valmap & ~bitpos);
				final int bitIndexNew = bitIndexNewOffset + Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos)) & (bitpos - 1)); 
				final Object[] nodesReplacement = ArrayUtils.arraycopyAndMoveToBack(nodes, valIndex, bitIndexNew, nodeNew);
				
				return Result.fromModified(new InplaceIndexNode<>(
						bitmap | bitpos, valmap & ~bitpos, nodesReplacement, cachedSize + 1));				
			} else {
				// it's a TrieMap node, not a inplace value
				@SuppressWarnings("unchecked")
				final AbstractNode<K> subNode = (AbstractNode<K>) nodes[bitIndex];

				// immutable copy subNode
				final AbstractNode<K> subNodeReplacement = subNode.updated(key, hash, shift + BIT_PARTITION_SIZE, comparator).getNode();

				if (subNode == subNodeReplacement)
					return Result.fromUnchanged(this);

				final Object[] nodesReplacement = ArrayUtils.arraycopyAndSet(nodes, bitIndex, subNodeReplacement);

				return Result.fromModified(new InplaceIndexNode<>(
						bitmap, valmap, nodesReplacement, cachedSize + 1));		
			}
		}
		
		InplaceIndexNode<K> editAndInsert(AtomicReference<Thread> mutator, int index, Object elementNew) {		
			Object[] editableNodes = ArrayUtils.arraycopyAndInsert(this.nodes, index, elementNew);
			
			if (this.mutator == mutator) {
				this.nodes = editableNodes;
				return this;
			} else {
				return new InplaceIndexNode<>(mutator, editableNodes);
			}
		}
		
		// TODO: only copy when not yet editable
		InplaceIndexNode<K> editAndMoveToBack(AtomicReference<Thread> mutator, int indexOld, int indexNew, Object elementNew) {
			Object[] editableNodes = ArrayUtils.arraycopyAndMoveToBack(this.nodes, indexOld, indexNew, elementNew);
			
			if (this.mutator == mutator) {
				this.nodes = editableNodes;
				return this;
			} else {
				return new InplaceIndexNode<>(mutator, editableNodes);
			}		
		}
		
		// TODO: only copy when not yet editable
		InplaceIndexNode<K> editAndSet(AtomicReference<Thread> mutator, int index, Object elementNew) {
			Object[] editableNodes = ArrayUtils.arraycopyAndSet(this.nodes, index, elementNew);
			
			if (this.mutator == mutator) {
				this.nodes = editableNodes;
				return this;
			} else {
				return new InplaceIndexNode<>(mutator, editableNodes);
			}
		}
		
		InplaceIndexNode<K> editAndRemove(AtomicReference<Thread> mutator, int index) {
			Object[] editableNodes = ArrayUtils.arraycopyAndRemove(this.nodes, index);
			
			if (this.mutator == mutator) {
				this.nodes = editableNodes;
				return this;
			} else {
				return new InplaceIndexNode<>(mutator, editableNodes);
			}
		}
		
		@Override
		public Result<K> updated(AtomicReference<Thread> mutator, K key, int hash, int shift, Comparator<Object> comparator) {
			final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);
			final int bitIndex = bitIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			if ((bitmap & bitpos) == 0) { // no value
				InplaceIndexNode<K> editableNode = editAndInsert(mutator, valIndex, key);
				editableNode.updateMetadata(bitmap | bitpos, valmap | bitpos, cachedSize + 1,
						cachedValmapBitCount + 1);			
				return Result.fromModified(editableNode);
			}

			if ((valmap & bitpos) != 0) { // inplace value
				if (comparator.compare(nodes[valIndex], key) == 0) {
					return Result.fromUnchanged(this);
				} else {					
					// TODO: simplify this line
					int bitIndexNewOffset = Integer.bitCount(valmap & ~bitpos);
					int bitIndexNew = bitIndexNewOffset + Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos)) & (bitpos - 1)); 								
					AbstractNode nodeNew = mergeNodes(nodes[valIndex], nodes[valIndex].hashCode(), key, hash, shift + BIT_PARTITION_SIZE);				
					
					InplaceIndexNode<K> editableNode = editAndMoveToBack(mutator, valIndex, bitIndexNew, nodeNew);
					editableNode.updateMetadata(bitmap | bitpos, valmap & ~bitpos, cachedSize + 1,
							cachedValmapBitCount - 1);								
					return Result.fromModified(editableNode); 
				}
			} else { // node (not value)
				@SuppressWarnings("unchecked")
				AbstractNode<K> subNode = (AbstractNode<K>) nodes[bitIndex];
										
				Result resultNode = subNode.updated(mutator, key, hash, shift + BIT_PARTITION_SIZE, comparator);
				
				if (resultNode.isModified()) {
					InplaceIndexNode<K> editableNode = editAndSet(mutator, bitIndex, resultNode.getNode());
					editableNode.updateMetadata(bitmap, valmap, cachedSize + 1, cachedValmapBitCount);
					return Result.fromModified(editableNode);
				} else {
					return Result.fromUnchanged(subNode);
				}
			}
		}

		@Override
		public Result<K> removed(K key, int hash, int shift, Comparator<Object> comparator) {
			final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);
			final int bitIndex = bitIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			if ((bitmap & bitpos) == 0)
				return Result.fromUnchanged(this);

			if ((valmap & bitpos) == 0) {
				// it's a TrieMap node, not a inplace value
				@SuppressWarnings("unchecked")
				final AbstractNode<K> subNode = (AbstractNode<K>) nodes[bitIndex];
				
				final Result<K> result = subNode.removed(key, hash, shift + BIT_PARTITION_SIZE, comparator);	
				
				if (!result.isModified())
					return Result.fromUnchanged(this);

				// TODO: optimization if singleton element node is returned
				final AbstractNode<K> subNodeReplacement = result.getNode();
				final Object[] nodesReplacement = ArrayUtils.arraycopyAndSet(nodes, bitIndex, subNodeReplacement);
				return Result.fromModified(new InplaceIndexNode<>(bitmap, valmap, nodesReplacement, cachedSize - 1));
			} else {
				// it's an inplace value
				if (comparator.compare(nodes[valIndex], key) != 0)
					return Result.fromUnchanged(this);

				if (arity() == 1) {
					return Result.fromModified(EMPTY_NODE);
//				} else if (arity() == 2 && bitmap == valmap) { // two values
//					return (valIndex == 0) ? ModificationResult.fromValue(nodes[1]) : ModificationResult.fromValue(nodes[0]);
				} else {
					final Object[] nodesReplacement = ArrayUtils.arraycopyAndRemove(nodes, valIndex);
					return Result.fromModified(new InplaceIndexNode<>(bitmap & ~bitpos, valmap & ~bitpos, nodesReplacement, cachedSize - 1));
				}
			}
		}
		
		@Override
		public Result<K> removed(AtomicReference<Thread> mutator, K key, int hash, int shift, Comparator<Object> comparator) {
			final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);
			final int bitIndex = bitIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			if ((bitmap & bitpos) == 0) {
				return Result.fromUnchanged(this);
			}

			if ((valmap & bitpos) == 0) {
				// it's a TrieMap node, not a inplace value
				@SuppressWarnings("unchecked")
				final AbstractNode<K> subNode = (AbstractNode<K>) nodes[bitIndex];
		
				Result resultNode = subNode.removed(mutator, key, hash, shift + BIT_PARTITION_SIZE, comparator);
				
				if (resultNode.isModified()) {
					InplaceIndexNode<K> editableNode = editAndSet(mutator, valIndex, resultNode.getNode());
					editableNode.updateMetadata(bitmap, valmap, cachedSize - 1, cachedValmapBitCount);
					return Result.fromModified(editableNode);
				} else {
					return Result.fromUnchanged(subNode);
				}
			} else {
				// it's an inplace value
				if (comparator.compare(nodes[valIndex], key) != 0) {
					return Result.fromUnchanged(this);
				} else {
					// TODO: optimization if singleton element node is returned
					InplaceIndexNode<K> editableNode = editAndRemove(mutator, valIndex);
					editableNode.updateMetadata(this.bitmap & ~bitpos,
							this.valmap & ~bitpos, cachedSize - 1, cachedValmapBitCount - 1);
					return Result.fromModified(editableNode);
				}
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<K> valueIterator() {
//			if (cachedValSize == 0) 
//				return Collections.emptyIterator();
//			else
				return (Iterator) ArrayIterator.of(nodes, 0, cachedValmapBitCount);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<AbstractNode<K>> nodeIterator() {
//			if (cachedValSize == nodes.length)
//				return Collections.emptyIterator();
//			else
				return (Iterator) ArrayIterator.of(nodes, cachedValmapBitCount, nodes.length - cachedValmapBitCount);
		}

		@Override
		boolean hasValues() {	
			return cachedValmapBitCount != 0;
		}

		@Override
		boolean hasNodes() {
			return cachedValmapBitCount != nodes.length;
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
			InplaceIndexNode that = (InplaceIndexNode) other;
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

	@SuppressWarnings("rawtypes")
	private static final class HashCollisionNode<K> extends AbstractNode<K> {
		private final K[] keys;
		private final int hash;

		HashCollisionNode(int hash, K[] keys) {
			this.keys = keys;
			this.hash = hash;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		Iterator<K> valueIterator() {
			return ArrayIterator.of(keys);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<AbstractNode<K>> nodeIterator() {
			return Collections.emptyIterator(); 
		}
		
		@Override
		public boolean contains(Object key, int hash, int shift, Comparator<Object> comparator) {
			for (K k : keys) {
				if (comparator.compare(k, key) == 0)
					return true;
			}
			return false;
		}

		/**
		 * Inserts an object if not yet present. Note, that this implementation always
		 * returns a new immutable {@link TrieMap} instance.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public Result<K> updated(K key, int hash, int shift, Comparator comparator) {
			if (this.hash != hash)
				return Result.fromModified(mergeNodes(
						(AbstractNode) this, this.hash, key, hash, shift));

			if (contains(key, hash, shift, comparator))
				return Result.fromUnchanged(this);

			final K[] keysNew = (K[]) ArrayUtils.arraycopyAndInsert(keys, keys.length, key);
			return Result.fromModified(new HashCollisionNode<>(hash,
					keysNew));
		}

		@Override
		public Result<K> updated(AtomicReference<Thread> mutator, K key, int hash,
				int shift, Comparator<Object> cmp) {	
			return updated(key, hash, shift, cmp);
		}

		/**
		 * Removes an object if present. Note, that this implementation always
		 * returns a new immutable {@link TrieMap} instance.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public Result<K> removed(K key, int hash, int shift, Comparator comparator) {
			// TODO: optimize in general
			// TODO: optimization if singleton element node is returned

			for (int i = 0; i < keys.length; i++) {
				if (comparator.compare(keys[i], key) == 0)
					return Result.fromModified(new HashCollisionNode<>(
							hash, (K[]) ArrayUtils.arraycopyAndRemove(keys, i)));
			}
			return Result.fromUnchanged(this);
		}
		
		@Override
		Result<K> removed(AtomicReference<Thread> mutator, K key, int hash,
				int shift, Comparator<Object> comparator) {
			return removed(key, hash, shift, comparator);
		}	

		@Override
		boolean hasValues() {
			return true;
		}

		@Override
		boolean hasNodes() {
			return false;
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
		public boolean equals(Object obj) {
			if (null == obj) {
				return false;
			}
			if (this == obj) {
				return true;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}

			HashCollisionNode other = (HashCollisionNode) obj;
			
			if (hash != other.hash) {
				return false;
			}

			// not possible due to arbitrary order
//			if (!Arrays.equals(keys, other.keys)) {
//				return false;
//			}
			
			for (K key : keys) {
				// TODO cleanup!
				// NOTE: 0, 0 used because contains does not reference them.
				if (!other.contains(key, 0, 0, TrieMap.equalityComparator())) {
					return false;
				}
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
	
}
