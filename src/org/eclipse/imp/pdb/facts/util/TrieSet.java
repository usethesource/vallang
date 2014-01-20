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

/*
 * Set as Hash Array Mapped Trie.
 * 
 * Uses:
 *   Inlined Leafs (with valmap)
 *   Orders first values then nodes (to achive better iteration performance)
 *   Hash code follows java.util.Set contract
 */
public class TrieSet<K> extends AbstractImmutableSet<K> {
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static final TrieSet EMPTY = new TrieSet(AbstractNode.EMPTY_NODE, 0);
	
	private final AbstractNode<K> rootNode;
	private final int hashCode;

	TrieSet(AbstractNode<K> rootNode, int hashCode) {
		this.rootNode = rootNode;
		this.hashCode = hashCode;
	}
	
	@SafeVarargs
	public static final <K> ImmutableSet<K> of(K... elements) {
		@SuppressWarnings("unchecked")
		ImmutableSet<K> result = TrieSet.EMPTY;
		for (K k : elements) result = result.__insert(k);
		return result;
	}
	
	@SafeVarargs
	public static final <K> TransientSet<K> transientOf(K... elements) {
		@SuppressWarnings("unchecked")
		TransientSet<K> transientSet = new TransientTrieSet<>(EMPTY);
		for (K k : elements) 
			transientSet.__insert(k);
		return transientSet;
	}
		
	@SuppressWarnings("unchecked")
	protected static final <K> Comparator<K> equalityComparator() {
		return EqualityUtils.getDefaultEqualityComparator();
	}
	
	@SuppressWarnings("unchecked")
	protected static final <K> Comparator<K> equivalenceComparator() {
		return EqualityUtils.getEquivalenceComparator();
	}
	
	@Override
	public TrieSet<K> __insert(K k) {
		return __insertEquivalent(k, equalityComparator());
	}

	@Override
	public TrieSet<K> __insertEquivalent(K key, Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final AbstractNode.Result<K> result = rootNode.updated(key, keyHash, 0, cmp);
						
		if (result.isModified())
			return new TrieSet<K>(result.getNode(), hashCode + keyHash);

		return this;
	}
	
	@Override
	public ImmutableSet<K> __insertAll(ImmutableSet<? extends K> set) {
		return __insertAllEquivalent(set, equivalenceComparator());
	}	

	@Override
	public ImmutableSet<K> __insertAllEquivalent(ImmutableSet<? extends K> set, Comparator<Object> cmp) {
		TransientSet<K> tmp = asTransient(); 		
		tmp.__insertAllEquivalent(set, cmp);		
		return tmp.freeze();
	}
	
	@Override
	public ImmutableSet<K> __retainAll(ImmutableSet<? extends K> set) {
		return __retainAllEquivalent(set, equivalenceComparator());	
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
		final AbstractNode.Result<K> result = rootNode.removed(key, keyHash, 0, cmp);
								
		if (result.isModified())
			return new TrieSet<K>(result.getNode(), hashCode - keyHash);

		return this;
	}

	@Override
	public ImmutableSet<K> __removeAll(ImmutableSet<? extends K> set) {
		return __removeAllEquivalent(set, equivalenceComparator());
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
		return rootNode.contains(o, o.hashCode(), 0, equivalenceComparator());
	}
	
	@Override
	public boolean containsEquivalent(Object o, Comparator<Object> cmp) {
		return rootNode.contains(o, o.hashCode(), 0, cmp);
	}

	@Override
	public int size() {
		return rootNode.size();
	}

	@Override
	public Iterator<K> iterator() {
		return new TrieSetIterator<K>(rootNode);
	}
	
	/**
	 * Iterator that first iterates over inlined-values and then
	 * continues depth first recursively.
	 */
	private static class TrieSetIterator<K> implements Iterator<K> {

		final Deque<Iterator<AbstractNode<K>>> nodeIteratorStack;
		Iterator<K> valueIterator;
		
