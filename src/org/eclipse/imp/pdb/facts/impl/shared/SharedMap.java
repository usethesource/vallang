/*******************************************************************************
* Copyright (c) 2009, 2012 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Arnold Lankamp - interfaces and implementation
*    Anya Helene Bagge
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.shared;

import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.fast.Map;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesHashMap;
import org.eclipse.imp.pdb.facts.impl.util.sharing.IShareable;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

/**
 * Implementation of shareable maps.
 * 
 * @author Arnold Lankamp
 */
public class SharedMap extends Map implements IShareable{
	
	protected SharedMap(Type mapType, ShareableValuesHashMap data){
		super(mapType, data);
	}
	
	public IMap put(IValue key, IValue value){
		ShareableValuesHashMap newData = new ShareableValuesHashMap(data);
		newData.put(key, value);
		
		Type newMapType = mapType;
		Type newKeyType = mapType.getKeyType().lub(key.getType());
		Type newValueType = mapType.getValueType().lub(value.getType());
		if(newKeyType != mapType.getKeyType() || newValueType != mapType.getValueType()) {
			 newMapType = TypeFactory.getInstance().mapType(newKeyType, mapType.getKeyLabel(), newValueType, mapType.getValueLabel());
		}


		return new SharedMapWriter(newMapType, newData).done();
	}
	
	public IMap common(IMap other){
		ShareableValuesHashMap commonData = new ShareableValuesHashMap();
		Iterator<Entry<IValue, IValue>> entryIterator;
		
		IMap theOtherMap;
		
		if(other.size() <= size()){
			entryIterator = other.entryIterator();
			theOtherMap = this;
		}else{
			entryIterator = entryIterator();
			theOtherMap = other;
		}
		
		while(entryIterator.hasNext()){
			Entry<IValue, IValue> entry = entryIterator.next();
			IValue key = entry.getKey();
			IValue value = entry.getValue();
			if(value.isEqual(theOtherMap.get(key))){
				commonData.put(key, value);
			}
		}

		return new SharedMapWriter(mapType.lub(other.getType()), commonData).done();
	}
	
	public IMap compose(IMap other){
		ShareableValuesHashMap newData = new ShareableValuesHashMap();
		
		SharedMap otherMap = (SharedMap) other;
		
		Iterator<Entry<IValue, IValue>> entryIterator = entryIterator();
		while(entryIterator.hasNext()){
			Entry<IValue,IValue> entry = entryIterator.next();
			IValue value = otherMap.get(entry.getValue());
			if(value != null){
				newData.put(entry.getKey(), value);
			}
		}
		
		Type newMapType;
		if(mapType.hasFieldNames() && otherMap.mapType.hasFieldNames()) {
			newMapType = TypeFactory.getInstance().mapType(mapType.getKeyType(), mapType.getKeyLabel(), 
				otherMap.mapType.getValueType(), otherMap.mapType.getValueLabel());
		}
		else {
			newMapType = TypeFactory.getInstance().mapType(mapType.getKeyType(), otherMap.mapType.getValueType());
		}
		return new SharedMapWriter(newMapType, newData).done();
	}
	
	public IMap join(IMap other){
		ShareableValuesHashMap newData;
		Iterator<Entry<IValue, IValue>> entryIterator;
		
		SharedMap otherMap = (SharedMap) other;
		
		newData = new ShareableValuesHashMap(data);
		entryIterator = otherMap.entryIterator();
		
		while(entryIterator.hasNext()){
			Entry<IValue, IValue> entry = entryIterator.next();
			newData.put(entry.getKey(), entry.getValue());
		}
		
		return new SharedMapWriter(mapType.lub(otherMap.mapType), newData).done();
	}
	
	public IMap remove(IMap other){
		ShareableValuesHashMap newData = new ShareableValuesHashMap(data);
		
		Iterator<IValue> keysIterator = other.iterator();
		while(keysIterator.hasNext()){
			newData.remove(keysIterator.next());
		}
		
		return new SharedMapWriter(mapType, newData).done();
	}
	
	public boolean equivalent(IShareable shareable){
		return super.equals(shareable);
	}
	
	public boolean equals(Object o){
		return (this == o);
	}
}
