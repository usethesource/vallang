/** 
* Copyright (c) 2023, Davy Landman, SWAT.engineering
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
import java.util.Arrays;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
* <p>
* A Hash Consing implementation that uses weak references for old entries, to clear memory in case values are not needed anymore.
* It is safe to use in a multi-threaded context, and will always return the same reference, even in a race between multiple threads.
* Note that it uses a circular buffer for hot entries, estimating the size of the hot entries will help with the performance of the hash consing lookup
* </p>
* 
* @author Davy Landman
*/
public class WeakReferenceHashConsingBuffer<T extends @NonNull Object> implements HashConsingMap<T> {
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

    private static final class FifoEntry<T extends @NonNull Object> {
        final T value;
        volatile boolean used;

        FifoEntry(T value) {
            this.value = value;
            this.used = false;
        }
    }
    
    /** 
     *   We keep the most recently used entries in two fifo arrays, inspired by s3-fifo paper
     */
    private final FifoEntry<T>[] newEntries;
    private final FifoEntry<T>[] hotEntries;
    private volatile int newWritten = -1;
    private volatile int hotWritten = -1; 

    /** 
     * All entries are also stored in a WeakReference, this helps with clearing memory
     * if entries are not referenced anymore
     */
    private final Cache<WeakReferenceWrap<T>, T> coldEntries;
    
    
    public WeakReferenceHashConsingBuffer() {
        this(8 * 1024);
    }
    
    public WeakReferenceHashConsingBuffer(int hotSize) {
        newEntries = new FifoEntry[Math.max(hotSize / 10, 1)];
        hotEntries = new FifoEntry[hotSize];
        Arrays.fill(newEntries, null);
        Arrays.fill(hotEntries, null);

        coldEntries = Caffeine.newBuilder()
            .weakValues()
            .initialCapacity(hotSize)
            .scheduler(Scheduler.systemScheduler())
            .build();
    }
    
    
    @Override
    public T get(T key) {
        for (var e : newEntries) {
            if (e == null) {
                // unlikely, but can happen in case of an new map
                break;
            }
            if (e.value.equals(key)) {
                e.used = true;
                return e.value;
            }
        }
        for (var e : hotEntries) {
            if (e == null) {
                // reach non-used part, only reasonable in case of an empty map
                break;
            }
            if (e.value.equals(key)) {
                e.used = true;
                return e.value;
            }
        }
        T cold = coldEntries.get(new WeakReferenceWrap<>(key), k -> key);
        // after this, either we just put it in cold, or we got an old version back from
        // cold, so we are gonna put it back in the hot entries
        // note: the possible race between multiple puts is no problem, because
        // the coldEntries get will have made sure it will be of the same instance
        putHot(cold);
        return cold;
    }


    private void putHot(T newEntry) {
        final var newEntries = this.newEntries;
        final var hotEntries = this.hotEntries;

        // note that in a scenario where there are more parallel putHot calls then
        // the newEntries.length, there will be threads writing to the same
        // position in the array. In this case, some entries will get lost
        // but will always be promoted back from the coldEntries at a later point
        int insertPos = (++newWritten) % newEntries.length;
        var oldEntry = newEntries[insertPos];
        newEntries[insertPos] = new FifoEntry<>(newEntry);
        while (oldEntry != null && oldEntry.used) {
            // promote it to the hotEntries fifo
            oldEntry.used = false;
            insertPos = (++hotWritten) % hotEntries.length;
            var nextOld = hotEntries[insertPos];
            hotEntries[insertPos] = oldEntry;
            oldEntry = nextOld;
            // since we take from the front and add to the back of the
            // circular buffer we could get into a loop
            // however, we mark the `used` as false when we add them to the front
            // so in most cases, we will find a point of insertion.
            // but if the hotList is very active, we will be looping a lot here
            // so we have to take care to pick a good size
        }
    }

}
