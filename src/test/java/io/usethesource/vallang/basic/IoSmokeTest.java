/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

 *******************************************************************************/

package io.usethesource.vallang.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.ExpectedType;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.exceptions.UnsupportedTypeException;
import io.usethesource.vallang.io.StandardTextReader;
import io.usethesource.vallang.io.StandardTextWriter;
import io.usethesource.vallang.io.XMLReader;
import io.usethesource.vallang.io.XMLWriter;
import io.usethesource.vallang.io.binary.SerializableValue;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

@SuppressWarnings("unused")
public class IoSmokeTest {

    public static TypeStore store = new TypeStore();
    private static TypeFactory tf = TypeFactory.getInstance();
    private static Type Boolean = tf.abstractDataType(store, "Boolean");

    private static Type Name = tf.abstractDataType(store, "Name");
    private static Type True = tf.constructor(store, Boolean, "true");
    
    private static Type False = tf.constructor(store, Boolean, "false");
    private static Type And = tf.constructor(store, Boolean, "and", Boolean, Boolean);
    private static Type Or = tf.constructor(store, Boolean, "or", tf.listType(Boolean));
    private static Type Not = tf.constructor(store, Boolean, "not", Boolean);
    private static Type TwoTups = tf.constructor(store, Boolean, "twotups",
            tf.tupleType(Boolean, Boolean), tf.tupleType(Boolean, Boolean));
    private static Type NameNode = tf.constructor(store, Name, "name", tf.stringType());
    private static Type Friends = tf.constructor(store, Boolean, "friends", tf.listType(Name));
    private static Type Couples = tf.constructor(store, Boolean, "couples", tf.lrelType(Name, Name));

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testSerializable(IValueFactory vf, @ExpectedType("Boolean") IConstructor t) throws IOException {
        SerializableValue<IValue> v = new SerializableValue<IValue>(vf, t);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        v.write(buf);
        SerializableValue<IValue> w =
                SerializableValue.<IValue>read(new ByteArrayInputStream(buf.toByteArray()));

        if (!v.getValue().isEqual(w.getValue())) {
            fail();
        }
    }

    private static IValue name(IValueFactory vf, String n) {
        return vf.constructor(NameNode, vf.string(n));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testToString(IValueFactory vf) throws FactTypeUseException, IOException {
        StandardTextReader reader = new StandardTextReader();
        TypeFactory TF = TypeFactory.getInstance();

        // there are specialized implementations for different sizes of constructors.
        // each must have a specialized toString implementation to work correctly.

        TypeStore store = new TypeStore();
        Type A = TF.abstractDataType(store, "A");

        Type[] examples = new Type[] {
                TF.constructor(store, A, "x1", TF.integerType(), "a1"),
                TF.constructor(store, A, "x2", TF.integerType(), "a1", TF.integerType(), "a2"),
                TF.constructor(store, A, "x3", TF.integerType(), "a1", TF.integerType(), "a2", TF.integerType(), "a3"),
                TF.constructor(store, A, "x4", TF.integerType(), "a1", TF.integerType(), "a2", TF.integerType(), "a3", TF.integerType(), "a4"),
                TF.constructor(store, A, "x5", TF.integerType(), "a1", TF.integerType(), "a2", TF.integerType(), "a3", TF.integerType(), "a4", TF.integerType(), "a5"),
                TF.constructor(store, A, "x6", TF.integerType(), "a1", TF.integerType(), "a2", TF.integerType(), "a3", TF.integerType(), "a4", TF.integerType(), "a5", TF.integerType(), "a6"),
                TF.constructor(store, A, "x7", TF.integerType(), "a1", TF.integerType(), "a2", TF.integerType(), "a3", TF.integerType(), "a4", TF.integerType(), "a5", TF.integerType(), "a6", TF.integerType(), "a7"),
                TF.constructor(store, A, "x8", TF.integerType(), "a1", TF.integerType(), "a2", TF.integerType(), "a3", TF.integerType(), "a4", TF.integerType(), "a5", TF.integerType(), "a6", TF.integerType(), "a7", TF.integerType(), "a8"),
        };

        for (int i = 0; i < 8; i++) {
            IValue[] kids = new IValue[i];
            for (int k = 0; k < i; k++) {
                kids[k] = vf.integer(k);
            }
            IConstructor cons = vf.constructor(examples[i], kids);
            String string = cons.toString();
            IValue result = reader.read(vf, store, A, new StringReader(string));

            assertEquals(result, cons);
        }

    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testStandardReader(IValueFactory vf) throws FactTypeUseException, IOException {
        StandardTextReader reader = new StandardTextReader();

        IValue s = reader.read(vf, new StringReader("\"a b c\""));
        assertEquals(s, vf.string("a b c"));

        IValue v = reader.read(vf, new StringReader("\"f\"(\"a b c\")"));
        assertEquals(v, vf.node("f", vf.string("a b c")));

        IValue vv = reader.read(vf, new StringReader("\"f\"(\"a b c\", x=1)"));
        assertEquals(vv, vf.node("f", vf.string("a b c")).asWithKeywordParameters().setParameter("x", vf.integer(1)));

        //      System.err.println(vf.constructor(True).asWithKeywordParameters().setParameter("x", vf.integer(1)));
        IValue vvv = reader.read(vf, store, Boolean, new StringReader("\\true(x=1)"));
        assertEquals(vvv, vf.constructor(True).asWithKeywordParameters().setParameter("x", vf.integer(1)));

        IValue r = reader.read(vf, new StringReader("[1.7976931348623157E+308]"));
        System.err.println(r);
        assertEquals(r, vf.list(vf.real("1.7976931348623157E+308")));

        IValue m = reader.read(vf, new StringReader("()"));
        System.err.println(m);
        assertEquals(m, vf.mapWriter().done());

        IValue t = reader.read(vf, new StringReader("<()>"));
        System.err.println(t);
        assertEquals(t, vf.tuple(vf.mapWriter().done()));

        StringWriter w = new StringWriter();
        new StandardTextWriter().write(vf.tuple(), w);
        IValue u = reader.read(vf, new StringReader(w.toString()));
        System.err.println(u);
        assertEquals(u, vf.tuple());
    }

}
