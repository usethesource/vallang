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

import static org.junit.Assert.fail;

import java.util.Iterator;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.IList;
import io.usethesource.vallang.IListWriter;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;

public class ListRelationSmokeTest {

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
  
  private IList listOfIntegers(IValueFactory vf) {
      IListWriter lw = vf.listWriter();
      
      for (IValue i : integers(vf)) {
          lw.append(i);
      }
      
      return lw.done();
  }
  
  private IList listOfDoubles(IValueFactory vf) {
      IListWriter lw = vf.listWriter();
      
      for (IValue i : doubles(vf)) {
          lw.append(i);
      }
      
      return lw.done();
  }
  
  private IList integerListRelation(IValueFactory vf) {
      IListWriter lw = vf.listWriter();
      
      for (IValue i : integerTuples(vf)) {
          lw.append(i);
      }
      
      return lw.done();
  }
  
  private IList doubleListRelation(IValueFactory vf) {
      IListWriter lw = vf.listWriter();
      
      for (IValue i : doubleTuples(vf)) {
          lw.append(i);
      }
      
      return lw.done();
  }
  

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testIsEmpty(IValueFactory vf) {
    if (integerListRelation(vf).isEmpty()) {
      fail("integerRelation is not empty");
    }

    if (!vf.list().isEmpty()) {
      fail("this relation should be empty");
    }

    IList emptyRel = vf.list();
    if (!emptyRel.isEmpty()) {
      fail("empty relation is not empty?");
    }
    if (!emptyRel.getType().isListRelation()) {
      fail("empty relation should have relation type");
    }

  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testSize(IValueFactory vf) {
    if (integerListRelation(vf).length() != integerTuples(vf).length) {
      fail("relation size is not correct");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testArity(IValueFactory vf) {
    if (integerListRelation(vf).asRelation().arity() != 2) {
      fail("arity should be 2");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testProductIRelation(IValueFactory vf) {
    IList prod = integerListRelation(vf).product(integerListRelation(vf));

    if (prod.asRelation().arity() != 2) {
      fail("arity of product should be 2");
    }

    if (prod.length() != integerListRelation(vf).length() * integerListRelation(vf).length()) {
      fail("size of product should be square of size of integerRelation");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testProductIList(IValueFactory vf) {
    IList prod = integerListRelation(vf).product(listOfIntegers(vf));

    if (prod.asRelation().arity() != 2) {
      fail("arity of product should be 2");
    }

    if (prod.length() != integerListRelation(vf).length() * listOfIntegers(vf).length()) {
      fail("size of product should be square of size of integerRelation");
    }
  }

//  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void xtestClosure(IValueFactory vf) {
    try {
      if (!integerListRelation(vf).asRelation().closure().isEqual(integerListRelation(vf))) {
        fail("closure adds extra tuples?");
      }
    } catch (FactTypeUseException e) {
      fail("integerRelation is reflexive, so why an error?");
    }

    try {
      ITuple t1 = vf.tuple(integers(vf)[0], integers(vf)[1]);
      IList rel = vf.list(t1);

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

      IList test = vf.list(t1, t2, t3);
      IList closed = test.asRelation().closure();

      if (closed.asRelation().arity() != test.asRelation().arity()) {
        fail("closure should produce relations of same arity");
      }

      if (closed.length() != 6) {
        fail("closure contains too few elements");
      }

      if (!closed.intersect(test).isEqual(test)) {
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
      IList comp = integerListRelation(vf).asRelation().compose(integerListRelation(vf).asRelation());

      if (comp.asRelation().arity() != integerListRelation(vf).asRelation().arity() * 2 - 2) {
        fail(
            "composition is a product with the last column of the first relation and the first column of the last relation removed");
      }

      if (comp.length() != integerListRelation(vf).length() * integers(vf).length) {
        fail("number of expected tuples is off");
      }
    } catch (FactTypeUseException e) {
      fail("the above should be type correct");
    }

    try {
      ITuple t1 = vf.tuple(integers(vf)[0], doubles(vf)[0]);
      ITuple t2 = vf.tuple(integers(vf)[1], doubles(vf)[1]);
      ITuple t3 = vf.tuple(integers(vf)[2], doubles(vf)[2]);
      IList rel1 = vf.list(t1, t2, t3);

      ITuple t4 = vf.tuple(doubles(vf)[0], integers(vf)[0]);
      ITuple t5 = vf.tuple(doubles(vf)[1], integers(vf)[1]);
      ITuple t6 = vf.tuple(doubles(vf)[2], integers(vf)[2]);
      IList rel2 = vf.list(t4, t5, t6);

      ITuple t7 = vf.tuple(integers(vf)[0], integers(vf)[0]);
      ITuple t8 = vf.tuple(integers(vf)[1], integers(vf)[1]);
      ITuple t9 = vf.tuple(integers(vf)[2], integers(vf)[2]);
      IList rel3 = vf.list(t7, t8, t9);

      IList comp = rel1.asRelation().compose(rel2.asRelation());

      if (!comp.isEqual(rel3)) {
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
        if (!integerListRelation(vf).contains(t)) {
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
      // IList rel = integerListRelation(vf).insert(vf.tuple(vf.integer(0),vf.integer(0)));
      //
      // if (!rel.isEqual(integerListRelation)) {
      // fail("insert into a relation of an existing tuple should not change the relation");
      // }

      IListWriter relw3 = vf.listWriter();
      relw3.insertAll(integerListRelation(vf));
      IList rel3 = relw3.done();

      final ITuple tuple = vf.tuple(vf.integer(100), vf.integer(100));
      IList rel4 = rel3.insert(tuple);

      if (rel4.length() != integerListRelation(vf).length() + 1) {
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
    IList empty1 = vf.list();
    IList empty2 = vf.list();

    try {
      final IList intersection = empty1.intersect(empty2);
      if (!intersection.isEmpty()) {
        fail("empty intersection failed");
      }

      Type type = intersection.getType();
      if (!type.getFieldType(0).isBottom()) {
        fail("intersection should produce lub types");
      }
    } catch (FactTypeUseException e) {
      fail("intersecting types which have a lub should be possible");
    }

    try {
      if (!integerListRelation(vf).intersect(doubleListRelation(vf)).isEmpty()) {
        fail("non-intersecting relations should produce empty intersections");
      }

      IList oneTwoThree = vf.list(integerTuples(vf)[0], integerTuples(vf)[1], integerTuples(vf)[2]);
      IList threeFourFive = vf.list(integerTuples(vf)[2], integerTuples(vf)[3], integerTuples(vf)[4]);
      IList result = vf.list(integerTuples(vf)[2]);

      if (!oneTwoThree.intersect(threeFourFive).isEqual(result)) {
        fail("intersection failed");
      }
      if (!threeFourFive.intersect(oneTwoThree).isEqual(result)) {
        fail("intersection should be commutative");
      }

      if (!oneTwoThree.intersect(vf.list())
          .isEmpty()) {
        fail("intersection with empty set should produce empty");
      }

    } catch (FactTypeUseException e) {
      fail("the above should all be type safe");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testIntersectIList(IValueFactory vf) {
    IList empty1 = vf.list();
    IList empty2 = vf.list();

    try {
      final IList intersection = empty1.intersect(empty2);
      if (!intersection.isEmpty()) {
        fail("empty intersection failed");
      }

      Type type = intersection.getType();
      if (!type.getFieldType(0).isBottom()) {
        fail("empty intersection should produce void type");
      }
    } catch (FactTypeUseException e) {
      fail("intersecting types which have a lub should be possible");
    }

    try {
      if (!integerListRelation(vf).intersect(doubleListRelation(vf)).isEmpty()) {
        fail("non-intersecting relations should produce empty intersections");
      }

      IList oneTwoThree = vf.list(integerTuples(vf)[0], integerTuples(vf)[1], integerTuples(vf)[2]);
      IList threeFourFive = vf.list(integerTuples(vf)[2], integerTuples(vf)[3], integerTuples(vf)[4]);
      IList result = vf.list(integerTuples(vf)[2]);

      if (!oneTwoThree.intersect(threeFourFive).isEqual(result)) {
        fail("intersection failed");
      }
      if (!threeFourFive.intersect(oneTwoThree).isEqual(result)) {
        fail("intersection should be commutative");
      }

      if (!oneTwoThree.intersect(vf.list())
          .isEmpty()) {
        fail("intersection with empty list should produce empty");
      }

    } catch (FactTypeUseException e) {
      fail("the above should all be type safe");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testConcatIListRelation(IValueFactory vf) {
    IList empty1 = vf.list();
    IList empty2 = vf.list();

    try {
      final IList concat = (IList) empty1.concat(empty2);
      if (!concat.isEmpty()) {
        fail("empty concat failed");
      }

      Type type = concat.getType();
      if (!type.getFieldType(0).isBottom()) {
        fail("concat should produce void type");
      }
    } catch (FactTypeUseException e) {
      fail("concat types which have a lub should be possible");
    }

    try {
      if (integerListRelation(vf).concat(doubleListRelation(vf)).length() != integerListRelation(vf).length()
          + doubleListRelation(vf).length()) {
        fail(
            "non-intersecting non-intersectiopn relations should produce relation that is the sum of the sizes");
      }

      IList oneTwoThree = vf.list(integerTuples(vf)[0], integerTuples(vf)[1], integerTuples(vf)[2]);
      IList threeFourFive = vf.list(integerTuples(vf)[3], integerTuples(vf)[4]);
      IList result1 = vf.list(integerTuples(vf)[0], integerTuples(vf)[1], integerTuples(vf)[2],
          integerTuples(vf)[3], integerTuples(vf)[4]);
      IList result2 = vf.list(integerTuples(vf)[3], integerTuples(vf)[4], integerTuples(vf)[0],
          integerTuples(vf)[1], integerTuples(vf)[2]);

      if (!oneTwoThree.concat(threeFourFive).isEqual(result1)) {
        fail("concat 1 failed");
      }
      if (!threeFourFive.concat(oneTwoThree).isEqual(result2)) {
        fail("concat 2 failed");
      }

      if (!oneTwoThree.concat(vf.list())
          .isEqual(oneTwoThree)) {
        fail("concat with empty set should produce same set");
      }

    } catch (FactTypeUseException e) {
      fail("the above should all be type safe");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testIterator(IValueFactory vf) {
    try {
      Iterator<IValue> it = integerListRelation(vf).iterator();

      int i;
      for (i = 0; it.hasNext(); i++) {
        ITuple t = (ITuple) it.next();

        if (!integerListRelation(vf).contains(t)) {
          fail("iterator produces strange elements?");
        }
      }

      if (i != integerListRelation(vf).length()) {
        fail("iterator skipped elements");
      }
    } catch (FactTypeUseException e) {
      fail("the above should be type correct");
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testCarrier(IValueFactory vf, TypeFactory tf) {
    IList carrier = integerListRelation(vf).asRelation().carrier();

    if (!carrier.isEqual(listOfIntegers(vf))) {
      fail("carrier should be equal to this set");
    }

    try {
      ITuple t1 = vf.tuple(integers(vf)[0], doubles(vf)[0]);
      ITuple t2 = vf.tuple(integers(vf)[1], doubles(vf)[1]);
      ITuple t3 = vf.tuple(integers(vf)[2], doubles(vf)[2]);
      IList rel1 = vf.list(t1, t2, t3);

      IList carrier1 = rel1.asRelation().carrier();

      if (carrier1.getElementType() != tf.numberType()) {
        fail("expected number type on carrier");
      }

      if (carrier1.length() != 6) {
        fail("carrier does not contain all elements");
      }

      if (carrier1.intersect(listOfIntegers(vf)).length() != 3) {
        fail("integers should be in there still");
      }

      if (carrier1.intersect(listOfDoubles(vf)).length() != 3) {
        fail("doubles should be in there still");
      }
    } catch (FactTypeUseException e) {
      fail("the above should be type correct");
    }

  }
}
