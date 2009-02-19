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

import java.util.Iterator;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.IllegalConstructorApplicationException;
import org.eclipse.imp.pdb.facts.exceptions.UndeclaredAnnotationException;

/**
 * A tree type is a type of tree node, defined by its name, the types of
 * its children and the type it produces. Example tree types would be:
 * 
 * Address ::= dutchAddress(Street, City, Postcode)
 * Address ::= usAddress(Street, City, State, PostalCode)
 * 
 * Here Address is the AbstractDataType, the type a tree produces. dutchAddress
 * and usAddress are the names of the node types and the other capitalized names
 * are the types of the children.
 * 
 * Children types can also be named as in:
 * Boolean ::= and(Boolean lhs, Boolean rhs)
 * Boolean ::= or(Boolean lhs, Boolean rhs)
 * 
 */
/*package*/ final class ConstructorType extends Type {
	protected final TupleType fChildrenTypes;
	protected final AbstractDataType fADT;
	protected final String fName;
	
	/* package */ ConstructorType(String name, TupleType childrenTypes, AbstractDataType adt) {
		fName = name;
		fChildrenTypes = childrenTypes;
		fADT = adt;
	}
	
	@Override
	public boolean isSubtypeOf(Type other) {
		if (other == this || other == fADT) {
			return true;
		}
		else {
			return fADT.isSubtypeOf(other);
		}
	}
	
	@Override
	public Type lub(Type other) {
		if (other.isConstructorType()) {
			return fADT.lub(other.getAbstractDataType());
		}
		else if (other == fADT) {
			return fADT;
		}
		else if (other.isNodeType()) {
			return other;
		}
		else {
			return super.lub(other);
		}
	}

	@Override
	public int hashCode() {
		return 21 + 44927 * ((fName != null) ? fName.hashCode() : 1) + 
		181 * fChildrenTypes.hashCode() + 
		354767453 * fADT.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof ConstructorType) {
			return ((fName == null) ? ((ConstructorType) o).fName == null : fName
					.equals(((ConstructorType) o).fName))
					&& fChildrenTypes == ((ConstructorType) o).fChildrenTypes
					&& fADT == fADT;
		}
		return false;
	}
	
	@Override
	public boolean isConstructorType() {
		return true;
	}
	
	@Override
	public boolean isNodeType() {
		return true; // an ADT constructor is a node
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(fADT);
		builder.append("::=");
		builder.append(fName);
		builder.append("(");

		Iterator<Type> iter = fChildrenTypes.iterator();
		while(iter.hasNext()) {
			builder.append(iter.next());

			if (iter.hasNext()) {
				builder.append(",");
			}
		}
		builder.append(")");

		return builder.toString();
	}
	
	@Override
	public int getArity() {
		return fChildrenTypes.getArity();
	}
	
	@Override
	public int getFieldIndex(String fieldName) throws FactTypeUseException {
		return fChildrenTypes.getFieldIndex(fieldName);
	}
	
	@Override
	public boolean hasField(String fieldName) {
		return fChildrenTypes.hasField(fieldName);
	}
	
	@Override
	public TupleType getFieldTypes() {
		return fChildrenTypes;
	}
	

	@Override
	public String getName() {
		return fName;
	}
	
	@Override
	public Type getAbstractDataType() {
		return fADT;
	}
	
	@Override
	public Type getFieldType(int i) {
		return fChildrenTypes.getFieldType(i);
	}
	
	@Override
	public <T> T accept(ITypeVisitor<T> visitor) {
		return visitor.visitConstructor(this);
	}

	@Override
	public IValue make(IValueFactory f) {
		return f.constructor(this);
	}
	
	@Override
	public IValue make(IValueFactory f, int arg) {
		TypeFactory tf = TypeFactory.getInstance();
		
		if (getArity() == 1 && getFieldType(0).isSubtypeOf(tf.integerType())) {
			return make(f, f.integer(arg));
		}

		throw new IllegalConstructorApplicationException(this, tf.tupleType(tf.integerType()));
	}
	
	@Override
	public IValue make(IValueFactory f, double arg) {
		TypeFactory tf = TypeFactory.getInstance();
		if (getArity() == 1 && getFieldType(0).isSubtypeOf(tf.doubleType())) {
			return make(f, f.dubble(arg));
		}
		
		throw new IllegalConstructorApplicationException(this, tf.tupleType(tf.doubleType()));
	}
	
	@Override
	public IValue make(IValueFactory f, String arg) {
		TypeFactory tf = TypeFactory.getInstance();
		if (getArity() == 1 && getFieldType(0).isSubtypeOf(tf.stringType())) {
			return make(f, f.string(arg));
		}
		
		throw new IllegalConstructorApplicationException(this, tf.tupleType(tf.stringType()));
	}
	
	@Override
	public IValue make(IValueFactory vf, IValue... args) {
		return vf.constructor(this, args);
	}
	
	@Override
	public IValue make(IValueFactory f, String name, IValue... children) {
		if (!name.equals(fName)) {
			throw new UnsupportedOperationException(name + " does not match constructor name " + getName());
		}
		
		Type childrenTypes = TypeFactory.getInstance().tupleType(children);
		if (!childrenTypes.isSubtypeOf(fChildrenTypes)) {
			throw new IllegalConstructorApplicationException(this, childrenTypes);
		}
		
		return make(f, children);
	}
	
	@Override
	public void match(Type matched, Map<Type, Type> bindings)
			throws FactTypeUseException {
		super.match(matched, bindings);
		fADT.match(matched.getAbstractDataType(), bindings);
		getFieldTypes().match(matched.getFieldTypes(), bindings);
	}
	
	@Override
	public Type instantiate(Map<Type, Type> bindings) {
		return TypeFactory.getInstance().constructorFromTuple(fADT.instantiate(bindings), getName(), getFieldTypes().instantiate(bindings));
	}
	
	@Override
	public boolean declaresAnnotation(String label) {
		return TypeFactory.getInstance().getAnnotationType(this, label) != null;
	}
	
	@Override
	public Type getAnnotationType(String label) throws FactTypeUseException {
		Type type = TypeFactory.getInstance().getAnnotationType(this, label);
		
		if (type == null) {
			throw new UndeclaredAnnotationException(getAbstractDataType(), label);
		}
		
		return type;
	}
}
