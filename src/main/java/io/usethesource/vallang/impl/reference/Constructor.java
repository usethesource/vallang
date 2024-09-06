package io.usethesource.vallang.impl.reference;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.capsule.util.collection.AbstractSpecialisedImmutableMap;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IWithKeywordParameters;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.exceptions.UnexpectedChildTypeException;
import io.usethesource.vallang.impl.fields.AbstractDefaultWithKeywordParameters;
import io.usethesource.vallang.impl.fields.ConstructorWithKeywordParametersFacade;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.visitors.IValueVisitor;

/**
 * Implementation of a typed tree node with access to children via labels
 */
public class Constructor extends Node implements IConstructor {
    /*package*/ Constructor(Type type, IValue[] children) {
        super(type.getName(), type, children);
        assert type.getAbstractDataType().isParameterized() ? type.getAbstractDataType().isOpen() : true;
    }

    /*package*/ Constructor(Type type) {
        this(type, new IValue[0]);
    }

    private Constructor(Constructor other, int childIndex, IValue newChild) {
        super(other, childIndex, newChild);
    }

    @Override
    public Type getType() {
        return getConstructorType().getAbstractDataType();
    }

    @Override
    public Type getUninstantiatedConstructorType() {
        return fType;
    }

    public Type getConstructorType() {
        if (fType.getAbstractDataType().isParameterized()) {
            assert fType.getAbstractDataType().isOpen();

            // this assures we always have the most concrete type for constructors.
            Type[] actualTypes = new Type[fChildren.length];
            for (int i = 0; i < fChildren.length; i++) {
                actualTypes[i] = fChildren[i].getType();
            }

            Map<Type,Type> bindings = new HashMap<Type,Type>();
            fType.getFieldTypes().match(TypeFactory.getInstance().tupleType(actualTypes), bindings);

            for (Type field : fType.getAbstractDataType().getTypeParameters()) {
                if (!bindings.containsKey(field)) {
                    bindings.put(field, TypeFactory.getInstance().voidType());
                }
            }

            return fType.instantiate(bindings);
        }

        return fType;
    }

    public IValue get(String label) {
        return super.get(fType.getFieldIndex(label));
    }

    public Type getChildrenTypes() {
        return fType.getFieldTypes();
    }

    @Override
    public IConstructor set(int i, IValue newChild) throws IndexOutOfBoundsException {
        checkChildType(i, newChild);
        return new Constructor(this, i, newChild);
    }


    public IConstructor set(String label, IValue newChild) throws FactTypeUseException {
        int childIndex = fType.getFieldIndex(label);
        checkChildType(childIndex, newChild);
        return new Constructor(this, childIndex, newChild);
    }

    private void checkChildType(int i, IValue newChild) {
        Type type = newChild.getType();
        Type expectedType = getConstructorType().getFieldType(i);
        if (!type.isSubtypeOf(expectedType)) {
            throw new UnexpectedChildTypeException(expectedType, type);
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        else if(obj == null) {
            return false;
        }
        else if (getClass() == obj.getClass()) {
            Constructor other = (Constructor) obj;
            return fType == other.fType && super.equals(obj);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return 17 + ~super.hashCode();
    }

    @Override
    public <T, E extends Throwable> T accept(IValueVisitor<T,E> v) throws E {
        return v.visitConstructor(this);
    }

    @Override
    public boolean has(String label) {
        return getConstructorType().hasField(label);
    }

    @Override
    public boolean mayHaveKeywordParameters() {
        return true;
    }

    @Override
    public IWithKeywordParameters<IConstructor> asWithKeywordParameters() {
        return new AbstractDefaultWithKeywordParameters<IConstructor>(this, AbstractSpecialisedImmutableMap.<String,IValue>mapOf()) {
            @Override
            protected IConstructor wrap(IConstructor content, io.usethesource.capsule.Map.Immutable<String, IValue> parameters) {
                return new ConstructorWithKeywordParametersFacade(content, parameters);
            }

            @Override
            public boolean hasParameters() {
                return parameters != null && parameters.size() > 0;
            }

            @Override
            @SuppressWarnings("return.type.incompatible")
            public java.util.Set<String> getParameterNames() {
                return Collections.unmodifiableSet(parameters.keySet());
            }

            @Override
            public Map<String, IValue> getParameters() {
                return Collections.unmodifiableMap(parameters);
            }
        };
    }
}
