/*******************************************************************************
* Copyright (c) 2007, 2008 IBM Corporation & CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.pdb.facts.impl.hash;

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.IRelationWriter;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.ITree;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.BaseValueFactory;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.TreeNodeType;
import org.eclipse.imp.pdb.facts.type.TupleType;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

public class ValueFactory extends BaseValueFactory {
	private static final ValueFactory sInstance = new ValueFactory();
	public static ValueFactory getInstance() {
		return sInstance;
	}

	private ValueFactory() {
	}

	public IRelation relation(TupleType tupleType) {
		return new Relation(TypeFactory.getInstance().relType(tupleType));
	}
	
	public IRelation relation(IValue... rest) {
		Type elementType = TypeFactory.getInstance().voidType();
		for (IValue elem : rest) {
			elementType = (TupleType) elementType.lub(elem.getType());
		}
		Relation result = (Relation) TypeFactory.getInstance().relType((TupleType) elementType.getBaseType()).make(this);
		IRelationWriter rw = result.getWriter();
		
		if (!elementType.getBaseType().isTupleType()) {
			throw new FactTypeError("elements are not tuples");
		}
			
		for (IValue elem : rest) {
			rw.insert((ITuple) elem);
		}
		
		rw.done();
		return result;
	}

	public ISet set(Type eltType) {
		return new Set(eltType);
	}

	public ISet set(IValue... rest) throws FactTypeError {
		Type elementType = TypeFactory.getInstance().voidType();
		
		for (IValue elem : rest) {
			elementType = elementType.lub(elem.getType());
		}
		Set result = (Set) TypeFactory.getInstance().setType(elementType).make(this);
		ISetWriter sw = result.getWriter();
		for (IValue elem : rest) {
			sw.insert(elem);
		}
		
		sw.done();

		return result;
	}

	public IList list(Type eltType) {
		return new List(eltType);
	}

	public IList list(IValue... rest) {
		Type elementType = TypeFactory.getInstance().voidType();
		for (IValue elem : rest) {
			elementType = elementType.lub(elem.getType());
		}
		
		List result = (List) TypeFactory.getInstance().listType(elementType).make(this);
		IListWriter lw = result.getWriter();
		for (IValue elem : rest) {
			lw.append(elem);
		}

		lw.done();
		return result;
	}

	public ITuple tuple() {
		return new Tuple(new IValue[0]);
	}
	
	public ITuple tuple(IValue... args) {
		IValue[] tmp = new IValue[args.length];
		System.arraycopy(args, 0, tmp, 0, args.length);
		return new Tuple(tmp);
	}
	
	public ITree tree(TreeNodeType type, IValue... children) {
		return new Tree(this, type, children);
	}
	
	public ITree tree(TreeNodeType type, java.util.List<IValue> children) {
		return new Tree(this, type, children);
	}
	
	public ITree tree(TreeNodeType type) {
		return new Tree(this, type);
	}

	public IMap map(Type key, Type value) {
		return new Map(key, value);
	}
}
