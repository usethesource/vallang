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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IListWriter;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.TypeFactory.RandomTypesConfig;
import io.usethesource.vallang.type.TypeFactory.TypeReifier;
import io.usethesource.vallang.type.TypeFactory.TypeValues;

/*package*/ class ListType extends DefaultSubtypeOfValue {
    protected final Type fEltType;

    /*package*/ ListType(Type eltType) {
        fEltType = eltType;
    }

    public static class Info extends TypeReifier {

        public Info(TypeValues symbols) {
            super(symbols);
        }

        @Override
        public Type fromSymbol(IConstructor symbol, TypeStore store, Function<IConstructor, Set<IConstructor>> grammar) {
            if (symbol.getConstructorType() == getListType()) {
                return tf().listType(symbols().fromSymbol((IConstructor) symbol.get("symbol"), store, grammar));
            }
            else {
                // TODO remove; this is for bootstrapping with an old version
                return tf().listType(symbols().fromSymbols((IList) symbol.get("symbols"), store, grammar));
            }
        }

        @Override
        public Type getSymbolConstructorType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Type> getSymbolConstructorTypes() {
            return Arrays.stream(new Type[] {
                    getListType(),
                    getRelType() // TODO: can be removed after bootstrap
            }).collect(Collectors.toSet());
        }

        private Type getRelType() {
            return symbols().typeSymbolConstructor("lrel", tf().listType(symbols().symbolADT()), "symbols");
        }

        private Type getListType() {
            return symbols().typeSymbolConstructor("list", symbols().symbolADT(), "symbol");
        }

        @Override
        public IConstructor toSymbol(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar,
                Set<IConstructor> done) {
            return vf.constructor(getListType(), type.getElementType().asSymbol(vf, store, grammar, done));
        }

        @Override
        public void asProductions(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar,
                Set<IConstructor> done) {
            type.getElementType().asProductions(vf, store, grammar, done);
        }

        @Override
        public Type randomInstance(BiFunction<TypeStore, RandomTypesConfig, Type> next, TypeStore store, RandomTypesConfig rnd) {
            return tf().listType(next.apply(store, rnd));
        }

        @Override
        public boolean isRecursive() {
            return false;
        }
    }

    @Override
    public TypeReifier getTypeReifier(TypeValues symbols) {
        return new Info(symbols);
    }

    @Override
    public Type getElementType() {
        return fEltType;
    }

    @Override
    public boolean hasFieldNames() {
        return fEltType.hasFieldNames();
    }

    @Override
    public boolean isList() {
        return true;
    }

    @Override
    public boolean isListRelation() {
        return fEltType.isTuple() || fEltType.isBottom();
    }

    @Override
    public boolean hasField(String fieldName) {
        return fEltType.hasField(fieldName);
    }

    @Override
    public int getFieldIndex(String fieldName) {
        return fEltType.getFieldIndex(fieldName);
    }

    @Override
    public Type getFieldType(int i) {
        return fEltType.getFieldType(i);
    }

    @Override
    public String getFieldName(int i) {
        return fEltType.getFieldName(i);
    }

    @Override
    public String[] getFieldNames() {
        return fEltType.getFieldNames();
    }

    @Override
    public int getArity() {
        return fEltType.getArity();
    }

    @Override
    public Type getFieldType(String fieldName) throws FactTypeUseException {
        return fEltType.getFieldType(fieldName);
    }

    @Override
    public Type getFieldTypes() {
        return fEltType.getFieldTypes();
    }

    @Override
    public Type carrier() {
        return fEltType.carrier();
    }

    @Override
    public Type closure() {
        return TF.listType(fEltType.closure());
    }

    @Override
    public Type compose(Type other) {
        return TF.listType(fEltType.compose(other.getElementType()));
    }

    @Override
    public Type select(int... fields) {
        return TF.listType(fEltType.select(fields));
    }

    @Override
    public Type select(String... names) {
        return TF.listType(fEltType.select(names));
    }

    @Override
    public String toString() {
        if (fEltType.isFixedWidth() && !fEltType.equivalent(VoidType.getInstance())) {
            StringBuilder sb = new StringBuilder();
            sb.append("lrel[");
            int idx = 0;
            Iterator<Type> iter = fEltType.iterator();
            while(iter.hasNext()) {
                Type elemType = iter.next();
                if (idx++ > 0) {
                    sb.append(",");
                }
                sb.append(elemType.toString());
                if (hasFieldNames()) {
                    sb.append(" " + fEltType.getFieldName(idx - 1));
                }
            }
            sb.append("]");
            return sb.toString();
        }
        else {
            return "list[" + fEltType + "]";
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if(o == this) {
            return true;
        }
        else if (o instanceof ListType) {
            ListType other = (ListType) o;
            return fEltType == other.fEltType;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return 75703 + 104543 * fEltType.hashCode();
    }

    @Override
    public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
        return visitor.visitList(this);
    }

    @Override
    public boolean isOpen() {
        return fEltType.isOpen();
    }

    @Override
    public boolean intersects(Type other) {
        return other.intersectsWithList(this);
    }

    @Override
    protected boolean intersectsWithList(Type type) {
        // there is always the empty list!
        return true;
    }

    @Override
    protected boolean isSupertypeOf(Type type) {
        return type.isSubtypeOfList(this);
    }

    @Override
    protected boolean isSubtypeOfList(Type type) {
        return fEltType.isSubtypeOf(type.getElementType());
    }

    @Override
    public Type lub(Type other) {
        return other.lubWithList(this);
    }

    @Override
    public Type glb(Type type) {
        return type.glbWithList(this);
    }

    @Override
    public Type lubWithList(Type type) {
        return this == type ? this : TF.listType(fEltType.lub(type.getElementType()));
    }

    @Override
    protected Type glbWithList(Type type) {
        return this == type ? this : TF.listType(fEltType.glb(type.getElementType()));
    }

    @Override
    public boolean match(Type matched, Map<Type, Type> bindings) throws FactTypeUseException {
        if (!super.match(matched, bindings)) {
            return false;
        }
        else if (matched.isList() || (matched.isAliased() && matched.getAliased().isList()) || matched.isBottom()) {
            return getElementType().match(matched.getElementType(), bindings);
        }

        return true;
    }

    @Override
    public Type instantiate(Map<Type, Type> bindings) {
        return TypeFactory.getInstance().listType(getElementType().instantiate(bindings));
    }

    @Override
    public IValue randomValue(Random random, IValueFactory vf, TypeStore store, Map<Type, Type> typeParameters,
            int maxDepth, int maxWidth) {
        IListWriter result = vf.listWriter();
        if (maxDepth > 0 && random.nextBoolean()) {
            int size = Math.min(maxWidth, 1 + random.nextInt(maxDepth));

            if (!getElementType().isSubtypeOf(TypeFactory.getInstance().voidType())) {
                for (int i =0; i < size; i++) {
                    result.append(getElementType().randomValue(random, vf, store, typeParameters, maxDepth - 1, maxWidth));
                }
            }
        }

        IList done = result.done();
        match(done.getType(), typeParameters);

        return done;
    }
}
