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

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;

/**
 * A NamedTreeType is an algebraic sort. A sort is produced by constructors, @see TreeType.
 * There can be many constructors for a single sort.
 * 
 * @see TreeNodeType
 */
/*package*/ final class NamedTreeType extends Type {
	/* package */ final String fName;
	
	/* package */ NamedTreeType(String name) {
		fName = name;
	}
	
	@Override
	public boolean isNamedTreeType() {
		return true;
	}
	
	@Override
	public boolean isSubtypeOf(Type other) {
		if (other.isTreeType()) {
			return true;
		}
		else {
			return super.isSubtypeOf(other);
		}
	}
	
	@Override
	public boolean isTreeType() {
		return true;
	}

	@Override
	public Type lub(Type other) {
		if (other.isTreeType()) {
			return other;
		}
		else {
			return super.lub(other);
		}
	}
	
	@Override
	public String toString() {
		return fName;
	}
	
	@Override
	public int hashCode() {
		return 49991 + 49831 * fName.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof NamedTreeType) {
			NamedTreeType other = (NamedTreeType) o;
			return fName.equals(other.fName);
		}
		return false;
	}
	
	@Override
	public String getName() {
		return fName;
	}
	
	@Override
	public <T> T accept(ITypeVisitor<T> visitor) {
		return visitor.visitNamedTree(this);
	}
	
	@Override
	public IValue make(IValueFactory vf, int arg) {
		TypeFactory tf = TypeFactory.getInstance();
		Type node = tf.lookupAnonymousTreeNodeType(this);
		
		if (node != null) {
			return node.make(vf, arg);
		}
		else {
			throw new FactTypeError("This sort does not have an anonymous int constructor: " + this);
		}
	}
	
	@Override
	public IValue make(IValueFactory vf, double arg) {
		TypeFactory tf = TypeFactory.getInstance();
		Type node = tf.lookupAnonymousTreeNodeType(this);
		
		if (node != null) {
			return node.make(vf, arg);
		}
		else {
			throw new FactTypeError("This sort does not have an anonymous double constructor: " + this);
		}
	}
	
	@Override
	public IValue make(IValueFactory vf, String arg) {
		TypeFactory tf = TypeFactory.getInstance();
		Type node = tf.lookupAnonymousTreeNodeType(this);
		
		if (node != null) {
			return node.make(vf, arg);
		}
		else {
			throw new FactTypeError("This sort does not have an anonymous string constructor: " + this);
		}
	}
	
	@Override
	public IValue make(IValueFactory f, String name, IValue... children) {
		List<Type> possible = TypeFactory.getInstance().lookupTreeNodeType(this, name);
		Type childrenTypes = TypeFactory.getInstance().tupleType(children);
		
		for (Type node : possible) {
			if (childrenTypes.isSubtypeOf(node.getFieldTypes())) {
				return node.make(f, children);
			}
		}
		
		throw new FactTypeError("This sort does not have a constructor with this signature: " + name + ":" + childrenTypes);
	}
}
