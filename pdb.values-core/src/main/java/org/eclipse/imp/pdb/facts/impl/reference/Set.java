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
 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Paul Klint - Paul.Klint@cwi.nl - CWI
 *
 * Based on code by:
 *
 *   * Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
 *******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.reference;

import java.util.Iterator;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.impl.AbstractSet;
import org.eclipse.imp.pdb.facts.impl.func.SetFunctions;
import org.eclipse.imp.pdb.facts.type.Type;

/*package*/ class Set extends AbstractSet {

	final Type type;
	final java.util.Set<IValue> content;

	/*package*/ Set(Type elementType, java.util.Set<IValue> content) {
		super();
		this.type = inferSetOrRelType(elementType, content);
		this.content = content;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public boolean isEmpty() {
		return content.isEmpty();
	}

	@Override
	public int size() {
		return content.size();
	}

	@Override
	public boolean contains(IValue e) {
		return content.contains(e);
	}

	@Override
	public int hashCode() {
		return content.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return SetFunctions.equals(getValueFactory(), this, other);
	}

	@Override
	public boolean isEqual(IValue other) {
		return SetFunctions.isEqual(getValueFactory(), this, other);
	}

	@Override
	protected IValueFactory getValueFactory() {
		return ValueFactory.getInstance();
	}

	@Override
	public Iterator<IValue> iterator() {
		return content.iterator();
	}

}
