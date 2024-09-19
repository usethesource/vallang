package io.usethesource.vallang.io.binary.util;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicReference;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class FileChannelDirectInputStream extends ByteBufferInputStream {
    private final FileChannel channel;
    private final boolean small;
    private boolean closed = false;

    public FileChannelDirectInputStream(FileChannel channel) throws IOException {
        super(smallFile(channel) ? getSmallBuffer(channel) : channel.map(MapMode.READ_ONLY, 0, channel.size()));
        this.channel = channel;
        this.small = smallFile(channel);
    }

    private static ByteBuffer getSmallBuffer(FileChannel channel) throws IOException {
        return (ByteBuffer) DirectByteBufferCache.getInstance().get((int)channel.size()).flip();
    }
    private static boolean smallFile(FileChannel channel) throws IOException {
        return channel.size() < 8*1024;
    }
    @Override
    protected ByteBuffer refill(ByteBuffer torefill) throws IOException {
        if (small) {
            torefill.clear();
            channel.read(torefill);
            torefill.flip();
        }
        return torefill;
    }

    @Override
    public void close() throws IOException {
        if (!closed ) {
            closed = true;
            try (FileChannel chan = channel){
                if (!small) {
                    closeDirectBuffer(source);
                }
                else {
                    DirectByteBufferCache.getInstance().put(source);
                }
            }
        }
    }

    private static final @MonotonicNonNull MethodHandle INVOKE_CLEANER;
    private static final @MonotonicNonNull Object THE_UNSAFE;

    // see https://stackoverflow.com/a/19447758/11098
    static {
        final AtomicReference<@Nullable MethodHandle> cleaner = new AtomicReference<>();
        final AtomicReference<@Nullable Object> unsafe = new AtomicReference<>();
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            Class<?> unsafeClass;
            try {
                unsafeClass = Class.forName("sun.misc.Unsafe");
            } catch(Exception ex) {
                // jdk.internal.misc.Unsafe doesn't yet have an invokeCleaner() method,
                // but that method should be added if sun.misc.Unsafe is removed.
                try {
                    unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
                } catch (Exception e) {
                    unsafeClass = null;
                    System.err.println(FileChannelDirectInputStream.class.getName() + ": cannot locate unsafe class :" + e);
                }
            }
            if (unsafeClass == null) {
                System.err.println(FileChannelDirectInputStream.class.getName() + ": cannot locate unsafe class");
                return null;
            }
            try {
                cleaner.set(MethodHandles.lookup().findVirtual(
                    unsafeClass, "invokeCleaner",
                    MethodType.methodType(void.class, ByteBuffer.class)));


                Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
                theUnsafeField.setAccessible(true);
                @SuppressWarnings("nullness") // CF marks .get as not accepting null, since the api is
                Object unsafeValue = theUnsafeField.get(null);
                unsafe.set(unsafeValue);
                return null;
            }
            catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
                System.err.println(FileChannelDirectInputStream.class.getName() + ": cannot locate unsafe instance or invokeCleaner: " + e);
                cleaner.set(null);
                unsafe.set(null);
                return null;
            }
        });
        INVOKE_CLEANER = cleaner.get();
        THE_UNSAFE = unsafe.get();
    }
    // from: http://stackoverflow.com/a/19447758/11098
    private static void closeDirectBuffer(@Nullable ByteBuffer cb) {
        if (cb==null || !cb.isDirect()) { return; }
        if (INVOKE_CLEANER != null && THE_UNSAFE != null) {
            try {
                INVOKE_CLEANER.invoke(THE_UNSAFE, cb);
            } catch (Throwable e) {
                System.err.println(FileChannelDirectInputStream.class.getName() + ": error cleaning: " + e);
            }
        }
        else {
            // we could not find a cleaner, so as a fall-back we are gonna request a gc
            cb = null;
            System.gc();
        }
    }

}
