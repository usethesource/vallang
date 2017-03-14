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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Random;

import io.usethesource.vallang.IListWriter;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.Setup;
import io.usethesource.vallang.io.binary.message.IValueReader;
import io.usethesource.vallang.io.binary.message.IValueWriter;
import io.usethesource.vallang.io.binary.stream.IValueInputStream;
import io.usethesource.vallang.io.binary.stream.IValueOutputStream;
import io.usethesource.vallang.io.binary.util.WindowSizes;
import io.usethesource.vallang.io.binary.wire.binary.BinaryWireInputStream;
import io.usethesource.vallang.io.binary.wire.binary.BinaryWireOutputStream;
import io.usethesource.vallang.io.old.BinaryWriter;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;
import io.usethesource.vallang.util.RandomValues;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.io.binary.wire.IWireInputStream;
import io.usethesource.vallang.io.binary.wire.IWireOutputStream;
import io.usethesource.vallang.type.Type;

import static org.junit.Assert.fail;

/**
 * @author Arnold Lankamp
 */
@RunWith(Parameterized.class)
public final class BinaryIoSmokeTest {

  @Parameterized.Parameters
  public static Iterable<? extends Object> data() {
    return Setup.valueFactories();
  }

  private final IValueFactory vf;

  public BinaryIoSmokeTest(IValueFactory vf) {
    this.vf = vf;
  }

  @Test
  public void testBinaryIO() {
    TypeStore ts = new TypeStore();
    RandomValues.addNameType(ts);
    for (IValue value : RandomValues.getTestValues(vf)) {
      ioRoundTrip(value, 0);
    }
  }

  @Test
  public void testRandomBinaryIO() {
    TypeStore ts = new TypeStore();
    Type name = RandomValues.addNameType(ts);
    Random r = new Random(42);
    for (int i = 0; i < 20; i++) {
      IValue value = RandomValues.generate(name, ts, vf, r, 10);
      ioRoundTrip(value, 42);
    }
  }
  
  @Test
  public void testRandomBinaryLargeFilesIO() {
    TypeStore ts = new TypeStore();
    Type name = RandomValues.addNameType(ts);
    Random r = new Random();
    int seed = r.nextInt();
    r.setSeed(seed);
    IListWriter writer = vf.listWriter();
    for (int i = 0; i < 20; i++) {
        writer.append(RandomValues.generate(name, ts, vf, r, 10));      
    }
    ioRoundTripFile(writer.done(), seed);
  }


  @Test
  public void testConstructorTypeWithLabel() {
    TypeFactory tf = TypeFactory.getInstance();
    TypeStore ts = new TypeStore();
    Type adt = tf.abstractDataType(ts, "A");
    Type cons = tf.constructor(ts, adt, "b", tf.integerType(), "i");
    iopRoundTrip(cons, 0);
  }

  @Test
  public void testConstructorTypeWithParams() {
    TypeFactory tf = TypeFactory.getInstance();
    TypeStore ts = new TypeStore();
    Type adt = tf.abstractDataType(ts, "A", tf.parameterType("T"));
    Type cons = tf.constructor(ts, adt, "b", tf.parameterType("T"), "tje");
    iopRoundTrip(cons, 0);
  }

  @Test
  public void testConstructorTypeWithInstanciatedParams() {
    TypeFactory tf = TypeFactory.getInstance();
    TypeStore ts = new TypeStore();
    Type adt = tf.abstractDataType(ts, "A", tf.parameterType("T"));
    Type cons = tf.constructor(ts, adt, "b", tf.parameterType("T"), "tje");

    HashMap<Type, Type> binding = new HashMap<>();
    binding.put(tf.parameterType("T"), tf.integerType());
    iopRoundTrip(cons.instantiate(binding), 0);
  }

  @Test
  public void testRandomTypesIO() {
    TypeFactory tf = TypeFactory.getInstance();
    TypeStore ts = new TypeStore();
    Random r = new Random();
    int seed = r.nextInt();
    r.setSeed(seed);
    for (int i = 0; i < 1000; i++) {
      Type tp = tf.randomType(ts, r, 5);
      iopRoundTrip(tp, seed);
    }
  }

  @Test
  public void testSmallRandomTypesIO() {
    TypeFactory tf = TypeFactory.getInstance();
    TypeStore ts = new TypeStore();
    Random r = new Random();
    int seed = r.nextInt();
    r.setSeed(seed);
    for (int i = 0; i < 1000; i++) {
      Type tp = tf.randomType(ts, r, 3);
      iopRoundTrip(tp, seed);
    }
  }

