/*******************************************************************************
 * Copyright (c) 2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package io.usethesource.vallang.impl;

import io.usethesource.vallang.IRelation;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.exceptions.IllegalOperationException;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;

public abstract class AbstractSet extends AbstractValue implements ISet {

  public AbstractSet() {
    super();
  }

  @Override
  public ISet empty() {
      return (ISet) writer().done();
  }
  
  protected static TypeFactory getTypeFactory() {
    return TypeFactory.getInstance();
  }

  protected static Type inferSetOrRelType(final Type elementType, final Iterable<IValue> content) {
    return inferSetOrRelType(elementType, content.iterator().hasNext() == false);
  }

  /*
   * TODO: get rid of code duplication (@see AbstractList.inferListOrRelType)
   */
  protected static Type inferSetOrRelType(final Type elementType, final boolean isEmpty) {
    final Type inferredElementType;
    final Type inferredCollectionType;

    // is collection empty?
    if (isEmpty) {
      inferredElementType = getTypeFactory().voidType();
    } else {
      inferredElementType = elementType;
    }

    // consists collection out of tuples?
    if (inferredElementType.isFixedWidth()) {
      inferredCollectionType = getTypeFactory().relTypeFromTuple(inferredElementType);
    } else {
      inferredCollectionType = getTypeFactory().setType(inferredElementType);
    }

    return inferredCollectionType;
  }

  protected abstract IValueFactory getValueFactory();

  @Override
  public Type getElementType() {
    return getType().getElementType();
  }

  protected static final void validateIsRelation(ISet set) {
    if (!set.isRelation()) {
      throw new IllegalOperationException("Cannot be viewed as a relation.", set.getType());
    }
  }

  @Override
  public IRelation<ISet> asRelation() {
    validateIsRelation(this);
    return new DefaultRelationViewOnSet(this);
  }
  
  @Override
  public boolean match(IValue other) {
      return ISet.super.match(other);
  }
}
