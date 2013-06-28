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

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.impl.AbstractMap;
import org.eclipse.imp.pdb.facts.impl.func.MapFunctions;
import org.eclipse.imp.pdb.facts.type.Type;

import java.util.Iterator;
import java.util.Map.Entry;

/*package*/ class Map extends AbstractMap {

	final Type type;
	final java.util.Map<IValue, IValue> content;

	/*package*/ Map(Type candidateMapType, java.util.Map<IValue, IValue> content) {
		super();
		this.type = inferMapType(candidateMapType, content);
		this.content = content;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	protected IValueFactory getValueFactory() {
		return ValueFactory.getInstance();
	}

	public boolean isEmpty() {
		return content.isEmpty();
	}

	public int size() {
		return content.size();
	}

	public IValue get(IValue key) {
		return content.get(key);
	}

	public Iterator<IValue> iterator() {
		return content.keySet().iterator();
	}

	public Iterator<IValue> valueIterator() {
		return content.values().iterator();
	}

	public Iterator<Entry<IValue, IValue>> entryIterator() {
		return content.entrySet().iterator();
	}

	@Override
	public int hashCode() {
		return content.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return MapFunctions.equals(getValueFactory(), this, other);
	}

	@Override
	public boolean isEqual(IValue other) {
		return MapFunctions.isEqual(getValueFactory(), this, other);
	}

}
