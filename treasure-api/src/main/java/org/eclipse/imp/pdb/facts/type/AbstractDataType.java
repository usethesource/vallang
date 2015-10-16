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

package org.eclipse.imp.pdb.facts.type;

import java.util.Map;

import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.UndeclaredAbstractDataTypeException;
import org.eclipse.imp.pdb.facts.exceptions.UndeclaredAnnotationException;

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
        fName = name.intern();
        fParameters = parameters;
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
        return !fParameters.equivalent(VoidType.getInstance());
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
    public boolean equals(Object o) {
        if (o instanceof ConstructorType)
            return false;
        if (o instanceof AbstractDataType) {
            AbstractDataType other = (AbstractDataType) o;
            return fName.equals(other.fName) && fParameters == other.fParameters;
        }
        return false;
    }

    @Override
    public Type instantiate(Map<Type, Type> bindings) {
        if (bindings.isEmpty()) {
            return this;
        }

        Type[] params = new Type[0];
        if (isParameterized()) {
            params = new Type[fParameters.getArity()];
            int i = 0;
            for (Type p : fParameters) {
                params[i++] = p.instantiate(bindings);
            }
        }

        TypeStore store = new TypeStore();
        store.declareAbstractDataType(this);

        return TypeFactory.getInstance().abstractDataType(store, fName, params);
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
        return super.match(matched, bindings) && fParameters.match(matched.getTypeParameters(), bindings);
    }

    @Override
    public boolean declaresAnnotation(TypeStore store, String label) {
        return store.getAnnotationType(this, label) != null;
    }

    @Override
    public Type getAnnotationType(TypeStore store, String label) throws FactTypeUseException {
        Type type = store.getAnnotationType(this, label);

        if (type == null) {
            throw new UndeclaredAnnotationException(this, label);
        }

        return type;
    }
}
