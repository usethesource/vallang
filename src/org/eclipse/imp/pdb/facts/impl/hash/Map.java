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
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.Value;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.MapType;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

class Map extends Value implements IMap {
	static class MapWriter implements IMapWriter {
		private Map fMap; 

		public MapWriter(Type key, Type value) {
			fMap = new Map(key, value);
		}

		public IMap getMap() {
			return fMap;
		}

		public void putAll(IMap map) throws FactTypeError {
			MapType mapType = (MapType) map.getType().getBaseType();
			fMap.check(mapType.getKeyType(), mapType.getValueType());
			
			for (IValue key : map) {
			  fMap.put(key, map.get(key));
			}
		}
		
		public void putAll(java.util.Map<? extends IValue, ? extends IValue> map)
				throws FactTypeError {
			for (IValue key : map.keySet()) {
				IValue value = map.get(key);
				fMap.check(key.getType(), value.getType());
				fMap.put(key, value);
			}
		}

		public void put(IValue key, IValue value) throws FactTypeError {
			fMap.put(key, value);
		}
		
		public IMap done() {
			return fMap;
		}
	}

	final HashMap<IValue,IValue> fMap;

	/* package */Map(MapType mapType) {
		super(mapType);
		fMap = new HashMap<IValue,IValue>();
	}
	
	/* package */Map(Type keyType, Type valueType) {
		this(TypeFactory.getInstance().mapType(keyType, valueType));
	}
	
	/**
	 * Used for efficient cloning.
	 * @param other
	 */
	private Map(Map other) {
		super(other.fType);
		fMap = other.fMap;
	}

	public boolean isEmpty() {
		return fMap.isEmpty();
	}

	public int size() {
		return fMap.size();
	}

	/**
	 * iterates over the keys of the map
	 */
	public Iterator<IValue> iterator() {
		return fMap.keySet().iterator();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		int idx = 0;
		for (IValue a : this) {
			if (idx++ > 0) {
				sb.append(",");
			}
			sb.append(a.toString() + ":" + get(a).toString());
		}
		sb.append(")");
		return sb.toString();
	}

	public Type getKeyType() {
		return ((MapType) fType.getBaseType()).getKeyType();
	}
	
	public Type getValueType() {
		return ((MapType) fType.getBaseType()).getValueType();
	}

	private void check(Type eltType, Type valueType) throws FactTypeError {
		if (!eltType.isSubtypeOf(getKeyType())) {
			throw new FactTypeError("Key type " + eltType + " is not a subtype of " + getKeyType());
		}
	    if (!valueType.isSubtypeOf(getValueType())) {
			throw new FactTypeError("Value type " + eltType + " is not a subtype of " + getKeyType());
		}
	}

	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Map)) {
			return false;
		}
		
		Map other = (Map) o;
		
		return fMap.equals(other.fMap);
	}

	public <T> T accept(IValueVisitor<T> v) throws VisitorException {
		return v.visitMap(this);
	}

	public int arity() {
		return fMap.size();
	}

	public boolean containsKey(IValue key) throws FactTypeError {
		return fMap.containsKey(key);
	}

	public boolean containsValue(IValue value) throws FactTypeError {
		return fMap.containsValue(value);
	}

	public IValue get(IValue key) {
		return fMap.get(key);
	}

	public IMap put(IValue key, IValue value) throws FactTypeError {
		IMapWriter sw = new MapWriter(getKeyType(), getValueType());
		sw.putAll(this);
		sw.put(key, value);
		return sw.done();
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new Map(this);
	}
}
