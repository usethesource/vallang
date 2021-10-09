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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import javax.management.RuntimeErrorException;
import org.checkerframework.checker.nullness.qual.NonNull;

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
    private final Map<T,WeakReference<T>> data;
    private final Object writeLock;
    
    
    public WeakWriteLockingHashConsingMap() {
        this(16);
    }
    
    public WeakWriteLockingHashConsingMap(int size) {
        data = new WeakHashMap<>(size);
        try {
            writeLock = MethodHandles.privateLookupIn(WeakHashMap.class, MethodHandles.lookup())
                .findVarHandle(WeakHashMap.class, "queue", ReferenceQueue.class)
                .get(data);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Cannot locate internal writelock", e);
        }
    }
    
    
    @Override
    public T get(T key) {
        // first we try to get the value without taking a lock, just seeing if it's there
        WeakReference<T> result = data.get(key);
        if (result != null) {
            // so it was already there, but might have been cleared.
            T actualResult = result.get();
            if (actualResult != null) {
                // the returned entry was not cleared yet
                return actualResult;
            }
        }
        // now that we know that we most likely have to insert a new mapping, we acquire a write lock
        WeakReference<T> value = new WeakReference<>(key);
        // we race against the garbage collector clearing weakreferences, not other threads trying to update the value
        while (true) {
            synchronized (writeLock) {
                result = data.merge(key, value, (oldValue, newValue) -> oldValue.get() == null ? newValue : oldValue);
            }
            if (result == value) {
                // a regular put
                return key;
            }
            else {
                // it was already in there (we lost the race for a write lock to another thread that wanted to insert the same object)
                T actualResult = result.get();
                if (actualResult != null) {
                    // value was already in there, and also still held a reference (which is true for most cases, as the condition for the merge checked the reference)
                    return actualResult;
                }
                // otherwise we try again, since in this small window between the merge and the get() the GC cleared it.
            }
        }
    }
    
}
