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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
* <p>
* A Hash Consing implementation that uses weak references for old entries, to clear memory in case values are not needed anymore.
* It is safe to use in a multi-threaded context, and will always return the same reference, even in a race between multiple threads.
* </p>
* 
* @author Davy Landman
*/
public class WeakReferenceHashConsingMap<T extends @NonNull Object> implements HashConsingMap<T> {
    /**
    * Class that adds the hash-code-equals contract on top of a weak reference
    * So that we can use them as keys in Caffeine.
    * Caffeine doesn't support weakKeys with regular equality contract.
    */
    private static class WeakReferenceWrap<T extends @NonNull Object> extends WeakReference<T>  {
        private final int hash;
        
        public WeakReferenceWrap(T referent, int hash, ReferenceQueue<? super T> cleared) {
            super(referent, cleared);
            this.hash = hash;
        }
        
        @Override
        public int hashCode() {
            return hash;
        }
        
        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof WeakReferenceWrap<?>) {
                WeakReferenceWrap<?> wrappedObj = (WeakReferenceWrap<?>) obj;
                if (wrappedObj.hash == hash) {
                    Object self = super.get();
                    if (self == null) {
                        return false;
                    }
                    Object other = wrappedObj.get();
                    return other != null && self.equals(other);
                }
            }
            return false;
        }
    }

    private static class HotEntry<T extends @NonNull Object> {
        private final T value;
        private final int hash;
        private volatile int lastUsed;

        HotEntry(T value, int hash) {
            this.value = value;
            this.hash = hash;
            lastUsed = SecondsTicker.current();
        }
    }

    private static class LookupKey<T extends @NonNull Object> {
        private final T value;
        private final int hash;

        LookupKey(T value, int hash) {
            this.value = value;
            this.hash = hash;
        }
        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof WeakReferenceWrap<?>) {
                var actual = (WeakReferenceWrap<?>)obj;
                return actual.hash == hash && value.equals(actual.get());
            }
            return false;
        }

    }

    
    /** 
     * We keep the most recently used entries in a simple open addressing map for quick access
     * In case of hash collisions, the entry is overwritten, which doesn't matter too much
     * The cleanup happens in a side thread.
     * 
     * Note that even though everything is using atomic operations, 
     * threads can have different views on the contents of the hotEntries array.
     * This is not a problem, as it acts like a thread local LRU in that case.
     * 
     * The coldEntries is the only map that should keep the state consistent across threads.
     */
    private final HotEntry<T>[] hotEntries;
    private final int mask;

    /** 
     * All entries are also stored in a WeakReference, this helps with clearing memory
     * if entries are not referenced anymore
     */
	private final ReferenceQueue<T> queue = new ReferenceQueue<T>();
    private final Map<Object, WeakReferenceWrap<T>> coldEntries;
    
    
    public WeakReferenceHashConsingMap() {
        this(16, (int)TimeUnit.MINUTES.toSeconds(30));
    }

    public WeakReferenceHashConsingMap(int size, int demoteAfterSeconds) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size should be a positive number");
        }
        // size should be a power of two
        size = Integer.highestOneBit(size - 1) << 1;
        hotEntries = new HotEntry[size]; 
        this.mask = size - 1;

        coldEntries = new ConcurrentHashMap<>(size);
        
        cleanup(demoteAfterSeconds, hotEntries, coldEntries, queue);
    }
    
    
    private static <T extends @NonNull Object> void cleanup(int demoteAfterSeconds, @Nullable HotEntry<T>[] hotEntries, Map<Object, WeakReferenceWrap<T>> coldEntries,
		ReferenceQueue<T> queue) {
        try {
            final int now = SecondsTicker.current();
            for (int i = 0; i < hotEntries.length; i++) {
                var entry = hotEntries[i];
                if (entry != null && (now - entry.lastUsed >= demoteAfterSeconds)) {
                    hotEntries[i] = null;
                }
            }
            List<WeakReferenceWrap<?>> toCleanup = new ArrayList<>();
            synchronized(queue) {
                Reference<?> cleared;
                while ((cleared = queue.poll()) != null) {
                    if (cleared instanceof WeakReferenceWrap<?>) {
                        toCleanup.add((WeakReferenceWrap<?>) cleared);
                    }
                }
            }
            toCleanup.forEach(coldEntries::remove);
        } finally {
            CompletableFuture
                .delayedExecutor(Math.max(1, demoteAfterSeconds / 10), TimeUnit.SECONDS)
                .execute(() -> cleanup(demoteAfterSeconds, hotEntries, coldEntries, queue));
        }
    }

    private static int improve(int hash) {
        // base on XXH32_avalanche from xxHash (BSD2 license, Yann Collet)
        hash ^= hash >>> 15;
        hash *= 0x85EBCA77;
        hash ^= hash >>> 13;
        hash *= 0xC2B2AE3D;
        return hash ^ (hash >>> 16);
    }

    @Override
    public T get(T key) {
        final int hash = key.hashCode();
        final int hotIndex = improve(hash) & mask;
        final var hotEntries = this.hotEntries;
        var hotEntry = hotEntries[hotIndex];
        if (hotEntry != null && hotEntry.hash == hash && hotEntry.value.equals(key)) {
            hotEntry.lastUsed = SecondsTicker.current();
            return hotEntry.value;
        }

        // fast path, it' already in cold, 
        // we avoid making a new weak reference just for search
        var fastGet = coldEntries.get(new LookupKey<>(key, hash));
        T result = fastGet == null ? null : fastGet.get();
        if (result == null) {
            // we create a weak reference once
            // and try to win putting it in
            var keyWrapped = new WeakReferenceWrap<>(key, hash, queue);
            while (result == null) {
                var winRace = coldEntries.putIfAbsent(keyWrapped, keyWrapped);
                result = winRace == null ? key : winRace.get();
            }
        }
        // after this, either we just put it in cold, or we got an old version back from
        // cold, so we are gonna put it back in the hot entries
        // note: the possible race between multiple puts is no problem, because
        // the coldEntries get will have made sure it will be of the same instance
        hotEntries[hotIndex] = new HotEntry<>(result, hash);
        return result;
    }

}
