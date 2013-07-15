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
 *   
 * Based on code from:
 *
 *   * org.eclipse.imp.pdb.facts.impl.fast.Node
 *******************************************************************************/
package org.eclipse.imp.pdb.facts.impl;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IAnnotatable;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.util.ShareableHashMap;


/**
 * A generic wrapper for an {@link IValue} that associates annotations to it. 
 *
 * @param <T> the interface over which this annotation wrapper closes
 */
public abstract class AbstractDefaultEmptyAnnotatable<T extends IValue> implements IAnnotatable<T> {

	protected final T content;
	protected final ShareableHashMap<String, IValue> annotations; // TODO: change to interface Map<String, IValue>
	
	public AbstractDefaultEmptyAnnotatable(T content) {
		this.content = content;
		this.annotations = new ShareableHashMap<>();
	}
	
	public AbstractDefaultEmptyAnnotatable(T content, ShareableHashMap<String, IValue> annotations) {
		this.content = content;
		this.annotations = annotations;
	}
	
	protected abstract T wrap(final T content, final ShareableHashMap<String, IValue> annotations);
	
	@Override
	public boolean hasAnnotations() {
		return annotations.size() > 0;
	}
	
	@Override
	public Map<String, IValue> getAnnotations() {
		return annotations;
	}
	
	@Override
	public T removeAnnotations() {
		return content;
	}
	
	@Override
	public boolean hasAnnotation(String label) throws FactTypeUseException {
		return annotations.contains(label);
	}
	
	@Override
	public IValue getAnnotation(String label) throws FactTypeUseException {
		return annotations.get(label);
	}

	@Override
	public T setAnnotation(String label, IValue newValue)
			throws FactTypeUseException {
		return wrap(content, getUpdatedAnnotations(label, newValue));
	}

	@Override
	public T removeAnnotation(String label) {
		return wrap(content, getUpdatedAnnotations(label));
	}
	
	@Override
	public T setAnnotations(Map<String, IValue> annotations) {
		return wrap(content, getSetAnnotations(annotations));
	}

	@Override
	public T joinAnnotations(Map<String, IValue> annotations) {
		return wrap(content, getUpdatedAnnotations(annotations));
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(
			String label, IValue value) {
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
		newAnnotations.put(label, value);
		return newAnnotations;
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(
			String label) {
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
		newAnnotations.remove(label);
		return newAnnotations;
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(
			Map<String, IValue> newAnnos) {
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
		
		Iterator<Map.Entry<String, IValue>> newAnnosIterator = newAnnos.entrySet().iterator();
		while(newAnnosIterator.hasNext()){
			Map.Entry<String, IValue> entry = newAnnosIterator.next();
			String key = entry.getKey();
			IValue value = entry.getValue();
			
			newAnnotations.put(key, value);
		}
		
		return newAnnotations;
	}
	
	protected ShareableHashMap<String, IValue> getSetAnnotations(
			Map<String, IValue> newAnnos) {
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
		
		Iterator<Map.Entry<String, IValue>> newAnnosIterator = newAnnos.entrySet().iterator();
		while(newAnnosIterator.hasNext()){
			Map.Entry<String, IValue> entry = newAnnosIterator.next();
			String key = entry.getKey();
			IValue value = entry.getValue();
			
			newAnnotations.put(key, value);
		}
		
		return newAnnotations;
	}
	
}
