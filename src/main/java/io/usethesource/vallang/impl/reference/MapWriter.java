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
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IMap;
import io.usethesource.vallang.IMapWriter;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IWriter;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.util.AbstractTypeBag;
import io.usethesource.vallang.util.ValueEqualsWrapper;

/*package*/ class MapWriter implements IMapWriter {
	private AbstractTypeBag keyTypeBag;
    private AbstractTypeBag valTypeBag;
	private final java.util.Map<ValueEqualsWrapper, IValue> mapContent;
	private @MonotonicNonNull Map constructedMap;

	/*package*/ MapWriter() {
		super();
		
		keyTypeBag = AbstractTypeBag.of();
        valTypeBag = AbstractTypeBag.of();
		
		mapContent = new java.util.HashMap<>();
	}

	@Override
	public Iterator<IValue> iterator() {
	    return new Iterator<IValue>() {
	        Iterator<ValueEqualsWrapper> it = mapContent.keySet().iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public IValue next() {
                return it.next().getValue();
            }
        };
	}
	
	@Override
	public @Nullable IValue get(IValue key) {
	    return mapContent.get(new ValueEqualsWrapper(key));
	}
	
	@Override
	public void insertTuple(IValue... fields) {
	    if (fields.length != 2) {
	        throw new IllegalArgumentException("can only insert tuples of arity 2 into a map");
	    }
	    
	    put(fields[0], fields[1]);
	}
	
	private void checkMutation() {
		if (constructedMap != null) {
			throw new UnsupportedOperationException("Mutation of a finalized list is not supported.");
		}
	}
	
	@Override
	public void putAll(IMap map) throws FactTypeUseException{
		checkMutation();
		
		for (Entry<IValue, IValue> entry : (Iterable<Entry<IValue, IValue>>) () -> map.entryIterator()) {
		    updateTypes(entry.getKey(), entry.getValue());
		    mapContent.put(new ValueEqualsWrapper(entry.getKey()), entry.getValue());
		}
	}
	
	private void updateTypes(IValue key, IValue value) {
	    ValueEqualsWrapper kw = new ValueEqualsWrapper(key);
        if (mapContent.containsKey(kw)) {
	        // key will be overwritten, so the corresponding value must be subtracted from the type bag too
	        valTypeBag = valTypeBag.decrease(mapContent.get(kw).getType());
	        valTypeBag = valTypeBag.increase(value.getType());
	    }
	    else {
	        keyTypeBag = keyTypeBag.increase(key.getType());
	        valTypeBag = valTypeBag.increase(value.getType());
	    }
	}

	@Override
	public void putAll(java.util.Map<IValue, IValue> map) throws FactTypeUseException{
		checkMutation();
		for(Entry<IValue, IValue> entry : map.entrySet()){
			IValue value = entry.getValue();
			updateTypes(entry.getKey(), value);
			mapContent.put(new ValueEqualsWrapper(entry.getKey()), value);
		}
	}

	@Override
	public void put(IValue key, IValue value) throws FactTypeUseException{
		checkMutation();
		updateTypes(key, value);
		mapContent.put(new ValueEqualsWrapper(key), value);
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
			constructedMap = new Map(IMap.TF.mapType(keyTypeBag.lub(), valTypeBag.lub()), mapContent);
		}

		return constructedMap;
	}	
	
	@Override
    public Supplier<IWriter<IMap>> supplier() {
        return () -> ValueFactory.getInstance().mapWriter();
    }
}
