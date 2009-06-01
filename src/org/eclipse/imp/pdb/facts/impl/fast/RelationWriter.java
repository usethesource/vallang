/*******************************************************************************
* Copyright (c) 2009 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Arnold Lankamp - interfaces and implementation
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.fast;

import java.util.Iterator;

import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.IRelationWriter;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesHashSet;
import org.eclipse.imp.pdb.facts.type.Type;

// TODO Add checking.
/**
 * Implementation of IRelationWriter.
 * 
 * @author Arnold Lankamp
 */
public class RelationWriter implements IRelationWriter{
	protected final Type tupleType;
	
	protected final ShareableValuesHashSet data;
	
	protected IRelation constructedRelation;
	
	protected RelationWriter(Type tupleType){
		super();
		
		this.tupleType = tupleType;
		
		data = new ShareableValuesHashSet();
		
		constructedRelation = null;
	}
	
	protected RelationWriter(Type tupleType, ShareableValuesHashSet data){
		super();
		
		this.tupleType = tupleType;
		this.data = new ShareableValuesHashSet(data);
		
		constructedRelation = null;
	}
	
	public void insert(IValue element){
		checkMutation();
		
		data.add(element);
	}
	
	public void insert(IValue... elements){
		checkMutation();
		
		for(int i = elements.length - 1; i >= 0; i--){
			data.add(elements[i]);
		}
	}
	
	public void insertAll(Iterable<IValue> collection){
		checkMutation();
		
		Iterator<IValue> collectionIterator = collection.iterator();
		while(collectionIterator.hasNext()){
			data.add(collectionIterator.next());
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
	
	public IRelation done(){
		if(constructedRelation == null) constructedRelation = new Relation(tupleType, data);
		
		return constructedRelation;
	}
}