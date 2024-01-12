/*******************************************************************************
 * Copyright (c) 2013-2014 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package io.usethesource.vallang.impl.persistent;

import static io.usethesource.vallang.impl.persistent.SetWriter.asInstanceOf;
import static io.usethesource.vallang.impl.persistent.SetWriter.isTupleOfArityTwo;
import static io.usethesource.vallang.impl.persistent.ValueCollectors.toSet;
import static io.usethesource.vallang.impl.persistent.ValueCollectors.toSetMultimap;

import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IRelation;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;

public final class EmptySet implements ISet {
  private static final Type EMPTY_SET_TYPE = TypeFactory.getInstance().setType(TypeFactory.getInstance().voidType());
  public static final EmptySet EMPTY_SET = new EmptySet();

  private EmptySet() {}

  public static final ISet of() {
    return EMPTY_SET;
  }

  @Override
  public String toString() {
      return defaultToString();
  }

  public static final ISet of(final IValue firstElement) {
    final Type firstElementType = firstElement.getType();

    if (isTupleOfArityTwo.test(firstElementType)) {
      return Stream.of(firstElement).map(asInstanceOf(ITuple.class))
          .collect(toSetMultimap(tuple -> tuple.get(0), tuple -> tuple.get(1)));
    } else {
      return Stream.of(firstElement).collect(toSet());
    }
  }

  @Override
  public ISetWriter writer() {
      return ValueFactory.getInstance().setWriter();
  }

  @Override
  public Type getType() {
    return EMPTY_SET_TYPE;
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public ISet insert(IValue value) {
    // TODO: move smart constructor
    return of(value);
  }

  @Override
  public ISet delete(IValue value) {
    return this;
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public boolean contains(IValue value) {
    return false;
  }

  @Override
  public Iterator<IValue> iterator() {
    return Collections.emptyIterator();
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other == this;
  }

  @Override
  public boolean match(IValue other) {
    return other == this;
  }

  @Override
  public ISet union(ISet other) {
    return other;
  }

  @Override
  public ISet intersect(ISet other) {
    return this;
  }

  @Override
  public ISet subtract(ISet other) {
    return this;
  }

  @Override
  public ISet product(ISet that) {
    return this;
  }

  @Override
  public boolean isSubsetOf(ISet other) {
    return true;
  }
  
  @Override
  public IRelation<ISet> asRelation() {
      return new IRelation<ISet>() {
        @Override
        public ISet asContainer() {
            return EmptySet.this;
        }
        
        @Override
        public ISet compose(IRelation<ISet> that) {
            return EmptySet.this;
        }
        
        @Override
        public ISet closure(boolean forceDepthFirst) {
            return EmptySet.this;
        }
        
        @Override
        public ISet closureStar(boolean forceDepthFirst) {
            return EmptySet.this;
        }
          
        @Override
        public ISet carrier() {
            return EmptySet.this;
        }
        
        @Override
        public ISet domain() {
            return EmptySet.this;
        }
        
        @Override
        public ISet range() {
            return EmptySet.this;
        }
        
        @Override
        public ISet empty() {
            return EmptySet.this;
        }
        
        @Override
        public ISet index(IValue key) {
            return EmptySet.this;
        }
      };
  }

}
