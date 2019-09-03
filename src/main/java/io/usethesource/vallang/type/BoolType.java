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

package io.usethesource.vallang.type;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.type.TypeFactory.RandomTypesConfig;

import org.checkerframework.checker.nullness.qual.Nullable;

/*package*/ final class BoolType extends DefaultSubtypeOfValue {
	private final static class InstanceKeeper {
		public final static BoolType sInstance = new BoolType();
	}

	public static BoolType getInstance() {
		return InstanceKeeper.sInstance;
	}

	public static class Info implements TypeFactory.TypeReifier {
		@Override
		public Type getSymbolConstructorType() {
			return symbols().typeSymbolConstructor("bool");
		}

		@Override
		public Type fromSymbol(IConstructor symbol, TypeStore store,
				Function<IConstructor, Set<IConstructor>> grammar) {
			return getInstance();
		}

        @Override
        public Type randomInstance(Supplier<Type> next, RandomTypesConfig rnd) {
            return tf().boolType();
        }
	}

	@Override
	public TypeFactory.TypeReifier getTypeReifier() {
		return new Info();
	}
	
	/**
	 * Should never need to be called; there should be only one instance of
	 * IntegerType
	 */
	@Override
	public boolean equals(@Nullable Object obj) {
    return obj == BoolType.getInstance();
}

	@Override
	public int hashCode() {
		return 84121;
	}

	@Override
	public String toString() {
		return "bool";
	}

	@Override
	public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
		return visitor.visitBool(this);
	}

	@Override
	protected boolean isSupertypeOf(Type type) {
		return type.isSubtypeOfBool(this);
	}

	@Override
	public Type lub(Type other) {
		return other.lubWithBool(this);
	}

	@Override
	protected boolean isSubtypeOfBool(Type type) {
		return true;
	}

	@Override
	protected Type lubWithBool(Type type) {
		return this;
	}

	@Override
	public Type glb(Type type) {
		return type.glbWithBool(this);
	}

	@Override
	protected Type glbWithBool(Type type) {
		return this;
	}
}
