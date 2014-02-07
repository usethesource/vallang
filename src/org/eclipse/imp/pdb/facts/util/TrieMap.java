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

import static org.eclipse.imp.pdb.facts.util.AbstractSpecialisedImmutableJdkMap.mapOf;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.*;

@SuppressWarnings("rawtypes")
public class TrieMap<K,V> extends AbstractImmutableMap<K,V> {

	@SuppressWarnings("unchecked")
	private static final TrieMap EMPTY_INDEX_MAP = new TrieMap(VerboseNode.EMPTY_INDEX_NODE);
	
	@SuppressWarnings("unchecked")	
	private static final TrieMap EMPTY_INPLACE_INDEX_MAP = new TrieMap(CompactNode.EMPTY_INPLACE_INDEX_NODE);
	
	final private AbstractNode<K,V> rootNode;
	
	TrieMap(AbstractNode<K,V> rootNode) {
		this.rootNode = rootNode;
	}	
	
	@SuppressWarnings("unchecked")
	public static final <K,V> ImmutableMap<K,V> of() {
//		return TrieMap.EMPTY_INDEX_MAP;
		return TrieMap.EMPTY_INPLACE_INDEX_MAP;
	}	
	
	@SuppressWarnings("unchecked")
	public static final <K,V> TransientMap<K,V> transientOf() {
//		return TrieMap.EMPTY_INDEX_MAP.asTransient();
		return TrieMap.EMPTY_INPLACE_INDEX_MAP.asTransient();
	}	
		
	@SuppressWarnings("unchecked")
	protected static final <K> Comparator<K> equalityComparator() {
		return EqualityUtils.getDefaultEqualityComparator();
	}
	
	private boolean invariant() {		
//		int _hash = 0; 
//		for (K key : this) {
//			_hash += key.hashCode();
//		}
//		return this.hashCode == _hash;
		return false;
	}
	
	@Override
	public TrieMap<K, V> __put(K k, V v) {
		return __putEquivalent(k, v, equalityComparator());
	}

