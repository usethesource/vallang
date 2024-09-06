/*******************************************************************************
 * Copyright (c) 2012 Centrum Wiskunde en Informatica (CWI)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Anya Helene Bagge (University of Bergen) - implementation
 *    Arnold Lankamp - base implementation (from BinaryIoSmokeTest.java)
 *******************************************************************************/
package io.usethesource.vallang.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.IMap;
import io.usethesource.vallang.IMapWriter;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

/**
 * @author Anya Helene Bagge
 */
public final class MapSmokeTest {

    public TypeStore store = new TypeStore();

    enum Kind {
        BINARY
    };

    private static final TypeFactory tf = TypeFactory.getInstance();
    private Type a = tf.abstractDataType(store, "A");
    private Type b = tf.abstractDataType(store, "B");

    private IMap[] testMaps(IValueFactory vf) {
        IMap empty = vf.mapWriter().done();

        IMap[] testMaps = new IMap[] {empty,
                empty.put(vf.string("Bergen"), vf.string("Amsterdam")),
                empty.put(vf.string("Bergen"), vf.string("Amsterdam"))
                .put(vf.string("Mango"), vf.string("Yummy")),
                empty.put(vf.string("Bergen"), vf.string("Amsterdam"))
                .put(vf.string("Amsterdam"), vf.string("Frankfurt")),
                empty.put(vf.string("Bergen"), vf.string("Amsterdam"))
                .put(vf.string("Amsterdam"), vf.string("Frankfurt"))
                .put(vf.string("Frankfurt"), vf.string("Moscow")),
                empty.put(vf.string("Bergen"), vf.string("Rainy"))
                .put(vf.string("Helsinki"), vf.string("Cold")),
                empty.put(vf.string("Mango"), vf.string("Sweet"))
                .put(vf.string("Banana"), vf.string("Yummy"))};

        return testMaps;
    }

