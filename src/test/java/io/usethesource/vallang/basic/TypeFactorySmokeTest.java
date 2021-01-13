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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.exceptions.FactTypeDeclarationException;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

public final class TypeFactorySmokeTest {

    private static TypeFactory ft = TypeFactory.getInstance();

    private static Type[] types = new Type[] { ft.integerType(), ft.realType(), ft.sourceLocationType(), ft.valueType(),
            ft.listType(ft.integerType()), ft.setType(ft.realType()) };

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testGetInstance() {
        if (TypeFactory.getInstance() != ft) {
            fail("getInstance did not return the same reference");
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testGetTypeByDescriptor() {
        // TODO: needs to be tested, after we've implemented it
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testValueType() {
        if (ft.valueType() != ft.valueType()) {
            fail("valueType should be canonical");
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testIntegerType() {
        if (ft.integerType() != ft.integerType()) {
            fail("integerType should be canonical");
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testDoubleType() {
        if (ft.realType() != ft.realType()) {
            fail("doubleType should be canonical");
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testStringType() {
        if (ft.stringType() != ft.stringType()) {
            fail("stringType should be canonical");
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testSourceLocationType() {
        if (ft.sourceLocationType() != ft.sourceLocationType()) {
            fail("sourceLocationType should be canonical");
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testTupleTypeOfType() {
        Type t = ft.tupleType(types[0]);

        if (t != ft.tupleType(types[0])) {
            fail("tuple types should be canonical");
        }

        checkTupleTypeOf(t, 1);
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testTupleTypeOfTypeType() {
        Type t = ft.tupleType(types[0], types[1]);

        if (t != ft.tupleType(types[0], types[1])) {
            fail("tuple types should be canonical");
        }

        checkTupleTypeOf(t, 2);
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testTupleTypeOfTypeTypeType() {
        Type t = ft.tupleType(types[0], types[1], types[2]);

        if (t != ft.tupleType(types[0], types[1], types[2])) {
            fail("tuple types should be canonical");
        }

        checkTupleTypeOf(t, 3);
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testTupleTypeOfTypeTypeTypeType() {
        Type t = ft.tupleType(types[0], types[1], types[2], types[3]);

        if (t != ft.tupleType(types[0], types[1], types[2], types[3])) {
            fail("tuple types should be canonical");
        }

        checkTupleTypeOf(t, 4);
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testTupleTypeOfTypeTypeTypeTypeType() {
        Type t = ft.tupleType(types[0], types[1], types[2], types[3], types[4]);

        if (t != ft.tupleType(types[0], types[1], types[2], types[3], types[4])) {
            fail("tuple types should be canonical");
        }

        checkTupleTypeOf(t, 5);
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testTupleTypeOfTypeTypeTypeTypeTypeType() {
        Type t = ft.tupleType(types[0], types[1], types[2], types[3], types[4], types[5]);

        if (t != ft.tupleType(types[0], types[1], types[2], types[3], types[4], types[5])) {
            fail("tuple types should be canonical");
        }

        checkTupleTypeOf(t, 6);
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testTupleTypeOfTypeTypeTypeTypeTypeTypeType() {
        Type t = ft.tupleType(types[0], types[1], types[2], types[3], types[4], types[5]);

        if (t != ft.tupleType(types[0], types[1], types[2], types[3], types[4], types[5])) {
            fail("tuple types should be canonical");
        }

        checkTupleTypeOf(t, 6);
    }

    private void checkTupleTypeOf(Type t, int width) {

        if (t.getArity() != width) {
            fail("tuple arity broken");
        }

        for (int i = 0; i < t.getArity(); i++) {
            if (t.getFieldType(i) != types[i % types.length]) {
                fail("Tuple field type unexpected");
            }
        }
    }

    private void checkRelationTypeOf(Type t, int width) {

        if (t.getArity() != width) {
            fail("relation arity broken");
        }

        for (int i = 0; i < t.getArity(); i++) {
            if (t.getFieldType(i) != types[i % types.length]) {
                fail("Relation field type unexpected");
            }
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testTupleTypeOfIValueArray(IValueFactory vf) {
        // a and b shadow the 'types' field
        try {
            @SuppressWarnings("deprecation")
            IValue[] a = new IValue[] { vf.integer(1), vf.real(1.0),
                    vf.sourceLocation(new URI("file://bla"), 0, 0, 0, 0, 0, 0) };
            @SuppressWarnings("deprecation")
            IValue[] b = new IValue[] { vf.integer(1), vf.real(1.0),
                    vf.sourceLocation(new URI("file://bla"), 0, 0, 0, 0, 0, 0) };
            Type t = ft.tupleType(a);

            if (t != ft.tupleType(b)) {
                fail("tuples should be canonical");
            }

            checkTupleTypeOf(t, 3);
        } catch (URISyntaxException e) {
            fail(e.toString());
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testSetTypeOf() {
        Type type = ft.setType(ft.integerType());

        if (type != ft.setType(ft.integerType())) {
            fail("set should be canonical");
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testRelTypeType() {
        try {
            TypeStore store = new TypeStore();
            Type namedType = ft.aliasType(store, "myTuple", ft.tupleType(ft.integerType(), ft.integerType()));
            // note that the declared type of namedType needs to be Type
            Type type = ft.relTypeFromTuple(namedType);

            Type namedType2 = ft.aliasType(store, "myTuple", ft.tupleType(ft.integerType(), ft.integerType()));

            if (type != ft.relTypeFromTuple(namedType2)) {
                fail("relation types should be canonical");
            }

            if (type.getFieldType(0) != ft.integerType() && type.getFieldType(1) != ft.integerType()) {
                fail("relation should mimick tuple field types");
            }
        } catch (FactTypeUseException e) {
            fail("type error for correct relation");
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testListRelTypeType() {
        try {
            TypeStore store = new TypeStore();
            Type namedType = ft.aliasType(store, "myTuple", ft.tupleType(ft.integerType(), ft.integerType()));
            // note that the declared type of namedType needs to be Type
            Type type = ft.lrelTypeFromTuple(namedType);

            Type namedType2 = ft.aliasType(store, "myTuple", ft.tupleType(ft.integerType(), ft.integerType()));

            if (type != ft.lrelTypeFromTuple(namedType2)) {
                fail("list relation types should be canonical");
            }

            if (type.getFieldType(0) != ft.integerType() && type.getFieldType(1) != ft.integerType()) {
                fail("list relation should mimick tuple field types");
            }
        } catch (FactTypeUseException e) {
            fail("type error for correct list relation");
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testRelTypeNamedType() {
        try {
            TypeStore store = new TypeStore();
            Type namedType = ft.aliasType(store, "myTuple", ft.tupleType(ft.integerType(), ft.integerType()));
            // note that the declared type of namedType needs to be AliasType
            Type type = ft.relTypeFromTuple(namedType);

            Type namedType2 = ft.aliasType(store, "myTuple", ft.tupleType(ft.integerType(), ft.integerType()));

            if (type != ft.relTypeFromTuple(namedType2)) {
                fail("relation types should be canonical");
            }
        } catch (FactTypeUseException e) {
            fail("type error for correct relation");
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testListRelTypeNamedType() {
        try {
            TypeStore store = new TypeStore();
            Type namedType = ft.aliasType(store, "myTuple", ft.tupleType(ft.integerType(), ft.integerType()));
            // note that the declared type of namedType needs to be AliasType
            Type type = ft.lrelTypeFromTuple(namedType);

            Type namedType2 = ft.aliasType(store, "myTuple", ft.tupleType(ft.integerType(), ft.integerType()));

            if (type != ft.lrelTypeFromTuple(namedType2)) {
                fail("list relation types should be canonical");
            }
        } catch (FactTypeUseException e) {
            fail("type error for correct list relation");
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testRelTypeTupleType() {
        Type tupleType = ft.tupleType(ft.integerType(), ft.integerType());
        // note that the declared type of tupleType needs to be TupleType
        Type type = ft.relTypeFromTuple(tupleType);

        Type tupleType2 = ft.tupleType(ft.integerType(), ft.integerType());

        if (type != ft.relTypeFromTuple(tupleType2)) {
            fail("relation types should be canonical");
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testListRelTypeTupleType() {
        Type tupleType = ft.tupleType(ft.integerType(), ft.integerType());
        // note that the declared type of tupleType needs to be TupleType
        Type type = ft.lrelTypeFromTuple(tupleType);

        Type tupleType2 = ft.tupleType(ft.integerType(), ft.integerType());

        if (type != ft.lrelTypeFromTuple(tupleType2)) {
            fail("list relation types should be canonical");
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testRelTypeOfType() {
        Type type = ft.relType(types[0]);

        if (type != ft.relType(types[0])) {
            fail("relation types should be canonical");
        }

        checkRelationTypeOf(type, 1);
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testRelTypeOfTypeType() {
        Type type = ft.relType(types[0], types[1]);

        if (type != ft.relType(types[0], types[1])) {
            fail("relation types should be canonical");
        }

        checkRelationTypeOf(type, 2);
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testRelTypeOfTypeTypeType() {
        Type type = ft.relType(types[0], types[1], types[2]);

        if (type != ft.relType(types[0], types[1], types[2])) {
            fail("relation types should be canonical");
        }

        checkRelationTypeOf(type, 3);
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testRelTypeOfTypeTypeTypeType() {
        Type type = ft.relType(types[0], types[1], types[2], types[3]);

        if (type != ft.relType(types[0], types[1], types[2], types[3])) {
            fail("relation types should be canonical");
        }
        checkRelationTypeOf(type, 4);
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testRelTypeOfTypeTypeTypeTypeType() {
        Type type = ft.relType(types[0], types[1], types[2], types[3], types[4]);

        if (type != ft.relType(types[0], types[1], types[2], types[3], types[4])) {
            fail("relation types should be canonical");
        }
        checkRelationTypeOf(type, 5);
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testRelTypeOfTypeTypeTypeTypeTypeType() {
        Type type = ft.relType(types[0], types[1], types[2], types[3], types[4], types[5]);

        if (type != ft.relType(types[0], types[1], types[2], types[3], types[4], types[5])) {
            fail("relation types should be canonical");
        }
        checkRelationTypeOf(type, 6);
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testRelTypeOfTypeTypeTypeTypeTypeTypeType() {
        Type type = ft.relType(types[0], types[1], types[2], types[3], types[4], types[5]);

        if (type != ft.relType(types[0], types[1], types[2], types[3], types[4], types[5])) {
            fail("relation types should be canonical");
        }
        checkRelationTypeOf(type, 6);
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testNamedType() {
        try {
            TypeStore ts = new TypeStore();
            Type t1 = ft.aliasType(ts, "myType", ft.integerType());
            Type t2 = ft.aliasType(ts, "myType", ft.integerType());

            if (t1 != t2) {
                fail("named types should be canonical");
            }

            try {
                ft.aliasType(ts, "myType", ft.realType());
                fail("Should not be allowed to redeclare a type name");
            } catch (FactTypeDeclarationException e) {
                // this should happen
            }
        } catch (FactTypeDeclarationException e) {
            fail("the above should be type correct");
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testListType() {
        Type t1 = ft.listType(ft.integerType());
        Type t2 = ft.listType(ft.integerType());

        if (t1 != t2) {
            fail("named types should be canonical");
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testFunctionLub(TypeFactory tf) {
        Type t1 = tf.functionType(tf.integerType(), tf.tupleType(tf.integerType(), tf.integerType()), tf.tupleEmpty());
        Type t2 = tf.functionType(tf.integerType(), tf.tupleType(tf.rationalType(), tf.rationalType()),
                tf.tupleEmpty());

        // if the arity is the same, the lub is still a function type (for computing the
        // types of overloaded functions)
        assertTrue(!t1.lub(t2).isTop());
        assertTrue(t1.getArity() == t1.lub(t2).getArity());

        // but if its not the same, then we default to value, because we don't know how
        // to call a function with different amounts of parameters
        Type t3 = tf.functionType(tf.integerType(), tf.tupleType(tf.stringType()), tf.tupleEmpty());
        assertTrue(t1.lub(t3).isTop());
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testHigherOrderSelfMatchOfFunctionType(TypeFactory tf) {
        Type returnType = tf.parameterType("Ret");
        Type arg1 = tf.parameterType("Arg1");
        Type arg2 = tf.parameterType("Arg2");
        // &Ret curried(&Arg2 arg2)
        Type curriedFunction = tf.functionType(returnType, tf.tupleType(arg2), tf.tupleEmpty());
        // &Ret func (&Arg1 arg1, &Arg2 arg2)
        Type parameterFunction = tf.functionType(returnType, tf.tupleType(arg1, arg2), tf.tupleEmpty());
        // &Ret (&Arg2) curry (&Return (&Arg1, &Arg2) func, &Arg1 arg1)
        Type curry = tf.functionType(curriedFunction, tf.tupleType(parameterFunction, arg1), tf.tupleEmpty());

        // First we rename the type parameters for the purpose of parameter hygiene:
        Map<Type, Type> renamings = new HashMap<>();
        curry.match(tf.voidType(), renamings);

        for (Type key : renamings.keySet()) {
            renamings.put(key, tf.parameterType(key.getName() + "'"));
        }

        Type renamedCurry = curry.instantiate(renamings);

        // now we self apply the curry function with an add function
        Type addFunction = tf.functionType(tf.integerType(), tf.tupleType(tf.integerType(), tf.integerType()),
                tf.tupleEmpty());
        Type curriedAdd = tf.functionType(tf.integerType(), tf.tupleType(tf.integerType()), tf.tupleEmpty());
        Type curriedCurryReturn = tf.functionType(curriedAdd, tf.tupleType(tf.integerType()), tf.tupleEmpty());

        // the goal is to arrive at a binding of &Ret to an instantiated int (int)
        // curried function, via parameter matching
        Type actualParameterTypes = tf.tupleType(renamedCurry, addFunction);
        Type formalParameterTypes = curry.getFieldTypes();

        Map<Type, Type> bindings = new HashMap<>();

        // this is where the bug was/is.
        // applying curry(curry, add) should give `int(int arg2) (int arg1)` as a return
        // type.
        formalParameterTypes.match(actualParameterTypes, bindings);

        // instead we get the uninstantiated version &Ret' (&Arg2') (&Arg1')
        assertTrue(curry.getReturnType().instantiate(bindings) == curriedCurryReturn);
    }

    @ParameterizedTest
    @ArgumentsSource(ValueProvider.class)
    public void testUnificationOfNestedNonLinearTypeParameters(TypeFactory tf, TypeStore store) throws IOException {
        Type formals = tf.fromString(store, new StringReader("tuple[list[tuple[&T0,&T1]],set[&T0]]"));
        Type actuals = tf.fromString(store, new StringReader("tuple[lrel[int,int],set[void]]"));
        Type T0 = tf.parameterType("T0");
        Type T0expected = tf.integerType().lub(tf.voidType());

        Map<Type,Type> bindings = new HashMap<>();
        formals.match(actuals, bindings);

        assertTrue(T0expected == bindings.get(T0));
    }
}
