/*******************************************************************************
 * Copyright (c) 2009 Centrum Wiskunde en Informatica (CWI)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Arnold Lankamp - interfaces and implementation
 *******************************************************************************/
package io.usethesource.vallang.basic;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.ArgumentsMaxDepth;
import io.usethesource.vallang.ArgumentsMaxWidth;
import io.usethesource.vallang.ExpectedType;
import io.usethesource.vallang.GivenValue;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.io.binary.message.IValueReader;
import io.usethesource.vallang.io.binary.message.IValueWriter;
import io.usethesource.vallang.io.binary.stream.IValueInputStream;
import io.usethesource.vallang.io.binary.stream.IValueOutputStream;
import io.usethesource.vallang.io.binary.util.WindowSizes;
import io.usethesource.vallang.io.binary.wire.IWireInputStream;
import io.usethesource.vallang.io.binary.wire.IWireOutputStream;
import io.usethesource.vallang.io.binary.wire.binary.BinaryWireInputStream;
import io.usethesource.vallang.io.binary.wire.binary.BinaryWireOutputStream;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;
import io.usethesource.vallang.visitors.ValueStreams;

public final class BinaryIoSmokeTest extends BooleanStoreProvider {

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testSmallBinaryIO(IValueFactory vf, TypeStore ts, IValue value) throws IOException {
        ioRoundTrip(vf, ts, value);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testRegression40(IValueFactory vf, TypeStore store, 
            @GivenValue("twotups(<\\true(),twotups(<not(\\true()),and(\\false(),\\true())>,<twotups(<couples([]),\\true()>,<or([]),friends([])>),twotups(<or([]),or([])>,<or([]),\\true()>)>)>,<twotups(<not(\\true()),and(\\true(),\\true())>,<twotups(<couples([]),couples([])>,<\\true(),couples([])>),not(\\true())>),and(or([\\true()]),twotups(<or([]),\\true()>,<or([]),\\false()>))>)") 
            @ExpectedType("Boolean") 
            IConstructor t) throws FactTypeUseException, IOException {
        ioRoundTrip(vf, store, t);
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testRegression42_2(IValueFactory vf, TypeStore store, @GivenValue("(\"\"():4,\"\"():3)") IValue v) throws IOException {
        ioRoundTrip(vf, store, v);
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testSmallBinaryFileIO(IValueFactory vf, TypeStore ts, IValue value) throws IOException {
        ioRoundTripFile(vf, ts, value);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class) @ArgumentsMaxDepth(10) @ArgumentsMaxWidth(20)
    public void testLargeBinaryIO(IValueFactory vf, TypeStore ts, IValue value) throws IOException {
        ioRoundTrip(vf, ts, value);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class) @ArgumentsMaxDepth(4) @ArgumentsMaxWidth(8)
     public void testRandomBinaryFileIO(IValueFactory vf, TypeStore ts, IValue value) throws IOException {
        ioRoundTripFile(vf, ts, value);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class) @ArgumentsMaxDepth(10) @ArgumentsMaxWidth(20)
    public void testLargeBinaryFileIO(IValueFactory vf, TypeStore ts, IList list) throws IOException {
        ioRoundTripFile(vf, ts, list);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class) @ArgumentsMaxDepth(10) @ArgumentsMaxWidth(20)
    public void testRandomBinaryLargeFilesIO2(IValueFactory vf, TypeStore ts, IList list) throws FileNotFoundException, IOException {
        ioRoundTripFile2(vf, ts, list);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testConstructorTypeWithLabel(IValueFactory vf, TypeFactory tf, TypeStore ts) throws IOException {
        Type adt = tf.abstractDataType(ts, "A");
        Type cons = tf.constructor(ts, adt, "b", tf.integerType(), "i");
        iopRoundTrip(vf, ts, cons);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testConstructorTypeWithParams(IValueFactory vf, TypeFactory tf, TypeStore ts) throws IOException {
        Type adt = tf.abstractDataType(ts, "A", tf.parameterType("T"));
        Type cons = tf.constructor(ts, adt, "b", tf.parameterType("T"), "tje");
        iopRoundTrip(vf, ts, cons);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testConstructorTypeWithInstanciatedParams(IValueFactory vf, TypeFactory tf, TypeStore ts) throws IOException {
        Type adt = tf.abstractDataType(ts, "A", tf.parameterType("T"));
        Type cons = tf.constructor(ts, adt, "b", tf.parameterType("T"), "tje");

        HashMap<Type, Type> binding = new HashMap<>();
        binding.put(tf.parameterType("T"), tf.integerType());
        iopRoundTrip(vf, ts, cons.instantiate(binding));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class) @ArgumentsMaxDepth(3) @ArgumentsMaxWidth(6)
    public void testSmallRandomTypesIO(IValueFactory vf, TypeStore ts, Type tp) throws IOException {
        iopRoundTrip(vf, ts, tp);
    }

 
    @ParameterizedTest @ArgumentsSource(ValueProvider.class) @ArgumentsMaxDepth(12) @ArgumentsMaxWidth(6)
    public void testDeepRandomValuesIO(IValueFactory vf, TypeStore ts, IValue val) throws IOException {
        try {
            ioRoundTrip(vf, ts, val);
        }
        catch (Throwable e) {
            System.err.println("Deep random value failed, now trying to find the simplest sub-term which causes the failure...");
            // Now we've found a bug in a (probably) very, very complex value.
            // Usually it has several megabytes of random data. 

            // We try to find a minimal sub-value which still fails somehow by
            // visiting each sub-term and running the test again on this.
            // The first sub-term to fail is reported via the AssertionFailed exception.

            // If only the big original term fails after all, then the BottomUp strategy
            // will try that (its the last value of the stream) and fail again in 
            // the same way as above.
            ValueStreams.bottomupbf(val).forEach(v -> {
                try {
                    ioRoundTrip(vf, ts, v);
                } catch (IOException error) {
                    fail(error);
                }
            });
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testConstructorWithParameterized1(IValueFactory vf, TypeFactory tf, TypeStore ts) throws IOException {
        Type adt = tf.abstractDataType(ts, "A", tf.parameterType("T"));
        Type cons = tf.constructor(ts, adt, "b", tf.parameterType("T"), "tje");

        ioRoundTrip(vf, ts, vf.constructor(cons, vf.integer(42)));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testConstructorWithParameterized2(IValueFactory vf, TypeFactory tf, TypeStore ts) throws IOException {
        Type adt = tf.abstractDataType(ts, "A", tf.parameterType("T"));
        Type cons = tf.constructor(ts, adt, "b", tf.parameterType("T"), "tje");

        ioRoundTrip(vf, ts, vf.constructor(cons, vf.integer(42)));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testAliasedTupleInAdt(IValueFactory vf, TypeFactory tf, TypeStore ts) throws IOException {
        Type aliased = tf.aliasType(ts, "XX", tf.integerType());
        Type tuple = tf.tupleType(aliased, tf.stringType());
        Type adt = tf.abstractDataType(ts, "A");
        Type cons = tf.constructor(ts, adt, "b", tuple);

        ioRoundTrip(vf, ts, vf.constructor(cons, vf.tuple(vf.integer(1), vf.string("a"))));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void iopRoundTrip(IValueFactory vf, TypeStore ts, Type tp) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (IWireOutputStream w = new BinaryWireOutputStream(buffer, 1000)) {
            IValueWriter.write(w, vf, WindowSizes.SMALL_WINDOW, tp);
        }
        
        try (IWireInputStream read =
                new BinaryWireInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            Type result = IValueReader.readType(read, vf, () -> ts);
            if (tp != result) {
                String message = "Not equal: \n\t" + result + " expected: " + tp;
                System.err.println(message);
                fail(message);
            }
        }
    }

    private void ioRoundTrip(IValueFactory vf, TypeStore ts, IValue value) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (IValueOutputStream w = new IValueOutputStream(buffer, vf, IValueOutputStream.CompressionRate.Normal)) {
            w.write(value);
        }
        try (IValueInputStream read = new IValueInputStream(new ByteArrayInputStream(buffer.toByteArray()), vf, () -> ts)) {
            IValue result = read.read();
            if (!value.equals(result)) {
                String message = "Not equal:\n\t" + value + " : " + value.getType()
                + "\n\t" + result + " : " + result.getType();
                System.err.println("Test fail:");
                System.err.println(message);
                System.err.flush();
                fail(message);
            }
            else if (value.getType() != result.getType()) {
                String message = "Type's not equal:\n\t" + value.getType() 
                + "\n\t" + result.getType();
                System.err.println(message);
                fail(message);
            }
            else if (value instanceof IConstructor) {
                Type expectedConstructorType = ((IConstructor)value).getConstructorType();
                Type returnedConstructorType = ((IConstructor)result).getConstructorType();
                if (expectedConstructorType != returnedConstructorType) {
                    String message = "Constructor Type's not equal:\n\t" + expectedConstructorType 
                            + "\n\t" + returnedConstructorType;
                    System.err.println(message);
                    fail(message);

                }
            }
        }
    }

    private void ioRoundTripFile(IValueFactory vf, TypeStore ts, IValue value) throws IOException {
        long fileSize = 0;
        File target = File.createTempFile("valllang-test-file", "something");
        target.deleteOnExit();
        try (IValueOutputStream w = new IValueOutputStream(FileChannel.open(target.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE), vf, IValueOutputStream.CompressionRate.Normal)) {
            w.write(value);
        }
        fileSize = Files.size(target.toPath());
        try (IValueInputStream read = new IValueInputStream(FileChannel.open(target.toPath(), StandardOpenOption.READ), vf, () -> ts)) {
            IValue result = read.read();
            if (!value.equals(result)) {
                String message = "Not equal: size: " + fileSize +") \n\t" + value + " : " + value.getType()
                + "\n\t" + result + " : " + result.getType();
                System.err.println(message);
                fail(message);
            }
        }
        finally {
            target.delete();
        }
    }

    private void ioRoundTripFile2(IValueFactory vf, TypeStore ts, IValue value) throws FileNotFoundException, IOException {
        long fileSize = 0;
        File target = File.createTempFile("valllang-test-file", "something");
        target.deleteOnExit();
        try (IValueOutputStream w = new IValueOutputStream(new FileOutputStream(target), vf, IValueOutputStream.CompressionRate.Normal)) {
            w.write(value);
        }
        fileSize = Files.size(target.toPath());
        try (IValueInputStream read = new IValueInputStream(new FileInputStream(target), vf, () -> ts)) {
            IValue result = read.read();
            if (!value.equals(result)) {
                String message = "Not equal: (size: " + fileSize +") \n\t" + value + " : " + value.getType()
                + "\n\t" + result + " : " + result.getType();
                System.err.println(message);
                fail(message);
            }
        }
        finally {
            target.delete();
        }
    }
}
