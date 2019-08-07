package io.usethesource.vallang.io.binary.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Objects;
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
    // from: http://stackoverflow.com/a/19447758/11098
    private static void closeDirectBuffer(@Nullable ByteBuffer cb) {
        if (cb==null || !cb.isDirect()) return;

        // we could use this type cast and call functions without reflection code,
        // but static import from sun.* package is risky for non-SUN virtual machine.
        //try { ((sun.nio.ch.DirectBuffer)cb).cleaner().clean(); } catch (Exception ex) { }
        try {
            Method cleaner = cb.getClass().getMethod("cleaner");
            cleaner.setAccessible(true);
            Method clean = Class.forName("sun.misc.Cleaner").getMethod("clean");
            clean.setAccessible(true);
            clean.invoke(Objects.requireNonNull(cleaner.invoke(cb)));
        } catch(Exception ex) { }
        cb = null;
    }

}