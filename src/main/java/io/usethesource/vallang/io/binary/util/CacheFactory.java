/** 
 * Copyright (c) 2016, Davy Landman, Centrum Wiskunde & Informatica (CWI) 
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
package io.usethesource.vallang.io.binary.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Since we are constructing and deconstructing a lot of windows, use this factory to build them.
 * For caching reasons, also return the windows to this factory, so they can be reused again.
 *
 */
public class CacheFactory<T> {
	
	
	// or when more memory is needed.
	private static final class LastUsedTracker<T> extends SoftReference<T> {
        private final long lastUsed;

        public LastUsedTracker(T obj, ReferenceQueue<T> queue) {
            super(obj, queue);
	        this.lastUsed = System.nanoTime();
        } 
        
        public boolean clearIfOlderThan(long timeStamp) {
            if (timeStamp > lastUsed) {
                clear();
                return true;
            }
            return false;
        }
	}
	
	private static final class SoftPool<T> {
	    private final Deque<LastUsedTracker<T>> dequeue = new ConcurrentLinkedDeque<>();
	    private final ReferenceQueue<T> references = new ReferenceQueue<>();
	    
	    public void performHouseKeeping() {
	        synchronized (references) {
	            Object cleared;
	            while ((cleared = references.poll()) != null) {
	                dequeue.removeLastOccurrence(cleared);
	            }
            }
	    }

        public @Nullable SoftReference<T> poll() {
            return dequeue.poll();
        }

        public void push(T o) {
            dequeue.push(new LastUsedTracker<>(o, references));
        }

        public Iterator<LastUsedTracker<T>> descendingIterator() {
            return dequeue.descendingIterator();
        }
	}

	private final Map<Integer, SoftPool<T>> caches = new ConcurrentHashMap<>();
	private final Function<T, T> cleaner;
	private final long expireNanos;

	public CacheFactory(int expireAfter, TimeUnit unit, Function<T, T> clearer) {
	    this.expireNanos = unit.toNanos(expireAfter);
		this.cleaner = clearer;
		registerInstance(this);
	}

	/**
	 * Private internal function, to be called from the cleanup thread
	 */
	private void cleanup() {
		long cleanBefore = System.nanoTime() - expireNanos;
		for (SoftPool<T> v : caches.values()) {
			Iterator<LastUsedTracker<T>> it = v.descendingIterator();
			while (it.hasNext()) {
				LastUsedTracker<T> current = it.next();
				if (current.clearIfOlderThan(cleanBefore)) {
					it.remove();
				}
				else {
					break; // end of the chain of outdated stuff reached
				}
			}
			v.performHouseKeeping();
		}
	}
	
	public @NonNull T get(int size, Function<Integer, @NonNull T> computeNew) {
        SoftPool<T> reads = caches.computeIfAbsent(size, i -> new SoftPool<>());
        SoftReference<T> tracker;
        while ((tracker = reads.poll()) != null) {
            T result = tracker.get();
            if (result != null) {
            	return result;
            }
        }
        return computeNew.apply(size);
	}
	
	public void put(int size, T returned) {
	    if (returned != null) {
	    	returned = cleaner.apply(returned);
            SoftPool<T> entries = caches.computeIfAbsent(size, i -> new SoftPool<>());
            entries.push(returned);
	    }
	}
	

    private static final ConcurrentLinkedQueue<WeakReference<CacheFactory<?>>> CLEANUP_CACHES = new ConcurrentLinkedQueue<>();


    @SuppressWarnings({"argument", "arguments"}) // passed in reference might not be completly initialized
    private static void registerInstance(@UnknownInitialization CacheFactory<?> cache) {
        CLEANUP_CACHES.add(new WeakReference<>(cache));
    }

    static {
        cleanupRunner(); // start the scheduler
    }

    private static void cleanupRunner() {
        CompletableFuture
            .delayedExecutor(1, TimeUnit.SECONDS)
            .execute(CacheFactory::cleanupRunner);
        try {
            Iterator<WeakReference<CacheFactory<?>>> it = CLEANUP_CACHES.iterator();
            while (it.hasNext()) {
                CacheFactory<?> cur = it.next().get();
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