	@Override
	public TrieMap<K, V> __putEquivalent(K k, V v, Comparator<Object> cmp) {
		final AbstractNode.Result<K, V> result = rootNode.updated(null, k, k.hashCode(), v, 0, cmp);

		if (result.isModified())
			return new TrieMap<K, V>(result.getNode());

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
	public TrieMap<K, V> __remove(K k) {
		return __removeEquivalent(k, equalityComparator());
	}

	@Override
	public TrieMap<K, V> __removeEquivalent(K k, Comparator<Object> cmp) {
		final AbstractNode.Result<K, V> result = rootNode.removed(null, k, k.hashCode(), 0, cmp);

		if (result.isModified())
			return new TrieMap<K, V>(result.getNode());

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
		return rootNode.size();
	}

	@Override
	public Iterator<K> keyIterator() {
		return new TrieMapIterator<>((CompactNode<K, V>) rootNode);
	}

	@Override
	public Iterator<V> valueIterator() {
		return new TrieMapValueIterator<>(new TrieMapIterator<>(
				(CompactNode<K, V>) rootNode));
	}	
	
	@Override
	public Iterator<Map.Entry<K, V>> entryIterator() {
		return new TrieMapEntryIterator<>(new TrieMapIterator<>(
				(CompactNode<K, V>) rootNode));
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
		
//	/**
//	 * Iterator that first iterates over inlined-values and then
//	 * continues depth first recursively.
//	 */
//	private static class TrieMapEntryIterator implements Iterator {
//
//		final Deque<Iterator<AbstractNode>> nodeIterationStack;
//		AbstractNode next;
//		
//		TrieMapEntryIterator(AbstractNode rootNode) {			
//			next = null;
//			nodeIterationStack = new ArrayDeque<>();
//			
//			if (rootNode.isLeaf()) {
//				next = rootNode;
//			} else if (rootNode.size() > 0) {
//				nodeIterationStack.push(rootNode.nodeIterator());
//			}
//		}
//
//		@Override
//		public boolean hasNext() {
//			while (true) {
//				if (next != null) {
//					return true;
//				} else {					
//					if (nodeIterationStack.isEmpty()) {
//						return false;
//					} else {
//						if (nodeIterationStack.peek().hasNext()) {
//							AbstractNode innerNode = nodeIterationStack.peek().next();						
//							
//							if (innerNode.isLeaf())
//								next = innerNode;
//							else if (innerNode.size() > 0) {
//								nodeIterationStack.push(innerNode.nodeIterator());
//							}
//							continue;
//						} else {
//							nodeIterationStack.pop();
//							continue;
//						}
//					}
//				}
//			}
//		}
//
//		@Override
//		public Object next() {
//			if (!hasNext()) throw new NoSuchElementException();
//	
//			Object result = next;
//			next = null;
//			
//			return result;
//		}
//
//		@Override
//		public void remove() {
//			throw new UnsupportedOperationException();
//		}
//	}

	/**
	 * Iterator that first iterates over inlined-values and then continues depth
	 * first recursively.
	 */
	private static class TrieMapIterator<K, V> implements SupplierIterator<K, V> {

		final Deque<Iterator<AbstractNode<K, V>>> nodeIteratorStack;
		SupplierIterator<K, V> valueIterator;

		TrieMapIterator(CompactNode<K, V> rootNode) {
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
							CompactNode<K, V> innerNode = (CompactNode<K, V>) nodeIteratorStack
									.peek().next();

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

	private static abstract class AbstractNode<K, V> {

		protected static final int BIT_PARTITION_SIZE = 5;
		protected static final int BIT_PARTITION_MASK = 0x1f;
				
		abstract boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> comparator);
		
		abstract Optional<Map.Entry<K,V>> findByKey(Object key, int hash, int shift, Comparator<Object> cmp);
		
		abstract Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val,
						int shift, Comparator<Object> cmp);
			
		abstract Result<K, V> removed(AtomicReference<Thread> mutator, K key, int hash, int shift,
						Comparator<Object> cmp);
		
		static boolean isAllowedToEdit(AtomicReference<Thread> x, AtomicReference<Thread> y) {
			return x != null && y != null && (x == y || x.get() == y.get());
		}
		
		// TODO: is also in CompactNode 
		abstract Iterator<AbstractNode<K, V>> nodeIterator();
		
//		abstract K getValue(int index);
//		
//		abstract public AbstractNode<K, V> getNode(int index);
		
		// TODO: replaces calls by hasNodes()
		abstract boolean isLeaf();
		
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
			@SuppressWarnings("unchecked")
			final AbstractNode<K, V>[] arrayNew = new AbstractNode[array.length];
			System.arraycopy(array, 0, arrayNew, 0, array.length);
			arrayNew[index] = elementNew;
			return arrayNew;
		}

		static <K, V> AbstractNode<K, V>[] arraycopyAndInsert(AbstractNode<K, V>[] array, int index, AbstractNode<K, V> elementNew) {
			@SuppressWarnings("unchecked")
			final AbstractNode<K, V>[] arrayNew = new AbstractNode[array.length + 1];
			System.arraycopy(array, 0, arrayNew, 0, index);
			arrayNew[index] = elementNew;
			System.arraycopy(array, index, arrayNew, index + 1, array.length - index);
			return arrayNew;
		}

		static <K, V> AbstractNode<K, V>[] arraycopyAndRemove(AbstractNode<K, V>[] array, int index) {
			@SuppressWarnings("unchecked")
			final AbstractNode<K, V>[] arrayNew = new AbstractNode[array.length - 1];
			System.arraycopy(array, 0, arrayNew, 0, index);
			System.arraycopy(array, index + 1, arrayNew, index, array.length - index - 1);
			return arrayNew;
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
	
	
	private static abstract class CompactNode<K, V> extends AbstractNode<K, V> {
		abstract boolean hasNodes();

		abstract Iterator<AbstractNode<K, V>> nodeIterator();

		abstract int nodeArity();
		
		abstract boolean hasValues();

		abstract SupplierIterator<K, V> valueIterator();

		abstract int valueArity();
		
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
			return (size() - valueArity() >= 2 * (arity() - valueArity()));
		}		
		
		static final CompactNode EMPTY_INPLACE_INDEX_NODE = new InplaceIndexNode(0, 0, new CompactNode[0], 0);

		@SuppressWarnings("unchecked")
		static <K, V> CompactNode<K, V> mergeNodes(K key0, int keyHash0, V val0, K key1, int keyHash1, V val1, int shift) {
			assert key0.equals(key1) == false;
			
			if (keyHash0 == keyHash1)
				return new InplaceHashCollisionNode<>(keyHash0, (K[]) new Object[] { key0, key1 },
								(V[]) new Object[] { val0, val1 });

			final int mask0 = (keyHash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (keyHash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level
				final int bitmap = (1 << mask0) | (1 << mask1);
				final int valmap = (1 << mask0) | (1 << mask1);
				final Object[] nodes = new Object[4];

				if (mask0 < mask1) {
					nodes[0] = key0;
					nodes[1] = val0;					
					nodes[2] = key1;
					nodes[3] = val1;
				} else {
					nodes[0] = key1;
					nodes[1] = val1;					
					nodes[2] = key0;
					nodes[3] = val0;
				}

				return new InplaceIndexNode<>(bitmap, valmap, nodes, 2);
			} else {
				// values fit on next level
				final int bitmap = (1 << mask0);
				final int valmap = 0;
				final CompactNode<K, V> node = mergeNodes(key0, keyHash0, val0, key1, keyHash1, val1, shift + BIT_PARTITION_SIZE);

				return new InplaceIndexNode<>(bitmap, valmap, node, 2);
			}
		}

		static <K, V> CompactNode<K, V> mergeNodes(CompactNode<K, V> node0, int keyHash0, K key1, int keyHash1, V val1, int shift) {
			final int mask0 = (keyHash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (keyHash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level
				final int bitmap = (1 << mask0) | (1 << mask1);
				final int valmap = (1 << mask1);
				final Object[] nodes = new Object[3];

				// store values before node
				nodes[0] = key1;
				nodes[1] = val1;
				nodes[2] = node0;

				return new InplaceIndexNode<>(bitmap, valmap, nodes, node0.size() + 1);
			} else {
				// values fit on next level
				final int bitmap = (1 << mask0);
				final int valmap = 0;
				final CompactNode<K, V> node = mergeNodes(node0, keyHash0, key1, keyHash1, val1, shift + BIT_PARTITION_SIZE);

				return new InplaceIndexNode<>(bitmap, valmap, node, node.size());
			}
		}
		
		@Override
		boolean isLeaf() {
			return false;
		};
	}

	private static abstract class VerboseNode<K, V> extends AbstractNode<K, V> {
		
		@SuppressWarnings("unchecked")
		static final AbstractNode EMPTY_INDEX_NODE = new IndexNode(0, new AbstractNode[0], 0);
		
		@SuppressWarnings("unchecked")
		static <K, V> VerboseNode<K, V> mergeNodes(VerboseNode<K, V> node0, int hash0, VerboseNode<K, V> node1,
				int hash1, int shift) {
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
	}
	
	private static final class InplaceIndexNode<K, V> extends CompactNode<K, V> {
		private AtomicReference<Thread> mutator;

		private int bitmap;
		private int valmap;
		private Object[] nodes;
		private int cachedSize;
		private int cachedValmapBitCount;

		InplaceIndexNode(AtomicReference<Thread> mutator, int bitmap, int valmap, Object[] nodes, int cachedSize) {
			assert (2 * Integer.bitCount(valmap) + Integer.bitCount(bitmap ^ valmap) == nodes.length);
			
			this.mutator = mutator;

			this.bitmap = bitmap;
			this.valmap = valmap;
			this.nodes = nodes;
			this.cachedSize = cachedSize;

			this.cachedValmapBitCount = Integer.bitCount(valmap);
		
			for (int i = 0; i < 2 * cachedValmapBitCount; i++)
				assert ((nodes[i] instanceof AbstractNode) == false);

			for (int i = 2 * cachedValmapBitCount; i < nodes.length; i++)
				assert ((nodes[i] instanceof AbstractNode) == true);
			
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
			return 2 * cachedValmapBitCount + Integer.bitCount((bitmap ^ valmap) & (bitpos - 1));
		}

		final int valIndex(int bitpos) {
			return 2 * Integer.bitCount(valmap & (bitpos - 1));
		}

		private void updateMetadata(int bitmap, int valmap, int cachedSize, int cachedValmapBitCount) {
			assert (2 * Integer.bitCount(valmap) + Integer.bitCount(bitmap ^ valmap) == nodes.length);

			this.bitmap = bitmap;
			this.valmap = valmap;
			this.cachedSize = cachedSize;
			this.cachedValmapBitCount = cachedValmapBitCount;
			
			for (int i = 0; i < cachedValmapBitCount; i++)
				assert ((nodes[i] instanceof AbstractNode) == false);

			for (int i = 2 * cachedValmapBitCount; i < nodes.length; i++)
				assert ((nodes[i] instanceof AbstractNode) == true);
			
			// assert invariant
			assert nodeInvariant();
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

					if (cmp.compare(nodes[valIndex+1], val) == 0)
						return Result.unchanged(this);			
					
					// update mapping
					final Object[] nodesNew = copyAndSet(nodes, valIndex + 1, val);
					return Result.modified(new InplaceIndexNode<K, V>(bitmap, valmap, nodesNew, cachedSize));
				}

				final AbstractNode<K, V> nodeNew = mergeNodes((K) nodes[valIndex], nodes[valIndex].hashCode(),
						(V) nodes[valIndex + 1], key, keyHash, val, shift + BIT_PARTITION_SIZE);

				final int offset = 2 * (cachedValmapBitCount - 1);
				final int index = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos)) & (bitpos - 1));

				InplaceIndexNode<K, V> editableNode = editAndMoveToBackPair(mutator, valIndex, offset + index, nodeNew);
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
			InplaceIndexNode<K, V> editableNode = editAndInsertPair(mutator, valIndex(bitpos), key, val);
			editableNode.updateMetadata(bitmap | bitpos, valmap | bitpos, cachedSize + 1, cachedValmapBitCount + 1);
			return Result.modified(editableNode);
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

				if (this.arity() == 1) {
					return Result.modified(EMPTY_INPLACE_INDEX_NODE);
				} else if (this.arity() == 2 && valmap == bitmap) {
					/*
					 * Create root node with singleton element. This node will
					 * be a) either be the new root returned, or b) unwrapped
					 * and inlined.
					 */
					final K theOtherKey = (K) ((valIndex == 0) ? nodes[1] : nodes[0]);
					final V theOtherVal = (V) ((valIndex == 0) ? nodes[3] : nodes[1]);
					return EMPTY_INPLACE_INDEX_NODE.updated(mutator, theOtherKey, theOtherKey.hashCode(), theOtherVal, 0, comparator);
				} else {
					InplaceIndexNode<K, V> editableNode = editAndRemovePair(mutator, valIndex);
					editableNode.updateMetadata(this.bitmap & ~bitpos, this.valmap & ~bitpos, cachedSize - 1,
							cachedValmapBitCount - 1);
					return Result.modified(editableNode);
				}
			}

			if ((bitmap & bitpos) != 0) { // node (not value)
				final int bitIndex = bitIndex(bitpos);
				final AbstractNode<K, V> subNode = (AbstractNode<K, V>) nodes[bitIndex];
				final Result<K, V> subNodeResult = subNode.removed(mutator, key, keyHash, shift + BIT_PARTITION_SIZE, comparator);

				if (!subNodeResult.isModified())
					return Result.unchanged(this);

				// TODO: rework result that it can take CompactNode instead of AbstractNode
				final CompactNode<K, V> subNodeNew = (CompactNode<K, V>) subNodeResult.getNode();
				
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
						InplaceIndexNode<K, V> editableNode0 = editAndRemovePair(mutator, bitIndex);
						editableNode0.updateMetadata(bitmap & ~bitpos, valmap, cachedSize - 1, cachedValmapBitCount);
						return Result.modified(editableNode0);

					case 1:
						// inline value (move to front)
						final int valIndexNew = Integer.bitCount((valmap | bitpos) & (bitpos - 1));

						InplaceIndexNode<K, V> editableNode1 = editAndMoveToFrontPair(mutator, bitIndex, valIndexNew,
								subNodeNew.headKey(), subNodeNew.headVal());
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

		InplaceIndexNode<K, V> editAndInsertPair(AtomicReference<Thread> mutator, int index,
						Object keyNew, Object valNew) {
			final Object[] editableNodes = copyAndInsertPair(this.nodes, index, keyNew, valNew);

			if (isAllowedToEdit(this.mutator, mutator)) {
				this.nodes = editableNodes;
				return this;
			}

			return new InplaceIndexNode<>(mutator, editableNodes);
		}

		InplaceIndexNode<K, V> editAndRemovePair(AtomicReference<Thread> mutator, int index) {
			final Object[] editableNodes = copyAndRemovePair(this.nodes, index);

			if (isAllowedToEdit(this.mutator, mutator)) {
				this.nodes = editableNodes;
				return this;
			}

			return new InplaceIndexNode<>(mutator, editableNodes);
		}

		InplaceIndexNode<K, V> editAndSet(AtomicReference<Thread> mutator, int index,
						Object elementNew) {
			if (isAllowedToEdit(this.mutator, mutator)) {
				// no copying if already editable
				this.nodes[index] = elementNew;
				return this;
			} else {
				final Object[] editableNodes = copyAndSet(this.nodes, index, elementNew);
				return new InplaceIndexNode<>(mutator, editableNodes);
			}
		}

		InplaceIndexNode<K, V> editAndMoveToBackPair(AtomicReference<Thread> mutator, int indexOld,
						int indexNew, Object elementNew) {
			final Object[] editableNodes = copyAndMoveToBackPair(this.nodes, indexOld, indexNew,
							elementNew);

			if (isAllowedToEdit(this.mutator, mutator)) {
				this.nodes = editableNodes;
				return this;
			}

			return new InplaceIndexNode<>(mutator, editableNodes);
		}

		InplaceIndexNode<K, V> editAndMoveToFrontPair(AtomicReference<Thread> mutator,
						int indexOld, int indexNew, Object keyNew, Object valNew) {
			final Object[] editableNodes = copyAndMoveToFrontPair(this.nodes, indexOld, indexNew,
							keyNew, valNew);

			if (isAllowedToEdit(this.mutator, mutator)) {
				this.nodes = editableNodes;
				return this;
			}

			return new InplaceIndexNode<>(mutator, editableNodes);
		}		
		
		@Override
		SupplierIterator<K, V> valueIterator() {
			final int offset = 2 * cachedValmapBitCount; 
			return ArrayKeyValueIterator.of(nodes, 0, offset);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<AbstractNode<K, V>> nodeIterator() {
			final int offset = 2 * cachedValmapBitCount; 
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
			final int offset = 2 * cachedValmapBitCount;
			return nodes.length - offset;
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
			return valueArity() + nodeArity();
		}

		@Override
		int size() {
			return cachedSize;
		}
	}	
	
	// replace by immutable cons list
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
				keysAndVals[2*i] = keys[i];
				keysAndVals[2*i+1] = vals[i];
			}
							
			return ArrayKeyValueIterator.of(keysAndVals);
		}

		@Override
		public String toString() {
			final Object[] keysAndVals = new Object[keys.length + vals.length];
			for (int i = 0; i < keys.length; i++) {
				keysAndVals[2*i] = keys[i];
				keysAndVals[2*i+1] = vals[i];
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
		Result<K, V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift, Comparator<Object> cmp) {
			if (this.hash != keyHash)
				return Result.modified(mergeNodes((CompactNode<K, V>) this, this.hash, key, keyHash, val, shift));

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
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int keyHash, int shift, Comparator<Object> cmp) {
			for (int i = 0; i < keys.length; i++) {
				if (cmp.compare(keys[i], key) == 0) {
					if (this.arity() == 1) {
						return Result.modified(EMPTY_INPLACE_INDEX_NODE);
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node will
						 * be a) either be the new root returned, or b) unwrapped
						 * and inlined.
						 */
						final K theOtherKey = (i == 0) ? keys[1] : keys[0];
						final V theOtherVal = (i == 0) ? vals[1] : vals[0];
						return EMPTY_INPLACE_INDEX_NODE.updated(mutator, theOtherKey, keyHash, theOtherVal, 0, cmp);
					} else {
						return Result.modified(new InplaceHashCollisionNode<>(keyHash, (K[]) copyAndRemove(keys, i),
								(V[]) copyAndRemove(vals, i)));
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
		
//		@Override
//		K getValue(int index) {
//			return keys[index];
//		}
//		
//		@Override
//		public AbstractNode<K, V> getNode(int index) {
//			throw new IllegalStateException("Is leaf node.");
//		}

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
		Optional<Map.Entry<K,V>> findByKey(Object key, int hash, int shift, Comparator<Object> cmp) {			
			for (int i = 0; i < keys.length; i++) {
				final K _key = keys[i];
				if (cmp.compare(key, _key) == 0) {
					final V _val = vals[i];
					return Optional.of((java.util.Map.Entry<K, V>) mapOf(_key, _val));					
				}				
			}
			return Optional.empty();
		}
	}
	
	private static final class IndexNode<K, V> extends VerboseNode<K, V> {

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
		
		IndexNode<K,V> editAndInsert(AtomicReference<Thread> mutator, int index, AbstractNode elementNew) {		
			AbstractNode<K, V>[] editableNodes = arraycopyAndInsert(this.nodes, index, elementNew);
			
			if (isAllowedToEdit(this.mutator, mutator)) {
				this.nodes = editableNodes;
				return this;
			} else {
				return new IndexNode<>(mutator, editableNodes);
			}
		}
		
		IndexNode<K,V> editAndSet(AtomicReference<Thread> mutator, int index, AbstractNode<K, V> elementNew) {
			if (isAllowedToEdit(this.mutator, mutator)) {
				nodes[index] = elementNew;
				return this;
			} else {
				final AbstractNode<K, V>[] editableNodes = arraycopyAndSet(this.nodes, index, elementNew);
				return new IndexNode<>(mutator, editableNodes);
			}
		}
		
		IndexNode<K,V> editAndRemove(AtomicReference<Thread> mutator, int index) {
			AbstractNode<K, V>[] editableNodes = arraycopyAndRemove(this.nodes, index);
			
			if (isAllowedToEdit(this.mutator, mutator)) {
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
		public Result<K,V> removed(AtomicReference<Thread> mutator, K key, int hash, int shift, Comparator<Object> comparator) {
			final int mask = (hash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);
			final int bitIndex = index(bitpos);

			if ((bitmap & bitpos) == 0) { // no value
				return Result.unchanged(this);
			} else { // nested node
				final AbstractNode<K,V> subNode = (AbstractNode<K,V>) nodes[bitIndex];
				
				final Result<K,V> result = subNode.removed(mutator, key, hash, shift + BIT_PARTITION_SIZE, comparator);	
				
				if (!result.isModified())
					return Result.unchanged(this);

				final AbstractNode<K,V> subNodeReplacement = result.getNode();
				
				if (this.arity() == 1 && (subNodeReplacement.size() == 0 || subNodeReplacement.size() == 1)) {
					// escalate (singleton or empty) result
					return result;
				} else {
					// modify current node (set replacement node)
					final IndexNode<K, V> editableNode = editAndSet(mutator, bitIndex,
									subNodeReplacement);
					editableNode.updateMetadata(bitmap, cachedSize - 1);
					return Result.modified(editableNode);

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

	private static final class LeafNode<K, V> extends VerboseNode<K, V> implements Map.Entry<K, V> {

		private final K key;
		private final V val;
		private final int keyHash;

		LeafNode(K key, int keyHash, V val) {
			this.key = key;
			this.val = val;
			this.keyHash = keyHash;
		}

		@Override
		Result<K,V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift, Comparator<Object> cmp) {
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
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int hash, int shift,
						Comparator<Object> cmp) {
			if (cmp.compare(this.key, key) == 0) {
				return Result.modified(EMPTY_INDEX_NODE);
			} else {
				return Result.unchanged(this);
			}
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
	
	// replace by immutable cons list
	private static final class LeafHashCollisionNode<K, V> extends VerboseNode<K, V> {
		private final AbstractNode<K,V>[] leafs;
		private final int hash;

		LeafHashCollisionNode(int hash, AbstractNode<K, V>[] leafs) {
			this.leafs = leafs;
			this.hash = hash;
		}
		
		@Override
		public String toString() {
			return Arrays.toString(leafs);
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
		Result<K,V> updated(AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift, Comparator<Object> cmp) {
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

		/**
		 * Removes an object if present. Note, that this implementation always
		 * returns a new immutable {@link TrieMap} instance.
		 */
		@Override
		Result<K, V> removed(AtomicReference<Thread> mutator, K key, int hash, int shift,
						Comparator<Object> cmp) {
			for (int i = 0; i < leafs.length; i++) {
				if (cmp.compare(leafs[i], key) == 0) {
					if (this.arity() == 1) {
						return Result.modified(EMPTY_INDEX_NODE);
					} else if (this.arity() == 2) {
						final AbstractNode<K, V> theOther = (i == 0) ? leafs[1] : leafs[0];
						return Result.modified(theOther);
					} else {
						return Result.modified(new LeafHashCollisionNode<>(hash,
										arraycopyAndRemove(leafs, i)));
					}
				}
			}
			return Result.unchanged(this);
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
			
			outerLoop:
			for (AbstractNode thisLeaf : this.leafs) {
				for (AbstractNode thatLeaf : that.leafs) {				
					if (thisLeaf.equals(thatLeaf)) {
						continue outerLoop; 
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
