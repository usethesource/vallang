package io.usethesource.vallang.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

public class WeakWriteLockingHashConsingMap<T> implements HashConsingMap<T> {
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
    

    private final Map<WeakReferenceWrap<T>,WeakReferenceWrap<T>> data = new HashMap<>();
    private final ReferenceQueue<T> cleared = new ReferenceQueue<>();
    
    
    public WeakWriteLockingHashConsingMap() {
        Cleanup.register(this);
    }
    

    @Override
    public T get(T key) {
        WeakReferenceWrap<T> result = data.get(key);
        if (result != null) {
        	T actualResult = result.get();
        	if (actualResult != null) {
                return actualResult;
        	}
        }
        synchronized (this) {
            WeakReferenceWrap<T> keyLookup = new WeakReferenceWrap<>(key.hashCode(), key, cleared);
        	while (true) {
        		result = data.merge(keyLookup, keyLookup, (oldValue, newValue) -> oldValue.get() == null ? newValue : oldValue);
        		if (result == keyLookup) {
        			// a regular put
        			return key;
        		}
        		else {
        			T actualResult = result.get();
        			if (actualResult != null) {
        				// value was already in there, and also still held a reference (which is true for most cases)
        				keyLookup.clear(); // avoid getting a cleared reference in the queue
        				return actualResult;
        			}
        		}
        	}
		}
    }

    private void cleanup() {
        WeakReferenceWrap<?> c = (WeakReferenceWrap<?>) cleared.poll();
        if (c != null) {
        	synchronized (this) {
        		while (c != null) {
                    data.remove(c);
        			c = (WeakReferenceWrap<?>) cleared.poll();
                }
			}
        }
    }

    private static class Cleanup extends Thread {
        private final ConcurrentLinkedDeque<WeakReference<WeakWriteLockingHashConsingMap<?>>> caches;

        private Cleanup() { 
            caches = new ConcurrentLinkedDeque<>();
            setDaemon(true);
            setName("Cleanup Thread for " + WeakWriteLockingHashConsingMap.class.getName());
            start();
        }

        private static class InstanceHolder {
            static final Cleanup INSTANCE = new Cleanup();
        }

        public static void register(WeakWriteLockingHashConsingMap<?> cache) {
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
