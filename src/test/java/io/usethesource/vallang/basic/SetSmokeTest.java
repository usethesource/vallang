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

import java.util.Iterator;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.TypeFactory;

public final class SetSmokeTest {

    private IValue[] integers(IValueFactory vf) {
        IValue[] integers = new IValue[100];

        for (int i = 0; i < integers.length; i++) {
            integers[i] = vf.integer(i);
        }

        return integers;
    }
    
    private IValue[] doubles(IValueFactory vf) {
        IValue[] integers = new IValue[100];

        for (int i = 0; i < integers.length; i++) {
            integers[i] = vf.real(i);
        }

        return integers;
    }
    
  private ISet integerUniverse(IValueFactory vf) {
      ISetWriter w = vf.setWriter();
      
      for (IValue i : integers(vf)) {
          w.insert(i);
      }
      
      return w.done();
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testInsert(IValueFactory vf) {
    ISet set1 = vf.set();
    ISet set2;

    try {
      set2 = set1.insert(integers(vf)[0]);

      if (set2.size() != 1) {
        fail("insertion failed");
      }

      if (!set2.contains(integers(vf)[0])) {
        fail("insertion failed");
      }

    } catch (FactTypeUseException e1) {
      fail("type checking error:" + e1);
    }

    ISetWriter numberSet = vf.setWriter();

    try {
      numberSet.insert(integers(vf)[0]);
      numberSet.insert(doubles(vf)[0]);
    } catch (FactTypeUseException e) {
      fail("should be able to insert subtypes:" + e);
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testEmpty(IValueFactory vf) {
    ISet emptySet = vf.set();
    if (!emptySet.isEmpty()) {
      fail("empty set is not empty?");
    }

    if (!emptySet.getType().isRelation()) {
      fail("empty set should have relation type (yes really!)");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testContains(IValueFactory vf) {
    ISet set1 = vf.set(integers(vf)[0], integers(vf)[1]);

    try {
      set1.contains(integers(vf)[0]);
    } catch (FactTypeUseException e) {
      fail("should be able to check for containment of integers");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testIntersect(IValueFactory vf) {
    ISet set1 = vf.set();
    ISet set2 = vf.set();
    ISet set3 = vf.set(integers(vf)[0], integers(vf)[1], integers(vf)[2]);
    ISet set4 = vf.set(integers(vf)[2], integers(vf)[3], integers(vf)[4]);
    ISet set5 = vf.set(integers(vf)[3], integers(vf)[4], integers(vf)[5]);

    try {
      if (!set1.intersect(set2).isEmpty()) {
        fail("intersect of empty sets");
      }

      if (!set1.intersect(set3).isEmpty()) {
        fail("intersect with empty set");
      }

      if (!set3.intersect(set1).isEmpty()) {
        fail("insersect with empty set");
      }

      if (set3.intersect(set4).size() != 1) {
        fail("insersect failed");
      }

      if (!set4.intersect(set3).contains(integers(vf)[2])) {
        fail("intersect failed");
      }

      if (set4.intersect(set5).size() != 2) {
        fail("insersect failed");
      }

      if (!set5.intersect(set4).contains(integers(vf)[3])
          || !set5.intersect(set4).contains(integers(vf)[4])) {
        fail("intersect failed");
      }

      if (!set5.intersect(set3).isEmpty()) {
        fail("non-intersection sets");
      }

    } catch (FactTypeUseException et) {
      fail("this shouls all be typesafe");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testIsEmpty(IValueFactory vf) {
    if (integerUniverse(vf).isEmpty()) {
      fail("an empty universe is not so cosy");
    }

    if (!vf.set().isEmpty()) {
      fail("what's in an empty set?");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testSize(IValueFactory vf) {
    if (vf.set().size() != 0) {
      fail("empty sets have size 0");
    }

    if (vf.set(integers(vf)[0]).size() != 1) {
      fail("singleton set should have size 1");
    }

    if (integerUniverse(vf).size() != integers(vf).length) {
      fail("weird size of universe");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testSubtract(IValueFactory vf) {
    ISet set1 = vf.set();
    ISet set2 = vf.set();
    ISet set3 = vf.set(integers(vf)[0], integers(vf)[1], integers(vf)[2]);
    ISet set4 = vf.set(integers(vf)[2], integers(vf)[3], integers(vf)[4]);
    ISet set5 = vf.set(integers(vf)[3], integers(vf)[4], integers(vf)[5]);

    try {
      if (!set1.subtract(set2).isEmpty()) {
        fail("subtract of empty sets");
      }

      if (!set1.subtract(set3).isEmpty()) {
        fail("subtract with empty set");
      }

      if (!set3.subtract(set1).isEqual(set3)) {
        fail("subtract with empty set");
      }

      if (!set1.subtract(set3).isEqual(set1)) {
        fail("subtract with empty set");
      }

      if (set3.subtract(set4).size() != 2) {
        fail("subtract failed");
      }

      if (set4.subtract(set3).contains(integers(vf)[2])) {
        fail("subtract failed");
      }

      if (set4.subtract(set5).size() != 1) {
        fail("insersect failed");
      }

      if (set5.subtract(set4).contains(integers(vf)[3]) || set5.subtract(set4).contains(integers(vf)[4])) {
        fail("subtract failed");
      }

    } catch (FactTypeUseException et) {
      fail("this shouls all be typesafe");
    }

  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testUnion(IValueFactory vf) {
    ISet set1 = vf.set();
    ISet set2 = vf.set();
    ISet set3 = vf.set(integers(vf)[0], integers(vf)[1], integers(vf)[2]);
    ISet set4 = vf.set(integers(vf)[2], integers(vf)[3], integers(vf)[4]);
    ISet set5 = vf.set(integers(vf)[3], integers(vf)[4], integers(vf)[5]);

    try {
      if (!set1.union(set2).isEmpty()) {
        fail("union of empty sets");
      }

      if (!set1.union(set3).isEqual(set3)) {
        fail("union with empty set");
      }

      if (!set3.union(set1).isEqual(set3)) {
        fail("union with empty set");
      }

      if (!set1.union(set3).isEqual(set3)) {
        fail("union with empty set");
      }

      if (set3.union(set4).size() != 5) {
        fail("union failed");
      }

      if (!set4.union(set3).contains(integers(vf)[0]) || !set4.union(set3).contains(integers(vf)[1])
          || !set4.union(set3).contains(integers(vf)[2]) || !set4.union(set3).contains(integers(vf)[3])
          || !set4.union(set3).contains(integers(vf)[4])) {
        fail("union failed");
      }

      if (set4.union(set5).size() != 4) {
        fail("union failed");
      }

    } catch (FactTypeUseException et) {
      fail("this shouls all be typesafe");
    }

  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testIterator(IValueFactory vf) {
    try {
      Iterator<IValue> it = integerUniverse(vf).iterator();
      int i;
      for (i = 0; it.hasNext(); i++) {
        if (!integerUniverse(vf).contains(it.next())) {
          fail("iterator produces something weird");
        }
      }
      if (i != integerUniverse(vf).size()) {
        fail("iterator did not iterate over everything");
      }
    } catch (FactTypeUseException e) {
      fail("should be type correct");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testGetElementType(IValueFactory vf) {
    if (!integerUniverse(vf).getElementType().isInteger()) {
      fail("elementType is broken");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testProductISet(IValueFactory vf) {
    ISet test = vf.set(integers(vf)[0], integers(vf)[1], integers(vf)[2], integers(vf)[3]);
    ISet prod = test.product(test);

    if (prod.asRelation().arity() != 2) {
      fail("product's arity should be 2");
    }

    if (prod.size() != test.size() * test.size()) {
      fail("product's size should be square of size");
    }

  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testProductIRelation(IValueFactory vf) {
    ISet test = vf.set(integers(vf)[0], integers(vf)[1], integers(vf)[2], integers(vf)[3]);
    ISet prod = test.product(test);
    ISet prod2 = test.product(prod);

    if (prod2.asRelation().arity() != 2) {
      fail("product's arity should be 2");
    }

    if (prod2.size() != test.size() * prod.size()) {
      fail("product's size should be multiplication of arguments' sizes");
    }

  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testTypeDoubleInsertOneRemoveWithSet(IValueFactory vf, TypeFactory tf) {
    ISet set1 = vf.set().insert(doubles(vf)[0]).insert(integers(vf)[0]).insert(integers(vf)[0]);
    ISet set2 = set1.delete(integers(vf)[0]);

    assertEquals(tf.realType(), set2.getElementType());
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testTypeDoubleInsertOneRemoveWithSetWriter(IValueFactory vf, TypeFactory tf) {
    ISetWriter w = vf.setWriter();
    w.insert(doubles(vf)[0]);
    w.insert(integers(vf)[0]);
    w.insert(integers(vf)[0]);
    ISet set1 = w.done();
    ISet set2 = set1.delete(integers(vf)[0]);

    assertEquals(tf.realType(), set2.getElementType());
  }

}
