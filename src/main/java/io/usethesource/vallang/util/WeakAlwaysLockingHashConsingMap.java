package io.usethesource.vallang.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

public class WeakAlwaysLockingHashConsingMap<T> implements HashConsingMap<T> {
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
    
    
    public WeakAlwaysLockingHashConsingMap() {
        Cleanup.register(this);
    }
    

    @Override
    public T get(T key) {
    	int hash = key.hashCode();
    	WeakReferenceWrap<T> keyLookup = new WeakReferenceWrap<>(hash, key, cleared);
    	while (true) {
    		synchronized (this) {
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
        private final ConcurrentLinkedDeque<WeakReference<WeakAlwaysLockingHashConsingMap<?>>> caches;

        private Cleanup() { 
            caches = new ConcurrentLinkedDeque<>();
            setDaemon(true);
            setName("Cleanup Thread for " + WeakAlwaysLockingHashConsingMap.class.getName());
            start();
        }

        private static class InstanceHolder {
            static final Cleanup INSTANCE = new Cleanup();
        }

        public static void register(WeakAlwaysLockingHashConsingMap<?> cache) {
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
                    Iterator<WeakReference<WeakAlwaysLockingHashConsingMap<?>>> it = caches.iterator();
                    while (it.hasNext()) {
                        WeakAlwaysLockingHashConsingMap<?> cur = it.next().get();
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
