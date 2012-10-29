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
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesHashMap;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * Implementation of IMap.
 * 
 * @author Arnold Lankamp
 */
public class Map extends Value implements IMap{
	protected final static TypeFactory typeFactory = TypeFactory.getInstance();
	
	protected final Type mapType;
	protected final Type keyType;
	protected final Type valueType;
	
	protected final ShareableValuesHashMap data;
	
	protected Map(Type keyType, Type valueType, ShareableValuesHashMap data){
		super();
		
		this.mapType = typeFactory.mapType(keyType, valueType);
		this.keyType = keyType;
		this.valueType = valueType;
		
		this.data = data;
	}

	public Type getType(){
		return mapType;
	}
	
	public Type getKeyType(){
		return keyType;
	}
	
	public Type getValueType(){
		return valueType;
	}
	
	public int size(){
		return data.size();
	}
	
	public int arity(){
		return size();
	}
	
	public boolean isEmpty(){
		return data.isEmpty();
	}
	
	public IValue get(IValue key){
		return data.get(key);
	}
	
	public Iterator<IValue> iterator(){
		return data.keysIterator();
	}
	
	public Iterator<Entry<IValue, IValue>> entryIterator(){
		return data.entryIterator();
	}
	
	public Iterator<IValue> valueIterator(){
		return data.valuesIterator();
	}

	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
		return v.visitMap(this);
	}
	
	public boolean containsKey(IValue key){
		return data.contains(key);
	}

	public boolean containsValue(IValue value){
		Iterator<IValue> valuesIterator = data.valuesIterator();
		while(valuesIterator.hasNext()){
			if(valuesIterator.next().isEqual(value)) return true;
		}
		
		return false;
	}
	
	public boolean isSubMap(IMap other){
		Map otherMap = (Map) other;
		
		Iterator<IValue> keysIterator = iterator();
		while(keysIterator.hasNext()){
			if(!otherMap.data.contains(keysIterator.next())) return false;
		}
		
		return true;
	}
	
	public IMap put(IValue key, IValue value){
		ShareableValuesHashMap newData = new ShareableValuesHashMap(data);
		newData.put(key, value);
		
		Type newKeyType = keyType.lub(key.getType());
		Type newValueType = valueType.lub(value.getType());
		return new MapWriter(newKeyType, newValueType, newData).done();
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

		Type newKeyType = keyType.lub(other.getKeyType());
		Type newValueType = valueType.lub(other.getValueType());
		return new MapWriter(newKeyType, newValueType, commonData).done();
	}
	
	public IMap compose(IMap other){
		ShareableValuesHashMap newData = new ShareableValuesHashMap();
		
		Map otherMap = (Map) other;
		
		Iterator<Entry<IValue, IValue>> entryIterator = entryIterator();
		while(entryIterator.hasNext()){
			Entry<IValue,IValue> entry = entryIterator.next();
			IValue value = otherMap.get(entry.getValue());
			if(value != null){
				newData.put(entry.getKey(), value);
			}
		}
		
		return new MapWriter(keyType, otherMap.valueType, newData).done();
	}
	
	public IMap join(IMap other){
		ShareableValuesHashMap newData;
		Iterator<Entry<IValue, IValue>> entryIterator;
		
		Map otherMap = (Map) other;
		
		if(otherMap.size() <= size()){
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
		return new MapWriter(newKeyType, newValueType, newData).done();
	}
	
	public IMap remove(IMap other){
		ShareableValuesHashMap newData = new ShareableValuesHashMap(data);
		
		Iterator<IValue> keysIterator = other.iterator();
		while(keysIterator.hasNext()){
			newData.remove(keysIterator.next());
		}
		
		return new MapWriter(keyType, valueType, newData).done();
	}
	
	public int hashCode(){
		return data.hashCode();
	}
	
	public boolean equals(Object o){
		if(o == this) return true;
		if(o == null) return false;
		
		if(o.getClass() == getClass()){
			Map otherMap = (Map) o;
			
			if(mapType != otherMap.mapType) return false;
			
			return data.equals(otherMap.data);
		}
		
		return false;
	}
	
	public boolean isEqual(IValue value){
		if(value == this) return true;
		if(value == null) return false;
		
		if(value instanceof Map){
			Map otherMap = (Map) value;
			
			if (size() == 0 && otherMap.size() == 0) return true;
			
			if(!keyType.comparable(otherMap.keyType) || !valueType.comparable(otherMap.valueType)) return false;
			
			return data.isEqual(otherMap.data);
		}
		
		return false;
	}
}
