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
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.StampedLock;

/**
 * <p>
 * 	   A container specifically designed for hash consing, entries are tracked with WeakReferences, to avoid leaking memory.
 *     The cache is thread safe, meaning the {@link #get} method can be called without any locking requirements.
 * 	   Multiple insertions can occur concurrently while atomicity is guaranteed. The container only locks over all threads during a resize.
 *     With the following guarantees: 
 * <ul>
 *    <li> As long as there are strong references to the key, all calls to {@link #get} will return the same reference for something that {@link Object#equals} key (not just reference equality, as Caffeine and WeakHashMap do)
 *    <li> There will only be one entry for a given key, there is no point in time where you can get two different references for the same key (as long as the entry wasn't cleared because there was no strong reference to the key between those two calls)
 *    <li> If the key is in the cache, retrieving the value will never block the code.
 *    <li> If the key is *not* in the cache, and multiple threads call {@link #get} for the same key, one of those references will be added and that reference returned to all these concurrent calls.
 *    <li> If the key is *not* in the cache, the {@link #get} rarely blocks, only in case of a resize (which happens at 80% fill rate, and grows/shrinks exponentially)
 * </ul>
 * @author Davy Landman
 */
public class WeakBarelyLockingHashConsingSet<T> implements HashConsingMap<T> {

    /**
     * <p>
     * 		Buckets of the Hash Table, every bucket is the start of a linked list of hash collision nodes.
     * </p>
     * 
     * <p>
     *		New entries are only added to the head of the bucket, so by comparing the reference at the head of the bucket, you know the chain has not been extended (cleanup can remove intermediate nodes, but that is not a problem for lookup or insertion) 
     * </p>
     */
    private volatile AtomicReferenceArray<WeakNode<T>> table;

    /**
     * Read write lock around the reference to the table. Read lock when you want to insert stuff into the table, write lock when you want to change the reference of the table (only happens during resize).
     */
    private final StampedLock lock = new StampedLock();

    /**
     * Keeps track of how many items are in the table, can temporarily be out of sync with the table.
     */
    private volatile int count = 0;

    /**
     * The GC notifies us about references that are cleared via this queue. Polling is cheap (just a volatile read) if it is empty.
     */
    private final ReferenceQueue<T> cleared = new ReferenceQueue<>();

    private static final int MINIMAL_CAPACITY = 1 << 4;
    private static final int MAX_CAPACITY = 1 << 30;


    public WeakBarelyLockingHashConsingSet() {
        this(MINIMAL_CAPACITY);
    }
    /**
     * Construct a new WeakReference cache. 
     * @param initialCapacity the size of the cache to start with. will be rounded up towards the closest power of two.
     */
    public WeakBarelyLockingHashConsingSet(int initialCapacity) {
        table = new AtomicReferenceArray<>(nextPowerOfTwo(initialCapacity));
        Cleanup.register(this);
    }

    private static int nextPowerOfTwo(int capacity) {
        return Integer.highestOneBit(capacity - 1) << 1;
    }
    /**
     * Lookup key in the cache, if it's not in there, put it there, atomically. 
     * @param key lookup something that is equal to this key in the cache.
     * @return the stored entry for this key, or this key
     */
    @Override
    public T get(T key) {
        if (key == null) {
            throw new IllegalArgumentException("No null keys allowed");
        }

        // We do a dirty read on the table, it could be that it's being resized, but that doesn't matter.
        // We are just doing a quick lookup first, to try to see if it is the current table, 
        // Even during resizes, the current table remains valid to read from, it's only inserting that's not supported during resize.
       
        int hash = key.hashCode();
        AtomicReferenceArray<WeakNode<T>> table = this.table; // read the table once, to assure that the next operations all work on the same table
        int bucket = bucket(hash, table.length());
        WeakNode<T> bucketHead = table.get(bucket); 
        T found = lookup(key, hash, bucketHead, null);
        if (found != null) {
            return found;
        }

        // not found, so let's try to insert it, this will acquire a read lock on the table, so it can block if on another thread a resizes is happening.
        // otherwise, nothing wrong, multiple inserts can happen next to each other
        return insert(key, hash, bucketHead);
    }

