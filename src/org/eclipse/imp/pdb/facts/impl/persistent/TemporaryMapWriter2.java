/*******************************************************************************
* Copyright (c) 2009, 2012 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Arnold Lankamp - interfaces and implementation
*    Anya Helene Bagge - labels
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.persistent;

import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedElementTypeException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.util.AbstractTypeBag;
import org.eclipse.imp.pdb.facts.util.TransientMap;
import org.eclipse.imp.pdb.facts.util.TrieMap;

// TODO Add checking.
/**
 * Implementation of IMapWriter.
 * 
 * @author Arnold Lankamp
 */
/*package*/ class TemporaryMapWriter2 implements IMapWriter{
	protected final AbstractTypeBag keyTypeBag;
	protected final AbstractTypeBag valTypeBag;
	protected final TransientMap<IValue,IValue> mapContent;
	
	protected final boolean checkUpperBound;
	protected final Type upperBoundKeyType;
	protected final Type upperBoundValType;
	protected IMap constructedMap;
	
	/*package*/ TemporaryMapWriter2() {
		super();
		
		this.checkUpperBound = false;
		this.upperBoundKeyType = null;
		this.upperBoundValType = null;

		keyTypeBag = AbstractTypeBag.of();
		valTypeBag = AbstractTypeBag.of();
		mapContent = TrieMap.transientOf();
		constructedMap = null;
	}
	
	/*package*/ TemporaryMapWriter2(Type prototypeType) {
		super();
		
//		/*
//		 * TODO: msteindorfer: review this (legacy?) code snippet. I don't know
//		 * exactly about its semantics.
//		 */
//		if(mapType.isFixedWidth() && mapType.getArity() >= 2) {
//			mapType = TypeFactory.getInstance().mapTypeFromTuple(mapType);
//		}
		
		this.checkUpperBound = false;
		this.upperBoundKeyType = prototypeType.getKeyType();
		this.upperBoundValType = prototypeType.getValueType();

		if (prototypeType.hasFieldNames()) {
			keyTypeBag = AbstractTypeBag.of(prototypeType.getKeyLabel());
			valTypeBag = AbstractTypeBag.of(prototypeType.getValueLabel());			
		} else {
			keyTypeBag = AbstractTypeBag.of();
			valTypeBag = AbstractTypeBag.of();
		}

		mapContent = TrieMap.transientOf();
		constructedMap = null;		
	}
	
	@Override
	public void put(IValue key, IValue value){
		checkMutation();
		
		final Type keyType = key.getType();
		final Type valType = value.getType();

		if (checkUpperBound) {
			if (!keyType.isSubtypeOf(upperBoundKeyType))
				throw new UnexpectedElementTypeException(upperBoundKeyType, keyType);
			
			if (!valType.isSubtypeOf(upperBoundValType))
				throw new UnexpectedElementTypeException(upperBoundValType, valType);
		}

		// TODO: it is currently not possible to observe if a value was
		// replaced, thus the least upper bound of the value type might be
		// incorrect
		mapContent.__put(key, value);
		
		keyTypeBag.increase(keyType);
		valTypeBag.increase(valType);
	}
	
	@Override
	public void putAll(IMap map){
		checkMutation();
		
		Iterator<Entry<IValue, IValue>> entryIterator = map.entryIterator();
		while(entryIterator.hasNext()){
			Entry<IValue, IValue> entry = entryIterator.next();
			IValue key = entry.getKey();
			IValue value = entry.getValue();
			
			this.put(key, value);			
		}
	}
	
	@Override
	public void putAll(java.util.Map<IValue, IValue> map){
		checkMutation();
		
		Iterator<Entry<IValue, IValue>> entryIterator = map.entrySet().iterator();
		while(entryIterator.hasNext()){
			Entry<IValue, IValue> entry = entryIterator.next();
			IValue key = entry.getKey();
			IValue value = entry.getValue();
			
			this.put(key, value);			
		}
	}
	
	@Override
	public void insert(IValue... values){
		checkMutation();
		
		for(int i = values.length - 1; i >= 0; i--){
			IValue value = values[i];
			
			if(!(value instanceof ITuple)) throw new IllegalArgumentException("Argument must be of ITuple type.");
			
			ITuple tuple = (ITuple) value;
			
			if(tuple.arity() != 2) throw new IllegalArgumentException("Tuple must have an arity of 2.");
			
			IValue key = tuple.get(0);
			IValue value2 = tuple.get(1);
			
			this.put(key, value2);
		}
	}
	
	@Override
	public void insertAll(Iterable<? extends IValue> collection){
		checkMutation();
		
		Iterator<? extends IValue> collectionIterator = collection.iterator();
		while(collectionIterator.hasNext()){
			IValue value = collectionIterator.next();
			
			if(!(value instanceof ITuple)) throw new IllegalArgumentException("Argument must be of ITuple type.");
			
			ITuple tuple = (ITuple) value;
			
			if(tuple.arity() != 2) throw new IllegalArgumentException("Tuple must have an arity of 2.");
			
			IValue key = tuple.get(0);
			IValue value2 = tuple.get(1);

			this.put(key, value2);
		}
	}
	
	protected void checkMutation() {
		if (constructedMap != null)
			throw new UnsupportedOperationException(
					"Mutation of a finalized map is not supported.");
	}
	
	@Override
	public IMap done(){
		if (constructedMap == null) {
			constructedMap = new PDBPersistentHashMap(keyTypeBag, valTypeBag, mapContent.freeze());
		}

		return constructedMap;
	}
}
