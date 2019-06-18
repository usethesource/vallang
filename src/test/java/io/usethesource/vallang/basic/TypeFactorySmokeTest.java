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

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.Setup;
import io.usethesource.vallang.exceptions.FactTypeDeclarationException;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

@RunWith(Parameterized.class)
public final class TypeFactorySmokeTest {

  @Parameterized.Parameters
  public static Iterable<? extends Object> data() {
    return Setup.valueFactories();
  }

  private final IValueFactory vf;

  public TypeFactorySmokeTest(IValueFactory vf) {
    this.vf = vf;
  }

  private TypeFactory ft = TypeFactory.getInstance();

  private Type[] types = new Type[] {ft.integerType(), ft.realType(), ft.sourceLocationType(),
      ft.valueType(), ft.listType(ft.integerType()), ft.setType(ft.realType())};

  @Test
  public void testGetInstance() {
    if (TypeFactory.getInstance() != ft) {
      fail("getInstance did not return the same reference");
    }
  }

  @Test
  public void testGetTypeByDescriptor() {
    // TODO: needs to be tested, after we've implemented it
  }

  @Test
  public void testValueType() {
    if (ft.valueType() != ft.valueType()) {
      fail("valueType should be canonical");
    }
  }

  @Test
  public void testIntegerType() {
    if (ft.integerType() != ft.integerType()) {
      fail("integerType should be canonical");
    }
  }

  @Test
  public void testDoubleType() {
    if (ft.realType() != ft.realType()) {
      fail("doubleType should be canonical");
    }
  }

  @Test
  public void testStringType() {
    if (ft.stringType() != ft.stringType()) {
      fail("stringType should be canonical");
    }
  }

  @Test
  public void testSourceLocationType() {
    if (ft.sourceLocationType() != ft.sourceLocationType()) {
      fail("sourceLocationType should be canonical");
    }
  }

  @Test
  public void testTupleTypeOfType() {
    Type t = ft.tupleType(types[0]);

    if (t != ft.tupleType(types[0])) {
      fail("tuple types should be canonical");
    }

    checkTupleTypeOf(t, 1);
  }

  @Test
  public void testTupleTypeOfTypeType() {
    Type t = ft.tupleType(types[0], types[1]);

    if (t != ft.tupleType(types[0], types[1])) {
      fail("tuple types should be canonical");
    }

    checkTupleTypeOf(t, 2);
  }

  @Test
  public void testTupleTypeOfTypeTypeType() {
    Type t = ft.tupleType(types[0], types[1], types[2]);

    if (t != ft.tupleType(types[0], types[1], types[2])) {
      fail("tuple types should be canonical");
    }

    checkTupleTypeOf(t, 3);
  }

  @Test
  public void testTupleTypeOfTypeTypeTypeType() {
    Type t = ft.tupleType(types[0], types[1], types[2], types[3]);

    if (t != ft.tupleType(types[0], types[1], types[2], types[3])) {
      fail("tuple types should be canonical");
    }

    checkTupleTypeOf(t, 4);
  }

  @Test
  public void testTupleTypeOfTypeTypeTypeTypeType() {
    Type t = ft.tupleType(types[0], types[1], types[2], types[3], types[4]);

    if (t != ft.tupleType(types[0], types[1], types[2], types[3], types[4])) {
      fail("tuple types should be canonical");
    }

    checkTupleTypeOf(t, 5);
  }

  @Test
  public void testTupleTypeOfTypeTypeTypeTypeTypeType() {
    Type t = ft.tupleType(types[0], types[1], types[2], types[3], types[4], types[5]);

    if (t != ft.tupleType(types[0], types[1], types[2], types[3], types[4], types[5])) {
      fail("tuple types should be canonical");
    }

    checkTupleTypeOf(t, 6);
  }

  @Test
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

  @Test
  public void testTupleTypeOfIValueArray() {
    // a and b shadow the 'types' field
      try {
          @SuppressWarnings("deprecation")
          IValue[] a = new IValue[] {vf.integer(1), vf.real(1.0),
                  vf.sourceLocation(new URI("file://bla"), 0, 0, 0, 0, 0, 0)};
          @SuppressWarnings("deprecation")
          IValue[] b = new IValue[] {vf.integer(1), vf.real(1.0),
                  vf.sourceLocation(new URI("file://bla"), 0, 0, 0, 0, 0, 0)};
          Type t = ft.tupleType(a);

          if (t != ft.tupleType(b)) {
        fail("tuples should be canonical");
      }

      checkTupleTypeOf(t, 3);
    } catch (URISyntaxException e) {
      fail(e.toString());
    }
  }

  @Test
  public void testSetTypeOf() {
    Type type = ft.setType(ft.integerType());

    if (type != ft.setType(ft.integerType())) {
      fail("set should be canonical");
    }
  }

