package io.usethesource.vallang.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import org.junit.jupiter.api.Test;
import io.usethesource.vallang.io.binary.util.FileChannelDirectInputStream;
import io.usethesource.vallang.io.binary.util.FileChannelDirectOutputStream;

class FileChannelOutputStreamTest {
    private static final Path targetFile;
    static {
        Path file = null;
        try {
            file = Files.createTempFile("file-channel-test", "bin");
        } catch (IOException e) {
            System.err.println(e);
        }
        targetFile = file;
    }

    private FileChannel openWriteChannel() throws IOException {
        return FileChannel.open(targetFile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private FileChannel openReadChannel() throws IOException {
        return FileChannel.open(targetFile, StandardOpenOption.READ);
    }

    @Test
    void testSimpleWrite() throws IOException {
        roundTripChannel(new byte[][]{{42}});
    }

    @Test
    void testBigWrite() throws IOException {
        byte[] buffer = new byte[1024*1024];
        new Random().nextBytes(buffer);
        roundTripChannel(new byte[][]{buffer});
    }

    @Test
    void testChunkedBigWrite() throws IOException {
        byte[][] buffers = new byte[1024][];
        var r = new Random();
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = new byte[i * 128];
            r.nextBytes(buffers[i]);
        }
        roundTripChannel(buffers);
    }


    private void roundTripChannel(byte[][] buffers) throws IOException {
        writeChannelInBulk(buffers);
        verifyChannelInBulk(buffers);
        writeChannelBytePerByte(buffers);
        verifyChannelBytePerByte(buffers);
    }

    private void verifyChannelBytePerByte(byte[][] buffers) throws IOException {
        try (var reader = new FileChannelDirectInputStream(openReadChannel())) {
            for (byte[] expected: buffers) {
                for (byte expect: expected) {
                    assertEquals(expect & 0xFF, reader.read());
                }
            }
        }
    }


    private void verifyChannelInBulk(byte[][] buffers) throws IOException {
        try (var reader = new FileChannelDirectInputStream(openReadChannel())) {
            for (byte[] expected: buffers) {
                byte[] actual = new byte[expected.length];
                reader.read(actual);
                assertArrayEquals(expected, actual);
            }
        }
    }

    private void writeChannelBytePerByte(byte[][] buffers) throws IOException {
        try (var writer = new FileChannelDirectOutputStream(openWriteChannel(), 1)) {
            for (byte[] buf: buffers) {
                for (byte b: buf) {
                    writer.write(b);
                }
            }
        }
    }

    private void writeChannelInBulk(byte[][] buffers) throws IOException {
        try (var writer = new FileChannelDirectOutputStream(openWriteChannel(), 1)) {
            for (byte[] buf: buffers) {
                writer.write(buf);
            }
        }
    }

}
