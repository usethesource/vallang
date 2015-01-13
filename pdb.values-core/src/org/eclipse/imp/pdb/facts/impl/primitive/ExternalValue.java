/*******************************************************************************
 * Copyright (c) 2009-2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Jurgen Vinju - initial API and implementation
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.primitive;

import org.eclipse.imp.pdb.facts.IExternalValue;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.AbstractValue;
import org.eclipse.imp.pdb.facts.type.ExternalType;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;

/**
 * See {@link IExternalValue}
 * <br>
 * Note that NORMAL USE OF THE PDB DOES NOT REQUIRE EXTENDING THIS CLASS.
 */
public abstract class ExternalValue extends AbstractValue implements IExternalValue {

	private final Type type;

	@Override
	public boolean isEqual(IValue other) {
		return equals(other);
	}

	@Override
	public Type getType() {
		return type;
	}

	protected ExternalValue(ExternalType type) {
		this.type = type;
	}

	@Override
	public <T, E extends Throwable> T accept(IValueVisitor<T,E> v) throws E {
		return v.visitExternal(this);
	}
}
