/*******************************************************************************
 * Copyright (c) 2013-2014 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI  
 *******************************************************************************/
package io.usethesource.vallang.impl.persistent;

import io.usethesource.vallang.IMapWriter;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.IValue;

public class ValueFactory extends io.usethesource.vallang.impl.fast.ValueFactory {

	protected ValueFactory() {
		super();
	}

	private static class InstanceKeeper {
		public final static ValueFactory instance = new ValueFactory();
	}

	public static ValueFactory getInstance() {
		return InstanceKeeper.instance;
	}

	@Override
	public ISetWriter setWriter() {
		return new SetWriter((a,b) -> tuple(a,b));
	}

	@Override
	public ISet set(IValue... elements) {
		ISetWriter setWriter = setWriter();
		setWriter.insert(elements);
		return setWriter.done();
	}

	@Override
	public IMapWriter mapWriter() {
		return new MapWriter();
	}

	@Override
	public String toString() {
		return "VF_PDB_PERSISTENT";
	}
}
