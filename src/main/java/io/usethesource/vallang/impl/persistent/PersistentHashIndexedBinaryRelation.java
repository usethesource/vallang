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

import static io.usethesource.vallang.impl.persistent.SetWriter.USE_MULTIMAP_BINARY_RELATIONS;
import static io.usethesource.vallang.impl.persistent.SetWriter.asInstanceOf;
import static io.usethesource.vallang.impl.persistent.SetWriter.isTupleOfArityTwo;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.Set.Immutable;
import io.usethesource.capsule.SetMultimap;
import io.usethesource.capsule.core.PersistentTrieSetMultimap;
import io.usethesource.capsule.util.ArrayUtilsInt;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import io.usethesource.vallang.IRelation;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.exceptions.IllegalOperationException;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.util.AbstractTypeBag;

public final class PersistentHashIndexedBinaryRelation implements ISet {

  private Type cachedRelationType;
  private final AbstractTypeBag keyTypeBag;
  private final AbstractTypeBag valTypeBag;
  private final SetMultimap.Immutable<IValue, IValue> content;

  /**
   * Construction of persistent indexed binary relation with multi-map backend.
   *
   * DO NOT CALL OUTSIDE OF {@link PersistentSetFactory}.
   *
   * @param keyTypeBag precise dynamic type of first data column
   * @param valTypeBag precise dynamic type of second data column
   * @param content immutable multi-map
   */
  PersistentHashIndexedBinaryRelation(AbstractTypeBag keyTypeBag, AbstractTypeBag valTypeBag,
      SetMultimap.Immutable<IValue, IValue> content) {
    this.keyTypeBag = Objects.requireNonNull(keyTypeBag);
    this.valTypeBag = Objects.requireNonNull(valTypeBag);
    this.content = Objects.requireNonNull(content);

    assert USE_MULTIMAP_BINARY_RELATIONS
        && isTupleOfArityTwo.test(TF.tupleType(keyTypeBag.lub(), valTypeBag.lub()));
    assert USE_MULTIMAP_BINARY_RELATIONS && !content.isEmpty();
    assert USE_MULTIMAP_BINARY_RELATIONS && checkDynamicType(keyTypeBag, valTypeBag, content);
  }

  private static final boolean checkDynamicType(final AbstractTypeBag keyTypeBag,
      final AbstractTypeBag valTypeBag, final SetMultimap.Immutable<IValue, IValue> content) {

    AbstractTypeBag expectedKeyTypeBag = content.entrySet().stream().map(Map.Entry::getKey)
        .map(IValue::getType).collect(AbstractTypeBag.toTypeBag());

    AbstractTypeBag expectedValTypeBag = content.entrySet().stream().map(Map.Entry::getValue)
        .map(IValue::getType).collect(AbstractTypeBag.toTypeBag());

    // the label is not on the stream of values and keys
    // so we have to set that back to the type bag,
    // else the equals that does compare the labels can fail.
    expectedKeyTypeBag = expectedKeyTypeBag.setLabel(keyTypeBag.getLabel());
    expectedValTypeBag = expectedValTypeBag.setLabel(valTypeBag.getLabel());

    boolean keyTypesEqual = expectedKeyTypeBag.equals(keyTypeBag);
    boolean valTypesEqual = expectedValTypeBag.equals(valTypeBag);

    return keyTypesEqual && valTypesEqual;
  }
  
  @Override
  public ISetWriter writer() {
      return ValueFactory.getInstance().setWriter();
  }

  @Override
  public Type getType() {
    if (cachedRelationType == null) {
      final String keyLabel = keyTypeBag.getLabel();
      final String valLabel = valTypeBag.getLabel();

      if (keyLabel != null && valLabel != null) {
        final Type tupleType = TF.tupleType(
            new Type[] {keyTypeBag.lub(), valTypeBag.lub()}, new String[] {keyLabel, valLabel});

        cachedRelationType = TF.relTypeFromTuple(tupleType);
      } else {
        cachedRelationType = TF.relType(keyTypeBag.lub(), valTypeBag.lub());
      }
    }
    return cachedRelationType;
  }

