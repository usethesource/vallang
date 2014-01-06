/*******************************************************************************
 * Copyright (c) 2013 CWI
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

@SuppressWarnings("rawtypes")
public abstract class TrieSet<K> extends AbstractImmutableSet<K> {

	protected static final int BIT_PARTITION_SIZE = 5;
	protected static final int BIT_PARTITION_MASK = 0x1f;

	protected static final TrieSet EMPTY = new InplaceIndexNode(0, 0, new TrieSet[0], 0);

	@SafeVarargs
	public static final <K> ImmutableSet<K> of(K... elements) {
		@SuppressWarnings("unchecked")
		ImmutableSet<K> result = TrieSet.EMPTY;
		for (K k : elements) result = result.__insert(k);
		return result;
	}
	
	@SafeVarargs
	public static final <K> TransientSet<K> transientOf(K... elements) {
		TransientSet<K> transientSet = new TransientTrieSet<>();
		for (K k : elements) 
			transientSet.__insert(k);
		return transientSet;
	}
		
//	@SuppressWarnings("unchecked")
//	protected static final <K> Comparator<K> equalityComparator() {
//		return EqualityUtils.getDefaultEqualityComparator();
//	}
	
	@SuppressWarnings("unchecked")
	protected static final <K> Comparator<K> equivalenceComparator() {
		return EqualityUtils.getEquivalenceComparator();
	}
	
	@SuppressWarnings("unchecked")
	static <K> TrieSet<K> mergeNodes(Object node0, int hash0, Object node1, int hash1, int shift) {
		assert (!(node0 instanceof TrieSet));
		assert (!(node1 instanceof TrieSet));

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
			final TrieSet node = mergeNodes(node0, hash0, node1, hash1, shift + BIT_PARTITION_SIZE);

			return new InplaceIndexNode<>(bitmap, valmap, node, 1);
		}
	}

	static <K> TrieSet<K> mergeNodes(TrieSet node0, int hash0, TrieSet node1, int hash1, int shift) {
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
			final TrieSet node = mergeNodes(node0, hash0, node1, hash1, shift + BIT_PARTITION_SIZE);

			return new InplaceIndexNode<>(bitmap, valmap, node, node0.size());
		}
	}

	static <K> TrieSet<K> mergeNodes(TrieSet node0, int hash0, Object node1, int hash1, int shift) {
		assert (!(node1 instanceof TrieSet));

		final int mask0 = (hash0 >>> shift) & BIT_PARTITION_MASK;
		final int mask1 = (hash1 >>> shift) & BIT_PARTITION_MASK;

		if (mask0 != mask1) {
			// both nodes fit on same level
			final int bitmap = (1 << mask0) | (1 << mask1);
			final int valmap = (1 << mask1);
			final Object[] nodes = new Object[2];

			if (mask0 < mask1) {
				nodes[0] = node0;
				nodes[1] = node1;
			} else {
				nodes[0] = node1;
				nodes[1] = node0;
			}

			return new InplaceIndexNode<>(bitmap, valmap, nodes, node0.size() + 1);
		} else {
			// values fit on next level
			final int bitmap = (1 << mask0);
			final int valmap = 0;
			final TrieSet node = mergeNodes(node0, hash0, node1, hash1, shift + BIT_PARTITION_SIZE);

			return new InplaceIndexNode<>(bitmap, valmap, node, node.size());
		}
	}

	abstract TrieSet<K> updated(K key, int hash, int shift, boolean doMutate, Comparator<Object> cmp);

	@Override
	public TrieSet<K> __insert(K k) {
		return updated(k, k.hashCode(), 0, false, equivalenceComparator());
	}

	@Override
	public TrieSet<K> __insertEquivalent(K k, Comparator<Object> cmp) {
		return updated(k, k.hashCode(), 0, false, cmp);
	}
	
	/*
	 * TODO: support fast batch operations.
	 */
	@Override
	public ImmutableSet<K> __insertAll(Set<? extends K> set) {
		@SuppressWarnings("unchecked")
		TrieSet<K> result = (TrieSet<K>) TrieSet.of();		
		for (K e : set)
			result = result.updated(e, e.hashCode(), 0, false, equivalenceComparator());		
		return result;
	}	

	/*
	 * TODO: support fast batch operations.
	 */
	@Override
	public ImmutableSet<K> __insertAllEquivalent(Set<? extends K> set, Comparator<Object> cmp) {
		@SuppressWarnings("unchecked")
		TrieSet<K> result = (TrieSet<K>) TrieSet.of();		
		for (K e : set)
			result = result.updated(e, e.hashCode(), 0, false, cmp);		
		return result;
	}
	
	abstract TrieSet<K> removed(K key, int hash, int shift, boolean doMutate, Comparator<Object> comparator);

	@Override
	public TrieSet<K> __remove(K k) {
		return removed(k, k.hashCode(), 0, false, equivalenceComparator());
	}

	@Override
	public TrieSet<K> __removeEquivalent(K k, Comparator<Object> cmp) {
		return removed(k, k.hashCode(), 0, false, cmp);
	}
	
	abstract boolean contains(Object key, int hash, int shift, Comparator<Object> comparator);

	@Override
	public boolean contains(Object o) {
		return contains(o, o.hashCode(), 0, equivalenceComparator());
	}
	
	@Override
	public boolean containsEquivalent(Object o, Comparator<Object> cmp) {
		return contains(o, o.hashCode(), 0, cmp);
	}

	abstract Iterator<K> flatIterator();
	
}

