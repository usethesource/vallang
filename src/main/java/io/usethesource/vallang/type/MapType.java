/*******************************************************************************
* Copyright (c) 2008, 2012, 2016 CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
*    Anya Helene Bagge - labels
*    Jurgen Vinju - reification
*******************************************************************************/

package io.usethesource.vallang.type;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IMap;
import io.usethesource.vallang.IMapWriter;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.TypeFactory.RandomTypesConfig;
import io.usethesource.vallang.type.TypeFactory.TypeValues;

/*package*/ class MapType extends DefaultSubtypeOfValue {
    protected final Type fKeyType;
    protected final Type fValueType;

    /*package*/ MapType(Type keyType, Type valueType) {
        fKeyType= keyType;
        fValueType = valueType;
    }

    public static class Info extends TypeFactory.TypeReifier {
        public Info(TypeValues symbols) {
            super(symbols);
        }

        @Override
        public Type getSymbolConstructorType() {
            return symbols().typeSymbolConstructor("map", symbols().symbolADT(), "from", symbols().symbolADT(), "to");
        }

        @Override
        public Type fromSymbol(IConstructor symbol, TypeStore store, Function<IConstructor, Set<IConstructor>> grammar) {
            IConstructor from = (IConstructor) symbol.get("from");
            IConstructor to = (IConstructor) symbol.get("to");
            String fromLabel = null;
            String toLabel = null;

            if (symbols().isLabel(from)) {
                fromLabel = symbols().getLabel(from);
                from = (IConstructor) from.get("symbol");
            }
            if (symbols().isLabel(to)) {
                toLabel = symbols().getLabel(to);
                to = (IConstructor) to.get("symbol");
            }
            if (fromLabel != null && toLabel != null) {
                return tf().mapType(symbols().fromSymbol(from, store, grammar), fromLabel, symbols().fromSymbol(to, store, grammar), toLabel);
            }
            else {
                return tf().mapType(symbols().fromSymbol(from, store, grammar), symbols().fromSymbol(to, store, grammar));
            }
        }

        @Override
        public IConstructor toSymbol(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar,
                                 Set<IConstructor> done) {
            if (type.hasFieldNames()) {
                return vf.constructor(getSymbolConstructorType(), symbols().labelSymbol(vf, type.getKeyType().asSymbol(vf, store, grammar, done), type.getKeyLabel()),  symbols().labelSymbol(vf, type.getValueType().asSymbol(vf, store, grammar, done), type.getValueLabel()));
            }
            else {
                return vf.constructor(getSymbolConstructorType(), type.getKeyType().asSymbol(vf, store, grammar, done), type.getValueType().asSymbol(vf, store, grammar, done));
            }
        }

        @Override
        public boolean isRecursive() {
            return false;
        }

        @Override
        public void asProductions(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar,
                Set<IConstructor> done) {
            type.getKeyType().asProductions(vf, store, grammar, done);
            type.getValueType().asProductions(vf, store, grammar, done);
        }

        @Override
        public Type randomInstance(BiFunction<TypeStore, RandomTypesConfig, Type> next, TypeStore store, RandomTypesConfig rnd) {
            if (rnd.isWithMapFieldNames() && rnd.nextBoolean()) {
                return tf().mapType(next.apply(store, rnd), randomLabel(rnd), next.apply(store, rnd), randomLabel(rnd));
            }
            else {
                return tf().mapType(next.apply(store, rnd), next.apply(store, rnd));
            }
        }
    }

    @Override
    public TypeFactory.TypeReifier getTypeReifier(TypeValues symbols) {
        return new Info(symbols);
    }

    @Override
    public Type getKeyType() {
        return fKeyType;
    }

    @Override
    public int getArity() {
        return 2;
    }

    @Override
    public Type getValueType() {
        return fValueType;
    }

    @Override
    public boolean isMap() {
        return true;
    }

    @Override
    public boolean hasFieldNames() {
        return false;
    }

    @Override
    public Type getFieldType(int i) {
        switch (i) {
            case 0: return fKeyType;
            case 1: return fValueType;
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public Type select(int... fields) {
        return TypeFactory.getInstance().setType(getFieldTypes().select(fields));
    }

    @Override
    public Type getFieldTypes() {
        return TypeFactory.getInstance().tupleType(fKeyType, fValueType);
    }

    @Override
    public Type carrier() {
        TypeFactory tf = TypeFactory.getInstance();
        return tf.setType(fKeyType.lub(fValueType));
    }

    @Override
    public int hashCode() {
        return 56509 + 3511 * fKeyType.hashCode() + 1171 * fValueType.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }

        if (!obj.getClass().equals(getClass())) {
            return false;
        }

        MapType other= (MapType) obj;

        // N.B.: The element type must have been created and canonicalized before any
        // attempt to manipulate the outer type (i.e. SetType), so we can use object
        // identity here for the fEltType.
        return fKeyType == other.fKeyType && fValueType == other.fValueType;
    }

    @Override
    public String toString() {
        return "map[" + fKeyType + ", " + fValueType + "]";
    }

    @Override
    public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
        return visitor.visitMap(this);
    }

    @Override
    public boolean intersects(Type other) {
        return other.intersectsWithMap(this);
    }

    @Override
    protected boolean intersectsWithMap(Type type) {
        // there is always the empty map!
        return true;
    }

    @Override
    protected boolean isSupertypeOf(Type type) {
        return type.isSubtypeOfMap(this);
    }

    @Override
    public Type lub(Type other) {
        return other.lubWithMap(this);
    }

    @Override
    public Type glb(Type type) {
        return type.glbWithMap(this);
    }

    @Override
    protected boolean isSubtypeOfMap(Type type) {
        return fKeyType.isSubtypeOf(type.getKeyType())
            && fValueType.isSubtypeOf(type.getValueType());
    }

    @Override
    protected Type lubWithMap(Type type) {
        return this == type ? this : TF.mapTypeFromTuple(getFieldTypes().lub(type.getFieldTypes()));
    }

    @Override
    protected Type glbWithMap(Type type) {
        return this == type ? this : TF.mapTypeFromTuple(getFieldTypes().glb(type.getFieldTypes()));
    }

    @Override
    public boolean isOpen() {
        return fKeyType.isOpen() || fValueType.isOpen();
    }

    @Override
    public boolean match(Type matched, Map<Type, Type> bindings) throws FactTypeUseException {
        if (!super.match(matched, bindings)) {
            return false;
        }

        if (matched.isMap() || (matched.isAliased() && matched.getAliased().isMap()) || matched.isBottom()) {
            return getKeyType().match(matched.getKeyType(), bindings)
                && getValueType().match(matched.getValueType(), bindings);
        }

        return true;
    }

    @Override
    public Type instantiate(Map<Type, Type> bindings) {
        return TypeFactory.getInstance().mapType(getKeyType().instantiate(bindings),
            getValueType().instantiate(bindings));
    }

    @Override
    public IValue randomValue(Random random, RandomTypesConfig typesConfig, IValueFactory vf, TypeStore store,
            Map<Type, Type> typeParameters, int maxDepth, int maxWidth) {
        IMapWriter result = vf.mapWriter();
        if (maxDepth > 0 && random.nextBoolean()) {
            int size = Math.min(maxWidth, 1 + random.nextInt(maxDepth));

            if (!getKeyType().isBottom() && !getValueType().isBottom()) {
                for (int i =0; i < size; i++) {
                    result.put(
                            getKeyType().randomValue(random, typesConfig, vf, store, typeParameters, maxDepth - 1, maxWidth),
                            getValueType().randomValue(random, typesConfig, vf, store, typeParameters, maxDepth - 1, maxWidth));
                }
            }
        }

        IMap done = result.done();
        match(done.getType(), typeParameters);

        return done;
    }
}
