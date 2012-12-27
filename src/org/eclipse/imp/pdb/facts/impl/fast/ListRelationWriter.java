/*******************************************************************************
* Copyright (c) 2009 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Arnold Lankamp - interfaces and implementation
*    Paul Klint (Paul.Klint@cwi.nl) - adapted for ListRealtion
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.fast;

import java.util.Iterator;

import org.eclipse.imp.pdb.facts.IListRelation;
import org.eclipse.imp.pdb.facts.IListRelationWriter;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesList;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

// TODO Add checking.
/**
 * Implementation of IListRelationWriter.
 * 
 * @author Arnold Lankamp
 * @author Paul Klint
 */
public class ListRelationWriter implements IListRelationWriter{
	protected Type tupleType;
	
	protected final ShareableValuesList data;
	
	protected IListRelation constructedRelation;

	protected final boolean inferred;
	
	protected ListRelationWriter(Type tupleType){
		super();
		
		if (!tupleType.isTupleType()) {
			throw new IllegalArgumentException("should be a tuple type");
		}
		
		this.tupleType = tupleType;
		this.inferred = false;
		
		data = new ShareableValuesList();
		
		constructedRelation = null;
	}
	
	protected ListRelationWriter(){
		super();
		
		this.tupleType = TypeFactory.getInstance().voidType();
		
		data = new ShareableValuesList();
		
		constructedRelation = null;
		this.inferred = true;
	}
	
	protected ListRelationWriter(Type tupleType, ShareableValuesList data){
		super();
		
		this.tupleType = tupleType;
		this.data = new ShareableValuesList(data);
		this.inferred = false;
		
		constructedRelation = null;
	}
	
	private void checkBounds(IValue[] elems, int start, int length){
		if(start < 0) throw new ArrayIndexOutOfBoundsException("start < 0");
		if((start + length) > elems.length) throw new ArrayIndexOutOfBoundsException("(start + length) > elems.length");
	}
	
	private void updateType(IValue element) {
		if (inferred) {
			tupleType = tupleType.lub(element.getType());
			if (!tupleType.isTupleType()) {
				throw new IllegalArgumentException("relations can only contain tuples of the same arity");
			} 
		}
	}
	
	public void delete(IValue element){
		checkMutation();
		
		data.remove(element);
	}
	
	public int size(){
		return data.size();
	}

	protected void checkMutation(){
		if(constructedRelation != null) throw new UnsupportedOperationException("Mutation of a finalized map is not supported.");
	}
	
	public IListRelation done(){
		if (constructedRelation == null) {
		  constructedRelation = new ListRelation(data.isEmpty() ? TypeFactory.getInstance().voidType() : tupleType, data);
		}
		
		return constructedRelation;
	}

	public void append(IValue... elems){
		checkMutation();
		
		for(IValue elem : elems){
			updateType(elem);
			data.append(elem);
		}
	}
	
	public void appendAll(Iterable<? extends IValue> collection){
		checkMutation();
		
		Iterator<? extends IValue> collectionIterator = collection.iterator();
		while(collectionIterator.hasNext()){
			IValue next = collectionIterator.next();
			updateType(next);
			data.append(next);
		}
	}
	
	
	public void insert(IValue... elements){
		insert(elements, 0, elements.length);
	}
	
	public void insert(IValue[] elements, int start, int length){
		checkMutation();
		checkBounds(elements, start, length);
		
		for(int i = start + length - 1; i >= start; i--){
			updateType(elements[i]);
			data.insert(elements[i]);
		}
	}
	
	public void insertAll(Iterable<? extends IValue> collection){
		checkMutation();
		
		Iterator<? extends IValue> collectionIterator = collection.iterator();
		while(collectionIterator.hasNext()){
			IValue next = collectionIterator.next();
			updateType(next);
			data.insert(next);
		}
	}
	
	public void insertAt(int index, IValue element){
		checkMutation();
		
		updateType(element);
		data.insertAt(index, element);
	}
	
	public void insertAt(int index, IValue... elements){
		insertAt(index, elements, 0, 0);
	}
	
	public void insertAt(int index, IValue[] elements, int start, int length){
		checkMutation();
		checkBounds(elements, start, length);
		
		for(int i = start + length - 1; i >= start; i--){
			updateType(elements[i]);
			data.insertAt(index, elements[i]);
		}
	}
	
	public void replaceAt(int index, IValue element){
		checkMutation();
		
		updateType(element);
		data.set(index, element);
	}
	
	public void delete(int index){
		checkMutation();
		
		data.remove(index);
	}
}