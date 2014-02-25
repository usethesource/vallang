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

import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;

public interface IWithKeywordParameters<T extends IValue> {

	/**
	 * Get the value of a parmeter
	 * 
	 * @param label identifies the parameter
	 * @return a value if the parameter has a value on this node or null otherwise
	 */
	public IValue getParameter(String label) throws FactTypeUseException;
	
	/**
	 * Set the value of an parameter
	 * 
	 * @param label identifies the parameter
	 * @param newValue the new value for the parameter
	 * @return a new node where the value of the parameter is replaced (if previously present) or newly added
	 * @throws FactTypeUseException when the type of the new value is not comparable to the old parameter value
	 */
	public T setParameter(String label, IValue newValue) throws FactTypeUseException;

	/**
	 * Check whether a certain parameter is set.
	 * 
	 * @param label identifies the parameter
	 * @return true iff the parameter has a value on this node
	 * @throws FactTypeUseException when no parameter with this label is defined for this type of node.
	 */
	public boolean hasParameter(String label) throws FactTypeUseException;

	/**
	 * Check whether any parameters are present.
	 */
	public boolean hasParameters();
}
