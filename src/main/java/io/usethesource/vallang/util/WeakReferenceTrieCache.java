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
    private final TrieNode<T> root = new IntermediateNode<T>(new NormalNode<T>(0, (IntermediateNode<T>[]) new IntermediateNode[0]));
    private final ReferenceQueue<Object> cleared = new ReferenceQueue<>();
    
    

    @Override
    public T get(T key) {
        int hash = key.hashCode();
        return root.getOrInsert(key, hash, 0, null, cleared);
    }
    
    
    private static interface TrieNode<T> {
        T getOrInsert(T key, int hash, int level, IntermediateNode<T> parent, ReferenceQueue<? super T> cleared);
    }
    
    private static class LeafNode<T> implements TrieNode<T>  {
        private final int hash;
        private final WeakReference<T> key;
        
        public LeafNode(T key, int hash, ReferenceQueue<? super T> clearedReferences) {
            this.hash = hash;
            this.key = new WeakReference<T>(key, clearedReferences);
        }
        
        @Override
        public T getOrInsert(T key, int hash, int level, IntermediateNode<T> parent, ReferenceQueue<? super T> cleared) {
            TrieNode<T> newParent = null;
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
        public T getOrInsert(T key, int hash, int level, IntermediateNode<T> parent, ReferenceQueue<? super T> cleared) {
            for (LeafNode<T> n : entries) {
                T result = n.getOrInsert(key, hash, level, parent, cleared);
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
        private final IntermediateNode<T>[] data;
        
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
        
        public NormalNode(int bitmap, IntermediateNode<T>[] data) {
            this.bitmap = bitmap;
            this.data = data;
        }
        
        
        @SuppressWarnings("unchecked")
        public static <T> TrieNode<T> build(LeafNode<T> first, LeafNode<T> second, int level) {
            TrieNode<T> newNode;
            if (level != BOTTOM_LEVEL) {
                int firstIndex = index(first.hash, level);
                int secondIndex = index(second.hash, level);
                IntermediateNode<T>[] data;
                if (firstIndex < secondIndex) {
                    data = new IntermediateNode[2];
                    data[0] = new IntermediateNode<>(first);
                    data[1] = new IntermediateNode<>(second);
                }
                else if (firstIndex > secondIndex) {
                    data = new IntermediateNode[2];
                    data[0] = new IntermediateNode<>(second);
                    data[1] = new IntermediateNode<>(first);
                }
                else{
                    // same index at this level
                    data = new IntermediateNode[1];
                    data[0] = new IntermediateNode<>(build(first, second, level + 1));
                }
                return new NormalNode<>(1 << firstIndex | 1 << secondIndex, data);
            }
            else {
                return new CollisionNode<>(new LeafNode[] { first, second });
            }
        }

        public static <T> TrieNode<T> grow(NormalNode<T> old, LeafNode<T> newNode, int newIndex) {
            @SuppressWarnings("unchecked")
            IntermediateNode<T>[] newData = new IntermediateNode[old.data.length + 1];
            int newBitmap = old.bitmap | 1 << newIndex;
            int newOffset = offsetIfSet(newBitmap, newIndex);
            if (newOffset > 0) {
                System.arraycopy(old.data, 0, newData, 0, newOffset);
            }
            newData[newOffset] = new IntermediateNode<>(newNode);
            if (newOffset + 1 < newData.length) {
                System.arraycopy(old.data, newOffset, newData, newOffset + 1, old.data.length - newOffset);
            }
            return new NormalNode<>(newBitmap, newData);
        }

        
        @Override
        public T getOrInsert(T key, int hash, int level, IntermediateNode<T> parent, ReferenceQueue<? super T> cleared) {
            int index = index(hash, level);
            int offset = offsetIfSet(bitmap, index);
            if (offset != -1) {
                IntermediateNode<T> currentNode = data[offset];
                T result = currentNode.getOrInsert(key, hash, level + 1, null, cleared);
                if (result == null) {
                    // we have to insert it, and we have reached a leaf node (or a collision node), so we can expand it to either a new trie node, or a collision node
                    TrieNode<T> oldLeafNode = currentNode.actual.get();
                    LeafNode<T> newLeafNode = new LeafNode<>(key, hash, cleared);
                    TrieNode<T> newTree;
                    if (oldLeafNode instanceof LeafNode) {
                        newTree = build((LeafNode<T>)oldLeafNode, newLeafNode, level + 1);
                    }
                    else {
                        assert oldLeafNode instanceof CollisionNode;
                        newTree = ((CollisionNode<T>)oldLeafNode).appendCollision(newLeafNode);
                    }
                    if (currentNode.actual.compareAndSet(oldLeafNode, newTree)) {
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
                if (parent.replace(this, newTree)) {
                    return key;
                }
                else {
                    // failed since the tree was updated, so let's retry
                    newLeafNode.key.clear();
                    return parent.getOrInsert(key, hash, level, null, cleared);
                }
            }
        }
    }
    
    private static class IntermediateNode<T> implements TrieNode<T> {
        private final AtomicReference<TrieNode<T>> actual;
        
        public IntermediateNode(TrieNode<T> actual) {
            this.actual = new AtomicReference<>(actual);
        }
        
        @Override
        public T getOrInsert(T key, int hash, int level, IntermediateNode<T> parent, ReferenceQueue<? super T> cleared) {
            return actual.get().getOrInsert(key, hash, level, this, cleared);
        }
        
        public boolean replace(TrieNode<T> expected, TrieNode<T> newValue) {
            return actual.compareAndSet(expected, newValue);
        }
    }
}
