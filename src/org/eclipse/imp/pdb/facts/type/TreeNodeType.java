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

/**
 * A tree type is a type of tree node, defined by its name, the types of
 * its children and the type it produces. Example tree types would be:
 * 
 * Address ::= dutchAddress(Street, City, Postcode)
 * Address ::= usAddress(Street, City, State, PostalCode)
 * 
 * Here Address is the NamedTreeType, the type a tree produces. dutchAddress
 * and usAddress are the names of the node types and the other capitalized names
 * are the types of the children.
 * 
 * Children types can also be named as in:
 * Boolean ::= and(Boolean lhs, Boolean rhs)
 * Boolean ::= or(Boolean lhs, Boolean rhs)
 * 
 */
/*package*/ final class TreeNodeType extends Type {
	protected final TupleType fChildrenTypes;
	protected final NamedTreeType fTreeType;
	protected final String fName;
	
	/* package */ TreeNodeType(String name, TupleType childrenTypes, NamedTreeType treeType) {
		fName = name;
		fChildrenTypes = childrenTypes;
		fTreeType = treeType;
	}
	
	@Override
	public boolean isSubtypeOf(Type other) {
		if (other == this || other == fTreeType) {
			return true;
		}
		else {
			return fTreeType.isSubtypeOf(other);
		}
	}
	
	@Override
	public Type lub(Type other) {
		if (other.isTreeNodeType()) {
			return fTreeType.lub(other.getSuperType());
		}
		else {
			return super.lub(other);
		}
	}

	@Override
	public int hashCode() {
		return 21 + 44927 * ((fName != null) ? fName.hashCode() : 1) + 
		181 * fChildrenTypes.hashCode() + 
		354767453 * fTreeType.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof TreeNodeType) {
			return ((fName == null) ? ((TreeNodeType) o).fName == null : fName
					.equals(((TreeNodeType) o).fName))
					&& fChildrenTypes == ((TreeNodeType) o).fChildrenTypes
					&& fTreeType == fTreeType;
		}
		return false;
	}
	
	@Override
	public boolean isTreeNodeType() {
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(fTreeType);
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
	public int getFieldIndex(String fieldName) throws FactTypeError {
		return fChildrenTypes.getFieldIndex(fieldName);
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
	public Type getSuperType() {
		return fTreeType;
	}
	
	@Override
	public Type getFieldType(int i) {
		return fChildrenTypes.getFieldType(i);
	}
	
	@Override
	public <T> T accept(ITypeVisitor<T> visitor) {
		return visitor.visitTreeNode(this);
	}

	@Override
	public IValue make(IValueFactory f) {
		return f.tree(this);
	}
	
	@Override
	public IValue make(IValueFactory vf, int arg) {
		return vf.tree(this, vf.integer(arg));
	}
	
	@Override
	public IValue make(IValueFactory vf, double arg) {
		return vf.tree(this, vf.dubble(arg));
	}
	
	@Override
	public IValue make(IValueFactory vf, String arg) {
		return vf.tree(this, vf.string(arg));
	}
	
	@Override
	public IValue make(IValueFactory vf, IValue... args) {
		if (fName == null) { // anonymous constructor
			return vf.tree(this, fChildrenTypes.getFieldType(0).make(vf, args));
		}
		else {
		  return vf.tree(this, args);
		}
	}
	
	@Override
	public IValue make(IValueFactory f, String name, IValue... children) {
		if (!name.equals(fName)) {
			throw new FactTypeError("This constructor has a different name from: " + name);
		}
		
		Type childrenTypes = TypeFactory.getInstance().tupleType(children);
		if (!childrenTypes.isSubtypeOf(fChildrenTypes)) {
			throw new FactTypeError("These children don't fit this tree node: " + fChildrenTypes);
		}
		
		return make(f, children);
	}
	
	@Override
	public void match(Type matched, Map<Type, Type> bindings)
			throws FactTypeError {
		super.match(matched, bindings);
		getSuperType().match(matched.getSuperType(), bindings);
		getFieldTypes().match(matched.getFieldTypes(), bindings);
	}
	
	@Override
	public Type instantiate(Map<Type, Type> bindings) {
		return TypeFactory.getInstance().treeNodeType((NamedTreeType) getSuperType().instantiate(bindings), getName(), (TupleType) getFieldTypes().instantiate(bindings));
	}
}
