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
import java.util.Map.Entry;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.impl.AbstractMap;
import org.eclipse.imp.pdb.facts.type.Type;

/*package*/ class Map extends AbstractMap {

	final java.util.Map<IValue, IValue> content;

	/*package*/ Map(Type candidateMapType, java.util.Map<IValue, IValue> content){
		super(inferMapType(candidateMapType, content));
				
		this.content = content;
	}
	
	public int size() {
		return content.size();
	}

	public boolean isEmpty() {
		return content.isEmpty();
	}

	public IValue get(IValue key) {
		return content.get(key);
	}
	
	public Iterator<IValue> iterator() {
		return content.keySet().iterator();
	}

	public Iterator<Entry<IValue, IValue>> entryIterator() {
		return content.entrySet().iterator();
	}

	public Iterator<IValue> valueIterator() {
		return content.values().iterator();
	}

	@Override
	public int hashCode() {
		return content.hashCode();
	}

	@Override
	protected IValueFactory getValueFactory() {
		return ValueFactory.getInstance();
	}

}