  @Test
  public void testOldFilesStillSupported() {
    TypeStore ts = new TypeStore();
    Type name = RandomValues.addNameType(ts);
    Random r = new Random(42);
    for (int i = 0; i < 20; i++) {
      IValue value = RandomValues.generate(name, ts, vf, r, 10);
      ioRoundTripOld(value, 42);
    }
  }

  @Test
  public void testConstructorWithParameterized1() {
    TypeStore ts = new TypeStore();
    TypeFactory tf = TypeFactory.getInstance();
    Type adt = tf.abstractDataType(ts, "A", tf.parameterType("T"));
    Type cons = tf.constructor(ts, adt, "b", tf.parameterType("T"), "tje");

    HashMap<Type, Type> binding = new HashMap<>();
    binding.put(tf.parameterType("T"), tf.integerType());
    ioRoundTrip(vf.constructor(cons.instantiate(binding), vf.integer(42)), 0);
  }

  @Test
  public void testConstructorWithParameterized2() {
    TypeStore ts = new TypeStore();
    TypeFactory tf = TypeFactory.getInstance();
    Type adt = tf.abstractDataType(ts, "A", tf.parameterType("T"));
    Type cons = tf.constructor(ts, adt, "b", tf.parameterType("T"), "tje");

    ioRoundTrip(vf.constructor(cons, vf.integer(42)), 0);
  }

  private void iopRoundTrip(Type tp, int seed) {
    try {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      try (IWireOutputStream w = new BinaryWireOutputStream(buffer, 1000)) {
        IValueWriter.write(w, vf, WindowSizes.SMALL_WINDOW, tp);
      }
      try (IWireInputStream read =
          new BinaryWireInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
        Type result = IValueReader.readType(read, vf, Setup.TYPE_STORE_SUPPLIER);
        if (!tp.equals(result)) {
          String message = "Not equal: (seed: " + seed + ") \n\t" + result + " expected: " + seed;
          System.err.println(message);
          fail(message);
        }
      }
    } catch (IOException ioex) {
      ioex.printStackTrace();
      fail(ioex.getMessage());
    }
  }

  private void ioRoundTrip(IValue value, int seed) {
    try {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      try (IValueOutputStream w = new IValueOutputStream(buffer, vf, IValueOutputStream.CompressionRate.Normal)) {
        w.write(value);
      }
      try (IValueInputStream read = new IValueInputStream(
          new ByteArrayInputStream(buffer.toByteArray()), vf, Setup.TYPE_STORE_SUPPLIER)) {
        IValue result = read.read();
        if (!value.isEqual(result)) {
          String message = "Not equal: (seed: " + seed + ") \n\t" + value + " : " + value.getType()
              + "\n\t" + result + " : " + result.getType();
          System.err.println(message);
          fail(message);
        }
      }
    } catch (IOException ioex) {
      ioex.printStackTrace();
      fail(ioex.getMessage());
    }
  }
  private void ioRoundTripFile(IValue value, int seed) {
      try {
          File target = File.createTempFile("valllang-test-file", "for-" + seed);
          target.deleteOnExit();
          try (IValueOutputStream w = new IValueOutputStream(new FileOutputStream(target), vf, IValueOutputStream.CompressionRate.Normal)) {
              w.write(value);
          }
          try (IValueInputStream read = new IValueInputStream(FileChannel.open(target.toPath(), StandardOpenOption.READ), vf, Setup.TYPE_STORE_SUPPLIER)) {
              IValue result = read.read();
              if (!value.isEqual(result)) {
                  String message = "Not equal: (seed: " + seed + ") \n\t" + value + " : " + value.getType()
                  + "\n\t" + result + " : " + result.getType();
                  System.err.println(message);
                  fail(message);
              }
          }
          finally {
              target.delete();
          }
      } catch (IOException ioex) {
          ioex.printStackTrace();
          fail(ioex.getMessage());
      }
  }


  private void ioRoundTripOld(IValue value, int seed) {
    try {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      BinaryWriter w = new BinaryWriter(value, buffer, new TypeStore());
      w.serialize();
      buffer.flush();
      try (IValueInputStream read = new IValueInputStream(
          new ByteArrayInputStream(buffer.toByteArray()), vf, Setup.TYPE_STORE_SUPPLIER)) {
        IValue result = read.read();
        if (!value.isEqual(result)) {
          String message = "Not equal: (seed: " + seed + ") \n\t" + value + " : " + value.getType()
              + "\n\t" + result + " : " + result.getType();
          System.err.println(message);
          fail(message);
        }
      }
    } catch (IOException ioex) {
      ioex.printStackTrace();
      fail(ioex.getMessage());
    }
  }
}
