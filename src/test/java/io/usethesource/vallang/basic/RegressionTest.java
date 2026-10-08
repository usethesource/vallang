package io.usethesource.vallang.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.GivenValue;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

public class RegressionTest {

    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testParameterizedConstructorUpdatePreservesDeclaration(IValueFactory vf, TypeFactory tf, TypeStore store) {
        Type parameter = tf.parameterType("T");
        Type adt = tf.abstractDataType(store, "P", parameter);
        Type constructor = tf.constructor(store, adt, "p", parameter, "x");
        store.declareKeywordParameter(constructor, "k", tf.integerType());
        IConstructor original = vf.constructor(constructor, vf.integer(1)).asWithKeywordParameters()
            .setParameter("k", vf.integer(2));

        IConstructor updated = original.set("x", vf.integer(3));
        assertEquals(constructor, updated.getUninstantiatedConstructorType());
        assertTrue(store.hasKeywordParameter(updated.getUninstantiatedConstructorType(), "k"));
        assertEquals(vf.integer(2), updated.asWithKeywordParameters().getParameter("k"));
        assertEquals(vf.integer(3), updated.get("x"));
        assertEquals(vf.integer(1), original.get("x"));
    }


    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testConstructorEquality(IValueFactory vf, TypeFactory tf) throws Exception {
        final TypeStore ts = new TypeStore();
        final Type adtType = tf.abstractDataType(ts, "n");
        final Type c0Type = tf.constructor (ts, adtType, "c");
        final Type c1Type = tf.constructor (ts, adtType, "c1", tf.integerType());

        final IConstructor c0Normal = vf.constructor(c0Type);      final IConstructor c0WithKWParams = vf.constructor(c0Type).asWithKeywordParameters().setParameters(Collections.emptyMap());

        final IConstructor c1Normal = vf.constructor(c1Type, vf.integer(1));
        final IConstructor c1WithKWParams = vf.constructor(c1Type, vf.integer(1)).asWithKeywordParameters().setParameters(Collections.emptyMap());

        assertTrue(c0WithKWParams.equals(c0Normal));
        assertTrue(c0Normal.equals(c0WithKWParams));
        assertTrue(c1WithKWParams.equals(c1Normal));
        assertTrue(c1Normal.equals(c1WithKWParams));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testRegression42_cause(IValueFactory vf, TypeStore store, @GivenValue("(\"\"():4,\"\"():3)") IValue v,  @GivenValue("(\"\"():4,\"\"():3)") IValue u) throws IOException {
        assertTrue(v.equals(u));
    }
}
