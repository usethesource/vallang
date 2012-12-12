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
import org.eclipse.imp.pdb.facts.type.TypeFactory;

// TODO Add checking.
/**
 * Implementation of IMapWriter.
 * 
 * @author Arnold Lankamp
 */
public class MapWriter implements IMapWriter{
	protected Type keyType;
	protected Type valueType;
	
	protected final ShareableValuesHashMap data;
	
	protected IMap constructedMap;
	protected final boolean inferred;
	
	protected MapWriter(Type keyType, Type valueType){
		super();
		
		this.keyType = keyType;
		this.valueType = valueType;
		this.inferred = false;
		
		data = new ShareableValuesHashMap();
		
		constructedMap = null;
	}
	
	protected MapWriter(){
		super();
		
		this.keyType = TypeFactory.getInstance().voidType();
		this.valueType =  TypeFactory.getInstance().voidType();
		this.inferred = true;
		
		data = new ShareableValuesHashMap();
		
		constructedMap = null;
	}
	
	protected MapWriter(Type keyType, Type valueType, ShareableValuesHashMap data){
		super();
		
		this.keyType = keyType;
		this.valueType = valueType;
		this.data = data;
		this.inferred = false;
		
		constructedMap = null;
	}
	
	public void put(IValue key, IValue value){
		checkMutation();
		updateTypes(key,value);
		
		data.put(key, value);
	}
	
	private void updateTypes(IValue key, IValue value) {
		if (inferred) {
			keyType = keyType.lub(key.getType());
			valueType = valueType.lub(value.getType());
		}
	}

	public void putAll(IMap map){
		checkMutation();
		
		Iterator<Entry<IValue, IValue>> entryIterator = map.entryIterator();
		while(entryIterator.hasNext()){
			Entry<IValue, IValue> entry = entryIterator.next();
			IValue key = entry.getKey();
			IValue value = entry.getValue();
			updateTypes(key,value);
			data.put(key, value);
		}
	}
	
	public void putAll(java.util.Map<IValue, IValue> map){
		checkMutation();
		
		Iterator<Entry<IValue, IValue>> entryIterator = map.entrySet().iterator();
		while(entryIterator.hasNext()){
			Entry<IValue, IValue> entry = entryIterator.next();
			IValue key = entry.getKey();
			IValue value = entry.getValue();
			updateTypes(key,value);
			data.put(key,value);
		}
	}
	
	public void insert(IValue... values){
		checkMutation();
		
		for(int i = values.length - 1; i >= 0; i--){
			IValue value = values[i];
			
			if(!(value instanceof ITuple)) throw new IllegalArgumentException("Argument must be of ITuple type.");
			
			ITuple tuple = (ITuple) value;
			
			if(tuple.arity() != 2) throw new IllegalArgumentException("Tuple must have an arity of 2.");
			
			IValue key = tuple.get(0);
			IValue value2 = tuple.get(1);
			updateTypes(key,value2);
			put(key, value2);
		}
	}
	
	public void insertAll(Iterable<? extends IValue> collection){
		checkMutation();
		
		Iterator<? extends IValue> collectionIterator = collection.iterator();
		while(collectionIterator.hasNext()){
			IValue value = collectionIterator.next();
			
			if(!(value instanceof ITuple)) throw new IllegalArgumentException("Argument must be of ITuple type.");
			
			ITuple tuple = (ITuple) value;
			
			if(tuple.arity() != 2) throw new IllegalArgumentException("Tuple must have an arity of 2.");
			
			IValue key = tuple.get(0);
			IValue value2 = tuple.get(1);
			updateTypes(key,value2);
			put(key, value2);
		}
	}
	
	protected void checkMutation(){
		if(constructedMap != null) throw new UnsupportedOperationException("Mutation of a finalized map is not supported.");
	}
	
	public IMap done(){
		if(constructedMap == null) {
		  if (data.isEmpty()) {
		    Type voidType = TypeFactory.getInstance().voidType();
        return new Map(voidType, voidType, data);
		  }
		  else {
		    constructedMap = new Map(keyType, valueType, data);
		  }
		}
		
		return constructedMap;
	}
}