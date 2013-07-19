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
package org.eclipse.imp.pdb.facts.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArrayIterator<E> implements Iterator<E> {

	final E[] values;
	int currentIndex;

	public ArrayIterator(final E[] values) {
		this.values = values;
		this.currentIndex = 0;
	}

	@Override
	public boolean hasNext() {
		return currentIndex < values.length;
	}

	@Override
	public E next() {
		if (!hasNext()) throw new NoSuchElementException();
		return values[currentIndex++];
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	public static <E> ArrayIterator<E> of(E[] array) {
		return new ArrayIterator<>(array);
	}

}
