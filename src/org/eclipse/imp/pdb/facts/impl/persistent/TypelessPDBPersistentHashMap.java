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

import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.impl.AbstractMap;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.util.ImmutableMap;

/*
 * Operates:
 * 		* without types
 * 		* with equals() instead of isEqual()
 */
public final class TypelessPDBPersistentHashMap extends AbstractMap {
		
	private final ImmutableMap<IValue,IValue> content; 
	
	protected TypelessPDBPersistentHashMap(ImmutableMap<IValue, IValue> content) {
		this.content = content;
	}
	
	@Override
	protected IValueFactory getValueFactory() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Type getType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		return content.isEmpty();
	}

	@Override
	public int size() {
		return content.size();
	}
	
	@Override
	public IMap put(IValue key, IValue value) {
//		final ImmutableMap<IValue,IValue> contentNew = 
//				content.__put(key, value);
//		
//		if (content == contentNew)
//			return this;
//
//		return new TypelessPDBPersistentHashMap(contentNew);
		
		return new TypelessPDBPersistentHashMap(content.__put(key, value));
	}
	
	public IMap removeKey(IValue key) {
		return new TypelessPDBPersistentHashMap(content.__remove(key));
	}
	
	@Override
	public boolean containsKey(IValue key) {
		return content.containsKey(key);
	}

	@Override
	public boolean containsValue(IValue value) {
		return content.containsValue(value);
	}
	
	@Override
	public IValue get(IValue key) {
		return content.get(key);
	}

	@Override
	public int hashCode() {
		return content.hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (other == null)
			return false;
		
		if (other instanceof TypelessPDBPersistentHashMap) {
			TypelessPDBPersistentHashMap that = (TypelessPDBPersistentHashMap) other;

			if (this.size() != that.size())
				return false;
			
			return content.equals(that.content);
		}
		
		return false;
	}
	
	@Override
	public boolean isEqual(IValue other) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<IValue> iterator() {
		return content.keyIterator();
	}
	
	@Override
	public Iterator<IValue> valueIterator() {
		return content.valueIterator();
	}

	@Override
	public Iterator<Entry<IValue, IValue>> entryIterator() {
		return content.entryIterator();
	}

	@Override
	public IMap join(IMap that) {
		// TODO Auto-generated method stub
		return super.join(that);
	}

	@Override
	public IMap remove(IMap that) {
		// TODO Auto-generated method stub
		return super.remove(that);
	}

	@Override
	public IMap compose(IMap that) {
		// TODO Auto-generated method stub
		return super.compose(that);
	}

	@Override
	public IMap common(IMap that) {
		// TODO Auto-generated method stub
		return super.common(that);
	}

	@Override
	public boolean isSubMap(IMap that) {
		// TODO Auto-generated method stub
		return super.isSubMap(that);
	}

}
