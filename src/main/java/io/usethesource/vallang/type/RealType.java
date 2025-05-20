/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and 2015 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
 *    Jurgen Vinju

 *******************************************************************************/

package io.usethesource.vallang.type;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.random.util.RandomUtil;
import io.usethesource.vallang.type.TypeFactory.RandomTypesConfig;
import io.usethesource.vallang.type.TypeFactory.TypeValues;
import org.checkerframework.checker.nullness.qual.Nullable;

/*package*/ final class RealType extends NumberType {
    private static final class InstanceKeeper {
        public static final RealType sInstance = new RealType();
    }

    public static RealType getInstance() {
        return InstanceKeeper.sInstance;
    }

    public static class Info extends TypeFactory.TypeReifier {

        public Info(TypeValues symbols) {
            super(symbols);
        }

        @Override
        public Type getSymbolConstructorType() {
            return symbols().typeSymbolConstructor("real");
        }

        @Override
        public Type fromSymbol(IConstructor symbol, TypeStore store,
                           Function<IConstructor, Set<IConstructor>> grammar) {
            return getInstance();
        }

        @Override
        public Type randomInstance(BiFunction<TypeStore, RandomTypesConfig, Type> next, TypeStore store, RandomTypesConfig rnd) {
            return tf().realType();
        }
    }

    @Override
    public TypeFactory.TypeReifier getTypeReifier(TypeValues symbols) {
        return new Info(symbols);
    }

    /**
     * Should never need to be called; there should be only one instance of
     * IntegerType
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        return obj == RealType.getInstance();
    }

    @Override
    public int hashCode() {
        return 84121;
    }

    @Override
    public String toString() {
        return "real";
    }

    @Override
    public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
        return visitor.visitReal(this);
    }

    @Override
    public boolean isReal() {
        return true;
    }

    @Override
    protected boolean isSupertypeOf(Type type) {
        return type.isSubtypeOfReal(this);
    }

    @Override
    public Type lub(Type type) {
        return type.lubWithReal(this);
    }

    @Override
    public boolean intersects(Type other) {
        return other.intersectsWithReal(this);
    }

    @Override
    protected boolean intersectsWithReal(Type type) {
        return true;
    }

    @Override
    protected boolean intersectsWithRational(Type type) {
        return false;
    }

    @Override
    protected boolean intersectsWithInteger(Type type) {
        return false;
    }

    @Override
    public Type glb(Type type) {
        return type.glbWithReal(this);
    }

    @Override
    protected Type lubWithReal(Type type) {
        return this;
    }

    @Override
    protected Type glbWithReal(Type type) {
        return this;
    }

    @Override
    protected Type glbWithNumber(Type type) {
        return this;
    }

    @Override
    protected Type glbWithRational(Type type) {
        return VoidType.getInstance();
    }

    @Override
    protected Type glbWithInteger(Type type) {
        return VoidType.getInstance();
    }

    @Override
    protected boolean isSubtypeOfReal(Type type) {
        return true;
    }

    @Override
    public IValue randomValue(Random random, RandomTypesConfig typesConfig, IValueFactory vf, TypeStore store, Map<Type, Type> typeParameters,
            int maxDepth, int maxWidth) {
        if (RandomUtil.oneEvery(random, 5)) {
            return vf.real(10 * random.nextDouble());
        }
        if (RandomUtil.oneEvery(random, 5)) {
            return vf.real(-10 * random.nextDouble());
        }
        if (RandomUtil.oneEvery(random, 10)) {
            return vf.real(random.nextDouble());
        }
        if (RandomUtil.oneEvery(random, 10)) {
            return vf.real(-random.nextDouble());
        }

        if (RandomUtil.oneEvery(random, 20) && maxDepth > 0) {
            BigDecimal r = BigDecimal.valueOf(random.nextDouble()).multiply(BigDecimal.valueOf(random.nextInt(10000)));
            r = r.multiply(BigDecimal.valueOf(random.nextInt()).add(BigDecimal.valueOf(1000)));
            return vf.real(r.toString());
        }

        return vf.real(0.0);
    }
}
