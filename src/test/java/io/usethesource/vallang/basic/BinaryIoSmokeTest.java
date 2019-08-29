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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Random;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.ExpectedType;
import io.usethesource.vallang.GivenValue;
import io.usethesource.vallang.IBool;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IDateTime;
import io.usethesource.vallang.IExternalValue;
import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IListWriter;
import io.usethesource.vallang.IMap;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.IRational;
import io.usethesource.vallang.IReal;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.io.StandardTextReader;
import io.usethesource.vallang.io.binary.message.IValueReader;
import io.usethesource.vallang.io.binary.message.IValueWriter;
import io.usethesource.vallang.io.binary.stream.IValueInputStream;
import io.usethesource.vallang.io.binary.stream.IValueOutputStream;
import io.usethesource.vallang.io.binary.util.WindowSizes;
import io.usethesource.vallang.io.binary.wire.IWireInputStream;
import io.usethesource.vallang.io.binary.wire.IWireOutputStream;
import io.usethesource.vallang.io.binary.wire.binary.BinaryWireInputStream;
import io.usethesource.vallang.io.binary.wire.binary.BinaryWireOutputStream;
import io.usethesource.vallang.io.old.BinaryWriter;
import io.usethesource.vallang.random.RandomValueGenerator;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;
import io.usethesource.vallang.util.RandomValues;
import io.usethesource.vallang.visitors.BottomUpStreamer;
import io.usethesource.vallang.visitors.BottomUpVisitor;
import io.usethesource.vallang.visitors.IValueVisitor;

/**
 * @author Arnold Lankamp
 */