    private static int bucket(int hash, int tableLength) {
        // since we are only using the last bits, take the msb and add them to the mix
        return (hash ^ (hash >> 16)) % tableLength;
    }

    /**
     * Insert the value if still possible (can be a race between another insert for the same key). The entry that won the race (to insert itself in the hash collision chain) will be returned.
     * @param key the key entry
     * @param hash hash of the key
     * @param notFoundIn head of the hash collision chain where it should be inserted at, used to avoid a double lookup
     * @param value new value to insert
     * @return value that is now in the hash table
     */
    private T insert(final T key, final int hash, WeakNode<T> notFoundIn) {
        resize(); // check if we might need to grow or shrink
        final WeakNode<T> toInsert = new WeakNode<>(key, hash, cleared);
        while (true) {
            final AtomicReferenceArray<WeakNode<T>> table = this.table; // we copy the table field once per iteration, the read lock at the end checks if it has changed since then
            int bucket = bucket(hash, table.length());
            WeakNode<T> currentBucketHead = table.get(bucket);
            if (currentBucketHead != notFoundIn) {
                // the head of the chain has changed, so it might be that now the key is there, so we have to lookup again, but we stop when we find the old head again (or null)
                T otherResult = lookup(key, hash, currentBucketHead, notFoundIn);
                if (otherResult != null) {
                    // we lost the race
                	toInsert.key.clear(); // release unused WeakNode
                    return otherResult;
                }
                notFoundIn = currentBucketHead;
            }
            toInsert.next.set(currentBucketHead); // we prepare our entry to be at the head of the chain

            long stamp = lock.readLock(); // we get a read lock on the table, so we can put something in it, to protect against a table resize in process
            try {
                if (table == this.table && table.compareAndSet(bucket, currentBucketHead, toInsert)) {
                    // if the table didn't change since we found our bucket and our chain
                    // and if the head of the collision chain was still was we expected, we've managed to insert ourselfs in the chain
                    // so we've won any possible races
                    count++;
                    return key;
                }
            }
            finally {
                lock.unlockRead(stamp);
            }
        }
    }

    private T lookup(T key, int hash, WeakNode<T> bucketEntry, WeakNode<T> stopAfter) {
        while (bucketEntry != null && bucketEntry != stopAfter) {
            WeakChildReference<T> ref = bucketEntry.key;
			if (ref.hash == hash) {
                T other = ref.get();
                if (other != null && key.equals(other)) {
                    return other;
                }
            }
            bucketEntry = bucketEntry.next.get();
        }
        return null;
    }