@SuppressWarnings("rawtypes")
/*package*/ final class InplaceIndexNode<K> extends TrieSet<K> {

	private int bitmap;
	private int valmap;
	private Object[] nodes;
	private int cachedSize;

	InplaceIndexNode(int bitmap, int valmap, Object[] nodes, int cachedSize) {
		assert (Integer.bitCount(bitmap) == nodes.length);

		this.bitmap = bitmap;
		this.valmap = valmap;
		this.nodes = nodes;

		this.cachedSize = cachedSize;
	}

	InplaceIndexNode(int bitmap, int valmap, Object node, int cachedSize) {
		this(bitmap, valmap, new Object[]{node}, cachedSize);
	}

	final int index(int bitpos) {
		return Integer.bitCount(bitmap & (bitpos - 1));
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean contains(Object key, int hash, int shift, Comparator<Object> comparator) {
		final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
		final int bitpos = (1 << mask);

		if ((bitmap & bitpos) != 0) {			
			if ((valmap & bitpos) != 0) {
				return comparator.compare(nodes[index(bitpos)], key) == 0;
			} else {
				return ((TrieSet<K>) nodes[index(bitpos)]).contains(key, hash, shift + BIT_PARTITION_SIZE, comparator);
			}
		}
		return false;
	}

	@Override
	public TrieSet<K> updated(K key, int hash, int shift, boolean doMutate, Comparator<Object> comparator) {
		final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
		final int bitpos = (1 << mask);
		final int index = index(bitpos);

		if ((bitmap & bitpos) == 0) {
			// no entry, create new node with inplace value		
			final Object[] nodesReplacement = ArrayUtils.arraycopyAndInsert(nodes, index, key);
			
			if (doMutate) {
				// mutate state
				nodes = nodesReplacement;
				bitmap |= bitpos;
				valmap |= bitpos;
				cachedSize = cachedSize + 1;				
				return this;
			} else {
				// immutable copy
				return new InplaceIndexNode<>(bitmap | bitpos, valmap | bitpos, nodesReplacement, this.size() + 1);	
			}
		}

		if ((valmap & bitpos) != 0) {
			// it's an inplace value
			if (comparator.compare(nodes[index], key) == 0)
				return this;

			final TrieSet nodeNew = mergeNodes(nodes[index], nodes[index].hashCode(), key, hash, shift + BIT_PARTITION_SIZE);
			
			if (doMutate) {
				// mutate state
				nodes[index] = nodeNew;
				bitmap |=  bitpos;
				valmap &= ~bitpos;
				cachedSize = cachedSize + 1;
				return this;
			} else {
				// immutable copy
				final Object[] nodesReplacement = ArrayUtils.arraycopyAndSet(nodes, index, nodeNew);
				return new InplaceIndexNode<>(bitmap | bitpos, valmap & ~bitpos, nodesReplacement, this.size() + 1);				
			}			
		} else {
			// it's a TrieSet node, not a inplace value
			@SuppressWarnings("unchecked")
			final TrieSet<K> subNode = (TrieSet<K>) nodes[index];

			if (doMutate) {
				// mutate subNode state
				final int oldSize = subNode.size();
				
				/*
				 * Returned node might be different, e.g. HashCollisionNode can
				 * become nested in a InplaceIndexNode.
				 */
				final TrieSet<K> subNodeReplacement = subNode.updated(key, hash, shift + BIT_PARTITION_SIZE, true, comparator);
				nodes[index] = subNodeReplacement;
				
				if (oldSize != subNodeReplacement.size()) {
					// update this node's statistics
					cachedSize = cachedSize + 1;
				}
				return this;					
			} else {
				// immutable copy subNode
				final TrieSet<K> subNodeReplacement = subNode.updated(key, hash, shift + BIT_PARTITION_SIZE, false, comparator);
	
				if (subNode == subNodeReplacement)
					return this;
	
				assert(subNode.size() == subNodeReplacement.size() - 1);
	
				final Object[] nodesReplacement = ArrayUtils.arraycopyAndSet(nodes, index, subNodeReplacement);
				return new InplaceIndexNode<>(bitmap, valmap, nodesReplacement, this.size() + 1);			
			}
		}
	}

	@Override
	public TrieSet<K> removed(K key, int hash, int shift, boolean doMutate, Comparator<Object> comparator) {
		final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
		final int bitpos = (1 << mask);
		final int index = index(bitpos);

		if ((bitmap & bitpos) == 0)
			return this;

		if ((valmap & bitpos) == 0) {
			// it's a TrieSet node, not a inplace value
			@SuppressWarnings("unchecked")
			final TrieSet<K> subNode = (TrieSet<K>) nodes[index];
			
			if (doMutate) {
				final int oldSize = subNode.size();
				
				/*
				 * Returned node might be different, e.g. HashCollisionNode can
				 * become nested in a InplaceIndexNode.
				 */
				final TrieSet<K> subNodeReplacement = subNode.removed(key, hash, shift + BIT_PARTITION_SIZE, true, comparator);
				nodes[index] = subNodeReplacement;
				
				if (oldSize != subNodeReplacement.size()) {
					// update this node's statistics
					cachedSize = cachedSize - 1;
				}	
				return this;
			} else {				
				final TrieSet<K> subNodeReplacement = subNode.removed(key, hash, shift + BIT_PARTITION_SIZE, false, comparator);
				
				if (subNode == subNodeReplacement)
					return this;
				
				// TODO: optimization if singleton element node is returned
				final Object[] nodesReplacement = ArrayUtils.arraycopyAndSet(nodes, index, subNodeReplacement);
				return new InplaceIndexNode<>(bitmap, valmap, nodesReplacement, this.size() - 1);
			}
		} else {
			// it's an inplace value
			if (comparator.compare(nodes[index], key) != 0)
				return this;

			if (doMutate) {
				// TODO: optimization if singleton element node is returned
				this.nodes = ArrayUtils.arraycopyAndRemove(nodes, index);
				this.bitmap &= ~bitpos;
				this.valmap &= ~bitpos;
				this.cachedSize = cachedSize - 1;
				return this;
			} else {							
				// TODO: optimization if singleton element node is returned
				final Object[] nodesReplacement = ArrayUtils.arraycopyAndRemove(nodes, index);
				return new InplaceIndexNode<>(bitmap & ~bitpos, valmap & ~bitpos, nodesReplacement, this.size() - 1);
			}
		}
	}

	@Override
	public int size() {
		return cachedSize;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<K> iterator() {
		return new RecursiveIterator(nodes);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Iterator<K> flatIterator() {
		return (Iterator<K>) ArrayIterator.of(nodes);
	}

	/**
	 * Recursive iterator: if an element is of type iterator, the elements
	 * iterator will be added, instead of the element itself.
	 *
	 * Underlying implementation assumption: no iterable element returns and empty iterator.
	 */
	private static class RecursiveIterator implements Iterator {

		final Stack<Iterator> itStack;

		RecursiveIterator(Object[] array) {
			itStack = new Stack<>();
			itStack.push(ArrayIterator.of(array));
		}

		@Override
		public boolean hasNext() {
			while (!itStack.isEmpty()) {
				if (itStack.peek().hasNext())
					return true;
				else
					itStack.pop();
			}
			return false;
		}

		@Override
		public Object next() {
			if (!hasNext()) throw new NoSuchElementException();
			final Object next = itStack.peek().next();

			if (next instanceof TrieSet) {
				itStack.push(((TrieSet) next).flatIterator());

				assert hasNext();
				return next();
			} else {
				return next;
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

}	

@SuppressWarnings("rawtypes")
/*package*/ final class HashCollisionNode<K> extends TrieSet<K> {

	private final K[] keys;
	private final int hash;

	HashCollisionNode(int hash, K[] keys) {
		this.keys = keys;
		this.hash = hash;
	}

	@Override
	public int size() {
		return keys.length;
	}

	@Override
	public Iterator<K> iterator() {
		return ArrayIterator.of(keys);
	}
	
	@Override
	public Iterator<K> flatIterator() {
		return ArrayIterator.of(keys);
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
	public TrieSet<K> updated(K key, int hash, int shift, boolean doMutate, Comparator comparator) {
		if (this.hash != hash)
			return mergeNodes((TrieSet) this, this.hash, key, hash, shift);

		if (contains(key, hash, shift, comparator))
			return this;

		final K[] keysNew = (K[]) ArrayUtils.arraycopyAndInsert(keys, keys.length, key);
		return new HashCollisionNode<>(hash, keysNew);
	}

	/**
	 * Removes an object if present. Note, that this implementation always
	 * returns a new immutable {@link TrieSet} instance.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public TrieSet<K> removed(K key, int hash, int shift, boolean doMutation, Comparator comparator) {
		// TODO: optimize in general
		// TODO: optimization if singleton element node is returned

		for (int i = 0; i < keys.length; i++) {
			if (comparator.compare(keys[i], key) == 0)
				return new HashCollisionNode<>(hash, (K[]) ArrayUtils.arraycopyAndRemove(keys, i));
		}
		return this;
	}

}

/*
 * TODO: exchange TrieSet.equivalenceComparator() with standard equality operator
 */
class TransientTrieSet<E> implements TransientSet<E> {
	
	protected TrieSet<E> content;
	
	/*package*/ TransientTrieSet() {
		// allocated a new empty instance, because transient allows inplace modification.
		content = new InplaceIndexNode<>(0, 0, new TrieSet[0], 0);
	}
	
	@Override
	public boolean __insert(E e) {
		return __insertEquivalent(e, TrieSet.equivalenceComparator());
	}

	@Override
	public boolean __insertEquivalent(E e, Comparator<Object> cmp) {
		int sizeBeforeUpdate = content.size();
		content = content.updated(e, e.hashCode(), 0, true, cmp);
		
		return sizeBeforeUpdate != content.size();
	}

	@Override
	public boolean __remove(E e) {
		return __removeEquivalent(e, TrieSet.equivalenceComparator());
	}

	@Override
	public boolean __removeEquivalent(E e, Comparator<Object> cmp) {
		int sizeBeforeUpdate = content.size();
		content = content.removed((E) e, e.hashCode(), 0, true, cmp);

		return sizeBeforeUpdate != content.size();
	}

	@Override
	public boolean __insertAll(Set<? extends E> set) {
		return __insertAllEquivalent(set, TrieSet.equivalenceComparator());
	}

	@Override
	public boolean __insertAllEquivalent(Set<? extends E> set,
			Comparator<Object> cmp) {
		boolean modified = false;

		for (E e : set)
			modified |= __insertEquivalent(e, cmp);
						
		return modified;	
	}	
	
//	@Override
//	public boolean removeAll(Collection<?> c) {
//		boolean modified = false;
//
//		for (Object o : c)
//			modified |= remove(o);
//						
//		return modified;
//	}
//
//	@Override
//	public boolean retainAll(Collection<?> c) {
//		throw new UnsupportedOperationException();
//	}
//
//	@Override
//	public void clear() {
//		// allocated a new empty instance, because transient allows inplace modification.
//		content = new InplaceIndexNode<>(0, 0, new TrieSet[0], 0);
//	}	
//
//	@Override
//	public Iterator<E> iterator() {
//		return content.iterator();
//	}
//
//	@Override
//	public int size() {
//		return content.size();
//	}
//
//	@Override
//	public boolean isEmpty() {
//		return content.isEmpty();
//	}
//
//	@Override
//	public boolean contains(Object o) {
//		return content.contains(o);
//	}
//	
//	@Override
//	public boolean containsEquivalent(Object o, Comparator<Object> cmp) {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public Object[] toArray() {
//		return content.toArray();
//	}
//
//	@Override
//	public <T> T[] toArray(T[] a) {
//		return content.toArray(a);
//	}
//
//	@Override
//	public boolean containsAll(Collection<?> c) {
//		return content.containsAll(c);
//	}

	@Override
	public boolean equals(Object o) {
		return content.equals(o);
	}
	
	@Override
	public int hashCode() {
		return content.hashCode();
	}

	@Override
	public String toString() {
		return content.toString();
	}
	
	@Override
	public ImmutableSet<E> freeze() {
		return content;
	}
	
}