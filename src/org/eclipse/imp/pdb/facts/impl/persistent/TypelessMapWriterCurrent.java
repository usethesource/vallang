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
package org.eclipse.imp.pdb.facts.impl.persistent;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.util.TransientMap;
import org.eclipse.imp.pdb.facts.util.DefaultTrieMap;

/*
 * Operates:
 * 		* without types
 * 		* with equals() instead of isEqual()
 */
final class TypelessMapWriterCurrent implements IMapWriter {

	protected final TransientMap<IValue, IValue> mapContent;
	protected IMap constructedMap;

	TypelessMapWriterCurrent() {
		super();

		mapContent = DefaultTrieMap.transientOf();
		constructedMap = null;
	}

	@Override
	public void put(IValue key, IValue value) {
		checkMutation();

		@SuppressWarnings("unused")
		final IValue replaced = mapContent.__put(key, value);
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
			final Entry<IValue, IValue> entry = entryIterator.next();
			final IValue key = entry.getKey();
			final IValue value = entry.getValue();

			@SuppressWarnings("unused")
			final IValue replaced = mapContent.__put(key, value);
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
			constructedMap = new TypelessPDBPersistentHashMap(mapContent.freeze());
		}

		return constructedMap;
	}
}
