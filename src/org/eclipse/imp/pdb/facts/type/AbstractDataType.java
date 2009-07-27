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
import java.util.Set;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.UndeclaredAnnotationException;
import org.eclipse.imp.pdb.facts.exceptions.UndeclaredConstructorException;

/**
 * A AbstractDataType is an algebraic sort. A sort is produced by constructors, @see NodeType.
 * There can be many constructors for a single sort.
 * 
 * @see ConstructorType
 */
/*package*/ final class AbstractDataType extends Type {
	private final String fName;
	private final Type fParameters;
	
	/* package */ AbstractDataType(String name, Type parameters) {
		fName = name;
		fParameters = parameters;
	}
	
	@Override
	public boolean isAbstractDataType() {
		return true;
	}
	
	@Override
	public boolean isNodeType() {
		return true; // All ADT's are built from nodes.
	}
	
	@Override
	public boolean isParameterized() {
		return !fParameters.isVoidType();
	}
	
	@Override
	public boolean isSubtypeOf(Type other) {
		if (other == this || other.isValueType()) {
			return true;
		}
		else if (other.isAbstractDataType() && other.getName().equals(getName())) {
			return fParameters.isSubtypeOf(other.getTypeParameters());
		}
		else if (other.isAliasType()) {
			return isSubtypeOf(other.getAliased());
		}
		
		return TypeFactory.getInstance().nodeType().isSubtypeOf(other);
	}
	
	@Override
	public Type lub(Type other) {
		if (other == this || other.isVoidType()) {
			return this;
		}
		else if (other.isAbstractDataType() && other.getName().equals(getName())) {
			return TypeFactory.getInstance().abstractDataTypeFromTuple(new TypeStore(), getName(), fParameters.lub(other.getTypeParameters()));
		}
		else if (other.isConstructorType() && other.getAbstractDataType().getName().equals(getName())) {
			return TypeFactory.getInstance().abstractDataTypeFromTuple(new TypeStore(), getName(), fParameters.lub(other.getAbstractDataType().getTypeParameters()));
		}
		else {
			return TypeFactory.getInstance().nodeType().lub(other);
		}
	}
	
	@Override
	public boolean hasField(String fieldName, TypeStore store) {
		// we look up by name because this might be an instantiated parameterized data-type
		// which will not be present in the store.
		
		Type parameterizedADT = store.lookupAbstractDataType(getName());
		for (Type alt : store.lookupAlternatives(parameterizedADT)) {
			if (alt.isConstructorType()) {
				if (alt.hasField(fieldName)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(fName);
		if (!fParameters.isVoidType()) {
			sb.append("[");
			int idx= 0;
			for(Type elemType: fParameters) {
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
		if (o instanceof AbstractDataType) {
			AbstractDataType other = (AbstractDataType) o;
			return fName.equals(other.fName) && fParameters == other.fParameters;
		}
		return false;
	}
	
	@Override
	public Type instantiate(TypeStore store, Map<Type, Type> bindings) {
		Type[] params = new Type[0];
		if (isParameterized()) {
			params = new Type[fParameters.getArity()];
			int i = 0;
			for (Type p : fParameters) {
				params[i] = p.instantiate(store, bindings);
			}
		}
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
	public <T> T accept(ITypeVisitor<T> visitor) {
		return visitor.visitAbstractData(this);
	}
	
	private IValue wrap(IValueFactory vf, TypeStore store, Type wrapped, IValue value) {
		for (Type alt : store.lookupAlternatives(this)) {
			if (alt.getArity() == 1 && alt.getFieldType(0).isSubtypeOf(wrapped)) {
				return alt.make(vf, value);
			}
		}
		
		throw new UndeclaredConstructorException(this, TypeFactory.getInstance().tupleType(wrapped));
	}

	@Override
	public IValue make(IValueFactory vf, TypeStore store, boolean arg) {
		Type wrapped = TypeFactory.getInstance().boolType();
		IValue value = wrapped.make(vf, arg);
		
		return wrap(vf, store, wrapped, value);
	}
	
	@Override
	public IValue make(IValueFactory vf, TypeStore store, int arg) {
		Type wrapped = TypeFactory.getInstance().integerType();
		IValue value = wrapped.make(vf, arg);
		
		return wrap(vf, store, wrapped, value);
	}
	
	@Override
	public IValue make(IValueFactory vf, TypeStore store, double arg) {
		Type wrapped = TypeFactory.getInstance().realType();
		IValue value = wrapped.make(vf, arg);
		
		return wrap(vf, store, wrapped, value);
	}

	
	@Override
	public IValue make(IValueFactory vf, TypeStore store, String arg) {
		Type wrapped = TypeFactory.getInstance().stringType();
		IValue value = wrapped.make(vf, arg);
		
		return wrap(vf, store, wrapped, value);
	}
	
	@Override
	public IValue make(IValueFactory f, TypeStore store, IValue... children) {
		Set<Type> possible = store.lookupAlternatives(this);
		Type childrenTypes = store.getFactory().tupleType(children);
		
		for (Type node : possible) {
			if (childrenTypes.isSubtypeOf(node.getFieldTypes())) {
				return node.make(f, children);
			}
		}
		
		throw new UndeclaredConstructorException(this, childrenTypes);
	}
	
	@Override
	public IValue make(IValueFactory f, TypeStore store, String name, IValue... children) {
		Set<Type> possible = store.lookupConstructor(this, name);
		Type childrenTypes = store.getFactory().tupleType(children);
		
		for (Type node : possible) {
			if (childrenTypes.isSubtypeOf(node.getFieldTypes())) {
				return node.make(f, children);
			}
		}
		
		throw new UndeclaredConstructorException(this, childrenTypes);
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
