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

import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.type.TypeFactory.TypeReifier;
import org.checkerframework.checker.nullness.qual.Nullable;

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
	        public Type randomInstance(Supplier<Type> next, TypeStore store, Random rnd) {
	            return tf().dateTimeType();
	        }

        public String randomLabel() {
            return null;
        }
	}

	@Override
	public TypeReifier getTypeReifier() {
		return new Info();
	}
	
	@Override
	public boolean equals(@Nullable Object obj) {
    return obj == DateTimeType.getInstance();
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
