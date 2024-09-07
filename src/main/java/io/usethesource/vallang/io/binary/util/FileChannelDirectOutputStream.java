package io.usethesource.vallang.io.binary.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileChannelDirectOutputStream extends ByteBufferOutputStream {
    private boolean closing = false;
    private int flushes = 0;
    private final FileChannel channel;
    private final int growEvery;

    public FileChannelDirectOutputStream(FileChannel channel, int growEvery) {
        super(DirectByteBufferCache.getInstance().get(8*1024));
        this.channel = channel;
        this.growEvery = growEvery;
    }

    @Override
    protected ByteBuffer flush(ByteBuffer toflush) throws IOException {
        channel.write(toflush);
        toflush.clear();
        flushes++;
        if (flushes == growEvery && !closing) {
            flushes = 0;
            return grow(toflush);
        }
        return toflush;
    }

    protected ByteBuffer grow(ByteBuffer current) {
        int currentCapacity = current.capacity();
        if (currentCapacity < (1024*1024)) {
            DirectByteBufferCache.getInstance().put(current);
            return DirectByteBufferCache.getInstance().get(currentCapacity * 2);
        }
        return current;
    }

    @Override
    public void write(ByteBuffer buf) throws IOException {
        if (target.remaining() >= buf.remaining()) {
            target.put(buf);
        }
        else {
            int oldLimit = buf.limit();
            buf.limit(buf.position() + target.remaining());
            target.put(buf);
            buf.limit(oldLimit);

            flush();
            if (target.remaining() >= buf.remaining()) {
                // now it does fit, no problem
                target.put(buf);
            }
            else {
                // directly write the remaining buffer to the channel
                // skipping our internal buffer
                assert target.position() == 0;
                channel.write(buf);
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            try (FileChannel chan = channel) {
                closing = true;
                flush();
                super.close();
            }
            finally {
                DirectByteBufferCache.getInstance().put(target);
            }
        }
    }
}