  private final <K extends IValue, V extends IValue> BiFunction<IValue, IValue, ITuple> tupleConverter() {
    return (first, second) -> Tuple.newTuple(first, second);
  }

  @Override
  public boolean isEmpty() {
    return content.isEmpty();
  }

  @Override
  public ISet insert(IValue value) {
    if (!isTupleOfArityTwo.test(value.getType())) {
      /*
       * NOTE: conversion of set representations are assumed to be scarce, but are costly if they
       * happen though.
       */
      final Stream<ITuple> tupleStream = content.tupleStream(tupleConverter());
      return Stream.concat(tupleStream, Stream.of(value)).collect(ValueCollectors.toSet());
    }

    final ITuple tuple = (ITuple) value;
    final IValue key = tuple.get(0);
    final IValue val = tuple.get(1);

    final SetMultimap.Immutable<IValue, IValue> contentNew = content.__insert(key, val);

    if (content == contentNew) {
      return this;
    }

    final AbstractTypeBag keyTypeBagNew = keyTypeBag.increase(key.getType());
    final AbstractTypeBag valTypeBagNew = valTypeBag.increase(val.getType());

    return PersistentSetFactory.from(keyTypeBagNew, valTypeBagNew, contentNew);
  }

  @Override
  public ISet delete(IValue value) {
    if (!isTupleOfArityTwo.test(value.getType())) {
      return this;
    }

    final ITuple tuple = (ITuple) value;
    final IValue key = tuple.get(0);
    final IValue val = tuple.get(1);

    final SetMultimap.Immutable<IValue, IValue> contentNew = content.__remove(key, val);

    if (content == contentNew) {
      return this;
    }

    final AbstractTypeBag keyTypeBagNew = keyTypeBag.decrease(key.getType());
    final AbstractTypeBag valTypeBagNew = valTypeBag.decrease(val.getType());

    return PersistentSetFactory.from(keyTypeBagNew, valTypeBagNew, contentNew);
  }

  @Override
  public int size() {
    return content.size();
  }

  @Override
  public boolean contains(IValue value) {
    if (!isTupleOfArityTwo.test(value.getType())) {
      return false;
    }

    final ITuple tuple = (ITuple) value;
    final IValue key = tuple.get(0);
    final IValue val = tuple.get(1);

    return content.containsEntry(key, val);
  }

  @Override
  public Iterator<IValue> iterator() {
    // TODO: make method co-variant
    return content.tupleIterator(Tuple::newTuple);
  }

  @Override
  public int hashCode() {
    final int hashCode =
        StreamSupport.stream(spliterator(), false).mapToInt(tuple -> tuple.hashCode()).sum();

    return hashCode;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (other == null) {
      return false;
    }

    if (other instanceof PersistentHashIndexedBinaryRelation) {
      PersistentHashIndexedBinaryRelation that = (PersistentHashIndexedBinaryRelation) other;

      if (this.getType() != that.getType()) {
        return false;
      }

      if (this.size() != that.size()) {
        return false;
      }

      return content.equals(that.content);
    }

    if (other instanceof ISet) {
      ISet that = (ISet) other;

      if (this.getType() != that.getType()) {
        return false;
      }

      if (this.size() != that.size()) {
        return false;
      }

      /**
       * TODO: simplify by adding stream support to {@link ISet}. Such that block below could be
       * simplified to
       *   {@code thatStream.allMatch(unboxTupleAndThen(content::containsEntry))}
       * with signature
       *   {@code Predicate<IValue> unboxTupleAndThen(BiFunction<IValue, IValue, Boolean> consumer)}
       */
      for (IValue value : that) {
        if (!isTupleOfArityTwo.test(value.getType())) {
          return false;
        }

        final ITuple tuple = (ITuple) value;
        final IValue key = tuple.get(0);
        final IValue val = tuple.get(1);

        if (!content.containsEntry(key, val)) {
          return false;
        }
      }

      return true;
    }

    return false;
  }

