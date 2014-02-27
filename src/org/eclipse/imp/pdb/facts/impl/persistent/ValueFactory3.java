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
 *******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.persistent;

import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;

public class ValueFactory3 extends org.eclipse.imp.pdb.facts.impl.fast.ValueFactory {

	protected ValueFactory3() {
		super();
	}

	private static class InstanceKeeper {
		public final static ValueFactory3 instance = new ValueFactory3();
	}

	public static ValueFactory3 getInstance() {
		return InstanceKeeper.instance;
	}

	public ISetWriter setWriter(Type elementType) {
		return new TemporarySetWriter3(elementType);
	}

	public ISetWriter setWriter() {
		return new TemporarySetWriter3();
	}

	public ISetWriter relationWriter(Type tupleType) {
		return new TemporarySetWriter3(tupleType);
	}

	public ISetWriter relationWriter() {
		return new TemporarySetWriter3();
	}

	public ISet set(Type elementType) {
		return setWriter().done();
	}

	public ISet set(IValue... elements) {
		ISetWriter setWriter = setWriter();
		setWriter.insert(elements);
		return setWriter.done();
	}

	public ISet relation(Type tupleType) {
		return relationWriter(tupleType).done();
	}

	public ISet relation(IValue... elements) {
		return set(elements);
	}	
	
	@Override
	public String toString() {
		return "VF_PDB_PERSISTENT_3";
	}

}
