package io.usethesource.vallang.io.binary.util;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.github.luben.zstd.ZstdDirectBufferDecompressingStream;

public class DirectZstdInputStream extends ByteBufferInputStream {

    private final ByteBufferInputStream orginalStream;
    private ZstdDirectBufferDecompressingStream decompressor;
    private boolean closed = false;

    public DirectZstdInputStream(ByteBufferInputStream originalStream) {
        super(constructDecompressedBuffer(originalStream));
        this.orginalStream = originalStream;
        decompressor = new ZstdDirectBufferDecompressingStream(originalStream.getByteBuffer());
    }
    
    @Override
    protected ByteBuffer refill(ByteBuffer torefill) throws IOException {
        torefill.clear();
        decompressor.read(torefill);
        torefill.flip();
        return torefill;
    }

    private static ByteBuffer constructDecompressedBuffer(ByteBufferInputStream source) {
        int compressedSize = source.getByteBuffer().remaining();
        ByteBuffer result = DirectByteBufferCache.getInstance().get(Math.min(compressedSize * 2, ZstdDirectBufferDecompressingStream.recommendedTargetBufferSize()));
        result.limit(0); // delay compression for first read
        return result;
    }
    
    @Override
    public void close() throws IOException {
        if (!closed ) {
            closed = true;
            try (Closeable old = orginalStream) {
                decompressor.close();
                super.close();
            }
            finally {
                DirectByteBufferCache.getInstance().put(super.source);
            }
        }
    }

}
