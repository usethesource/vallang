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

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.impl.AbstractMap;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.util.EqualityUtils;
import org.eclipse.imp.pdb.facts.util.ImmutableMap;
import org.eclipse.imp.pdb.facts.util.TrieMap;

public final class PDBPersistentHashMap extends AbstractMap {
	
//	private static final Object PLACEHOLDER = null; 
	
	@SuppressWarnings("unchecked")
	private static final Comparator<Object> equalityComparator = EqualityUtils.getDefaultEqualityComparator();
	
	@SuppressWarnings("unchecked")
	private static final Comparator<Object> equivalenceComparator = EqualityUtils.getEquivalenceComparator();
	
//	private Type cachedMapType;
	private Type inferredKeyType; // = getTypeFactory().voidType();
	private Type inferredValType; // = getTypeFactory().voidType();
	private final ImmutableMap<IValue,IValue> content;

//	public PDBPersistentHashMap() {
////		this.cachedMapType = null;
//		this.content = TrieMap.of();
//	}

	public PDBPersistentHashMap(Type keyType, Type valType, ImmutableMap<IValue,IValue> content) {
		Objects.requireNonNull(content);
		this.inferredKeyType = keyType;
		this.inferredValType = valType;
		this.content = content;
	}

	@Override
	protected IValueFactory getValueFactory() {
		return ValueFactory1.getInstance();
	}

	/*
	 * TODO: incorporate inferMapType(..)
	 */
	@Override
	public Type getType() {	
		// calculate dynamic element type
		if (inferredKeyType == null || inferredValType == null) {
			inferredKeyType = getTypeFactory().voidType();
			inferredValType = getTypeFactory().voidType();
			
			for (Entry<IValue,IValue> entry : content.entrySet()) {
				inferredKeyType = inferredKeyType.lub(entry.getKey().getType());
				inferredValType = inferredValType.lub(entry.getValue().getType());				
			}
			
//			cachedMapType = getTypeFactory().mapType(inferredKeyType, inferredValType);
		}
		
//		return cachedMapType;
		
		return getTypeFactory().mapType(inferredKeyType, inferredValType);
	}

	@Override
	public boolean isEmpty() {
		return content.isEmpty();
	}

	@Override
	public IMap put(IValue key, IValue value) {
		final ImmutableMap<IValue,IValue> contentNew = 
				content.__putEquivalent(key, value, equivalenceComparator);

		// TODO: clean-up quick-fix 
		final Type newKeyType = inferredKeyType.lub(key.getType());
		final Type newValType = inferredValType.lub(value.getType());
		
		if (content == contentNew)
			return this;

		return new PDBPersistentHashMap(newKeyType, newValType, contentNew);
	}

//	@Override
//	public ISet delete(IValue value) {
//		final ImmutableMap<IValue,Object> contentNew = 
//				content.__removeEquivalent(value, equivalenceComparator);
//
//		if (content == contentNew)
//			return this;
//
//		return new PDBPersistentHashMap(contentNew);
//	}

	@Override
	public int size() {
		return content.size();
	}

	@Override
	public boolean containsKey(IValue key) {
		return content.containsKeyEquivalent(key, equivalenceComparator);
	}

	@Override
	public IValue get(IValue key) {
		return content.get(key);
	}
	
	@Override
	public Iterator<IValue> iterator() {
		return content.keyIterator();
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
		
		if (other instanceof PDBPersistentHashMap) {
			PDBPersistentHashMap that = (PDBPersistentHashMap) other;

			if (this.size() != that.size())
				return false;

			return content.equals(that.content);
		}
		
		if (other instanceof IMap) {
			IMap that = (IMap) other;

			// not necessary because of tightly calculated dynamic types
//			if (this.getType() != that.getType())
//				return false;
			
			if (this.size() != that.size())
				return false;
			
	        // TODO: API is missing a containsAll() equivalent
			for (IValue e : that) {
	            if (!content.containsKeyEquivalent(e, equalityComparator)) {
	                return false;
	            } else if (!content.get(e).equals(that.get(e))) {
	            	return false;
	            }
			}
			
	        return true;			
		}
		
		return false;
	}
	
	@Override
	public boolean isEqual(IValue other) {
		if (other == this)
			return true;
		if (other == null)
			return false;
		
		if (other instanceof IMap) {
			IMap that = (IMap) other;
			
			if (this.size() != that.size())
				return false;
			
	        // TODO: API is missing a containsAll() equivalent
			for (IValue e : that) {
	            if (!content.containsKeyEquivalent(e, equivalenceComparator)) {
	                return false; 
	            } else if (!content.get(e).isEqual(that.get(e))) { // TODO: about interface: get has to lookup based on isEquals (use comparator)!!!
	            	return false;
	            }
			}
			
	        return true;			
		}
		
		return false;
	}

	@Override
	public Iterator<IValue> valueIterator() {
		// TODO: create TrieMapvalueIterator().
		return content.values().iterator();
	}

	@Override
	public Iterator<Entry<IValue, IValue>> entryIterator() {
		return content.entryIterator();
	}

}
