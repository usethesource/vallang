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

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
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
    private static class WeakReferenceWrap<T extends @NonNull Object> extends WeakReference<T> {
        private final int hash;
        
        public WeakReferenceWrap(T referent) {
            super(referent);
            this.hash = referent.hashCode();
        }
        
        @Override
        public int hashCode() {
            return hash;
        }
        
        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof WeakReferenceWrap) {
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
    
    /** 
     *   We keep the most recently used entries in a strong cache for quick access
     */
    private final Cache<T, T> hotEntries;
    /** 
     * All entries are also stored in a WeakReference, this helps with clearing memory
     * if entries are not referenced anymore
     */
    private final Cache<WeakReferenceWrap<T>, T> coldEntries;
    
    
    public WeakReferenceHashConsingMap() {
        this(16, (int)TimeUnit.MINUTES.toSeconds(30));
    }

    private static long simulateNanoTicks() {
        return TimeUnit.SECONDS.toNanos(SecondsTicker.current());

    }
    
    public WeakReferenceHashConsingMap(int size, int demoteAfterSeconds) {
        hotEntries = Caffeine.newBuilder()
            .ticker(WeakReferenceHashConsingMap::simulateNanoTicks)
            .expireAfterAccess(Duration.ofSeconds(demoteAfterSeconds))
            .scheduler(Scheduler.systemScheduler())
            .initialCapacity(size)
            .build();

        coldEntries = Caffeine.newBuilder()
            .weakValues()
            .initialCapacity(size)
            .scheduler(Scheduler.systemScheduler())
            .build();
    }
    
    
    @Override
    public T get(T key) {
        T hot = hotEntries.getIfPresent(key);
        if (hot != null) {
            return hot;
        }
        T cold = coldEntries.get(new WeakReferenceWrap<>(key), k -> key);
        // after this, either we just put it in cold, or we got an old version back from
        // cold, so we are gonna put it back in the hot entries
        // note: the possible race between multiple puts is no problem, because
        // the coldEntries get will have made sure it will be of the same instance
        hotEntries.put(cold, cold);
        return cold;
    }

}
