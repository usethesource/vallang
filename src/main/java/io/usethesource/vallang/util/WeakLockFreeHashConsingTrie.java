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
public class WeakLockFreeHashConsingTrie<T> implements HashConsingMap<T> {
    

    private final AtomicReference<TrieNode<T>> root = new AtomicReference<>(new NormalNode0<T>());
    private final ReferenceQueue<Object> cleared = new ReferenceQueue<>();
    
    @Override
    public T get(T key) {
        int hash = key.hashCode();
        return root.get().getOrInsert(key, hash, hash, 0, root, cleared);
    }
    
    
    private static interface TrieNode<T> {
        T getOrInsert(T key, int hash, int hashLevel, int level, AtomicReference<TrieNode<T>> parent, ReferenceQueue<? super T> cleared);
        TrieNode<T> grow(LeafNode<T> newChild, int index, int level);
    }

    private abstract static class LeafNode<T> implements TrieNode<T>  {
        protected final int hash;
        public LeafNode(int hash) {
            this.hash = hash;
        }

    }
    
    private static class SingleLeafNode<T> extends LeafNode<T>  {
        private final WeakReference<T> key;
        
        public SingleLeafNode(T key, int hash, ReferenceQueue<? super T> clearedReferences) {
            super(hash);
            this.key = new WeakReference<T>(key, clearedReferences);
        }
        
        @Override
        public T getOrInsert(T key, int hash, int level, int hashLevel, AtomicReference<TrieNode<T>> parent, ReferenceQueue<? super T> cleared) {
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
        
        @SuppressWarnings("unchecked")
        @Override
        public TrieNode<T> grow(LeafNode<T> newChild, int index, int level) {
            if (this.hash == newChild.hash) {
                if (this.key.get() == null) {
                    return newChild;
                }
                return new CollisionNode<>(hash, (LeafNode<T>[])new LeafNode[] { this, newChild });
            }
            else {
                return NormalNode.build(this, newChild, level + 1);
            }
        }
        
    }

    private static class CollisionNode<T> extends LeafNode<T> {
        
        private final LeafNode<T>[] entries;

        public CollisionNode(int hash, LeafNode<T>[] entries) {
            super(hash);
            this.entries = entries;
        }

        @Override
        public T getOrInsert(T key, int hash, int hashLevel, int level, AtomicReference<TrieNode<T>> parent, ReferenceQueue<? super T> cleared) {
            for (LeafNode<T> n : entries) {
                T result = n.getOrInsert(key, hash, hashLevel, level, parent, cleared);
                if (result != null) {
                    return result;
                }
            }
            // a new node we have to add to this collision node.
            SingleLeafNode<T> newChildNode = new SingleLeafNode<>(key, hash, cleared);
            TrieNode<T> newNode = grow(newChildNode, NormalNode.index(hash, level), level);
            if (parent.compareAndSet(this, newNode)) {
            	return key;
            }
            newChildNode.key.clear();
            return parent.get().getOrInsert(key, hash, hashLevel, level, parent, cleared);
        }

        
        @Override
        public TrieNode<T> grow(LeafNode<T> newChild, int index, int level) {
            if (newChild.hash == hash) {
                LeafNode<T>[] newEntries = Arrays.copyOf(entries, entries.length + 1);
                newEntries[newEntries.length - 1] = newChild;
                return new CollisionNode<>(hash, newEntries);
            }
            else {
                // we are a non bottom level collision node, and we find a non collision node
                // we have to expand to fill again
                return NormalNode.build(this, newChild, level);
            }
        }

    }

    
    private static abstract class NormalNode<T> implements TrieNode<T> {
        protected final int bitmap;
        
        static final int BITS_PER_LEVEL = 5;
        static final int LEVEL_MASK = ((1 << (BITS_PER_LEVEL))- 1);

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
        
