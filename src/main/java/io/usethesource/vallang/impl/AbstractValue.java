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
package io.usethesource.vallang.impl;

import io.usethesource.vallang.IAnnotatable;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.exceptions.IllegalOperationException;
import io.usethesource.vallang.visitors.IValueVisitor;
import io.usethesource.vallang.IWithKeywordParameters;
import io.usethesource.vallang.io.StandardTextWriter;
import io.usethesource.vallang.type.Type;

public abstract class AbstractValue implements IValue {

	protected AbstractValue() {
		super();
	}

	@Override
	public boolean isAnnotatable() {
		return false;
	}

	@Override
	public IAnnotatable<? extends IValue> asAnnotatable() {
		throw new IllegalOperationException("Cannot be viewed as annotatable.", getType());
	}

	@Override
	public boolean mayHaveKeywordParameters() {
		return false;
	}

	@Override
	public IWithKeywordParameters<? extends IValue> asWithKeywordParameters() {
		throw new IllegalOperationException("Cannot be viewed as with keyword parameters.",
				getType());
	}

	@Override
	public Type getType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEqual(IValue other) {
		return equals(other);
	}
	
	@Override
    public boolean match(IValue other) {
        return equals(other);
    }

	public String toString() {
		return StandardTextWriter.valueToString(this);
	}

}
