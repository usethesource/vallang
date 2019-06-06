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
package io.usethesource.vallang.impl.reference;

import java.util.Iterator;
import java.util.Map.Entry;

import io.usethesource.vallang.IMap;
import io.usethesource.vallang.IMapWriter;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;

/*package*/ class MapWriter implements IMapWriter {
	private Type keyTupe;
	private Type valueType;
	private final java.util.HashMap<IValue, IValue> mapContent;
	private Map constructedMap;

	/*package*/ MapWriter() {
		super();
		
		this.keyTupe = TypeFactory.getInstance().voidType();
		this.valueType = TypeFactory.getInstance().voidType();
		
		mapContent = new java.util.HashMap<>();
	}

	@Override
	public Iterator<IValue> iterator() {
	    return mapContent.keySet().iterator();
	}
	
	@Override
	public IValue get(IValue key) {
	    return mapContent.get(key);
	}
	
	@Override
	public void insertTuple(IValue... fields) {
	    if (fields.length != 2) {
	        throw new IllegalArgumentException("can only insert tuples of arity 2 into a map");
	    }
	    
	    put(fields[0], fields[1]);
	}
	
	private void checkMutation() {
		if (constructedMap != null)
			throw new UnsupportedOperationException(
					"Mutation of a finalized list is not supported.");
	}
	
	@Override
	public void putAll(IMap map) throws FactTypeUseException{
		checkMutation();
		
		for(IValue key : map){
			IValue value = map.get(key);
			updateTypes(key, value);
			mapContent.put(key, value);
		}
	}
	
	private void updateTypes(IValue key, IValue value) {
	    keyTupe = keyTupe.lub(key.getType());
	    valueType = valueType.lub(value.getType());
	}

	@Override
	public void putAll(java.util.Map<IValue, IValue> map) throws FactTypeUseException{
		checkMutation();
		for(Entry<IValue, IValue> entry : map.entrySet()){
			IValue value = entry.getValue();
			updateTypes(entry.getKey(), value);
			mapContent.put(entry.getKey(), value);
		}
	}

	@Override
	public void put(IValue key, IValue value) throws FactTypeUseException{
		checkMutation();
		updateTypes(key,value);
		mapContent.put(key, value);
	}
	
	@Override
	public void insert(IValue... value) throws FactTypeUseException {
		for (IValue tuple : value) {
			ITuple t = (ITuple) tuple;
			IValue key = t.get(0);
			IValue value2 = t.get(1);
			updateTypes(key,value2);
			put(key, value2);
		}
	}
	
	@Override
	public IMap done(){
		if (constructedMap == null) {
			constructedMap = new Map(IMap.TF.mapType(keyTupe, valueType), mapContent);
		}

		return constructedMap;
	}	
	
}
