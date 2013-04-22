/*******************************************************************************
* Copyright (c) 2007, 2008, 2012 IBM Corporation & CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
*    Jurgen Vinju (jurgen@vinju.org)
*    Anya Helene Bagge - rational support, labeled maps and tuples
*******************************************************************************/

package org.eclipse.imp.pdb.facts.impl.reference;

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
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedElementTypeException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

/**
 * This is a reference implementation for an @{link IValueFactory}. It uses
 * the Java standard library to implement it in a most straightforward but
 * not necessarily very efficient manner.
 */
public class ValueFactory extends org.eclipse.imp.pdb.facts.impl.fast.FastBaseValueFactory {
	private static final ValueFactory sInstance = new ValueFactory();
	public static ValueFactory getInstance() {
		return sInstance;
	}

	private ValueFactory() {
		super();
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
		return new RelationWriter(tupleType);
	}
	
	public IRelationWriter relationWriter() {
		return new RelationWriter();
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
		
		return new SetWriter(eltType);
	}
	
	public ISetWriter setWriter() {
		return new SetWriter();
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
		return new ListWriter(eltType);
	}
	
	public IListWriter listWriter() {
		return new ListWriter();
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

	public ITuple tuple(Type type, IValue... args) {
		checkNull((Object[]) args);

		return new Tuple(type, args.clone());
	}
	
	public INode node(String name) {
		checkNull(name);
		return new Node(name);
	}
	
	public INode node(String name, java.util.Map<String, IValue> annotations, IValue... children) {
		checkNull(name);
		checkNull(annotations);
		checkNull((Object[]) children);
				
		return new Node(name, annotations, children);
	}
	
	public INode node(String name, IValue... children) {
		checkNull(name);
		checkNull((Object[]) children);
		return new Node(name, children);
	}
	
	@Override
	public INode node(String name,  IValue[] children, java.util.Map<String, IValue> keyArgValues)
			throws FactTypeUseException {
		checkNull(name);
		checkNull((Object[]) children);
//		checkNull(keyArgValues); // fails; are null values allowed?
		
		return new Node(name, children.clone(), keyArgValues);
	}
		
	public IConstructor constructor(Type constructorType, IValue... children) {
		checkNull(constructorType);
		checkNull((Object[]) children);
		
		Type instantiatedType = inferInstantiatedTypeOfConstructor(constructorType, children);
		return new Constructor(instantiatedType, children);
	}
	
	public IConstructor constructor(Type constructorType, java.util.Map<String,IValue> annotations, IValue... children) {
		checkNull(constructorType);
		checkNull(annotations);
		checkNull((Object[]) children);
				
		return new Constructor(constructorType, children).setAnnotations(annotations);
	}
	
	public IConstructor constructor(Type constructorType) {
		checkNull(constructorType);
		
		Type instantiatedType = inferInstantiatedTypeOfConstructor(constructorType, new IValue[0]);		
		return new Constructor(instantiatedType);
	}

	public IMap map(Type keyType, Type valueType) {
		checkNull(keyType);
		checkNull(valueType);
		return mapWriter(keyType, valueType).done();
	}
	
	public IMap map(Type mapType) {
		checkNull(mapType);
		return mapWriter(mapType).done();
	}
	public IMapWriter mapWriter(Type keyType, Type valueType) {
		checkNull(keyType);
		checkNull(valueType);
		return new MapWriter(TypeFactory.getInstance().mapType(keyType, valueType));
	}
	
	public IMapWriter mapWriter(Type mapType) {
		checkNull(mapType);
		return new MapWriter(mapType);
	}

	public IMapWriter mapWriter() {
		return new MapWriter();
	}

	public IListRelationWriter listRelationWriter(Type tupleType) {
		checkNull(tupleType);
		return new ListRelationWriter(tupleType);
	}

	public IListRelationWriter listRelationWriter() {
		return new ListRelationWriter();
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
		
		IListRelationWriter rw = listRelationWriter(elementType);
		rw.append(tuples);
		return rw.done();
	}
	
}
