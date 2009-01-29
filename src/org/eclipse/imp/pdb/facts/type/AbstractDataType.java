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

import java.util.List;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;

/**
 * A AbstractDataType is an algebraic sort. A sort is produced by constructors, @see NodeType.
 * There can be many constructors for a single sort.
 * 
 * @see ConstructorType
 */
/*package*/ final class AbstractDataType extends Type {
	/* package */ final String fName;
	/* package */ final Type fParameters;
	
	/* package */ AbstractDataType(String name, Type parameters) {
		fName = name;
		fParameters = parameters;
	}
	
	@Override
	public boolean isAbstractDataType() {
		return true;
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
		else if (other.isAliasType()) {
			return isSubtypeOf(other.getAliased());
		}
		
		return TypeFactory.getInstance().nodeType().isSubtypeOf(other);
	}
	
	@Override
	public Type lub(Type other) {
		if (other == this) {
			return this;
		}
		else if (other.isConstructorType() && other.getAbstractDataType() == this) {
			return this;
		}
		else {
			return TypeFactory.getInstance().nodeType().lub(other);
		}
	}
	
	@Override
	public boolean hasField(String fieldName) {
		for (Type alt : TypeFactory.getInstance().lookupAlternatives(this)) {
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
	public Type instantiate(Map<Type, Type> bindings) {
		Type[] params = new Type[0];
		if (isParameterized()) {
			params = new Type[fParameters.getArity()];
			int i = 0;
			for (Type p : fParameters) {
				params[i] = p.instantiate(bindings);
			}
		}
		return TypeFactory.getInstance().abstractDataType(fName, params);
	}
	
	@Override
	public String getName() {
		return fName;
	}
	
	@Override
	public <T> T accept(ITypeVisitor<T> visitor) {
		return visitor.visitAbstractData(this);
	}
	
	private IValue wrap(IValueFactory vf, Type wrapped, IValue value) {
		for (Type alt : TypeFactory.getInstance().lookupAlternatives(this)) {
			if (alt.getArity() == 1 && alt.getFieldType(0).isSubtypeOf(wrapped)) {
				return alt.make(vf, value);
			}
		}
		
		throw new FactTypeError("This adt, " + this + ", does not have any constructor to wrap a " + wrapped);
	}
	
	@Override
	public IValue make(IValueFactory vf, int arg) {
		Type wrapped = TypeFactory.getInstance().integerType();
		IValue value = wrapped.make(vf, arg);
		
		return wrap(vf, wrapped, value);
	}
	
	@Override
	public IValue make(IValueFactory vf, double arg) {
		Type wrapped = TypeFactory.getInstance().doubleType();
		IValue value = wrapped.make(vf, arg);
		
		return wrap(vf, wrapped, value);
	}

	
	@Override
	public IValue make(IValueFactory vf, String arg) {
		Type wrapped = TypeFactory.getInstance().stringType();
		IValue value = wrapped.make(vf, arg);
		
		return wrap(vf, wrapped, value);
	}
	
	@Override
	public IValue make(IValueFactory f, String name, IValue... children) {
		List<Type> possible = TypeFactory.getInstance().lookupConstructor(this, name);
		Type childrenTypes = TypeFactory.getInstance().tupleType(children);
		
		for (Type node : possible) {
			if (childrenTypes.isSubtypeOf(node.getFieldTypes())) {
				return node.make(f, children);
			}
		}
		
		throw new FactTypeError("This adt does not have a constructor with this signature: " + name + ":" + childrenTypes);
	}
	
	@Override
	public boolean declaresAnnotation(String label) {
		return TypeFactory.getInstance().getAnnotationType(this, label) != null;
	}
	
	@Override
	public Type getAnnotationType(String label) throws FactTypeError {
		Type type = TypeFactory.getInstance().getAnnotationType(this, label);
		
		if (type == null) {
			throw new FactTypeError("This type does not have an annotation labeled " + label);
		}
		
		return type;
	}
}
