/*******************************************************************************
 * Copyright (c) 2014 CWI
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

public class ArrayKeyValueIterator<K, V> implements Iterator<K> {

	final Object[] values;
	final int end;
	int currentIndex;

	public ArrayKeyValueIterator(final Object[] values, int start, int end) {
		assert start <= end && end <= values.length;
		assert (end - start) % 2 == 0;

		this.values = values;
		this.end = end;
		this.currentIndex = start;
	}

	@Override
	public boolean hasNext() {
		return currentIndex < end;
	}

	@SuppressWarnings("unchecked")
	@Override
	public K next() {
		if (!hasNext())
			throw new NoSuchElementException();

		final K currentKey = (K) values[currentIndex];
		currentIndex += 2;

		return currentKey;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	public static <K, V> ArrayKeyValueIterator<K, V> of(Object[] array) {
		return new ArrayKeyValueIterator<>(array, 0, array.length);
	}

	public static <K, V> ArrayKeyValueIterator<K, V> of(Object[] array, int start, int length) {
		return new ArrayKeyValueIterator<>(array, start, start + length);
	}

}
