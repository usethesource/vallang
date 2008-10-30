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

package org.eclipse.imp.pdb.facts.impl.hash;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.imp.pdb.facts.ITree;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.impl.Value;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.NamedType;
import org.eclipse.imp.pdb.facts.type.TreeNodeType;
import org.eclipse.imp.pdb.facts.type.TupleType;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * Naive implementation of a typed tree node, using array of children.
 * 
 *
 */
public class Tree extends Value implements ITree {
    protected ArrayList<IValue> fChildren;
    protected IValueFactory fFactory;
    protected TreeNodeType fType;
    
	/*package*/ Tree(IValueFactory factory, NamedType type, IValue[] children) {
		this(factory, (TreeNodeType) type.getBaseType(), children);
		
	}
	
	/*package*/ Tree(IValueFactory factory, TreeNodeType type, IValue[] children) {
		super(type);
		fType = type;
		fFactory = factory;
		
		if (children != null) {
			fChildren = new ArrayList<IValue>(children.length);
			for (IValue child : children) {
				fChildren.add(child);
			}
		}
	}

	public Tree(IValueFactory factory, TreeNodeType type) {
		super(type);
		fType = type;
		fFactory = factory;
		fChildren = new ArrayList<IValue>();
	}

	public Tree(IValueFactory factory, NamedType type) {
		this(factory, (TreeNodeType) type.getBaseType());
	}

	
	public Tree(ValueFactory factory, TreeNodeType type, List<IValue> children) {
		super(type);
		fType = type;
		fFactory = factory;
		
		fChildren = new ArrayList<IValue>();
		fChildren.addAll(children);
	}

	public IValue accept(IValueVisitor v) throws VisitorException {
		return v.visitTree(this);
	}

	public int arity() {
		return ((TreeNodeType) fType).getArity();
	}

	public IValue get(int i) {
		try {
		 return fChildren.get(i);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			throw new FactTypeError("Tree node does not have child at pos " + i, e);
		}
	}

	public IValue get(String label) {
		return get(((TreeNodeType) fType).getChildIndex(label));
	}

	public Iterable<IValue> getChildren() {
		return this;
	}

	public TupleType getChildrenTypes() {
		return ((TreeNodeType) fType).getChildrenTypes();
	}

	public String getName() {
		return ((TreeNodeType) fType).getName();
	}

	public TreeNodeType getTreeNodeType() {
		return fType;
	}
	
	@Override
	/**
	 * returns the type this tree node produces
	 */
	public Type getType() {
		return fType.getTreeSortType();
	}

	@SuppressWarnings("unchecked")
	public  ITree set(int i, IValue newChild) {
		Tree tmp = new Tree(fFactory, fType, null);
	    tmp.fChildren = (ArrayList<IValue>) fChildren.clone();
		tmp.fChildren.set(i, newChild);
		return tmp;
	}
	
	public ITree set(String label, IValue newChild) {
		return set(((TreeNodeType) fType).getChildIndex(label), newChild);
	}

	public Iterator<IValue> iterator() {
		return fChildren.iterator();
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		String name = fType.getName();
		
		if (name != null) {
			builder.append(name);
		}
		
		if (name != null) {
			if (fChildren.size() > 0) {
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
		} else {
			// anonymous constructors always have one child
			builder.append(fChildren.get(0).toString());
		}
		
		return builder.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Tree) {
			Tree other = (Tree) obj;
		   return fType == 	other.fType && fChildren.equals(other.fChildren);
		}
		else {
			return false;
		}
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		Tree tmp;
		
		if (getType() instanceof NamedType) {
			tmp =  new Tree(fFactory, (NamedType) getType());
		}
		else {
			tmp = new Tree(fFactory, getTreeNodeType());
		}
		
		// no need to clone children, since IValues are immutable
		tmp.fChildren = fChildren;
		return tmp;
	}
}
