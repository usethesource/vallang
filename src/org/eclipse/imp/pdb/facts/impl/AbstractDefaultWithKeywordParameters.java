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
	
	@Override
	public String toString() {
		return content.toString();
	}

  @Override
  public IValue getParameter(String label) throws FactTypeUseException {
    return parameters.get(label);
  }

  @Override
  public T setParameter(String label, IValue newValue) throws FactTypeUseException {
    return wrap(content, parameters.__put(label, newValue));
  }

  @Override
  public boolean hasParameter(String label) throws FactTypeUseException {
    return parameters.containsKey(label);
  }

  @Override
  public boolean hasParameters() {
    return parameters.size() > 0;
  }
  
  @Override
  public Set<String> getParameterNames() {
    return Collections.unmodifiableSet(parameters.keySet()); 
  }
  
  @Override
  public Map<String,IValue> getParameters() {
    return parameters;
  }
  
  @Override
  public boolean isEqual(IWithKeywordParameters<T> other) {
    if (!getClass().equals(other.getClass())) {
      return false;
    }
    
    AbstractDefaultWithKeywordParameters<? extends IValue> o = (AbstractDefaultWithKeywordParameters<?>) other;
    
    if (!content.isEqual(o.content)) {
      return false;
    }
    
    if (!parameters.keySet().equals(o.parameters.keySet())) {
      return false;
    }
    
    for (String key : parameters.keySet()) {
      if (!parameters.get(key).isEqual(o.parameters.get(key))) {
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
