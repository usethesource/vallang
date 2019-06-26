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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IMapWriter;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

public final class EqualitySmokeTest {

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testInteger(IValueFactory vf) {
    assertTrue(vf.integer(0).isEqual(vf.integer(0)));
    assertFalse(vf.integer(0).isEqual(vf.integer(1)));
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testDouble(IValueFactory vf) {
    assertTrue(vf.real(0.0).isEqual(vf.real(0.0)));
    assertTrue(vf.real(1.0).isEqual(vf.real(1.00000)));
    assertFalse(vf.real(0.0).isEqual(vf.real(1.0)));
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testString(IValueFactory vf) {
    assertTrue(vf.string("").isEqual(vf.string("")));
    assertTrue(vf.string("a").isEqual(vf.string("a")));
    assertFalse(vf.string("a").isEqual(vf.string("b")));
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testEmptyCollectionsAreVoid(IValueFactory vf, TypeFactory tf) {
    assertTrue(vf.list().getElementType().isSubtypeOf(tf.voidType()));
    assertTrue(vf.set().getElementType().isSubtypeOf(tf.voidType()));
    assertTrue(vf.map().getKeyType().isSubtypeOf(tf.voidType()));
    assertTrue(vf.map().getValueType().isSubtypeOf(tf.voidType()));

    assertTrue(vf.listWriter().done().getElementType().isSubtypeOf(tf.voidType()));
    assertTrue(vf.setWriter().done().getElementType().isSubtypeOf(tf.voidType()));
    assertTrue(vf.mapWriter().done().getKeyType().isSubtypeOf(tf.voidType()));
    assertTrue(vf.mapWriter().done().getValueType().isSubtypeOf(tf.voidType()));
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testList(IValueFactory vf) {
    assertTrue(vf.list().isEqual(vf.list()), "empty lists are always equal");

    assertTrue(vf.list(vf.integer(1)).isEqual(vf.list(vf.integer(1))));
    assertFalse(vf.list(vf.integer(1)).isEqual(vf.list(vf.integer(0))));

    assertTrue(vf.list(vf.list()).isEqual(vf.list(vf.list())));
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testSet(IValueFactory vf) {
    assertTrue(vf.set().isEqual(vf.set()), "empty sets are always equal");

    assertTrue(vf.set(vf.integer(1)).isEqual(vf.set(vf.integer(1))));
    assertFalse(vf.set(vf.integer(1)).isEqual(vf.set(vf.integer(0))));

    assertTrue(vf.set(vf.set()).isEqual(vf.set(vf.set())));
  }

  /**
   * Documenting the current relationship between Node and Constructor in terms of equality and hash
   * codes.
   */
  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testConstructorIsEqualToConstructor(IValueFactory vf, TypeFactory tf) {
    final INode n = vf.node("constructorComparableName", vf.integer(1), vf.integer(2));

    final TypeStore ts = new TypeStore();
    final Type adtType = tf.abstractDataType(ts, "adtTypeNameThatIsIgnored");
    final Type constructorType = tf.constructor(ts, adtType, "constructorComparableName",
        tf.integerType(), tf.integerType());

    final IConstructor c = vf.constructor(constructorType, vf.integer(1), vf.integer(2));

    // they are not the same
    assertFalse(n.equals(c));
    assertFalse(c.equals(n));
    /*
     * TODO: what is the general contract between isEqual() and hashCode()?
     */
    assertFalse(n.hashCode() == c.hashCode());

    // unidirectional: n -> c = false
    assertFalse(n.isEqual(c));

    // unidirectional: c -> n = false
    assertFalse(c.isEqual(n));
  }
  
  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testNodeMatch(IValueFactory vf) {
      final INode n = vf.node("hello");
      final INode m = n.asWithKeywordParameters().setParameter("x", vf.integer(0));
      
      assertFalse(n.equals(m));
      assertFalse(m.equals(n));
      assertTrue(n.match(m));
      assertTrue(m.match(n));
      assertFalse(m.isEqual(n));
      assertFalse(n.isEqual(m));
      
      final INode a = vf.node("hello", vf.string("bye"));
      final INode b = a.asWithKeywordParameters().setParameter("x", vf.integer(0));
      
      assertFalse(a.equals(b));
      assertFalse(b.equals(a));
      assertTrue(a.match(b));
      assertTrue(b.match(a));
      assertTrue(a.match(a));
      assertTrue(b.match(b));
      assertFalse(b.isEqual(a));
      assertFalse(a.isEqual(b));
      
      assertTrue(vf.list(a).match(vf.list(b)));
      assertTrue(vf.list(b).match(vf.list(a)));
      assertTrue(vf.set(a).match(vf.set(b)));
      assertTrue(vf.set(b).match(vf.set(a)));
      assertTrue(vf.tuple(a).match(vf.tuple(b)));
      assertTrue(vf.tuple(b).match(vf.tuple(a)));
      
      final IMapWriter map1 = vf.mapWriter();
      final IMapWriter map2 = vf.mapWriter();
      map1.put(a, vf.integer(0));
      map2.put(b, vf.integer(0));
      assertTrue(map1.done().match(map2.done()));
  }
  
  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testConstructorMatch(IValueFactory vf, TypeFactory tf) {
      final TypeStore store = new TypeStore();
      final Type Hello = tf.abstractDataType(store, "Hello");
      final Type Cons = tf.constructor(store, Hello, "bye");
      store.declareKeywordParameter(Cons, "x", tf.integerType());
      
      final IConstructor n = vf.constructor(Cons);
      final IConstructor m = n.asWithKeywordParameters().setParameter("x", vf.integer(0));
      
      assertFalse(n.equals(m));
      assertFalse(m.equals(n));
      assertTrue(n.match(m));
      assertTrue(m.match(n));
      assertTrue(n.match(n));
      assertTrue(m.match(m));
      assertFalse(m.isEqual(n));
      assertFalse(n.isEqual(m));
      
      Type AR = tf.constructor(store, Hello, "aurevoir", tf.stringType(), "greeting");
      store.declareKeywordParameter(AR, "x", tf.integerType());
      
      final IConstructor a = vf.constructor(AR, vf.string("bye"));
      final IConstructor b = a.asWithKeywordParameters().setParameter("x", vf.integer(0));
      
      assertFalse(a.equals(b));
      assertFalse(b.equals(a));
      assertTrue(a.match(b));
      assertTrue(b.match(a));
      assertTrue(a.match(a));
      assertTrue(b.match(b));
      assertFalse(b.isEqual(a));
      assertFalse(a.isEqual(b));
      
      assertTrue(vf.list(a).match(vf.list(b)));
      assertTrue(vf.list(b).match(vf.list(a)));
      assertTrue(vf.set(a).match(vf.set(b)));
      assertTrue(vf.set(b).match(vf.set(a)));
      assertTrue(vf.tuple(a).match(vf.tuple(b)));
      assertTrue(vf.tuple(b).match(vf.tuple(a)));
      
      final IMapWriter map1 = vf.mapWriter();
      final IMapWriter map2 = vf.mapWriter();
      map1.put(a, vf.integer(0));
      map2.put(b, vf.integer(0));
      assertTrue(map1.done().match(map2.done()));
      
  }
}
