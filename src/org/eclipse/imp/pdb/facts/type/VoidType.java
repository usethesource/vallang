/*******************************************************************************
 * Copyright (c) 2008 CWI.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jurgen Vinju - jurgen@vinju.org

 *******************************************************************************/

package org.eclipse.imp.pdb.facts.type;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.imp.pdb.facts.exceptions.IllegalOperationException;

/**
 * The void type represents an empty collection of values. I.e. it is a subtype
 * of all types, the bottom of the type hierarchy.
 * 
 * This type does not have any values with it naturally and can, for example, be
 * used to elegantly initialize computations that involve least upper bounds.
 */
/* package */final class VoidType extends Type {
  private static final class InstanceKeeper {
    public final static VoidType sInstance = new VoidType();
  }

  public static VoidType getInstance() {
    return InstanceKeeper.sInstance;
  }

  private VoidType() {
    super();
  }

  @Override
  public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
    return visitor.visitVoid(this);
  }

  @Override
  protected boolean isSupertypeOf(Type type) {
    return type.isSubtypeOfVoid(this);
  }

  @Override
  public Type lub(Type other) {
    return other.lubWithVoid(this);
  }
  
  @Override
  public Type closure() {
    return this;
  }

  @Override
  public boolean isFixedWidth() {
	  return true;
  }

  @Override
  protected boolean isSubtypeOfAbstractData(Type type) {
    return true;
  }

  @Override
  protected boolean isSubtypeOfBool(Type type) {
    return true;
  }

  @Override
  protected boolean isSubtypeOfConstructor(Type type) {
    return true;
  }

  @Override
  protected boolean isSubtypeOfDateTime(Type type) {
    return true;
  }

  @Override
  protected boolean isSubtypeOfExternal(Type type) {
    return true;
  }

  @Override
  protected boolean isSubtypeOfInteger(Type type) {
    return true;
  }

  @Override
  protected boolean isSubtypeOfList(Type type) {
    return true;
  }

  @Override
  protected boolean isSubtypeOfListRelation(Type type) {
    return true;
  }

  @Override
  protected boolean isSubtypeOfMap(Type type) {
    return true;
  }

  @Override
  protected boolean isSubtypeOfNode(Type type) {
    return true;
  }

  @Override
  protected boolean isSubtypeOfNumber(Type type) {
    return true;
  }

  @Override
  protected boolean isSubtypeOfRational(Type type) {
    return true;
  }

  @Override
  protected boolean isSubtypeOfReal(Type type) {
    return true;
  }

  @Override
  protected boolean isSubtypeOfRelation(Type type) {
    return true;
  }

  @Override
  protected boolean isSubtypeOfSet(Type type) {
    return true;
  }

  @Override
  protected boolean isSubtypeOfSourceLocation(Type type) {
    return true;
  }

  @Override
  protected boolean isSubtypeOfString(Type type) {
    return true;
  }

  @Override
  protected boolean isSubtypeOfTuple(Type type) {
    return true;
  }

  @Override
  protected boolean isSubtypeOfVoid(Type type) {
    return true;
  }

  @Override
  protected boolean isSubtypeOfValue(Type type) {
    return true;
  }

  @Override
  protected Type lubWithAbstractData(Type type) {
    return type;
  }

  @Override
  protected Type lubWithBool(Type type) {
    return type;
  }

  @Override
  protected Type lubWithConstructor(Type type) {
    return type;
  }

  @Override
  protected Type lubWithDateTime(Type type) {
    return type;
  }

  @Override
  protected Type lubWithExternal(Type type) {
    return type;
  }

  @Override
  protected Type lubWithInteger(Type type) {
    return type;
  }

  @Override
  protected Type lubWithList(Type type) {
    return type;
  }

  @Override
  protected Type lubWithMap(Type type) {
    return type;
  }

  @Override
  protected Type lubWithNode(Type type) {
    return type;
  }

  @Override
  protected Type lubWithNumber(Type type) {
    return type;
  }

  @Override
  protected Type lubWithRational(Type type) {
    return type;
  }

  @Override
  protected Type lubWithReal(Type type) {
    return type;
  }

  @Override
  protected Type lubWithSet(Type type) {
    return type;
  }

  @Override
  protected Type lubWithSourceLocation(Type type) {
    return type;
  }

  @Override
  protected Type lubWithString(Type type) {
    return type;
  }

  @Override
  protected Type lubWithTuple(Type type) {
    return type;
  }

  @Override
  protected Type lubWithValue(Type type) {
    return type;
  }

  @Override
  protected Type lubWithVoid(Type type) {
    return type;
  }

  @Override
  public String toString() {
    return "void";
  }

  @Override
  public boolean equals(Object obj) {
    return obj == VoidType.getInstance();
  }

  @Override
  public int hashCode() {
    return 199;
  }

  @Override
  public int getArity() {
    return 0;
  }

  @Override
  public Type getAliased() {
    return this;
  }

  @Override
  public Type getElementType() {
    return this;
  }

  @Override
  public int getFieldIndex(String fieldName) {
    throw new IllegalOperationException("getFieldIndex", this);
  }

  @Override
  public boolean hasFieldNames() {
    return false;
  }

  @Override
  public boolean hasField(String fieldName) {
	  return false;
  }
  
  @Override
  public boolean hasField(String fieldName, TypeStore store) {
	  return false;
  }
  
  @Override
  public String getFieldName(int i) {
    return null;
  }
  
  @Override
	public String[] getFieldNames() {
	  return new String[0];
	}

  @Override
  public String getKeyLabel() {
    return null;
  }

  @Override
  public String getValueLabel() {
    return null;
  }

  @Override
  public Type select(int... fields) {
    return this;
  }

  @Override
  public Type select(String... names) {
    return this;
  }

  @Override
  public Type getAbstractDataType() {
    return this;
  }

  @Override
  public Type getBound() {
    return this;
  }

  @Override
  public Type getTypeParameters() {
    return this;
  }

  @Override
  public Type getFieldType(int i) {
    return this;
  }

  @Override
  public Type getFieldType(String fieldName) {
    return this;
  }

  @Override
  public Type getFieldTypes() {
    return this;
  }

  @Override
  public Type getKeyType() {
    return this;
  }

  @Override
  public Type getValueType() {
    return this;
  }

  @Override
  public Type compose(Type other) {
    return this;
  }

  @Override
  public Type carrier() {
    return this;
  }

  @Override
  public String getName() {
    return "";
  }

  @Override
  public Type instantiate(Map<Type, Type> bindings) {
    return this;
  }

  @Override
  public Iterator<Type> iterator() {
    return new Iterator<Type>() {
      boolean once = false;

      public boolean hasNext() {
        return !once;
      }

      public Type next() {
        once = true;
        return VoidType.this;
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
  public Type glb(Type type) {
    return type.glbWithVoid(this);
  }

  @Override
  protected Type glbWithReal(Type type) {
    return this;
  }

  @Override
  protected Type glbWithInteger(Type type) {
    return this;
  }

  @Override
  protected Type glbWithRational(Type type) {
    return this;
  }

  @Override
  protected Type glbWithList(Type type) {
    return this;
  }

  @Override
  protected Type glbWithMap(Type type) {
    return this;
  }

  @Override
  protected Type glbWithNumber(Type type) {
    return this;
  }

  @Override
  protected Type glbWithSet(Type type) {
    return this;
  }

  @Override
  protected Type glbWithSourceLocation(Type type) {
    return this;
  }

  @Override
  protected Type glbWithString(Type type) {
    return this;
  }

  @Override
  protected Type glbWithNode(Type type) {
    return this;
  }

  @Override
  protected Type glbWithConstructor(Type type) {
    return this;
  }

  @Override
  protected Type glbWithAbstractData(Type type) {
    return this;
  }

  @Override
  protected Type glbWithTuple(Type type) {
    return this;
  }

  @Override
  protected Type glbWithValue(Type type) {
    return this;
  }

  @Override
  protected Type glbWithVoid(Type type) {
    return this;
  }

  @Override
  protected Type glbWithBool(Type type) {
    return this;
  }

  @Override
  protected Type glbWithDateTime(Type type) {
    return this;
  }
}
