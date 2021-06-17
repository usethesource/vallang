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
package io.usethesource.vallang.io.binary.wire.binary;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.io.binary.util.ByteBufferInputStream;
import io.usethesource.vallang.io.binary.util.TaggedInt;
import io.usethesource.vallang.io.binary.util.TrackLastRead;
import io.usethesource.vallang.io.binary.util.WindowCacheFactory;
import io.usethesource.vallang.io.binary.wire.FieldKind;
import io.usethesource.vallang.io.binary.wire.IWireInputStream;


public class BinaryWireInputStream implements IWireInputStream {
    private static final byte[] WIRE_VERSION = new byte[] { 1, 0, 0 };
    private final InputStream __stream;
    private final TrackLastRead<String> stringsRead;
    private boolean closed = false;
    private int current;
    private int messageID;
    private int fieldType;
    private int fieldID;
    private @Nullable String stringValue;
    private int intValue;
    private byte @Nullable [] bytesValue;
    private int nestedType;
    private String @Nullable [] stringValues;
    private int @Nullable [] intValues;
    private int nestedLength;

    public BinaryWireInputStream(InputStream stream) throws IOException {
        this(stream, 8*1024);
    }
        
    public BinaryWireInputStream(InputStream stream, int bufferSize) throws IOException {
        if (stream instanceof BufferedInputStream || stream instanceof ByteBufferInputStream) {
            __stream = stream;
        }
        else {
            this.__stream = new BufferedInputStream(stream, bufferSize);
        }
        
        byte[] header = readBytes(stream, WIRE_VERSION.length);
        if (!Arrays.equals(WIRE_VERSION, header)) {
            throw new IOException("Unsupported wire format");
        }
        int stringReadSize = decodeInteger(__stream);
        this.stringsRead = WindowCacheFactory.getInstance().getTrackLastRead(stringReadSize);
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            try {
                __stream.close();
            } finally {
                closed = true;
                WindowCacheFactory.getInstance().returnTrackLastRead(stringsRead);
            }
        }
        else {
            throw new IOException("Already closed");
        }
    }

    private void assertNotClosed() throws IOException {
        if (closed) {
            throw new IOException("Stream already closed"); 
        }
    }

    private byte[] readBytes(int len) throws IOException {
        return readBytes(__stream, len);        
    }

    private static byte[] readBytes(InputStream stream, int len) throws IOException, EOFException {
        byte[] result = new byte[len];
        
        int pos = 0;        
        while (pos < len) {
            int read = stream.read(result, pos, len - pos);
            if (read == -1) {
                throw new EOFException();
            }
            pos += read;
        }
        
        return result;
    }
    
    /*
     * LEB128 decoding (or actually LEB32) of positive and negative integers, negative integers always use 5 bytes, positive integers are compact.
     */
    private int decodeInteger()  throws IOException {
        return decodeInteger(__stream);
    }
    
    private static int decodeInteger(InputStream stream) throws IOException {
        try {
            // manually unrolling the loop was the fastest for reading, yet not for writing
            byte b = (byte) stream.read();
            if ((b & 0x80) == 0) {
                return b;
            }

            int result = b & 0x7F;

            b = (byte) stream.read();
            result ^= ((b & 0x7F) << 7);
            if ((b & 0x80) == 0) {
                return result;
            }

            b = (byte) stream.read();
            result ^= ((b & 0x7F) << 14);
            if ((b & 0x80) == 0) {
                return result;
            }

            b = (byte) stream.read();
            result ^= ((b & 0x7F) << 21);
            if ((b & 0x80) == 0) {
                return result;
            }

            b = (byte) stream.read();
            result ^= ((b & 0x7F) << 28);
            if ((b & 0x80) == 0) {
                return result;
            }
            throw new IOException("Incorrect integer");
        }
        catch (BufferUnderflowException e) {
            throw new EOFException();
        }
    }

    /*
     * A string is encoded to UTF8 and stored with a prefix of the amount of bytes needed
     */
    private String decodeString() throws IOException {
        int len = decodeInteger();
        byte[] bytes = readBytes(len);
        // this is the fastest way, other paths to a string lead to an extra allocated char array
        return new String(bytes, StandardCharsets.UTF_8);
    }


    @Override
    public int next() throws IOException {
        assertNotClosed();
        // clear memory
        intValues = null;
        stringValues = null;
        int next = decodeInteger();
        if (next == 0) {
            return current = MESSAGE_END;
        }
        fieldID = TaggedInt.getOriginal(next);
        fieldType = TaggedInt.getTag(next);
        switch (fieldType) {
            case 0:
                // special case that signals starts of values
                messageID = fieldID;
                return current = MESSAGE_START;
            case FieldKind.NESTED:
                // only case where we don't read the value
                break;
            case FieldKind.STRING:
                stringValue = decodeString();
                stringsRead.read(stringValue);
                break;
            case FieldKind.INT:
                intValue = decodeInteger();
                break;
            case FieldKind.PREVIOUS_STR:
                int reference = decodeInteger();
                fieldType = TaggedInt.getTag(reference);
                assert fieldType == FieldKind.STRING;
                stringValue = stringsRead.lookBack(TaggedInt.getOriginal(reference));
                break;
            case FieldKind.REPEATED:
                int flaggedAmount = decodeInteger();
                nestedType = TaggedInt.getTag(flaggedAmount);
                nestedLength = TaggedInt.getOriginal(flaggedAmount);
                if (nestedLength == TaggedInt.MAX_ORIGINAL_VALUE) {
                    nestedLength = decodeInteger();
                }
                switch (nestedType) {
                    case FieldKind.Repeated.BYTES:
                        bytesValue = readBytes(nestedLength);
                        break;
                    case FieldKind.Repeated.INTS:
                        int[] intValues = new int[nestedLength];
                        for (int i = 0; i < nestedLength; i++) {
                            intValues[i] = decodeInteger();
                        }
                        this.intValues = intValues;
                        break;
                    case FieldKind.Repeated.STRINGS: 
                        String[] stringValues = new String[nestedLength];
                        for (int i = 0; i < nestedLength; i++) {
                            stringValues[i]= readNestedString();
                        }
                        this.stringValues = stringValues;
                        break;
                    case FieldKind.Repeated.NESTEDS:
                        break;
                    default:
                        throw new IOException("Unsupported nested type:" + nestedType);
                }
                break;
            default:
                throw new IOException("Unexpected wire type: " + fieldType);
        }
        return current = FIELD;
    }

    private String readNestedString() throws IOException {
        int reference = decodeInteger();
        String result;
        if (TaggedInt.getTag(reference) == FieldKind.STRING) {
            // normal string
            result = decodeString();
            stringsRead.read(result);
        }
        else {
            assert TaggedInt.getTag(reference) == FieldKind.PREVIOUS_STR;
            result = stringsRead.lookBack(TaggedInt.getOriginal(reference));
        }
        return result;
    }


    @Override
    public int current() {
        return current;
    }

    @Override
    public int message() {
        assert current == MESSAGE_START;
        return messageID;
    }

    @Override
    public int field() {
        assert current == FIELD;
        return fieldID;
    }
    
    @Override
    public int getInteger() {
        assert fieldType == FieldKind.INT;
        return intValue;
    }

    @Override
    public String getString() {
        assert fieldType == FieldKind.STRING;
        return nonNull(stringValue);
    }

    private static <T> T nonNull(@Nullable T field) {
        if (field == null) {
            throw new RuntimeException("Unexpected null field");
        }
        return field;
    }
    
    
    @Override
    public byte[] getBytes() {
        assert fieldType == FieldKind.REPEATED && nestedType == FieldKind.Repeated.BYTES;
        return nonNull(bytesValue);
    }
    
    @Override
    public int getFieldType() {
        assert current == FIELD;
        return fieldType;
    }
    
    @Override
    public int getRepeatedType() {
        assert current == FIELD && fieldType == FieldKind.REPEATED;
        return nestedType;
    }
    
    @Override
    public int getRepeatedLength() {
        assert current == FIELD && fieldType == FieldKind.REPEATED;
        return nestedLength;
    }

    @Override
    public String[] getStrings() {
        assert getRepeatedType() == FieldKind.Repeated.STRINGS;
        return nonNull(stringValues);
    }
    
    @Override
    public int[] getIntegers() {
        assert getRepeatedType() == FieldKind.Repeated.INTS;
        return nonNull(intValues);
    }
    
    @Override
    public void skipMessage() throws IOException {
        int toSkip = 1;
        while (toSkip != 0) {
            switch (next()) {
                case MESSAGE_START:
                    toSkip++;
                    break;
                case MESSAGE_END:
                    toSkip--;
                    break;
                default:
                    break;
            }
        }
    }
}
