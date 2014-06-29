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

public interface ImmutableMap<K, V> extends Map<K, V> {

    boolean containsKey(Object o);
    
	boolean containsKeyEquivalent(Object o, Comparator<Object> cmp);

    V get(Object o);
    
    V getEquivalent(Object o, Comparator<Object> cmp);
	
    boolean containsValue(Object o);
    
	boolean containsValueEquivalent(Object o, Comparator<Object> cmp);
	
//	boolean containsAll(Collection<?> c);
//	
//	boolean containsAllEquivalent(Collection<?> c, Comparator<Object> cmp);
	
	ImmutableMap<K, V> __put(K key, V value);

	ImmutableMap<K, V> __putEquivalent(K key, V value, Comparator<Object> cmp);
	
	ImmutableMap<K, V> __putAll(Map<? extends K, ? extends V> map);
	
	ImmutableMap<K, V> __putAllEquivalent(Map<? extends K, ? extends V> map, Comparator<Object> cmp);
	
	ImmutableMap<K, V> __remove(K key);
	
	ImmutableMap<K, V> __removeEquivalent(K key, Comparator<Object> cmp);

	Iterator<K> keyIterator();
	
	Iterator<V> valueIterator();
	
	Iterator<Map.Entry<K, V>> entryIterator();
	
//	SupplierIterator<K, V> supplierIterator();
	
	public abstract TransientMap<K, V> asTransient();

	public abstract boolean isTransientSupported();
	
}
