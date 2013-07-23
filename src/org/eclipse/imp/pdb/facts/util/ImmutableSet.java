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
import java.util.Set;

public interface ImmutableSet<E> extends Set<E> {
	ImmutableSet<E> __insert(E e);

	ImmutableSet<E> __insertAll(Set<? extends E> set);
	
	ImmutableSet<E> __remove(E e);

	/*
	 * TODO: move up to Collection Interface that is concerned with different
	 * equalities
	 */
	boolean containsEquivalent(Object o, Comparator<Object> cmp);

	ImmutableSet<E> __insertEquivalent(E e, Comparator<Object> cmp);
	
	ImmutableSet<E> __insertAllEquivalent(Set<? extends E> set, Comparator<Object> cmp);

	ImmutableSet<E> __removeEquivalent(E e, Comparator<Object> cmp);

}