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
import java.util.function.BiFunction;
import java.util.function.Function;
import static io.usethesource.vallang.random.util.RandomUtil.oneEvery;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.type.TypeFactory.RandomTypesConfig;
import io.usethesource.vallang.type.TypeFactory.TypeValues;
import org.checkerframework.checker.nullness.qual.Nullable;

/*package*/ final class IntegerType extends NumberType {
    private static final class InstanceKeeper {
        public static final IntegerType sInstance= new IntegerType();
    }

    public static IntegerType getInstance() {
        return InstanceKeeper.sInstance;
    }

    public static class Info extends TypeFactory.TypeReifier {
        public Info(TypeValues symbols) {
            super(symbols);
        }

        @Override
        public Type getSymbolConstructorType() {
            return symbols().typeSymbolConstructor("int");
        }

        @Override
        public Type fromSymbol(IConstructor symbol, TypeStore store,
                Function<IConstructor, Set<IConstructor>> grammar) {
            return getInstance();
        }

        @Override
        public Type randomInstance(BiFunction<TypeStore, RandomTypesConfig, Type> next, TypeStore store, RandomTypesConfig rnd) {
            return tf().integerType();
        }
    }

    @Override
    public boolean isInteger() {
        return true;
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
        return obj == IntegerType.getInstance();
    }

    @Override
    public int hashCode() {
        return 74843;
    }

    @Override
    public String toString() {
        return "int";
    }

    @Override
    public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
        return visitor.visitInteger(this);
    }

    @Override
    protected boolean isSupertypeOf(Type type) {
        return type.isSubtypeOfInteger(this);
    }

    @Override
    public Type lub(Type other) {
        return other.lubWithInteger(this);
    }

    @Override
    public Type glb(Type type) {
        return type.glbWithInteger(this);
    }

    @Override
    protected boolean isSubtypeOfInteger(Type type) {
        return true;
    }

    @Override
    protected Type lubWithInteger(Type type) {
        return this;
    }

    @Override
    public boolean intersects(Type other) {
        return other.intersectsWithInteger(this);
    }

    @Override
    protected boolean intersectsWithInteger(Type type) {
        return true;
    }

    @Override
    protected boolean intersectsWithRational(Type type) {
        return false;
    }

    @Override
    protected boolean intersectsWithReal(Type type) {
        return false;
    }

    @Override
    protected Type glbWithReal(Type type) {
        return TF.voidType();
    }

    @Override
    protected Type glbWithRational(Type type) {
        return TF.voidType();
    }

    @Override
    protected Type glbWithNumber(Type type) {
        return this;
    }

    @Override
    public IValue randomValue(Random random, IValueFactory vf, TypeStore store, Map<Type, Type> typeParameters,
            int maxDepth, int maxBreadth) {
        if (oneEvery(random, 5) && maxDepth > 1) {
            return vf.integer(random.nextInt());
        }

        if (oneEvery(random, 5)) {
            return vf.integer(random.nextInt(10));
        }

        if (oneEvery(random, 5)) {
            return vf.integer(-random.nextInt(10));
        }

        if (oneEvery(random, 20) && maxDepth > 1) {
            // sometimes, a very huge number
            IInteger result = vf.integer(random.nextLong());
            do {
                result = result.multiply(vf.integer(random.nextLong()));
            }
            while (random.nextFloat() > 0.4);
            return result.add(vf.integer(random.nextInt()));
        }

        return vf.integer(0);
    }
}
