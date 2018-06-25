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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

public class WeakLockFreeHashConsingMap<T> implements HashConsingMap<T> {
    private static class WeakReferenceWrap<T> extends WeakReference<T> {
        private final int hash;

        public WeakReferenceWrap(int hash, T referent, ReferenceQueue<? super T> q) {
            super(referent, q);
            this.hash = hash;
        }
        
        @Override
        public int hashCode() {
            return hash;
        }
        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object obj) {
            if (obj == null || hash != obj.hashCode() ) {
                return false;
            }
            // same hash, so have to get the reference
            T self = get();
            if (self == null) {
                return false;
            }
            T other;
            if ((obj instanceof WeakReferenceWrap<?>)) {
                other = ((WeakReferenceWrap<T>) obj).get();
            }
            else {
                other = (T) obj;
            }
            return other != null && other.equals(self);
        }
    }
    

    private final ConcurrentMap<WeakReferenceWrap<T>,WeakReferenceWrap<T>> data = new ConcurrentHashMap<>();
    private final ReferenceQueue<T> cleared = new ReferenceQueue<>();
    
    
    public WeakLockFreeHashConsingMap() {
        Cleanup.register(this);
    }
    

    @Override
    public T get(T key) {
        int hash = key.hashCode();
        WeakReferenceWrap<T> keyLookup = new WeakReferenceWrap<>(hash, key, cleared);
        while (true) {
            WeakReferenceWrap<T> result = data.merge(keyLookup, keyLookup, (oldValue, newValue) -> oldValue.get() != null ? oldValue : newValue);
            T actualResult = result.get();
            if (actualResult != null) {
                if (result != keyLookup) {
                    keyLookup.clear();
                }
                return actualResult;
            }
        }
    }

    private void cleanup() {
        WeakReferenceWrap<?> c;
        while ((c = (WeakReferenceWrap<?>) cleared.poll()) != null) {
            data.remove(c);
        }
        
    }

    private static class Cleanup extends Thread {
        private final ConcurrentLinkedDeque<WeakReference<WeakLockFreeHashConsingMap<?>>> caches;

        private Cleanup() { 
            caches = new ConcurrentLinkedDeque<>();
            setDaemon(true);
            setName("Cleanup Thread for " + WeakLockFreeHashConsingMap.class.getName());
            start();
        }

        private static class InstanceHolder {
            static final Cleanup INSTANCE = new Cleanup();
        }

        public static void register(WeakLockFreeHashConsingMap<?> cache) {
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
                    Iterator<WeakReference<WeakLockFreeHashConsingMap<?>>> it = caches.iterator();
                    while (it.hasNext()) {
                        WeakLockFreeHashConsingMap<?> cur = it.next().get();
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


}
