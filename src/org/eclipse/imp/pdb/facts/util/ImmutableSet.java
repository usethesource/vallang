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

public interface ImmutableSet<E> extends ImmutableCollection<E>, Set<E> {
	
    E get(Object o);
    
    E getEquivalent(Object o, Comparator<Object> cmp);
		
	ImmutableSet<E> __insert(E e);
	
	ImmutableSet<E> __insertEquivalent(E e, Comparator<Object> cmp);

	ImmutableSet<E> __insertAll(ImmutableSet<? extends E> set);	
	
	ImmutableSet<E> __insertAllEquivalent(ImmutableSet<? extends E> set, Comparator<Object> cmp);
	
	ImmutableSet<E> __retainAll(ImmutableSet<? extends E> set);
	
	ImmutableSet<E> __retainAllEquivalent(ImmutableSet<? extends E> set, Comparator<Object> cmp);
	
	ImmutableSet<E> __removeAll(ImmutableSet<? extends E> set);
	
	ImmutableSet<E> __removeAllEquivalent(ImmutableSet<? extends E> set, Comparator<Object> cmp);
	
	ImmutableSet<E> __remove(E e);

	ImmutableSet<E> __removeEquivalent(E e, Comparator<Object> cmp);

	SupplierIterator<E, E> keyIterator();	
	
	public abstract TransientSet<E> asTransient();

	public abstract boolean isTransientSupported();

}