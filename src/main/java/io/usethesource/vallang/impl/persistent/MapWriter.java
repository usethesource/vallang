/*******************************************************************************
 * Copyright (c) 2013-2014 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package io.usethesource.vallang.impl.persistent;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import io.usethesource.capsule.Map;
import io.usethesource.vallang.IMap;
import io.usethesource.vallang.IMapWriter;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IWriter;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.util.AbstractTypeBag;

final class MapWriter implements IMapWriter {

	private AbstractTypeBag keyTypeBag = AbstractTypeBag.of();
	private AbstractTypeBag valTypeBag = AbstractTypeBag.of();
	private final Map.Transient<IValue, IValue> mapContent = Map.Transient.of();

	private @MonotonicNonNull IMap constructedMap;

	@Override
	public void put(IValue key, IValue value) {
		checkMutation();

		final Type keyType = key.getType();
		final Type valType = value.getType();

		int oldSize = mapContent.size();
		
		final IValue replaced = mapContent.__put(key, value);

		if (oldSize == mapContent.size() && replaced == null) {
		    // update nothing because they key/value pair was already there
		}
		else if (replaced != null) {
		    // only update the val since the key was already there
		    valTypeBag = valTypeBag.decrease(replaced.getType()).increase(valType);
		} 
		else {
		    // add the new entry for both bags since its entirely new
		    keyTypeBag = keyTypeBag.increase(keyType); 
		    valTypeBag = valTypeBag.increase(valType);
		}

		assert mapContent.size() == keyTypeBag.sum();
	}

    @Override
	public void putAll(IMap map) {
		putAll(map.entryIterator());
	}

	@Override
	public void putAll(java.util.Map<IValue, IValue> map) {
		putAll(map.entrySet().iterator());
	}

	private void putAll(Iterator<Entry<IValue, IValue>> entryIterator) {
		checkMutation();

		while (entryIterator.hasNext()) {
			Entry<IValue, IValue> entry = entryIterator.next();
			IValue key = entry.getKey();
			IValue value = entry.getValue();

			this.put(key, value);
		}
	}

	@Override
	public void insert(IValue... values) {
		insertAll(Arrays.asList(values));
	}

	@Override
	public void insertAll(Iterable<? extends IValue> collection) {
		checkMutation();

		Iterator<? extends IValue> collectionIterator = collection.iterator();
		while (collectionIterator.hasNext()) {
			final Object item = collectionIterator.next();

			if (!(item instanceof ITuple)) {
				throw new IllegalArgumentException("Argument must be of ITuple type.");
			}

			final ITuple tuple = (ITuple) item;

			if (tuple.arity() != 2) {
				throw new IllegalArgumentException("Tuple must have an arity of 2.");
			}

			put(tuple.get(0), tuple.get(1));
		}
	}

	protected void checkMutation() {
		if (constructedMap != null) {
			throw new UnsupportedOperationException("Mutation of a finalized map is not supported.");
		}
	}

	@Override
	public IMap done() {
		if (constructedMap == null) {
			constructedMap = new PersistentHashMap(keyTypeBag, valTypeBag, mapContent.freeze());
		}

		return constructedMap;
	}

    @Override
    public void insertTuple(IValue... fields) {
        insert(ValueFactory.getInstance().tuple(fields));
    }

    @Override
    public Iterator<IValue> iterator() {
        return mapContent.keyIterator();
    }
    
    @Override
    public IValue get(IValue key) {
        return mapContent.get(key);
    }

    @Override
    public Supplier<IWriter<IMap>> supplier() {
        return () -> ValueFactory.getInstance().mapWriter();
    }
}
