/** 
 * Copyright (c) 2017, Davy Landman, SWAT.engineering
 * All rights reserved. 
 *  
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met: 
 *  
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 *  
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */ 
package io.usethesource.vallang.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;

/**
 * <p>
 *     A cache that stores the key as a weak reference, and optionally also the value. After either one of the references is cleared, the entry is dropped from the cache.
 *     The cache is thread safe, meaning the {@link #get} method can be called without any locking requirements.
 *     With the following guarantees: 
 * <p>
 * <ul>
 *    <li> As long as there are strong references to both the key and the value, all calls to {@link #get} will return the value reference for something that {@link Object#equals} key (not just reference equality, as Caffeine and WeakHashMap do)
 *    <li> There will only be one entry for a given key, there is no point in time where you can get two different values for the same key (as long as there was a strong reference to key between two calls)
 *    <li> If the key is in the cache, getting the value will rarely block (it can block in case the cache is being resized and at the same time some keys/values were cleared).
 * </ul>
 * 
 * <p>
 *     Warning: use this class only if you know that both the key and value will also be kept around somewhere. 
 *     Also check the {@link WeakHashMap} that keeps a strong reference to the Value, however it uses reference equality on the key.
 * </p>
 * @author Davy Landman
 *
 * @param <K>
 * @param <V>
 */
public class WeakReferenceCache<K,V> {

	private volatile AtomicReferenceArray<Entry<K,V>> table;
	private volatile int count;
	private final ReferenceQueue<Object> cleared;
	private final StampedLock lock;

	private static final int MINIMAL_CAPACITY = 1 << 4;
	private static final int MAX_CAPACITY = 1 << 30;
	private final boolean weakValues;

	public WeakReferenceCache() {
		this(true);
	}
	public WeakReferenceCache(boolean weakValues) {
		this.weakValues = weakValues;
		table = new AtomicReferenceArray<>(MINIMAL_CAPACITY);
		count = 0;
		cleared = new ReferenceQueue<>();
		lock = new StampedLock();
	}
	
	public V get(K key, Function<K, V> generateValue) {
		if (key == null) {
			throw new IllegalArgumentException();
		}
		cleanup();
		int hash = key.hashCode();
		AtomicReferenceArray<Entry<K,V>> table = this.table;
		int bucket = bucket(table.length(), hash);
		Entry<K, V> bucketHead = table.get(bucket); // just so we k
		V found = lookup(key, hash, bucketHead);
		if (found != null) {
			return found;
		}

		// not found
		resize();
		return insertIfPossible(key, hash, bucketHead, generateValue.apply(key));
	}

	private int bucket(int tableLength,int hash) {
		// since we are only using the last bits, take the msb and add them to the mix
		return (hash ^ (hash >> 16)) & (tableLength - 1);
	}

	private V insertIfPossible(final K key, final int hash, Entry<K, V> notFoundIn, final V result) {
		final Entry<K, V> toInsert = new Entry<>(key, result, hash, weakValues, cleared);
		while (true) {
			final AtomicReferenceArray<Entry<K, V>> table = this.table;
			int bucket = bucket(table.length(), hash);
			Entry<K, V> currentBucketHead = table.get(bucket);
			if (currentBucketHead != notFoundIn) {
				// the head of the chain has changed, so it might be that now the key is there, so we have to lookup again
				V otherResult = lookup(key, hash, currentBucketHead);
				if (otherResult != null) {
					return otherResult;
				}
				notFoundIn = currentBucketHead;
			}
			toInsert.next.set(currentBucketHead);
			
			long stamp = lock.readLock(); // we get a read lock on the table, so we can put something in it, to protect against a table resize in process
			try {
                if (table == this.table && table.compareAndSet(bucket, currentBucketHead, toInsert)) {
                	count++;
                    return result;
                }
			}
			finally {
				lock.unlockRead(stamp);
			}
		}
	}

