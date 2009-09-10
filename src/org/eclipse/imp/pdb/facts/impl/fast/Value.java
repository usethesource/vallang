/*******************************************************************************
* Copyright (c) 2009 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Jurgen Vinju - interface and implementation
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.fast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.io.StandardTextWriter;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/*package*/ abstract class Value implements IValue {

	abstract public <T> T accept(IValueVisitor<T> v) throws VisitorException;

	abstract public Type getType();

	abstract public boolean isEqual(IValue other);

	@Override
	public String toString() {
		try {
    		ByteArrayOutputStream stream = new ByteArrayOutputStream();
    		new StandardTextWriter().write(this, stream);
			return stream.toString();
		} catch (IOException e) {
			// this never happens
			return null;
		} 
	}
}
