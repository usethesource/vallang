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
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.IRelation;
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
	
	public IRelation relation(IValue... tuples) {
		Type elementType = lub(tuples);
	
		if (!elementType.getBaseType().isTupleType()) {
			throw new FactTypeError("elements are not tuples");
		}
		
		ISetWriter rw = setWriter((TupleType) elementType);
		rw.insert(tuples);
		return (IRelation) rw.done();
	}
	
	public ISetWriter relationWriter(TupleType tupleType) {
		return new Set.SetWriter(tupleType);
	}

	public ISet set(Type eltType) {
		if (eltType.getBaseType().isTupleType()) {
			return new Relation(eltType);
		}
		else {
		  return new Set(eltType);
		}
	}
	
	public ISetWriter setWriter(Type eltType) {
		return new Set.SetWriter(eltType);
	}

	public ISet set(IValue... elems) throws FactTypeError {
		Type elementType = lub(elems);
		
		ISetWriter sw = new Set.SetWriter(elementType);
		sw.insert(elems);
		return sw.done();
	}

	public IList list(Type eltType) {
		return new List(eltType);
	}
	
	public IListWriter listWriter(Type eltType) {
		return new List.ListWriter(eltType);
	}

	public IList list(IValue... rest) {
		Type elementType = lub(rest);
		IListWriter lw = new List.ListWriter(elementType);
		lw.append(rest);
		return lw.done();
	}

	private Type lub(IValue... elems) {
		Type elementType = TypeFactory.getInstance().voidType();
		for (IValue elem : elems) {
			elementType = elementType.lub(elem.getType());
		}
		return elementType;
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
	
	public IMapWriter mapWriter(Type key, Type value) {
		return new Map.MapWriter(key, value);
	}
}