  @Override
  public boolean isEqual(IValue other) {
    if (other == this) {
      return true;
    }
    if (other == null) {
      return false;
    }

    if (other instanceof ISet) {
      ISet that = (ISet) other;

      if (this.size() != that.size()) {
        return false;
      }

      for (IValue value : that) {
        if (!isTupleOfArityTwo.test(value.getType())) {
          return false;
        }

        final ITuple tuple = (ITuple) value;
        final IValue key = tuple.get(0);
        final IValue val = tuple.get(1);

        /*
         * TODO: reconsider hiding of comparator vs exposition via argument
         *
         * TODO: containsEntry in isEquals does not use equivalence explicitly here
         * content.containsEntryEquivalent(key, val, equivalenceComparator);
         */
        if (!content.containsEntry(key, val)) {
          return false;
        }
      }

      return true;
    }

    return false;
  }
  
  @Override
  public ISet union(ISet other) {
    if (other == this) {
      return this;
    }
    if (other == null) {
      return this;
    }

    if (other instanceof PersistentHashIndexedBinaryRelation) {
      PersistentHashIndexedBinaryRelation that = (PersistentHashIndexedBinaryRelation) other;

      final SetMultimap.Immutable<IValue, IValue> one;
      final SetMultimap.Immutable<IValue, IValue> two;
      AbstractTypeBag keyTypeBagNew;
      AbstractTypeBag valTypeBagNew;
      final ISet def;

      if (that.size() >= this.size()) {
        def = that;
        one = that.content;
        keyTypeBagNew = that.keyTypeBag;
        valTypeBagNew = that.valTypeBag;
        two = this.content;
      } else {
        def = this;
        one = this.content;
        keyTypeBagNew = this.keyTypeBag;
        valTypeBagNew = this.valTypeBag;
        two = that.content;
      }

      final SetMultimap.Transient<IValue, IValue> tmp = one.asTransient();
      boolean modified = false;

      for (Map.Entry<IValue, IValue> entry : two.entrySet()) {
        final IValue key = entry.getKey();
        final IValue val = entry.getValue();

        if (tmp.__insert(key, val)) {
          modified = true;
          keyTypeBagNew = keyTypeBagNew.increase(key.getType());
          valTypeBagNew = valTypeBagNew.increase(val.getType());
        }
      }

      if (modified) {
        return PersistentSetFactory.from(keyTypeBagNew, valTypeBagNew, tmp.freeze());
      }
      return def;
    } else {
      return ISet.super.union(other);
    }
  }

  @Override
  public ISet intersect(ISet other) {
    if (other == this) {
      return this;
    }
    if (other == null) {
      return EmptySet.EMPTY_SET;
    }

    if (other instanceof PersistentHashIndexedBinaryRelation) {
      PersistentHashIndexedBinaryRelation that = (PersistentHashIndexedBinaryRelation) other;

      final SetMultimap.Immutable<IValue, IValue> one;
      final SetMultimap.Immutable<IValue, IValue> two;
      AbstractTypeBag keyTypeBagNew;
      AbstractTypeBag valTypeBagNew;
      final ISet def;

      if (that.size() >= this.size()) {
        def = this;
        one = this.content;
        keyTypeBagNew = this.keyTypeBag;
        valTypeBagNew = this.valTypeBag;
        two = that.content;
      } else {
        def = that;
        one = that.content;
        keyTypeBagNew = that.keyTypeBag;
        valTypeBagNew = that.valTypeBag;
        two = this.content;
      }

      final SetMultimap.Transient<IValue, IValue> tmp = one.asTransient();
      boolean modified = false;

      for (Iterator<Map.Entry<IValue, IValue>> it = tmp.entryIterator(); it.hasNext(); ) {
        final Map.Entry<IValue, IValue> tuple = it.next();
        final IValue key = tuple.getKey();
        final IValue val = tuple.getValue();

        if (!two.containsEntry(key, val)) {
          it.remove();
          modified = true;
          keyTypeBagNew = keyTypeBagNew.decrease(key.getType());
          valTypeBagNew = valTypeBagNew.decrease(val.getType());
        }
      }

      if (modified) {
        return PersistentSetFactory.from(keyTypeBagNew, valTypeBagNew, tmp.freeze());
      }
      return def;
    } else {
      return ISet.super.intersect(other);
    }
  }

