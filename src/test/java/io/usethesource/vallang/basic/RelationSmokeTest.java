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

import java.util.Iterator;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;

public final class RelationSmokeTest {

    private IValue[] integers(IValueFactory vf) {
        IValue[] integers = new IValue[5];

        for (int i = 0; i < integers.length; i++) {
            integers[i] = vf.integer(i);
        }

        return integers;
    }

    private IValue[] doubles(IValueFactory vf) {
        IValue[] doubles = new IValue[10];

        for (int i = 0; i < doubles.length; i++) {
            doubles[i] = vf.real(i);
        }

        return doubles;
    }


    private ITuple[] integerTuples(IValueFactory vf) {
        IValue[] integers = integers(vf);
        ITuple[] integerTuples = new ITuple[integers.length * integers.length];

        for (int i = 0; i < integers.length; i++) {
            for (int j = 0; j < integers.length; j++) {
                ITuple t = vf.tuple(integers[i], integers[j]);
                integerTuples[i * integers.length + j] = t;
            }
        }

        return integerTuples;
    }

    private ITuple[] doubleTuples(IValueFactory vf) {
        IValue[] integers = doubles(vf);
        ITuple[] integerTuples = new ITuple[integers.length * integers.length];

        for (int i = 0; i < integers.length; i++) {
            for (int j = 0; j < integers.length; j++) {
                ITuple t = vf.tuple(integers[i], integers[j]);
                integerTuples[i * integers.length + j] = t;
            }
        }

        return integerTuples;
    }

    private ISet setOfIntegers(IValueFactory vf) {
        ISetWriter lw = vf.setWriter();

        for (IValue i : integers(vf)) {
            lw.append(i);
        }

        return lw.done();
    }

    private ISet setOfDoubles(IValueFactory vf) {
        ISetWriter lw = vf.setWriter();

        for (IValue i : doubles(vf)) {
            lw.append(i);
        }

        return lw.done();
    }

    private ISet integerRelation(IValueFactory vf) {
        ISetWriter lw = vf.setWriter();

        for (IValue i : integerTuples(vf)) {
            lw.append(i);
        }

        return lw.done();
    }

