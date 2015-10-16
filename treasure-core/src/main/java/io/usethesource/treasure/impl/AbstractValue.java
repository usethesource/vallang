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
package io.usethesource.treasure.impl;

import io.usethesource.treasure.IAnnotatable;
import io.usethesource.treasure.IValue;
import io.usethesource.treasure.IWithKeywordParameters;
import io.usethesource.treasure.exceptions.IllegalOperationException;
import io.usethesource.treasure.io.StandardTextWriter;
import io.usethesource.treasure.type.Type;
import io.usethesource.treasure.visitors.IValueVisitor;

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

	public String toString() {
		return StandardTextWriter.valueToString(this);
	}

}
