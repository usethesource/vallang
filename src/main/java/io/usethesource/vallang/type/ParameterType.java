/*******************************************************************************
 * Copyright (c) 2008 CWI.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jurgen Vinju - initial API and implementation
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
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.TypeFactory.RandomTypesConfig;
import io.usethesource.vallang.type.TypeFactory.TypeReifier;
import io.usethesource.vallang.type.TypeFactory.TypeValues;



/**
 * A Parameter Type can be used to represent an abstract type,
 * i.e. a type that needs to be instantiated with an actual type
 * later.
 */
/*package*/ final class ParameterType extends DefaultSubtypeOfValue {
    private final String fName;
    private final Type fBound;

    /* package */ ParameterType(String name, Type bound) {
        fName = name.intern();
        fBound = bound;
    }

    /* package */ ParameterType(String name) {
        fName = name.intern();
        fBound = TypeFactory.getInstance().valueType();
    }

    public static class Info extends TypeReifier {
        public Info(TypeValues symbols) {
            super(symbols);
        }

        @Override
        public Type getSymbolConstructorType() {
            return symbols().typeSymbolConstructor("parameter", tf().stringType() , "name", symbols().symbolADT(), "bound");
        }

        @Override
        public Type fromSymbol(IConstructor symbol, TypeStore store, Function<IConstructor, Set<IConstructor>> grammar) {
            return tf().parameterType(((IString) symbol.get("name")).getValue(), symbols().fromSymbol((IConstructor) symbol.get("bound"), store, grammar));
        }

        @Override
        public Type randomInstance(BiFunction<TypeStore, RandomTypesConfig, Type> next, TypeStore store, RandomTypesConfig rnd) {
            if (rnd.isWithTypeParameters()) {
                return tf().parameterType(randomLabel(rnd));
            }

            return next.apply(store, rnd);
        }

        @Override
        public IConstructor toSymbol(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar,
                Set<IConstructor> done) {
            return vf.constructor(getSymbolConstructorType(), vf.string(type.getName()), type.getBound().asSymbol(vf, store, grammar, done));
        }

        @Override
        public void asProductions(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar,
                Set<IConstructor> done) {
            type.getBound().asProductions(vf, store, grammar, done);
        }
    }

    @Override
    public TypeReifier getTypeReifier(TypeValues symbols) {
        return new Info(symbols);
    }

    @Override
    public Type getTypeParameters() {
        return getBound().getTypeParameters();
    }

    @Override
    public Type getBound() {
        return fBound;
    }

    @Override
    public String getName() {
        return fName;
    }

    @Override
    public int getArity(){
        return fBound.getArity();
    }

    @Override
    public Type getFieldType(int i){
        return fBound.getFieldType(i);
    }

    @Override
    public String[] getFieldNames(){
        return fBound.getFieldNames();
    }

    @Override
    public String toString() {
        return fBound.equivalent(ValueType.getInstance()) ? "&" + fName : "&" + fName + "<:" + fBound.toString();
    }

    @Override
    public int hashCode() {
        return 49991 + 49831 * fName.hashCode() + 133020331 * fBound.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null) {
            return false;
        }

        if (o instanceof ParameterType) {
            ParameterType other = (ParameterType) o;
            return fName.equals(other.fName) && fBound == other.fBound;
        }
        return false;
    }

    @Override
    public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
        return visitor.visitParameter(this);
    }

    @Override
    public boolean intersects(Type other) {
        if (other == this) {
            return true;
        }

        return getBound().intersects(other);
    }

    @Override
    /**
     * Type parameters are universal, extending their semantics over
     * possible extensions of the vallang type system.
     */
    protected boolean intersectsWithExternal(Type type) {
        return getBound().intersects(type);
    }
    /**
     * Read this as "could be instantiated as a super-type of"
     */
    @Override
    protected boolean isSupertypeOf(Type type) {
        if (type == this) {
            // here we assume hygenic type parameter binding
            return true;
        }

        // if the type parameter is totally free,
        // it is bound by `value` only, then it can
        // be a supertype of anything. This is also
        // a stop condition for an infinite recursion.
        if (getBound().isTop()) {
            return true;
        }

        // otherwise, the instantiated type goes no higher than the
        // bound, so if the bound is not a supertype,
        // then the parameter can't be either:
        return getBound().isSupertypeOf(type);
    }

    @Override
    protected boolean isSubtypeOfAbstractData(Type type) {
        return couldBeSubtypeOf(type);
    }

    /**
     * @return true iff this parameter type when instantiated could
     * be a sub-type of the given type.
     */
    private boolean couldBeSubtypeOf(Type type) {
        // the only way that this is impossible if is the compared type
        // is not comparable to the bound
        return getBound().comparable(type);
    }

    @Override
    protected boolean isSubtypeOfBool(Type type) {
        return couldBeSubtypeOf(type);
    }

    @Override
    protected boolean isSubtypeOfConstructor(Type type) {
        return couldBeSubtypeOf(type);
    }

    @Override
    protected boolean isSubtypeOfDateTime(Type type) {
        return couldBeSubtypeOf(type);
    }

    @Override
    protected boolean isSubtypeOfExternal(Type type) {
        return couldBeSubtypeOf(type);
    }

    @Override
    protected boolean isSubtypeOfInteger(Type type) {
        return couldBeSubtypeOf(type);
    }

    @Override
    protected boolean isSubtypeOfList(Type type) {
        return couldBeSubtypeOf(type);
    }

    @Override
    protected boolean isSubtypeOfMap(Type type) {
        return couldBeSubtypeOf(type);
    }

    @Override
    protected boolean isSubtypeOfNode(Type type) {
        return couldBeSubtypeOf(type);
    }

    @Override
    protected boolean isSubtypeOfNumber(Type type) {
        return couldBeSubtypeOf(type);
    }

    @Override
    protected boolean isSubtypeOfParameter(Type type) {
        return couldBeSubtypeOf(type);
    }

    @Override
    protected boolean isSubtypeOfRational(Type type) {
        return couldBeSubtypeOf(type);
    }

    @Override
    protected boolean isSubtypeOfReal(Type type) {
        return couldBeSubtypeOf(type);
    }

    @Override
    protected boolean isSubtypeOfSet(Type type) {
        return couldBeSubtypeOf(type);
    }

    @Override
    protected boolean isSubtypeOfSourceLocation(Type type) {
        return couldBeSubtypeOf(type);
    }

    @Override
    protected boolean isSubtypeOfString(Type type) {
        return couldBeSubtypeOf(type);
    }

    @Override
    protected boolean isSubtypeOfTuple(Type type) {
        return couldBeSubtypeOf(type);
    }

    @Override
    protected boolean isSubtypeOfValue(Type type) {
        return couldBeSubtypeOf(type);
    }

    @Override
    protected boolean isSubtypeOfVoid(Type type) {
        return couldBeSubtypeOf(type);
    }
    @Override
    protected boolean intersectsWithValue(Type type) {
        return true;
    }

    @Override
    protected boolean intersectsWithReal(Type type) {
        return getBound().intersects(type);
    }

    @Override
    protected boolean intersectsWithInteger(Type type) {
        return getBound().intersects(type);
    }
    @Override
    protected boolean intersectsWithRational(Type type) {
        return getBound().intersects(type);
    }
    @Override
    protected boolean intersectsWithList(Type type) {
        return getBound().intersects(type);
    }
    @Override
    protected boolean intersectsWithMap(Type type) {
        return getBound().intersects(type);
    }
    @Override
    protected boolean intersectsWithNumber(Type type) {
        return getBound().intersects(type);
    }
    @Override
    protected boolean intersectsWithRelation(Type type) {
        return getBound().intersects(type);
    }

    @Override
    protected boolean intersectsWithSet(Type type) {
        return getBound().intersects(type);
    }

    @Override
    protected boolean intersectsWithSourceLocation(Type type) {
        return getBound().intersects(type);
    }

    @Override
    protected boolean intersectsWithString(Type type) {
        return getBound().intersects(type);
    }

    @Override
    protected boolean intersectsWithNode(Type type) {
        return getBound().intersects(type);
    }

    @Override
    protected boolean intersectsWithConstructor(Type type) {
        return getBound().intersects(type);
    }

    @Override
    protected boolean intersectsWithAbstractData(Type type) {
        return getBound().intersects(type);
    }

    @Override
    protected boolean intersectsWithTuple(Type type) {
        return getBound().intersects(type);
    }

    @Override
    protected boolean intersectsWithFunction(Type type) {
        return getBound().intersects(type);
    }

    @Override
    protected boolean intersectsWithVoid(Type type) {
        // the intersection of void, even with itself is empty.
        return false;
    }

    @Override
    protected boolean intersectsWithBool(Type type) {
        return getBound().intersects(type);
    }

    @Override
    protected boolean intersectsWithDateTime(Type type) {
        return getBound().intersects(type);
    }

    @Override
    public Type lub(Type type) {
        if (type == this) {
            return this;
        }

        if (type.isSubtypeOf(getBound())) {
            return this;
        }

        return getBound().lub(type);
    }

    @Override
    public Type glb(Type type) {
        if (type == this) {
            return this;
        }

        if (type.isSupertypeOf(getBound())) {
            return this;
        }

        return getBound().glb(type);
    }

    @Override
    public boolean match(Type matched, Map<Type, Type> bindings) throws FactTypeUseException {
        if (!super.match(matched, bindings)) {
            return false;
        }

        Type earlier = bindings.get(this);

        if (earlier != null) {
            // now it is time for earlier bindings to have an effect
            // on the current binding if there is recursive use of type parameters
            // such as in self-application of higher-order functions like "curry"
            Type lub = earlier;
            Map<Type, Type> newBindings = new HashMap<>(bindings);

            if (matched.isOpen() && !matched.isParameter() && matched.match(earlier, newBindings)) {
                if (newBindings.size() > bindings.size()) {
                    bindings.put(this, matched.instantiate(newBindings));
                    bindings.putAll(newBindings);
                }

                return true;
            }
            // matched must be on the left side of lub here to prevent loss of field names
            else if ((lub = matched.lub(earlier)).isSubtypeOf(getBound())) {
                bindings.put(this, lub);
                getBound().match(matched, bindings);
                return true;
            }
            else {
                return false;
            }
        }
        else {
            bindings.put(this, matched);
            getBound().match(matched, bindings);
        }

        return true;
    }

    @Override
    public Type instantiate(Map<Type, Type> bindings) {
        Type result = bindings.get(this);

        if (result != null && result != this) {
            try {
                return result.instantiate(bindings);
            }
            catch (StackOverflowError e) {
                assert false : "type bindings are not hygenic: cyclic parameter binding leads to infinite recursion";
                return result;
            }
        }
        else {
            return TypeFactory.getInstance().parameterType(fName, getBound().instantiate(bindings));
        }
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isParameter() {
        return true;
    }

    @Override
    public IValue randomValue(Random random, RandomTypesConfig typesConfig, IValueFactory vf, TypeStore store,
            Map<Type, Type> typeParameters, int maxDepth, int maxWidth) {
        IValue val = getBound().randomValue(random, typesConfig, vf, store, typeParameters, maxDepth, maxWidth);

        inferBinding(typeParameters, val);

        return val;
    }

    private void inferBinding(Map<Type, Type> typeParameters, IValue val) {
        Type tv = typeParameters.get(this);

        if (tv != null) {
            tv = tv.lub(val.getType());
        }
        else {
            tv = val.getType();
        }

        typeParameters.put(this, tv);
    }
}