        public NormalNode(int bitmap) {
            this.bitmap = bitmap;
        }
        
        
        public static <T> TrieNode<T> build(LeafNode<T> first, LeafNode<T> second, int level) {
            int firstIndex = index(first.hash, level);
            int secondIndex = index(second.hash, level);
            int newBitmap = 1 << firstIndex | 1 << secondIndex;
            if (firstIndex < secondIndex) {
            	return new NormalNode2<>(newBitmap, new AtomicReference<>(first), new AtomicReference<>(second));
            }
            else if (firstIndex > secondIndex) {
            	return new NormalNode2<>(newBitmap, new AtomicReference<>(second), new AtomicReference<>(first));
            }
            else{
                // same index at this level
            	return new NormalNode1<>(newBitmap, new AtomicReference<>(build(first, second, level + 1)));
            }
        }


        
        @Override
        public T getOrInsert(T key, int hash, int hashLevel, int level, AtomicReference<TrieNode<T>> parent, ReferenceQueue<? super T> cleared) {
        	assert level < 8;
            int index = hashLevel & LEVEL_MASK;
            int offset = offsetIfSet(bitmap, index);
            if (offset != -1) {
                AtomicReference<TrieNode<T>> currentIntermediateNode = get(offset);
                TrieNode<T> currentRealNode = currentIntermediateNode.get();
                T result = currentRealNode.getOrInsert(key, hash, hashLevel >>> BITS_PER_LEVEL, level + 1, currentIntermediateNode, cleared);
                if (result == null) {
                    // we have to insert it, and we have reached a leaf node (or a collision node), so we can expand it to either a new trie node, or a collision node
                    SingleLeafNode<T> newLeafNode = new SingleLeafNode<>(key, hash, cleared);
                    TrieNode<T> newTree = currentRealNode.grow(newLeafNode, index, level);
                    if (currentIntermediateNode.compareAndSet(currentRealNode, newTree)) {
                        return key;
                    }
                    else {
                        // failed since the tree was updated, so let's retry
                        newLeafNode.key.clear();
                        return getOrInsert(key, hash, hashLevel, level, parent, cleared);
                    }
                }
                return result;
            }
            else {
                // not in this level, so we replace ourself with a node that does have it 
                SingleLeafNode<T> newLeafNode = new SingleLeafNode<>(key, hash, cleared);
                TrieNode<T> newTree = grow(newLeafNode, index, level);
                if (parent.compareAndSet(this, newTree)) {
                    return key;
                }
                else {
                    // failed since the tree was updated, so let's retry
                    newLeafNode.key.clear();
                    return parent.get().getOrInsert(key, hash, hashLevel, level, parent, cleared);
                }
            }
        }

		protected abstract AtomicReference<TrieNode<T>> get(int offset);
    }
    
    private static class NormalNode0<T> extends NormalNode<T> {
    	
    	public NormalNode0() {
    		super(0);
		}

		@Override
		public TrieNode<T> grow(LeafNode<T> newChild, int index, int level) {
			return new NormalNode1<>(1 << index, new AtomicReference<>(newChild));
		}

		@Override
		protected AtomicReference<TrieNode<T>> get(int offset) {
			throw new ArrayIndexOutOfBoundsException(offset);
		}
    }

    private static class NormalNode1<T> extends NormalNode<T> {
    	private final AtomicReference<TrieNode<T>> entry;

		public NormalNode1(int bitmap, AtomicReference<TrieNode<T>> entry) {
    		super(bitmap);
			this.entry = entry;
		}
		
		@Override
		public TrieNode<T> grow(LeafNode<T> newChild, int index, int level) {
			int newBitmap = bitmap | 1 << index;
			int newOffset = offsetIfSet(newBitmap, index);
			if (newOffset == 0) {
				return new NormalNode2<>(newBitmap, new AtomicReference<>(newChild), entry);
			}
			else {
				return new NormalNode2<>(newBitmap, entry, new AtomicReference<>(newChild));
			}
		}
		
		@Override
		protected AtomicReference<TrieNode<T>> get(int offset) {
			return entry;
		}
    }
    
    private static class NormalNode2<T> extends NormalNode<T> {
    	private final AtomicReference<TrieNode<T>> first;
    	private final AtomicReference<TrieNode<T>> second;

		public NormalNode2(int newBitmap, AtomicReference<TrieNode<T>> first, AtomicReference<TrieNode<T>> second) {
    		super(newBitmap);
			this.first = first;
			this.second = second;
		}
		
		@Override
		public TrieNode<T> grow(LeafNode<T> newChild, int index, int level) {
			int newBitmap = bitmap | 1 << index;
			int newOffset = offsetIfSet(newBitmap, index);
			AtomicReference<TrieNode<T>> newChildRef = new AtomicReference<>(newChild);
			switch (newOffset) {
				case 0: return new NormalNode3<>(newBitmap, newChildRef, first, second);
				case 1: return new NormalNode3<>(newBitmap, first, newChildRef, second);
				case 2: return new NormalNode3<>(newBitmap, first, second, newChildRef);
				default: throw new ArrayIndexOutOfBoundsException(newOffset);
			}
		}
		
		
		@Override
		protected AtomicReference<TrieNode<T>> get(int offset) {
			switch (offset) {
				case 0: return first;
				case 1: return second;
				default: throw new ArrayIndexOutOfBoundsException(offset);
			}
		}
    }

