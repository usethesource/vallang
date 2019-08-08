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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
* <p>
* A Hash Consing implementation that only acquires a lock when the key is not in the connection.
* It is safe to use in a multi-threaded context, and will always return the same reference, even in a race between multiple threads.
* </p>
* 
* <p>
* It keeps the key inside a weak-reference, so the entries are cleared as soon as there are no more strong references to it.
* </p>
* @author Davy Landman
*/
public class WeakWriteLockingHashConsingMap<T extends @NonNull Object> implements HashConsingMap<T> {
    /**
    * Class that adds the hash-code-equals contract on top of a weak reference
    */
    private static class WeakReferenceWrap<T extends @NonNull Object> extends WeakReference<T> {
        private final int hash;
        
        public WeakReferenceWrap(int hash, T referent, ReferenceQueue<? super T> q) {
            super(referent, q);
            this.hash = hash;
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
                    Object self = get();
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
    * Special wrapper that is used for lookup, so that we don't have to create a weak-reference when we don't need it.
    * It would have been nicer if we could use the normal equals method of the target object, but that won't be able to work together with the WeakReferenceWrap.
    */
    private static final class LookupWrapper<T extends @NonNull Object> {
        private final int hash;
        private final T ref;
        public LookupWrapper(int hash, T ref) {
            this.hash = hash;
            this.ref = ref;
        }
        
        @Override
        public int hashCode() {
            return hash;
        }
        
        @Override
        public boolean equals(@Nullable Object obj) {
    // only internal use of this class

            if (obj instanceof WeakReferenceWrap) {
                WeakReferenceWrap<?> wrappedObj = (WeakReferenceWrap<?>) obj;
                if (wrappedObj.hash == hash) {
                    Object other = wrappedObj.get();
                    return other != null && ref.equals(other);
                }
            }
            return false;
}
    }
    
    
    private final Map<WeakReferenceWrap<T>,WeakReferenceWrap<T>> data;
    private final ReferenceQueue<T> cleared = new ReferenceQueue<>();
    
    
    public WeakWriteLockingHashConsingMap() {
        this(16);
    }
    
    public WeakWriteLockingHashConsingMap(int size) {
        data = new HashMap<>(size);
        Cleanup.Instance.register(this);
    }
    
    
    @Override
    public T get(T key) {
        // first we try to get the value without building a soft reference
        LookupWrapper<T> keyLookup = new LookupWrapper<>(key.hashCode(), key);
        @SuppressWarnings("unlikely-arg-type")
        WeakReferenceWrap<T> result = data.get(keyLookup);
        if (result != null) {
            T actualResult = result.get();
            if (actualResult != null) {
                // the returned entry was not cleared yet
                return actualResult;
            }
        }
        // now that we know that we most likely have to insert a new mapping, we acquire a write lock
        synchronized (this) {
            WeakReferenceWrap<T> keyPut = new WeakReferenceWrap<>(keyLookup.hash, key, cleared);
            // we race against the garbage collector clearing weakreferences, not other threads
            while (true) {
                result = data.merge(keyPut, keyPut, (oldValue, newValue) -> oldValue.get() == null ? newValue : oldValue);
                if (result == keyPut) {
                    // a regular put
                    return key;
                }
                else {
                    // it was already in there (we lost the race for a write lock to another thread that wanted to insert the same object)
                    T actualResult = result.get();
                    if (actualResult != null) {
                        // value was already in there, and also still held a reference (which is true for most cases, as the condition for the merge checked the reference)
                        keyPut.clear(); // avoid getting a cleared reference in the queue
                        return actualResult;
                    }
                }
            }
        }
    }
    
    private void cleanup() {
        // do a cheap poll
        WeakReferenceWrap<? extends @NonNull Object> c = (WeakReferenceWrap<? extends @NonNull Object>) cleared.poll();
        if (c != null) {
            // acquire a write lock only when it's needed
            synchronized (this) {
                while (c != null) {
                    data.remove(c);
                    c = (WeakReferenceWrap<? extends @NonNull Object>) cleared.poll();
                }
            }
        }
    }

    /**
     * Cleanup singleton that wraps {@linkplain CleanupThread}
     */
    private static enum Cleanup {
        Instance;
        private final CleanupThread thread;
        private Cleanup() {
            thread = new CleanupThread();
            thread.start();
        }
        public void register(@UnknownInitialization WeakWriteLockingHashConsingMap<?> cache) {
            thread.register(cache);
        }

    }

    /**
    * A special thread that tries to cleanup the containers once every second
    * 
    * This way, a get is never blocked for a long time, just to cleanup some old references.
    */
    private static class CleanupThread extends Thread {
        private final ConcurrentLinkedQueue<WeakReference<WeakWriteLockingHashConsingMap<?>>> caches = new ConcurrentLinkedQueue<>();
        
        private CleanupThread() { 
        }

        @Override
        public synchronized void start() {
            setDaemon(true);
            setName("Cleanup Thread for " + WeakWriteLockingHashConsingMap.class.getName());
            super.start();
        }

        @SuppressWarnings("initialization")
        public void register(@UnknownInitialization WeakWriteLockingHashConsingMap<?> cache) {
            caches.add(new WeakReference<>(cache));
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
                    Iterator<WeakReference<WeakWriteLockingHashConsingMap<?>>> it = caches.iterator();
                    while (it.hasNext()) {
                        WeakWriteLockingHashConsingMap<?> cur = it.next().get();
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
