/*******************************************************************************
* Copyright (c) 2008 CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    jurgen@vinju.org

*******************************************************************************/

package org.eclipse.imp.pdb.facts.impl.hash;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.Value;
import org.eclipse.imp.pdb.facts.impl.Writer;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.MapType;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

class Map extends Value implements IMap{
	private final HashMap<IValue,IValue> content;
	
	/* package */Map(Type keyType, Type valueType, HashMap<IValue, IValue> content){
		super(TypeFactory.getInstance().mapType(keyType, valueType));
		
		this.content = content;
	}
	
	@SuppressWarnings("unchecked")
	private Map(Map other, String label, IValue anno) {
		super(other, label, anno);
		
		content = (HashMap<IValue, IValue>) other.content.clone();
	}

	/*package*/ Map(Type keyType, Type valueType,
			HashMap<IValue, IValue> mapContent,
			HashMap<String, IValue> annotations) {
		super(TypeFactory.getInstance().mapType(keyType, valueType), annotations);
		
		this.content = mapContent;
	}

	public int size(){
		return content.size();
	}

	public int arity(){
		return content.size();
	}

	public boolean isEmpty() {
		return content.isEmpty();
	}

	public IValue get(IValue key){
		return content.get(key);
	}
	
	public Iterator<IValue> iterator(){
		return content.keySet().iterator();
	}

	public boolean containsKey(IValue key) throws FactTypeError{
		return content.containsKey(key);
	}

	public boolean containsValue(IValue value) throws FactTypeError{
		return content.containsValue(value);
	}

	public IMap put(IValue key, IValue value) throws FactTypeError{
		IMapWriter sw = new MapWriter(getKeyType().lub(key.getType()), getValueType().lub(value.getType()));
		sw.putAll(this);
		sw.put(key, value);
		sw.setAnnotations(fAnnotations);
		return sw.done();
	}
	
	public IMap join(IMap other) {
		IMapWriter sw = new MapWriter(getKeyType().lub(other.getKeyType()), getValueType().lub(other.getValueType()));
		sw.putAll(this);
		sw.putAll(other);
		return sw.done();
	}
	
	public IMap common(IMap other) {
		IMapWriter sw = new MapWriter(getKeyType().lub(other.getKeyType()), getValueType().lub(other.getValueType()));
		for (IValue key : this) {
			IValue thisValue = get(key);
			IValue otherValue = other.get(key);
			if (otherValue != null && thisValue.equals(otherValue)) {
				sw.put(key, thisValue);
			}
		}
		return sw.done();
	}
	
	public IMap remove(IMap other) {
		IMapWriter sw = new MapWriter(getKeyType().lub(other.getKeyType()), getValueType().lub(other.getValueType()));
		for (IValue key : this) {
			if (!other.containsKey(key)) {
				sw.put(key, get(key));
			}
		}
		return sw.done();
	}
	
	public boolean isSubMap(IMap other) {
		for (IValue key : this) {
			IValue thisValue = get(key);
			IValue otherValue = other.get(key);
			
			if (otherValue == null) {
				return false;
			}
			else if (!thisValue.equals(otherValue)) {
				return false;
			}
		}
		
		return true;
	}
	
	public boolean equals(Object o){
		if(!(o instanceof Map)) return false;
		
		Map other = (Map) o;
		
		return content.equals(other.content);
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("(");
		
		Iterator<IValue> mapIterator = iterator();
		if(mapIterator.hasNext()){
			IValue key = mapIterator.next();
			sb.append(key + ":" + get(key));
			
			while(mapIterator.hasNext()){
				sb.append(",");
				key = mapIterator.next();
				sb.append(key + ":" + get(key));
			}
		}
		
		sb.append(")");
		
		return sb.toString();
	}

	public Type getKeyType(){
		return ((MapType) fType.getBaseType()).getKeyType();
	}
	
	public Type getValueType(){
		return ((MapType) fType.getBaseType()).getValueType();
	}

	private static void check(Type key, Type value, Type keyType, Type valueType) throws FactTypeError{
		if(!key.isSubtypeOf(keyType)) throw new FactTypeError("Key type " + key + " is not a subtype of " + keyType);
		if(!value.isSubtypeOf(valueType)) throw new FactTypeError("Value type " + value + " is not a subtype of " + valueType);
	}

	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
		return v.visitMap(this);
	}
	
	protected IValue clone(String label, IValue anno)  {
		return new Map(this, label, anno);
	}
	
	static MapWriter createMapWriter(Type keyType, Type valueType){
		return new MapWriter(keyType, valueType);
	}
	
	private static class MapWriter extends Writer implements IMapWriter{
		private final Type keyType;
		private final Type valueType;
		private final HashMap<IValue, IValue> mapContent;
		
		private Map constructedMap; 

		public MapWriter(Type keyType, Type valueType){
			super();
			
			this.keyType = keyType;
			this.valueType = valueType;
			
			mapContent = new HashMap<IValue, IValue>();
		}

		private void checkMutation(){
			if(constructedMap != null) throw new UnsupportedOperationException("Mutation of a finalized list is not supported.");
		}
		
		public void putAll(IMap map) throws FactTypeError{
			checkMutation();
			MapType mapType = (MapType) map.getType().getBaseType();
			check(mapType.getKeyType(), mapType.getValueType(), keyType, valueType);
			
			for(IValue key : map){
				mapContent.put(key, map.get(key));
			}
		}
		
		public void putAll(java.util.Map<IValue, IValue> map) throws FactTypeError{
			checkMutation();
			for(IValue key : map.keySet()){
				IValue value = map.get(key);
				check(key.getType(), value.getType(), keyType, valueType);
				mapContent.put(key, value);
			}
		}

		public void put(IValue key, IValue value) throws FactTypeError{
			checkMutation();
			mapContent.put(key, value);
		}
		
		public void insert(IValue... value) throws FactTypeError {
			for(IValue key : value){
				ITuple t = (ITuple) key;
				put(t.get(0), t.get(1));
			}
		}
		
		public IMap done(){
			if(constructedMap == null) {
				constructedMap = new Map(keyType, valueType, mapContent, fAnnotations);
			}
			
			return constructedMap;
		}

	}
}