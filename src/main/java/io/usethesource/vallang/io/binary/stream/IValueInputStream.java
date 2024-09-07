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
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.io.binary.message.IValueReader;
import io.usethesource.vallang.io.binary.util.FileChannelDirectInputStream;
import io.usethesource.vallang.io.binary.wire.binary.BinaryWireInputStream;
import io.usethesource.vallang.type.TypeStore;

/**
 * Reader for binary serialized IValues written using the {@linkplain IValueOutputStream}. <br />
 * <br />
 * At the moment, it automatically detects the old serializer format, and try to read using the old {@linkplain  BinaryReader}.
 */
public class IValueInputStream implements Closeable {

    private final @Nullable BinaryWireInputStream reader;
    private final IValueFactory vf;
    private final Supplier<TypeStore> typeStoreSupplier;

    /**
     * This will <strong>consume</strong> the whole stream (or at least more than needed due to buffering), don't use the InputStream afterwards!
     */
    public IValueInputStream(InputStream in, IValueFactory vf, Supplier<TypeStore> typeStoreSupplier) throws IOException {
        this.vf = vf;
        this.typeStoreSupplier = typeStoreSupplier;
        byte[] currentHeader = new byte[Header.MAIN.length];
        int read = 0;
        while (read < currentHeader.length) {
            read += in.read(currentHeader, read, currentHeader.length - read);
        }
        if (!Arrays.equals(currentHeader, Header.MAIN)) {
            throw new IOException("Incorrect file header, expected: [" + toHex(Header.MAIN) + "] but found: [" + toHex(currentHeader) + "]");
        }

        int compression = in.read();
        in = Compressor.wrapStream(in, compression);
        reader = new BinaryWireInputStream(in);
    }


    private static String toHex(byte[] main) {
        String result = "";
        for (byte b : main) {
            result += String.format("%#04x ", b);
        }
        return result.trim();
    }


    public IValueInputStream(FileChannel channel, IValueFactory vf, Supplier<TypeStore> typeStoreSupplier) throws IOException {
        this(new FileChannelDirectInputStream(channel), vf, typeStoreSupplier);
    }

    public IValue read() throws IOException {
        if (reader == null) {
            throw new IllegalStateException("Incorrect initialization");
        }
        return IValueReader.readValue(reader, vf, typeStoreSupplier);
    }

    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }
}

