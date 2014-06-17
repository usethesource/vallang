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

	private static final boolean DEBUG = false;
	private static final boolean USE_STACK_ITERATOR = true; // does not effect
															// TransientMap

	private final AbstractMapNode<K, V> rootNode;
	private final int hashCode;
	private final int cachedSize;

	TrieMap(AbstractMapNode<K, V> rootNode, int hashCode, int cachedSize) {
		this.rootNode = rootNode;
		this.hashCode = hashCode;
		this.cachedSize = cachedSize;
		if (DEBUG) {
			assert invariant();
		}
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
			final V val = (V) keyValuePairs[i + 1];

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
			final V val = (V) keyValuePairs[i + 1];

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

	@Override
	public TrieMap<K, V> __put(K key, V val) {
		final int keyHash = key.hashCode();
		final Result<K, V, ? extends AbstractMapNode<K, V>> result = rootNode.updated(null, key,
						keyHash, val, 0);

		if (result.isModified()) {
			if (result.hasReplacedValue()) {
				final int valHashOld = result.getReplacedValue().hashCode();
				final int valHashNew = val.hashCode();

				return new TrieMap<K, V>(result.getNode(), hashCode + (keyHash ^ valHashNew)
								- (keyHash ^ valHashOld), cachedSize);
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

				return new TrieMap<K, V>(result.getNode(), hashCode + (keyHash ^ valHashNew)
								- (keyHash ^ valHashOld), cachedSize);
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

			final int valHash = rootNode.findByKey(key, keyHash, 0).get().getValue().hashCode();

			return new TrieMap<K, V>(result.getNode(), hashCode - (keyHash ^ valHash),
							cachedSize - 1);
		}

		return this;
	}

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

				@SuppressWarnings("deprecation")
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

	static final class TransientTrieMap<K, V> extends AbstractMap<K, V> implements
					TransientMap<K, V> {
		final private AtomicReference<Thread> mutator;
		private AbstractMapNode<K, V> rootNode;
		private int hashCode;
		private int cachedSize;

		TransientTrieMap(TrieMap<K, V> trieMap) {
			this.mutator = new AtomicReference<Thread>(Thread.currentThread());
			this.rootNode = trieMap.rootNode;
			this.hashCode = trieMap.hashCode;
			this.cachedSize = trieMap.cachedSize;
			if (DEBUG) {
				assert invariant();
			}
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
			final Optional<Map.Entry<K, V>> result = rootNode.findByKey(key, key.hashCode(), 0);

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

					if (DEBUG) {
						assert invariant();
					}
					return old;
				} else {
					final int valHashNew = val.hashCode();

					hashCode += keyHash ^ valHashNew;
					cachedSize += 1;

					if (DEBUG) {
						assert invariant();
					}
					return null;
				}
			}

			if (DEBUG) {
				assert invariant();
			}
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

					if (DEBUG) {
						assert invariant();
					}
					return old;
				} else {
					final int valHashNew = val.hashCode();

					hashCode += keyHash ^ valHashNew;
					cachedSize += 1;

					if (DEBUG) {
						assert invariant();
					}
					return null;
				}
			}

			if (DEBUG) {
				assert invariant();
			}
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

				final int valHash = rootNode.findByKey(key, keyHash, 0).get().getValue().hashCode();

				rootNode = result.getNode();
				hashCode -= keyHash ^ valHash;
				cachedSize -= 1;

				if (DEBUG) {
					assert invariant();
				}
				return true;
			}

			if (DEBUG) {
				assert invariant();
			}
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

				if (DEBUG) {
					assert invariant();
				}
				return true;
			}

			if (DEBUG) {
				assert invariant();
			}
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
			// return new TrieMapValueIterator<>(keyIterator());
			return new MapValueIterator<>(rootNode); // TODO: iterator does not
														// support removal
		}

		@Override
		public Iterator<Map.Entry<K, V>> entryIterator() {
			// return new TrieMapEntryIterator<>(keyIterator());
			return new MapEntryIterator<>(rootNode); // TODO: iterator does not
														// support removal
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

	// TODO: replace by immutable cons list
	private static final class HashCollisionMapNode<K, V> extends CompactMapNode<K, V> {
		private final K[] keys;
		private final V[] vals;
		private final int hash;

		HashCollisionMapNode(int hash, K[] keys, V[] vals) {
			super(null, 0, 0); // TODO: Split compact node base class and remove dependency on constructor
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
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator,
						K key, int keyHash, V val, int shift, Comparator<Object> cmp) {
			if (this.hash != keyHash) {
				return Result.modified(mergeNodes(this, this.hash, key, keyHash, val, shift));
			}

			for (int i = 0; i < keys.length; i++) {
				if (cmp.compare(keys[i], key) == 0) {

					final V currentVal = vals[i];

					if (cmp.compare(currentVal, val) == 0) {
						return Result.unchanged(this);
					}

					final CompactMapNode<K, V> thisNew;

					// // update mapping
					// if (isAllowedToEdit(this.mutator, mutator)) {
					// // no copying if already editable
					// this.vals[i] = val;
					// thisNew = this;
					// } else {
					@SuppressWarnings("unchecked")
					final V[] editableVals = (V[]) copyAndSet(this.vals, i, val);

					thisNew = new HashCollisionMapNode<>(this.hash, this.keys, editableVals);
					// }

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
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator,
						K key, int keyHash, int shift, Comparator<Object> cmp) {
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
						return CompactMapNode.<K, V> valNodeOf(mutator).updated(mutator,
										theOtherKey, keyHash, theOtherVal, 0, cmp);
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
				@SuppressWarnings("deprecation")
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
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator,
						K key, int keyHash, V val, int shift) {
			return updated(mutator, key, keyHash, val, shift,
							EqualityUtils.getDefaultEqualityComparator());
		}

		// TODO: generate instead of delegate
		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator,
						K key, int keyHash, int shift) {
			return removed(mutator, key, keyHash, shift,
							EqualityUtils.getDefaultEqualityComparator());
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

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return this;
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			// TODO Auto-generated method stub
			return null;
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
			TrieMap<?, ?> that = (TrieMap<?, ?>) other;

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
	 * For analysis purposes only. Payload X Node
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

	private static final class BitmapIndexedMapNode<K, V> extends CompactMapNode<K, V> {
		private AtomicReference<Thread> mutator;

		private Object[] nodes;
		// final private int bitmap;
		// final private int valmap;
		final private byte payloadArity;

		BitmapIndexedMapNode(AtomicReference<Thread> mutator, int bitmap, int valmap, Object[] nodes,
						byte payloadArity) {
			super(mutator, bitmap, valmap);

			assert (2 * Integer.bitCount(valmap) + Integer.bitCount(bitmap ^ valmap) == nodes.length);

			this.mutator = mutator;

			this.nodes = nodes;
			// this.bitmap = bitmap;
			// this.valmap = valmap;
			this.payloadArity = payloadArity;

			assert (payloadArity == Integer.bitCount(valmap));
			// assert (payloadArity() >= 2 || nodeArity() >= 1); // =
			// // SIZE_MORE_THAN_ONE

			// for (int i = 0; i < 2 * payloadArity; i++)
			// assert ((nodes[i] instanceof CompactNode) == false);
			//
			// for (int i = 2 * payloadArity; i < nodes.length; i++)
			// assert ((nodes[i] instanceof CompactNode) == true);

			// assert invariant
			assert nodeInvariant();
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
		public CompactMapNode<K, V> getNode(int index) {
			final int offset = 2 * payloadArity;
			return (CompactMapNode<K, V>) nodes[offset + index];
		}

		@Override
		SupplierIterator<K, V> payloadIterator() {
			return ArrayKeyValueIterator.of(nodes, 0, 2 * payloadArity);
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
				final byte pos = recoverMask(valmap, (byte) (i + 1));
				bldr.append(String.format("@%d: %s=%s", pos, getKey(i), getValue(i)));

				if (!((i + 1) == payloadArity())) {
					bldr.append(", ");
				}
			}

			if (payloadArity() > 0 && nodeArity() > 0) {
				bldr.append(", ");
			}

			for (byte i = 0; i < nodeArity(); i++) {
				final byte pos = recoverMask(bitmap ^ valmap, (byte) (i + 1));
				bldr.append(String.format("@%d: %s", pos, getNode(i)));

				if (!((i + 1) == nodeArity())) {
					bldr.append(", ");
				}
			}

			bldr.append(']');
			return bldr.toString();
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return this;
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			final CompactMapNode<K, V> thisNew;
			final int valIndex = 2 * index;

			if (isAllowedToEdit(this.mutator, mutator)) {
				// no copying if already editable
				this.nodes[valIndex + 1] = val;
				thisNew = this;
			} else {
				final Object[] editableNodes = copyAndSet(this.nodes, valIndex + 1, val);

				thisNew = CompactMapNode.<K, V> valNodeOf(mutator, bitmap, valmap, editableNodes,
								payloadArity);
			}

			return thisNew;
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = 2 * Integer.bitCount(valmap & (bitpos - 1));
			final Object[] editableNodes = copyAndInsertPair(this.nodes, valIndex, key, val);

			final CompactMapNode<K, V> thisNew = CompactMapNode.<K, V> valNodeOf(mutator, bitmap
							| bitpos, valmap | bitpos, editableNodes, (byte) (payloadArity + 1));

			return thisNew;
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = 2 * Integer.bitCount(valmap & (bitpos - 1));
			final Object[] editableNodes = copyAndRemovePair(this.nodes, valIndex);

			final CompactMapNode<K, V> thisNew = CompactMapNode.<K, V> valNodeOf(mutator, this.bitmap
							& ~bitpos, this.valmap & ~bitpos, editableNodes, (byte) (payloadArity - 1));

			return thisNew;
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			final int bitIndex = 2 * payloadArity + index;
			final CompactMapNode<K, V> thisNew;

			// modify current node (set replacement node)
			if (isAllowedToEdit(this.mutator, mutator)) {
				// no copying if already editable
				this.nodes[bitIndex] = node;
				thisNew = this;
			} else {
				final Object[] editableNodes = copyAndSet(this.nodes, bitIndex, node);

				thisNew = CompactMapNode.<K, V> valNodeOf(mutator, bitmap, valmap, editableNodes,
								payloadArity);
			}

			return thisNew;
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = 2 * payloadArity + Integer.bitCount((bitmap ^ valmap) & (bitpos - 1));
			final Object[] editableNodes = copyAndRemovePair(this.nodes, bitIndex);

			final CompactMapNode<K, V> thisNew = CompactMapNode.<K, V> valNodeOf(mutator, bitmap
							& ~bitpos, valmap, editableNodes, payloadArity);

			return thisNew;
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			// final int bitIndex = 2 * payloadArity + Integer.bitCount((bitmap ^
			// valmap) & (bitpos - 1));
			final int valIndex = 2 * Integer.bitCount(valmap & (bitpos - 1));

			final int offset = 2 * (payloadArity - 1);
			final int index = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos)) & (bitpos - 1));

			final Object[] editableNodes = copyAndMoveToBackPair(this.nodes, valIndex, offset + index,
							node);

			final CompactMapNode<K, V> thisNew = CompactMapNode.<K, V> valNodeOf(mutator, bitmap
							| bitpos, valmap & ~bitpos, editableNodes, (byte) (payloadArity - 1));

			return thisNew;
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = 2 * payloadArity + Integer.bitCount((bitmap ^ valmap) & (bitpos - 1));
			final int valIndexNew = Integer.bitCount((valmap | bitpos) & (bitpos - 1));

			final Object[] editableNodes = copyAndMoveToFrontPair(this.nodes, bitIndex, valIndexNew,
							node.headKey(), node.headVal());

			final CompactMapNode<K, V> thisNew = CompactMapNode.<K, V> valNodeOf(mutator, bitmap,
							valmap | bitpos, editableNodes, (byte) (payloadArity + 1));

			return thisNew;
		}
	}

	private static abstract class CompactMapNode<K, V> extends AbstractMapNode<K, V> {

		protected static final int BIT_PARTITION_SIZE = 5;
		protected static final int BIT_PARTITION_MASK = 0x1f;

		protected final int bitmap;
		protected final int valmap;

		CompactMapNode(final AtomicReference<Thread> mutator, final int bitmap, final int valmap) {
			super();
			this.bitmap = bitmap;
			this.valmap = valmap;
		}

		static final byte SIZE_EMPTY = 0b00;
		static final byte SIZE_ONE = 0b01;
		static final byte SIZE_MORE_THAN_ONE = 0b10;

		abstract CompactMapNode<K, V> convertToGenericNode();

		/**
		 * Abstract predicate over a node's size. Value can be either
		 * {@value #SIZE_EMPTY}, {@value #SIZE_ONE}, or {@value #SIZE_MORE_THAN_ONE}
		 * .
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
		abstract CompactMapNode<K, V> getNode(int index);

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

		abstract CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val);

		abstract CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos,
						K key, V val);

		abstract CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos);

		abstract CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node);

		abstract CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos);

		abstract CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node);

		abstract CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node);

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
				final int valmap = 1 << mask0 | 1 << mask1;

				if (mask0 < mask1) {
					return valNodeOf(null, valmap, valmap, key0, val0, key1, val1);
				} else {
					return valNodeOf(null, valmap, valmap, key1, val1, key0, val0);
				}
			} else {
				// values fit on next level
				final CompactMapNode<K, V> node = mergeNodes(key0, keyHash0, val0, key1, keyHash1,
								val1, shift + BIT_PARTITION_SIZE);

				final int bitmap = 1 << mask0;
				return valNodeOf(null, bitmap, 0, node);
			}
		}

		static final <K, V> CompactMapNode<K, V> mergeNodes(CompactMapNode<K, V> node0, int keyHash0,
						K key1, int keyHash1, V val1, int shift) {
			final int mask0 = (keyHash0 >>> shift) & BIT_PARTITION_MASK;
			final int mask1 = (keyHash1 >>> shift) & BIT_PARTITION_MASK;

			if (mask0 != mask1) {
				// both nodes fit on same level
				final int bitmap = 1 << mask0 | 1 << mask1;
				final int valmap = 1 << mask1;

				// store values before node
				return valNodeOf(null, bitmap, valmap, key1, val1, node0);
			} else {
				// values fit on next level
				final CompactMapNode<K, V> node = mergeNodes(node0, keyHash0, key1, keyHash1, val1,
								shift + BIT_PARTITION_SIZE);

				final int bitmap = 1 << mask0;
				return valNodeOf(null, bitmap, 0, node);
			}
		}

		static final CompactMapNode EMPTY_INPLACE_INDEX_NODE;

		static {
			EMPTY_INPLACE_INDEX_NODE = new Map0To0Node<>(null, 0, 0);
		};

		static final <K, V> CompactMapNode<K, V> valNodeOf(AtomicReference<Thread> mutator, int bitmap,
						int valmap, Object[] nodes, byte payloadArity) {
			return new BitmapIndexedMapNode<>(mutator, bitmap, valmap, nodes, payloadArity);
		}

		// TODO: consolidate and remove
		static final <K, V> CompactMapNode<K, V> valNodeOf(AtomicReference<Thread> mutator) {
			return valNodeOf(mutator, 0, 0);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap) {
			return new Map0To0Node<>(mutator, bitmap, valmap);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final CompactMapNode<K, V> node1) {
			return new Map0To1Node<>(mutator, bitmap, valmap, node1);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2) {
			return new Map0To2Node<>(mutator, bitmap, valmap, node1, node2);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3) {
			return new Map0To3Node<>(mutator, bitmap, valmap, node1, node2, node3);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3,
						final CompactMapNode<K, V> node4) {
			return new Map0To4Node<>(mutator, bitmap, valmap, node1, node2, node3, node4);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3,
						final CompactMapNode<K, V> node4, final CompactMapNode<K, V> node5) {
			return new Map0To5Node<>(mutator, bitmap, valmap, node1, node2, node3, node4, node5);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3,
						final CompactMapNode<K, V> node4, final CompactMapNode<K, V> node5,
						final CompactMapNode<K, V> node6) {
			return new Map0To6Node<>(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node6);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3,
						final CompactMapNode<K, V> node4, final CompactMapNode<K, V> node5,
						final CompactMapNode<K, V> node6, final CompactMapNode<K, V> node7) {
			return new Map0To7Node<>(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node6,
							node7);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3,
						final CompactMapNode<K, V> node4, final CompactMapNode<K, V> node5,
						final CompactMapNode<K, V> node6, final CompactMapNode<K, V> node7,
						final CompactMapNode<K, V> node8) {
			return new Map0To8Node<>(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node6,
							node7, node8);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1) {
			return new Map1To0Node<>(mutator, bitmap, valmap, key1, val1);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1,
						final CompactMapNode<K, V> node1) {
			return new Map1To1Node<>(mutator, bitmap, valmap, key1, val1, node1);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2) {
			return new Map1To2Node<>(mutator, bitmap, valmap, key1, val1, node1, node2);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3) {
			return new Map1To3Node<>(mutator, bitmap, valmap, key1, val1, node1, node2, node3);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3, final CompactMapNode<K, V> node4) {
			return new Map1To4Node<>(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3, final CompactMapNode<K, V> node4,
						final CompactMapNode<K, V> node5) {
			return new Map1To5Node<>(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4,
							node5);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3, final CompactMapNode<K, V> node4,
						final CompactMapNode<K, V> node5, final CompactMapNode<K, V> node6) {
			return new Map1To6Node<>(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4,
							node5, node6);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3, final CompactMapNode<K, V> node4,
						final CompactMapNode<K, V> node5, final CompactMapNode<K, V> node6,
						final CompactMapNode<K, V> node7) {
			return new Map1To7Node<>(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4,
							node5, node6, node7);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3, final CompactMapNode<K, V> node4,
						final CompactMapNode<K, V> node5, final CompactMapNode<K, V> node6,
						final CompactMapNode<K, V> node7, final CompactMapNode<K, V> node8) {
			return valNodeOf(mutator, bitmap, valmap, new Object[] { key1, val1, node1, node2, node3,
							node4, node5, node6, node7, node8 }, (byte) 1);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2) {
			return new Map2To0Node<>(mutator, bitmap, valmap, key1, val1, key2, val2);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final CompactMapNode<K, V> node1) {
			return new Map2To1Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, node1);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2) {
			return new Map2To2Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3) {
			return new Map2To3Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2,
							node3);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3,
						final CompactMapNode<K, V> node4) {
			return new Map2To4Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2,
							node3, node4);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3,
						final CompactMapNode<K, V> node4, final CompactMapNode<K, V> node5) {
			return new Map2To5Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2,
							node3, node4, node5);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3,
						final CompactMapNode<K, V> node4, final CompactMapNode<K, V> node5,
						final CompactMapNode<K, V> node6) {
			return new Map2To6Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2,
							node3, node4, node5, node6);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3,
						final CompactMapNode<K, V> node4, final CompactMapNode<K, V> node5,
						final CompactMapNode<K, V> node6, final CompactMapNode<K, V> node7) {
			return valNodeOf(mutator, bitmap, valmap, new Object[] { key1, val1, key2, val2, node1,
							node2, node3, node4, node5, node6, node7 }, (byte) 2);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3) {
			return new Map3To0Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final CompactMapNode<K, V> node1) {
			return new Map3To1Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2) {
			return new Map3To2Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3,
							node1, node2);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3) {
			return new Map3To3Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3,
							node1, node2, node3);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3,
						final CompactMapNode<K, V> node4) {
			return new Map3To4Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3,
							node1, node2, node3, node4);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3,
						final CompactMapNode<K, V> node4, final CompactMapNode<K, V> node5) {
			return new Map3To5Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3,
							node1, node2, node3, node4, node5);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3,
						final CompactMapNode<K, V> node4, final CompactMapNode<K, V> node5,
						final CompactMapNode<K, V> node6) {
			return valNodeOf(mutator, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3,
							val3, node1, node2, node3, node4, node5, node6 }, (byte) 3);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4) {
			return new Map4To0Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
							val4);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4,
						final CompactMapNode<K, V> node1) {
			return new Map4To1Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
							val4, node1);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2) {
			return new Map4To2Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
							val4, node1, node2);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3) {
			return new Map4To3Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
							val4, node1, node2, node3);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3, final CompactMapNode<K, V> node4) {
			return new Map4To4Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
							val4, node1, node2, node3, node4);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3, final CompactMapNode<K, V> node4,
						final CompactMapNode<K, V> node5) {
			return valNodeOf(mutator, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3,
							val3, key4, val4, node1, node2, node3, node4, node5 }, (byte) 4);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4,
						final K key5, final V val5) {
			return new Map5To0Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
							val4, key5, val5);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4,
						final K key5, final V val5, final CompactMapNode<K, V> node1) {
			return new Map5To1Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
							val4, key5, val5, node1);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4,
						final K key5, final V val5, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2) {
			return new Map5To2Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
							val4, key5, val5, node1, node2);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4,
						final K key5, final V val5, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3) {
			return new Map5To3Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
							val4, key5, val5, node1, node2, node3);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4,
						final K key5, final V val5, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3,
						final CompactMapNode<K, V> node4) {
			return valNodeOf(mutator, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3,
							val3, key4, val4, key5, val5, node1, node2, node3, node4 }, (byte) 5);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4,
						final K key5, final V val5, final K key6, final V val6) {
			return new Map6To0Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
							val4, key5, val5, key6, val6);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4,
						final K key5, final V val5, final K key6, final V val6,
						final CompactMapNode<K, V> node1) {
			return new Map6To1Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
							val4, key5, val5, key6, val6, node1);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4,
						final K key5, final V val5, final K key6, final V val6,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2) {
			return new Map6To2Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
							val4, key5, val5, key6, val6, node1, node2);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4,
						final K key5, final V val5, final K key6, final V val6,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3) {
			return valNodeOf(mutator, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3,
							val3, key4, val4, key5, val5, key6, val6, node1, node2, node3 }, (byte) 6);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4,
						final K key5, final V val5, final K key6, final V val6, final K key7,
						final V val7) {
			return new Map7To0Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
							val4, key5, val5, key6, val6, key7, val7);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4,
						final K key5, final V val5, final K key6, final V val6, final K key7,
						final V val7, final CompactMapNode<K, V> node1) {
			return new Map7To1Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
							val4, key5, val5, key6, val6, key7, val7, node1);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4,
						final K key5, final V val5, final K key6, final V val6, final K key7,
						final V val7, final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2) {
			return valNodeOf(mutator, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3,
							val3, key4, val4, key5, val5, key6, val6, key7, val7, node1, node2 },
							(byte) 7);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4,
						final K key5, final V val5, final K key6, final V val6, final K key7,
						final V val7, final K key8, final V val8) {
			return new Map8To0Node<>(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
							val4, key5, val5, key6, val6, key7, val7, key8, val8);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4,
						final K key5, final V val5, final K key6, final V val6, final K key7,
						final V val7, final K key8, final V val8, final CompactMapNode<K, V> node1) {
			return valNodeOf(mutator, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3,
							val3, key4, val4, key5, val5, key6, val6, key7, val7, key8, val8, node1 },
							(byte) 8);
		}

		static final <K, V> CompactMapNode<K, V> valNodeOf(final AtomicReference<Thread> mutator,
						final int bitmap, final int valmap, final K key1, final V val1, final K key2,
						final V val2, final K key3, final V val3, final K key4, final V val4,
						final K key5, final V val5, final K key6, final V val6, final K key7,
						final V val7, final K key8, final V val8, final K key9, final V val9) {
			return valNodeOf(mutator, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3,
							val3, key4, val4, key5, val5, key6, val6, key7, val7, key8, val8, key9,
							val9 }, (byte) 9);
		}

		final int keyIndex(int bitpos) {
			return Integer.bitCount(valmap & (bitpos - 1));
		}

		final int valIndex(int bitpos) {
			return Integer.bitCount(valmap & (bitpos - 1));
		}

		// TODO: obviate necessity for bitmap ^ valmap
		final int nodeIndex(int bitpos) {
			return Integer.bitCount((bitmap ^ valmap) & (bitpos - 1));
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) {
				return getKey(keyIndex(bitpos)).equals(key);
			}

			if ((bitmap & bitpos) != 0) {
				return getNode(nodeIndex(bitpos)).containsKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			}

			return false;
		}

		@Override
		boolean containsKey(Object key, int keyHash, int shift, Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) {
				return cmp.compare(getKey(keyIndex(bitpos)), key) == 0;
			}

			if ((bitmap & bitpos) != 0) {
				return getNode(nodeIndex(bitpos)).containsKey(key, keyHash, shift + BIT_PARTITION_SIZE,
								cmp);
			}

			return false;
		}

		@Override
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				// final int valIndex = valIndex(bitpos);

				if (getKey(keyIndex(bitpos)).equals(key)) {
					final K _key = getKey(keyIndex(bitpos));
					final V _val = getValue(valIndex(bitpos));

					final Map.Entry<K, V> entry = entryOf(_key, _val);
					return Optional.of(entry);
				}

				return Optional.empty();
			}

			if ((bitmap & bitpos) != 0) { // node (not value)
				final AbstractMapNode<K, V> subNode = getNode(nodeIndex(bitpos));

				return subNode.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
			}

			return Optional.empty();
		}

		@Override
		Optional<java.util.Map.Entry<K, V>> findByKey(Object key, int keyHash, int shift,
						Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				// final int valIndex = valIndex(bitpos);

				if (cmp.compare(getKey(keyIndex(bitpos)), key) == 0) {
					final K _key = getKey(keyIndex(bitpos));
					final V _val = getValue(valIndex(bitpos));

					final Map.Entry<K, V> entry = entryOf(_key, _val);
					return Optional.of(entry);
				}

				return Optional.empty();
			}

			if ((bitmap & bitpos) != 0) { // node (not value)
				final AbstractMapNode<K, V> subNode = getNode(nodeIndex(bitpos));

				return subNode.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE, cmp);
			}

			return Optional.empty();
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final K currentKey = getKey(keyIndex(bitpos));

				if (currentKey.equals(key)) {
					final V currentVal = getValue(valIndex(bitpos));

					if (currentVal.equals(val)) {
						return Result.unchanged(this);
					}

					// update mapping
					final CompactMapNode<K, V> thisNew = copyAndSetValue(mutator, valIndex(bitpos), val);

					return Result.updated(thisNew, currentVal);
				} else {
					final CompactMapNode<K, V> nodeNew = mergeNodes(getKey(keyIndex(bitpos)),
									getKey(keyIndex(bitpos)).hashCode(), getValue(valIndex(bitpos)),
									key, keyHash, val, shift + BIT_PARTITION_SIZE);

					final CompactMapNode<K, V> thisNew = copyAndMigrateFromInlineToNode(mutator,
									bitpos, nodeNew);

					return Result.modified(thisNew);
				}
			} else if ((bitmap & bitpos) != 0) { // node (not value)
				final CompactMapNode<K, V> subNode = getNode(nodeIndex(bitpos));

				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = subNode.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE);

				if (!nestedResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactMapNode<K, V> thisNew = copyAndSetNode(mutator, nodeIndex(bitpos),
								nestedResult.getNode());

				if (nestedResult.hasReplacedValue()) {
					return Result.updated(thisNew, nestedResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else {
				// no value
				final CompactMapNode<K, V> thisNew = copyAndInsertValue(mutator, bitpos, key, val);

				return Result.modified(thisNew);
			}
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> updated(AtomicReference<Thread> mutator, K key,
						int keyHash, V val, int shift, Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final K currentKey = getKey(keyIndex(bitpos));

				if (cmp.compare(currentKey, key) == 0) {
					final V currentVal = getValue(valIndex(bitpos));

					if (cmp.compare(currentVal, val) == 0) {
						return Result.unchanged(this);
					}

					// update mapping
					final CompactMapNode<K, V> thisNew = copyAndSetValue(mutator, valIndex(bitpos), val);

					return Result.updated(thisNew, currentVal);
				} else {
					final CompactMapNode<K, V> nodeNew = mergeNodes(getKey(keyIndex(bitpos)),
									getKey(keyIndex(bitpos)).hashCode(), getValue(valIndex(bitpos)),
									key, keyHash, val, shift + BIT_PARTITION_SIZE);

					final CompactMapNode<K, V> thisNew = copyAndMigrateFromInlineToNode(mutator,
									bitpos, nodeNew);

					return Result.modified(thisNew);
				}
			} else if ((bitmap & bitpos) != 0) { // node (not value)
				final CompactMapNode<K, V> subNode = getNode(nodeIndex(bitpos));

				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = subNode.updated(
								mutator, key, keyHash, val, shift + BIT_PARTITION_SIZE, cmp);

				if (!nestedResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactMapNode<K, V> thisNew = copyAndSetNode(mutator, nodeIndex(bitpos),
								nestedResult.getNode());

				if (nestedResult.hasReplacedValue()) {
					return Result.updated(thisNew, nestedResult.getReplacedValue());
				}

				return Result.modified(thisNew);
			} else {
				// no value
				final CompactMapNode<K, V> thisNew = copyAndInsertValue(mutator, bitpos, key, val);

				return Result.modified(thisNew);
			}
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (getKey(valIndex).equals(key)) {
					if (this.arity() == 9) {
						final CompactMapNode<K, V> thisNew = copyAndRemoveValue(mutator, bitpos)
										.convertToGenericNode();

						return Result.modified(thisNew);
					} else {
						final CompactMapNode<K, V> thisNew = copyAndRemoveValue(mutator, bitpos);

						return Result.modified(thisNew);
					}
				} else {
					return Result.unchanged(this);
				}
			} else if ((bitmap & bitpos) != 0) { // node (not value)
				final CompactMapNode<K, V> subNode = getNode(nodeIndex(bitpos));
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = subNode.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE);

				if (!nestedResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactMapNode<K, V> subNodeNew = nestedResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case 0: {
					if (this.arity() == 9) {
						// remove node and convert
						final CompactMapNode<K, V> thisNew = copyAndRemoveNode(mutator, bitpos)
										.convertToGenericNode();

						return Result.modified(thisNew);
					} else {
						// remove node
						final CompactMapNode<K, V> thisNew = copyAndRemoveNode(mutator, bitpos);

						return Result.modified(thisNew);
					}
				}
				case 1: {
					// inline value (move to front)
					final CompactMapNode<K, V> thisNew = copyAndMigrateFromNodeToInline(mutator,
									bitpos, subNodeNew);

					return Result.modified(thisNew);
				}
				default: {
					// modify current node (set replacement node)
					final CompactMapNode<K, V> thisNew = copyAndSetNode(mutator, bitpos, subNodeNew);

					return Result.modified(thisNew);
				}
				}
			}

			return Result.unchanged(this);
		}

		@Override
		Result<K, V, ? extends CompactMapNode<K, V>> removed(AtomicReference<Thread> mutator, K key,
						int keyHash, int shift, Comparator<Object> cmp) {
			final int mask = (keyHash >>> shift) & BIT_PARTITION_MASK;
			final int bitpos = (1 << mask);

			if ((valmap & bitpos) != 0) { // inplace value
				final int valIndex = valIndex(bitpos);

				if (cmp.compare(getKey(valIndex), key) == 0) {
					if (this.arity() == 9) {
						final CompactMapNode<K, V> thisNew = copyAndRemoveValue(mutator, bitpos)
										.convertToGenericNode();

						return Result.modified(thisNew);
					} else {
						final CompactMapNode<K, V> thisNew = copyAndRemoveValue(mutator, bitpos);

						return Result.modified(thisNew);
					}
				} else {
					return Result.unchanged(this);
				}
			} else if ((bitmap & bitpos) != 0) { // node (not value)
				final CompactMapNode<K, V> subNode = getNode(nodeIndex(bitpos));
				final Result<K, V, ? extends CompactMapNode<K, V>> nestedResult = subNode.removed(
								mutator, key, keyHash, shift + BIT_PARTITION_SIZE, cmp);

				if (!nestedResult.isModified()) {
					return Result.unchanged(this);
				}

				final CompactMapNode<K, V> subNodeNew = nestedResult.getNode();

				switch (subNodeNew.sizePredicate()) {
				case 0: {
					if (this.arity() == 9) {
						// remove node and convert
						final CompactMapNode<K, V> thisNew = copyAndRemoveNode(mutator, bitpos)
										.convertToGenericNode();

						return Result.modified(thisNew);
					} else {
						// remove node
						final CompactMapNode<K, V> thisNew = copyAndRemoveNode(mutator, bitpos);

						return Result.modified(thisNew);
					}
				}
				case 1: {
					// inline value (move to front)
					final CompactMapNode<K, V> thisNew = copyAndMigrateFromNodeToInline(mutator,
									bitpos, subNodeNew);

					return Result.modified(thisNew);
				}
				default: {
					// modify current node (set replacement node)
					final CompactMapNode<K, V> thisNew = copyAndSetNode(mutator, bitpos, subNodeNew);

					return Result.modified(thisNew);
				}
				}
			}

			return Result.unchanged(this);
		}

	}

	private static final class Map0To0Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;

		Map0To0Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return Collections.<CompactMapNode<K, V>> emptyIterator();
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] {}, (byte) 0);
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

	private static final class Map0To1Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final CompactMapNode<K, V> node1;

		Map0To1Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final CompactMapNode<K, V> node1) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.node1 = node1;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1);
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
		K headKey() {
			throw new UnsupportedOperationException("Node does not directly contain a key.");
		}

		@Override
		V headVal() {
			throw new UnsupportedOperationException("Node does not directly contain a value.");
		}

		@Override
		CompactMapNode<K, V> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { node1 }, (byte) 0);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

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
			Map0To1Node<?, ?> that = (Map0To1Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!node1.equals(that.node1)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s]", recoverMask(bitmap ^ valmap, (byte) 1), node1);
		}

	}

	private static final class Map0To2Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;

		Map0To2Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.node1 = node1;
			this.node2 = node2;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2);
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, node1, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node2);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { node1, node2 }, (byte) 0);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + node1.hashCode();

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

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
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
			return String.format("[@%d: %s, @%d: %s]", recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2);
		}

	}

	private static final class Map0To3Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;

		Map0To3Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3);
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node, node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, node1, node, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, node1, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node2, node3);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node3);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { node1, node2, node3 }, (byte) 0);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

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

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
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
			return String.format("[@%d: %s, @%d: %s, @%d: %s]", recoverMask(bitmap ^ valmap, (byte) 1),
							node1, recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3);
		}

	}

	private static final class Map0To4Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;
		private final CompactMapNode<K, V> node4;

		Map0To4Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3, final CompactMapNode<K, V> node4) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;
			this.node4 = node4;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3, node4);
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3, node4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node, node2, node3, node4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, node1, node, node3, node4);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node, node4);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node2, node3, node4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, node1, node3, node4);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node4);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node2, node3, node4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node3, node4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { node1, node2, node3, node4 },
							(byte) 0);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

			result = prime * result + node3.hashCode();

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

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
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
			return String.format("[@%d: %s, @%d: %s, @%d: %s, @%d: %s]",
							recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3,
							recoverMask(bitmap ^ valmap, (byte) 4), node4);
		}

	}

	private static final class Map0To5Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;
		private final CompactMapNode<K, V> node4;
		private final CompactMapNode<K, V> node5;

		Map0To5Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3, final CompactMapNode<K, V> node4,
						final CompactMapNode<K, V> node5) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;
			this.node4 = node4;
			this.node5 = node5;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3, node4, node5);
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 5;
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
		CompactMapNode<K, V> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;
			case 2:
				return node3;
			case 3:
				return node4;
			case 4:
				return node5;
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3, node4, node5);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node, node2, node3, node4, node5);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, node1, node, node3, node4, node5);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node, node4, node5);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node, node5);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node2, node3, node4, node5);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, node1, node3, node4, node5);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node4, node5);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node5);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node2, node3, node4, node5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node3, node4, node5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node4, node5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3, node5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3, node4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { node1, node2, node3, node4, node5 },
							(byte) 0);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

			result = prime * result + node3.hashCode();

			result = prime * result + node4.hashCode();

			result = prime * result + node5.hashCode();

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
			Map0To5Node<?, ?> that = (Map0To5Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
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
			if (!node5.equals(that.node5)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s, @%d: %s, @%d: %s, @%d: %s, @%d: %s]",
							recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3,
							recoverMask(bitmap ^ valmap, (byte) 4), node4,
							recoverMask(bitmap ^ valmap, (byte) 5), node5);
		}

	}

	private static final class Map0To6Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;
		private final CompactMapNode<K, V> node4;
		private final CompactMapNode<K, V> node5;
		private final CompactMapNode<K, V> node6;

		Map0To6Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3, final CompactMapNode<K, V> node4,
						final CompactMapNode<K, V> node5, final CompactMapNode<K, V> node6) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;
			this.node4 = node4;
			this.node5 = node5;
			this.node6 = node6;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3, node4, node5, node6);
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 6;
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
		CompactMapNode<K, V> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;
			case 2:
				return node3;
			case 3:
				return node4;
			case 4:
				return node5;
			case 5:
				return node6;
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3, node4, node5,
								node6);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node, node2, node3, node4, node5, node6);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, node1, node, node3, node4, node5, node6);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node, node4, node5, node6);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node, node5, node6);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node, node6);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node2, node3, node4, node5, node6);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, node1, node3, node4, node5, node6);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node4, node5, node6);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node5, node6);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node6);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node2, node3, node4, node5,
									node6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node3, node4, node5,
									node6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node4, node5,
									node6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3, node5,
									node6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3, node4,
									node6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3, node4,
									node5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { node1, node2, node3, node4, node5,
							node6 }, (byte) 0);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

			result = prime * result + node3.hashCode();

			result = prime * result + node4.hashCode();

			result = prime * result + node5.hashCode();

			result = prime * result + node6.hashCode();

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
			Map0To6Node<?, ?> that = (Map0To6Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
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
			if (!node5.equals(that.node5)) {
				return false;
			}
			if (!node6.equals(that.node6)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s, @%d: %s, @%d: %s, @%d: %s, @%d: %s, @%d: %s]",
							recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3,
							recoverMask(bitmap ^ valmap, (byte) 4), node4,
							recoverMask(bitmap ^ valmap, (byte) 5), node5,
							recoverMask(bitmap ^ valmap, (byte) 6), node6);
		}

	}

	private static final class Map0To7Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;
		private final CompactMapNode<K, V> node4;
		private final CompactMapNode<K, V> node5;
		private final CompactMapNode<K, V> node6;
		private final CompactMapNode<K, V> node7;

		Map0To7Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3, final CompactMapNode<K, V> node4,
						final CompactMapNode<K, V> node5, final CompactMapNode<K, V> node6,
						final CompactMapNode<K, V> node7) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;
			this.node4 = node4;
			this.node5 = node5;
			this.node6 = node6;
			this.node7 = node7;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3, node4, node5, node6, node7);
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 7;
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
		CompactMapNode<K, V> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;
			case 2:
				return node3;
			case 3:
				return node4;
			case 4:
				return node5;
			case 5:
				return node6;
			case 6:
				return node7;
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3, node4, node5,
								node6, node7);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node, node2, node3, node4, node5, node6,
								node7);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, node1, node, node3, node4, node5, node6,
								node7);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node, node4, node5, node6,
								node7);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node, node5, node6,
								node7);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node, node6,
								node7);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node,
								node7);
			case 6:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node6,
								node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node2, node3, node4, node5, node6, node7);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, node1, node3, node4, node5, node6, node7);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node4, node5, node6, node7);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node5, node6, node7);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node6, node7);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node7);
			case 6:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node6);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node2, node3, node4, node5,
									node6, node7);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node3, node4, node5,
									node6, node7);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node4, node5,
									node6, node7);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3, node5,
									node6, node7);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3, node4,
									node6, node7);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3, node4,
									node5, node7);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 6:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3, node4,
									node5, node6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { node1, node2, node3, node4, node5,
							node6, node7 }, (byte) 0);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

			result = prime * result + node3.hashCode();

			result = prime * result + node4.hashCode();

			result = prime * result + node5.hashCode();

			result = prime * result + node6.hashCode();

			result = prime * result + node7.hashCode();

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
			Map0To7Node<?, ?> that = (Map0To7Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
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
			if (!node5.equals(that.node5)) {
				return false;
			}
			if (!node6.equals(that.node6)) {
				return false;
			}
			if (!node7.equals(that.node7)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s, @%d: %s, @%d: %s, @%d: %s, @%d: %s, @%d: %s, @%d: %s]",
							recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3,
							recoverMask(bitmap ^ valmap, (byte) 4), node4,
							recoverMask(bitmap ^ valmap, (byte) 5), node5,
							recoverMask(bitmap ^ valmap, (byte) 6), node6,
							recoverMask(bitmap ^ valmap, (byte) 7), node7);
		}

	}

	private static final class Map0To8Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;
		private final CompactMapNode<K, V> node4;
		private final CompactMapNode<K, V> node5;
		private final CompactMapNode<K, V> node6;
		private final CompactMapNode<K, V> node7;
		private final CompactMapNode<K, V> node8;

		Map0To8Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3, final CompactMapNode<K, V> node4,
						final CompactMapNode<K, V> node5, final CompactMapNode<K, V> node6,
						final CompactMapNode<K, V> node7, final CompactMapNode<K, V> node8) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;
			this.node4 = node4;
			this.node5 = node5;
			this.node6 = node6;
			this.node7 = node7;
			this.node8 = node8;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3, node4, node5, node6, node7, node8);
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 8;
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
		CompactMapNode<K, V> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;
			case 2:
				return node3;
			case 3:
				return node4;
			case 4:
				return node5;
			case 5:
				return node6;
			case 6:
				return node7;
			case 7:
				return node8;
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3, node4, node5,
								node6, node7, node8);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node, node2, node3, node4, node5, node6,
								node7, node8);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, node1, node, node3, node4, node5, node6,
								node7, node8);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node, node4, node5, node6,
								node7, node8);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node, node5, node6,
								node7, node8);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node, node6,
								node7, node8);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node,
								node7, node8);
			case 6:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node6,
								node, node8);
			case 7:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node6,
								node7, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node2, node3, node4, node5, node6, node7,
								node8);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, node1, node3, node4, node5, node6, node7,
								node8);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node4, node5, node6, node7,
								node8);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node5, node6, node7,
								node8);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node6, node7,
								node8);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node7,
								node8);
			case 6:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node6,
								node8);
			case 7:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node6,
								node7);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node2, node3, node4, node5,
									node6, node7, node8);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node3, node4, node5,
									node6, node7, node8);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node4, node5,
									node6, node7, node8);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3, node5,
									node6, node7, node8);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3, node4,
									node6, node7, node8);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3, node4,
									node5, node7, node8);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 6:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3, node4,
									node5, node6, node8);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 7:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, node1, node2, node3, node4,
									node5, node6, node7);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { node1, node2, node3, node4, node5,
							node6, node7, node8 }, (byte) 0);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

			result = prime * result + node3.hashCode();

			result = prime * result + node4.hashCode();

			result = prime * result + node5.hashCode();

			result = prime * result + node6.hashCode();

			result = prime * result + node7.hashCode();

			result = prime * result + node8.hashCode();

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
			Map0To8Node<?, ?> that = (Map0To8Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
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
			if (!node5.equals(that.node5)) {
				return false;
			}
			if (!node6.equals(that.node6)) {
				return false;
			}
			if (!node7.equals(that.node7)) {
				return false;
			}
			if (!node8.equals(that.node8)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format(
							"[@%d: %s, @%d: %s, @%d: %s, @%d: %s, @%d: %s, @%d: %s, @%d: %s, @%d: %s]",
							recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3,
							recoverMask(bitmap ^ valmap, (byte) 4), node4,
							recoverMask(bitmap ^ valmap, (byte) 5), node5,
							recoverMask(bitmap ^ valmap, (byte) 6), node6,
							recoverMask(bitmap ^ valmap, (byte) 7), node7,
							recoverMask(bitmap ^ valmap, (byte) 8), node8);
		}

	}

	private static final class Map1To0Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;

		Map1To0Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return Collections.<CompactMapNode<K, V>> emptyIterator();
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1 });
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1 }, (byte) 1);
		}

		@Override
		byte sizePredicate() {
			return SIZE_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

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

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
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
			return String.format("[@%d: %s=%s]", recoverMask(valmap, (byte) 1), key1, val1);
		}

	}

	private static final class Map1To1Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final CompactMapNode<K, V> node1;

		Map1To1Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final CompactMapNode<K, V> node1) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.node1 = node1;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1 });
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, node1 }, (byte) 1);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

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

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s=%s, @%d: %s]", recoverMask(valmap, (byte) 1), key1, val1,
							recoverMask(bitmap ^ valmap, (byte) 1), node1);
		}

	}

	private static final class Map1To2Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;

		Map1To2Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.node1 = node1;
			this.node2 = node2;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1 });
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, node, node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, node1, node, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node2);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, node1, node2 }, (byte) 1);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + node1.hashCode();

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

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
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
			return String.format("[@%d: %s=%s, @%d: %s, @%d: %s]", recoverMask(valmap, (byte) 1), key1,
							val1, recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2);
		}

	}

	private static final class Map1To3Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;

		Map1To3Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1 });
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, node1, node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node, node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, node, node1, node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, node1, node, node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node2, node3);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node3);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, node1, node2, node3 },
							(byte) 1);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

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

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
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
			return String.format("[@%d: %s=%s, @%d: %s, @%d: %s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1,
							recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3);
		}

	}

	private static final class Map1To4Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;
		private final CompactMapNode<K, V> node4;

		Map1To4Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3,
						final CompactMapNode<K, V> node4) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;
			this.node4 = node4;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3, node4);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1 });
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, node1, node2, node3, node4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2, node3,
								node4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2, node3,
								node4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node, node2, node3, node4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node, node3, node4);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node, node4);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node2, node3, node4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node3, node4);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node4);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, node, node1, node2, node3, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, node1, node, node2, node3, node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node, node3, node4);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node, node4);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node2, node3, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node2, node3, node4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node3, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node3, node4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2, node4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2, node3);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, node1, node2, node3,
							node4 }, (byte) 1);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

			result = prime * result + node3.hashCode();

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
			Map1To4Node<?, ?> that = (Map1To4Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
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
			return String.format("[@%d: %s=%s, @%d: %s, @%d: %s, @%d: %s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1,
							recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3,
							recoverMask(bitmap ^ valmap, (byte) 4), node4);
		}

	}

	private static final class Map1To5Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;
		private final CompactMapNode<K, V> node4;
		private final CompactMapNode<K, V> node5;

		Map1To5Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3,
						final CompactMapNode<K, V> node4, final CompactMapNode<K, V> node5) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;
			this.node4 = node4;
			this.node5 = node5;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3, node4, node5);
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 5;
		}

		@Override
		SupplierIterator<K, V> payloadIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1 });
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
		CompactMapNode<K, V> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;
			case 2:
				return node3;
			case 3:
				return node4;
			case 4:
				return node5;
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, node1, node2, node3, node4, node5);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2, node3,
								node4, node5);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2, node3,
								node4, node5);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node, node2, node3, node4, node5);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node, node3, node4, node5);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node, node4, node5);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node, node5);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node2, node3, node4, node5);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node3, node4, node5);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node4, node5);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node5);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, node, node1, node2, node3, node4, node5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, node1, node, node2, node3, node4, node5);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node, node3, node4, node5);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node, node4, node5);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node, node5);
				case 5:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node2, node3,
									node4, node5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node2, node3,
									node4, node5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node3,
									node4, node5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node3,
									node4, node5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2,
									node4, node5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2,
									node4, node5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2,
									node3, node5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2,
									node3, node5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2,
									node3, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2,
									node3, node4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, node1, node2, node3,
							node4, node5 }, (byte) 1);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

			result = prime * result + node3.hashCode();

			result = prime * result + node4.hashCode();

			result = prime * result + node5.hashCode();

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
			Map1To5Node<?, ?> that = (Map1To5Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
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
			if (!node5.equals(that.node5)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s=%s, @%d: %s, @%d: %s, @%d: %s, @%d: %s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1,
							recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3,
							recoverMask(bitmap ^ valmap, (byte) 4), node4,
							recoverMask(bitmap ^ valmap, (byte) 5), node5);
		}

	}

	private static final class Map1To6Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;
		private final CompactMapNode<K, V> node4;
		private final CompactMapNode<K, V> node5;
		private final CompactMapNode<K, V> node6;

		Map1To6Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3,
						final CompactMapNode<K, V> node4, final CompactMapNode<K, V> node5,
						final CompactMapNode<K, V> node6) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;
			this.node4 = node4;
			this.node5 = node5;
			this.node6 = node6;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3, node4, node5, node6);
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 6;
		}

		@Override
		SupplierIterator<K, V> payloadIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1 });
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
		CompactMapNode<K, V> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;
			case 2:
				return node3;
			case 3:
				return node4;
			case 4:
				return node5;
			case 5:
				return node6;
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, node1, node2, node3, node4, node5,
								node6);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2, node3,
								node4, node5, node6);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2, node3,
								node4, node5, node6);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node6);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node, node2, node3, node4, node5,
								node6);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node, node3, node4, node5,
								node6);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node, node4, node5,
								node6);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node, node5,
								node6);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4, node,
								node6);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4,
								node5, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node2, node3, node4, node5, node6);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node3, node4, node5, node6);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node4, node5, node6);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node5, node6);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4, node6);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4, node5);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, node, node1, node2, node3, node4, node5,
									node6);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, node1, node, node2, node3, node4, node5,
									node6);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node, node3, node4, node5,
									node6);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node, node4, node5,
									node6);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node, node5,
									node6);
				case 5:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node,
									node6);
				case 6:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node6,
									node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node2, node3,
									node4, node5, node6);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node2, node3,
									node4, node5, node6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node3,
									node4, node5, node6);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node3,
									node4, node5, node6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2,
									node4, node5, node6);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2,
									node4, node5, node6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2,
									node3, node5, node6);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2,
									node3, node5, node6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2,
									node3, node4, node6);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2,
									node3, node4, node6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2,
									node3, node4, node5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2,
									node3, node4, node5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, node1, node2, node3,
							node4, node5, node6 }, (byte) 1);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

			result = prime * result + node3.hashCode();

			result = prime * result + node4.hashCode();

			result = prime * result + node5.hashCode();

			result = prime * result + node6.hashCode();

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
			Map1To6Node<?, ?> that = (Map1To6Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
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
			if (!node5.equals(that.node5)) {
				return false;
			}
			if (!node6.equals(that.node6)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s=%s, @%d: %s, @%d: %s, @%d: %s, @%d: %s, @%d: %s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1,
							recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3,
							recoverMask(bitmap ^ valmap, (byte) 4), node4,
							recoverMask(bitmap ^ valmap, (byte) 5), node5,
							recoverMask(bitmap ^ valmap, (byte) 6), node6);
		}

	}

	private static final class Map1To7Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;
		private final CompactMapNode<K, V> node4;
		private final CompactMapNode<K, V> node5;
		private final CompactMapNode<K, V> node6;
		private final CompactMapNode<K, V> node7;

		Map1To7Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3,
						final CompactMapNode<K, V> node4, final CompactMapNode<K, V> node5,
						final CompactMapNode<K, V> node6, final CompactMapNode<K, V> node7) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;
			this.node4 = node4;
			this.node5 = node5;
			this.node6 = node6;
			this.node7 = node7;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3, node4, node5, node6, node7);
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 7;
		}

		@Override
		SupplierIterator<K, V> payloadIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1 });
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
		CompactMapNode<K, V> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;
			case 2:
				return node3;
			case 3:
				return node4;
			case 4:
				return node5;
			case 5:
				return node6;
			case 6:
				return node7;
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, node1, node2, node3, node4, node5,
								node6, node7);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2, node3,
								node4, node5, node6, node7);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2, node3,
								node4, node5, node6, node7);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node6,
								node7);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node, node2, node3, node4, node5,
								node6, node7);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node, node3, node4, node5,
								node6, node7);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node, node4, node5,
								node6, node7);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node, node5,
								node6, node7);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4, node,
								node6, node7);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4,
								node5, node, node7);
			case 6:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4,
								node5, node6, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node2, node3, node4, node5,
								node6, node7);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node3, node4, node5,
								node6, node7);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node4, node5,
								node6, node7);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node5,
								node6, node7);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4,
								node6, node7);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4,
								node5, node7);
			case 6:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4,
								node5, node6);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, node, node1, node2, node3, node4, node5,
									node6, node7);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, node1, node, node2, node3, node4, node5,
									node6, node7);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node, node3, node4, node5,
									node6, node7);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node, node4, node5,
									node6, node7);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node, node5,
									node6, node7);
				case 5:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node,
									node6, node7);
				case 6:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node6,
									node, node7);
				case 7:
					return valNodeOf(mutator, bitmap, valmap, node1, node2, node3, node4, node5, node6,
									node7, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node2, node3,
									node4, node5, node6, node7);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node2, node3,
									node4, node5, node6, node7);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node3,
									node4, node5, node6, node7);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node3,
									node4, node5, node6, node7);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2,
									node4, node5, node6, node7);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2,
									node4, node5, node6, node7);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2,
									node3, node5, node6, node7);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2,
									node3, node5, node6, node7);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2,
									node3, node4, node6, node7);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2,
									node3, node4, node6, node7);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2,
									node3, node4, node5, node7);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2,
									node3, node4, node5, node7);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 6:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, node1, node2,
									node3, node4, node5, node6);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, node1, node2,
									node3, node4, node5, node6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, node1, node2, node3,
							node4, node5, node6, node7 }, (byte) 1);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

			result = prime * result + node3.hashCode();

			result = prime * result + node4.hashCode();

			result = prime * result + node5.hashCode();

			result = prime * result + node6.hashCode();

			result = prime * result + node7.hashCode();

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
			Map1To7Node<?, ?> that = (Map1To7Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
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
			if (!node5.equals(that.node5)) {
				return false;
			}
			if (!node6.equals(that.node6)) {
				return false;
			}
			if (!node7.equals(that.node7)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format(
							"[@%d: %s=%s, @%d: %s, @%d: %s, @%d: %s, @%d: %s, @%d: %s, @%d: %s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1,
							recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3,
							recoverMask(bitmap ^ valmap, (byte) 4), node4,
							recoverMask(bitmap ^ valmap, (byte) 5), node5,
							recoverMask(bitmap ^ valmap, (byte) 6), node6,
							recoverMask(bitmap ^ valmap, (byte) 7), node7);
		}

	}

	private static final class Map2To0Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;

		Map2To0Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return Collections.<CompactMapNode<K, V>> emptyIterator();
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2 });
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2 }, (byte) 2);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

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

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
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
			return String.format("[@%d: %s=%s, @%d: %s=%s]", recoverMask(valmap, (byte) 1), key1, val1,
							recoverMask(valmap, (byte) 2), key2, val2);
		}

	}

	private static final class Map2To1Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final CompactMapNode<K, V> node1;

		Map2To1Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2,
						final CompactMapNode<K, V> node1) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.node1 = node1;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2 });
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, node1);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, node1);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, node1 },
							(byte) 2);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

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

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s=%s, @%d: %s=%s, @%d: %s]", recoverMask(valmap, (byte) 1),
							key1, val1, recoverMask(valmap, (byte) 2), key2, val2,
							recoverMask(bitmap ^ valmap, (byte) 1), node1);
		}

	}

	private static final class Map2To2Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;

		Map2To2Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.node1 = node1;
			this.node2 = node2;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2 });
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, node1, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1,
								node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1,
								node2);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1,
								node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node, node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node, node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node2);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap,
							new Object[] { key1, val1, key2, val2, node1, node2 }, (byte) 2);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + node1.hashCode();

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

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
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
			return String.format("[@%d: %s=%s, @%d: %s=%s, @%d: %s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2);
		}

	}

	private static final class Map2To3Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;

		Map2To3Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2 });
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, node1, node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, node1, node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1,
								node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1,
								node2, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1,
								node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node, node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node, node1, node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node, node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node2, node, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node2, node3, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node, node1, node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node, node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node2,
									node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node2,
									node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node2,
									node3);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1,
									node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1,
									node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1,
									node3);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1,
									node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1,
									node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1,
									node2);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, node1, node2,
							node3 }, (byte) 2);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

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
			Map2To3Node<?, ?> that = (Map2To3Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
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
			return String.format("[@%d: %s=%s, @%d: %s=%s, @%d: %s, @%d: %s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3);
		}

	}

	private static final class Map2To4Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;
		private final CompactMapNode<K, V> node4;

		Map2To4Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3, final CompactMapNode<K, V> node4) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;
			this.node4 = node4;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3, node4);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2 });
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, node1, node2, node3,
								node4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, node1, node2, node3,
								node4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1,
								node2, node3, node4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1,
								node2, node3, node4);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1,
								node2, node3, node4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node2, node3, node4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node, node2, node3,
								node4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node, node3,
								node4);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node,
								node4);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node3,
								node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node2, node3, node4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node3, node4);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node4);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node, node1, node2, node3,
									node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node, node2, node3,
									node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node2, node, node3,
									node4);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node2, node3, node,
									node4);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node2, node3, node4,
									node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node, node1, node2, node3,
									node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node, node2, node3,
									node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node, node3,
									node4);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node,
									node4);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4,
									node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node2,
									node3, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node2,
									node3, node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node2,
									node3, node4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1,
									node3, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1,
									node3, node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1,
									node3, node4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1,
									node2, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1,
									node2, node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1,
									node2, node4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1,
									node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1,
									node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1,
									node2, node3);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, node1, node2,
							node3, node4 }, (byte) 2);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

			result = prime * result + node3.hashCode();

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
			Map2To4Node<?, ?> that = (Map2To4Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
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
			return String.format("[@%d: %s=%s, @%d: %s=%s, @%d: %s, @%d: %s, @%d: %s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3,
							recoverMask(bitmap ^ valmap, (byte) 4), node4);
		}

	}

	private static final class Map2To5Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;
		private final CompactMapNode<K, V> node4;
		private final CompactMapNode<K, V> node5;

		Map2To5Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3, final CompactMapNode<K, V> node4,
						final CompactMapNode<K, V> node5) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;
			this.node4 = node4;
			this.node5 = node5;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3, node4, node5);
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 5;
		}

		@Override
		SupplierIterator<K, V> payloadIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2 });
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
		CompactMapNode<K, V> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;
			case 2:
				return node3;
			case 3:
				return node4;
			case 4:
				return node5;
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, node1, node2, node3,
								node4, node5);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, node1, node2, node3,
								node4, node5);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1,
								node2, node3, node4, node5);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1,
								node2, node3, node4, node5);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1,
								node2, node3, node4, node5);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node2, node3, node4, node5);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4, node5);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node, node2, node3,
								node4, node5);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node, node3,
								node4, node5);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node,
								node4, node5);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node3,
								node, node5);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node3,
								node4, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node2, node3, node4,
								node5);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node3, node4,
								node5);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node4,
								node5);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node3,
								node5);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node3,
								node4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node, node1, node2, node3,
									node4, node5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node, node2, node3,
									node4, node5);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node2, node, node3,
									node4, node5);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node2, node3, node,
									node4, node5);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node2, node3, node4,
									node, node5);
				case 5:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node2, node3, node4,
									node5, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node, node1, node2, node3,
									node4, node5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node, node2, node3,
									node4, node5);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node, node3,
									node4, node5);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node,
									node4, node5);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4,
									node, node5);
				case 5:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4,
									node5, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node2,
									node3, node4, node5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node2,
									node3, node4, node5);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node2,
									node3, node4, node5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1,
									node3, node4, node5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1,
									node3, node4, node5);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1,
									node3, node4, node5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1,
									node2, node4, node5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1,
									node2, node4, node5);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1,
									node2, node4, node5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1,
									node2, node3, node5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1,
									node2, node3, node5);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1,
									node2, node3, node5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1,
									node2, node3, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1,
									node2, node3, node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1,
									node2, node3, node4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, node1, node2,
							node3, node4, node5 }, (byte) 2);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

			result = prime * result + node3.hashCode();

			result = prime * result + node4.hashCode();

			result = prime * result + node5.hashCode();

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
			Map2To5Node<?, ?> that = (Map2To5Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
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
			if (!node5.equals(that.node5)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format(
							"[@%d: %s=%s, @%d: %s=%s, @%d: %s, @%d: %s, @%d: %s, @%d: %s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3,
							recoverMask(bitmap ^ valmap, (byte) 4), node4,
							recoverMask(bitmap ^ valmap, (byte) 5), node5);
		}

	}

	private static final class Map2To6Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;
		private final CompactMapNode<K, V> node4;
		private final CompactMapNode<K, V> node5;
		private final CompactMapNode<K, V> node6;

		Map2To6Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3, final CompactMapNode<K, V> node4,
						final CompactMapNode<K, V> node5, final CompactMapNode<K, V> node6) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;
			this.node4 = node4;
			this.node5 = node5;
			this.node6 = node6;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3, node4, node5, node6);
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 6;
		}

		@Override
		SupplierIterator<K, V> payloadIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2 });
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
		CompactMapNode<K, V> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;
			case 2:
				return node3;
			case 3:
				return node4;
			case 4:
				return node5;
			case 5:
				return node6;
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, node1, node2, node3,
								node4, node5, node6);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, node1, node2, node3,
								node4, node5, node6);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1,
								node2, node3, node4, node5, node6);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1,
								node2, node3, node4, node5, node6);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1,
								node2, node3, node4, node5, node6);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node2, node3, node4,
								node5, node6);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4,
								node5, node6);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node, node2, node3,
								node4, node5, node6);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node, node3,
								node4, node5, node6);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node,
								node4, node5, node6);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node3,
								node, node5, node6);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node3,
								node4, node, node6);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node3,
								node4, node5, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node2, node3, node4,
								node5, node6);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node3, node4,
								node5, node6);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node4,
								node5, node6);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node3,
								node5, node6);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node3,
								node4, node6);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node3,
								node4, node5);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node, node1, node2, node3,
									node4, node5, node6);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node, node2, node3,
									node4, node5, node6);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node2, node, node3,
									node4, node5, node6);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node2, node3, node,
									node4, node5, node6);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node2, node3, node4,
									node, node5, node6);
				case 5:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node2, node3, node4,
									node5, node, node6);
				case 6:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, node1, node2, node3, node4,
									node5, node6, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node, node1, node2, node3,
									node4, node5, node6);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node, node2, node3,
									node4, node5, node6);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node, node3,
									node4, node5, node6);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node,
									node4, node5, node6);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4,
									node, node5, node6);
				case 5:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4,
									node5, node, node6);
				case 6:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, node1, node2, node3, node4,
									node5, node6, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node2,
									node3, node4, node5, node6);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node2,
									node3, node4, node5, node6);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node2,
									node3, node4, node5, node6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1,
									node3, node4, node5, node6);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1,
									node3, node4, node5, node6);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1,
									node3, node4, node5, node6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1,
									node2, node4, node5, node6);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1,
									node2, node4, node5, node6);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1,
									node2, node4, node5, node6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1,
									node2, node3, node5, node6);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1,
									node2, node3, node5, node6);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1,
									node2, node3, node5, node6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1,
									node2, node3, node4, node6);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1,
									node2, node3, node4, node6);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1,
									node2, node3, node4, node6);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, node1,
									node2, node3, node4, node5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, node1,
									node2, node3, node4, node5);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, node1,
									node2, node3, node4, node5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, node1, node2,
							node3, node4, node5, node6 }, (byte) 2);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

			result = prime * result + node3.hashCode();

			result = prime * result + node4.hashCode();

			result = prime * result + node5.hashCode();

			result = prime * result + node6.hashCode();

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
			Map2To6Node<?, ?> that = (Map2To6Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
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
			if (!node5.equals(that.node5)) {
				return false;
			}
			if (!node6.equals(that.node6)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format(
							"[@%d: %s=%s, @%d: %s=%s, @%d: %s, @%d: %s, @%d: %s, @%d: %s, @%d: %s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3,
							recoverMask(bitmap ^ valmap, (byte) 4), node4,
							recoverMask(bitmap ^ valmap, (byte) 5), node5,
							recoverMask(bitmap ^ valmap, (byte) 6), node6);
		}

	}

	private static final class Map3To0Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;

		Map3To0Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return Collections.<CompactMapNode<K, V>> emptyIterator();
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3 });
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3 },
							(byte) 3);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

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

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
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
			return String.format("[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s]", recoverMask(valmap, (byte) 1),
							key1, val1, recoverMask(valmap, (byte) 2), key2, val2,
							recoverMask(valmap, (byte) 3), key3, val3);
		}

	}

	private static final class Map3To1Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;
		private final CompactMapNode<K, V> node1;

		Map3To1Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3, final CompactMapNode<K, V> node1) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.node1 = node1;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3 });
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3, node1);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3, node1);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3,
								node1);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3,
								node1);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3,
								node1);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val,
								node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node1);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node1);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3,
							node1 }, (byte) 3);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();

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

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(valmap, (byte) 3), key3, val3,
							recoverMask(bitmap ^ valmap, (byte) 1), node1);
		}

	}

	private static final class Map3To2Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;

		Map3To2Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3, final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.node1 = node1;
			this.node2 = node2;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3 });
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3, node1,
								node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3, node1,
								node2);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val, node1,
								node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3,
								node1, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3,
								node1, node2);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3,
								node1, node2);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val,
								node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node1, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node1, node2);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node,
								node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node, node1,
									node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node1, node,
									node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node1, node2,
									node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node, node1,
									node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node1, node,
									node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node1, node2,
									node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node, node1,
									node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node,
									node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2,
									node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, node2);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, node2);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, node1);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, node1);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, node1);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3,
							node1, node2 }, (byte) 3);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();

			result = prime * result + node1.hashCode();

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
			Map3To2Node<?, ?> that = (Map3To2Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
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
			return String.format("[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(valmap, (byte) 3), key3, val3,
							recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2);
		}

	}

	private static final class Map3To3Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;

		Map3To3Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3 });
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3, node1,
								node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3, node1,
								node2, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val, node1,
								node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3,
								node1, node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3,
								node1, node2, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3,
								node1, node2, node3);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val,
								node1, node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node1, node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node1, node2, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node,
								node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node2, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node2,
								node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node, node1,
									node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node1, node,
									node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node1, node2,
									node, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node1, node2,
									node3, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node, node1,
									node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node1, node,
									node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node1, node2,
									node, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node1, node2,
									node3, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node, node1,
									node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node,
									node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2,
									node, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2,
									node3, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, node2, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, node2, node3);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, node1, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, node1, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, node1, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, node1, node3);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, node1, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, node1, node2);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, node1, node2);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3,
							node1, node2, node3 }, (byte) 3);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

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
			Map3To3Node<?, ?> that = (Map3To3Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
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
			return String.format("[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s, @%d: %s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(valmap, (byte) 3), key3, val3,
							recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3);
		}

	}

	private static final class Map3To4Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;
		private final CompactMapNode<K, V> node4;

		Map3To4Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3,
						final CompactMapNode<K, V> node4) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;
			this.node4 = node4;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3, node4);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3 });
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3, node1,
								node2, node3, node4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3, node1,
								node2, node3, node4);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val, node1,
								node2, node3, node4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3,
								node1, node2, node3, node4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3,
								node1, node2, node3, node4);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3,
								node1, node2, node3, node4);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val,
								node1, node2, node3, node4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node1, node2, node3,
								node4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node1, node2, node3,
								node4);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node3,
								node4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node,
								node2, node3, node4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node, node3, node4);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node2, node, node4);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node2, node3, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node2,
								node3, node4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node3, node4);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node2, node4);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node, node1,
									node2, node3, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node1, node,
									node2, node3, node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node1, node2,
									node, node3, node4);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node1, node2,
									node3, node, node4);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node1, node2,
									node3, node4, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node, node1,
									node2, node3, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node1, node,
									node2, node3, node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node1, node2,
									node, node3, node4);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node1, node2,
									node3, node, node4);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node1, node2,
									node3, node4, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node, node1,
									node2, node3, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node,
									node2, node3, node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2,
									node, node3, node4);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2,
									node3, node, node4);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2,
									node3, node4, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, node2, node3, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, node2, node3, node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, node2, node3, node4);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, node2, node3, node4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, node1, node3, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, node1, node3, node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, node1, node3, node4);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, node1, node3, node4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, node1, node2, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, node1, node2, node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, node1, node2, node4);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, node1, node2, node4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, node1, node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, node1, node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, node1, node2, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, node1, node2, node3);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3,
							node1, node2, node3, node4 }, (byte) 3);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

			result = prime * result + node3.hashCode();

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
			Map3To4Node<?, ?> that = (Map3To4Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
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
			return String.format(
							"[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s, @%d: %s, @%d: %s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(valmap, (byte) 3), key3, val3,
							recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3,
							recoverMask(bitmap ^ valmap, (byte) 4), node4);
		}

	}

	private static final class Map3To5Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;
		private final CompactMapNode<K, V> node4;
		private final CompactMapNode<K, V> node5;

		Map3To5Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3,
						final CompactMapNode<K, V> node4, final CompactMapNode<K, V> node5) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;
			this.node4 = node4;
			this.node5 = node5;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3, node4, node5);
		}

		@Override
		boolean hasNodes() {
			return true;
		}

		@Override
		int nodeArity() {
			return 5;
		}

		@Override
		SupplierIterator<K, V> payloadIterator() {
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3 });
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
		CompactMapNode<K, V> getNode(int index) {
			switch (index) {
			case 0:
				return node1;
			case 1:
				return node2;
			case 2:
				return node3;
			case 3:
				return node4;
			case 4:
				return node5;
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3, node1,
								node2, node3, node4, node5);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3, node1,
								node2, node3, node4, node5);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val, node1,
								node2, node3, node4, node5);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3,
								node1, node2, node3, node4, node5);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3,
								node1, node2, node3, node4, node5);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3,
								node1, node2, node3, node4, node5);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val,
								node1, node2, node3, node4, node5);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node1, node2, node3,
								node4, node5);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node1, node2, node3,
								node4, node5);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2, node3,
								node4, node5);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node,
								node2, node3, node4, node5);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node, node3, node4, node5);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node2, node, node4, node5);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node2, node3, node, node5);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node2, node3, node4, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node2,
								node3, node4, node5);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node3, node4, node5);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node2, node4, node5);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node2, node3, node5);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node2, node3, node4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node, node1,
									node2, node3, node4, node5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node1, node,
									node2, node3, node4, node5);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node1, node2,
									node, node3, node4, node5);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node1, node2,
									node3, node, node4, node5);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node1, node2,
									node3, node4, node, node5);
				case 5:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, node1, node2,
									node3, node4, node5, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node, node1,
									node2, node3, node4, node5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node1, node,
									node2, node3, node4, node5);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node1, node2,
									node, node3, node4, node5);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node1, node2,
									node3, node, node4, node5);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node1, node2,
									node3, node4, node, node5);
				case 5:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, node1, node2,
									node3, node4, node5, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node, node1,
									node2, node3, node4, node5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node,
									node2, node3, node4, node5);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2,
									node, node3, node4, node5);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2,
									node3, node, node4, node5);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2,
									node3, node4, node, node5);
				case 5:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, node1, node2,
									node3, node4, node5, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, node2, node3, node4, node5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, node2, node3, node4, node5);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, node2, node3, node4, node5);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, node2, node3, node4, node5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, node1, node3, node4, node5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, node1, node3, node4, node5);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, node1, node3, node4, node5);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, node1, node3, node4, node5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, node1, node2, node4, node5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, node1, node2, node4, node5);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, node1, node2, node4, node5);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, node1, node2, node4, node5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, node1, node2, node3, node5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, node1, node2, node3, node5);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, node1, node2, node3, node5);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, node1, node2, node3, node5);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, node1, node2, node3, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, node1, node2, node3, node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, node1, node2, node3, node4);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, node1, node2, node3, node4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3,
							node1, node2, node3, node4, node5 }, (byte) 3);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

			result = prime * result + node3.hashCode();

			result = prime * result + node4.hashCode();

			result = prime * result + node5.hashCode();

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
			Map3To5Node<?, ?> that = (Map3To5Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
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
			if (!node5.equals(that.node5)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format(
							"[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s, @%d: %s, @%d: %s, @%d: %s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(valmap, (byte) 3), key3, val3,
							recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3,
							recoverMask(bitmap ^ valmap, (byte) 4), node4,
							recoverMask(bitmap ^ valmap, (byte) 5), node5);
		}

	}

	private static final class Map4To0Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;
		private final K key4;
		private final V val4;

		Map4To0Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3, final K key4, final V val4) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return Collections.<CompactMapNode<K, V>> emptyIterator();
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3, key4,
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3, key4, val4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3, key4, val4);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val, key4, val4);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3,
								key4, val4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3,
								key4, val4);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3,
								key4, val4);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val,
								key4, val4);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3,
							key4, val4 }, (byte) 4);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();

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

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
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
			return String.format("[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(valmap, (byte) 3), key3, val3,
							recoverMask(valmap, (byte) 4), key4, val4);
		}

	}

	private static final class Map4To1Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;
		private final K key4;
		private final V val4;
		private final CompactMapNode<K, V> node1;

		Map4To1Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3, final K key4, final V val4, final CompactMapNode<K, V> node1) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.node1 = node1;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3, key4,
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3, key4,
								val4, node1);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3, key4,
								val4, node1);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val, key4,
								val4, node1);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3,
								key4, val4, node1);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3,
								key4, val4, node1);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3,
								key4, val4, node1);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val,
								key4, val4, node1);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key, val, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, node1);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, node1);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, node1);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, node,
									node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4,
									node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, node,
									node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4,
									node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, node,
									node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4,
									node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node,
									node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3,
									node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, key4, val4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, key4, val4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, key4, val4);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, key4, val4);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key, val);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3,
							key4, val4, node1 }, (byte) 4);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();

			result = prime * result + key4.hashCode();
			result = prime * result + val4.hashCode();

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
			Map4To1Node<?, ?> that = (Map4To1Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
				return false;
			}
			if (!key4.equals(that.key4)) {
				return false;
			}
			if (!val4.equals(that.val4)) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(valmap, (byte) 3), key3, val3,
							recoverMask(valmap, (byte) 4), key4, val4,
							recoverMask(bitmap ^ valmap, (byte) 1), node1);
		}

	}

	private static final class Map4To2Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;
		private final K key4;
		private final V val4;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;

		Map4To2Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3, final K key4, final V val4, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.node1 = node1;
			this.node2 = node2;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3, key4,
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3, key4,
								val4, node1, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3, key4,
								val4, node1, node2);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val, key4,
								val4, node1, node2);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val, node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3,
								key4, val4, node1, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3,
								key4, val4, node1, node2);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3,
								key4, val4, node1, node2);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val,
								key4, val4, node1, node2);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key, val, node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, node1,
								node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, node1,
								node2);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, node1,
								node2);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node1, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, node,
									node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4,
									node1, node, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4,
									node1, node2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, node,
									node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4,
									node1, node, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4,
									node1, node2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, node,
									node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4,
									node1, node, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4,
									node1, node2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node,
									node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3,
									node1, node, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3,
									node1, node2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, key4, val4, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, key4, val4, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, key4, val4, node2);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, key4, val4, node2);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key, val, node2);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, key4, val4, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, key4, val4, node1);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, key4, val4, node1);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, key4, val4, node1);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key, val, node1);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3,
							key4, val4, node1, node2 }, (byte) 4);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();

			result = prime * result + key4.hashCode();
			result = prime * result + val4.hashCode();

			result = prime * result + node1.hashCode();

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
			Map4To2Node<?, ?> that = (Map4To2Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
				return false;
			}
			if (!key4.equals(that.key4)) {
				return false;
			}
			if (!val4.equals(that.val4)) {
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
			return String.format("[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(valmap, (byte) 3), key3, val3,
							recoverMask(valmap, (byte) 4), key4, val4,
							recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2);
		}

	}

	private static final class Map4To3Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;
		private final K key4;
		private final V val4;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;

		Map4To3Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3, final K key4, final V val4, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3, key4,
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3, key4,
								val4, node1, node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3, key4,
								val4, node1, node2, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val, key4,
								val4, node1, node2, node3);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val, node1, node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3,
								key4, val4, node1, node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3,
								key4, val4, node1, node2, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3,
								key4, val4, node1, node2, node3);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val,
								key4, val4, node1, node2, node3);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key, val, node1, node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, node1,
								node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, node1,
								node2, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, node1,
								node2, node3);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node, node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node1, node, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node1, node2, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node1, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, node,
									node1, node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4,
									node1, node, node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4,
									node1, node2, node, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4,
									node1, node2, node3, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, node,
									node1, node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4,
									node1, node, node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4,
									node1, node2, node, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4,
									node1, node2, node3, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, node,
									node1, node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4,
									node1, node, node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4,
									node1, node2, node, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4,
									node1, node2, node3, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node,
									node1, node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3,
									node1, node, node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3,
									node1, node2, node, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3,
									node1, node2, node3, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, key4, val4, node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, key4, val4, node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, key4, val4, node2, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, key4, val4, node2, node3);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key, val, node2, node3);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, key4, val4, node1, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, key4, val4, node1, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, key4, val4, node1, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, key4, val4, node1, node3);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key, val, node1, node3);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, key4, val4, node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, key4, val4, node1, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, key4, val4, node1, node2);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, key4, val4, node1, node2);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key, val, node1, node2);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3,
							key4, val4, node1, node2, node3 }, (byte) 4);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();

			result = prime * result + key4.hashCode();
			result = prime * result + val4.hashCode();

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

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
			Map4To3Node<?, ?> that = (Map4To3Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
				return false;
			}
			if (!key4.equals(that.key4)) {
				return false;
			}
			if (!val4.equals(that.val4)) {
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
			return String.format(
							"[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s, @%d: %s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(valmap, (byte) 3), key3, val3,
							recoverMask(valmap, (byte) 4), key4, val4,
							recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3);
		}

	}

	private static final class Map4To4Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;
		private final K key4;
		private final V val4;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;
		private final CompactMapNode<K, V> node4;

		Map4To4Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3, final K key4, final V val4, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2, final CompactMapNode<K, V> node3,
						final CompactMapNode<K, V> node4) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;
			this.node4 = node4;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3, node4);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3, key4,
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
		CompactMapNode<K, V> getNode(int index) {
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
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3, key4,
								val4, node1, node2, node3, node4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3, key4,
								val4, node1, node2, node3, node4);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val, key4,
								val4, node1, node2, node3, node4);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val, node1, node2, node3, node4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3,
								key4, val4, node1, node2, node3, node4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3,
								key4, val4, node1, node2, node3, node4);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3,
								key4, val4, node1, node2, node3, node4);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val,
								key4, val4, node1, node2, node3, node4);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key, val, node1, node2, node3, node4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, node1,
								node2, node3, node4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, node1,
								node2, node3, node4);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, node1,
								node2, node3, node4);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node1,
								node2, node3, node4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node, node2, node3, node4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node1, node, node3, node4);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node1, node2, node, node4);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node1, node2, node3, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node2, node3, node4);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node1, node3, node4);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node1, node2, node4);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node1, node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, node,
									node1, node2, node3, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4,
									node1, node, node2, node3, node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4,
									node1, node2, node, node3, node4);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4,
									node1, node2, node3, node, node4);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4,
									node1, node2, node3, node4, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, node,
									node1, node2, node3, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4,
									node1, node, node2, node3, node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4,
									node1, node2, node, node3, node4);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4,
									node1, node2, node3, node, node4);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4,
									node1, node2, node3, node4, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, node,
									node1, node2, node3, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4,
									node1, node, node2, node3, node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4,
									node1, node2, node, node3, node4);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4,
									node1, node2, node3, node, node4);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4,
									node1, node2, node3, node4, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, node,
									node1, node2, node3, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3,
									node1, node, node2, node3, node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3,
									node1, node2, node, node3, node4);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3,
									node1, node2, node3, node, node4);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3,
									node1, node2, node3, node4, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, key4, val4, node2, node3, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, key4, val4, node2, node3, node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, key4, val4, node2, node3, node4);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, key4, val4, node2, node3, node4);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key, val, node2, node3, node4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, key4, val4, node1, node3, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, key4, val4, node1, node3, node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, key4, val4, node1, node3, node4);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, key4, val4, node1, node3, node4);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key, val, node1, node3, node4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, key4, val4, node1, node2, node4);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, key4, val4, node1, node2, node4);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, key4, val4, node1, node2, node4);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, key4, val4, node1, node2, node4);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key, val, node1, node2, node4);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, key4, val4, node1, node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, key4, val4, node1, node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, key4, val4, node1, node2, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, key4, val4, node1, node2, node3);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key, val, node1, node2, node3);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3,
							key4, val4, node1, node2, node3, node4 }, (byte) 4);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();

			result = prime * result + key4.hashCode();
			result = prime * result + val4.hashCode();

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

			result = prime * result + node3.hashCode();

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
			Map4To4Node<?, ?> that = (Map4To4Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
				return false;
			}
			if (!key4.equals(that.key4)) {
				return false;
			}
			if (!val4.equals(that.val4)) {
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
			return String.format(
							"[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s, @%d: %s, @%d: %s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(valmap, (byte) 3), key3, val3,
							recoverMask(valmap, (byte) 4), key4, val4,
							recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3,
							recoverMask(bitmap ^ valmap, (byte) 4), node4);
		}

	}

	private static final class Map5To0Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;
		private final K key4;
		private final V val4;
		private final K key5;
		private final V val5;

		Map5To0Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3, final K key4, final V val4, final K key5, final V val5) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.key5 = key5;
			this.val5 = val5;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return Collections.<CompactMapNode<K, V>> emptyIterator();
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3, key4,
							val4, key5, val5 });
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
			return 5;
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
		CompactMapNode<K, V> getNode(int index) {
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
			case 4:
				return key5;
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
			case 4:
				return val5;
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
			case 4:
				return entryOf(key5, val5);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3, key4,
								val4, key5, val5);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3, key4,
								val4, key5, val5);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val, key4,
								val4, key5, val5);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val, key5, val5);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3,
								key4, val4, key5, val5);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3,
								key4, val4, key5, val5);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3,
								key4, val4, key5, val5);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val,
								key4, val4, key5, val5);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key, val, key5, val5);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
								val5);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
								val5);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
								val5);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
								val5);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
									val5, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
									val5, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
									val5, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
									val5, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3,
							key4, val4, key5, val5 }, (byte) 5);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();

			result = prime * result + key4.hashCode();
			result = prime * result + val4.hashCode();

			result = prime * result + key5.hashCode();
			result = prime * result + val5.hashCode();

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
			Map5To0Node<?, ?> that = (Map5To0Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
				return false;
			}
			if (!key4.equals(that.key4)) {
				return false;
			}
			if (!val4.equals(that.val4)) {
				return false;
			}
			if (!key5.equals(that.key5)) {
				return false;
			}
			if (!val5.equals(that.val5)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(valmap, (byte) 3), key3, val3,
							recoverMask(valmap, (byte) 4), key4, val4, recoverMask(valmap, (byte) 5),
							key5, val5);
		}

	}

	private static final class Map5To1Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;
		private final K key4;
		private final V val4;
		private final K key5;
		private final V val5;
		private final CompactMapNode<K, V> node1;

		Map5To1Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3, final K key4, final V val4, final K key5, final V val5,
						final CompactMapNode<K, V> node1) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.key5 = key5;
			this.val5 = val5;
			this.node1 = node1;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3, key4,
							val4, key5, val5 });
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
			return 5;
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
		CompactMapNode<K, V> getNode(int index) {
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
			case 3:
				return key4;
			case 4:
				return key5;
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
			case 4:
				return val5;
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
			case 4:
				return entryOf(key5, val5);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3, key4,
								val4, key5, val5, node1);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3, key4,
								val4, key5, val5, node1);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val, key4,
								val4, key5, val5, node1);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val, key5, val5, node1);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3,
								key4, val4, key5, val5, node1);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3,
								key4, val4, key5, val5, node1);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3,
								key4, val4, key5, val5, node1);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val,
								key4, val4, key5, val5, node1);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key, val, key5, val5, node1);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key, val, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
								val5, node1);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
								val5, node1);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
								val5, node1);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
								val5, node1);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
									val5, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
									val5, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
									val5, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
									val5, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
									val5, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
									val5, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
									val5, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
									val5, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, key4, val4, key5, val5);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, key4, val4, key5, val5);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, key4, val4, key5, val5);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, key4, val4, key5, val5);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key, val, key5, val5);
				case 5:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key, val);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3,
							key4, val4, key5, val5, node1 }, (byte) 5);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();

			result = prime * result + key4.hashCode();
			result = prime * result + val4.hashCode();

			result = prime * result + key5.hashCode();
			result = prime * result + val5.hashCode();

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
			Map5To1Node<?, ?> that = (Map5To1Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
				return false;
			}
			if (!key4.equals(that.key4)) {
				return false;
			}
			if (!val4.equals(that.val4)) {
				return false;
			}
			if (!key5.equals(that.key5)) {
				return false;
			}
			if (!val5.equals(that.val5)) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format(
							"[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(valmap, (byte) 3), key3, val3,
							recoverMask(valmap, (byte) 4), key4, val4, recoverMask(valmap, (byte) 5),
							key5, val5, recoverMask(bitmap ^ valmap, (byte) 1), node1);
		}

	}

	private static final class Map5To2Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;
		private final K key4;
		private final V val4;
		private final K key5;
		private final V val5;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;

		Map5To2Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3, final K key4, final V val4, final K key5, final V val5,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.key5 = key5;
			this.val5 = val5;
			this.node1 = node1;
			this.node2 = node2;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3, key4,
							val4, key5, val5 });
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
			return 5;
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
		CompactMapNode<K, V> getNode(int index) {
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
			case 2:
				return key3;
			case 3:
				return key4;
			case 4:
				return key5;
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
			case 4:
				return val5;
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
			case 4:
				return entryOf(key5, val5);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3, key4,
								val4, key5, val5, node1, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3, key4,
								val4, key5, val5, node1, node2);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val, key4,
								val4, key5, val5, node1, node2);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val, key5, val5, node1, node2);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val, node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3,
								key4, val4, key5, val5, node1, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3,
								key4, val4, key5, val5, node1, node2);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3,
								key4, val4, key5, val5, node1, node2);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val,
								key4, val4, key5, val5, node1, node2);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key, val, key5, val5, node1, node2);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key, val, node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
								val5, node1, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
								val5, node1, node2);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
								val5, node1, node2);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
								val5, node1, node2);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, node, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, node1, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
									val5, node, node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
									val5, node1, node, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
									val5, node1, node2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
									val5, node, node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
									val5, node1, node, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
									val5, node1, node2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
									val5, node, node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
									val5, node1, node, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
									val5, node1, node2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
									val5, node, node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
									val5, node1, node, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
									val5, node1, node2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, node, node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, node1, node, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, node1, node2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, key4, val4, key5, val5, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, key4, val4, key5, val5, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, key4, val4, key5, val5, node2);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, key4, val4, key5, val5, node2);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key, val, key5, val5, node2);
				case 5:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key, val, node2);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, key4, val4, key5, val5, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, key4, val4, key5, val5, node1);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, key4, val4, key5, val5, node1);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, key4, val4, key5, val5, node1);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key, val, key5, val5, node1);
				case 5:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key, val, node1);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3,
							key4, val4, key5, val5, node1, node2 }, (byte) 5);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();

			result = prime * result + key4.hashCode();
			result = prime * result + val4.hashCode();

			result = prime * result + key5.hashCode();
			result = prime * result + val5.hashCode();

			result = prime * result + node1.hashCode();

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
			Map5To2Node<?, ?> that = (Map5To2Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
				return false;
			}
			if (!key4.equals(that.key4)) {
				return false;
			}
			if (!val4.equals(that.val4)) {
				return false;
			}
			if (!key5.equals(that.key5)) {
				return false;
			}
			if (!val5.equals(that.val5)) {
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
			return String.format(
							"[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(valmap, (byte) 3), key3, val3,
							recoverMask(valmap, (byte) 4), key4, val4, recoverMask(valmap, (byte) 5),
							key5, val5, recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2);
		}

	}

	private static final class Map5To3Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;
		private final K key4;
		private final V val4;
		private final K key5;
		private final V val5;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;
		private final CompactMapNode<K, V> node3;

		Map5To3Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3, final K key4, final V val4, final K key5, final V val5,
						final CompactMapNode<K, V> node1, final CompactMapNode<K, V> node2,
						final CompactMapNode<K, V> node3) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.key5 = key5;
			this.val5 = val5;
			this.node1 = node1;
			this.node2 = node2;
			this.node3 = node3;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2, node3);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3, key4,
							val4, key5, val5 });
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
			return 5;
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
		CompactMapNode<K, V> getNode(int index) {
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
			case 1:
				return key2;
			case 2:
				return key3;
			case 3:
				return key4;
			case 4:
				return key5;
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
			case 4:
				return val5;
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
			case 4:
				return entryOf(key5, val5);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3, key4,
								val4, key5, val5, node1, node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3, key4,
								val4, key5, val5, node1, node2, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val, key4,
								val4, key5, val5, node1, node2, node3);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val, key5, val5, node1, node2, node3);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val, node1, node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3,
								key4, val4, key5, val5, node1, node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3,
								key4, val4, key5, val5, node1, node2, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3,
								key4, val4, key5, val5, node1, node2, node3);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val,
								key4, val4, key5, val5, node1, node2, node3);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key, val, key5, val5, node1, node2, node3);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key, val, node1, node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
								val5, node1, node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
								val5, node1, node2, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
								val5, node1, node2, node3);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
								val5, node1, node2, node3);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, node1, node2, node3);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, node, node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, node1, node, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, node1, node2, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, node2, node3);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, node1, node3);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
									val5, node, node1, node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
									val5, node1, node, node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
									val5, node1, node2, node, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
									val5, node1, node2, node3, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
									val5, node, node1, node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
									val5, node1, node, node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
									val5, node1, node2, node, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
									val5, node1, node2, node3, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
									val5, node, node1, node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
									val5, node1, node, node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
									val5, node1, node2, node, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
									val5, node1, node2, node3, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
									val5, node, node1, node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
									val5, node1, node, node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
									val5, node1, node2, node, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
									val5, node1, node2, node3, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, node, node1, node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, node1, node, node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, node1, node2, node, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, node1, node2, node3, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, key4, val4, key5, val5, node2, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, key4, val4, key5, val5, node2, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, key4, val4, key5, val5, node2, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, key4, val4, key5, val5, node2, node3);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key, val, key5, val5, node2, node3);
				case 5:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key, val, node2, node3);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, key4, val4, key5, val5, node1, node3);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, key4, val4, key5, val5, node1, node3);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, key4, val4, key5, val5, node1, node3);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, key4, val4, key5, val5, node1, node3);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key, val, key5, val5, node1, node3);
				case 5:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key, val, node1, node3);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, key4, val4, key5, val5, node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, key4, val4, key5, val5, node1, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, key4, val4, key5, val5, node1, node2);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, key4, val4, key5, val5, node1, node2);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key, val, key5, val5, node1, node2);
				case 5:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key, val, node1, node2);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3,
							key4, val4, key5, val5, node1, node2, node3 }, (byte) 5);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();

			result = prime * result + key4.hashCode();
			result = prime * result + val4.hashCode();

			result = prime * result + key5.hashCode();
			result = prime * result + val5.hashCode();

			result = prime * result + node1.hashCode();

			result = prime * result + node2.hashCode();

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
			Map5To3Node<?, ?> that = (Map5To3Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
				return false;
			}
			if (!key4.equals(that.key4)) {
				return false;
			}
			if (!val4.equals(that.val4)) {
				return false;
			}
			if (!key5.equals(that.key5)) {
				return false;
			}
			if (!val5.equals(that.val5)) {
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
			return String.format(
							"[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s, @%d: %s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(valmap, (byte) 3), key3, val3,
							recoverMask(valmap, (byte) 4), key4, val4, recoverMask(valmap, (byte) 5),
							key5, val5, recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2,
							recoverMask(bitmap ^ valmap, (byte) 3), node3);
		}

	}

	private static final class Map6To0Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;
		private final K key4;
		private final V val4;
		private final K key5;
		private final V val5;
		private final K key6;
		private final V val6;

		Map6To0Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3, final K key4, final V val4, final K key5, final V val5,
						final K key6, final V val6) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.key5 = key5;
			this.val5 = val5;
			this.key6 = key6;
			this.val6 = val6;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return Collections.<CompactMapNode<K, V>> emptyIterator();
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3, key4,
							val4, key5, val5, key6, val6 });
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
			return 6;
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
		CompactMapNode<K, V> getNode(int index) {
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
			case 4:
				return key5;
			case 5:
				return key6;
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
			case 4:
				return val5;
			case 5:
				return val6;
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
			case 4:
				return entryOf(key5, val5);
			case 5:
				return entryOf(key6, val6);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3, key4,
								val4, key5, val5, key6, val6);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val, key4,
								val4, key5, val5, key6, val6);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val, key5, val5, key6, val6);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val, key6, val6);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3,
								key4, val4, key5, val5, key6, val6);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3,
								key4, val4, key5, val5, key6, val6);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3,
								key4, val4, key5, val5, key6, val6);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val,
								key4, val4, key5, val5, key6, val6);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key, val, key5, val5, key6, val6);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key, val, key6, val6);
			case 6:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
								val5, key6, val6);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
								val5, key6, val6);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
								val5, key6, val6);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
								val5, key6, val6);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key6, val6);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
									val5, key6, val6, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
									val5, key6, val6, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
									val5, key6, val6, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
									val5, key6, val6, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key6, val6, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3,
							key4, val4, key5, val5, key6, val6 }, (byte) 6);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();

			result = prime * result + key4.hashCode();
			result = prime * result + val4.hashCode();

			result = prime * result + key5.hashCode();
			result = prime * result + val5.hashCode();

			result = prime * result + key6.hashCode();
			result = prime * result + val6.hashCode();

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
			Map6To0Node<?, ?> that = (Map6To0Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
				return false;
			}
			if (!key4.equals(that.key4)) {
				return false;
			}
			if (!val4.equals(that.val4)) {
				return false;
			}
			if (!key5.equals(that.key5)) {
				return false;
			}
			if (!val5.equals(that.val5)) {
				return false;
			}
			if (!key6.equals(that.key6)) {
				return false;
			}
			if (!val6.equals(that.val6)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format(
							"[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(valmap, (byte) 3), key3, val3,
							recoverMask(valmap, (byte) 4), key4, val4, recoverMask(valmap, (byte) 5),
							key5, val5, recoverMask(valmap, (byte) 6), key6, val6);
		}

	}

	private static final class Map6To1Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;
		private final K key4;
		private final V val4;
		private final K key5;
		private final V val5;
		private final K key6;
		private final V val6;
		private final CompactMapNode<K, V> node1;

		Map6To1Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3, final K key4, final V val4, final K key5, final V val5,
						final K key6, final V val6, final CompactMapNode<K, V> node1) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.key5 = key5;
			this.val5 = val5;
			this.key6 = key6;
			this.val6 = val6;
			this.node1 = node1;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3, key4,
							val4, key5, val5, key6, val6 });
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
			return 6;
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
		CompactMapNode<K, V> getNode(int index) {
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
			case 3:
				return key4;
			case 4:
				return key5;
			case 5:
				return key6;
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
			case 4:
				return val5;
			case 5:
				return val6;
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
			case 4:
				return entryOf(key5, val5);
			case 5:
				return entryOf(key6, val6);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, node1);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3, key4,
								val4, key5, val5, key6, val6, node1);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val, key4,
								val4, key5, val5, key6, val6, node1);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val, key5, val5, key6, val6, node1);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val, key6, val6, node1);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3,
								key4, val4, key5, val5, key6, val6, node1);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3,
								key4, val4, key5, val5, key6, val6, node1);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3,
								key4, val4, key5, val5, key6, val6, node1);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val,
								key4, val4, key5, val5, key6, val6, node1);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key, val, key5, val5, key6, val6, node1);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key, val, key6, val6, node1);
			case 6:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key, val, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
								val5, key6, val6, node1);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
								val5, key6, val6, node1);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
								val5, key6, val6, node1);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
								val5, key6, val6, node1);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key6, val6, node1);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
									val5, key6, val6, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
									val5, key6, val6, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
									val5, key6, val6, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
									val5, key6, val6, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
									val5, key6, val6, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
									val5, key6, val6, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
									val5, key6, val6, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
									val5, key6, val6, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key6, val6, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key6, val6, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, key4, val4, key5, val5, key6, val6);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, key4, val4, key5, val5, key6, val6);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, key4, val4, key5, val5, key6, val6);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, key4, val4, key5, val5, key6, val6);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key, val, key5, val5, key6, val6);
				case 5:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key, val, key6, val6);
				case 6:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key6, val6, key, val);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3,
							key4, val4, key5, val5, key6, val6, node1 }, (byte) 6);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();

			result = prime * result + key4.hashCode();
			result = prime * result + val4.hashCode();

			result = prime * result + key5.hashCode();
			result = prime * result + val5.hashCode();

			result = prime * result + key6.hashCode();
			result = prime * result + val6.hashCode();

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
			Map6To1Node<?, ?> that = (Map6To1Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
				return false;
			}
			if (!key4.equals(that.key4)) {
				return false;
			}
			if (!val4.equals(that.val4)) {
				return false;
			}
			if (!key5.equals(that.key5)) {
				return false;
			}
			if (!val5.equals(that.val5)) {
				return false;
			}
			if (!key6.equals(that.key6)) {
				return false;
			}
			if (!val6.equals(that.val6)) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format(
							"[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(valmap, (byte) 3), key3, val3,
							recoverMask(valmap, (byte) 4), key4, val4, recoverMask(valmap, (byte) 5),
							key5, val5, recoverMask(valmap, (byte) 6), key6, val6,
							recoverMask(bitmap ^ valmap, (byte) 1), node1);
		}

	}

	private static final class Map6To2Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;
		private final K key4;
		private final V val4;
		private final K key5;
		private final V val5;
		private final K key6;
		private final V val6;
		private final CompactMapNode<K, V> node1;
		private final CompactMapNode<K, V> node2;

		Map6To2Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3, final K key4, final V val4, final K key5, final V val5,
						final K key6, final V val6, final CompactMapNode<K, V> node1,
						final CompactMapNode<K, V> node2) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.key5 = key5;
			this.val5 = val5;
			this.key6 = key6;
			this.val6 = val6;
			this.node1 = node1;
			this.node2 = node2;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1, node2);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3, key4,
							val4, key5, val5, key6, val6 });
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
			return 6;
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
		CompactMapNode<K, V> getNode(int index) {
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
			case 2:
				return key3;
			case 3:
				return key4;
			case 4:
				return key5;
			case 5:
				return key6;
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
			case 4:
				return val5;
			case 5:
				return val6;
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
			case 4:
				return entryOf(key5, val5);
			case 5:
				return entryOf(key6, val6);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, node1, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3, key4,
								val4, key5, val5, key6, val6, node1, node2);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val, key4,
								val4, key5, val5, key6, val6, node1, node2);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val, key5, val5, key6, val6, node1, node2);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val, key6, val6, node1, node2);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val, node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3,
								key4, val4, key5, val5, key6, val6, node1, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3,
								key4, val4, key5, val5, key6, val6, node1, node2);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3,
								key4, val4, key5, val5, key6, val6, node1, node2);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val,
								key4, val4, key5, val5, key6, val6, node1, node2);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key, val, key5, val5, key6, val6, node1, node2);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key, val, key6, val6, node1, node2);
			case 6:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key, val, node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
								val5, key6, val6, node1, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
								val5, key6, val6, node1, node2);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
								val5, key6, val6, node1, node2);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
								val5, key6, val6, node1, node2);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key6, val6, node1, node2);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, node1, node2);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, node, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, node1, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, node2);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
									val5, key6, val6, node, node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
									val5, key6, val6, node1, node, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
									val5, key6, val6, node1, node2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
									val5, key6, val6, node, node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
									val5, key6, val6, node1, node, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
									val5, key6, val6, node1, node2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
									val5, key6, val6, node, node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
									val5, key6, val6, node1, node, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
									val5, key6, val6, node1, node2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
									val5, key6, val6, node, node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
									val5, key6, val6, node1, node, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
									val5, key6, val6, node1, node2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key6, val6, node, node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key6, val6, node1, node, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key6, val6, node1, node2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, node, node1, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, node1, node, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, node1, node2, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, key4, val4, key5, val5, key6, val6, node2);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, key4, val4, key5, val5, key6, val6, node2);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, key4, val4, key5, val5, key6, val6, node2);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, key4, val4, key5, val5, key6, val6, node2);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key, val, key5, val5, key6, val6, node2);
				case 5:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key, val, key6, val6, node2);
				case 6:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key6, val6, key, val, node2);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, key4, val4, key5, val5, key6, val6, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, key4, val4, key5, val5, key6, val6, node1);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, key4, val4, key5, val5, key6, val6, node1);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, key4, val4, key5, val5, key6, val6, node1);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key, val, key5, val5, key6, val6, node1);
				case 5:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key, val, key6, val6, node1);
				case 6:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key6, val6, key, val, node1);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3,
							key4, val4, key5, val5, key6, val6, node1, node2 }, (byte) 6);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();

			result = prime * result + key4.hashCode();
			result = prime * result + val4.hashCode();

			result = prime * result + key5.hashCode();
			result = prime * result + val5.hashCode();

			result = prime * result + key6.hashCode();
			result = prime * result + val6.hashCode();

			result = prime * result + node1.hashCode();

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
			Map6To2Node<?, ?> that = (Map6To2Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
				return false;
			}
			if (!key4.equals(that.key4)) {
				return false;
			}
			if (!val4.equals(that.val4)) {
				return false;
			}
			if (!key5.equals(that.key5)) {
				return false;
			}
			if (!val5.equals(that.val5)) {
				return false;
			}
			if (!key6.equals(that.key6)) {
				return false;
			}
			if (!val6.equals(that.val6)) {
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
			return String.format(
							"[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(valmap, (byte) 3), key3, val3,
							recoverMask(valmap, (byte) 4), key4, val4, recoverMask(valmap, (byte) 5),
							key5, val5, recoverMask(valmap, (byte) 6), key6, val6,
							recoverMask(bitmap ^ valmap, (byte) 1), node1,
							recoverMask(bitmap ^ valmap, (byte) 2), node2);
		}

	}

	private static final class Map7To0Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;
		private final K key4;
		private final V val4;
		private final K key5;
		private final V val5;
		private final K key6;
		private final V val6;
		private final K key7;
		private final V val7;

		Map7To0Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3, final K key4, final V val4, final K key5, final V val5,
						final K key6, final V val6, final K key7, final V val7) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.key5 = key5;
			this.val5 = val5;
			this.key6 = key6;
			this.val6 = val6;
			this.key7 = key7;
			this.val7 = val7;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return Collections.<CompactMapNode<K, V>> emptyIterator();
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3, key4,
							val4, key5, val5, key6, val6, key7, val7 });
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
			return 7;
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
		CompactMapNode<K, V> getNode(int index) {
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
			case 4:
				return key5;
			case 5:
				return key6;
			case 6:
				return key7;
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
			case 4:
				return val5;
			case 5:
				return val6;
			case 6:
				return val7;
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
			case 4:
				return entryOf(key5, val5);
			case 5:
				return entryOf(key6, val6);
			case 6:
				return entryOf(key7, val7);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key7, val7);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3, key4,
								val4, key5, val5, key6, val6, key7, val7);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val, key4,
								val4, key5, val5, key6, val6, key7, val7);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val, key5, val5, key6, val6, key7, val7);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val, key6, val6, key7, val7);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val, key7, val7);
			case 6:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key7, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3,
								key4, val4, key5, val5, key6, val6, key7, val7);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3,
								key4, val4, key5, val5, key6, val6, key7, val7);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3,
								key4, val4, key5, val5, key6, val6, key7, val7);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val,
								key4, val4, key5, val5, key6, val6, key7, val7);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key, val, key5, val5, key6, val6, key7, val7);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key, val, key6, val6, key7, val7);
			case 6:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key, val, key7, val7);
			case 7:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key7, val7, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
								val5, key6, val6, key7, val7);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
								val5, key6, val6, key7, val7);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
								val5, key6, val6, key7, val7);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
								val5, key6, val6, key7, val7);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key6, val6, key7, val7);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key7, val7);
			case 6:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
									val5, key6, val6, key7, val7, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
									val5, key6, val6, key7, val7, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
									val5, key6, val6, key7, val7, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
									val5, key6, val6, key7, val7, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key6, val6, key7, val7, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key7, val7, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 6:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key6, val6, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3,
							key4, val4, key5, val5, key6, val6, key7, val7 }, (byte) 7);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();

			result = prime * result + key4.hashCode();
			result = prime * result + val4.hashCode();

			result = prime * result + key5.hashCode();
			result = prime * result + val5.hashCode();

			result = prime * result + key6.hashCode();
			result = prime * result + val6.hashCode();

			result = prime * result + key7.hashCode();
			result = prime * result + val7.hashCode();

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
			Map7To0Node<?, ?> that = (Map7To0Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
				return false;
			}
			if (!key4.equals(that.key4)) {
				return false;
			}
			if (!val4.equals(that.val4)) {
				return false;
			}
			if (!key5.equals(that.key5)) {
				return false;
			}
			if (!val5.equals(that.val5)) {
				return false;
			}
			if (!key6.equals(that.key6)) {
				return false;
			}
			if (!val6.equals(that.val6)) {
				return false;
			}
			if (!key7.equals(that.key7)) {
				return false;
			}
			if (!val7.equals(that.val7)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format(
							"[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(valmap, (byte) 3), key3, val3,
							recoverMask(valmap, (byte) 4), key4, val4, recoverMask(valmap, (byte) 5),
							key5, val5, recoverMask(valmap, (byte) 6), key6, val6,
							recoverMask(valmap, (byte) 7), key7, val7);
		}

	}

	private static final class Map7To1Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;
		private final K key4;
		private final V val4;
		private final K key5;
		private final V val5;
		private final K key6;
		private final V val6;
		private final K key7;
		private final V val7;
		private final CompactMapNode<K, V> node1;

		Map7To1Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3, final K key4, final V val4, final K key5, final V val5,
						final K key6, final V val6, final K key7, final V val7,
						final CompactMapNode<K, V> node1) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.key5 = key5;
			this.val5 = val5;
			this.key6 = key6;
			this.val6 = val6;
			this.key7 = key7;
			this.val7 = val7;
			this.node1 = node1;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return ArrayIterator.of(node1);
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3, key4,
							val4, key5, val5, key6, val6, key7, val7 });
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
			return 7;
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
		CompactMapNode<K, V> getNode(int index) {
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
			case 3:
				return key4;
			case 4:
				return key5;
			case 5:
				return key6;
			case 6:
				return key7;
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
			case 4:
				return val5;
			case 5:
				return val6;
			case 6:
				return val7;
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
			case 4:
				return entryOf(key5, val5);
			case 5:
				return entryOf(key6, val6);
			case 6:
				return entryOf(key7, val7);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key7, val7, node1);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3, key4,
								val4, key5, val5, key6, val6, key7, val7, node1);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val, key4,
								val4, key5, val5, key6, val6, key7, val7, node1);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val, key5, val5, key6, val6, key7, val7, node1);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val, key6, val6, key7, val7, node1);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val, key7, val7, node1);
			case 6:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key7, val, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3,
								key4, val4, key5, val5, key6, val6, key7, val7, node1);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3,
								key4, val4, key5, val5, key6, val6, key7, val7, node1);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3,
								key4, val4, key5, val5, key6, val6, key7, val7, node1);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val,
								key4, val4, key5, val5, key6, val6, key7, val7, node1);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key, val, key5, val5, key6, val6, key7, val7, node1);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key, val, key6, val6, key7, val7, node1);
			case 6:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key, val, key7, val7, node1);
			case 7:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key7, val7, key, val, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
								val5, key6, val6, key7, val7, node1);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
								val5, key6, val6, key7, val7, node1);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
								val5, key6, val6, key7, val7, node1);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
								val5, key6, val6, key7, val7, node1);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key6, val6, key7, val7, node1);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key7, val7, node1);
			case 6:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, node1);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key7, val7, node);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			final int bitIndex = nodeIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;

			switch (bitIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key7, val7);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
									val5, key6, val6, key7, val7, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
									val5, key6, val6, key7, val7, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
									val5, key6, val6, key7, val7, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
									val5, key6, val6, key7, val7, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
									val5, key6, val6, key7, val7, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
									val5, key6, val6, key7, val7, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
									val5, key6, val6, key7, val7, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
									val5, key6, val6, key7, val7, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key6, val6, key7, val7, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key6, val6, key7, val7, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key7, val7, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key7, val7, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 6:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key6, val6, node, node1);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key6, val6, node1, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = nodeIndex(bitpos);
			final int valIndex = valIndex(bitpos);

			final int valmap = this.valmap | bitpos;

			final K key = node.headKey();
			final V val = node.headVal();

			switch (bitIndex) {
			case 0:
				switch (valIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3,
									val3, key4, val4, key5, val5, key6, val6, key7, val7);
				case 1:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3,
									val3, key4, val4, key5, val5, key6, val6, key7, val7);
				case 2:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3,
									val3, key4, val4, key5, val5, key6, val6, key7, val7);
				case 3:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key,
									val, key4, val4, key5, val5, key6, val6, key7, val7);
				case 4:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key, val, key5, val5, key6, val6, key7, val7);
				case 5:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key, val, key6, val6, key7, val7);
				case 6:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key6, val6, key, val, key7, val7);
				case 7:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key6, val6, key7, val7, key, val);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3,
							key4, val4, key5, val5, key6, val6, key7, val7, node1 }, (byte) 7);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();

			result = prime * result + key4.hashCode();
			result = prime * result + val4.hashCode();

			result = prime * result + key5.hashCode();
			result = prime * result + val5.hashCode();

			result = prime * result + key6.hashCode();
			result = prime * result + val6.hashCode();

			result = prime * result + key7.hashCode();
			result = prime * result + val7.hashCode();

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
			Map7To1Node<?, ?> that = (Map7To1Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
				return false;
			}
			if (!key4.equals(that.key4)) {
				return false;
			}
			if (!val4.equals(that.val4)) {
				return false;
			}
			if (!key5.equals(that.key5)) {
				return false;
			}
			if (!val5.equals(that.val5)) {
				return false;
			}
			if (!key6.equals(that.key6)) {
				return false;
			}
			if (!val6.equals(that.val6)) {
				return false;
			}
			if (!key7.equals(that.key7)) {
				return false;
			}
			if (!val7.equals(that.val7)) {
				return false;
			}
			if (!node1.equals(that.node1)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format(
							"[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(valmap, (byte) 3), key3, val3,
							recoverMask(valmap, (byte) 4), key4, val4, recoverMask(valmap, (byte) 5),
							key5, val5, recoverMask(valmap, (byte) 6), key6, val6,
							recoverMask(valmap, (byte) 7), key7, val7,
							recoverMask(bitmap ^ valmap, (byte) 1), node1);
		}

	}

	private static final class Map8To0Node<K, V> extends CompactMapNode<K, V> {
		private final int bitmap;
		private final int valmap;
		private final K key1;
		private final V val1;
		private final K key2;
		private final V val2;
		private final K key3;
		private final V val3;
		private final K key4;
		private final V val4;
		private final K key5;
		private final V val5;
		private final K key6;
		private final V val6;
		private final K key7;
		private final V val7;
		private final K key8;
		private final V val8;

		Map8To0Node(final AtomicReference<Thread> mutator, final int bitmap, final int valmap,
						final K key1, final V val1, final K key2, final V val2, final K key3,
						final V val3, final K key4, final V val4, final K key5, final V val5,
						final K key6, final V val6, final K key7, final V val7, final K key8,
						final V val8) {
			super(mutator, bitmap, valmap);
			this.bitmap = bitmap;
			this.valmap = valmap;
			this.key1 = key1;
			this.val1 = val1;
			this.key2 = key2;
			this.val2 = val2;
			this.key3 = key3;
			this.val3 = val3;
			this.key4 = key4;
			this.val4 = val4;
			this.key5 = key5;
			this.val5 = val5;
			this.key6 = key6;
			this.val6 = val6;
			this.key7 = key7;
			this.val7 = val7;
			this.key8 = key8;
			this.val8 = val8;

			assert nodeInvariant();
		}

		@Override
		Iterator<CompactMapNode<K, V>> nodeIterator() {
			return Collections.<CompactMapNode<K, V>> emptyIterator();
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
			return ArrayKeyValueIterator.of(new Object[] { key1, val1, key2, val2, key3, val3, key4,
							val4, key5, val5, key6, val6, key7, val7, key8, val8 });
		}

		@Override
		boolean hasPayload() {
			return true;
		}

		@Override
		int payloadArity() {
			return 8;
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
		CompactMapNode<K, V> getNode(int index) {
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
			case 4:
				return key5;
			case 5:
				return key6;
			case 6:
				return key7;
			case 7:
				return key8;
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
			case 4:
				return val5;
			case 5:
				return val6;
			case 6:
				return val7;
			case 7:
				return val8;
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
			case 4:
				return entryOf(key5, val5);
			case 5:
				return entryOf(key6, val6);
			case 6:
				return entryOf(key7, val7);
			case 7:
				return entryOf(key8, val8);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetValue(AtomicReference<Thread> mutator, int index, V val) {
			switch (index) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key1, val, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key7, val7, key8, val8);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val, key3, val3, key4,
								val4, key5, val5, key6, val6, key7, val7, key8, val8);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val, key4,
								val4, key5, val5, key6, val6, key7, val7, key8, val8);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val, key5, val5, key6, val6, key7, val7, key8, val8);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val, key6, val6, key7, val7, key8, val8);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val, key7, val7, key8, val8);
			case 6:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key7, val, key8, val8);
			case 7:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key7, val7, key8, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndInsertValue(AtomicReference<Thread> mutator, int bitpos, K key,
						V val) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap | bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key, val, key1, val1, key2, val2, key3, val3,
								key4, val4, key5, val5, key6, val6, key7, val7, key8, val8);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key, val, key2, val2, key3, val3,
								key4, val4, key5, val5, key6, val6, key7, val7, key8, val8);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key, val, key3, val3,
								key4, val4, key5, val5, key6, val6, key7, val7, key8, val8);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key, val,
								key4, val4, key5, val5, key6, val6, key7, val7, key8, val8);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key, val, key5, val5, key6, val6, key7, val7, key8, val8);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key, val, key6, val6, key7, val7, key8, val8);
			case 6:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key, val, key7, val7, key8, val8);
			case 7:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key7, val7, key, val, key8, val8);
			case 8:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key7, val7, key8, val8, key, val);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveValue(AtomicReference<Thread> mutator, int bitpos) {
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap & ~bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
								val5, key6, val6, key7, val7, key8, val8);
			case 1:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
								val5, key6, val6, key7, val7, key8, val8);
			case 2:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
								val5, key6, val6, key7, val7, key8, val8);
			case 3:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
								val5, key6, val6, key7, val7, key8, val8);
			case 4:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key6, val6, key7, val7, key8, val8);
			case 5:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key7, val7, key8, val8);
			case 6:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key8, val8);
			case 7:
				return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
								val4, key5, val5, key6, val6, key7, val7);
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndSetNode(AtomicReference<Thread> mutator, int index,
						CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndRemoveNode(AtomicReference<Thread> mutator, int bitpos) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromInlineToNode(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			final int bitIndex = Integer.bitCount(((bitmap | bitpos) ^ (valmap & ~bitpos))
							& (bitpos - 1));
			final int valIndex = valIndex(bitpos);

			final int bitmap = this.bitmap | bitpos;
			final int valmap = this.valmap & ~bitpos;

			switch (valIndex) {
			case 0:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key2, val2, key3, val3, key4, val4, key5,
									val5, key6, val6, key7, val7, key8, val8, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 1:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key3, val3, key4, val4, key5,
									val5, key6, val6, key7, val7, key8, val8, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 2:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key4, val4, key5,
									val5, key6, val6, key7, val7, key8, val8, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 3:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key5,
									val5, key6, val6, key7, val7, key8, val8, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 4:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key6, val6, key7, val7, key8, val8, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 5:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key7, val7, key8, val8, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 6:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key6, val6, key8, val8, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			case 7:
				switch (bitIndex) {
				case 0:
					return valNodeOf(mutator, bitmap, valmap, key1, val1, key2, val2, key3, val3, key4,
									val4, key5, val5, key6, val6, key7, val7, node);
				default:
					throw new IllegalStateException("Index out of range.");
				}
			default:
				throw new IllegalStateException("Index out of range.");
			}
		}

		@Override
		CompactMapNode<K, V> copyAndMigrateFromNodeToInline(AtomicReference<Thread> mutator,
						int bitpos, CompactMapNode<K, V> node) {
			throw new IllegalStateException("Index out of range.");
		}

		@Override
		CompactMapNode<K, V> convertToGenericNode() {
			return valNodeOf(null, bitmap, valmap, new Object[] { key1, val1, key2, val2, key3, val3,
							key4, val4, key5, val5, key6, val6, key7, val7, key8, val8 }, (byte) 8);
		}

		@Override
		byte sizePredicate() {
			return SIZE_MORE_THAN_ONE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bitmap;
			result = prime * result + valmap;

			result = prime * result + key1.hashCode();
			result = prime * result + val1.hashCode();

			result = prime * result + key2.hashCode();
			result = prime * result + val2.hashCode();

			result = prime * result + key3.hashCode();
			result = prime * result + val3.hashCode();

			result = prime * result + key4.hashCode();
			result = prime * result + val4.hashCode();

			result = prime * result + key5.hashCode();
			result = prime * result + val5.hashCode();

			result = prime * result + key6.hashCode();
			result = prime * result + val6.hashCode();

			result = prime * result + key7.hashCode();
			result = prime * result + val7.hashCode();

			result = prime * result + key8.hashCode();
			result = prime * result + val8.hashCode();

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
			Map8To0Node<?, ?> that = (Map8To0Node<?, ?>) other;

			if (bitmap != that.bitmap) {
				return false;
			}
			if (valmap != that.valmap) {
				return false;
			}

			if (!key1.equals(that.key1)) {
				return false;
			}
			if (!val1.equals(that.val1)) {
				return false;
			}
			if (!key2.equals(that.key2)) {
				return false;
			}
			if (!val2.equals(that.val2)) {
				return false;
			}
			if (!key3.equals(that.key3)) {
				return false;
			}
			if (!val3.equals(that.val3)) {
				return false;
			}
			if (!key4.equals(that.key4)) {
				return false;
			}
			if (!val4.equals(that.val4)) {
				return false;
			}
			if (!key5.equals(that.key5)) {
				return false;
			}
			if (!val5.equals(that.val5)) {
				return false;
			}
			if (!key6.equals(that.key6)) {
				return false;
			}
			if (!val6.equals(that.val6)) {
				return false;
			}
			if (!key7.equals(that.key7)) {
				return false;
			}
			if (!val7.equals(that.val7)) {
				return false;
			}
			if (!key8.equals(that.key8)) {
				return false;
			}
			if (!val8.equals(that.val8)) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format(
							"[@%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s, @%d: %s=%s]",
							recoverMask(valmap, (byte) 1), key1, val1, recoverMask(valmap, (byte) 2),
							key2, val2, recoverMask(valmap, (byte) 3), key3, val3,
							recoverMask(valmap, (byte) 4), key4, val4, recoverMask(valmap, (byte) 5),
							key5, val5, recoverMask(valmap, (byte) 6), key6, val6,
							recoverMask(valmap, (byte) 7), key7, val7, recoverMask(valmap, (byte) 8),
							key8, val8);
		}

	}

}
