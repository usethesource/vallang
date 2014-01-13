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

import java.util.Comparator;
import java.util.Map;

public interface TransientMap<E,V> { // extends ImmutableCollection<E>, Set<E> {

	boolean __put(E e, V v);
	
	boolean __putEquivalent(E e, V v, Comparator<Object> cmp);

	boolean __putAll(Map<? extends E, ? extends V> map);	
	
	boolean __putAllEquivalent(Map<? extends E, ? extends V> map, Comparator<Object> cmp);
	
	boolean __remove(E e);
	
	boolean __removeEquivalent(E e, Comparator<Object> cmp);
	
	ImmutableMap<E,V> freeze();
	
}
