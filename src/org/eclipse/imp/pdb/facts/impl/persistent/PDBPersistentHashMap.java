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
import org.eclipse.imp.pdb.facts.util.AbstractTypeBag;
import org.eclipse.imp.pdb.facts.util.EqualityUtils;
import org.eclipse.imp.pdb.facts.util.ImmutableMap;

public final class PDBPersistentHashMap extends AbstractMap {
		
	@SuppressWarnings("unchecked")
	private static final Comparator<Object> equivalenceComparator = EqualityUtils.getEquivalenceComparator();
	
	private Type cachedMapType;
	private final AbstractTypeBag keyTypeBag; 
	private final AbstractTypeBag valTypeBag;
	private final ImmutableMap<IValue,IValue> content; 
	
	/* 
	 * Passing an pre-calulated map type is only allowed from inside this class.
	 */
	protected PDBPersistentHashMap(AbstractTypeBag keyTypeBag,
			AbstractTypeBag valTypeBag, ImmutableMap<IValue, IValue> content) {
		Objects.requireNonNull(content);
		this.cachedMapType = null;
		this.keyTypeBag = keyTypeBag;
		this.valTypeBag = valTypeBag;
		this.content = content;
	}
	
	@Override
	protected IValueFactory getValueFactory() {
		return ValueFactory1.getInstance();
	}

	@Override
	public Type getType() {
		if (cachedMapType == null) {
			final Type keyType = keyTypeBag.lub();
			final Type valType = valTypeBag.lub();
	
			final String keyLabel = keyTypeBag.getLabel();
			final String valLabel = valTypeBag.getLabel();

			if (keyLabel != null && valLabel != null) {
				cachedMapType = getTypeFactory().mapType(keyType, keyLabel, valType, valLabel);
			} else { 
				cachedMapType = getTypeFactory().mapType(keyType, valType);
			}
		}
		return cachedMapType;		
	}

	@Override
	public boolean isEmpty() {
		return content.isEmpty();
	}

	@Override
	public IMap put(IValue key, IValue value) {
		final ImmutableMap<IValue,IValue> contentNew = 
				content.__putEquivalent(key, value, equivalenceComparator);
		
		if (content == contentNew)
			return this;

		final AbstractTypeBag keyBagNew = keyTypeBag.clone();
		final AbstractTypeBag valBagNew = valTypeBag.clone();

		if (content.size() == contentNew.size()) {
			// value replaced
			final IValue replaced = content.getEquivalent(key, equivalenceComparator);
			valBagNew.decrease(replaced.getType());
			valBagNew.increase(value.getType());
		} else {
			// pair added
			keyBagNew.increase(key.getType());			
			valBagNew.increase(value.getType());
		}
		
		return new PDBPersistentHashMap(keyBagNew, valBagNew, contentNew);
	}
	
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
		return content.getEquivalent(key, equivalenceComparator);
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

			if (this.getType() != that.getType())
				return false;
			
			if (this.size() != that.size())
				return false;
			
			for (IValue e : that) {
	            if (!content.containsKey(e)) {
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

			for (IValue e : that) {
				if (!containsKey(e)) {
					return false;
				} else if (!get(e).isEqual(that.get(e))) {
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