  @Override
  public ISet subtract(ISet other) {
    if (other == this) {
      return EmptySet.EMPTY_SET;
    }
    if (other == null) {
      return this;
    }

    if (other instanceof PersistentHashIndexedBinaryRelation) {
      PersistentHashIndexedBinaryRelation that = (PersistentHashIndexedBinaryRelation) other;

      final SetMultimap.Immutable<IValue, IValue> one;
      final SetMultimap.Immutable<IValue, IValue> two;
      AbstractTypeBag keyTypeBagNew;
      AbstractTypeBag valTypeBagNew;
      final ISet def;

      def = this;
      one = this.content;
      keyTypeBagNew = this.keyTypeBag;
      valTypeBagNew = this.valTypeBag;
      two = that.content;

      final SetMultimap.Transient<IValue, IValue> tmp = one.asTransient();
      boolean modified = false;

      for (Map.Entry<IValue, IValue> tuple : two.entrySet()) {
        final IValue key = tuple.getKey();
        final IValue val = tuple.getValue();

        if (tmp.__remove(key, val)) {
          modified = true;
          keyTypeBagNew = keyTypeBagNew.decrease(key.getType());
          valTypeBagNew = valTypeBagNew.decrease(val.getType());
        }
      }

      if (modified) {
        return PersistentSetFactory.from(keyTypeBagNew, valTypeBagNew, tmp.freeze());
      }
      return def;
    } else {
      return ISet.super.subtract(other);
    }
  }

