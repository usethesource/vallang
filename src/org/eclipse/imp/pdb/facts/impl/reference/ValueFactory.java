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

package org.eclipse.imp.pdb.facts.impl.reference;

import java.util.HashMap;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListRelation;
import org.eclipse.imp.pdb.facts.IListRelationWriter;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.IRelationWriter;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedElementTypeException;
import org.eclipse.imp.pdb.facts.impl.BaseValueFactory;
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

	private void checkNull(Object ...args ) {
		for (Object a : args) {
			if (a == null) {
				throw new NullPointerException();
			}
		}
	}
	
	public IRelation relation(Type tupleType) {
		checkNull(tupleType);
		return relationWriter(tupleType).done();
	}
	
	public IRelation relation(IValue... tuples) {
		checkNull((Object[]) tuples);
		Type elementType = lub(tuples);
	
		if (!elementType.isTupleType()) {
			TypeFactory tf = TypeFactory.getInstance();
			throw new UnexpectedElementTypeException(tf.tupleType(tf.voidType()), elementType);
		}
		
		ISetWriter rw = setWriter(elementType);
		rw.insert(tuples);
		return (IRelation) rw.done();
	}
	
	public IRelationWriter relationWriter(Type tupleType) {
		checkNull(tupleType);
		return Relation.createRelationWriter(tupleType);
	}
	
	public IRelationWriter relationWriter() {
		return Relation.createRelationWriter();
	}

	public ISet set(Type eltType){
		checkNull(eltType);
		return setWriter(eltType).done();
	}
	
	public ISetWriter setWriter(Type eltType) {
		checkNull(eltType);
		if (eltType.isTupleType()) {
			return relationWriter(eltType);
		}
		
		return Set.createSetWriter(eltType);
	}
	
	public ISetWriter setWriter() {
		return Set.createSetWriter();
	}

	public ISet set(IValue... elems) throws FactTypeUseException {
		checkNull((Object[]) elems);
		Type elementType = lub(elems);
		
		ISetWriter sw = setWriter(elementType);
		sw.insert(elems);
		return sw.done();
	}

	public IList list(Type eltType) {
		checkNull(eltType);
		return listWriter(eltType).done();
	}
	
	public IListWriter listWriter(Type eltType) {
		checkNull(eltType);
		return List.createListWriter(eltType);
	}
	
	public IListWriter listWriter() {
		return List.createListWriter();
	}

	public IList list(IValue... rest) {
		checkNull((Object[]) rest);
		Type eltType = lub(rest);
		IListWriter lw =  listWriter(eltType);
		lw.append(rest);
		return lw.done();
	}

	private Type lub(IValue... elems) {
		checkNull((Object[]) elems);
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
		checkNull((Object[]) args);
		
		return new Tuple(args.clone());
	}
	
	public INode node(String name) {
		checkNull(name);
		return new Node(name);
	}
	
	public INode node(String name, java.util.Map<String, IValue> annotations, IValue... children) {
		checkNull(name);
		checkNull((Object[]) children);
		return new Node(name, annotations, children);
	}
	
	public INode node(String name, IValue... children) {
		checkNull(name);
		checkNull((Object[]) children);
		return new Node(name, children);
	}
	
	public IConstructor constructor(Type constructorType, IValue... children) {
		checkNull(constructorType);
		checkNull((Object[]) children);
		java.util.Map<Type, Type> bindings = new HashMap<Type,Type>();
		TypeFactory tf = TypeFactory.getInstance();
		Type params = constructorType.getAbstractDataType().getTypeParameters();
		for (Type p : params) {
			if (p.isParameterType()) {
				bindings.put(p, tf.voidType());
			}
		}
		constructorType.getFieldTypes().match(tf.tupleType(children), bindings);
		
		return new Constructor(constructorType.instantiate(bindings), children);
	}
	
	public IConstructor constructor(Type constructorType, java.util.Map<String,IValue> annotations, IValue... children) {
		Constructor cons = (Constructor) constructor(constructorType, children);
		return new Constructor(cons, annotations);
	}
	
	public IConstructor constructor(Type constructorType) {
		checkNull(constructorType);
		TypeFactory tf = TypeFactory.getInstance();
		java.util.Map<Type, Type> bindings = new HashMap<Type,Type>();
		Type params = constructorType.getAbstractDataType().getTypeParameters();
		for (Type p : params) {
			if (p.isParameterType()) {
				bindings.put(p, tf.voidType());
			}
		}
		return new Constructor(constructorType.instantiate(bindings));
	}

	public IMap map(Type keyType, Type valueType) {
		checkNull(keyType);
		checkNull(valueType);
		return mapWriter(keyType, valueType).done();
	}
	
	public IMapWriter mapWriter(Type keyType, Type valueType) {
		checkNull(keyType);
		checkNull(valueType);
		return Map.createMapWriter(keyType, valueType);
	}
	
	public IMapWriter mapWriter() {
		return Map.createMapWriter();
	}

	public IString string(int[] chars) {
		StringBuilder b = new StringBuilder(chars.length);
		for (int ch : chars) {
			b.appendCodePoint(ch);
		}
		return string(b.toString());
	}

	public IString string(int ch) {
		StringBuilder b = new StringBuilder(1);
		b.appendCodePoint(ch);
		return string(b.toString());
	}

	public IListRelationWriter listRelationWriter(Type tupleType) {
		checkNull(tupleType);
		return ListRelation.createRelationWriter(tupleType);
	}

	public IListRelationWriter listRelationWriter() {
		return ListRelation.createRelationWriter();
	}

	public IListRelation listRelation(Type tupleType) {
		checkNull(tupleType);
		return listRelationWriter(tupleType).done();
	}

	public IListRelation listRelation(IValue... tuples) {
		checkNull((Object[]) tuples);
		Type elementType = lub(tuples);
	
		if (!elementType.isTupleType()) {
			TypeFactory tf = TypeFactory.getInstance();
			throw new UnexpectedElementTypeException(tf.tupleType(tf.voidType()), elementType);
		}
		
		IListWriter rw = listWriter(elementType);
		rw.insert(tuples);
		return (IListRelation) rw.done();
	}
}