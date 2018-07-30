package io.usethesource.vallang.io;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;

import org.junit.Test;

import io.usethesource.vallang.io.binary.util.DelayedZstdOutputStream;
import io.usethesource.vallang.io.binary.util.DirectZstdInputStream;
import io.usethesource.vallang.io.binary.util.FileChannelDirectInputStream;
import io.usethesource.vallang.io.binary.util.FileChannelDirectOutputStream;

public class ZstdWrappingStreamsTest {
    
    private static File getTempFile(String name) throws IOException {
        File result = File.createTempFile(ZstdWrappingStreamsTest.class.getName(), "temp-file-" + name);
        result.deleteOnExit();
        return result;
    }
    @Test
    public void writeSmallFile() throws IOException {
        File temp = getTempFile("small");
        try {
            writeCompressed(temp, getRandomBytes(10));
            assertNotEquals(0, Files.size(temp.toPath()));
        } 
        finally {
            temp.delete();
        }
    }
    
    @Test
    public void writeLargeFile() throws IOException {
        File temp = getTempFile("large");
        try {
            writeCompressed(temp, getRandomBytes(10_000));
            assertNotEquals(0, Files.size(temp.toPath()));
        } 
        finally {
            temp.delete();
        }
    }
    
    @Test
    public void writeHugeFile() throws IOException {
        File temp = getTempFile("large");
        try {
            writeCompressed(temp, getRandomBytes(8_000_000));
            assertNotEquals(0, Files.size(temp.toPath()));
        } 
        finally {
            temp.delete();
        }
    }

    
    @Test
    public void roundTripSmallFile() throws IOException {
        roundTrip(getRandomBytes(10));
    }

    @Test
    public void roundTripLargeFile() throws IOException {
        roundTrip(getRandomBytes(10_000));
    }
    
    @Test
    public void roundTripHugeFile() throws IOException {
        roundTrip(getRandomBytes(8_000_000));
    }
    
    @Test
    public void roundTripRandomSizes() throws IOException {
        Random r = new Random();
        for (int i = 1; i < 100; i++) {
            roundTrip(getRandomBytes(1 + r.nextInt(1_000_000)));
        }
    }
    
    @Test
    public void roundTripSmallSizes() throws IOException {
        for (int i = 1; i < 100; i++) {
            roundTrip(getRandomBytes(i));
        }
    }
    
    @Test 
    public void multiThreadedWrite() throws Throwable {
        int THREADS = 100;
        final Collection<Throwable> fails = new ConcurrentLinkedDeque<>();
        final Semaphore startRunning = new Semaphore(0);
        final Semaphore doneRunning = new Semaphore(0);
        for (int i = 0; i < THREADS; i++) {
            Random r = new Random();
            r.setSeed(i + r.nextInt());
            new Thread(() -> {
                try {
                    byte[] bytes = getRandomBytes(1 + r.nextInt(255));
                    startRunning.acquire();
                    roundTrip(bytes);
                } 
                catch (Throwable t) {
                    fails.add(t);
                }
                finally {
                    doneRunning.release();
                }
                
            }).start();
        }
        startRunning.release(THREADS);
        doneRunning.acquire(THREADS);
        if (!fails.isEmpty()) {
            throw fails.iterator().next();
        }
    }

    private byte[] getRandomBytes(int size) {
        Random r = new Random();
        byte[] data = new byte[size];
        r.nextBytes(data);
        return data;
    }
    
    private void roundTrip(byte[] data) throws IOException {
        File temp = getTempFile("roundtrip");
        try {
            writeCompressed(temp, data);
            byte[] data2 = readCompressed(temp);
            assertArrayEquals(data, data2);
            
            temp.delete();
            writeCompressedOneBytePerTime(temp, data);
            data2 = readCompressed(temp);
            assertArrayEquals(data, data2);

        } 
        finally {
            temp.delete();
        }
    }
    private void writeCompressedOneBytePerTime(File temp, byte[] data) throws IOException {
        try (DelayedZstdOutputStream stream = new DelayedZstdOutputStream(new FileChannelDirectOutputStream(FileChannel.open(temp.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE), 10), 1, 3)) {
            stream.write(ByteBuffer.allocate(4).putInt(data.length).array());
            for (byte b: data) {
                stream.write(b);
            }
        }
        
    }
    private void writeCompressed(File temp, byte[] data) throws IOException {
        try (DelayedZstdOutputStream stream = new DelayedZstdOutputStream(new FileChannelDirectOutputStream(FileChannel.open(temp.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE), 10), 1, 3)) {
            stream.write(ByteBuffer.allocate(4).putInt(data.length).array());
            stream.write(data);
        }
    }
        
    private byte[] readCompressed(File temp) throws IOException {
        try (FileChannelDirectInputStream raw = new FileChannelDirectInputStream(FileChannel.open(temp.toPath(), StandardOpenOption.READ))) {
            if (raw.read() == 0) {
                return getBytes(raw);
            }
            else {
                try (DirectZstdInputStream dec = new DirectZstdInputStream(raw)) {
                    return getBytes(dec);
                }
            }
        }
    }
    
    private byte[] getBytes(InputStream raw) throws IOException {
        ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
        lengthBuffer.put((byte)raw.read());
        lengthBuffer.put((byte)raw.read());
        lengthBuffer.put((byte)raw.read());
        lengthBuffer.put((byte)raw.read());
        lengthBuffer.flip();
        byte[] result = new byte[lengthBuffer.getInt()];
        int read = 0;
        while (read < result.length) {
            read += raw.read(result, read, result.length - read);
        }
        return result;
    }
}
