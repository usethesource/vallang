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

/**
 * A tree type is a type of tree node, defined by its name, the types of
 * its children and the type it produces. Example tree types would be:
 * 
 * Address ::= dutchAddress(Street, City, Postcode)
 * Address ::= usAddress(Street, City, State, PostalCode)
 * 
 * Here Address is the TreeSortType, the type a tree produces, dutchAddress
 * and usAddress are the names of the node types and the other capitalized names
 * are the types of the children.
 * 
 * Children types can also be named as in:
 * Boolean ::= and(Boolean lhs, Boolean rhs)
 * Boolean ::= or(Boolean lhs, Boolean rhs)
 * 
 */
public class TreeNodeType extends Type {
	protected TupleType fChildrenTypes;
	protected TreeSortType fNodeType;
	protected String fName;
	
	/* package */ TreeNodeType(String name, TupleType childrenTypes, TreeSortType nodeType) {
		fName = name;
		fChildrenTypes = childrenTypes;
		fNodeType = nodeType;
	}
	
	@Override
	public String getTypeDescriptor() {
	  StringBuilder builder = new StringBuilder();
	  builder.append(fNodeType.getTypeDescriptor());
	  builder.append("::=");
	  builder.append(fName);
	  builder.append("(");
	  
	  Iterator<Type> iter = fChildrenTypes.iterator();
	  while(iter.hasNext()) {
		  builder.append(iter.next().getTypeDescriptor());
		  
		  if (iter.hasNext()) {
			  builder.append(",");
		  }
	  }
	  builder.append(")");
	  
	  return builder.toString();
	}

	@Override
	public boolean isSubtypeOf(Type other) {
		if (other == this) {
			return true;
		}
		else if (other == fNodeType) {
			return true;
		}
		else {
			return fNodeType.isSubtypeOf(other);
		}
	}

	@Override
	public Type lub(Type other) {
		if (this == other) {
			return this;
		}
		else if (other.isTreeNodeType()) {
			return fNodeType.lub(((TreeNodeType) other).fNodeType);
		}
		
		return TypeFactory.getInstance().valueType();
	}

	@Override
	public int hashCode() {
		return 21 + 44927 * ((fName != null) ? fName.hashCode() : 1) + 
		181 * fChildrenTypes.hashCode() + 
		354767453 * fNodeType.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof TreeNodeType) {
			return ((fName == null) ? ((TreeNodeType) o).fName == null : fName
					.equals(((TreeNodeType) o).fName))
					&& fChildrenTypes == ((TreeNodeType) o).fChildrenTypes
					&& fNodeType == fNodeType;
		}
		return false;
	}
	
	@Override
	public boolean isTreeNodeType() {
		return true;
	}
	
	@Override
	public String toString() {
		return getTypeDescriptor();
	}
	
	public int getArity() {
		return fChildrenTypes.getArity();
	}
	
	public int getChildIndex(String fieldName) {
		return fChildrenTypes.getFieldIndex(fieldName);
	}
	
	public TupleType getChildrenTypes() {
		return fChildrenTypes;
	}

	public String getName() {
		return fName;
	}
	
	public TreeSortType getTreeSortType() {
		return fNodeType;
	}
	
	public Type getChildType(int i) {
		return fChildrenTypes.getFieldType(i);
	}
	
	@Override
	public Type getBaseType() {
		return fNodeType;
	}
}
