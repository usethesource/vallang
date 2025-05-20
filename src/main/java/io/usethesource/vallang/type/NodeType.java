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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.random.util.RandomUtil;
import io.usethesource.vallang.type.TypeFactory.RandomTypesConfig;
import io.usethesource.vallang.type.TypeFactory.TypeReifier;
import io.usethesource.vallang.type.TypeFactory.TypeValues;

/**
 * A type for values that are nodes. All INode have the type NodeType, and all
 * IConstructors have NodeType as a supertype.
 */
class NodeType extends DefaultSubtypeOfValue {
    protected static class InstanceKeeper {
        public static final NodeType sInstance = new NodeType();
    }

    public static NodeType getInstance() {
        return InstanceKeeper.sInstance;
    }

    public static class Info extends TypeReifier {
        public Info(TypeValues symbols) {
            super(symbols);
        }

        @Override
        public Type getSymbolConstructorType() {
            return symbols().typeSymbolConstructor("node");
        }

        @Override
        public Type fromSymbol(IConstructor symbol, TypeStore store,
                Function<IConstructor, Set<IConstructor>> grammar) {
            return getInstance();
        }

        @Override
        public Type randomInstance(BiFunction<TypeStore, RandomTypesConfig, Type> next, TypeStore store, RandomTypesConfig rnd) {
            return tf().nodeType();
        }
    }

    @Override
    public TypeReifier getTypeReifier(TypeValues symbols) {
        return new Info(symbols);
    }


    @Override
    public String toString() {
        return "node";
    }

    /**
     * Should never be called, NodeType is a singleton
     */
    @Override
    public boolean equals(@Nullable Object o) {
        return o == NodeType.getInstance();
    }

    @Override
    public int hashCode() {
        return 20102;
    }

    @Override
    protected boolean isSupertypeOf(Type type) {
        return type.isSubtypeOfNode(this);
    }

    @Override
    public Type lub(Type other) {
        return other.lubWithNode(this);
    }

    @Override
    public boolean intersects(Type other) {
        return other.intersectsWithNode(this);
    }

    @Override
    protected boolean intersectsWithNode(Type type) {
        return true;
    }

    @Override
    protected boolean intersectsWithAbstractData(Type type) {
        return true;
    }

    @Override
    protected boolean intersectsWithConstructor(Type type) {
        return true;
    }

    @Override
    protected boolean isSubtypeOfNode(Type type) {
        return true;
    }

    @Override
    protected Type lubWithAbstractData(Type type) {
        return this;
    }

    @Override
    protected Type lubWithConstructor(Type type) {
        return this;
    }

    @Override
    protected Type lubWithNode(Type type) {
        return type;
    }

    @Override
    public Type glb(Type type) {
        return type.glbWithNode(this);
    }

    @Override
    protected Type glbWithNode(Type type) {
        return this;
    }

    @Override
    protected Type glbWithConstructor(Type type) {
        return type;
    }

    @Override
    protected Type glbWithAbstractData(Type type) {
        return type;
    }

    @Override
    public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
        return visitor.visitNode(this);
    }

    @Override
    public IValue randomValue(Random random, RandomTypesConfig typesConfig, IValueFactory vf, TypeStore store, Map<Type, Type> typeParameters, int maxDepth, int maxWidth) {
        String name = random.nextBoolean() ? RandomUtil.string(random, 1 + random.nextInt(5)) : RandomUtil.stringAlpha(random, random.nextInt(5));

        int arity = maxDepth <= 0 ? 0 : random.nextInt(maxDepth);
        IValue[] args = new IValue[arity];
        for (int i = 0; i < arity; i++) {
            args[i] = TypeFactory.getInstance().valueType().randomValue(random, typesConfig, vf, store, typeParameters, maxDepth - 1, maxWidth);
        }

        if (RandomUtil.oneEvery(random, 4) &&  maxDepth > 0) {
            int kwArity = 1 + random.nextInt(maxDepth);
            Map<String, IValue> kwParams = new HashMap<>(kwArity);
            for (int i = 0; i < kwArity; i++) {
                String kwName = "";
                while (kwName.isEmpty()) {
                    // names have to start with alpha character
                    kwName = RandomUtil.stringAlpha(random, 3);
                }
                kwName += RandomUtil.stringAlphaNumeric(random, 4);
                kwParams.put(kwName, TypeFactory.getInstance().valueType().randomValue(random, typesConfig, vf, store, typeParameters, maxDepth - 1, maxWidth));
            }

            return vf.node(name, args, kwParams);
        }
        return vf.node(name, args);
    }

    @Override
    public boolean isNode() {
        return true;
    }
}
