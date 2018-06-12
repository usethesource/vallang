package io.usethesource.vallang.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ctrie based cache
 * @author Davy
 *
 */
public class WeakReferenceTrieCache<T> implements HashConsingMap<T> {
    

    @SuppressWarnings("unchecked")
    private final AtomicReference<TrieNode<T>> root = new AtomicReference<>(new NormalNode<T>(0, (AtomicReference<TrieNode<T>>[]) new AtomicReference[0]));
    private final ReferenceQueue<Object> cleared = new ReferenceQueue<>();
    
    

    @Override
    public T get(T key) {
        int hash = key.hashCode();
        return root.get().getOrInsert(key, hash, 0, root, cleared);
    }
    
    
    private static interface TrieNode<T> {
        T getOrInsert(T key, int hash, int level, AtomicReference<TrieNode<T>> parent, ReferenceQueue<? super T> cleared);
    }
    
    private static class LeafNode<T> implements TrieNode<T>  {
        private final int hash;
        private final WeakReference<T> key;
        
        public LeafNode(T key, int hash, ReferenceQueue<? super T> clearedReferences) {
            this.hash = hash;
            this.key = new WeakReference<T>(key, clearedReferences);
        }
        
        @Override
        public T getOrInsert(T key, int hash, int level, AtomicReference<TrieNode<T>> parent, ReferenceQueue<? super T> cleared) {
            return get(key, hash);
        }

        private T get(T key, int hash) {
            if (hash == this.hash) {
                T self = this.key.get();
                if (self != null && (self == key || self.equals(key))) {
                    return self;
                }
            }
            return null;
        }
        
    }

    private static class CollisionNode<T> implements TrieNode<T> {
        
        private final LeafNode<T>[] entries;

        public CollisionNode(LeafNode<T>[] entries) {
            this.entries = entries;
        }

        @Override
        public T getOrInsert(T key, int hash, int level, AtomicReference<TrieNode<T>> parent, ReferenceQueue<? super T> cleared) {
            for (LeafNode<T> n : entries) {
                T result = n.get(key, hash);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }

        public TrieNode<T> appendCollision(LeafNode<T> newEntry) {
            LeafNode<T>[] newEntries = Arrays.copyOf(entries, entries.length + 1);
            newEntries[newEntries.length - 1] = newEntry;
            return new CollisionNode<>(newEntries);
        }

    }

    
    private static class NormalNode<T> implements TrieNode<T> {
        private final int bitmap;
        private final AtomicReference<TrieNode<T>>[] data;
        
        static final int BITS_PER_LEVEL = 5;
        static final int LEVEL_MASK = ((1 << (BITS_PER_LEVEL))- 1);
        static final int BOTTOM_LEVEL = (32 / BITS_PER_LEVEL) + 1;

        static int index(int hash, int level) {
            return (hash >>> (BITS_PER_LEVEL * level)) & LEVEL_MASK;
        }

        static int offsetIfSet(int bitmap, int index) {
            assert index >= 0 && index <= 31;

            int isSet = bitmap & (0b1 << index);
            if (isSet != 0) {
                int bitmapChunk = bitmap & ((int) ((1L << (index+1)) - 1));
                return Integer.bitCount(bitmapChunk) - 1;
            }
            else {
                return -1;
            }
        }
        
        public NormalNode(int bitmap, AtomicReference<TrieNode<T>>[] data) {
            this.bitmap = bitmap;
            this.data = data;
        }
        
        
        @SuppressWarnings("unchecked")
        public static <T> TrieNode<T> build(LeafNode<T> first, LeafNode<T> second, int level) {
            if (level != BOTTOM_LEVEL) {
                int firstIndex = index(first.hash, level);
                int secondIndex = index(second.hash, level);
                AtomicReference<TrieNode<T>>[] data;
                if (firstIndex < secondIndex) {
                    data = new AtomicReference[2];
                    data[0] = new AtomicReference<>(first);
                    data[1] = new AtomicReference<>(second);
                }
                else if (firstIndex > secondIndex) {
                    data = new AtomicReference[2];
                    data[0] = new AtomicReference<>(second);
                    data[1] = new AtomicReference<>(first);
                }
                else{
                    // same index at this level
                    data = new AtomicReference[1];
                    data[0] = new AtomicReference<>(build(first, second, level + 1));
                }
                return new NormalNode<>(1 << firstIndex | 1 << secondIndex, data);
            }
            else {
                return new CollisionNode<>(new LeafNode[] { first, second });
            }
        }

        public static <T> TrieNode<T> grow(NormalNode<T> old, LeafNode<T> newNode, int newIndex) {
            @SuppressWarnings("unchecked")
            AtomicReference<TrieNode<T>>[] newData = new AtomicReference[old.data.length + 1];
            int newBitmap = old.bitmap | 1 << newIndex;
            int newOffset = offsetIfSet(newBitmap, newIndex);
            if (newOffset > 0) {
                System.arraycopy(old.data, 0, newData, 0, newOffset);
            }
            newData[newOffset] = new AtomicReference<>(newNode);
            if (newOffset + 1 < newData.length) {
                System.arraycopy(old.data, newOffset, newData, newOffset + 1, old.data.length - newOffset);
            }
            return new NormalNode<>(newBitmap, newData);
        }

        
        @Override
        public T getOrInsert(T key, int hash, int level, AtomicReference<TrieNode<T>> parent, ReferenceQueue<? super T> cleared) {
            int index = index(hash, level);
            int offset = offsetIfSet(bitmap, index);
            if (offset != -1) {
                AtomicReference<TrieNode<T>> currentNode = data[offset];
                TrieNode<T> oldLeafNode = currentNode.get();
                T result = oldLeafNode.getOrInsert(key, hash, level + 1, currentNode, cleared);
                if (result == null) {
                    // we have to insert it, and we have reached a leaf node (or a collision node), so we can expand it to either a new trie node, or a collision node
                    LeafNode<T> newLeafNode = new LeafNode<>(key, hash, cleared);
                    TrieNode<T> newTree;
                    if (oldLeafNode instanceof LeafNode) {
                        newTree = build((LeafNode<T>)oldLeafNode, newLeafNode, level + 1);
                    }
                    else if (oldLeafNode instanceof CollisionNode) {
                        newTree = ((CollisionNode<T>)oldLeafNode).appendCollision(newLeafNode);
                    }
                    else {
                        throw new RuntimeException("Unexpected leaf: " + oldLeafNode);
                    }
                    if (currentNode.compareAndSet(oldLeafNode, newTree)) {
                        return key;
                    }
                    else {
                        // failed since the tree was updated, so let's retry
                        newLeafNode.key.clear();
                        return getOrInsert(key, hash, level, parent, cleared);
                    }
                }
                return result;
            }
            else {
                // not in this level, so we replace ourself with a node that does have it 
                LeafNode<T> newLeafNode = new LeafNode<>(key, hash, cleared);
                TrieNode<T> newTree = grow(this, newLeafNode, index);
                if (parent.compareAndSet(this, newTree)) {
                    return key;
                }
                else {
                    // failed since the tree was updated, so let's retry
                    newLeafNode.key.clear();
                    return parent.get().getOrInsert(key, hash, level, parent, cleared);
                }
            }
        }
    }
}
