/*******************************************************************************
 * Copyright (c) 2009-2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package io.usethesource.treasure.impl;

import io.usethesource.treasure.IValue;
import io.usethesource.treasure.IWriter;
import io.usethesource.treasure.exceptions.FactTypeUseException;

public abstract class AbstractWriter implements IWriter {
	public void insertAll(Iterable<? extends IValue> collection) throws FactTypeUseException {
		for (IValue v : collection) {
			insert(v);
		}
	}
}
