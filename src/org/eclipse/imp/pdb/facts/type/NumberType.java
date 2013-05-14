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

/**
 * A type for values that are either ints or reals
 */
/* package */class NumberType extends DefaultSubtypeOfValue {
  protected final static NumberType sInstance = new NumberType();

  protected NumberType() {
    super();
  }

  public static NumberType getInstance() {
    return sInstance;
  }

  @Override
  public String toString() {
    return "num";
  }

  @Override
  protected boolean isSupertypeOf(Type type) {
    return type.isSubtypeOfNumber(this);
  }

  @Override
  public Type lub(Type other) {
    return other.lubWithNumber(this);
  }

  @Override
  protected boolean isSubtypeOfNumber(Type type) {
    return true;
  }

  @Override
  protected Type lubWithNumber(Type type) {
    return type;
  }

  @Override
  protected Type lubWithInteger(Type type) {
    return NumberType.getInstance();
  }

  @Override
  protected Type lubWithReal(Type type) {
    return NumberType.getInstance();
  }

  @Override
  protected Type lubWithRational(Type type) {
    return NumberType.getInstance();
  }
  
  @Override
  public Type glb(Type type) {
    return type.glbWithNumber(this);
  }
  
  @Override
  protected Type glbWithNumber(Type type) {
    return this;
  }

  @Override
  protected Type glbWithInteger(Type type) {
    return type;
  }
  
  @Override
  protected Type glbWithReal(Type type) {
    return type;
  }
  
  @Override
  protected Type glbWithRational(Type type) {
    return type;
  }
  
  /**
   * Should never be called, NodeType is a singleton
   */
  @Override
  public boolean equals(Object o) {
    return o == NumberType.getInstance();
  }

  @Override
  public int hashCode() {
    return 133020331;
  }

  @Override
  public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
    return visitor.visitNumber(this);
  }
}
