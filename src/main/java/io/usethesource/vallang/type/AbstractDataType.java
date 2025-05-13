/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    jurgen@vinju.org
 *******************************************************************************/

package io.usethesource.vallang.type;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IListWriter;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.exceptions.UndeclaredAbstractDataTypeException;
import io.usethesource.vallang.random.util.RandomUtil;
import io.usethesource.vallang.type.TypeFactory.RandomTypesConfig;
import io.usethesource.vallang.type.TypeFactory.TypeReifier;
import io.usethesource.vallang.type.TypeFactory.TypeValues;

/**
 * A AbstractDataType is an algebraic sort. A sort is produced by
 * constructors, @see NodeType. There can be many constructors for a single
 * sort.
 *
 * @see ConstructorType
 */
/* package */ class AbstractDataType extends NodeType {
    private final String fName;
    private final Type fParameters;

    protected AbstractDataType(String name, Type parameters) {
        fName = name;
        fParameters = parameters;
    }

    public static class Info extends TypeReifier {
        public Info(TypeValues symbols) {
            super(symbols);
        }

        @Override
        public Type getSymbolConstructorType() {
            return symbols().typeSymbolConstructor("adt", TF.stringType(), "name", TF.listType(symbols().symbolADT()), "parameters");
        }

        @Override
        public IConstructor toSymbol(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar, Set<IConstructor> done) {
            IConstructor res = simpleToSymbol(type, vf, store, grammar, done);

            if (!done.contains(res)) {
                done.add(res);

                if (type.getTypeParameters().getArity() != 0) {
                    // we have to look up the original definition to find the original constructors
                    Type adt = store.lookupAbstractDataType(type.getName());
                    if (adt != null) {
                        type = adt; // then collect the grammar for the uninstantiated adt.
                    }
                }

                asProductions(type, vf, store, grammar, done);
            }

            return res;
        }

        private IConstructor simpleToSymbol(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar, Set<IConstructor> done) {
            IListWriter w = vf.listWriter();
            Type params = type.getTypeParameters();

            if (params.getArity() > 0) {
                for (Type param : params) {
                    w.append(param.asSymbol(vf, store, grammar, done));
                }
            }

            return vf.constructor(getSymbolConstructorType(), vf.string(type.getName()), w.done());
        }

        @Override
        public void asProductions(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar, Set<IConstructor> done) {
            store.lookupAlternatives(type).stream().forEach(x -> x.asProductions(vf, store, grammar, done));
        }

        @Override
        public Type fromSymbol(IConstructor symbol, TypeStore store, Function<IConstructor,Set<IConstructor>> grammar) {
            String name = ((IString) symbol.get("name")).getValue();
            Type adt = store.lookupAbstractDataType(name);

            if (adt != null) {
                // this stops infinite recursions while exploring the type store.
                return adt;
            }

            Type params = symbols().fromSymbols((IList) symbol.get("parameters"), store, grammar);
            if (params.isBottom() || params.getArity() == 0) {
                adt = TF.abstractDataType(store, name);
            }
            else {
                adt = TF.abstractDataTypeFromTuple(store, name, params);

                // chances are that the params have already been instantiated,
                // but the grammar contains uninstantiated definitions.
                // we have to find the uninstantiated symbol in the grammar now
                // to be able to create the constructors properly.
            }

            // explore the rest of the definition and add it to the store
            for (IConstructor t : grammar.apply(symbol)) {
                ((ConstructorType.Info) t.getConstructorType().getTypeReifier(symbols())).fromProduction(t, store, grammar);
            }

            return adt;
        }

        @Override
        public boolean isRecursive() {
            return true;
        }

        @Override
        public Type randomInstance(Function<RandomTypesConfig,Type> next, TypeStore store, RandomTypesConfig rnd) {
            if (!rnd.isWithRandomAbstractDatatypes()) {
                return next.apply(rnd);
            }

            if (rnd.nextBoolean()) {
                Type[] adts = store.getAbstractDataTypes().toArray(new Type[0]);

                if (adts.length > 0) { // otherwise we will generate a new instance down below
                    return adts[rnd.nextInt(adts.length)];
                }
            }

            Type adt;

            String adtName;
            do {
                // do not accidentally generate an ADT which already exists.
                adtName = randomLabel(rnd);
            } while (store.lookupAbstractDataType(adtName) != null);

            if (rnd.nextBoolean()) {
                Type param1 = new ParameterType.Info(symbols()).randomInstance(next, store, rnd.withTypeParameters());

                if (rnd.nextBoolean()) {
                    // first declare the open type:
                    adt = tf().abstractDataTypeFromTuple(store, adtName, tf().tupleType(param1));
                }
                else {
                    Type param2 = new ParameterType.Info(symbols()).randomInstance(next, store, rnd.withTypeParameters());

                    // first declare the open type
                    adt = tf().abstractDataTypeFromTuple(store, adtName, tf().tupleType(param1, param2));
                }
            }
            else {
                adt = tf().abstractDataType(store, adtName);
            }

            // should be defined by at least one nullary constructor
            tf().constructor(store, adt, randomLabel(rnd));

            if (rnd.nextBoolean()) {
                // and perhaps we generate another one with it:
                System.err.println("CONS: " + tf().constructorFromTuple(store, randomInstance(next, store, rnd), randomLabel(rnd), randomTuple(next, store, rnd.withMapFieldNames())));
            }

            return adt;
        }
    }

    @Override
    public TypeReifier getTypeReifier(TypeValues symbols) {
        return new Info(symbols);
    }

    @Override
    protected boolean isSupertypeOf(Type type) {
        return type.isSubtypeOfAbstractData(this);
    }

    @Override
    public boolean isOpen() {
        return getTypeParameters().isOpen();
    }

    @Override
    public Type lub(Type other) {
        return other.lubWithAbstractData(this);
    }

    @Override
    public Type glb(Type type) {
        return type.glbWithAbstractData(this);
    }

    @Override
    public boolean intersects(Type other) {
        return other.intersectsWithAbstractData(this);
    }

    @Override
    protected boolean intersectsWithAbstractData(Type type) {
        if (!isParameterized()) {
            return type == this;
        }

        if (type.getName().equals(this.getName())) {
            return getTypeParameters().intersects(type.getTypeParameters());
        }

        return false;
    }

    @Override
    protected boolean isSubtypeOfNode(Type type) {
        return true;
    }

    @Override
    protected boolean isSubtypeOfAbstractData(Type type) {
        if (this == type) {
            return true;
        }

        if (getName().equals(type.getName())) {
            return getTypeParameters().isSubtypeOf(type.getTypeParameters());
        }

        return false;
    }

    @Override
    protected Type lubWithAbstractData(Type type) {
        if (this == type) {
            return this;
        }

        if (fName.equals(type.getName())) {
            return TF.abstractDataTypeFromTuple(new TypeStore(), fName,
                    getTypeParameters().lub(type.getTypeParameters()));
        }

        return TF.nodeType();
    }

    @Override
    protected Type lubWithConstructor(Type type) {
        return lubWithAbstractData(type.getAbstractDataType());
    }

    @Override
    protected Type glbWithNode(Type type) {
        return this;
    }

    @Override
    protected Type glbWithAbstractData(Type type) {
        if (this == type) {
            return this;
        }

        if (fName.equals(type.getName())) {
            return TF.abstractDataTypeFromTuple(new TypeStore(), fName,
                    getTypeParameters().glb(type.getTypeParameters()));
        }

        return TF.voidType();
    }

    @Override
    protected Type glbWithConstructor(Type type) {
        if (type.isSubtypeOf(this)) {
            return type;
        }

        return TF.voidType();
    }

    @Override
    public boolean isParameterized() {
        return !fParameters.isBottom();
    }

    @Override
    public boolean hasField(String fieldName, TypeStore store) {
        // we look up by name because this might be an instantiated
        // parameterized data-type
        // which will not be present in the store.

        Type parameterizedADT = store.lookupAbstractDataType(getName());

        if (parameterizedADT == null) {
            throw new UndeclaredAbstractDataTypeException(this);
        }

        for (Type alt : store.lookupAlternatives(parameterizedADT)) {
            if (alt.hasField(fieldName, store)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean hasKeywordField(String fieldName, TypeStore store) {
        // we look up by name because this might be an instantiated
        // parameterized data-type
        // which will not be present in the store.

        Type parameterizedADT = store.lookupAbstractDataType(getName());

        if (parameterizedADT == null) {
            throw new UndeclaredAbstractDataTypeException(this);
        }

        if (store.getKeywordParameterType(this, fieldName) != null) {
            return true;
        }

        for (Type alt : store.lookupAlternatives(parameterizedADT)) {
            if (alt.hasKeywordField(fieldName, store)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(fName);
        if (isParameterized()) {
            sb.append("[");
            int idx = 0;
            for (Type elemType : fParameters) {
                if (idx++ > 0) {
                    sb.append(",");
                }
                sb.append(elemType.toString());
            }
            sb.append("]");
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return 49991 + 49831 * fName.hashCode() + 49991 + fParameters.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null) {
            return false;
        }

        if (o.getClass().equals(getClass())) {
            AbstractDataType other = (AbstractDataType) o;
            return fName.equals(other.fName) && fParameters == other.fParameters;
        }

        return false;
    }

    @Override
    public Type instantiate(Map<Type, Type> bindings) {
        if (bindings.isEmpty() || !isParameterized()) {
            return this;
        }

        TypeStore store = new TypeStore();

        // TODO: find out why we had this call
        // store.declareAbstractDataType(this);

        // Here it is important _not_ to reuse TupleType.instantiate, since
        // that has a normalizing feature to `void` if one of the parameters
        // reduced to `void`. The type parameters of an ADT
        // can be void and still the ADT type can have values,
        Type params = instantiateTuple((TupleType) fParameters, bindings);

        return TypeFactory.getInstance().abstractDataTypeFromTuple(store, fName, params);
    }

    @Override
    public String getName() {
        return fName;
    }

    @Override
    public Type getTypeParameters() {
        return fParameters;
    }

    @Override
    public Type getAbstractDataType() {
        return this;
    }

    @Override
    public <T, E extends Throwable> T accept(ITypeVisitor<T, E> visitor) throws E {
        return visitor.visitAbstractData(this);
    }

    @Override
    public boolean match(Type matched, Map<Type, Type> bindings) throws FactTypeUseException {
        if (!super.match(matched, bindings)) {
            return false;
        }

        if (matched.isAbstractData() || (matched.isAliased() && matched.getAliased().isAbstractData()) || matched.isBottom()) {
            return getTypeParameters().match(matched.getTypeParameters(), bindings);
        }

        return true;
    }

    @Override
    public IValue randomValue(Random random, IValueFactory vf, TypeStore store, Map<Type, Type> typeParameters,
            int maxDepth, int maxWidth) {
        IValue done = RandomUtil.randomADT(this, random, vf, store, typeParameters, maxDepth, maxWidth);
        match(done.getType(), typeParameters);
        return done;
    }

    @Override
    public boolean isAbstractData() {
        return true;
    }
}
