package io.usethesource.vallang.specification;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.IValue;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

public class TypeTest {

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void isomorphicStringTest(TypeFactory tf, TypeStore store, Type t) throws IOException {
        assertTrue(tf.fromString(store, new StringReader(t.toString())) == t);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void canonicalTypes(TypeFactory tf, Type t, Type u) {
        if (t.equals(u)) {
            assertTrue(t == u);
        }
        
        if (t == u) {
            assertTrue(t.equals(u));
            assertTrue(u.equals(t));
        }
    }
    
    @SuppressWarnings("deprecation")
    public void emptyTupleNeverHasLabels(TypeFactory tf) {
        assertFalse(tf.tupleType(new Object[0]).hasFieldNames());
        assertFalse(tf.tupleType(new Type[0], new String[0]).hasFieldNames());
        assertTrue(tf.tupleEmpty() == tf.tupleType(new IValue[0]));
        assertTrue(tf.tupleEmpty() == tf.tupleType(new Object[0]));
        assertTrue(tf.tupleEmpty() == tf.tupleType(new Type[0], new String[0]));
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void covariance(TypeFactory tf, Type t, Type u) {
        if (!t.comparable(u)) {
            return;
        }
        
        if (t.isSubtypeOf(u)) {
            assertTrue(tf.listType(t).isSubtypeOf(tf.listType(u)));
            assertTrue(tf.setType(t).isSubtypeOf(tf.setType(u)));
            assertTrue(tf.tupleType(t).isSubtypeOf(tf.tupleType(u)));
            assertTrue(tf.mapType(t,tf.integerType()).isSubtypeOf(tf.mapType(u, tf.integerType())));
            assertTrue(tf.mapType(tf.integerType(), t).isSubtypeOf(tf.mapType(tf.integerType(), u)));
        }
        else if (u.isSubtypeOf(t)) {
            covariance(tf, u, t);
        }
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void voidIsBottom(TypeFactory tf, Type t) {
        assertTrue(tf.voidType().isSubtypeOf(t));
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void valueIsTop(TypeFactory tf, Type t) {
        assertTrue(t.isSubtypeOf(tf.valueType()));
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void subtypeIsReflexive(Type t) {
        assertTrue(t.isSubtypeOf(t));
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void subtypeIsTransitive(Type t, Type u, Type v) {
        if (t.isSubtypeOf(u) && u.isSubtypeOf(v)) {
            assertTrue(t.isSubtypeOf(v));
        }
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void subtypeIsAntiCommutative(Type t, Type u) {
        if (t.isStrictSubtypeOf(u)) {
            assertTrue(!u.isStrictSubtypeOf(t));
        }
    }
}
