/*******************************************************************************
* Copyright (c) 2007, 2008 IBM Corporation & CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
*    Jurgen Vinju (jurgen@vinju.org)         
*******************************************************************************/

package org.eclipse.imp.pdb.facts.impl.hash;

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.IRelationWriter;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.ITree;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.BaseValueFactory;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

/**
 * This is a reference implementation for an @{link IValueFactory}. It uses
 * the Java standard library to implement it in a most straightforward but
 * not necessarily very efficient manner.
 *
 */
public class ValueFactory extends BaseValueFactory {
	private static final ValueFactory sInstance = new ValueFactory();
	public static ValueFactory getInstance() {
		return sInstance;
	}

	private ValueFactory() {
		super();
	}

	public IRelation relation(Type tupleType) {
		return relationWriter(tupleType).done();
	}
	
	public IRelation relation(IValue... tuples) {
		Type elementType = lub(tuples);
	
		if (!elementType.isTupleType()) {
			throw new FactTypeError("elements are not tuples");
		}
		
		ISetWriter rw = setWriter(elementType);
		rw.insert(tuples);
		return (IRelation) rw.done();
	}
	
	public IRelationWriter relationWriter(Type tupleType) {
		return Relation.createRelationWriter(tupleType);
	}

	public ISet set(Type eltType){
		return setWriter(eltType).done();
	}
	
	public ISetWriter setWriter(Type eltType) {
		if (eltType.isTupleType()) {
			return relationWriter(eltType);
		}
		else {
		  return Set.createSetWriter(eltType);
		}
	}

	public ISet set(IValue... elems) throws FactTypeError {
		Type elementType = lub(elems);
		
		ISetWriter sw = setWriter(elementType);
		sw.insert(elems);
		return sw.done();
	}

	public IList list(Type eltType) {
		return listWriter(eltType).done();
	}
	
	public IListWriter listWriter(Type eltType) {
		return List.createListWriter(eltType);
	}

	public IList list(IValue... rest) {
		Type eltType = lub(rest);
		IListWriter lw =  listWriter(eltType);
		lw.append(rest);
		return lw.done();
	}

	private static Type lub(IValue... elems) {
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
	
	public ITree tree(String name) {
		return new Tree(name);
	}
	
	public ITree tree(String name, IValue... children) {
		return new Tree(name, children);
	}
	
	public INode tree(Type treeNodeType, IValue... children) {
		return new Node(treeNodeType, children);
	}
	
	public INode tree(Type treenodetype) {
		return new Node(treenodetype);
	}

	public IMap map(Type keyType, Type valueType) {
		return mapWriter(keyType, valueType).done();
	}
	
	public IMapWriter mapWriter(Type keyType, Type valueType) {
		return Map.createMapWriter(keyType, valueType);
	}
}