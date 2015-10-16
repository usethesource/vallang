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
package io.usethesource.treasure.impl.persistent;

import io.usethesource.treasure.IMap;
import io.usethesource.treasure.IMapWriter;
import io.usethesource.treasure.ISet;
import io.usethesource.treasure.ISetWriter;
import io.usethesource.treasure.IValue;
import io.usethesource.treasure.type.Type;
import io.usethesource.treasure.type.TypeFactory;

public class ValueFactory extends io.usethesource.treasure.impl.fast.ValueFactory {

	protected ValueFactory() {
		super();
	}

	private static class InstanceKeeper {
		public final static ValueFactory instance = new ValueFactory();
	}

	public static ValueFactory getInstance() {
		return InstanceKeeper.instance;
	}

	public ISetWriter setWriter(Type elementType) {
		return new SetWriter(elementType);
	}

	public ISetWriter setWriter() {
		return new SetWriter();
	}

	public ISetWriter relationWriter(Type tupleType) {
		return new SetWriter(tupleType);
	}

	public ISetWriter relationWriter() {
		return new SetWriter();
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
	public IMapWriter mapWriter(Type keyType, Type valueType) {
		return mapWriter(TypeFactory.getInstance().mapType(keyType, valueType));
	}

	@Override
	public IMapWriter mapWriter(Type mapType) {
		return new MapWriter(mapType);
	}

	@Override
	public IMapWriter mapWriter() {
		return new MapWriter();
	}

	@Override
	public IMap map(Type mapType) {
		return mapWriter(mapType).done();
	}

	@Override
	public IMap map(Type keyType, Type valueType) {
		return mapWriter(keyType, valueType).done();
	}

	@Override
	public String toString() {
		return "VF_PDB_PERSISTENT";
	}

}
