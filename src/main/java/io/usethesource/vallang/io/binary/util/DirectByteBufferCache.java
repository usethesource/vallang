package io.usethesource.vallang.io.binary.util;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class DirectByteBufferCache {
    
    static private class InstanceHolder {
        static final DirectByteBufferCache sInstance = new DirectByteBufferCache();
    }
    
    public static DirectByteBufferCache getInstance() {
        return InstanceHolder.sInstance;
    }
    
    private DirectByteBufferCache() { }
        
    
    private final CacheFactory<ByteBuffer> buffers = new CacheFactory<>(3, TimeUnit.SECONDS, DirectByteBufferCache::clear);

    
    private static Boolean clear(ByteBuffer b) {
        b.clear();
        return true;
    }
    
    private static int roundSize(int size) {
        return (int)(Math.ceil(size / (8*1024.0)) * (8*1024));
    }
    
    public ByteBuffer get(int size) {
        return getExact(roundSize(size));
    }
    
    public void put(ByteBuffer returned) {
        buffers.put(returned.capacity(), returned);
    }

    public ByteBuffer getExact(int size) {
        return buffers.get(size, ByteBuffer::allocateDirect);
    }
}
