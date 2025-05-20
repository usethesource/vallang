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

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.TypeFactory.RandomTypesConfig;
import io.usethesource.vallang.type.TypeFactory.TypeValues;

/**
 * A AliasType is a named for a type, i.e. a type alias that can be used to
 * abstract from types that have a complex structure. Since a
 *
 * @{link Type} represents a set of values, a AliasType is defined to be an
 *        alias (because it represents the same set of values).
 *        <p>
 *        Detail: This API does not allow @{link IValue}'s of the basic types
 *        (int, double, etc) to be tagged with a AliasType. Instead the
 *        immediately surrounding structure, such as a set or a relation will
 *        refer to the AliasType.
 */
/* package */ final class AliasType extends Type {
    private final String fName;
    private final Type fAliased;
    private final Type fParameters;

    /* package */ AliasType(String name, Type aliased) {
        fName = name;
        fAliased = aliased;
        fParameters = TypeFactory.getInstance().voidType();
    }

    /* package */ AliasType(String name, Type aliased, Type parameters) {
        fName = name;
        fAliased = aliased;
        fParameters = parameters;
    }

    public static class Info extends TypeFactory.TypeReifier {

        public Info(TypeValues symbols) {
            super(symbols);
        }

        @Override
        public Type getSymbolConstructorType() {
            return symbols().typeSymbolConstructor("alias", TF.stringType(), "name", TF.listType(symbols().symbolADT()), "parameters", symbols().symbolADT(), "aliased");
        }

        @Override
        public Type fromSymbol(IConstructor symbol, TypeStore store, Function<IConstructor, Set<IConstructor>> grammar) {
            String name = ((IString) symbol.get("name")).getValue();
            Type aliased = symbols().fromSymbol((IConstructor) symbol.get("aliased"), store, grammar);
            IList parameters = (IList) symbol.get("parameters");

            // we expand the aliases here, but only if the
            // type parameters have been instantiated or there are none
            if (parameters.isEmpty()) {
                return aliased;
            }
            else {
                Type params = symbols().fromSymbols(parameters, store, grammar);
                return params.isOpen() ? TF.aliasTypeFromTuple(store, name, aliased,  params) : aliased;
            }
        }

        @Override
        public IConstructor toSymbol(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar,
                                 Set<IConstructor> done) {
            // we expand aliases away here!
            return type.getAliased().asSymbol(vf, store, grammar, done);
        }

        @Override
        public void asProductions(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar,
                Set<IConstructor> done) {
            type.getAliased().asProductions(vf, store, grammar, done);
            type.getTypeParameters().asProductions(vf, store, grammar, done);
        }

        @Override
        public Type randomInstance(BiFunction<TypeStore, RandomTypesConfig, Type> next, TypeStore store, RandomTypesConfig rnd) {
            // we don't generate aliases because we also never reify them
            if (!rnd.isWithAliases()) {
                return tf().randomType(store, rnd);
            }
            else {
                // TODO: could generate some type parameters as well
                return tf().aliasType(store, randomLabel(rnd), tf().randomType(store, rnd));
            }
        }
    }

    @Override
    public TypeFactory.TypeReifier getTypeReifier(TypeValues symbols) {
        return new Info(symbols);
    }

    @Override
    public boolean isParameterized() {
        return !fParameters.isBottom();
    }

    @Override
    public boolean isOpen() {
        return fParameters.isOpen() || fAliased.isOpen();
    }

    @Override
    public boolean isAliased() {
        return true;
    }

    @Override
    public boolean isFixedWidth() {
        return fAliased.isFixedWidth();
    }

    /**
     * @return the type parameters of the alias type, void when there are none
     */
    @Override
    public Type getTypeParameters() {
        return fParameters;
    }

    @Override
    public String getName() {
        return fName;
    }

    @Override
    public Type getAliased() {
        return fAliased;
    }

    @Override
    protected boolean isSupertypeOf(Type type) {
        return type.isSubtypeOfAlias(this);
    }

    @Override
    public Type lub(Type other) {
        return other.lubWithAlias(this);
    }

    @Override
    public Type glb(Type type) {
        return type.glbWithAlias(this);
    }

    @Override
    protected Type lubWithAlias(Type type) {
        if (this == type) {
            return this;
        }
        if (getName().equals(type.getName())) {
            return TypeFactory.getInstance().aliasTypeFromTuple(new TypeStore(), type.getName(),
                    getAliased().lub(type.getAliased()), getTypeParameters().lub(type.getTypeParameters()));
        }

        return getAliased().lub(type);
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
        return 49991 + 49831 * fName.hashCode() + 67349 * fAliased.hashCode() + 1433 * fParameters.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null) {
            return false;
        }

        if (o == this) {
            return true;
        }

        if (o instanceof AliasType) {
            AliasType other = (AliasType) o;
            return fName.equals(other.fName) && fAliased == other.fAliased && fParameters == other.fParameters;
        }
        return false;
    }

    @Override
    public <T, E extends Throwable> T accept(ITypeVisitor<T, E> visitor) throws E {
        return visitor.visitAlias(this);
    }

    @Override
    public int getArity() {
        return fAliased.getArity();
    }

    @Override
    public Type getBound() {
        return fAliased.getBound();
    }

    @Override
    public Type getElementType() {
        return fAliased.getElementType();
    }

    @Override
    public int getFieldIndex(String fieldName) {
        return fAliased.getFieldIndex(fieldName);
    }

    @Override
    public String getFieldName(int i) {
        return fAliased.getFieldName(i);
    }

    @Override
    public String getKeyLabel() {
        return fAliased.getKeyLabel();
    }

    @Override
    public String getValueLabel() {
        return fAliased.getValueLabel();
    }

    @Override
    public Type getFieldType(int i) {
        return fAliased.getFieldType(i);
    }

    @Override
    public Type getFieldType(String fieldName) {
        return fAliased.getFieldType(fieldName);
    }

    @Override
    public Type getFieldTypes() {
        return fAliased.getFieldTypes();
    }

    @Override
    public Type getKeyType() {
        return fAliased.getKeyType();
    }

    @Override
    public boolean isBottom() {
        return fAliased.isBottom();
    }

    @Override
    public Type getValueType() {
        return fAliased.getValueType();
    }

    @Override
    public Type compose(Type other) {
        return fAliased.compose(other);
    }

    @Override
    public boolean hasFieldNames() {
        return fAliased.hasFieldNames();
    }

    @Override
    public boolean hasKeywordField(String fieldName, TypeStore store) {
        return fAliased.hasKeywordField(fieldName, store);
    }

    @Override
    public Type instantiate(Map<Type, Type> bindings) {
        if (isParameterized()) {
            // note how we do not declare the new alias anywhere.
            return TypeFactory.getInstance().getFromCache(new AliasType(fName, fAliased.instantiate(bindings), fParameters.instantiate(bindings)));
        }

        // if an alias is not parametrized, then instantiation should not have an effect.
        // if it would have an effect, then the alias type would contain an unbound type parameter
        // which is a bug we assume is absent here.
        return this;
    }

    @Override
    public boolean match(Type matched, Map<Type, Type> bindings) throws FactTypeUseException {
        return super.match(matched, bindings) && fAliased.match(matched, bindings);
    }

    @Override
    public Iterator<Type> iterator() {
        return fAliased.iterator();
    }

    @Override
    public Type select(int... fields) {
        return fAliased.select(fields);
    }

    @Override
    public Type select(String... names) {
        return fAliased.select(names);
    }

    @Override
    public boolean hasField(String fieldName) {
        return fAliased.hasField(fieldName);
    }

    @Override
    public boolean hasField(String fieldName, TypeStore store) {
        return fAliased.hasField(fieldName, store);
    }

    @Override
    protected boolean isSubtypeOfReal(Type type) {
        return fAliased.isSubtypeOfReal(type);
    }

    @Override
    protected boolean isSubtypeOfInteger(Type type) {
        return fAliased.isSubtypeOfInteger(type);
    }

    @Override
    protected boolean isSubtypeOfRational(Type type) {
        return fAliased.isSubtypeOfRational(type);
    }

    @Override
    protected boolean isSubtypeOfList(Type type) {
        return fAliased.isSubtypeOfList(type);
    }

    @Override
    protected boolean isSubtypeOfMap(Type type) {
        return fAliased.isSubtypeOfMap(type);
    }

    @Override
    protected boolean isSubtypeOfNumber(Type type) {
        return fAliased.isSubtypeOfNumber(type);
    }

    @Override
    protected boolean isSubtypeOfSet(Type type) {
        return fAliased.isSubtypeOfSet(type);
    }

    @Override
    protected boolean isSubtypeOfSourceLocation(Type type) {
        return fAliased.isSubtypeOfSourceLocation(type);
    }

    @Override
    protected boolean isSubtypeOfString(Type type) {
        return fAliased.isSubtypeOfString(type);
    }

    @Override
    protected boolean isSubtypeOfNode(Type type) {
        return fAliased.isSubtypeOfNode(type);
    }

    @Override
    protected boolean isSubtypeOfConstructor(Type type) {
        return fAliased.isSubtypeOfConstructor(type);
    }

    @Override
    protected boolean isSubtypeOfAbstractData(Type type) {
        return fAliased.isSubtypeOfAbstractData(type);
    }

    @Override
    protected boolean isSubtypeOfTuple(Type type) {
        return fAliased.isSubtypeOfTuple(type);
    }

    @Override
    protected boolean isSubtypeOfFunction(Type type) {
        return fAliased.isSubtypeOfFunction(type);
    }

    @Override
    protected boolean isSubtypeOfVoid(Type type) {
        return fAliased.isSubtypeOfVoid(type);
    }

    @Override
    protected boolean isSubtypeOfBool(Type type) {
        return fAliased.isSubtypeOfBool(type);
    }

    @Override
    protected boolean isSubtypeOfExternal(Type type) {
        return fAliased.isSubtypeOfExternal(type);
    }

    @Override
    protected boolean isSubtypeOfDateTime(Type type) {
        return fAliased.isSubtypeOfDateTime(type);
    }

    @Override
    protected Type lubWithReal(Type type) {
        return fAliased.lubWithReal(type);
    }

    @Override
    protected Type lubWithInteger(Type type) {
        return fAliased.lubWithInteger(type);
    }

    @Override
    protected Type lubWithRational(Type type) {
        return fAliased.lubWithRational(type);
    }

    @Override
    protected Type lubWithList(Type type) {
        return fAliased.lubWithList(type);
    }

    @Override
    protected Type lubWithMap(Type type) {
        return fAliased.lubWithMap(type);
    }

    @Override
    protected Type lubWithNumber(Type type) {
        return fAliased.lubWithNumber(type);
    }

    @Override
    protected Type lubWithSet(Type type) {
        return fAliased.lubWithSet(type);
    }

    @Override
    protected Type lubWithSourceLocation(Type type) {
        return fAliased.lubWithSourceLocation(type);
    }

    @Override
    protected Type lubWithString(Type type) {
        return fAliased.lubWithString(type);
    }

    @Override
    protected Type lubWithNode(Type type) {
        return fAliased.lubWithNode(type);
    }

    @Override
    protected Type lubWithConstructor(Type type) {
        return fAliased.lubWithConstructor(type);
    }

    @Override
    protected Type lubWithAbstractData(Type type) {
        return fAliased.lubWithAbstractData(type);
    }

    @Override
    protected Type lubWithTuple(Type type) {
        return fAliased.lubWithTuple(type);
    }

    @Override
    protected Type lubWithFunction(Type type) {
        return fAliased.lubWithFunction(type);
    }

    @Override
    protected Type lubWithValue(Type type) {
        return fAliased.lubWithValue(type);
    }

    @Override
    protected Type lubWithVoid(Type type) {
        return fAliased.lubWithVoid(type);
    }

    @Override
    protected Type lubWithBool(Type type) {
        return fAliased.lubWithBool(type);
    }

    @Override
    protected Type lubWithExternal(Type type) {
        return fAliased.lubWithExternal(type);
    }

    @Override
    protected Type lubWithDateTime(Type type) {
        return fAliased.lubWithDateTime(type);
    }

    @Override
    protected boolean isSubtypeOfValue(Type type) {
        return true;
    }

    @Override
    protected Type glbWithReal(Type type) {
        return fAliased.glbWithReal(type);
    }

    @Override
    protected Type glbWithInteger(Type type) {
        return fAliased.glbWithInteger(type);
    }

    @Override
    protected Type glbWithRational(Type type) {
        return fAliased.glbWithRational(type);
    }

    @Override
    protected Type glbWithList(Type type) {
        return fAliased.glbWithList(type);
    }

    @Override
    protected Type glbWithMap(Type type) {
        return fAliased.glbWithMap(type);
    }

    @Override
    protected Type glbWithNumber(Type type) {
        return fAliased.glbWithNumber(type);
    }

    @Override
    protected Type glbWithSet(Type type) {
        return fAliased.glbWithSet(type);
    }

    @Override
    protected Type glbWithSourceLocation(Type type) {
        return fAliased.glbWithSourceLocation(type);
    }

    @Override
    protected Type glbWithString(Type type) {
        return fAliased.glbWithString(type);
    }

    @Override
    protected Type glbWithNode(Type type) {
        return fAliased.glbWithNode(type);
    }

    @Override
    protected Type glbWithConstructor(Type type) {
        return fAliased.glbWithConstructor(type);
    }

    @Override
    protected Type glbWithAlias(Type type) {
        if (this == type) {
            return this;
        }
        if (getName().equals(type.getName())) {
            return TypeFactory.getInstance().aliasTypeFromTuple(new TypeStore(), type.getName(),
                    getAliased().glb(type.getAliased()), getTypeParameters().glb(type.getTypeParameters()));
        }

        return getAliased().glb(type);
    }

    @Override
    protected Type glbWithAbstractData(Type type) {
        return fAliased.glbWithAbstractData(type);
    }

    @Override
    protected Type glbWithTuple(Type type) {
        return fAliased.glbWithTuple(type);
    }

    @Override
    protected Type glbWithFunction(Type type) {
        return fAliased.glbWithFunction(type);
    }

    @Override
    protected Type glbWithValue(Type type) {
        return fAliased.glbWithValue(type);
    }

    @Override
    protected Type glbWithVoid(Type type) {
        return fAliased.glbWithVoid(type);
    }

    @Override
    protected Type glbWithBool(Type type) {
        return fAliased.glbWithBool(type);
    }

    @Override
    protected Type glbWithExternal(Type type) {
        return fAliased.glbWithExternal(type);
    }

    @Override
    protected Type glbWithDateTime(Type type) {
        return fAliased.glbWithDateTime(type);
    }

    @Override
    public Type getAbstractDataType() {
        return fAliased.getAbstractDataType();
    }

    @Override
    public IValue randomValue(Random random, RandomTypesConfig typesConfig, IValueFactory vf, TypeStore store, Map<Type, Type> typeParameters,
            int maxDepth, int maxBreadth) {
        return getAliased().randomValue(random, typesConfig, vf, store, typeParameters, maxDepth, maxBreadth);
    }

    @Override
    public boolean intersects(Type other) {
        return false;
    }

    @Override
    protected boolean intersectsWithReal(Type type) {
        return fAliased.intersectsWithReal(type);
    }

    @Override
    protected boolean intersectsWithInteger(Type type) {
        return fAliased.intersectsWithInteger(type);
    }

    @Override
    protected boolean intersectsWithRational(Type type) {
        return fAliased.intersectsWithRational(type);
    }

    @Override
    protected boolean intersectsWithList(Type type) {
        return fAliased.intersectsWithList(type);
    }

    @Override
    protected boolean intersectsWithMap(Type type) {
        return fAliased.intersectsWithMap(type);
    }

    @Override
    protected boolean intersectsWithNumber(Type type) {
        return fAliased.intersectsWithNumber(type);
    }

    @Override
    protected boolean intersectsWithSet(Type type) {
        return fAliased.intersectsWithSet(type);
    }

    @Override
    protected boolean intersectsWithSourceLocation(Type type) {
        return fAliased.intersectsWithSourceLocation(type);
    }

    @Override
    protected boolean intersectsWithString(Type type) {
        return fAliased.intersectsWithString(type);
    }

    @Override
    protected boolean intersectsWithNode(Type type) {
        return fAliased.intersectsWithNode(type);
    }

    @Override
    protected boolean intersectsWithConstructor(Type type) {
        return fAliased.intersectsWithConstructor(type);
    }

    @Override
    protected boolean intersectsWithAbstractData(Type type) {
        return fAliased.intersectsWithAbstractData(type);
    }

    @Override
    protected boolean intersectsWithTuple(Type type) {
        return fAliased.intersectsWithTuple(type);
    }

    @Override
    protected boolean intersectsWithFunction(Type type) {
        return fAliased.intersectsWithFunction(type);
    }

    @Override
    protected boolean intersectsWithValue(Type type) {
        return fAliased.intersectsWithValue(type);
    }

    @Override
    protected boolean intersectsWithVoid(Type type) {
        return fAliased.intersectsWithVoid(type);
    }

    @Override
    protected boolean intersectsWithBool(Type type) {
        return fAliased.intersectsWithBool(type);
    }

    @Override
    protected boolean intersectsWithExternal(Type type) {
        return fAliased.intersectsWithExternal(type);
    }

    @Override
    protected boolean intersectsWithDateTime(Type type) {
        return fAliased.intersectsWithDateTime(type);
    }
}
