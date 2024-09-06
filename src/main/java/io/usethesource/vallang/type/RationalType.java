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

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.type.TypeFactory.RandomTypesConfig;
import io.usethesource.vallang.type.TypeFactory.TypeValues;
import org.checkerframework.checker.nullness.qual.Nullable;

/*package*/ final class RationalType extends NumberType {
    private static final class InstanceKeeper {
        public final static RationalType sInstance= new RationalType();
    }

    public static RationalType getInstance() {
        return InstanceKeeper.sInstance;
    }

    public static class Info extends TypeFactory.TypeReifier {

        public Info(TypeValues symbols) {
            super(symbols);
        }

        @Override
        public Type getSymbolConstructorType() {
            return symbols().typeSymbolConstructor("rat");
        }

        @Override
        public Type fromSymbol(IConstructor symbol, TypeStore store,
                           Function<IConstructor, Set<IConstructor>> grammar) {
            return getInstance();
        }

        @Override
        public Type randomInstance(Supplier<Type> next, TypeStore store, RandomTypesConfig rnd) {
            return tf().rationalType();
        }
    }

    @Override
    public TypeFactory.TypeReifier getTypeReifier(TypeValues symbols) {
        return new Info(symbols);
    }

    /**
     * Should never need to be called; there should be only one instance of IntegerType
     */
    @Override
    public boolean equals(@Nullable Object obj) {
    return obj == RationalType.getInstance();
}

    @Override
    public int hashCode() {
        return 212873;
    }

    @Override
    public String toString() {
        return "rat";
    }

    @Override
    public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
        return visitor.visitRational(this);
    }

    @Override
    protected boolean isSupertypeOf(Type type) {
        return type.isSubtypeOfRational(this);
    }

    @Override
    public Type lub(Type other) {
        return other.lubWithRational(this);
    }

    @Override
    protected Type lubWithRational(Type type) {
        return this;
    }

    @Override
    protected boolean isSubtypeOfRational(Type type) {
        return true;
    }

    @Override
    public boolean intersects(Type other) {
        return other.intersectsWithRational(this);
    }

    @Override
    protected boolean intersectsWithRational(Type type) {
        return true;
    }

    @Override
    protected boolean intersectsWithInteger(Type type) {
        return false;
    }

    @Override
    protected boolean intersectsWithReal(Type type) {
        return false;
    }

    @Override
    public Type glb(Type type) {
        return type.glbWithRational(this);
    }

    @Override
    protected Type glbWithNumber(Type type) {
        return this;
    }

    @Override
    protected Type glbWithRational(Type type) {
        return this;
    }

    @Override
    protected Type glbWithReal(Type type) {
        return VoidType.getInstance();
    }

    protected Type glbWithInteger(Type type) {
        return VoidType.getInstance();
    }

    @Override
    public IValue randomValue(Random random, IValueFactory vf, TypeStore store, Map<Type, Type> typeParameters,
            int maxDepth, int maxWidth) {
        return vf.rational(random.nextInt(), random.nextInt() + 1);
    }

    @Override
    public boolean isRational() {
        return true;
    }
}