  @Override
  public IRelation<ISet> asRelation() {
      
    final PersistentHashIndexedBinaryRelation thisSet = PersistentHashIndexedBinaryRelation.this;

    return new IRelation<ISet>() {

        @Override
        public ISet asContainer() {
            return thisSet;
        }

        @Override
        public ISet compose(IRelation<ISet> otherSetRelation) {
            if (otherSetRelation.getClass() != this.getClass()) {
                return IRelation.super.compose(otherSetRelation);
            }
            
            // Here we can optimize the compose operation because we have already an index in memory
            // for both relations

            final PersistentHashIndexedBinaryRelation thatSet =
                    (PersistentHashIndexedBinaryRelation) otherSetRelation.asContainer();

            final SetMultimap.Immutable<IValue, IValue> xy = thisSet.content;
            final SetMultimap.Immutable<IValue, IValue> yz = thatSet.content;

            /**
             * The code below is still sub-optimal because it operates on the logical (rather than the structural) level.
             *
             * TODO: nodes should get proper support for stream processing such that the following template can be used:
             *
             *    // @formatter:off
             *    final Stream<BiConsumer<IValue, IValue>> localStream = null;
             *    final Node updatedNode = localStream
             *    .filter((x, y) -> yz.containsKey(y))
             *    .mapValues(y -> yz.get(y))
             *    .collect(toNode());
             *    // @formatter:on
             */
            final SetMultimap.Transient<IValue, IValue> xz = xy.asTransient();

            for (IValue x : xy.keySet()) {
                final Set.Immutable<IValue> ys = xy.get(x);
                // TODO: simplify expression with nullable data
                final Set.Immutable<IValue> zs = ys.stream()
                        .flatMap(y -> Optional.ofNullable(yz.get(y)).orElseGet(Set.Immutable::of).stream())
                        .collect(CapsuleCollectors.toSet());

                if (zs == null) {
                    xz.__remove(x);
                } else {
                    // xz.__put(x, zs); // TODO: requires node batch update support

                    xz.__remove(x);
                    zs.forEach(z -> xz.__insert(x, z));
                }
            }

            final SetMultimap.Immutable<IValue, IValue> data = xz.freeze();

            final AbstractTypeBag keyTypeBag = data.entrySet().stream().map(Map.Entry::getKey)
                    .map(IValue::getType).collect(AbstractTypeBag.toTypeBag());

            final AbstractTypeBag valTypeBag = data.entrySet().stream().map(Map.Entry::getValue)
                    .map(IValue::getType).collect(AbstractTypeBag.toTypeBag());

            return PersistentSetFactory.from(keyTypeBag, valTypeBag, data);
        }

      @Override
      public int arity() {
        return 2;
      }

      @Override
      public ISet project(int... fieldIndexes) {
        if (Arrays.equals(fieldIndexes, ArrayUtilsInt.arrayOfInt(0))) {
          return domain();
        }

        if (Arrays.equals(fieldIndexes, ArrayUtilsInt.arrayOfInt(1))) {
          return range();
        }

        if (Arrays.equals(fieldIndexes, ArrayUtilsInt.arrayOfInt(0, 1))) {
          return thisSet;
        }

        // TODO: replace by `inverse` API of subsequent capsule release
        if (Arrays.equals(fieldIndexes, ArrayUtilsInt.arrayOfInt(1, 0))) {
          final SetMultimap.Transient<IValue, IValue> builder =
                  PersistentTrieSetMultimap.transientOf(Object::equals);

          content.entryIterator().forEachRemaining(
              tuple -> builder.__insert(tuple.getValue(), tuple.getKey()));

          
          return PersistentSetFactory.from(valTypeBag, keyTypeBag, builder.freeze());
        }

        throw new IllegalStateException("Binary relation patterns exhausted.");
      }

      @Override
      public ISet projectByFieldNames(String... fieldNames) {
        final Type fieldTypeType = thisSet.getType().getFieldTypes();

        if (!fieldTypeType.hasFieldNames()) {
          throw new IllegalOperationException("select with field names",
              thisSet.getType());
        }

        final int[] fieldIndices =
            Stream.of(fieldNames).mapToInt(fieldTypeType::getFieldIndex).toArray();

        return project(fieldIndices);
      }

      /**
       * Flattening Set[Tuple[Tuple[K, V]], _] to Multimap[K, V].
       *
       * @return canonical set of keys
       */
      @Override
      public ISet domain() {
        final Type fieldType0 = thisSet.getType().getFieldType(0);

        if (isTupleOfArityTwo.test(fieldType0)) {
          // TODO: use lazy keySet view instead of materialized data structure
          return thisSet.content.keySet().stream().map(asInstanceOf(ITuple.class))
              .collect(ValueCollectors.toSetMultimap(fieldType0.getOptionalFieldName(0), tuple -> tuple.get(0),
                  fieldType0.getOptionalFieldName(1), tuple -> tuple.get(1)));
        }

        /**
         * NOTE: the following call to {@code stream().collect(toSet())} is suboptimal because
         * {@code thisSet.content.keySet()} already produces the result (modulo dynamic types). The
         * usage of streams solely hides the calculation of precise dynamic type of the set.
         */
        return thisSet.content.keySet().stream().collect(ValueCollectors.toSet());

        // final Immutable<IValue> columnData = (Immutable<IValue>)
        // thisSet.content.keySet();
        // final AbstractTypeBag columnElementTypeBag =
        // columnData.stream().map(IValue::getType).collect(toTypeBag());
        //
        // return PersistentHashSet.from(columnElementTypeBag, columnData);
      }

      /**
       * Flattening Set[Tuple[_, Tuple[K, V]]] to Multimap[K, V].
       *
       * @return canonical set of values
       */
      @Override
      public ISet range() {
        return thisSet.content.values().stream().collect(ValueCollectors.toSet());
      }

      @Override
      public String toString() {
        return thisSet.toString();
      }

      @Override
      public ISet index(IValue key) {
        Immutable<IValue> values = thisSet.content.get(key);
        if (values == null) {
          return EmptySet.EMPTY_SET;
        }

        return PersistentSetFactory.from(values);
      }
    };
  }

}
