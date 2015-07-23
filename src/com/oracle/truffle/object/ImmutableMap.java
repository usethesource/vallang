/*******************************************************************************
 * Copyright (c) 2013-2015 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI  
 *******************************************************************************/
package com.oracle.truffle.object;

import java.util.Map;

public interface ImmutableMap<K, V> extends Map<K, V> {

	ImmutableMap<K, V> copyAndPut(final K key, final V value);

	ImmutableMap<K, V> copyAndRemove(final K key);

	@Override
	default V put(final K key, final V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	default void putAll(final Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	default V remove(final Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	default void clear() {
		throw new UnsupportedOperationException();
	}

}