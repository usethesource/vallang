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
import java.util.Set;

public interface TransientSet<E> extends ImmutableCollection<E>, Set<E> {

    E get(Object o);
    
    E getEquivalent(Object o, Comparator<Object> cmp);
	
    boolean contains(Object o);
    
	boolean containsEquivalent(Object o, Comparator<Object> cmp);
	
	boolean __insert(E e);
	
	boolean __insertEquivalent(E e, Comparator<Object> cmp);

	boolean __insertAll(ImmutableSet<? extends E> set);	
	
	boolean __insertAllEquivalent(ImmutableSet<? extends E> set, Comparator<Object> cmp);
	
	boolean __retainAll(ImmutableSet<? extends E> set);
	
	boolean __retainAllEquivalent(ImmutableSet<? extends E> set, Comparator<Object> cmp);
	
	boolean __remove(E e);
	
	boolean __removeEquivalent(E e, Comparator<Object> cmp);

	boolean __removeAll(ImmutableSet<? extends E> set);
	
	boolean __removeAllEquivalent(ImmutableSet<? extends E> set, Comparator<Object> cmp);
	
	SupplierIterator<E, E> keyIterator();
	
	ImmutableSet<E> freeze();
	
}
