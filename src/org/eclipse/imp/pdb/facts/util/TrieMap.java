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

import static org.eclipse.imp.pdb.facts.util.AbstractSpecialisedImmutableMap.entryOf;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndInsert;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndInsertPair;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndMoveToBackPair;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndMoveToFrontPair;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndRemove;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndRemovePair;
import static org.eclipse.imp.pdb.facts.util.ArrayUtils.copyAndSet;

import java.text.DecimalFormat;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("rawtypes")
public class TrieMap<K, V> extends AbstractImmutableMap<K, V> {

	@SuppressWarnings("unchecked")
	private static final TrieMap EMPTY_INPLACE_INDEX_MAP = new TrieMap(
					CompactMapNode.EMPTY_INPLACE_INDEX_NODE, 0, 0);

	private static final boolean USE_SPECIALIAZIONS = true;
	private static final boolean USE_STACK_ITERATOR = true; // does not effect TransientMap

	private final AbstractMapNode<K, V> rootNode;
	private final int hashCode;
	private final int cachedSize;

	TrieMap(AbstractMapNode<K, V> rootNode, int hashCode, int cachedSize) {
		this.rootNode = rootNode;
		this.hashCode = hashCode;
		this.cachedSize = cachedSize;
		assert invariant();
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> ImmutableMap<K, V> of() {
		return TrieMap.EMPTY_INPLACE_INDEX_MAP;
	}
	
	@SuppressWarnings("unchecked")
	public static final <K, V> ImmutableMap<K, V> of(Object... keyValuePairs) {
		if (keyValuePairs.length % 2 != 0) {
			throw new IllegalArgumentException(
							"Length of argument list is uneven: no key/value pairs.");
		}
		
		ImmutableMap<K, V> result = TrieMap.EMPTY_INPLACE_INDEX_MAP;
		
		for (int i = 0; i < keyValuePairs.length; i += 2) {
			final K key = (K) keyValuePairs[i];
			final V val = (V) keyValuePairs[i+1];
			
			result = result.__put(key, val);
		}
		
		return result;
	}	

	@SuppressWarnings("unchecked")
	public static final <K, V> TransientMap<K, V> transientOf() {
		return TrieMap.EMPTY_INPLACE_INDEX_MAP.asTransient();
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> TransientMap<K, V> transientOf(Object... keyValuePairs) {
		if (keyValuePairs.length % 2 != 0) {
			throw new IllegalArgumentException(
							"Length of argument list is uneven: no key/value pairs.");
		}
		
		final TransientMap<K, V> result = TrieMap.EMPTY_INPLACE_INDEX_MAP.asTransient();
		
		for (int i = 0; i < keyValuePairs.length; i += 2) {
			final K key = (K) keyValuePairs[i];
			final V val = (V) keyValuePairs[i+1];
			
			result.__put(key, val);
		}
		
		return result;
	}	
	
	private boolean invariant() {
		int _hash = 0;
		int _count = 0;

		for (Iterator<Map.Entry<K, V>> it = entryIterator(); it.hasNext();) {
			final Map.Entry<K, V> entry = it.next();
			
			_hash += entry.getKey().hashCode() ^ entry.getValue().hashCode();
			_count += 1;
		}

		return this.hashCode == _hash && this.cachedSize == _count;
	}

	@Override
	public TrieMap<K, V> __put(K key, V val) {
		final int keyHash = key.hashCode();
		final Result<K, V, ? extends AbstractMapNode<K, V>> result = rootNode.updated(null, key,
						keyHash, val, 0);

		if (result.isModified()) {
			if (result.hasReplacedValue()) {
				final int valHashOld = result.getReplacedValue().hashCode();
				final int valHashNew = val.hashCode();

				return new TrieMap<K, V>(result.getNode(), hashCode
								+ (keyHash ^ valHashNew) - (keyHash ^ valHashOld), cachedSize);
			}

			final int valHash = val.hashCode();
			return new TrieMap<K, V>(result.getNode(), hashCode + (keyHash ^ valHash),
							cachedSize + 1);
		}

		return this;
	}

	@Override
	public TrieMap<K, V> __putEquivalent(K key, V val, Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final Result<K, V, ? extends AbstractMapNode<K, V>> result = rootNode.updated(null, key,
						keyHash, val, 0, cmp);

		if (result.isModified()) {
			if (result.hasReplacedValue()) {
				final int valHashOld = result.getReplacedValue().hashCode();
				final int valHashNew = val.hashCode();

				return new TrieMap<K, V>(result.getNode(), hashCode
								+ (keyHash ^ valHashNew) - (keyHash ^ valHashOld), cachedSize);
			}

			final int valHash = val.hashCode();
			return new TrieMap<K, V>(result.getNode(), hashCode + (keyHash ^ valHash),
							cachedSize + 1);
		}

		return this;
	}

	@Override
	public ImmutableMap<K, V> __putAll(Map<? extends K, ? extends V> map) {
		TransientMap<K, V> tmp = asTransient();
		tmp.__putAll(map);
		return tmp.freeze();	
	}

	@Override
	public ImmutableMap<K, V> __putAllEquivalent(Map<? extends K, ? extends V> map,
					Comparator<Object> cmp) {
		TransientMap<K, V> tmp = asTransient();
		tmp.__putAllEquivalent(map, cmp);
		return tmp.freeze();
	}

	@Override
	public TrieMap<K, V> __remove(K key) {
		final int keyHash = key.hashCode();
		final Result<K, V, ? extends AbstractMapNode<K, V>> result = rootNode.removed(null, key,
						keyHash, 0);

		if (result.isModified()) {
			// TODO: carry deleted value in result
			// assert result.hasReplacedValue();
			// final int valHash = result.getReplacedValue().hashCode();

			final int valHash = rootNode.findByKey(key, keyHash, 0).get().getValue()
							.hashCode();

			return new TrieMap<K, V>(result.getNode(), hashCode - (keyHash ^ valHash),
							cachedSize - 1);
		}

		return this;	}

	@Override
	public TrieMap<K, V> __removeEquivalent(K key, Comparator<Object> cmp) {
		final int keyHash = key.hashCode();
		final Result<K, V, ? extends AbstractMapNode<K, V>> result = rootNode.removed(null, key,
						keyHash, 0, cmp);

		if (result.isModified()) {
			// TODO: carry deleted value in result
			// assert result.hasReplacedValue();
			// final int valHash = result.getReplacedValue().hashCode();

			final int valHash = rootNode.findByKey(key, keyHash, 0, cmp).get().getValue()
							.hashCode();

			return new TrieMap<K, V>(result.getNode(), hashCode - (keyHash ^ valHash),
							cachedSize - 1);
		}

		return this;
	}

	@Override
	public boolean containsKey(Object o) {
		return rootNode.containsKey(o, o.hashCode(), 0);
	}

	@Override
	public boolean containsKeyEquivalent(Object o, Comparator<Object> cmp) {
		return rootNode.containsKey(o, o.hashCode(), 0, cmp);
	}

	@Override
	public boolean containsValue(Object o) {
		for (Iterator<V> iterator = valueIterator(); iterator.hasNext();) {
			if (iterator.next().equals(o)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsValueEquivalent(Object o, Comparator<Object> cmp) {
		for (Iterator<V> iterator = valueIterator(); iterator.hasNext();) {
			if (cmp.compare(iterator.next(), o) == 0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public V get(Object key) {
		final Optional<Map.Entry<K, V>> result = rootNode.findByKey(key, key.hashCode(), 0);

		if (result.isPresent()) {
			return result.get().getValue();
		} else {
			return null;
		}
	}

	@Override
	public V getEquivalent(Object key, Comparator<Object> cmp) {
		final Optional<Map.Entry<K, V>> result = rootNode.findByKey(key, key.hashCode(), 0, cmp);

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
		if (USE_STACK_ITERATOR) {
			return new MapKeyIterator<>(rootNode);
		} else {
			return new TrieMapIterator<>((CompactMapNode<K, V>) rootNode);
		}
	}

	@Override
	public Iterator<V> valueIterator() {
		return new MapValueIterator<>(rootNode);
	}

	@Override
	public Iterator<Map.Entry<K, V>> entryIterator() {
		return new MapEntryIterator<>(rootNode);
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		Set<java.util.Map.Entry<K, V>> entrySet = null;

		if (entrySet == null) {
			entrySet = new AbstractSet<java.util.Map.Entry<K, V>>() {
				@Override
				public Iterator<java.util.Map.Entry<K, V>> iterator() {
					return new Iterator<Entry<K, V>>() {
						private final Iterator<Entry<K, V>> i = entryIterator();

						@Override
						public boolean hasNext() {
							return i.hasNext();
						}

						@Override
						public Entry<K, V> next() {
							return i.next();
						}

						@Override
						public void remove() {
							i.remove();
						}
					};
				}

				@Override
				public int size() {
					return TrieMap.this.size();
				}

				@Override
				public boolean isEmpty() {
					return TrieMap.this.isEmpty();
				}

				@Override
				public void clear() {
					TrieMap.this.clear();
				}

				@Override
				public boolean contains(Object k) {
					return TrieMap.this.containsKey(k);
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

		final Deque<Iterator<? extends CompactMapNode<K, V>>> nodeIteratorStack;
		SupplierIterator<K, V> valueIterator;

		TrieMapIterator(CompactMapNode<K, V> rootNode) {
			if (rootNode.hasPayload()) {
				valueIterator = rootNode.payloadIterator();
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
							CompactMapNode<K, V> innerNode = nodeIteratorStack.peek().next();

							if (innerNode.hasPayload())
								valueIterator = innerNode.payloadIterator();

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
	
	/**
	 * Iterator skeleton that uses a fixed stack in depth.
	 */
	private static abstract class AbstractMapIterator<K, V> {
		protected int currentValueCursor;
		protected int currentValueLength;
		protected AbstractMapNode<K, V> currentValueNode;

		private int currentStackLevel;
		private int[] nodeCursorsAndLengths = new int[16 * 2];

		@SuppressWarnings("unchecked")
		AbstractMapNode<K, V>[] nodes = new AbstractMapNode[16];

		AbstractMapIterator(AbstractMapNode<K, V> rootNode) {
			currentStackLevel = 0;

			currentValueNode = rootNode;
			currentValueCursor = 0;
			currentValueLength = rootNode.payloadArity();

			nodes[0] = rootNode;
			nodeCursorsAndLengths[0] = 0;
			nodeCursorsAndLengths[1] = rootNode.nodeArity();
		}

		public boolean hasNext() {
			if (currentValueCursor < currentValueLength) {
				return true;
			} else {
				/*
				 * search for next node that contains values
				 */
				while (currentStackLevel >= 0) {
					final int currentCursorIndex = currentStackLevel * 2;
					final int currentLengthIndex = currentCursorIndex + 1;
					
					final int nodeCursor = nodeCursorsAndLengths[currentCursorIndex];
					final int nodeLength = nodeCursorsAndLengths[currentLengthIndex];

					if (nodeCursor < nodeLength) {
						final AbstractMapNode<K, V> nextNode = nodes[currentStackLevel]
										.getNode(nodeCursor);
						nodeCursorsAndLengths[currentCursorIndex]++;

						final int nextValueLength = nextNode.payloadArity();
						final int nextNodeLength = nextNode.nodeArity();

						if (nextNodeLength > 0) {
							/*
							 * put node on next stack level for depth-first
							 * traversal
							 */
							final int nextStackLevel = ++currentStackLevel;
							final int nextCursorIndex = nextStackLevel * 2;
							final int nextLengthIndex = nextCursorIndex + 1;
							
							nodes[nextStackLevel] = nextNode;
							nodeCursorsAndLengths[nextCursorIndex] = 0;
							nodeCursorsAndLengths[nextLengthIndex] = nextNodeLength;
						}

						if (nextValueLength != 0) {
							/*
							 * found for next node that contains values
							 */
							currentValueNode = nextNode;
							currentValueCursor = 0;
							currentValueLength = nextValueLength;
							return true;
						}
					} else {
						currentStackLevel--;
					}
				}
			}

			return false;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private static final class MapKeyIterator<K, V> extends AbstractMapIterator<K, V> implements
					SupplierIterator<K, V> {

		MapKeyIterator(AbstractMapNode<K, V> rootNode) {
			super(rootNode);
		}

		@Override
		public K next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			} else {
				return currentValueNode.getKey(currentValueCursor++);
			}
		}

		@Override
		public V get() {
			throw new UnsupportedOperationException();
		}
	}

	private static final class MapValueIterator<K, V> extends AbstractMapIterator<K, V> implements
					SupplierIterator<V, K> {

		MapValueIterator(AbstractMapNode<K, V> rootNode) {
			super(rootNode);
		}

		@Override
		public V next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			} else {
				return currentValueNode.getValue(currentValueCursor++);
			}
		}

		@Override
		public K get() {
			throw new UnsupportedOperationException();
		}
	}

	private static final class MapEntryIterator<K, V> extends AbstractMapIterator<K, V> implements
					SupplierIterator<Map.Entry<K, V>, K> {

		MapEntryIterator(AbstractMapNode<K, V> rootNode) {
			super(rootNode);
		}

		@Override
		public Map.Entry<K, V> next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			} else {
				return currentValueNode.getKeyValueEntry(currentValueCursor++);
			}
		}

		@Override
		public K get() {
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

	static final class TransientTrieMap<K, V> extends AbstractMap<K, V> implements TransientMap<K, V> {
		final private AtomicReference<Thread> mutator;
		private AbstractMapNode<K, V> rootNode;
		private int hashCode;
		private int cachedSize;

		TransientTrieMap(TrieMap<K, V> trieMap) {
			this.mutator = new AtomicReference<Thread>(Thread.currentThread());
			this.rootNode = trieMap.rootNode;
			this.hashCode = trieMap.hashCode;
			this.cachedSize = trieMap.cachedSize;
			assert invariant();
		}

		// TODO: merge with TrieMap invariant (as function)
		private boolean invariant() {
			int _hash = 0;

			for (Iterator<Map.Entry<K, V>> it = entryIterator(); it.hasNext();) {
				final Map.Entry<K, V> entry = it.next();
				
				_hash += entry.getKey().hashCode() ^ entry.getValue().hashCode();
			}

			return this.hashCode == _hash;
		}

		@Override
		public boolean containsKey(Object o) {
			return rootNode.containsKey(o, o.hashCode(), 0);
		}

		@Override
		public boolean containsKeyEquivalent(Object o, Comparator<Object> cmp) {
			return rootNode.containsKey(o, o.hashCode(), 0, cmp);
		}

		@Override
		public V get(Object key) {
			final Optional<Map.Entry<K, V>> result = rootNode
							.findByKey(key, key.hashCode(), 0);

			if (result.isPresent()) {
				return result.get().getValue();
			} else {
				return null;
			}
		}

		@Override
		public V getEquivalent(Object key, Comparator<Object> cmp) {
			final Optional<Map.Entry<K, V>> result = rootNode
							.findByKey(key, key.hashCode(), 0, cmp);

			if (result.isPresent()) {
				return result.get().getValue();
			} else {
				return null;
			}
		}

		@Override
		public V __put(K key, V val) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final Result<K, V, ? extends AbstractMapNode<K, V>> result = rootNode.updated(mutator,
							key, keyHash, val, 0);

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
		public V __putEquivalent(K key, V val, Comparator<Object> cmp) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final Result<K, V, ? extends AbstractMapNode<K, V>> result = rootNode.updated(mutator,
							key, keyHash, val, 0, cmp);

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
		public boolean __remove(K key) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final Result<K, V, ? extends AbstractMapNode<K, V>> result = rootNode.removed(mutator,
							key, keyHash, 0);

			if (result.isModified()) {
				// TODO: carry deleted value in result
				// assert result.hasReplacedValue();
				// final int valHash = result.getReplacedValue().hashCode();

				final int valHash = rootNode.findByKey(key, keyHash, 0).get().getValue()
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
		public boolean __removeEquivalent(K key, Comparator<Object> cmp) {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			final int keyHash = key.hashCode();
			final Result<K, V, ? extends AbstractMapNode<K, V>> result = rootNode.removed(mutator,
							key, keyHash, 0, cmp);

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
			boolean modified = false;

			for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
				final boolean isPresent = containsKey(entry.getKey());
				final V replaced = __put(entry.getKey(), entry.getValue());

				if (!isPresent || replaced != null) {
					modified = true;
				}
			}

			return modified;
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

		@Override
		public Set<java.util.Map.Entry<K, V>> entrySet() {
			Set<java.util.Map.Entry<K, V>> entrySet = null;

			if (entrySet == null) {
				entrySet = new AbstractSet<java.util.Map.Entry<K, V>>() {
					@Override
					public Iterator<java.util.Map.Entry<K, V>> iterator() {
						return new Iterator<Entry<K, V>>() {
							private final Iterator<Entry<K, V>> i = entryIterator();

							@Override
							public boolean hasNext() {
								return i.hasNext();
							}

							@Override
							public Entry<K, V> next() {
								return i.next();
							}

							@Override
							public void remove() {
								i.remove();
							}
						};
					}

					@Override
					public int size() {
						return TransientTrieMap.this.size();
					}

					@Override
					public boolean isEmpty() {
						return TransientTrieMap.this.isEmpty();
					}

					@Override
					public void clear() {
						TransientTrieMap.this.clear();
					}

					@Override
					public boolean contains(Object k) {
						return TransientTrieMap.this.containsKey(k);
					}
				};
			}
			return entrySet;
		}		
		
		@Override
		public SupplierIterator<K, V> keyIterator() {
			return new TransientMapKeyIterator<>(this);
		}

		@Override
		public Iterator<V> valueIterator() {
//			return new TrieMapValueIterator<>(keyIterator());
			return new MapValueIterator<>(rootNode); // TODO: iterator does not support removal
		}

		@Override
		public Iterator<Map.Entry<K, V>> entryIterator() {
//			return new TrieMapEntryIterator<>(keyIterator());
			return new MapEntryIterator<>(rootNode); // TODO: iterator does not support removal
		}		
		
		/**
		 * Iterator that first iterates over inlined-values and then continues
		 * depth first recursively.
		 */
		private static class TransientMapKeyIterator<K, V> extends AbstractMapIterator<K, V>
						implements SupplierIterator<K, V> {

			final TransientTrieMap<K, V> transientTrieMap;
			K lastKey;

			TransientMapKeyIterator(TransientTrieMap<K, V> transientTrieMap) {
				super(transientTrieMap.rootNode);
				this.transientTrieMap = transientTrieMap;
			}

			@Override
			public K next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				} else {
					lastKey = currentValueNode.getKey(currentValueCursor++);
					return lastKey;
				}
			}

			@Override
			public V get() {
				throw new UnsupportedOperationException();
			}

			/*
			 * TODO: test removal with iteration rigorously
			 */
			@Override
			public void remove() {
				transientTrieMap.__remove(lastKey);
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
		public ImmutableMap<K, V> freeze() {
			if (mutator.get() == null) {
				throw new IllegalStateException("Transient already frozen.");
			}

			mutator.set(null);
			return new TrieMap<K, V>(rootNode, hashCode, cachedSize);
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
	
	protected static abstract class AbstractMapNode<K, V> extends AbstractNode<K, V> {

		protected static final int BIT_PARTITION_SIZE = 5;
		protected static final int BIT_PARTITION_MASK = 0x1f;

		abstract boolean containsKey(Object key, int keyHash, int shift);
		
		abstract boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp);

		abstract Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift);
		
		abstract Optional<Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp);

		abstract Result<K, V, ? extends AbstractMapNode<K, V>> updated(
						AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift);		
		
		abstract Result<K, V, ? extends AbstractMapNode<K, V>> updated(
						AtomicReference<Thread> mutator, K key, int keyHash, V val, int shift,
						Comparator<Object> cmp);

		abstract Result<K, V, ? extends AbstractMapNode<K, V>> removed(
						AtomicReference<Thread> mutator, K key, int keyHash, int shift);
		
		abstract Result<K, V, ? extends AbstractMapNode<K, V>> removed(
						AtomicReference<Thread> mutator, K key, int keyHash, int shift,
						Comparator<Object> cmp);

		static final boolean isAllowedToEdit(AtomicReference<Thread> x, AtomicReference<Thread> y) {
			return x != null && y != null && (x == y || x.get() == y.get());
		}

		abstract K getKey(int index);

		abstract V getValue(int index);

		abstract Map.Entry<K, V> getKeyValueEntry(int index);
		
		abstract AbstractMapNode<K, V> getNode(int index);

		abstract boolean hasNodes();

		abstract Iterator<? extends AbstractMapNode<K, V>> nodeIterator();

		abstract int nodeArity();

		abstract boolean hasPayload();

		abstract SupplierIterator<K, V> payloadIterator();

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
			final SupplierIterator<K, V> it = new MapKeyIterator<>(this);

			int size = 0;
			while (it.hasNext()) {
				size += 1;
				it.next();
			}

			return size;
		}
	}

	private static abstract class CompactMapNode<K, V> extends AbstractMapNode<K, V> {
		static final byte SIZE_EMPTY = 0b00;
		static final byte SIZE_ONE = 0b01;
		static final byte SIZE_MORE_THAN_ONE = 0b10;

		@Override
		abstract Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator,
						K key, int keyHash, V val, int shift);

		@Override
		abstract Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator,
						K key, int keyHash, V val, int shift, Comparator<Object> cmp);	
		
		@Override
		abstract Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator,
						K key, int hash, int shift);
		
		@Override
		abstract Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator,
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

		/**
		 * Returns the first value stored within this node.
		 * 
		 * @return first value
		 */
		abstract V headVal();

		@Override
		abstract Iterator<? extends CompactMapNode<K, V>> nodeIterator();
		
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

		static final CompactMapNode EMPTY_INPLACE_INDEX_NODE; 
					
		static {
			if (USE_SPECIALIAZIONS) {
				EMPTY_INPLACE_INDEX_NODE = new Map0To0Node<>(null);
			} else {
				EMPTY_INPLACE_INDEX_NODE = new BitmapIndexedMapNode<>(null, 0, 0, new Object[] {},
								(byte) 0);
			}
		};

		@SuppressWarnings("unchecked")
		static final <K, V> CompactMapNode<K, V> valNodeOf(AtomicReference<Thread> mutator) {
			return EMPTY_INPLACE_INDEX_NODE;
		}
		
		static final <K, V> CompactMapNode<K, V> valNodeOf(AtomicReference<Thread> mutator,
						int bitmap, int valmap, Object[] nodes, byte payloadArity) {
			return new BitmapIndexedMapNode<>(mutator, bitmap, valmap, nodes, payloadArity);
		}		
		
		static final <K, V> CompactMapNode<K, V> valNodeOf(AtomicReference<Thread> mutator, byte pos,
						CompactMapNode<K, V> node) {
			switch (pos) {
			case 0:
				return new SingletonMapNodeAtMask0Node<>(node);
			case 1:
				return new SingletonMapNodeAtMask1Node<>(node);
			case 2:
				return new SingletonMapNodeAtMask2Node<>(node);
			case 3:
				return new SingletonMapNodeAtMask3Node<>(node);
			case 4:
				return new SingletonMapNodeAtMask4Node<>(node);
			case 5:
				return new SingletonMapNodeAtMask5Node<>(node);
			case 6:
				return new SingletonMapNodeAtMask6Node<>(node);
			case 7:
				return new SingletonMapNodeAtMask7Node<>(node);
			case 8:
				return new SingletonMapNodeAtMask8Node<>(node);
			case 9:
				return new SingletonMapNodeAtMask9Node<>(node);
			case 10:
				return new SingletonMapNodeAtMask10Node<>(node);
			case 11:
				return new SingletonMapNodeAtMask11Node<>(node);
			case 12:
				return new SingletonMapNodeAtMask12Node<>(node);
			case 13:
				return new SingletonMapNodeAtMask13Node<>(node);
			case 14:
				return new SingletonMapNodeAtMask14Node<>(node);
			case 15:
				return new SingletonMapNodeAtMask15Node<>(node);
			case 16:
				return new SingletonMapNodeAtMask16Node<>(node);
			case 17:
				return new SingletonMapNodeAtMask17Node<>(node);
			case 18:
				return new SingletonMapNodeAtMask18Node<>(node);
			case 19:
				return new SingletonMapNodeAtMask19Node<>(node);
			case 20:
				return new SingletonMapNodeAtMask20Node<>(node);
			case 21:
				return new SingletonMapNodeAtMask21Node<>(node);
			case 22:
				return new SingletonMapNodeAtMask22Node<>(node);
			case 23:
				return new SingletonMapNodeAtMask23Node<>(node);
			case 24:
				return new SingletonMapNodeAtMask24Node<>(node);
			case 25:
				return new SingletonMapNodeAtMask25Node<>(node);
			case 26:
				return new SingletonMapNodeAtMask26Node<>(node);
			case 27:
				return new SingletonMapNodeAtMask27Node<>(node);
			case 28:
				return new SingletonMapNodeAtMask28Node<>(node);
			case 29:
				return new SingletonMapNodeAtMask29Node<>(node);
			case 30:
				return new SingletonMapNodeAtMask30Node<>(node);
			case 31:
				return new SingletonMapNodeAtMask31Node<>(node);

			default:
				throw new IllegalStateException("Position out of range.");
			}
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final byte npos1, final CompactMapNode<K, V> node1, final byte npos2,
						final CompactMapNode<K, V> node2) {
			return new Map0To2Node<>(mutator, npos1, node1, npos2, node2);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final byte npos1, final CompactMapNode<K, V> node1, final byte npos2,
						final CompactMapNode<K, V> node2, final byte npos3,
						final CompactMapNode<K, V> node3) {
			return new Map0To3Node<>(mutator, npos1, node1, npos2, node2, npos3, node3);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final byte npos1, final CompactMapNode<K, V> node1, final byte npos2,
						final CompactMapNode<K, V> node2, final byte npos3,
						final CompactMapNode<K, V> node3, final byte npos4,
						final CompactMapNode<K, V> node4) {
			return new Map0To4Node<>(mutator, npos1, node1, npos2, node2, npos3, node3, npos4,
							node4);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final byte npos1, final CompactMapNode<K, V> node1, final byte npos2,
						final CompactMapNode<K, V> node2, final byte npos3,
						final CompactMapNode<K, V> node3, final byte npos4,
						final CompactMapNode<K, V> node4, final byte npos5,
						final CompactMapNode<K, V> node5) {
			final int bitmap = 0 | (1 << npos1) | (1 << npos2) | (1 << npos3) | (1 << npos4)
							| (1 << npos5);
			final int valmap = 0;

			return valNodeOf(mutator, bitmap, valmap, new Object[] { node1, node2, node3, node4,
							node5 }, (byte) 0);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final V val1) {
			return new Map1To0Node<>(mutator, pos1, key1, val1);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final V val1, final byte npos1,
						final CompactMapNode<K, V> node1) {
			return new Map1To1Node<>(mutator, pos1, key1, val1, npos1, node1);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final V val1, final byte npos1,
						final CompactMapNode<K, V> node1, final byte npos2,
						final CompactMapNode<K, V> node2) {
			return new Map1To2Node<>(mutator, pos1, key1, val1, npos1, node1, npos2, node2);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final V val1, final byte npos1,
						final CompactMapNode<K, V> node1, final byte npos2,
						final CompactMapNode<K, V> node2, final byte npos3,
						final CompactMapNode<K, V> node3) {
			return new Map1To3Node<>(mutator, pos1, key1, val1, npos1, node1, npos2, node2, npos3,
							node3);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final V val1, final byte npos1,
						final CompactMapNode<K, V> node1, final byte npos2,
						final CompactMapNode<K, V> node2, final byte npos3,
						final CompactMapNode<K, V> node3, final byte npos4,
						final CompactMapNode<K, V> node4) {
			final int bitmap = 0 | (1 << pos1) | (1 << npos1) | (1 << npos2) | (1 << npos3)
							| (1 << npos4);
			final int valmap = 0 | (1 << pos1);

			return valNodeOf(mutator, bitmap, valmap, new Object[] { key1, val1, node1, node2,
							node3, node4 }, (byte) 1);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final V val1, final byte pos2, final K key2,
						final V val2) {
			return new Map2To0Node<>(mutator, pos1, key1, val1, pos2, key2, val2);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final V val1, final byte pos2, final K key2,
						final V val2, final byte npos1, final CompactMapNode<K, V> node1) {
			return new Map2To1Node<>(mutator, pos1, key1, val1, pos2, key2, val2, npos1, node1);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final V val1, final byte pos2, final K key2,
						final V val2, final byte npos1, final CompactMapNode<K, V> node1,
						final byte npos2, final CompactMapNode<K, V> node2) {
			return new Map2To2Node<>(mutator, pos1, key1, val1, pos2, key2, val2, npos1, node1,
							npos2, node2);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final V val1, final byte pos2, final K key2,
						final V val2, final byte npos1, final CompactMapNode<K, V> node1,
						final byte npos2, final CompactMapNode<K, V> node2, final byte npos3,
						final CompactMapNode<K, V> node3) {
			final int bitmap = 0 | (1 << pos1) | (1 << pos2) | (1 << npos1) | (1 << npos2)
							| (1 << npos3);
			final int valmap = 0 | (1 << pos1) | (1 << pos2);

			return valNodeOf(mutator, bitmap, valmap, new Object[] { key1, val1, key2, val2, node1,
							node2, node3 }, (byte) 2);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final V val1, final byte pos2, final K key2,
						final V val2, final byte pos3, final K key3, final V val3) {
			return new Map3To0Node<>(mutator, pos1, key1, val1, pos2, key2, val2, pos3, key3, val3);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final V val1, final byte pos2, final K key2,
						final V val2, final byte pos3, final K key3, final V val3,
						final byte npos1, final CompactMapNode<K, V> node1) {
			return new Map3To1Node<>(mutator, pos1, key1, val1, pos2, key2, val2, pos3, key3, val3,
							npos1, node1);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final V val1, final byte pos2, final K key2,
						final V val2, final byte pos3, final K key3, final V val3,
						final byte npos1, final CompactMapNode<K, V> node1, final byte npos2,
						final CompactMapNode<K, V> node2) {
			final int bitmap = 0 | (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << npos1)
							| (1 << npos2);
			final int valmap = 0 | (1 << pos1) | (1 << pos2) | (1 << pos3);

			return valNodeOf(mutator, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3,
							val3, node1, node2 }, (byte) 3);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final V val1, final byte pos2, final K key2,
						final V val2, final byte pos3, final K key3, final V val3, final byte pos4,
						final K key4, final V val4) {
			return new Map4To0Node<>(mutator, pos1, key1, val1, pos2, key2, val2, pos3, key3, val3,
							pos4, key4, val4);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final V val1, final byte pos2, final K key2,
						final V val2, final byte pos3, final K key3, final V val3, final byte pos4,
						final K key4, final V val4, final byte npos1,
						final CompactMapNode<K, V> node1) {
			final int bitmap = 0 | (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << pos4)
							| (1 << npos1);
			final int valmap = 0 | (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << pos4);

			return valNodeOf(mutator, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3,
							val3, key4, val4, node1 }, (byte) 4);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final byte pos1, final K key1, final V val1, final byte pos2, final K key2,
						final V val2, final byte pos3, final K key3, final V val3, final byte pos4,
						final K key4, final V val4, final byte pos5, final K key5, final V val5) {
			final int bitmap = 0 | (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << pos4)
							| (1 << pos5);
			final int valmap = 0 | (1 << pos1) | (1 << pos2) | (1 << pos3) | (1 << pos4)
							| (1 << pos5);

			return valNodeOf(mutator, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3,
							val3, key4, val4, key5, val5 }, (byte) 5);
		}

		@SuppressWarnings("unchecked")
		static final <K, V> CompactMapNode<K, V> mergeNodes(K key0, int keyHash0, V val0, K key1,
						int keyHash1, V val1, int shift) {
			assert key0.equals(key1) == false;

			if (keyHash0 == keyHash1) {
				return new HashCollisionMapNode<>(keyHash0, (K[]) new Object[] { key0, key1 },
								(V[]) new Object[] { val0, val1 });
			}

			final int mask0 = (keyHash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (keyHash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {	
				// both nodes fit on same level
				
				if (USE_SPECIALIAZIONS) {
					if (mask0 < mask1) {
						return valNodeOf(null, (byte) mask0, key0, val0, (byte) mask1, key1, val1);
					} else {
						return valNodeOf(null, (byte) mask1, key1, val1, (byte) mask0, key0, val0);
					}					
				} else {
					final int valmap = 1 << mask0 | 1 << mask1;
		
					if (mask0 < mask1) {
						return valNodeOf(null, valmap, valmap, new Object[] { key0, val0, key1,
										val1 }, (byte) 2);
					} else {
						return valNodeOf(null, valmap, valmap, new Object[] { key1, val1, key0,
										val0 }, (byte) 2);
					}
				}
			} else {
				// values fit on next level
				final CompactMapNode<K, V> node = mergeNodes(key0, keyHash0, val0, key1, keyHash1,
								val1, shift + BIT_PARTITION_SIZE);

				if (USE_SPECIALIAZIONS) {				
					return valNodeOf(null, (byte) mask0, node);
				} else {
					final int bitmap = 1 << mask0;
					return valNodeOf(null, bitmap, 0, new Object[] { node }, (byte) 0);
				}
			}
		}

		static final <K, V> CompactMapNode<K, V> mergeNodes(CompactMapNode<K, V> node0, int keyHash0,
						K key1, int keyHash1, V val1, int shift) {
			final int mask0 = (keyHash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (keyHash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level
				
				if (USE_SPECIALIAZIONS) {
					// store values before node
					return valNodeOf(null, (byte) mask1, key1, val1, (byte) mask0, node0);
				} else {
					final int bitmap = 1 << mask0 | 1 << mask1;
					final int valmap = 1 << mask1;

					// store values before node
					return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, node0 },
									(byte) 1);
				}
			} else {
				// values fit on next level
				final CompactMapNode<K, V> node = mergeNodes(node0, keyHash0, key1, keyHash1, val1,
								shift + BIT_PARTITION_SIZE);

				if (USE_SPECIALIAZIONS) {				
					return valNodeOf(null, (byte) mask0, node);
				} else {
					final int bitmap = 1 << mask0;
					return valNodeOf(null, bitmap, 0, new Object[] { node }, (byte) 0);
				}
			}
		}
	}

	private static final class BitmapIndexedMapNode<K, V> extends CompactMapNode<K, V> {
		private AtomicReference<Thread> mutator;

		private Object[] nodes;
		final private int bitmap;
		final private int valmap;
		final private byte payloadArity;

		BitmapIndexedMapNode(AtomicReference<Thread> mutator, int bitmap, int valmap, Object[] nodes,
						byte payloadArity) {
			assert (2 * Integer.bitCount(valmap) + Integer.bitCount(bitmap ^ valmap) == nodes.length);

			this.mutator = mutator;

			this.nodes = nodes;
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.payloadArity = payloadArity;

			assert (payloadArity == Integer.bitCount(valmap));
//			assert (payloadArity() >= 2 || nodeArity() >= 1); // =
//															// SIZE_MORE_THAN_ONE

			// for (int i = 0; i < 2 * payloadArity; i++)
			// assert ((nodes[i] instanceof CompactNode) == false);
			//
			// for (int i = 2 * payloadArity; i < nodes.length; i++)
			// assert ((nodes[i] instanceof CompactNode) == true);

			// assert invariant
			assert nodeInvariant();
		}

		final int bitIndex(int bitpos) {
			return 2 * payloadArity + Integer.bitCount((bitmap ^ valmap) & (bitpos - 1));
		}

		final int valIndex(int bitpos) {
			return 2 * Integer.bitCount(valmap & (bitpos - 1));
		}

		@SuppressWarnings("unchecked")
		@Override
		boolean containsKey(Object key, int keyHash, int shift) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) {
				return nodes[valIndex(bitpos)].equals(key);
			}

			if ((bitmap & bitpos) != 0) {
				return ((AbstractMapNode<K, V>) nodes[bitIndex(bitpos)]).containsKey(key, keyHash, shift
								+ BIT_PARTITION_SIZE);
			}

			return false;
		}

		@SuppressWarnings("unchecked")
		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) {
				return cmp.compare(nodes[valIndex(bitpos)], key) == 0;
			}

			if ((bitmap & bitpos) != 0) {
				return ((AbstractMapNode<K, V>) nodes[bitIndex(bitpos)]).containsKey(key, keyHash, shift
								+ BIT_PARTITION_SIZE, cmp);
			}

			return false;
		}

		@SuppressWarnings("unchecked")
		@Override
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (nodes[valIndex].equals(key)) {
					final K _key = (K) nodes[valIndex];
					final V _val = (V) nodes[valIndex + 1];

					final Map.Entry<K, V> entry = entryOf(_key, _val);
					return Optional.of(entry);
				}

				return Optional.empty();
			}

			if ((bitmap & bitpos) != 0) { // node (not value)
				final AbstractMapNode<K, V> subNode = ((AbstractMapNode<K, V>) nodes[bitIndex(bitpos)]);

				return subNode.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (cmp.compare(nodes[valIndex], key) == 0) {
					final K _key = (K) nodes[valIndex];
					final V _val = (V) nodes[valIndex + 1];

					final Map.Entry<K, V> entry = entryOf(_key, _val);
					return Optional.of(entry);
				}

				return Optional.empty();
			}

			if ((bitmap & bitpos) != 0) { // node (not value)
				final AbstractMapNode<K, V> subNode = ((AbstractMapNode<K, V>) nodes[bitIndex(bitpos)]);

				return subNode.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				final Object currentKey = nodes[valIndex];

				if (currentKey.equals(key)) {

					final Object currentVal = nodes[valIndex + 1];

					if (false && currentVal.equals(val)) {
						return Result.unchanged(this);
					}

					// update mapping
					final CompactMapNode<K, V> thisNew;

					if (isAllowedToEdit(this.mutator, mutator)) {
						// no copying if already editable
						this.nodes[valIndex + 1] = val;
						thisNew = this;
					} else {
						final Object[] editableNodes = copyAndSet(this.nodes, valIndex + 1, val);

						thisNew = CompactMapNode.<K, V> valNodeOf(mutator, bitmap, valmap, editableNodes,
										payloadArity);
					}

					return Result.updated(thisNew, (V) currentVal);
				} else {
					final CompactMapNode<K, V> nodeNew = mergeNodes((K) nodes[valIndex],
									nodes[valIndex].hashCode(), (V) nodes[valIndex + 1], key, keyHash,
									val, shift + BIT_PARTITION_SIZE);

					final int offset = 2 * (payloadArity - 1);
					final int index = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
									& (bitpos - 1));

					final Object[] editableNodes = copyAndMoveToBackPair(this.nodes, valIndex, offset
									+ index, nodeNew);

					final CompactMapNode<K, V> thisNew = CompactMapNode.<K, V> valNodeOf(mutator, bitmap
									| bitpos, valmap & ~bitpos, editableNodes, (byte) (payloadArity - 1));

					return Result.modified(thisNew);
				}
			} else if ((bitmap & bitpos) != 0) { // node (not value)
				final int bitIndex = bitIndex(bitpos);
				final CompactMapNode<K, V> subNode = (CompactMapNode<K, V>) nodes[bitIndex];

				final Result<K, V, ? extends CompactMapNode<K, V>> subNodeResult = subNode.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactMapNode<K, V> thisNew;

				// modify current node (set replacement node)
				if (isAllowedToEdit(this.mutator, mutator)) {
					// no copying if already editable
					this.nodes[bitIndex] = subNodeResult.getNode();
					thisNew = this;
				} else {
					final Object[] editableNodes = copyAndSet(this.nodes, bitIndex,
									subNodeResult.getNode());

					thisNew = CompactMapNode.<K, V> valNodeOf(mutator, bitmap, valmap, editableNodes,
									payloadArity);
				}

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else {
				// no value
				final Object[] editableNodes = copyAndInsertPair(this.nodes, valIndex(bitpos), key, val);

				final CompactMapNode<K, V> thisNew = CompactMapNode.<K, V> valNodeOf(mutator,
								bitmap | bitpos, valmap | bitpos, editableNodes,
								(byte) (payloadArity + 1));

				return Result.modified(thisNew);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				final Object currentKey = nodes[valIndex];

				if (cmp.compare(currentKey, key) == 0) {

					final Object currentVal = nodes[valIndex + 1];

					if (false && cmp.compare(currentVal, val) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					final CompactMapNode<K, V> thisNew;

					if (isAllowedToEdit(this.mutator, mutator)) {
						// no copying if already editable
						this.nodes[valIndex + 1] = val;
						thisNew = this;
					} else {
						final Object[] editableNodes = copyAndSet(this.nodes, valIndex + 1, val);

						thisNew = CompactMapNode.<K, V> valNodeOf(mutator, bitmap, valmap, editableNodes,
										payloadArity);
					}

					return Result.updated(thisNew, (V) currentVal);
				} else {
					final CompactMapNode<K, V> nodeNew = mergeNodes((K) nodes[valIndex],
									nodes[valIndex].hashCode(), (V) nodes[valIndex + 1], key, keyHash,
									val, shift + BIT_PARTITION_SIZE);

					final int offset = 2 * (payloadArity - 1);
					final int index = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
									& (bitpos - 1));

					final Object[] editableNodes = copyAndMoveToBackPair(this.nodes, valIndex, offset
									+ index, nodeNew);

					final CompactMapNode<K, V> thisNew = CompactMapNode.<K, V> valNodeOf(mutator, bitmap
									| bitpos, valmap & ~bitpos, editableNodes, (byte) (payloadArity - 1));

					return Result.modified(thisNew);
				}
			} else if ((bitmap & bitpos) != 0) { // node (not value)
				final int bitIndex = bitIndex(bitpos);
				final CompactMapNode<K, V> subNode = (CompactMapNode<K, V>) nodes[bitIndex];

				final Result<K, V, ? extends CompactMapNode<K, V>> subNodeResult = subNode.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!subNodeResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactMapNode<K, V> thisNew;

				// modify current node (set replacement node)
				if (isAllowedToEdit(this.mutator, mutator)) {
					// no copying if already editable
					this.nodes[bitIndex] = subNodeResult.getNode();
					thisNew = this;
				} else {
					final Object[] editableNodes = copyAndSet(this.nodes, bitIndex,
									subNodeResult.getNode());

					thisNew = CompactMapNode.<K, V> valNodeOf(mutator, bitmap, valmap, editableNodes,
									payloadArity);
				}

				if (subNodeResult.hasReplacedValue()) {
					return Result.updated(thisNew, subNodeResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else {
				// no value
				final Object[] editableNodes = copyAndInsertPair(this.nodes, valIndex(bitpos), key, val);

				final CompactMapNode<K, V> thisNew = CompactMapNode.<K, V> valNodeOf(mutator,
								bitmap | bitpos, valmap | bitpos, editableNodes,
								(byte) (payloadArity + 1));

				return Result.modified(thisNew);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (nodes[valIndex].equals(key)) {
					if (!USE_SPECIALIAZIONS && this.payloadArity() == 2 && this.nodeArity() == 0) {
						/*
						 * Create new node with remaining pair. The new node will a)
						 * either become the new root returned, or b) unwrapped and
						 * inlined during returning.
						 */
						final CompactMapNode<K, V> thisNew;
						final int newValmap = (shift == 0) ? this.valmap & ~bitpos
										: 1 << (keyHash & BIT_PARTITION_MASK);

						if (valIndex == 0) {
							thisNew = CompactMapNode.<K, V> valNodeOf(mutator, newValmap, newValmap,
											new Object[] { nodes[2], nodes[3] }, (byte) (1));
						} else {
							thisNew = CompactMapNode.<K, V> valNodeOf(mutator, newValmap, newValmap,
											new Object[] { nodes[0], nodes[1] }, (byte) (1));
						}

						return Result.modified(thisNew);
					} else if (USE_SPECIALIAZIONS && this.arity() == 5) {
						return Result.modified(removeInplaceValueAndConvertSpecializedNode(mask, bitpos));
					} else {
						final Object[] editableNodes = copyAndRemovePair(this.nodes, valIndex);

						final CompactMapNode<K, V> thisNew = CompactMapNode.<K, V> valNodeOf(mutator,
										this.bitmap & ~bitpos, this.valmap & ~bitpos, editableNodes,
										(byte) (payloadArity - 1));

						return Result.modified(thisNew);
					}
				} else {
					return Result.unchanged(this);
				}
			} else if ((bitmap & bitpos) != 0) { // node (not value)
				final int bitIndex = bitIndex(bitpos);
				final CompactMapNode<K, V> subNode = (CompactMapNode<K, V>) nodes[bitIndex];
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = subNode.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (!nestedResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactMapNode<K, V> subNodeNew = nestedResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case 0: {
					if (!USE_SPECIALIAZIONS && this.payloadArity() == 0 && this.nodeArity() == 1) {
						// escalate (singleton or empty) result
						return nestedResult;
					} else if (USE_SPECIALIAZIONS && this.arity() == 5) {
						return Result.modified(removeSubNodeAndConvertSpecializedNode(mask, bitpos));
					} else {
						// remove node
						final Object[] editableNodes = copyAndRemovePair(this.nodes, bitIndex);

						final CompactMapNode<K, V> thisNew = CompactMapNode.<K, V> valNodeOf(mutator,
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

						final Object[] editableNodes = copyAndMoveToFrontPair(this.nodes, bitIndex,
										valIndex, subNodeNew.headKey(), subNodeNew.headVal());

						final CompactMapNode<K, V> thisNew = CompactMapNode.<K, V> valNodeOf(mutator,
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

						final CompactMapNode<K, V> thisNew = CompactMapNode.<K, V> valNodeOf(mutator,
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
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (cmp.compare(nodes[valIndex], key) == 0) {
					if (!USE_SPECIALIAZIONS && this.payloadArity() == 2 && this.nodeArity() == 0) {
						/*
						 * Create new node with remaining pair. The new node will a)
						 * either become the new root returned, or b) unwrapped and
						 * inlined during returning.
						 */
						final CompactMapNode<K, V> thisNew;
						final int newValmap = (shift == 0) ? this.valmap & ~bitpos
										: 1 << (keyHash & BIT_PARTITION_MASK);

						if (valIndex == 0) {
							thisNew = CompactMapNode.<K, V> valNodeOf(mutator, newValmap, newValmap,
											new Object[] { nodes[2], nodes[3] }, (byte) (1));
						} else {
							thisNew = CompactMapNode.<K, V> valNodeOf(mutator, newValmap, newValmap,
											new Object[] { nodes[0], nodes[1] }, (byte) (1));
						}

						return Result.modified(thisNew);
					} else if (USE_SPECIALIAZIONS && this.arity() == 5) {
						return Result.modified(removeInplaceValueAndConvertSpecializedNode(mask, bitpos));
					} else {
						final Object[] editableNodes = copyAndRemovePair(this.nodes, valIndex);

						final CompactMapNode<K, V> thisNew = CompactMapNode.<K, V> valNodeOf(mutator,
										this.bitmap & ~bitpos, this.valmap & ~bitpos, editableNodes,
										(byte) (payloadArity - 1));

						return Result.modified(thisNew);
					}
				} else {
					return Result.unchanged(this);
				}
			} else if ((bitmap & bitpos) != 0) { // node (not value)
				final int bitIndex = bitIndex(bitpos);
				final CompactMapNode<K, V> subNode = (CompactMapNode<K, V>) nodes[bitIndex];
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = subNode.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!nestedResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactMapNode<K, V> subNodeNew = nestedResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case 0: {
					if (!USE_SPECIALIAZIONS && this.payloadArity() == 0 && this.nodeArity() == 1) {
						// escalate (singleton or empty) result
						return nestedResult;
					} else if (USE_SPECIALIAZIONS && this.arity() == 5) {
						return Result.modified(removeSubNodeAndConvertSpecializedNode(mask, bitpos));
					} else {
						// remove node
						final Object[] editableNodes = copyAndRemovePair(this.nodes, bitIndex);

						final CompactMapNode<K, V> thisNew = CompactMapNode.<K, V> valNodeOf(mutator,
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

						final Object[] editableNodes = copyAndMoveToFrontPair(this.nodes, bitIndex,
										valIndex, subNodeNew.headKey(), subNodeNew.headVal());

						final CompactMapNode<K, V> thisNew = CompactMapNode.<K, V> valNodeOf(mutator,
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

						final CompactMapNode<K, V> thisNew = CompactMapNode.<K, V> valNodeOf(mutator,
										bitmap, valmap, editableNodes, payloadArity);

						return Result.modified(thisNew);
					}
				}
				}
			}

			return Result.unchanged(this);
		}
		
		@SuppressWarnings("unchecked")
		private CompactMapNode<K, V> removeInplaceValueAndConvertSpecializedNode(final int mask,
						final int bitpos) {
			switch (this.payloadArity) { // 0 <= payloadArity <= 5
			case 1: {
				final int nmap = ((bitmap & ~bitpos) ^ (valmap & ~bitpos));
				final byte npos1 = recoverMask(nmap, (byte) 1);
				final byte npos2 = recoverMask(nmap, (byte) 2);
				final byte npos3 = recoverMask(nmap, (byte) 3);
				final byte npos4 = recoverMask(nmap, (byte) 4);
				final CompactMapNode<K, V> node1 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 0];
				final CompactMapNode<K, V> node2 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 1];
				final CompactMapNode<K, V> node3 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 2];
				final CompactMapNode<K, V> node4 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 3];

				return valNodeOf(mutator, npos1, node1, npos2, node2, npos3, node3, npos4, node4);
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
				final CompactMapNode<K, V> node1 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 0];
				final CompactMapNode<K, V> node2 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 1];
				final CompactMapNode<K, V> node3 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 2];

				if (mask < pos1) {
					key1 = (K) nodes[2];
					val1 = (V) nodes[3];
				} else {
					key1 = (K) nodes[0];
					val1 = (V) nodes[1];
				}

				return valNodeOf(mutator, pos1, key1, val1, npos1, node1, npos2, node2, npos3,
								node3);
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
				final CompactMapNode<K, V> node1 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 0];
				final CompactMapNode<K, V> node2 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 1];

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

				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, npos1, node1, npos2,
								node2);
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
				final CompactMapNode<K, V> node1 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 0];

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

				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3, key3, val3,
								npos1, node1);
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

				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3, key3, val3,
								pos4, key4, val4);
			}
			default: {
				throw new IllegalStateException();
			}				
			}
		}

		@SuppressWarnings("unchecked")
		private CompactMapNode<K, V> removeSubNodeAndConvertSpecializedNode(final int mask,
						final int bitpos) {
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

				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3, key3, val3,
								pos4, key4, val4);
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
				final CompactMapNode<K, V> node1;

				if (mask < npos1) {
					node1 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 1];
				} else {
					node1 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 0];
				}

				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3, key3, val3,
								npos1, node1);
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
				final CompactMapNode<K, V> node1;
				final CompactMapNode<K, V> node2;

				if (mask < npos1) {
					node1 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 1];
					node2 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 2];
				} else if (mask < npos2) {
					node1 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 0];
					node2 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 2];
				} else {
					node1 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 0];
					node2 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 1];
				}

				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, npos1, node1, npos2,
								node2);
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
				final CompactMapNode<K, V> node1;
				final CompactMapNode<K, V> node2;
				final CompactMapNode<K, V> node3;

				if (mask < npos1) {
					node1 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 1];
					node2 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 2];
					node3 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 3];
				} else if (mask < npos2) {
					node1 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 0];
					node2 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 2];
					node3 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 3];
				} else if (mask < npos3) {
					node1 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 0];
					node2 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 1];
					node3 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 3];
				} else {
					node1 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 0];
					node2 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 1];
					node3 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 2];
				}

				return valNodeOf(mutator, pos1, key1, val1, npos1, node1, npos2, node2, npos3,
								node3);
			}
			case 5: {
				final int nmap = ((bitmap & ~bitpos) ^ valmap);
				final byte npos1 = recoverMask(nmap, (byte) 1);
				final byte npos2 = recoverMask(nmap, (byte) 2);
				final byte npos3 = recoverMask(nmap, (byte) 3);
				final byte npos4 = recoverMask(nmap, (byte) 4);
				final CompactMapNode<K, V> node1;
				final CompactMapNode<K, V> node2;
				final CompactMapNode<K, V> node3;
				final CompactMapNode<K, V> node4;

				if (mask < npos1) {
					node1 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 1];
					node2 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 2];
					node3 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 3];
					node4 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 4];
				} else if (mask < npos2) {
					node1 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 0];
					node2 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 2];
					node3 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 3];
					node4 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 4];
				} else if (mask < npos3) {
					node1 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 0];
					node2 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 1];
					node3 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 3];
					node4 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 4];
				} else if (mask < npos4) {
					node1 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 0];
					node2 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 1];
					node3 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 2];
					node4 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 4];
				} else {
					node1 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 0];
					node2 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 1];
					node3 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 2];
					node4 = (CompactMapNode<K, V>) nodes[2 * payloadArity + 3];
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
			return (K) nodes[2 * index];
		}

		@SuppressWarnings("unchecked")
		@Override
		V getValue(int index) {
			return (V) nodes[2 * index + 1];
		}
		
		@SuppressWarnings("unchecked")
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf((K) nodes[2 * index], (V) nodes[2 * index + 1]);
		}		

		@SuppressWarnings("unchecked")
		@Override
		public AbstractMapNode<K, V> getNode(int index) {
			final int offset = 2 * payloadArity;
			return (AbstractMapNode<K, V>) nodes[offset + index];
		}

		@Override
		SupplierIterator<K, V> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(nodes, 0, 2 * payloadArity);
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			final int offset = 2 * payloadArity;

			for (int i = offset; i < nodes.length - offset; i++) {
				assert ((nodes[i] instanceof AbstractMapNode) == true);
			}

			return (Iterator) ArrayIterator.of(nodes, offset, nodes.length - offset);
		}

		@SuppressWarnings("unchecked")
		@Override
		K headKey() {
			assert hasPayload();
			return (K) nodes[0];
		}

		@SuppressWarnings("unchecked")
		@Override
		V headVal() {
			assert hasPayload();
			return (V) nodes[1];
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
			return 2 * payloadArity != nodes.length;
		}

		@Override
		int nodeArity() {
			return nodes.length - 2 * payloadArity;
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
			BitmapIndexedMapNode<?, ?> that = (BitmapIndexedMapNode<?, ?>) other;
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
	        final StringBuilder bldr = new StringBuilder();
	        bldr.append('[');
	        	        
	        for (byte i = 0; i < payloadArity(); i++) {
	        	final byte pos = (byte) recoverMask(valmap, (byte) (i+1));	
	            bldr.append(String.format("@%d: %s=%s", pos, getKey(i), getValue(i)));		
	            // bldr.append(String.format("@%d: %s=%s", pos, "key", "val"));			

		        if (!((i + 1) == payloadArity())) {
		        	bldr.append(", ");
		        }
	        }

	        if (payloadArity() > 0 && nodeArity() > 0) {
	        	bldr.append(", ");
	        }
	        
	        for (byte i = 0; i < nodeArity(); i++) {
	        	final byte pos = (byte) recoverMask(bitmap ^ valmap, (byte) (i+1));	
	        	bldr.append(String.format("@%d: %s", pos, getNode(i)));
	        	// bldr.append(String.format("@%d: %s", pos, "node"));			

		        if (!((i + 1) == nodeArity())) {
		        	bldr.append(", ");
		        }
	        }
	        
	        bldr.append(']');    
			return bldr.toString();
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
	private static final class HashCollisionMapNode<K, V> extends CompactMapNode<K, V> {
		private final K[] keys;
		private final V[] vals;
		private final int hash;

		HashCollisionMapNode(int hash, K[] keys, V[] vals) {
			this.keys = keys;
			this.vals = vals;
			this.hash = hash;

			assert payloadArity() >= 2;
		}

		@Override
		SupplierIterator<K, V> payloadIterator() {
			// TODO: change representation of keys and values
			assert keys.length == vals.length;

			final Object[] keysAndVals = new Object[keys.length + vals.length];
			for (int i = 0; i < keys.length; i++) {
				keysAndVals[2 * i] = keys[i];
				keysAndVals[2 * i + 1] = vals[i];
			}

			return ArrayKeyValueSupplierIterator.of(keysAndVals);
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
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return Collections.emptyIterator();
		}

		@Override
		K headKey() {
			assert hasPayload();
			return keys[0];
		}

		@Override
		V headVal() {
			assert hasPayload();
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
		 * always returns a new immutable {@link TrieMap} instance.
		 */
		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			if (this.hash != keyHash) {
				return Result.modified(mergeNodes(this, this.hash, key, keyHash, val, shift));
			}

			for (int i = 0; i < keys.length; i++) {
				if (cmp.compare(keys[i], key) == 0) {
					
					final V currentVal = vals[i];
					
					if (false && cmp.compare(currentVal, val) == 0) {
						return Result.unchanged(this);
					}
					
					final CompactMapNode<K, V> thisNew;
					
//					// update mapping
//					if (isAllowedToEdit(this.mutator, mutator)) {
//						// no copying if already editable
//						this.vals[i] = val;
//						thisNew = this;
//					} else {
						@SuppressWarnings("unchecked")
						final V[] editableVals = (V[]) copyAndSet(this.vals, i, val);

						thisNew = new HashCollisionMapNode<>(this.hash, this.keys, editableVals);
//					}

					return Result.updated(thisNew, currentVal);						
				}
			}
			
			// no value
			@SuppressWarnings("unchecked")
			final K[] keysNew = (K[]) copyAndInsert(keys, keys.length, key);
			@SuppressWarnings("unchecked")
			final V[] valsNew = (V[]) copyAndInsert(vals, vals.length, val);
			return Result.modified(new HashCollisionMapNode<>(keyHash, keysNew, valsNew));
		}

		/**
		 * Removes an object if present. Note, that this implementation always
		 * returns a new immutable {@link TrieMap} instance.
		 */
		@SuppressWarnings("unchecked")
		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			for (int i = 0; i < keys.length; i++) {
				if (cmp.compare(keys[i], key) == 0) {
					if (this.arity() == 1) {
						return Result.modified(CompactMapNode.<K, V> valNodeOf(mutator));
					} else if (this.arity() == 2) {
						/*
						 * Create root node with singleton element. This node
						 * will be a) either be the new root returned, or b)
						 * unwrapped and inlined.
						 */
						final K theOtherKey = (i == 0) ? keys[1] : keys[0];
						final V theOtherVal = (i == 0) ? vals[1] : vals[0];
						return CompactMapNode.<K, V> valNodeOf(mutator).updated(mutator, theOtherKey,
										keyHash, theOtherVal, 0, cmp);
					} else {
						return Result.modified(new HashCollisionMapNode<>(keyHash,
										(K[]) copyAndRemove(keys, i), (V[]) copyAndRemove(vals, i)));
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
		V getValue(int index) {
			return vals[index];
		}
		
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			return entryOf(keys[index], vals[index]);
		}

		@Override
		public CompactMapNode<K, V> getNode(int index) {
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

			HashCollisionMapNode<?, ?> that = (HashCollisionMapNode<?, ?>) other;

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
				final Object otherVal = it.get();

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

		// TODO: generate instead of delegate
		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift) {
			return updated(mutator, key, keyHash, val, shift, EqualityUtils.getDefaultEqualityComparator());
		}

		// TODO: generate instead of delegate
		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			return removed(mutator, key, keyHash, shift, EqualityUtils.getDefaultEqualityComparator());
		}

		// TODO: generate instead of delegate
		@Override
		boolean containsKey(Object key, int keyHash, int shift) {
			return containsKey(key, keyHash, shift, EqualityUtils.getDefaultEqualityComparator());
		}

		// TODO: generate instead of delegate
		@Override
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift) {
			return findByKey(key, keyHash, shift, EqualityUtils.getDefaultEqualityComparator());
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

		if (other instanceof TrieMap) {
			TrieMap that = (TrieMap) other;

			if (this.size() != that.size()) {
				return false;
			}

			return rootNode.equals(that.rootNode);
		}

		return super.equals(other);
	}

	/*
	 * For analysis purposes only.
	 */
	protected AbstractMapNode<K, V> getRootNode() {
		return rootNode;
	}
	
	/*
	 * For analysis purposes only.
	 */
	protected Iterator<AbstractMapNode<K, V>> nodeIterator() {
		return new TrieMapNodeIterator<>(rootNode);
	}
	
	/*
	 * For analysis purposes only.
	 */
	protected int getNodeCount() {
		final Iterator<AbstractMapNode<K, V>> it = nodeIterator();
		int sumNodes = 0;
				
		for (; it.hasNext(); it.next()) {
			sumNodes += 1;
		}
		
		return sumNodes;
	}
	
	/*
	 * For analysis purposes only.
	 *
	 * Payload X Node
	 */
	protected int[][] arityCombinationsHistogram() {
		final Iterator<AbstractMapNode<K, V>> it = nodeIterator();		
		final int[][] sumArityCombinations = new int[33][33];
		
		while (it.hasNext()) {
			final AbstractMapNode<K, V> node = it.next();
			sumArityCombinations[node.payloadArity()][node.nodeArity()] += 1;
		}
		
		return sumArityCombinations;		
	}
	
	/*
	 * For analysis purposes only.
	 */
	protected int[] arityHistogram() {
		final int[][] sumArityCombinations = arityCombinationsHistogram();
		final int[] sumArity = new int[33];
		
		final int maxArity = 32; // TODO: factor out constant
		
		for (int j = 0; j <= maxArity; j++) {
			for (int maxRestArity = maxArity - j, k = 0; k <= maxRestArity - j; k++) {
				sumArity[j + k] += sumArityCombinations[j][k];
			}
		}
		
		return sumArity;
	}

	/*
	 * For analysis purposes only.
	 */
	protected void printStatistics() {
		final int[][] sumArityCombinations = arityCombinationsHistogram();
		final int[] sumArity = arityHistogram();
		final int sumNodes = getNodeCount();

		final int[] cumsumArity = new int[33];
		for (int cumsum = 0, i = 0; i < 33; i++) {
			cumsum += sumArity[i];
			cumsumArity[i] = cumsum;
		}

		final float threshhold = 0.01f; // for printing results
		for (int i = 0; i < 33; i++) {
			float arityPercentage = (float) (sumArity[i]) / sumNodes;
			float cumsumArityPercentage = (float) (cumsumArity[i]) / sumNodes;

			if (arityPercentage != 0 && arityPercentage >= threshhold) {
				// details per level
				StringBuilder bldr = new StringBuilder();
				int max = i;
				for (int j = 0; j <= max; j++) {
					for (int k = max - j; k <= max - j; k++) {
						float arityCombinationsPercentage = (float) (sumArityCombinations[j][k])
										/ sumNodes;

						if (arityCombinationsPercentage != 0
										&& arityCombinationsPercentage >= threshhold) {
							bldr.append(String.format("%d/%d: %s, ", j, k, new DecimalFormat(
											"0.00%").format(arityCombinationsPercentage)));
						}
					}
				}
				final String detailPercentages = bldr.toString();

				// overview
				System.out.println(String.format("%2d: %s\t[cumsum = %s]\t%s", i,
								new DecimalFormat("0.00%").format(arityPercentage),
								new DecimalFormat("0.00%").format(cumsumArityPercentage),
								detailPercentages));
			}
		}
	}
		
	/**
	 * Iterator that first iterates over inlined-values and then continues depth
	 * first recursively.
	 */
	private static class TrieMapNodeIterator<K, V> implements Iterator<AbstractMapNode<K, V>> {

		final Deque<Iterator<? extends AbstractMapNode<K, V>>> nodeIteratorStack;

		TrieMapNodeIterator(AbstractMapNode<K, V> rootNode) {
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
		public AbstractMapNode<K, V> next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			AbstractMapNode<K, V> innerNode = nodeIteratorStack.peek().next();

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

	private abstract static class AbstractSingletonMapNode<K, V> extends CompactMapNode<K, V> {

		protected abstract byte npos1();

		protected final CompactMapNode<K, V> node1;

		AbstractSingletonMapNode(CompactMapNode<K, V> node1) {
			this.node1 = node1;
		}
		
		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == npos1()) {
				final CompactMapNode<K, V> subNode = node1;

				final Result<K, V, ? extends CompactMapNode<K, V>> subNodeResult = subNode.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (subNodeResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, mask, subNodeResult.getNode());

					if (subNodeResult.hasReplacedValue()) {
						result = Result.updated(thisNew, subNodeResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == npos1()) {
				final CompactMapNode<K, V> subNode = node1;

				final Result<K, V, ? extends CompactMapNode<K, V>> subNodeResult = subNode.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (subNodeResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, mask, subNodeResult.getNode());

					if (subNodeResult.hasReplacedValue()) {
						result = Result.updated(thisNew, subNodeResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}
		
		private CompactMapNode<K, V> inlineValue(AtomicReference<Thread> mutator, byte mask, K key, V val) {
			return valNodeOf(mutator, mask, key, val, npos1(), node1);
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == npos1()) {
				final Result<K, V, ? extends CompactMapNode<K, V>> subNodeResult = node1.removed(mutator,
								key, keyHash, shift + BIT_PARTITION_SIZE);

				if (subNodeResult.isModified()) {
					final CompactMapNode<K, V> subNodeNew = subNodeResult.getNode();

					switch (subNodeNew.sizePredicate()) {
					case SIZE_EMPTY:
					case SIZE_ONE:
						// escalate (singleton or empty) result
						result = subNodeResult;
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, mask, subNodeNew));
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
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == npos1()) {
				final Result<K, V, ? extends CompactMapNode<K, V>> subNodeResult = node1.removed(mutator,
								key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (subNodeResult.isModified()) {
					final CompactMapNode<K, V> subNodeNew = subNodeResult.getNode();

					switch (subNodeNew.sizePredicate()) {
					case SIZE_EMPTY:
					case SIZE_ONE:
						// escalate (singleton or empty) result
						result = subNodeResult;
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, mask, subNodeNew));
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
		boolean containsKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return false;
			}
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				return node1.containsKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else {
				return false;
			}
		}

		@Override
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return Optional.empty();
			}
		}

		@Override
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == npos1()) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else {
				return Optional.empty();
			}
		}

		@Override
		K getKey(int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		V getValue(int index) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		public AbstractMapNode<K, V> getNode(int index) {
			if (index == 0) {
				return node1;
			} else {
				throw new IndexOutOfBoundsException();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.<CompactMapNode<K, V>> of(new CompactMapNode[] { node1 });
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
		SupplierIterator<K, V> payloadIterator() {
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
			AbstractSingletonMapNode<?, ?> that = (AbstractSingletonMapNode<?, ?>) other;

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

	private static final class SingletonMapNodeAtMask0Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 0;
		}

		SingletonMapNodeAtMask0Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask1Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 1;
		}

		SingletonMapNodeAtMask1Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask2Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 2;
		}

		SingletonMapNodeAtMask2Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask3Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 3;
		}

		SingletonMapNodeAtMask3Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask4Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 4;
		}

		SingletonMapNodeAtMask4Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask5Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 5;
		}

		SingletonMapNodeAtMask5Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask6Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 6;
		}

		SingletonMapNodeAtMask6Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask7Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 7;
		}

		SingletonMapNodeAtMask7Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask8Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 8;
		}

		SingletonMapNodeAtMask8Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask9Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 9;
		}

		SingletonMapNodeAtMask9Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask10Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 10;
		}

		SingletonMapNodeAtMask10Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask11Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 11;
		}

		SingletonMapNodeAtMask11Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask12Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 12;
		}

		SingletonMapNodeAtMask12Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask13Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 13;
		}

		SingletonMapNodeAtMask13Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask14Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 14;
		}

		SingletonMapNodeAtMask14Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask15Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 15;
		}

		SingletonMapNodeAtMask15Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask16Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 16;
		}

		SingletonMapNodeAtMask16Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask17Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 17;
		}

		SingletonMapNodeAtMask17Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask18Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 18;
		}

		SingletonMapNodeAtMask18Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask19Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 19;
		}

		SingletonMapNodeAtMask19Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask20Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 20;
		}

		SingletonMapNodeAtMask20Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask21Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 21;
		}

		SingletonMapNodeAtMask21Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask22Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 22;
		}

		SingletonMapNodeAtMask22Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask23Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 23;
		}

		SingletonMapNodeAtMask23Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask24Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 24;
		}

		SingletonMapNodeAtMask24Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask25Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 25;
		}

		SingletonMapNodeAtMask25Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask26Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 26;
		}

		SingletonMapNodeAtMask26Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask27Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 27;
		}

		SingletonMapNodeAtMask27Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask28Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 28;
		}

		SingletonMapNodeAtMask28Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask29Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 29;
		}

		SingletonMapNodeAtMask29Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask30Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 30;
		}

		SingletonMapNodeAtMask30Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class SingletonMapNodeAtMask31Node<K, V> extends AbstractSingletonMapNode<K, V> {

		@Override
		protected byte npos1() {
			return 31;
		}

		SingletonMapNodeAtMask31Node(CompactMapNode<K, V> node1) {
			super(node1);
		}

	}

	private static final class Map0To0Node<K, V> extends CompactMapNode<K, V> {

		Map0To0Node(final AtomicReference<Thread> mutator) {

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			return Result.modified(valNodeOf(mutator, mask, key, val));
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			return Result.modified(valNodeOf(mutator, mask, key, val));
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			return Result.unchanged(this);
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
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
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift) {
			return Optional.empty();
		}

		@Override
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			return Optional.empty();
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
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
		SupplierIterator<K, V> payloadIterator() {
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
		V headVal() {
			throw new UnsupportedOperationException("Node does not directly contain a value.");
		}

		@Override
		AbstractMapNode<K, V> getNode(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		K getKey(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		V getValue(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
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

	private static final class Map0To2Node<K, V> extends CompactMapNode<K, V> {

		private final byte npos1;
		private final CompactMapNode<K, V> node1;

		private final byte npos2;
		private final CompactMapNode<K, V> node2;

		Map0To2Node(final AtomicReference<Thread> mutator, final byte npos1,
						final CompactMapNode<K, V> node1, final byte npos2,
						final CompactMapNode<K, V> node2) {

			this.npos1 = npos1;
			this.node1 = node1;

			this.npos2 = npos2;
			this.node2 = node2;

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, mask,
									nestedResult.getNode(), npos2, node2);

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask,
									nestedResult.getNode());

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, mask,
									nestedResult.getNode(), npos2, node2);

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask,
									nestedResult.getNode());

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
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
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
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
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
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
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
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

		private CompactMapNode<K, V> inlineValue(AtomicReference<Thread> mutator, byte mask, K key,
						V val) {
			return valNodeOf(mutator, mask, key, val, npos1, node1, npos2, node2);
		}

		private CompactMapNode<K, V> removeNode1AndInlineValue(AtomicReference<Thread> mutator,
						byte mask, K key, V val) {
			return valNodeOf(mutator, mask, key, val, npos2, node2);
		}

		private CompactMapNode<K, V> removeNode2AndInlineValue(AtomicReference<Thread> mutator,
						byte mask, K key, V val) {
			return valNodeOf(mutator, mask, key, val, npos1, node1);
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
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift) {
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
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
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
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.<CompactMapNode<K, V>> of(new CompactMapNode[] { node1, node2 });
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
		SupplierIterator<K, V> payloadIterator() {
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
		V headVal() {
			throw new UnsupportedOperationException("Node does not directly contain a value.");
		}

		@Override
		AbstractMapNode<K, V> getNode(int index) {
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
		V getValue(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
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
			Map0To2Node<?, ?> that = (Map0To2Node<?, ?>) other;

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

	private static final class Map0To3Node<K, V> extends CompactMapNode<K, V> {

		private final byte npos1;
		private final CompactMapNode<K, V> node1;

		private final byte npos2;
		private final CompactMapNode<K, V> node2;

		private final byte npos3;
		private final CompactMapNode<K, V> node3;

		Map0To3Node(final AtomicReference<Thread> mutator, final byte npos1,
						final CompactMapNode<K, V> node1, final byte npos2,
						final CompactMapNode<K, V> node2, final byte npos3,
						final CompactMapNode<K, V> node3) {

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
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, mask,
									nestedResult.getNode(), npos2, node2, npos3, node3);

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask,
									nestedResult.getNode(), npos3, node3);

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos3) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node3.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
									mask, nestedResult.getNode());

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, mask,
									nestedResult.getNode(), npos2, node2, npos3, node3);

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask,
									nestedResult.getNode(), npos3, node3);

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos3) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node3.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
									mask, nestedResult.getNode());

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
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
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
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
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node3.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode3AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
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
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
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
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
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
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node3.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode3AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
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

		private CompactMapNode<K, V> inlineValue(AtomicReference<Thread> mutator, byte mask, K key,
						V val) {
			return valNodeOf(mutator, mask, key, val, npos1, node1, npos2, node2, npos3, node3);
		}

		private CompactMapNode<K, V> removeNode1AndInlineValue(AtomicReference<Thread> mutator,
						byte mask, K key, V val) {
			return valNodeOf(mutator, mask, key, val, npos2, node2, npos3, node3);
		}

		private CompactMapNode<K, V> removeNode2AndInlineValue(AtomicReference<Thread> mutator,
						byte mask, K key, V val) {
			return valNodeOf(mutator, mask, key, val, npos1, node1, npos3, node3);
		}

		private CompactMapNode<K, V> removeNode3AndInlineValue(AtomicReference<Thread> mutator,
						byte mask, K key, V val) {
			return valNodeOf(mutator, mask, key, val, npos1, node1, npos2, node2);
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
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift) {
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
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
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
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator
							.<CompactMapNode<K, V>> of(new CompactMapNode[] { node1, node2, node3 });
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
		SupplierIterator<K, V> payloadIterator() {
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
		V headVal() {
			throw new UnsupportedOperationException("Node does not directly contain a value.");
		}

		@Override
		AbstractMapNode<K, V> getNode(int index) {
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
		V getValue(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
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
			Map0To3Node<?, ?> that = (Map0To3Node<?, ?>) other;

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

	private static final class Map0To4Node<K, V> extends CompactMapNode<K, V> {

		private final byte npos1;
		private final CompactMapNode<K, V> node1;

		private final byte npos2;
		private final CompactMapNode<K, V> node2;

		private final byte npos3;
		private final CompactMapNode<K, V> node3;

		private final byte npos4;
		private final CompactMapNode<K, V> node4;

		Map0To4Node(final AtomicReference<Thread> mutator, final byte npos1,
						final CompactMapNode<K, V> node1, final byte npos2,
						final CompactMapNode<K, V> node2, final byte npos3,
						final CompactMapNode<K, V> node3, final byte npos4,
						final CompactMapNode<K, V> node4) {

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
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, mask,
									nestedResult.getNode(), npos2, node2, npos3, node3, npos4, node4);

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask,
									nestedResult.getNode(), npos3, node3, npos4, node4);

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos3) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node3.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
									mask, nestedResult.getNode(), npos4, node4);

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos4) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node4.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
									npos3, node3, mask, nestedResult.getNode());

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, mask,
									nestedResult.getNode(), npos2, node2, npos3, node3, npos4, node4);

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, mask,
									nestedResult.getNode(), npos3, node3, npos4, node4);

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos3) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node3.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
									mask, nestedResult.getNode(), npos4, node4);

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos4) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node4.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, npos1, node1, npos2, node2,
									npos3, node3, mask, nestedResult.getNode());

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
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
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
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
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node3.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode3AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
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
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node4.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode4AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
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
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
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
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
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
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node3.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode3AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
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
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node4.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode4AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
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

		private CompactMapNode<K, V> inlineValue(AtomicReference<Thread> mutator, byte mask, K key,
						V val) {
			return valNodeOf(mutator, mask, key, val, npos1, node1, npos2, node2, npos3, node3, npos4,
							node4);
		}

		private CompactMapNode<K, V> removeNode1AndInlineValue(AtomicReference<Thread> mutator,
						byte mask, K key, V val) {
			return valNodeOf(mutator, mask, key, val, npos2, node2, npos3, node3, npos4, node4);
		}

		private CompactMapNode<K, V> removeNode2AndInlineValue(AtomicReference<Thread> mutator,
						byte mask, K key, V val) {
			return valNodeOf(mutator, mask, key, val, npos1, node1, npos3, node3, npos4, node4);
		}

		private CompactMapNode<K, V> removeNode3AndInlineValue(AtomicReference<Thread> mutator,
						byte mask, K key, V val) {
			return valNodeOf(mutator, mask, key, val, npos1, node1, npos2, node2, npos4, node4);
		}

		private CompactMapNode<K, V> removeNode4AndInlineValue(AtomicReference<Thread> mutator,
						byte mask, K key, V val) {
			return valNodeOf(mutator, mask, key, val, npos1, node1, npos2, node2, npos3, node3);
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
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift) {
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
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
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
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.<CompactMapNode<K, V>> of(new CompactMapNode[] { node1, node2, node3,
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
		SupplierIterator<K, V> payloadIterator() {
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
		V headVal() {
			throw new UnsupportedOperationException("Node does not directly contain a value.");
		}

		@Override
		AbstractMapNode<K, V> getNode(int index) {
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
		V getValue(int index) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
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
			Map0To4Node<?, ?> that = (Map0To4Node<?, ?>) other;

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

	private static final class Map1To0Node<K, V> extends CompactMapNode<K, V> {

		private final byte pos1;
		private final K key1;
		private final V val1;

		Map1To0Node(final AtomicReference<Thread> mutator, final byte pos1, final K key1, final V val1) {

			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					if (false && val.equals(val1)) {
						result = Result.unchanged(this);
					} else {
						// update key1, val1
						result = Result.updated(valNodeOf(mutator, pos1, key1, val), val1);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, mask, node));
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (false && cmp.compare(val, val1) == 0) {
						result = Result.unchanged(this);
					} else {
						// update key1, val1
						result = Result.updated(valNodeOf(mutator, pos1, key1, val), val1);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, mask, node));
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					// remove key1, val1
					result = Result.modified(CompactMapNode.<K, V> valNodeOf(mutator));
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					// remove key1, val1
					result = Result.modified(CompactMapNode.<K, V> valNodeOf(mutator));
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		private CompactMapNode<K, V> inlineValue(AtomicReference<Thread> mutator, byte mask, K key,
						V val) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, val, pos1, key1, val1);
			} else {
				return valNodeOf(mutator, pos1, key1, val1, mask, key, val);
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
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && key.equals(key1)) {
				return Optional.of(entryOf(key1, val1));
			} else {
				return Optional.empty();
			}
		}

		@Override
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
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
		SupplierIterator<K, V> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(new Object[] { key1, val1 });
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
		V headVal() {
			return val1;
		}

		@Override
		AbstractMapNode<K, V> getNode(int index) {
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
		V getValue(int index) {
			switch (index) {
			case 0:
				return val1;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			switch (index) {
			case 0:
				return entryOf(key1, val1);
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
			Map1To0Node<?, ?> that = (Map1To0Node<?, ?>) other;

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
			return String.format("[@%d: %s=%s]", pos1, key1, val1);
		}

	}

	private static final class Map1To1Node<K, V> extends CompactMapNode<K, V> {

		private final byte pos1;
		private final K key1;
		private final V val1;

		private final byte npos1;
		private final CompactMapNode<K, V> node1;

		Map1To1Node(final AtomicReference<Thread> mutator, final byte pos1, final K key1, final V val1,
						final byte npos1, final CompactMapNode<K, V> node1) {

			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;

			this.npos1 = npos1;
			this.node1 = node1;

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					if (false && val.equals(val1)) {
						result = Result.unchanged(this);
					} else {
						// update key1, val1
						result = Result.updated(valNodeOf(mutator, pos1, key1, val, npos1, node1), val1);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, mask, node, npos1, node1));
					} else {
						result = Result.modified(valNodeOf(mutator, npos1, node1, mask, node));
					}
				}
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
									nestedResult.getNode());

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (false && cmp.compare(val, val1) == 0) {
						result = Result.unchanged(this);
					} else {
						// update key1, val1
						result = Result.updated(valNodeOf(mutator, pos1, key1, val, npos1, node1), val1);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, mask, node, npos1, node1));
					} else {
						result = Result.modified(valNodeOf(mutator, npos1, node1, mask, node));
					}
				}
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
									nestedResult.getNode());

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, mask, updatedNode));
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
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, mask, updatedNode));
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

		private CompactMapNode<K, V> inlineValue(AtomicReference<Thread> mutator, byte mask, K key,
						V val) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, val, pos1, key1, val1, npos1, node1);
			} else {
				return valNodeOf(mutator, pos1, key1, val1, mask, key, val, npos1, node1);
			}
		}

		private CompactMapNode<K, V> removeNode1AndInlineValue(AtomicReference<Thread> mutator,
						byte mask, K key, V val) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, val, pos1, key1, val1);
			} else {
				return valNodeOf(mutator, pos1, key1, val1, mask, key, val);
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
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && key.equals(key1)) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return Optional.empty();
			}
		}

		@Override
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.<CompactMapNode<K, V>> of(new CompactMapNode[] { node1 });
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
		SupplierIterator<K, V> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(new Object[] { key1, val1 });
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
		V headVal() {
			return val1;
		}

		@Override
		AbstractMapNode<K, V> getNode(int index) {
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
		V getValue(int index) {
			switch (index) {
			case 0:
				return val1;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			switch (index) {
			case 0:
				return entryOf(key1, val1);
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
			Map1To1Node<?, ?> that = (Map1To1Node<?, ?>) other;

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
			return String.format("[@%d: %s=%s, @%d: %s]", pos1, key1, val1, npos1, node1);
		}

	}

	private static final class Map1To2Node<K, V> extends CompactMapNode<K, V> {

		private final byte pos1;
		private final K key1;
		private final V val1;

		private final byte npos1;
		private final CompactMapNode<K, V> node1;

		private final byte npos2;
		private final CompactMapNode<K, V> node2;

		Map1To2Node(final AtomicReference<Thread> mutator, final byte pos1, final K key1, final V val1,
						final byte npos1, final CompactMapNode<K, V> node1, final byte npos2,
						final CompactMapNode<K, V> node2) {

			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;

			this.npos1 = npos1;
			this.node1 = node1;

			this.npos2 = npos2;
			this.node2 = node2;

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					if (false && val.equals(val1)) {
						result = Result.unchanged(this);
					} else {
						// update key1, val1
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val, npos1, node1, npos2, node2),
										val1);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

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
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
									nestedResult.getNode(), npos2, node2);

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
									node1, mask, nestedResult.getNode());

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (false && cmp.compare(val, val1) == 0) {
						result = Result.unchanged(this);
					} else {
						// update key1, val1
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val, npos1, node1, npos2, node2),
										val1);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

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
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
									nestedResult.getNode(), npos2, node2);

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
									node1, mask, nestedResult.getNode());

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, mask,
										updatedNode, npos2, node2));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node2
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1,
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
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, mask,
										updatedNode, npos2, node2));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node2
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1,
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

		private CompactMapNode<K, V> inlineValue(AtomicReference<Thread> mutator, byte mask, K key,
						V val) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, val, pos1, key1, val1, npos1, node1, npos2, node2);
			} else {
				return valNodeOf(mutator, pos1, key1, val1, mask, key, val, npos1, node1, npos2, node2);
			}
		}

		private CompactMapNode<K, V> removeNode1AndInlineValue(AtomicReference<Thread> mutator,
						byte mask, K key, V val) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, val, pos1, key1, val1, npos2, node2);
			} else {
				return valNodeOf(mutator, pos1, key1, val1, mask, key, val, npos2, node2);
			}
		}

		private CompactMapNode<K, V> removeNode2AndInlineValue(AtomicReference<Thread> mutator,
						byte mask, K key, V val) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, val, pos1, key1, val1, npos1, node1);
			} else {
				return valNodeOf(mutator, pos1, key1, val1, mask, key, val, npos1, node1);
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
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && key.equals(key1)) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return Optional.empty();
			}
		}

		@Override
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
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
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.<CompactMapNode<K, V>> of(new CompactMapNode[] { node1, node2 });
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
		SupplierIterator<K, V> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(new Object[] { key1, val1 });
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
		V headVal() {
			return val1;
		}

		@Override
		AbstractMapNode<K, V> getNode(int index) {
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
		V getValue(int index) {
			switch (index) {
			case 0:
				return val1;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			switch (index) {
			case 0:
				return entryOf(key1, val1);
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
			Map1To2Node<?, ?> that = (Map1To2Node<?, ?>) other;

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
			return String.format("[@%d: %s=%s, @%d: %s, @%d: %s]", pos1, key1, val1, npos1, node1,
							npos2, node2);
		}

	}

	private static final class Map1To3Node<K, V> extends CompactMapNode<K, V> {

		private final byte pos1;
		private final K key1;
		private final V val1;

		private final byte npos1;
		private final CompactMapNode<K, V> node1;

		private final byte npos2;
		private final CompactMapNode<K, V> node2;

		private final byte npos3;
		private final CompactMapNode<K, V> node3;

		Map1To3Node(final AtomicReference<Thread> mutator, final byte pos1, final K key1, final V val1,
						final byte npos1, final CompactMapNode<K, V> node1, final byte npos2,
						final CompactMapNode<K, V> node2, final byte npos3,
						final CompactMapNode<K, V> node3) {

			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;

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
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					if (false && val.equals(val1)) {
						result = Result.unchanged(this);
					} else {
						// update key1, val1
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val, npos1, node1, npos2, node2,
														npos3, node3), val1);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

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
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
									nestedResult.getNode(), npos2, node2, npos3, node3);

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
									node1, mask, nestedResult.getNode(), npos3, node3);

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos3) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node3.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
									node1, npos2, node2, mask, nestedResult.getNode());

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (false && cmp.compare(val, val1) == 0) {
						result = Result.unchanged(this);
					} else {
						// update key1, val1
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val, npos1, node1, npos2, node2,
														npos3, node3), val1);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

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
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, mask,
									nestedResult.getNode(), npos2, node2, npos3, node3);

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
									node1, mask, nestedResult.getNode(), npos3, node3);

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos3) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node3.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, npos1,
									node1, npos2, node2, mask, nestedResult.getNode());

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2, npos3,
									node3));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, mask,
										updatedNode, npos2, node2, npos3, node3));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node2
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1,
										mask, updatedNode, npos3, node3));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos3) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node3.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode3AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node3
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1,
										npos2, node2, mask, updatedNode));
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
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, npos1, node1, npos2, node2, npos3,
									node3));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, mask,
										updatedNode, npos2, node2, npos3, node3));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node2
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1,
										mask, updatedNode, npos3, node3));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos3) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node3.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode3AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node3
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1,
										npos2, node2, mask, updatedNode));
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

		private CompactMapNode<K, V> inlineValue(AtomicReference<Thread> mutator, byte mask, K key,
						V val) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, val, pos1, key1, val1, npos1, node1, npos2, node2,
								npos3, node3);
			} else {
				return valNodeOf(mutator, pos1, key1, val1, mask, key, val, npos1, node1, npos2, node2,
								npos3, node3);
			}
		}

		private CompactMapNode<K, V> removeNode1AndInlineValue(AtomicReference<Thread> mutator,
						byte mask, K key, V val) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, val, pos1, key1, val1, npos2, node2, npos3, node3);
			} else {
				return valNodeOf(mutator, pos1, key1, val1, mask, key, val, npos2, node2, npos3, node3);
			}
		}

		private CompactMapNode<K, V> removeNode2AndInlineValue(AtomicReference<Thread> mutator,
						byte mask, K key, V val) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, val, pos1, key1, val1, npos1, node1, npos3, node3);
			} else {
				return valNodeOf(mutator, pos1, key1, val1, mask, key, val, npos1, node1, npos3, node3);
			}
		}

		private CompactMapNode<K, V> removeNode3AndInlineValue(AtomicReference<Thread> mutator,
						byte mask, K key, V val) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, val, pos1, key1, val1, npos1, node1, npos2, node2);
			} else {
				return valNodeOf(mutator, pos1, key1, val1, mask, key, val, npos1, node1, npos2, node2);
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
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && key.equals(key1)) {
				return Optional.of(entryOf(key1, val1));
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
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
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
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator
							.<CompactMapNode<K, V>> of(new CompactMapNode[] { node1, node2, node3 });
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
		SupplierIterator<K, V> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(new Object[] { key1, val1 });
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
		V headVal() {
			return val1;
		}

		@Override
		AbstractMapNode<K, V> getNode(int index) {
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
		V getValue(int index) {
			switch (index) {
			case 0:
				return val1;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			switch (index) {
			case 0:
				return entryOf(key1, val1);
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
			Map1To3Node<?, ?> that = (Map1To3Node<?, ?>) other;

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
			return String.format("[@%d: %s=%s, @%d: %s, @%d: %s, @%d: %s]", pos1, key1, val1, npos1,
							node1, npos2, node2, npos3, node3);
		}

	}

	private static final class Map2To0Node<K, V> extends CompactMapNode<K, V> {

		private final byte pos1;
		private final K key1;
		private final V val1;

		private final byte pos2;
		private final K key2;
		private final V val2;

		Map2To0Node(final AtomicReference<Thread> mutator, final byte pos1, final K key1, final V val1,
						final byte pos2, final K key2, final V val2) {

			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;

			this.pos2 = pos2;
			this.key2 = key2;
			this.val2 = val2;

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					if (false && val.equals(val1)) {
						result = Result.unchanged(this);
					} else {
						// update key1, val1
						result = Result.updated(valNodeOf(mutator, pos1, key1, val, pos2, key2, val2),
										val1);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos2, key2, val2, mask, node));
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					if (false && val.equals(val2)) {
						result = Result.unchanged(this);
					} else {
						// update key2, val2
						result = Result.updated(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val),
										val2);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, mask, node));
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (false && cmp.compare(val, val1) == 0) {
						result = Result.unchanged(this);
					} else {
						// update key1, val1
						result = Result.updated(valNodeOf(mutator, pos1, key1, val, pos2, key2, val2),
										val1);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos2, key2, val2, mask, node));
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					if (false && cmp.compare(val, val2) == 0) {
						result = Result.unchanged(this);
					} else {
						// update key2, val2
						result = Result.updated(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val),
										val2);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, mask, node));
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					/*
					 * Create node with pair key2, val2. This node will a) either
					 * become the new root returned, or b) unwrapped and inlined.
					 */
					final byte pos2AtShiftZero = (shift == 0) ? pos2
									: (byte) (keyHash & BIT_PARTITION_MASK);
					result = Result.modified(valNodeOf(mutator, pos2AtShiftZero, key2, val2));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					/*
					 * Create node with pair key1, val1. This node will a) either
					 * become the new root returned, or b) unwrapped and inlined.
					 */
					final byte pos1AtShiftZero = (shift == 0) ? pos1
									: (byte) (keyHash & BIT_PARTITION_MASK);
					result = Result.modified(valNodeOf(mutator, pos1AtShiftZero, key1, val1));
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					/*
					 * Create node with pair key2, val2. This node will a) either
					 * become the new root returned, or b) unwrapped and inlined.
					 */
					final byte pos2AtShiftZero = (shift == 0) ? pos2
									: (byte) (keyHash & BIT_PARTITION_MASK);
					result = Result.modified(valNodeOf(mutator, pos2AtShiftZero, key2, val2));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					/*
					 * Create node with pair key1, val1. This node will a) either
					 * become the new root returned, or b) unwrapped and inlined.
					 */
					final byte pos1AtShiftZero = (shift == 0) ? pos1
									: (byte) (keyHash & BIT_PARTITION_MASK);
					result = Result.modified(valNodeOf(mutator, pos1AtShiftZero, key1, val1));
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		private CompactMapNode<K, V> inlineValue(AtomicReference<Thread> mutator, byte mask, K key,
						V val) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, val, pos1, key1, val1, pos2, key2, val2);
			} else if (mask < pos2) {
				return valNodeOf(mutator, pos1, key1, val1, mask, key, val, pos2, key2, val2);
			} else {
				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, mask, key, val);
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
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && key.equals(key1)) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && key.equals(key2)) {
				return Optional.of(entryOf(key2, val2));
			} else {
				return Optional.empty();
			}
		}

		@Override
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(entryOf(key2, val2));
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
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
		SupplierIterator<K, V> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(new Object[] { key1, val1, key2, val2 });
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
		V headVal() {
			return val1;
		}

		@Override
		AbstractMapNode<K, V> getNode(int index) {
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
		V getValue(int index) {
			switch (index) {
			case 0:
				return val1;
			case 1:
				return val2;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			switch (index) {
			case 0:
				return entryOf(key1, val1);
			case 1:
				return entryOf(key2, val2);
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
			Map2To0Node<?, ?> that = (Map2To0Node<?, ?>) other;

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
			return String.format("[@%d: %s=%s, @%d: %s=%s]", pos1, key1, val1, pos2, key2, val2);
		}

	}

	private static final class Map2To1Node<K, V> extends CompactMapNode<K, V> {

		private final byte pos1;
		private final K key1;
		private final V val1;

		private final byte pos2;
		private final K key2;
		private final V val2;

		private final byte npos1;
		private final CompactMapNode<K, V> node1;

		Map2To1Node(final AtomicReference<Thread> mutator, final byte pos1, final K key1, final V val1,
						final byte pos2, final K key2, final V val2, final byte npos1,
						final CompactMapNode<K, V> node1) {

			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;

			this.pos2 = pos2;
			this.key2 = key2;
			this.val2 = val2;

			this.npos1 = npos1;
			this.node1 = node1;

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					if (false && val.equals(val1)) {
						result = Result.unchanged(this);
					} else {
						// update key1, val1
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val, pos2, key2, val2, npos1,
														node1), val1);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos2, key2, val2, mask, node,
										npos1, node1));
					} else {
						result = Result.modified(valNodeOf(mutator, pos2, key2, val2, npos1, node1,
										mask, node));
					}
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					if (false && val.equals(val2)) {
						result = Result.unchanged(this);
					} else {
						// update key2, val2
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val1, pos2, key2, val, npos1,
														node1), val2);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, mask, node,
										npos1, node1));
					} else {
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1,
										mask, node));
					}
				}
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2,
									key2, val2, mask, nestedResult.getNode());

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (false && cmp.compare(val, val1) == 0) {
						result = Result.unchanged(this);
					} else {
						// update key1, val1
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val, pos2, key2, val2, npos1,
														node1), val1);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos2, key2, val2, mask, node,
										npos1, node1));
					} else {
						result = Result.modified(valNodeOf(mutator, pos2, key2, val2, npos1, node1,
										mask, node));
					}
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					if (false && cmp.compare(val, val2) == 0) {
						result = Result.unchanged(this);
					} else {
						// update key2, val2
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val1, pos2, key2, val, npos1,
														node1), val2);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, mask, node,
										npos1, node1));
					} else {
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1,
										mask, node));
					}
				}
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2,
									key2, val2, mask, nestedResult.getNode());

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, pos2, key2, val2, npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					// remove key2, val2
					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
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
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, pos2, key2, val2, npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					// remove key2, val2
					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
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

		private CompactMapNode<K, V> inlineValue(AtomicReference<Thread> mutator, byte mask, K key,
						V val) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, val, pos1, key1, val1, pos2, key2, val2, npos1,
								node1);
			} else if (mask < pos2) {
				return valNodeOf(mutator, pos1, key1, val1, mask, key, val, pos2, key2, val2, npos1,
								node1);
			} else {
				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, mask, key, val, npos1,
								node1);
			}
		}

		private CompactMapNode<K, V> removeNode1AndInlineValue(AtomicReference<Thread> mutator,
						byte mask, K key, V val) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, val, pos1, key1, val1, pos2, key2, val2);
			} else if (mask < pos2) {
				return valNodeOf(mutator, pos1, key1, val1, mask, key, val, pos2, key2, val2);
			} else {
				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, mask, key, val);
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
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && key.equals(key1)) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && key.equals(key2)) {
				return Optional.of(entryOf(key2, val2));
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return Optional.empty();
			}
		}

		@Override
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(entryOf(key2, val2));
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.<CompactMapNode<K, V>> of(new CompactMapNode[] { node1 });
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
		SupplierIterator<K, V> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(new Object[] { key1, val1, key2, val2 });
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
		V headVal() {
			return val1;
		}

		@Override
		AbstractMapNode<K, V> getNode(int index) {
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
		V getValue(int index) {
			switch (index) {
			case 0:
				return val1;
			case 1:
				return val2;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			switch (index) {
			case 0:
				return entryOf(key1, val1);
			case 1:
				return entryOf(key2, val2);
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
			Map2To1Node<?, ?> that = (Map2To1Node<?, ?>) other;

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
			return String.format("[@%d: %s=%s, @%d: %s=%s, @%d: %s]", pos1, key1, val1, pos2, key2,
							val2, npos1, node1);
		}

	}

	private static final class Map2To2Node<K, V> extends CompactMapNode<K, V> {

		private final byte pos1;
		private final K key1;
		private final V val1;

		private final byte pos2;
		private final K key2;
		private final V val2;

		private final byte npos1;
		private final CompactMapNode<K, V> node1;

		private final byte npos2;
		private final CompactMapNode<K, V> node2;

		Map2To2Node(final AtomicReference<Thread> mutator, final byte pos1, final K key1, final V val1,
						final byte pos2, final K key2, final V val2, final byte npos1,
						final CompactMapNode<K, V> node1, final byte npos2,
						final CompactMapNode<K, V> node2) {

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

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					if (false && val.equals(val1)) {
						result = Result.unchanged(this);
					} else {
						// update key1, val1
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val, pos2, key2, val2, npos1,
														node1, npos2, node2), val1);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos2, key2, val2, mask, node,
										npos1, node1, npos2, node2));
					} else if (mask < npos2) {
						result = Result.modified(valNodeOf(mutator, pos2, key2, val2, npos1, node1,
										mask, node, npos2, node2));
					} else {
						result = Result.modified(valNodeOf(mutator, pos2, key2, val2, npos1, node1,
										npos2, node2, mask, node));
					}
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					if (false && val.equals(val2)) {
						result = Result.unchanged(this);
					} else {
						// update key2, val2
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val1, pos2, key2, val, npos1,
														node1, npos2, node2), val2);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, mask, node,
										npos1, node1, npos2, node2));
					} else if (mask < npos2) {
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1,
										mask, node, npos2, node2));
					} else {
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1,
										npos2, node2, mask, node));
					}
				}
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2,
									key2, val2, mask, nestedResult.getNode(), npos2, node2);

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2,
									key2, val2, npos1, node1, mask, nestedResult.getNode());

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (false && cmp.compare(val, val1) == 0) {
						result = Result.unchanged(this);
					} else {
						// update key1, val1
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val, pos2, key2, val2, npos1,
														node1, npos2, node2), val1);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos2, key2, val2, mask, node,
										npos1, node1, npos2, node2));
					} else if (mask < npos2) {
						result = Result.modified(valNodeOf(mutator, pos2, key2, val2, npos1, node1,
										mask, node, npos2, node2));
					} else {
						result = Result.modified(valNodeOf(mutator, pos2, key2, val2, npos1, node1,
										npos2, node2, mask, node));
					}
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					if (false && cmp.compare(val, val2) == 0) {
						result = Result.unchanged(this);
					} else {
						// update key2, val2
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val1, pos2, key2, val, npos1,
														node1, npos2, node2), val2);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, mask, node,
										npos1, node1, npos2, node2));
					} else if (mask < npos2) {
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1,
										mask, node, npos2, node2));
					} else {
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1,
										npos2, node2, mask, node));
					}
				}
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2,
									key2, val2, mask, nestedResult.getNode(), npos2, node2);

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2,
									key2, val2, npos1, node1, mask, nestedResult.getNode());

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, pos2, key2, val2, npos1, node1, npos2,
									node2));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					// remove key2, val2
					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1, npos2,
									node2));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
										mask, updatedNode, npos2, node2));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node2
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
										npos1, node1, mask, updatedNode));
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
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, pos2, key2, val2, npos1, node1, npos2,
									node2));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					// remove key2, val2
					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, npos1, node1, npos2,
									node2));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
										mask, updatedNode, npos2, node2));
						break;

					default:
						throw new IllegalStateException("Size predicate violates node invariant.");
					}
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos2) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node2.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode2AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node2
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
										npos1, node1, mask, updatedNode));
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

		private CompactMapNode<K, V> inlineValue(AtomicReference<Thread> mutator, byte mask, K key,
						V val) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, val, pos1, key1, val1, pos2, key2, val2, npos1,
								node1, npos2, node2);
			} else if (mask < pos2) {
				return valNodeOf(mutator, pos1, key1, val1, mask, key, val, pos2, key2, val2, npos1,
								node1, npos2, node2);
			} else {
				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, mask, key, val, npos1,
								node1, npos2, node2);
			}
		}

		private CompactMapNode<K, V> removeNode1AndInlineValue(AtomicReference<Thread> mutator,
						byte mask, K key, V val) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, val, pos1, key1, val1, pos2, key2, val2, npos2,
								node2);
			} else if (mask < pos2) {
				return valNodeOf(mutator, pos1, key1, val1, mask, key, val, pos2, key2, val2, npos2,
								node2);
			} else {
				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, mask, key, val, npos2,
								node2);
			}
		}

		private CompactMapNode<K, V> removeNode2AndInlineValue(AtomicReference<Thread> mutator,
						byte mask, K key, V val) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, val, pos1, key1, val1, pos2, key2, val2, npos1,
								node1);
			} else if (mask < pos2) {
				return valNodeOf(mutator, pos1, key1, val1, mask, key, val, pos2, key2, val2, npos1,
								node1);
			} else {
				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, mask, key, val, npos1,
								node1);
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
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && key.equals(key1)) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && key.equals(key2)) {
				return Optional.of(entryOf(key2, val2));
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else if (mask == npos2) {
				return node2.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return Optional.empty();
			}
		}

		@Override
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
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
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.<CompactMapNode<K, V>> of(new CompactMapNode[] { node1, node2 });
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
		SupplierIterator<K, V> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(new Object[] { key1, val1, key2, val2 });
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
		V headVal() {
			return val1;
		}

		@Override
		AbstractMapNode<K, V> getNode(int index) {
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
		V getValue(int index) {
			switch (index) {
			case 0:
				return val1;
			case 1:
				return val2;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			switch (index) {
			case 0:
				return entryOf(key1, val1);
			case 1:
				return entryOf(key2, val2);
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
			Map2To2Node<?, ?> that = (Map2To2Node<?, ?>) other;

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
			return String.format("[@%d: %s=%s, @%d: %s=%s, @%d: %s, @%d: %s]", pos1, key1, val1, pos2,
							key2, val2, npos1, node1, npos2, node2);
		}

	}

	private static final class Map3To0Node<K, V> extends CompactMapNode<K, V> {

		private final byte pos1;
		private final K key1;
		private final V val1;

		private final byte pos2;
		private final K key2;
		private final V val2;

		private final byte pos3;
		private final K key3;
		private final V val3;

		Map3To0Node(final AtomicReference<Thread> mutator, final byte pos1, final K key1, final V val1,
						final byte pos2, final K key2, final V val2, final byte pos3, final K key3,
						final V val3) {

			this.pos1 = pos1;
			this.key1 = key1;
			this.val1 = val1;

			this.pos2 = pos2;
			this.key2 = key2;
			this.val2 = val2;

			this.pos3 = pos3;
			this.key3 = key3;
			this.val3 = val3;

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					if (false && val.equals(val1)) {
						result = Result.unchanged(this);
					} else {
						// update key1, val1
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val, pos2, key2, val2, pos3,
														key3, val3), val1);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos2, key2, val2, pos3, key3, val3,
									mask, node));
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					if (false && val.equals(val2)) {
						result = Result.unchanged(this);
					} else {
						// update key2, val2
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val1, pos2, key2, val, pos3,
														key3, val3), val2);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos3, key3, val3,
									mask, node));
				}
			} else if (mask == pos3) {
				if (key.equals(key3)) {
					if (false && val.equals(val3)) {
						result = Result.unchanged(this);
					} else {
						// update key3, val3
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3,
														key3, val), val3);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key3, key3.hashCode(), val3, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
									mask, node));
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (false && cmp.compare(val, val1) == 0) {
						result = Result.unchanged(this);
					} else {
						// update key1, val1
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val, pos2, key2, val2, pos3,
														key3, val3), val1);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos2, key2, val2, pos3, key3, val3,
									mask, node));
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					if (false && cmp.compare(val, val2) == 0) {
						result = Result.unchanged(this);
					} else {
						// update key2, val2
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val1, pos2, key2, val, pos3,
														key3, val3), val2);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos3, key3, val3,
									mask, node));
				}
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					if (false && cmp.compare(val, val3) == 0) {
						result = Result.unchanged(this);
					} else {
						// update key3, val3
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3,
														key3, val), val3);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key3, key3.hashCode(), val3, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
									mask, node));
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, pos2, key2, val2, pos3, key3, val3));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					// remove key2, val2
					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos3, key3, val3));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos3) {
				if (key.equals(key3)) {
					// remove key3, val3
					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2));
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, pos2, key2, val2, pos3, key3, val3));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					// remove key2, val2
					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos3, key3, val3));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					// remove key3, val3
					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2));
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		private CompactMapNode<K, V> inlineValue(AtomicReference<Thread> mutator, byte mask, K key,
						V val) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, val, pos1, key1, val1, pos2, key2, val2, pos3,
								key3, val3);
			} else if (mask < pos2) {
				return valNodeOf(mutator, pos1, key1, val1, mask, key, val, pos2, key2, val2, pos3,
								key3, val3);
			} else if (mask < pos3) {
				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, mask, key, val, pos3,
								key3, val3);
			} else {
				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3, key3, val3, mask,
								key, val);
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
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && key.equals(key1)) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && key.equals(key2)) {
				return Optional.of(entryOf(key2, val2));
			} else if (mask == pos3 && key.equals(key3)) {
				return Optional.of(entryOf(key3, val3));
			} else {
				return Optional.empty();
			}
		}

		@Override
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && cmp.compare(key, key1) == 0) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && cmp.compare(key, key2) == 0) {
				return Optional.of(entryOf(key2, val2));
			} else if (mask == pos3 && cmp.compare(key, key3) == 0) {
				return Optional.of(entryOf(key3, val3));
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
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
		SupplierIterator<K, V> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(new Object[] { key1, val1, key2, val2, key3, val3 });
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
		V headVal() {
			return val1;
		}

		@Override
		AbstractMapNode<K, V> getNode(int index) {
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
		V getValue(int index) {
			switch (index) {
			case 0:
				return val1;
			case 1:
				return val2;
			case 2:
				return val3;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			switch (index) {
			case 0:
				return entryOf(key1, val1);
			case 1:
				return entryOf(key2, val2);
			case 2:
				return entryOf(key3, val3);
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
			Map3To0Node<?, ?> that = (Map3To0Node<?, ?>) other;

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
			return String.format("[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s]", pos1, key1, val1, pos2, key2,
							val2, pos3, key3, val3);
		}

	}

	private static final class Map3To1Node<K, V> extends CompactMapNode<K, V> {

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
		private final CompactMapNode<K, V> node1;

		Map3To1Node(final AtomicReference<Thread> mutator, final byte pos1, final K key1, final V val1,
						final byte pos2, final K key2, final V val2, final byte pos3, final K key3,
						final V val3, final byte npos1, final CompactMapNode<K, V> node1) {

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

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					if (false && val.equals(val1)) {
						result = Result.unchanged(this);
					} else {
						// update key1, val1
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val, pos2, key2, val2, pos3,
														key3, val3, npos1, node1), val1);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos2, key2, val2, pos3, key3, val3,
										mask, node, npos1, node1));
					} else {
						result = Result.modified(valNodeOf(mutator, pos2, key2, val2, pos3, key3, val3,
										npos1, node1, mask, node));
					}
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					if (false && val.equals(val2)) {
						result = Result.unchanged(this);
					} else {
						// update key2, val2
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val1, pos2, key2, val, pos3,
														key3, val3, npos1, node1), val2);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos3, key3, val3,
										mask, node, npos1, node1));
					} else {
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos3, key3, val3,
										npos1, node1, mask, node));
					}
				}
			} else if (mask == pos3) {
				if (key.equals(key3)) {
					if (false && val.equals(val3)) {
						result = Result.unchanged(this);
					} else {
						// update key3, val3
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3,
														key3, val, npos1, node1), val3);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key3, key3.hashCode(), val3, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
										mask, node, npos1, node1));
					} else {
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
										npos1, node1, mask, node));
					}
				}
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2,
									key2, val2, pos3, key3, val3, mask, nestedResult.getNode());

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (false && cmp.compare(val, val1) == 0) {
						result = Result.unchanged(this);
					} else {
						// update key1, val1
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val, pos2, key2, val2, pos3,
														key3, val3, npos1, node1), val1);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos2, key2, val2, pos3, key3, val3,
										mask, node, npos1, node1));
					} else {
						result = Result.modified(valNodeOf(mutator, pos2, key2, val2, pos3, key3, val3,
										npos1, node1, mask, node));
					}
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					if (false && cmp.compare(val, val2) == 0) {
						result = Result.unchanged(this);
					} else {
						// update key2, val2
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val1, pos2, key2, val, pos3,
														key3, val3, npos1, node1), val2);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos3, key3, val3,
										mask, node, npos1, node1));
					} else {
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos3, key3, val3,
										npos1, node1, mask, node));
					}
				}
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					if (false && cmp.compare(val, val3) == 0) {
						result = Result.unchanged(this);
					} else {
						// update key3, val3
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3,
														key3, val, npos1, node1), val3);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key3, key3.hashCode(), val3, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					if (mask < npos1) {
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
										mask, node, npos1, node1));
					} else {
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
										npos1, node1, mask, node));
					}
				}
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> thisNew = valNodeOf(mutator, pos1, key1, val1, pos2,
									key2, val2, pos3, key3, val3, mask, nestedResult.getNode());

					if (nestedResult.hasReplacedValue()) {
						result = Result.updated(thisNew, nestedResult.getReplacedValue());
					} else {
						result = Result.modified(thisNew);
					}
				} else {
					result = Result.unchanged(this);
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, pos2, key2, val2, pos3, key3, val3,
									npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					// remove key2, val2
					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos3, key3, val3,
									npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos3) {
				if (key.equals(key3)) {
					// remove key3, val3
					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
									npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
										pos3, key3, val3, mask, updatedNode));
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
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, pos2, key2, val2, pos3, key3, val3,
									npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					// remove key2, val2
					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos3, key3, val3,
									npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					// remove key3, val3
					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
									npos1, node1));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == npos1) {
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = node1.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (nestedResult.isModified()) {
					final CompactMapNode<K, V> updatedNode = nestedResult.getNode();

					switch (updatedNode.sizePredicate()) {
					case SIZE_ONE:
						// inline sub-node value
						result = Result.modified(removeNode1AndInlineValue(mutator, mask,
										updatedNode.headKey(), updatedNode.headVal()));
						break;

					case SIZE_MORE_THAN_ONE:
						// update node1
						result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
										pos3, key3, val3, mask, updatedNode));
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

		private CompactMapNode<K, V> inlineValue(AtomicReference<Thread> mutator, byte mask, K key,
						V val) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, val, pos1, key1, val1, pos2, key2, val2, pos3,
								key3, val3, npos1, node1);
			} else if (mask < pos2) {
				return valNodeOf(mutator, pos1, key1, val1, mask, key, val, pos2, key2, val2, pos3,
								key3, val3, npos1, node1);
			} else if (mask < pos3) {
				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, mask, key, val, pos3,
								key3, val3, npos1, node1);
			} else {
				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3, key3, val3, mask,
								key, val, npos1, node1);
			}
		}

		private CompactMapNode<K, V> removeNode1AndInlineValue(AtomicReference<Thread> mutator,
						byte mask, K key, V val) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, val, pos1, key1, val1, pos2, key2, val2, pos3,
								key3, val3);
			} else if (mask < pos2) {
				return valNodeOf(mutator, pos1, key1, val1, mask, key, val, pos2, key2, val2, pos3,
								key3, val3);
			} else if (mask < pos3) {
				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, mask, key, val, pos3,
								key3, val3);
			} else {
				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3, key3, val3, mask,
								key, val);
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
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && key.equals(key1)) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && key.equals(key2)) {
				return Optional.of(entryOf(key2, val2));
			} else if (mask == pos3 && key.equals(key3)) {
				return Optional.of(entryOf(key3, val3));
			} else if (mask == npos1) {
				return node1.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			} else {
				return Optional.empty();
			}
		}

		@Override
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
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
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.<CompactMapNode<K, V>> of(new CompactMapNode[] { node1 });
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
		SupplierIterator<K, V> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(new Object[] { key1, val1, key2, val2, key3, val3 });
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
		V headVal() {
			return val1;
		}

		@Override
		AbstractMapNode<K, V> getNode(int index) {
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
		V getValue(int index) {
			switch (index) {
			case 0:
				return val1;
			case 1:
				return val2;
			case 2:
				return val3;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			switch (index) {
			case 0:
				return entryOf(key1, val1);
			case 1:
				return entryOf(key2, val2);
			case 2:
				return entryOf(key3, val3);
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
			Map3To1Node<?, ?> that = (Map3To1Node<?, ?>) other;

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
			return String.format("[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s]", pos1, key1, val1,
							pos2, key2, val2, pos3, key3, val3, npos1, node1);
		}

	}

	private static final class Map4To0Node<K, V> extends CompactMapNode<K, V> {

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

		Map4To0Node(final AtomicReference<Thread> mutator, final byte pos1, final K key1, final V val1,
						final byte pos2, final K key2, final V val2, final byte pos3, final K key3,
						final V val3, final byte pos4, final K key4, final V val4) {

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

			assert nodeInvariant();
			assert USE_SPECIALIAZIONS;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					if (false && val.equals(val1)) {
						result = Result.unchanged(this);
					} else {
						// update key1, val1
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val, pos2, key2, val2, pos3,
														key3, val3, pos4, key4, val4), val1);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos2, key2, val2, pos3, key3, val3,
									pos4, key4, val4, mask, node));
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					if (false && val.equals(val2)) {
						result = Result.unchanged(this);
					} else {
						// update key2, val2
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val1, pos2, key2, val, pos3,
														key3, val3, pos4, key4, val4), val2);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos3, key3, val3,
									pos4, key4, val4, mask, node));
				}
			} else if (mask == pos3) {
				if (key.equals(key3)) {
					if (false && val.equals(val3)) {
						result = Result.unchanged(this);
					} else {
						// update key3, val3
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3,
														key3, val, pos4, key4, val4), val3);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key3, key3.hashCode(), val3, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
									pos4, key4, val4, mask, node));
				}
			} else if (mask == pos4) {
				if (key.equals(key4)) {
					if (false && val.equals(val4)) {
						result = Result.unchanged(this);
					} else {
						// update key4, val4
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3,
														key3, val3, pos4, key4, val), val4);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key4, key4.hashCode(), val4, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
									pos3, key3, val3, mask, node));
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					if (false && cmp.compare(val, val1) == 0) {
						result = Result.unchanged(this);
					} else {
						// update key1, val1
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val, pos2, key2, val2, pos3,
														key3, val3, pos4, key4, val4), val1);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key1, key1.hashCode(), val1, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos2, key2, val2, pos3, key3, val3,
									pos4, key4, val4, mask, node));
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					if (false && cmp.compare(val, val2) == 0) {
						result = Result.unchanged(this);
					} else {
						// update key2, val2
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val1, pos2, key2, val, pos3,
														key3, val3, pos4, key4, val4), val2);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key2, key2.hashCode(), val2, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos3, key3, val3,
									pos4, key4, val4, mask, node));
				}
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					if (false && cmp.compare(val, val3) == 0) {
						result = Result.unchanged(this);
					} else {
						// update key3, val3
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3,
														key3, val, pos4, key4, val4), val3);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key3, key3.hashCode(), val3, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
									pos4, key4, val4, mask, node));
				}
			} else if (mask == pos4) {
				if (cmp.compare(key, key4) == 0) {
					if (false && cmp.compare(val, val4) == 0) {
						result = Result.unchanged(this);
					} else {
						// update key4, val4
						result = Result.updated(
										valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3,
														key3, val3, pos4, key4, val), val4);
					}
				} else {
					// merge into node
					final CompactMapNode<K, V> node = mergeNodes(key4, key4.hashCode(), val4, key,
									keyHash, val, shift + BIT_PARTITION_SIZE);

					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
									pos3, key3, val3, mask, node));
				}
			} else {
				// no value
				result = Result.modified(inlineValue(mutator, mask, key, val));
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (key.equals(key1)) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, pos2, key2, val2, pos3, key3, val3,
									pos4, key4, val4));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (key.equals(key2)) {
					// remove key2, val2
					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos3, key3, val3,
									pos4, key4, val4));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos3) {
				if (key.equals(key3)) {
					// remove key3, val3
					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
									pos4, key4, val4));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos4) {
				if (key.equals(key4)) {
					// remove key4, val4
					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
									pos3, key3, val3));
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);
			final Result<K, V, ? extends CompactMapNode<K, V>> result;

			if (mask == pos1) {
				if (cmp.compare(key, key1) == 0) {
					// remove key1, val1
					result = Result.modified(valNodeOf(mutator, pos2, key2, val2, pos3, key3, val3,
									pos4, key4, val4));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos2) {
				if (cmp.compare(key, key2) == 0) {
					// remove key2, val2
					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos3, key3, val3,
									pos4, key4, val4));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos3) {
				if (cmp.compare(key, key3) == 0) {
					// remove key3, val3
					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
									pos4, key4, val4));
				} else {
					result = Result.unchanged(this);
				}
			} else if (mask == pos4) {
				if (cmp.compare(key, key4) == 0) {
					// remove key4, val4
					result = Result.modified(valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2,
									pos3, key3, val3));
				} else {
					result = Result.unchanged(this);
				}
			} else {
				result = Result.unchanged(this);
			}

			return result;
		}

		private CompactMapNode<K, V> inlineValue(AtomicReference<Thread> mutator, byte mask, K key,
						V val) {
			if (mask < pos1) {
				return valNodeOf(mutator, mask, key, val, pos1, key1, val1, pos2, key2, val2, pos3,
								key3, val3, pos4, key4, val4);
			} else if (mask < pos2) {
				return valNodeOf(mutator, pos1, key1, val1, mask, key, val, pos2, key2, val2, pos3,
								key3, val3, pos4, key4, val4);
			} else if (mask < pos3) {
				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, mask, key, val, pos3,
								key3, val3, pos4, key4, val4);
			} else if (mask < pos4) {
				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3, key3, val3, mask,
								key, val, pos4, key4, val4);
			} else {
				return valNodeOf(mutator, pos1, key1, val1, pos2, key2, val2, pos3, key3, val3, pos4,
								key4, val4, mask, key, val);
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
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift) {
			final byte mask = (byte) ((keyHash >>> shift) & BIT_PARTITION_MASK);

			if (mask == pos1 && key.equals(key1)) {
				return Optional.of(entryOf(key1, val1));
			} else if (mask == pos2 && key.equals(key2)) {
				return Optional.of(entryOf(key2, val2));
			} else if (mask == pos3 && key.equals(key3)) {
				return Optional.of(entryOf(key3, val3));
			} else if (mask == pos4 && key.equals(key4)) {
				return Optional.of(entryOf(key4, val4));
			} else {
				return Optional.empty();
			}
		}

		@Override
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
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
			} else {
				return Optional.empty();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
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
		SupplierIterator<K, V> payloadIterator() {
			return ArrayKeyValueSupplierIterator.of(new Object[] { key1, val1, key2, val2, key3, val3, key4,
							val4 });
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
		V headVal() {
			return val1;
		}

		@Override
		AbstractMapNode<K, V> getNode(int index) {
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
		V getValue(int index) {
			switch (index) {
			case 0:
				return val1;
			case 1:
				return val2;
			case 2:
				return val3;
			case 3:
				return val4;
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		Map.Entry<K, V> getKeyValueEntry(int index) {
			switch (index) {
			case 0:
				return entryOf(key1, val1);
			case 1:
				return entryOf(key2, val2);
			case 2:
				return entryOf(key3, val3);
			case 3:
				return entryOf(key4, val4);
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
			Map4To0Node<?, ?> that = (Map4To0Node<?, ?>) other;

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
			return String.format("[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s]", pos1, key1, val1,
							pos2, key2, val2, pos3, key3, val3, pos4, key4, val4);
		}

	}

}