	private V lookup(K key, int hash, Entry<K, V> bucketEntry) {
		while (bucketEntry != null) {
			if (bucketEntry.hash == hash) {
				Object other = bucketEntry.key.get();
				if (other != null && key.equals(other)) {
					@SuppressWarnings("unchecked")
					V result = (V) bucketEntry.value.get();
					if (result != null) {
						return result;
					}
				}
			}
			bucketEntry = bucketEntry.next.get();
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private void cleanup() {
		WeakChild<Entry<K,V>> clearedReference = (WeakChild<Entry<K, V>>) cleared.poll();
		if (clearedReference != null) {
			int totalCleared = 0;
			long stamp = lock.readLock(); // we get a read lock on the table, so we can remove some stuff, to protect against a table resize in process
			try {
				// quickly consumed the whole cleared pool: to avoid too many threads trying to do a cleanup 
				List<WeakChild<Entry<K,V>>> toClear = new ArrayList<>();
				while (clearedReference != null) {
					toClear.add(clearedReference);
					clearedReference = (WeakChild<Entry<K,V>>) cleared.poll();
				}

				final AtomicReferenceArray<Entry<K, V>> table = this.table;
				final int currentLength = table.length();
				for (WeakChild<Entry<K,V>> e: toClear) {
					Entry<K, V> mapNode = e.getParent();
					if (mapNode != null) {
                        // avoid multiple threads clearing the same entry (for example the key and value both got cleared)
						synchronized (mapNode) {
							mapNode = e.getParent(); // make sure it wasn't cleared since we got the lock
							if (mapNode != null) {

								int bucket = bucket(currentLength, mapNode.hash);
								while (true) {
                                    Entry<K,V> prev = null;
                                    Entry<K,V> cur = table.get(bucket);
                                    while (cur != mapNode) {
                                        prev = cur;
                                        cur = cur.next.get();
                                        assert cur != null; // we have to find entry in this bucket
                                    }
                                    if (prev == null) {
                                        // at the head, so we can just replace the head
                                        if (table.compareAndSet(bucket, mapNode, mapNode.next.get())) {
                                        	break; // we replaced the head, so continue
                                        }
                                    }
                                    else {
                                    	if (prev.next.compareAndSet(mapNode, mapNode.next.get())) {
                                    		break; // managed to replace the next pointer in the chain 
                                    	}
                                    }
								}
								count--;
								totalCleared++;
								// keep the next pointer intact, in case someone is following this chain.
								// we do clear the rest
								e.parent = null;
								mapNode.key.clear();
								mapNode.value.clear();
							}

						}

					}
				}
			}
			finally {
				lock.unlockRead(stamp);
			}
			if (totalCleared > 1024) {
				// let's check for a resize
				resize();
			}
		}
	}

	private void resize() {
		final AtomicReferenceArray<Entry<K, V>> table = this.table;
		int newSize = calculateNewSize(table);

		if (newSize != table.length()) {
			// We have to grow, so we have to lock the table against new inserts.
			long stamp = lock.writeLock();
			try {
				final AtomicReferenceArray<Entry<K, V>> oldTable = this.table;
				final int oldLength = oldTable.length();
				if (oldTable != table) {
					// someone else already changed the table
					newSize = calculateNewSize(oldTable);
					if (newSize == oldLength) {
						return;
					}
				}
				final AtomicReferenceArray<Entry<K,V>> newTable = new AtomicReferenceArray<>(newSize);
				for (int i = 0; i < oldLength; i++) {
					Entry<K,V> current = oldTable.get(i);
					while (current != null) {
						int newBucket = bucket(newSize, current.hash);
						newTable.set(newBucket, new Entry<>(current.key, current.value, current.hash, newTable.get(newBucket)));
						current = current.next.get();
					}
				}
				this.table = newTable;
			}
			finally {
				lock.unlockWrite(stamp);
			}
		}
	}

	private int calculateNewSize(final AtomicReferenceArray<Entry<K, V>> table) {
		int newSize = table.length();
		int newCount = this.count + 1;
		if (newCount > newSize * 0.8) {
			newSize <<= 1;
		}
		else if (newSize != MINIMAL_CAPACITY && newCount < (newSize >> 2)) {
			// shrank quite a bit, so it makes sens to resize
			// find the smallest next power for the 
			newSize = Integer.highestOneBit(newCount - 1) << 1;
		}

		if (newSize < 0 || newSize > MAX_CAPACITY) {
			newSize = MAX_CAPACITY;
		}
		else if (newSize == 0) {
			newSize = MINIMAL_CAPACITY;
		}
		return newSize;
	}

	
	private abstract static interface EntryChild<P> {
		Object get();
		void clear();
		P getParent();
		void setParent(P parent);
	}

	

	
	private static final class WeakChild<P> extends WeakReference<Object> implements EntryChild<P>  {
		private volatile P parent;

		public WeakChild(Object referent, P parent, ReferenceQueue<? super Object> q) {
			super(referent, q);
			this.parent = parent;
		}

		@Override
		public P getParent() {
			return parent;
		}

		@Override
		public void setParent(P parent) {
			this.parent = parent;
		}
	}

	private static final class StrongChild<P> implements EntryChild<P> {
		private volatile Object ref;

		public StrongChild(Object referent) {
			this.ref = referent;
		}
		
		@Override
		public Object get() {
			return ref;
		}
		
		@Override
		public void clear() {
			ref = null;
		}
		@Override
		public P getParent() {
			throw new RuntimeException("Should never be called");
		}
		@Override
		public void setParent(P parent) {
			// noop to make the code simpeler
		}
	}
	

	private static final class Entry<K, V> {
		private final int hash;

		private final AtomicReference<Entry<K,V>> next;

		private final EntryChild<Entry<K,V>> key;
		private final EntryChild<Entry<K,V>> value;

		public Entry(K key, V value, int hash, boolean weakValues, ReferenceQueue<? super Object> q) {
			this.hash = hash;
			this.key = new WeakChild<>(key, this, q);
			if (weakValues) {
				this.value = key == value ? this.key : new WeakChild<>(value, this, q); // safe a reference in case of identity cache
			}
			else {
				this.value = new StrongChild<>(value);
			}
			this.next = new AtomicReference<>(null);
		}

		public Entry(EntryChild<Entry<K,V>> key, EntryChild<Entry<K,V>> value, int hash, Entry<K,V> next) {
			this.hash = hash;
			this.key = key;
			this.value = value;
			this.next = new AtomicReference<>(next);
			key.setParent(this);
			value.setParent(this);
		}
	}
}
