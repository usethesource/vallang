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
package org.eclipse.imp.pdb.facts.impl.shared;

import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.fast.Map;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesHashMap;
import org.eclipse.imp.pdb.facts.impl.util.sharing.IShareable;
import org.eclipse.imp.pdb.facts.type.Type;

/**
 * Implementation of shareable maps.
 * 
 * @author Arnold Lankamp
 */
public class SharedMap extends Map implements IShareable{
	
	protected SharedMap(Type keyType, Type valueType, ShareableValuesHashMap data){
		super(keyType, valueType, data);
	}
	
	public IMap put(IValue key, IValue value){
		ShareableValuesHashMap newData = new ShareableValuesHashMap(data);
		newData.put(key, value);
		
		Type newKeyType = keyType.lub(key.getType());
		Type newValueType = valueType.lub(value.getType());
		return new SharedMapWriter(newKeyType, newValueType, newData).done();
	}
	
	public IMap common(IMap other){
		ShareableValuesHashMap commonData = new ShareableValuesHashMap();
		Iterator<Entry<IValue, IValue>> entryIterator;
		
		IMap theOtherMap;
		
		if(other.size() < size()){
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

		Type newKeyType = keyType.lub(other.getKeyType());
		Type newValueType = valueType.lub(other.getValueType());
		return new SharedMapWriter(newKeyType, newValueType, commonData).done();
	}
	
	public IMap join(IMap other){
		ShareableValuesHashMap newData;
		Iterator<Entry<IValue, IValue>> entryIterator;
		
		SharedMap otherMap = (SharedMap) other;
		
		if(otherMap.size() < size()){
			newData = new ShareableValuesHashMap(data);
			entryIterator = otherMap.entryIterator();
		}else{
			newData = new ShareableValuesHashMap(otherMap.data);
			entryIterator = entryIterator();
		}
		
		while(entryIterator.hasNext()){
			Entry<IValue, IValue> entry = entryIterator.next();
			newData.put(entry.getKey(), entry.getValue());
		}
		
		Type newKeyType = keyType.lub(otherMap.keyType);
		Type newValueType = valueType.lub(otherMap.valueType);
		return new SharedMapWriter(newKeyType, newValueType, newData).done();
	}
	
	public IMap remove(IMap other){
		ShareableValuesHashMap newData = new ShareableValuesHashMap(data);
		
		Iterator<IValue> keysIterator = other.iterator();
		while(keysIterator.hasNext()){
			newData.remove(keysIterator.next());
		}
		
		return new SharedMapWriter(keyType, valueType, newData).done();
	}
	
	public boolean equivalent(IShareable shareable){
		return super.equals(shareable);
	}
	
	public boolean equals(Object o){
		return (this == o);
	}
}