    private StringPair[] keyValues(IValueFactory vf) {

        String[] strings = new String[] {"Bergen", "Amsterdam", "Frankfurt", "Helsinki", "Moscow",
                "Rainy", "Cold", "Mango", "Banana", "Sweet", "Yummy"};
        List<String> list1 = Arrays.asList(strings);
        List<String> list2 = Arrays.asList(strings);
        Collections.shuffle(list1);
        Collections.shuffle(list2);
        StringPair[] keyValues = new StringPair[strings.length];
        for (int i = 0; i < strings.length; i++) {
            keyValues[i] = new StringPair(vf.string(list1.get(i)), vf.string(list2.get(i)));
        }

        return keyValues;
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testLabels(IValueFactory vf) {
        // make a labeled map type, and the labels should match
        Type type = tf.mapType(a, "apple", b, "banana");

        assertEquals("apple", type.getKeyLabel());
        assertEquals("banana", type.getValueLabel());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testTwoLabels1(IValueFactory vf) {
        // make two map types with same key/value types but different labels,
        // and the labels should be kept distinct
        Type type1 = tf.mapType(a, "apple", b, "banana");
        Type type2 = tf.mapType(a, "orange", b, "mango");

        assertEquals("apple", type1.getKeyLabel());
        assertEquals("banana", type1.getValueLabel());
        assertEquals("orange", type2.getKeyLabel());
        assertEquals("mango", type2.getValueLabel());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testTwoLabels2(IValueFactory vf) {
        Type type1 = tf.mapType(a, "apple", b, "banana");
        Type type2 = tf.mapType(a, "orange", b, "mango");

        assertTrue(type1.equivalent(type2), "Two map types with different labels should be equivalent");
        assertTrue(type2.equivalent(type1), "Two map types with different labels should be equivalent");
        Assertions.assertFalse(type1.equals(type2), "Two map types with different labels should not be equals");
        Assertions.assertFalse(type2.equals(type1), "Two map types with different labels should not be equals");

        Type type3 = tf.mapType(a, b);
        Assertions.assertTrue(type1.equivalent(type3), "Labeled and unlabeled maps should be equivalent");
        Assertions.assertTrue(type3.equivalent(type1), "Labeled and unlabeled maps should be equivalent");
        Assertions.assertTrue(type2.equivalent(type3), "Labeled and unlabeled maps should be equivalent");
        Assertions.assertTrue(type3.equivalent(type2), "Labeled and unlabeled maps should be equivalent");
        Assertions.assertFalse(type1.equals(type3), "Labeled and unlabeled maps should not be equals");
        Assertions.assertFalse(type3.equals(type1), "Labeled and unlabeled maps should not be equals");
        Assertions.assertFalse(type2.equals(type3), "Labeled and unlabeled maps should not be equals");
        Assertions.assertFalse(type3.equals(type2), "Labeled and unlabeled maps should not be equals");
    }

    /**
     * Check basic properties of put()
     */
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testPut(IValueFactory vf) {
        for (IMap map : testMaps(vf)) {
            for (StringPair p : keyValues(vf)) {
                IMap newMap = map.put(p.a, p.b);
                assertTrue(newMap.containsKey(p.a));
                assertEquals(p.b, newMap.get(p.a));
                assertTrue(map.getType().isSubtypeOf(newMap.getType()));
            }
        }
    }

    /**
     * Check that putting doesn't modify original map, and doesn't modify other elements.
     */
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testPutModification(IValueFactory vf) {
        for (IMap map : testMaps(vf)) {
            for (StringPair p : keyValues(vf)) { // testing with an arbitrary element of map is sufficient
                if (map.containsKey(p.a)) {
                    IValue val = map.get(p.a);
                    for (StringPair q : keyValues(vf)) {
                        IMap newMap = map.put(q.a, q.b);
                        assertEquals(val, map.get(p.a)); // original is never modified
                        if (!p.a.equals(q.a)) {
                            assertEquals(val, newMap.get(p.a)); // only element q.a is modified
                        }
                    }
                }
            }
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testCommon(IValueFactory vf) {
        for (IMap map1 : testMaps(vf)) {
            for (IMap map2 : testMaps(vf)) {
                IMap map3 = map1.common(map2);
                // all common values are present
                for (IValue key : map1) {
                    if (map1.get(key).equals(map2.get(key))) {
                        assertEquals(map1.get(key), map3.get(key));
                    }
                }
                // type is lub of map1 and map2 types
                if (!map3.isEmpty()) {
                    Assertions.assertTrue(map1.getType().isSubtypeOf(map3.getType()), map1.getType().toString() + " <: " + map3.getType());
                    Assertions.assertTrue(map2.getType().isSubtypeOf(map3.getType()), map2.getType().toString() + " <: " + map3.getType());
                }

                // dynamic map values should never have labels
                assertFalse(map1.getType().hasFieldNames());
                assertFalse(map2.getType().hasFieldNames());
                assertFalse(map3.getType().hasFieldNames());
            }
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testJoin(IValueFactory vf) {
        for (IMap map1 : testMaps(vf)) {
            for (IMap map2 : testMaps(vf)) {
                IMap map3 = map1.join(map2);
                // should contain all values from map2...
                for (IValue key : map2) {
                    assertEquals(map2.get(key), map3.get(key));
                }
                // ...and all values from map1 unless the keys are in map2
                for (IValue key : map1) {
                    if (!map2.containsKey(key)) {
                        assertEquals(map1.get(key), map3.get(key));
                    }
                }

                // type is lub of map1 and map2 types
                if (!map3.isEmpty()) {
                    Assertions.assertTrue(map1.getType().isSubtypeOf(map3.getType()), map1.getType().toString() + " <: " + map3.getType());
                    Assertions.assertTrue(map2.getType().isSubtypeOf(map3.getType()), map2.getType().toString() + " <: " + map3.getType());
                }
            }
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testCompose(IValueFactory vf) {
        for (IMap map1 : testMaps(vf)) {
            for (IMap map2 : testMaps(vf)) {
                IMap map3 = map1.compose(map2);
                // should map keys in map1 to values in map2
                for (IValue key : map1) {
                    if (map2.containsKey(map1.get(key)))
                        assertEquals(map2.get(map1.get(key)), map3.get(key));
                    else
                        assertNull(map3.get(key));
                }

                // type is key type of map1 and value type of map2
                if (!map3.isEmpty()) {
                    assertEquals(map1.getType().getKeyType(), map3.getType().getKeyType());
                    assertEquals(map2.getType().getValueType(), map3.getType().getValueType());
                }

                // check labels
                if (map1.getType().hasFieldNames() && map2.getType().hasFieldNames()) {
                    assertEquals(map1.getType().getKeyLabel(), map3.getType().getKeyLabel());
                    assertEquals(map2.getType().getValueLabel(), map3.getType().getValueLabel());
                } else {
                    assertFalse(map3.getType().hasFieldNames());
                }
            }

        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testRemove(IValueFactory vf) {
        for (IMap map1 : testMaps(vf)) {
            for (IMap map2 : testMaps(vf)) {
                IMap map3 = map1.remove(map2);
                for (IValue key : map2) {
                    Assertions.assertFalse(map3.containsKey(key), "Key " + key + " should not exist");
                }

                // type is same as map1
                if (!map3.isEmpty()) {
                    assertEquals(map1.getType(), map3.getType());
                }

                // labels are same as map1
                if (map1.getType().hasFieldNames()) {
                    assertEquals(map1.getType().getKeyLabel(), map3.getType().getKeyLabel());
                    assertEquals(map1.getType().getValueLabel(), map3.getType().getValueLabel());
                }
            }

        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testRemoveKey(IValueFactory vf) {
        for (IMap map1 : testMaps(vf)) {
            for (IMap map2 : testMaps(vf)) {
                for (IValue key: map2) {
                    IMap map3 = map1.removeKey(key);
                    Assertions.assertFalse(map3.containsKey(key), "Key " + key + " should not exist anymore");
                    if (map1.getType().hasFieldNames()) {
                        assertEquals(map1.getType().getKeyLabel(), map3.getType().getKeyLabel());
                        assertEquals(map1.getType().getValueLabel(), map3.getType().getValueLabel());
                    }
                }
            }

        }
    }


    static class TestValue {
        Type type;
        IValue value;
        String keyLabel;
        String valueLabel;

        TestValue(IValueFactory vf, MapSmokeTest baseTestMap, String key, String value, String keyLabel,
                String valueLabel) {
            TypeFactory tf = TypeFactory.getInstance();
            this.keyLabel = keyLabel;
            this.valueLabel = valueLabel;

            if (keyLabel != null && valueLabel != null) {
                type = tf.mapType(tf.stringType(), keyLabel, tf.stringType(), valueLabel);
            }
            else {
                type = tf.mapType(tf.stringType(), tf.stringType());
            }

            this.value = vf.mapWriter().done().put(vf.string(key), vf.string(value));
        }

        public String toString() {
            return value.toString();
        }
    }

    static class StringPair {
        IString a;
        IString b;

        StringPair(IString a, IString b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString() {
            return String.format("(%s,%s)", a, b);
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testPutReplaceGet(IValueFactory vf) {
        final IMap m1 =
                vf.mapWriter().done().put(vf.integer(1), vf.integer(1)).put(vf.integer(1), vf.integer(2));

        assertEquals(1, m1.size());
        assertEquals(vf.integer(2), m1.get(vf.integer(1)));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testDynamicTypesAfterMapUpdatesGrow(IValueFactory vf) {
        final IMap m1 =
                vf.mapWriter().done().put(vf.integer(1), vf.integer(1)).put(vf.integer(1), vf.real(1));

        assertEquals(1, m1.size());
        assertEquals(tf.integerType(), m1.getType().getKeyType());
        assertEquals(tf.realType(), m1.getType().getValueType());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testDynamicTypesAfterMapWriterUpdatesGrow(IValueFactory vf) {
        final IMapWriter w1 = vf.mapWriter();
        w1.put(vf.integer(1), vf.integer(1));
        w1.put(vf.integer(1), vf.real(1));

        final IMap m1 = w1.done();

        assertEquals(1, m1.size());
        assertEquals(tf.integerType(), m1.getType().getKeyType());
        assertEquals(tf.realType(), m1.getType().getValueType());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testDynamicTypesAfterMapUpdatesShrink(IValueFactory vf) {
        final IMap m1 = vf.mapWriter().done().put(vf.integer(1), vf.integer(1))
                .put(vf.integer(1), vf.real(1)).put(vf.integer(1), vf.integer(1));

        assertEquals(1, m1.size());
        assertEquals(tf.integerType(), m1.getType().getKeyType());
        assertEquals(tf.integerType(), m1.getType().getValueType());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testDynamicTypesAfterMapWriterUpdatesShrink(IValueFactory vf) {
        final IMapWriter w1 = vf.mapWriter();
        w1.put(vf.integer(1), vf.integer(1));
        w1.put(vf.integer(1), vf.real(1));
        w1.put(vf.integer(1), vf.integer(1));

        final IMap m1 = w1.done();

        assertEquals(1, m1.size());
        assertEquals(tf.integerType(), m1.getType().getKeyType());
        assertEquals(tf.integerType(), m1.getType().getValueType());
    }
}
