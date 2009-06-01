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
import java.util.Map.Entry;

import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesHashMap;
import org.eclipse.imp.pdb.facts.type.Type;

// TODO Add checking.
/**
 * Implementation of IMapWriter.
 * 
 * @author Arnold Lankamp
 */
public class MapWriter implements IMapWriter{
	protected final Type keyType;
	protected final Type valueType;
	
	protected final ShareableValuesHashMap data;
	
	protected IMap constructedMap;
	
	protected MapWriter(Type keyType, Type valueType){
		super();
		
		this.keyType = keyType;
		this.valueType = valueType;
		
		data = new ShareableValuesHashMap();
		
		constructedMap = null;
	}
	
	protected MapWriter(Type keyType, Type valueType, ShareableValuesHashMap data){
		super();
		
		this.keyType = keyType;
		this.valueType = valueType;
		this.data = data;
		
		constructedMap = null;
	}
	
	public void put(IValue key, IValue value){
		checkMutation();
		
		data.put(key, value);
	}
	
	public void putAll(IMap map){
		checkMutation();
		
		Iterator<Entry<IValue, IValue>> entryIterator = map.entryIterator();
		while(entryIterator.hasNext()){
			Entry<IValue, IValue> entry = entryIterator.next();
			data.put(entry.getKey(), entry.getValue());
		}
	}
	
	public void putAll(java.util.Map<IValue, IValue> map){
		checkMutation();
		
		Iterator<Entry<IValue, IValue>> entryIterator = map.entrySet().iterator();
		while(entryIterator.hasNext()){
			Entry<IValue, IValue> entry = entryIterator.next();
			data.put(entry.getKey(), entry.getValue());
		}
	}
	
	public void insert(IValue... values){
		checkMutation();
		
		for(int i = values.length - 1; i >= 0; i--){
			IValue value = values[i];
			
			if(!(value instanceof ITuple)) throw new IllegalArgumentException("Argument must be of ITuple type.");
			
			ITuple tuple = (ITuple) value;
			
			if(tuple.arity() != 2) throw new IllegalArgumentException("Tuple must have an arity of 2.");
			
			put(tuple.get(0), tuple.get(1));
		}
	}
	
	public void insertAll(Iterable<IValue> collection){
		checkMutation();
		
		Iterator<IValue> collectionIterator = collection.iterator();
		while(collectionIterator.hasNext()){
			IValue value = collectionIterator.next();
			
			if(!(value instanceof ITuple)) throw new IllegalArgumentException("Argument must be of ITuple type.");
			
			ITuple tuple = (ITuple) value;
			
			if(tuple.arity() != 2) throw new IllegalArgumentException("Tuple must have an arity of 2.");
			
			put(tuple.get(0), tuple.get(1));
		}
	}
	
	protected void checkMutation(){
		if(constructedMap != null) throw new UnsupportedOperationException("Mutation of a finalized map is not supported.");
	}
	
	public IMap done(){
		if(constructedMap == null) constructedMap = new Map(keyType, valueType, data);
		
		return constructedMap;
	}
}