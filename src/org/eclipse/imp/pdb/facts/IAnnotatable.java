/*******************************************************************************
 * Copyright (c) 2008-2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI  
 *******************************************************************************/
package org.eclipse.imp.pdb.facts;

import java.util.Map;

import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;

public interface IAnnotatable<T extends IValue> {

	/**
	 * Get the value of an annotation
	 * 
	 * @param label identifies the annotation
	 * @return a value if the annotation has a value on this node or null otherwise
	 */
	public IValue getAnnotation(String label) throws FactTypeUseException;
	
	/**
	 * Set the value of an annotation
	 * 
	 * @param label identifies the annotation
	 * @param newValue the new value for the annotation
	 * @return a new node where the value of the annotation is replaced (if previously present) or newly added
	 * @throws FactTypeUseException when the type of the new value is not comparable to the old annotation value
	 */
	public T setAnnotation(String label, IValue newValue) throws FactTypeUseException;

	/**
	 * Check whether a certain annotation is set.
	 * 
	 * @param label identifies the annotation
	 * @return true iff the annotation has a value on this node
	 * @throws FactTypeUseException when no annotation with this label is defined for this type of node.
	 */
	public boolean hasAnnotation(String label) throws FactTypeUseException;

	/**
	 * Check whether any annotations are present.
	 */
	public boolean hasAnnotations();
	
	/**
	 * @return a map of annotation labels to annotation values.
	 */
	public Map<String,IValue> getAnnotations();
	
	/**
	 * Overwrites all annotations by replacing them with new ones.
	 * @param annotations 
	 * @return a new node with new annotations
	 */
	public T setAnnotations(Map<String, IValue> annotations); 
	
	/**
	 * Adds a number of annotations to this value. The given annotations
	 * will overwrite existing ones if the keys are the same.
	 * 
	 * @param annotations 
	 * @return a new node with new annotations
	 */
	public T joinAnnotations(Map<String, IValue> annotations); 
	
	/**
	 * Remove the annotation identified by key
	 * @param key
	 * @return a new node without the given annotation
	 */
	public T removeAnnotation(String key);
	
	/**
	 * Remove all annotations on this IValue
	 * @return a new IValue without any annotations
	 */
	public T removeAnnotations();
	
}
