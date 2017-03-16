package io.usethesource.vallang.io.binary.util;

import java.lang.reflect.Method;
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
//        buffers.put(returned.capacity(), returned);
        closeDirectBuffer(returned);
    }
    // from: http://stackoverflow.com/a/19447758/11098
    private static void closeDirectBuffer(ByteBuffer cb) {
        if (cb==null || !cb.isDirect()) return;

        // we could use this type cast and call functions without reflection code,
        // but static import from sun.* package is risky for non-SUN virtual machine.
        //try { ((sun.nio.ch.DirectBuffer)cb).cleaner().clean(); } catch (Exception ex) { }
        try {
            Method cleaner = cb.getClass().getMethod("cleaner");
            cleaner.setAccessible(true);
            Method clean = Class.forName("sun.misc.Cleaner").getMethod("clean");
            clean.setAccessible(true);
            clean.invoke(cleaner.invoke(cb));
        } catch(Exception ex) { }
        cb = null;
    }



    public ByteBuffer getExact(int size) {
        return ByteBuffer.allocateDirect(size);
//        ByteBuffer result = buffers.get(size, ByteBuffer::allocateDirect);
//        return result;
    }
}
