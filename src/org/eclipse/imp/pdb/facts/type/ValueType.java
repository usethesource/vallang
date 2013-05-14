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

package org.eclipse.imp.pdb.facts.type;

/* package */class ValueType extends Type {
  protected static class InstanceHolder {
    public static final ValueType sInstance = new ValueType();
  }

  public static ValueType getInstance() {
    return InstanceHolder.sInstance;
  }

  protected ValueType() {
    super();
  }

  @Override
  public String toString() {
    return "value";
  }

  /**
   * Should never be called, ValueType is a singleton
   */
  @Override
  public boolean equals(Object o) {
    return o == ValueType.getInstance();
  }

  @Override
  public int hashCode() {
    return 2141;
  }

  @Override
  public <T, E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
    return visitor.visitValue(this);
  }

  @Override
  public Type lub(Type other) {
    return other.lubWithValue(this);
  }

  @Override
  public Type glb(Type type) {
    return type.glbWithValue(this);
  }
  
  @Override
  protected boolean isSupertypeOf(Type type) {
    return type.isSubtypeOfValue(this);
  }

  @Override
  protected boolean isSubtypeOfValue(Type type) {
    return true;
  }

  @Override
  protected boolean isSubtypeOfReal(Type type) {
    return false;
  }

  @Override
  protected boolean isSubtypeOfInteger(Type type) {
    return false;
  }

  @Override
  protected boolean isSubtypeOfRational(Type type) {
    return false;
  }

  @Override
  protected boolean isSubtypeOfList(Type type) {
    return false;
  }

  @Override
  protected boolean isSubtypeOfMap(Type type) {
    return false;
  }

  @Override
  protected boolean isSubtypeOfNumber(Type type) {
    return false;
  }

  @Override
  protected boolean isSubtypeOfRelation(Type type) {
    return false;
  }

  @Override
  protected boolean isSubtypeOfListRelation(Type type) {
    return false;
  }

  @Override
  protected boolean isSubtypeOfSet(Type type) {
    return false;
  }

  @Override
  protected boolean isSubtypeOfSourceLocation(Type type) {
    return false;
  }

  @Override
  protected boolean isSubtypeOfString(Type type) {
    return false;
  }

  @Override
  protected boolean isSubtypeOfNode(Type type) {
    return false;
  }

  @Override
  protected boolean isSubtypeOfConstructor(Type type) {
    return false;
  }

  @Override
  protected boolean isSubtypeOfAbstractData(Type type) {
    return false;
  }

  @Override
  protected boolean isSubtypeOfTuple(Type type) {
    return false;
  }

  @Override
  protected boolean isSubtypeOfVoid(Type type) {
    return false;
  }

  @Override
  protected boolean isSubtypeOfBool(Type type) {
    return false;
  }

  @Override
  protected boolean isSubtypeOfExternal(Type type) {
    return false;
  }

  @Override
  protected boolean isSubtypeOfDateTime(Type type) {
    return false;
  }

  @Override
  protected Type lubWithValue(Type type) {
    return ValueType.getInstance();
  }
  
  protected Type lubWithReal(Type type) {
    return ValueType.getInstance();
  }
  
  protected Type lubWithInteger(Type type) {
    return ValueType.getInstance();
  }

  protected Type lubWithRational(Type type) {
    return ValueType.getInstance();
  }

  protected Type lubWithList(Type type) {
    return ValueType.getInstance();
  }

  protected Type lubWithMap(Type type) {
    return ValueType.getInstance();
  }

  protected Type lubWithNumber(Type type) {
    return ValueType.getInstance();
  }

  protected Type lubWithSet(Type type) {
    return ValueType.getInstance();
  }

  protected Type lubWithSourceLocation(Type type) {
    return ValueType.getInstance();
  }

  protected Type lubWithString(Type type) {
    return ValueType.getInstance();
  }

  protected Type lubWithNode(Type type) {
    return ValueType.getInstance();
  }

  protected Type lubWithConstructor(Type type) {
    return ValueType.getInstance();
  }

  protected Type lubWithAbstractData(Type type) {
    return ValueType.getInstance();
  }

  protected Type lubWithTuple(Type type) {
    return ValueType.getInstance();
  }

  protected Type lubWithVoid(Type type) {
    /* this is for the semantics of the sub-classes of ValueType
     * the lub with void for any type should be that other type
     * and not value.
     */
    return this;
  }

  protected Type lubWithBool(Type type) {
    return ValueType.getInstance();
  }

  protected Type lubWithDateTime(Type type) {
    return ValueType.getInstance();
  }
  
  @Override
  protected Type glbWithValue(Type type) {
    return this; // such that sub-classes do not have to override
  }
  
  protected Type glbWithReal(Type type) {
    return type;
  }
  
  protected Type glbWithInteger(Type type) {
    return type;
  }

  protected Type glbWithRational(Type type) {
    return type;
  }

  protected Type glbWithList(Type type) {
    return type;
  }

  protected Type glbWithMap(Type type) {
    return type;
  }

  protected Type glbWithNumber(Type type) {
    return type;
  }

  protected Type glbWithRelation(Type type) {
    return type;
  }

  protected Type glbWithListRelation(Type type) {
    return type;
  }

  protected Type glbWithSet(Type type) {
    return type;
  }

  protected Type glbWithSourceLocation(Type type) {
    return type;
  }

  protected Type glbWithString(Type type) {
    return type;
  }

  protected Type glbWithNode(Type type) {
    return type;
  }

  protected Type glbWithConstructor(Type type) {
    return type;
  }

  protected Type glbWithAbstractData(Type type) {
    return type;
  }

  protected Type glbWithTuple(Type type) {
    return type;
  }

  protected Type glbWithVoid(Type type) {
    return type;
  }

  protected Type glbWithBool(Type type) {
    return type;
  }

  protected Type glbWithDateTime(Type type) {
    return type;
  }
}
