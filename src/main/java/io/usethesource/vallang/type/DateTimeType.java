/*******************************************************************************
 * Copyright (c) 2009 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mark Hills (Mark.Hills@cwi.nl) - initial API and implementation
 *******************************************************************************/
package io.usethesource.vallang.type;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.type.TypeFactory.RandomTypesConfig;
import io.usethesource.vallang.type.TypeFactory.TypeReifier;
import io.usethesource.vallang.type.TypeFactory.TypeValues;

/**
 * @author mhills
 *
 */
public class DateTimeType extends DefaultSubtypeOfValue {
    private static final class InstanceKeeper {
        public static final DateTimeType sInstance= new DateTimeType();
    }

    public static DateTimeType getInstance() {
        return InstanceKeeper.sInstance;
    }

    public static class Info extends TypeReifier {
        public Info(TypeValues symbols) {
            super(symbols);
        }

        @Override
        public Type getSymbolConstructorType() {
            return symbols().typeSymbolConstructor("datetime");
        }

        @Override
        public Type fromSymbol(IConstructor symbol, TypeStore store,
                Function<IConstructor, Set<IConstructor>> grammar) {
            return getInstance();
        }

        @Override
        public Type randomInstance(BiFunction<TypeStore, RandomTypesConfig, Type> next, TypeStore store, RandomTypesConfig rnd) {
            return tf().dateTimeType();
        }
    }

    @Override
    public TypeReifier getTypeReifier(TypeValues symbols) {
        return new Info(symbols);
    }

    private static long between(Random r, ChronoField chrono) {
        return between(r, chrono.range().getMinimum(), chrono.range().getMaximum());
    }

    private static long between(Random r, long a, long b) {
        return r.longs(1, a, b).sum();
    }

    @Override
    public IValue randomValue(Random random, IValueFactory vf, TypeStore store, Map<Type, Type> typeParameters,
            int maxDepth, int maxBreadth) {
        boolean partialDateTime = "true".equals(System.getProperty("vallang.random.partialDateTime"));
        boolean zoneOffsets = "true".equals(System.getProperty("vallang.random.zoneOffsets"));

        try {
            if (partialDateTime && random.nextDouble() > 0.8) {
                LocalTime result = LocalTime.ofSecondOfDay(between(random, ChronoField.SECOND_OF_DAY));
                return vf.time(
                    result.getHour(),
                    result.getMinute(),
                    result.getSecond(),
                    (int) TimeUnit.MILLISECONDS.convert(result.getNano(), TimeUnit.NANOSECONDS)
                );
            }
            if (partialDateTime && random.nextDouble() > 0.8) {
                LocalDate result = LocalDate.ofEpochDay(between(random,
                    LocalDate.of(0,1, 1).toEpochDay(),
                    LocalDate.of(9999,1, 1).toEpochDay()
                ));
                return vf.date(
                    result.getYear(),
                    result.getMonthValue(),
                    result.getDayOfMonth()
                );
            }
            Instant result = Instant.ofEpochSecond(between(random,
                -TimeUnit.DAYS.toSeconds(700),
                Instant.now().getEpochSecond() + TimeUnit.DAYS.toSeconds(700)
                ), 0
            );
            if (!zoneOffsets || random.nextDouble() > 0.5) {
                return vf.datetime(result.toEpochMilli());
            }
            ZoneOffset off = ZoneOffset.ofTotalSeconds((int)between(random, ChronoField.OFFSET_SECONDS));
            return vf.datetime(result.toEpochMilli(),
                (int)TimeUnit.HOURS.convert(off.getTotalSeconds(), TimeUnit.SECONDS),
                (int)TimeUnit.MINUTES.convert(off.getTotalSeconds(), TimeUnit.SECONDS) % 60
            );
        } catch (DateTimeException e) {
            // this may happen if the generated random time does
            // not exist due to timezone shifting or due to historical
            // calendar standardization changes
            // So, we just try again until we hit a better random date
            return randomValue(random, vf, store, typeParameters, maxDepth, maxBreadth);
            // The chances of continued failure before we run out of stack are low.
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj == DateTimeType.getInstance();
    }

    @Override
    public boolean isDateTime() {
        return true;
    }

    @Override
    public int hashCode() {
        return 63097;
    }

    @Override
    public String toString() {
        return "datetime";
    }

    @Override
    public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
        return visitor.visitDateTime(this);
    }

    @Override
    protected boolean isSupertypeOf(Type type) {
        return type.isSubtypeOfDateTime(this);
    }

    @Override
    public Type lub(Type other) {
        return other.lubWithDateTime(this);
    }

    @Override
    public Type glb(Type type) {
        return type.glbWithDateTime(this);
    }

    @Override
    protected boolean isSubtypeOfDateTime(Type type) {
        return true;
    }

    @Override
    public boolean intersects(Type other) {
        return other.intersectsWithDateTime(this);
    }

    @Override
    protected boolean intersectsWithDateTime(Type type) {
        return true;
    }

    @Override
    protected Type lubWithDateTime(Type type) {
        return this;
    }

    @Override
    protected Type glbWithDateTime(Type type) {
        return this;
    }
}
