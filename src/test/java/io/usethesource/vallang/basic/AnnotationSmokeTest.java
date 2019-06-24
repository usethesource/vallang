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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IMap;
import io.usethesource.vallang.IMapWriter;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.exceptions.FactTypeDeclarationException;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

@SuppressWarnings("deprecation")
public final class AnnotationSmokeTest {
    private static TypeFactory tf = TypeFactory.getInstance();
    public static TypeStore store = new TypeStore();
    private static Type E = tf.abstractDataType(store, "E");
    private static Type N = tf.constructor(store, E, "n", tf.integerType());

    static {
        store.declareAnnotation(E, "x", tf.integerType());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testDeclarationOnNonAllowedType(TypeStore ts) {
        try {
            ts.declareAnnotation(tf.integerType(), "a", tf.integerType());
        } catch (FactTypeDeclarationException e) {
            // this should happen
        }
        try {
            ts.declareAnnotation(tf.realType(), "a", tf.integerType());
        } catch (FactTypeDeclarationException e) {
            // this should happen
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testDoubleDeclaration(TypeStore ts) {
        try {
            ts.declareAnnotation(E, "size", tf.integerType());
        } catch (FactTypeDeclarationException | FactTypeUseException e) {
            fail(e.toString());
        }

        try {
            ts.declareAnnotation(E, "size", tf.realType());
            fail("double declaration is not allowed");
        } catch (FactTypeDeclarationException e) {
            // this should happen
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testSetAnnotation(TypeStore ts, IValueFactory vf) {
        IConstructor n = vf.constructor(N, vf.integer(0));
        ts.declareAnnotation(E, "size", tf.integerType());

        try {
            n.asAnnotatable().setAnnotation("size", vf.integer(0));
        } catch (FactTypeDeclarationException | FactTypeUseException e) {
            fail(e.toString());
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testGetAnnotation(IValueFactory vf, TypeStore ts) {
        IConstructor n = vf.constructor(N, vf.integer(0));
        ts.declareAnnotation(E, "size", tf.integerType());

        try {
            if (n.asAnnotatable().getAnnotation("size") != null) {
                fail("annotation should be null");
            }
        } catch (FactTypeUseException e) {
            fail(e.toString());
        }

        IConstructor m = n.asAnnotatable().setAnnotation("size", vf.integer(1));
        IValue b = m.asAnnotatable().getAnnotation("size");
        if (!b.isEqual(vf.integer(1))) {
            fail();
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testImmutability(IValueFactory vf, TypeStore ts) {
        IConstructor n = vf.constructor(N, vf.integer(0));
        ts.declareAnnotation(E, "size", tf.integerType());

        IConstructor m = n.asAnnotatable().setAnnotation("size", vf.integer(1));

        if (m == n) {
            fail("annotation setting should change object identity");
        }

        assertTrue(m.isEqual(n));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testDeclaresAnnotation(IValueFactory vf, TypeStore ts) {
        IConstructor n = vf.constructor(N, vf.integer(0));
        ts.declareAnnotation(E, "size", tf.integerType());

        if (!n.declaresAnnotation(ts, "size")) {
            fail();
        }

        if (n.declaresAnnotation(ts, "size2")) {
            fail();
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testEqualityNode(IValueFactory vf, TypeStore ts) {
        final INode n = vf.node("hello");
        final INode na = n.asAnnotatable().setAnnotation("audience", vf.string("world"));

        assertEqualityOfValueWithAndWithoutAnnotations(n, na, vf);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testEqualityConstructor(IValueFactory vf, TypeStore ts) {
        final IConstructor n = vf.constructor(N, vf.integer(1));
        final IConstructor na = n.asAnnotatable().setAnnotation("x", vf.integer(1));

        assertEqualityOfValueWithAndWithoutAnnotations(n, na, vf);
    }

    public void assertEqualityOfValueWithAndWithoutAnnotations(IValue n, IValue na, IValueFactory vf) {

        assertIsEqualButNotEquals(n, na);

        assertIsEqualButNotEquals(vf.set(n), vf.set(na));
        assertIsEqualButNotEquals(vf.set(vf.set(n)), vf.set(vf.set(na)));

        assertIsEqualButNotEquals(vf.list(n), vf.list(na));
        assertIsEqualButNotEquals(vf.list(vf.list(n)), vf.list(vf.list(na)));

        // check: with keys
        {
            final IMap mapN = createMap(vf, n, vf.integer(1));
            final IMap mapNA = createMap(vf, na, vf.integer(1));
            final IMap mapMapN = createMap(vf, mapN, vf.integer(1));
            final IMap mapMapNA = createMap(vf, mapNA, vf.integer(1));

            assertIsEqualButNotEquals(mapN, mapNA);
            assertIsEqualButNotEquals(mapMapN, mapMapNA);
        }

        // check: with values
        {
            final IMap mapN = createMap(vf, vf.integer(1), n);
            final IMap mapNA = createMap(vf, vf.integer(1), na);
            final IMap mapMapN = createMap(vf, vf.integer(1), mapN);
            final IMap mapMapNA = createMap(vf, vf.integer(1), mapNA);

            assertIsEqualButNotEquals(mapN, mapNA);
            assertIsEqualButNotEquals(mapMapN, mapMapNA);
        }

        assertIsEqualButNotEquals(vf.node("nestingInNode", n), vf.node("nestingInNode", na));

        final TypeStore ts = new TypeStore();
        final Type adtType = tf.abstractDataType(ts, "adtTypeNameThatIsIgnored");
        final Type constructorType =
                tf.constructor(ts, adtType, "nestingInConstructor", tf.valueType());

        assertIsEqualButNotEquals(vf.constructor(constructorType, n),
                vf.constructor(constructorType, na));
    }

    /**
     * Create a @IMap from a variable argument list.
     * 
     * @param keyValuePairs a sequence of alternating keys and values
     * @return an new @IMap instance
     */
    public IMap createMap(IValueFactory vf, IValue... keyValuePairs) {
        assert (keyValuePairs.length % 2 == 0);

        IMapWriter w = vf.mapWriter();

        for (int i = 0; i < keyValuePairs.length / 2; i++) {
            w.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }

        return w.done();
    }

    /**
     * Asserting the current implementation w.r.t. hash codes and different equalities. Note, that
     * this does not reflect the envisioned design that we are working towards (= structurally where
     * annotations contribute to equality and hash code).
     * 
     * @param a an object that does not use annotations
     * @param b a structurally equal object to {@literal a} with annotations
     */
    public void assertIsEqualButNotEquals(IValue a, IValue b) {
        assertFalse(a.equals(b));

        assertTrue(a.isEqual(b));
        assertTrue(a.hashCode() == b.hashCode());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testNoKeywordParametersOnAnnotatedNode(IValueFactory vf, TypeStore ts) {
        try {
            vf.node("hallo").asAnnotatable().setAnnotation("a", vf.integer(1)).asWithKeywordParameters()
            .setParameter("b", vf.integer(2));
        } catch (UnsupportedOperationException e) {
            assertTrue(true);
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testAnnotationsOnNodeWithKeywordParameters(IValueFactory vf, TypeStore ts) {
        try {
            vf.node("hallo").asWithKeywordParameters().setParameter("b", vf.integer(2)).asAnnotatable()
            .setAnnotation("a", vf.integer(1));
        } catch (UnsupportedOperationException e) {
            assertTrue(true);
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testNodeAnnotation(IValueFactory vf, TypeStore ts) {
        ts.declareAnnotation(tf.nodeType(), "foo", tf.boolType());
        INode n = vf.node("hello");
        INode na = n.asAnnotatable().setAnnotation("foo", vf.bool(true));

        assertTrue(na.asAnnotatable().getAnnotation("foo").getType().isBool());

        // annotations on node type should be propagated
        assertTrue(ts.getAnnotationType(tf.nodeType(), "foo").isBool());
        assertTrue(ts.getAnnotations(E).containsKey("foo"));

        // annotations sets should not collapse into one big set
        ts.declareAnnotation(E, "a", tf.integerType());
        ts.declareAnnotation(N, "b", tf.boolType());
        assertTrue(!ts.getAnnotations(E).equals(ts.getAnnotations(N)));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testNodeKeywordParameter(IValueFactory vf, TypeStore ts) {
        INode n = vf.node("hello");
        INode na = n.asWithKeywordParameters().setParameter("foo", vf.bool(true));

        assertTrue(na.asWithKeywordParameters().getParameter("foo").getType().isBool());
        assertTrue(na.asWithKeywordParameters().getParameter("foo").equals(vf.bool(true)));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testConstructorKeywordParameter(IValueFactory vf, TypeStore ts) {
        Type adt = tf.abstractDataType(ts, "adt");
        Type cons = tf.constructorFromTuple(ts, adt, "cons", tf.tupleEmpty());

        IConstructor n1 = vf.constructor(cons);

        // overrides work
        IConstructor n2 = n1.asWithKeywordParameters().setParameter("foo", vf.bool(false));
        assertTrue(n2.asWithKeywordParameters().getParameter("foo").isEqual(vf.bool(false)));

        // keywordparameters work on equality:
        assertFalse(n1.isEqual(n2));
    }

}
