package io.usethesource.vallang.specification;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.IValue;
import io.usethesource.vallang.TypeConfig;
import io.usethesource.vallang.TypeConfig.Option;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.exceptions.FactTypeDeclarationException;
import io.usethesource.vallang.exceptions.TypeParseError;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

public class TypeTest {

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void randomTypeGeneratorTestIfTypeStoreContainsGeneratedTypes(TypeStore store, Type t) {
        assertTrue(t.isAbstractData() ? store.lookupAbstractDataType(t.getName()) != null : true);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void isomorphicStringTest(TypeFactory tf, TypeStore store, Type t) throws IOException {
        // no support for parameter types, aliases and tuple field names yet
        assertSame(tf.fromString(store, new StringReader(t.toString())), t);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class) @TypeConfig(Option.ALL)
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

    @ParameterizedTest @ArgumentsSource(ValueProvider.class) @TypeConfig(Option.ALL)
    public void compareTo(Type t, Type u) {
        if (t.isSubtypeOf(u)) {
            if (!t.equivalent(u)) {
                assertTrue(t.compareTo(u) == -1);
            }
            else {
                assertTrue(t.compareTo(u) == t.toString().compareTo(u.toString()));
            }
        }
        else if (u.isSubtypeOf(t)) {
            if (!t.equivalent(u)) {
                assertTrue(t.compareTo(u) == 1);
            }
            else {
                assertTrue(t.compareTo(u) == t.toString().compareTo(u.toString()));
            }
        }
        else {
            assertTrue(t.compareTo(u) == t.toString().compareTo(u.toString()));
            assertTrue(u.compareTo(t) == u.toString().compareTo(t.toString()));
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class) @TypeConfig(Option.ALL)
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

    @ParameterizedTest @ArgumentsSource(ValueProvider.class) @TypeConfig(Option.ALL)
    public void subtypeIsReflexive(Type t) {
        assertTrue(t.isSubtypeOf(t));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class) @TypeConfig(Option.ALL)
    public void equalsReflective(Type t) {
        assertTrue(t.equals(t));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    @TypeConfig({Option.ALIASES, Option.TUPLE_FIELDNAMES})
    public void subtypeIsTransitive(Type t, Type u, Type v) {
        // because type parameters are variant in both directions they are excluded
        if (t.isSubtypeOf(u) && u.isSubtypeOf(v)) {
            assertTrue(t.isSubtypeOf(v));
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    @TypeConfig(Option.ALL)
    public void subtypeIsAntiCommutative(Type t, Type u) {
        if (t.isStrictSubtypeOf(u)) {
            assertTrue(!u.isStrictSubtypeOf(t));
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void aliasInstantiationSharingIsGuaranteed(TypeFactory tf, TypeStore store, Type t) {
        Type tp = tf.parameterType("T");
        Map<Type, Type> bindings = Collections.singletonMap(tp, t);

        Type alias = tf.aliasType(store, "A", tf.listType(tp), tp);
        assertTrue(alias.instantiate(bindings) == alias.instantiate(bindings));

        alias = tf.aliasType(store, "B", tf.setType(tp), tp);
        assertTrue(alias.instantiate(bindings) == alias.instantiate(bindings));

        alias = tf.aliasType(store, "C", tf.mapType(tp, tp), tp);
        assertTrue(alias.instantiate(bindings) == alias.instantiate(bindings));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void aliasInstantiationKeepsLabels(TypeFactory tf, TypeStore store, Type t, Type u) {
        Type T = tf.parameterType("T");
        Type U = tf.parameterType("U");
        Map<Type, Type> bindings = Stream.of(new AbstractMap.SimpleEntry<>(T,t), new AbstractMap.SimpleEntry<>(U,u))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Type alias = tf.aliasType(store, "A", tf.mapType(T, "x", U, "y"), T, U);
        assertTrue(alias.instantiate(bindings) == alias.instantiate(bindings));
        assertTrue(alias.getAliased().instantiate(bindings).hasFieldNames());

        @SuppressWarnings("deprecation")
        Type alias2 = tf.aliasType(store, "B", tf.relType(T, "x", U, "y"), T, U);
        assertTrue(alias2.instantiate(bindings) == alias2.instantiate(bindings));

        for (Type b : bindings.values()) {
            if (b.isBottom()) {
                return; // then the tuple type will reduce to void and we have nothing to check.
            }
        }
        assertTrue(alias2.getAliased().instantiate(bindings).hasFieldNames());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testRelations(Type t) {
        if (t.isSet() && t.getElementType().isTuple() && !t.isRelation()) {
            fail("Sets of tuples should be relations");
        }
        if (t.isRelation() && !t.getElementType().isTuple() && !t.getElementType().isBottom()) {
            fail("Relations should contain tuples");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testParameterizedAlias(TypeFactory ft, TypeStore ts) {
        Type T = ft.parameterType("T");
        // DiGraph[&T] = rel[&T from ,&T to]
        @SuppressWarnings("deprecation")
        Type relType = ft.relType(T, "from", T, "to");
        Type DiGraph = ft.aliasType(ts, "DiGraph", relType, T);
        Type IntInstance = ft.relType(ft.integerType(), ft.integerType());
        Type ValueInstance = ft.relType(ft.valueType(), ft.valueType());

       // after instantiation rel[int,int] is a sub-type of rel[&T, &T], and vice versa, and also reflectively
        assertTrue(IntInstance.isSubtypeOf(DiGraph));
        assertTrue(DiGraph.isSubtypeOf(IntInstance));
        assertTrue(DiGraph.isSubtypeOf(DiGraph));

        // vice versa: the aliased type can be matched using the alias type
        Map<Type, Type> bindings = new HashMap<>();
        assertTrue(DiGraph.match(IntInstance, bindings));
        assertTrue(bindings.get(T) == ft.integerType());

        // and the alias type can be matched by the aliased type (not we reuse the bindings from the previous assert)
        Type instanceDiGraph = DiGraph.instantiate(bindings);
        bindings = new HashMap<>(); // reset the bindings
        assertTrue(relType.match(instanceDiGraph, bindings));
        assertTrue(bindings.get(T) == ft.integerType());

        // before instantiation, the parameterized type rel[&T, &T]
        // could be instantiated later by rel[int, int]
        assertTrue(DiGraph.isSubtypeOf(IntInstance));

        // the generic graph is also always a sub-type of the most general instantiation
        assertTrue(DiGraph.isSubtypeOf(ValueInstance));

        bindings = new HashMap<>();
        DiGraph.match(IntInstance, bindings);
        assertTrue(bindings.get(T) == ft.integerType());

        // after instantiation, the parameterized type is an alias for rel[int,
        // int]
        Type ComputedInstance = DiGraph.instantiate(bindings); // DiGraph[int]
        assertTrue(ComputedInstance.equivalent(IntInstance));
        assertFalse(ValueInstance.isSubtypeOf(ComputedInstance));

        // and sub-typing remains co-variant:
        assertTrue(IntInstance.isSubtypeOf(ValueInstance));
        assertTrue(ComputedInstance.isSubtypeOf(ValueInstance));

        try {
            ft.aliasType(ts, "DiGraph", ft.setType(T), T);
            fail("should not be able to redefine alias");
        } catch (FactTypeDeclarationException e) {
            // this should happen
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testADT(TypeFactory ft, TypeStore store) {
        Type E = ft.abstractDataType(store, "E");

        Assertions.assertTrue(E.isSubtypeOf(ft.nodeType()), "Abstract data-types are composed of constructors which are tree nodes");

        assertTrue(E.isSubtypeOf(ft.valueType()));
        assertTrue(E.isSubtypeOf(ft.nodeType()));
        assertTrue(E.lub(ft.nodeType()).isNode());
        assertTrue(ft.nodeType().lub(E).isNode());

        Type f = ft.constructor(store, E, "f", ft.integerType(), "i");
        Type g = ft.constructor(store, E, "g", ft.integerType(), "j");

        assertTrue(f.isSubtypeOf(ft.nodeType()));

        assertTrue(f.lub(ft.nodeType()).isNode());
        assertTrue(ft.nodeType().lub(f).isNode());

        Type a = ft.aliasType(store, "a", ft.integerType());

        Assertions.assertFalse(
                f.isSubtypeOf(ft.integerType()) || f.isSubtypeOf(ft.stringType()) || f.isSubtypeOf(a));
        Assertions.assertFalse(
                g.isSubtypeOf(ft.integerType()) || g.isSubtypeOf(ft.stringType()) || g.isSubtypeOf(a));
        Assertions.assertFalse(!f.isSubtypeOf(E) || !g.isSubtypeOf(E), "constructors are subtypes of the adt");

        Assertions.assertFalse(f.isSubtypeOf(g) || g.isSubtypeOf(f), "alternative constructors should be incomparable");

        Assertions.assertTrue(f.isSubtypeOf(ft.nodeType()), "A constructor should be a node");
        Assertions.assertTrue(g.isSubtypeOf(ft.nodeType()), "A constructor should be a node");
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testVoid(TypeFactory ft, Type t) {
        assertTrue(ft.voidType().isSubtypeOf(t));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testVoidProblem1(TypeFactory ft) {
        assertFalse(ft.listType(ft.voidType()).isSubtypeOf(ft.voidType()));
        assertFalse(ft.setType(ft.voidType()).isSubtypeOf(ft.voidType()));
        assertFalse(ft.relType(ft.voidType()).isSubtypeOf(ft.voidType()));
        assertFalse(ft.mapType(ft.voidType(), ft.voidType()).isSubtypeOf(ft.voidType()));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testTupleWithVoidIsVoid(TypeFactory ft) {
        assertTrue(ft.tupleType(ft.voidType()).isSubtypeOf(ft.voidType()));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void idempotentLub(Type t) {
        Type l = t.lub(t);
        assertTrue (l.lub(t) == l);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void idempotentGlb(Type t) {
        Type g = t.glb(t);
        if (g.glb(t) != g) {
            fail("glb is not idempotent for " + t + ", g = t.glb(t) =" + g + ", g.glb(t) " + g.glb(t));
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void commutativeLub(Type t1, Type t2) {
        Type lub1 = t1.lub(t2);
        Type lub2 = t2.lub(t1);

        if (lub1 != lub2) {
            System.err.println("Failure:");
            System.err.println(t1 + ".lub(" + t2 + ") = " + lub1);
            System.err.println(t2 + ".lub(" + t1 + ") = " + lub2);
            fail("lub should be commutative");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void commutativeGlb(Type t1, Type t2) {
        Type lub1 = t1.glb(t2);
        Type lub2 = t2.glb(t1);

        if (lub1 != lub2) {
            System.err.println("Failure:");
            System.err.println(t1 + ".glb(" + t2 + ") = " + lub1);
            System.err.println(t2 + ".glb(" + t1 + ") = " + lub2);
            fail("glb should be commutative");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void orderedLub(Type t1, Type t2) {
        if (t1.comparable(t2)) {
            if (t1.isSubtypeOf(t2)) {
                assertTrue(t2.isSubtypeOf(t1.lub(t2)));
            }
            if (t2.isSubtypeOf(t1)) {
                assertTrue(t1.isSubtypeOf(t1.lub(t2)));
            }
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void orderedGlb(Type t1, Type t2) {
        if (t1.comparable(t2)) {
            if (t1.isSubtypeOf(t2)) {
                assertTrue(t1.glb(t2).isSubtypeOf(t1));
            }
            if (t2.isSubtypeOf(t1)) {
                assertTrue(t1.glb(t2).isSubtypeOf(t2));
            }
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void lubWithVoid(TypeFactory tf, Type t) {
        assertTrue(tf.voidType().lub(t) == t);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void glbWithVoid(TypeFactory tf, Type t) {
        assertTrue(tf.voidType().glb(t) == tf.voidType());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void lubWithValue(TypeFactory tf, Type t) {
        assertTrue(tf.valueType().lub(t) == tf.valueType());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void glbWithValue(TypeFactory tf, Type t) {
        assertTrue(tf.valueType().glb(t) == t);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testGetTypeDescriptor(Type t1, Type t2) {
        if (t1.toString().equals(t2.toString())) {
            if (t1 != t2) {
                fail("Type descriptors should be canonical:" + t1.toString() + " == " + t2.toString());
            }
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testMatchAndInstantiate(TypeFactory ft, TypeStore store) {
        Type X = ft.parameterType("X");
        Map<Type, Type> bindings = new HashMap<>();

        Type subject = ft.integerType();
        X.match(subject, bindings);

        if (!bindings.get(X).equals(subject)) {
            fail("simple match failed");
        }

        if (!X.instantiate(bindings).equals(subject)) {
            fail("instantiate failed");
        }

        Type relXX = ft.relType(X, X);
        bindings.clear();
        subject = ft.relType(ft.integerType(), ft.integerType());
        relXX.match(subject, bindings);

        if (!bindings.get(X).equals(ft.integerType())) {
            fail("relation match failed");
        }

        if (!relXX.instantiate(bindings).equals(subject)) {
            fail("instantiate failed");
        }

        bindings.clear();
        subject = ft.relType(ft.integerType(), ft.realType());
        relXX.match(subject, bindings);

        Type lub = ft.integerType().lub(ft.realType());
        if (!bindings.get(X).equals(lub)) {
            fail("lubbing during matching failed");
        }

        if (!relXX.instantiate(bindings).equals(ft.relType(lub, lub))) {
            fail("instantiate failed");
        }

        bindings.clear();
        Type adtX = ft.abstractDataType(store, "A", X);
        bindings.put(X, ft.voidType());
        assertTrue(adtX.instantiate(bindings).isSubtypeOf(adtX));

        bindings.put(X, ft.integerType());
        assertTrue(adtX.instantiate(bindings).isSubtypeOf(adtX));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testAlias(TypeFactory ft) {
        Type alias = ft.aliasType(new TypeStore(), "myValue", ft.valueType());

        assertTrue(alias.isSubtypeOf(ft.valueType()));
        assertTrue(ft.valueType().isSubtypeOf(alias));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void allComparableTypesIntersect(Type t, Type u) {
        if (!t.isBottom() && !u.isBottom() && t.comparable(u)) {
            if (!t.intersects(u)) {
                fail("comparable types should also intersect: " + t + ", " + u);
            }
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void intersectionIsCommutative(Type t, Type u) {
        if (t.intersects(u)) {
            if (!u.intersects(t)) {
                fail("intersection should be commutative: " + t + "," + u);
            }
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void intersectionIsReflexive(Type t) {
        if (!t.isBottom() && !t.intersects(t)) {
            fail("intersection should be reflexive: " + t);
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void nothingIntersectsVoid(TypeFactory tf, Type t) {
        if (t.intersects(tf.voidType())) {
            fail("intersect with void?");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void everythingIntersectsValue(TypeFactory tf, Type t) {
        if (!t.isBottom() && !t.intersects(tf.valueType())) {
            fail("does not intersect with value?");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void intersectionIsNonVoidGreatestLowerBound(Type t, Type u) {
        // t /\ u != {} <==> glb(t, u) != void

        if (t.intersects(u)) {
            if(t.glb(u).isBottom()) {
                fail("glb should not be bottom for intersecting types:" + t + ", " + u + ": glb is " + t.glb(u));
            }
        }

        if (!t.glb(u).isBottom()) {
            assertTrue(t.intersects(u));
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void collectionTypesAlwaysIntersect(TypeFactory tf, Type t, Type u) {
        assertTrue(tf.setType(t).intersects(tf.setType(u)));
        assertTrue(tf.listType(t).intersects(tf.listType(u)));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void mapTypesAlwaysIntersect(TypeFactory tf, Type t, Type u) {
        assertTrue(tf.mapType(t, u).intersects(tf.mapType(u, t)));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void openTypeParametersAlwaysMatch(TypeFactory tf, Type matched) {
        assertTrue(tf.parameterType("Free").match(matched, new HashMap<>()));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void openTypeParametersAlwaysMatchReversed(TypeFactory tf, Type matched) {
        assertTrue(matched.match(tf.parameterType("Free"), new HashMap<>()));
    }
}
