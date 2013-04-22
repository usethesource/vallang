/*******************************************************************************
 * Copyright (c) 2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Paul Klint - Paul.Klint@cwi.nl - CWI
 *   * Anya Helene Bagge - anya@ii.uib.no - UiB
 *
 * Based on code by:
 *
 *   * Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
 *******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.reference;

import java.util.Map.Entry;

import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedMapKeyTypeException;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedMapValueTypeException;
import org.eclipse.imp.pdb.facts.impl.Writer;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

/*package*/ class MapWriter extends Writer implements IMapWriter {
	private Type mapType; 
	private Type keyType;
	private Type valueType;
	private final boolean inferred;
	private final java.util.HashMap<IValue, IValue> mapContent;
	private Map constructedMap;

	/*package*/ MapWriter(){
		super();
		
		this.mapType = null;
		this.keyType = TypeFactory.getInstance().voidType();
		this.valueType = TypeFactory.getInstance().voidType();
		this.inferred = true;
		
		mapContent = new java.util.HashMap<IValue, IValue>();
	}

	/*package*/ MapWriter(Type mapType){
		super();
		
		if(mapType.isTupleType() && mapType.getArity() >= 2) {
			mapType = TypeFactory.getInstance().mapTypeFromTuple(mapType);
		}
		
		if(!mapType.isMapType()) throw new IllegalArgumentException("Argument must be a map type or tuple type: " + mapType);

		this.mapType = mapType;
		this.keyType = mapType.getKeyType();
		this.valueType = mapType.getValueType();
		this.inferred = false;
		
		mapContent = new java.util.HashMap<IValue, IValue>();
	}
	
	private static void check(Type key, Type value, Type keyType, Type valueType)
			throws FactTypeUseException {
		if (!key.isSubtypeOf(keyType)) {
			throw new UnexpectedMapKeyTypeException(keyType, key);
		}
		if (!value.isSubtypeOf(valueType)) {
			throw new UnexpectedMapValueTypeException(valueType, value);
		}
	}
	
	private void checkMutation() {
		if (constructedMap != null)
			throw new UnsupportedOperationException(
					"Mutation of a finalized list is not supported.");
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
		for(Entry<IValue, IValue> entry : map.entrySet()){
			IValue value = entry.getValue();
			updateTypes(entry.getKey(), value);
			check(entry.getKey().getType(), value.getType(), keyType, valueType);
			mapContent.put(entry.getKey(), value);
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
		if (constructedMap == null) {
			if (mapType == null) {
				mapType = TypeFactory.getInstance().mapType(keyType, valueType);
			}
			constructedMap = new Map(mapType, mapContent);
		}

		return constructedMap;
	}

}