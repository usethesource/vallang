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

/**
 * A Tree Sort is an algebraic sort. A sort is produced by constructors, @see TreeType.
 * There can be many constructors for a single sort.
 * 
 * @see TreeNodeType
 */
public class TreeSortType extends Type {
	/* package */ String fName;
	
	/* package */ TreeSortType(String name) {
		fName = name;
	}
	
	@Override
	public boolean isTreeSortType() {
		return true;
	}
	
	/**
	 * @return the first super type of this type that is not a NamedType.
	 */
	public Type getBaseType() {
		return this;
	}

	@Override
	public String getTypeDescriptor() {
            return fName;
	}

	@Override
	public boolean isSubtypeOf(Type other) {
		if (other == this) {
			return true;
		}
		
		return false;
	}

	@Override
	public Type lub(Type other) {
		if (other == this) {
			return this;
		}
		else if (other.isNamedType()) {
    		return other.lub(this);
    	}
		
		return TypeFactory.getInstance().valueType();
	}
	
	@Override
	public String toString() {
		return fName;
	}
	
	@Override
	public int hashCode() {
		return 49991 + 49831 * fName.hashCode() + 67349;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof NamedType) {
			NamedType other = (NamedType) o;
			return fName.equals(other.fName);
		}
		return false;
	}
	
}
