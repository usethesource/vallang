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
/* package */class NumberType extends ValueType {
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
    return this;
  }

  @Override
  protected Type lubWithInteger(Type type) {
    return this;
  }

  @Override
  protected Type lubWithReal(Type type) {
    return this;
  }

  @Override
  protected Type lubWithRational(Type type) {
    return this;
  }

  /**
   * Should never be called, NodeType is a singleton
   */
  @Override
  public boolean equals(Object o) {
    return (o instanceof NumberType);
  }

  @Override
  public int hashCode() {
    return 133020331;
  }

  @Override
  public <T> T accept(ITypeVisitor<T> visitor) {
    return visitor.visitNumber(this);
  }
}
