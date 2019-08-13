/*******************************************************************************
 * Copyright (c) 2014 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI  
 *******************************************************************************/
package io.usethesource.vallang.util;

import static io.usethesource.capsule.util.stream.CapsuleCollectors.UNORDERED;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.util.stream.DefaultCollector;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;

/**
 * Stores mapping (Type -> Integer) to keep track of a collection's element types. The least upper
 * bound type of is calculated on basis of the map keys.
 */
public abstract class AbstractTypeBag implements Cloneable {

    public abstract AbstractTypeBag increase(Type t);

    public abstract AbstractTypeBag decrease(Type t);

    public abstract Type lub();

    public abstract AbstractTypeBag clone();

    public static AbstractTypeBag of(Type... ts) {
        return TypeBag.of(ts);
    }

    public abstract int size();

    /**
     * Implementation of <@link AbstractTypeBag/> that cached the current least upper bound.
     */
    private static class TypeBag extends AbstractTypeBag {
        private final Map.Immutable<Type, Integer> countMap;

        private @MonotonicNonNull Type cachedLub;

        private TypeBag(Map.Immutable<Type, Integer> countMap) {
            this.countMap = countMap;
        }

        private TypeBag(Map.Immutable<Type, Integer> countMap, Type cachedLub) {
            this.countMap = countMap;
            this.cachedLub = cachedLub;
        }

        public static final AbstractTypeBag of(final Type... ts) {
            AbstractTypeBag result = new TypeBag(Map.Immutable.of());

            for (Type t : ts) {
                result = result.increase(t);
            }

            return result;
        }

        @Override
        public AbstractTypeBag increase(Type t) {
            final Integer oldCount = countMap.get(t);
            final Map.Immutable<Type, Integer> newCountMap;

            if (oldCount == null) {
                newCountMap = countMap.__put(t, 1);

                if (cachedLub == null) {
                    return new TypeBag(newCountMap);
                } else {
                    // update cached type
                    final Type newCachedLub = cachedLub.lub(t);
                    return new TypeBag(newCountMap, newCachedLub);
                }
            } else {
                newCountMap = countMap.__put(t, oldCount + 1);
                return new TypeBag(newCountMap);
            }
        }

        @Override
        public AbstractTypeBag decrease(Type t) {
            final Integer oldCount = countMap.get(t);

            if (oldCount == null) {
                throw new IllegalStateException(String.format("Type '%s' was not present.", t));
            } else if (oldCount > 1) {
                // update and decrease count; lub stays the same
                final Map.Immutable<Type, Integer> newCountMap = countMap.__put(t, oldCount - 1);
                return cachedLub != null ? new TypeBag(newCountMap, cachedLub) : new TypeBag(newCountMap);
            } else {
                // count was zero, thus remove entry and invalidate cached type
                final Map.Immutable<Type, Integer> newCountMap = countMap.__remove(t);
                return new TypeBag(newCountMap);
            }
        }

        @Override
        public Type lub() {
            if (cachedLub == null) {
                Type inferredLubType = TypeFactory.getInstance().voidType();
                for (Type t : countMap.keySet()) {
                    inferredLubType = inferredLubType.lub(t);
                }
                cachedLub = inferredLubType;
            }
            return cachedLub;
        }

        @Override
        public AbstractTypeBag clone() {
            return new TypeBag(countMap);
        }

        @Override
        public String toString() {
            return String.format("PreciseType(members=%s)", countMap.toString());
        }

        @Override
        public int size() {
            return countMap.size();
        }

        @Override
        public int hashCode() {
            return countMap.hashCode();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            
            TypeBag typeBag = (TypeBag) o;
            
            return countMap.equals(typeBag.countMap);
        }
    }

    @SuppressWarnings("unchecked")
    public static <M extends Map.Transient<Type, Integer>> Collector<Type, ?, ? extends AbstractTypeBag> toTypeBag() {
        final BiConsumer<M, Type> accumulator = (countMap, type0) -> countMap.compute(type0,
                (type1, count) -> count == null ? 1 : count + 1);

        final BinaryOperator<M> combiner = (countMap1, countMap2) -> {
            countMap2.forEach((type, count2) -> {
                final Integer count1 = countMap1.getOrDefault(type, 0);
                countMap1.compute(type, (t, c) -> count1 + count2);
            });

            return countMap1;
        };

        final Supplier<? extends Map.Transient<Type, Integer>> supplier = Map.Transient::of;

        return new DefaultCollector<>((Supplier<M>) supplier, accumulator, combiner,
                (countMap) -> new TypeBag(countMap.freeze()), UNORDERED);
    }
}
