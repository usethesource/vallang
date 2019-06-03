/*******************************************************************************
* Copyright (c) 2009, 2012 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Arnold Lankamp - interfaces and implementation
*    Anya Helene Bagge - labels
*******************************************************************************/
package io.usethesource.vallang.impl.fast;

import java.util.Iterator;
import java.util.Map.Entry;

import io.usethesource.vallang.IMap;
import io.usethesource.vallang.IMapWriter;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.impl.util.collections.ShareableValuesHashMap;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;

// TODO Add checking.
/**
 * Implementation of IMapWriter.
 * 
 * @author Arnold Lankamp
 */
/*package*/ class MapWriter implements IMapWriter{
	protected Type keyType;
	protected Type valueType;
	
	protected final ShareableValuesHashMap data;
	
	protected IMap constructedMap;
    private boolean inferredTypeinvalidated = false;
	
	/*package*/ MapWriter(){
		super();
		
		this.keyType = TypeFactory.getInstance().voidType();
		this.valueType =  TypeFactory.getInstance().voidType();
		
		data = new ShareableValuesHashMap();
		
		constructedMap = null;
	}
	
	/*package*/ MapWriter(Type mapType, ShareableValuesHashMap data){
		super();
		
		this.keyType = mapType.getKeyType();
        this.valueType =  mapType.getValueType();
		this.data = data;
		
		constructedMap = null;
	}

	@Override
	public void put(IValue key, IValue value){
		checkMutation();
		updateTypes(key,value);
		
		IValue replaced = data.put(key, value);
		
		if (replaced != null) {
			inferredTypeinvalidated = true;
		}
	}
	
	private void updateTypes(IValue key, IValue value) {
	    keyType = keyType.lub(key.getType());
	    valueType = valueType.lub(value.getType());
	}

	@Override
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
	
	@Override
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
	
	@Override
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
	
	@Override
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
	
	protected void checkMutation() {
		if (constructedMap != null)
			throw new UnsupportedOperationException(
					"Mutation of a finalized map is not supported.");
	}
	
	@Override
	public IMap done(){
		if(constructedMap == null) {
			Type mapType = TypeFactory.getInstance().mapType(keyType, valueType);
			
			if (data.isEmpty()) {
				Type voidType = TypeFactory.getInstance().voidType();
				Type voidMapType = TypeFactory.getInstance().mapType(voidType, mapType.getKeyLabel(), voidType, mapType.getValueLabel());

				constructedMap = Map.newMap(voidMapType, data);
			} else {
				if (inferredTypeinvalidated) {
					Type voidType = TypeFactory.getInstance().voidType();
					
					keyType = voidType;
					valueType = voidType;
					
					for (Iterator<Entry<IValue, IValue>> it = data.entryIterator(); it.hasNext(); ) {
						final Entry<IValue, IValue> currentEntry = it.next();
						
						keyType = keyType.lub(currentEntry.getKey().getType());
						valueType = valueType.lub(currentEntry.getValue().getType());
						
						mapType = TypeFactory.getInstance().mapType(keyType, mapType.getKeyLabel(), valueType, mapType.getValueLabel());
					}
				}
				
				constructedMap = Map.newMap(mapType, data);
			}
		}

		return constructedMap;
	}
}
