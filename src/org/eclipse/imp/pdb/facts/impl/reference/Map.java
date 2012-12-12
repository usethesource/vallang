/*******************************************************************************
 * Copyright (c) 2008, 2012 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI  
 *******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.reference;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedMapKeyTypeException;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedMapValueTypeException;
import org.eclipse.imp.pdb.facts.impl.Value;
import org.eclipse.imp.pdb.facts.impl.Writer;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

class Map extends Value implements IMap{
	private final HashMap<IValue,IValue> content;
	private int fHash = 0;
	
	/* package */Map(Type keyType, Type valueType, HashMap<IValue, IValue> content){
		super(TypeFactory.getInstance().mapType(keyType, valueType));
		
		this.content = content;
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
	
	public Iterator<Entry<IValue,IValue>> entryIterator() {
		return content.entrySet().iterator();
	}

	public Iterator<IValue> valueIterator() {
		return content.values().iterator();
	}

	public boolean containsKey(IValue key) {
		return content.containsKey(key);
	}

	public boolean containsValue(IValue value) {
		return content.containsValue(value);
	}

	public IMap put(IValue key, IValue value) {
		IMapWriter sw = new MapWriter(getKeyType().lub(key.getType()), getValueType().lub(value.getType()));
		sw.putAll(this);
		sw.put(key, value);
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
			if (otherValue != null && thisValue.isEqual(otherValue)) {
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
			if (!other.containsKey(key)) {
				return false;
			}
		}
		
		return true;
	}
	
	public boolean equals(Object o){
		if(getClass() == o.getClass()) {
			Map other = (Map) o;
			
			return content.equals(other.content);
		}
		return false;
	}

	@Override
	public int hashCode() {
		if (fHash == 0) {
			fHash = content.hashCode();
		}
		return fHash;
	}
	
	public Type getKeyType(){
		return fType.getKeyType();
	}
	
	public Type getValueType(){
		return fType.getValueType();
	}

	private static void check(Type key, Type value, Type keyType, Type valueType) throws FactTypeUseException{
		if(!key.isSubtypeOf(keyType)) {
			throw new UnexpectedMapKeyTypeException(keyType, key);
		}
		if(!value.isSubtypeOf(valueType)) {
			throw new UnexpectedMapValueTypeException(valueType, value);
		}
	}

	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
		return v.visitMap(this);
	}
	
	static MapWriter createMapWriter(Type keyType, Type valueType){
		return new MapWriter(keyType, valueType);
	}
	
	static MapWriter createMapWriter(){
		return new MapWriter();
	}
	
	public IMap compose(IMap other) {
		IMapWriter w = new MapWriter(getKeyType(), other.getValueType());
		
		Iterator<Entry<IValue,IValue>> iter = entryIterator();
		while (iter.hasNext()) {
			Entry<IValue,IValue> e = iter.next();
			IValue value = other.get(e.getValue());
			if (value != null) {
				w.put(e.getKey(), value);
			}
		}
		
		return w.done();
	}
	
	private static class MapWriter extends Writer implements IMapWriter{
		private Type keyType;
		private Type valueType;
		private final boolean inferred;
		private final HashMap<IValue, IValue> mapContent;
		private Map constructedMap; 

		public MapWriter(Type keyType, Type valueType){
			super();
			
			this.keyType = keyType;
			this.valueType = valueType;
			this.inferred = false;
			
			mapContent = new HashMap<IValue, IValue>();
		}
		
		public MapWriter(){
			super();
			
			this.keyType = TypeFactory.getInstance().voidType();
			this.valueType = TypeFactory.getInstance().voidType();
			this.inferred = true;
			
			mapContent = new HashMap<IValue, IValue>();
		}

		private void checkMutation(){
			if(constructedMap != null) throw new UnsupportedOperationException("Mutation of a finalized list is not supported.");
		}
		
		public void putAll(IMap map) throws FactTypeUseException{
			checkMutation();
			Type mapType = map.getType();
			check(mapType.getKeyType(), mapType.getValueType(), keyType, valueType);
			
			for(IValue key : map){
				IValue value = map.get(key);
				updateTypes(key, value);
				mapContent.put(key, value);
			}
		}
		
		private void updateTypes(IValue key, IValue value) {
			if (inferred) {
				keyType = keyType.lub(key.getType());
				valueType = valueType.lub(value.getType());
			}
			
		}

		public void putAll(java.util.Map<IValue, IValue> map) throws FactTypeUseException{
			checkMutation();
			for(IValue key : map.keySet()){
				IValue value = map.get(key);
				updateTypes(key, value);
				check(key.getType(), value.getType(), keyType, valueType);
				mapContent.put(key, value);
			}
		}

		public void put(IValue key, IValue value) throws FactTypeUseException{
			checkMutation();
			updateTypes(key,value);
			mapContent.put(key, value);
		}
		
		public void insert(IValue... value) throws FactTypeUseException {
			for(IValue tuple : value){
				ITuple t = (ITuple) tuple;
				IValue key = t.get(0);
				IValue value2 = t.get(1);
				updateTypes(key,value2);
				put(key, value2);
			}
		}
		
		public IMap done(){
			if(constructedMap == null) {
			  Type voidType = TypeFactory.getInstance().voidType();
				constructedMap = new Map(mapContent.isEmpty() ? voidType : keyType, mapContent.isEmpty() ? voidType : valueType, mapContent);
			}
			
			return constructedMap;
		}
	}
	
}