    /**
     * Try to see if there is enough space in the table, or if it should be grown/shrunk.
     * Only lock when we need to resize.
     */
    private void resize() {
        final AtomicReferenceArray<WeakNode<T>> table = this.table;
        int newSize = calculateNewSize(table);

        if (newSize != table.length()) {
            // We have to grow, so we have to get an exclusive lock on the table, so nobody is inserting/cleaning.
            long stamp = lock.writeLock();
            try {
                // now it could be that another thread also triggered a resize, so we have to make sure we are not too late
                final AtomicReferenceArray<WeakNode<T>> oldTable = this.table;
                final int oldLength = oldTable.length();
                if (oldTable != table) {
                    // someone else already changed the table, so recalculate the size, see if we still need to resize
                    newSize = calculateNewSize(oldTable);
                    if (newSize == oldLength) {
                        return;
                    }
                }
                final AtomicReferenceArray<WeakNode<T>> newTable = new AtomicReferenceArray<>(newSize);
                for (int i = 0; i < oldLength; i++) {
                    WeakNode<T> current = oldTable.get(i);
                    while (current != null) {
                        int newBucket = bucket(current.key.hash, newSize);
                        // we cannot change the old entry, as the lookups are still happening on them (as intended)
                        // so we build a new entry, that replaces the old entry for the aspect of the reference queue.
                        newTable.set(newBucket, new WeakNode<>(current.key, newTable.get(newBucket)));
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


    /**
     * Should only be called from the cleanup thread
     */
    @SuppressWarnings("unchecked")
    private void cleanup() {
        WeakChildReference<T> clearedReference = (WeakChildReference<T>) cleared.poll();
        if (clearedReference != null) {
            int totalCleared = 0;
            long stamp = lock.readLock(); // we get a read lock on the table, so we can remove some stuff, to protect against a table resize in process
            try {
                final AtomicReferenceArray<WeakNode<T>> table = this.table;
                final int currentLength = table.length();

                while (clearedReference != null) {
                	int bucket = bucket(clearedReference.hash, currentLength);
                	while (true) {
                		WeakNode<T> prev = null;
                		WeakNode<T> cur = table.get(bucket);
                		while (cur.key != clearedReference) {
                			prev = cur;
                			cur = cur.next.get();
                			assert cur != null; // we have to find entry in this bucket
                		}
                		if (prev == null) {
                			// at the head, so we can just replace the head
                			if (table.compareAndSet(bucket, cur, cur.next.get())) {
                				break; // we replaced the head, so continue
                			}
                		}
                		else {
                			if (prev.next.compareAndSet(cur, cur.next.get())) {
                				break; // managed to replace the next pointer in the chain 
                			}
                		}
                	}
                	count--;
                	totalCleared++;
                    clearedReference = (WeakChildReference<T>) cleared.poll();
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


    /**
     * A special class that takes care to periodically cleanup any references that are cleared.
     * 
     * The reason it's in a seperate thread is to avoid having the {@link #get} invocations block due to cleanup.
     * The alternative approach of only cleaning up during insertion can leave the cache quite full with stale entries if no new entries are added anymore.
     *
     */
    private static class Cleanup extends Thread {
        private final Queue<WeakReference<WeakBarelyLockingHashConsingSet<?>>> caches;

        private Cleanup() { 
            caches = new ConcurrentLinkedQueue<>();
            setDaemon(true);
            setName("Cleanup Thread for " + WeakBarelyLockingHashConsingSet.class.getName());
            start();
        }

        // Lazy initialized singleton
        private static class InstanceHolder {
            static final Cleanup INSTANCE = new Cleanup();
        }

        public static void register(WeakBarelyLockingHashConsingSet<?> cache) {
            InstanceHolder.INSTANCE.caches.add(new WeakReference<>(cache));
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
                try {
                    Iterator<WeakReference<WeakBarelyLockingHashConsingSet<?>>> it = caches.iterator();
                    while (it.hasNext()) {
                        WeakBarelyLockingHashConsingSet<?> cur = it.next().get();
                        if (cur == null) {
                            it.remove();
                        }
                        else {
                            cur.cleanup();
                        }
                    }
                }
                catch (Throwable e) {
                    System.err.println("Cleanup thread failed with: " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
        }



    }

    private int calculateNewSize(final AtomicReferenceArray<WeakNode<T>> table) {
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
        else if (newSize < MINIMAL_CAPACITY) {
            newSize = MINIMAL_CAPACITY;
        }
        return newSize;
    }


    /**
     * A weak reference that stores the hash, so we can remove it when it's cleared
     */
    private static final class WeakChildReference<T> extends WeakReference<T>  {
        private final int hash;

        public WeakChildReference(T referent, int hash, ReferenceQueue<? super T> q) {
            super(referent, q);
			this.hash = hash;
        }
    }


    /**
     * Main node of the hash table, next field constructs the overloaded chain.
     */
    private static final class WeakNode<T> {

        private final AtomicReference<WeakNode<T>> next;

        private final WeakChildReference<T> key;

        public WeakNode(T key, int hash, ReferenceQueue<? super T> q) {
            this.key = new WeakChildReference<>(key, hash, q);
            this.next = new AtomicReference<>(null);
        }

        /**
         * During a resize, we construct a new Node, but reuse the references from the old node
         */
        public WeakNode(WeakChildReference<T> key, WeakNode<T> next) {
            this.key = key;
            this.next = new AtomicReference<>(next);
        }
    }
}