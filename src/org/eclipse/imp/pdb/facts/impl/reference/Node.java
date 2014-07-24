/*******************************************************************************
 * Copyright (c) 2007-2013 IBM Corporation & CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.reference;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.impl.AbstractNode;
import org.eclipse.imp.pdb.facts.impl.func.NodeFunctions;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.util.ArrayIterator;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;

/**
 * Naive implementation of an untyped tree node, using array of children.
 */
/*package*/ class Node extends AbstractNode implements INode {
	protected final static Type VALUE_TYPE = TypeFactory.getInstance().valueType();

	@Override
	public Type getType() {
		return fType;
	}

	protected final Type fType;
	protected final IValue[] fChildren;
	protected final String fName;
	protected int fHash = 0;
	protected final String[] keyArgNames;
	
	/*package*/ Node(String name, IValue[] children) {
		super();
		fType = TypeFactory.getInstance().nodeType();
		fName = name;
		fChildren = children.clone();
		keyArgNames = null;
	}
		
	protected Node(String name, Type type, IValue[] children) {
		super();
		fType = type;
		fName = name;
		fChildren = children.clone();
		keyArgNames = null;
	}
	
	/*package*/ Node(String name, IValue[] children, Map<String, IValue> keyArgValues){
		super();
		fType = TypeFactory.getInstance().nodeType();
		fName = (name != null ? name.intern() : null); // Handle (weird) special case.
		if(keyArgValues != null){
			int nkw = keyArgValues.size();
			IValue[] extendedChildren = new IValue[children.length + nkw];
			for(int i = 0; i < children.length;i++){
				extendedChildren[i] = children[i];
			}

			String keyArgNames[]= new String[nkw];
			int k = 0;
			for(String kw : keyArgValues.keySet()){
				keyArgNames[k++] = kw;
			}
			for(int i = 0; i < nkw; i++){
				extendedChildren[children.length + i] = keyArgValues.get(keyArgNames[i]);
			}
			this.fChildren = extendedChildren;
			this.keyArgNames = keyArgNames;
		} else {
			this.fChildren = children;
			this.keyArgNames = null;
		}
	}
				
	/*package*/ Node(String name) {
		this(name, new IValue[0]);
	}

	/**
	 * Replaces a child
	 * @param other
	 * @param index
	 * @param newChild
	 */
	protected Node(Node other, int index, IValue newChild) {
		super();
		fType = other.fType;
		fName = other.fName;
		fChildren = other.fChildren.clone();
		fChildren[index] = newChild;
		keyArgNames = null;
	}
	
	@Override
	public <T, E extends Throwable> T accept(IValueVisitor<T,E> v) throws E {
		return v.visitNode(this);
	}

	@Override
	public int arity() {
		return fChildren.length;
	}

	@Override
	protected IValueFactory getValueFactory() {
		return ValueFactory.getInstance();
	}

	@Override
	public IValue get(int i) throws IndexOutOfBoundsException {
		try {
		 return fChildren[i];
		}
		catch (ArrayIndexOutOfBoundsException e) {
			throw new IndexOutOfBoundsException("Node node does not have child at pos " + i);
		}
	}

	@Override
	public Iterable<IValue> getChildren() {
		return this;
	}

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public  INode set(int i, IValue newChild) throws IndexOutOfBoundsException {
		try {
			return new Node(this, i, newChild);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IndexOutOfBoundsException("Node node does not have child at pos " + i);
		}
	}

	@Override
	public Iterator<IValue> iterator() {
		return ArrayIterator.of(fChildren);
	}
	
	@Override
	public boolean equals(Object obj) {
		  if(this == obj) {
			  return true;
		  }
		  else if(obj == null) {
			  return false;
		  }
		  else if (getClass() == obj.getClass()) {
			Node other = (Node) obj;
			
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
	public boolean isEqual(IValue value){
		return NodeFunctions.isEqual(getValueFactory(), this, value);
	}
	
	public int computeHashCode() {
       int hash = fName != null ? fName.hashCode() : 0;
       
	   for (int i = 0; i < fChildren.length; i++) {
	     hash = (hash << 1) ^ (hash >> 1) ^ fChildren[i].hashCode();
	   }
	   return hash;
	}

	@Override
	public int hashCode() {
		if (fHash == 0) {
			fHash = computeHashCode();
		}
		return fHash;
	}

}
