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
package org.eclipse.imp.pdb.facts.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

public interface TransientMap<K, V> extends Map<K, V> {

	boolean containsKey(Object o);

	boolean containsKeyEquivalent(Object o, Comparator<Object> cmp);

	V get(Object o);

	V getEquivalent(Object o, Comparator<Object> cmp);

	V __put(K k, V v);

	V __putEquivalent(K k, V v, Comparator<Object> cmp);

	boolean __putAll(Map<? extends K, ? extends V> map);

	boolean __putAllEquivalent(Map<? extends K, ? extends V> map, Comparator<Object> cmp);

	boolean __remove(K k);

	boolean __removeEquivalent(K k, Comparator<Object> cmp);

	SupplierIterator<K, V> keyIterator();
	
	Iterator<V> valueIterator();
	
	Iterator<Map.Entry<K, V>> entryIterator();	
	
	ImmutableMap<K, V> freeze();

}
