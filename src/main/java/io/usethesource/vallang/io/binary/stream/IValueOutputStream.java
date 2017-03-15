/** 
 * Copyright (c) 2016, Davy Landman, Paul Klint, Centrum Wiskunde & Informatica (CWI) 
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
package io.usethesource.vallang.io.binary.stream;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;

import com.github.luben.zstd.ZstdDirectBufferDecompressingStream;

import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.io.binary.message.IValueWriter;
import io.usethesource.vallang.io.binary.util.ByteBufferOutputStream;
import io.usethesource.vallang.io.binary.util.DelayedCompressionOutputStream;
import io.usethesource.vallang.io.binary.util.DelayedZstdOutputStream;
import io.usethesource.vallang.io.binary.util.FileChannelDirectOutputStream;
import io.usethesource.vallang.io.binary.util.WindowSizes;
import io.usethesource.vallang.io.binary.wire.IWireOutputStream;
import io.usethesource.vallang.io.binary.wire.binary.BinaryWireOutputStream;
            
/**
 * A binary serializer for IValues. <br/>
 * <br />
 * Note that when writing multiple IValues, you have to take care of storing this arity yourself.  <br/>
 * <br />
 * When you want to nest the IValue's in another stream, you will have to use the {@link IValueWriter IValueWriter} static methods.
 * This does enforce you to adopt the same {@link IWireOutputStream wire format} format.
 */
public class IValueOutputStream implements Closeable {
    

    /**
     * Compression of the serializer, balances lookback windows and compression algorithm
     */
    public enum CompressionRate {
        /**
         * Use only for debugging!
         */
        NoSharing(Header.Compression.NONE, 0),
        None(Header.Compression.NONE, 0),
        Light(Header.Compression.ZSTD, 1),
        Normal(Header.Compression.ZSTD, 5),
        Strong(Header.Compression.ZSTD, 13),
        Extreme(Header.Compression.XZ, 6), 
        ;

        private final int compressionAlgorithm;
        private final int compressionLevel;

        CompressionRate(int compressionAlgorithm, int compressionLevel) {
            this.compressionLevel = compressionLevel;
            this.compressionAlgorithm = compressionAlgorithm;
        } 
    }
    
    
    
    
    private CompressionRate compression;
    private OutputStream rawStream;
    private IWireOutputStream writer;
    private final IValueFactory vf;

    public IValueOutputStream(OutputStream out, IValueFactory vf) throws IOException {
        this(out, vf, CompressionRate.Normal);
    }
    
    public IValueOutputStream(FileChannel channel, IValueFactory vf, CompressionRate compression) throws IOException {
        this(byteBufferedOutput(channel), vf, compression);
    }

    
    public IValueOutputStream(OutputStream out, IValueFactory vf, CompressionRate compression) throws IOException {
        out.write(Header.MAIN);
        this.rawStream = out;
        this.compression = compression;
        this.writer = null;
        this.vf = vf;
    }

    private static OutputStream byteBufferedOutput(FileChannel channel) {
        return new FileChannelDirectOutputStream(channel, 10);
    }
    
    
    public void write(IValue value) throws IOException {
        WindowSizes sizes = compression.compressionLevel == 0 ? WindowSizes.NO_WINDOW : WindowSizes.NORMAL_WINDOW;
        if (writer == null) {
            writer = initializeWriter(sizes);
        }
        IValueWriter.write(writer, vf, sizes, value);
    }



    private static int fallbackIfNeeded(int compressionAlgorithm) {
        if (compressionAlgorithm == Header.Compression.ZSTD && ! Compressor.zstdAvailable()) {
            return Header.Compression.GZIP;
        }
        return compressionAlgorithm;
    }

    private IWireOutputStream initializeWriter(WindowSizes sizes) throws IOException {
        if (sizes == WindowSizes.NO_WINDOW || sizes == WindowSizes.TINY_WINDOW) {
            compression = CompressionRate.None;
        }
        int algorithm = fallbackIfNeeded(compression.compressionAlgorithm);
        if (rawStream instanceof ByteBufferOutputStream && algorithm == Header.Compression.ZSTD && ((ByteBufferOutputStream)rawStream).getBuffer().isDirect()) {
            rawStream = new DelayedZstdOutputStream((ByteBufferOutputStream)rawStream, algorithm, compression.compressionLevel);
        }
        else {
            rawStream = new DelayedCompressionOutputStream(rawStream, algorithm, o ->
                Compressor.wrapStream(o, algorithm, compression.compressionLevel)
            );
        }
        return new BinaryWireOutputStream(rawStream, sizes.stringsWindow);
    }


    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
        else {
            rawStream.close();
        }
    }
}
