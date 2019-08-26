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

import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.IList;
import io.usethesource.vallang.IListWriter;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.TypeFactory;

public final class ListSmokeTest {

    private IValue[] integers(IValueFactory vf) {
        IValue[] integers = new IValue[20];

        for (int i = 0; i < integers.length; i++) {
            integers[i] = vf.integer(i);
        }

        return integers;
    }

    private IList integersList(IValueFactory vf) {
        IListWriter lw = vf.listWriter();

        for (IValue i : integers(vf)) {
            lw.append(i);
        }

        return lw.done();
    }

    private IList emptyIntegerList(IValueFactory vf) {
        return vf.list();
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testGetElementType(IValueFactory vf, TypeFactory tf) {
        if (!integersList(vf).getElementType().isSubtypeOf(tf.integerType())) {
            fail("funny getElementType");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testAppend(IValueFactory vf, TypeFactory tf) {
        try {
            IValue newValue = vf.integer(integers(vf).length);
            IList longer = integersList(vf).append(newValue);

            if (longer.length() != integersList(vf).length() + 1) {
                fail("append failed");
            }

            if (!longer.get(integersList(vf).length()).equals(newValue)) {
                fail("element was not appended");
            }

        } catch (FactTypeUseException e) {
            fail("the above should be type correct");
        }

        try {
            if (!integersList(vf).append(vf.real(2)).getElementType().equivalent(tf.numberType())) {
                fail("append should lub the element type");
            }
        } catch (FactTypeUseException e) {
            // this should happen
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testGet(IValueFactory vf) {
        for (int i = 0; i < integers(vf).length; i++) {
            if (!integersList(vf).get(i).equals(integers(vf)[i])) {
                fail("get failed");
            }
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testInsert(IValueFactory vf, TypeFactory tf) {
        try {
            IValue newValue = vf.integer(integers(vf).length);
            IList longer = integersList(vf).insert(newValue);

            if (longer.length() != integersList(vf).length() + 1) {
                fail("append failed");
            }

            if (!longer.get(0).equals(newValue)) {
                fail("element was not insrrted");
            }

        } catch (FactTypeUseException e) {
            fail("the above should be type correct");
        }

        try {
            if (!integersList(vf).insert(vf.real(2)).getElementType().equivalent(tf.numberType())) {
                fail("insert should lub the element type");
            }
        } catch (FactTypeUseException e) {
            // this should happen
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testLength(IValueFactory vf) {
        if (vf.list().length() != 0) {
            fail("empty list should be size 0");
        }

        if (integersList(vf).length() != integers(vf).length) {
            fail("length does not count amount of elements");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testReverse(IValueFactory vf) {
        IList reverse = integersList(vf).reverse();

        if (reverse.getType() != integersList(vf).getType()) {
            fail("reverse should keep type");
        }

        if (reverse.length() != integersList(vf).length()) {
            fail("length of reverse is different");
        }

        for (int i = 0; i < integers(vf).length; i++) {
            if (!reverse.get(i).equals(integers(vf)[integers(vf).length - i - 1])) {
                fail("reverse did something funny: " + reverse + " is not reverse of " + integersList(vf));
            }
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testShuffle(IValueFactory vf) {
        IList shuffle = integersList(vf).shuffle(new Random());

        if (shuffle.getType() != integersList(vf).getType()) {
            fail("shuffle should keep type");
        }

        if (shuffle.length() != integersList(vf).length()) {
            fail("length after shuffle is different");
        }
    }

    // doesn't completly test distribution, but at least protects against some cases
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testShuffleFirstLast(IValueFactory vf) {
        Set<IValue> first = new HashSet<>();
        Set<IValue> last = new HashSet<>();
        Random r = new Random();
        for (int i = 0; i < 20 * integersList(vf).length(); i++) {
            IList shuffled = integersList(vf).shuffle(r);
            first.add(shuffled.get(0));
            last.add(shuffled.get(shuffled.length() - 1));
        }
        for (IValue v : integersList(vf)) {
            if (!first.contains(v)) {
                fail("The shuffle doesn't shuffle the first index correctly");
            }
            if (!last.contains(v)) {
                fail("The shuffle doesn't shuffle the last index correctly");
            }
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testReverseEmpty(IValueFactory vf) {
        IList reverse = emptyIntegerList(vf).reverse();

        if (reverse.getType() != emptyIntegerList(vf).getType()) {
            fail("reverse should keep type");
        }

        if (reverse.length() != emptyIntegerList(vf).length()) {
            fail("length of reverse is different");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testIterator(IValueFactory vf) {
        Iterator<IValue> it = integersList(vf).iterator();

        int i;
        for (i = 0; it.hasNext(); i++) {
            IValue v = it.next();
            if (!v.equals(integers(vf)[i])) {
                fail("iterator does not iterate in order");
            }
        }
    }

    // NOTE: This is not a very good test, but sufficient for it's purpose.
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testSubList(IValueFactory vf) {
        // Front
        IListWriter flw = vf.listWriter();
        for (int i = 0; i < 20; i++) {
            flw.append(vf.integer(i));
        }
        IList fList = flw.done();

        // Back
        IListWriter blw = vf.listWriter();
        for (int i = 19; i >= 0; i--) {
            blw.insert(vf.integer(i));
        }
        IList bList = blw.done();

        // Overlap
        IListWriter olw = vf.listWriter();
        for (int i = 9; i >= 0; i--) {
            olw.insert(vf.integer(i));
        }
        for (int i = 10; i < 20; i++) {
            olw.append(vf.integer(i));
        }
        IList oList = olw.done();

        IList fSubList = fList.sublist(0, 5);
        IList bSubList = bList.sublist(0, 5);
        IList oSubList = oList.sublist(0, 5);
        checkSubListEquality(fSubList, bSubList, oSubList);

        fSubList = fList.sublist(1, 5);
        bSubList = bList.sublist(1, 5);
        oSubList = oList.sublist(1, 5);
        checkSubListEquality(fSubList, bSubList, oSubList);

        fSubList = fList.sublist(0, 15);
        bSubList = bList.sublist(0, 15);
        oSubList = oList.sublist(0, 15);
        checkSubListEquality(fSubList, bSubList, oSubList);

        fSubList = fList.sublist(1, 15);
        bSubList = bList.sublist(1, 15);
        oSubList = oList.sublist(1, 15);
        checkSubListEquality(fSubList, bSubList, oSubList);

        fSubList = fList.sublist(5, 5);
        bSubList = bList.sublist(5, 5);
        oSubList = oList.sublist(5, 5);
        checkSubListEquality(fSubList, bSubList, oSubList);

        fSubList = fList.sublist(5, 10);
        bSubList = bList.sublist(5, 10);
        oSubList = oList.sublist(5, 10);
        checkSubListEquality(fSubList, bSubList, oSubList);

        fSubList = fList.sublist(15, 5);
        bSubList = bList.sublist(15, 5);
        oSubList = oList.sublist(15, 5);
        checkSubListEquality(fSubList, bSubList, oSubList);
    }

    private static void checkSubListEquality(IList fList, IList bList, IList oList) {
        if (!fList.equals(bList) || !bList.equals(oList))
            fail("IList#subList is broken: " + fList + " " + bList + " " + oList);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testIsSubListOf(IValueFactory vf) {
        IListWriter w = vf.listWriter();

        for (int i = integers(vf).length - 1; i >= 0; i -= 2) {
            w.insert(vf.integer(i));
        }

        IList even = w.done();

        w = vf.listWriter();

        for (int i = integers(vf).length - 2; i >= 0; i -= 2) {
            w.insert(vf.integer(i));
        }

        IList odd = w.done();
        if (!integersList(vf).isSubListOf(integersList(vf)))
            fail("integerList should be sublist of integerList");
        if (!even.isSubListOf(integersList(vf)))
            fail("even should be sublist of integerList");
        if (!odd.isSubListOf(integersList(vf)))
            fail("odd should be sublist of integerList");

        if (integersList(vf).isSubListOf(even))
            fail("integerList cannot be sublist of even");
        if (integersList(vf).isSubListOf(odd))
            fail("integerList cannot be sublist of odd");
        if (even.isSubListOf(odd))
            fail("even cannot be sublist of odd");
        if (odd.isSubListOf(even))
            fail("odd cannot be sublist of even");

        IList L123 = vf.list(integers(vf)[1], integers(vf)[2], integers(vf)[3]);
        IList L918273 =
                vf.list(integers(vf)[9], integers(vf)[1], integers(vf)[8], integers(vf)[2], integers(vf)[7], integers(vf)[3]);
        IList L918372 =
                vf.list(integers(vf)[9], integers(vf)[1], integers(vf)[8], integers(vf)[3], integers(vf)[7], integers(vf)[2]);

        if (!L123.isSubListOf(L918273))
            fail("123 is sublist of 918273");
        if (L123.isSubListOf(L918372))
            fail("123 is not a sublist of 918372");
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testSubtract(IValueFactory vf) {
        IList L12312 = vf.list(integers(vf)[1], integers(vf)[2], integers(vf)[3], integers(vf)[1], integers(vf)[2]);
        IList L123 = vf.list(integers(vf)[1], integers(vf)[2], integers(vf)[3]);
        IList L12 = vf.list(integers(vf)[1], integers(vf)[2]);
        IList L321321 =
                vf.list(integers(vf)[3], integers(vf)[2], integers(vf)[1], integers(vf)[3], integers(vf)[2], integers(vf)[1]);

        if (!checkListEquality(L12312.subtract(L123), L12))
            fail("12312 subtract 123 should be 12");
        if (!L12312.subtract(L321321).isEmpty())
            fail("12312 subtract 123213213 should be empty");
    }

    private boolean checkListEquality(IList lst1, IList lst2) {
        return lst1.isSubListOf(lst2) && lst2.isSubListOf(lst2);

    }
}
