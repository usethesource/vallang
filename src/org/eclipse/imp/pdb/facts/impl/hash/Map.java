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
import org.eclipse.imp.pdb.facts.impl.WritableValue;
import org.eclipse.imp.pdb.facts.impl.WriterBase;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.MapType;
import org.eclipse.imp.pdb.facts.type.NamedType;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

class Map extends WritableValue<IMapWriter> implements IMap {
	static class MapWriter extends WriterBase<IMapWriter> implements IMapWriter {
		private Map fMap; // cached for convenience (avoids casts on every
		// insert)

		public MapWriter(Map map) {
			super(map);
			fMap = map;
		}

		public IMap getMap() {
			return fMap;
		}

		public void putAll(IMap map) throws FactTypeError {
			checkMutable();
			MapType mapType = (MapType) map.getType().getBaseType();
			fMap.check(mapType.getKeyType(), mapType.getValueType());
			
			for (IValue key : map) {
			  fMap.put(key, map.get(key));
			}
		}

		public void put(IValue key, IValue value) throws FactTypeError {
			fMap.put(key, value);
		}
	}

	HashMap<IValue,IValue> fMap = new HashMap<IValue,IValue>();

	/* package */Map(NamedType mapType) throws FactTypeError {
		super(mapType);
		if (!mapType.getBaseType().isMapType()) {
			throw new FactTypeError("named type is not a set:" + mapType);
		}
	}

	/* package */Map(MapType mapType) {
		super(mapType);
	}
	
	/* package */Map(Type keyType, Type valueType) {
		super(TypeFactory.getInstance().mapType(keyType, valueType));
	}

	@Override
	protected IMapWriter createWriter() {
		return new MapWriter(this);
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
		sb.append("{ ");
		int idx = 0;
		for (IValue a : this) {
			if (idx++ > 0)
				sb.append(",\n  ");
			sb.append(a.toString());
		}
		sb.append("\n}");
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

	public IValue accept(IValueVisitor v) throws VisitorException {
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
		Map result = new Map(getKeyType(), getValueType());
		IMapWriter sw = result.getWriter();
		sw.putAll(this);
		sw.put(key, value);
		sw.done();

		return result;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Object clone() throws CloneNotSupportedException {
		Map tmp;
		
		if (getType() instanceof NamedType) {
		    tmp =  new Map((NamedType) getType());
		}
		else {
			tmp = new Map(getKeyType(), getValueType());
		}
	
		// we don't have to clone fList if this instance is not mutable anymore,
		// otherwise we certainly do, to prevent modification of the original list.
		if (isMutable()) {
			tmp.fMap = (HashMap<IValue, IValue>) fMap.clone();
		}
		else {
			tmp.fMap = fMap;
			tmp.getWriter().done();
		}
		
		return tmp;
	}
}
