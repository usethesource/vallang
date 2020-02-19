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

import java.util.Calendar;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.type.TypeFactory.RandomTypesConfig;
import io.usethesource.vallang.type.TypeFactory.TypeReifier;

/**
 * @author mhills
 *
 */
public class DateTimeType extends DefaultSubtypeOfValue {
	private static final class InstanceKeeper {
		public final static DateTimeType sInstance= new DateTimeType();
	}

	public static DateTimeType getInstance() {
		return InstanceKeeper.sInstance;
	}

	public static class Info implements TypeReifier {
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
		public Type randomInstance(Supplier<Type> next, TypeStore store, RandomTypesConfig rnd) {
		    return tf().dateTimeType();
		}
	}

	@Override
	public TypeReifier getTypeReifier() {
		return new Info();
	}
	
	@Override
	public IValue randomValue(Random random, IValueFactory vf, TypeStore store, Map<Type, Type> typeParameters,
	        int maxDepth, int maxBreadth) {
	    Calendar cal = Calendar.getInstance();
        try {
            int milliOffset = random.nextInt(1000) * (random.nextBoolean() ? -1 : 1);
            cal.roll(Calendar.MILLISECOND, milliOffset);
            int second = random.nextInt(60) * (random.nextBoolean() ? -1 : 1);
            cal.roll(Calendar.SECOND, second);
            int minute = random.nextInt(60) * (random.nextBoolean() ? -1 : 1);
            cal.roll(Calendar.MINUTE, minute);
            int hour = random.nextInt(60) * (random.nextBoolean() ? -1 : 1);
            cal.roll(Calendar.HOUR_OF_DAY, hour);
            int day = random.nextInt(30) * (random.nextBoolean() ? -1 : 1);
            cal.roll(Calendar.DAY_OF_MONTH, day);
            int month = random.nextInt(12) * (random.nextBoolean() ? -1 : 1);
            cal.roll(Calendar.MONTH, month);

            // make sure we do not go over the 4 digit year limit, which breaks things
            int year = random.nextInt(5000) * (random.nextBoolean() ? -1 : 1);

            // make sure we don't go into negative territory
            if (cal.get(Calendar.YEAR) + year < 1) {
                cal.add(Calendar.YEAR, 1);
            }
            else {
                cal.add(Calendar.YEAR, year);
            }

            return vf.datetime(cal.getTimeInMillis());
        }
        catch (IllegalArgumentException e) {
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
	protected Type lubWithDateTime(Type type) {
		return this;
	}

	@Override
	protected Type glbWithDateTime(Type type) {
		return this;
	}
}
