package io.usethesource.vallang.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

public class WeakConcurrentHashConsingMap<T> implements HashConsingMap<T> {
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
    
    
    public WeakConcurrentHashConsingMap() {
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
        private final ConcurrentLinkedDeque<WeakReference<WeakConcurrentHashConsingMap<?>>> caches;

        private Cleanup() { 
            caches = new ConcurrentLinkedDeque<>();
            setDaemon(true);
            setName("Cleanup Thread for " + WeakConcurrentHashConsingMap.class.getName());
            start();
        }

        private static class InstanceHolder {
            static final Cleanup INSTANCE = new Cleanup();
        }

        public static void register(WeakConcurrentHashConsingMap<?> cache) {
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
                    Iterator<WeakReference<WeakConcurrentHashConsingMap<?>>> it = caches.iterator();
                    while (it.hasNext()) {
                        WeakConcurrentHashConsingMap<?> cur = it.next().get();
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
