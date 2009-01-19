/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   jurgen@vinju.org
*******************************************************************************/

package org.eclipse.imp.pdb.facts.impl.reference;

import java.util.Iterator;

import org.eclipse.imp.pdb.facts.ITree;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.Value;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * Naive implementation of an untyped tree node, using array of children.
 */
public class Tree extends Value implements ITree {
    protected final IValue[] fChildren;
    protected final String fName;
	
	/*package*/ Tree(String name, IValue[] children) {
		super(TypeFactory.getInstance().treeType());
		fName = name;
		fChildren = new IValue[children.length];
		System.arraycopy(children, 0, fChildren, 0, children.length);
	}
	
	protected Tree(String name, Type type, IValue[] children) {
		super(type);
		fName = name;
		fChildren = new IValue[children.length];
		System.arraycopy(children, 0, fChildren, 0, children.length);
	}
	
	/*package*/ Tree(String name) {
		this(name, new IValue[0]);
	}

	protected Tree(Tree other, int index, IValue newChild) {
		super(other);
		fName = other.fName;
		fChildren = other.fChildren.clone();
		fChildren[index] = newChild;
	}
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException {
		return v.visitTree(this);
	}

	public int arity() {
		return fChildren.length;
	}

	public IValue get(int i) throws IndexOutOfBoundsException {
		try {
		 return fChildren[i];
		}
		catch (ArrayIndexOutOfBoundsException e) {
			throw new IndexOutOfBoundsException("Tree node does not have child at pos " + i);
		}
	}

	public Iterable<IValue> getChildren() {
		return this;
	}

	public String getName() {
		return fName;
	}

	public  ITree set(int i, IValue newChild) throws IndexOutOfBoundsException {
		try {
			return new Tree(this, i, newChild);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IndexOutOfBoundsException("Tree node does not have child at pos " + i);
		}
	}

	public Iterator<IValue> iterator() {
		return new Iterator<IValue>() {
			private int i = 0;

			public boolean hasNext() {
				return i < fChildren.length;
			}

			public IValue next() {
				return fChildren[i++];
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		if (fName == null) {
			// if no name there is always exactly one child
			builder.append(fChildren[0].toString());
		}
		else {
			builder.append(fName);
			builder.append("(");

			Iterator<IValue> it = iterator();
			while (it.hasNext()) {
				builder.append(it.next().toString());
				if (it.hasNext()) {
					builder.append(",");
				}
			}
			builder.append(")");
		}
		
		return builder.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (getClass() == obj.getClass()) {
			Tree other = (Tree) obj;
			
			if (!fType.comparable(other.fType)) {
				return false;
			}
			
			if (fChildren.length != other.fChildren.length) {
				return false;		
			}
			
			if (fName == other.fName || (fName != null && fName.equals(other.fName))) {
				for (int i = 0; i < fChildren.length; i++) {
					if (!fChildren[i].equals(other.fChildren[i])) {
						return false;
					}
				}
			
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
       int hash = fName != null ? fName.hashCode() : 0;
       
	   for (int i = 0; i < fChildren.length; i++) {
	     hash = (hash << 1) ^ (hash >> 1) ^ fChildren[i].hashCode();
	   }
	   return hash;
	}
}