  @Test
  public void testRelTypeType() {
    try {
      TypeStore store = new TypeStore();
      Type namedType =
          ft.aliasType(store, "myTuple", ft.tupleType(ft.integerType(), ft.integerType()));
      // note that the declared type of namedType needs to be Type
      Type type = ft.relTypeFromTuple(namedType);

      Type namedType2 =
          ft.aliasType(store, "myTuple", ft.tupleType(ft.integerType(), ft.integerType()));

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

  @Test
  public void testListRelTypeType() {
    try {
      TypeStore store = new TypeStore();
      Type namedType =
          ft.aliasType(store, "myTuple", ft.tupleType(ft.integerType(), ft.integerType()));
      // note that the declared type of namedType needs to be Type
      Type type = ft.lrelTypeFromTuple(namedType);

      Type namedType2 =
          ft.aliasType(store, "myTuple", ft.tupleType(ft.integerType(), ft.integerType()));

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

  @Test
  public void testRelTypeNamedType() {
    try {
      TypeStore store = new TypeStore();
      Type namedType =
          ft.aliasType(store, "myTuple", ft.tupleType(ft.integerType(), ft.integerType()));
      // note that the declared type of namedType needs to be AliasType
      Type type = ft.relTypeFromTuple(namedType);

      Type namedType2 =
          ft.aliasType(store, "myTuple", ft.tupleType(ft.integerType(), ft.integerType()));

      if (type != ft.relTypeFromTuple(namedType2)) {
        fail("relation types should be canonical");
      }
    } catch (FactTypeUseException e) {
      fail("type error for correct relation");
    }
  }

  @Test
  public void testListRelTypeNamedType() {
    try {
      TypeStore store = new TypeStore();
      Type namedType =
          ft.aliasType(store, "myTuple", ft.tupleType(ft.integerType(), ft.integerType()));
      // note that the declared type of namedType needs to be AliasType
      Type type = ft.lrelTypeFromTuple(namedType);

      Type namedType2 =
          ft.aliasType(store, "myTuple", ft.tupleType(ft.integerType(), ft.integerType()));

      if (type != ft.lrelTypeFromTuple(namedType2)) {
        fail("list relation types should be canonical");
      }
    } catch (FactTypeUseException e) {
      fail("type error for correct list relation");
    }
  }

  @Test
  public void testRelTypeTupleType() {
    Type tupleType = ft.tupleType(ft.integerType(), ft.integerType());
    // note that the declared type of tupleType needs to be TupleType
    Type type = ft.relTypeFromTuple(tupleType);

    Type tupleType2 = ft.tupleType(ft.integerType(), ft.integerType());

    if (type != ft.relTypeFromTuple(tupleType2)) {
      fail("relation types should be canonical");
    }
  }

  @Test
  public void testListRelTypeTupleType() {
    Type tupleType = ft.tupleType(ft.integerType(), ft.integerType());
    // note that the declared type of tupleType needs to be TupleType
    Type type = ft.lrelTypeFromTuple(tupleType);

    Type tupleType2 = ft.tupleType(ft.integerType(), ft.integerType());

    if (type != ft.lrelTypeFromTuple(tupleType2)) {
      fail("list relation types should be canonical");
    }
  }

  @Test
  public void testRelTypeOfType() {
    Type type = ft.relType(types[0]);

    if (type != ft.relType(types[0])) {
      fail("relation types should be canonical");
    }

    checkRelationTypeOf(type, 1);
  }

  @Test
  public void testRelTypeOfTypeType() {
    Type type = ft.relType(types[0], types[1]);

    if (type != ft.relType(types[0], types[1])) {
      fail("relation types should be canonical");
    }

    checkRelationTypeOf(type, 2);
  }

  @Test
  public void testRelTypeOfTypeTypeType() {
    Type type = ft.relType(types[0], types[1], types[2]);

    if (type != ft.relType(types[0], types[1], types[2])) {
      fail("relation types should be canonical");
    }

    checkRelationTypeOf(type, 3);
  }

  @Test
  public void testRelTypeOfTypeTypeTypeType() {
    Type type = ft.relType(types[0], types[1], types[2], types[3]);

    if (type != ft.relType(types[0], types[1], types[2], types[3])) {
      fail("relation types should be canonical");
    }
    checkRelationTypeOf(type, 4);
  }

  @Test
  public void testRelTypeOfTypeTypeTypeTypeType() {
    Type type = ft.relType(types[0], types[1], types[2], types[3], types[4]);

    if (type != ft.relType(types[0], types[1], types[2], types[3], types[4])) {
      fail("relation types should be canonical");
    }
    checkRelationTypeOf(type, 5);
  }

  @Test
  public void testRelTypeOfTypeTypeTypeTypeTypeType() {
    Type type = ft.relType(types[0], types[1], types[2], types[3], types[4], types[5]);

    if (type != ft.relType(types[0], types[1], types[2], types[3], types[4], types[5])) {
      fail("relation types should be canonical");
    }
    checkRelationTypeOf(type, 6);
  }

  @Test
  public void testRelTypeOfTypeTypeTypeTypeTypeTypeType() {
    Type type = ft.relType(types[0], types[1], types[2], types[3], types[4], types[5]);

    if (type != ft.relType(types[0], types[1], types[2], types[3], types[4], types[5])) {
      fail("relation types should be canonical");
    }
    checkRelationTypeOf(type, 6);
  }

  @Test
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

  @Test
  public void testListType() {
    Type t1 = ft.listType(ft.integerType());
    Type t2 = ft.listType(ft.integerType());

    if (t1 != t2) {
      fail("named types should be canonical");
    }
  }
}