		TrieSetIterator(AbstractNode<K> rootNode) {			
			if (rootNode.hasValues()) {
				valueIterator = rootNode.valueIterator();
			} else {
				valueIterator = Collections.emptyIterator();
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
							AbstractNode<K> innerNode = nodeIteratorStack.peek().next();						
							
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
	public TransientSet<K> asTransient() {
		return new TransientTrieSet<K>(this);
	}
	
	/*
	 * TODO: exchange TrieSet.equivalenceComparator() with standard equality operator
	 */
	static final class TransientTrieSet<K> implements TransientSet<K> {		
		final private AtomicReference<Thread> mutator;		
		private AbstractNode<K> rootNode;
		private int hashCode;

				
		TransientTrieSet(TrieSet<K> trieSet) {
			this.mutator = new AtomicReference<Thread>(Thread.currentThread());
			this.rootNode = trieSet.rootNode;
		}

		@Override
		public boolean contains(Object o) {
			return rootNode.contains(o, o.hashCode(), 0, equivalenceComparator());
		}
		
		@Override
		public boolean containsEquivalent(Object o, Comparator<Object> cmp) {
			return rootNode.contains(o, o.hashCode(), 0, cmp);
		}	
		
		@Override
		public boolean __insert(K key) {
			return __insertEquivalent(key, TrieSet.equivalenceComparator());
		}

		@Override
		public boolean __insertEquivalent(K key, Comparator<Object> cmp) {
			final int keyHash = key.hashCode();
			final AbstractNode.Result<K> result = rootNode.updated(mutator, key, keyHash, 0, cmp);

			if (result.isModified()) {
				rootNode = result.getNode();
				hashCode += keyHash;
				return true;
			}
			
			return false;
		}

		@Override
		public boolean __insertAll(ImmutableSet<? extends K> set) {
			return __insertAllEquivalent(set, TrieSet.equivalenceComparator());
		}
		
		@Override
		public boolean __insertAllEquivalent(ImmutableSet<? extends K> set,
				Comparator<Object> cmp) {
			boolean modified = false;
			
			for (K key : set) {
				modified |= __insertEquivalent(key, cmp);
			}
			
			return modified;	
		}	

		@Override
		public boolean __retainAll(ImmutableSet<? extends K> set) {
			return __retainAllEquivalent(set, equivalenceComparator());	
		}

		@Override
		public boolean __retainAllEquivalent(ImmutableSet<? extends K> set,
				Comparator<Object> cmp) {
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
		public boolean __remove(K key) {
			return __removeEquivalent(key, TrieSet.equivalenceComparator());
		}

		@Override
		public boolean __removeEquivalent(K key, Comparator<Object> cmp) {
			final int keyHash = key.hashCode();
			final AbstractNode.Result<K> result = rootNode.removed(mutator, (K) key, keyHash, 0, cmp);

			if (result.isModified()) {
				rootNode = result.getNode();
				hashCode -= keyHash;
				return true;
			}
			
			return false;
		}

		@Override
		public boolean __removeAll(ImmutableSet<? extends K> set) {
			return __removeAllEquivalent(set, TrieSet.equivalenceComparator());
		}

		@Override
		public boolean __removeAllEquivalent(ImmutableSet<? extends K> set,
				Comparator<Object> cmp) {
			boolean modified = false;
			
			for (K key : set) {
				modified |= __removeEquivalent(key, cmp);
			}
			
			return modified;	
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
//			content = new InplaceIndexNode<>(0, 0, new TrieSet[0], 0);
//		}	
	//
//		@Override
//		public Iterator<K> iterator() {
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
		public Iterator<K> iterator() {
			return new TransientTrieSetIterator<K>(this);
		}
		
		/**
		 * Iterator that first iterates over inlined-values and then
		 * continues depth first recursively.
		 */
		private static class TransientTrieSetIterator<K> implements Iterator<K> {

			final TransientTrieSet<K> transientTrieSet;
			final Deque<Iterator<AbstractNode<K>>> nodeIteratorStack;
			Iterator<K> valueIterator;
			K lastValue;
			
			TransientTrieSetIterator(TransientTrieSet<K> transientTrieSet) {						
				this.transientTrieSet = transientTrieSet;
				
				AbstractNode<K> rootNode = transientTrieSet.rootNode;				
				if (rootNode.hasValues()) {
					valueIterator = rootNode.valueIterator();
				} else {
					valueIterator = Collections.emptyIterator();
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
								AbstractNode<K> innerNode = nodeIteratorStack.peek().next();						
								
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
				if (!hasNext()) throw new NoSuchElementException();
				lastValue = valueIterator.next();
				return lastValue;
			}

			@Override
			public void remove() {
				transientTrieSet.__remove(lastValue);
			}
		}
		
		@Override
		public String toString() {
			return rootNode.toString();
		}
		
		@Override
		public boolean equals(Object o) {
			return rootNode.equals(o);
		}
		
		@Override
		public int hashCode() {
			return hashCode;
		}

		// TODO: ensure that only freezed once (like in IWriter implementations)
		@Override
		public ImmutableSet<K> freeze() {
			mutator.set(null);
			return new TrieSet<K>(rootNode, hashCode);
		}
	}

	private static abstract class AbstractNode<K> {
		protected static final int BIT_PARTITION_SIZE = 5;
		protected static final int BIT_PARTITION_MASK = 0x1f;
		
		@SuppressWarnings("rawtypes")
		protected static final AbstractNode EMPTY_NODE = new InplaceIndexNode(0, 0, new Object[0], 0);
		
		abstract boolean contains(Object key, int hash, int shift, Comparator<Object> comparator);

		abstract Result<K> updated(K key, int hash, int shift, Comparator<Object> cmp);
		
		abstract Result<K> updated(AtomicReference<Thread> mutator, K key, int hash, int shift, Comparator<Object> cmp);
		
		abstract Result<K> removed(K key, int hash, int shift, Comparator<Object> comparator);
		
		abstract Result<K> removed(AtomicReference<Thread> mutator, K key, int hash, int shift, Comparator<Object> comparator);	
		
		abstract boolean hasValues();
		abstract Iterator<K> valueIterator();
		abstract int valueSize();
		
		abstract boolean hasNodes();
		abstract Iterator<AbstractNode<K>> nodeIterator();
		abstract int nodeSize();
		
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
		 * @return first value
		 */
		K head() {
			// TODO: optimize; push into node implementations and return node[0] iff hasValues()
			return valueIterator().next();
		}
		
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
				final AbstractNode<K> node = mergeNodes(node0, hash0, node1, hash1, shift + BIT_PARTITION_SIZE);

				return new InplaceIndexNode<>(bitmap, valmap, node, 2);
			}
		}

		static <K> AbstractNode<K> mergeNodes(AbstractNode<K> node0, int hash0, Object node1, int hash1, int shift) {
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
				final AbstractNode<K> node = mergeNodes(node0, hash0, node1, hash1, shift + BIT_PARTITION_SIZE);

				return new InplaceIndexNode<>(bitmap, valmap, node, node.size());
			}
		}
		
		protected static class Result<T> {
			private final AbstractNode<T> node;
			private final boolean isModified;
			
			public static <T> Result<T> fromModified(AbstractNode<T> node) {
				return new Result<>(node, true);
			}
			
			public static <T> Result<T> fromUnchanged(AbstractNode<T> node) {
				return new Result<>(node, false);
			}
			
			private Result(AbstractNode<T> node, boolean isMutated) {
				this.node = node;
				this.isModified = isMutated;
			}
			
			public AbstractNode<T> getNode() {
				return node;
			}
						
			public boolean isModified() {
				return isModified;
			}
		}
	}

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

		final int bitIndex(int bitpos) {
			return Integer.bitCount(valmap) + Integer.bitCount((bitmap ^ valmap) & (bitpos - 1));
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

		@SuppressWarnings("unchecked")
		@Override
		public Result<K> updated(K key, int hash, int shift, Comparator<Object> comparator) {
			final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((bitmap & bitpos) == 0) {
				Object[] nodesReplacement = ArrayUtils.arraycopyAndInsert(nodes, valIndex(bitpos), key);
				return Result.fromModified(new InplaceIndexNode<K>(
						bitmap | bitpos, valmap | bitpos, nodesReplacement, cachedSize + 1));
			}

			if ((valmap & bitpos) != 0) {
				// it's an inplace value
				final int valIndex = valIndex(bitpos);
				
				if (comparator.compare(nodes[valIndex], key) == 0)
					return Result.fromUnchanged(this);

				final AbstractNode<K> nodeNew = mergeNodes(nodes[valIndex], nodes[valIndex].hashCode(), key, hash, shift + BIT_PARTITION_SIZE);
				
				// immutable copy
				/** CODE DUPLCIATION **/
				final int bitIndexNewOffset = Integer.bitCount(valmap & ~bitpos);
				final int bitIndexNew = bitIndexNewOffset + Integer.bitCount((bitmap ^ (valmap & ~bitpos)) & (bitpos - 1)); 
				final Object[] nodesReplacement = ArrayUtils.arraycopyAndMoveToBack(nodes, valIndex, bitIndexNew, nodeNew);
				
				return Result.fromModified(new InplaceIndexNode<K>(
						bitmap, valmap & ~bitpos, nodesReplacement, cachedSize + 1));				
			} else {
				// it's a TrieSet node, not a inplace value
				final int bitIndex = bitIndex(bitpos);
				final AbstractNode<K> subNode = (AbstractNode<K>) nodes[bitIndex];

				// immutable copy subNode
				final AbstractNode<K> subNodeReplacement = subNode.updated(key, hash, shift + BIT_PARTITION_SIZE, comparator).getNode();

				if (subNode == subNodeReplacement)
					return Result.fromUnchanged(this);

				final Object[] nodesReplacement = ArrayUtils.arraycopyAndSet(nodes, bitIndex, subNodeReplacement);

				return Result.fromModified(new InplaceIndexNode<K>(
						bitmap, valmap, nodesReplacement, cachedSize + 1));		
			}
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Result<K> updated(AtomicReference<Thread> mutator, K key, int hash, int shift, Comparator<Object> comparator) {
			final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((bitmap & bitpos) == 0) { // no value
				InplaceIndexNode<K> editableNode = editAndInsert(mutator, valIndex(bitpos), key);
				editableNode.updateMetadata(bitmap | bitpos, valmap | bitpos, cachedSize + 1,
						cachedValmapBitCount + 1);			
				return Result.fromModified(editableNode);
			}

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (comparator.compare(nodes[valIndex], key) == 0) {
					return Result.fromUnchanged(this);
				} else {					
					// TODO: simplify this line
					int bitIndexNewOffset = Integer.bitCount(valmap & ~bitpos);
					int bitIndexNew = bitIndexNewOffset + Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos)) & (bitpos - 1)); 								
					AbstractNode<K> nodeNew = mergeNodes(nodes[valIndex], nodes[valIndex].hashCode(), key, hash, shift + BIT_PARTITION_SIZE);				
					
					InplaceIndexNode<K> editableNode = editAndMoveToBack(mutator, valIndex, bitIndexNew, nodeNew);
					editableNode.updateMetadata(bitmap | bitpos, valmap & ~bitpos, cachedSize + 1,
							cachedValmapBitCount - 1);								
					return Result.fromModified(editableNode); 
				}
			} else { // node (not value)
				final int bitIndex = bitIndex(bitpos);
				final AbstractNode<K> subNode = (AbstractNode<K>) nodes[bitIndex];
										
				final Result<K> resultNode = subNode.updated(mutator, key, hash, shift + BIT_PARTITION_SIZE, comparator);
				
				if (resultNode.isModified()) {
					InplaceIndexNode<K> editableNode = editAndSet(mutator, bitIndex, resultNode.getNode());
					editableNode.updateMetadata(bitmap, valmap, cachedSize + 1, cachedValmapBitCount);
					return Result.fromModified(editableNode);
				} else {
					return Result.fromUnchanged(subNode);
				}
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
		InplaceIndexNode<K> editAndMoveToFront(AtomicReference<Thread> mutator, int indexOld, int indexNew, Object elementNew) {
			Object[] editableNodes = ArrayUtils.arraycopyAndMoveToFront(this.nodes, indexOld, indexNew, elementNew);
			
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
		
		@SuppressWarnings("unchecked")
		@Override
		public Result<K> removed(K key, int hash, int shift, Comparator<Object> comparator) {
			final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((bitmap & bitpos) == 0)
				return Result.fromUnchanged(this);

			if ((valmap & bitpos) == 0) {
				// it's a TrieSet node, not a inplace value
				final int bitIndex = bitIndex(bitpos);
				final AbstractNode<K> subNode = (AbstractNode<K>) nodes[bitIndex];
				
				final Result<K> result = subNode.removed(key, hash, shift + BIT_PARTITION_SIZE, comparator);	
				
				if (!result.isModified())
					return Result.fromUnchanged(this);

				final AbstractNode<K> subNodeReplacement = result.getNode();

				if (this.arity() == 1) {
					if (subNodeReplacement == EMPTY_NODE) {
						// escalate empty result
						return result;
					} else if (subNodeReplacement.size() == 1) {
						// escalate singleton replacement element
						return result;			
					} else {
						assert subNodeReplacement.size() >= 2;   
						
						// modify current node (set replacement node)
						final Object[] nodesReplacement = ArrayUtils.arraycopyAndSet(nodes, bitIndex, subNodeReplacement);
						return Result.fromModified(new InplaceIndexNode<K>(bitmap, valmap, nodesReplacement, cachedSize - 1));
					}
				} else {
					assert this.arity() >= 2;
					
					if (subNodeReplacement == EMPTY_NODE) {
						// remove node
						final Object[] nodesReplacement = ArrayUtils.arraycopyAndRemove(nodes, bitIndex);
						return Result.fromModified(new InplaceIndexNode<K>(bitmap & ~bitpos, valmap, nodesReplacement, cachedSize - 1));						
					} else if (subNodeReplacement.size() == 1) {
						// inline value (move to front)						
						final int valIndexNew = Integer.bitCount((valmap | bitpos) & (bitpos - 1));
						
						final Object[] nodesReplacement = ArrayUtils.arraycopyAndMoveToFront(nodes, bitIndex, valIndexNew, subNodeReplacement.head());
						return Result.fromModified(new InplaceIndexNode<K>(bitmap, valmap | bitpos, nodesReplacement, cachedSize - 1));
					} else {
						// modify current node (set replacement node)
						final Object[] nodesReplacement = ArrayUtils.arraycopyAndSet(nodes, bitIndex, subNodeReplacement);
						return Result.fromModified(new InplaceIndexNode<K>(bitmap, valmap, nodesReplacement, cachedSize - 1));						
					}					
				}			
			} else {				
				// it's an inplace value
				final int valIndex = valIndex(bitpos);

				if (comparator.compare(nodes[valIndex], key) != 0) {
					return Result.fromUnchanged(this);
				} else {
					if (this.arity() == 1) { // && this.size() == 1
						return Result.fromModified(EMPTY_NODE);
					} else if (this.arity() == 2 && valmap == bitmap) {
						Object value = (valIndex == 0) ? nodes[1] : nodes[0];
						int map = 1 << (value.hashCode() & BIT_PARTITION_MASK);
						return Result.fromModified(new InplaceIndexNode<K>(map, map, value, cachedSize - 1));
					} else {
						final Object[] nodesReplacement = ArrayUtils.arraycopyAndRemove(nodes, valIndex);
						return Result.fromModified(new InplaceIndexNode<K>(bitmap & ~bitpos, valmap & ~bitpos, nodesReplacement, cachedSize - 1));
					}
				}
			}
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Result<K> removed(AtomicReference<Thread> mutator, K key, int hash, int shift, Comparator<Object> comparator) {
			final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((bitmap & bitpos) == 0) {
				return Result.fromUnchanged(this);
			}

			if ((valmap & bitpos) == 0) {
				// it's a TrieSet node, not a inplace value
				final int bitIndex = bitIndex(bitpos);
				
				final AbstractNode<K> subNode = (AbstractNode<K>) nodes[bitIndex];
		
				final Result<K> result = subNode.removed(mutator, key, hash, shift + BIT_PARTITION_SIZE, comparator);
				
				if (!result.isModified())
					return Result.fromUnchanged(this);
				
				final AbstractNode<K> subNodeReplacement = result.getNode();
			
				if (this.arity() == 1) {
					if (subNodeReplacement == EMPTY_NODE) {
						// escalate empty result
						return result;
					} else if (subNodeReplacement.size() == 1) {
						// escalate singleton replacement element
						return result;
					} else {
						assert subNodeReplacement.size() >= 2;   
						
						// modify current node (set replacement node)
						InplaceIndexNode<K> editableNode = editAndSet(mutator, bitIndex, subNodeReplacement);
						editableNode.updateMetadata(bitmap, valmap, cachedSize - 1, cachedValmapBitCount);
						return Result.fromModified(editableNode);
					}
				} else {
					assert this.arity() >= 2;
					
					if (subNodeReplacement == EMPTY_NODE) {
						// remove node
						InplaceIndexNode<K> editableNode = editAndRemove(mutator, bitIndex);
						editableNode.updateMetadata(bitmap & ~bitpos, valmap, cachedSize - 1, cachedValmapBitCount);
						return Result.fromModified(editableNode);		
					} else if (subNodeReplacement.size() == 1) {
						// inline value (move to front)
						final int valIndexNew = Integer.bitCount((valmap | bitpos) & (bitpos - 1));
						
						InplaceIndexNode<K> editableNode = editAndMoveToFront(mutator, bitIndex, valIndexNew, subNodeReplacement.head());
						editableNode.updateMetadata(bitmap, valmap | bitpos, cachedSize - 1, cachedValmapBitCount + 1);
						return Result.fromModified(editableNode);
					} else {
						// modify current node (set replacement node)
						InplaceIndexNode<K> editableNode = editAndSet(mutator, bitIndex, subNodeReplacement);
						editableNode.updateMetadata(bitmap, valmap, cachedSize - 1, cachedValmapBitCount);
						return Result.fromModified(editableNode);					
					}					
				}			
			} else {
				// it's an inplace value
				final int valIndex = valIndex(bitpos);
				
				if (comparator.compare(nodes[valIndex], key) != 0) {
					return Result.fromUnchanged(this);
				} else {
					if (this.arity() == 1) { // && this.size() == 1
						return Result.fromModified(EMPTY_NODE);
					} else if (this.arity() == 2 && valmap == bitmap) {
						Object value = (valIndex == 0) ? nodes[1] : nodes[0];
						int map = 1 << (value.hashCode() & BIT_PARTITION_MASK);
						return Result.fromModified(new InplaceIndexNode<K>(mutator, map, map, new Object[]{value}, cachedSize - 1));
					} else {
						InplaceIndexNode<K> editableNode = editAndRemove(mutator, valIndex);
						editableNode.updateMetadata(this.bitmap & ~bitpos,
								this.valmap & ~bitpos, cachedSize - 1, cachedValmapBitCount - 1);
						return Result.fromModified(editableNode);
					}								
				}
			}
		}
		
		@SuppressWarnings("unchecked")
		@Override
		Iterator<K> valueIterator() {
//			if (cachedValSize == 0) 
//				return Collections.emptyIterator();
//			else
				return (Iterator<K>) ArrayIterator.of(nodes, 0, cachedValmapBitCount);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
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
		int valueSize() {	
			return cachedValmapBitCount;
		}
		
		@Override
		boolean hasNodes() {
			return cachedValmapBitCount != nodes.length;
		}

		@Override
		int nodeSize() {	
			return nodes.length - cachedValmapBitCount;
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
			InplaceIndexNode<?> that = (InplaceIndexNode<?>) other;
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

	private static final class HashCollisionNode<K> extends AbstractNode<K> {
		private final K[] keys;
		private final int hash;

		HashCollisionNode(int hash, K[] keys) {
			this.keys = keys;
			this.hash = hash;
		}
		
		@Override
		Iterator<K> valueIterator() {
			return ArrayIterator.of(keys);
		}

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
		 * returns a new immutable {@link TrieSet} instance.
		 */
		@SuppressWarnings("unchecked")
		@Override
		Result<K> updated(K key, int hash, int shift, Comparator<Object> comparator) {
			if (this.hash != hash)
				return Result.fromModified(mergeNodes(
						(AbstractNode<K>) this, this.hash, key, hash, shift));

			if (contains(key, hash, shift, comparator))
				return Result.fromUnchanged(this);

			final K[] keysNew = (K[]) ArrayUtils.arraycopyAndInsert(keys, keys.length, key);
			return Result.fromModified(new HashCollisionNode<>(hash,
					keysNew));
		}

		@Override
		Result<K> updated(AtomicReference<Thread> mutator, K key, int hash,
				int shift, Comparator<Object> cmp) {	
			return updated(key, hash, shift, cmp);
		}

		/**
		 * Removes an object if present. Note, that this implementation always
		 * returns a new immutable {@link TrieSet} instance.
		 */
		@SuppressWarnings("unchecked")
		@Override
		Result<K> removed(K key, int hash, int shift, Comparator<Object> comparator) {
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
		int valueSize() {	
			return keys.length;
		}	

		@Override
		boolean hasNodes() {
			return false;
		}

		@Override
		int nodeSize() {	
			return 0;
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

			HashCollisionNode<?> other = (HashCollisionNode<?>) obj;
			
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
				if (!other.contains(key, 0, 0, TrieSet.equalityComparator())) {
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

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (other == null)
			return false;
		
		if (other instanceof TrieSet) {
			TrieSet<?> that = (TrieSet<?>) other;

			if (this.size() != that.size())
				return false;

			return rootNode.equals(that.rootNode);
		}
		
		return super.equals(other);
	}
	
}
