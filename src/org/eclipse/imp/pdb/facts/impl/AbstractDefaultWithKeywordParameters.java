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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IWithKeywordParameters;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.util.AbstractSpecialisedImmutableMap;
import org.eclipse.imp.pdb.facts.util.ImmutableMap;


/**
 * A generic wrapper for an {@link IValue} that associates keyword parameters to it. 
 *
 * @param <T> the interface over which this parameter wrapper closes
 */
public abstract class AbstractDefaultWithKeywordParameters<T extends IValue> implements IWithKeywordParameters<T> {
  protected final T content;
	protected final ImmutableMap<String, IValue> parameters;
		
	/**
	 * Creates an {@link IWithKeywordParameters} view on {@literal content} with empty
	 * annotations.
	 * 
	 * @param content
	 *            is the wrapped object that supports annotations
	 */
	public AbstractDefaultWithKeywordParameters(T content) {
		this.content = content;
		this.parameters = AbstractSpecialisedImmutableMap.mapOf();
	}
	
	/**
	 * Creates an {@link IWithKeywordParameters} view on {@link #content} with already
	 * provided {@link #parameters}.
	 * 
	 * @param content
	 *            is the wrapped object that supports annotations
	 * @param parameters
	 *            is the map of annotations associated to {@link #content}
	 */
	public AbstractDefaultWithKeywordParameters(T content, ImmutableMap<String, IValue> parameters) {
		this.content = content;
		this.parameters = parameters;
	}
	
	/**
	 * Wraps {@link #content} with other parameters. This methods is mandatory
	 * because of PDB's immutable value nature: Once parameters are modified, a
	 * new immutable view is returned.
	 * 
	 * @param content
	 *            is the wrapped object that supports annotations
	 * @param annotations
	 *            is the map of annotations associated to {@link #content}
	 * @return a new representations of {@link #content} with associated
	 *         {@link #parameters}
	 */
	protected abstract T wrap(final T content, final ImmutableMap<String, IValue> parameters);
	
	protected abstract IValue getDefault(String label);
	
	@Override
	public String toString() {
		return content.toString();
	}

  @Override
  public IValue getParameter(String label) throws FactTypeUseException {
    IValue res = parameters.get(label);
    return res != null ? res : getDefault(label);
  }

  @Override
  public T setParameter(String label, IValue newValue) throws FactTypeUseException {
    return wrap(content, parameters.__put(label, newValue));
  }

  @Override
  public boolean hasParameter(String label) throws FactTypeUseException {
    return parameters.containsKey(label) 
        || (content.getType().hasKeywordParameters() && content.getType().hasKeywordParameter(label));
  }

  @Override
  public boolean hasParameters() {
    return parameters.size() > 0;
  }
  
  @Override
  public Set<String> getParameterNames() {
    Set<String> names = new HashSet<>();
    if (content.getType().hasKeywordParameters()) {
      names.addAll(content.getType().getKeywordParameters());
    }
    return Collections.unmodifiableSet(parameters.keySet()); 
  }
  
  @Override
  public Map<String,IValue> getParameters() {
    Map<String,IValue> params = new HashMap<>();
    if (content.getType().hasKeywordParameters()) {
      params.putAll(content.getType().getKeywordParameterDefaults());
    }
    params.putAll(parameters);
    return Collections.unmodifiableMap(params);
  }
  
  @Override
  public boolean equals(Object other) {
    if (!getClass().equals(other.getClass())) {
      return false;
    }
    
    AbstractDefaultWithKeywordParameters<? extends IValue> o = (AbstractDefaultWithKeywordParameters<?>) other;
    
    if (!content.isEqual(o.content)) {
      return false;
    }
    
    Set<String> a = getParameterNames();
    Set<String> b = o.getParameterNames();
    if (!a.equals(b)) {
      return false;
    }
    
    for (String key : a) {
      if (!getParameter(key).equals(o.getParameter(key))) {
        return false;
      }
    }
    
    return true;
  }
  
  @Override
  public <U extends IWithKeywordParameters<? extends IValue>> boolean equalParameters(U other) {
    if (!(other instanceof AbstractDefaultWithKeywordParameters<?>)) {
      return false;
    }
    
    AbstractDefaultWithKeywordParameters<? extends IValue> o = (AbstractDefaultWithKeywordParameters<?>) other;
    
    // it is important to go through the public API here, since
    // default parameters may be retrieved from the types instead
    // of from the fields of the current wrapper class
    Set<String> a = getParameterNames();
    Set<String> b = o.getParameterNames();
    
    if (!a.equals(b)) {
      return false;
    }
    
    for (String key : a) {
      // TODO: isEqual should become equals when annotations have been removed.
      if (!getParameter(key).isEqual(o.getParameter(key))) {
        return false;
      }
    }
    
    return true;
  }
  
  @Override
  public T setParameters(Map<String, IValue> params) {
    return wrap(content, AbstractSpecialisedImmutableMap.mapOf(params));
  }
}
