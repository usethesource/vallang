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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WeakReferenceFlyweightCache<T> {
	private WeakEntry<T>[] table;
	private int count;
	private int shrinkMark;
	private int growMark;
	private final ReferenceQueue<T> cleared;
	private final ReadWriteLock lock;

	private static final int INITIAL_CAPACITY = 32;
	private static final int MAX_CAPACITY = 1 << 30;

	@SuppressWarnings("unchecked")
	public WeakReferenceFlyweightCache() {
		table = new WeakEntry[INITIAL_CAPACITY];
		shrinkMark = -1;
		growMark = (int) (INITIAL_CAPACITY * 0.8);
		count = 0;
		cleared = new ReferenceQueue<>();
		lock = new ReentrantReadWriteLock(true);
	}
	
	private int bucket(int hash) {
		return hash % table.length;
	}
	
	public T getFlyweight(T key) {
		if (key == null) {
			throw new IllegalArgumentException();
		}
		cleanup();
		int hash = key.hashCode();
		lock.readLock().lock();
		try {
            T result = lookup(key, hash);
            if (result != null) {
            	return result;
            }
		}
		finally {
			lock.readLock().unlock();
		}

		lock.writeLock().lock();
		try {
			// try again, maybe someone else beat us to the write lock
            T result = lookup(key, hash);
            if (result != null) {
            	return result;
            }
            resize();
            int bucket = bucket(hash);
			// put them in front of the chain
			table[bucket] = new WeakEntry<>(key, hash, table[bucket], cleared);
			count++;
			return key;
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	private T lookup(T key, int hash) {
		int bucket = bucket(hash);
		WeakEntry<T>[] table = this.table;
		WeakEntry<T> cur = table[bucket];
		while (cur != null) {
		    if (cur.hash == hash) {
		        T result = cur.get();
		        if (result != null && result.equals(key)) {
		            return result;
		        }
		    }
		    cur = cur.next;
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private void cleanup() {
		WeakEntry<T> entry = (WeakEntry<T>) cleared.poll();
		if (entry != null) {
			lock.writeLock().lock();
			try {
				// quickly consumed the whole cleared pool: to avoid too many threads also waiting for the same write lock
				List<WeakEntry<T>> toClear = new ArrayList<>();
				while (entry != null) {
					toClear.add(entry);
                    entry = (WeakEntry<T>) cleared.poll();
				}
				for (WeakEntry<T> e: toClear) {
                    int bucket = bucket(e.hash);
                    WeakEntry<T> prev = null;
                    WeakEntry<T> cur = table[bucket];
                    while (cur != e) {
                        prev = cur;
                        cur = cur.next;
                        assert cur != null; // we have to find entry in this bucket
                    }
                    if (prev == null) {
                        table[bucket] = e.next;
                    }
                    else {
                        prev.next = e.next;
                    }
                    count--;
                }
                if (count < shrinkMark) {
                    resize();
                }
			}
            finally {
            	lock.writeLock().unlock();
            }
		}
	}

	private boolean resize() {
		int newSize = table.length;
		int newCount = this.count + 1;
		if (newCount > growMark) {
			newSize = newSize * 2;
		}
		else if (newCount < shrinkMark) {
			newSize = INITIAL_CAPACITY;
			while (newCount > (newSize * 0.8)) {
				newSize *= 2;
			}
		}
		if (newSize <= 0 || newSize > MAX_CAPACITY) {
			newSize = MAX_CAPACITY;
		}

		if (newSize != table.length) {
			WeakEntry<T>[] oldTable = table;
			@SuppressWarnings("unchecked")
			WeakEntry<T>[] newTable = new WeakEntry[newSize];
			for (WeakEntry<T> rootEntry : oldTable) {
				WeakEntry<T> cur = rootEntry;
				while (cur != null) {
					WeakEntry<T> oldNext = cur.next; // store the current next part of the chain

					// put the entry at the front of the bucket
					int newBucket = cur.hash % newSize;
					cur.next = newTable[newBucket];
					newTable[newBucket] = cur;

					cur = oldNext; // move to the next part of the old chain
				}
			}
			table = newTable;
			shrinkMark = (int) ((newSize / 2) * 0.8);
			growMark = (int) ((newSize * 2) * 0.8);
			return true;
		}
		return false;
	}

	private static final class WeakEntry<T> extends WeakReference<T> {
		private final int hash;
		private WeakEntry<T> next;

		public WeakEntry(T referent, int hash, WeakEntry<T> next, ReferenceQueue<? super T> q) {
			super(referent, q);
			this.hash = hash;
			this.next = next;
		}
	}
}
