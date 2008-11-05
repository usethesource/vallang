/*******************************************************************************
* Copyright (c) 2008 CWI.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Jurgen Vinju - jurgen@vinju.org

*******************************************************************************/

package org.eclipse.imp.pdb.facts.type;

import org.eclipse.imp.pdb.facts.IValue;

/** 
 * The void type represents an empty collection of values. I.e. it 
 * is a subtype of all types, the bottom of the type hierarchy.
 * 
 * This type does not have any values with it naturally and can,
 * for example, be used to elegantly initialize computations that 
 * involve least upper bounds.
 */
public class VoidType extends Type {

	private static class InstanceHolder {
		static VoidType sInstance = new VoidType(); 
	}
	
	private VoidType() { }
	
	@Override
	public IValue accept(ITypeVisitor visitor) {
		return visitor.visitVoid(this);
	}

	@Override
	public boolean isSubtypeOf(Type other) {
		// the empty set is a subset of all sets,
		// and so the void type is a subtype of all types.
		return true;
	}

	@Override
	public Type lub(Type other) {
		// since void is the subtype of all other types
		// the lub is trivially the other type
		return other;
	}
	
	@Override
	public String toString() {
		return "void";
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof VoidType;
	}
	
	@Override
	public int hashCode() {
		return 199; 
	}

	public static VoidType getInstance() {
		return InstanceHolder.sInstance;
	}
}
