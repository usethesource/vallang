package io.usethesource.vallang.io.binary.util;

import java.nio.ByteBuffer;

public class DirectByteBufferCache {
    
    static private class InstanceHolder {
        static final DirectByteBufferCache sInstance = new DirectByteBufferCache();
    }
    
    public static DirectByteBufferCache getInstance() {
        return InstanceHolder.sInstance;
    }
    
    private DirectByteBufferCache() { }
        
    
    private final CacheFactory<ByteBuffer> buffers = new CacheFactory<>(DirectByteBufferCache::clear);

    
    private static Boolean clear(ByteBuffer b) {
        b.clear();
        return true;
    }
    
    private static int roundSize(int size) {
        return (size / (8*1024)) * (8*1024);
    }
    
    public ByteBuffer get(int size) {
        return buffers.get(roundSize(size), ByteBuffer::allocateDirect);
    }
    
    public void put(ByteBuffer returned) {
        buffers.put(returned.capacity(), returned);
    }
}
