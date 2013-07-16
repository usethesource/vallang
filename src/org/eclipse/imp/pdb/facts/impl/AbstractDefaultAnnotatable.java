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
 *******************************************************************************/
package org.eclipse.imp.pdb.facts.impl;

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
public abstract class AbstractDefaultAnnotatable<T extends IValue> implements IAnnotatable<T> {

	protected final T content;
	protected final ShareableHashMap<String, IValue> annotations; // TODO: change to interface Map<String, IValue>
		
	/**
	 * Creates an {@link IAnnotatable} view on {@literal content} with empty
	 * annotations.
	 * 
	 * @param content
	 *            is the wrapped object that supports annotations
	 */
	public AbstractDefaultAnnotatable(T content) {
		this.content = content;
		this.annotations = new ShareableHashMap<>();
	}
	
	/**
	 * Creates an {@link IAnnotatable} view on {@link #content} with already
	 * provided {@link #annotations}.
	 * 
	 * @param content
	 *            is the wrapped object that supports annotations
	 * @param annotations
	 *            is the map of annotations associated to {@link #content}
	 */
	public AbstractDefaultAnnotatable(T content, ShareableHashMap<String, IValue> annotations) {
		this.content = content;
		this.annotations = annotations;
	}
	
	/**
	 * Wraps {@link #content} with other annotations. This methods is mandatory
	 * because of PDB's immutable value nature: Once annotations are modified, a
	 * new immutable view is returned.
	 * 
	 * @param content
	 *            is the wrapped object that supports annotations
	 * @param annotations
	 *            is the map of annotations associated to {@link #content}
	 * @return a new representations of {@link #content} with associated
	 *         {@link #annotations}
	 */
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
		return wrap(content, cloneAnnotationsAndPut(label, newValue));
	}

	@Override
	public T removeAnnotation(String label) {
		return wrap(content, cloneAnnotationsAndRemove(label));
	}
	
	@Override
	public T setAnnotations(Map<String, IValue> otherAnnotations) {
		return wrap(content, replaceAnnotationsWith(otherAnnotations));
	}

	@Override
	public T joinAnnotations(Map<String, IValue> otherAnnotations) {
		return wrap(content, cloneAnnotationsAndJoinWith(otherAnnotations));
	}
	
	protected ShareableHashMap<String, IValue> cloneAnnotationsAndPut(
			String label, IValue value) {
		final ShareableHashMap<String, IValue> newAnnotations = 
				new ShareableHashMap<>(annotations);
				
		newAnnotations.put(label, value);
		return newAnnotations;
	}

	protected ShareableHashMap<String, IValue> cloneAnnotationsAndRemove(
			String label) {
		final ShareableHashMap<String, IValue> newAnnotations = 
				new ShareableHashMap<>(annotations);

		newAnnotations.remove(label);
		return newAnnotations;
	}
	
	protected ShareableHashMap<String, IValue> cloneAnnotationsAndJoinWith(
			Map<String, IValue> otherAnnotations) {
		final ShareableHashMap<String, IValue> newAnnotations = 
				new ShareableHashMap<>(annotations);
		
		for (Map.Entry<String, IValue> entry : otherAnnotations.entrySet())
			newAnnotations.put(entry.getKey(), entry.getValue());

		return newAnnotations;
	}
	
	protected ShareableHashMap<String, IValue> replaceAnnotationsWith(
			Map<String, IValue> otherAnnotations) {
		final ShareableHashMap<String, IValue> newAnnotations = 
				new ShareableHashMap<>();

		for (Map.Entry<String, IValue> entry : otherAnnotations.entrySet())
			newAnnotations.put(entry.getKey(), entry.getValue());
						
		return newAnnotations;
	}
	
//	@Override
//	public int hashCode() {
//		// TODO
//	}
//
//	@Override
//	public boolean equals(Object obj) {
//		// TODO
//	}	
	
}
