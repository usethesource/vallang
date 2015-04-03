/*******************************************************************************
 * Copyright (c) 2009-2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Jurgen Vinju - interface and implementation
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package org.eclipse.imp.pdb.facts.impl;

import org.eclipse.imp.pdb.facts.IAnnotatable;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IWithKeywordParameters;
import org.eclipse.imp.pdb.facts.exceptions.IllegalOperationException;
import org.eclipse.imp.pdb.facts.io.StandardTextWriter;

public abstract class AbstractValue implements IValue {

	protected AbstractValue() {
		super();
	}

	public String toString() {
		return StandardTextWriter.valueToString(this);
	}

	@Override
	public boolean isAnnotatable() {
		return false;
	}

	@Override
	public IAnnotatable<? extends IValue> asAnnotatable() {
		throw new IllegalOperationException(
				"Cannot be viewed as annotatable.", getType());
	}
	
	 @Override
	  public boolean mayHaveKeywordParameters() {
	    return false;
	  }

	  @Override
	  public IWithKeywordParameters<? extends IValue> asWithKeywordParameters() {
	    throw new IllegalOperationException(
	        "Cannot be viewed as with keyword parameters.", getType());
	  }
	  
	
}
