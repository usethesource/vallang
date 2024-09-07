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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.io.StandardTextReader;
import io.usethesource.vallang.io.StandardTextWriter;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

public class IoSmokeTest extends BooleanStoreProvider {

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
            IValue[] kids = new IValue[examples[i].getArity()];
            for (int k = 0; k < examples[i].getArity(); k++) {
                kids[k] = vf.integer(k);
            }
            IConstructor cons = vf.constructor(examples[i], kids);
            String string = cons.toString();
            IValue result = reader.read(vf, store, A, new StringReader(string));

            assertEquals(result, cons);
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testParametrizedDataType(IValueFactory vf, TypeFactory tf, TypeStore store) throws FactTypeUseException, IOException {
        Type T = tf.parameterType("T");
        Type MaybeT = tf.abstractDataType(store, "Maybe", T);
        Type just = tf.constructor(store, MaybeT, "just", T, "t");
        Type MaybeInt = MaybeT.instantiate(Map.of(T, tf.integerType()));
        Type B = tf.abstractDataType(store, "B");
        Type f = tf.constructor(store, B, "f", MaybeInt);

        StandardTextReader reader = new StandardTextReader();

        IValue s = reader.read(vf, store, B, new StringReader("f(just(1))"));
        assertEquals(vf.constructor(f, vf.constructor(just, vf.integer(1))), s);
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

        IValue vvv = reader.read(vf, store, Boolean, new StringReader("\\true(x=1)"));
        assertEquals(vvv, vf.constructor(True).asWithKeywordParameters().setParameter("x", vf.integer(1)));

        IValue r = reader.read(vf, new StringReader("[1.7976931348623157E+308]"));
        assertEquals(r, vf.list(vf.real("1.7976931348623157E+308")));

        IValue m = reader.read(vf, new StringReader("()"));
        assertEquals(m, vf.mapWriter().done());

        IValue t = reader.read(vf, new StringReader("<()>"));
        assertEquals(t, vf.tuple(vf.mapWriter().done()));

        StringWriter w = new StringWriter();
        new StandardTextWriter().write(vf.tuple(), w);
        IValue u = reader.read(vf, new StringReader(w.toString()));
        assertEquals(u, vf.tuple());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testLegacyAnnotationTextReaderNode(IValueFactory vf) throws FactTypeUseException, IOException {
        String input = "\"node\"()[@anno=2]";
        StandardTextReader reader = new StandardTextReader();
        IValue s = reader.read(vf, new StringReader(input));

        assertEquals(s.asWithKeywordParameters().getParameter("anno"), vf.integer(2));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testLegacyAnnotationTextReaderConstructor(IValueFactory vf, TypeFactory tf, TypeStore store) throws FactTypeUseException, IOException {
        Type A = tf.abstractDataType(store, "A");
        tf.constructor(store, A, "a");
        store.declareKeywordParameter(A, "anno", tf.integerType());

        String input = "a()[@anno=2]";
        StandardTextReader reader = new StandardTextReader();
        IValue s = reader.read(vf, store, A, new StringReader(input));

        assertEquals(s.asWithKeywordParameters().getParameter("anno"), vf.integer(2));
    }
}