@SuppressWarnings("deprecation")
public final class BinaryIoSmokeTest extends BooleanStoreProvider {

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testBinaryIO(IValueFactory vf) throws IOException {
        TypeStore ts = new TypeStore();
        RandomValues.addNameType(ts);
        for (IValue value : RandomValues.getTestValues(vf)) {
            ioRoundTrip(vf, ts, value, 0);
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testRegression40(IValueFactory vf, TypeStore store, 
            @GivenValue("twotups(<\\true(),twotups(<not(\\true()),and(\\false(),\\true())>,<twotups(<couples([]),\\true()>,<or([]),friends([])>),twotups(<or([]),or([])>,<or([]),\\true()>)>)>,<twotups(<not(\\true()),and(\\true(),\\true())>,<twotups(<couples([]),couples([])>,<\\true(),couples([])>),not(\\true())>),and(or([\\true()]),twotups(<or([]),\\true()>,<or([]),\\false()>))>)") 
            @ExpectedType("Boolean") 
            IConstructor t) throws FactTypeUseException, IOException {

        ioRoundTrip(vf, store, t, 0);
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testRegression42(IValueFactory vf, TypeStore store) {
        try {
            IValue val = new StandardTextReader().read(vf, new InputStreamReader(getClass().getResourceAsStream("hugeFailingFile1.txt")));
            
            BottomUpStreamer.stream(val).forEach(v -> {
                try {
                    ioRoundTrip(vf, store, v, 0);
                } catch (IOException e) {
                    fail();
                }
            });
            
            
        } catch (FactTypeUseException | IOException e) {
            fail();
        }
    }
    
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testBinaryFileIO(IValueFactory vf) throws IOException {
        TypeStore ts = new TypeStore();
        RandomValues.addNameType(ts);
        for (IValue value : RandomValues.getTestValues(vf)) {
            RandomValues.addNameType(ts);
            ioRoundTripFile(vf, ts, value, 0);
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testRandomBinaryIO(IValueFactory vf) throws IOException {
        TypeStore ts = new TypeStore();
        Type name = RandomValues.addNameType(ts);
        Random r = new Random(42);
        for (int i = 0; i < 20; i++) {
            IValue value = RandomValues.generate(name, ts, vf, r, 10, true);
            ioRoundTrip(vf, ts, value, 42);
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testRandomBinaryFileIO(IValueFactory vf) throws IOException {
        TypeStore ts = new TypeStore();
        Type name = RandomValues.addNameType(ts);
        Random r = new Random(42);
        for (int i = 0; i < 20; i++) {
            IValue value = RandomValues.generate(name, ts, vf, r, 10, true);
            ioRoundTripFile(vf, ts, value, 42);
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testRandomBinaryLargeFilesIO(IValueFactory vf) throws IOException {
        TypeStore ts = new TypeStore();
        Type name = RandomValues.addNameType(ts);
        Random r = new Random();
        int seed = r.nextInt();
        r.setSeed(seed);
        IListWriter writer = vf.listWriter();
        for (int i = 0; i < 5; i++) {
            writer.append(RandomValues.generate(name, ts, vf, r, 10, true));      
        }
        ioRoundTripFile(vf, ts, writer.done(), seed);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testRandomBinaryLargeFilesIO2(IValueFactory vf) throws FileNotFoundException, IOException {
        TypeStore ts = new TypeStore();
        Type name = RandomValues.addNameType(ts);
        Random r = new Random();
        int seed = r.nextInt();
        r.setSeed(seed);
        IListWriter writer = vf.listWriter();
        for (int i = 0; i < 5; i++) {
            writer.append(RandomValues.generate(name, ts, vf, r, 10, true));      
        }
        ioRoundTripFile2(vf, ts, writer.done(), seed);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testConstructorTypeWithLabel(IValueFactory vf) throws IOException {
        TypeFactory tf = TypeFactory.getInstance();
        TypeStore ts = new TypeStore();
        Type adt = tf.abstractDataType(ts, "A");
        Type cons = tf.constructor(ts, adt, "b", tf.integerType(), "i");
        iopRoundTrip(vf, ts, cons, 0);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testConstructorTypeWithParams(IValueFactory vf) throws IOException {
        TypeFactory tf = TypeFactory.getInstance();
        TypeStore ts = new TypeStore();
        Type adt = tf.abstractDataType(ts, "A", tf.parameterType("T"));
        Type cons = tf.constructor(ts, adt, "b", tf.parameterType("T"), "tje");
        iopRoundTrip(vf, ts, cons, 0);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testConstructorTypeWithInstanciatedParams(IValueFactory vf) throws IOException {
        TypeFactory tf = TypeFactory.getInstance();
        TypeStore ts = new TypeStore();
        Type adt = tf.abstractDataType(ts, "A", tf.parameterType("T"));
        Type cons = tf.constructor(ts, adt, "b", tf.parameterType("T"), "tje");

        HashMap<Type, Type> binding = new HashMap<>();
        binding.put(tf.parameterType("T"), tf.integerType());
        iopRoundTrip(vf, ts, cons.instantiate(binding), 0);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testRandomTypesIO(IValueFactory vf) throws IOException {
        TypeFactory tf = TypeFactory.getInstance();
        TypeStore ts = new TypeStore();
        Random r = new Random();
        int seed = r.nextInt();
        r.setSeed(seed);
        for (int i = 0; i < 1000; i++) {
            Type tp = tf.randomType(ts, r, 5);
            iopRoundTrip(vf, ts, tp, seed);
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testSmallRandomTypesIO(IValueFactory vf) throws IOException {
        TypeFactory tf = TypeFactory.getInstance();
        TypeStore ts = new TypeStore();
        Random r = new Random();
        int seed = r.nextInt();
        r.setSeed(seed);
        for (int i = 0; i < 1000; i++) {
            Type tp = tf.randomType(ts, r, 3);
            iopRoundTrip(vf, ts, tp, seed);
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testDeepRandomValuesIO(IValueFactory vf) throws IOException {
        Random r = new Random();
        int seed = r.nextInt();

        testDeepRandomValuesIO(seed, vf);
    }

    private void testDeepRandomValuesIO(int seed, IValueFactory vf) throws IOException {
        TypeFactory tf = TypeFactory.getInstance();
        TypeStore ts = new TypeStore();
        Random r = new Random(seed);
        RandomValueGenerator gen = new RandomValueGenerator(vf, r, 22, 6, true);
        for (int i = 0; i < 1000; i++) {
            IValue val = gen.generate(tf.valueType(), ts, null);
            ioRoundTrip(vf, ts, val, seed);
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testDeepRandomValuesIORegressions(IValueFactory vf) throws IOException {
        testDeepRandomValuesIO(1544959898, vf);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testOldFilesStillSupported(IValueFactory vf) throws IOException {
        TypeStore ts = new TypeStore();
        Type name = RandomValues.addNameType(ts);
        Random r = new Random(42);
        for (int i = 0; i < 20; i++) {
            IValue value = RandomValues.generate(name, ts, vf, r, 10, true);
            ioRoundTripOld(vf, ts, value, 42);
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testConstructorWithParameterized1(IValueFactory vf) throws IOException {
        TypeStore ts = new TypeStore();
        TypeFactory tf = TypeFactory.getInstance();
        Type adt = tf.abstractDataType(ts, "A", tf.parameterType("T"));
        Type cons = tf.constructor(ts, adt, "b", tf.parameterType("T"), "tje");

        ioRoundTrip(vf, ts, vf.constructor(cons, vf.integer(42)), 0);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testConstructorWithParameterized2(IValueFactory vf) throws IOException {
        TypeStore ts = new TypeStore();
        TypeFactory tf = TypeFactory.getInstance();
        Type adt = tf.abstractDataType(ts, "A", tf.parameterType("T"));
        Type cons = tf.constructor(ts, adt, "b", tf.parameterType("T"), "tje");

        ioRoundTrip(vf, ts, vf.constructor(cons, vf.integer(42)), 0);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testAliasedTupleInAdt(IValueFactory vf) throws IOException {
        TypeStore ts = new TypeStore();
        TypeFactory tf = TypeFactory.getInstance();
        Type aliased = tf.aliasType(ts, "XX", tf.integerType());
        Type tuple = tf.tupleType(aliased, tf.stringType());
        Type adt = tf.abstractDataType(ts, "A");
        Type cons = tf.constructor(ts, adt, "b", tuple);

        ioRoundTrip(vf, ts, vf.constructor(cons, vf.tuple(vf.integer(1), vf.string("a"))), 0);
    }

    private void iopRoundTrip(IValueFactory vf, TypeStore ts, Type tp, int seed) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (IWireOutputStream w = new BinaryWireOutputStream(buffer, 1000)) {
            IValueWriter.write(w, vf, WindowSizes.SMALL_WINDOW, tp);
        }
        try (IWireInputStream read =
                new BinaryWireInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            Type result = IValueReader.readType(read, vf, () -> ts);
            if (!tp.equals(result)) {
                String message = "Not equal: (seed: " + seed + ") \n\t" + result + " expected: " + seed;
                System.err.println(message);
                fail(message);
            }
        }
    }

    private void ioRoundTrip(IValueFactory vf, TypeStore ts, IValue value, int seed) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (IValueOutputStream w = new IValueOutputStream(buffer, vf, IValueOutputStream.CompressionRate.Normal)) {
            w.write(value);
        }
        try (IValueInputStream read = new IValueInputStream(new ByteArrayInputStream(buffer.toByteArray()), vf, () -> ts)) {
            IValue result = read.read();
            if (!value.isEqual(result)) {
                String message = "Not equal: (seed: " + seed + ") \n\t" + value + " : " + value.getType()
                + "\n\t" + result + " : " + result.getType();
                System.err.println(message);
                fail(message);
            }
            else if (value.getType() != result.getType()) {
                String message = "Type's not equal: (seed: " + seed + ") \n\t" + value.getType() 
                + "\n\t" + result.getType();
                System.err.println(message);
                fail(message);
            }
            else if (value instanceof IConstructor) {
                Type expectedConstructorType = ((IConstructor)value).getConstructorType();
                Type returnedConstructorType = ((IConstructor)result).getConstructorType();
                if (expectedConstructorType != returnedConstructorType) {
                    String message = "Constructor Type's not equal: (seed: " + seed + ") \n\t" + expectedConstructorType 
                            + "\n\t" + returnedConstructorType;
                    System.err.println(message);
                    fail(message);

                }
            }
        }
    }

    private void ioRoundTripFile(IValueFactory vf, TypeStore ts, IValue value, int seed) throws IOException {
        long fileSize = 0;
        File target = File.createTempFile("valllang-test-file", "for-" + seed);
        target.deleteOnExit();
        try (IValueOutputStream w = new IValueOutputStream(FileChannel.open(target.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE), vf, IValueOutputStream.CompressionRate.Normal)) {
            w.write(value);
        }
        fileSize = Files.size(target.toPath());
        try (IValueInputStream read = new IValueInputStream(FileChannel.open(target.toPath(), StandardOpenOption.READ), vf, () -> ts)) {
            IValue result = read.read();
            if (!value.isEqual(result)) {
                String message = "Not equal: (seed: " + seed + ", size: " + fileSize +") \n\t" + value + " : " + value.getType()
                + "\n\t" + result + " : " + result.getType();
                System.err.println(message);
                fail(message);
            }
        }
        finally {
            target.delete();
        }
    }

    private void ioRoundTripFile2(IValueFactory vf, TypeStore ts, IValue value, int seed) throws FileNotFoundException, IOException {
        long fileSize = 0;
        File target = File.createTempFile("valllang-test-file", "for-" + seed);
        target.deleteOnExit();
        try (IValueOutputStream w = new IValueOutputStream(new FileOutputStream(target), vf, IValueOutputStream.CompressionRate.Normal)) {
            w.write(value);
        }
        fileSize = Files.size(target.toPath());
        try (IValueInputStream read = new IValueInputStream(new FileInputStream(target), vf, () -> ts)) {
            IValue result = read.read();
            if (!value.isEqual(result)) {
                String message = "Not equal: (seed: " + seed + ", size: " + fileSize +") \n\t" + value + " : " + value.getType()
                + "\n\t" + result + " : " + result.getType();
                System.err.println(message);
                fail(message);
            }
        }
        finally {
            target.delete();
        }
    }

    private void ioRoundTripOld(IValueFactory vf, TypeStore ts, IValue value, int seed) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        BinaryWriter w = new BinaryWriter(value, buffer, new TypeStore());
        w.serialize();
        buffer.flush();
        try (IValueInputStream read = new IValueInputStream(
                new ByteArrayInputStream(buffer.toByteArray()), vf, () -> ts)) {
            IValue result = read.read();
            if (!value.isEqual(result)) {
                String message = "Not equal: (seed: " + seed + ") \n\t" + value + " : " + value.getType()
                + "\n\t" + result + " : " + result.getType();
                System.err.println(message);
                fail(message);
            }
        }
    }
}