    private static class NormalNode3<T> extends NormalNode<T> {
    	private final AtomicReference<TrieNode<T>> first;
    	private final AtomicReference<TrieNode<T>> second;
    	private final AtomicReference<TrieNode<T>> third;

		public NormalNode3(int newBitmap, AtomicReference<TrieNode<T>> first, AtomicReference<TrieNode<T>> second, AtomicReference<TrieNode<T>> third) {
    		super(newBitmap);
			this.first = first;
			this.second = second;
			this.third = third;
		}
		
		@Override
		public TrieNode<T> grow(LeafNode<T> newChild, int index, int level) {
			int newBitmap = bitmap | 1 << index;
			int newOffset = offsetIfSet(newBitmap, index);
			AtomicReference<TrieNode<T>> newChildRef = new AtomicReference<>(newChild);
			switch (newOffset) {
				case 0: return new NormalNode4<>(newBitmap, newChildRef, first, second, third);
				case 1: return new NormalNode4<>(newBitmap, first, newChildRef, second, third);
				case 2: return new NormalNode4<>(newBitmap, first, second, newChildRef, third);
				case 3: return new NormalNode4<>(newBitmap, first, second, third, newChildRef);
				default: throw new ArrayIndexOutOfBoundsException(newOffset);
			}
		}
		
		
		@Override
		protected AtomicReference<TrieNode<T>> get(int offset) {
			switch (offset) {
				case 0: return first;
				case 1: return second;
				case 2: return third;
				default: throw new ArrayIndexOutOfBoundsException(offset);
			}
		}
    }

    private static class NormalNode4<T> extends NormalNode<T> {
    	private final AtomicReference<TrieNode<T>> first;
    	private final AtomicReference<TrieNode<T>> second;
    	private final AtomicReference<TrieNode<T>> third;
    	private final AtomicReference<TrieNode<T>> fourth;

		public NormalNode4(int newBitmap, AtomicReference<TrieNode<T>> first, AtomicReference<TrieNode<T>> second, AtomicReference<TrieNode<T>> third, AtomicReference<TrieNode<T>> fourth) {
    		super(newBitmap);
			this.first = first;
			this.second = second;
			this.third = third;
			this.fourth = fourth;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public TrieNode<T> grow(LeafNode<T> newChild, int index, int level) {
			int newBitmap = bitmap | 1 << index;
			int newOffset = offsetIfSet(newBitmap, index);
			AtomicReference<TrieNode<T>> newChildRef = new AtomicReference<>(newChild);
			switch (newOffset) {
				case 0: return new NormalNodeN<>(newBitmap, new AtomicReference[] { newChildRef, first, second, third, fourth });
				case 1: return new NormalNodeN<>(newBitmap, new AtomicReference[] { first, newChildRef,  second, third, fourth });
				case 2: return new NormalNodeN<>(newBitmap, new AtomicReference[] { first, second, newChildRef, third, fourth });
				case 3: return new NormalNodeN<>(newBitmap, new AtomicReference[] { first, second, third, newChildRef, fourth });
				case 4: return new NormalNodeN<>(newBitmap, new AtomicReference[] { first, second, third, fourth, newChildRef });
				default: throw new ArrayIndexOutOfBoundsException(newOffset);
			}
		}
		
		
		@Override
		protected AtomicReference<TrieNode<T>> get(int offset) {
			switch (offset) {
				case 0: return first;
				case 1: return second;
				case 2: return third;
				case 3: return fourth;
				default: throw new ArrayIndexOutOfBoundsException(offset);
			}
		}
    }
    
    
    private static class NormalNodeN<T> extends NormalNode<T> {
        private final AtomicReference<TrieNode<T>>[] data;
        
        public NormalNodeN(int bitmap, AtomicReference<TrieNode<T>>[] data) {
        	super(bitmap);
        	this.data = data;
		}

        @Override
        public TrieNode<T> grow(LeafNode<T> newChild, int index, int level) {
            @SuppressWarnings("unchecked")
            AtomicReference<TrieNode<T>>[] newData = new AtomicReference[this.data.length + 1];
            int newBitmap = this.bitmap | 1 << index;
            int newOffset = offsetIfSet(newBitmap, index);
            if (newOffset > 0) {
                System.arraycopy(this.data, 0, newData, 0, newOffset);
            }
            newData[newOffset] = new AtomicReference<>(newChild);
            if (newOffset + 1 < newData.length) {
                System.arraycopy(this.data, newOffset, newData, newOffset + 1, this.data.length - newOffset);
            }
            return new NormalNodeN<>(newBitmap, newData);
        }
        
        @Override
        protected AtomicReference<TrieNode<T>> get(int offset) {
        	return data[offset];
        }
    }
}
