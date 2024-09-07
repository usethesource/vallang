package io.usethesource.vallang.io.binary.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public abstract class ByteBufferOutputStream extends OutputStream {

    protected ByteBuffer target;
    protected boolean closed = false;
    public ByteBufferOutputStream(ByteBuffer target) {
        this.target = target;
    }



    public ByteBuffer getBuffer() {
        return target;
    }

    /***
     * Flush the buffer (that has been flipped already) and return the same buffer (but cleared) or a new buffer for the next round.
     * @param toflush the flipped buffer to flush
     * @return a new or the same buffer that has room available for writing.
     * @throws IOException
     */
    protected abstract ByteBuffer flush(ByteBuffer toflush) throws IOException;

    @Override
    public void flush() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }

        target.flip();
        if (target.hasRemaining()) {
            target = flush(target);
            if (!target.hasRemaining()) {
                throw new IOException("flush implementation didn't correctly provide a new buffer to write to: " + target);
            }
        }
        else {
            // the buffer was empty, so flush was called on an already flushed stream
            // so we'll reset the buffer to make sure we don't continue with empty buffer
            target.clear();
        }
        assert target.hasRemaining(): "after a flush, we should have a buffer with some room. (it was: " + target + ")";
    }

    @Override
    public void close() throws IOException {
        if (!closed ) {
            try {
                flush();
            }
            finally {
                closed = true;
            }
        }
    }


    @Override
    public void write(int b) throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
        if (!target.hasRemaining()) {
            flush();
        }
        target.put((byte)(b & 0xFF));
    }
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }

        int written = 0;
        while (written < len) {
            if (!target.hasRemaining()) {
                flush();
            }
            int chunk = Math.min(target.remaining(), len - written);
            target.put(b, off + written, chunk);
            written += chunk;
        }
    }
    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void write(ByteBuffer buf) throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
        int originalLimit = buf.limit();
        while (buf.hasRemaining()) {
            if (!target.hasRemaining()) {
                flush();
            }
            int chunk = Math.min(target.remaining(), buf.remaining());
            buf.limit(buf.position() + chunk); // we have to move the limit around to fit the size of the target
            target.put(buf); // this api is limited so that it will always consume the full buffer, so thats why we move around the limit
            buf.limit(originalLimit);
        }
    }

}
