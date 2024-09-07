/**
 * Copyright (c) 2016, Davy Landman, Centrum Wiskunde & Informatica (CWI)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.usethesource.vallang.io.binary.util;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import com.github.luben.zstd.ZstdDirectBufferCompressingStream;

public class DelayedZstdOutputStream extends ByteBufferOutputStream {

    private static final int COMPRESS_AFTER = 4*1024;
    private ByteBufferOutputStream out;
    private int compressHeader;
    private final int level;
    private boolean firstFlush = true;
    private @MonotonicNonNull ZstdDirectBufferCompressingStream compressor = null;


    public DelayedZstdOutputStream(ByteBufferOutputStream out, int compressHeader, int level) throws IOException {
        super(DirectByteBufferCache.getInstance().get(COMPRESS_AFTER));
        if (out.target == this.target) {
            throw new RuntimeException("That shouldn't happen");
        }

        this.out = out;
        this.compressHeader = compressHeader;
        this.level = level;
    }

    @Override
    protected ByteBuffer flush(ByteBuffer toflush) throws IOException {
        if (out.target == this.target) {
            throw new RuntimeException("That shouldn't happen");
        }

        boolean increaseBufferForCompressor = false;
        if (firstFlush) {
            firstFlush = false;
            if (toflush.remaining() >= COMPRESS_AFTER) {
                // okay full buffer, so we have to start compression
                out.write(compressHeader);
                // a bit hacky, we reuse the same target buffer as the output stream
                compressor = new ZstdDirectBufferCompressingStream(out.target, level) {
                    @Override
                    protected ByteBuffer flushBuffer(ByteBuffer toFlush) throws IOException {
                        out.flush();
                        return out.target;
                    }
                };
                increaseBufferForCompressor = true;
            }
            else {
                // no compression
                out.write(0);
            }
        }

        if (compressor != null) {
            compressor.compress(toflush);
        }
        else {
            out.write(toflush);
        }
        toflush.clear();

        if (increaseBufferForCompressor) {
            DirectByteBufferCache.getInstance().put(toflush);
            return DirectByteBufferCache.getInstance().getExact(ZstdDirectBufferCompressingStream.recommendedOutputBufferSize());
        }
        return toflush;
    }


    @Override
    public void close() throws IOException {
        if (out.target == this.target) {
            throw new RuntimeException("That shouldn't happen");
        }

        if (!closed) {
            try {
                try {
                    flush(); // buffer to compressor
                    if (compressor != null) {
                        compressor.close(); // flush the compressor
                    }
                    super.close();
                }
                finally {
                    out.close();
                }
            }
            finally {
                DirectByteBufferCache.getInstance().put(target);
            }
        }
    }

}
