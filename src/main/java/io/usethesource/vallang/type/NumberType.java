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
import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.type.TypeFactory.RandomTypesConfig;
import io.usethesource.vallang.type.TypeFactory.TypeValues;

/**
 * A type for values that are either ints or reals
 */
/* package */class NumberType extends DefaultSubtypeOfValue {
    private static final class InstanceKeeper {
        protected static final NumberType sInstance = new NumberType();
    }

    public static NumberType getInstance() {
        return InstanceKeeper.sInstance;
    }

    public static class Info extends TypeFactory.TypeReifier {
        public Info(TypeValues symbols) {
            super(symbols);
        }

        @Override
        public Type getSymbolConstructorType() {
            return symbols().typeSymbolConstructor("num");
        }

        @Override
        public Type fromSymbol(IConstructor symbol, TypeStore store,
                           Function<IConstructor, Set<IConstructor>> grammar) {
            return getInstance();
        }

        @Override
        public Type randomInstance(Function<RandomTypesConfig,Type> next, TypeStore store, RandomTypesConfig rnd) {
            return tf().numberType();
        }
    }

    @Override
    public TypeFactory.TypeReifier getTypeReifier(TypeValues symbols) {
        return new Info(symbols);
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
    public boolean intersects(Type other) {
        return other.intersectsWithNumber(this);
    }


    @Override
    protected boolean intersectsWithNumber(Type type) {
        return true;
    }

    @Override
    protected boolean intersectsWithInteger(Type type) {
        return true;
    }

    @Override
    protected boolean intersectsWithRational(Type type) {
        return true;
    }

    @Override
    protected boolean intersectsWithReal(Type type) {
        return true;
    }


    @Override
    protected Type lubWithNumber(Type type) {
        return type;
    }

    @Override
    protected Type lubWithInteger(Type type) {
        return NumberType.getInstance();
    }

    @Override
    protected Type lubWithReal(Type type) {
        return NumberType.getInstance();
    }

    @Override
    protected Type lubWithRational(Type type) {
        return NumberType.getInstance();
    }

    @Override
    public Type glb(Type type) {
        return type.glbWithNumber(this);
    }

    @Override
    protected Type glbWithNumber(Type type) {
        return this;
    }

    @Override
    protected Type glbWithInteger(Type type) {
        return type;
    }

    @Override
    protected Type glbWithReal(Type type) {
        return type;
    }

    @Override
    protected Type glbWithRational(Type type) {
        return type;
    }

    /**
     * Should never be called, NodeType is a singleton
     */
    @Override
    public boolean equals(@Nullable Object o) {
        return o == NumberType.getInstance();
    }

    @Override
    public int hashCode() {
        return 133020331;
    }

    @Override
    public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
        return visitor.visitNumber(this);
    }

    @Override
    public IValue randomValue(Random random, IValueFactory vf, TypeStore store, Map<Type, Type> typeParameters,
            int maxDepth, int maxWidth) {
        switch (random.nextInt(3)) {
            case 0:
                return TypeFactory.getInstance().integerType().randomValue(random, vf, store, typeParameters, maxDepth, maxWidth);
            case 1:
                return TypeFactory.getInstance().realType().randomValue(random, vf, store, typeParameters, maxDepth, maxWidth);
            default:
                return TypeFactory.getInstance().rationalType().randomValue(random, vf, store, typeParameters, maxDepth, maxWidth);
        }
    }

    @Override
    public boolean isNumber() {
        return true;
    }
}
