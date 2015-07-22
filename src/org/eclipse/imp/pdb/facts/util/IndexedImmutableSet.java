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
package org.eclipse.imp.pdb.facts.util;

import java.util.Collection;
import java.util.Set;

public interface IndexedImmutableSet<K> extends Set<K> {

	K get(final Object o);

	IndexedImmutableSet<K> __insert(final K key);

	IndexedImmutableSet<K> __remove(final K key);

	@Override
	default void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	default boolean add(final K key) {
		throw new UnsupportedOperationException();
	}

	@Override
	default boolean addAll(Collection<? extends K> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	default boolean remove(final Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	default boolean removeAll(final Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	default boolean retainAll(final Collection<?> c) {
		throw new UnsupportedOperationException();
	}	
	
}