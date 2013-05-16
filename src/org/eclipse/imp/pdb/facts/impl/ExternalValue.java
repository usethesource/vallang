/*******************************************************************************
* Copyright (c) 2009 CWI.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Jurgen Vinju - initial API and implementation

*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl;

import org.eclipse.imp.pdb.facts.IExternalValue;
import org.eclipse.imp.pdb.facts.type.ExternalType;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * See {@link IExternalValue}
 * <br>
 * Note that NORMAL USE OF THE PDB DOES NOT REQUIRE EXTENDING THIS CLASS.
 */
public abstract class ExternalValue extends Value implements IExternalValue {
	protected ExternalValue(ExternalType type) {
		super(type);
	}

	public <T, E extends Throwable> T accept(IValueVisitor<T,E> v) throws E {
		return v.visitExternal(this);
	}
}