    private ISet doubleRelation(IValueFactory vf) {
        ISetWriter lw = vf.setWriter();

        for (IValue i : doubleTuples(vf)) {
            lw.append(i);
        }

        return lw.done();
    }


    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testIsEmpty(IValueFactory vf) {
        if (integerRelation(vf).isEmpty()) {
            fail("integerRelation is not empty");
        }

        if (!vf.set().isEmpty()) {
            fail("this relation should be empty");
        }

        ISet emptyRel = vf.set();
        if (!emptyRel.isEmpty()) {
            fail("empty relation is not empty?");
        }
        if (!emptyRel.getType().isRelation()) {
            fail("empty relation should have relation type");
        }

    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testSize(IValueFactory vf) {
        if (integerRelation(vf).size() != integerTuples(vf).length) {
            fail("relation size is not correct");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testArity(IValueFactory vf) {
        if (integerRelation(vf).asRelation().arity() != 2) {
            fail("arity should be 2");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testProductIRelation(IValueFactory vf) {
        ISet prod = integerRelation(vf).product(integerRelation(vf));

        if (prod.asRelation().arity() != 2) {
            fail("arity of product should be 2");
        }

        if (prod.size() != integerRelation(vf).size() * integerRelation(vf).size()) {
            fail("size of product should be square of size of integerRelation");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testProductISet(IValueFactory vf) {
        ISet prod = integerRelation(vf).product(setOfIntegers(vf));

        if (prod.asRelation().arity() != 2) {
            fail("arity of product should be 2");
        }

        if (prod.size() != integerRelation(vf).size() * setOfIntegers(vf).size()) {
            fail("size of product should be square of size of integerRelation");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testClosureFailureOnIrreflexiveRelation(IValueFactory vf) {
        // rascal>{<"",{}>}+
        // java.lang.AssertionError
        // (internal error)
        //         at $shell$(|main://$shell$|)
        // java.lang.AssertionError
        //         at io.usethesource.vallang.impl.persistent.PersistentHashIndexedBinaryRelation.<init>(PersistentHashIndexedBinaryRelation.java:74)
        //         at io.usethesource.vallang.impl.persistent.PersistentSetFactory.from(PersistentSetFactory.java:67)
        //         at io.usethesource.vallang.impl.persistent.PersistentHashIndexedBinaryRelation.closure(PersistentHashIndexedBinaryRelation.java:573)
        ISet input = vf.set(vf.tuple(vf.string(""), vf.set()));

        assertTrue(input.asRelation().closure().equals(input));
    }


    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testClosure(IValueFactory vf) {
        try {
            if (!integerRelation(vf).asRelation().closure().equals(integerRelation(vf))) {
                fail("closure adds extra tuples?");
            }
        } catch (FactTypeUseException e) {
            fail("integerRelation is reflexive, so why an error?");
        }

        try {
            ISet rel = vf.set();
            rel.asRelation().closure();
        } catch (FactTypeUseException e) {
            fail("reflexivity with subtyping is allowed");
        }

        try {
            ITuple t1 = vf.tuple(integers(vf)[0], integers(vf)[1]);
            ITuple t2 = vf.tuple(integers(vf)[1], integers(vf)[2]);
            ITuple t3 = vf.tuple(integers(vf)[2], integers(vf)[3]);
            ITuple t4 = vf.tuple(integers(vf)[0], integers(vf)[2]);
            ITuple t5 = vf.tuple(integers(vf)[1], integers(vf)[3]);
            ITuple t6 = vf.tuple(integers(vf)[0], integers(vf)[3]);

            ISet test = vf.set(t1, t2, t3);
            ISet closed = test.asRelation().closure();

            if (closed.asRelation().arity() != test.asRelation().arity()) {
                fail("closure should produce relations of same arity");
            }

            if (closed.size() != 6) {
                fail("closure contains too few elements");
            }

            if (!closed.intersect(test).equals(test)) {
                fail("closure should contain all original elements");
            }

            if (!closed.contains(t4) || !closed.contains(t5) || !closed.contains(t6)) {
                fail("closure does not contain required elements");
            }

        } catch (FactTypeUseException e) {
            fail("this should all be type correct");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testCompose(IValueFactory vf) {
        try {
            ISet comp = integerRelation(vf).asRelation().compose(integerRelation(vf).asRelation());

            if (comp.asRelation().arity() != integerRelation(vf).asRelation().arity() * 2 - 2) {
                fail("composition is a product with the last column of the first relation and the first column of the last relation removed");
            }

            if (comp.size() != integerRelation(vf).size()) {
                fail("numner of expected tuples is off");
            }
        } catch (FactTypeUseException e) {
            fail("the above should be type correct");
        }

        try {
            ITuple t1 = vf.tuple(integers(vf)[0], doubles(vf)[0]);
            ITuple t2 = vf.tuple(integers(vf)[1], doubles(vf)[1]);
            ITuple t3 = vf.tuple(integers(vf)[2], doubles(vf)[2]);
            ISet rel1 = vf.set(t1, t2, t3);

            ITuple t4 = vf.tuple(doubles(vf)[0], integers(vf)[0]);
            ITuple t5 = vf.tuple(doubles(vf)[1], integers(vf)[1]);
            ITuple t6 = vf.tuple(doubles(vf)[2], integers(vf)[2]);
            ISet rel2 = vf.set(t4, t5, t6);

            ITuple t7 = vf.tuple(integers(vf)[0], integers(vf)[0]);
            ITuple t8 = vf.tuple(integers(vf)[1], integers(vf)[1]);
            ITuple t9 = vf.tuple(integers(vf)[2], integers(vf)[2]);
            ISet rel3 = vf.set(t7, t8, t9);
            assertTrue(vf
                .set(vf.tuple(doubles(vf)[0], doubles(vf)[0])).asRelation().compose(rel1.asRelation()).isEmpty(),
                "Non-comparable types should yield empty composition result.");
            ISet comp = rel1.asRelation().compose(rel2.asRelation());

            if (!comp.equals(rel3)) {
                fail("composition does not produce expected result");
            }
        } catch (FactTypeUseException e) {
            fail("the above should be type correct");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testContains(IValueFactory vf) {
        try {
            for (ITuple t : integerTuples(vf)) {
                if (!integerRelation(vf).contains(t)) {
                    fail("contains returns false instead of true");
                }
            }
        } catch (FactTypeUseException e) {
            fail("this should be type correct");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testInsert(IValueFactory vf) {
        try {
            ISet rel = integerRelation(vf).insert(vf.tuple(vf.integer(0), vf.integer(0)));

            if (!rel.equals(integerRelation(vf))) {
                fail("insert into a relation of an existing tuple should not change the relation");
            }

            ISetWriter relw3 = vf.setWriter();
            relw3.insertAll(integerRelation(vf));
            ISet rel3 = relw3.done();

            final ITuple tuple = vf.tuple(vf.integer(100), vf.integer(100));
            ISet rel4 = rel3.insert(tuple);

            if (rel4.size() != integerRelation(vf).size() + 1) {
                fail("insert failed");
            }

            if (!rel4.contains(tuple)) {
                fail("insert failed");
            }

        } catch (FactTypeUseException e) {
            fail("the above should be type correct");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testIntersectIRelation(IValueFactory vf) {

        try {
            if (!integerRelation(vf).intersect(doubleRelation(vf)).isEmpty()) {
                fail("non-intersecting relations should produce empty intersections");
            }

            ISet oneTwoThree = vf.set(integerTuples(vf)[0], integerTuples(vf)[1], integerTuples(vf)[2]);
            ISet threeFourFive = vf.set(integerTuples(vf)[2], integerTuples(vf)[3], integerTuples(vf)[4]);
            ISet result = vf.set(integerTuples(vf)[2]);

            if (!oneTwoThree.intersect(threeFourFive).equals(result)) {
                fail("intersection failed");
            }
            if (!threeFourFive.intersect(oneTwoThree).equals(result)) {
                fail("intersection should be commutative");
            }

            if (!oneTwoThree.intersect(vf.set())
                .isEmpty()) {
                fail("intersection with empty set should produce empty");
            }

        } catch (FactTypeUseException e) {
            fail("the above should all be type safe");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testIntersectISet(IValueFactory vf, TypeFactory tf) {
        ISet empty1 = vf.set();
        ISet empty2 = vf.set();

        try {
            final ISet intersection = empty1.intersect(empty2);
            if (!intersection.isEmpty()) {
                fail("empty intersection failed");
            }

            Type type = intersection.getType();
            if (!type.getFieldType(0).isSubtypeOf(tf.numberType())) {
                fail("intersection should produce lub types");
            }
        } catch (FactTypeUseException e) {
            fail("intersecting types which have a lub should be possible");
        }

        try {
            if (!integerRelation(vf).intersect(doubleRelation(vf)).isEmpty()) {
                fail("non-intersecting relations should produce empty intersections");
            }

            ISet oneTwoThree = vf.set(integerTuples(vf)[0], integerTuples(vf)[1], integerTuples(vf)[2]);
            ISet threeFourFive = vf.set(integerTuples(vf)[2], integerTuples(vf)[3], integerTuples(vf)[4]);
            ISet result = vf.set(integerTuples(vf)[2]);

            if (!oneTwoThree.intersect(threeFourFive).equals(result)) {
                fail("intersection failed");
            }
            if (!threeFourFive.intersect(oneTwoThree).equals(result)) {
                fail("intersection should be commutative");
            }

            if (!oneTwoThree.intersect(vf.set())
                .isEmpty()) {
                fail("intersection with empty set should produce empty");
            }

        } catch (FactTypeUseException e) {
            fail("the above should all be type safe");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testSubtractIRelation(IValueFactory vf) {
        ISet empty1 = vf.set();
        ISet empty2 = vf.set();

        try {
            final ISet diff = empty1.subtract(empty2);
            if (!diff.isEmpty()) {
                fail("empty diff failed");
            }

        } catch (FactTypeUseException e) {
            fail("subtracting types which have a lub should be possible");
        }

        try {
            ISet oneTwoThree = vf.set(integerTuples(vf)[0], integerTuples(vf)[1], integerTuples(vf)[2]);
            ISet threeFourFive = vf.set(integerTuples(vf)[2], integerTuples(vf)[3], integerTuples(vf)[4]);
            ISet result1 = vf.set(integerTuples(vf)[0], integerTuples(vf)[1]);
            ISet result2 = vf.set(integerTuples(vf)[3], integerTuples(vf)[4]);

            if (!oneTwoThree.subtract(threeFourFive).equals(result1)) {
                fail("subtraction failed");
            }
            if (!threeFourFive.subtract(oneTwoThree).equals(result2)) {
                fail("subtraction failed");
            }

            ISet empty3 = vf.set();
            if (!empty3.subtract(threeFourFive).isEmpty()) {
                fail("subtracting from empty set should produce empty");
            }

        } catch (FactTypeUseException e) {
            fail("the above should all be type safe");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testSubtractIRelationWithHashCollisions(IValueFactory vf) {
        var val1 = vf.node("");
        var val2 = vf.string("\u0000");
        var val3 = vf.string("X");
        var empty = vf.list();
        assertTrue(val1.hashCode() == 0 && val1.hashCode() == val2.hashCode()
            , "these two values should have the same hash code (0) for this test to be effective");

        ISet whole = vf.set(vf.tuple(val1, empty), vf.tuple(val2, empty), vf.tuple(val3, empty));

        assertTrue(whole.delete(vf.tuple(val1, empty)).equals(vf.set(vf.tuple(val2, empty), vf.tuple(val3, empty))));
        // assertTrue(whole.delete(vf.tuple(val2, empty)).equals(vf.set(vf.tuple(val1, empty), vf.tuple(val3, empty))));
        // assertTrue(whole.delete(vf.tuple(val3, empty)).equals(vf.set(vf.tuple(val1, empty), vf.tuple(val2, empty))));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testSubtractISet(IValueFactory vf) {
        ISet empty1 = vf.set();
        ISet empty2 = vf.set();

        try {
            final ISet diff = empty1.subtract(empty2);
            if (!diff.isEmpty()) {
                fail("empty diff failed");
            }

        } catch (FactTypeUseException e) {
            fail("subtracting types which have a lub should be possible");
        }

        try {
            ISet oneTwoThree = vf.set(integerTuples(vf)[0], integerTuples(vf)[1], integerTuples(vf)[2]);
            ISet threeFourFive = vf.set(integerTuples(vf)[2], integerTuples(vf)[3], integerTuples(vf)[4]);
            ISet result1 = vf.set(integerTuples(vf)[0], integerTuples(vf)[1]);

            if (!oneTwoThree.subtract(threeFourFive).equals(result1)) {
                fail("subtraction failed");
            }

            ISet empty3 = vf.set();
            if (!empty3.subtract(threeFourFive).isEmpty()) {
                fail("subtracting from empty set should produce empty");
            }

        } catch (FactTypeUseException e) {
            fail("the above should all be type safe");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testUnionIRelation(IValueFactory vf) {
        try {
            if (integerRelation(vf).union(doubleRelation(vf)).size() != integerRelation(vf).size()
                + doubleRelation(vf).size()) {
                fail("non-intersecting non-intersectiopn relations should produce relation that is the sum of the sizes");
            }

            ISet oneTwoThree = vf.set(integerTuples(vf)[0], integerTuples(vf)[1], integerTuples(vf)[2]);
            ISet threeFourFive = vf.set(integerTuples(vf)[2], integerTuples(vf)[3], integerTuples(vf)[4]);
            ISet result = vf.set(integerTuples(vf)[0], integerTuples(vf)[1], integerTuples(vf)[2], integerTuples(vf)[3],
                integerTuples(vf)[4]);

            if (!oneTwoThree.union(threeFourFive).equals(result)) {
                fail("union failed");
            }
            if (!threeFourFive.union(oneTwoThree).equals(result)) {
                fail("union should be commutative");
            }

            if (!oneTwoThree.union(vf.set())
                .equals(oneTwoThree)) {
                fail("union with empty set should produce same set");
            }

        } catch (FactTypeUseException e) {
            fail("the above should all be type safe");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testEmptySetIsARelation(IValueFactory vf) {
        assertTrue(vf.set().getType().isRelation());

        ISet r = vf.set().insert(vf.tuple(vf.integer(1), vf.integer(2)));
        r = r.subtract(r);
        assertTrue(r.getType().isRelation());

        ISet s = vf.set().insert(vf.integer(1));
        s = s.subtract(s);
        assertTrue(s.getType().isRelation()); // yes really!
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testUnionISet(IValueFactory vf) {
        try {
            if (integerRelation(vf).union(doubleRelation(vf)).size() != integerRelation(vf).size()
                + doubleRelation(vf).size()) {
                fail("non-intersecting non-intersectiopn relations should produce relation that is the sum of the sizes");
            }

            ISet oneTwoThree = vf.set(integerTuples(vf)[0], integerTuples(vf)[1], integerTuples(vf)[2]);
            ISet threeFourFive = vf.set(integerTuples(vf)[2], integerTuples(vf)[3], integerTuples(vf)[4]);
            ISet result = vf.set(integerTuples(vf)[0], integerTuples(vf)[1], integerTuples(vf)[2], integerTuples(vf)[3], integerTuples(vf)[4]);

            if (!oneTwoThree.union(threeFourFive).equals(result)) {
                fail("union failed");
            }
            if (!threeFourFive.union(oneTwoThree).equals(result)) {
                fail("union should be commutative");
            }

            if (!oneTwoThree.union(vf.set()).equals(oneTwoThree)) {
                fail("union with empty set should produce same set");
            }

        } catch (FactTypeUseException e) {
            fail("the above should all be type safe");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testIterator(IValueFactory vf) {
        try {
            Iterator<IValue> it = integerRelation(vf).iterator();

            int i;
            for (i = 0; it.hasNext(); i++) {
                ITuple t = (ITuple) it.next();

                if (!integerRelation(vf).contains(t)) {
                    fail("iterator produces strange elements?");
                }
            }

            if (i != integerRelation(vf).size()) {
                fail("iterator skipped elements");
            }
        } catch (FactTypeUseException e) {
            fail("the above should be type correct");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testCarrier(IValueFactory vf, TypeFactory tf) {
        ISet carrier = integerRelation(vf).asRelation().carrier();

        if (!carrier.equals(setOfIntegers(vf))) {
            fail("carrier should be equal to this set");
        }

        try {
            ITuple t1 = vf.tuple(integers(vf)[0], doubles(vf)[0]);
            ITuple t2 = vf.tuple(integers(vf)[1], doubles(vf)[1]);
            ITuple t3 = vf.tuple(integers(vf)[2], doubles(vf)[2]);
            ISet rel1 = vf.set(t1, t2, t3);

            ISet carrier1 = rel1.asRelation().carrier();

            if (carrier1.getElementType() != tf.numberType()) {
                fail("expected number type on carrier");
            }

            if (carrier1.size() != 6) {
                fail("carrier does not contain all elements");
            }

            if (carrier1.intersect(setOfIntegers(vf)).size() != 3) {
                fail("integers should be in there still");
            }

            if (carrier1.intersect(setOfDoubles(vf)).size() != 3) {
                fail("doubles should be in there still");
            }
        } catch (FactTypeUseException e) {
            fail("the above should be type correct");
        }

    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testIndex(IValueFactory vf) {
        testIndex(integerRelation(vf));
        testIndex(doubleRelation(vf));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testEmptyIndex(IValueFactory vf) {
        assertTrue(integerRelation(vf).asRelation().index(vf.integer(integers(vf).length +  1)).isEmpty());
    }

    private void testIndex(ISet targetRel) {
        for (IValue key: targetRel.asRelation().domain()) {
            ISet values = targetRel.asRelation().index(key);
            for (IValue val : targetRel) {
                ITuple t = (ITuple) val;
                if (t.get(0).equals(key)) {
                    assertTrue(values.contains(t.get(1)));
                    values = values.delete(t.get(1));
                }
            }
            assertTrue(values.isEmpty());
        }
    }
}
